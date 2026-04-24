package io.minio.reactive.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** bucket CORS 配置。 */
public final class BucketCorsConfiguration {
  private final List<BucketCorsRule> rules;

  private BucketCorsConfiguration(List<BucketCorsRule> rules) {
    this.rules =
        rules == null
            ? Collections.<BucketCorsRule>emptyList()
            : Collections.unmodifiableList(new ArrayList<BucketCorsRule>(rules));
  }

  public static BucketCorsConfiguration of(List<BucketCorsRule> rules) {
    return new BucketCorsConfiguration(rules);
  }

  public static BucketCorsConfiguration empty() {
    return new BucketCorsConfiguration(Collections.<BucketCorsRule>emptyList());
  }

  public List<BucketCorsRule> rules() {
    return rules;
  }

  public boolean isEmpty() {
    return rules.isEmpty();
  }
}
