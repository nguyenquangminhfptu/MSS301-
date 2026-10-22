# MSS301 — Slot 13: Order Saga (Orchestration)

Bài làm theo `../Saga_guide.md`, Java 21, Spring Boot **4.1.1**.
Ba service và ba database riêng, không Kafka/Eureka:

| Service | API | Database trong Compose | Host database |
|---|---|---|---|
| order-service | http://localhost:8081/api/orders | MySQL 8.4 / orderdb | localhost:3307 |
| inventory-service | http://localhost:8082/api/inventory/products | MongoDB 8.0 / inventorydb / rs0 | localhost:27017 |
| payment-service | http://localhost:8083/api/payments/wallets | SQL Server 2022 / paymentdb | localhost:14330 |

## Chạy toàn hệ thống

Tại thư mục `slot13/saga-demo`:

```sh
docker compose up -d --build --wait
docker compose ps
```

Cả **6 container phải healthy**. Docker Desktop cần ít nhất 4 GB RAM.
SQL Server dùng `platform: linux/amd64` để chạy trên Apple Silicon bằng emulation.
Project Compose tên `slot13-saga`, volume độc lập với các Slot trước.
Mỗi service có Dockerfile build Maven/Java 21 và healthcheck Actuator.
Dừng nhưng giữ dữ liệu: `docker compose down`.

Dữ liệu khởi tạo: P1/Laptop/1000/10, P2/Mouse/20/5; C1=5000, C2=100.
Restart không reset tồn kho/ví: Mongo chỉ seed khi collection rỗng;
SQL Server dùng `IF NOT EXISTS` trong `data.sql`.

Để chủ động xóa **toàn bộ dữ liệu demo Slot 13** và chạy lại từ đầu:

```sh
docker compose down -v
docker compose up -d --build --wait
```

## Project kiểm thử Postman

Hai collection và environment nằm trong `postman/`; đã import vào **My Workspace** của Postman:

- **MSS301 Slot 13 - Saga automated**: KB1 → KB5 → KB7 và validation; **64 requests, 128 assertions**.
- **MSS301 Slot 13 - Saga outage tests**: KB6 và KB8, chạy riêng khi đã bố trí sự cố.
- Environment **MSS301 Slot 13 Local**: `orderUrl`, `inventoryUrl`, `paymentUrl`.

Mở collection automated → **Run** → chọn environment → giữ thứ tự và **1 iteration** → Run.
Collection tự lưu order IDs và snapshot kho/ví trong collection variables.
Kiểm tra cả HTTP, trạng thái order/Saga, failureReason, thứ tự log, reservation RELEASED,
chênh lệch kho/ví, gọi lặp, refund thực, input không hợp lệ, replay khác payload.
Bản JSON có thể import lại trên máy khác, không cần API key/Postman account để dùng Newman.

KB1 mỗi lần chạy tiêu thụ **2 P1 và 2000 của C1**; snapshot giúp so sánh chênh lệch,
nhưng vẫn cần C1 đủ tiền và kho đủ hàng. Khi không đủ, reset demo bằng lệnh trên.
KB7 dùng ID riêng cho reserve/pay/release/refund trực tiếp và tự hoàn trả tài nguyên của ID đó.
Không chạy các collection đồng thời với kiểm thử outage hoặc reset dữ liệu.

Chạy cùng collection bằng CLI:

```sh
npm ci
npm test
```

JSON/JUnit report được lưu vào `evidence/newman.json`, `evidence/newman.xml`.

## KB6 và KB8: kiểm tra partial failure

Chạy script có sẵn (cần `npm ci` trước):

```sh
python3 scripts/test-outages.py
```

Script chỉ stop/start service thuộc Compose project này, luôn restore service trong `finally`.
KB6: tắt Payment, gọi Saga, kiểm tra kho được trả; ví chỉ kiểm tra sau khi Payment bật lại.
KB8: đặt `PAYMENT_DELAY_MS=15000`, chờ log RESERVE_INVENTORY rồi tắt cả hai participant;
kiểm tra ba lần compensation lỗi, không có CANCEL_ORDER và kho bị giữ 1 P2.
Sau đó script bật lại service, đưa delay về 0 và gọi release để dọn tài nguyên test.
Order KB8 **vẫn COMPENSATING**: chưa có recovery job, việc trả kho thủ công không đồng nghĩa
đã hoàn tất toàn Saga. Đây là kết quả mong đợi của KB8 trong đề.

Để quan sát bằng Postman:

1. KB6: `docker compose stop payment-service`, chạy riêng folder KB6; sau đó
   `docker compose start payment-service` và kiểm tra số dư không đổi.
2. KB8: `PAYMENT_DELAY_MS=15000 docker compose up -d --wait order-service`;
   chạy folder KB8, trong lúc request đặt hàng chờ sau RESERVE_INVENTORY, chạy
   `docker compose stop -t 1 inventory-service payment-service` ở terminal khác.
3. Xem request Saga logs: có đúng ba `COMPENSATION / FAILED`; order PENDING/COMPENSATING.
4. Khôi phục: `docker compose start inventory-service payment-service`,
   `PAYMENT_DELAY_MS=0 docker compose up -d --wait`.
   Gọi `POST /api/inventory/release/{kb8OrderId}` để trả kho thủ công.

## Kiểm thử Java

```sh
# 7 unit tests Orchestrator, không cần database
sh scripts/verify.sh
# Thêm 3 MongoDB + 4 SQL Server tests, tổng 14 tests
# Ba DB Compose phải đang healthy
SAGA_INTEGRATION=true sh scripts/verify.sh
```

Integration test Mongo dùng database riêng `inventory_test`;
SQL Server dùng ví `IT` và payment ID `9000001`, dọn sau mỗi test.
Test SQL Server chạy trong paymentdb của demo, không chạy song song với reset/outage.
Các test kiểm tra rollback cả hai bản ghi và duplicate payment đồng thời.

## Hiểu transaction trong Saga

Đọc [SAGA_EXPLAINED.md](docs/SAGA_EXPLAINED.md) cho luồng thành công, compensation,
trạng thái, retry, idempotency và giới hạn của demo.
Đối chiếu từng TODO với commit tại [COMMITS.md](docs/COMMITS.md).
Kết quả và bằng chứng tại [RESULTS.md](docs/RESULTS.md).
