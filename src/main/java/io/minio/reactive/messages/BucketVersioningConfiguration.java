package io.minio.reactive.messages;

/** Bucket versioning 配置。 */
public final class BucketVersioningConfiguration {
  private final String status;
  private final String mfaDelete;

  private BucketVersioningConfiguration(String status, String mfaDelete) {
    this.status = status == null ? "" : status;
    this.mfaDelete = mfaDelete == null ? "" : mfaDelete;
  }

  public static BucketVersioningConfiguration enabled() {
    return new BucketVersioningConfiguration("Enabled", "");
  }

  public static BucketVersioningConfiguration suspended() {
    return new BucketVersioningConfiguration("Suspended", "");
  }

  public static BucketVersioningConfiguration of(String status, String mfaDelete) {
    return new BucketVersioningConfiguration(status, mfaDelete);
  }

  public String status() { return status; }
  public String mfaDelete() { return mfaDelete; }
  public boolean enabledStatus() { return "Enabled".equalsIgnoreCase(status); }
  public boolean suspendedStatus() { return "Suspended".equalsIgnoreCase(status); }
}
