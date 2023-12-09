package com.brash.filter;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.filter.data.SimilarItems;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Интерфейс, описывающий генерацию оценок рекомендаций
 */
public interface ItemToItemRecommendation {

    /**
     * Генерация оценок рекомендации для переданных пользователей и их элементов (mapUserAndItemsForMarks)
     * с помощью пар сходства элементов (partSimilarItems)
     */
    List<Mark> generateAllRecommendation(
            List<SimilarItems> similarItems,
            Map<User, Set<Item>> mapUserAndItemsForMarks
    );
}
