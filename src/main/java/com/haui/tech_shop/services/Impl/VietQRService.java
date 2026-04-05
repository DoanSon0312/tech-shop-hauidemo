package com.haui.tech_shop.services.Impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class VietQRService {

    @Value("${vietqr.bank-id}")
    private String bankId;

    @Value("${vietqr.account-no}")
    private String accountNo;

    @Value("${vietqr.account-name}")
    private String accountName;

    public String generateQRUrl(long amount, String description) {
        String encodedDesc = URLEncoder.encode(description, StandardCharsets.UTF_8);
        return String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                bankId, accountNo, amount, encodedDesc,
                URLEncoder.encode(accountName, StandardCharsets.UTF_8)
        );
    }

    public String getBankId() { return bankId; }
    public String getAccountNo() { return accountNo; }
    public String getAccountName() { return accountName; }
}