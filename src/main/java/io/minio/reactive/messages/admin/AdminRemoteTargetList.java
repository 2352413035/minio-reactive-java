package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** bucket remote target 只读摘要。 */
public final class AdminRemoteTargetList extends AdminJsonResult {
  private final List<Target> targets;

  private AdminRemoteTargetList(String rawJson, List<Target> targets) {
    super(rawJson, safeMap(rawJson));
    this.targets = Collections.unmodifiableList(new ArrayList<Target>(targets));
  }

  public static AdminRemoteTargetList parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    JsonNode array = JsonSupport.child(root, "Targets", "targets", "RemoteTargets", "remoteTargets");
    if (array == null && root != null && root.isArray()) {
      array = root;
    }
    List<Target> result = new ArrayList<Target>();
    if (array != null && array.isArray()) {
      for (JsonNode item : array) {
        result.add(
            new Target(
                JsonSupport.textAny(item, "Arn", "arn", "ARN"),
                JsonSupport.textAny(item, "Type", "type"),
                JsonSupport.textAny(item, "Endpoint", "endpoint"),
                JsonSupport.booleanAny(item, "Secure", "secure")));
      }
    }
    return new AdminRemoteTargetList(rawJson, result);
  }

  public List<Target> targets() {
    return targets;
  }

  public int targetCount() {
    return targets.size();
  }

  /** 单个 remote target 的安全摘要，不暴露凭据字段。 */
  public static final class Target {
    private final String arn;
    private final String type;
    private final String endpoint;
    private final boolean secure;

    private Target(String arn, String type, String endpoint, boolean secure) {
      this.arn = arn;
      this.type = type;
      this.endpoint = endpoint;
      this.secure = secure;
    }

    public String arn() {
      return arn;
    }

    public String type() {
      return type;
    }

    public String endpoint() {
      return endpoint;
    }

    public boolean secure() {
      return secure;
    }
  }

  private static java.util.Map<String, Object> safeMap(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    return root != null && root.isObject()
        ? JsonSupport.parseMap(rawJson)
        : java.util.Collections.<String, Object>emptyMap();
  }
}
