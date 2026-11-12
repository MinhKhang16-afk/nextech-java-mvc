package vn.nextech.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class VietQrService {

    @Value("${PAYMENT_BANK_CODE:}")
    private String bankCode;

    @Value("${PAYMENT_BANK_ACCOUNT:}")
    private String bankAccount;

    @Value("${PAYMENT_BANK_ACCOUNT_NAME:}")
    private String bankAccountName;

    public boolean isConfigured() {
        return bankCode != null
            && bankCode.matches("[0-9]{6}")
            && bankAccount != null
            && bankAccount.matches("[0-9]{6,20}")
            && bankAccountName != null
            && !bankAccountName.isBlank();
    }

    public String createQrUrl(long amount, String orderCode) {

        if (!isConfigured()) {
            throw new IllegalStateException(
                "Chưa cấu hình thông tin Vietcombank trong môi trường ứng dụng"
            );
        }

        if (amount <= 0 || orderCode == null || orderCode.isBlank()) {
            throw new IllegalArgumentException(
                "Số tiền hoặc mã đơn hàng không hợp lệ"
            );
        }

        String imageUrl =
            "https://img.vietqr.io/image/"
            + bankCode + "-"
            + bankAccount
            + "-compact2.png";

        return UriComponentsBuilder
            .fromUriString(imageUrl)
            .queryParam("amount", amount)
            .queryParam("addInfo", orderCode)
            .queryParam("accountName", bankAccountName)
            .build()
            .encode()
            .toUriString();
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public String getBankAccountName() {
        return bankAccountName;
    }
}