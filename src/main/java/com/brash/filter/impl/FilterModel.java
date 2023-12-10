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
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


import static com.brash.util.Utils.*;

/**
 * Реализация интерфейса, описывающего запуск алгоритма совместной фильтрации
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FilterModel implements Filter {

    private final ItemToItemSimilarity itemToItemSimilarity;
    private final ItemToItemRecommendation itemToItemRecommendation;

    private final ItemRepository itemRepository;
    private final MarkRepository markRepository;
    private final UserRepository userRepository;

    /**
     * Запуск обновления старых и генерации новых оценок рекомендации
     */
    @Override
    @Transactional
    public void updateRecommendations() {
        List<Item> items = itemRepository.findAll();
        List<User> users = userRepository.findAll();

        ItemNeighbours itemNeighbours = generateItemSimilarity(items);
        UserNeighbours userNeighbours = generateUserSimilarity(users);

        Map<Item, List<Mark>> mapForMarks = getUserAndItemForRecommendationMark(new HashSet<>(items), users);
        System.out.println("Начинается генерация рекомендаций");
        List<Mark> generatedMarks = itemToItemRecommendation.generateAllRecommendation(itemNeighbours, userNeighbours, mapForMarks);
        markRepository.saveAll(generatedMarks);
    }

    private ItemNeighbours generateItemSimilarity(List<Item> items) {
        List<HavingMarks> havingMarksItems = items.stream().map(item -> (HavingMarks)item).toList();
        List<SimilarItems> partsItem = itemToItemSimilarity.updateSimilarity(havingMarksItems);
        List<SimpleSimilarItems> similarItems = simplifySimilarItems(partsItem);
        return generateItemNeighbours(similarItems);
    }

    private UserNeighbours generateUserSimilarity(List<User> users) {
        List<HavingMarks> havingMarksUsers = users.stream().map(item -> (HavingMarks)item).toList();
        List<SimilarItems> partsUser = itemToItemSimilarity.updateSimilarity(havingMarksUsers);
        List<SimpleSimilarUsers> similarUsers = simplifySimilarUsers(partsUser);
        return generateUserNeighbours(similarUsers);
    }

    private UserNeighbours generateUserNeighbours(List<SimpleSimilarUsers> similarUsers) {
        UserNeighbours userNeighbours = new UserNeighbours(new HashMap<>());

        for (int i = 0; i < similarUsers.size(); i++) {
            User user = similarUsers.get(i).user1();
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

    private ItemNeighbours generateItemNeighbours(List<SimpleSimilarItems> similarItems) {
        ItemNeighbours itemNeighbours = new ItemNeighbours(new HashMap<>());

        for (int i = 0; i < similarItems.size(); i++) {
            Item item = similarItems.get(i).item1();
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
     * Создания набора пользователей для которых нужно сгенерировать
     * или обновить оценку рекомендации для указанных элементов.
     * @param items Все элементы системы
     * @param users Все пользователи системы
     * @return Набор пользователей для которых нужно сгенерировать
     * или обновить оценку рекомендации для указанных элементов.
     */
    private Map<Item, List<Mark>> getUserAndItemForRecommendationMark(Set<Item> items, List<User> users) {
        Map<Item, List<Mark>> generatingMarksForItem = new HashMap<>();

        for (User user : users) {
            try {
                Set<Item> allUserItems = user.getMarks().stream()
                        .map(Mark::getItem).collect(Collectors.toSet());
                Set<Item> itemsWithoutUserMark = Sets.difference(items, allUserItems);
                Set<Item> userItemsWithGeneratedMark = user.getMarks().stream()
                        .filter(Mark::getIsGenerated)
                        .map(Mark::getItem).collect(Collectors.toSet());
                userItemsWithGeneratedMark.addAll(itemsWithoutUserMark);
                for (Item item : userItemsWithGeneratedMark) {
                    if (generatingMarksForItem.containsKey(item)) {
                        generatingMarksForItem.get(item).add(new Mark().setItem(item).setUser(user).setIsGenerated(true));
                    } else {
                        generatingMarksForItem.put(item, new ArrayList<>(List.of(new Mark().setItem(item).setUser(user).setIsGenerated(true))));
                    }
                }
            } catch (Exception e) {
                log.error(Arrays.toString(e.getStackTrace()));
            }

        }
        return generatingMarksForItem;
    }
}
