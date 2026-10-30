# NexusTech E-commerce

Website thương mại điện tử bán sản phẩm công nghệ, xây dựng bằng Java Spring Boot MVC, Thymeleaf và MySQL. Ứng dụng gồm các chức năng xem sản phẩm, giỏ hàng, đặt hàng, quản lý theo chi nhánh, vận chuyển, chat, so sánh điện thoại và quản trị.

## Công nghệ

- Java 21
- Spring Boot 4
- Spring MVC, Thymeleaf, Spring Data JPA
- MySQL
- Maven Wrapper
- WebSocket STOMP/SockJS cho chat
- Tích hợp tùy chọn: VNPay, GHN, Cloudinary, SMTP và Gemini

## Yêu cầu

- JDK 21
- MySQL 8 trở lên
- Git (nếu clone project)
- Kết nối Internet để tải dependency Maven và sử dụng các dịch vụ ngoài

Không cần cài Maven riêng vì project có Maven Wrapper (`mvnw.cmd`).

## Lấy mã nguồn

```powershell
git clone <URL_REPOSITORY>
cd <TEN_THU_MUC_PROJECT>
```

## Cấu hình database

Ứng dụng mặc định kết nối MySQL tại `localhost:3306`, database `lwtck`.

Tạo database bằng MySQL Workbench hoặc MySQL CLI:

```sql
CREATE DATABASE lwtck
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Cấu hình thông tin kết nối cho phiên PowerShell hiện tại:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/lwtck?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci"
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "<MAT_KHAU_MYSQL_CUA_BAN>"
```

Thay tên đăng nhập và mật khẩu mẫu bằng thông tin MySQL trên máy của bạn. Các biến môi trường cần được đặt trong terminal trước khi chạy ứng dụng.

### Tạo cấu trúc bảng

Hibernate đang được cấu hình với `ddl-auto=update`. Khi chạy ứng dụng lần đầu, Hibernate tạo/cập nhật cấu trúc bảng trong database đã tạo ở bước trên.

### Nạp dữ liệu mẫu (không bắt buộc)

File `src/main/data/datalwtck.sql` chứa các câu lệnh thêm dữ liệu mẫu, không phải script tạo cấu trúc bảng.

1. Chạy ứng dụng ít nhất một lần để Hibernate tạo các bảng.
2. Dừng ứng dụng.
3. Mở `src/main/data/datalwtck.sql` bằng MySQL Workbench.
4. Database đã được tạo ở bước trước, vì vậy hãy bỏ qua hoặc comment phần `CREATE DATABASE` ở đầu file; giữ câu lệnh `USE lwtck`.
5. Chọn database `lwtck` và chạy phần còn lại của script.

Chỉ nạp dữ liệu mẫu một lần trên database trống để tránh lỗi trùng khóa.

## Cấu hình dịch vụ ngoài

Ứng dụng sử dụng `application.properties` làm cấu hình mặc định. Có thể đặt biến môi trường trước khi chạy để ghi đè các giá trị cấu hình tương ứng.

### Cloudinary

Cloudinary được dùng cho hình ảnh và file đính kèm. Cần cấu hình:

```powershell
$env:CLOUDINARY_CLOUD_NAME = "<CLOUD_NAME>"
$env:CLOUDINARY_API_KEY = "<API_KEY>"
$env:CLOUDINARY_API_SECRET = "<API_SECRET>"
```

### Email SMTP

SMTP được dùng cho email xác minh/OTP. Với Gmail, hãy dùng App Password thay vì mật khẩu đăng nhập Gmail:

```powershell
$env:SPRING_MAIL_USERNAME = "<EMAIL_GUI>"
$env:SPRING_MAIL_PASSWORD = "<GMAIL_APP_PASSWORD>"
```

Host và port SMTP mặc định đang được cấu hình trong `application.properties`.

### VNPay

VNPay dùng cho thanh toán trực tuyến. Để thử nghiệm, dùng thông tin môi trường sandbox do VNPay cấp:

```powershell
$env:VNPAY_TMN_CODE = "<TMN_CODE>"
$env:VNPAY_HASH_SECRET = "<HASH_SECRET>"
$env:VNPAY_PAYMENT_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
$env:VNPAY_RETURN_URL = "http://localhost:8080/payment/vnpay-return"
$env:VNPAY_QUERY_URL = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction"
$env:VNPAY_TIMEOUT_MINUTES = "15"
```

Cấu hình URL IPN `/payment/vnpay-ipn` trong merchant sandbox. VNPay cần gọi được IPN từ bên ngoài; khi chạy trên máy cá nhân, `localhost` thường không truy cập được từ VNPay và cần một URL public phù hợp để kiểm thử callback.

