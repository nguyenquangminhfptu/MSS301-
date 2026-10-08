# Hướng dẫn chụp ảnh và hoàn thiện báo cáo Assignment 1

Nguyen Quang Minh — DE190270 — SE19B04

## Tình trạng và phần cần bổ sung

Code F0–F11, commit theo TODO và các lần kiểm thử đã có. Báo cáo hiện có 15 hình dựng từ output thực tế của build, Newman, API/database và Git; đó không phải ảnh giao diện Postman Desktop. Cần bổ sung ảnh màn hình cho từng TODO và ảnh Postman/Collection Runner theo yêu cầu giảng viên. Không ghi đã có ảnh Desktop khi mới chỉ có Newman.

Báo cáo đã được sửa sang trường TOC tự động của Word, sử dụng Heading 1 cho mục lớn và Heading 2 cho TODO. Mục lục hiện hiển thị các mục lớn. Font văn bản và commit thống nhất Times New Roman. Sau khi thêm ảnh phải cập nhật toàn bộ mục lục để tính lại số trang.

## 1. Khởi động hệ thống trên Mac

Mở Docker Desktop và chờ Docker hoạt động. Trong Terminal chạy:

```bash
cd '/Users/quangminhnguyen/Documents/FPT_SEMESTER/KY8/MSS/MSS301 /Assignment 1/fu-cinema'
docker compose -p fu-cinema up -d
docker compose -p fu-cinema ps -a
./scripts/build.sh
./scripts/start.sh
curl http://localhost:9000/actuator/health
mkdir -p evidence/screenshots
```

Chờ SQL Server `healthy`, `cinema-sqlserver-init` `Exited (0)`; build `BUILD SUCCESS`; start `All four services ready`; health `UP`. Lệnh build chạy lại 27 unit test. Chụp Terminal có kết quả này cho TODO 0.1/0.3/0.4 và mục kiểm thử. Nếu start báo port occupied, kiểm tra ứng dụng đang dùng port; script không tự tắt ứng dụng khác. Không xóa volume database để chụp lại.

## 2. Import Postman Desktop

1. Mở Postman Desktop, dùng Import để chọn hai file có sẵn trong `postman/`:
   - `FUCinemaBookingSystem.postman_collection.json`
   - `FUCinema-Local.postman_environment.json`
2. Chọn Environment `FUCinema-Local`. Kiểm tra `gateway = http://localhost:9000`.
3. Chụp màn hình Environment và cây Collection gồm folder `01-Auth` đến `08-Report`.
4. Token/ID đang trống là bình thường: script request sẽ tự lưu khi chạy đúng thứ tự. Không nhập token cũ từ báo cáo.

## 3. Chạy Collection Runner và lấy ảnh tổng

1. Chọn collection `FUCinemaBookingSystem` → Run (hoặc menu `...`/chuột phải → Run collection tùy phiên bản).
2. Nếu có Run type, chọn Functional → Local. Chọn environment `FUCinema-Local`, Iterations = 1, Delay = 0.
3. Giữ đủ 85 request và thứ tự 01 → 08. Bật `Persist responses for a session` (bản cũ có thể ghi Save responses) để xem response sau chạy. Bật `Keep variable values` để dùng ID/token cho test tay.
4. Start run/Run collection. Chụp trang tổng: tên collection, environment, số request/test, 0 failed/0 errors. Với collection nguyên bản: 85 request, 159 assertion.
5. Chụp thêm kết quả từng folder, có tên request và test PASS. Nếu danh sách dài, chụp nhiều ảnh cuộn liên tiếp, không thu nhỏ đến mức không đọc được.
6. Bấm một request trong kết quả run để xem request body/response/status; lấy ảnh tại đó. Đừng Send lại riêng các request tạo/hủy sau khi toàn bộ flow đã chạy: dữ liệu đã đổi, nên có thể nhận 409 hoặc 400 đúng nghiệp vụ nhưng khác lần đầu.

