package com.fudn.orderservice;

import com.fudn.orderservice.repository.OrderRepository;
import com.fudn.orderservice.stub.InventoryStubs;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
class OrderServiceApplicationTests {
    @Container
    @ServiceConnection
    static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.3.0");

    @LocalServerPort
    private Integer port;
    @Autowired private OrderRepository orderRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        orderRepository.deleteAll();
    }

    @Test
    void shouldSubmitOrder() {
        InventoryStubs.stubInventoryCall("iphone_15", 1, true);
        RestAssured.given()
                .contentType("application/json")
                .body("""
                    {"skuCode":"iphone_15","price":1000,"quantity":1}
                    """)
                .when().post("/api/order")
                .then().statusCode(201)
                .body(is("Order Placed Successfully"));

        var orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        var order = orders.get(0);
        assertThat(order.getId()).isPositive();
        assertThat(order.getSkuCode()).isEqualTo("iphone_15");
        assertThat(order.getPrice()).isEqualByComparingTo("1000.00");
        assertThat(order.getQuantity()).isEqualTo(1);
        assertThat(UUID.fromString(order.getOrderNumber()).toString()).isEqualTo(order.getOrderNumber());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void shouldGenerateNewIdentityForEveryOrder() {
        InventoryStubs.stubInventoryCall("iphone_15", 1, true);
        for (int i = 0; i < 2; i++) {
            RestAssured.given().contentType("application/json")
                    .body("""
                        {"id":999,"skuCode":"iphone_15","price":1000,"quantity":1}
                        """)
                    .when().post("/api/order").then().statusCode(201);
        }
        var orders = orderRepository.findAll();
        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(o -> o.getId()).doesNotContain(999L).doesNotHaveDuplicates();
        assertThat(orders).extracting(o -> o.getOrderNumber()).doesNotHaveDuplicates();
    }

    @Test
    void shouldRejectOrderWhenInventoryIsInsufficient() {
        InventoryStubs.stubInventoryCall("iphone_15", 101, false);

        RestAssured.given()
                .contentType("application/json")
                .body("""
                    {"skuCode":"iphone_15","price":1000,"quantity":101}
                    """)
                .when().post("/api/order")
                .then().statusCode(500);

        assertThat(orderRepository.count()).isZero();
    }
}
