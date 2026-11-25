package com.pedidos360.backend_pedidos360.service;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class KafkaAdminService {

    private final KafkaAdmin kafkaAdmin;

    public KafkaAdminService(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    private AdminClient crearCliente() {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    public Set<String> listarTopicos() throws ExecutionException, InterruptedException {
        try (AdminClient client = crearCliente()) {
            return client.listTopics().names().get();
        }
    }

    public String crearTopico(String nombre, int particiones, short replicacion, long retencionMs)
            throws ExecutionException, InterruptedException {
        try (AdminClient client = crearCliente()) {
            Map<String, String> configs = new HashMap<>();
            configs.put("retention.ms", String.valueOf(retencionMs));
            NewTopic nuevoTopico = new NewTopic(nombre, particiones, replicacion).configs(configs);
            client.createTopics(Collections.singletonList(nuevoTopico)).all().get();
            return "Topico '" + nombre + "' creado exitosamente.";
        }
    }

    public String modificarRetencionTopico(String nombre, long retencionMs)
            throws ExecutionException, InterruptedException {
        try (AdminClient client = crearCliente()) {
            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
            ConfigEntry entry = new ConfigEntry("retention.ms", String.valueOf(retencionMs));
            Map<ConfigResource, Collection<AlterConfigOp>> updateConfigs = Collections.singletonMap(
                    resource,
                    Collections.singletonList(new AlterConfigOp(entry, AlterConfigOp.OpType.SET))
            );
            client.incrementalAlterConfigs(updateConfigs).all().get();
            return "Retencion del topico '" + nombre + "' actualizada a " + retencionMs + " ms.";
        }
    }

    public String eliminarTopico(String nombre) throws ExecutionException, InterruptedException {
        try (AdminClient client = crearCliente()) {
            client.deleteTopics(Collections.singletonList(nombre)).all().get();
            return "Topico '" + nombre + "' eliminado correctamente.";
        }
    }

    public List<Map<String, Object>> listarConsumerGroups() throws ExecutionException, InterruptedException {
        try (AdminClient client = crearCliente()) {
            Collection<ConsumerGroupListing> groups = client.listConsumerGroups().all().get();
            return groups.stream().map(g -> {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("groupId", g.groupId());
                info.put("isSimple", g.isSimpleConsumerGroup());
                info.put("state", g.state().map(Enum::name).orElse("UNKNOWN"));
                return info;
            }).collect(Collectors.toList());
        }
    }
}