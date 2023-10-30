package com.brash.filter.impl;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.data.jpa.MarkRepository;
import com.brash.filter.ItemToItemRecommendation;
import com.brash.filter.PartSimilarItems;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Реализация интерфейса, описывающего генерацию оценок рекомендаций
 */
@Component
@RequiredArgsConstructor
public class ItemToItemRecommendationImpl implements ItemToItemRecommendation {

    private final MarkRepository markRepository;

    /**
     * Генерация оценок рекомендации для переданных пользователей и их элементов (mapUserAndItemsForMarks)
     * с помощью пар сходства элементов (partSimilarItems)
     * @param partSimilarItems Сгенерированные пары сходства элементов с их оценками
     * @param mapForMarks Набор пользователей для которых нужно сгенерировать
     * или обновить оценку рекомендации для указанных элементов.
     * @return Список сгенерированных оценок
     */
    @Override
    public List<Mark> generateAllRecommendation(
            List<PartSimilarItems> partSimilarItems,
            Map<User, Set<Item>> mapForMarks
    ) {

        List<Mark> generatedMarks = new ArrayList<>();

        for (Map.Entry<User, Set<Item>> mapForMark : mapForMarks.entrySet()) {
            User currentUser = mapForMark.getKey();
            List<Mark> userMarks = new ArrayList<>(currentUser.getMarks());
            Set<Item> userMarkedItem = userMarks.stream()
                    .filter(mark -> !mark.getIsGenerated())
                    .map(Mark::getItem).collect(Collectors.toSet());
            for (Item item : mapForMark.getValue()) {
                // Выявляем пары сходства, где один элемент равен item
                // а для других есть оценка пользователя
                List<PartSimilarItems> partsForGenerateMark = getPartsForGenerateMark(
                        item,
                        partSimilarItems,
                        userMarkedItem
                );

                if (partsForGenerateMark == null) continue;

                // Выявляем оценки пользователя для всех элементов
                // отличных от item в partsForGenerateMark
                List<Mark> userMarksForGenerateNewMark = getUserMarksForGenerateMark(
                        item,
                        partsForGenerateMark,
                        userMarks
                );

                // Генерируем оценку рекомендации для элемента
                Mark generatedMarkForCurrentItem = generateMarkForCurrentItem(
                        partsForGenerateMark,
                        userMarksForGenerateNewMark,
                        item,
                        currentUser
                );

                generatedMarks.add(generatedMarkForCurrentItem);
            }
        }
        return generatedMarks;
    }

    /**
     * Генерация оценки для данных пользователя и элемента
     * @param partsForGenerateMark Пары сходства, где 1 элемент равен currentItem,
     *        а для другого есть оценка, поставленная пользователем
     * @param generatedMarkForCurrentItem Оценки поставленные пользователем
     *        для других элементов из partsForGenerateMark отличных от currentItem
     * @param currentItem Элемент, для которого генерируется оценка
     * @param currentUser Пользователь, для которого генерируется оценка
     * @return Сгенерированная оценка, которую остается только сохранить в бд
     */
    private Mark generateMarkForCurrentItem(List<PartSimilarItems> partsForGenerateMark,
                                            List<Mark> generatedMarkForCurrentItem,
                                            Item currentItem,
                                            User currentUser) {
        generatedMarkForCurrentItem.sort(Comparator.comparingLong(value -> value.getItem().getId()));
        double top = 0.0;
        double bottom = 0.0;
        for (PartSimilarItems part : partsForGenerateMark) {
            int foundMarkIndex = Collections.binarySearch(
                    generatedMarkForCurrentItem,
                    new Mark().setItem(part.getOtherItem(currentItem)),
                    Comparator.comparingLong(mark1 -> mark1.getItem().getId()));
            Mark foundMark = generatedMarkForCurrentItem.get(foundMarkIndex);
            top += foundMark.getMark() * part.similarValue;
            bottom += part.similarValue;
        }
        double markValue = top / bottom;
        Optional<Mark> markOptional = markRepository.findByUserEqualsAndItemEquals(currentUser, currentItem);
        if (markOptional.isPresent()) {
            Mark mark = markOptional.get();
            return mark.setMark(markValue);
        } else {
            return new Mark().setMark(markValue).setIsGenerated(true)
                    .setItem(currentItem).setUser(currentUser);
        }

    }

    /**
     * Выявление пар сходства, у которых один элемент равен itemForMark, а для
     * другого есть оценка пользователем
     * @param itemForMark Элемент, для которого выявляются пары сходства
     * @param allParts Все доступные пары сходства
     * @param markedItems Элементы, для которых есть поставленные пользователем
     *        оценки
     * @return Пары сходства, у которых один элемент равен itemForMark, а для
     * другого есть оценка пользователем
     */
    private List<PartSimilarItems> getPartsForGenerateMark
            (
                    Item itemForMark,
                    List<PartSimilarItems> allParts,
                    Set<Item> markedItems
    ) {
        List<PartSimilarItems> simForGenerateMark = new ArrayList<>();
        for (PartSimilarItems part : allParts) {
            if (part.items.contains(itemForMark) &&
                    markedItems.contains(part.getOtherItem(itemForMark))) {
                simForGenerateMark.add(part);
            }
        }
        if (simForGenerateMark.size() > 1) {
            return simForGenerateMark;
        } else {
            return null;
        }
    }

    /**
     * Выявление оценок пользователя, которые поставлены элементу
     * отличному от itemForMark в парах сходства partsForGenerateMark,
     * в которых один элемент равен itemForMark,
     * а для другого есть оценка пользователя
     * @param itemForMark Элемент, отличным от которого элементам
     *        ищутся оценки пользователя
     * @param partsForGenerateMark Пары сходства
     * @param userMarks Всн оценки пользователя
     * @return Оценки пользователя, которые поставлены элементу
     * отличному от itemForMark в парах сходства partsForGenerateMark
     */
    private List<Mark> getUserMarksForGenerateMark
            (
                    Item itemForMark,
                    List<PartSimilarItems> partsForGenerateMark,
                    List<Mark> userMarks
            )
    {
        List<Mark> userMarksForGenerateNewMark = new ArrayList<>();
        for (PartSimilarItems part : partsForGenerateMark) {
            int foundMarkIndex = Collections.binarySearch(
                    userMarks,
                    new Mark().setItem(part.getOtherItem(itemForMark)),
                    Comparator.comparingLong(mark1 -> mark1.getItem().getId()));
            if (foundMarkIndex >= 0)
                userMarksForGenerateNewMark.add(userMarks.get(foundMarkIndex));
        }
        return userMarksForGenerateNewMark;
    }
}
