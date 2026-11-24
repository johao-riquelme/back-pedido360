package com.pedidos360.backend_pedidos360.model;

import java.time.LocalDateTime;

public class EventoPago {

    private String buyOrder;
    private Long orderId;
    private Double monto;
    private String estadoPago;
    private String timestamp;

    public EventoPago() {
    }

    public EventoPago(String buyOrder, Long orderId, Double monto, String estadoPago) {
        this.buyOrder = buyOrder;
        this.orderId = orderId;
        this.monto = monto;
        this.estadoPago = estadoPago;
        this.timestamp = LocalDateTime.now().toString();
    }

    public String getBuyOrder() { return buyOrder; }
    public void setBuyOrder(String buyOrder) { this.buyOrder = buyOrder; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "EventoPago{buyOrder='" + buyOrder + "', orderId=" + orderId +
               ", monto=" + monto + ", estadoPago='" + estadoPago + "', timestamp='" + timestamp + "'}";
    }
}