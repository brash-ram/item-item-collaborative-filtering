package com.brash.util;

import com.brash.data.entity.Item;
import com.brash.filter.PartSimilarItems;

import java.util.ArrayList;
import java.util.List;

public class ItemUtils {

    public static List<PartSimilarItems> generatePairItems(List<Item> items) {
        List<PartSimilarItems> partSimilarItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            for (int n = 0; n < items.size(); n++) {
                List<Item> part = new ArrayList<>();
                part.add(item);
                part.add(items.get(n));
                partSimilarItems.add(new PartSimilarItems(part, null));
            }
        }
        return partSimilarItems;
    }
}
