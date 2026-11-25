package com.pedidos360.backend_pedidos360.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import com.pedidos360.backend_pedidos360.model.Order;
import com.pedidos360.backend_pedidos360.model.Product;
import com.pedidos360.backend_pedidos360.model.PaymentTransaction;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pedidos360.backend_pedidos360.config.RabbitMQConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class Receiver {

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.PAYMENTS_QUEUE)
    public void receiveMessage(String message) {

        try {
            // 1. Leer el evento recibido desde RabbitMQ
            JsonNode evento = objectMapper.readTree(message);

            if (evento == null ||
                !evento.path("orderId").canConvertToLong() ||
                !evento.path("buyOrder").isTextual() ||
                !evento.path("totalAmount").isNumber()) {

                throw new IllegalArgumentException(
                    "El mensaje de RabbitMQ es inválido."
                );
            }

            Long orderId = evento.get("orderId").longValue();
            String buyOrder = evento.get("buyOrder").asText();
            BigDecimal totalMensaje =
                evento.get("totalAmount").decimalValue();

            if (orderId <= 0 || buyOrder.isBlank()) {
                throw new IllegalArgumentException(
                    "El identificador del pedido o de Webpay es inválido."
                );
            }

            // 2. Buscar y bloquear la transacción de Webpay
            PaymentTransaction pago = entityManager
                .createQuery(
                    "SELECT p FROM PaymentTransaction p " +
                    "WHERE p.buyOrder = :buyOrder",
                    PaymentTransaction.class
                )
                .setParameter("buyOrder", buyOrder)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst()
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Transacción Webpay no encontrada: " + buyOrder
                    )
                );

            // 3. Comprobar que pertenece al pedido correcto
            if (!orderId.equals(pago.getOrderId())) {
                throw new IllegalStateException(
                    "El orderId no coincide con la transacción."
                );
            }

            if (pago.getAmount() == null ||
                pago.getAmount().compareTo(totalMensaje) != 0) {

                throw new IllegalStateException(
                    "El importe no coincide con la transacción Webpay."
                );
            }

            // 4. Evitar procesar dos veces el mismo pago
            if ("PROCESSED".equalsIgnoreCase(pago.getEstado())) {

                System.out.println(
                    "[RabbitMQ] Pago ya procesado: " + buyOrder
                );

                return;
            }

            // Solamente se pueden procesar pagos autorizados
            if (!"AUTHORIZED".equalsIgnoreCase(pago.getEstado())) {

                throw new IllegalStateException(
                    "El pago no está autorizado. Estado: " +
                    pago.getEstado()
                );
            }

            // 5. Buscar y bloquear el pedido original
            Order pedido = entityManager.find(
                Order.class,
                orderId,
                LockModeType.PESSIMISTIC_WRITE
            );

            if (pedido == null) {
                throw new IllegalStateException(
                    "Pedido no encontrado: #" + orderId
                );
            }

            if (!"CREADO".equalsIgnoreCase(pedido.getEstado())) {

                throw new IllegalStateException(
                    "El pedido #" + orderId +
                    " no está en estado CREADO. Estado actual: " +
                    pedido.getEstado()
                );
            }

            if (pedido.getTotal() == null ||
                BigDecimal.valueOf(pedido.getTotal())
                    .compareTo(pago.getAmount()) != 0) {

                throw new IllegalStateException(
                    "El total del pedido no coincide con el pago."
                );
            }

            // 6. Recuperar los productos validados que
            // fueron guardados en Neon al iniciar Webpay.
            List<Map<String, Object>> items =
                objectMapper.readValue(
                    pago.getItemsJson(),
                    new TypeReference<List<Map<String, Object>>>() {}
                );

            if (items == null || items.isEmpty()) {
                throw new IllegalStateException(
                    "La transacción no contiene productos."
                );
            }

            Map<Long, Integer> cantidades = new TreeMap<>();

            for (Map<String, Object> item : items) {

                if (item.get("productId") == null ||
                    item.get("quantity") == null) {

                    throw new IllegalArgumentException(
                        "Producto inválido en la transacción."
                    );
                }

                Long productId = Long.valueOf(
                    item.get("productId").toString()
                );

                int quantity = Integer.parseInt(
                    item.get("quantity").toString()
                );

                if (productId <= 0 || quantity <= 0) {
                    throw new IllegalArgumentException(
                        "Identificador o cantidad inválida."
                    );
                }

                cantidades.merge(
                    productId,
                    quantity,
                    Math::addExact
                );
            }

            // 7. Descontar el inventario con bloqueos
            for (Map.Entry<Long, Integer> item :
                    cantidades.entrySet()) {

                Long productId = item.getKey();
                int quantity = item.getValue();

                Product producto = entityManager.find(
                    Product.class,
                    productId,
                    LockModeType.PESSIMISTIC_WRITE
                );

                if (producto == null) {
                    throw new IllegalStateException(
                        "Producto no encontrado: " + productId
                    );
                }

                if (producto.getStock() == null ||
                    producto.getStock() < quantity) {

                    throw new IllegalStateException(
                        "Stock insuficiente para producto " +
                        productId
                    );
                }

                int nuevoStock =
                    producto.getStock() - quantity;

                producto.setStock(nuevoStock);

                System.out.println(
                    "[RabbitMQ] Producto #" + productId +
                    " | Nuevo stock: " + nuevoStock
                );
            }

            // 8. Actualizar el pedido ORIGINAL
            // Se conserva el nombre de la empresa.
            pedido.setDetalleProductos(pago.getItemsJson());
            pedido.setCantidad(cantidades.values().stream().reduce(0, Math::addExact));
            pedido.setProductoId(cantidades.size() == 1 ? cantidades.keySet().iterator().next() : null);
            Object nombre = items.size() == 1 ? items.get(0).get("nombre") : null;
            pedido.setProductoNombre(nombre instanceof String ? (String) nombre : null);
            pedido.setMetodoPago("Webpay Transbank");
            pedido.setEstado("PAGADO");

            // 9. Marcar la transacción como procesada
            pago.setEstado("PROCESSED");

            // 10. Sincronizar cambios con la base de datos.
            // La transacción se confirma al finalizar
            // correctamente este método.
            entityManager.flush();

            System.out.println(
                "[RabbitMQ] Pedido #" + orderId +
                " actualizado a PAGADO."
            );

            System.out.println(
                "[RabbitMQ] Transacción " + buyOrder +
                " actualizada a PROCESSED."
            );

        } catch (Exception e) {

            System.err.println(
                "[RabbitMQ] Error al procesar pago: " +
                e.getMessage()
            );

            // Propagar el error permite revertir
            // los cambios de PostgreSQL.
            throw new IllegalStateException(
                "No se pudo procesar el pago recibido.",
                e
            );
        }
    }
}
