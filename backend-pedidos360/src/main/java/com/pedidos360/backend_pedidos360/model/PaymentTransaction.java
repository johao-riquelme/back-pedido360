
package com.pedidos360.backend_pedidos360.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "transacciones_pago",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_pago_token_hash",
            columnNames = "token_hash"
        ),
        @UniqueConstraint(
            name = "uk_pago_buy_order",
            columnNames = "buy_order"
        )
    }
)
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID del pedido original en Neon
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    // Huella SHA-256 del token de Webpay.
    // No guardaremos el token original en texto plano.
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    // Referencia de Transbank
    @Column(name = "buy_order", nullable = false, length = 40)
    private String buyOrder;

    // Total validado por el backend
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    // Productos y cantidades validados antes del pago
    @Column(name = "items_json", nullable = false, columnDefinition = "TEXT")
    private String itemsJson;

    // CREATED, AUTHORIZED, PROCESSED, REJECTED
    @Column(nullable = false, length = 30)
    private String estado = "CREATED";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PaymentTransaction() {
    }

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getBuyOrder() {
        return buyOrder;
    }

    public void setBuyOrder(String buyOrder) {
        this.buyOrder = buyOrder;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getItemsJson() {
        return itemsJson;
    }

    public void setItemsJson(String itemsJson) {
        this.itemsJson = itemsJson;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
