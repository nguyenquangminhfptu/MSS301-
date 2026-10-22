package com.mss301.order.dto;

import java.math.BigDecimal;

public record PaymentRequest(Long orderId, String customerId, BigDecimal amount) {
}
