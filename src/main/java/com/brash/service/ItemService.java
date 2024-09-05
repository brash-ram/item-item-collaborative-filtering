package com.brash.service;

import com.brash.data.entity.Item;
import com.brash.dto.web.ItemsDTO;
import com.brash.exception.ItemNotFound;

public interface ItemService {

    Item addItem(long originalId);

    ItemsDTO getSimilarity(long itemId, int offset, int limit) throws ItemNotFound;

    void remove(long itemId);
}
