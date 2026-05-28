package com.codejudgex.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE           = "codejudgex.topic";
    public static final String EVALUATION_QUEUE   = "evaluation.queue";
    public static final String EVALUATION_RETRY   = "evaluation.retry";
    public static final String EVALUATION_DLQ     = "evaluation.dlq";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String PLAGIARISM_QUEUE   = "plagiarism.queue";

    public static final String ROUTING_SUBMISSION_CREATED  = "submission.created";
    public static final String ROUTING_NOTIFICATION        = "notification.requested";
    public static final String ROUTING_PLAGIARISM          = "plagiarism.requested";

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue evaluationQueue() {
        return QueueBuilder.durable(EVALUATION_QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", EVALUATION_DLQ)
                .build();
    }

    @Bean
    Queue evaluationRetry() {
        return QueueBuilder.durable(EVALUATION_RETRY)
                .withArgument("x-message-ttl", 30_000)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ROUTING_SUBMISSION_CREATED)
                .build();
    }

    @Bean
    Queue evaluationDlq() {
        return QueueBuilder.durable(EVALUATION_DLQ).build();
    }

    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    Queue plagiarismQueue() {
        return QueueBuilder.durable(PLAGIARISM_QUEUE).build();
    }

    @Bean
    Binding evaluationBinding(Queue evaluationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(evaluationQueue).to(exchange).with(ROUTING_SUBMISSION_CREATED);
    }

    @Bean
    Binding notificationBinding(Queue notificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationQueue).to(exchange).with(ROUTING_NOTIFICATION);
    }

    @Bean
    Binding plagiarismBinding(Queue plagiarismQueue, TopicExchange exchange) {
        return BindingBuilder.bind(plagiarismQueue).to(exchange).with(ROUTING_PLAGIARISM);
    }

    @Bean
    MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}
