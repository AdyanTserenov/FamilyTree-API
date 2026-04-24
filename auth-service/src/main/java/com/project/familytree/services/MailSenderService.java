package com.project.familytree.services;

import com.project.familytree.exceptions.EmailSenderException;
import com.project.familytree.impls.TreeRole;
import com.project.familytree.models.User;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailSenderService {
    private final JavaMailSender javaMailSender;
    private final EmailTemplateService emailTemplateService;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    public void sendPasswordResetEmail(String email, String token, String userName) {
        String resetLink = baseUrl + "/reset-password?token=" + token;
        String subject = "Сброс пароля - FamilyTree";

        MimeMessage message = buildMessage(subject, email, resetLink, userName);

        javaMailSender.send(message);
        log.info("Письмо для сброса пароля отправлено на: {}", email);
    }

    public void sendCreationEmail(String email, String token, String userName) {
        String creationLink = baseUrl + "/confirm-email?token=" + token;
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

    public void sendDeletionWarningEmail(String email, String userName) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setSubject("Ваш аккаунт будет удалён - FamilyTree");
            helper.setFrom("abtserenov@edu.hse.ru", "FamilyTree Support");
            helper.setTo(email);

            String name = (userName != null && !userName.isBlank()) ? userName : "пользователь";
            String html = """
                    <!DOCTYPE html>
                    <html lang="ru">
                    <head><meta charset="UTF-8"><title>Предупреждение об удалении аккаунта</title></head>
                    <body style="font-family: Arial, sans-serif; color: #333;">
                        <div style="max-width:600px;margin:20px auto;background:#fff;border-radius:8px;box-shadow:0 2px 10px rgba(0,0,0,0.1);">
                            <div style="background:#f59e0b;padding:30px;text-align:center;color:white;border-radius:8px 8px 0 0;">
                                <h1>⚠️ Предупреждение об удалении аккаунта</h1>
                            </div>
                            <div style="padding:30px;">
                                <p><strong>Здравствуйте, %s!</strong></p>
                                <p>Вы не входили в систему <strong>FamilyTree</strong> более 10 лет.</p>
                                <p>Если в течение <strong>7 дней</strong> не будет активности, ваш аккаунт и все данные будут безвозвратно удалены.</p>
                                <p>Чтобы сохранить аккаунт, просто войдите в систему.</p>
                                <div style="background:#fff3cd;border:1px solid #ffeaa7;border-radius:6px;padding:15px;margin:20px 0;">
                                    <strong>Внимание!</strong> Это автоматическое уведомление. Если вы не планируете использовать аккаунт, никаких действий не требуется.
                                </div>
                            </div>
                            <div style="background:#f8f9fa;padding:20px;text-align:center;color:#666;border-top:1px solid #e9ecef;">
                                <p>С уважением,<br><strong>Команда FamilyTree</strong></p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """.formatted(name);

            helper.setText(html, true);
            javaMailSender.send(message);
            log.info("Письмо-предупреждение об удалении отправлено на: {}", email);
        } catch (Exception e) {
            log.error("Ошибка отправки письма-предупреждения на {}: {}", email, e.getMessage());
            throw new EmailSenderException("Ошибка отправки письма-предупреждения");
        }
    }

    public void sendDeletionConfirmationEmail(String email, String userName,
                                               List<byte[]> gedcomFiles,
                                               List<String> fileNames) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();

            // Use multipart/mixed to support attachments
            MimeMultipart multipart = new MimeMultipart("mixed");

            // HTML body part
            String name = (userName != null && !userName.isBlank()) ? userName : "пользователь";
            String html = """
                    <!DOCTYPE html>
                    <html lang="ru">
                    <head><meta charset="UTF-8"><title>Аккаунт удалён</title></head>
                    <body style="font-family: Arial, sans-serif; color: #333;">
                        <div style="max-width:600px;margin:20px auto;background:#fff;border-radius:8px;box-shadow:0 2px 10px rgba(0,0,0,0.1);">
                            <div style="background:#ef4444;padding:30px;text-align:center;color:white;border-radius:8px 8px 0 0;">
                                <h1>Ваш аккаунт удалён</h1>
                            </div>
                            <div style="padding:30px;">
                                <p><strong>Здравствуйте, %s!</strong></p>
                                <p>Ваш аккаунт в системе <strong>FamilyTree</strong> был удалён в связи с длительным отсутствием активности.</p>
                                <p>Во вложении — ваши генеалогические данные в формате <strong>GEDCOM</strong>. Вы можете импортировать их в любую совместимую программу для работы с генеалогией.</p>
                                <div style="background:#f8d7da;border:1px solid #f5c6cb;border-radius:6px;padding:15px;margin:20px 0;">
                                    Все данные вашего аккаунта были безвозвратно удалены с наших серверов.
                                </div>
                            </div>
                            <div style="background:#f8f9fa;padding:20px;text-align:center;color:#666;border-top:1px solid #e9ecef;">
                                <p>С уважением,<br><strong>Команда FamilyTree</strong></p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """.formatted(name);

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(html, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            // Attach GEDCOM files
            if (gedcomFiles != null && fileNames != null) {
                for (int i = 0; i < gedcomFiles.size() && i < fileNames.size(); i++) {
                    byte[] fileBytes = gedcomFiles.get(i);
                    String fileName = fileNames.get(i);
                    if (fileBytes != null && fileName != null) {
                        MimeBodyPart attachmentPart = new MimeBodyPart();
                        attachmentPart.setContent(fileBytes, "text/plain; charset=UTF-8");
                        attachmentPart.setFileName(fileName);
                        multipart.addBodyPart(attachmentPart);
                    }
                }
            }

            message.setContent(multipart);
            message.setSubject("Ваш аккаунт удалён - FamilyTree");
            message.setFrom("abtserenov@edu.hse.ru");
            message.setRecipients(MimeMessage.RecipientType.TO,
                    jakarta.mail.internet.InternetAddress.parse(email));

            javaMailSender.send(message);
            log.info("Письмо-подтверждение удаления отправлено на: {}", email);
        } catch (Exception e) {
            log.error("Ошибка отправки письма-подтверждения удаления на {}: {}", email, e.getMessage());
            throw new EmailSenderException("Ошибка отправки письма-подтверждения удаления");
        }
    }

    public void sendInvitationEmail(
            String toEmail,
            String treeName,
            User inviter,
            TreeRole role,
            String inviteLink
    ) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setSubject("Вас пригласили в семейное древо «" + treeName + "»");
            helper.setFrom("abtserenov@edu.hse.ru", "FamilyTree Support");
            helper.setTo(toEmail);

            String inviterName = inviter.getFirstName() + " " + inviter.getLastName();
            String roleStr = switch (role) {
                case OWNER -> "Владелец";
                case EDITOR -> "Редактор";
                case VIEWER -> "Зритель";
            };

            String html = emailTemplateService.buildInvitationEmail(
                    treeName, inviterName, roleStr, inviteLink
            );

            helper.setText(html, true);
            javaMailSender.send(message);
            log.info("Приглашение отправлено на: {}", toEmail);
        } catch (Exception e) {
            log.error("Ошибка отправки приглашения: {}", e.getMessage());
            throw new EmailSenderException("Не удалось отправить приглашение");
        }
    }
}
