# BTL-ChanhChiMinhHuyen

## 1. Mô tả bài toán và phạm vi hệ thống

Dự án xây dựng hệ thống đấu giá trực tuyến theo mô hình Client/Server, được triển khai thực tế trên môi trường mạng. Người dùng có thể tham gia đấu giá realtime, sử dụng các tính năng tự động hóa thông minh và theo dõi biến động giá trực quan. Admin có giao diện quản trị toàn diện hệ thống.

Hệ thống quản lý phân quyền linh hoạt giữa Người mua (Bidder), Người bán (Seller) và Admin trên cùng một nền tảng cơ sở dữ liệu.

**Phạm vi đã thực hiện:**
- Quản lý tài khoản và phân quyền (User/Admin).
- Quản lý vòng đời sản phẩm và các phiên đấu giá.
- Giao tiếp realtime qua TCP Socket với các gói tin định dạng JSON.
- **Tính năng nâng cao:** Đặt giá tự động (Auto-bidding), Gia hạn tự động khi có tranh chấp ở những giây cuối (Anti-snipe), trực quan hóa bằng Đồ thị lịch sử giá và Hệ thống review uy tín.
- Triển khai thực tế toàn bộ hệ thống Server trên máy chủ mạng ảo (VPS).
- Kiểm thử luồng nghiệp vụ cốt lõi bằng JUnit 5.

---

## 2. Công nghệ sử dụng và yêu cầu môi trường

**Công nghệ sử dụng:**
- **Ngôn ngữ:** Java 21.
- **GUI:** JavaFX.
- **Kiến trúc:** Client/Server, MVC, Service layer, DAO layer.
- **Giao tiếp mạng:** TCP Socket, JSON message (Thư viện Gson).
- **Lưu trữ:** TiDB (Distributed Relational Database).
- **Build tool:** Maven.
- **Kiểm thử:** JUnit 5.
- **CI/CD:** GitHub Actions.

**Môi trường yêu cầu:**
- JDK 21.
- Khuyến khích sử dụng IDE (IntelliJ IDEA hoặc Eclipse/VS Code).
- Máy tính cần có kết nối Internet ổn định để Client có thể giao tiếp realtime với máy chủ VPS.

---

## 3. Cấu trúc thư mục dự án

```text
BTL_OOP_Java/
|-- bidding-client/              # 💻 MODULE CLIENT (Giao diện người dùng)
|   |-- pom.xml
|   `-- src/main/java/com/uet/bidding/client/
|       |-- controller/          # Xử lý sự kiện UI JavaFX, Đồ thị & Launcher.java
|       |-- model/               # Thực thể dữ liệu: User, Auction, Item, Bid...
|       |-- network/             # Gửi/nhận gói tin JSON qua Socket tới VPS
|       |-- service/             # Logic nghiệp vụ phía Client
|       `-- util/                # Tiện ích định dạng, alert, validation
|-- bidding-server/              # 🖥️ MODULE SERVER (Core Logic & Database)
|   |-- pom.xml
|   `-- src/main/java/com/uet/bidding/server/
|       |-- server/              # Mở port, quản lý ClientHandler (Đa luồng)
|       |-- dao/                 # Database Access Object (Truy vấn SQL tới TiDB)
|       |-- service/             # Logic lõi (Auto-bid, Anti-snipe, Timer)
|       `-- model/               # Các thực thể dữ liệu phía Server
|-- .github/workflows/           # Cấu hình CI/CD tự động (GitHub Actions)
|-- .gitignore
`-- README.md
```

---

## 4. Hướng dẫn giảng viên khởi chạy ứng dụng để chấm bài

> 💡 **Lưu ý quan trọng:** Hệ thống Server và Cơ sở dữ liệu TiDB đã được nhóm triển khai hoàn thiện và cấu hình chạy liên tục (24/7) trên VPS. Do đó, **thầy cô KHÔNG CẦN phải cấu hình Database hay khởi chạy Server thủ công**.

Thầy cô chỉ cần khởi chạy giao diện Client để trải nghiệm hệ thống theo các bước sau:

**Bước 1: Mở dự án**
Mở thư mục gốc của dự án (`BTL_OOP_Java`) bằng IDE (khuyến khích dùng IntelliJ IDEA) và chờ IDE tải xong các thư viện cấu hình trong file `pom.xml`.

**Bước 2: Khởi chạy Client**
- Trong cấu trúc thư mục của IDE, thầy cô điều hướng tới file chạy chính của Client theo đường dẫn:
  `bidding-client/src/main/java/com/uet/bidding/client/controller/Launcher.java`
- Bấm nút **Run** (biểu tượng tam giác xanh) bên cạnh class `Launcher` hoặc hàm `main` để khởi chạy ứng dụng.

**Bước 3: Trải nghiệm**
- Ứng dụng sẽ tự động kết nối Socket đến Server trên VPS. Thầy cô có thể đăng ký tài khoản mới hoặc sử dụng các tài khoản test để trải nghiệm đặt giá realtime.

---

## 5. Danh sách chức năng đã hoàn thành

### Người dùng & Nghiệp vụ đấu giá:
- Đăng ký, đăng nhập bảo mật, quản lý phiên làm việc linh hoạt.
- Xem danh sách, tìm kiếm và lọc sản phẩm đấu giá theo danh mục.
- **Đặt giá Realtime:** Đồng bộ giá thầu ngay lập tức giữa tất cả client đang theo dõi phiên đấu giá qua Socket.
- **Trực quan hóa:** Vẽ đồ thị biến động lịch sử đặt giá của từng sản phẩm.
- **Đánh giá hệ thống:** Người mua có thể gửi review, chấm điểm uy tín cho người bán sau khi phiên đấu giá kết thúc thành công.

### Tính năng tự động hóa & Xử lý phía Server:
- **Đặt giá tự động (Auto-bidding):** Hệ thống thay mặt người dùng tự động ra giá trong giới hạn đã cấu hình. Xử lý tốt bài toán tranh chấp đa luồng (Concurrency).
- **Gia hạn tự động (Anti-snipe):** Tự động cộng thêm thời gian vào phiên đấu giá nếu có lượt đặt giá ở những giây cuối cùng để đảm bảo sự công bằng.
- **Background Task:** Hệ thống Server tự động giám sát, thay đổi trạng thái và đóng các phiên đấu giá một cách chính xác khi hết giờ.
- Kiến trúc Multi-threading hỗ trợ xử lý đồng thời nhiều Client kết nối.

### Thiết kế hướng đối tượng (OOP Design):
- Áp dụng triệt để các tính chất cốt lõi: Đóng gói, Kế thừa, Đa hình (Ví dụ: Override phương thức hiển thị theo từng danh mục sản phẩm cụ thể).
- Áp dụng Design Pattern: **Factory Pattern** để khởi tạo linh hoạt các danh mục sản phẩm, **Singleton Pattern** để quản lý các dịch vụ Server toàn cục.
- Phân tách layer độc lập, rõ ràng (MVC, Service, DAO), dễ dàng bảo trì và mở rộng.

### Kiểm thử hệ thống:
- Luồng nghiệp vụ cốt lõi đã được bao phủ bởi các test case sử dụng JUnit 5.
- Thầy cô có thể chạy tự động toàn bộ Unit Test thông qua tính năng Run Test có sẵn trên IDE tại thư mục `test` của mỗi module.

---

## 6. Báo cáo chi tiết và Video Demo

- **Báo cáo PDF:** [Điền link Google Drive của nhóm bạn vào đây]
- **Video demo tính năng:** [Điền link video Demo của nhóm bạn vào đây]