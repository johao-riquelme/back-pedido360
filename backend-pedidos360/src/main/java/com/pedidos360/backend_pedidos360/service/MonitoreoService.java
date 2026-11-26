package com.pedidos360.backend_pedidos360.service;

import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

@Service
public class MonitoreoService {
    private static final Map<String,String> GROUPS = Map.of(
        "pedidos-eventos", "pedidos360-pedidos-group",
        "pagos-eventos", "pedidos360-auditoria-pagos-group");
    private final JdbcTemplate jdbc;
    private final AuditoriaKafkaService auditoria;
    private final KafkaAdmin kafkaAdmin;
    private final RabbitTemplate rabbit;
    private Map<String,Object> cache;
    private long cachedAt;

    public MonitoreoService(JdbcTemplate jdbc, AuditoriaKafkaService auditoria,
            KafkaAdmin kafkaAdmin, RabbitTemplate rabbit) {
        this.jdbc = jdbc; this.auditoria = auditoria; this.kafkaAdmin = kafkaAdmin; this.rabbit = rabbit;
    }

    public Map<String,Object> auditoria() {
        return Map.of("resumen", auditoria.resumen(), "eventos", jdbc.queryForList("""
            SELECT topico, particion, kafka_offset, pedido_id, estado, monto,
                   to_char(procesado_en AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') AS procesado_en
            FROM public.kafka_auditoria_eventos
            ORDER BY procesado_en DESC, topico, particion, kafka_offset DESC LIMIT 30
            """), "consultadoEn", Instant.now().toString());
    }

    public synchronized Map<String,Object> brokers() {
        if (cache != null && System.nanoTime() - cachedAt < TimeUnit.SECONDS.toNanos(25)) return cache;
        Map<String,Object> result = new LinkedHashMap<>();
        try { result.put("kafka", kafka()); }
        catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            result.put("kafka", Map.of("disponible", false, "error", "No se pudo consultar Kafka"));
        }
        List<Map<String,Object>> queues = new ArrayList<>();
        for (String name : List.of("pedidos360.payments.v2", "pedidos360.payments.failed")) {
            try {
                Map<String,Object> q = rabbit.execute(channel -> {
                    var info = channel.queueDeclarePassive(name);
                    return Map.of("nombre", name, "disponible", true, "pendientes", info.getMessageCount(),
                            "consumidores", info.getConsumerCount());
                });
                queues.add(Objects.requireNonNull(q));
            } catch (Exception ex) { queues.add(Map.of("nombre", name, "disponible", false)); }
        }
        result.put("rabbit", Map.of("colas", queues));
        result.put("consultadoEn", Instant.now().toString());
        cache = result; cachedAt = System.nanoTime();
        return result;
    }

    private Map<String,Object> kafka() throws Exception {
        Map<String,Object> cfg = new HashMap<>(kafkaAdmin.getConfigurationProperties());
        cfg.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
        cfg.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);
        AdminClient admin = AdminClient.create(cfg);
        try {
            int brokers = admin.describeCluster().nodes().get(5, TimeUnit.SECONDS).size();
            var topics = admin.describeTopics(GROUPS.keySet()).allTopicNames().get(5, TimeUnit.SECONDS);
            var groups = admin.describeConsumerGroups(GROUPS.values()).all().get(5, TimeUnit.SECONDS);
            Map<TopicPartition, OffsetSpec> request = new HashMap<>();
            topics.forEach((name, desc) -> desc.partitions().forEach(p ->
                request.put(new TopicPartition(name, p.partition()), OffsetSpec.latest())));
            var ends = admin.listOffsets(request).all().get(5, TimeUnit.SECONDS);
            List<Map<String,Object>> rows = new ArrayList<>();
            long lagTotal = 0; boolean known = true;
            for (String topic : new TreeSet<>(GROUPS.keySet())) {
                String group = GROUPS.get(topic);
                var offsets = admin.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata().get(5, TimeUnit.SECONDS);
                for (var p : topics.get(topic).partitions()) {
                    var tp = new TopicPartition(topic, p.partition());
                    OffsetAndMetadata committed = offsets.get(tp);
                    long end = ends.get(tp).offset();
                    Long lag = committed == null ? null : Math.max(0L, end - committed.offset());
                    if (lag == null) known = false; else lagTotal += lag;
                    Map<String,Object> row = new LinkedHashMap<>();
                    row.put("topico", topic); row.put("grupo", group);
                    row.put("estadoGrupo", groups.get(group).groupState().name());
                    row.put("particion", p.partition()); row.put("replicas", p.replicas().size());
                    row.put("offsetActual", committed == null ? null : committed.offset());
                    row.put("offsetFinal", end); row.put("lag", lag); rows.add(row);
                }
            }
            Map<String,Object> out = new LinkedHashMap<>();
            out.put("disponible", true); out.put("brokers", brokers); out.put("topicos", topics.size());
            out.put("grupos", groups.size()); out.put("lagTotal", known ? lagTotal : null); out.put("particiones", rows);
            return out;
        } finally { admin.close(Duration.ofSeconds(1)); }
    }
}