Mã HTTP 400/401/403/404/409 trong ca kiểm tra lỗi vẫn là kết quả đạt khi assertion mong đợi mã đó PASS. Có Failed assertion thì xử lý rồi chạy lại cả collection; không chỉ đổi dòng chữ trong báo cáo. Các số tổng doanh thu có thể tăng do dữ liệu các lần chạy trước, dùng response thực tế.

## 4. Bộ ảnh Postman cần chèn

Mỗi ảnh phải đọc được tên request, method, URL qua Gateway và status. Với request có body, chụp body gửi đi và response JSON; chụp thêm Test Results nếu tab này không nằm cùng khung. Với 204 chụp status và test, không cần response body. Che token khi giá trị xuất hiện.

| Folder | Request tiêu biểu cần chụp chi tiết | Chèn vào phần |
|---|---|---|
| 01-Auth | 1.1 Admin 200; 1.2 Customer 200; 1.3 sai mật khẩu 401; 1.4 INACTIVE 403; 1.6/1.7 token 401 | F1 và F10 |
| 02-Customer | 2.1 tạo 201; 2.2 trùng email 409; 2.3 validation 400; 2.6 Unicode; 2.8 đổi password 204; 2.13/2.15/2.16 CRUD; 2.18 header giả | F2, F3 và F10 |
| 03-Genre-Room | 3.4 trùng tên 409; 3.6 xóa genre có tham chiếu 409; 3.7 room 40 ghế 201; 3.8 thiếu token 401 | F4 |
| 04-Movie | 4.1 tạo phim 201; 4.2 search 200; 4.7 genre thiếu 404; 4.8 xóa genre của phim đang được dùng 409 | F5 |
| 05-Showtime | 5.1 endTime tự tính 201; 5.2 overlap 409; 5.7 search; 5.8 layout; 5.10 CANCELLED | F6 |
| 06-Booking | 6.1 40 ghế trống; 6.2 đặt 201/tổng 190000; 6.3 trùng ghế 409; 6.14 còn 37 ghế | F7 |
| 07-History-Cancel | 7.1 lịch sử; 7.2 sai chủ 403; 7.3 Admin 200; 7.5 hủy 200; 7.6 hủy lại 400; 7.7 đặt lại ghế 201 | F8 |
| 08-Report | 8.1 tổng và revenueByMovie 200; 8.2 ngày sai 400; 8.3 Customer 403; 8.5 kỳ trống 200 | F9 |
| Collection tổng | Runner 85 request, 159 assertion, 0 failed; ảnh 8 folder và Scripts → Post-response | F11 TODO 11.2/11.3 và README |

Bộ trên là các ảnh chi tiết quan trọng. Để chứng minh toàn bộ request, giữ thêm ảnh Runner của cả 8 folder. Để đáp ứng “theo từng TODO”, dùng checklist 54 dòng bên dưới; nhiều TODO về entity/config không có request riêng nên cần ảnh code/database/build tương ứng.

## 5. Integration test cần chứng minh

Luồng chính chạy qua các service thật, không dùng mock: login lấy JWT → tạo genre/room → tạo movie → tạo showtime → đặt vé → xem ghế → hủy/đặt lại → báo cáo. Lấy ảnh Postman ở các request 1.2, 3.7, 4.1, 5.1, 6.2, 6.14, 7.5, 7.7, 8.1; ghi rõ ID liên kết giữa các bước. Đối chiếu MySQL booking/booking_detail với showtimeId/movieId từ MongoDB để chứng minh dữ liệu đã lưu. 27 JUnit test là unit test; không gọi riêng kết quả JUnit là bằng chứng toàn bộ integration.

Test tay 6.15 BR14 theo Guide (thực hiện sau Runner):

