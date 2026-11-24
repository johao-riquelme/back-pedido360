package com.pedidos360.backend_pedidos360.controller;

import com.pedidos360.backend_pedidos360.service.KafkaAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/kafka-admin")
public class KafkaAdminController {

    private final KafkaAdminService kafkaAdminService;

    public KafkaAdminController(KafkaAdminService kafkaAdminService) {
        this.kafkaAdminService = kafkaAdminService;
    }

    @GetMapping("/topics")
    public ResponseEntity<?> listarTopicos() {
        try {
            return ResponseEntity.ok(kafkaAdminService.listarTopicos());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/topics")
    public ResponseEntity<?> crearTopico(@RequestBody Map<String, Object> body) {
        try {
            String nombre = body.get("nombre").toString();
            int particiones = Integer.parseInt(body.getOrDefault("particiones", 3).toString());
            short replicacion = Short.parseShort(body.getOrDefault("replicacion", 1).toString());
            long retencionMs = Long.parseLong(body.getOrDefault("retencionMs", 604800000L).toString());
            return ResponseEntity.ok(Map.of("mensaje",
                    kafkaAdminService.crearTopico(nombre, particiones, replicacion, retencionMs)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/topics/{nombre}/retencion")
    public ResponseEntity<?> actualizarRetencion(@PathVariable String nombre, @RequestBody Map<String, Object> body) {
        try {
            long retencionMs = Long.parseLong(body.get("retencionMs").toString());
            return ResponseEntity.ok(Map.of("mensaje",
                    kafkaAdminService.modificarRetencionTopico(nombre, retencionMs)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/topics/{nombre}")
    public ResponseEntity<?> eliminarTopico(@PathVariable String nombre) {
        try {
            return ResponseEntity.ok(Map.of("mensaje", kafkaAdminService.eliminarTopico(nombre)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/consumer-groups")
    public ResponseEntity<?> listarConsumerGroups() {
        try {
            return ResponseEntity.ok(kafkaAdminService.listarConsumerGroups());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}