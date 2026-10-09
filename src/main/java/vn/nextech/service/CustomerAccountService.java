package vn.nextech.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.nextech.model.CustomerAccount;
import vn.nextech.model.RegistrationForm;
import vn.nextech.repository.CustomerAccountRepository;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@Service
public class CustomerAccountService {
    private final CustomerAccountRepository accounts;
    private final PasswordEncoder encoder;

    public CustomerAccountService(CustomerAccountRepository accounts, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.encoder = encoder;
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public Optional<CustomerAccount> findByEmail(String email) {
        return accounts.findByEmail(normalizeEmail(email));
    }

    public void register(RegistrationForm form) {
        String email = normalizeEmail(form.getEmail());
        String password = form.getPassword();
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu không được vượt quá 72 byte UTF-8");
        }
        if (accounts.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email đã được đăng ký");
        }
        try {
            accounts.insert(form.getFullName().trim(), email,
                form.getPhone() == null ? "" : form.getPhone().trim(),
                encoder.encode(password));
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("Email đã được đăng ký", ex);
        }
    }
}
