package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import io.minio.reactive.util.MadminEncryptionSupport;
import java.nio.charset.StandardCharsets;

/** 创建服务账号后的响应结果。 */
public final class ServiceAccountCreateResult {
  private final boolean encrypted;
  private final EncryptedAdminResponse encryptedResponse;
  private final ServiceAccountCredentials credentials;
  private final String rawJson;

  private ServiceAccountCreateResult(boolean encrypted, EncryptedAdminResponse encryptedResponse, ServiceAccountCredentials credentials, String rawJson) {
    this.encrypted = encrypted;
    this.encryptedResponse = encryptedResponse;
    this.credentials = credentials;
    this.rawJson = rawJson == null ? "" : rawJson;
  }

  public static ServiceAccountCreateResult fromResponseBytes(byte[] responseBytes) {
    byte[] bytes = responseBytes == null ? new byte[0] : responseBytes;
    if (MadminEncryptionSupport.isEncrypted(bytes)) {
      return new ServiceAccountCreateResult(true, new EncryptedAdminResponse(bytes), null, "");
    }
    String json = new String(bytes, StandardCharsets.UTF_8);
    if (json.trim().isEmpty()) {
      return new ServiceAccountCreateResult(false, null, null, "");
    }
    JsonNode root = JsonSupport.parseTree(json);
    JsonNode credentials = root.get("credentials");
    return new ServiceAccountCreateResult(false, null, credentials == null ? null : ServiceAccountCredentials.parse(credentials), json);
  }

  public boolean encrypted() { return encrypted; }
  public EncryptedAdminResponse encryptedResponse() { return encryptedResponse; }
  public ServiceAccountCredentials credentials() { return credentials; }
  public String rawJson() { return rawJson; }

  /** 使用调用方显式提供的 secretKey 解密并解析服务账号创建结果。 */
  public ServiceAccountCreateResult decrypt(String secretKey) {
    if (!encrypted) {
      return this;
    }
    return fromResponseBytes(encryptedResponse.decrypt(secretKey));
  }
}