1. Duplicate request 6.13 sang collection riêng tên `Manual-Integration`, đổi tên `6.15 Movie Service unavailable`. Giữ POST `{{gateway}}/api/bookings`, Bearer `{{customer2Token}}`, đổi ghế A1 thành B2. Body:

```json
{"items":[{"showtimeId":"{{showtimeId}}","seatCode":"B2"}]}
```

2. Thay Scripts → Post-response của bản sao (không sửa request gốc):

```javascript
pm.test("Movie Service unavailable returns 503", () => {
    pm.response.to.have.status(503);
});
pm.test("Unified JSON error", () => {
    const e = pm.response.json();
    pm.expect(e.status).to.eql(503);
    pm.expect(e.message).to.include("unavailable");
});
```

3. Nếu đã khởi động bằng script ở trên, dừng đúng Movie Service trong Terminal tại fu-cinema:

```bash
kill "$(cat .run/movie-service.pid)"
lsof -iTCP:8082 -sTCP:LISTEN
```

Chờ port 8082 không còn listener, giữ Customer/Booking/Gateway chạy. Nếu chạy Movie trong IDE thì Stop riêng Movie tại IDE thay cho kill.
4. Send bản sao 6.15. Chụp status 503, JSON lỗi và Test Results PASS. Không đưa ca dừng service này vào Runner 85 request.
5. Khởi động lại ngay bằng `./scripts/start.sh` (hoặc Run Movie trong IDE), dù test 503 đạt hay chưa. Chờ ready, GET `{{gateway}}/api/movies` trả 200 và chụp ảnh phục hồi. Không gửi lại bản sao 6.15 sau phục hồi với assertion 503.
6. Chèn hai ảnh 503 và phục hồi 200 dưới TODO 7.4/7.5 hoặc phần kiểm thử bổ sung. Mô tả: “Khi Movie Service không hoạt động, Booking trả 503 qua Gateway; sau khi khởi động lại, API Movie trả 200.”

## 6. Checklist ảnh đủ từng TODO

Chụp bằng Shift + Command + 4 (vùng chọn) hoặc Shift + Command + 5 (cửa sổ) trên Mac. Để ảnh đọc rõ ở chiều rộng trang Word, chỉ chụp phần liên quan, giữ tên file hoặc URL và kết quả. Lưu `evidence/screenshots/TODO-7.5-booking-201.png` theo mẫu. Mỗi TODO có ít nhất một ảnh trực tiếp liên quan; có thể dùng nhiều ảnh code và kết quả. Nếu một ảnh chứng minh nhiều TODO, ghi rõ các TODO trong caption và dẫn chiếu ở mỗi mục, nhưng chụp riêng từng TODO sẽ dễ chấm hơn.

### F0

