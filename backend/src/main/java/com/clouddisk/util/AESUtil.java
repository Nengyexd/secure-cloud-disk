package com.clouddisk.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

/**
 * AES加密工具类
 * 提供文件加密、密码加密、主密钥生成等功能
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Slf4j
public class AESUtil {

    /**
     * AES密钥长度（256位）
     */
    private static final int AES_KEY_SIZE = 256;

    /**
     * GCM模式Tag长度（128位）
     */
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * GCM模式IV长度（96位/12字节）
     */
    private static final int GCM_IV_LENGTH = 12;

    /**
     * PBKDF2迭代次数
     */
    private static final int PBKDF2_ITERATIONS = 65536;

    /**
     * 盐值长度
     */
    private static final int SALT_LENGTH = 32;

    /**
     * AES算法
     */
    private static final String AES_ALGORITHM = "AES";

    /**
     * AES/GCM/NoPadding 加密模式
     */
    private static final String AES_GCM_MODE = "AES/GCM/NoPadding";

    /**
     * PBKDF2算法
     */
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * 生成AES随机密钥（用于文件加密）
     *
     * @return Base64编码的AES密钥
     */
    public static String generateAESKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGenerator.init(AES_KEY_SIZE);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.encodeBase64String(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            log.error("生成AES密钥失败", e);
            throw new RuntimeException("生成AES密钥失败", e);
        }
    }

    /**
     * 生成随机盐值
     *
     * @return Base64编码的盐值
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.encodeBase64String(salt);
    }

    /**
     * 使用PBKDF2从密码派生主密钥
     *
     * @param password 用户密码
     * @param salt     盐值
     * @return Base64编码的主密钥
     */
    public static String deriveMasterKeyFromPassword(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    Base64.decodeBase64(salt),
                    PBKDF2_ITERATIONS,
                    AES_KEY_SIZE
            );
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.encodeBase64String(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("派生主密钥失败", e);
            throw new RuntimeException("派生主密钥失败", e);
        }
    }

    /**
     * AES-GCM加密
     *
     * @param plaintext 明文
     * @param key       Base64编码的密钥
     * @return Base64编码的密文（包含IV）
     */
    public static String encrypt(String plaintext, String key) {
        try {
            // 生成随机IV
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);

            // 解码密钥
            byte[] keyBytes = Base64.decodeBase64(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);

            // 初始化Cipher
            Cipher cipher = Cipher.getInstance(AES_GCM_MODE);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // 加密
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            // 将IV和密文合并
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return Base64.encodeBase64String(byteBuffer.array());
        } catch (Exception e) {
            log.error("AES加密失败", e);
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * AES-GCM解密
     *
     * @param ciphertext Base64编码的密文（包含IV）
     * @param key        Base64编码的密钥
     * @return 明文
     */
    public static String decrypt(String ciphertext, String key) {
        try {
            // 解码密文
            byte[] decoded = Base64.decodeBase64(ciphertext);

            // 提取IV和密文
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] ciphertextBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertextBytes);

            // 解码密钥
            byte[] keyBytes = Base64.decodeBase64(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);

            // 初始化Cipher
            Cipher cipher = Cipher.getInstance(AES_GCM_MODE);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // 解密
            byte[] plaintext = cipher.doFinal(ciphertextBytes);
            return new String(plaintext);
        } catch (Exception e) {
            log.error("AES解密失败", e);
            throw new RuntimeException("AES解密失败", e);
        }
    }

    /**
     * 加密文件密钥（使用主密钥加密文件密钥）
     *
     * @param fileKey   文件密钥
     * @param masterKey 主密钥
     * @return 加密后的文件密钥
     */
    public static String encryptFileKey(String fileKey, String masterKey) {
        return encrypt(fileKey, masterKey);
    }

    /**
     * 解密文件密钥（使用主密钥解密文件密钥）
     *
     * @param encryptedFileKey 加密的文件密钥
     * @param masterKey        主密钥
     * @return 文件密钥
     */
    public static String decryptFileKey(String encryptedFileKey, String masterKey) {
        return decrypt(encryptedFileKey, masterKey);
    }
}
