package com.pedidos360.backend_pedidos360.repository;

import com.pedidos360.backend_pedidos360.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}