package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 管理端配置帮助响应，等价于 madmin-go 的 Help/HelpKV 结构。 */
public final class AdminConfigHelp extends AdminJsonResult {
  private final String subSys;
  private final String description;
  private final boolean multipleTargets;
  private final List<KeyHelp> keysHelp;

  private AdminConfigHelp(
      String rawJson,
      String subSys,
      String description,
      boolean multipleTargets,
      List<KeyHelp> keysHelp) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.subSys = subSys;
    this.description = description;
    this.multipleTargets = multipleTargets;
    this.keysHelp = Collections.unmodifiableList(new ArrayList<KeyHelp>(keysHelp));
  }

  public static AdminConfigHelp parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    List<KeyHelp> keys = new ArrayList<KeyHelp>();
    JsonNode source = JsonSupport.child(root, "keysHelp", "KeysHelp");
    if (source != null && source.isArray()) {
      for (JsonNode item : source) {
        keys.add(KeyHelp.parse(item));
      }
    }
    return new AdminConfigHelp(
        rawJson,
        JsonSupport.textAny(root, "subSys", "SubSys"),
        JsonSupport.textAny(root, "description", "Description"),
        JsonSupport.booleanAny(root, "multipleTargets", "MultipleTargets"),
        keys);
  }

  public String subSys() {
    return subSys;
  }

  public String description() {
    return description;
  }

  public boolean multipleTargets() {
    return multipleTargets;
  }

  public List<KeyHelp> keysHelp() {
    return keysHelp;
  }

  /** 只返回配置 key 名称，便于调用方构建下拉选择或做存在性判断。 */
  public List<String> keys() {
    List<String> keys = new ArrayList<String>();
    for (KeyHelp help : keysHelp) {
      keys.add(help.key());
    }
    return Collections.unmodifiableList(keys);
  }

  /** 单个配置 key 的帮助信息。 */
  public static final class KeyHelp {
    private final String key;
    private final String description;
    private final boolean optional;
    private final String type;
    private final boolean multipleTargets;

    private KeyHelp(
        String key, String description, boolean optional, String type, boolean multipleTargets) {
      this.key = key;
      this.description = description;
      this.optional = optional;
      this.type = type;
      this.multipleTargets = multipleTargets;
    }

    private static KeyHelp parse(JsonNode node) {
      return new KeyHelp(
          JsonSupport.textAny(node, "key", "Key"),
          JsonSupport.textAny(node, "description", "Description"),
          JsonSupport.booleanAny(node, "optional", "Optional"),
          JsonSupport.textAny(node, "type", "Type"),
          JsonSupport.booleanAny(node, "multipleTargets", "MultipleTargets"));
    }

    public String key() {
      return key;
    }

    public String description() {
      return description;
    }

    public boolean optional() {
      return optional;
    }

    public String type() {
      return type;
    }

    public boolean multipleTargets() {
      return multipleTargets;
    }
  }
}
