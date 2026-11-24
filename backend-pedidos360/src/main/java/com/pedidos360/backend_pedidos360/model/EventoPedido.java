package com.pedidos360.backend_pedidos360.model;

public class EventoPedido {

    private String pedidoId;
    private String estado;
    private Double monto;

    public EventoPedido() {
    }

    public EventoPedido(String pedidoId, String estado, Double monto) {
        this.pedidoId = pedidoId;
        this.estado = estado;
        this.monto = monto;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(String pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    @Override
    public String toString() {
        return "EventoPedido{pedidoId='" + pedidoId + "', estado='" + estado + "', monto=" + monto + "}";
    }
}