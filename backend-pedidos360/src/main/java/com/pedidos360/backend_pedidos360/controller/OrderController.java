package com.pedidos360.backend_pedidos360.controller;

import com.pedidos360.backend_pedidos360.model.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final List<Order> orders = new ArrayList<>();

    public OrderController() {
        orders.add(new Order(101L, "Empresa Alfa (Backend Spring)", 250000.0, "Pendiente", "2026-03-08"));
        orders.add(new Order(102L, "Comercial Beta (Backend Spring)", 120000.0, "Enviado", "2026-03-09"));
        orders.add(new Order(103L, "Distribuidora Gamma (Backend Spring)", 450000.0, "Entregado", "2026-03-10"));
    }

    // 1. OBTENER TODOS LOS PEDIDOS (GET)
    @GetMapping
    public List<Order> getOrders() {
        return orders;
    }

    // 2. CREAR UN NUEVO PEDIDO (POST)
    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        order.setId((long) (orders.size() + 101));
        if (order.getFecha() == null) {
            order.setFecha(LocalDate.now().toString());
        }
        orders.add(order);
        return order;
    }

    // 3. CAMBIAR ESTADO DE UN PEDIDO (PUT)
    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String nuevoEstado = body.get("estado");
        for (Order order : orders) {
            if (order.getId().equals(id)) {
                order.setEstado(nuevoEstado);
                return ResponseEntity.ok(order);
            }
        }
        return ResponseEntity.notFound().build();
    }

    // 4. ELIMINAR UN PEDIDO (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        boolean eliminado = orders.removeIf(order -> order.getId().equals(id));
        if (eliminado) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}