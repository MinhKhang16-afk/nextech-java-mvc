package vn.nextech.model;

public record OrderLine(long productId, String productName, int quantity, long price, long subtotal) {}
