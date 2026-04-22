package io.minio.reactive.util;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * MinIO madmin 加密载荷的兼容性辅助工具。
 *
 * <p>madmin-go 的加密数据格式为：32 字节 salt、1 字节算法 ID、8 字节 nonce、后续 DARE
 * 加密流。当前 Java 实现选择服务端已支持的 PBKDF2 + AES-GCM 算法 ID，避免引入 Argon2 或
 * ChaCha20 依赖，目标是对齐 `madmin.DecryptData` 支持的 PBKDF2 AES-GCM 流式分片格式；在接入管理端写接口前仍需补充 madmin-go 互操作测试。
 */
public final class MadminEncryptionSupport {
  private static final int SALT_LENGTH = 32;
  private static final int STORED_NONCE_LENGTH = 8;
  private static final int GCM_NONCE_LENGTH = 12;
  private static final int GCM_TAG_BITS = 128;
  private static final int GCM_TAG_BYTES = 16;
  private static final int BUFFER_SIZE = 1 << 14;
  private static final int PBKDF2_COST = 8192;
  private static final int ALGORITHM_OFFSET = 32;
  private static final byte ARGON2ID_AES_GCM = 0x00;
  private static final byte ARGON2ID_CHACHA20_POLY1305 = 0x01;
  private static final byte PBKDF2_AES_GCM = 0x02;
  private static final SecureRandom RANDOM = new SecureRandom();

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

  /**
   * 使用 madmin-go 兼容的 PBKDF2 + AES-GCM 格式加密数据。
   *
   * <p>返回格式为 `salt | 算法 ID | nonce | DARE 密文分片`。服务端 `madmin.DecryptData` 会根据算法 ID 选择 PBKDF2 AES-GCM 路径。当前实现已有 Java 端 round-trip 测试；接入管理端写接口前还需要使用 madmin-go 做互操作测试。
   */
  public static byte[] encryptData(String secretKey, byte[] plainData) {
    requireSecret(secretKey);
    byte[] data = plainData == null ? new byte[0] : plainData;
    try {
      byte[] salt = randomBytes(SALT_LENGTH);
      byte[] noncePrefix = randomBytes(STORED_NONCE_LENGTH);
      byte[] key = pbkdf2(secretKey, salt);
      byte[] associatedData = initialAssociatedData(key, noncePrefix);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      output.write(salt);
      output.write(PBKDF2_AES_GCM);
      output.write(noncePrefix);
      writeEncryptedFragments(output, key, noncePrefix, associatedData, data);
      return output.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("无法生成 madmin 兼容加密载荷", e);
    }
  }

  /** 解密 PBKDF2 + AES-GCM madmin 加密载荷，主要用于互操作和单元测试。 */
  public static byte[] decryptData(String secretKey, byte[] encryptedData) {
    requireSecret(secretKey);
    if (encryptedData == null || encryptedData.length < SALT_LENGTH + 1 + STORED_NONCE_LENGTH + GCM_TAG_BYTES) {
      throw new IllegalArgumentException("madmin 加密载荷长度不足");
    }
    byte algorithm = encryptedData[ALGORITHM_OFFSET];
    if (algorithm != PBKDF2_AES_GCM) {
      throw new IllegalArgumentException("当前只支持 PBKDF2 AES-GCM madmin 加密载荷");
    }
    try {
      byte[] salt = copy(encryptedData, 0, SALT_LENGTH);
      byte[] noncePrefix = copy(encryptedData, SALT_LENGTH + 1, STORED_NONCE_LENGTH);
      byte[] key = pbkdf2(secretKey, salt);
      byte[] associatedData = initialAssociatedData(key, noncePrefix);
      return readEncryptedFragments(encryptedData, SALT_LENGTH + 1 + STORED_NONCE_LENGTH, key, noncePrefix, associatedData);
    } catch (Exception e) {
      throw new IllegalStateException("无法解密 madmin 兼容加密载荷", e);
    }
  }

  private static void writeEncryptedFragments(
      ByteArrayOutputStream output,
      byte[] key,
      byte[] noncePrefix,
      byte[] associatedData,
      byte[] data)
      throws Exception {
    int offset = 0;
    int sequence = 1;
    while (data.length - offset > BUFFER_SIZE) {
      output.write(encryptFragment(key, noncePrefix, sequence++, associatedData, copy(data, offset, BUFFER_SIZE)));
      offset += BUFFER_SIZE;
    }
    byte[] finalAssociatedData = associatedData.clone();
    finalAssociatedData[0] = (byte) 0x80;
    output.write(encryptFragment(key, noncePrefix, sequence, finalAssociatedData, copy(data, offset, data.length - offset)));
  }

  private static byte[] readEncryptedFragments(
      byte[] encryptedData, int offset, byte[] key, byte[] noncePrefix, byte[] associatedData)
      throws Exception {
    ByteArrayOutputStream plain = new ByteArrayOutputStream();
    int sequence = 1;
    int chunkCipherLength = BUFFER_SIZE + GCM_TAG_BYTES;
    while (encryptedData.length - offset > chunkCipherLength) {
      plain.write(decryptFragment(key, noncePrefix, sequence++, associatedData, copy(encryptedData, offset, chunkCipherLength)));
      offset += chunkCipherLength;
    }
    byte[] finalAssociatedData = associatedData.clone();
    finalAssociatedData[0] = (byte) 0x80;
    plain.write(decryptFragment(key, noncePrefix, sequence, finalAssociatedData, copy(encryptedData, offset, encryptedData.length - offset)));
    return plain.toByteArray();
  }

  private static byte[] initialAssociatedData(byte[] key, byte[] noncePrefix) throws Exception {
    byte[] associatedData = new byte[1 + GCM_TAG_BYTES];
    associatedData[0] = 0x00;
    byte[] tag = encryptFragment(key, noncePrefix, 0, new byte[0], new byte[0]);
    System.arraycopy(tag, 0, associatedData, 1, GCM_TAG_BYTES);
    return associatedData;
  }

  private static byte[] encryptFragment(
      byte[] key, byte[] noncePrefix, int sequence, byte[] associatedData, byte[] plainData)
      throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce(noncePrefix, sequence)));
    cipher.updateAAD(associatedData);
    return cipher.doFinal(plainData);
  }

  private static byte[] decryptFragment(
      byte[] key, byte[] noncePrefix, int sequence, byte[] associatedData, byte[] cipherText)
      throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce(noncePrefix, sequence)));
    cipher.updateAAD(associatedData);
    return cipher.doFinal(cipherText);
  }

  private static byte[] nonce(byte[] noncePrefix, int sequence) {
    byte[] nonce = new byte[GCM_NONCE_LENGTH];
    System.arraycopy(noncePrefix, 0, nonce, 0, STORED_NONCE_LENGTH);
    ByteBuffer.wrap(nonce, STORED_NONCE_LENGTH, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(sequence);
    return nonce;
  }

  private static byte[] pbkdf2(String secretKey, byte[] salt) throws Exception {
    PBEKeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt, PBKDF2_COST, 256);
    return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
  }

  private static byte[] randomBytes(int length) {
    byte[] bytes = new byte[length];
    RANDOM.nextBytes(bytes);
    return bytes;
  }

  private static byte[] copy(byte[] source, int offset, int length) {
    byte[] target = new byte[length];
    System.arraycopy(source, offset, target, 0, length);
    return target;
  }

  private static void requireSecret(String secretKey) {
    if (secretKey == null || secretKey.isEmpty()) {
      throw new IllegalArgumentException("secretKey 不能为空");
    }
  }
}
