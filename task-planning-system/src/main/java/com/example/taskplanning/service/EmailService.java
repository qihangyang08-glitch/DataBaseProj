package com.example.taskplanning.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async("logTaskExecutor") // 使用我们已有的异步线程池
    public void sendVerificationEmail(String to, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 构建验证URL
            // 注意：在实际部署中，您可能需要将 "http://localhost:8080" 替换为前端应用的URL
            String verificationUrl = "http://localhost:8080/api/auth/verify-email?token=" + token;

            // 构建邮件内容 (HTML)
            String htmlContent = "<h1>任务规划平台 - 邮箱验证</h1>"
                    + "<p>感谢您的注册！请点击下面的链接来完成邮箱验证：</p>"
                    + "<a href=\"" + verificationUrl + "\">验证我的邮箱</a>"
                    + "<p>如果链接无法点击，请复制以下地址到浏览器地址栏打开：</p>"
                    + "<p>" + verificationUrl + "</p>"
                    + "<p>此链接将在24小时后失效。</p>";

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("请验证您的邮箱地址");
            helper.setText(htmlContent, true); // true表示这是一个HTML邮件

            mailSender.send(message);
            logger.info("Verification email sent successfully to {}", to);

        } catch (MessagingException e) {
            logger.error("Failed to send verification email to {}", to, e);
        }
    }
}