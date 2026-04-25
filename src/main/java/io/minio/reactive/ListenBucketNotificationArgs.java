package io.minio.reactive;

/** 监听 bucket 通知的参数对象。 */
public final class ListenBucketNotificationArgs extends BucketArgs {
  private final String events;

  private ListenBucketNotificationArgs(Builder builder) {
    super(builder);
    this.events = requireText("events", builder.events);
  }

  public String events() {
    return events;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends BucketArgs.AbstractBuilder<Builder> {
    private String events;

    public Builder events(String events) {
      this.events = events;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public ListenBucketNotificationArgs build() {
      return new ListenBucketNotificationArgs(this);
    }
  }
}
