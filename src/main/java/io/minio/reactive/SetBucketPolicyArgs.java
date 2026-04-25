package io.minio.reactive;

/** 设置 bucket policy JSON 的参数对象。 */
public final class SetBucketPolicyArgs extends BucketArgs {
  private final String policyJson;

  private SetBucketPolicyArgs(Builder builder) {
    super(builder);
    this.policyJson = requireText("policyJson", builder.policyJson);
  }

  public String policyJson() {
    return policyJson;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends BucketArgs.AbstractBuilder<Builder> {
    private String policyJson;

    public Builder policyJson(String policyJson) {
      this.policyJson = policyJson;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public SetBucketPolicyArgs build() {
      return new SetBucketPolicyArgs(this);
    }
  }
}
