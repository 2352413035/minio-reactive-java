package io.minio.reactive;

/** 终止 multipart upload 的参数对象。 */
public final class AbortMultipartUploadArgs extends ObjectArgs {
  private final String uploadId;
  private AbortMultipartUploadArgs(Builder builder) { super(builder); this.uploadId = requireText("uploadId", builder.uploadId); }
  public String uploadId() { return uploadId; }
  public static Builder builder() { return new Builder(); }
  public static final class Builder extends ObjectArgs.AbstractBuilder<Builder> {
    private String uploadId;
    public Builder uploadId(String uploadId) { this.uploadId = uploadId; return this; }
    @Override protected Builder self() { return this; }
    public AbortMultipartUploadArgs build() { return new AbortMultipartUploadArgs(this); }
  }
}
