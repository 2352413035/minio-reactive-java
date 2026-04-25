package io.minio.reactive.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MadminEncryptionSupportTest {
  @Test
  void shouldDetectKnownMadminEncryptedHeaderIds() {
    byte[] argonAes = new byte[41];
    byte[] argonChaCha = new byte[41];
    byte[] pbkdf = new byte[41];
    argonAes[32] = MadminEncryptionAlgorithm.ARGON2ID_AES_GCM.id();
    argonChaCha[32] = MadminEncryptionAlgorithm.ARGON2ID_CHACHA20_POLY1305.id();
    pbkdf[32] = MadminEncryptionAlgorithm.PBKDF2_AES_GCM.id();

    Assertions.assertTrue(MadminEncryptionSupport.isEncrypted(argonAes));
    Assertions.assertTrue(MadminEncryptionSupport.isEncrypted(argonChaCha));
    Assertions.assertTrue(MadminEncryptionSupport.isEncrypted(pbkdf));
    Assertions.assertEquals(
        MadminEncryptionAlgorithm.ARGON2ID_AES_GCM, MadminEncryptionSupport.algorithmOf(argonAes));
    Assertions.assertEquals(
        MadminEncryptionAlgorithm.ARGON2ID_CHACHA20_POLY1305,
        MadminEncryptionSupport.algorithmOf(argonChaCha));
    Assertions.assertEquals(
        MadminEncryptionAlgorithm.PBKDF2_AES_GCM, MadminEncryptionSupport.algorithmOf(pbkdf));
  }

  @Test
  void shouldExposeEncryptedAdminResponseAlgorithmForDiagnostics() {
    byte[] payload = new byte[41];
    payload[32] = MadminEncryptionAlgorithm.ARGON2ID_AES_GCM.id();
    io.minio.reactive.messages.admin.EncryptedAdminResponse response =
        new io.minio.reactive.messages.admin.EncryptedAdminResponse(payload);

    Assertions.assertTrue(response.isEncrypted());
    Assertions.assertEquals(MadminEncryptionAlgorithm.ARGON2ID_AES_GCM, response.algorithm());
    Assertions.assertEquals("Argon2id + AES-GCM", response.algorithmName());
    Assertions.assertEquals(41, response.encryptedSize());
    Assertions.assertFalse(response.decryptSupported());
    Assertions.assertTrue(response.requiresCryptoGate());
    Assertions.assertTrue(response.diagnosticMessage().contains("Crypto Gate"));
  }

  @Test
  void shouldExposeEncryptedAdminResponseSafeDiagnostics() {
    byte[] pbkdf = new byte[41];
    pbkdf[32] = MadminEncryptionAlgorithm.PBKDF2_AES_GCM.id();
    io.minio.reactive.messages.admin.EncryptedAdminResponse supported =
        new io.minio.reactive.messages.admin.EncryptedAdminResponse(pbkdf);
    io.minio.reactive.messages.admin.EncryptedAdminResponse plain =
        new io.minio.reactive.messages.admin.EncryptedAdminResponse(new byte[10]);
    io.minio.reactive.messages.admin.EncryptedAdminResponse empty =
        new io.minio.reactive.messages.admin.EncryptedAdminResponse(null);

    Assertions.assertTrue(supported.decryptSupported());
    Assertions.assertFalse(supported.requiresCryptoGate());
    Assertions.assertTrue(supported.diagnosticMessage().contains("当前支持"));
    Assertions.assertFalse(plain.isEncrypted());
    Assertions.assertEquals(10, plain.encryptedSize());
    Assertions.assertTrue(plain.diagnosticMessage().contains("不是已识别"));
    Assertions.assertEquals(0, empty.encryptedSize());
    Assertions.assertTrue(empty.diagnosticMessage().contains("未收到"));
  }

  @Test
  void shouldExposeOnlyPbkdf2AesGcmAsDecryptSupportedBeforeCryptoGatePass() {
    Assertions.assertFalse(MadminEncryptionAlgorithm.ARGON2ID_AES_GCM.decryptSupported());
    Assertions.assertFalse(
        MadminEncryptionAlgorithm.ARGON2ID_CHACHA20_POLY1305.decryptSupported());
    Assertions.assertTrue(MadminEncryptionAlgorithm.PBKDF2_AES_GCM.decryptSupported());
  }

  @Test
  void shouldRejectUnknownOrShortMadminEncryptedHeaderIds() {
    byte[] unknown = new byte[41];
    unknown[32] = 0x7F;

    Assertions.assertFalse(MadminEncryptionSupport.isEncrypted(null));
    Assertions.assertFalse(MadminEncryptionSupport.isEncrypted(new byte[32]));
    Assertions.assertFalse(MadminEncryptionSupport.isEncrypted(unknown));
  }

  @Test
  void shouldRoundTripPbkdf2AesGcmPayload() {
    byte[] plain = "{\"Version\":\"2012-10-17\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    byte[] encrypted = MadminEncryptionSupport.encryptData("secret", plain);
    byte[] decrypted = MadminEncryptionSupport.decryptData("secret", encrypted);

    Assertions.assertTrue(MadminEncryptionSupport.isEncrypted(encrypted));
    Assertions.assertArrayEquals(plain, decrypted);
  }

  @Test
  void shouldRoundTripLargePayloadAcrossFragments() {
    byte[] plain = new byte[(1 << 14) + 37];
    for (int i = 0; i < plain.length; i++) {
      plain[i] = (byte) (i % 251);
    }

    byte[] encrypted = MadminEncryptionSupport.encryptData("secret", plain);
    byte[] decrypted = MadminEncryptionSupport.decryptData("secret", encrypted);

    Assertions.assertArrayEquals(plain, decrypted);
  }

  @Test
  void shouldDecryptPbkdf2AesGcmMadminGoFixture() {
    byte[] fixture = readFixture("pbkdf2-aesgcm-go.base64");

    byte[] decrypted = MadminEncryptionSupport.decryptData("fixture-secret", fixture);

    Assertions.assertEquals(
        MadminEncryptionAlgorithm.PBKDF2_AES_GCM, MadminEncryptionSupport.algorithmOf(fixture));
    Assertions.assertEquals("madmin fixture payload", new String(decrypted, StandardCharsets.UTF_8));
  }

  @Test
  void shouldDecryptSupportedEncryptedAdminResponseWithExplicitSecretKey() {
    byte[] fixture = readFixture("pbkdf2-aesgcm-go.base64");
    io.minio.reactive.messages.admin.EncryptedAdminResponse response =
        new io.minio.reactive.messages.admin.EncryptedAdminResponse(fixture);

    Assertions.assertTrue(response.isEncrypted());
    Assertions.assertTrue(response.decryptSupported());
    Assertions.assertTrue(response.requiresSecretKey());
    Assertions.assertFalse(response.requiresCryptoGate());
    Assertions.assertEquals("madmin fixture payload", response.decryptAsUtf8("fixture-secret"));
    Assertions.assertArrayEquals(
        "madmin fixture payload".getBytes(StandardCharsets.UTF_8), response.decrypt("fixture-secret"));
  }

  @Test
  void shouldKeepDefaultArgon2idResponseAsEncryptedBoundaryWhenDecryptFails() {
    byte[] fixture = readFixture("argon2id-aesgcm-go-default.base64");
    io.minio.reactive.messages.admin.EncryptedAdminResponse response =
        new io.minio.reactive.messages.admin.EncryptedAdminResponse(fixture);

    IllegalArgumentException error =
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> response.decryptAsUtf8("fixture-secret"));

    Assertions.assertTrue(response.isEncrypted());
    Assertions.assertFalse(response.decryptSupported());
    Assertions.assertTrue(response.requiresSecretKey());
    Assertions.assertTrue(response.requiresCryptoGate());
    Assertions.assertTrue(error.getMessage().contains("Argon2id + AES-GCM"));
    Assertions.assertTrue(response.diagnosticMessage().contains("Crypto Gate"));
  }

  @Test
  void shouldDiagnoseUnsupportedDefaultArgon2idAesGcmFixture() {
    byte[] fixture = readFixture("argon2id-aesgcm-go-default.base64");

    IllegalArgumentException error =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> MadminEncryptionSupport.decryptData("fixture-secret", fixture));

    Assertions.assertTrue(MadminEncryptionSupport.isEncrypted(fixture));
    Assertions.assertEquals(
        MadminEncryptionAlgorithm.ARGON2ID_AES_GCM, MadminEncryptionSupport.algorithmOf(fixture));
    Assertions.assertTrue(error.getMessage().contains("当前只支持 PBKDF2 AES-GCM"));
    Assertions.assertTrue(error.getMessage().contains("Argon2id + AES-GCM"));
  }

  @Test
  void shouldDiagnoseUnsupportedArgon2idChaCha20Header() {
    byte[] fixture = new byte[32 + 1 + 8 + 16];
    fixture[32] = MadminEncryptionAlgorithm.ARGON2ID_CHACHA20_POLY1305.id();

    IllegalArgumentException error =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> MadminEncryptionSupport.decryptData("fixture-secret", fixture));

    Assertions.assertTrue(MadminEncryptionSupport.isEncrypted(fixture));
    Assertions.assertEquals(
        MadminEncryptionAlgorithm.ARGON2ID_CHACHA20_POLY1305,
        MadminEncryptionSupport.algorithmOf(fixture));
    Assertions.assertTrue(error.getMessage().contains("ChaCha20-Poly1305"));
  }


  @Test
  void shouldDiagnoseUnsupportedDefaultArgon2idChaChaFixtureWhenPresent() {
    org.junit.jupiter.api.Assumptions.assumeTrue(
        fixtureExists("argon2id-chacha20-go-default.base64"),
        "当前仓库或外部 fixture 目录未提供 Argon2id + ChaCha20-Poly1305 fixture");
    byte[] fixture = readFixture("argon2id-chacha20-go-default.base64");

    IllegalArgumentException error =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> MadminEncryptionSupport.decryptData("fixture-secret", fixture));

    Assertions.assertEquals(
        MadminEncryptionAlgorithm.ARGON2ID_CHACHA20_POLY1305,
        MadminEncryptionSupport.algorithmOf(fixture));
    Assertions.assertTrue(error.getMessage().contains("ChaCha20-Poly1305"));
  }


  private static boolean fixtureExists(String name) {
    String fixtureDir = System.getenv("MADMIN_FIXTURE_DIR");
    if (fixtureDir != null && !fixtureDir.trim().isEmpty()) {
      return new java.io.File(fixtureDir, name).isFile();
    }
    return MadminEncryptionSupportTest.class.getResource("/madmin-fixtures/" + name) != null;
  }

  private static byte[] readFixture(String name) {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      String fixtureDir = System.getenv("MADMIN_FIXTURE_DIR");
      InputStream classpathInput =
          fixtureDir == null || fixtureDir.trim().isEmpty()
              ? MadminEncryptionSupportTest.class.getResourceAsStream("/madmin-fixtures/" + name)
              : null;
      InputStream fileInput =
          fixtureDir == null || fixtureDir.trim().isEmpty()
              ? null
              : new java.io.FileInputStream(new java.io.File(fixtureDir, name));
      try (InputStream input = fileInput != null ? fileInput : classpathInput) {
        if (input == null) {
          throw new IllegalStateException("找不到 madmin 测试夹具: " + name);
        }
        byte[] buffer = new byte[1024];
        int read;
        while ((read = input.read(buffer)) >= 0) {
          output.write(buffer, 0, read);
        }
      }
      StringBuilder base64 = new StringBuilder();
      String text = new String(output.toByteArray(), StandardCharsets.UTF_8);
      String[] lines = text.split("\\r?\\n");
      for (String line : lines) {
        String trimmed = line.trim();
        if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
          base64.append(trimmed);
        }
      }
      return Base64.getDecoder().decode(base64.toString());
    } catch (Exception e) {
      throw new IllegalStateException("无法读取 madmin 测试夹具: " + name, e);
    }
  }
}
