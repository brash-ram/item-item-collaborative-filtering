package com.brash.filter.impl;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.filter.ItemToItemSimilarity;
import com.brash.filter.PartSimilarItems;
import com.brash.util.ItemUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Реализация интерфейса, описывающего создание пар сходства
 */
@Component
@RequiredArgsConstructor
public class ItemToItemSimilarityImpl implements ItemToItemSimilarity {

    /**
     * Значение коэффициента усиления и ослабления сходства.
     * Допустимые значения - [0; 1).
     */
    @Value("${similarity.update-factor}")
    private double UPDATE_FACTOR;

    /**
     * Получить пары сходства со значением их сходства
     * @param items элементы для составления пар
     * @return Пары сходства со значением их сходства
     */
    @Override
    @Transactional
    public List<PartSimilarItems>  updateSimilarity(List<Item> items) {
        List<PartSimilarItems> parts = calculateAllSimilarity(ItemUtils.generatePairItems(items));
        return filterUpdateValue(filterNullValue(parts));
    }

    /**
     * Фильтрация пар сходства от значений, для которых не получилось выставить оценку сходства
     * @param parts Пары сходства с нулевыми значениями оценки
     * @return Пары сходства без нулевых значений оценки
     */
    private List<PartSimilarItems> filterNullValue(List<PartSimilarItems> parts) {
        parts.removeIf(part -> part.similarValue == null);
        return parts;
    }

    /**
     * Изменение значения сходства в большую или меньшую сторону
     * в зависимости от положения относительно среднего арифметического всех оценок.
     * Если больше среднего арифметического, то умножается на коэффициент 1 + UPDATE_FACTOR,
     * если меньше, то на 1 - UPDATE_FACTOR
     * @param parts Пары сходства с ненулевыми значениями для обновления оценок
     * @return Пары сходства с обновленными оценками
     */
    private List<PartSimilarItems> filterUpdateValue(List<PartSimilarItems> parts) {
        double average = 0.0;
        for (PartSimilarItems part : parts) {
            average += part.similarValue;
        }
        average /= parts.size();
        for (PartSimilarItems part : parts) {
            if (part.similarValue > average) {
                part.similarValue *= 1 + UPDATE_FACTOR;
            } else {
                part.similarValue *= 1 - UPDATE_FACTOR;
            }
        }
        return parts;
    }

    /**
     * Расчет оценок для переданных пар сходства
     * @param parts Пары сходства без оценок
     * @return Пары сходства с частично или полностью рассчитанными оценками
     */
    private List<PartSimilarItems> calculateAllSimilarity(List<PartSimilarItems> parts) {
        for (PartSimilarItems part : parts) {
            List<Mark> marksItem1 = new ArrayList<>(); //from same users
            List<Mark> marksItem2 = new ArrayList<>();

            Item item1 = part.items.get(0);
            Item item2 = part.items.get(1);

            // Получаем пользователей, которые оценили item2
            List<User> usersFromMarksItem2 = item2.getMarks().stream()
                    .map(Mark::getUser).toList();
            List<User> intersectUsers = new ArrayList<>();

            // Идем по оценкам item1
            for (Mark mark : item1.getMarks()) {
                // Получаем не сгенерированную оценку у которой пользователь
                // также оценил item2
                if (!mark.getIsGenerated() && usersFromMarksItem2.contains(mark.getUser())) {
                    marksItem1.add(mark);
                    intersectUsers.add(mark.getUser());
                }
            }

            // Необходимо 2 оценки
            if (marksItem1.size() < 2) continue;

            // Идем по оценкам item2
            for (Mark mark : item2.getMarks()) {
                // Получаем не сгенерированную оценку у которой пользователь
                // также оценил item1
                if (!mark.getIsGenerated() && intersectUsers.contains(mark.getUser())) {
                    marksItem2.add(mark);
                    // Необходимо 2 оценки
                    if (marksItem2.size() == 2) break;
                }
            }

            // Очищаем список с оценками item2 от излишних оценок
            ListIterator<Mark> iterator = marksItem1.listIterator();
            while (iterator.hasNext()) {
                Mark markItem1 = iterator.next();
                boolean isDeleted = true;
                for (Mark markItem2 : marksItem2) {
                    if (markItem2.getUser().equals(markItem1.getUser())) {
                        isDeleted = false;
                        break;
                    }
                }
                if (isDeleted) iterator.remove();
            }
            // После этих действий в списках marksItem1 и marksItem2
            // находятся по 2 оценки от 2 одинаков пользователей для
            // item1 и item2

            if (marksItem2.size() < 2) continue;

            // Сортируем списки по пользователям
            marksItem1.sort(Comparator.comparingLong(o -> o.getUser().getId()));
            marksItem2.sort(Comparator.comparingLong(o -> o.getUser().getId()));

            // Рассчитываем оценку сходства для пары элементов part
            part.similarValue = calculateSimilar(
                    marksItem1.get(0).getMark(), marksItem1.get(1).getMark(),
                    marksItem2.get(0).getMark(), marksItem2.get(1).getMark()
            );
        }
        return parts;
    }

    /**
     * Расчет оценки сходства пары элементов.
     * @param markUser1Item1 Оценка от пользователя 1 для элемента 1
     * @param markUser1Item2 Оценка от пользователя 1 для элемента 2
     * @param markUser2Item1 Оценка от пользователя 2 для элемента 1
     * @param markUser2Item2 Оценка от пользователя 2 для элемента 2
     * @return Оценка сходства элементов
     */
    private double calculateSimilar(double markUser1Item1, double markUser1Item2,
                                    double markUser2Item1, double markUser2Item2) {
        double top = (markUser1Item1 * markUser2Item1) + (markUser1Item2 * markUser2Item2);
        double bottom = sqrt(pow(markUser1Item1, 2) + pow(markUser1Item2, 2)) *
                sqrt(pow(markUser2Item1, 2) + pow(markUser2Item2, 2));
        return top / bottom;
    }
}
