package com.mss301.payment.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record PaymentRequest(@NotNull @Positive Long orderId, @NotBlank String customerId, @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {
}
