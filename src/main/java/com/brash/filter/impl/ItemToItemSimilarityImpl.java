package com.brash.filter.impl;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.data.jpa.ItemRepository;
import com.brash.data.jpa.MarkRepository;
import com.brash.data.jpa.UserRepository;
import com.brash.filter.ItemToItemSimilarity;
import com.brash.filter.PartSimilarItems;
import com.brash.util.ItemUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.lang.Math.*;

@Component
@RequiredArgsConstructor
public class ItemToItemSimilarityImpl implements ItemToItemSimilarity {

    @Override
    @Transactional
    public List<PartSimilarItems>  updateSimilarity(List<Item> items) {
        return ItemUtils.generatePairItems(items);
    }

    private void calculateAllSimilarity(List<PartSimilarItems> parts) {
        for (PartSimilarItems part : parts) {
            List<Mark> marksItem1 = new ArrayList<>(); //from same users
            List<Mark> marksItem2 = new ArrayList<>();

            Item item1 = part.items.get(0);
            Item item2 = part.items.get(1);

            List<User> usersFromMarksItem2 = item2.getMarks().stream()
                    .map(Mark::getUser).toList();
            List<User> intersectUsers = new ArrayList<>();

            for (Mark mark : item1.getMarks()) {
                if (usersFromMarksItem2.contains(mark.getUser())) {
                    marksItem1.add(mark);
                    intersectUsers.add(mark.getUser());
                    if (marksItem1.size() == 2) break;
                }
            }

            if (marksItem1.size() < 2) continue;

            for (Mark mark : item2.getMarks()) {
                if (intersectUsers.contains(mark.getUser())) {
                    marksItem2.add(mark);
                    if (marksItem2.size() == 2) break;
                }
            }

            marksItem1.sort(Comparator.comparingLong(o -> o.getUser().getId()));
            marksItem2.sort(Comparator.comparingLong(o -> o.getUser().getId()));

            part.similarValue = calculateSimilar(
                    marksItem1.get(0).getMark(), marksItem1.get(1).getMark(),
                    marksItem2.get(0).getMark(), marksItem2.get(1).getMark()
            );
        }
    }

    private double calculateSimilar(int markUser1Item1, int markUser1Item2,
                                    int markUser2Item1, int markUser2Item2) {
        double top = (markUser1Item1 * markUser2Item1) + (markUser1Item2 * markUser2Item2);
        double bottom = sqrt(pow(markUser1Item1, 2) + pow(markUser1Item2, 2)) *
                sqrt(pow(markUser2Item1, 2) + pow(markUser2Item2, 2));
        return top / bottom;
    }
}
