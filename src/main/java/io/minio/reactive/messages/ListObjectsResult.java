package io.minio.reactive.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 对象列表 V2 单页响应的解析结果。 */
public final class ListObjectsResult {
  private final String name;
  private final String prefix;
  private final String delimiter;
  private final boolean truncated;
  private final String continuationToken;
  private final String nextContinuationToken;
  private final List<ObjectInfo> contents;
  private final List<String> commonPrefixes;

  public ListObjectsResult(
      String name,
      String prefix,
      String delimiter,
      boolean truncated,
      String continuationToken,
      String nextContinuationToken,
      List<ObjectInfo> contents,
      List<String> commonPrefixes) {
    this.name = name;
    this.prefix = prefix;
    this.delimiter = delimiter;
    this.truncated = truncated;
    this.continuationToken = continuationToken;
    this.nextContinuationToken = nextContinuationToken;
    this.contents = Collections.unmodifiableList(new ArrayList<ObjectInfo>(contents));
    this.commonPrefixes = Collections.unmodifiableList(new ArrayList<String>(commonPrefixes));
  }

  public String name() {
    return name;
  }

  public String prefix() {
    return prefix;
  }

  public String delimiter() {
    return delimiter;
  }

  public boolean isTruncated() {
    return truncated;
  }

  public String continuationToken() {
    return continuationToken;
  }

  public String nextContinuationToken() {
    return nextContinuationToken;
  }

  public List<ObjectInfo> contents() {
    return contents;
  }

  public List<String> commonPrefixes() {
    return commonPrefixes;
  }
}
