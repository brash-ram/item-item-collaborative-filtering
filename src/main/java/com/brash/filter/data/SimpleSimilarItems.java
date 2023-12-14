package com.brash.filter.data;

import com.brash.data.entity.Item;

/**
 * Простая пара сходства 2 элементов
 * @param item1 Первый элемент
 * @param item2 Второй элемент
 * @param similarValue Значение их сходства
 */
public record SimpleSimilarItems(
        Item item1,
        Item item2,
        double similarValue
) {
}
