package io.minio.reactive.util;

/**
 * MinIO madmin 加密载荷的兼容性辅助工具。
 *
 * <p>madmin-go 的加密数据格式为：32 字节 salt、1 字节算法 ID、8 字节 nonce、后续加密流。
 * 当前 SDK 只做格式识别，不实现加密/解密。原因是 madmin-go 使用 secure-io DARE 流格式，
 * 同时涉及 Argon2id、PBKDF2、AES-GCM、ChaCha20-Poly1305 等组合。为了避免产生不兼容密文，
 * 在完整实现和互操作测试之前，SDK 不会伪装支持 madmin 加密写接口。
 */
public final class MadminEncryptionSupport {
  private static final int SALT_LENGTH = 32;
  private static final int ALGORITHM_OFFSET = 32;
  private static final byte ARGON2ID_AES_GCM = 0x00;
  private static final byte ARGON2ID_CHACHA20_POLY1305 = 0x01;
  private static final byte PBKDF2_AES_GCM = 0x02;

  private MadminEncryptionSupport() {}

  /** 判断数据是否看起来像 madmin-go EncryptData 生成的加密载荷。 */
  public static boolean isEncrypted(byte[] data) {
    if (data == null || data.length <= SALT_LENGTH) {
      return false;
    }
    byte algorithm = data[ALGORITHM_OFFSET];
    return algorithm == ARGON2ID_AES_GCM
        || algorithm == ARGON2ID_CHACHA20_POLY1305
        || algorithm == PBKDF2_AES_GCM;
  }

  /** 当前版本尚未实现 madmin 加密载荷生成，调用方应继续使用高级兼容入口或 raw 兜底。 */
  public static byte[] encryptData(String secretKey, byte[] plainData) {
    throw new UnsupportedOperationException(
        "当前 SDK 尚未实现 madmin EncryptData 兼容加密；请暂时使用已加密载荷的高级兼容入口或 raw 兜底。",
        null);
  }
}
