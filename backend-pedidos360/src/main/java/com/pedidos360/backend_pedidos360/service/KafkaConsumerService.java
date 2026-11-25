package com.pedidos360.backend_pedidos360.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.pedidos360.backend_pedidos360.config.KafkaTopicConfig;
import com.pedidos360.backend_pedidos360.model.EventoPago;
import com.pedidos360.backend_pedidos360.model.EventoPedido;

@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_PEDIDOS, groupId = "pedidos360-pedidos-group")
    public void consumirEventoPedido(
            @Payload EventoPedido evento,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        if (evento == null || evento.getPedidoId() == null) {
            log.warn("[KAFKA CONSUMER PEDIDOS] Payload inválido descartado en partición {}", partition);
            return;
        }
        log.info("[KAFKA PEDIDOS] Partición: {} | Offset: {} | Procesando pedido: {}", partition, offset, evento);
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_PAGOS, groupId = "pedidos360-auditoria-pagos-group")
    public void consumirEventoPago(
            @Payload EventoPago eventoPago,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        if (eventoPago == null || eventoPago.getBuyOrder() == null || eventoPago.getMonto() == null) {
            log.warn("[KAFKA CONSUMER PAGOS] Payload de pago incompleto en partición {}", partition);
            return;
        }
        log.info("[KAFKA AUDITORÍA PAGOS] Partición: {} | Offset: {} | Registrando pago: {}", partition, offset, eventoPago);
    }
}