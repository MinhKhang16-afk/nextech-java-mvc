package vn.nextech.model;

import java.time.LocalDateTime;

public record OrderStatusEvent(String status, String note, LocalDateTime changedAt) {}
