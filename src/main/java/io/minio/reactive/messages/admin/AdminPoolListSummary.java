package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** Pool 列表摘要，保留完整 JSON，同时汇总 pool 数量和 decommission 状态计数。 */
public final class AdminPoolListSummary extends AdminJsonResult {
  private final int poolCount;
  private final int decommissionInfoCount;
  private final int activeDecommissionCount;
  private final int completedDecommissionCount;
  private final int failedDecommissionCount;
  private final int canceledDecommissionCount;
  private final long totalSize;
  private final long currentSize;

  private AdminPoolListSummary(
      String rawJson,
      int poolCount,
      int decommissionInfoCount,
      int activeDecommissionCount,
      int completedDecommissionCount,
      int failedDecommissionCount,
      int canceledDecommissionCount,
      long totalSize,
      long currentSize) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.poolCount = poolCount;
    this.decommissionInfoCount = decommissionInfoCount;
    this.activeDecommissionCount = activeDecommissionCount;
    this.completedDecommissionCount = completedDecommissionCount;
    this.failedDecommissionCount = failedDecommissionCount;
    this.canceledDecommissionCount = canceledDecommissionCount;
    this.totalSize = totalSize;
    this.currentSize = currentSize;
  }

  public static AdminPoolListSummary parse(String rawJson) {
    JsonNode pools = poolsNode(JsonSupport.parseTree(rawJson));
    int decommissionCount = 0;
    int activeCount = 0;
    int completeCount = 0;
    int failedCount = 0;
    int canceledCount = 0;
    long total = 0L;
    long current = 0L;
    if (pools != null && pools.isArray()) {
      for (JsonNode pool : pools) {
        AdminPoolStatusSummary summary = AdminPoolStatusSummary.fromNode(pool.toString(), pool);
        if (summary.decommissionInfoPresent()) {
          decommissionCount++;
          total += summary.totalSize();
          current += summary.currentSize();
        }
        if (summary.decommissionActive()) {
          activeCount++;
        }
        if (summary.decommissionComplete()) {
          completeCount++;
        }
        if (summary.decommissionFailed()) {
          failedCount++;
        }
        if (summary.decommissionCanceled()) {
          canceledCount++;
        }
      }
    }
    return new AdminPoolListSummary(
        rawJson,
        JsonSupport.nodeSize(pools),
        decommissionCount,
        activeCount,
        completeCount,
        failedCount,
        canceledCount,
        total,
        current);
  }

  private static JsonNode poolsNode(JsonNode root) {
    if (root != null && root.isArray()) {
      return root;
    }
    return JsonSupport.child(root, "pools", "Pools", "items");
  }

  public int poolCount() {
    return poolCount;
  }

  public int decommissionInfoCount() {
    return decommissionInfoCount;
  }

  public int activeDecommissionCount() {
    return activeDecommissionCount;
  }

  public int completedDecommissionCount() {
    return completedDecommissionCount;
  }

  public int failedDecommissionCount() {
    return failedDecommissionCount;
  }

  public int canceledDecommissionCount() {
    return canceledDecommissionCount;
  }

  public long totalSize() {
    return totalSize;
  }

  public long currentSize() {
    return currentSize;
  }
}
