# Inventory Service

Java 21, Spring Boot, Maven, Lombok, JPA, MySQL và Flyway. Package `com.fudn.inventoryservice`, port **8082**. Dùng chung container MySQL với Order Service, database riêng `inventory_service`.

## Chạy

Tại `Slot 5`:

```sh
docker compose up -d mysql
cd inventory-service
./mvnw spring-boot:run
```

Datasource mặc định: `jdbc:mysql://localhost:3306/inventory_service`, user `root`, password `mysql`. Có thể ghi đè `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`. Hibernate `ddl-auto=none`; Flyway quản lý schema.

`../order-service/mysql/init.sql` tạo cả hai database. Nếu MySQL đã có volume từ trước nhưng thiếu database, chạy tại `Slot 5`:

```sh
docker compose exec mysql mysql -uroot -p \
  -e 'CREATE DATABASE IF NOT EXISTS inventory_service;'
```

Nhập mật khẩu `mysql`. Không cần xóa dữ liệu Order Service.

## Migration và API

- `V1__init.sql`: tạo bảng `t_inventory`.
- `V2__add_inventory.sql`: thêm `iphone_15`, `pixel_8`, `galaxy_24`, `oneplus_12`, mỗi SKU 100 sản phẩm.
- Database trống chạy lần đầu sẽ có log `Successfully applied 2 migrations ... now at version v2`.

```sh
curl 'http://localhost:8082/api/inventory?skuCode=iphone_15&quantity=100'
# true
curl 'http://localhost:8082/api/inventory?skuCode=iphone_15&quantity=200'
# false
```

Response HTTP 200, boolean JSON. SKU không tồn tại trả false. Cả hai query parameter đều bắt buộc; quantity thiếu hoặc không phải số trả 400. Theo mẫu bài tập, chưa thêm validation quantity dương. API chỉ đọc, không trừ/giữ tồn; Order Service vẫn chạy độc lập.

## Postman

Import hai file trong `postman/`, chọn environment **inventory-service-local**, chạy collection **Inventory Service** gồm 10 trường hợp. Xem [kết quả kiểm thử](postman/TEST-RESULTS.md). Hoặc tại thư mục inventory-service:

```sh
npx --yes newman@6.2.1 run postman/inventory-service.postman_collection.json \
  -e postman/inventory-service-local.postman_environment.json
```

## Integration test

Docker Desktop phải chạy. Test dùng Testcontainers MySQL 8.3.0 riêng, `@ServiceConnection`, RestAssured và server cổng ngẫu nhiên:

```sh
./mvnw clean verify
```

Kiểm tra đủ hàng, vượt tồn, SKU không tồn tại, tham số sai, hai migration và 4 SKU mẫu. Không tự bỏ qua test khi Docker không chạy.

Nếu Docker Desktop macOS báo lỗi mount socket của Ryuk:

```sh
DOCKER_HOST="unix://$HOME/.docker/run/docker.sock" \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
./mvnw clean verify
```

Bản trước của project dùng bảng `inventory` và migration V1 khác. Không dùng migration mới trực tiếp trên lịch sử V1 cũ vì checksum khác. Trong lần triển khai này, đã xác nhận bảng inventory cũ rỗng và tạo lại riêng database inventory_service; dữ liệu order_service được giữ nguyên.
