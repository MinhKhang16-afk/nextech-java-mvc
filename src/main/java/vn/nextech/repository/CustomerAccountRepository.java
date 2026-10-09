package vn.nextech.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.nextech.model.CustomerAccount;

import java.util.List;
import java.util.Optional;

@Repository
public class CustomerAccountRepository {
    private final JdbcTemplate jdbc;

    public CustomerAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<CustomerAccount> findByEmail(String email) {
        List<CustomerAccount> accounts = jdbc.query(
            "SELECT tai_khoan_id, ho_ten, email, so_dien_thoai, mat_khau_hash, trang_thai, created_at " +
            "FROM tai_khoan WHERE email = ? LIMIT 1",
            (rs, row) -> new CustomerAccount(
                rs.getLong("tai_khoan_id"),
                rs.getString("ho_ten"),
                rs.getString("email"),
                rs.getString("so_dien_thoai"),
                rs.getString("mat_khau_hash"),
                rs.getBoolean("trang_thai"),
                rs.getTimestamp("created_at").toLocalDateTime()),
            email
        );
        return accounts.stream().findFirst();
    }

    public void insert(String fullName, String email, String phone, String passwordHash) {
        jdbc.update(
            "INSERT INTO tai_khoan(ho_ten, email, so_dien_thoai, mat_khau_hash) VALUES(?,?,?,?)",
            fullName, email, phone, passwordHash
        );
    }
}
