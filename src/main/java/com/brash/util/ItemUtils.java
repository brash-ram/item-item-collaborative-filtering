package com.brash.util;

import com.brash.data.entity.Item;
import com.brash.filter.PartSimilarItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс утилита для работы с элементами
 */
public class ItemUtils {

    /**
     * Генерация пар элементов из данного списка items.
     * Отсутствуют пары одинаковых элементов и перестановки в парах.
     * @param items Список элементов, для которого нужно сгенерировать пары элементов
     * @return Пары сходства элементов
     */
    public static List<PartSimilarItems> generatePairItems(List<Item> items) {
        List<PartSimilarItems> partSimilarItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            for (int n = i + 1; n < items.size(); n++) {
                List<Item> part = new ArrayList<>();
                part.add(item);
                part.add(items.get(n));
                partSimilarItems.add(new PartSimilarItems(part, null));
            }
        }
        return partSimilarItems;
    }
}
