package io.minio.reactive.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 分片列表响应的解析结果。 */
public final class ListPartsResult {
  private final String bucket;
  private final String key;
  private final String uploadId;
  private final boolean truncated;
  private final int nextPartNumberMarker;
  private final List<PartInfo> parts;

  public ListPartsResult(
      String bucket,
      String key,
      String uploadId,
      boolean truncated,
      int nextPartNumberMarker,
      List<PartInfo> parts) {
    this.bucket = bucket;
    this.key = key;
    this.uploadId = uploadId;
    this.truncated = truncated;
    this.nextPartNumberMarker = nextPartNumberMarker;
    this.parts = Collections.unmodifiableList(new ArrayList<PartInfo>(parts));
  }

  public String bucket() {
    return bucket;
  }

  public String key() {
    return key;
  }

  public String uploadId() {
    return uploadId;
  }

  public boolean isTruncated() {
    return truncated;
  }

  public int nextPartNumberMarker() {
    return nextPartNumberMarker;
  }

  public List<PartInfo> parts() {
    return parts;
  }
}
