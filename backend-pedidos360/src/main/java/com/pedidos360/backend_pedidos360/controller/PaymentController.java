package com.pedidos360.backend_pedidos360.controller;

import cl.transbank.webpay.webpayplus.WebpayPlus;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.backend_pedidos360.service.Sender;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private Sender sender;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Map<String, Object>> pendingOrders = new ConcurrentHashMap<>();

    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody Map<String, Object> payload) {
        try {
            Double totalAmount = Double.parseDouble(payload.get("totalAmount").toString());

            String buyOrder = "O-" + UUID.randomUUID().toString().substring(0, 8);
            String sessionId = "S-" + UUID.randomUUID().toString().substring(0, 8);

            String returnUrl = "https://34.207.115.35.sslip.io/api/payments/commit";

            WebpayPlus.Transaction transaction = new WebpayPlus.Transaction();
            WebpayPlusTransactionCreateResponse response = transaction.create(
                buyOrder,
                sessionId,
                totalAmount,
                returnUrl
            );

            pendingOrders.put(response.getToken(), new HashMap<>(payload));

            Map<String, String> result = new HashMap<>();
            result.put("url", response.getUrl());
            result.put("token", response.getToken());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error al iniciar la transacción con Webpay: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @RequestMapping(value = "/commit", method = {RequestMethod.GET, RequestMethod.POST})
    public void commitPayment(
            @RequestParam(value = "token_ws", required = false) String tokenWs,
            HttpServletResponse httpResponse
    ) throws IOException {
        try {
            if (tokenWs == null || tokenWs.isEmpty()) {
                httpResponse.sendRedirect("https://34.207.115.35.sslip.io/?payment=aborted");
                return;
            }
            WebpayPlus.Transaction transaction = new WebpayPlus.Transaction();
            var response = transaction.commit(tokenWs);

            Map<String, Object> orderData = pendingOrders.remove(tokenWs);

            if (response.getResponseCode() == 0) {
                // Intentar enviar a RabbitMQ pero sin interrumpir la redirección si falla
                try {
                    if (orderData == null) {
                        orderData = new HashMap<>();
                    }
                    orderData.put("buyOrder", response.getBuyOrder());
                    orderData.put("totalAmount", response.getAmount());

                    String jsonOrder = objectMapper.writeValueAsString(orderData);
                    sender.sendMessage(jsonOrder);
                } catch (Exception amqpEx) {
                    System.err.println("Error en RabbitMQ: " + amqpEx.getMessage());
                }

                // Redirigir al frontend desplegado en AWS EC2
                httpResponse.sendRedirect("https://34.207.115.35.sslip.io/?payment=success&buyOrder=" + response.getBuyOrder());
            } else {
                httpResponse.sendRedirect("https://34.207.115.35.sslip.io/?payment=rejected");
            }
        } catch (Exception e) {
            e.printStackTrace();
            httpResponse.sendRedirect("https://34.207.115.35.sslip.io/?payment=error");
        }
    }
}