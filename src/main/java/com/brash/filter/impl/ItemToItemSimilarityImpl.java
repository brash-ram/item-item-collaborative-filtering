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

@Component
@RequiredArgsConstructor
public class ItemToItemSimilarityImpl implements ItemToItemSimilarity {

    @Value("${similarity.update-factor}")
    private double UPDATE_FACTOR;

    @Override
    @Transactional
    public List<PartSimilarItems>  updateSimilarity(List<Item> items) {
        List<PartSimilarItems> parts = calculateAllSimilarity(ItemUtils.generatePairItems(items));
        return filterUpdateValue(filterNullValue(parts));
    }

    private List<PartSimilarItems> filterNullValue(List<PartSimilarItems> parts) {
        parts.removeIf(part -> part.similarValue == null);
        return parts;
    }

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

    private List<PartSimilarItems> calculateAllSimilarity(List<PartSimilarItems> parts) {
        for (PartSimilarItems part : parts) {
            List<Mark> marksItem1 = new ArrayList<>(); //from same users
            List<Mark> marksItem2 = new ArrayList<>();

            Item item1 = part.items.get(0);
            Item item2 = part.items.get(1);

            List<User> usersFromMarksItem2 = item2.getMarks().stream()
                    .map(Mark::getUser).toList();
            List<User> intersectUsers = new ArrayList<>();

            for (Mark mark : item1.getMarks()) {
                if (!mark.getIsGenerated() && usersFromMarksItem2.contains(mark.getUser())) {
                    marksItem1.add(mark);
                    intersectUsers.add(mark.getUser());
                }
            }

            if (marksItem1.size() < 2) continue;

            for (Mark mark : item2.getMarks()) {
                if (!mark.getIsGenerated() && intersectUsers.contains(mark.getUser())) {
                    marksItem2.add(mark);
                    if (marksItem2.size() == 2) break;
                }
            }

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

            if (marksItem2.size() < 2) continue;

            marksItem1.sort(Comparator.comparingLong(o -> o.getUser().getId()));
            marksItem2.sort(Comparator.comparingLong(o -> o.getUser().getId()));

            part.similarValue = calculateSimilar(
                    marksItem1.get(0).getMark(), marksItem1.get(1).getMark(),
                    marksItem2.get(0).getMark(), marksItem2.get(1).getMark()
            );
        }
        return parts;
    }

    private double calculateSimilar(double markUser1Item1, double markUser1Item2,
                                    double markUser2Item1, double markUser2Item2) {
        double top = (markUser1Item1 * markUser2Item1) + (markUser1Item2 * markUser2Item2);
        double bottom = sqrt(pow(markUser1Item1, 2) + pow(markUser1Item2, 2)) *
                sqrt(pow(markUser2Item1, 2) + pow(markUser2Item2, 2));
        return top / bottom;
    }
}
