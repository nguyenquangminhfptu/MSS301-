package com.mss301.inventory;

import com.mss301.inventory.dto.ReserveRequest;
import com.mss301.inventory.entity.*;
import com.mss301.inventory.repository.*;
import com.mss301.inventory.service.InventoryService;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.mongodb.uri=mongodb://localhost:27017/inventory_test?directConnection=true")
@EnabledIfEnvironmentVariable(named = "SAGA_INTEGRATION", matches = "true")
class InventoryTransactionTest {
    @Autowired InventoryService service;
    @Autowired ProductRepository products;
    @Autowired ReservationRepository reservations;
    @Autowired PlatformTransactionManager tx;

    @BeforeEach void setup() {
        reservations.deleteAll(); products.deleteAll();
        products.save(new Product("IT", "Test", BigDecimal.TEN, 5));
    }
    @Test void reserveAndReleaseAreIdempotent() {
        var request = new ReserveRequest(10001L, "IT", 2);
        service.reserve(request); service.reserve(request);
        assertThat(products.findById("IT").orElseThrow().getStock()).isEqualTo(3);
        service.release(10001L); service.release(10001L); service.release(99999L);
        assertThat(products.findById("IT").orElseThrow().getStock()).isEqualTo(5);
        assertThat(service.reserve(request).getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(products.findById("IT").orElseThrow().getStock()).isEqualTo(5);
    }
    @Test void bothDocumentsRollBackOnLocalFailure() {
        assertThatThrownBy(() -> new TransactionTemplate(tx).execute(status -> {
            service.reserve(new ReserveRequest(10002L, "IT", 2));
            throw new IllegalStateException("failure after writes");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(products.findById("IT").orElseThrow().getStock()).isEqualTo(5);
        assertThat(reservations.findById(10002L)).isEmpty();
    }
    @Test void insufficientStockAndConflictingReplayDoNotMutateStock() {
        assertThatThrownBy(() -> service.reserve(new ReserveRequest(10003L, "IT", 6)))
            .hasMessageContaining("409");
        service.reserve(new ReserveRequest(10003L, "IT", 1));
        assertThatThrownBy(() -> service.reserve(new ReserveRequest(10003L, "IT", 2)))
            .hasMessageContaining("409");
        assertThat(products.findById("IT").orElseThrow().getStock()).isEqualTo(4);
    }
}
