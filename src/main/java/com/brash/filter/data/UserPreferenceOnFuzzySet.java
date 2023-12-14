package com.brash.filter.data;

/**
 * Отношение пользователя к данному элементу на основании членства в нечетком множестве
 * @param preferenceL Оптимистическое отношение
 * @param preferenceD Пессимистическое отношение
 */
public record UserPreferenceOnFuzzySet(
        double preferenceL,
        double preferenceD
) {
}
