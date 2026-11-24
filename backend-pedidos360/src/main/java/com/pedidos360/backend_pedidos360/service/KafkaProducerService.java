package com.pedidos360.backend_pedidos360.service;

import com.pedidos360.backend_pedidos360.config.KafkaTopicConfig;
import com.pedidos360.backend_pedidos360.model.EventoPago;
import com.pedidos360.backend_pedidos360.model.EventoPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void enviarEvento(String key, EventoPedido evento) {
        kafkaTemplate.send(KafkaTopicConfig.TOPIC_PEDIDOS, key, evento)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[KAFKA PRODUCER] Enviado a '{}' | Key: {} | Partición: {} | Offset: {}",
                                KafkaTopicConfig.TOPIC_PEDIDOS, key,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("[KAFKA PRODUCER ERROR] Fallo al enviar a '{}': {}",
                                KafkaTopicConfig.TOPIC_PEDIDOS, ex.getMessage());
                    }
                });
    }

    public void enviarEventoPago(String key, EventoPago eventoPago) {
        kafkaTemplate.send(KafkaTopicConfig.TOPIC_PAGOS, key, eventoPago)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[KAFKA PRODUCER] Enviado a '{}' | Key: {} | Partición: {} | Offset: {}",
                                KafkaTopicConfig.TOPIC_PAGOS, key,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("[KAFKA PRODUCER ERROR] Fallo al enviar a '{}': {}",
                                KafkaTopicConfig.TOPIC_PAGOS, ex.getMessage());
                    }
                });
    }
}