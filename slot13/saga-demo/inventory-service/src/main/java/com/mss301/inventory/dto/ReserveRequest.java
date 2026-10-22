package com.mss301.inventory.dto;

import jakarta.validation.constraints.*;

public record ReserveRequest(@NotNull @Positive Long orderId, @NotBlank String productId, @Positive int quantity) {
}
