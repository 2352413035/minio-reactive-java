package io.minio.reactive;

/** 创建 multipart upload 的参数对象。 */
public final class CreateMultipartUploadArgs extends ObjectArgs {
  private final String contentType;

  private CreateMultipartUploadArgs(Builder builder) { super(builder); this.contentType = builder.contentType; }
  public String contentType() { return contentType; }
  public static Builder builder() { return new Builder(); }
  public static final class Builder extends ObjectArgs.AbstractBuilder<Builder> {
    private String contentType;
    public Builder contentType(String contentType) { this.contentType = contentType; return this; }
    @Override protected Builder self() { return this; }
    public CreateMultipartUploadArgs build() { return new CreateMultipartUploadArgs(this); }
  }
}
