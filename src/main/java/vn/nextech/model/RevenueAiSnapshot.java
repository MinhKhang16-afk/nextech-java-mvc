package vn.nextech.model;

import java.util.List;
import java.util.Map;

/** Aggregated, non-personal sales data sent to the AI report service. */
public record RevenueAiSnapshot(
        String periodLabel, String categoryLabel,
        long currentRevenue, long previousRevenue,
        long currentOrders, long previousOrders,
        long currentCompletedOrders, long previousCompletedOrders,
        long currentWaitingOrders, long currentShippingOrders, long currentCancelledOrders,
        long activeProducts, long lowStockProducts, long outOfStockProducts,
        List<Map<String, Object>> currentTopProducts,
        List<Map<String, Object>> previousTopProducts) {
}
