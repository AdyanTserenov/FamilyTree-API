package com.project.familytree.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailTemplateService {
    public String buildPasswordResetEmail(String resetLink, String userName) {
        return """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="UTF-8">
                <title>Сброс пароля</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        margin: 0;
                        padding: 0;
                        background-color: #f6f9fc
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background: #ffffff;
                        border-radius: 8px;
                        overflow: hidden;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }
                    .header {
                        background: #98ff98;
                        padding: 30px;
                        text-align: center;
                        color: white;
                    }
                    .content {
                        padding: 30px;
                    }
                    .button {
                        display: inline-block;
                        background: #98ff98;
                        color: white;
                        text-decoration: none;
                        padding: 12px 24px;
                        border-radius: 6px;
                        font-weight: bold;
                        margin: 20px 0;
                    }
                    .footer {
                        background: #f8f9fa;
                        padding: 20px;
                        text-align: center;
                        color: #666;
                        border-top: 1px solid #e9ecef;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Сброс пароля</h1>
                    </div>
                    <div class="content">
                        <p><strong>Здравствуйте%s!</strong></p>
                        <p>Вы запросили сброс пароля для вашего аккаунта.</p>
                        <p>Для установки нового пароля нажмите на кнопку ниже:</p>
                        <div style="text-align: center;">
                            <a href="%s" class="button">Сбросить пароль</a>
                        </div>
                        <p>Или скопируйте эту ссылку в браузер:</p>
                        <p style="background: #f8f9fa; padding: 10px; border-radius: 4px; word-break: break-all;">
                            %s
                        </p>
                        <div style="background: #fff3cd; border: 1px solid #ffeaa7; border-radius: 6px; padding: 15px; margin: 20px 0;">
                            <strong>Внимание!</strong><br>
                            Ссылка действительна в течение 1 часа.
                            Если вы не запрашивали сброс пароля, проигнорируйте это письмо.
                        </div>
                    </div>
                    <div class="footer">
                        <p>С уважением,<br><strong>Команда FamilyTree</strong></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                userName != null ? ", " + userName : "",
                resetLink,
                resetLink
        );
    }

    public String buildVerificationEmail(String verifyLink, String userName) {
        return """
        <!DOCTYPE html>
        <html lang="ru">
        <head>
            <meta charset="UTF-8">
            <title>Подтверждение email</title>
            <style>
                body {
                    font-family: Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    margin: 0;
                    padding: 0;
                    background-color: #f6f9fc
                }
                .container {
                    max-width: 600px;
                    margin: 20px auto;
                    background: #ffffff;
                    border-radius: 8px;
                    overflow: hidden;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                }
                .header {
                    background: #4ade80;;
                    padding: 30px;
                    text-align: center;
                    color: white;
                }
                .content {
                    padding: 30px;
                }
                .button {
                    display: inline-block;
                    background: #4ade80;
                    color: white;
                    text-decoration: none;
                    padding: 12px 24px;
                    border-radius: 6px;
                    font-weight: bold;
                    margin: 20px 0;
                }
                .footer {
                    background: #f8f9fa;
                    padding: 20px;
                    text-align: center;
                    color: #666;
                    border-top: 1px solid #e9ecef;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>Подтверждение email</h1>
                </div>
                <div class="content">
                    <p><strong>Здравствуйте%s!</strong></p>
                    <p>Спасибо за регистрацию в FamilyTree!</p>
                    <p>Пожалуйста, подтвердите ваш email, нажав на кнопку ниже:</p>
                    <div style="text-align: center;">
                        <a href="%s" class="button">Подтвердить email</a>
                    </div>
                    <p>Или скопируйте эту ссылку в браузер:</p>
                    <p style="background: #f8f9fa; padding: 10px; border-radius: 4px; word-break: break-all;">
                        %s
                    </p>
                    <div style="background: #fff3cd; border: 1px solid #ffeaa7; border-radius: 6px; padding: 15px; margin: 20px 0;">
                        <strong>Внимание!</strong><br>
                        Ссылка действительна в течение 24 часов.
                        Если вы не регистрировались, проигнорируйте это письмо.
                    </div>
                </div>
                <div class="footer">
                    <p>С уважением,<br><strong>Команда FamilyTree</strong></p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(
                userName != null ? ", " + userName : "",
                verifyLink,
                verifyLink
        );
    }
}