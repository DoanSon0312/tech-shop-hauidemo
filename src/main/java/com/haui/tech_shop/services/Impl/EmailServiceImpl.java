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

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${spring.mail.verify.host}")
    private String host;

    @Value("${resend.from-email}")
    private String fromEmail;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final WebClient webClient = WebClient.create("https://api.resend.com/emails");

    @Override
    public void sendEmailToVerifyAccount(String name, String to, String token) {
        try {
            String content = EmailUtil.getEmailMessage(name, host, token)
                    .replace("\n", "<br>");

            sendEmail(to, "Verify your account", content);

        } catch (Exception e) {
            throw new RuntimeException("Send email failed\n\n" + e.getMessage());
        }
    }

    @Override
    public void sendEmailToReactivePassword(String email) {
        try {
            User existingUser = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String newPassword = EmailUtil.generateRandomPassword();

            String content = "New your password is: " + newPassword +
                    "<br><br>The support by [Đ.N.T.Sơn]";

            sendEmail(existingUser.getEmail(), "Reset Password", content);

            existingUser.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(existingUser);

        } catch (Exception e) {
            throw new RuntimeException("Send email failed\n\n" + e.getMessage());
        }
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        webClient.post()
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(Map.of(
                        "from", fromEmail,
                        "to", List.of(to),
                        "subject", subject,
                        "html", htmlContent
                ))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
