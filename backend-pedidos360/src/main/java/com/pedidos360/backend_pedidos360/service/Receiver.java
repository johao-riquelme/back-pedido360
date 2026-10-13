package com.pedidos360.backend_pedidos360.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class Receiver {

    @RabbitListener(queues = "hello")
    public void receiveMessage(String message) {
        System.out.println(" [✓] Mensaje recibido de la cola 'hello': '" + message + "'");
    }
}