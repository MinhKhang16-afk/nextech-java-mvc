
package vn.nextech.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import vn.nextech.model.CheckoutForm;
import vn.nextech.model.OrderLine;
import vn.nextech.model.ShopOrder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbc;

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Ánh xạ dữ liệu đơn hàng từ MySQL sang ShopOrder
    private static final RowMapper<ShopOrder> ORDER = (rs, n) -> {

        Timestamp paidAt = rs.getTimestamp("thoi_gian_thanh_toan");

        return new ShopOrder(
            rs.getLong("don_hang_id"),
            rs.getString("ma_don_hang"),
            rs.getString("ten_nguoi_nhan"),
            rs.getString("sdt_nguoi_nhan"),
            rs.getString("dia_chi_giao_hang"),
            rs.getString("ghi_chu"),
            rs.getLong("tong_tien"),
            rs.getString("trang_thai"),
            rs.getTimestamp("thoi_gian_dat").toLocalDateTime(),

            // Thông tin thanh toán
            rs.getString("phuong_thuc_thanh_toan"),
            rs.getString("trang_thai_thanh_toan"),
            paidAt == null ? null : paidAt.toLocalDateTime(),
            rs.getString("ma_giao_dich_ngan_hang")
        );
    };

    // Tạo bản ghi và lấy ID tự động
    private long generated(String sql, Object... values) {

        KeyHolder holder = new GeneratedKeyHolder();

        jdbc.update(conn -> {

            PreparedStatement ps = conn.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
            );

            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }

            return ps;

        }, holder);

        return Objects.requireNonNull(holder.getKey()).longValue();
    }

    // Tạo thông tin khách hàng
    public long insertCustomer(CheckoutForm f) {

        return generated(
            "INSERT INTO khach_hang(ho_ten,so_dien_thoai,email,dia_chi) VALUES(?,?,?,?)",
            f.getFullName().trim(),
            f.getPhone().trim(),
            f.getEmail(),
            f.getAddress().trim()
        );
    }

    // Tạo đơn hàng và lưu phương thức thanh toán
    public long insertOrder(
            long customerId,
            String code,
            CheckoutForm f,
            long total,
            String paymentMethod) {

        String paymentStatus;

        if ("COD".equals(paymentMethod)) {

            paymentStatus = "CHUA_THANH_TOAN";

        } else if ("CHUYEN_KHOAN".equals(paymentMethod)) {

            paymentStatus = "CHO_KIEM_TRA";

        } else {

            throw new IllegalArgumentException(
                "Phương thức thanh toán không hợp lệ"
            );
        }

        String sql =
            "INSERT INTO don_hang(" +
            "ma_don_hang," +
            "khach_hang_id," +
            "ten_nguoi_nhan," +
            "sdt_nguoi_nhan," +
            "dia_chi_giao_hang," +
            "ghi_chu," +
            "tam_tinh," +
            "phi_giao_hang," +
            "tong_tien," +
            "phuong_thuc_thanh_toan," +
            "trang_thai," +
            "trang_thai_thanh_toan" +
            ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";

        return generated(
            sql,
            code,
            customerId,
            f.getFullName().trim(),
            f.getPhone().trim(),
            f.getAddress().trim(),
            f.getNote(),
            total,
            0,
            total,
            paymentMethod,
            "CHO_XAC_NHAN",
            paymentStatus
        );
    }

    // Lưu từng sản phẩm trong đơn hàng
    public void insertLine(
            long orderId,
            int productId,
            String name,
            int qty,
            long price,
            long subtotal) {

        jdbc.update(
            "INSERT INTO chi_tiet_don_hang(" +
            "don_hang_id,san_pham_id,ten_san_pham," +
            "so_luong,don_gia,thanh_tien" +
            ") VALUES(?,?,?,?,?,?)",
            orderId,
            productId,
            name,
            qty,
            price,
            subtotal
        );
    }

    // Trừ số lượng tồn kho
    public boolean decrementStock(int productId, int amount) {

        return jdbc.update(
            "UPDATE san_pham " +
            "SET so_luong_ton=so_luong_ton-? " +
            "WHERE san_pham_id=? " +
            "AND so_luong_ton>=? " +
            "AND trang_thai=1",
            amount,
            productId,
            amount
        ) == 1;
    }

    // Lấy danh sách đơn hàng
    public List<ShopOrder> all() {

        return jdbc.query(
            "SELECT * FROM don_hang ORDER BY don_hang_id DESC LIMIT 200",
            ORDER
        );
    }

    // Lấy thông tin một đơn hàng
    public ShopOrder get(long id) {

        return jdbc.query(
            "SELECT * FROM don_hang WHERE don_hang_id=?",
            ORDER,
            id
        ).stream()
            .findFirst()
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Không tìm thấy đơn hàng"
                )
            );
    }

    // Khóa đơn hàng khi cập nhật trạng thái
    public String lockStatus(long id) {

        return jdbc.query(
            "SELECT trang_thai FROM don_hang " +
            "WHERE don_hang_id=? FOR UPDATE",
            (rs, n) -> rs.getString(1),
            id
        ).stream()
            .findFirst()
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Không tìm thấy đơn hàng"
                )
            );
    }

    // Kiểm tra đơn chuyển khoản chưa được xác nhận thanh toán
    public boolean isTransferPaymentPending(long orderId) {

        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM don_hang " +
            "WHERE don_hang_id=? " +
            "AND phuong_thuc_thanh_toan='CHUYEN_KHOAN' " +
            "AND trang_thai_thanh_toan <> 'DA_THANH_TOAN'",
            Integer.class,
            orderId
        );

        return count != null && count > 0;
    }

    // Admin xác nhận đã nhận tiền chuyển khoản
    public int confirmTransferPayment(
            long orderId,
            String transactionCode) {

        return jdbc.update(
            "UPDATE don_hang SET " +
            "trang_thai_thanh_toan='DA_THANH_TOAN', " +
            "thoi_gian_thanh_toan=NOW(), " +
            "ma_giao_dich_ngan_hang=? " +
            "WHERE don_hang_id=? " +
            "AND phuong_thuc_thanh_toan='CHUYEN_KHOAN' " +
            "AND trang_thai_thanh_toan='CHO_KIEM_TRA' " +
            "AND trang_thai <> 'DA_HUY'",
            transactionCode,
            orderId
        );
    }

    // Lấy các sản phẩm thuộc đơn hàng
    public List<OrderLine> lines(long orderId) {

        return jdbc.query(
            "SELECT * FROM chi_tiet_don_hang WHERE don_hang_id=?",
            (rs, n) -> new OrderLine(
                rs.getInt("san_pham_id"),
                rs.getString("ten_san_pham"),
                rs.getInt("so_luong"),
                rs.getLong("don_gia"),
                rs.getLong("thanh_tien")
            ),
            orderId
        );
    }

    // Hoàn lại tồn kho khi hủy đơn
    public void restock(long orderId) {

        jdbc.update(
            "UPDATE san_pham p " +
            "JOIN chi_tiet_don_hang ct " +
            "ON ct.san_pham_id=p.san_pham_id " +
            "SET p.so_luong_ton=p.so_luong_ton+ct.so_luong " +
            "WHERE ct.don_hang_id=?",
            orderId
        );
    }

    // Cập nhật trạng thái đơn hàng
    public void updateStatus(long id, String status) {

        jdbc.update(
            "UPDATE don_hang SET trang_thai=? WHERE don_hang_id=?",
            status,
            id
        );
    }

    // Đếm tổng số đơn hàng
    public long countOrders() {

        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM don_hang",
            Long.class
        );
    }

    // Tính doanh thu từ các đơn hoàn tất
    public long totalRevenue() {

        return jdbc.queryForObject(
            "SELECT COALESCE(SUM(tong_tien),0) " +
            "FROM don_hang WHERE trang_thai='HOAN_TAT'",
            Long.class
        );
    }
}
