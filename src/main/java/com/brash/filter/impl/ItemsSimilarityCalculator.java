package com.brash.filter.impl;

import com.brash.data.entity.HavingMarks;
import com.brash.data.entity.Mark;
import com.brash.filter.ItemToItemSimilarity;
import com.brash.filter.data.*;
import com.brash.util.ItemUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;

import static com.brash.util.Utils.getAverageMarkFromUserOrItem;

/**
 * Реализация интерфейса, описывающего создание пар сходства
 */
@Component
@RequiredArgsConstructor
public class ItemsSimilarityCalculator implements ItemToItemSimilarity {


    /**
     * Значение коэффициента усиления и ослабления сходства.
     * Допустимые значения - [0; 1).
     */
    @Value("${similarity.smoothing-factor}")
    private double SMOOTHING_FACTOR;

    private static final double ZERO = 0.000000001;

    @Override
    public List<SimilarItems> updateSimilarity(List<HavingMarks> allItems) throws InterruptedException {
        List<FuzzySet> fuzzySets = getFuzzySets(allItems);
        calculatePreference(fuzzySets);
        List<SimilarItems> similarItems = ItemUtils.generatePairItems(fuzzySets);
        return generateSimilarity(similarItems);
    }

    private List<SimilarItems> generateSimilarity(List<SimilarItems> similarItems) throws InterruptedException {
        for (SimilarItems similarItem : similarItems) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread with generating similarity" + Thread.currentThread().getName() + "is interrupted");
            }
            double a = (double) similarItem.fuzzySet1.getSet().size() /
                    (similarItem.fuzzySet1.getSet().size() + similarItem.fuzzySet2.getSet().size());
            double divergenceKL = calculateDivergenceKL(
                    a,
                    similarItem.fuzzySet1.getPreferenceOnVagueSet(),
                    similarItem.fuzzySet2.getPreferenceOnVagueSet(),
                    checkZero(similarItem.fuzzySet1.getPreferenceOnVagueSet()) ||
                            checkZero(similarItem.fuzzySet1.getPreferenceOnVagueSet()) ?
                            this::calculatePreferenceProbabilityWithSmoothing :
                            this::calculatePreferenceProbability
            );
            similarItem.similarValue = 1 / (1 + divergenceKL);
        }
        return similarItems;
    }

    private double calculateDivergenceKL(
            double a,
            UserPreferenceOnVagueSet preferencesA,
            UserPreferenceOnVagueSet preferencesB,
            BinaryOperator<Double> calculatorPreferenceProbability
    ) {
        List<Double> preferencesAList = List.of(
                preferencesA.preferenceL(),
                preferencesA.preferenceD(),
                preferencesA.preferenceH(),
                preferencesA.preferenceM()
        );
        List<Double> preferencesBList = List.of(
                preferencesB.preferenceL(),
                preferencesB.preferenceD(),
                preferencesB.preferenceH(),
                preferencesB.preferenceM()
        );
        double size = preferencesAList.size();
        double divergenceKL = 0.0;
        for (int i = 0; i < size; i++) {
            double preferenceProbabilityA = calculatorPreferenceProbability.apply(preferencesAList.get(i), size);
            divergenceKL += a * preferenceProbabilityA * log2(
                    (a * preferenceProbabilityA) / ((1 - a) * calculatorPreferenceProbability.apply(preferencesBList.get(i), size))
            );
        }
        return divergenceKL;
    }

    private double log2(double value) {
        return Math.log(value) / Math.log(2);
    }

    private double calculatePreferenceProbabilityWithSmoothing(double preference, double lengthSet) {
        return (SMOOTHING_FACTOR + preference) / (1 + SMOOTHING_FACTOR * lengthSet);
    }

    private double calculatePreferenceProbability(double preference, double lengthSet) {
        return preference;
    }

    private boolean checkZero(UserPreferenceOnVagueSet preferences) {
        return preferences.preferenceL() < ZERO ||
                preferences.preferenceH() < ZERO ||
                preferences.preferenceM() < ZERO ||
                preferences.preferenceD() < ZERO;
    }

    private void calculatePreference(List<FuzzySet> fuzzySets) {
        for (FuzzySet set : fuzzySets) {
            int preferences = set.getSet().stream()
                    .map(FuzzySetItem::preference)
                    .reduce((acc, pref) -> acc += pref).orElse(0);
            double preferenceL = (double) preferences / set.getSet().size();
            double preferenceD = 1 - preferenceL;
            set.setPreferenceOnFuzzySet(new UserPreferenceOnFuzzySet(preferenceL, preferenceD));
            double preferenceLVagueSet = preferenceL - preferenceL * preferenceD;
            double preferenceDVagueSet = preferenceD - preferenceL * preferenceD;
            set.setPreferenceOnVagueSet(new UserPreferenceOnVagueSet(
                    preferenceLVagueSet,
                    preferenceDVagueSet,
                    2 * preferenceL * preferenceD,
                    (preferenceLVagueSet + (long)(1 - preferenceDVagueSet)) / 2
            ));
        }
    }

    private List<FuzzySet> getFuzzySets(List<HavingMarks> items) {
        List<FuzzySet> fuzzySet = new ArrayList<>();
        for (HavingMarks item : items) {
            List<Mark> notGeneratedMarks = item.getMarks().stream()
                    .filter(mark1 -> !mark1.getIsGenerated())
                    .toList();
            List<FuzzySetItem> fuzzySetItems = new ArrayList<>();
            for (Mark mark : notGeneratedMarks) {
                double averageMark = getAverageMarkFromUserOrItem(mark, item);
                FuzzySetItem fuzzySetItem = new FuzzySetItem(
                        mark,
                        mark.getMark() > averageMark ? 1 : 0
                );
                fuzzySetItems.add(fuzzySetItem);
            }

            if (fuzzySetItems.isEmpty()) continue;

            fuzzySet.add(new FuzzySet().setSet(fuzzySetItems));
        }
        return fuzzySet;
    }
