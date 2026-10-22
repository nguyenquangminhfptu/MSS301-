package com.mss301.payment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import java.math.BigDecimal;
import jakarta.persistence.Column;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Wallet {
    @Id
    private String customerId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;
}
