package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** Rebalance 状态摘要，保留完整 JSON，同时提取任务 ID、停止时间和 pool 运行计数。 */
public final class AdminRebalanceStatus extends AdminJsonResult {
  private final String operationId;
  private final String stoppedAt;
  private final int poolCount;
  private final int activePoolCount;

  private AdminRebalanceStatus(
      String rawJson, String operationId, String stoppedAt, int poolCount, int activePoolCount) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.operationId = operationId;
    this.stoppedAt = stoppedAt;
    this.poolCount = poolCount;
    this.activePoolCount = activePoolCount;
  }

  public static AdminRebalanceStatus parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    JsonNode pools = JsonSupport.child(root, "pools", "Pools");
    return new AdminRebalanceStatus(
        rawJson,
        JsonSupport.textAny(root, "ID", "id"),
        JsonSupport.textAny(root, "stoppedAt", "StoppedAt", "stopped_at"),
        JsonSupport.nodeSize(pools),
        countActivePools(pools));
  }

  private static int countActivePools(JsonNode pools) {
    if (pools == null || !pools.isArray()) {
      return 0;
    }
    int count = 0;
    for (JsonNode pool : pools) {
      if (!JsonSupport.textAny(pool, "status", "Status").trim().isEmpty()) {
        count++;
      }
    }
    return count;
  }

  public String operationId() {
    return operationId;
  }

  public String stoppedAt() {
    return stoppedAt;
  }

  public int poolCount() {
    return poolCount;
  }

  public int activePoolCount() {
    return activePoolCount;
  }
}
