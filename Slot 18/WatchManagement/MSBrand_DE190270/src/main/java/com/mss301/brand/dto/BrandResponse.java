package com.mss301.brand.dto;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BrandResponse {private Integer brandId; private String brandName; private String countryOfOrigin; private long watchCount;}
