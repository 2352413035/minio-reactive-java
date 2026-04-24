package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** 管理端数据用量摘要，提取对象数、桶数和容量相关字段。 */
public final class AdminDataUsageSummary extends AdminJsonResult {
  private final long objectsCount;
  private final long objectsTotalSize;
  private final long bucketsCount;
  private final long totalCapacity;
  private final long totalFreeCapacity;
  private final long totalUsedCapacity;

  private AdminDataUsageSummary(
      String rawJson,
      long objectsCount,
      long objectsTotalSize,
      long bucketsCount,
      long totalCapacity,
      long totalFreeCapacity,
      long totalUsedCapacity) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.objectsCount = objectsCount;
    this.objectsTotalSize = objectsTotalSize;
    this.bucketsCount = bucketsCount;
    this.totalCapacity = totalCapacity;
    this.totalFreeCapacity = totalFreeCapacity;
    this.totalUsedCapacity = totalUsedCapacity;
  }

  public static AdminDataUsageSummary parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    return new AdminDataUsageSummary(
        rawJson,
        JsonSupport.longAny(root, "objectsCount", "ObjectsTotalCount"),
        JsonSupport.longAny(root, "objectsTotalSize", "ObjectsTotalSize"),
        JsonSupport.longAny(root, "bucketsCount", "BucketsCount"),
        JsonSupport.longAny(root, "capacity", "Capacity", "totalCapacity"),
        JsonSupport.longAny(root, "freeCapacity", "FreeCapacity", "totalFreeCapacity"),
        JsonSupport.longAny(root, "usedCapacity", "UsedCapacity", "totalUsedCapacity"));
  }

  public long objectsCount() {
    return objectsCount;
  }

  public long objectsTotalSize() {
    return objectsTotalSize;
  }

  public long bucketsCount() {
    return bucketsCount;
  }

  public long totalCapacity() {
    return totalCapacity;
  }

  public long totalFreeCapacity() {
    return totalFreeCapacity;
  }

  public long totalUsedCapacity() {
    return totalUsedCapacity;
  }
}
