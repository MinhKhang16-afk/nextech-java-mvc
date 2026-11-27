package vn.nextech.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Thông tin an toàn dành cho màn hình quản trị; không chứa mật khẩu. */
public record AdminCustomerRow(
        long id,
        String fullName,
        String email,
        String phone,
        boolean active,
        LocalDateTime createdAt,
        LocalDate birthDate) {
}
