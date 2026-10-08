# NEXTECH — Website bán điện thoại và phụ kiện bằng **JAVA MVC + MySQL**

**Đây là bản JAVA hoàn toàn mới, KHÔNG phải bản PHP.** Dành cho người mới học lập trình web, triển khai bằng **Java 21, Spring Boot 3.4.5, Spring MVC, Thymeleaf, Spring JDBC, MySQL 8, Spring Security**, chạy trong **GitHub Codespaces + Docker Compose**.

> Bộ dữ liệu và ảnh là ví dụ phục vụ bài tập. Tài khoản quản trị và mật khẩu có trong `.env.example` chỉ dành cho môi trường demo; **phải đổi trước khi đặt cổng Codespaces ở chế độ Public**. Không dùng để kinh doanh khi chưa bổ sung bảo mật và vận hành thực tế.

## 0. Sửa nhầm lẫn với bản trước

Bạn đang mở repo `nextech-mvc` với `app/Controllers/*.php` và Dockerfile PHP. **Không thể chỉ thay `docker compose ...` rồi ứng dụng tự trở thành Java**. Cần đưa mã nguồn Java mới vào repo. Đơn giản và ít lỗi nhất là **tạo repository mới `nextech-java-mvc`** thay vì đổ file Java lẫn với file PHP. Giữ repo PHP cũ để đối chiếu hoặc xóa sau.

## 1. Kết quả sau khi chạy

- Website: `http://localhost:8080/` (trong Codespaces truy cập link chuyển tiếp cổng 8080)
- Danh sách: `/san-pham` (lọc theo danh mục, tìm kiếm, sắp xếp)
- Chi tiết: `/san-pham/1`
- Giỏ hàng: `/gio-hang`
- Đặt hàng: `/dat-hang`
- Đăng nhập quản trị: `/dang-nhap`
- Tiếp nhận đơn: `/quan-tri`
- Quản lý sản phẩm: `/quan-tri/san-pham`

Các danh mục: **Điện thoại, Laptop, Củ sạc, Dây cáp, USB & Lưu trữ, Phụ kiện**. Có **20 sản phẩm mẫu** kèm hình ảnh minh họa `.svg` nằm trong chính dự án.

## 2. Mô hình MVC

```text
Browser -> Controller (Spring MVC)
               |               \
            Service          View (Thymeleaf HTML/CSS)
               |
         Repository (Spring JDBC)
               |
           MySQL 8
```

```text
nextech-java-mvc/
├── .devcontainer/devcontainer.json     # cấu hình GitHub Codespaces
├── .env.example                        # mẫu mật khẩu (KHÔNG commit .env)
├── compose.yaml                        # 2 container: web + db
├── Dockerfile                          # build Java bằng Maven
├── pom.xml                             # thư viện Spring Boot
├── sql/nextech_shop.sql                # tạo DB bảng + 20 sản phẩm mẫu
├── src/main/java/vn/nextech/
│   ├── NextechApplication.java         # entrypoint Java
│   ├── config/SecurityConfig.java      # tài khoản quản trị
│   ├── controller/                      # điều hướng URL
│   ├── model/                           # đối tượng dữ liệu
│   ├── repository/                      # SQL sử dụng JdbcTemplate
│   └── service/                         # xử lý giỏ hàng, đặt hàng, ảnh
└── src/main/resources/
    ├── application.properties
    ├── templates/                       # Thymeleaf HTML + admin
    └── static/assets/                   # CSS + 20 ảnh SVG
```

## 3. Cách dễ nhất để đưa dự án vào GitHub (không lỗi mất folder)

**Khuyến nghị: tạo repo Java riêng.**

1. Vào <https://github.com/new>, tạo repository **`nextech-java-mvc`** (Private, không tạo sẵn README cũng được).
2. Mở repo vừa tạo, bấm **Code → Codespaces → Create codespace on main**. Nếu repo trống chưa có nhánh `main`, tạo README trên giao diện GitHub trước để có nhánh.
3. Trong cửa sổ VS Code Codespaces, nhấp phải thư mục gốc phía Explorer và chọn **Upload...** (hoặc kéo thả **`nextech-java-mvc.zip`** từ máy vào Explorer).
4. Đưa nguyên file ZIP lên thư mục gốc. Sau đó mở Terminal và chạy:

