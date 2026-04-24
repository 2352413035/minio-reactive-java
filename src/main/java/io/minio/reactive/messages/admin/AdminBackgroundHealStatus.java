package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** 后台 heal 状态摘要，保留完整 JSON，同时提取排障时最常看的计数。 */
public final class AdminBackgroundHealStatus extends AdminJsonResult {
  private final long scannedItemsCount;
  private final int offlineEndpointCount;
  private final int healDiskCount;
  private final int setCount;
  private final int mrfNodeCount;
  private final int storageClassParityCount;

  private AdminBackgroundHealStatus(
      String rawJson,
      long scannedItemsCount,
      int offlineEndpointCount,
      int healDiskCount,
      int setCount,
      int mrfNodeCount,
      int storageClassParityCount) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.scannedItemsCount = scannedItemsCount;
    this.offlineEndpointCount = offlineEndpointCount;
    this.healDiskCount = healDiskCount;
    this.setCount = setCount;
    this.mrfNodeCount = mrfNodeCount;
    this.storageClassParityCount = storageClassParityCount;
  }

  public static AdminBackgroundHealStatus parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    return new AdminBackgroundHealStatus(
        rawJson,
        JsonSupport.longAny(root, "ScannedItemsCount", "scannedItemsCount", "scanned_items_count"),
        JsonSupport.nodeSize(JsonSupport.child(root, "offline_nodes", "OfflineEndpoints", "offlineEndpoints")),
        JsonSupport.nodeSize(JsonSupport.child(root, "HealDisks", "healDisks", "heal_disks")),
        JsonSupport.nodeSize(JsonSupport.child(root, "sets", "Sets")),
        JsonSupport.nodeSize(JsonSupport.child(root, "mrf", "MRF")),
        JsonSupport.nodeSize(JsonSupport.child(root, "sc_parity", "SCParity", "scParity")));
  }

  public long scannedItemsCount() {
    return scannedItemsCount;
  }

  public int offlineEndpointCount() {
    return offlineEndpointCount;
  }

  public int healDiskCount() {
    return healDiskCount;
  }

  public int setCount() {
    return setCount;
  }

  public int mrfNodeCount() {
    return mrfNodeCount;
  }

  public int storageClassParityCount() {
    return storageClassParityCount;
  }
}
