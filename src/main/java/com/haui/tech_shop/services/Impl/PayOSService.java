package com.haui.tech_shop.services.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;

@Service
@RequiredArgsConstructor
public class PayOSService {

    private final PayOS payOS;

    /**
     * Tạo link thanh toán
     */
    public CreatePaymentLinkResponse createPaymentLink(long orderCode, long amount,                                        String description,                                        String returnUrl,                                        String cancelUrl) throws Exception {

        CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(amount)
                .description(description.length() > 25 ? description.substring(0, 25) : description)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();

        return payOS.paymentRequests().create(request);
    }

    /**
     * Lấy thông tin thanh toán
     */
    public PaymentLink getPaymentInfo(long orderCode) throws Exception {
        return payOS.paymentRequests().get(String.valueOf(orderCode));
    }

    /**
     * Hủy link thanh toán
     */
    public PaymentLink cancelPaymentLink(long orderCode) throws Exception {
        return payOS.paymentRequests().cancel(String.valueOf(orderCode));
    }
}
