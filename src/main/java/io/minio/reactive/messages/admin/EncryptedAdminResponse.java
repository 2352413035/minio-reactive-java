package io.minio.reactive.messages.admin;

import io.minio.reactive.util.MadminEncryptionSupport;
import io.minio.reactive.util.MadminEncryptionAlgorithm;

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
}
