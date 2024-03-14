package com.brash.util;

import com.brash.data.entity.Mark;
import com.brash.dto.web.ItemsDTO;
import com.brash.dto.web.MarkDTO;

import java.util.List;

public class Mapper {

    public static MarkDTO map(Mark mark) {
        return new MarkDTO(mark.getUser().getId(), mark.getItem().getId(), mark.getMark());
    }

    public static List<MarkDTO> map(List<Mark> marks) {
        return marks.stream()
                .map(Mapper::map)
                .toList();
    }

    public static ItemsDTO mapToItemsDTO(List<Mark> marks) {
        return new ItemsDTO(marks.stream()
                .map(mark -> mark.getItem().getOriginalId())
                .toList());
    }
}
