package com.brash.util;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.filter.PartSimilarItems;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ItemUtilsTests {

    @Test
    public void generatePairItemsTest() {
        Item[] arr = {
                new Item(1L, 1L, null),
                new Item(2L, 2L, null),
                new Item(3L, 3L, null),
                new Item(4L, 4L, null),
        };

        List<Item> items = List.of(arr);
        List<PartSimilarItems> parts = ItemUtils.generatePairItems(items);

        for (PartSimilarItems part : parts) {
            System.out.println(part.items.get(0).getId()+ " " + part.items.get(1).getId());
        }
        assertEquals(16, parts.size());
    }
    @Test
    public void comparatorTest() {
        List<Mark> marks = List.of(
                new Mark().setItem(new Item(1L, 1L, null)),
                new Mark().setItem(new Item(2L, 2L, null)),
                new Mark().setItem(new Item(3L, 3L, null))
        );

        int markIndex = Collections.binarySearch(
                marks,
                new Mark().setItem(new Item(3L, 3L, null)),
                Comparator.comparingLong(mark1 -> mark1.getItem().getId()));

        assertEquals(2, markIndex);

        int markIndex2 = Collections.binarySearch(
                marks,
                new Mark().setItem(new Item(1L, 3L, null)),
                Comparator.comparingLong(mark1 -> mark1.getItem().getId()));

        assertEquals(0, markIndex2);

        int markIndex3 = Collections.binarySearch(
                marks,
                new Mark().setItem(new Item(2L, 3L, null)),
                Comparator.comparingLong(mark1 -> mark1.getItem().getId()));

        assertEquals(1, markIndex3);
    }
}
