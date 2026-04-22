package io.minio.reactive.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MadminEncryptionSupportTest {
  @Test
  void shouldDetectKnownMadminEncryptedHeaderIds() {
    byte[] argonAes = new byte[41];
    byte[] argonChaCha = new byte[41];
    byte[] pbkdf = new byte[41];
    argonAes[32] = 0x00;
    argonChaCha[32] = 0x01;
    pbkdf[32] = 0x02;

    Assertions.assertTrue(MadminEncryptionSupport.isEncrypted(argonAes));
    Assertions.assertTrue(MadminEncryptionSupport.isEncrypted(argonChaCha));
    Assertions.assertTrue(MadminEncryptionSupport.isEncrypted(pbkdf));
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
  void shouldFailFastForUnsupportedEncryption() {
    Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> MadminEncryptionSupport.encryptData("secret", "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }
}
