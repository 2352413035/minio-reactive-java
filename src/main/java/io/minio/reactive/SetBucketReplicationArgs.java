package io.minio.reactive;

/** 设置 bucket replication XML 的参数对象。 */
public final class SetBucketReplicationArgs extends BucketArgs {
  private final String replicationXml;

  private SetBucketReplicationArgs(Builder builder) {
    super(builder);
    this.replicationXml = requireText("replicationXml", builder.replicationXml);
  }

  public String replicationXml() {
    return replicationXml;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends BucketArgs.AbstractBuilder<Builder> {
    private String replicationXml;

    public Builder replicationXml(String replicationXml) {
      this.replicationXml = replicationXml;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public SetBucketReplicationArgs build() {
      return new SetBucketReplicationArgs(this);
    }
  }
}
