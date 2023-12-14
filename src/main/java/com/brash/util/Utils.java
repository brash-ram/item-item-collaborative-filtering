package com.brash.util;

import com.brash.data.entity.HavingMarks;
import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.filter.data.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Класс вспомогательных функций для генерации оценок
 */
public class Utils {

    /**
     * Монитор для синхронизации сортировки списков
     */
    private static final Object sortLock = new Object();

    /**
     * Расчет логарифма по основанию 2
     * @param value Переменная
     * @return Логарифм по основанию 2
     */
    public static double log2(double value) {
        return Math.log(value) / Math.log(2);
    }

    /**
     * Получить элемент из нечеткого множества
     * @param fuzzySet Нечеткое множество
     * @return Элемент
     */
    public static Item getItemFromFuzzySet(FuzzySet fuzzySet) {
        return fuzzySet.getSet().get(0).mark().getItem();
    }

    /**
     * Получить пользователя из нечеткого множества
     * @param fuzzySet Нечеткое множество
     * @return Пользователь
     */
    public static User getUserFromFuzzySet(FuzzySet fuzzySet) {
        return fuzzySet.getSet().get(0).mark().getUser();
    }

    /**
     * Получить другой элемент из простой пары сходства
     * @param similarItems Пара сходства элементов
     * @param item Элемент
     * @return Противоположный переданному элемент
     */
    public static Item getOtherItem(SimpleSimilarItems similarItems, Item item) {
        return similarItems.item1().equals(item) ? similarItems.item2() : similarItems.item1();
    }

    /**
     * Получить другого пользователя из простой пары сходства
     * @param similarUsers Пара сходства пользователей
     * @param user Пользователь
     * @return Противоположный переданному пользователь
     */
    public static User getOtherUser(SimpleSimilarUsers similarUsers, User user) {
        return similarUsers.user1().equals(user) ? similarUsers.user2() : similarUsers.user1();
    }

    /**
     * Получить объект пары сходства с указанными пользователями
     * @param similarUsers Список пар сходства пользователей
     * @param user1 Пользователь 1
     * @param user2 Пользователь 2
     * @return Объект пары сходства
     * @throws Exception Объект с заданными пользователями не найден
     */
    public static SimpleSimilarUsers getSimilarUsers(List<SimpleSimilarUsers> similarUsers, User user1, User user2) throws Exception {
        for (SimpleSimilarUsers simpleSimilarUsers : similarUsers) {
            if ((simpleSimilarUsers.user1().equals(user1) || simpleSimilarUsers.user2().equals(user1)) &&
                    (simpleSimilarUsers.user1().equals(user2) || simpleSimilarUsers.user2().equals(user2))) {
                return simpleSimilarUsers;
            }
        }
        throw new Exception("No similar users for user1 " + user1.getId() + " and user2 " + user2.getId());
    }

    /**
     * Получить оценку от ближайшего соседа пользователя
     * @param marks Список оценок
     * @param userNeighbours Соседи пользователей
     * @param user Пользователь
     * @return Найденная оценка
     * @throws Exception Пользователь не имеет соседей или оценка не найдена
     */
    public static Mark getMarkFromSimilarUser(List<Mark> marks, UserNeighbours userNeighbours, User user) throws Exception {
        if (!userNeighbours.neighbours().containsKey(user) || userNeighbours.neighbours().get(user).isEmpty())
            throw new Exception("User " + user.getId() + " dont have neighbours");

        List<SimpleSimilarUsers> neighbours = getSortedListSimilarUsers(userNeighbours.neighbours().get(user));

        for (int i = neighbours.size() - 1; i >= 0; i--) {
            try {
                return getMarkFromUser(marks, getOtherUser(neighbours.get(i), user));
            } catch (Exception ignored) {
//                ignored.printStackTrace();
            }
        }
        throw new Exception("No similar user with mark for item");
    }

    /**
     * Получить оценку пользователя из списка оценок
     * @param marks Оценки
     * @param user Пользователь
     * @return Найденная оценка
     * @throws Exception Оценка не найдена
     */
    public static Mark getMarkFromUser(List<Mark> marks, User user) throws Exception {
        for (Mark mark : marks) {
            if (mark.getUser().equals(user)) {
                return mark;
            }
        }
        throw new Exception("Mark from user " + user.getId() + " not found for items");
    }

    /**
     * Получить среднюю арифметическое оценок
     * @param markForSameItem Список оценок
     * @return Среднее арифметическое оценок
     */
    public static double getAverageMark(List<Mark> markForSameItem) {
        double top = 0;
        for (Mark mark : markForSameItem) {
            top += mark.getMark();
        }
        return top / markForSameItem.size();
    }

    /**
     * Получить среднее арифметическое оценок от объекта имеющего оценки
     * @param mark Оценка
     * @param type Тип элемента
     * @return Среднее арифметическое оценок
     */
    public static double getAverageMarkFromUserOrItem(Mark mark, HavingMarks type) {
        List<Mark> marks;
        if (type instanceof Item) {
            marks = new ArrayList<>(mark.getUser().getMarks());
        } else {
            marks = new ArrayList<>(mark.getItem().getMarks());
        }

        return getAverageMark(marks);
    }

    /**
     * Отсортировать список пар сходства пользователей
     * @param neighbours Пары сходства пользователей
     * @return Отсортированный список
     */
    private static List<SimpleSimilarUsers> getSortedListSimilarUsers(List<SimpleSimilarUsers> neighbours) {
        synchronized (sortLock) {
            neighbours.sort(Comparator.comparingDouble(SimpleSimilarUsers::similarValue));
        }
        return neighbours;
    }
}
