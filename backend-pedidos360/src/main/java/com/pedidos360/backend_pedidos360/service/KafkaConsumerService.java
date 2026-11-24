package com.pedidos360.backend_pedidos360.service;

import com.pedidos360.backend_pedidos360.config.KafkaTopicConfig;
import com.pedidos360.backend_pedidos360.model.EventoPago;
import com.pedidos360.backend_pedidos360.model.EventoPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    @RetryableTopic(attempts = "3", backOff = @BackOff(delay = 2000))
    @KafkaListener(topics = KafkaTopicConfig.TOPIC_PEDIDOS, groupId = "pedidos360-pedidos-group")
    public void consumirEventoPedido(
            @Payload EventoPedido evento,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        if (evento == null || evento.getPedidoId() == null) {
            log.warn("[KAFKA CONSUMER PEDIDOS] Payload inválido descartado en partición {}", partition);
            return;
        }
        procesarLogicaPedido(evento, partition, offset);
    }

    @RetryableTopic(attempts = "3", backOff = @BackOff(delay = 2000))
    @KafkaListener(topics = KafkaTopicConfig.TOPIC_PAGOS, groupId = "pedidos360-auditoria-pagos-group")
    public void consumirEventoPago(
            @Payload EventoPago eventoPago,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        if (eventoPago == null || eventoPago.getBuyOrder() == null || eventoPago.getMonto() == null) {
            log.warn("[KAFKA CONSUMER PAGOS] Payload de pago incompleto en partición {}", partition);
            return;
        }
        procesarAuditoriaPago(eventoPago, partition, offset);
    }

    // Separación entre infraestructura de mensajería y lógica de negocio
    private void procesarLogicaPedido(EventoPedido evento, int partition, long offset) {
        log.info("[KAFKA PEDIDOS] Partición: {} | Offset: {} | Procesando pedido: {}", partition, offset, evento);
    }

    private void procesarAuditoriaPago(EventoPago eventoPago, int partition, long offset) {
        log.info("[KAFKA AUDITORÍA PAGOS] Partición: {} | Offset: {} | Registrando pago: {}", partition, offset, eventoPago);
    }
}