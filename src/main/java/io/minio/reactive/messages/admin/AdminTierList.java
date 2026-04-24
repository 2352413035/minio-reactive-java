package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 远端 tier 配置列表摘要，保留原始 JSON，但不暴露可能被 MinIO 脱敏的凭据字段。 */
public final class AdminTierList {
  private final String rawJson;
  private final List<Tier> tiers;

  private AdminTierList(String rawJson, List<Tier> tiers) {
    this.rawJson = rawJson == null ? "" : rawJson;
    this.tiers = Collections.unmodifiableList(new ArrayList<Tier>(tiers));
  }

  public static AdminTierList parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    List<Tier> result = new ArrayList<Tier>();
    if (root != null && root.isArray()) {
      for (JsonNode item : root) {
        result.add(
            new Tier(
                JsonSupport.textAny(item, "Name", "name"),
                JsonSupport.textAny(item, "Type", "type"),
                JsonSupport.textAny(item, "Version", "version")));
      }
    }
    return new AdminTierList(rawJson, result);
  }

  public String rawJson() {
    return rawJson;
  }

  public List<Tier> tiers() {
    return tiers;
  }

  public int tierCount() {
    return tiers.size();
  }

  /** 单个 tier 的公开摘要；凭据字段应继续留在原始加密/脱敏响应里，不做普通 getter。 */
  public static final class Tier {
    private final String name;
    private final String type;
    private final String version;

    private Tier(String name, String type, String version) {
      this.name = name;
      this.type = type;
      this.version = version;
    }

    public String name() {
      return name;
    }

    public String type() {
      return type;
    }

    public String version() {
      return version;
    }
  }
}
