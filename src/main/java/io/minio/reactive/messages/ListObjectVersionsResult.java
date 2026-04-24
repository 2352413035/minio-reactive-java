package io.minio.reactive.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** ListObjectVersions 的分页结果，包含对象版本、删除标记和下一页 marker。 */
public final class ListObjectVersionsResult {
  private final String bucket;
  private final String prefix;
  private final String keyMarker;
  private final String versionIdMarker;
  private final String nextKeyMarker;
  private final String nextVersionIdMarker;
  private final String delimiter;
  private final int maxKeys;
  private final boolean truncated;
  private final List<ObjectVersionInfo> versions;
  private final List<DeleteMarkerInfo> deleteMarkers;
  private final List<String> commonPrefixes;

  public ListObjectVersionsResult(
      String bucket,
      String prefix,
      String keyMarker,
      String versionIdMarker,
      String nextKeyMarker,
      String nextVersionIdMarker,
      String delimiter,
      int maxKeys,
      boolean truncated,
      List<ObjectVersionInfo> versions,
      List<DeleteMarkerInfo> deleteMarkers,
      List<String> commonPrefixes) {
    this.bucket = bucket == null ? "" : bucket;
    this.prefix = prefix == null ? "" : prefix;
    this.keyMarker = keyMarker == null ? "" : keyMarker;
    this.versionIdMarker = versionIdMarker == null ? "" : versionIdMarker;
    this.nextKeyMarker = nextKeyMarker == null ? "" : nextKeyMarker;
    this.nextVersionIdMarker = nextVersionIdMarker == null ? "" : nextVersionIdMarker;
    this.delimiter = delimiter == null ? "" : delimiter;
    this.maxKeys = maxKeys;
    this.truncated = truncated;
    this.versions = Collections.unmodifiableList(new ArrayList<ObjectVersionInfo>(versions));
    this.deleteMarkers = Collections.unmodifiableList(new ArrayList<DeleteMarkerInfo>(deleteMarkers));
    this.commonPrefixes = Collections.unmodifiableList(new ArrayList<String>(commonPrefixes));
  }

  public String bucket() { return bucket; }

  public String prefix() { return prefix; }

  public String keyMarker() { return keyMarker; }

  public String versionIdMarker() { return versionIdMarker; }

  public String nextKeyMarker() { return nextKeyMarker; }

  public String nextVersionIdMarker() { return nextVersionIdMarker; }

  public String delimiter() { return delimiter; }

  public int maxKeys() { return maxKeys; }

  public boolean isTruncated() { return truncated; }

  public List<ObjectVersionInfo> versions() { return versions; }

  public List<DeleteMarkerInfo> deleteMarkers() { return deleteMarkers; }

  public List<String> commonPrefixes() { return commonPrefixes; }
}
