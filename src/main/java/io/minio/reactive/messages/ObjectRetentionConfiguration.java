package io.minio.reactive.messages;

/**
 * 对象保留策略配置。
 *
 * <p>S3 只接受 `GOVERNANCE` 与 `COMPLIANCE` 两种模式；保留到期时间使用服务端兼容的
 * ISO-8601 字符串表示，调用方可以按自己的时间库生成。
 */
public final class ObjectRetentionConfiguration {
  public static final String GOVERNANCE = "GOVERNANCE";
  public static final String COMPLIANCE = "COMPLIANCE";

  private final String mode;
  private final String retainUntilDate;

  private ObjectRetentionConfiguration(String mode, String retainUntilDate) {
    this.mode = normalizeMode(mode);
    this.retainUntilDate = retainUntilDate == null ? "" : retainUntilDate.trim();
  }

  public static ObjectRetentionConfiguration of(String mode, String retainUntilDate) {
    return new ObjectRetentionConfiguration(mode, retainUntilDate);
  }

  public static ObjectRetentionConfiguration governance(String retainUntilDate) {
    return of(GOVERNANCE, retainUntilDate);
  }

  public static ObjectRetentionConfiguration compliance(String retainUntilDate) {
    return of(COMPLIANCE, retainUntilDate);
  }

  public String mode() {
    return mode;
  }

  public String retainUntilDate() {
    return retainUntilDate;
  }

  public boolean isEnabled() {
    return !mode.isEmpty() && !retainUntilDate.isEmpty();
  }

  private static String normalizeMode(String mode) {
    if (mode == null || mode.trim().isEmpty()) {
      return "";
    }
    String value = mode.trim().toUpperCase(java.util.Locale.ROOT);
    if (!GOVERNANCE.equals(value) && !COMPLIANCE.equals(value)) {
      throw new IllegalArgumentException("对象保留模式只能是 GOVERNANCE 或 COMPLIANCE");
    }
    return value;
  }
}
