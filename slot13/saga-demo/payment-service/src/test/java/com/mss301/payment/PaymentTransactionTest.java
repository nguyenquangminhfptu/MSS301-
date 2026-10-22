package com.mss301.payment;

import com.mss301.payment.dto.PaymentRequest;
import com.mss301.payment.entity.*;
import com.mss301.payment.repository.*;
import com.mss301.payment.service.PaymentService;
import java.math.BigDecimal;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "SAGA_INTEGRATION", matches = "true")
class PaymentTransactionTest {
    @Autowired PaymentService service;
    @Autowired WalletRepository wallets;
    @Autowired PaymentRepository payments;
    @Autowired PlatformTransactionManager tx;
    @BeforeEach void setup() {
        payments.deleteById(9000001L);
        wallets.save(new Wallet("IT", new BigDecimal("100.00")));
    }
    @AfterEach void cleanup() { payments.deleteById(9000001L); wallets.deleteById("IT"); }
    PaymentRequest request() { return new PaymentRequest(9000001L, "IT", new BigDecimal("20.00")); }
    @Test void payAndRefundAreIdempotent() {
        service.pay(request()); service.pay(request());
        assertThat(wallets.findById("IT").orElseThrow().getBalance()).isEqualByComparingTo("80.00");
        service.refund(9000001L); service.refund(9000001L); service.refund(9999999L);
        assertThat(service.pay(request()).getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(wallets.findById("IT").orElseThrow().getBalance()).isEqualByComparingTo("100.00");
    }
    @Test void walletAndPaymentRollBackTogether() {
        assertThatThrownBy(() -> new TransactionTemplate(tx).execute(status -> {
            service.pay(request()); throw new IllegalStateException("failure after writes");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(wallets.findById("IT").orElseThrow().getBalance()).isEqualByComparingTo("100.00");
        assertThat(payments.findById(9000001L)).isEmpty();
    }
    @Test void insufficientBalanceDoesNotCreatePayment() {
        assertThatThrownBy(() -> service.pay(new PaymentRequest(9000001L, "IT", new BigDecimal("1000"))))
            .hasMessageContaining("402").hasMessageContaining("Số dư không đủ");
        assertThat(payments.findById(9000001L)).isEmpty();
    }
    @Test void concurrentDuplicatePayChargesOnce() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> service.pay(request()));
            var b = pool.submit(() -> service.pay(request()));
            a.get(15, TimeUnit.SECONDS); b.get(15, TimeUnit.SECONDS);
        }
        assertThat(wallets.findById("IT").orElseThrow().getBalance()).isEqualByComparingTo("80.00");
    }
}
