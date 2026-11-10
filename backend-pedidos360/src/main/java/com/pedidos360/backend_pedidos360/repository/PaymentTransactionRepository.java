
package com.pedidos360.backend_pedidos360.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pedidos360.backend_pedidos360.model.PaymentTransaction;

@Repository
public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTokenHash(String tokenHash);

    Optional<PaymentTransaction> findByBuyOrder(String buyOrder);

    List<PaymentTransaction> findByOrderId(Long orderId);

    List<PaymentTransaction> findByEstadoIn(
        Collection<String> estados
    );
}
