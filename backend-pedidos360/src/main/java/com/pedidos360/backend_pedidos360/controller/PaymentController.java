package com.pedidos360.backend_pedidos360.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import cl.transbank.webpay.webpayplus.WebpayPlus;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*") // Permite peticiones desde tu frontend en Angular
public class PaymentController {

    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody Map<String, Object> payload) {
        try {
            // 1. Obtener el monto total enviado desde el frontend
            Double totalAmount = Double.parseDouble(payload.get("totalAmount").toString());
            
            // 2. Generar identificadores únicos de compra y sesión
            String buyOrder = "O-" + UUID.randomUUID().toString().substring(0, 8);
            String sessionId = "S-" + UUID.randomUUID().toString().substring(0, 8);
            
            // 3. URL de retorno tras el pago en Transbank
            String returnUrl = "http://localhost:8080/api/payments/commit";

            // 4. Crear la instancia de la transacción e iniciar en Webpay Plus (SDK v3.x)
            WebpayPlus.Transaction transaction = new WebpayPlus.Transaction();
            WebpayPlusTransactionCreateResponse response = transaction.create(
                buyOrder, 
                sessionId, 
                totalAmount, 
                returnUrl
            );

            // 5. Retornar la URL de Transbank y el token hacia Angular
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

    // Endpoint de retorno donde Transbank responde tras el pago
    @RequestMapping(value = "/commit", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> commitPayment(@RequestParam("token_ws") String tokenWs) {
        try {
            // Instanciar la transacción y confirmar el pago con Transbank (SDK v3.x)
            WebpayPlus.Transaction transaction = new WebpayPlus.Transaction();
            var response = transaction.commit(tokenWs);
            
            if (response.getResponseCode() == 0) {
                // AQUÍ ENGANCHAREMOS EL PASO 3: Publicar evento a RabbitMQ para guardar la orden automáticamente
                return ResponseEntity.ok("Pago Aprobado en Transbank. Orden: " + response.getBuyOrder());
            } else {
                return ResponseEntity.badRequest().body("El pago fue rechazado por Transbank.");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al confirmar el pago: " + e.getMessage());
        }
    }
}
