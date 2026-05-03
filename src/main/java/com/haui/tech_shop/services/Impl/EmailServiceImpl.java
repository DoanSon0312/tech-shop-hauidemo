package com.haui.tech_shop.services.Impl;

import com.haui.tech_shop.entities.User;
import com.haui.tech_shop.repositories.UserRepository;
import com.haui.tech_shop.services.interfaces.EmailService;
import com.haui.tech_shop.utils.EmailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    @Value("${brevo.api-key}")
    private String apiKey;

    @Value("${spring.mail.verify.host}")
    private String host;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final WebClient webClient = WebClient.create("https://api.brevo.com/v3/smtp/email");

    @Override
    public void sendEmailToVerifyAccount(String name, String to, String token) {
        try {
            String content = EmailUtil.getEmailMessage(name, host, token)
                    .replace("\n", "<br>");

            sendEmail(to, "New User Account Verification", content);

        } catch (Exception e) {
            throw new RuntimeException("Has error occurred while sending email to verify account\n\n" + e.getMessage());
        }
    }

    @Override
    public void sendEmailToReactivePassword(String email) {
        try {
            User existingUser = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new RuntimeException("User not found at send email get password"));

            String newPassword = EmailUtil.generateRandomPassword();

            String content = "New your password is: " + newPassword
                    + "<br><br>The support by [Đ.N.T.Sơn]";

            sendEmail(existingUser.getEmail(), "Provide New Password", content);

            existingUser.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(existingUser);

        } catch (Exception e) {
            throw new RuntimeException("Has error occurred while sending email to get password\n\n" + e.getMessage());
        }
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        webClient.post()
                .header("api-key", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(Map.of(
                        "sender", Map.of(
                                "email", fromEmail,
                                "name", "Tech Shop"
                        ),
                        "to", List.of(Map.of("email", to)),
                        "subject", subject,
                        "htmlContent", htmlContent
                ))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
