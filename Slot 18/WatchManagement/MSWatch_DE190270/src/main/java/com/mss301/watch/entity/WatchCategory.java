package com.mss301.watch.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="WatchCategories") @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WatchCategory { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="CategoryID") private Integer categoryId; @Column(name="CategoryName",nullable=false,length=100) private String categoryName; @Column(name="Description",length=500) private String description; @Column(name="MovementType",length=100) private String movementType; @Column(name="PriceRange",length=100) private String priceRange; }
