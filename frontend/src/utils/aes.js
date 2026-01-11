import CryptoJS from 'crypto-js'

/**
 * AES-256-GCM 解密工具
 */
export class AESUtil {
  /**
   * 解密文件
   * @param {ArrayBuffer} encryptedData - 加密的文件数据（IV + 密文）
   * @param {string} fileKeyHex - 文件密钥（Base64字符串）
   * @param {string} masterKeyHex - 主密钥（Base64字符串）
   * @returns {ArrayBuffer} 解密后的文件数据
   */
  static async decryptFile(encryptedData, fileKeyHex, masterKeyHex) {
    try {
      const fileKey = await this.decryptFileKey(fileKeyHex, masterKeyHex)

      const dataView = new Uint8Array(encryptedData)
      const ivLength = 12
      const iv = dataView.slice(0, ivLength)
      const ciphertext = dataView.slice(ivLength)

      const decrypted = await this.decryptWithWebCrypto(ciphertext, fileKey, iv)

      return decrypted
    } catch (error) {
      console.error('文件解密失败:', error)
      throw new Error('文件解密失败')
    }
  }

  /**
   * 解密文件密钥
   * @param {string} encryptedKeyBase64 - 加密的文件密钥（Base64，包含IV+密文+Tag）
   * @param {string} masterKeyBase64 - 主密钥（Base64）
   * @returns {Uint8Array} 解密后的文件密钥
   */
  static async decryptFileKey(encryptedKeyBase64, masterKeyBase64) {
    try {
      const encryptedData = Uint8Array.from(atob(encryptedKeyBase64), c => c.charCodeAt(0))
      const masterKeyBytes = Uint8Array.from(atob(masterKeyBase64), c => c.charCodeAt(0))

      const ivLength = 12
      const iv = encryptedData.slice(0, ivLength)
      const ciphertext = encryptedData.slice(ivLength)

      const cryptoKey = await crypto.subtle.importKey(
        'raw',
        masterKeyBytes,
        { name: 'AES-GCM' },
        false,
        ['decrypt']
      )

      const decrypted = await crypto.subtle.decrypt(
        {
          name: 'AES-GCM',
          iv: iv,
          tagLength: 128
        },
        cryptoKey,
        ciphertext
      )

      const fileKeyBytes = new Uint8Array(decrypted)
      const fileKeyString = new TextDecoder().decode(fileKeyBytes)
      const actualFileKey = Uint8Array.from(atob(fileKeyString), c => c.charCodeAt(0))

      return actualFileKey
    } catch (error) {
      console.error('解密文件密钥失败:', error)
      throw error
    }
  }

  /**
   * 使用 Web Crypto API 解密数据
   * @param {Uint8Array} ciphertext - 密文
   * @param {Uint8Array} key - 密钥
   * @param {Uint8Array} iv - IV
   * @returns {Promise<ArrayBuffer>} 解密后的数据
   */
  static async decryptWithWebCrypto(ciphertext, key, iv) {
    try {
      const cryptoKey = await crypto.subtle.importKey(
        'raw',
        key,
        { name: 'AES-GCM' },
        false,
        ['decrypt']
      )

      const decrypted = await crypto.subtle.decrypt(
        {
          name: 'AES-GCM',
          iv: iv,
          tagLength: 128
        },
        cryptoKey,
        ciphertext
      )

      return decrypted
    } catch (error) {
      console.error('Web Crypto API 解密失败:', error)
      throw error
    }
  }

  /**
   * 生成随机主密钥（用于注册时）
   * @returns {string} 十六进制格式的主密钥
   */
  static generateMasterKey() {
    const randomBytes = CryptoJS.lib.WordArray.random(32)
    return randomBytes.toString(CryptoJS.enc.Hex)
  }
}

export default AESUtil
