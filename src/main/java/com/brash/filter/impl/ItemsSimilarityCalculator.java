package com.brash.filter.impl;

import com.brash.data.entity.HavingMarks;
import com.brash.data.entity.Mark;
import com.brash.filter.ItemToItemSimilarity;
import com.brash.filter.data.*;
import com.brash.util.FilterUtils;
import com.brash.util.ItemUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;

import static com.brash.util.FilterUtils.getAverageMarkFromUserOrItem;
import static com.brash.util.FilterUtils.log2;

/**
 * Реализация интерфейса, описывающего создание пар сходства на основе нечетких множеств
 */
@Component
@RequiredArgsConstructor
public class ItemsSimilarityCalculator implements ItemToItemSimilarity {


    /**
     * Значение коэффициента сглаживания вероятностей предпочтения на неопределенном множестве
     */
    @Value("${similarity.smoothing-factor}")
    private double SMOOTHING_FACTOR;

    /**
     * Значение по которому определяется необходимость сглаживания
     */
    private static final double ZERO = 0.000000001;

    /**
     * Получить пары сходства со значением их сходства
     * @param allItems элементы или пользователи для составления пар
     * @return Пары сходства со значением их сходства
     */
    @Override
    public List<SimilarItems> updateSimilarity(List<HavingMarks> allItems) throws InterruptedException {
        List<FuzzySet> fuzzySets = getFuzzySets(allItems);
        calculatePreference(fuzzySets);
        List<SimilarItems> similarItems = ItemUtils.generatePairItems(fuzzySets);
        return generateSimilarity(similarItems);
    }

    /**
     * Генерация значения сходства на основе определения расхождения KL по теории дивергенции
     * @param similarItems Список всех пар сходства
     * @return Список пар сходства со сгенерированной оценкой сходства
     * @throws InterruptedException Возникает при прерывании потока
     */
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

    /**
     * Расчет расхождения KL по предпочтениям пользователя на неопределенном множестве
     * @param a Весовой коэффициент множества А к сумме множеств А и B
     * @param preferencesA Предпочтения пользователя на неопределенном множестве А
     * @param preferencesB Предпочтения пользователя на неопределенном множестве B
     * @param calculatorPreferenceProbability Функция для определения предпочтения
     *                                        (со сглаживанием или без)
     * @return Расхождение KL
     */
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

    /**
     * Функция расчета сглаженного предпочтения
     * @param preference Значение предпочтения
     * @param lengthSet Размер набора
     * @return Сглаженное предпочтение
     */
    private double calculatePreferenceProbabilityWithSmoothing(double preference, double lengthSet) {
        return (SMOOTHING_FACTOR + preference) / (1 + SMOOTHING_FACTOR * lengthSet);
    }


    /**
     * Функция расчета предпочтения без сглаживания
     * @param preference Значение предпочтения
     * @param lengthSet Размер набора
     * @return Предпочтение
     */
    private double calculatePreferenceProbability(double preference, double lengthSet) {
        return preference;
    }

    /**
     * Проверка на нулевое значение в предпочтениях на неопределенном множестве
     * @param preferences Предпочтения на неопределенном множестве
     * @return Есть нулевые значения или нет
     */
    private boolean checkZero(UserPreferenceOnVagueSet preferences) {
        return preferences.preferenceL() < ZERO ||
                preferences.preferenceH() < ZERO ||
                preferences.preferenceM() < ZERO ||
                preferences.preferenceD() < ZERO;
    }

    /**
     * Рассчитывает предпочтения на нечетком и неопределенном множестве
     * @param fuzzySets Набор нечетких множеств
     */
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

    /**
     * Формирует нечеткие множества на основе списка элементов имеющих оценки
     * (пользователей или элементов)
     * @param items Набор элементов или пользователей
     * @return Список нечетких множеств
     */
    private List<FuzzySet> getFuzzySets(List<HavingMarks> items) {
        List<FuzzySet> fuzzySet = new ArrayList<>();
        for (HavingMarks item : items) {
            List<Mark> notGeneratedMarks = FilterUtils.getNotGeneratedMarks(item.getMarks());
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
}
