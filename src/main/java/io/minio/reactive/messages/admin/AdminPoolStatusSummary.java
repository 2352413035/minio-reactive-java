package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** Pool 状态摘要，保留完整 JSON，同时提取 decommission 进度和风险标志。 */
public final class AdminPoolStatusSummary extends AdminJsonResult {
  private final int id;
  private final String commandLine;
  private final String lastUpdate;
  private final boolean decommissionInfoPresent;
  private final boolean decommissionComplete;
  private final boolean decommissionFailed;
  private final boolean decommissionCanceled;
  private final long totalSize;
  private final long currentSize;
  private final long objectsDecommissioned;
  private final long objectsDecommissionedFailed;
  private final long bytesDecommissioned;
  private final long bytesDecommissionedFailed;

  private AdminPoolStatusSummary(
      String rawJson,
      int id,
      String commandLine,
      String lastUpdate,
      boolean decommissionInfoPresent,
      boolean decommissionComplete,
      boolean decommissionFailed,
      boolean decommissionCanceled,
      long totalSize,
      long currentSize,
      long objectsDecommissioned,
      long objectsDecommissionedFailed,
      long bytesDecommissioned,
      long bytesDecommissionedFailed) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.id = id;
    this.commandLine = commandLine;
    this.lastUpdate = lastUpdate;
    this.decommissionInfoPresent = decommissionInfoPresent;
    this.decommissionComplete = decommissionComplete;
    this.decommissionFailed = decommissionFailed;
    this.decommissionCanceled = decommissionCanceled;
    this.totalSize = totalSize;
    this.currentSize = currentSize;
    this.objectsDecommissioned = objectsDecommissioned;
    this.objectsDecommissionedFailed = objectsDecommissionedFailed;
    this.bytesDecommissioned = bytesDecommissioned;
    this.bytesDecommissionedFailed = bytesDecommissionedFailed;
  }

  public static AdminPoolStatusSummary parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    return fromNode(rawJson, root);
  }

  static AdminPoolStatusSummary fromNode(String rawJson, JsonNode pool) {
    JsonNode decommission = JsonSupport.child(pool, "decommissionInfo", "Decommission", "decommission");
    return new AdminPoolStatusSummary(
        rawJson,
        JsonSupport.intAny(pool, "id", "ID"),
        JsonSupport.textAny(pool, "cmdline", "CmdLine", "commandLine"),
        JsonSupport.textAny(pool, "lastUpdate", "LastUpdate"),
        decommission != null && !decommission.isNull(),
        JsonSupport.booleanAny(decommission, "complete", "Complete"),
        JsonSupport.booleanAny(decommission, "failed", "Failed"),
        JsonSupport.booleanAny(decommission, "canceled", "Canceled"),
        JsonSupport.longAny(decommission, "totalSize", "TotalSize"),
        JsonSupport.longAny(decommission, "currentSize", "CurrentSize"),
        JsonSupport.longAny(decommission, "objectsDecommissioned", "ItemsDecommissioned"),
        JsonSupport.longAny(decommission, "objectsDecommissionedFailed", "ItemsDecommissionFailed"),
        JsonSupport.longAny(decommission, "bytesDecommissioned", "BytesDone"),
        JsonSupport.longAny(decommission, "bytesDecommissionedFailed", "BytesFailed"));
  }

  boolean decommissionActive() {
    return decommissionInfoPresent && !decommissionComplete && !decommissionFailed && !decommissionCanceled;
  }

  public int id() {
    return id;
  }

  public String commandLine() {
    return commandLine;
  }

  public String lastUpdate() {
    return lastUpdate;
  }

  public boolean decommissionInfoPresent() {
    return decommissionInfoPresent;
  }

  public boolean decommissionComplete() {
    return decommissionComplete;
  }

  public boolean decommissionFailed() {
    return decommissionFailed;
  }

  public boolean decommissionCanceled() {
    return decommissionCanceled;
  }

  public long totalSize() {
    return totalSize;
  }

  public long currentSize() {
    return currentSize;
  }

  public long objectsDecommissioned() {
    return objectsDecommissioned;
  }

  public long objectsDecommissionedFailed() {
    return objectsDecommissionedFailed;
  }

  public long bytesDecommissioned() {
    return bytesDecommissioned;
  }

  public long bytesDecommissionedFailed() {
    return bytesDecommissionedFailed;
  }
}
