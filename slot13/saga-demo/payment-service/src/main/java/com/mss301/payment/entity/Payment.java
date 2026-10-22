package com.mss301.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import jakarta.persistence.Column;

@Entity
@Getter @Setter @NoArgsConstructor
public class Payment {
    @Id
    private Long orderId;
    private String customerId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
}
