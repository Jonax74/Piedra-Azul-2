package com.piedrazul.citas.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Value("${rabbitmq.exchange.personas}")
    private String personasExchange;

    @Value("${rabbitmq.routing.cita-agendada}")
    private String routingKeyCitaAgendada;

    @Value("${rabbitmq.routing.cita-cancelada}")
    private String routingKeyCitaCancelada;

    @Value("${rabbitmq.routing.cita-reagendada}")
    private String routingKeyCitaReagendada;

    @Value("${rabbitmq.queue.paciente-creado}")
    private String queuePacienteCreado;

    @Value("${rabbitmq.queue.paciente-actualizado}")
    private String queuePacienteActualizado;

    @Value("${rabbitmq.queue.medico-creado}")
    private String queueMedicoCreado;

    @Value("${rabbitmq.queue.medico-actualizado}")
    private String queueMedicoActualizado;

    @Value("${rabbitmq.queue.disponibilidad-actualizada}")
    private String queueDisponibilidadActualizada;

    @Value("${rabbitmq.queue.disponibilidad-eliminada}")
    private String queueDisponibilidadEliminada;

    @Value("${rabbitmq.queue.disponibilidad-modificada}")
    private String queueDisponibilidadModificada;

    @Bean
    public TopicExchange citasExchange() {
        return new TopicExchange(citasExchange);
    }

    @Bean
    public TopicExchange personasExchange() {
        return new TopicExchange(personasExchange);
    }

    @Bean
    public Queue pacienteCreadoQueue() {
        return new Queue(queuePacienteCreado, true);
    }

    @Bean
    public Queue pacienteActualizadoQueue() {
        return new Queue(queuePacienteActualizado, true);
    }

    @Bean
    public Queue medicoCreadoQueue() {
        return new Queue(queueMedicoCreado, true);
    }

    @Bean
    public Queue medicoActualizadoQueue() {
        return new Queue(queueMedicoActualizado, true);
    }

    @Bean
    public Queue disponibilidadActualizadaQueue() {
        return new Queue(queueDisponibilidadActualizada, true);
    }

    @Bean
    public Queue disponibilidadEliminadaQueue() {
        return new Queue(queueDisponibilidadEliminada, true);
    }

    @Bean
    public Queue disponibilidadModificadaQueue() {
        return new Queue(queueDisponibilidadModificada, true);
    }

    @Bean
    public Binding pacienteCreadoBinding() {
        return BindingBuilder
                .bind(pacienteCreadoQueue())
                .to(personasExchange())
                .with("paciente.creado");
    }

    @Bean
    public Binding pacienteActualizadoBinding() {
        return BindingBuilder
                .bind(pacienteActualizadoQueue())
                .to(personasExchange())
                .with("paciente.actualizado");
    }

    @Bean
    public Binding medicoCreadoBinding() {
        return BindingBuilder
                .bind(medicoCreadoQueue())
                .to(personasExchange())
                .with("medico.creado");
    }

    @Bean
    public Binding medicoActualizadoBinding() {
        return BindingBuilder
                .bind(medicoActualizadoQueue())
                .to(personasExchange())
                .with("medico.actualizado");
    }

    @Bean
    public Binding disponibilidadActualizadaBinding() {
        return BindingBuilder
                .bind(disponibilidadActualizadaQueue())
                .to(personasExchange())
                .with("disponibilidad.actualizada");
    }

    @Bean
    public Binding disponibilidadEliminadaBinding() {
        return BindingBuilder
                .bind(disponibilidadEliminadaQueue())
                .to(personasExchange())
                .with("disponibilidad.eliminada");
    }

    @Bean
    public Binding disponibilidadModificadaBinding() {
        return BindingBuilder
                .bind(disponibilidadModificadaQueue())
                .to(personasExchange())
                .with("disponibilidad.modificada");
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}