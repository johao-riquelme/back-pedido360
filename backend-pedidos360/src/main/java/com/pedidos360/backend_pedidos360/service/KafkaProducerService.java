package com.pedidos360.backend_pedidos360.service;

import com.pedidos360.backend_pedidos360.model.EventoPedido;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, EventoPedido> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, EventoPedido> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void enviarEvento(String key, EventoPedido evento) {
        // Envía al topic usando la key para asegurar el orden por pedido/usuario
        kafkaTemplate.send("pedidos-eventos", key, evento);
    }
}