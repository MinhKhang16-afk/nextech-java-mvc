
package vn.nextech.model;

import java.time.LocalDateTime;

public record ShopOrder(
    long id,
    String code,
    String customerName,
    String phone,
    String address,
    String note,
    long total,
    String status,
    LocalDateTime createdAt,

    // Thông tin thanh toán
    String paymentMethod,
    String paymentStatus,
    LocalDateTime paidAt,
    String bankTransactionCode
) {}
