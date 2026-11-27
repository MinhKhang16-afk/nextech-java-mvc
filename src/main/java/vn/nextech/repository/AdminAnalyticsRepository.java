package vn.nextech.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.nextech.model.BestSellingProduct;
import vn.nextech.model.BirthdayCustomer;
import vn.nextech.model.RevenuePoint;
import vn.nextech.model.TopCustomer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AdminAnalyticsRepository {
    private static final int LOW_STOCK_LIMIT = 5;
    private static final String CUSTOMER_KEY =
        "COALESCE(NULLIF(LOWER(TRIM(email)),''), " +
        "NULLIF(CONCAT('phone:',REPLACE(REPLACE(REPLACE(REPLACE(TRIM(so_dien_thoai),' ',''),'.',''),'-',''),'+','')),'phone:'), " +
        "CONCAT('customer:',khach_hang_id))";
    private final JdbcTemplate jdbc;

    public AdminAnalyticsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private String orderScope(LocalDateTime from, LocalDateTime to, Integer categoryId,
                              List<Object> args) {
        args.add(from);
        args.add(to);
        StringBuilder where = new StringBuilder(
            "dh.thoi_gian_dat >= ? AND dh.thoi_gian_dat < ?"
        );
        if (categoryId != null && categoryId > 0) {
            where.append(" AND EXISTS (SELECT 1 FROM chi_tiet_don_hang ct_filter ")
                 .append("JOIN san_pham p_filter ON p_filter.san_pham_id=ct_filter.san_pham_id ")
                 .append("WHERE ct_filter.don_hang_id=dh.don_hang_id ")
                 .append("AND p_filter.danh_muc_id=?)");
            args.add(categoryId);
        }
        return where.toString();
    }

    private long orderCount(LocalDateTime from, LocalDateTime to, Integer categoryId,
                            String status) {
        List<Object> args = new ArrayList<>();
        String where = orderScope(from, to, categoryId, args);
        String sql = "SELECT COUNT(*) FROM don_hang dh WHERE " + where;
        if (status != null) {
            sql += " AND dh.trang_thai=?";
            args.add(status);
        }
        Long result = jdbc.queryForObject(sql, Long.class, args.toArray());
        return result == null ? 0L : result;
    }

    public long totalOrders(LocalDateTime from, LocalDateTime to, Integer categoryId) {
        return orderCount(from, to, categoryId, null);
    }

    public long ordersByStatus(LocalDateTime from, LocalDateTime to,
                               Integer categoryId, String status) {
        return orderCount(from, to, categoryId, status);
    }

    public long revenue(LocalDateTime from, LocalDateTime to, Integer categoryId) {
        Long result;
        if (categoryId == null || categoryId <= 0) {
            result = jdbc.queryForObject(
                "SELECT COALESCE(SUM(dh.tong_tien),0) FROM don_hang dh " +
                "WHERE dh.trang_thai='HOAN_TAT' AND dh.thoi_gian_dat >= ? AND dh.thoi_gian_dat < ?",
                Long.class, from, to
            );
        } else {
            result = jdbc.queryForObject(
                "SELECT COALESCE(SUM(ct.thanh_tien),0) " +
                "FROM don_hang dh " +
                "JOIN chi_tiet_don_hang ct ON ct.don_hang_id=dh.don_hang_id " +
                "JOIN san_pham sp ON sp.san_pham_id=ct.san_pham_id " +
                "WHERE dh.trang_thai='HOAN_TAT' AND dh.thoi_gian_dat >= ? " +
                "AND dh.thoi_gian_dat < ? AND sp.danh_muc_id=?",
                Long.class, from, to, categoryId
            );
        }
        return result == null ? 0L : result;
    }

    public List<RevenuePoint> revenueChart(String period, LocalDateTime from,
                                           LocalDateTime to, Integer categoryId) {
        String bucket = switch (period) {
            case "DAY" -> "DATE_FORMAT(dh.thoi_gian_dat, '%H:00')";
            case "YEAR" -> "DATE_FORMAT(dh.thoi_gian_dat, '%m/%Y')";
            default -> "DATE_FORMAT(dh.thoi_gian_dat, '%d/%m')";
        };
        List<RevenuePoint> raw;
        if (categoryId == null || categoryId <= 0) {
            raw = jdbc.query(
                "SELECT " + bucket + " AS nhan, COALESCE(SUM(dh.tong_tien),0) AS doanh_thu " +
                "FROM don_hang dh WHERE dh.trang_thai='HOAN_TAT' " +
                "AND dh.thoi_gian_dat >= ? AND dh.thoi_gian_dat < ? " +
                "GROUP BY " + bucket + " ORDER BY MIN(dh.thoi_gian_dat)",
                (rs, row) -> new RevenuePoint(
                    rs.getString("nhan"), rs.getLong("doanh_thu"), 0
                ),
                from, to
            );
        } else {
            raw = jdbc.query(
                "SELECT " + bucket + " AS nhan, COALESCE(SUM(ct.thanh_tien),0) AS doanh_thu " +
                "FROM don_hang dh " +
                "JOIN chi_tiet_don_hang ct ON ct.don_hang_id=dh.don_hang_id " +
                "JOIN san_pham sp ON sp.san_pham_id=ct.san_pham_id " +
                "WHERE dh.trang_thai='HOAN_TAT' AND dh.thoi_gian_dat >= ? " +
                "AND dh.thoi_gian_dat < ? AND sp.danh_muc_id=? " +
                "GROUP BY " + bucket + " ORDER BY MIN(dh.thoi_gian_dat)",
                (rs, row) -> new RevenuePoint(
                    rs.getString("nhan"), rs.getLong("doanh_thu"), 0
                ),
                from, to, categoryId
            );
        }
        long max = raw.stream().mapToLong(RevenuePoint::amount).max().orElse(0L);
        if (max == 0) return raw;
        return raw.stream().map(point -> new RevenuePoint(
            point.label(),
            point.amount(),
            Math.max(5, (int) Math.round(point.amount() * 100.0 / max))
        )).toList();
    }

    public long totalCustomers() {
        Long value = jdbc.queryForObject(
            "SELECT COUNT(*) FROM (SELECT " + CUSTOMER_KEY +
            " AS customer_key FROM khach_hang GROUP BY customer_key) c",
            Long.class
        );
        return value == null ? 0L : value;
    }

    public long customerAccounts() {
        Long value = jdbc.queryForObject("SELECT COUNT(*) FROM tai_khoan", Long.class);
        return value == null ? 0L : value;
    }

    public long newCustomers(LocalDate from, LocalDate to) {
        Long value = jdbc.queryForObject(
            "SELECT COUNT(*) FROM (" +
            "SELECT " + CUSTOMER_KEY + " AS customer_key, MIN(created_at) AS first_seen " +
            "FROM khach_hang GROUP BY customer_key HAVING first_seen >= ? AND first_seen < ?" +
            ") c",
            Long.class, from, to
        );
        return value == null ? 0L : value;
    }

    public List<BirthdayCustomer> birthdaysInMonth(int month) {
        return jdbc.query(
            "SELECT ho_ten, email, ngay_sinh FROM tai_khoan " +
            "WHERE trang_thai=1 AND ngay_sinh IS NOT NULL AND MONTH(ngay_sinh)=? " +
            "ORDER BY DAY(ngay_sinh), ho_ten LIMIT 100",
            (rs, row) -> new BirthdayCustomer(
                rs.getString("ho_ten"),
                rs.getString("email"),
                rs.getDate("ngay_sinh").toLocalDate()
            ),
            month
        );
    }

    private long productCount(String condition, Integer categoryId) {
        String sql = "SELECT COUNT(*) FROM san_pham p WHERE p.trang_thai=1 AND (" +
                     condition + ")";
        List<Object> args = new ArrayList<>();
        if (categoryId != null && categoryId > 0) {
            sql += " AND p.danh_muc_id=?";
            args.add(categoryId);
        }
        Long value = jdbc.queryForObject(sql, Long.class, args.toArray());
        return value == null ? 0L : value;
    }

    public long activeProducts(Integer categoryId) {
        return productCount("1=1", categoryId);
    }

    public long sellingProducts(Integer categoryId) {
        return productCount("p.so_luong_ton > 0", categoryId);
    }

    public long lowStockProducts(Integer categoryId) {
        return productCount("p.so_luong_ton BETWEEN 1 AND " + LOW_STOCK_LIMIT, categoryId);
    }

    public long outOfStockProducts(Integer categoryId) {
        return productCount("p.so_luong_ton <= 0", categoryId);
    }

    public List<BestSellingProduct> bestSellingProducts(LocalDateTime from,
                                                         LocalDateTime to,
                                                         Integer categoryId) {
        StringBuilder sql = new StringBuilder(
            "SELECT ct.san_pham_id, ct.ten_san_pham, dm.ten_danh_muc, " +
            "SUM(ct.so_luong) AS so_luong_ban, " +
            "SUM(CASE WHEN dh.trang_thai='HOAN_TAT' THEN ct.thanh_tien ELSE 0 END) AS doanh_thu " +
            "FROM chi_tiet_don_hang ct " +
            "JOIN don_hang dh ON dh.don_hang_id=ct.don_hang_id " +
            "JOIN san_pham sp ON sp.san_pham_id=ct.san_pham_id " +
            "JOIN danh_muc dm ON dm.danh_muc_id=sp.danh_muc_id " +
            "WHERE dh.thoi_gian_dat >= ? AND dh.thoi_gian_dat < ? " +
            "AND dh.trang_thai <> 'DA_HUY'"
        );
        List<Object> args = new ArrayList<>();
        args.add(from);
        args.add(to);
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND sp.danh_muc_id=?");
            args.add(categoryId);
        }
        sql.append(" GROUP BY ct.san_pham_id, ct.ten_san_pham, dm.ten_danh_muc ")
           .append("ORDER BY so_luong_ban DESC, doanh_thu DESC LIMIT 10");
        return jdbc.query(sql.toString(), (rs, row) -> new BestSellingProduct(
            rs.getString("ten_san_pham"),
            rs.getString("ten_danh_muc"),
            rs.getLong("so_luong_ban"),
            rs.getLong("doanh_thu")
        ), args.toArray());
    }

    public List<TopCustomer> topCustomers(LocalDateTime from, LocalDateTime to,
                                           Integer categoryId) {
        StringBuilder sql = new StringBuilder(
            "SELECT MAX(kh.ho_ten) AS ho_ten, " +
            "COALESCE(NULLIF(MAX(kh.email),''), MAX(kh.so_dien_thoai), '—') AS lien_he, " +
            "COUNT(DISTINCT dh.don_hang_id) AS so_don, " +
            "COALESCE(SUM(CASE WHEN dh.trang_thai='HOAN_TAT' " +
            "THEN ct.thanh_tien ELSE 0 END),0) AS chi_tieu " +
            "FROM khach_hang kh " +
            "JOIN don_hang dh ON dh.khach_hang_id=kh.khach_hang_id " +
            "JOIN chi_tiet_don_hang ct ON ct.don_hang_id=dh.don_hang_id " +
            "JOIN san_pham sp ON sp.san_pham_id=ct.san_pham_id " +
            "WHERE dh.thoi_gian_dat >= ? AND dh.thoi_gian_dat < ? " +
            "AND dh.trang_thai <> 'DA_HUY'"
        );
        List<Object> args = new ArrayList<>();
        args.add(from);
        args.add(to);
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND sp.danh_muc_id=?");
            args.add(categoryId);
        }
        sql.append(
            " GROUP BY COALESCE(NULLIF(LOWER(TRIM(kh.email)),''), " +
            "NULLIF(CONCAT('phone:',REPLACE(REPLACE(REPLACE(REPLACE(TRIM(kh.so_dien_thoai),' ',''),'.',''),'-',''),'+','')),'phone:'), " +
            "CONCAT('customer:',kh.khach_hang_id)) " +
            "ORDER BY chi_tieu DESC, so_don DESC LIMIT 10"
        );
        return jdbc.query(sql.toString(), (rs, row) -> new TopCustomer(
            rs.getString("ho_ten"),
            rs.getString("lien_he"),
            rs.getLong("so_don"),
            rs.getLong("chi_tieu")
        ), args.toArray());
    }
}
