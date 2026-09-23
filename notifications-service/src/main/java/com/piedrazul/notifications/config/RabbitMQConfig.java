package com.piedrazul.notifications.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.citas}")
    private String citasExchange;

    @Value("${rabbitmq.queue.cita-agendada}")
    private String queueCitaAgendada;

    @Value("${rabbitmq.queue.cita-cancelada}")
    private String queueCitaCancelada;

    @Value("${rabbitmq.queue.cita-reagendada}")
    private String queueCitaReagendada;

    @Value("${rabbitmq.routing.cita-agendada}")
    private String routingKeyCitaAgendada;

    @Value("${rabbitmq.routing.cita-cancelada}")
    private String routingKeyCitaCancelada;

    @Value("${rabbitmq.routing.cita-reagendada}")
    private String routingKeyCitaReagendada;

    @Bean
    public TopicExchange citasExchange() {
        return new TopicExchange(citasExchange);
    }

    @Bean
    public Queue citaAgendadaQueue() {
        return new Queue(queueCitaAgendada, true);
    }

    @Bean
    public Queue citaCanceladaQueue() {
        return new Queue(queueCitaCancelada, true);
    }

    @Bean
    public Queue citaReagendadaQueue() {
        return new Queue(queueCitaReagendada, true);
    }

    @Bean
    public Binding citaAgendadaBinding() {
        return BindingBuilder
                .bind(citaAgendadaQueue())
                .to(citasExchange())
                .with(routingKeyCitaAgendada);
    }

    @Bean
    public Binding citaCanceladaBinding() {
        return BindingBuilder
                .bind(citaCanceladaQueue())
                .to(citasExchange())
                .with(routingKeyCitaCancelada);
    }

    @Bean
    public Binding citaReagendadaBinding() {
        return BindingBuilder
                .bind(citaReagendadaQueue())
                .to(citasExchange())
                .with(routingKeyCitaReagendada);
    }

    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}