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
     * Генерация рекомендаций
     * @param itemNeighbours Элементы и их ближайшие соседи
     * @param userNeighbours Пользователи и их ближайшие соседи
     * @param generatingMarks Таблица элементов для которых нужно сгенерировать оценки
     * @return Сгенерированные оценки
     */
    List<Mark> generateAllRecommendation(
            ItemNeighbours itemNeighbours,
            UserNeighbours userNeighbours,
            Map<Item, List<Mark>> generatingMarks
    );
}
