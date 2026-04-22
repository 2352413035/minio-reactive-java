package io.minio.reactive.messages.admin;

import io.minio.reactive.util.MadminEncryptionSupport;

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
}
