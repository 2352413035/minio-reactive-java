package io.minio.reactive;

/** 设置 bucket encryption XML 的参数对象。 */
public final class SetBucketEncryptionArgs extends BucketArgs {
  private final String encryptionXml;

  private SetBucketEncryptionArgs(Builder builder) {
    super(builder);
    this.encryptionXml = requireText("encryptionXml", builder.encryptionXml);
  }

  public String encryptionXml() {
    return encryptionXml;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends BucketArgs.AbstractBuilder<Builder> {
    private String encryptionXml;

    public Builder encryptionXml(String encryptionXml) {
      this.encryptionXml = encryptionXml;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public SetBucketEncryptionArgs build() {
      return new SetBucketEncryptionArgs(this);
    }
  }
}
