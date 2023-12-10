package com.brash.util;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.filter.data.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class Utils {
    public static Item getItemFromFuzzySet(FuzzySet fuzzySet) {
        return fuzzySet.getSet().get(0).mark().getItem();
    }

    public static User getUserFromFuzzySet(FuzzySet fuzzySet) {
        return fuzzySet.getSet().get(0).mark().getUser();
    }

    public static Item getOtherItem(SimpleSimilarItems similarItems, Item item) {
        return similarItems.item1().equals(item) ? similarItems.item2() : similarItems.item1();
    }

    public static User getOtherUser(SimpleSimilarUsers similarUsers, User user) {
        return similarUsers.user1().equals(user) ? similarUsers.user2() : similarUsers.user1();
    }

    public static SimpleSimilarUsers getSimilarUser(UserNeighbours userNeighbours, User user) throws Exception {
        if (!userNeighbours.neighbours().containsKey(user))
            throw new Exception("User " + user.getId() + " dont have neighbours");

        List<SimpleSimilarUsers> neighbours = getSortedListSimilarUsers(userNeighbours.neighbours().get(user));

        return neighbours.get(neighbours.size() - 1);
    }

    public static SimpleSimilarUsers getSimilarUsers(List<SimpleSimilarUsers> similarUsers, User user1, User user2) throws Exception {
        for (SimpleSimilarUsers simpleSimilarUsers : similarUsers) {
            if ((simpleSimilarUsers.user1().equals(user1) || simpleSimilarUsers.user2().equals(user1)) &&
                    (simpleSimilarUsers.user1().equals(user2) || simpleSimilarUsers.user2().equals(user2))) {
                return simpleSimilarUsers;
            }
        }
        throw new Exception("No similar users for user1 " + user1.getId() + " and user2 " + user2.getId());
    }

    public static Mark getMarkFromSimilarUser(List<Mark> marks, UserNeighbours userNeighbours, User user) throws Exception {
        if (!userNeighbours.neighbours().containsKey(user))
            throw new Exception("User " + user.getId() + " dont have neighbours");

        List<SimpleSimilarUsers> neighbours = getSortedListSimilarUsers(userNeighbours.neighbours().get(user));

        for (int i = neighbours.size() - 1; i >= 0; i--) {
            try {
                return getMarkFromUser(marks, getOtherUser(neighbours.get(i), user));
            } catch (Exception ignored) {

            }
        }
        throw new Exception("No similar user with mark for item");
    }

    public static Mark getMarkFromUser(List<Mark> marks, User user) throws Exception {
        for (Mark mark : marks) {
            if (mark.getUser().equals(user)) {
                return mark;
            }
        }
        throw new Exception("Mark from user " + user.getId() + " not found for items");
    }

    public static boolean fuzzySetContainsUser(User user, FuzzySet fuzzySet) {
        for (FuzzySetItem item : fuzzySet.getSet()) {
            if (item.mark().getUser().equals(user)) {
                return true;
            }
        }
        return false;
    }

    public static double getAverageMark(List<Mark> markForSameItem) {
        double top = 0;
        for (Mark mark : markForSameItem) {
            top += mark.getMark();
        }
        return top / markForSameItem.size();
    }

    private static List<SimpleSimilarUsers> getSortedListSimilarUsers(List<SimpleSimilarUsers> neighbours) {
        neighbours.sort(Comparator.comparingDouble(SimpleSimilarUsers::similarValue));
        return neighbours;
    }
}