```bash
unzip -o nextech-java-mvc.zip
ls -la
```

Bạn phải thấy **`pom.xml`, `compose.yaml`, `Dockerfile`, `.env.example`, `.devcontainer/`, `src/`, `sql/`** ở ngay thư mục làm việc gốc. Nếu sau giải nén chúng nằm trong thư mục con `nextech-java-mvc/`, dùng `cd nextech-java-mvc` trước các lệnh dưới.

5. Xóa file ZIP ở Codespaces và push mã nguồn lên GitHub:

```bash
rm nextech-java-mvc.zip
git add -A
git commit -m "Add Java Spring MVC NexTech shop"
git push
```

Nếu bạn upload từ GitHub web, **phải giữ nguyên folder**. Không upload toàn bộ các `.java` thành file ngoài cùng root.

## 4. Khởi tạo môi trường và chạy website trên GitHub Codespaces

Trong Terminal, tại thư mục chứa `pom.xml`:

**Bước A — Tạo tệp cấu hình**

```bash
cp .env.example .env
```

Mở `.env` và sửa mật khẩu quản trị và MySQL thành mật khẩu riêng của bạn **trước khi chia sẻ demo**. Codespaces thường đã có `docker` và Docker Compose.

**Bước B — Khởi động MySQL và Java:**

```bash
docker compose up -d --build
```

Lần đầu Docker tải image Maven, Java và MySQL, nên thời gian có thể mất vài phút. Nếu terminal báo lỗi, xem `docker compose logs --tail=100 web db`.

**Bước C — Kiểm tra:**

```bash
docker compose ps
docker compose logs -f web
```

Khi thấy log `Started NextechApplication` và container `web` / `db` đang chạy, nhấn **Ports** ở đáy Codespaces → cổng **8080** → **Open in Browser**. Với Codespaces, *không* dùng `localhost` trên laptop của bạn để vào service trên đám mây trừ khi cổng được forward về local.

**Tài khoản quản trị demo:** Username: `admin`; Password ban đầu: `ChangeMeBeforeSharing123` (đọc hoặc chỉnh biến `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD` trong `.env`). **Không để mật khẩu demo khi công khai demo**.

## 5. Test đơn hàng từ website đến MySQL

1. Mở trang chủ → **Sản phẩm** → chọn ví dụ **Củ sạc GaN 65W** → Thêm vào giỏ hàng.
2. Mở **Giỏ hàng**, chỉnh số lượng, bấm **Tiến hành đặt hàng**.
3. Điền họ tên, số điện thoại, email, địa chỉ. Bấm **Xác nhận đặt hàng**.
4. Nếu thành công, web hiện mã đơn `DH...` và ghi 3 nhóm dữ liệu MySQL: `khach_hang`, `don_hang`, `chi_tiet_don_hang`. Đơn ở trạng thái `CHO_XAC_NHAN`, sản phẩm được trừ tồn.
5. Đăng nhập `/dang-nhap` → `/quan-tri` để xem đơn và chuyển **Chờ xác nhận → Đã xác nhận → Đang giao → Hoàn tất**. Đơn chưa giao có thể hủy, số hàng hoàn lại kho.

Mở Terminal kiểm tra trực tiếp:

```bash
docker compose exec db sh -lc 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "SELECT ma_don_hang,ten_nguoi_nhan,tong_tien,trang_thai FROM don_hang ORDER BY don_hang_id DESC LIMIT 10;"'
```

```bash
docker compose exec db sh -lc 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "SELECT * FROM chi_tiet_don_hang ORDER BY chi_tiet_don_hang_id DESC LIMIT 10;"'
```

```bash
docker compose exec db sh -lc 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "SELECT ma_san_pham,ten_san_pham,so_luong_ton FROM san_pham LIMIT 20;"'
```

