-- NexTech Admin Analytics: bổ sung ngày sinh cho tài khoản khách hàng.
-- Chạy MỘT LẦN sau khi đã sao lưu database.
-- Câu lệnh chỉ thêm cột nullable, không cập nhật/xóa dữ liệu cũ.
ALTER TABLE tai_khoan
    ADD COLUMN ngay_sinh DATE NULL AFTER so_dien_thoai;

CREATE INDEX idx_tai_khoan_ngay_sinh
    ON tai_khoan (ngay_sinh);
