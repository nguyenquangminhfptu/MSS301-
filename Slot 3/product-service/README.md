# Slot 3 — Product Service

Đã hoàn thành TODO 1–5: cập nhật sản phẩm theo ID, xoá sản phẩm và trả
HTTP 404 kèm thông báo khi không tìm thấy sản phẩm.

| Method | Endpoint | Kết quả thành công |
|---|---|---|
| POST | `/api/products` | 201, sản phẩm mới |
| GET | `/api/products` | 200, danh sách sản phẩm |
| PUT | `/api/products/{id}` | 200, sản phẩm đã cập nhật |
| DELETE | `/api/products/{id}` | 204, body rỗng |

PUT giữ nguyên ID và cập nhật name, description, price trên bản ghi cũ.
DELETE kiểm tra sự tồn tại trước khi xoá. PUT và DELETE trả 404 nếu ID
không tồn tại.

## Chạy ứng dụng

Cần Docker và JDK 25 theo cấu hình mặc định trong pom.xml.

```sh
docker compose up -d
./mvnw spring-boot:run
```

API tại `http://localhost:8080`, Mongo Express tại `http://localhost:8081`.
Hướng dẫn Postman nằm trong [Part1_ProductService_Guide_test.md](Part1_ProductService_Guide_test.md).

## Kiểm thử

Bật Docker rồi chạy:

```sh
./mvnw clean test
```

Testcontainers tự tạo MongoDB riêng cho kiểm thử.
Nếu máy dùng JDK 21, có thể ghi đè phiên bản biên dịch mà không sửa pom.xml.
Lệnh đã chạy trên macOS:

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw -Djava.version=21 clean test
```

Kết quả kiểm thử ngày 23/09/2026 trên JDK 21 và MongoDB 7.0.5:
12 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS.
Bộ ProductUpdateDeleteGradingTests vượt qua 6/6 tiêu chí, đạt 10/10 điểm.
