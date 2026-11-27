package vn.nextech.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.nextech.model.AdminCustomerRow;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AdminCustomerRepository {
    private final JdbcTemplate jdbc;

    public AdminCustomerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_COLUMNS =
        "SELECT tai_khoan_id, ho_ten, email, so_dien_thoai, trang_thai, created_at, ngay_sinh " +
        "FROM tai_khoan ";

    private static AdminCustomerRow map(java.sql.ResultSet rs) throws java.sql.SQLException {
        Date birthDate = rs.getDate("ngay_sinh");
        return new AdminCustomerRow(
            rs.getLong("tai_khoan_id"),
            rs.getString("ho_ten"),
            rs.getString("email"),
            rs.getString("so_dien_thoai"),
            rs.getBoolean("trang_thai"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            birthDate == null ? null : birthDate.toLocalDate()
        );
    }

    private String where(String query, Boolean active, List<Object> args) {
        StringBuilder sql = new StringBuilder(" WHERE 1=1 ");
        if (query != null && !query.isBlank()) {
            String keyword = "%" + query.trim() + "%";
            sql.append(" AND (ho_ten LIKE ? OR email LIKE ? OR so_dien_thoai LIKE ?) ");
            args.add(keyword);
            args.add(keyword);
            args.add(keyword);
        }
        if (active != null) {
            sql.append(" AND trang_thai=? ");
            args.add(active ? 1 : 0);
        }
        return sql.toString();
    }

    public List<AdminCustomerRow> search(String query, Boolean active, int limit, int offset) {
        List<Object> args = new ArrayList<>();
        String where = where(query, active, args);
        args.add(limit);
        args.add(offset);
        return jdbc.query(
            SELECT_COLUMNS + where + " ORDER BY tai_khoan_id DESC LIMIT ? OFFSET ?",
            (rs, row) -> map(rs),
            args.toArray()
        );
    }

    public long countSearch(String query, Boolean active) {
        List<Object> args = new ArrayList<>();
        String where = where(query, active, args);
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM tai_khoan " + where,
            Long.class,
            args.toArray()
        );
        return count == null ? 0L : count;
    }

    public Optional<AdminCustomerRow> findById(long id) {
        return jdbc.query(
            SELECT_COLUMNS + " WHERE tai_khoan_id=?",
            (rs, row) -> map(rs),
            id
        ).stream().findFirst();
    }

    public int updateProfile(long id, String fullName, String phone,
                             java.time.LocalDate birthDate) {
        return jdbc.update(
            "UPDATE tai_khoan SET ho_ten=?, so_dien_thoai=?, ngay_sinh=? " +
            "WHERE tai_khoan_id=?",
            fullName,
            phone,
            birthDate,
            id
        );
    }

    public int countAll() {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM tai_khoan", Integer.class);
        return total == null ? 0 : total;
    }

    public int countActive() {
        Integer total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM tai_khoan WHERE trang_thai=1",
            Integer.class
        );
        return total == null ? 0 : total;
    }

    /** true = mở khóa; false = khóa. Không xóa tài khoản và đơn hàng. */
    public boolean setActive(long accountId, boolean active) {
        return jdbc.update(
            "UPDATE tai_khoan SET trang_thai=? WHERE tai_khoan_id=?",
            active ? 1 : 0,
            accountId
        ) == 1;
    }
}
