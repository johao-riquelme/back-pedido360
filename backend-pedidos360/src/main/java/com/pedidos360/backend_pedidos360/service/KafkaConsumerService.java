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
    private final AuditoriaKafkaService auditoria;

    public KafkaConsumerService(AuditoriaKafkaService auditoria) {
        this.auditoria = auditoria;
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_PEDIDOS, groupId = "pedidos360-pedidos-group")
    public void consumirEventoPedido(
            @Payload EventoPedido evento,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        if (evento == null || !textoValido(evento.getPedidoId(), 120)
                || !textoValido(evento.getEstado(), 80) || !montoValido(evento.getMonto())) {
            log.warn("[KAFKA CONSUMER PEDIDOS] Payload invalido descartado: particion {} offset {}", partition, offset);
            return;
        }
        int filas = auditoria.registrar(KafkaTopicConfig.TOPIC_PEDIDOS, partition, offset,
                evento.getPedidoId(), evento.getPedidoId(), evento.getEstado(), evento.getMonto());
        log.info("[KAFKA PEDIDOS] Particion: {} | Offset: {} | Pedido: {} | Auditoria: {}",
                partition, offset, evento.getPedidoId(), filas == 1 ? "PERSISTIDO" : "DUPLICADO");
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_PAGOS, groupId = "pedidos360-auditoria-pagos-group")
    public void consumirEventoPago(
            @Payload EventoPago eventoPago,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        if (eventoPago == null || eventoPago.getOrderId() == null
                || !textoValido(eventoPago.getBuyOrder(), 120)
                || !textoValido(eventoPago.getEstadoPago(), 80) || !montoValido(eventoPago.getMonto())) {
            log.warn("[KAFKA CONSUMER PAGOS] Payload invalido descartado: particion {} offset {}", partition, offset);
            return;
        }
        int filas = auditoria.registrar(KafkaTopicConfig.TOPIC_PAGOS, partition, offset,
                eventoPago.getOrderId().toString(), eventoPago.getBuyOrder(),
                eventoPago.getEstadoPago(), eventoPago.getMonto());
        log.info("[KAFKA AUDITORIA PAGOS] Particion: {} | Offset: {} | Pedido: {} | Auditoria: {}",
                partition, offset, eventoPago.getOrderId(), filas == 1 ? "PERSISTIDO" : "DUPLICADO");
    }

    private static boolean textoValido(String valor, int maximo) {
        return valor != null && !valor.isBlank() && valor.length() <= maximo;
    }

    private static boolean montoValido(Double valor) {
        return valor != null && Double.isFinite(valor) && valor >= 0 && valor < 1e16;
    }
}
