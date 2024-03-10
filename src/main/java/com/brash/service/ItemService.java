package com.brash.service;

import com.brash.data.entity.Item;
import com.brash.dto.web.ItemDTO;
import com.brash.dto.web.ItemsDTO;

import java.util.List;

public interface ItemService {

    Item addItem(long originalId);

    ItemsDTO getSimilarity(long itemId, int offset, int limit);
}
