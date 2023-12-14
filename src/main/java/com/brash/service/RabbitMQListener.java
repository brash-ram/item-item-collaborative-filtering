package com.brash.service;

import com.brash.dto.rabbit.RabbitItemDTO;
import com.brash.dto.rabbit.RabbitMarkDTO;
import com.brash.dto.rabbit.RabbitUserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@RabbitListener(queues = "${rabbit.queue}")
@Component
@RequiredArgsConstructor
public class RabbitMQListener {

    private final UserService userService;
    private final ItemService itemService;
    private final MarkService markService;

    @RabbitHandler
    public void addNewUser(RabbitUserDTO dto) {
        userService.addUser(dto.id());
    }


    @RabbitHandler
    public void addNewItem(RabbitItemDTO dto) {
        itemService.addItem(dto.id());
    }

    @RabbitHandler
    public void addNewMark(RabbitMarkDTO dto) {
        markService.addMark(dto.mark(), dto.userId(), dto.itemId());
    }
}
