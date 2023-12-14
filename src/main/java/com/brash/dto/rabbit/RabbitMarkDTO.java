package com.brash.dto.rabbit;

public record RabbitMarkDTO (
        long userId,
        long itemId,
        double mark
) {
}
