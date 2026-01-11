package com.clouddisk.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 邮件服务
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 发送验证码邮件（异步）
     *
     * @param toEmail  收件人邮箱
     * @param code     验证码
     * @param codeType 验证码类型：1-注册验证，2-登录验证，3-密码重置
     */
    @Async
    public void sendVerificationCode(String toEmail, String code, Integer codeType) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);

            String subject = "";
            String content = "";

            switch (codeType) {
                case 1:
                    subject = "【吉亦云盘】注册验证码";
                    content = String.format(
                            "尊敬的用户，您好！\n\n" +
                                    "您正在注册吉亦云盘账户，验证码为：%s\n\n" +
                                    "验证码5分钟内有效，请勿泄露给他人。\n\n" +
                                    "如非本人操作，请忽略此邮件。\n\n" +
                                    "——吉亦云盘",
                            code);
                    break;
                case 2:
                    subject = "【吉亦云盘】登录验证码";
                    content = String.format(
                            "尊敬的用户，您好！\n\n" +
                                    "您正在登录吉亦云盘，验证码为：%s\n\n" +
                                    "验证码5分钟内有效，请勿泄露给他人。\n\n" +
                                    "如非本人操作，请立即修改密码。\n\n" +
                                    "——吉亦云盘",
                            code);
                    break;
                case 3:
                    subject = "【吉亦云盘】密码重置验证码";
                    content = String.format(
                            "尊敬的用户，您好！\n\n" +
                                    "您正在重置吉亦云盘密码，验证码为：%s\n\n" +
                                    "验证码5分钟内有效，请勿泄露给他人。\n\n" +
                                    "如非本人操作，请忽略此邮件。\n\n" +
                                    "——吉亦云盘",
                            code);
                    break;
                default:
                    throw new IllegalArgumentException("未知的验证码类型");
            }

            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("验证码邮件发送成功：{} -> {}", fromEmail, toEmail);
        } catch (Exception e) {
            log.error("验证码邮件发送失败：{}", toEmail, e);
            throw new RuntimeException("验证码邮件发送失败", e);
        }
    }
}
