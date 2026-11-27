package vn.nextech.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordResetRepository {
    private final JdbcTemplate jdbc;

    public PasswordResetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int countRecentRequests(long accountId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM tai_khoan_password_reset " +
            "WHERE tai_khoan_id=? AND created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)",
            Integer.class, accountId
        );
        return count == null ? 0 : count;
    }

    public void invalidateOutstanding(long accountId) {
        jdbc.update(
            "UPDATE tai_khoan_password_reset SET used_at=NOW() " +
            "WHERE tai_khoan_id=? AND used_at IS NULL",
            accountId
        );
    }

    public void insert(long accountId, String tokenHash) {
        jdbc.update(
            "INSERT INTO tai_khoan_password_reset(tai_khoan_id,token_hash,expires_at) " +
            "VALUES(?,?,DATE_ADD(NOW(), INTERVAL 30 MINUTE))",
            accountId, tokenHash
        );
    }

    public int consumeAndUpdatePassword(String tokenHash, String passwordHash) {
        return jdbc.update(
            "UPDATE tai_khoan tk " +
            "JOIN tai_khoan_password_reset t ON t.tai_khoan_id=tk.tai_khoan_id " +
            "SET tk.mat_khau_hash=?, t.used_at=NOW() " +
            "WHERE t.token_hash=? AND t.used_at IS NULL AND t.expires_at>NOW() " +
            "AND tk.trang_thai=1",
            passwordHash, tokenHash
        );
    }
}
