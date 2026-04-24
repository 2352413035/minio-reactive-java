package io.minio.reactive.messages;

/** 对象 Legal Hold 配置，使用 S3 兼容的 ON/OFF 状态。 */
public final class ObjectLegalHoldConfiguration {
  public static final String ON = "ON";
  public static final String OFF = "OFF";

  private final String status;

  private ObjectLegalHoldConfiguration(String status) {
    this.status = normalizeStatus(status);
  }

  public static ObjectLegalHoldConfiguration of(String status) {
    return new ObjectLegalHoldConfiguration(status);
  }

  public static ObjectLegalHoldConfiguration enabled() {
    return of(ON);
  }

  public static ObjectLegalHoldConfiguration disabled() {
    return of(OFF);
  }

  public String status() {
    return status;
  }

  public boolean enabledValue() {
    return ON.equals(status);
  }

  private static String normalizeStatus(String status) {
    if (status == null || status.trim().isEmpty()) {
      return "";
    }
    String value = status.trim().toUpperCase(java.util.Locale.ROOT);
    if (!ON.equals(value) && !OFF.equals(value)) {
      throw new IllegalArgumentException("Legal Hold 状态只能是 ON 或 OFF");
    }
    return value;
  }
}
