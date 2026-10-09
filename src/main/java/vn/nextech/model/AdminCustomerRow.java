package vn.nextech.model;

import java.time.LocalDateTime;

/** Thong tin tai khoan duoc phep hien thi trong trang quan tri (khong bao gom mat khau). */
public record AdminCustomerRow(
        long id,
        String fullName,
        String email,
        String phone,
        boolean active,
        LocalDateTime createdAt) {
}
