package com.brash.filter;

import com.brash.data.entity.HavingMarks;
import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.filter.data.SimilarItems;

import java.util.List;

/**
 * Интерфейс, описывающий создание пар сходства элементов
 */
public interface ItemToItemSimilarity {

    /**
     * Получить пары сходства со значением их сходства
     * @param items элементы для составления пар
     * @return Пары сходства со значением их сходства
     */
    List<SimilarItems> updateSimilarity(List<HavingMarks> items);
}
