package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** 管理端存储信息摘要，保留完整 JSON，同时提取磁盘和后端状态的常用字段。 */
public final class AdminStorageSummary extends AdminJsonResult {
  private final int diskCount;
  private final int onlineDiskCount;
  private final int offlineDiskCount;
  private final int healingDiskCount;
  private final String backendType;

  private AdminStorageSummary(
      String rawJson,
      int diskCount,
      int onlineDiskCount,
      int offlineDiskCount,
      int healingDiskCount,
      String backendType) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.diskCount = diskCount;
    this.onlineDiskCount = onlineDiskCount;
    this.offlineDiskCount = offlineDiskCount;
    this.healingDiskCount = healingDiskCount;
    this.backendType = backendType;
  }

  public static AdminStorageSummary parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    JsonNode disks = JsonSupport.child(root, "Disks", "disks");
    JsonNode backend = JsonSupport.child(root, "Backend", "backend");
    return new AdminStorageSummary(
        rawJson,
        disks == null || !disks.isArray() ? 0 : disks.size(),
        JsonSupport.sumNumericObjectValues(JsonSupport.child(backend, "OnlineDisks", "onlineDisks")),
        JsonSupport.sumNumericObjectValues(JsonSupport.child(backend, "OfflineDisks", "offlineDisks")),
        countHealingDisks(disks),
        JsonSupport.textAny(backend, "Type", "type"));
  }

  private static int countHealingDisks(JsonNode disks) {
    if (disks == null || !disks.isArray()) {
      return 0;
    }
    int count = 0;
    for (JsonNode disk : disks) {
      if (JsonSupport.booleanAny(disk, "healing", "Healing")) {
        count++;
      }
    }
    return count;
  }

  public int diskCount() {
    return diskCount;
  }

  public int onlineDiskCount() {
    return onlineDiskCount;
  }

  public int offlineDiskCount() {
    return offlineDiskCount;
  }

  public int healingDiskCount() {
    return healingDiskCount;
  }

  public String backendType() {
    return backendType;
  }
}
