package com.pedidos360.backend_pedidos360.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Sender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend("hello", message);
        System.out.println(" [x] Mensaje enviado a RabbitMQ: '" + message + "'");
    }
}