//
//    /**
//     * Изменение значения сходства в большую или меньшую сторону
//     * в зависимости от положения относительно среднего арифметического всех оценок.
//     * Если больше среднего арифметического, то умножается на коэффициент 1 + UPDATE_FACTOR,
//     * если меньше, то на 1 - UPDATE_FACTOR
//     * @param parts Пары сходства с ненулевыми значениями для обновления оценок
//     * @return Пары сходства с обновленными оценками
//     */
//    private List<SimilarItems> filterUpdateValue(List<SimilarItems> parts) {
//        double average = 0.0;
//        for (SimilarItems part : parts) {
//            average += part.similarValue;
//        }
//        average /= parts.size();
//        for (SimilarItems part : parts) {
//            if (part.similarValue > average) {
//                part.similarValue *= 1 + UPDATE_FACTOR;
//            } else {
//                part.similarValue *= 1 - UPDATE_FACTOR;
//            }
//        }
//        return parts;
//    }
//
//    /**
//     * Расчет оценок для переданных пар сходства
//     * @param parts Пары сходства без оценок
//     * @return Пары сходства с частично или полностью рассчитанными оценками
//     */
//    private List<SimilarItems> calculateAllSimilarity(List<SimilarItems> parts) {
//        for (SimilarItems part : parts) {
//            List<Mark> marksItem1 = new ArrayList<>(); //from same users
//            List<Mark> marksItem2 = new ArrayList<>();
//
//            Item item1 = part.items.get(0);
//            Item item2 = part.items.get(1);
//
//            // Получаем пользователей, которые оценили item2
//            List<User> usersFromMarksItem2 = item2.getMarks().stream()
//                    .map(Mark::getUser).toList();
//            List<User> intersectUsers = new ArrayList<>();
//
//            // Идем по оценкам item1
//            for (Mark mark : item1.getMarks()) {
//                // Получаем не сгенерированную оценку у которой пользователь
//                // также оценил item2
//                if (!mark.getIsGenerated() && usersFromMarksItem2.contains(mark.getUser())) {
//                    marksItem1.add(mark);
//                    intersectUsers.add(mark.getUser());
//                }
//            }
//
//            // Необходимо 2 оценки
//            if (marksItem1.size() < 2) continue;
//
//            // Идем по оценкам item2
//            for (Mark mark : item2.getMarks()) {
//                // Получаем не сгенерированную оценку у которой пользователь
//                // также оценил item1
//                if (!mark.getIsGenerated() && intersectUsers.contains(mark.getUser())) {
//                    marksItem2.add(mark);
//                    // Необходимо 2 оценки
//                    if (marksItem2.size() == 2) break;
//                }
//            }
//
//            // Очищаем список с оценками item2 от излишних оценок
//            ListIterator<Mark> iterator = marksItem1.listIterator();
//            while (iterator.hasNext()) {
//                Mark markItem1 = iterator.next();
//                boolean isDeleted = true;
//                for (Mark markItem2 : marksItem2) {
//                    if (markItem2.getUser().equals(markItem1.getUser())) {
//                        isDeleted = false;
//                        break;
//                    }
//                }
//                if (isDeleted) iterator.remove();
//            }
//            // После этих действий в списках marksItem1 и marksItem2
//            // находятся по 2 оценки от 2 одинаков пользователей для
//            // item1 и item2
//
//            if (marksItem2.size() < 2) continue;
//
//            // Сортируем списки по пользователям
//            marksItem1.sort(Comparator.comparingLong(o -> o.getUser().getId()));
//            marksItem2.sort(Comparator.comparingLong(o -> o.getUser().getId()));
//
//            // Рассчитываем оценку сходства для пары элементов part
//            part.similarValue = calculateSimilar(
//                    marksItem1.get(0).getMark(), marksItem1.get(1).getMark(),
//                    marksItem2.get(0).getMark(), marksItem2.get(1).getMark()
//            );
//        }
//        return parts;
//    }
//
//    /**
//     * Расчет оценки сходства пары элементов.
//     * @param markUser1Item1 Оценка от пользователя 1 для элемента 1
//     * @param markUser1Item2 Оценка от пользователя 1 для элемента 2
//     * @param markUser2Item1 Оценка от пользователя 2 для элемента 1
//     * @param markUser2Item2 Оценка от пользователя 2 для элемента 2
//     * @return Оценка сходства элементов
//     */
//    private double calculateSimilar(double markUser1Item1, double markUser1Item2,
//                                    double markUser2Item1, double markUser2Item2) {
//        double top = (markUser1Item1 * markUser2Item1) + (markUser1Item2 * markUser2Item2);
//        double bottom = sqrt(pow(markUser1Item1, 2) + pow(markUser1Item2, 2)) *
//                sqrt(pow(markUser2Item1, 2) + pow(markUser2Item2, 2));
//        return top / bottom;
//    }
}
