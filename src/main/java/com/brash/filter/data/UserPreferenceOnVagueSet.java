package com.brash.filter.data;

/**
 * Отношение пользователя к данному элементу на основании предпочтений пользователя в нечетком множестве
 * @param preferenceL Вероятность, что пользователь поддерживает элемент
 * @param preferenceD Вероятность, что пользователь не поддерживает элемент
 * @param preferenceH Вероятность, что пользователь колеблется
 * @param preferenceM Вероятность, что центр интервала в неопределенном множестве
 */
public record UserPreferenceOnVagueSet(
        double preferenceL,
        double preferenceD,
        double preferenceH,
        double preferenceM
) {
}
