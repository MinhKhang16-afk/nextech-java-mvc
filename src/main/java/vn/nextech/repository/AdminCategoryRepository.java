package vn.nextech.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.nextech.model.AdminCategoryRow;
import vn.nextech.model.CategoryForm;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class AdminCategoryRepository {
    private final JdbcTemplate jdbc;

    public AdminCategoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_ALL =
        "SELECT dm.danh_muc_id, dm.ma_danh_muc, dm.ten_danh_muc, dm.mo_ta, " +
        "dm.thu_tu, dm.trang_thai, COUNT(sp.san_pham_id) AS so_san_pham " +
        "FROM danh_muc dm LEFT JOIN san_pham sp " +
        "ON sp.danh_muc_id=dm.danh_muc_id ";

    private static AdminCategoryRow map(ResultSet rs) throws SQLException {
        return new AdminCategoryRow(
            rs.getInt("danh_muc_id"),
            rs.getString("ma_danh_muc"),
            rs.getString("ten_danh_muc"),
            rs.getString("mo_ta"),
            rs.getInt("thu_tu"),
            rs.getBoolean("trang_thai"),
            rs.getLong("so_san_pham")
        );
    }

    public List<AdminCategoryRow> findAll() {
        return jdbc.query(
            SELECT_ALL +
            "GROUP BY dm.danh_muc_id, dm.ma_danh_muc, dm.ten_danh_muc, dm.mo_ta, dm.thu_tu, dm.trang_thai " +
            "ORDER BY dm.thu_tu, dm.ten_danh_muc",
            (rs, row) -> map(rs)
        );
    }

    public Optional<AdminCategoryRow> findById(int id) {
        return jdbc.query(
            SELECT_ALL +
            "WHERE dm.danh_muc_id=? " +
            "GROUP BY dm.danh_muc_id, dm.ma_danh_muc, dm.ten_danh_muc, dm.mo_ta, dm.thu_tu, dm.trang_thai",
            (rs, row) -> map(rs),
            id
        ).stream().findFirst();
    }

    public boolean codeExists(String code, int exceptId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM danh_muc WHERE ma_danh_muc=? AND danh_muc_id<>?",
            Integer.class, code, exceptId
        );
        return count != null && count > 0;
    }

    public long countProducts(int categoryId) {
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM san_pham WHERE danh_muc_id=?",
            Long.class, categoryId
        );
        return count == null ? 0L : count;
    }

    public void create(CategoryForm form) {
        jdbc.update(
            "INSERT INTO danh_muc(ma_danh_muc, ten_danh_muc, mo_ta, thu_tu, trang_thai) VALUES(?,?,?,?,?)",
            form.getCode().trim().toUpperCase(java.util.Locale.ROOT),
            form.getName().trim(), blankToNull(form.getDescription()),
            form.getSortOrder(), form.isActive() ? 1 : 0
        );
    }

    public int update(int id, CategoryForm form) {
        return jdbc.update(
            "UPDATE danh_muc SET ma_danh_muc=?, ten_danh_muc=?, mo_ta=?, thu_tu=?, trang_thai=? " +
            "WHERE danh_muc_id=?",
            form.getCode().trim().toUpperCase(java.util.Locale.ROOT),
            form.getName().trim(), blankToNull(form.getDescription()),
            form.getSortOrder(), form.isActive() ? 1 : 0, id
        );
    }

    public int setActive(int id, boolean active) {
        return jdbc.update("UPDATE danh_muc SET trang_thai=? WHERE danh_muc_id=?",
            active ? 1 : 0, id);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
