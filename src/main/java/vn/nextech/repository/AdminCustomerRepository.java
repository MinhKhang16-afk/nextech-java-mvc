package vn.nextech.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.nextech.model.AdminCustomerRow;

import java.util.List;

@Repository
public class AdminCustomerRepository {
    private final JdbcTemplate jdbc;

    public AdminCustomerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AdminCustomerRow> search(String query) {
        String sql = "SELECT tai_khoan_id, ho_ten, email, so_dien_thoai, trang_thai, created_at " +
                     "FROM tai_khoan ";
        var mapper = (org.springframework.jdbc.core.RowMapper<AdminCustomerRow>) (rs, rowNum) ->
            new AdminCustomerRow(
                rs.getLong("tai_khoan_id"),
                rs.getString("ho_ten"),
                rs.getString("email"),
                rs.getString("so_dien_thoai"),
                rs.getBoolean("trang_thai"),
                rs.getTimestamp("created_at").toLocalDateTime()
            );
        if (query == null || query.isBlank()) {
            return jdbc.query(sql + "ORDER BY tai_khoan_id DESC LIMIT 200", mapper);
        }
        String keyword = "%" + query.trim() + "%";
        return jdbc.query(sql +
                "WHERE ho_ten LIKE ? OR email LIKE ? OR so_dien_thoai LIKE ? " +
                "ORDER BY tai_khoan_id DESC LIMIT 200",
                mapper, keyword, keyword, keyword);
    }

    public int countAll() {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM tai_khoan", Integer.class);
        return total == null ? 0 : total;
    }

    public int countActive() {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM tai_khoan WHERE trang_thai = 1", Integer.class);
        return total == null ? 0 : total;
    }

    /** true = mo khoa; false = khoa. Khong xoa tai khoan va don hang. */
    public boolean setActive(long accountId, boolean active) {
        return jdbc.update("UPDATE tai_khoan SET trang_thai = ? WHERE tai_khoan_id = ?",
                active ? 1 : 0, accountId) == 1;
    }
}
