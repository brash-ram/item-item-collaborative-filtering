package com.brash.config;

import com.brash.dto.rabbit.RabbitItemDTO;
import com.brash.dto.rabbit.RabbitMarkDTO;
import com.brash.dto.rabbit.RabbitUserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.SameLen;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.ClassMapper;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Конфигурация spring приложения
 */
@Configuration
@RequiredArgsConstructor
@EnableWebMvc
@Slf4j
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
    public ClassMapper classMapper() {
        log.error("start");
        Map<String, Class< ? >> mappings = new HashMap<>();
        mappings.put(rabbitMQConfig.addItemClass(), RabbitItemDTO.class);
        mappings.put(rabbitMQConfig.addUserClass(), RabbitUserDTO.class);
        mappings.put(rabbitMQConfig.addMarkClass(), RabbitMarkDTO.class);

        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages(getPackage(rabbitMQConfig.addItemClass()));
        classMapper.setTrustedPackages(getPackage(rabbitMQConfig.addUserClass()));
        classMapper.setTrustedPackages(getPackage(rabbitMQConfig.addMarkClass()));
        classMapper.setIdClassMapping(mappings);
        log.error("finish");
        return classMapper;
    }

    @Bean
    public MessageConverter jsonMessageConverter(ClassMapper classMapper) {
        Jackson2JsonMessageConverter jsonConverter = new Jackson2JsonMessageConverter();
        jsonConverter.setClassMapper(classMapper);
        return jsonConverter;
    }

    private String getPackage(String classPath) {
        StringBuilder packagePath = new StringBuilder();
        List<String> paths = Arrays.stream(classPath.split(".")).toList();

        for (String i : paths) {
            packagePath.append(i).append(".");
        }
        packagePath.append("*");
        log.info(packagePath.toString());
        log.info(classPath);
        return packagePath.toString();
    }
}
