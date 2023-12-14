package com.brash.filter.data;

import com.brash.data.entity.Mark;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Элемент нечеткого множества
 * @param mark Оценка элемента или пользователя
 * @param preference Уровень членства
 */
public record FuzzySetItem(
        Mark mark,

        @Min(0)
        @Max(1)
        int preference
) {}
