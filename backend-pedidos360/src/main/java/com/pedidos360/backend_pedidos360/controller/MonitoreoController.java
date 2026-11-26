package com.pedidos360.backend_pedidos360.controller;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import com.pedidos360.backend_pedidos360.service.MonitoreoService;

@RestController
@RequestMapping("/api/monitoreo")
public class MonitoreoController {
    private final MonitoreoService service;
    public MonitoreoController(MonitoreoService service) { this.service = service; }
    @GetMapping("/auditoria")
    public ResponseEntity<Map<String,Object>> auditoria() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.auditoria());
    }
    @GetMapping("/brokers")
    public ResponseEntity<Map<String,Object>> brokers() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.brokers());
    }
}
