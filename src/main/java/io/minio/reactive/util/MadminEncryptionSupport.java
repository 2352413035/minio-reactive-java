package io.minio.reactive.util;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * MinIO madmin 加密载荷的兼容性辅助工具。
 *
 * <p>madmin-go 的加密数据格式为：32 字节 salt、1 字节算法 ID、8 字节 nonce、后续 DARE
 * 加密流。阶段 111 起本工具对齐 minio-java：使用 Bouncy Castle 支持 Argon2id + AES-GCM
 * 与 Argon2id + ChaCha20-Poly1305，同时保留 PBKDF2 + AES-GCM 以兼容 FIPS/历史夹具。
 */
public final class MadminEncryptionSupport {
  private static final int SALT_LENGTH = 32;
  private static final int STORED_NONCE_LENGTH = 8;
  private static final int GCM_NONCE_LENGTH = 12;
  private static final int GCM_TAG_BITS = 128;
  private static final int GCM_TAG_BYTES = 16;
  private static final int BUFFER_SIZE = 1 << 14;
  private static final int PBKDF2_COST = 8192;
  private static final int ARGON2ID_TIME = 1;
  private static final int ARGON2ID_MEMORY_KB = 64 * 1024;
  private static final int ARGON2ID_THREADS = 4;
  private static final int ALGORITHM_OFFSET = 32;
  private static final SecureRandom RANDOM = new SecureRandom();

  private MadminEncryptionSupport() {}

  /** 判断数据是否看起来像 madmin-go EncryptData 生成的加密载荷。 */
  public static boolean isEncrypted(byte[] data) {
    return algorithmOf(data) != null;
  }

  /**
   * 读取 madmin-go 载荷头部中的算法 ID。
   *
   * <p>返回 null 表示载荷太短或算法 ID 不在 madmin-go 公开格式范围内。
   */
  public static MadminEncryptionAlgorithm algorithmOf(byte[] data) {
    if (data == null || data.length <= SALT_LENGTH) {
      return null;
    }
    return MadminEncryptionAlgorithm.fromId(data[ALGORITHM_OFFSET]);
  }

  /**
   * 使用 madmin-go 兼容的 PBKDF2 + AES-GCM 格式加密数据。
   *
   * <p>SDK 发往 MinIO 管理端的写请求继续默认使用 PBKDF2 + AES-GCM：它是 madmin-go 支持的
   * FIPS 路径，成本低且兼容旧阶段已有 lab 证据。读取服务端默认响应时，{@link #decryptData(String, byte[])}
   * 已支持 Argon2id 系列。
   */
  public static byte[] encryptData(String secretKey, byte[] plainData) {
    return encryptData(secretKey, plainData, MadminEncryptionAlgorithm.PBKDF2_AES_GCM);
  }

  /**
   * 使用指定 madmin 算法加密数据。
   *
   * <p>该入口主要用于互操作测试和未来高级场景。普通 Admin 写接口仍应走 {@link #encryptData(String, byte[])}，
   * 避免把默认写入成本提升为 Argon2id。
   */
  public static byte[] encryptData(
      String secretKey, byte[] plainData, MadminEncryptionAlgorithm algorithm) {
    requireSecret(secretKey);
    if (algorithm == null || !algorithm.decryptSupported()) {
      throw new IllegalArgumentException("madmin 加密算法当前不可用");
    }
    byte[] data = plainData == null ? new byte[0] : plainData;
    try {
      byte[] salt = randomBytes(SALT_LENGTH);
      byte[] noncePrefix = randomBytes(STORED_NONCE_LENGTH);
      byte[] key = deriveKey(algorithm, secretKey, salt);
      byte[] associatedData = initialAssociatedData(algorithm, key, noncePrefix);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      output.write(salt);
      output.write(algorithm.id());
      output.write(noncePrefix);
      writeEncryptedFragments(output, algorithm, key, noncePrefix, associatedData, data);
      return output.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("无法生成 madmin 兼容加密载荷", e);
    }
  }

  /** 解密 madmin-go 兼容加密载荷；调用方必须显式提供对应账号的 secretKey。 */
  public static byte[] decryptData(String secretKey, byte[] encryptedData) {
    requireSecret(secretKey);
    if (encryptedData == null || encryptedData.length < SALT_LENGTH + 1 + STORED_NONCE_LENGTH + GCM_TAG_BYTES) {
      throw new IllegalArgumentException("madmin 加密载荷长度不足");
    }
    MadminEncryptionAlgorithm algorithm = algorithmOf(encryptedData);
    if (algorithm == null) {
      throw new IllegalArgumentException("madmin 加密载荷算法 ID 无法识别");
    }
    if (!algorithm.decryptSupported()) {
      throw new IllegalArgumentException("当前 Java 端暂不支持 " + algorithm.displayName() + " madmin 加密载荷");
    }
    try {
      byte[] salt = copy(encryptedData, 0, SALT_LENGTH);
      byte[] noncePrefix = copy(encryptedData, SALT_LENGTH + 1, STORED_NONCE_LENGTH);
      byte[] key = deriveKey(algorithm, secretKey, salt);
      byte[] associatedData = initialAssociatedData(algorithm, key, noncePrefix);
      return readEncryptedFragments(
          encryptedData, SALT_LENGTH + 1 + STORED_NONCE_LENGTH, algorithm, key, noncePrefix, associatedData);
    } catch (Exception e) {
      throw new IllegalStateException("无法解密 madmin 兼容加密载荷", e);
    }
  }

