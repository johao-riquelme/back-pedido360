package com.pedidos360.backend_pedidos360.service;

import com.pedidos360.backend_pedidos360.model.EventoPedido;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    @KafkaListener(topics = "pedidos-eventos", groupId = "pedidos360-kafka-group")
    public void escucharEvento(
            @Payload EventoPedido evento,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        System.out.printf("Kafka recibió pedido -> Partición: %d | Offset: %d | Datos: %s%n",
                partition, offset, evento);
        
        // Aquí agregas tu lógica (ej. auditoría, analítica, notificaciones)
    }
}