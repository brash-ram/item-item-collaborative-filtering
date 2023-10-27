package com.brash.filter;

import com.brash.data.entity.Item;

import java.util.List;

public interface ItemToItemSimilarity {

    List<PartSimilarItems> updateSimilarity(List<Item> items);
}
