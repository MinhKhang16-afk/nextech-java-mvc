
package vn.nextech.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.nextech.model.CartItem;
import vn.nextech.model.CheckoutForm;
import vn.nextech.model.OrderReceipt;
import vn.nextech.model.Product;
import vn.nextech.repository.OrderRepository;
import vn.nextech.repository.ShopRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final ShopRepository products;

    public OrderService(
            OrderRepository orders,
            ShopRepository products) {
        this.orders = orders;
        this.products = products;
    }

    // Tạo đơn hàng
    @Transactional
    public OrderReceipt place(
            CheckoutForm form,
            Map<Integer, Integer> cart,
            Long accountId) {

        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống");
        }

        if (cart.size() > 100) {
            throw new IllegalArgumentException(
                "Giỏ hàng có quá nhiều dòng"
            );
        }

        String paymentMethod = form.getPaymentMethod();

        if (!"COD".equals(paymentMethod)
                && !"CHUYEN_KHOAN".equals(paymentMethod)) {
            throw new IllegalArgumentException(
                "Phương thức thanh toán không hợp lệ"
            );
        }

        long total = 0;
        List<CartItem> snapshot = new ArrayList<>();

        // Đọc giá và tồn kho từ MySQL
        for (Map.Entry<Integer, Integer> line : cart.entrySet()) {

            int qty = line.getValue();

            if (qty < 1 || qty > 999) {
                throw new IllegalArgumentException(
                    "Số lượng không hợp lệ"
                );
            }

            Product p = products.activeById(line.getKey())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Có sản phẩm đã ngừng bán"
                    )
                );

            if (p.price() < 0 || p.stock() < qty) {
                throw new IllegalArgumentException(
                    "Không đủ hàng: " + p.name()
                );
            }

            total = Math.addExact(
                total,
                Math.multiplyExact(p.price(), qty)
            );

            snapshot.add(new CartItem(p, qty));
        }

        long customer = orders.insertCustomer(form);

        String code = "DH"
            + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyMMddHHmmss")
            )
            + "-"
            + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        long order = orders.insertOrder(
            customer,
            accountId,
            code,
            form,
            total,
            paymentMethod
        );
        orders.addStatusHistory(order, "CHO_XAC_NHAN", "Đơn hàng đã được tiếp nhận.");

        for (CartItem line : snapshot) {

            Product p = line.product();
            int qty = line.quantity();

            if (!orders.decrementStock(p.id(), qty)) {
                throw new IllegalArgumentException(
                    "Hết hàng hoặc số lượng vừa thay đổi: "
                    + p.name()
                );
            }

            orders.insertLine(
                order,
                p.id(),
                p.name(),
                qty,
                p.price(),
                line.subtotal()
            );
        }

        return new OrderReceipt(code, total, paymentMethod);
    }

    // Admin xác nhận đã nhận được tiền chuyển khoản
    @Transactional
    public void confirmTransferPayment(
            long orderId,
            String transactionCode) {

        if (orderId <= 0) {
            throw new IllegalArgumentException(
                "Mã đơn hàng không hợp lệ"
            );
        }

        if (transactionCode != null) {
            transactionCode = transactionCode.trim();

            if (transactionCode.isEmpty()) {
                transactionCode = null;
            }

            if (transactionCode != null
                    && transactionCode.length() > 100) {
                throw new IllegalArgumentException(
                    "Mã giao dịch không được vượt quá 100 ký tự"
                );
            }
        }

        int updated = orders.confirmTransferPayment(
            orderId,
            transactionCode
        );

        if (updated != 1) {
            throw new IllegalArgumentException(
                "Không thể xác nhận thanh toán. "
                + "Đơn hàng không phải chuyển khoản "
                + "hoặc đã được xử lý trước đó."
            );
        }
    }

    // Cập nhật trạng thái xử lý đơn hàng
    @Transactional
    public void changeStatus(long orderId, String requested) {

        String current = orders.lockStatus(orderId);

        List<String> allowed = switch (current) {
            case "CHO_XAC_NHAN" ->
                List.of("DA_XAC_NHAN", "DA_HUY");

            case "DA_XAC_NHAN" ->
                List.of("DANG_GIAO", "DA_HUY");

            case "DANG_GIAO" ->
                List.of("HOAN_TAT");

            default -> List.of();
        };

        if (!allowed.contains(requested)) {
            throw new IllegalArgumentException(
                "Không thể chuyển từ "
                + current
                + " sang "
                + requested
            );
        }

        // Đơn chuyển khoản phải được xác nhận thanh toán
        // trước khi admin xác nhận xử lý đơn hàng.
        if ("DA_XAC_NHAN".equals(requested)
                && orders.isTransferPaymentPending(orderId)) {
            throw new IllegalArgumentException(
                "Đơn hàng chưa được xác nhận thanh toán chuyển khoản"
            );
        }

        if ("DA_HUY".equals(requested)) {
            orders.restock(orderId);
        }

        orders.updateStatus(orderId, requested);
        String trackingNote = switch (requested) {
            case "DA_XAC_NHAN" -> "Cửa hàng đã xác nhận đơn hàng.";
            case "DANG_GIAO" -> "Đơn hàng đang được vận chuyển đến bạn.";
            case "HOAN_TAT" -> "Đơn hàng đã được giao hoàn tất.";
            case "DA_HUY" -> "Đơn hàng đã bị hủy.";
            default -> "Trạng thái đơn hàng được cập nhật.";
        };
        orders.addStatusHistory(orderId, requested, trackingNote);
    }
}
