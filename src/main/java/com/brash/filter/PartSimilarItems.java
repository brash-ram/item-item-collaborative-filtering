package com.brash.filter;

import com.brash.data.entity.Item;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class PartSimilarItems {
     public List<Item> items;
     public Double similarValue;
}
