package com.pedidos360.backend_pedidos360.controller;

import com.pedidos360.backend_pedidos360.model.Order;
import com.pedidos360.backend_pedidos360.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    // 1. OBTENER TODOS LOS PEDIDOS DESDE NEON (GET)
    @GetMapping
    public List<Order> getOrders() {
        return orderRepository.findAll();
    }

    // 2. CREAR UN NUEVO PEDIDO EN NEON (POST)
    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        if (order.getFecha() == null) {
            order.setFecha(LocalDate.now().toString());
        }
        // Neon / PostgreSQL genera el ID automáticamente mediante @GeneratedValue
        return orderRepository.save(order);
    }

    // 3. CAMBIAR ESTADO DE UN PEDIDO EN NEON (PUT /api/orders/{id}/status)
    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String nuevoEstado = body.get("estado");
        
        return orderRepository.findById(id).map(order -> {
            order.setEstado(nuevoEstado);
            Order updatedOrder = orderRepository.save(order);
            return ResponseEntity.ok(updatedOrder);
        }).orElse(ResponseEntity.notFound().build());
    }

    // 4. ELIMINAR UN PEDIDO EN NEON (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}