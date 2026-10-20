package com.pedidos360.backend_pedidos360.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.backend_pedidos360.model.Order;
import com.pedidos360.backend_pedidos360.model.Product;
import com.pedidos360.backend_pedidos360.repository.OrderRepository;
import com.pedidos360.backend_pedidos360.repository.ProductRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class Receiver {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    @RabbitListener(queues = "hello")
    public void receiveMessage(String message) {
        System.out.println(" [✓] Mensaje recibido de la cola 'hello': '" + message + "'");

        try {
            // 1. Convertir el JSON recibido en un Mapa
            Map<String, Object> orderData = objectMapper.readValue(
                message, new TypeReference<Map<String, Object>>() {}
            );

            // 2. Descontar el stock en la tabla de productos
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");
            
            if (items != null) {
                for (Map<String, Object> item : items) {
                    Long productId = Long.valueOf(item.get("productId").toString());
                    int quantity = Integer.parseInt(item.get("quantity").toString());

                    Product product = productRepository.findById(productId).orElse(null);
                    if (product != null) {
                        int nuevoStock = Math.max(0, product.getStock() - quantity);
                        product.setStock(nuevoStock);
                        productRepository.save(product);
                        System.out.println(" [✓] Stock actualizado para producto ID " + productId + ": " + nuevoStock);
                    }
                }
            }

            // 3. Guardar el pedido usando los atributos exactos de tu Order.java
            Order newOrder = new Order();
            newOrder.setTotal(Double.valueOf(orderData.get("totalAmount").toString()));
            newOrder.setEstado("PAGADO");
            
            // Generar fecha como String
            String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            newOrder.setFecha(fechaActual);
            
            // Usamos 'cliente' para guardar el identificador de Transbank
            newOrder.setCliente("Webpay: " + orderData.get("buyOrder")); 

            orderRepository.save(newOrder);

            System.out.println(" [✓] Pedido guardado exitosamente en la Base de Datos.");

        } catch (Exception e) {
            System.err.println(" [x] Error al procesar el pedido en Receiver: " + e.getMessage());
            e.printStackTrace();
        }
    }
}