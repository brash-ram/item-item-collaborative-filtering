package com.brash.controller;

import com.brash.data.entity.Mark;
import com.brash.dto.web.ItemsDTO;
import com.brash.service.ItemService;
import com.brash.service.MarkService;
import com.brash.util.Mapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/filter")
public class FilterController {

//    private final UserService userService;
    private final ItemService itemService;
    private final MarkService markService;

    @Operation(
            summary = "Получить все элементы по убыванию рейтинга оценки для пользователя"
    )
    @GetMapping("/all")
    public ResponseEntity<ItemsDTO> getAll(@NotNull @RequestParam long userId,
                                                 @NotNull @RequestParam int offset,
                                                 @NotNull @RequestParam int limit) {
        List<Mark> marks = markService.getMarks(userId, offset, limit);
        return ResponseEntity.ok(Mapper.mapToItemsDTO(marks));
    }

    @Operation(
            summary = "Получить элементы с сгенерированными оценками по убыванию рейтинга для пользователя"
    )
    @GetMapping("/generated")
    public ResponseEntity<ItemsDTO> getAllGeneratedMarks(@NotNull @RequestParam long userId,
                                                              @NotNull @RequestParam int offset,
                                                              @NotNull @RequestParam int limit) {
        List<Mark> marks = markService.getGeneratedMarks(userId, offset, limit);
        return ResponseEntity.ok(Mapper.mapToItemsDTO(marks));
    }

    @Operation(
            summary = "Получить схожие элементы с переданным по убыванию рейтинга оценки сходства"
    )
    @GetMapping("/item")
    public ResponseEntity<ItemsDTO> getSimilarityItems(@NotNull @RequestParam long itemId,
                                                            @NotNull @RequestParam int offset,
                                                            @NotNull @RequestParam int limit) {
        ItemsDTO items = itemService.getSimilarity(itemId, offset, limit);
        return ResponseEntity.ok(items);
    }
}
