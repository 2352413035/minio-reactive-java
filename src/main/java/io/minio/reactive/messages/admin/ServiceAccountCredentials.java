package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** 服务账号创建响应中的凭证信息。 */
public final class ServiceAccountCredentials {
  private final String accessKey;
  private final String secretKey;
  private final String sessionToken;
  private final String expiration;

  private ServiceAccountCredentials(String accessKey, String secretKey, String sessionToken, String expiration) {
    this.accessKey = accessKey == null ? "" : accessKey;
    this.secretKey = secretKey == null ? "" : secretKey;
    this.sessionToken = sessionToken == null ? "" : sessionToken;
    this.expiration = expiration == null ? "" : expiration;
  }

  public static ServiceAccountCredentials parse(JsonNode node) {
    return new ServiceAccountCredentials(
        JsonSupport.text(node, "accessKey"),
        JsonSupport.text(node, "secretKey"),
        JsonSupport.text(node, "sessionToken"),
        JsonSupport.text(node, "expiration"));
  }

  public String accessKey() { return accessKey; }
  public String secretKey() { return secretKey; }
  public String sessionToken() { return sessionToken; }
  public String expiration() { return expiration; }
}
