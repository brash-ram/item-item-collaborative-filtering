package com.brash.filter.impl;

import com.brash.data.entity.HavingMarks;
import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.data.jpa.ItemRepository;
import com.brash.data.jpa.MarkRepository;
import com.brash.data.jpa.UserRepository;
import com.brash.filter.Filter;
import com.brash.filter.ItemToItemRecommendation;
import com.brash.filter.ItemToItemSimilarity;
import com.brash.filter.data.*;
import com.brash.util.Utils;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.brash.util.Utils.getItemFromFuzzySet;
import static com.brash.util.Utils.getUserFromFuzzySet;

/**
 * Реализация интерфейса, описывающего запуск алгоритма совместной фильтрации
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FilterModel implements Filter, AutoCloseable {

    private final ItemToItemSimilarity itemToItemSimilarity;
    private final ItemToItemRecommendation itemToItemRecommendation;

    private final ItemRepository itemRepository;
    private final MarkRepository markRepository;
    private final UserRepository userRepository;

    private final ExecutorService executorService;

    /**
     * Запуск обновления старых и генерации новых оценок рекомендации
     */
    @Override
    @Transactional
    public void updateRecommendations() {
        List<Item> items = itemRepository.findAll();
        List<User> users = userRepository.findAll();

        Future<ItemNeighbours> itemNeighboursFuture = executorService.submit(() -> generateItemSimilarity(items));
        Future<UserNeighbours> userNeighboursFuture = executorService.submit(() -> generateUserSimilarity(users));
        Future<Map<Item, List<Mark>>> futureMapForMarks = executorService.submit(() -> getUserAndItemForRecommendationMark(new HashSet<>(items), users));

        ItemNeighbours itemNeighbours;
        UserNeighbours userNeighbours;
        Map<Item, List<Mark>> mapForMarks;
        try {
            itemNeighbours = itemNeighboursFuture.get();
            userNeighbours = userNeighboursFuture.get();
            mapForMarks = futureMapForMarks.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return;
        }

        List<Mark> generatedMarks = itemToItemRecommendation.generateAllRecommendation(itemNeighbours, userNeighbours, mapForMarks);
        markRepository.saveAll(generatedMarks);
    }

    /**
     * Закрытие потоков при закрытии приложения
     */
    @Override
    public void close() {
        executorService.shutdownNow();
    }

    /**
     * Запускает генерацию значений сходства элементов
     * @param items Список всех элементов участвующих в генерации оценок
     * @return Список элементов и их ближайших соседей с оценкой сходства
     * @throws InterruptedException Возникает при прерывании потока
     */
    private ItemNeighbours generateItemSimilarity(List<Item> items) throws InterruptedException {
        List<HavingMarks> havingMarksItems = items.stream().map(item -> (HavingMarks)item).toList();
        List<SimilarItems> partsItem = itemToItemSimilarity.updateSimilarity(havingMarksItems);
        List<SimpleSimilarItems> similarItems = simplifySimilarItems(partsItem);
        return generateItemNeighbours(similarItems);
    }

    /**
     * Запускает генерацию значений сходства пользователей
     * @param users Список всех пользователей участвующих в генерации оценок
     * @return Список пользователей и их ближайших соседей с оценкой сходства
     * @throws InterruptedException Возникает при прерывании потока
     */
    private UserNeighbours generateUserSimilarity(List<User> users) throws InterruptedException {
        List<HavingMarks> havingMarksUsers = users.stream().map(item -> (HavingMarks)item).toList();
        List<SimilarItems> partsUser = itemToItemSimilarity.updateSimilarity(havingMarksUsers);
        List<SimpleSimilarUsers> similarUsers = simplifySimilarUsers(partsUser);
        return generateUserNeighbours(similarUsers);
    }

    /**
     * Формирует структуру пользователей и из ближайших соседей
     * @param similarUsers Список пар сходства пользователей
     * @return Структуру пользователей и из ближайших соседей
     * @throws InterruptedException Возникает при прерывании потока
     */
    private UserNeighbours generateUserNeighbours(List<SimpleSimilarUsers> similarUsers) throws InterruptedException {
        UserNeighbours userNeighbours = new UserNeighbours(new HashMap<>());

        for (int i = 0; i < similarUsers.size(); i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread with generating similarity" + Thread.currentThread().getName() + "is interrupted");
            }

            User user = similarUsers.get(i).user1();
            if (userNeighbours.neighbours().containsKey(user))
                continue;
            List<SimpleSimilarUsers> neighbours = new ArrayList<>();
            for (SimpleSimilarUsers similarUser : similarUsers) {
                User similarUser1 = similarUser.user1();
                User similarUser2 = similarUser.user2();
                if (user.equals(similarUser1) || user.equals(similarUser2)) {
                    neighbours.add(similarUser);
                }
            }
            userNeighbours.neighbours().put(user, neighbours);
        }

        User user = similarUsers.get(similarUsers.size() - 1).user2();
        List<SimpleSimilarUsers> neighbours = new ArrayList<>();
        for (SimpleSimilarUsers similarUser : similarUsers) {
            User similarUser1 = similarUser.user1();
            User similarUser2 = similarUser.user2();
            if (user.equals(similarUser1) || user.equals(similarUser2)) {
                neighbours.add(similarUser);
            }
        }
        userNeighbours.neighbours().put(user, neighbours);

        return userNeighbours;
    }

    /**
     * Формирует структуру элементов и из ближайших соседей
     * @param similarItems Список пар сходства элементов
     * @return Структуру элементов и из ближайших соседей
     * @throws InterruptedException Возникает при прерывании потока
     */
    private ItemNeighbours generateItemNeighbours(List<SimpleSimilarItems> similarItems) throws InterruptedException {
        ItemNeighbours itemNeighbours = new ItemNeighbours(new HashMap<>());

        for (int i = 0; i < similarItems.size(); i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread with generating similarity" + Thread.currentThread().getName() + "is interrupted");
            }

            Item item = similarItems.get(i).item1();
            if (itemNeighbours.neighbours().containsKey(item))
                continue;
            List<SimpleSimilarItems> neighbours = new ArrayList<>();
            for (SimpleSimilarItems similarItem : similarItems) {
                Item similarItem1 = similarItem.item1();
                Item similarItem2 = similarItem.item2();
                if (item.equals(similarItem1) || item.equals(similarItem2)) {
                    neighbours.add(similarItem);
                }
            }
            itemNeighbours.neighbours().put(item, neighbours);
        }

        Item item = similarItems.get(similarItems.size() - 1).item2();
        List<SimpleSimilarItems> neighbours = new ArrayList<>();
        for (SimpleSimilarItems similarItem : similarItems) {
            Item similarItem1 = similarItem.item1();
            Item similarItem2 = similarItem.item2();
            if (item.equals(similarItem1) || item.equals(similarItem2)) {
                neighbours.add(similarItem);
            }
        }
        itemNeighbours.neighbours().put(item, neighbours);

        return itemNeighbours;
    }

    /**
     * Формирует список простых пар сходства на основании пар сходства нечетких множеств
     * @param similarItems Список пар сходства нечетких множеств
     * @return Список простых пар сходства элементов
     */
    private List<SimpleSimilarItems> simplifySimilarItems(List<SimilarItems> similarItems) {
        List<SimpleSimilarItems> simpleSimilarItems = new ArrayList<>();
        for (SimilarItems item : similarItems) {
            simpleSimilarItems.add(
                    new SimpleSimilarItems(
                            getItemFromFuzzySet(item.fuzzySet1),
                            getItemFromFuzzySet(item.fuzzySet2),
                            item.similarValue
                    )
            );
        }
        return simpleSimilarItems;
    }


    /**
     * Формирует список простых пар сходства на основании пар сходства нечетких множеств
     * @param similarItems Список пар сходства нечетких множеств
     * @return Список простых пар сходства пользователей
     */
    private List<SimpleSimilarUsers> simplifySimilarUsers(List<SimilarItems> similarItems) {
        List<SimpleSimilarUsers> simpleSimilarUsers = new ArrayList<>();
        for (SimilarItems item : similarItems) {
            simpleSimilarUsers.add(
                    new SimpleSimilarUsers(
                            getUserFromFuzzySet(item.fuzzySet1),
                            getUserFromFuzzySet(item.fuzzySet2),
                            item.similarValue
                    )
            );
        }
        return simpleSimilarUsers;
    }

    /**
     * Создания набора элементов и его оценок, которые нужно сгенерировать
     * @param items Все элементы системы
     * @param users Все пользователи системы
     * @return Набор элементов и его оценок, которые нужно сгенерировать
     */
    private Map<Item, List<Mark>> getUserAndItemForRecommendationMark(Set<Item> items, List<User> users) {
        Map<Item, List<Mark>> generatingMarksForItem = new HashMap<>();

        for (User user : users) {
            try {
                Set<Item> allUserItems = user.getMarks().stream()
                        .map(Mark::getItem).collect(Collectors.toSet());
                Set<Item> itemsWithoutUserMark = Sets.difference(items, allUserItems);

                List<Mark> generatedMarks = user.getMarks().stream()
                        .filter(Mark::getIsGenerated)
                        .toList();

                Set<Item> userItemsWithGeneratedMark = generatedMarks.stream()
                        .map(Mark::getItem).collect(Collectors.toSet());

                userItemsWithGeneratedMark.addAll(itemsWithoutUserMark);
                for (Item item : userItemsWithGeneratedMark) {
                    Mark mark = null;
                    try {
                        mark = Utils.getMarkFromUser(generatedMarks, user);
                    } catch (Exception ignored) {
                        mark = new Mark().setItem(item).setUser(user).setIsGenerated(true);
                    }

                    if (generatingMarksForItem.containsKey(item)) {
                        generatingMarksForItem.get(item).add(mark);
                    } else {
                        generatingMarksForItem.put(
                                item,
                                new ArrayList<>(List.of(mark))
                        );
                    }
                }
            } catch (Exception e) {
                log.error(Arrays.toString(e.getStackTrace()));
            }

        }
        return generatingMarksForItem;
    }
}
