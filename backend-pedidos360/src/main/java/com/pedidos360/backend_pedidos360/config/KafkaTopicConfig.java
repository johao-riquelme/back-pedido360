package com.pedidos360.backend_pedidos360.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String TOPIC_PEDIDOS = "pedidos-eventos";
    public static final String TOPIC_PAGOS = "pagos-eventos";

    @Bean
    public NewTopic pedidosEventosTopic() {
        return TopicBuilder.name(TOPIC_PEDIDOS)
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // 7 días
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    @Bean
    public NewTopic pagosEventosTopic() {
        return TopicBuilder.name(TOPIC_PAGOS)
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "2592000000") // 30 días (auditoría de pagos)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }
}