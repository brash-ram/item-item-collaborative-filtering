package com.brash.filter;

import com.brash.data.entity.Item;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@ToString
public class PartSimilarItems {
     public List<Item> items;
     public Double similarValue;

     public Item getOtherItem(Item item) {
          if (!items.contains(item)) return null;
          if (items.get(0).equals(item)) {
               return items.get(1);
          } else {
               return items.get(0);
          }
     }
}
