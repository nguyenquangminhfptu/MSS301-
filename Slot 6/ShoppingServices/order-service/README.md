# Order Service

Triển khai theo bài thực hành: Java 21, Maven, Spring Boot, Lombok, Spring Data JPA, MySQL và Flyway. Package `com.fudn.orderservice`; cổng `8081`.

## 1. Khởi động MySQL

Mở Docker Desktop, chạy tại thư mục `Slot 5/order-service`:

```sh
docker compose up -d mysql
docker compose ps
```

Một container MySQL 8.3.0 phục vụ cả hai database `order_service` và `inventory_service`. File `mysql/init.sql` tạo database lần đầu; dữ liệu được giữ ở `docker/mysql/data` (đã loại khỏi Git và Docker build context). Compose không dùng trường `version` đã lỗi thời.

Kết nối IntelliJ / MySQL client:

| Thuộc tính | Giá trị |
|---|---|
| Host | localhost |
| Port | 3306 |
| User | root |
| Password | mysql |
| Database | order_service |
| URL | jdbc:mysql://localhost:3306/order_service |

Nếu thay `MYSQL_ROOT_PASSWORD` khi khởi tạo container, đặt `DB_PASSWORD` tương ứng khi chạy ứng dụng. Thay biến môi trường không đổi mật khẩu của database đã tồn tại.

## 2. Chạy ứng dụng

```sh
./mvnw spring-boot:run
```

`application.properties` dùng datasource trên, `spring.jpa.hibernate.ddl-auto=none`, cổng `8081`. Flyway chạy `V1__init.sql` để tạo bảng `t_orders`; log lần đầu có `Successfully applied 1 migration`. Có thể ghi đè `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT` bằng biến môi trường.

## 3. Thử API

Import hai file `postman/order-service.postman_collection.json` và `postman/order-service-local.postman_environment.json` vào Postman. Chọn environment **order-service-local**, mở collection **Order Service**, rồi chạy **01 Create Order - Success** hoặc chọn **Run collection** để chạy đủ 8 trường hợp. Xem [hướng dẫn và kết quả Postman](postman/TEST-RESULTS.md). Hoặc dùng curl:

```sh
curl -i -X POST http://localhost:8081/api/order \
  -H 'Content-Type: application/json' \
  -d '{"skuCode":"iphone_15","price":1000,"quantity":1}'
```

Kết quả: HTTP **201 Created**, body text:

```text
Order Placed Successfully
```

Xem dữ liệu thực trong MySQL (nhập mật khẩu `mysql`):

```sh
docker compose exec mysql mysql -uroot -p order_service \
  -e 'SELECT * FROM t_orders; SELECT version, description, success FROM flyway_schema_history;'
```

`OrderRequest` gồm `id`, `skuCode`, `price`, `quantity`; `id` trong request không dùng khi tạo mới. Database tự tăng ID; service sinh `orderNumber` bằng UUID. Mỗi request lưu một hàng vào `t_orders`, không gọi inventory-service ở bước bài tập này.

## 4. Integration test

Docker Desktop phải chạy. Testcontainers tự tạo MySQL **riêng** cho kiểm thử, không dùng dữ liệu container Compose.

```sh
./mvnw test
./mvnw clean verify
```

`OrderServiceApplicationTests` dùng `@ServiceConnection`, Testcontainers MySQL 8.3.0, server cổng ngẫu nhiên và RestAssured. Test kiểm tra HTTP 201, message, dữ liệu lưu thật, Flyway và ID/UUID của các đơn. Không tự bỏ qua test khi thiếu Docker.

## Cấu trúc

```text
src/main/java/com/fudn/orderservice/
  OrderServiceApplication.java
  controller/OrderController.java
  dto/OrderRequest.java
  model/Order.java
  repository/OrderRepository.java
  service/OrderService.java
src/main/resources/
  application.properties
  db/migration/V1__init.sql
src/test/java/com/fudn/orderservice/OrderServiceApplicationTests.java
mysql/init.sql
docker-compose.yml
postman/order-service.postman_collection.json
```

Đây là API theo mẫu bài tập: chưa bổ sung validation nghiệp vụ, xác thực, giữ/trừ tồn kho hoặc thanh toán. Giá được lấy từ request.

Bản trước trong cuộc hội thoại dùng `/api/orders`, DTO `items` và bảng `orders/order_items`; bản này thay bằng contract của đề. Compose mới dùng cổng 3306 và thư mục dữ liệu mới, không xóa các volume của bản trước. Không trỏ bản này vào schema Flyway cũ vì migration V1 đã thay đổi; dùng database trống của Compose mới.

Nếu Docker Desktop trên macOS dùng socket `~/.docker/run/docker.sock` và Ryuk báo lỗi mount socket, chạy:

```sh
DOCKER_HOST="unix://$HOME/.docker/run/docker.sock" \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
./mvnw clean verify
```