| TODO | Chụp gì | Nội dung phải thấy |
|---|---|---|
| 0.1 | docker-compose.yml, cây thư mục 4 service; Terminal `docker compose -p fu-cinema ps -a` | 3 database Up, SQL Server healthy, sqlserver-init Exited (0). |
| 0.2 | sqlserver/init.sql và mysql/init.sql; công cụ DB hoặc query trong Terminal | cinema_customer ở SQL Server, cinema_booking ở MySQL, cinema_movie ở MongoDB. Không tạo cả ba trong MySQL. |
| 0.3 | pom.xml của customer/movie/booking, cây thư mục và Terminal build | Java 21, Boot 4.1.0, các starter đúng và BUILD SUCCESS. |
| 0.4 | api-gateway/pom.xml, GatewayApplication.java; GET /actuator/health | Gateway MVC, Resource Server, Actuator; port 9000 và UP. |
| 0.5 | application.properties của cả 3 service; .run/*-service.log | Port, URL database, ddl-auto=none; log kết nối/khởi động thành công. |
| 0.6 | exception/GlobalExceptionHandler.java và ErrorResponse.java; Postman 2.3 hoặc 6.3 | JSON gồm timestamp, status, error, message, path; status 400/409 đúng và Tests PASS. |

### F1

| TODO | Chụp gì | Nội dung phải thấy |
|---|---|---|
| 1.1 | customer-service/application.properties; Postman 1.1 | Admin/JWT config và login 200 với role ADMIN. |
| 1.2 | config/PasswordConfig.java; kết quả AuthAndJwtTests trong Terminal/IDE | BCryptPasswordEncoder và test hash seed PASS. |
| 1.3 | security/JwtService.java; kết quả AuthAndJwtTests | HS256, uid, role, sub, iat, exp và test chữ ký/claims PASS. |
| 1.4 | service/AuthService.java; Postman 1.1–1.5 | Admin/Customer ACTIVE 200, sai mật khẩu 401, INACTIVE 403. |
| 1.5 | controller/AuthController.java; Postman 1.1, 1.2 | POST /api/auth/login trả tokenType, expiresIn, role, userId, email. |

### F2

| TODO | Chụp gì | Nội dung phải thấy |
|---|---|---|
| 2.1 | customer-service/db/migration/V1__init.sql, V2__seed.sql; bảng customer + flyway_schema_history | 3 customer mẫu, Chi INACTIVE; tên có dấu đúng; migration success. |
| 2.2 | Customer.java, CustomerRepository.java; Postman 2.5 hoặc 2.11 | Entity, enum, repository; profile/search trả dữ liệu đúng. |
| 2.3 | RegisterRequest.java, ProfileUpdateRequest.java; Postman 2.3 | Annotation validation và response 400 nêu field sai. |
| 2.4 | CustomerService.register; Postman 2.1, 2.2 | Tạo 201 ACTIVE, không lộ password; trùng email 409. |
| 2.5 | CustomerService phần profile/password; Postman 2.6–2.9 | Cập nhật tên có dấu 200; mật khẩu cũ sai 400, đổi đúng 204, login mới 200. |
| 2.6 | CustomerController.java; Postman 2.1, 2.5, 2.8 | Các endpoint register/me/me-password chạy đúng. |

### F3

| TODO | Chụp gì | Nội dung phải thấy |
|---|---|---|
| 3.1 | AdminCustomerRequest.java; Postman 2.13 | DTO có customerStatus; tạo khách hàng bởi Admin 201. |
| 3.2 | CustomerService phần admin; Postman 2.11–2.16 | Search, create/update; delete 204 và đọc lại INACTIVE 200. |
| 3.3 | CustomerController phần Admin; Postman 2.10, 2.13, 2.15 | Customer bị 403; Admin tạo 201/xóa mềm 204. |

### F4

| TODO | Chụp gì | Nội dung phải thấy |
|---|---|---|
| 4.1 | DataSeeder.java; MongoDB collection trước và sau restart movie-service | Dữ liệu mẫu có ObjectId cố định; mỗi collection không rỗng không bị nhân đôi. Chụp counts thật của lần chạy của bạn. |
| 4.2 | Genre.java, CinemaRoom.java, các repository; MongoDB indexes hoặc Postman 3.7 | @Document, @Indexed(unique=true); totalSeats=40 với 5 hàng × 8 ghế. |
| 4.3 | GenreService.java, RoomService.java; Postman 3.4, 3.6, 5.11 | Tên trùng/tham chiếu không được xóa: 409. |
| 4.4 | GenreController.java, RoomController.java; Postman 3.1, 3.3, 3.7, 3.8 | GET genre public; Customer ghi 403; Admin tạo room 201; không token 401. |

### F5

| TODO | Chụp gì | Nội dung phải thấy |
|---|---|---|
| 5.1 | Movie.java; MongoDB movies hoặc Postman 4.1 | genreId là String/ObjectId tham chiếu, document có các field phim. |
| 5.2 | MovieService phần MongoTemplate/Criteria; Postman 4.2, 4.3 | Search keyword galaxy; lọc genre/status đúng. |
| 5.3 | MovieService phần CRUD; Postman 4.1, 4.7, 4.8 | Tạo 201, genre không tồn tại 404, genre có phim không xóa được 409; chụp thêm DELETE /api/movies/{{movieId}} (Admin) sau 5.1 để chứng minh phim có showtime trả 409. |
| 5.4 | MovieController.java; Postman 4.1–4.8 | Endpoint public/admin đúng, response movieId 24 hex. |

### F6

| TODO | Chụp gì | Nội dung phải thấy |
|---|---|---|
| 6.1 | Showtime.java; MongoDB showtimes | ticketPrice Decimal128, roomId/startTime compound index; startTime là BSON Date. |
| 6.2 | ShowtimeRepository.java; Postman 5.2 | Derived query overlap và trùng giờ cùng phòng trả 409. |
| 6.3 | ShowtimeService phần create/update; Postman 5.1, 5.2, 5.4–5.6 | Tạo 201, endTime tự tính 120 phút; overlap 409, input nghiệp vụ sai 400. |
| 6.4 | ShowtimeService phần cancel/search; Postman 5.7, 5.9, 5.10 | Search đúng ngày/movie; hủy xong đọc lại CANCELLED. |
| 6.5 | ShowtimeController.java; Postman 5.8 | GET chi tiết trả seatRows, seatsPerRow, ticketPrice để Booking sử dụng. |

### F7

| TODO | Chụp gì | Nội dung phải thấy |
|---|---|---|
| 7.1 | booking-service/pom.xml, BookingServiceApplication.java; Terminal build | OpenFeign dependency/BOM, @EnableFeignClients, BUILD SUCCESS. |
| 7.2 | booking-service/db/migration/V1__init.sql, V2__seat_reservations.sql; MySQL tables | booking, booking_detail, seat_reservation; VARCHAR(24), Flyway success. |
| 7.3 | Booking.java, BookingDetail.java; MySQL booking_detail sau Postman 6.2 | OneToMany/cascade; snapshot movie/room/giá và ghế đã được lưu. |
| 7.4 | MovieClient.java, ShowtimeResponse.java; Postman 6.2 và test tay 6.15 | Feign gọi Movie; đặt vé có snapshot và lỗi Movie outage trả 503. |
| 7.5 | BookingService.create; Postman 6.2–6.13 | Đặt thành công 201, trùng ghế 409, ghế/input sai 400; giá do server tính. |
| 7.6 | BookingService.getSeatMap; Postman 6.1 và 6.14 | Trước đặt 40 ghế trống; sau đặt 37, bookedSeats A1/E5/E6 trong lần chạy mới. |
| 7.7 | BookingController.java; Postman 6.1, 6.2, 6.11, 6.12 | Seat map public; Customer tạo 201, không token 401, Admin đặt 403. |

### F8

| TODO | Chụp gì | Nội dung phải thấy |
|---|---|---|
| 8.1 | BookingService.getMyBookings; Postman 7.1 | Chỉ booking của Customer hiện tại, bookingDate giảm dần. |
| 8.2 | BookingService.getById; Postman 7.2, 7.3 | Customer khác 403; Admin xem được 200. |
| 8.3 | BookingService.cancel; Postman 7.4–7.7; BookingRulesTests | Sai chủ sở hữu 403, hủy 200, hủy lại 400, đặt lại ghế 201; test deadline PASS. |
| 8.4 | BookingController phần history/cancel/admin; Postman 7.1, 7.5, 7.8 | /my, /{id}/cancel, danh sách Admin hoạt động đúng. |

### F9

| TODO | Chụp gì | Nội dung phải thấy |
|---|---|---|
| 9.1 | BookingRepository.findForReport; Postman 8.1 | Query chỉ CONFIRMED, khoảng ngày đúng, sắp xếp ngày giảm dần. |
| 9.2 | BookingService.report; Postman 8.1, 8.5 | Tổng booking/vé/doanh thu khớp response; loại CANCELLED, revenueByMovie giảm dần. Ghi số thực tế của lần chạy, không mặc định 285000 nếu đã có dữ liệu cũ. |
| 9.3 | BookingController phần report; Postman 8.1–8.5 | Admin 200, ngày sai/thiếu ngày 400, Customer 403, kỳ trống tổng bằng 0. |

### F10

| TODO | Chụp gì | Nội dung phải thấy |
|---|---|---|
| 10.1 | api-gateway/application.properties; Terminal startup/health | 9000, URL 8081/8082/8083; shared JWT config. |
| 10.2 | UserHeaderFilter.java; Postman 2.18 (Headers + response) | Header giả X-User-Id/X-User-Role bị ghi đè; response vẫn là an@gmail.com. |
| 10.3 | Routes.java; Postman login, movies, booking qua localhost:9000 | 3 nhóm route đến đúng service; mọi request qua Gateway. |
| 10.4 | SecurityConfig.java; Postman 1.6, 1.7, 2.10, 6.12 | Không/sai token 401; sai role 403; Tests PASS. |

### F11

| TODO | Chụp gì | Nội dung phải thấy |
|---|---|---|
| 11.1 | Postman Environment FUCinema-Local | gateway, token variables, ID variables và ngày; che giá trị token khi chụp. |
| 11.2 | Postman Collection mở đủ 8 folder; Scripts → Post-response của 6.2 hoặc 8.1 | 85 request có test status/body/lưu biến; chụp script và kết quả Tests PASS. |
| 11.3 | Postman Collection Runner tổng; 8 ảnh kết quả từng folder; file export | 85 requests, 159 assertions, 0 failed; environment và collection JSON nộp kèm. |

## 7. Commit message đã dùng

Báo cáo đã ghi hash và subject thật tại mỗi TODO. Không cần tạo lại commit cho code đã hoàn tất. Dưới đây là các commit triển khai gốc; TODO nhỏ liên quan được gộp theo Guide. Các commit fix/test/docs bổ sung cũng còn trong lịch sử Git.

| TODO | Commit | Message thực tế |
|---|---|---|
| 0.1 | `a89371d` | `chore(infra): add docker compose for sql server, mongodb and mysql` |
| 0.2 | `4c1cf9d` | `chore(infra): add init scripts for sql server and mysql databases` |
| 0.3 | `70df9b3` | `build(customer): bootstrap customer-service with required starters` |
| 0.3 | `ddbf8e1` | `build(movie): bootstrap movie-service with required starters` |
| 0.3 | `6bbe0af` | `build(booking): bootstrap booking-service with required starters` |
| 0.4 | `375394a` | `build(gateway): bootstrap api-gateway with required starters` |
| 0.5, 1.1 | `ad4e4b1` | `chore(customer): configure database and application settings` |
| 0.6 | `e6d13e0` | `feat(customer): add unified JSON exception handling` |
| 0.5 | `1504ec0` | `chore(movie): configure database and application settings` |
| 0.6 | `273deb5` | `feat(movie): add unified JSON exception handling` |
| 0.5 | `6672269` | `chore(booking): configure database and application settings` |
| 0.6 | `8309a72` | `feat(booking): add unified JSON exception handling` |
| 2.1 | `1bfdce3` | `feat(customer): add unicode t-sql migrations and bcrypt seed data` |
| 2.2 | `ee1b03c` | `feat(customer): add customer entity and repository` |
| 1.2 | `01ee7d8` | `feat(customer): add bcrypt password encoder bean` |
| 1.3 | `7097661` | `feat(customer): sign HS256 access tokens with user claims` |
| 1.4 | `36f2734` | `feat(customer): authenticate admin and active customers` |
| 1.5 | `7f37959` | `feat(customer): expose login endpoint` |
| 2.3 | `7cffb2f` | `feat(customer): add validated registration and profile DTOs` |
| 3.1 | `748dff5` | `feat(customer): add admin customer request DTO` |
| 2.4 | `c0d6663` | `feat(customer): register customers with unique email and bcrypt` |
| 2.5 | `54e1256` | `feat(customer): implement profile updates and password changes` |
| 3.2 | `dca17df` | `feat(customer): implement admin search and customer soft delete` |
| 2.6 | `19318a4` | `feat(customer): expose registration and profile endpoints` |
| 3.3 | `4096362` | `feat(customer): expose admin customer CRUD endpoints` |
| 4.2 | `74c3f18` | `feat(movie): add genre and room documents with unique indexes` |
| 5.1 | `14c8a1f` | `feat(movie): add movie document and genre reference` |
| 6.1 | `93a4aa0` | `feat(movie): add showtime document with decimal128 price` |
| 6.2 | `ac626cf` | `feat(movie): query overlapping scheduled showtimes` |
| 4.3 | `ea7dcd4` | `feat(movie): implement genre and room CRUD with delete guards` |
| 5.2, 5.3 | `efd8897` | `feat(movie): search and manage movies with genre validation` |
| 6.3, 6.4 | `43b5eb0` | `feat(movie): schedule and cancel showtimes with overlap checks` |
| 4.1 | `bfcf600` | `feat(movie): seed cinema catalog with fixed ObjectIds` |
| 4.4 | `2898336` | `feat(movie): expose genre and room CRUD endpoints` |
| 5.4 | `274a5d7` | `feat(movie): expose movie search and CRUD endpoints` |
| 6.5 | `44ef11a` | `feat(movie): expose showtime scheduling endpoints` |
| 7.2 | `e0497ad` | `feat(booking): add mysql migrations with ObjectId references` |
| 7.3 | `3b22c7e` | `feat(booking): add booking and ticket entities` |
| 7.4 | `b15d51a` | `feat(booking): add OpenFeign showtime client and booking DTOs` |
| 7.1 | `1d8cf05` | `build(booking): enable OpenFeign client discovery` |
| 9.1 | `82cf6c2` | `feat(booking): query bookings and confirmed revenue by date range` |
| 7.5 | `2862462` | `feat(booking): create bookings with server prices and seat validation` |
| 7.6 | `2be8b69` | `feat(booking): compute public seat availability` |
| 8.1 | `e8adfa3` | `feat(booking): list customer history sorted by booking date` |
| 8.2 | `841ecc8` | `feat(booking): restrict booking details to owner or admin` |
| 8.3 | `f3c5773` | `feat(booking): cancel bookings with ownership and two hour deadline` |
| 9.2 | `43dfd2c` | `feat(booking): aggregate confirmed revenue sorted descending` |
| 7.7 | `f14e82d` | `feat(booking): expose booking creation and public seat map` |
| 8.4 | `190a326` | `feat(booking): expose history detail cancel and admin list` |
| 9.3 | `1e50f73` | `feat(booking): expose confirmed revenue report endpoint` |
| 10.1 | `9a68103` | `chore(gateway): configure service routes and shared JWT secret` |
| 10.2 | `ee9e000` | `feat(gateway): replace client headers with verified JWT user context` |
| 10.3 | `71445d4` | `feat(gateway): route customer movie and booking APIs` |
| 10.4 | `183beae` | `feat(gateway): authorize API requests by verified JWT role` |
| 11.1 | `89ef3b2` | `test(postman): add local test environment` |
| 11.2 | `4038fac` | `test(postman): add 85 request collection and assertions` |
| 11.3 | `2173b9e` | `test(postman): add repeatable runner and redact exported tokens` |

TODO 11.3 còn có `2173b9e test(postman): add repeatable runner and redact exported tokens` và `7044f1b docs: add run guide and test accounts to README`. Có thể chụp trang Commits trên GitHub hoặc Terminal từ thư mục gốc MSS301:

```bash
git log 5e0657b --reverse --format='%h %s%n%b'
```

Sau khi thêm ảnh, commit ảnh và báo cáo theo nội dung thật, ví dụ:

```text
docs(evidence): add TODO completion screenshots

Refs: TODO 0.1-10.4
```

```text
docs(postman): add desktop runner and integration screenshots

Refs: TODO 11.3; Postman 6.15
```

Chỉ stage các file ảnh và báo cáo liên quan; không dùng `git add .` vì repo có thay đổi từ bài khác.

## 8. Điền ảnh vào Word và hoàn tất nộp bài

Mở `Assignment 1_Report.docx`. Dưới đúng heading TODO, giữ đoạn Kết quả và Commit có sẵn, sau đó Insert → Pictures → Picture from File. Đặt ảnh In Line with Text, giữ tỉ lệ, chiều rộng không vượt 6.5 inch (16.5 cm). Caption dùng Times New Roman 10–11 pt, căn giữa; văn bản thường Times New Roman 13 pt, căn đều, giãn dòng 1.3; heading dùng đúng Heading 1/2 có sẵn. Không gõ tay lại heading hay số trang mục lục.

Ví dụ dưới TODO 7.5:

> Hình TODO 7.5a. Postman request 6.2 tạo booking qua Gateway, trả 201; server tính tổng 190.000 đồng cho E5 và E6.
>
> Hình TODO 7.5b. Postman request 6.3 đặt lại E5 của cùng suất chiếu trả 409; assertion Status 409 PASS.

Sau ảnh viết 1–2 câu giải thích điều được chứng minh; dùng số thật trong response của bạn. Đừng chỉ chèn ảnh không chú thích. Với ảnh lấy từ Runner, ghi “Kết quả request … trong Collection Runner”.

Sau khi chèn đủ ảnh: bấm mục lục → Update Table/Update Field → Update entire table. Nếu dùng Word Mac không thấy menu, chọn mục lục rồi Fn+F9 hoặc Control-click → Update Field (tên/phím có thể tùy cấu hình Word). Nếu muốn mục lục liệt kê cả 54 TODO, dùng References → Table of Contents → Custom Table of Contents → Show levels = 2; sau đó Update entire table. Mục lục sẽ dài hơn và số trang thay đổi.

Kiểm tra ở Print Layout: ảnh/commit không bị cắt, caption cùng trang với ảnh, header/footer đúng tên, tất cả mục lục nhảy đúng heading và số trang đã cập nhật. Lưu bản chuẩn `fu-cinema/docs/Assignment 1_Report.docx`, đồng bộ bản ngoài `Assignment 1/Assignment 1_Report.docx` và tạo lại ZIP từ bản mới. Chèn ảnh Runner Desktop vào README khi đã chụp được; giữ ảnh Newman được ghi đúng nguồn.

Export collection và environment theo Guide nếu bạn đã thay đổi chúng. Giữ bản environment nộp bài có token để trống; script login tự tạo lại. Test tay 6.15 có thể export collection riêng kèm ảnh. Nộp Word, source, collection/environment JSON và ảnh thực tế; ZIP trước khi bổ sung ảnh chưa phải bản nộp cuối cùng theo yêu cầu ảnh của giảng viên.

Sau khi chụp xong có thể dừng riêng bài này:

```bash
./scripts/stop.sh
docker compose -p fu-cinema stop
```

## Tài liệu đối chiếu

- `Assignment1.md`, mục 6 (54 TODO), mục 7 (checklist).
- `Assignment1_Guide.md`, mục 7.1–7.3 (Postman), ca 6.15 (BR14), mục 9.4 (commit).
- [Postman Collection Runner](https://learning.postman.com/docs/tests-and-scripts/running-collections/intro-to-collection-runs): cách cấu hình run, giữ response, xem từng request và lưu biến. Giao diện có thể khác giữa các phiên bản; collection dự án đã chứa sẵn scripts, không cần gõ lại.
