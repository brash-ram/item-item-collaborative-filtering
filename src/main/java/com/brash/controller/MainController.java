package com.brash.controller;

import com.brash.data.entity.Mark;
import com.brash.dto.ItemDTO;
import com.brash.dto.AddMarkDTO;
import com.brash.dto.MarkDTO;
import com.brash.dto.UserDTO;
import com.brash.exception.NoAvailableMarkException;
import com.brash.filter.Filter;
import com.brash.service.ItemService;
import com.brash.service.MarkService;
import com.brash.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/filter")
public class MainController {
    private final UserService userService;
    private final ItemService itemService;
    private final MarkService markService;

    @PostMapping("/add/user")
    public ResponseEntity<UserDTO> addUser(@NotNull @Valid @RequestBody UserDTO userDTO) {
        userService.addUser(userDTO.id());
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("/add/item")
    public ResponseEntity<ItemDTO> addItem(@NotNull @Valid @RequestBody ItemDTO itemDTO) {
        itemService.addItem(itemDTO.id());
        return ResponseEntity.ok(itemDTO);
    }

    @PostMapping("/add/mark")
    public ResponseEntity<AddMarkDTO> addMark(@NotNull @Valid @RequestBody AddMarkDTO addMarkDTO) {
        markService.addMark(addMarkDTO.mark(), addMarkDTO.userId(), addMarkDTO.itemId());
        return ResponseEntity.ok(addMarkDTO);
    }

    @GetMapping("/get/mark")
    public ResponseEntity<MarkDTO> getMark(@NotNull @Valid @RequestParam Long userId,
                                              @NotNull @Valid @RequestParam Long itemId)
            throws NoAvailableMarkException {
        Mark mark = markService.getMark(userId, itemId);
        return ResponseEntity.ok(new MarkDTO(userId, itemId, mark.getMark()));
    }

    @GetMapping("/get/mark/all")
    public ResponseEntity<List<MarkDTO>> getAllGeneratedMarks(@NotNull @Valid @RequestParam Long userId)
            throws NoAvailableMarkException {
        List<MarkDTO> marks = markService.getGeneratedMarksDto(userId);
        return ResponseEntity.ok(marks);
    }
}
