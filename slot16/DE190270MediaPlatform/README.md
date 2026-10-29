# MSS301 Slot 16 DE190270

Bài làm đề `DeThiSampleNew/DeThiSample_New.docx`: nền tảng đặt chiến dịch quảng cáo với User Service, Post Service và Media Gateway. Mã sinh viên: **DE190270**.

Java 21, Spring Boot **4.1.0**, Spring Cloud **2025.1.2**, SpringDoc **3.0.3**, SQL Server 2016 trở lên. Mỗi project có `pom.xml` riêng và có thể mở/chạy độc lập trong IntelliJ; `pom.xml` ở thư mục này hỗ trợ build cả ba.

| Project | Package gốc | Port | Swagger |
|---|---|---:|---|
| DE190270UserService | fu.de190270.user | 8081 | http://localhost:8081/swagger-ui.html |
| DE190270PostService | fu.de190270.post | 8082 | http://localhost:8082/swagger-ui.html |
| DE190270MediaGateway | fu.de190270.gateway | 8080 | http://localhost:8080/swagger-ui.html |

## Chạy bằng Docker và Maven

Cần JDK 21, Maven 3.9+, Docker Desktop đang chạy. Thực hiện tại thư mục chứa README này:

```bash
docker compose -p de190270-slot16 up -d
docker compose -p de190270-slot16 ps -a
docker compose -p de190270-slot16 logs sqlserver-init
```

Đợi `sqlserver` healthy và `sqlserver-init` kết thúc với mã **0**. SQL Server dùng cổng 1433; container chạy `linux/amd64` để dùng được trên Mac Apple Silicon. Script tạo database `MSS301_2026_PE_MEDIA`, bảng `users`, bảng `posts` và ba creator mẫu. Bảng được tạo bằng SQL theo đề; **hai service đều đặt `spring.jpa.hibernate.ddl-auto=none`**.

Docker dùng mật khẩu bootstrap khi khởi tạo SQL Server, sau đó đặt tài khoản local về **sa/sa** đúng cấu hình đề. Dữ liệu được lưu trong Docker volume; chạy lại khởi tạo không xóa dữ liệu.

```bash
./scripts/build.sh
./scripts/start.sh
python3 scripts/test-smoke.py
```

Mở **http://localhost:8080/swagger-ui.html** và chọn `user-service` hoặc `post-service`. Trên macOS, các script tự chọn JDK 21 đã cài. Log và PID của ba ứng dụng nằm trong `.run/`. Có thể dùng `./scripts/start.sh --foreground` để giữ tiến trình trong terminal tự động hóa.

Nếu dùng SQL Server đã cài sẵn, chạy `sql/init.sql` trong SSMS trước khi khởi động hai service, với tài khoản `sa/sa`. File `sql/docker-exam-login.sql` chỉ được script Docker gọi cho container local.

## Chạy từng project trong IntelliJ hoặc terminal

Mở `pom.xml`, chọn Project SDK **21**, Reload Maven. Các main class:

- `fu.de190270.user.DE190270UserServiceApplication`
- `fu.de190270.post.DE190270PostServiceApplication`
- `fu.de190270.gateway.DE190270MediaGatewayApplication`

Hoặc chạy mỗi lệnh trong một terminal riêng:

```bash
mvn -f DE190270UserService/pom.xml spring-boot:run
mvn -f DE190270PostService/pom.xml spring-boot:run
mvn -f DE190270MediaGateway/pom.xml spring-boot:run
```

## API và quy tắc nghiệp vụ

Gateway chuyển `/api/users` và `/api/users/**` tới 8081, `/api/posts` và `/api/posts/**` tới 8082. Post Service gọi User Service bằng **OpenFeign**, nhận `ApiResponse<UserDTO>` có kiểu dữ liệu cụ thể. User Service và Post Service có Controller → Service → Repository; Gateway dùng WebFlux và `CorsWebFilter`. Cả ba bật Swagger, OpenAPI và CORS.

| Method | URL | Chức năng |
|---|---|---|
| POST | /api/users | Tạo user, mặc định ACTIVE |
| PUT | /api/users/{userId} | Cập nhật các trường được cung cấp |
| GET | /api/users/{userId} | Chi tiết user |
| DELETE | /api/users/{userId} | Đặt trạng thái DEACTIVATED |
| GET | /api/users | Phân trang, lọc role và status |
| POST | /api/posts | Tạo chiến dịch, mặc định SCHEDULED |
| PUT | /api/posts/{postId} | Cập nhật từng phần, tính lại tổng tiền |
| GET | /api/posts/{postId} | Chi tiết có đối tượng user lấy qua Feign |
| DELETE | /api/posts/{postId} | Đặt trạng thái CANCELLED |
| GET | /api/posts | Phân trang, lọc advertiserName chứa chuỗi và status |

