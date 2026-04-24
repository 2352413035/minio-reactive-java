package io.minio.reactive.http;

import io.minio.reactive.ReactiveMinioClientConfig;
import io.minio.reactive.util.S3Escaper;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

/**
 * S3 兼容请求的内部模型。
 *
 * <p>这个对象的意义不只是“装参数”，更重要的是让签名器和 HTTP 发送层
 * 使用同一份 method、URI、query、headers、body 语义，避免“签名看见的请求”和
 * “真正发出去的请求”不一致。
 */
public final class S3Request {
  private final HttpMethod method;
  private final String bucket;
  private final String object;
  private final String path;
  private final String region;
  private final String serviceName;
  private final Map<String, String> headers;
  private final Map<String, String> queryParameters;
  private final byte[] body;
  private final MediaType contentType;

  private S3Request(Builder builder) {
    this.method = Objects.requireNonNull(builder.method, "HTTP 方法不能为空");
    this.bucket = builder.bucket;
    this.object = builder.object;
    this.path = builder.path;
    this.region = builder.region;
    this.serviceName = builder.serviceName == null ? "s3" : builder.serviceName;
    this.headers = Collections.unmodifiableMap(new LinkedHashMap<String, String>(builder.headers));
    this.queryParameters =
        Collections.unmodifiableMap(new LinkedHashMap<String, String>(builder.queryParameters));
    this.body = builder.body;
    this.contentType =
        builder.contentType == null ? MediaType.APPLICATION_OCTET_STREAM : builder.contentType;
  }

  public static Builder builder() {
    return new Builder();
  }

  public HttpMethod method() {
    return method;
  }

  public String bucket() {
    return bucket;
  }

  public String object() {
    return object;
  }

  public String path() {
    return path;
  }

  public String region() {
    return region;
  }

  public String serviceName() {
    return serviceName;
  }

  public Map<String, String> headers() {
    return headers;
  }

  public Map<String, String> queryParameters() {
    return queryParameters;
  }

  public byte[] body() {
    return body;
  }

  public boolean hasBody() {
    return body != null;
  }

  public MediaType contentType() {
    return contentType;
  }

  public String canonicalUri() {
    if (path != null && !path.trim().isEmpty()) {
      return S3Escaper.canonicalPath(path);
    }
    return S3Escaper.canonicalUri(bucket, object);
  }

  public String canonicalQueryString() {
    return S3Escaper.canonicalQueryString(queryParameters);
  }

  public URI toUri(ReactiveMinioClientConfig config) {
    // 这里生成的 URI 文本必须与签名阶段使用的 canonical URI/query 保持一致。
    StringBuilder builder = new StringBuilder(config.endpoint());
    builder.append(canonicalUri());
    String canonicalQueryString = canonicalQueryString();
    if (!canonicalQueryString.isEmpty()) {
      builder.append('?').append(canonicalQueryString);
    }
    return URI.create(builder.toString());
  }

  public Builder toBuilder() {
    Builder builder = new Builder();
    builder.method = this.method;
    builder.bucket = this.bucket;
    builder.object = this.object;
    builder.path = this.path;
    builder.region = this.region;
    builder.serviceName = this.serviceName;
    builder.headers.putAll(this.headers);
    builder.queryParameters.putAll(this.queryParameters);
    builder.body = this.body;
    builder.contentType = this.contentType;
    return builder;
  }

  public static final class Builder {
    private HttpMethod method;
    private String bucket;
    private String object;
    private String path;
    private String region;
    private String serviceName;
    private final Map<String, String> headers = new LinkedHashMap<String, String>();
    private final Map<String, String> queryParameters = new LinkedHashMap<String, String>();
    private byte[] body;
    private MediaType contentType;

    private Builder() {}

    public Builder method(HttpMethod method) {
      this.method = method;
      return this;
    }

    public Builder bucket(String bucket) {
      this.bucket = bucket;
      return this;
    }

    public Builder object(String object) {
      this.object = object;
      return this;
    }

    public Builder path(String path) {
      this.path = path;
      return this;
    }

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    public Builder serviceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder header(String name, String value) {
      this.headers.put(name, value);
      return this;
    }

    public Builder queryParameter(String name, String value) {
      this.queryParameters.put(name, value);
      return this;
    }

    public Builder body(byte[] body) {
      this.body = body;
      return this;
    }

    public Builder contentType(String contentType) {
      this.contentType = MediaType.parseMediaType(contentType);
      return this;
    }

    public Builder contentType(MediaType contentType) {
      this.contentType = contentType;
      return this;
    }

    public S3Request build() {
      return new S3Request(this);
    }
  }
}
