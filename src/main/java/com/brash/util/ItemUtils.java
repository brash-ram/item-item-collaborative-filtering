package com.brash.util;

import com.brash.filter.data.FuzzySet;
import com.brash.filter.data.SimilarItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс утилита для работы с элементами
 */
public class ItemUtils {

    /**
     * Генерация пар нечетких множеств из данного списка items.
     * Отсутствуют пары одинаковых элементов и перестановки в парах.
     * @param fuzzySets Список элементов, для которого нужно сгенерировать пары элементов
     * @return Пары сходства элементов
     */
    public static List<SimilarItems> generatePairItems(List<FuzzySet> fuzzySets) {
        List<SimilarItems> similarItems = new ArrayList<>();
        for (int i = 0; i < fuzzySets.size(); i++) {
            for (int n = i + 1; n < fuzzySets.size(); n++) {
                similarItems.add(new SimilarItems().setFuzzySet1(fuzzySets.get(i)).setFuzzySet2(fuzzySets.get(n)));
            }
        }
        return similarItems;
    }
}
