-- NexTech: them thanh toan chuyen khoan
-- Khong xoa du lieu hien tai

ALTER TABLE don_hang
MODIFY COLUMN phuong_thuc_thanh_toan
ENUM('COD', 'CHUYEN_KHOAN')
NOT NULL DEFAULT 'COD';

ALTER TABLE don_hang
ADD COLUMN trang_thai_thanh_toan
ENUM('CHUA_THANH_TOAN', 'CHO_KIEM_TRA', 'DA_THANH_TOAN')
NOT NULL DEFAULT 'CHUA_THANH_TOAN';

ALTER TABLE don_hang
ADD COLUMN thoi_gian_thanh_toan DATETIME NULL;

ALTER TABLE don_hang
ADD COLUMN ma_giao_dich_ngan_hang VARCHAR(100) NULL;
