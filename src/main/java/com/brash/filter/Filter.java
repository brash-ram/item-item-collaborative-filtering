package com.brash.filter;

/**
 * Интерфейс, описывающий запуск алгоритма совместной фильтрации
 */
public interface Filter {

    /**
     * Запуск обновления старых и генерации новых оценок рекомендации
     */
    void updateRecommendations();
}