### GHN

Cấu hình thông tin API GHN nếu muốn kiểm thử tạo vận đơn:

```powershell
$env:GHN_API_BASE_URL = "https://online-gateway.ghn.vn/shiip/public-api"
$env:GHN_API_TOKEN = "<GHN_TOKEN>"
$env:GHN_API_SHOP_ID = "<GHN_SHOP_ID>"
$env:GHN_API_DEFAULT_DISTRICT_ID = "<DISTRICT_ID>"
$env:GHN_API_DEFAULT_WARD_CODE = "<WARD_CODE>"
```

Không dùng thông tin GHN thật trong README hoặc commit vào repository.

### Gemini (tùy chọn)

Gemini được dùng cho chức năng chat AI trong khu vực quản trị:

```powershell
$env:GEMINI_API_KEY = "<GEMINI_API_KEY>"
$env:GEMINI_API_MODEL = "gemini-3.5-flash-lite"
```

Nếu không cấu hình API key, các chức năng không phụ thuộc Gemini vẫn có thể được sử dụng.

### Một số cấu hình tùy chọn khác

```powershell
$env:CHECKOUT_HOLD_DURATION_MINUTES = "15"
$env:CHECKOUT_EXPIRY_SCAN_MILLIS = "60000"
$env:RETURN_WINDOW_DAYS = "15"
```

Địa chỉ mặc định được lấy từ API địa điểm cấu hình trong ứng dụng. WebSocket chat sử dụng broker tích hợp của ứng dụng, không cần cài broker riêng.

## Chạy ứng dụng

Sau khi cấu hình MySQL và các thông tin bắt buộc, chạy từ thư mục gốc project:

```powershell
.\mvnw.cmd spring-boot:run
```

Mở website tại:

```text
http://localhost:8080
```

Dừng ứng dụng bằng `Ctrl+C` trong terminal đang chạy.

## Chạy kiểm thử

```powershell
.\mvnw.cmd test
```

Một số kiểm thử tích hợp có thể cần database và cấu hình ứng dụng hoạt động. Nếu kiểm thử lỗi do không kết nối được MySQL, hãy kiểm tra cấu hình database và trạng thái MySQL trước.

## Các luồng chức năng chính

- **Checkout:** kiểm tra giỏ hàng và tồn kho, tạm giữ hàng, áp dụng mã giảm giá; COD tạo đơn trực tiếp, VNPay chỉ tạo đơn sau khi xác minh thanh toán thành công.
- **Điều phối đơn:** nhân viên chi nhánh xác nhận đơn, phân công shipper và theo dõi trạng thái giao hàng.
- **Chat:** tải hội thoại/lịch sử qua HTTP; giao diện gửi tin bằng HTTP và nhận tin mới qua WebSocket.
- **So sánh sản phẩm:** chọn hai điện thoại cùng danh mục để đối chiếu thông số, giá và tình trạng tồn kho.
- **Vận chuyển:** tích hợp GHN cho việc tạo vận đơn; callback vận chuyển được nhận tại `/shipping/ghn/webhook`.
- **AI CHATBOX cho admin** tích hợp gọi api gemini để thực hiện lên truy vấn từ database.
- **Biểu đồ thống kê** giúp admin và manager có thể thống kê trực quan trông qua bảng
- **Giữ đơn đang đặt hàng trong 15p** giúp khách hàng ko phải tạo lại đơn hàng trong 15p
  

## Cấu trúc thư mục chính

```text
src/
├── main/
│   ├── java/example/com/ltw/       # Mã nguồn Java
│   ├── resources/
│   │   ├── application.properties # Cấu hình ứng dụng
│   │   ├── static/                # CSS, JavaScript, hình ảnh
│   │   └── templates/             # Giao diện Thymeleaf
│   └── data/datalwtck.sql         # Dữ liệu mẫu
└── test/                          # Kiểm thử
```

## Lưu ý bảo mật

Không lưu mật khẩu, API key, token hoặc secret thật trong Git, README hay ảnh chụp màn hình. Hãy dùng biến môi trường hoặc secret manager cho các môi trường triển khai.

Trước khi công khai repository, kiểm tra `src/main/resources/application.properties`. Nếu file có chứa thông tin xác thực thật, hãy thay mới/thu hồi các thông tin đó và chuyển cấu hình sang biến môi trường. Không dùng database mẫu hoặc tài khoản thử nghiệm trong môi trường production.

Ngoài ra, `ddl-auto=update` phù hợp cho môi trường phát triển; môi trường production nên dùng quy trình migration database được kiểm soát.
