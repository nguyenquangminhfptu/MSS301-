package com.fudn.inventoryservice;

import com.fudn.inventoryservice.repository.InventoryRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryServiceApplicationTests {
    @Container
    @ServiceConnection
    static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.3.0");

    @LocalServerPort private Integer port;
    @Autowired private InventoryRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    void shouldReadInventory() {
        for (int quantity : new int[]{1, 100}) {
            RestAssured.given().queryParam("skuCode", "iphone_15").queryParam("quantity", quantity)
                    .when().get("/api/inventory").then().statusCode(200).body(is("true"));
        }
        for (int quantity : new int[]{200, 1000}) {
            RestAssured.given().queryParam("skuCode", "iphone_15").queryParam("quantity", quantity)
                    .when().get("/api/inventory").then().statusCode(200).body(is("false"));
        }
        assertThat(repository.findAll()).allSatisfy(item -> assertThat(item.getQuantity()).isEqualTo(100));
    }

    @Test
    void shouldReturnFalseForUnknownSku() {
        RestAssured.given().queryParam("skuCode", "missing").queryParam("quantity", 1)
                .when().get("/api/inventory").then().statusCode(200).body(is("false"));
    }

    @Test
    void shouldRejectMissingOrMalformedQuantity() {
        RestAssured.given().queryParam("skuCode", "iphone_15")
                .when().get("/api/inventory").then().statusCode(400);
        RestAssured.given().queryParam("skuCode", "iphone_15").queryParam("quantity", "abc")
                .when().get("/api/inventory").then().statusCode(400);
    }

    @Test
    void shouldApplyBothMigrationsAndSeedFourSkus() {
        assertThat(repository.findAll()).extracting(item -> item.getSkuCode())
                .containsExactlyInAnyOrder("iphone_15", "pixel_8", "galaxy_24", "oneplus_12");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class)).isEqualTo(2);
    }
}
