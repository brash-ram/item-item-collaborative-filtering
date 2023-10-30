package com.brash.filter;

import com.brash.data.entity.Item;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * Пара схожих элементов и их оценка схожести
 */
@AllArgsConstructor
@ToString
public class PartSimilarItems {
     /**
      * В списке 2 элемента, которые составляют пару сходства
      */
     public List<Item> items;

     /**
      * Значение сходства элементов
      */
     public Double similarValue;

     /**
      * Получить другой элемент из списка, в отличие от данного
      * @param item элемент из списка
      * @return отличный элемент от переданного элемента
      */
     public Item getOtherItem(Item item) {
          if (!items.contains(item)) return null;
          if (items.get(0).equals(item)) {
               return items.get(1);
          } else {
               return items.get(0);
          }
     }
}
