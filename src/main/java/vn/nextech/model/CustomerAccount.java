package vn.nextech.model;

import java.time.LocalDateTime;

public record CustomerAccount(
        long id,
        String fullName,
        String email,
        String phone,
        String passwordHash,
        boolean active,
        LocalDateTime createdAt) {
}
