package com.pedidos360.backend_pedidos360.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pedidos360.backend_pedidos360.model.Order;
import com.pedidos360.backend_pedidos360.repository.OrderRepository;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public List<Order> getOrders() {
        return orderRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Order order) {

        if (order == null) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "No se recibieron datos del pedido.")
            );
        }

        if (order.getCliente() == null ||
            order.getCliente().isBlank()) {

            return ResponseEntity.badRequest().body(
                Map.of("error", "El nombre de la empresa es obligatorio.")
            );
        }

        if (order.getTotal() == null ||
            !Double.isFinite(order.getTotal()) ||
            order.getTotal() <= 0) {

            return ResponseEntity.badRequest().body(
                Map.of("error", "El total del pedido no es válido.")
            );
        }

        order.setId(null);
        order.setEstado("CREADO");
        order.setFecha(LocalDate.now().toString());

        if ((order.getDetalleProductos() == null || order.getDetalleProductos().isBlank())
                && order.getProductoNombre() != null && !order.getProductoNombre().isBlank()) {
            int qty = (order.getCantidad() != null && order.getCantidad() > 0) ? order.getCantidad() : 1;
            order.setDetalleProductos(order.getProductoNombre().trim() + " (x" + qty + ")");
        }

        Order nuevoPedido = orderRepository.save(order);

        System.out.println(
            "[Pedidos360] Pedido creado #" + nuevoPedido.getId()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(nuevoPedido);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            Map.of(
                "error",
                "El estado del pedido no puede modificarse " +
                "desde esta ruta."
            )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable Long id) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            Map.of(
                "error",
                "La eliminación de pedidos está deshabilitada."
            )
        );
    }
}