package com.brash.filter.impl;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.data.jpa.MarkRepository;
import com.brash.filter.ItemToItemRecommendation;
import com.brash.filter.PartSimilarItems;
import jakarta.servlet.http.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ItemToItemRecommendationImpl implements ItemToItemRecommendation {

    private final MarkRepository markRepository;

    @Override
    public List<Mark> generateAllRecommendation(
            List<PartSimilarItems> partSimilarItems,
            Map<User, Set<Item>> mapForMarks
    ) {

        return getUserAndItemForMark(partSimilarItems, mapForMarks);
    }

    private List<Mark> getUserAndItemForMark(
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
                List<PartSimilarItems> partsForGenerateMark = getPartsForGenerateMark(
                        item,
                        partSimilarItems,
                        userMarkedItem
                );

                if (partsForGenerateMark == null) continue;

                List<Mark> userMarksForGenerateNewMark = getUserMarksForGenerateMark(
                        item,
                        partsForGenerateMark,
                        userMarks
                );

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
            userMarksForGenerateNewMark.add(userMarks.get(foundMarkIndex));
        }
        return userMarksForGenerateNewMark;
    }
}
