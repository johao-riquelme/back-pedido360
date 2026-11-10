
package com.pedidos360.backend_pedidos360.service;

import com.pedidos360.backend_pedidos360.config.RabbitMQConfig;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class Sender {

    private final RabbitTemplate rabbitTemplate;

    public Sender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(String message) {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                "No se puede enviar un mensaje vacío."
            );
        }

        CorrelationData correlationData =
            new CorrelationData(UUID.randomUUID().toString());

        try {
            rabbitTemplate.convertAndSend(
                "",
                RabbitMQConfig.PAYMENTS_QUEUE,
                message,
                amqpMessage -> {
                    amqpMessage.getMessageProperties()
                        .setDeliveryMode(
                            MessageDeliveryMode.PERSISTENT
                        );
                    return amqpMessage;
                },
                correlationData
            );

            // Esperar la confirmación del broker.
            var confirm = correlationData.getFuture()
                .get(10, TimeUnit.SECONDS);

            if (!confirm.isAck()) {
                throw new IllegalStateException(
                    "RabbitMQ no confirmó el mensaje: " +
                    confirm.getReason()
                );
            }

            // Detectar mensajes sin ruta válida.
            if (correlationData.getReturned() != null) {
                throw new IllegalStateException(
                    "RabbitMQ devolvió el mensaje: " +
                    "no encontró una ruta válida."
                );
            }

            System.out.println(
                "[RabbitMQ] Publicación confirmada en " +
                RabbitMQConfig.PAYMENTS_QUEUE
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                "Se interrumpió la confirmación de RabbitMQ.",
                e
            );

        } catch (ExecutionException |
                 TimeoutException |
                 AmqpException e) {

            throw new IllegalStateException(
                "No se pudo confirmar la publicación en RabbitMQ.",
                e
            );
        }
    }
}
