package com.haui.tech_shop.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/test")
public class TestErrorController {

    // 1. RuntimeException - LỖI NGHIÊM TRỌNG → Slack + Seq
    @GetMapping("/error-500")
    public String testError500() {
        throw new RuntimeException("Test lỗi 500 - kiểm tra Slack + Seq");
    }

    // 2. NullPointerException - LỖI NGHIÊM TRỌNG → Slack + Seq
    @GetMapping("/error-null")
    public String testNullPointer() {
        String s = null;
        s.length();
        return "500";
    }

    // 3. ArithmeticException - LỖI NGHIÊM TRỌNG → Slack + Seq
    @GetMapping("/error-arithmetic")
    public String testArithmetic() {
        int result = 10 / 0;
        return "500";
    }

    // 4. StackOverflow - LỖI NGHIÊM TRỌNG → Slack + Seq
    @GetMapping("/error-stackoverflow")
    public String testStackOverflow() {
        return testStackOverflow();
    }

    // 5. IllegalStateException - LỖI NGHIÊM TRỌNG → Slack + Seq
    @GetMapping("/error-db")
    public String testDbError() {
        throw new IllegalStateException("Database connection failed - simulated");
    }
}
