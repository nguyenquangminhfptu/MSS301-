# Kết quả kiểm thử Inventory Service

Ngày 29/09/2026, chạy bằng **Newman 6.2.1** (CLI của Postman), không thao tác trực tiếp trên giao diện Postman. Base URL `http://localhost:8082`.

**10 request, 16 assertion PASS, không có lỗi.**

| # | Test case | HTTP thực tế | Body | Kết quả |
|---|---|---:|---|---|
| 1 | Check Stock - In Stock | 200 | true | PASS |
| 2 | Check Stock - Insufficient Stock | 200 | false | PASS |
| 3 | Check Stock - Boundary Equal | 200 | true | PASS |
| 4 | Check Stock - Boundary Plus One | 200 | false | PASS |
| 5 | Check Stock - Unknown SKU | 200 | false | PASS |
| 6 | Check Stock - Missing Quantity | 400 | JSON lỗi | PASS |
| 7 | Check Stock - Missing SKU | 400 | JSON lỗi | PASS |
| 8 | Check Stock - Invalid Quantity Type | 400 | JSON lỗi | PASS |
| 9 | Check Stock - Negative Quantity - Current Behavior | 200 | true | PASS |
| 10 | Check Stock - Wrong Method | 405 | JSON lỗi | PASS |

Boundary: pixel_8 quantity=100 trả true, quantity=101 trả false, xác nhận điều kiện `>=` đúng. SKU không tồn tại trả false, không phải 404. Thiếu tham số hoặc quantity=abc trả 400; POST trả 405.

**Case 9:** quantity=-5 vẫn trả 200/true vì chưa có validation số dương. Đây là kết quả đúng với code Part 1, đồng thời là điểm cần cải tiến. Chưa thay đổi logic nghiệp vụ. Có thể thêm `@Min(1)` trong bước nâng cấp rồi đổi kỳ vọng sang 400.

## Database

Đã so sánh toàn bộ các dòng trước và sau khi chạy: **không thay đổi**. Bảng `t_inventory` vẫn có 4 SKU, mỗi SKU quantity=100:

| sku_code | quantity |
|---|---:|
| iphone_15 | 100 |
| pixel_8 | 100 |
| galaxy_24 | 100 |
| oneplus_12 | 100 |

Chi tiết: [database-after-tests.tsv](database-after-tests.tsv). Response và assertion từng case: [test-results.json](test-results.json).

## Import và chạy lại

1. Import `inventory-service.postman_collection.json` và `inventory-service-local.postman_environment.json`.
2. Chọn environment **inventory-service-local**, biến `inventory_base_url=http://localhost:8082`.
3. Chọn collection **Inventory Service** → **Run collection**, chạy đủ 10 request.

Hoặc tại thư mục `Slot 5/inventory-service`:

```sh
npx --yes newman@6.2.1 run postman/inventory-service.postman_collection.json \
  -e postman/inventory-service-local.postman_environment.json
```
