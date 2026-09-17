package com.pedidos360.backend_pedidos360.model;

import jakarta.persistence.*;

@Entity
@Table(name = "pedidos")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cliente;
    private Double total;
    private String estado;
    private String fecha;

    public Order() {}

    public Order(Long id, String cliente, Double total, String estado, String fecha) {
        this.id = id;
        this.cliente = cliente;
        this.total = total;
        this.estado = estado;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
}