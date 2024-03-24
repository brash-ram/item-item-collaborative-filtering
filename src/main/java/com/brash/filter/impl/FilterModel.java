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
import com.brash.filter.SimilarityStorage;
import com.brash.filter.data.*;
import com.brash.util.FilterUtils;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.brash.util.FilterUtils.getItemFromFuzzySet;
import static com.brash.util.FilterUtils.getUserFromFuzzySet;

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

    private final SimilarityStorage similarityStorage;

    /**
     * Запуск обновления старых и генерации новых оценок рекомендации
     */
    @Override
    @Transactional
    @SneakyThrows
    public void updateRecommendations() {
        List<Item> items = itemRepository.findAll();
        List<User> users = userRepository.findAll();

        Future<ItemNeighbours> itemNeighboursFuture = executorService.submit(() -> getItemNeighbours(items));
        Future<UserNeighbours> userNeighboursFuture = executorService.submit(() -> getUserNeighbours(users));
        Future<Map<Item, List<Mark>>> futureMapForMarks = executorService.submit(() -> getUserAndItemForRecommendationMark(new HashSet<>(items), users));

        ItemNeighbours itemNeighbours;
        UserNeighbours userNeighbours;
        Map<Item, List<Mark>> mapForMarks;

        itemNeighbours = itemNeighboursFuture.get();
        if (itemNeighbours == null) {
            userNeighboursFuture.cancel(false);
            futureMapForMarks.cancel(false);
            return;
        }
        userNeighbours = userNeighboursFuture.get();
        mapForMarks = futureMapForMarks.get();

        List<Mark> generatedMarks = itemToItemRecommendation.generateAllRecommendation(itemNeighbours, userNeighbours, mapForMarks);
        markRepository.saveAll(generatedMarks);
    }

    @Override
    public void generateItemAndUserSimilarity() {
        List<Item> items = itemRepository.findAll();
        List<User> users = userRepository.findAll();

        executorService.execute(() -> {
            try {
                generateItemSimilarity(items);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        });

        executorService.execute(() -> {
            try {
                generateUserSimilarity(users);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        });
    }

    /**
     * Закрытие потоков при закрытии приложения
     */
    @Override
    public void close() {
        executorService.shutdownNow();
    }

    /**
     * Получить сходства элементов
     * @return Список элементов и их ближайших соседей с оценкой сходства или null, если невозможно получить список
     * @throws InterruptedException Возникает при прерывании потока
     */
    private ItemNeighbours getItemNeighbours(List<Item> items) throws InterruptedException {
        if (similarityStorage.getItemNeighbours() == null) {
            if (similarityStorage.isGenerating()) return null;

            generateItemSimilarity(items);
        }
        return similarityStorage.getItemNeighbours();
    }

    /**
     * Получить сходства элементов
     * @return Список элементов и их ближайших соседей с оценкой сходства или null, если невозможно получить список
     * @throws InterruptedException Возникает при прерывании потока
     */
    private UserNeighbours getUserNeighbours(List<User> users) throws InterruptedException {
        if (similarityStorage.getUserNeighbours() == null) {
            if (similarityStorage.isGenerating()) return null;

            generateUserSimilarity(users);
        }
        return similarityStorage.getUserNeighbours();
    }

    /**
     * Запускает генерацию значений сходства элементов
     *
     * @param items Список всех элементов участвующих в генерации оценок
     * @throws InterruptedException Возникает при прерывании потока
     */
    private void generateItemSimilarity(List<Item> items) throws InterruptedException {
        List<HavingMarks> havingMarksItems = items.stream().map(item -> (HavingMarks)item).toList();
        if (havingMarksItems.isEmpty()) {
            return;
        }
        List<SimilarItems> partsItem = itemToItemSimilarity.updateSimilarity(havingMarksItems);
        List<SimpleSimilarItems> similarItems = simplifySimilarItems(partsItem);
        ItemNeighbours result = generateItemNeighbours(similarItems);
        similarityStorage.setItemNeighbours(result);
    }

    /**
     * Запускает генерацию значений сходства пользователей
     *
     * @param users Список всех пользователей участвующих в генерации оценок
     * @throws InterruptedException Возникает при прерывании потока
     */
    private void generateUserSimilarity(List<User> users) throws InterruptedException {
        List<HavingMarks> havingMarksUsers = users.stream().map(item -> (HavingMarks)item).toList();
        if (havingMarksUsers.isEmpty()) {
            return;
        }
        List<SimilarItems> partsUser = itemToItemSimilarity.updateSimilarity(havingMarksUsers);
        List<SimpleSimilarUsers> similarUsers = simplifySimilarUsers(partsUser);
        UserNeighbours result = generateUserNeighbours(similarUsers);
        for (var userEntry : result.neighbours().entrySet()) {
            FilterUtils.getSortedListSimilarUsers(userEntry.getValue());
        }
        similarityStorage.setUserNeighbours(result);
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
            FilterUtils.addUserNeighbours(userNeighbours, similarUsers, user);
        }

        if (!similarUsers.isEmpty()) {
            User user = similarUsers.getLast().user2();
            FilterUtils.addUserNeighbours(userNeighbours, similarUsers, user);
        }

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
            FilterUtils.addItemNeighbours(itemNeighbours, similarItems, item);
        }

        if (!similarItems.isEmpty()) {
            Item item = similarItems.getLast().item2();
            FilterUtils.addItemNeighbours(itemNeighbours, similarItems, item);
        }

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
                        .filter(Mark::isGenerated)
                        .toList();

                Set<Item> userItemsWithGeneratedMark = generatedMarks.stream()
                        .map(Mark::getItem).collect(Collectors.toSet());

                userItemsWithGeneratedMark.addAll(itemsWithoutUserMark);
                for (Item item : userItemsWithGeneratedMark) {
                    Mark mark;
                    try {
                        mark = FilterUtils.getMarkFromUser(generatedMarks, user);
                    } catch (Exception ignored) {
                        mark = new Mark().setItem(item).setUser(user).setGenerated(true);
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
