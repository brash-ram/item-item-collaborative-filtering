package com.brash.service.impl;

import com.brash.data.entity.Mark;
import com.brash.data.jpa.ItemRepository;
import com.brash.data.jpa.MarkRepository;
import com.brash.data.jpa.UserRepository;
import com.brash.service.MarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarkServiceImpl implements MarkService {

    private final MarkRepository markRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    public Mark addMark(int mark, long userId, long itemId) {
        return markRepository.save(
                new Mark()
                        .setItem(itemRepository.findByOriginalId(itemId))
                        .setUser(userRepository.findByOriginalId(userId))
        );
    }

    @Override
    public List<Mark> getAllMarks() {
        return null;
    }
}
