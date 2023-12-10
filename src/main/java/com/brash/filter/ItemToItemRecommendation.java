package com.brash.filter;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.filter.data.ItemNeighbours;
import com.brash.filter.data.UserNeighbours;

import java.util.List;
import java.util.Map;

/**
 * Интерфейс, описывающий генерацию оценок рекомендаций
 */
public interface ItemToItemRecommendation {

    /**
     * Генерация оценок рекомендации для переданных пользователей и их элементов (mapUserAndItemsForMarks)
     * с помощью пар сходства элементов (partSimilarItems)
     */
    List<Mark> generateAllRecommendation(
            ItemNeighbours itemNeighbours,
            UserNeighbours userNeighbours,
            Map<Item, List<Mark>> generatingMarks
    );
}
