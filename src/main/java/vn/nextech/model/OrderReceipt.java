package vn.nextech.model;

public record OrderReceipt(
    String code,
    long total,
    String paymentMethod
) {}