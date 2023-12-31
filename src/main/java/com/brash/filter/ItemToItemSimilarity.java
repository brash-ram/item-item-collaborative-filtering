package com.brash.filter;

import com.brash.data.entity.HavingMarks;
import com.brash.filter.data.SimilarItems;

import java.util.List;

/**
 * Интерфейс, описывающий создание пар сходства элементов и пользователей
 */
public interface ItemToItemSimilarity {

    /**
     * Получить пары сходства со значением их сходства
     * @param items элементы или пользователи для составления пар
     * @return Пары сходства со значением их сходства
     */
    List<SimilarItems> updateSimilarity(List<HavingMarks> items) throws InterruptedException;
}
