package com.brash.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Конфигурация spring приложения
 */
@Configuration
public class ApplicationConfiguration {

    /**
     * Создание ExecutorService для работы с потоками
     * @return ExecutorService для работы с потоками
     */
    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }
}
