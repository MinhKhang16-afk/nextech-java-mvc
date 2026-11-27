-- NEXTECH: lich su mua hang, tracking, avatar va dat lai mat khau.
-- Chi chay mot lan tren database da co tai_khoan va don_hang.
-- Sao luu database truoc khi chay. Script khong xoa bang hay du lieu.
ALTER TABLE tai_khoan
    ADD COLUMN avatar_path VARCHAR(255) NULL;

ALTER TABLE don_hang
    ADD COLUMN tai_khoan_id BIGINT NULL AFTER khach_hang_id,
    ADD INDEX idx_don_hang_tai_khoan (tai_khoan_id),
    ADD CONSTRAINT fk_don_hang_tai_khoan
        FOREIGN KEY (tai_khoan_id) REFERENCES tai_khoan(tai_khoan_id)
        ON DELETE SET NULL;

-- Gắn các đơn cũ vào tài khoản khi email người nhận trùng email đăng ký.
UPDATE don_hang dh
JOIN khach_hang kh ON kh.khach_hang_id = dh.khach_hang_id
JOIN tai_khoan tk ON LOWER(TRIM(tk.email)) = LOWER(TRIM(kh.email))
SET dh.tai_khoan_id = tk.tai_khoan_id
WHERE dh.tai_khoan_id IS NULL
  AND kh.email IS NOT NULL
  AND TRIM(kh.email) <> '';

CREATE TABLE IF NOT EXISTS don_hang_lich_su (
    lich_su_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    don_hang_id BIGINT NOT NULL,
    trang_thai VARCHAR(30) NOT NULL,
    ghi_chu VARCHAR(500) NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lich_su_don_hang FOREIGN KEY (don_hang_id)
        REFERENCES don_hang(don_hang_id) ON DELETE CASCADE,
    INDEX idx_lich_su_don_hang_time (don_hang_id, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tai_khoan_password_reset (
    reset_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tai_khoan_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_account FOREIGN KEY (tai_khoan_id)
        REFERENCES tai_khoan(tai_khoan_id) ON DELETE CASCADE,
    INDEX idx_password_reset_account_time (tai_khoan_id, created_at),
    INDEX idx_password_reset_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
