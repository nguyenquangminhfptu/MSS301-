package com.mss301.inventory.entity;

import lombok.*;
import org.springframework.data.annotation.Version;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("product")
@Getter @Setter @NoArgsConstructor 
public class Product {
    @Id
    private String id;
    private String name;
    private BigDecimal price;
    private int stock;
    public Product(String id, String name, BigDecimal price, int stock) {
        this.id = id; this.name = name; this.price = price; this.stock = stock;
    }
    @Version private Long version;
}
