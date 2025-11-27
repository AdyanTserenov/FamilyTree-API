package com.project.familytree.services;

import com.project.familytree.exceptions.EmailSenderException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailSenderService {
    private final JavaMailSender javaMailSender;
    private final EmailTemplateService emailTemplateService;

    public void sendPasswordResetEmail(String email, String token, String userName) {
        String resetLink = "https://simulteh.com/reset-password?token=" + token;
        String subject = "Сброс пароля - FamilyTree";

        MimeMessage message = buildMessage(subject, email, resetLink, userName);

        javaMailSender.send(message);
        log.info("Письмо для сброса пароля отправлено на: {}", email);
    }

    public void sendCreationEmail(String email, String token, String userName) {
        String creationLink = "https://simulteh.com/confirm?token=" + token;
        String subject = "Подтверждение регистрации - FamilyTree";

        MimeMessage message = buildMessage(subject, email, creationLink, userName);

        javaMailSender.send(message);
        log.info("Письмо для подтверждения регистрации отправлено на: {}", email);

    }

    private MimeMessage buildMessage(String subject, String email, String link, String userName) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setSubject(subject);
            helper.setFrom("abtserenov@edu.hse.ru", "FamilyTree Support");
            helper.setTo(email);

            String emailContent;
            if (subject.equals("Подтверждение регистрации - FamilyTree")) {
                emailContent = emailTemplateService.buildVerificationEmail(link, userName);
            } else {
                emailContent = emailTemplateService.buildPasswordResetEmail(link, userName);
            }

            helper.setText(emailContent, true);

            return message;
        } catch (Exception e) {
            log.error("Ошибка отправки письма: {}", e.getMessage());
            throw new EmailSenderException("Ошибка отправки письма");
        }
    }
}
