package io.minio.reactive.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** bucket notification 的单个目标配置。 */
public final class BucketNotificationTarget {
  private final String type;
  private final String id;
  private final String arn;
  private final List<String> events;
  private final String filterPrefix;
  private final String filterSuffix;

  public BucketNotificationTarget(
      String type, String id, String arn, List<String> events, String filterPrefix, String filterSuffix) {
    this.type = normalize(type);
    this.id = normalize(id);
    this.arn = normalize(arn);
    this.events = immutableCopy(events);
    this.filterPrefix = normalize(filterPrefix);
    this.filterSuffix = normalize(filterSuffix);
  }

  public static BucketNotificationTarget queue(String arn, List<String> events) {
    return new BucketNotificationTarget("Queue", "", arn, events, "", "");
  }

  public static BucketNotificationTarget topic(String arn, List<String> events) {
    return new BucketNotificationTarget("Topic", "", arn, events, "", "");
  }

  public static BucketNotificationTarget cloudFunction(String arn, List<String> events) {
    return new BucketNotificationTarget("CloudFunction", "", arn, events, "", "");
  }

  public String type() {
    return type;
  }

  public String id() {
    return id;
  }

  public String arn() {
    return arn;
  }

  public List<String> events() {
    return events;
  }

  public String filterPrefix() {
    return filterPrefix;
  }

  public String filterSuffix() {
    return filterSuffix;
  }

  private static List<String> immutableCopy(List<String> values) {
    if (values == null || values.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> result = new ArrayList<String>();
    for (String value : values) {
      if (value != null && !value.trim().isEmpty()) {
        result.add(value.trim());
      }
    }
    return Collections.unmodifiableList(result);
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim();
  }
}
