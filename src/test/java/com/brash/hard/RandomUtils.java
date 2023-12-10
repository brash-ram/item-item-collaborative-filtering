package com.brash.hard;

import com.brash.data.entity.Item;

import java.util.List;
import java.util.Random;

public class RandomUtils {

    private Random random = new Random();

    public int getRandomInt(int min, int max) {
        return random.nextInt(max - min) + min;
    }

    public Item getRandomUniqueItem(List<Item> items, List<Item> markedItems) {
        while (true) {
            int index = getRandomInt(0, items.size() - 1);
            Item item = items.get(index);
            if (!markedItems.contains(item))
                return item;
        }
    }
}
