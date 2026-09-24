package com.pedidos360.backend_pedidos360.config;

import com.pedidos360.backend_pedidos360.model.Order;
import com.pedidos360.backend_pedidos360.model.Product;
import com.pedidos360.backend_pedidos360.repository.OrderRepository;
import com.pedidos360.backend_pedidos360.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(OrderRepository orderRepository, ProductRepository productRepository) {
        return args -> {
            if (orderRepository.count() == 0) {
                orderRepository.save(new Order(null, "Empresa Alfa (Neon DB)", 250000.0, "Pendiente", "2026-03-08"));
                orderRepository.save(new Order(null, "Comercial Beta (Neon DB)", 120000.0, "Enviado", "2026-03-09"));
                orderRepository.save(new Order(null, "Distribuidora Gamma (Neon DB)", 450000.0, "Entregado", "2026-03-10"));
            }

            if (productRepository.count() == 0) {
                productRepository.save(new Product(null, "Laptop Pro 15", 1200.00, 15, "Electrónica"));
                productRepository.save(new Product(null, "Mouse Inalámbrico", 25.50, 50, "Accesorios"));
                productRepository.save(new Product(null, "Teclado Mecánico", 85.00, 30, "Accesorios"));
            }

            System.out.println(">> Datos de pedidos y productos inicializados en Neon PostgreSQL.");
        };
    }
}