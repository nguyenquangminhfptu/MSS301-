# ShoppingServices — Part 2 OpenFeign

Ba service được sao chép từ Slot 5 và đặt trong một workspace chung:

| Service | Port | Database | Vai trò |
|---|---:|---|---|
| product-service | 8080 | MongoDB | Quản lý sản phẩm |
| order-service | 8081 | MySQL `order_service` | Tạo đơn hàng |
| inventory-service | 8082 | MySQL `inventory_service` | Kiểm tra tồn kho |

Order Service gọi đồng bộ Inventory Service bằng OpenFeign trước khi lưu đơn. Nếu Inventory trả `false`, Order Service trả HTTP 500 theo contract Part 2 và transaction không tạo đơn mới.

## Chạy Part 2

Mở Docker Desktop, sau đó chạy MySQL dùng chung:

```sh
cd order-service
docker compose up -d mysql
```

Trong hai terminal khác:

```sh
cd inventory-service
./mvnw spring-boot:run
```

```sh
cd order-service
./mvnw spring-boot:run
```

`inventory.url` mặc định là `http://localhost:8082`; có thể ghi đè bằng biến môi trường `INVENTORY_URL`.

## Kiểm thử

```sh
cd order-service
DOCKER_HOST="unix://$HOME/.docker/run/docker.sock" \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
./mvnw clean verify
```

Test Order dùng MySQL Testcontainers và WireMock. WireMock kiểm tra path/query string Feign gửi tới Inventory, gồm cả nhánh đủ hàng và hết hàng. Xem [hướng dẫn Part 2 của Order Service](order-service/PART2_OPENFEIGN.md).