## 6. Thêm ảnh sản phẩm, thêm và sửa sản phẩm

Đăng nhập **`/quan-tri`** → **Quản lý sản phẩm** → **Thêm sản phẩm** hoặc **Chỉnh sửa**. Điền mã, tên, danh mục, giá, tồn kho, mô tả, thông số. Ảnh tải lên chỉ chấp nhận JPEG/PNG và được xử lý lại thành PNG, lưu trong Docker volume `upload_data` (ảnh minh họa đi kèm đã nằm tại `src/main/resources/static/assets/img/products/`). Hình sản phẩm truy cập qua `/uploads/<UUID>.png`.

Để thêm dữ liệu mẫu bằng SQL, sửa `sql/nextech_shop.sql` *trước lần tạo DB đầu tiên*, hoặc thực hiện câu lệnh INSERT trực tiếp trong MySQL. File init của Docker chỉ chạy khi volume MySQL còn trống; khởi động lại container **không chạy lại seed**.

## 7. Những lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| `unknown flag: --buil` | Gõ đầy đủ `docker compose up -d --build` (`build` có **d** cuối) |
| `pom.xml` không thấy | Chưa giải nén đúng thư mục gốc: chạy `pwd` và `ls -la` |
| `8080` không mở | Kiểm tra `docker compose ps`, `docker compose logs --tail=100 web` và tab **Ports** |
| MySQL kết nối thất bại | Kiểm tra db container, `.env`, log db; chờ healthcheck xong |
| `Access denied for user` | Mật khẩu MySQL trong volume cũ khác `.env` hiện tại. Tránh `down -v` nếu có dữ liệu cần giữ; khôi phục đúng mật khẩu / backup trước. |
| 403 khi gửi form | CSRF token hết hạn, tải lại trang rồi thử tiếp. |
| Đăng nhập quản trị thất bại | Kiểm tra `.env` và `docker compose up -d --force-recreate web` |
| `No space left on device` | Kiểm tra dung lượng Codespaces và dọn các image không dùng. **Không xóa volume MySQL nếu chưa backup**. |

**Dừng service, vẫn giữ data:**

```bash
docker compose down
```

**Xem nhật ký:**

```bash
docker compose logs --tail=100 web db
```

**Lưu ý quan trọng:** `docker compose down -v` **xóa cả dữ liệu MySQL và ảnh upload** của dự án này. Chỉ sử dụng khi muốn reset toàn bộ lab và không cần dữ liệu cũ.

## 8. Triển khai ngoài GitHub Codespaces

- GitHub repository chứa code, **không chứa server web đang chạy**.
- GitHub Pages **không chạy Java Spring Boot hay MySQL**.
- Codespaces cho phép mở cổng 8080 để demo trong phiên phát triển, **không phải hosting 24/7**.
- Khi muốn dùng lâu dài: deploy Java container và MySQL trên máy chủ/VPS hoặc dịch vụ hosting hỗ trợ Docker/MySQL. Đừng đưa `.env` hoặc mật khẩu lên repo.

## 9. Bảo mật và giới hạn của demo

- Đã sử dụng Spring Security cho trang admin, BCrypt cho mật khẩu quản trị, CSRF token, Prepared Statements qua JdbcTemplate, hạn chế loại file và định dạng lại ảnh, kiểm tra đơn trên server và dùng giao dịch cho ghi đơn/trừ tồn.
- Chưa có tài khoản khách đăng nhập, theo dõi giao hàng công khai, thanh toán thực, email, giao hàng tự động, backup hoặc chính sách bảo vệ dữ liệu cá nhân. Cần bổ sung trước khi bán hàng thật.

## 10. Chạy không qua Docker (máy cá nhân có Java 21 + Maven + MySQL)

1. Tạo database `nextech_shop`, import `sql/nextech_shop.sql` qua MySQL Workbench hoặc CLI.
2. Đặt `DB_URL`, `DB_USER`, `DB_PASSWORD`, `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD` cho process Java.
3. Từ thư mục có `pom.xml`, chạy `mvn spring-boot:run`.
4. Mở `http://localhost:8080`.
