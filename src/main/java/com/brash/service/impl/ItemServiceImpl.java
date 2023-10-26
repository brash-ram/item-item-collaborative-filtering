package com.brash.service.impl;

import com.brash.data.entity.Item;
import com.brash.data.jpa.ItemRepository;
import com.brash.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    @Override
    public Item addItem(long originalId) {
        return itemRepository.save(new Item().setOriginalId(originalId));
    }
}
