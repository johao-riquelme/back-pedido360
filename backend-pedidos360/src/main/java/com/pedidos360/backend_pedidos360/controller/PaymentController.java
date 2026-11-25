package com.pedidos360.backend_pedidos360.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pedidos360.backend_pedidos360.model.EventoPago;
import com.pedidos360.backend_pedidos360.model.EventoPedido;
import com.pedidos360.backend_pedidos360.model.Order;
import com.pedidos360.backend_pedidos360.model.PaymentTransaction;
import com.pedidos360.backend_pedidos360.model.Product;
import com.pedidos360.backend_pedidos360.repository.OrderRepository;
import com.pedidos360.backend_pedidos360.repository.PaymentTransactionRepository;
import com.pedidos360.backend_pedidos360.repository.ProductRepository;
import com.pedidos360.backend_pedidos360.service.KafkaProducerService;
import com.pedidos360.backend_pedidos360.service.Sender;

import cl.transbank.webpay.webpayplus.WebpayPlus;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final Sender sender;
    private final KafkaProducerService kafkaProducerService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final ObjectMapper objectMapper;

    @Value("${pedidos360.public-url:https://3.85.124.151.sslip.io}")
    private String publicUrl;

    @Value("${pedidos360.frontend-url:https://3.85.124.151.sslip.io}")
    private String frontendUrl;

    public PaymentController(
            Sender sender,
            KafkaProducerService kafkaProducerService,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            PaymentTransactionRepository paymentRepository,
            ObjectMapper objectMapper) {
        this.sender = sender;
        this.kafkaProducerService = kafkaProducerService;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody Map<String, Object> payload) {
        try {
            if (payload.get("orderId") == null || !(payload.get("items") instanceof List<?>)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Faltan orderId o items."));
            }

            Long orderId = Long.valueOf(payload.get("orderId").toString());
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));

            if (!"CREADO".equalsIgnoreCase(order.getEstado())) {
                return ResponseEntity.badRequest().body(Map.of("error", "El pedido no está en estado CREADO."));
            }

            // Marcar como EXPIRED transacciones 'CREATED' anteriores abandonadas para permitir reintentos
            List<PaymentTransaction> previas = paymentRepository.findByOrderId(orderId);
            for (PaymentTransaction p : previas) {
                if ("AUTHORIZED".equals(p.getEstado()) || "PROCESSED".equals(p.getEstado())) {
                    return ResponseEntity.status(409).body(Map.of(
                            "error", "Este pedido ya tiene una transacción confirmada o en proceso."));
                }
                if ("CREATED".equals(p.getEstado())) {
                    p.setEstado("EXPIRED");
                    paymentRepository.save(p);
                }
            }
            paymentRepository.flush();

            List<?> rawItems = (List<?>) payload.get("items");
            if (rawItems.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El carrito está vacío."));
            }

            Map<Long, Integer> quantities = new TreeMap<>();
            for (Object raw : rawItems) {
                if (!(raw instanceof Map<?, ?> item) ||
                        item.get("productId") == null || item.get("quantity") == null) {
                    throw new IllegalArgumentException("Producto o cantidad inválidos.");
                }
                Long productId = Long.valueOf(item.get("productId").toString());
                int quantity = Integer.parseInt(item.get("quantity").toString());
                if (quantity <= 0) {
                    throw new IllegalArgumentException("Cantidad inválida.");
                }
                quantities.merge(productId, quantity, Math::addExact);
            }

            BigDecimal total = BigDecimal.ZERO;
            List<Map<String, Object>> verifiedItems = new ArrayList<>();
            for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
                Product product = productRepository.findById(entry.getKey())
                        .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));
                int quantity = entry.getValue();
                if (product.getStock() == null || product.getStock() < quantity) {
                    throw new IllegalArgumentException("Stock insuficiente para " + product.getNombre());
                }
                BigDecimal price = BigDecimal.valueOf(product.getPrecio());
                total = total.add(price.multiply(BigDecimal.valueOf(quantity)));

                Map<String, Object> item = new HashMap<>();
                item.put("productId", entry.getKey());
                item.put("quantity", quantity);
                item.put("price", product.getPrecio());
                item.put("nombre", product.getNombre());
                verifiedItems.add(item);
            }

            if (order.getTotal() == null ||
                    total.signum() <= 0 ||
                    total.compareTo(BigDecimal.valueOf(order.getTotal())) != 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "El monto del pedido no coincide con los precios de Neon."));
            }

            // Validar monto mínimo exigido por Transbank Webpay Plus ($50 CLP)
            if (total.compareTo(BigDecimal.valueOf(50)) < 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "El monto mínimo para pagar con Webpay es de $50 CLP."));
            }

            // Guardar el carrito verificado, no el precio enviado por el navegador.
            String verifiedJson = objectMapper.writeValueAsString(verifiedItems);
            order.setDetalleProductos(verifiedJson);
            order.setCantidad(quantities.values().stream().reduce(0, Math::addExact));
            order.setProductoId(verifiedItems.size() == 1 ? quantities.keySet().iterator().next() : null);
            order.setProductoNombre(verifiedItems.size() == 1 ? (String) verifiedItems.get(0).get("nombre") : null);
            order.setMetodoPago("Webpay Transbank");
            orderRepository.saveAndFlush(order);

            String buyOrder = "O-" + UUID.randomUUID().toString().substring(0, 8);
            String sessionId = "S-" + UUID.randomUUID().toString().substring(0, 8);
            String returnUrl = baseUrl() + "/api/payments/commit";

            WebpayPlus.Transaction transaction = new WebpayPlus.Transaction();
            WebpayPlusTransactionCreateResponse webpay = transaction.create(
                    buyOrder, sessionId, total.doubleValue(), returnUrl);

            PaymentTransaction payment = new PaymentTransaction();
            payment.setOrderId(orderId);
            payment.setBuyOrder(buyOrder);
            payment.setTokenHash(sha256(webpay.getToken()));
            payment.setAmount(total);
            payment.setItemsJson(verifiedJson);
            payment.setEstado("CREATED");
            paymentRepository.saveAndFlush(payment);

            return ResponseEntity.ok(Map.of("url", webpay.getUrl(), "token", webpay.getToken()));

        } catch (IllegalArgumentException | ArithmeticException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[Webpay] Error al iniciar transacción", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "No fue posible iniciar Webpay. Intenta nuevamente."));
        }
    }

    @RequestMapping(value = "/commit", method = {RequestMethod.GET, RequestMethod.POST})
    public void commitPayment(
            @RequestParam(value = "token_ws", required = false) String tokenWs,
            @RequestParam(value = "TBK_TOKEN", required = false) String abortToken,
            HttpServletResponse httpResponse) throws IOException {

        // Manejo de anulación explícita del usuario en la pasarela (TBK_TOKEN)
        if (tokenWs == null || tokenWs.isBlank()) {
            if (abortToken != null && !abortToken.isBlank()) {
                paymentRepository.findByTokenHash(sha256(abortToken)).ifPresent(p -> {
                    p.setEstado("ABORTED");
                    paymentRepository.saveAndFlush(p);
                });
            }
            redirect(httpResponse, "aborted");
            return;
        }

        try {
            PaymentTransaction payment = paymentRepository.findByTokenHash(sha256(tokenWs))
                    .orElse(null);

            if (payment == null) {
                log.error("[Webpay] No existe una transacción local para el token recibido.");
                redirect(httpResponse, "pending");
                return;
            }

            if ("PROCESSED".equals(payment.getEstado())) {
                redirect(httpResponse, "success");
                return;
            }
            if ("REJECTED".equals(payment.getEstado())) {
                redirect(httpResponse, "rejected");
                return;
            }

            if ("CREATED".equals(payment.getEstado())) {
                WebpayPlus.Transaction transaction = new WebpayPlus.Transaction();
                var response = transaction.commit(tokenWs);

                boolean authorized = response.getResponseCode() == 0
                        && "AUTHORIZED".equalsIgnoreCase(response.getStatus());
                if (!authorized) {
                    payment.setEstado("REJECTED");
                    paymentRepository.saveAndFlush(payment);
                    redirect(httpResponse, "rejected");
                    return;
                }

                BigDecimal amount = BigDecimal.valueOf(response.getAmount());
                if (!payment.getBuyOrder().equals(response.getBuyOrder()) ||
                        payment.getAmount().compareTo(amount) != 0) {
                    log.error("[Webpay] Referencia o importe no coincide, pago #{}", payment.getId());
                    redirect(httpResponse, "pending");
                    return;
                }

                payment.setEstado("AUTHORIZED");
                paymentRepository.saveAndFlush(payment);
            } else if (!"AUTHORIZED".equals(payment.getEstado())) {
                redirect(httpResponse, "pending");
                return;
            }

            publishPayment(payment);
            redirect(httpResponse, "success");

        } catch (Exception e) {
            log.error("[Webpay] Confirmación pendiente", e);
            redirect(httpResponse, "pending");
        }
    }

    private void publishPayment(PaymentTransaction payment) throws Exception {
        // 1. Mensajería transaccional en RabbitMQ
        List<Map<String, Object>> items = objectMapper.readValue(
                payment.getItemsJson(), new TypeReference<List<Map<String, Object>>>() { });

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", payment.getOrderId());
        orderData.put("buyOrder", payment.getBuyOrder());
        orderData.put("totalAmount", payment.getAmount());
        orderData.put("items", items);
        sender.sendMessage(objectMapper.writeValueAsString(orderData));

        // 2. Streaming de evento en Kafka - Tópico 1: pedidos-eventos
        EventoPedido eventoKafka = new EventoPedido(
                String.valueOf(payment.getOrderId()),
                "AUTHORIZED",
                payment.getAmount().doubleValue()
        );
        kafkaProducerService.enviarEvento(eventoKafka.getPedidoId(), eventoKafka);

        // 3. Streaming de evento en Kafka - Tópico 2: pagos-eventos
        EventoPago eventoPago = new EventoPago(
                payment.getBuyOrder(),
                payment.getOrderId(),
                payment.getAmount().doubleValue(),
                "AUTHORIZED"
        );
        kafkaProducerService.enviarEventoPago(payment.getBuyOrder(), eventoPago);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private String baseUrl() {
        return publicUrl.replaceAll("/+$", "");
    }

    private void redirect(HttpServletResponse response, String paymentState) throws IOException {
        response.sendRedirect(frontendUrl.replaceAll("/+$", "") + "/customer?payment=" + paymentState);
    }
}
