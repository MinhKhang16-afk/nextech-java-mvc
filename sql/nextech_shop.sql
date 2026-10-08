-- NEXTECH SHOP: Cơ sở dữ liệu thương mại điện tử (mẫu demo)
-- Chuyển đổi cấu trúc hợp lý từ mẫu 10 bảng trà sữa: bỏ topping/size, thêm tồn kho/thông số
-- Không DROP DATABASE để tránh xóa dữ liệu hiện có.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS danh_muc (
  danh_muc_id INT AUTO_INCREMENT PRIMARY KEY,
  ma_danh_muc VARCHAR(30) NOT NULL UNIQUE,
  ten_danh_muc VARCHAR(100) NOT NULL,
  mo_ta VARCHAR(255),
  thu_tu INT NOT NULL DEFAULT 0,
  trang_thai TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS san_pham (
  san_pham_id INT AUTO_INCREMENT PRIMARY KEY,
  ma_san_pham VARCHAR(30) NOT NULL UNIQUE,
  ten_san_pham VARCHAR(150) NOT NULL,
  danh_muc_id INT NOT NULL,
  mo_ta TEXT,
  thong_so TEXT,
  hinh_anh VARCHAR(255),
  gia_ban DECIMAL(12,0) NOT NULL,
  so_luong_ton INT NOT NULL DEFAULT 0,
  noi_bat TINYINT(1) NOT NULL DEFAULT 0,
  trang_thai TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_sp_dm FOREIGN KEY(danh_muc_id) REFERENCES danh_muc(danh_muc_id),
  INDEX idx_sp_danh_muc(danh_muc_id),
  INDEX idx_sp_trang_thai(trang_thai)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS khach_hang (
  khach_hang_id INT AUTO_INCREMENT PRIMARY KEY,
  ho_ten VARCHAR(120) NOT NULL,
  so_dien_thoai VARCHAR(20) NOT NULL,
  email VARCHAR(100),
  dia_chi VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS don_hang (
  don_hang_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  ma_don_hang VARCHAR(40) NOT NULL UNIQUE,
  khach_hang_id INT NOT NULL,
  ten_nguoi_nhan VARCHAR(120) NOT NULL,
  sdt_nguoi_nhan VARCHAR(20) NOT NULL,
  dia_chi_giao_hang VARCHAR(255) NOT NULL,
  ghi_chu VARCHAR(500),
  tam_tinh DECIMAL(12,0) NOT NULL DEFAULT 0,
  phi_giao_hang DECIMAL(12,0) NOT NULL DEFAULT 0,
  tong_tien DECIMAL(12,0) NOT NULL DEFAULT 0,
  phuong_thuc_thanh_toan ENUM('COD') NOT NULL DEFAULT 'COD',
  trang_thai ENUM('CHO_XAC_NHAN','DA_XAC_NHAN','DANG_GIAO','HOAN_TAT','DA_HUY') NOT NULL DEFAULT 'CHO_XAC_NHAN',
  thoi_gian_dat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_dh_kh FOREIGN KEY(khach_hang_id) REFERENCES khach_hang(khach_hang_id),
  INDEX idx_dh_thoi_gian(thoi_gian_dat),
  INDEX idx_dh_trang_thai(trang_thai)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chi_tiet_don_hang (
  chi_tiet_don_hang_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  don_hang_id BIGINT NOT NULL,
  san_pham_id INT NOT NULL,
  ten_san_pham VARCHAR(150) NOT NULL,
  so_luong INT NOT NULL,
  don_gia DECIMAL(12,0) NOT NULL,
  thanh_tien DECIMAL(12,0) NOT NULL,
  CONSTRAINT fk_ct_dh FOREIGN KEY(don_hang_id) REFERENCES don_hang(don_hang_id),
  CONSTRAINT fk_ct_sp FOREIGN KEY(san_pham_id) REFERENCES san_pham(san_pham_id),
  INDEX idx_ct_dh(don_hang_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO danh_muc (danh_muc_id, ma_danh_muc, ten_danh_muc, mo_ta, thu_tu) VALUES
(1,'DIEN_THOAI','Điện thoại','Smartphone nhiều phân khúc',1),
(2,'LAPTOP','Laptop','Máy tính xách tay học tập và làm việc',2),
(3,'CU_SAC','Củ sạc','Bộ sạc nhanh và sạc GaN',3),
(4,'DAY_CAP','Dây cáp','Dây sạc, cáp dữ liệu',4),
(5,'USB','USB & Lưu trữ','USB và ổ cứng gắn ngoài',5),
(6,'PHU_KIEN','Phụ kiện','Tai nghe, pin dự phòng và phụ kiện khác',6);

-- Dữ liệu sản phẩm minh họa, không phải bảng giá/hình chụp của nhãn hàng thật.
INSERT IGNORE INTO san_pham
(ma_san_pham,ten_san_pham,danh_muc_id,mo_ta,thong_so,hinh_anh,gia_ban,so_luong_ton,noi_bat)
VALUES
('DT001','Điện thoại Nova X6 128GB',1,'Thiết kế trẻ trung, màn hình mượt và pin dùng cả ngày.','Màn hình: 6.5 inch\nBộ nhớ: 128GB\nRAM: 8GB\nPin: 5000mAh','assets/img/products/DT001.svg',5990000,14,1),
('DT002','Điện thoại Orion 5G 256GB',1,'Kết nối 5G tốc độ cao, chụp ảnh rõ nét.','Màn hình: 6.7 inch\nBộ nhớ: 256GB\nRAM: 8GB\nMạng: 5G','assets/img/products/DT002.svg',8990000,9,1),
('DT003','Điện thoại Zenith Pro 256GB',1,'Hiệu năng mạnh mẽ cho làm việc và giải trí.','Màn hình: OLED 6.7 inch\nBộ nhớ: 256GB\nRAM: 12GB\nSạc: USB-C','assets/img/products/DT003.svg',12490000,6,1),
('DT004','Điện thoại Mini One 128GB',1,'Gọn nhẹ, tiện lợi cho học sinh sinh viên.','Màn hình: 6.1 inch\nBộ nhớ: 128GB\nRAM: 6GB\nPin: 4500mAh','assets/img/products/DT004.svg',4290000,21,0),
('LT001','Laptop StudyBook 14',2,'Laptop 14 inch tối ưu học tập, văn phòng và lập trình.','CPU: Core i5\nRAM: 16GB\nSSD: 512GB\nMàn hình: 14 inch FHD','assets/img/products/LT001.svg',14990000,8,1),
('LT002','Laptop AirLite 13',2,'Thiết kế gọn nhẹ, pin bền bỉ khi di chuyển.','CPU: Ryzen 5\nRAM: 16GB\nSSD: 512GB\nMàn hình: 13.3 inch','assets/img/products/LT002.svg',13490000,5,1),
('LT003','Laptop Creator 15',2,'Không gian hiển thị lớn cho sáng tạo và thiết kế.','CPU: Core i7\nRAM: 16GB\nSSD: 1TB\nMàn hình: 15.6 inch','assets/img/products/LT003.svg',18990000,4,0),
('SC001','Củ sạc nhanh USB-C PD 20W',3,'Bộ sạc nhỏ gọn tương thích thiết bị hỗ trợ PD.','Công suất: 20W\nCổng: USB-C\nNguồn vào: 100-240V','assets/img/products/SC001.svg',199000,55,1),
('SC002','Củ sạc GaN 65W 3 cổng',3,'Một củ sạc cho điện thoại, tablet và laptop tương thích.','Công suất tối đa: 65W\nCổng: 2 USB-C + 1 USB-A','assets/img/products/SC002.svg',499000,24,1),
('SC003','Củ sạc nhanh 35W hai cổng',3,'Sạc hai thiết bị linh hoạt khi đi học hoặc đi làm.','Công suất: 35W\nCổng: USB-C + USB-A','assets/img/products/SC003.svg',329000,40,0),
('CAP001','Dây cáp USB-C to USB-C 1m',4,'Cáp sạc USB-C và truyền dữ liệu tiện dụng.','Chiều dài: 1m\nChuẩn đầu nối: USB-C / USB-C','assets/img/products/CAP001.svg',89000,130,1),
('CAP002','Dây cáp USB-C to Lightning 1m',4,'Dây cáp cho thiết bị có cổng Lightning tương thích.','Chiều dài: 1m\nĐầu nối: USB-C / Lightning','assets/img/products/CAP002.svg',149000,77,0),
('CAP003','Dây cáp USB-A to USB-C 2m',4,'Cáp dài 2m phù hợp dùng ở bàn làm việc.','Chiều dài: 2m\nĐầu nối: USB-A / USB-C','assets/img/products/CAP003.svg',99000,95,0),
('USB001','USB Flash 3.2 64GB',5,'Lưu trữ tài liệu và chuyển tập tin nhanh gọn.','Dung lượng: 64GB\nGiao tiếp: USB 3.2','assets/img/products/USB001.svg',129000,65,1),
('USB002','USB Dual USB-C 128GB',5,'USB hai đầu dùng thuận tiện trên nhiều thiết bị.','Dung lượng: 128GB\nCổng: USB-A / USB-C','assets/img/products/USB002.svg',249000,32,0),
('USB003','SSD di động Pocket 512GB',5,'Ổ SSD di động nhỏ gọn cho dữ liệu và dự án.','Dung lượng: 512GB\nGiao tiếp: USB-C\nThiết kế: Di động','assets/img/products/USB003.svg',899000,16,0),
('PK001','Tai nghe không dây Wave Buds',6,'Tai nghe Bluetooth gọn nhẹ dùng hằng ngày.','Kết nối: Bluetooth\nHộp sạc: USB-C','assets/img/products/PK001.svg',590000,28,1),
('PK002','Giá đỡ laptop nhôm gấp gọn',6,'Nâng laptop lên tầm mắt, tối ưu góc làm việc.','Chất liệu: Nhôm\nKiểu dáng: Gấp gọn','assets/img/products/PK002.svg',259000,43,0),
('PK003','Pin dự phòng 10000mAh',6,'Nguồn điện dự phòng hữu ích khi di chuyển.','Dung lượng: 10000mAh\nCổng: USB-C + USB-A','assets/img/products/PK003.svg',349000,60,1),
('PK004','Hub USB-C đa năng 5 trong 1',6,'Mở rộng kết nối cho laptop có USB-C tương thích.','Cổng: HDMI, USB-A, USB-C\nChuẩn: Hub 5 trong 1','assets/img/products/PK004.svg',439000,18,0);

CREATE OR REPLACE VIEW v_san_pham AS
SELECT sp.san_pham_id, sp.ma_san_pham, sp.ten_san_pham, dm.ten_danh_muc,
       sp.gia_ban, sp.so_luong_ton, sp.hinh_anh, sp.trang_thai
FROM san_pham sp JOIN danh_muc dm ON sp.danh_muc_id=dm.danh_muc_id;

CREATE OR REPLACE VIEW v_don_hang AS
SELECT dh.don_hang_id, dh.ma_don_hang, dh.ten_nguoi_nhan,
       dh.sdt_nguoi_nhan, dh.dia_chi_giao_hang, dh.tong_tien,
       dh.trang_thai, dh.thoi_gian_dat
FROM don_hang dh;
