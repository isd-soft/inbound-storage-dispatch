package com.isd.wms.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${wms.app.url:http://localhost:8080}")
    private String appUrl;

    public void sendVerificationEmail(String toEmail, String username, String token) {
        log.info("Preparing verification email for user: '{}' to address: {}", username, toEmail);

        try {
            Context ctx = new Context();
            ctx.setVariable("username", username);
            ctx.setVariable("email", toEmail);
            ctx.setVariable("confirmUrl", appUrl + "/api/auth/verify?token=" + token);

            log.debug("Processing Thymeleaf template 'email/operator-welcome' for user '{}'", username);
            String html = templateEngine.process("email/operator-welcome", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Account verification — ISD Warehouse");
            helper.setText(html, true);

            log.debug("Sending MimeMessage via SMTP server to {}...", toEmail);
            mailSender.send(message);
            log.info("Verification email successfully sent to {}", toEmail);

        } catch (Exception e) {
            log.error("CRITICAL: Failed to send verification email to {}. Reason: ", toEmail, e);
            throw new RuntimeException("Failed to send verification email");
        }
    }
}
