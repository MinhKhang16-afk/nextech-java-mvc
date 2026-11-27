package vn.nextech.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nextech.model.CustomerAccount;
import vn.nextech.repository.PasswordResetRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {
    private final PasswordResetRepository tokens;
    private final CustomerAccountService customers;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean mailEnabled;
    private final String fromAddress;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(PasswordResetRepository tokens,
                                CustomerAccountService customers,
                                PasswordEncoder encoder,
                                ObjectProvider<JavaMailSender> mailSender,
                                @Value("${app.mail.enabled:false}") boolean mailEnabled,
                                @Value("${app.mail.from:noreply@nextech.local}") String fromAddress) {
        this.tokens = tokens;
        this.customers = customers;
        this.encoder = encoder;
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
    }

    public boolean isMailEnabled() {
        return mailEnabled && mailSender.getIfAvailable() != null;
    }

    public void requestReset(String email, String resetUrl) {
        if (!isMailEnabled()) {
            throw new IllegalStateException("Chưa cấu hình dịch vụ gửi email. Hãy cấu hình MAIL_ENABLED, MAIL_HOST và thông tin SMTP trong .env.");
        }
        CustomerAccount account = customers.findByEmail(email)
            .filter(CustomerAccount::active)
            .orElse(null);
        // Do not reveal whether an email exists.
        if (account == null || tokens.countRecentRequests(account.id()) >= 3) return;

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String tokenHash = hash(token);
        tokens.invalidateOutstanding(account.id());
        tokens.insert(account.id(), tokenHash);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(account.email());
        message.setSubject("Đặt lại mật khẩu NexTech");
        message.setText("Xin chào " + account.fullName() + ",

" +
            "Bạn vừa yêu cầu đặt lại mật khẩu NexTech. Liên kết chỉ có hiệu lực trong 30 phút và chỉ sử dụng một lần:

" +
            resetUrl + "?token=" + token + "

" +
            "Nếu bạn không yêu cầu, hãy bỏ qua email này.");
        try {
            mailSender.getObject().send(message);
        } catch (MailException ex) {
            tokens.invalidateOutstanding(account.id());
            throw new IllegalStateException("Không gửi được email đặt lại mật khẩu. Hãy kiểm tra cấu hình SMTP.", ex);
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
        }
        if (newPassword == null || newPassword.length() < 8 ||
                newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 8 ký tự và tối đa 72 byte UTF-8.");
        }
        int updated = tokens.consumeAndUpdatePassword(hash(token), encoder.encode(newPassword));
        if (updated != 1) {
            throw new IllegalArgumentException("Liên kết đặt lại mật khẩu không hợp lệ, đã sử dụng hoặc hết hạn.");
        }
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Không hỗ trợ SHA-256.", ex);
        }
    }
}
