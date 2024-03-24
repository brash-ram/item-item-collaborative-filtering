package com.brash.filter.impl;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.filter.ItemToItemRecommendation;
import com.brash.filter.data.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static com.brash.util.FilterUtils.*;

/**
 * Реализация генератора оценок на основе определения соседей как элементов так и пользователей
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationGenerator implements ItemToItemRecommendation {

    /**
     * ExecutorService для параллельной обработки
     */
    private final ExecutorService executorService;

    /**
     * Монитор для синхронизации списка сгенерированных оценок
     */
    private final Object lock = new Object();

    /**
     * Список сгенерированных оценок
     */
    private List<Mark> generatedMarks;

    /**
     * Генерация рекомендаций на основе определения соседей как элементов так и пользователей
     * @param itemNeighbours Элементы и их ближайшие соседи
     * @param userNeighbours Пользователи и их ближайшие соседи
     * @param generatingMarks Таблица элементов для которых нужно сгенерировать оценки
     * @return Сгенерированные оценки
     */
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

            if (marksForGeneratingWithItem == null)
                continue;

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
                                    getOtherItem(item, currentItem).getNotGeneratedMarks().stream()
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
                            log.error(Arrays.toString(e.getStackTrace()));
//                            e.printStackTrace();
                        }
                        latch.countDown();
                    });
                } else if (neighboursWithMark.isEmpty()) {
                    executorService.execute(() -> {
                        try {
                            generateMarkOnVagueSet(mark, neighbours, userNeighbours);
                        } catch (Exception e) {
                            log.error(Arrays.toString(e.getStackTrace()));
//                            e.printStackTrace();
                        }
                        latch.countDown();
                    });
                } else {
                    List<SimpleSimilarItems> neighboursWithoutMark = neighbours.stream()
                            .filter(item ->
                                    !getOtherItem(item, currentItem).getNotGeneratedMarks().stream()
                                            .map(Mark::getUser)
                                            .toList()
                                            .contains(currentUser)
                            )
                            .toList();
                    executorService.execute(() -> {
                        try {
                            generateMarkOnSparseData(mark, neighboursWithMark, neighboursWithoutMark, userNeighbours);
                        } catch (Exception e) {
                            log.error(Arrays.toString(e.getStackTrace()));
//                            e.printStackTrace();
                        }
                        latch.countDown();
                    });
                }
            }
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }
        return generatedMarks;
    }

    /**
     * Генерирует оценку на основе оценок пользователя и ближайших соседей пользователя.
     * Используется когда пользователь оценил не всех ближайших соседей элемента.
     * Отсутствующие оценки берутся у ближайшего соседа пользователя.
     * @param mark Оценка для генерации
     * @param neighboursWithMark Соседи элемента с оценкой пользователя
     * @param neighboursWithoutMark Соседи элемента без оценки пользователя
     * @param similarUsers Соседи пользователей
     * @throws Exception Ошибка отсутствия ожидаемой оценки пользователя
     */
    private void generateMarkOnSparseData(
            Mark mark,
            List<SimpleSimilarItems> neighboursWithMark,
            List<SimpleSimilarItems> neighboursWithoutMark,
            UserNeighbours similarUsers
    ) throws Exception {
        Item currentItem = mark.getItem();
        User currentUser = mark.getUser();
        double averageMarkValueCurrentItem = currentItem.getAverageMarks();
        double top = 0.0;
        double bottom = 0.0;
        for (SimpleSimilarItems item : neighboursWithMark) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            Mark userMark = getMarkFromUser(
                    getOtherItem(item, currentItem).getNotGeneratedMarks(),
                    currentUser
            );

            double averageMarkValueNeighboringItem = userMark.getItem().getAverageMarks();

            top += item.similarValue() * (userMark.getMark() - averageMarkValueNeighboringItem);
            bottom += Math.abs(item.similarValue());
        }

        for (SimpleSimilarItems item : neighboursWithoutMark) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            SimilarUser similarUserWithMark;

            try {
                similarUserWithMark = getMarkFromSimilarUser(
                        getOtherItem(item, currentItem).getNotGeneratedMarks(),
                        similarUsers,
                        currentUser
                );
            } catch (Exception e) {
//                log.error(e.getMessage());
                e.printStackTrace();
                continue;
            }
            Mark neighboringMark = similarUserWithMark.mark();
            SimpleSimilarUsers similarUser = similarUserWithMark.similarUsers();

            double averageMarkValueNeighboringItem = neighboringMark.getItem().getAverageMarks();

            top += similarUser.similarValue() * item.similarValue() *
                    (neighboringMark.getMark() - averageMarkValueNeighboringItem);

            bottom += similarUser.similarValue() * item.similarValue();
        }

        if (top == 0.0 || bottom == 0.0)
            throw new Exception("Failed to calculate rating for item " + currentItem.getId() + " and user " + currentUser.getId());
        mark.setMark(averageMarkValueCurrentItem + (top / bottom));
        synchronized (lock) {
            generatedMarks.add(mark);
        }
    }

    /**
     * Генерирует оценку на основе оценок ближайших соседей пользователя.
     * Используется когда пользователь не оценил всех ближайших соседей элемента.
     * Отсутствующие оценки берутся у ближайшего соседа пользователя.
     * @param mark Оценка для генерации
     * @param neighbours Соседи элемента
     * @param similarUsers Соседи пользователей
     * @throws Exception Ошибка отсутствия ожидаемой оценки пользователя
     */
    private void generateMarkOnVagueSet(
            Mark mark,
            List<SimpleSimilarItems> neighbours,
            UserNeighbours similarUsers
    ) throws Exception {
        Item currentItem = mark.getItem();
        User currentUser = mark.getUser();
        double averageMarkValueCurrentItem = currentItem.getAverageMarks();
        double top = 0.0;
        double bottom = 0.0;
        for (SimpleSimilarItems item : neighbours) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            SimilarUser similarUserWithMark;

            try {
                similarUserWithMark = getMarkFromSimilarUser(
                        getOtherItem(item, currentItem).getNotGeneratedMarks(),
                        similarUsers,
                        currentUser
                );
            } catch (Exception e) {
//                log.error(e.getMessage());
                e.printStackTrace();
                continue;
            }
            Mark neighboringMark = similarUserWithMark.mark();
            SimpleSimilarUsers similarUser = similarUserWithMark.similarUsers();

            double averageMarkValueNeighboringItem = neighboringMark.getItem().getAverageMarks();

            top += similarUser.similarValue() * item.similarValue() *
                    (neighboringMark.getMark() - averageMarkValueNeighboringItem);

            bottom += similarUser.similarValue() * item.similarValue();
        }
        if (top == 0.0 || bottom == 0.0)
            throw new Exception("Failed to calculate rating for item " + currentItem.getId() + " and user " + currentUser.getId());

        mark.setMark(averageMarkValueCurrentItem + (top / bottom));
        synchronized (lock) {
            generatedMarks.add(mark);
        }
    }

    /**
     * Генерирует оценку на основе оценок пользователя.
     * Используется когда пользователь оценил всех ближайших соседей элемента.
     * @param mark Оценка для генерации
     * @param neighbours Соседи элемента
     * @throws Exception Ошибка отсутствия ожидаемой оценки пользователя
     */
    private void generateMarkOnMeanCentering(
            Mark mark,
            List<SimpleSimilarItems> neighbours
    ) throws Exception {
        Item currentItem = mark.getItem();
        User currentUser = mark.getUser();
        double averageMarkValueCurrentItem = currentItem.getAverageMarks();
        double top = 0.0;
        double bottom = 0.0;
        for (SimpleSimilarItems item : neighbours) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            Mark userMark = getMarkFromUser(
                    getOtherItem(item, currentItem).getNotGeneratedMarks(),
                    currentUser
            );

            double averageMarkValueNeighboringItem = userMark.getItem().getAverageMarks();

            top += item.similarValue() * (userMark.getMark() - averageMarkValueNeighboringItem);
            bottom += Math.abs(item.similarValue());
        }
        if (top == 0.0 || bottom == 0.0)
            throw new Exception("Failed to calculate rating for item " + currentItem.getId() + " and user " + currentUser.getId());

        mark.setMark(averageMarkValueCurrentItem + (top / bottom));
        synchronized (lock) {
            generatedMarks.add(mark);
        }
    }
}
