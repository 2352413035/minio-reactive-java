package io.minio.reactive.messages.kms;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** KMS key 状态检查结果。 */
public final class KmsKeyStatus extends KmsJsonResult {
  private final String keyId;
  private final String encryptionError;
  private final String decryptionError;

  private KmsKeyStatus(String rawJson, String keyId, String encryptionError, String decryptionError) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.keyId = keyId;
    this.encryptionError = encryptionError;
    this.decryptionError = decryptionError;
  }

  public static KmsKeyStatus parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    return new KmsKeyStatus(
        rawJson,
        JsonSupport.text(root, "key-id"),
        JsonSupport.text(root, "encryptionErr"),
        JsonSupport.text(root, "decryptionErr"));
  }

  public String keyId() {
    return keyId;
  }

  public String encryptionError() {
    return encryptionError;
  }

  public String decryptionError() {
    return decryptionError;
  }

  public boolean isOk() {
    return encryptionError.isEmpty() && decryptionError.isEmpty();
  }
}
