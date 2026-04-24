package io.minio.reactive.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** ListMultipartUploads 的分页结果，包含未完成上传会话和下一页 marker。 */
public final class ListMultipartUploadsResult {
  private final String bucket;
  private final String prefix;
  private final String keyMarker;
  private final String uploadIdMarker;
  private final String nextKeyMarker;
  private final String nextUploadIdMarker;
  private final String delimiter;
  private final int maxUploads;
  private final boolean truncated;
  private final List<MultipartUploadInfo> uploads;
  private final List<String> commonPrefixes;

  public ListMultipartUploadsResult(
      String bucket,
      String prefix,
      String keyMarker,
      String uploadIdMarker,
      String nextKeyMarker,
      String nextUploadIdMarker,
      String delimiter,
      int maxUploads,
      boolean truncated,
      List<MultipartUploadInfo> uploads,
      List<String> commonPrefixes) {
    this.bucket = bucket == null ? "" : bucket;
    this.prefix = prefix == null ? "" : prefix;
    this.keyMarker = keyMarker == null ? "" : keyMarker;
    this.uploadIdMarker = uploadIdMarker == null ? "" : uploadIdMarker;
    this.nextKeyMarker = nextKeyMarker == null ? "" : nextKeyMarker;
    this.nextUploadIdMarker = nextUploadIdMarker == null ? "" : nextUploadIdMarker;
    this.delimiter = delimiter == null ? "" : delimiter;
    this.maxUploads = maxUploads;
    this.truncated = truncated;
    this.uploads = Collections.unmodifiableList(new ArrayList<MultipartUploadInfo>(uploads));
    this.commonPrefixes = Collections.unmodifiableList(new ArrayList<String>(commonPrefixes));
  }

  public String bucket() { return bucket; }

  public String prefix() { return prefix; }

  public String keyMarker() { return keyMarker; }

  public String uploadIdMarker() { return uploadIdMarker; }

  public String nextKeyMarker() { return nextKeyMarker; }

  public String nextUploadIdMarker() { return nextUploadIdMarker; }

  public String delimiter() { return delimiter; }

  public int maxUploads() { return maxUploads; }

  public boolean isTruncated() { return truncated; }

  public List<MultipartUploadInfo> uploads() { return uploads; }

  public List<String> commonPrefixes() { return commonPrefixes; }
}
