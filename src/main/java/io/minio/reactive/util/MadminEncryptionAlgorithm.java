package io.minio.reactive.util;

/**
 * MinIO madmin 加密载荷头部中的算法 ID。
 *
 * <p>这里刻意把“可识别”和“当前 Java 端可解密”分开：SDK 可以识别 madmin-go 默认
 * Argon2id 载荷，但在没有通过 crypto 依赖 ADR 前，只允许解密 PBKDF2 + AES-GCM。
 */
public enum MadminEncryptionAlgorithm {
  ARGON2ID_AES_GCM((byte) 0x00, "Argon2id + AES-GCM", false),
  ARGON2ID_CHACHA20_POLY1305((byte) 0x01, "Argon2id + ChaCha20-Poly1305", false),
  PBKDF2_AES_GCM((byte) 0x02, "PBKDF2 + AES-GCM", true);

  private final byte id;
  private final String displayName;
  private final boolean decryptSupported;

  MadminEncryptionAlgorithm(byte id, String displayName, boolean decryptSupported) {
    this.id = id;
    this.displayName = displayName;
    this.decryptSupported = decryptSupported;
  }

  /** 返回 madmin-go 载荷头部中的算法 ID。 */
  public byte id() {
    return id;
  }

  /** 返回适合错误信息和文档展示的算法名称。 */
  public String displayName() {
    return displayName;
  }

  /** 当前 Java 端是否已经支持解密该算法。 */
  public boolean decryptSupported() {
    return decryptSupported;
  }

  /** 根据 madmin-go 算法 ID 查找枚举；未知 ID 返回 null。 */
  public static MadminEncryptionAlgorithm fromId(byte id) {
    for (MadminEncryptionAlgorithm algorithm : values()) {
      if (algorithm.id == id) {
        return algorithm;
      }
    }
    return null;
  }
}
