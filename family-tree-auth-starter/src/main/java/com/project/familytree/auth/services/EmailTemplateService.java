package com.project.familytree.auth.services;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    public String buildVerificationEmail(String verificationLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Подтверждение email</title>
                </head>
                <body>
                    <h1>Добро пожаловать в FamilyTree!</h1>
                    <p>Пожалуйста, подтвердите ваш email, перейдя по ссылке:</p>
                    <a href="%s">%s</a>
                    <p>Ссылка действительна 10 минут.</p>
                    <p>С уважением,<br><strong>Команда FamilyTree</strong></p>
                </body>
                </html>
                """.formatted(verificationLink, verificationLink);
    }

    public String buildPasswordResetEmail(String resetLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Сброс пароля</title>
                </head>
                <body>
                    <h1>Сброс пароля в FamilyTree</h1>
                    <p>Для сброса пароля перейдите по ссылке:</p>
                    <a href="%s">%s</a>
                    <p>Ссылка действительна 10 минут.</p>
                    <p>Если вы не запрашивали сброс, игнорируйте это письмо.</p>
                    <p>С уважением,<br><strong>Команда FamilyTree</strong></p>
                </body>
                </html>
                """.formatted(resetLink, resetLink);
    }
}