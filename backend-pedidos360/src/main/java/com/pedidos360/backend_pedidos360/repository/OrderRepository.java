package com.pedidos360.backend_pedidos360.repository;

import com.pedidos360.backend_pedidos360.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}