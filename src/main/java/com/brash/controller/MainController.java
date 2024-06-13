package com.brash.controller;

import com.brash.dto.web.AddMarkDTO;
import com.brash.dto.web.ItemDTO;
import com.brash.dto.web.UserDTO;
import com.brash.exception.ItemNotFound;
import com.brash.exception.NoAvailableMarkException;
import com.brash.exception.UserNotFound;
import com.brash.service.ItemService;
import com.brash.service.MarkService;
import com.brash.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/general")
public class MainController {
    private final UserService userService;
    private final ItemService itemService;
    private final MarkService markService;

    @Operation(
        summary = "Добавить пользователя"
    )
    @PostMapping("/add/user")
    public ResponseEntity<UserDTO> addUser(@NotNull @Valid @RequestBody UserDTO userDTO) {
        userService.addUser(userDTO.id());
        return ResponseEntity.ok(userDTO);
    }

    @Operation(
            summary = "Добавить элемент"
    )
    @PostMapping("/add/item")
    public ResponseEntity<ItemDTO> addItem(@NotNull @Valid @RequestBody ItemDTO itemDTO) {
        itemService.addItem(itemDTO.id());
        return ResponseEntity.ok(itemDTO);
    }

    @Operation(
            summary = "Добавить оценку"
    )
    @PostMapping("/add/mark")
    public ResponseEntity<AddMarkDTO> addMark(@NotNull @Valid @RequestBody AddMarkDTO addMarkDTO) throws UserNotFound, ItemNotFound, NoAvailableMarkException {
        markService.addMark(addMarkDTO.mark(), addMarkDTO.userId(), addMarkDTO.itemId());
        return ResponseEntity.ok(addMarkDTO);
    }

//    @Operation(
//            summary = "Получить оценку"
//    )
//    @GetMapping("/mark")
//    public ResponseEntity<MarkDTO> getMark(@NotNull @RequestParam long userId,
//                                           @NotNull @RequestParam long itemId) throws NoAvailableMarkException {
//        Mark mark = markService.getMark(userId, itemId);
//        return ResponseEntity.ok(new MarkDTO(userId, itemId, mark.getMark()));
//    }

}
