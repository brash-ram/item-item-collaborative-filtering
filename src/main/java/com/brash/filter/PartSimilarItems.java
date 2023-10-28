package com.brash.filter;

import com.brash.data.entity.Item;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class PartSimilarItems {
     public List<Item> items;
     public Double similarValue;

     public boolean hasItems(Item i1, Item i2) {
          return !i1.equals(i2) && items.contains(i1) && items.contains(i2);
     }

     public Item getOtherItem(Item item) {
          if (!items.contains(item)) return null;
          if (items.get(0).equals(item)) {
               return items.get(1);
          } else {
               return items.get(0);
          }
     }
}
