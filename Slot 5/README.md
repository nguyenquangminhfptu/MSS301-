# MSS301 — Order Service và Inventory Service

| Service | Port | Database | Package |
|---|---:|---|---|
| order-service | 8081 | order_service | com.fudn.orderservice |
| inventory-service | 8082 | inventory_service | com.fudn.inventoryservice |

Hai service dùng chung **một container MySQL 8.3.0**, cổng host `3306`, nhưng dùng database riêng. User mặc định `root`, password `mysql` theo bài thực hành. Product-service hiện có chạy riêng.

## Chạy toàn bộ

Mở Docker Desktop và chạy tại `Slot 5`:

```sh
docker compose up -d --build
docker compose logs -f order-service inventory-service
```

Hoặc chỉ chạy database, rồi chạy mỗi ứng dụng bằng Maven/IDE:

```sh
docker compose up -d mysql
```

Trong hai terminal riêng:

```sh
cd order-service
./mvnw spring-boot:run
```

```sh
cd inventory-service
./mvnw spring-boot:run
```

File Compose tại `order-service` cũng chạy được độc lập bằng `docker compose up -d mysql`. Hai file dùng chung project name và cùng cấu hình MySQL; chọn một vị trí chạy lệnh nhất quán. Init SQL tại `order-service/mysql/init.sql` tạo cả hai database khi thư mục dữ liệu còn trống.

## Order Service theo đề

```sh
curl -i -X POST http://localhost:8081/api/order \
  -H 'Content-Type: application/json' \
  -d '{"skuCode":"iphone_15","price":1000,"quantity":1}'
```

Trả **201**, text `Order Placed Successfully`; lưu vào `t_orders`. Theo bài thực hành, bước này tạo đơn độc lập và chưa gọi kiểm tra inventory.

Xem [hướng dẫn Order Service](order-service/README.md) để cấu hình IntelliJ, import Postman, kiểm tra bảng MySQL và chạy integration test Testcontainers + RestAssured.

## Inventory Service

```sh
curl 'http://localhost:8082/api/inventory?skuCode=iphone_15&quantity=100'
# true
curl 'http://localhost:8082/api/inventory?skuCode=iphone_15&quantity=200'
# false
```

Flyway V1 tạo `t_inventory`; V2 thêm 4 SKU với quantity=100. API chỉ kiểm tra tồn và trả boolean. Xem [hướng dẫn Inventory Service](inventory-service/README.md).

## Kiểm thử

Từ `Slot 5`:

```sh
mvn -f order-service/pom.xml clean verify
mvn -f inventory-service/pom.xml clean verify
```

Cả hai service kiểm thử với MySQL thật do Testcontainers quản lý, yêu cầu Docker đang chạy.
