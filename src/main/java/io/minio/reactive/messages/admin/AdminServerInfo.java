package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** 管理端 ServerInfo 响应中相对稳定的摘要信息。 */
public final class AdminServerInfo extends AdminJsonResult {
  private final String mode;
  private final String region;
  private final String deploymentId;
  private final int bucketCount;
  private final long objectCount;
  private final int serverCount;

  private AdminServerInfo(
      String rawJson,
      String mode,
      String region,
      String deploymentId,
      int bucketCount,
      long objectCount,
      int serverCount) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.mode = mode;
    this.region = region;
    this.deploymentId = deploymentId;
    this.bucketCount = bucketCount;
    this.objectCount = objectCount;
    this.serverCount = serverCount;
  }

  public static AdminServerInfo parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    JsonNode buckets = root.get("buckets");
    JsonNode objects = root.get("objects");
    JsonNode servers = root.get("servers");
    return new AdminServerInfo(
        rawJson,
        JsonSupport.text(root, "mode"),
        JsonSupport.text(root, "region"),
        JsonSupport.text(root, "deploymentID"),
        JsonSupport.intValue(buckets, "count"),
        JsonSupport.longValue(objects, "count"),
        servers == null || !servers.isArray() ? 0 : servers.size());
  }

  public String mode() {
    return mode;
  }

  public String region() {
    return region;
  }

  public String deploymentId() {
    return deploymentId;
  }

  public int bucketCount() {
    return bucketCount;
  }

  public long objectCount() {
    return objectCount;
  }

  public int serverCount() {
    return serverCount;
  }
}
