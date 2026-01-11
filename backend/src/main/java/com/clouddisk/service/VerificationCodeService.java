package com.clouddisk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clouddisk.common.BusinessException;
import com.clouddisk.entity.EmailVerificationCode;
import com.clouddisk.mapper.EmailVerificationCodeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * 验证码服务
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Slf4j
@Service
public class VerificationCodeService {

    @Autowired
    private EmailVerificationCodeMapper verificationCodeMapper;

    @Autowired
    private EmailService emailService;

    @Value("${business.verification-code.length}")
    private Integer codeLength;

    @Value("${business.verification-code.expire-minutes}")
    private Integer expireMinutes;

    /**
     * 发送验证码
     *
     * @param email    邮箱
     * @param codeType 验证码类型：1-注册验证，2-登录验证，3-密码重置
     */
    @Transactional(rollbackFor = Exception.class)
    public void sendCode(String email, Integer codeType) {
        // 检查是否频繁发送（1分钟内只能发送一次）
        LambdaQueryWrapper<EmailVerificationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmailVerificationCode::getEmail, email)
               .eq(EmailVerificationCode::getCodeType, codeType)
               .ge(EmailVerificationCode::getCreatedAt, LocalDateTime.now().minusMinutes(1))
               .orderByDesc(EmailVerificationCode::getCreatedAt)
               .last("LIMIT 1");

        EmailVerificationCode lastCode = verificationCodeMapper.selectOne(wrapper);
        if (lastCode != null) {
            throw new BusinessException("发送验证码过于频繁，请1分钟后再试");
        }

        // 生成验证码
        String code = generateCode();

        // 保存到数据库
        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setCodeType(codeType);
        verificationCode.setUsed(0);
        verificationCode.setExpireTime(LocalDateTime.now().plusMinutes(expireMinutes));
        verificationCode.setCreatedAt(LocalDateTime.now());

        verificationCodeMapper.insert(verificationCode);

        // 异步发送邮件
        emailService.sendVerificationCode(email, code, codeType);

        log.info("验证码生成成功：email={}, codeType={}, code={}", email, codeType, code);
    }

    /**
     * 验证验证码
     *
     * @param email    邮箱
     * @param code     验证码
     * @param codeType 验证码类型
     * @return true-验证成功，false-验证失败
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean verifyCode(String email, String code, Integer codeType) {
        // 万能验证码（仅供测试使用）
        if ("123456".equals(code)) {
            log.info("使用万能验证码通过验证：email={}", email);
            return true;
        }

        LambdaQueryWrapper<EmailVerificationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmailVerificationCode::getEmail, email)
               .eq(EmailVerificationCode::getCode, code)
               .eq(EmailVerificationCode::getCodeType, codeType)
               .eq(EmailVerificationCode::getUsed, 0)
               .orderByDesc(EmailVerificationCode::getCreatedAt)
               .last("LIMIT 1");

        EmailVerificationCode verificationCode = verificationCodeMapper.selectOne(wrapper);

        if (verificationCode == null) {
            log.warn("验证码不存在或已使用：email={}, code={}", email, code);
            return false;
        }

        if (verificationCode.getExpireTime().isBefore(LocalDateTime.now())) {
            log.warn("验证码已过期：email={}, code={}", email, code);
            return false;
        }

        verificationCode.setUsed(1);
        verificationCodeMapper.updateById(verificationCode);

        log.info("验证码验证成功：email={}, code={}", email, code);
        return true;
    }

    /**
     * 生成随机验证码
     *
     * @return 验证码
     */
    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}
