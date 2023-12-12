package com.brash.filter.impl;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.filter.ItemToItemRecommendation;
import com.brash.filter.data.ItemNeighbours;
import com.brash.filter.data.SimpleSimilarItems;
import com.brash.filter.data.SimpleSimilarUsers;
import com.brash.filter.data.UserNeighbours;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static com.brash.util.Utils.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationGenerator implements ItemToItemRecommendation {

    private final ExecutorService executorService;

    private final Object lock = new Object();

    private List<Mark> generatedMarks;

    @Override
    public List<Mark> generateAllRecommendation(
            ItemNeighbours itemNeighbours,
            UserNeighbours userNeighbours,
            Map<Item, List<Mark>> generatingMarks
    ) {
        generatedMarks = new ArrayList<>();

        int numberLatch = 0;
        for (Map.Entry<Item, List<SimpleSimilarItems>> entry : itemNeighbours.neighbours().entrySet()) {
            Item currentItem = entry.getKey();
            List<Mark> marksForGeneratingWithItem = generatingMarks.get(currentItem);
            numberLatch += marksForGeneratingWithItem.size();
        }

        CountDownLatch latch = new CountDownLatch(numberLatch);

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
                if (neighbours.size() == neighboursWithMark.size()) {
                    executorService.execute(() -> {
                        try {
                            generateMarkOnMeanCentering(mark, neighbours);
                        } catch (Exception e) {
//                            log.error(Arrays.toString(e.getStackTrace()));
                            e.printStackTrace();
                        }
                        latch.countDown();
                    });
//                        generateMarkOnMeanCentering(mark, neighbours);
                } else if (neighboursWithMark.size() == 0) {
                    executorService.execute(() -> {
                        try {
                            generateMarkOnVagueSet(mark, neighbours, userNeighbours);
                        } catch (Exception e) {
//                            log.error(Arrays.toString(e.getStackTrace()));
                            e.printStackTrace();
                        }
                        latch.countDown();
                    });
//                    generateMarkOnVagueSet(mark, neighbours, userNeighbours);
                } else {
                    List<SimpleSimilarItems> neighboursWithoutMark = neighbours.stream()
                            .filter(item ->
                                    !getOtherItem(item, currentItem).getMarks().stream()
                                            .map(Mark::getUser)
                                            .toList()
                                            .contains(currentUser)
                            )
                            .toList();
                    executorService.execute(() -> {
                        try {
                            generateMarkOnSparseData(mark, neighboursWithMark, neighboursWithoutMark, userNeighbours);
                        } catch (Exception e) {
//                            log.error(Arrays.toString(e.getStackTrace()));
                            e.printStackTrace();
                        }
                        latch.countDown();
                    });
//                    generateMarkOnSparseData(mark, neighboursWithMark, neighboursWithoutMark, userNeighbours);
                }
            }
        }
        try {
            latch.await();
        } catch (InterruptedException ignored) {
        }
        return generatedMarks;
    }

    private void generateMarkOnSparseData(
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
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            Mark neighboringMark = getMarkFromUser(new ArrayList<>(getOtherItem(item, currentItem).getMarks()), currentUser);

            double averageMarkValueNeighboringItem = getAverageMark(new ArrayList<>(neighboringMark.getItem().getMarks()));

            top += item.similarValue() * (neighboringMark.getMark() - averageMarkValueNeighboringItem);
            bottom += Math.abs(item.similarValue());
        }

        for (SimpleSimilarItems item : neighboursWithoutMark) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            Mark neighboringMark;
            try {
                neighboringMark = getMarkFromSimilarUser(
                        new ArrayList<>(getOtherItem(item, currentItem).getMarks()),
                        similarUsers,
                        currentUser
                );
            } catch (Exception e) {
//                log.error(e.getMessage());
                e.printStackTrace();
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
        synchronized (lock) {
            generatedMarks.add(mark);
        }

//        return mark;
    }

    private void generateMarkOnVagueSet(Mark mark, List<SimpleSimilarItems> neighbours, UserNeighbours similarUsers) throws Exception {
        Item currentItem = mark.getItem();
        User currentUser = mark.getUser();
        double averageMarkValueCurrentItem = getAverageMark(new ArrayList<>(currentItem.getMarks()));
        double top = 0.0;
        double bottom = 0.0;
        for (SimpleSimilarItems item : neighbours) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            Mark neighboringMark;
            try {
                neighboringMark = getMarkFromSimilarUser(
                        new ArrayList<>(getOtherItem(item, currentItem).getMarks()),
                        similarUsers,
                        currentUser
                );
            } catch (Exception e) {
//                log.error(e.getMessage());
                e.printStackTrace();
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
        synchronized (lock) {
            generatedMarks.add(mark);
        }

//        return mark;
    }

    private void generateMarkOnMeanCentering(Mark mark, List<SimpleSimilarItems> neighbours) throws Exception {
        Item currentItem = mark.getItem();
        User currentUser = mark.getUser();
        double averageMarkValueCurrentItem = getAverageMark(new ArrayList<>(currentItem.getMarks()));
        double top = 0.0;
        double bottom = 0.0;
        for (SimpleSimilarItems item : neighbours) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            Mark neighboringMark = getMarkFromUser(new ArrayList<>(getOtherItem(item, currentItem).getMarks()), currentUser);
            double averageMarkValueNeighboringItem = getAverageMark(new ArrayList<>(neighboringMark.getItem().getMarks()));
            top += item.similarValue() * (neighboringMark.getMark() - averageMarkValueNeighboringItem);
            bottom += Math.abs(item.similarValue());
        }
        if (top == 0.0 || bottom == 0.0)
            throw new Exception("Failed to calculate rating for item " + currentItem.getId() + " and user " + currentUser.getId());

        mark.setMark(averageMarkValueCurrentItem + (top / bottom));
        synchronized (lock) {
            generatedMarks.add(mark);
        }

//        return mark;
    }
}
