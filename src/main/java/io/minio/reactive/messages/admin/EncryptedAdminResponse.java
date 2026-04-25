package io.minio.reactive.messages.admin;

import io.minio.reactive.util.MadminEncryptionAlgorithm;
import io.minio.reactive.util.MadminEncryptionSupport;
import java.nio.charset.StandardCharsets;

/** 管理端返回的加密响应载荷。 */
public final class EncryptedAdminResponse {
  private final byte[] encryptedData;

  public EncryptedAdminResponse(byte[] encryptedData) {
    this.encryptedData = encryptedData == null ? new byte[0] : encryptedData.clone();
  }

  public byte[] encryptedData() {
    return encryptedData.clone();
  }

  public boolean isEncrypted() {
    return MadminEncryptionSupport.isEncrypted(encryptedData);
  }

  /** 返回载荷头部声明的 madmin 加密算法；未知或非加密载荷返回 null。 */
  public MadminEncryptionAlgorithm algorithm() {
    return MadminEncryptionSupport.algorithmOf(encryptedData);
  }

  /** 返回适合日志和错误提示的算法名称。 */
  public String algorithmName() {
    MadminEncryptionAlgorithm algorithm = algorithm();
    return algorithm == null ? "" : algorithm.displayName();
  }

  /** 返回加密载荷字节数；只用于诊断，不代表明文大小。 */
  public int encryptedSize() {
    return encryptedData.length;
  }

  /** 当前 Java 端是否已经支持解密该响应声明的算法。 */
  public boolean decryptSupported() {
    MadminEncryptionAlgorithm algorithm = algorithm();
    return algorithm != null && algorithm.decryptSupported();
  }

  /** madmin 加密响应是否需要调用方提供对应账号的 secretKey 才能解密。 */
  public boolean requiresSecretKey() {
    return isEncrypted();
  }

  /**
   * 使用调用方显式提供的 secretKey 解密载荷。
   *
   * <p>该方法只支持当前 Java 运行时已经放行的算法；MinIO 默认 Argon2id 系列载荷仍会抛出中文错误，调用方应保留
   * `EncryptedAdminResponse` 作为回退边界，不能把失败结果伪装成空明文。
   */
  public byte[] decrypt(String secretKey) {
    return MadminEncryptionSupport.decryptData(secretKey, encryptedData);
  }

  /** 使用 UTF-8 将解密后的明文字节转成字符串。 */
  public String decryptAsUtf8(String secretKey) {
    return new String(decrypt(secretKey), StandardCharsets.UTF_8);
  }

  /** 是否仍需要 Crypto Gate 放行后才能解密。 */
  public boolean requiresCryptoGate() {
    return isEncrypted() && !decryptSupported();
  }

  /** 返回不包含敏感内容的中文诊断说明。 */
  public String diagnosticMessage() {
    if (encryptedData.length == 0) {
      return "未收到 madmin 加密响应载荷";
    }
    MadminEncryptionAlgorithm algorithm = algorithm();
    if (algorithm == null) {
      return "响应不是已识别的 madmin 加密载荷";
    }
    if (algorithm.decryptSupported()) {
      return "madmin 加密算法 " + algorithm.displayName() + " 当前支持 Java 端解密";
    }
    return "madmin 加密算法 " + algorithm.displayName() + " 当前需要 Crypto Gate 放行后才能解密";
  }
}
