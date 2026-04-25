package io.minio.reactive;

import io.minio.reactive.messages.CompletePart;
import java.util.List;

/** 完成 multipart upload 的参数对象。 */
public final class CompleteMultipartUploadArgs extends ObjectArgs {
  private final String uploadId;
  private final List<CompletePart> parts;

  private CompleteMultipartUploadArgs(Builder builder) {
    super(builder);
    this.uploadId = requireText("uploadId", builder.uploadId);
    this.parts = copyList(builder.parts, "parts");
  }
  public String uploadId() { return uploadId; }
  public List<CompletePart> parts() { return parts; }
  public static Builder builder() { return new Builder(); }
  public static final class Builder extends ObjectArgs.AbstractBuilder<Builder> {
    private String uploadId; private Iterable<CompletePart> parts;
    public Builder uploadId(String uploadId) { this.uploadId = uploadId; return this; }
    public Builder parts(Iterable<CompletePart> parts) { this.parts = parts; return this; }
    @Override protected Builder self() { return this; }
    public CompleteMultipartUploadArgs build() { return new CompleteMultipartUploadArgs(this); }
  }
}
