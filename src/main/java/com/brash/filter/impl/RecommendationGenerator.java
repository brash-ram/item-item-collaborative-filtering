package com.brash.filter.impl;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.filter.ItemToItemRecommendation;
import com.brash.filter.data.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.brash.util.Utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RecommendationGenerator implements ItemToItemRecommendation {

    @Override
    public List<Mark> generateAllRecommendation(
            ItemNeighbours itemNeighbours,
            UserNeighbours userNeighbours,
            Map<Item, List<Mark>> generatingMarks
    ) {
        List<Mark> generatedMarks = new ArrayList<>();
        for (Map.Entry<Item, List<SimpleSimilarItems>> entry : itemNeighbours.neighbours().entrySet()) {
            Item currentItem = entry.getKey();
            List<SimpleSimilarItems> neighbours = entry.getValue();
            List<Mark> marksForGeneratingWithItem = generatingMarks.get(currentItem);

            if (marksForGeneratingWithItem == null)
                continue;

            for (Mark mark : marksForGeneratingWithItem) {
                User currentUser = mark.getUser();
                List<SimpleSimilarItems> neighboursWithMark = neighbours.stream()
                        .filter(item ->
                                    getOtherItem(item, currentItem).getMarks().stream()
                                            .map(Mark::getUser)
                                            .toList()
                                            .contains(currentUser)
                                )
                        .toList();
                try {
                    if (neighbours.size() == neighboursWithMark.size()) {
                        generatedMarks.add(generateMarkOnMeanCentering(mark, neighbours));
                    } else if (neighboursWithMark.size() == 0) {
                        generatedMarks.add(generateMarkOnVagueSet(mark, neighbours, userNeighbours));
                    } else {
                        List<SimpleSimilarItems> neighboursWithoutMark = neighbours.stream()
                                .filter(item ->
                                        !getOtherItem(item, currentItem).getMarks().stream()
                                                .map(Mark::getUser)
                                                .toList()
                                                .contains(currentUser)
                                )
                                .toList();
                        generatedMarks.add(generateMarkOnSparseData(mark, neighboursWithMark, neighboursWithoutMark, userNeighbours));
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }
        return generatedMarks;
    }

    private Mark generateMarkOnSparseData(
            Mark mark,
            List<SimpleSimilarItems> neighboursWithMark,
            List<SimpleSimilarItems> neighboursWithoutMark,
            UserNeighbours similarUsers
    ) throws Exception {
        Item currentItem = mark.getItem();
        User currentUser = mark.getUser();
        double averageMarkValueCurrentItem = getAverageMark(new ArrayList<>(currentItem.getMarks()));
        double top = 0.0;
        double bottom = 0.0;
        for (SimpleSimilarItems item : neighboursWithMark) {
            Mark neighboringMark = getMarkFromUser(new ArrayList<>(getOtherItem(item, currentItem).getMarks()), currentUser);

            double averageMarkValueNeighboringItem = getAverageMark(new ArrayList<>(neighboringMark.getItem().getMarks()));

            top += item.similarValue() * (neighboringMark.getMark() - averageMarkValueNeighboringItem);
            bottom += Math.abs(item.similarValue());
        }

        for (SimpleSimilarItems item : neighboursWithoutMark) {
            Mark neighboringMark;
            try {
                neighboringMark = getMarkFromSimilarUser(
                        new ArrayList<>(getOtherItem(item, currentItem).getMarks()),
                        similarUsers,
                        currentUser
                );
            } catch (Exception e) {
                log.error(e.getMessage());
                continue;
            }

            SimpleSimilarUsers similarUser = getSimilarUsers(similarUsers.neighbours().get(currentUser), currentUser, neighboringMark.getUser());

            double averageMarkValueNeighboringItem = getAverageMark(new ArrayList<>(neighboringMark.getItem().getMarks()));

            top += similarUser.similarValue() * item.similarValue() * (neighboringMark.getMark() - averageMarkValueNeighboringItem);
            bottom += similarUser.similarValue() * item.similarValue();
        }

        if (top == 0.0 || bottom == 0.0)
            throw new Exception("Failed to calculate rating for item " + currentItem.getId() + " and user " + currentUser.getId());

        mark.setMark(averageMarkValueCurrentItem + (top / bottom));
        return mark;
    }

    private Mark generateMarkOnVagueSet(Mark mark, List<SimpleSimilarItems> neighbours, UserNeighbours similarUsers) throws Exception {
        Item currentItem = mark.getItem();
        User currentUser = mark.getUser();
        double averageMarkValueCurrentItem = getAverageMark(new ArrayList<>(currentItem.getMarks()));
        double top = 0.0;
        double bottom = 0.0;
        for (SimpleSimilarItems item : neighbours) {
            Mark neighboringMark;
            try {
                neighboringMark = getMarkFromSimilarUser(
                        new ArrayList<>(getOtherItem(item, currentItem).getMarks()),
                        similarUsers,
                        currentUser
                );
            } catch (Exception e) {
                log.error(e.getMessage());
                continue;
            }

            SimpleSimilarUsers similarUser = getSimilarUsers(similarUsers.neighbours().get(currentUser), currentUser, neighboringMark.getUser());

            double averageMarkValueNeighboringItem = getAverageMark(new ArrayList<>(neighboringMark.getItem().getMarks()));

            top += similarUser.similarValue() * item.similarValue() * (neighboringMark.getMark() - averageMarkValueNeighboringItem);
            bottom += similarUser.similarValue() * item.similarValue();
        }
        if (top == 0.0 || bottom == 0.0)
            throw new Exception("Failed to calculate rating for item " + currentItem.getId() + " and user " + currentUser.getId());

        mark.setMark(averageMarkValueCurrentItem + (top / bottom));
        return mark;
    }

    private Mark generateMarkOnMeanCentering(Mark mark, List<SimpleSimilarItems> neighbours) throws Exception {
        Item currentItem = mark.getItem();
        User currentUser = mark.getUser();
        double averageMarkValueCurrentItem = getAverageMark(new ArrayList<>(currentItem.getMarks()));
        double top = 0.0;
        double bottom = 0.0;
        for (SimpleSimilarItems item : neighbours) {
            Mark neighboringMark = getMarkFromUser(new ArrayList<>(getOtherItem(item, currentItem).getMarks()), currentUser);
            double averageMarkValueNeighboringItem = getAverageMark(new ArrayList<>(neighboringMark.getItem().getMarks()));
            top += item.similarValue() * (neighboringMark.getMark() - averageMarkValueNeighboringItem);
            bottom += Math.abs(item.similarValue());
        }
        if (top == 0.0 || bottom == 0.0)
            throw new Exception("Failed to calculate rating for item " + currentItem.getId() + " and user " + currentUser.getId());

        mark.setMark(averageMarkValueCurrentItem + (top / bottom));
        return mark;
    }
}
