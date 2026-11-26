package com.pedidos360.backend_pedidos360.service;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaKafkaService {
    private final JdbcTemplate jdbc;

    public AuditoriaKafkaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int registrar(String topico, int particion, long offset, String pedidoId,
                         String referencia, String estado, double monto) {
        return jdbc.update("""
            INSERT INTO public.kafka_auditoria_eventos
                (topico, particion, kafka_offset, pedido_id, referencia, estado, monto)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (topico, particion, kafka_offset) DO NOTHING
            """, topico, particion, offset, pedidoId, referencia, estado, BigDecimal.valueOf(monto));
    }

    public Map<String, Object> resumen() {
        return jdbc.queryForMap("""
            WITH autorizados AS (
                SELECT DISTINCT ON (referencia) referencia, monto
                FROM public.kafka_auditoria_eventos
                WHERE topico = 'pagos-eventos' AND estado = 'AUTHORIZED'
                ORDER BY referencia, procesado_en, particion, kafka_offset
            )
            SELECT COUNT(*) AS eventos_total,
                   COUNT(*) FILTER (WHERE topico = 'pedidos-eventos') AS eventos_pedidos,
                   COUNT(*) FILTER (WHERE topico = 'pagos-eventos') AS eventos_pagos,
                   (SELECT COUNT(*) FROM autorizados) AS pagos_autorizados,
                   (SELECT COALESCE(SUM(monto), 0) FROM autorizados) AS monto_autorizado
            FROM public.kafka_auditoria_eventos
            """);
    }
}
