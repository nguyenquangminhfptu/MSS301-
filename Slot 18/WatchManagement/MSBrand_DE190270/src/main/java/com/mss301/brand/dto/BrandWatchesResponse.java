package com.mss301.brand.dto;
import java.util.List;
public record BrandWatchesResponse(Integer brandId,String brandName,long watchCount,List<Integer> watchIds) {}
