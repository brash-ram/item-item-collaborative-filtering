package com.brash.filter;

import com.brash.data.entity.Item;

import java.util.List;

/**
 * Интерфейс, описывающий создание пар сходства
 */
public interface ItemToItemSimilarity {

    /**
     * Получить пары сходства со значением их сходства
     * @param items элементы для составления пар
     * @return Пары сходства со значением их сходства
     */
    List<PartSimilarItems> updateSimilarity(List<Item> items);
}
