package io.minio.reactive.messages;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 对象写入类操作的轻量结果。
 *
 * <p>当前用于 appendObject 等 MinIO 扩展写入接口。S3/MinIO 的写入响应通常把 ETag、
 * versionId 等信息放在响应头里，因此这里保留完整 headers，同时提取常用字段方便业务读取。
 */
public final class ObjectWriteResult {
  private final String bucket;
  private final String object;
  private final String etag;
  private final String versionId;
  private final Map<String, List<String>> headers;

  public ObjectWriteResult(String bucket, String object, Map<String, List<String>> headers) {
    this.bucket = bucket;
    this.object = object;
    this.headers = headers == null ? Collections.<String, List<String>>emptyMap() : headers;
    this.etag = firstHeader(this.headers, "ETag");
    this.versionId = firstHeader(this.headers, "x-amz-version-id");
  }

  public String bucket() {
    return bucket;
  }

  public String object() {
    return object;
  }

  public String etag() {
    return etag;
  }

  public String versionId() {
    return versionId;
  }

  public Map<String, List<String>> headers() {
    return headers;
  }

  private static String firstHeader(Map<String, List<String>> headers, String name) {
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(name) && !entry.getValue().isEmpty()) {
        return entry.getValue().get(0);
      }
    }
    return "";
  }
}
