
package com.pedidos360.backend_pedidos360.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Nueva cola principal para los pagos
    public static final String PAYMENTS_QUEUE =
            "pedidos360.payments.v2";

    // Cola para mensajes que no pudieron procesarse
    public static final String PAYMENTS_DLQ =
            "pedidos360.payments.failed";

    // Exchange de mensajes fallidos
    public static final String DLX =
            "pedidos360.payments.dlx";

    // Clave de enrutamiento de mensajes fallidos
    public static final String DLQ_ROUTING_KEY =
            "pedidos360.payments.failed";

    @Bean
    public Queue paymentsQueue() {
        return QueueBuilder
                .durable(PAYMENTS_QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue paymentsDeadLetterQueue() {
        return QueueBuilder
                .durable(PAYMENTS_DLQ)
                .build();
    }

    @Bean
    public DirectExchange paymentsDeadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
public Binding paymentsDeadLetterBinding(
        @Qualifier("paymentsDeadLetterQueue") Queue paymentsDeadLetterQueue,
        DirectExchange paymentsDeadLetterExchange) {

    return BindingBuilder
            .bind(paymentsDeadLetterQueue)
            .to(paymentsDeadLetterExchange)
            .with(DLQ_ROUTING_KEY);
}
}
