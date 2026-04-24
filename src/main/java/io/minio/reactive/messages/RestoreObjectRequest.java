package io.minio.reactive.messages;

/**
 * 对象恢复请求。
 *
 * <p>MinIO/S3 restore 主要用于归档对象恢复。当前模型覆盖最常见的恢复天数和恢复优先级；
 * 更复杂的 select restore 仍可使用 advanced 字符串入口。
 */
public final class RestoreObjectRequest {
  private final int days;
  private final String tier;

  private RestoreObjectRequest(int days, String tier) {
    if (days <= 0) {
      throw new IllegalArgumentException("恢复天数必须大于 0");
    }
    this.days = days;
    this.tier = tier == null ? "" : tier.trim();
  }

  public static RestoreObjectRequest of(int days) {
    return new RestoreObjectRequest(days, "");
  }

  public static RestoreObjectRequest of(int days, String tier) {
    return new RestoreObjectRequest(days, tier);
  }

  public int days() {
    return days;
  }

  public String tier() {
    return tier;
  }
}
