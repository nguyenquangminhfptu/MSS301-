# ShoppingServices — Part 1–5 / Slot 9

**Nguyen Quang Minh — DE190270 — SE19B04**

Bản sao độc lập từ `Slot 7/ShoppingServices`, giữ Product CRUD, Inventory,
Order/OpenFeign và Gateway/OAuth2 JWT. Part 5 hoàn thành đủ **DOC-1–DOC-16**.

## Khởi động

Cần Docker Desktop đang chạy, JDK 21, Python 3 và mạng để tải dependency lần đầu.
Mở terminal tại thư mục `Slot 9/ShoppingServices`:

```sh
python3 scripts/local.py start
```

Script build cả bốn ứng dụng, khởi động infrastructure bằng root Compose,
import realm demo, chờ docs trả HTTP 200 rồi thông báo các service đã sẵn sàng.
Trên macOS script tự chọn JDK 21; trên hệ điều hành khác đặt `JAVA_HOME` tới JDK 21.
Log và PID nằm trong `.run/` và không được commit.

| Thành phần | Cổng localhost | Đường dẫn |
|---|---:|---|
| API Gateway | 9000 | `/swagger-ui.html` |
| Product | 8080 | `/swagger-ui.html`, `/api-docs` |
| Order | 8081 | `/swagger-ui.html`, `/api-docs` |
| Inventory | 8082 | `/swagger-ui.html`, `/api-docs` |
| Keycloak | 8181 | `/realms/spring-microservices-realm` |
| MongoDB | 27029 | Profile `local` của Product |
| MySQL | 3339 | `order_service`, `inventory_service` |

Chạy root `docker-compose.yml`; các Compose trong từng service là tài liệu
Part 1–4 đã sao chép. Root Compose dùng tên project `mss301-slot9`, cổng và volume
riêng, tránh trùng môi trường cũ. Profile `local` chọn database ở bảng trên.
Có thể chạy từng service bằng `sh ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
sau khi `docker compose up -d --wait`.

Dừng các ứng dụng của workspace này, giữ dữ liệu Docker:

```sh
python3 scripts/local.py stop
docker compose stop
```

## Dùng Swagger

1. Mở **http://localhost:9000/swagger-ui.html**. Không cần đăng nhập để xem docs.
2. Chọn **Product Service**, **Order Service**, **Inventory Service** trong dropdown.
3. Product có `GET/POST /api/products`, `PUT/DELETE /api/products/{id}`;
   Order có `POST /api/order`; Inventory có `GET /api/inventory`.
4. Import collection và environment trong `postman/`. Chọn environment
   **MSS301 Slot 9 Local**, gửi request **01 - Keycloak demo token**.
5. Copy giá trị `access_token` trong response. Tại Swagger chọn **Authorize**,
   dán token **không kèm chữ Bearer**, chọn **Apply credentials**, **Close**.
6. Mở `GET /api/products` → **Try it out** → **Execute**. Kết quả: HTTP 200.
   Swagger gửi API tới Gateway port 9000 nhờ server URL tương đối `/`.
7. Đổi definition có thể xóa authorization; nhập lại token nếu cần. Token demo
   hết hạn sau khoảng 5 phút; gửi lại request Keycloak để lấy token mới.

Tài khoản demo local: user `student`, password `demo-local-123`, public client
`shopping-swagger`. Keycloak Admin Console: http://localhost:8181/admin,
`admin` / `admin`. Đây là các giá trị demo đi kèm môi trường thực hành local.

Gateway cho phép công khai Swagger assets, `/v3/api-docs/**`, `/aggregate/**`
và health check. Các `/api/**` vẫn yêu cầu JWT: thiếu hoặc sai token trả 401.
Các backend trực tiếp giữ nguyên mô hình Part 1–4: JWT được kiểm tra tại Gateway.

## Kiểm thử

Từ thư mục ShoppingServices:

```sh
sh scripts/verify.sh
```

Lệnh chạy **toàn bộ** test Part 1–4 và Part 5 trên cả bốn module. Database test
là Testcontainers, Inventory giả lập cho Order là WireMock; không cần khởi động
stack local. Docker Desktop vẫn phải đang chạy. Gateway tests mock JwtDecoder.

Chạy riêng Swagger tests, tại thư mục từng service:

```sh
sh ./mvnw test -Dtest=SwaggerIntegrationTest   # product / inventory / order
sh ./mvnw test -Dtest=SwaggerSecurityTest      # api-gateway
```

Kết quả đã kiểm tra: **44 tests, 0 failures/errors/skips**. Chi tiết và ảnh thật:
[docs/PART5_RESULTS.md](docs/PART5_RESULTS.md).

## Postman / Newman

Import cả hai file:

- `postman/slot9-swagger.postman_collection.json`
- `postman/slot9-local.postman_environment.json`

Chọn environment, chạy cả collection theo thứ tự, 1 iteration. Collection tự lấy
JWT rồi kiểm tra docs trực tiếp/tổng hợp, CORS, anonymous/invalid JWT và CRUD,
Inventory, Order → Feign → Inventory. Collection tạo một order trong database
Slot 9 và xóa sản phẩm demo do nó vừa tạo. Không export token vào repository.

Có thể chạy bằng Newman:

```sh
npx --yes newman@6.2.1 run postman/slot9-swagger.postman_collection.json \
  -e postman/slot9-local.postman_environment.json
```

Kết quả lần chạy đã lưu: **23 requests, 53 assertions, 0 failed** tại
[docs/evidence/newman-results.txt](docs/evidence/newman-results.txt).
Các báo cáo `postman/TEST-RESULTS.md` trong từng service là lịch sử Part 1–4,
không phải kết quả mới của Slot 9.

## Khác biệt cần thiết so với snippet trong đề

- Spring Boot **3.5.14** ở ba backend dùng Springdoc **2.8.17**;
  Gateway Boot **4.1.0** dùng Springdoc **3.1.1**. Bản 2.5.0 trong snippet dành
  cho Boot 3.2, không phù hợp nền tảng hiện có. Xem
  [bảng tương thích chính thức](https://springdoc.org/v2/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot).
- Package Product thực tế là `com.fudn.product_service`, nên config đặt cùng
  cây package để được Spring scan. API thực tế là `/api/products` (số nhiều).
- Gateway hiện dùng API `http()` + `.before(uri(...))`; Swagger route dùng
  `.before(setPath("/api-docs"))` tương ứng phiên bản Cloud hiện có.
- CORS Gateway cho phép thêm PUT/DELETE/OPTIONS để giữ đủ Product CRUD.
- Order test dùng `application-test.properties` + profile `test` để cấu hình
  WireMock không che mất `application.properties` thật.
- Product target Java 21 như ba module còn lại. Metadata title/version/license
  giữ đúng đề. Các liên kết Wiki `dummy-url` là placeholder được đề cung cấp.
