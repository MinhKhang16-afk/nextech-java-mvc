-- Chạy một lần trên MySQL hiện tại (volume đã có dữ liệu).
-- Không DROP bảng, không thay đổi các đơn hàng cũ.
CREATE TABLE IF NOT EXISTS tai_khoan (
  tai_khoan_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  ho_ten VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL,
  so_dien_thoai VARCHAR(20) NOT NULL DEFAULT '',
  mat_khau_hash VARCHAR(100) NOT NULL,
  trang_thai TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_tai_khoan_email(email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
