# BỔ SUNG TÀI KHOẢN KHÁCH HÀNG — NEXTECH

Bản vá này áp dụng cho dự án **Java Spring Boot MVC + MySQL** hiện có trong GitHub Codespaces.

## Có gì mới?
- `/tai-khoan/dang-ky`: Đăng ký bằng họ tên, email, số điện thoại tùy chọn, mật khẩu, xác nhận mật khẩu.
- `/tai-khoan/dang-nhap`: Đăng nhập khách hàng bằng email và mật khẩu.
- `/tai-khoan`: Trang thông tin tài khoản sau đăng nhập.
- `/dang-xuat` (POST): Đăng xuất qua Spring Security.
- Vẫn giữ `/dang-nhap` cho ADMIN, `/quan-tri` dành riêng cho ADMIN.
- Bảng `tai_khoan` riêng, mật khẩu băm BCrypt; không thay đổi các bảng đơn hàng cũ.
- Khi khách hàng đăng nhập rồi đặt hàng, form thanh toán tự điền tên, email, số điện thoại (nếu có).

## Cách áp dụng trên Codespaces (không xóa dữ liệu)

1. **Sao lưu** mã nguồn và database. Đảm bảo source đã `git push`. Nếu cần, xuất DB bằng `mysqldump` và lưu file ngoài GitHub.
2. Trong VS Code Codespaces, upload `nextech-customer-login-patch.zip` vào **thư mục gốc `/workspaces/nextech-java-mvc`**, nơi có `pom.xml`.
3. Trên Terminal:

```bash
cd /workspaces/nextech-java-mvc
unzip -o nextech-customer-login-patch.zip
```

4. **Rất quan trọng:** Cơ sở dữ liệu MySQL hiện tại có volume rồi, nên thêm file SQL vào thư mục `sql/` sẽ **KHÔNG tự chạy lại**. Hãy chạy lệnh tạo bảng dưới đây **một lần** (an toàn khi chạy lại vì `IF NOT EXISTS`):

```bash
docker compose up -d db
docker compose exec -T db sh -lc 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' < sql/upgrade_customer_accounts.sql
```

5. Build lại Java và khởi động:

```bash
docker compose up -d --build web
docker compose ps
```

6. Mở Ports → 8080 → Open in Browser, kiểm tra lần lượt `/tai-khoan/dang-ky`, `/tai-khoan/dang-nhap`, `/tai-khoan`.
7. Đăng nhập thử admin qua `/dang-nhap` để xác nhận khu quản trị cũ không bị ảnh hưởng.

## Kiểm tra dữ liệu đăng ký

```bash
docker compose exec db sh -lc 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "SELECT tai_khoan_id, ho_ten, email, created_at FROM tai_khoan ORDER BY tai_khoan_id DESC LIMIT 10;"'
```

Không SELECT hoặc công khai trường `mat_khau_hash`.

## Lưu mã nguồn

```bash
git status
git add src/main/java/vn/nextech src/main/resources/templates/customer src/main/resources/templates/fragments.html src/main/resources/static/assets/css/style.css sql/upgrade_customer_accounts.sql
# Kiểm tra trước khi commit; không commit .env hay bản sao lưu DB.
git commit -m "Add customer registration and login"
git push origin main
```

## Lưu ý
- Bản vá **không sửa `compose.yaml`, `home.html` hoặc `products.html`** vì bạn đã chỉnh các file đó để chạy trên Codespaces.
- Tài khoản khách dùng bảng `tai_khoan`, khác `khach_hang` (thông tin người nhận đơn). Chưa có lịch sử đơn gắn với tài khoản.
- Các tài khoản mới chưa được xác minh email, chưa có quên/đặt lại mật khẩu, rate limiting hay MFA: phù hợp bài thực hành, **chưa đủ bảo mật để triển khai thương mại thực tế**.
- Không chạy `docker compose down -v`.
