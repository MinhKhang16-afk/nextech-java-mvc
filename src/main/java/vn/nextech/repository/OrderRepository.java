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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Repository
public class OrderRepository {
    private final JdbcTemplate jdbc;

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

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
            rs.getString("phuong_thuc_thanh_toan"),
            rs.getString("trang_thai_thanh_toan"),
            paidAt == null ? null : paidAt.toLocalDateTime(),
            rs.getString("ma_giao_dich_ngan_hang")
        );
    };

    private long generated(String sql, Object... values) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < values.length; i++) ps.setObject(i + 1, values[i]);
            return ps;
        }, holder);
        return Objects.requireNonNull(holder.getKey()).longValue();
    }

    public long insertCustomer(CheckoutForm f) {
        return generated(
            "INSERT INTO khach_hang(ho_ten,so_dien_thoai,email,dia_chi) VALUES(?,?,?,?)",
            f.getFullName().trim(), f.getPhone().trim(), f.getEmail(), f.getAddress().trim()
        );
    }

    public long insertOrder(long customerId, String code, CheckoutForm f,
                            long total, String paymentMethod) {
        String paymentStatus;
        if ("COD".equals(paymentMethod)) paymentStatus = "CHUA_THANH_TOAN";
        else if ("CHUYEN_KHOAN".equals(paymentMethod)) paymentStatus = "CHO_KIEM_TRA";
        else throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");

        String sql =
            "INSERT INTO don_hang(ma_don_hang,khach_hang_id,ten_nguoi_nhan,sdt_nguoi_nhan," +
            "dia_chi_giao_hang,ghi_chu,tam_tinh,phi_giao_hang,tong_tien," +
            "phuong_thuc_thanh_toan,trang_thai,trang_thai_thanh_toan) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        return generated(
            sql, code, customerId, f.getFullName().trim(), f.getPhone().trim(),
            f.getAddress().trim(), f.getNote(), total, 0, total,
            paymentMethod, "CHO_XAC_NHAN", paymentStatus
        );
    }

    public void insertLine(long orderId, int productId, String name, int qty,
                           long price, long subtotal) {
        jdbc.update(
            "INSERT INTO chi_tiet_don_hang(don_hang_id,san_pham_id,ten_san_pham," +
            "so_luong,don_gia,thanh_tien) VALUES(?,?,?,?,?,?)",
            orderId, productId, name, qty, price, subtotal
        );
    }

    public boolean decrementStock(int productId, int amount) {
        return jdbc.update(
            "UPDATE san_pham SET so_luong_ton=so_luong_ton-? " +
            "WHERE san_pham_id=? AND so_luong_ton>=? AND trang_thai=1",
            amount, productId, amount
        ) == 1;
    }

    public List<ShopOrder> all() {
        return jdbc.query(
            "SELECT * FROM don_hang ORDER BY don_hang_id DESC LIMIT 200",
            ORDER
        );
    }

    private String adminOrderWhere(String query, String status, String paymentMethod,
                                   String paymentStatus, LocalDateTime from,
                                   LocalDateTime to, List<Object> args) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        if (query != null && !query.isBlank()) {
            String keyword = "%" + query.trim() + "%";
            where.append(" AND (ma_don_hang LIKE ? OR ten_nguoi_nhan LIKE ? OR sdt_nguoi_nhan LIKE ?) ");
            args.add(keyword);
            args.add(keyword);
            args.add(keyword);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND trang_thai=? ");
            args.add(status);
        }
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            where.append(" AND phuong_thuc_thanh_toan=? ");
            args.add(paymentMethod);
        }
        if (paymentStatus != null && !paymentStatus.isBlank()) {
            where.append(" AND trang_thai_thanh_toan=? ");
            args.add(paymentStatus);
        }
        if (from != null) {
            where.append(" AND thoi_gian_dat>=? ");
            args.add(from);
        }
        if (to != null) {
            where.append(" AND thoi_gian_dat<? ");
            args.add(to);
        }
        return where.toString();
    }

    public List<ShopOrder> searchAdmin(String query, String status,
                                       String paymentMethod, String paymentStatus,
                                       LocalDateTime from, LocalDateTime to,
                                       int limit, int offset) {
        List<Object> args = new ArrayList<>();
        String where = adminOrderWhere(query, status, paymentMethod, paymentStatus, from, to, args);
        args.add(limit);
        args.add(offset);
        return jdbc.query(
            "SELECT * FROM don_hang " + where +
            " ORDER BY don_hang_id DESC LIMIT ? OFFSET ?",
            ORDER,
            args.toArray()
        );
    }

    public long countAdmin(String query, String status, String paymentMethod,
                           String paymentStatus, LocalDateTime from, LocalDateTime to) {
        List<Object> args = new ArrayList<>();
        String where = adminOrderWhere(query, status, paymentMethod, paymentStatus, from, to, args);
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM don_hang " + where,
            Long.class,
            args.toArray()
        );
        return count == null ? 0L : count;
    }

    public ShopOrder get(long id) {
        return jdbc.query(
            "SELECT * FROM don_hang WHERE don_hang_id=?",
            ORDER,
            id
        ).stream().findFirst().orElseThrow(
            () -> new IllegalArgumentException("Không tìm thấy đơn hàng")
        );
    }

    public String lockStatus(long id) {
        return jdbc.query(
            "SELECT trang_thai FROM don_hang WHERE don_hang_id=? FOR UPDATE",
            (rs, n) -> rs.getString(1),
            id
        ).stream().findFirst().orElseThrow(
            () -> new IllegalArgumentException("Không tìm thấy đơn hàng")
        );
    }

    public boolean isTransferPaymentPending(long orderId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM don_hang WHERE don_hang_id=? " +
            "AND phuong_thuc_thanh_toan='CHUYEN_KHOAN' " +
            "AND trang_thai_thanh_toan <> 'DA_THANH_TOAN'",
            Integer.class, orderId
        );
        return count != null && count > 0;
    }

    public int confirmTransferPayment(long orderId, String transactionCode) {
        return jdbc.update(
            "UPDATE don_hang SET trang_thai_thanh_toan='DA_THANH_TOAN', " +
            "thoi_gian_thanh_toan=NOW(), ma_giao_dich_ngan_hang=? " +
            "WHERE don_hang_id=? AND phuong_thuc_thanh_toan='CHUYEN_KHOAN' " +
            "AND trang_thai_thanh_toan='CHO_KIEM_TRA' AND trang_thai<>'DA_HUY'",
            transactionCode, orderId
        );
    }

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

    public void restock(long orderId) {
        jdbc.update(
            "UPDATE san_pham p JOIN chi_tiet_don_hang ct " +
            "ON ct.san_pham_id=p.san_pham_id " +
            "SET p.so_luong_ton=p.so_luong_ton+ct.so_luong WHERE ct.don_hang_id=?",
            orderId
        );
    }

    public void updateStatus(long id, String status) {
        jdbc.update("UPDATE don_hang SET trang_thai=? WHERE don_hang_id=?", status, id);
    }

    public long countOrders() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM don_hang", Long.class);
    }

    public long totalRevenue() {
        return jdbc.queryForObject(
            "SELECT COALESCE(SUM(tong_tien),0) FROM don_hang WHERE trang_thai='HOAN_TAT'",
            Long.class
        );
    }
}
