# Part 2 — Order Service gọi Inventory Service bằng OpenFeign

`InventoryClient` là Feign interface gọi `GET /api/inventory` với hai query param `skuCode` và `quantity`. URL được lấy từ `inventory.url`, mặc định `http://localhost:8082`, nên không phải thay đổi mã nguồn khi đổi môi trường.

`OrderService.placeOrder()` gọi Inventory trước khi `save`. Khi response là `false`, service ném `RuntimeException` với message `Product with Skucode <sku> is not in stock`; Spring trả HTTP 500 theo yêu cầu Part 2. Vì exception xảy ra trước lệnh save, không tạo đơn mới.

## Chạy thủ công

Khởi động MySQL, Inventory Service (8082), rồi Order Service (8081). Kiểm tra trực tiếp Inventory:

```sh
curl 'http://localhost:8082/api/inventory?skuCode=iphone_15&quantity=100'
# true
curl 'http://localhost:8082/api/inventory?skuCode=iphone_15&quantity=101'
# false
```

Tạo đơn với tồn kho đủ:

```sh
curl -i -X POST http://localhost:8081/api/order \
  -H 'Content-Type: application/json' \
  -d '{"skuCode":"iphone_15","price":1000,"quantity":100}'
```

Kết quả là HTTP 201, body `Order Placed Successfully`. Với `quantity:101`, kết quả là HTTP 500 và MySQL không có dòng đơn mới.

Import `postman/openfeign-order-service.postman_collection.json` vào Postman để chạy hai trường hợp trên. Collection dùng biến `order_base_url=http://localhost:8081`.

## Integration test

`OrderServiceApplicationTests` dùng Testcontainers MySQL và `@AutoConfigureWireMock(port = 0)`. Test properties đổi `inventory.url` sang port WireMock ngẫu nhiên. `InventoryStubs` khớp path và từng query param độc lập, nên test sẽ fail nếu Feign gửi sai tên hoặc giá trị param.

```sh
DOCKER_HOST="unix://$HOME/.docker/run/docker.sock" \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
./mvnw clean verify
```