  private static void writeEncryptedFragments(
      ByteArrayOutputStream output,
      MadminEncryptionAlgorithm algorithm,
      byte[] key,
      byte[] noncePrefix,
      byte[] associatedData,
      byte[] data)
      throws Exception {
    int offset = 0;
    int sequence = 1;
    while (data.length - offset > BUFFER_SIZE) {
      output.write(
          encryptFragment(algorithm, key, noncePrefix, sequence++, associatedData, copy(data, offset, BUFFER_SIZE)));
      offset += BUFFER_SIZE;
    }
    byte[] finalAssociatedData = associatedData.clone();
    finalAssociatedData[0] = (byte) 0x80;
    output.write(
        encryptFragment(algorithm, key, noncePrefix, sequence, finalAssociatedData, copy(data, offset, data.length - offset)));
  }

  private static byte[] readEncryptedFragments(
      byte[] encryptedData,
      int offset,
      MadminEncryptionAlgorithm algorithm,
      byte[] key,
      byte[] noncePrefix,
      byte[] associatedData)
      throws Exception {
    ByteArrayOutputStream plain = new ByteArrayOutputStream();
    int sequence = 1;
    int chunkCipherLength = BUFFER_SIZE + GCM_TAG_BYTES;
    while (encryptedData.length - offset > chunkCipherLength) {
      plain.write(
          decryptFragment(
              algorithm, key, noncePrefix, sequence++, associatedData, copy(encryptedData, offset, chunkCipherLength)));
      offset += chunkCipherLength;
    }
    byte[] finalAssociatedData = associatedData.clone();
    finalAssociatedData[0] = (byte) 0x80;
    plain.write(
        decryptFragment(
            algorithm, key, noncePrefix, sequence, finalAssociatedData, copy(encryptedData, offset, encryptedData.length - offset)));
    return plain.toByteArray();
  }

  private static byte[] initialAssociatedData(
      MadminEncryptionAlgorithm algorithm, byte[] key, byte[] noncePrefix) throws Exception {
    byte[] associatedData = new byte[1 + GCM_TAG_BYTES];
    associatedData[0] = 0x00;
    byte[] tag = encryptFragment(algorithm, key, noncePrefix, 0, new byte[0], new byte[0]);
    System.arraycopy(tag, 0, associatedData, 1, GCM_TAG_BYTES);
    return associatedData;
  }

  private static byte[] encryptFragment(
      MadminEncryptionAlgorithm algorithm,
      byte[] key,
      byte[] noncePrefix,
      int sequence,
      byte[] associatedData,
      byte[] plainData)
      throws Exception {
    if (algorithm == MadminEncryptionAlgorithm.ARGON2ID_CHACHA20_POLY1305) {
      return bouncyAead(true, key, nonce(noncePrefix, sequence), associatedData, plainData);
    }
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(
        Cipher.ENCRYPT_MODE,
        new SecretKeySpec(key, "AES"),
        new GCMParameterSpec(GCM_TAG_BITS, nonce(noncePrefix, sequence)));
    cipher.updateAAD(associatedData);
    return cipher.doFinal(plainData);
  }

  private static byte[] decryptFragment(
      MadminEncryptionAlgorithm algorithm,
      byte[] key,
      byte[] noncePrefix,
      int sequence,
      byte[] associatedData,
      byte[] cipherText)
      throws Exception {
    if (algorithm == MadminEncryptionAlgorithm.ARGON2ID_CHACHA20_POLY1305) {
      return bouncyAead(false, key, nonce(noncePrefix, sequence), associatedData, cipherText);
    }
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(
        Cipher.DECRYPT_MODE,
        new SecretKeySpec(key, "AES"),
        new GCMParameterSpec(GCM_TAG_BITS, nonce(noncePrefix, sequence)));
    cipher.updateAAD(associatedData);
    return cipher.doFinal(cipherText);
  }

  private static byte[] bouncyAead(
      boolean encrypt, byte[] key, byte[] nonce, byte[] associatedData, byte[] input)
      throws InvalidCipherTextException {
    AEADCipher cipher = new ChaCha20Poly1305();
    cipher.init(encrypt, new AEADParameters(new KeyParameter(key), GCM_TAG_BITS, nonce));
    cipher.processAADBytes(associatedData, 0, associatedData.length);
    byte[] output = new byte[cipher.getOutputSize(input.length)];
    int offset = cipher.processBytes(input, 0, input.length, output, 0);
    int total = offset + cipher.doFinal(output, offset);
    if (total == output.length) {
      return output;
    }
    return copy(output, 0, total);
  }

  private static byte[] nonce(byte[] noncePrefix, int sequence) {
    byte[] nonce = new byte[GCM_NONCE_LENGTH];
    System.arraycopy(noncePrefix, 0, nonce, 0, STORED_NONCE_LENGTH);
    ByteBuffer.wrap(nonce, STORED_NONCE_LENGTH, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(sequence);
    return nonce;
  }

  private static byte[] deriveKey(
      MadminEncryptionAlgorithm algorithm, String secretKey, byte[] salt) throws Exception {
    if (algorithm == MadminEncryptionAlgorithm.PBKDF2_AES_GCM) {
      return pbkdf2(secretKey, salt);
    }
    return argon2id(secretKey, salt);
  }

  private static byte[] pbkdf2(String secretKey, byte[] salt) throws Exception {
    PBEKeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt, PBKDF2_COST, 256);
    return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
  }

  private static byte[] argon2id(String secretKey, byte[] salt) {
    Argon2BytesGenerator generator = new Argon2BytesGenerator();
    generator.init(
        new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt)
            .withMemoryAsKB(ARGON2ID_MEMORY_KB)
            .withParallelism(ARGON2ID_THREADS)
            .withIterations(ARGON2ID_TIME)
            .build());
    byte[] key = new byte[32];
    generator.generateBytes(secretKey.getBytes(StandardCharsets.UTF_8), key);
    return key;
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
