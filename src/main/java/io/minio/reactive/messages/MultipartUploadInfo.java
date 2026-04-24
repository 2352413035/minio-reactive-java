package io.minio.reactive.messages;

/** ListMultipartUploads 响应中的分片上传会话摘要。 */
public final class MultipartUploadInfo {
  private final String key;
  private final String uploadId;
  private final String initiated;
  private final String storageClass;
  private final String ownerId;
  private final String ownerDisplayName;

  public MultipartUploadInfo(
      String key,
      String uploadId,
      String initiated,
      String storageClass,
      String ownerId,
      String ownerDisplayName) {
    this.key = key == null ? "" : key;
    this.uploadId = uploadId == null ? "" : uploadId;
    this.initiated = initiated == null ? "" : initiated;
    this.storageClass = storageClass == null ? "" : storageClass;
    this.ownerId = ownerId == null ? "" : ownerId;
    this.ownerDisplayName = ownerDisplayName == null ? "" : ownerDisplayName;
  }

  public String key() { return key; }

  public String uploadId() { return uploadId; }

  public String initiated() { return initiated; }

  public String storageClass() { return storageClass; }

  public String ownerId() { return ownerId; }

  public String ownerDisplayName() { return ownerDisplayName; }
}
