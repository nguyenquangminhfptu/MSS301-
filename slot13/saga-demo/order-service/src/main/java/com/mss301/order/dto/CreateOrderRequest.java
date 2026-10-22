package com.mss301.order.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record CreateOrderRequest(@NotBlank String customerId, @NotBlank String productId, @Positive int quantity, @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {
}
