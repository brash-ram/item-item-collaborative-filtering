package com.brash.hard.gauss;

import com.brash.data.entity.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.brash.hard.gauss.GaussHardTestSettings.*;

public class GaussUtils {

    public static Map<User, Integer> getUsersWithGaussianDistribution(List<User> users) {
        Map<User, Integer> usersWithGaussianDistribution = new HashMap<>();

        Map<Integer, Integer> numberItemsMap = new HashMap<>() {{
            put((int) (NUMBER_ITEMS * 0.001), (int) (users.size() * 0.002));
            put((int) (NUMBER_ITEMS * 0.01), (int) (users.size() * 0.021));
            put((int) (NUMBER_ITEMS * 0.03), (int) (users.size() * 0.136));
            put((int) (NUMBER_ITEMS * 0.05), (int) (users.size() * 0.341));
            put((int) (NUMBER_ITEMS * 0.07), (int) (users.size() * 0.341));
            put((int) (NUMBER_ITEMS * 0.1), (int) (users.size() * 0.136));
            put((int) (NUMBER_ITEMS * 0.15), (int) (users.size() * 0.021));
            put((int) (NUMBER_ITEMS * 0.30), (int) (users.size() * 0.002));
        }};

        for (User user : users) {
            boolean added = false;
            for (Map.Entry<Integer, Integer> entry : numberItemsMap.entrySet()) {
                if (entry.getValue() != 0) {
                    usersWithGaussianDistribution.put(user, entry.getKey());
                    numberItemsMap.put(entry.getKey(), entry.getValue() - 1);
                    added = true;
                    break;
                }
            }

            if (!added) {
                usersWithGaussianDistribution.put(user, (int) (NUMBER_ITEMS * 0.05));
            }
        }

        return usersWithGaussianDistribution;
    }
}
