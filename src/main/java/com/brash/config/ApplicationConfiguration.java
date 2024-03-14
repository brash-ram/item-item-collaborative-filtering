package com.brash.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Конфигурация spring приложения
 */
@Configuration
@RequiredArgsConstructor
@EnableWebMvc
public class ApplicationConfiguration {

    private final RabbitMQConfig rabbitMQConfig;

//    @Value("${time-update-filter}")
//    private int timeUpdateFilter;
//
//    @Value("${time-update-similarity}")
//    private int ;

//    @Bean("delay")
//    public long getDelay() {
//        return config.scheduler().interval().toMillis();
//    }

    /**
     * Создание ExecutorService для работы с потоками
     * @return ExecutorService для работы с потоками
     */
    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @Bean
    public Queue queue() {
        return QueueBuilder.durable(rabbitMQConfig.queue())
                .build();
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(rabbitMQConfig.exchange());
    }

    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(rabbitMQConfig.queue());
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