Phân trang dùng `page=0`, `size=10`, giới hạn `1..100`; kết quả được sắp theo ID để ổn định. Bộ lọc enum sai, số trang âm, email sai, chuỗi trắng, dữ liệu thiếu hoặc vượt giới hạn đều trả HTTP 406/status 2. `PUT` giữ nguyên những trường thiếu hoặc có giá trị `null`. ID và tổng tiền do server quản lý.

Ngày nhập và xuất là **dd/MM/yyyy**, không có thành phần giờ. `endDate` phải sau `startDate`; số ngày bằng chênh lệch hai ngày, không tính ngày kết thúc. Công thức đúng theo đề: **totalCost = ratePerPost × số ngày**. `numberOfPosts` phải từ 1 đến `maxDailyPosts` của creator. Tổng tiền trong request không ghi đè được kết quả server tính. Cập nhật dùng đơn giá hiện tại của creator, kiểm tra dữ liệu hiệu lực trước khi sửa entity và lưu trong transaction. Việc tạo chiến dịch hoặc chuyển sang creator khác yêu cầu creator tồn tại và ACTIVE.

| status trong JSON | HTTP | Ý nghĩa |
|---:|---|---|
| 1 | 200 / 201 | Thành công |
| 2 | 406 | Validation |
| 3 | 226 | Trùng username |
| 4 | 404 | Không tìm thấy |
| 5 | 400 | User không ACTIVE |
| 0 | 500 | Lỗi server hoặc User Service không phản hồi |

Mọi kết quả nghiệp vụ có `status`, `message`, `data`, `timestamp`; timestamp theo UTC ISO 8601, ví dụ `2026-10-29T15:30:00Z`. Khi lấy danh sách post, `content` chứa `PostDetailDTO`, thay `userId` bằng `user`.

Ví dụ tạo creator qua Gateway:

```bash
curl -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"username":"creator_demo","email":"demo@example.com","role":"CREATOR","ratePerPost":500000,"maxDailyPosts":3,"level":1}'
```

Dùng `userId` nhận được để tạo chiến dịch:

```json
{
  "advertiserName": "Shop ABC",
  "advertiserEmail": "ads@example.com",
  "advertiserPhone": "0900000000",
  "startDate": "25/07/2026",
  "endDate": "28/07/2026",
  "numberOfPosts": 2,
  "userId": 1
}
```

Gửi JSON trên bằng `POST http://localhost:8080/api/posts`. Với đơn giá 500000, kết quả `totalCost=1500000`.

## Kiểm thử

`./scripts/build.sh` chạy `mvn clean verify`. Các kiểm thử API dùng H2 ở **test scope** và schema SQL tạo tường minh, vẫn giữ `ddl-auto=none`. Post Service dùng máy chủ HTTP giả lập để kiểm tra Feign thực tế, gồm 404 và 503. Gateway chạy HTTP thật để kiểm tra định tuyến, giữ HTTP 226, CORS và Swagger.

`python3 scripts/test-smoke.py` yêu cầu cả ba ứng dụng và SQL Server đang chạy. Script tạo dữ liệu test riêng, kiểm tra CRUD qua Gateway, Unicode, ngày lưu/đọc, tính lại tiền, giới hạn số bài, cập nhật sai không được lưu và các mã lỗi. Kết quả ghi vào `evidence/smoke-results.json`; dữ liệu test kết thúc với post CANCELLED và user DEACTIVATED.

Đã xác minh ngày **29/10/2026**: `mvn clean verify` thành công với **42 kiểm thử**, cùng **28 kiểm tra qua Gateway với SQL Server thật**, không có lỗi. Xem [kết quả build](evidence/verification.txt) và [kết quả chạy thật](evidence/smoke-results.json).

## Đóng gói và dừng ứng dụng

```bash
./scripts/stop.sh
./scripts/package.sh
```

Script đóng gói chạy `mvn clean` và tạo **DE190270_Slot16_Submission.zip** ở thư mục `slot16`, bao gồm ba project, SQL, test và hướng dẫn; loại `target`, log và cấu hình IDE. Cần chạy build lại trước khi dùng `start.sh` sau khi đóng gói.

Dừng database khi không sử dụng:

```bash
docker compose -p de190270-slot16 down
```

Tài liệu phiên bản: [Spring Cloud 2025.1.2 hỗ trợ Boot 4.1.0](https://spring.io/blog/2026/06/11/spring-cloud-2025-1-2-aka-oakwood-has-been-released/), [SpringDoc 3.x hỗ trợ Boot 4](https://springdoc.org/).
