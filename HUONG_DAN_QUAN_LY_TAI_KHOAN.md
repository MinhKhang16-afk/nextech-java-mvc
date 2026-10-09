# Thêm quản lý tài khoản khách hàng cho NexTech

Bản vá dùng cho dự án NexTech Java Spring Boot đã áp dụng `nextech-customer-login-patch.zip` trước đó.

## Chức năng

- Admin đăng nhập ở `/dang-nhap`, vào `/quan-tri/tai-khoan`.
- Danh sách tài khoản, họ tên, email, số điện thoại, thời gian đăng ký, trạng thái.
- Tìm theo tên/email/số điện thoại và khóa/mở khóa tài khoản (gửi POST với CSRF).
- **Không hiển thị mật khẩu**, không xóa tài khoản, không sửa đơn hàng.
- Cột `trang_thai` đã tồn tại trong `sql/upgrade_customer_accounts.sql`; **không cần chạy SQL mới**.

## Cài đặt trên GitHub Codespaces

1. Đảm bảo đã commit/push các thay đổi trước đây và sao lưu database.
2. Upload file `nextech-admin-account-patch.zip` vào **thư mục gốc** `/workspaces/nextech-java-mvc/`.
3. Trong Terminal:

```bash
cd /workspaces/nextech-java-mvc
unzip -o nextech-admin-account-patch.zip
docker compose up -d --build web
docker compose ps
```

4. Đăng nhập admin ở `/dang-nhap`, chọn menu **Quản lý tài khoản**.
5. Tạo thử một tài khoản ở `/tai-khoan/dang-ky`, trở lại Admin để xem, tìm kiếm, khóa và mở khóa.

## Giới hạn

- Việc khóa tài khoản ngăn những **lần đăng nhập mới** qua `UserDetailsService` (trạng thái `disabled`); session của khách đã đăng nhập có thể còn hợp lệ cho đến khi đăng xuất/hết hạn.
- Chỉ hiển thị tối đa 200 tài khoản theo ID giảm dần; đây là giới hạn giao diện demo, không phải phân trang chuẩn.
- Không thay đổi `compose.yaml`, `SecurityConfig.java`, `home.html`, `products.html`, các bảng đơn hàng hay mật khẩu admin.

## Lưu mã nguồn

```bash
rm -f nextech-admin-account-patch.zip
git status
git add src/main/java/vn/nextech/controller/AdminCustomerController.java src/main/java/vn/nextech/model/AdminCustomerRow.java src/main/java/vn/nextech/repository/AdminCustomerRepository.java src/main/resources/templates/admin/customers.html src/main/resources/templates/admin/nav.html HUONG_DAN_QUAN_LY_TAI_KHOAN.md
git commit -m "Add customer account management for admin"
git push origin main
```

Giữ `.env` và dữ liệu sao lưu chứa khách hàng ở ngoài GitHub công khai.
