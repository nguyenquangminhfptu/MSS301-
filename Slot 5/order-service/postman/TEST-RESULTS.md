# Kết quả kiểm thử Order Service

Ngày kiểm thử: 29/09/2026. Base URL: `http://localhost:8081`. Chạy collection bằng **Newman 6.2.1** (CLI của Postman), không thao tác trực tiếp trên giao diện Postman.

## Chuẩn bị đã xác nhận

- Container `mss301-shared-mysql-1` dùng MySQL 8.3.0, trạng thái healthy; service Compose tên `mysql`.
- Database `order_service` và bảng `t_orders` tồn tại; Order Service đang chạy cổng 8081.
- Flyway V1 (`init`) có `success=1`.
- Trước khi chạy có **1 đơn cũ**, ID 1. Giữ nguyên dữ liệu này, không xóa bảng để test.

## Kết quả

| # | Trường hợp | Kỳ vọng | Thực tế | Kết quả |
|---|---|---:|---:|---|
| 1 | Đặt hàng hợp lệ | 201 | 201 | PASS |
| 2 | Giá 899.99, số lượng 5 | 201 | 201 | PASS |
| 3 | Thiếu skuCode | 201 | 201 | PASS — ghi nhận chưa validation |
| 4 | quantity = "abc" | 400 | 400 | PASS |
| 5 | Thiếu Content-Type | 415 | 415 | PASS |
| 6 | Body JSON {} | 201 | 201 | PASS — ghi nhận chưa validation |
| 7 | GET /api/order | 405 | 405 | PASS |
| 8 | POST /api/orders | 404 | 404 | PASS |

Các response 201 đều có raw text `Order Placed Successfully`, Content-Type `text/plain;charset=UTF-8`.

Lần chạy đầu: 8 request, 16 assertion pass. Newman tự thêm `text/plain` trong case 5. Sau đó đã chỉnh `protocolProfileBehavior.disabledSystemHeaders` để tắt header tự động, chạy lại riêng case 5: 2 assertion pass, xác nhận **không có Content-Type hoạt động**, response vẫn 415. Kết quả từng case mới nhất lưu trong [test-results.json](test-results.json).

Case 4 trả JSON lỗi gồm `timestamp`, `status:400`, `error:"Bad Request"`, `path:"/api/order"`. Response mặc định không công khai chi tiết lỗi Jackson. Case 6 gửi đối tượng JSON `{}`, không phải request không có body.

## Đối chiếu MySQL

Sau test có **5 đơn**, tăng đúng **4 dòng**. Các request 400/415/405/404 không tạo thêm dòng. Mỗi dòng có order_number hợp lệ dạng UUID.

| ID | Case | sku_code | price | quantity |
|---|---|---|---:|---:|
| 1 | Dữ liệu cũ giữ nguyên | iphone_15 | 1000.00 | 1 |
| 2 | 1 | iphone_15 | 1000.00 | 1 |
| 3 | 2 | pixel_8 | 899.99 | 5 |
| 4 | 3 | NULL | 1000.00 | 1 |
| 5 | 6 | NULL | NULL | NULL |

Chi tiết ID, UUID và Flyway: [database-after-tests.tsv](database-after-tests.tsv). Giá `899.99` được lưu đúng decimal, không bị làm tròn thành số nguyên.

## Chạy lại trong Postman

1. Import `order-service.postman_collection.json` và `order-service-local.postman_environment.json`.
2. Chọn environment **order-service-local**, biến `order_base_url=http://localhost:8081`.
3. Mở collection **Order Service**, chọn **Run collection** và chạy đủ 8 request.
4. Kiểm tra Test Results. Với case 5, giữ thiết lập tắt Content-Type tự động; đừng đổi body sang raw JSON rồi bật lại header.

Chạy từ thư mục `Slot 5/order-service` bằng CLI:

```sh
npx --yes newman@6.2.1 run postman/order-service.postman_collection.json \
  -e postman/order-service-local.postman_environment.json
```

Mỗi lần chạy toàn bộ sẽ tạo thêm 4 đơn, bao gồm 2 đơn thiếu dữ liệu theo hành vi Part 1. Không có bước tự xóa dữ liệu.

## Ghi nhận validation

Case 3 và 6 chứng minh API hiện nhận dữ liệu thiếu. Chưa đổi code nghiệp vụ để giữ đúng bài Part 1. Khi nâng cấp, có thể thêm `@Valid`, `@NotBlank` cho skuCode, `@NotNull` và ràng buộc số dương cho price/quantity; lúc đó đổi kỳ vọng hai case này sang 400 và kiểm tra không lưu dữ liệu.
