package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Tier 统计摘要，保留完整 JSON，同时汇总 tier 名称、对象数、版本数和容量。 */
public final class AdminTierStatsSummary extends AdminJsonResult {
  private final List<String> tierNames;
  private final long totalSize;
  private final long totalVersions;
  private final long totalObjects;

  private AdminTierStatsSummary(
      String rawJson, List<String> tierNames, long totalSize, long totalVersions, long totalObjects) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.tierNames = Collections.unmodifiableList(new ArrayList<String>(tierNames));
    this.totalSize = totalSize;
    this.totalVersions = totalVersions;
    this.totalObjects = totalObjects;
  }

  public static AdminTierStatsSummary parse(String rawJson) {
    JsonNode tiers = tiersNode(JsonSupport.parseTree(rawJson));
    List<String> names = new ArrayList<String>();
    long size = 0L;
    long versions = 0L;
    long objects = 0L;
    if (tiers != null && tiers.isArray()) {
      for (JsonNode tier : tiers) {
        String name = JsonSupport.textAny(tier, "Name", "name");
        if (!name.isEmpty()) {
          names.add(name);
        }
        JsonNode stats = JsonSupport.child(tier, "Stats", "stats");
        size += JsonSupport.longAny(stats, "totalSize", "TotalSize");
        versions += JsonSupport.longAny(stats, "numVersions", "NumVersions");
        objects += JsonSupport.longAny(stats, "numObjects", "NumObjects");
      }
    }
    return new AdminTierStatsSummary(rawJson, names, size, versions, objects);
  }

  private static JsonNode tiersNode(JsonNode root) {
    if (root != null && root.isArray()) {
      return root;
    }
    return JsonSupport.child(root, "tiers", "Tiers", "items");
  }

  public int tierCount() {
    return tierNames.size();
  }

  public List<String> tierNames() {
    return tierNames;
  }

  public long totalSize() {
    return totalSize;
  }

  public long totalVersions() {
    return totalVersions;
  }

  public long totalObjects() {
    return totalObjects;
  }
}
