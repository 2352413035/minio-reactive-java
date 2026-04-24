package io.minio.reactive.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** bucket notification 配置。 */
public final class BucketNotificationConfiguration {
  private final List<BucketNotificationTarget> targets;
  private final String rawXml;

  public BucketNotificationConfiguration(List<BucketNotificationTarget> targets, String rawXml) {
    this.targets =
        targets == null
            ? Collections.<BucketNotificationTarget>emptyList()
            : Collections.unmodifiableList(new ArrayList<BucketNotificationTarget>(targets));
    this.rawXml = rawXml == null ? "" : rawXml;
  }

  public static BucketNotificationConfiguration of(List<BucketNotificationTarget> targets) {
    return new BucketNotificationConfiguration(targets, "");
  }

  public static BucketNotificationConfiguration empty() {
    return new BucketNotificationConfiguration(Collections.<BucketNotificationTarget>emptyList(), "");
  }

  public List<BucketNotificationTarget> targets() {
    return targets;
  }

  public String rawXml() {
    return rawXml;
  }

  public boolean isEmpty() {
    return targets.isEmpty();
  }
}
