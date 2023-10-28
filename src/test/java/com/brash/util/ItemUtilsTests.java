package com.brash.util;

import com.brash.data.entity.Item;
import com.brash.filter.PartSimilarItems;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemUtilsTests {

    @Test
    public void generatePairItemsTest() {
        List<Item> items = List.of(
                new Item(1L, 1L, null),
                new Item(2L, 2L, null),
                new Item(3L, 3L, null),
                new Item(4L, 4L, null)
        );
        List<PartSimilarItems> parts = ItemUtils.generatePairItems(items);

        for (PartSimilarItems part : parts) {
            System.out.println(part.items.get(0).getId()+ " " + part.items.get(1).getId());
        }
        assertEquals(6, parts.size());
    }
}
