package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** 站点复制基础信息摘要，避免暴露服务账号 access key，仅统计是否存在。 */
public final class AdminSiteReplicationInfoSummary extends AdminJsonResult {
  private final boolean enabled;
  private final String name;
  private final int siteCount;
  private final boolean serviceAccountAccessKeyPresent;
  private final String apiVersion;

  private AdminSiteReplicationInfoSummary(
      String rawJson,
      boolean enabled,
      String name,
      int siteCount,
      boolean serviceAccountAccessKeyPresent,
      String apiVersion) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.enabled = enabled;
    this.name = name;
    this.siteCount = siteCount;
    this.serviceAccountAccessKeyPresent = serviceAccountAccessKeyPresent;
    this.apiVersion = apiVersion;
  }

  public static AdminSiteReplicationInfoSummary parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    String accessKey = JsonSupport.textAny(root, "serviceAccountAccessKey", "ServiceAccountAccessKey");
    return new AdminSiteReplicationInfoSummary(
        rawJson,
        JsonSupport.booleanAny(root, "enabled", "Enabled"),
        JsonSupport.textAny(root, "name", "Name"),
        JsonSupport.nodeSize(JsonSupport.child(root, "sites", "Sites")),
        !accessKey.isEmpty(),
        JsonSupport.textAny(root, "apiVersion", "APIVersion"));
  }

  public boolean enabled() {
    return enabled;
  }

  public String name() {
    return name;
  }

  public int siteCount() {
    return siteCount;
  }

  public boolean serviceAccountAccessKeyPresent() {
    return serviceAccountAccessKeyPresent;
  }

  public String apiVersion() {
    return apiVersion;
  }
}
