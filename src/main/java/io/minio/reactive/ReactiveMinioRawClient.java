package io.minio.reactive;

import io.minio.reactive.catalog.MinioApiEndpoint;
import io.minio.reactive.credentials.ReactiveCredentials;
import io.minio.reactive.credentials.ReactiveCredentialsProvider;
import io.minio.reactive.credentials.StaticCredentialsProvider;
import io.minio.reactive.http.ReactiveHttpClient;
import io.minio.reactive.http.S3Request;
import io.minio.reactive.signer.S3RequestSigner;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Raw reactive executor for the complete MinIO HTTP route catalog.
 *
 * <p>This class is intentionally protocol-level: it lets the SDK expose every MinIO server interface
 * even when a higher-level typed request/response model has not been introduced yet.
 */
public final class ReactiveMinioRawClient {
  private static final Pattern PATH_VARIABLE = Pattern.compile("\\{([^}/:]+)(?::[^}]*)?}");

  private final ReactiveMinioClientConfig config;
  private final ReactiveCredentialsProvider credentialsProvider;
  private final ReactiveHttpClient httpClient;
  private final S3RequestSigner signer;

  ReactiveMinioRawClient(
      ReactiveMinioClientConfig config,
      ReactiveCredentialsProvider credentialsProvider,
      ReactiveHttpClient httpClient,
      S3RequestSigner signer) {
    this.config = config;
    this.credentialsProvider = credentialsProvider;
    this.httpClient = httpClient;
    this.signer = signer;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Mono<Integer> executeToStatus(MinioApiEndpoint endpoint) {
    return executeToStatus(
        endpoint,
        Collections.<String, String>emptyMap(),
        Collections.<String, String>emptyMap(),
        Collections.<String, String>emptyMap(),
        null,
        null);
  }

  public Mono<Integer> executeToStatus(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return prepare(endpoint, pathVariables, queryParameters, headers, body, contentType)
        .flatMap(httpClient::exchangeToStatus);
  }

  public Mono<String> executeToString(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return prepare(endpoint, pathVariables, queryParameters, headers, body, contentType)
        .flatMap(httpClient::exchangeToString);
  }

  public Mono<byte[]> executeToBytes(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return prepare(endpoint, pathVariables, queryParameters, headers, body, contentType)
        .flatMap(httpClient::exchangeToByteArray);
  }

  public Flux<byte[]> executeToBody(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return prepare(endpoint, pathVariables, queryParameters, headers, body, contentType)
        .flatMapMany(httpClient::exchangeToBody);
  }

  public Mono<Map<String, List<String>>> executeToHeaders(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return prepare(endpoint, pathVariables, queryParameters, headers, body, contentType)
        .flatMap(httpClient::exchangeToHeaders);
  }

  public Mono<Void> executeToVoid(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return prepare(endpoint, pathVariables, queryParameters, headers, body, contentType)
        .flatMap(httpClient::exchangeToVoid);
  }

  public S3Request requestFor(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    Map<String, String> safePathVariables = safe(pathVariables);
    Map<String, String> safeQueryParameters = safe(queryParameters);
    Map<String, String> safeHeaders = safe(headers);

    Map<String, String> mergedQuery = new LinkedHashMap<String, String>();
    mergedQuery.putAll(endpoint.defaultQueryParameters());
    mergedQuery.putAll(safeQueryParameters);
    validateRequiredQuery(endpoint, mergedQuery);

    S3Request.Builder builder =
        S3Request.builder()
            .method(toHttpMethod(endpoint.method()))
            .path(expandPath(endpoint.pathTemplate(), safePathVariables))
            .region(config.region())
            .serviceName(signingService(endpoint));

    for (Map.Entry<String, String> entry : mergedQuery.entrySet()) {
      builder.queryParameter(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : safeHeaders.entrySet()) {
      validateCallerHeader(endpoint, entry.getKey());
      builder.header(entry.getKey(), entry.getValue());
    }
    if (body != null) {
      builder.header("Content-Length", Integer.toString(body.length));
      builder.body(body);
      if (contentType != null && !contentType.trim().isEmpty()) {
        builder.contentType(contentType);
      }
    }
    return builder.build();
  }

  private Mono<S3Request> prepare(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    final S3Request request =
        requestFor(endpoint, pathVariables, queryParameters, headers, body, contentType);
    if (!endpoint.requiresSigV4()) {
      return Mono.just(request);
    }
    return credentialsProvider
        .getCredentials()
        .defaultIfEmpty(ReactiveCredentials.anonymous())
        .map(credentials -> signer.sign(request, config, credentials));
  }

  private static void validateRequiredQuery(
      MinioApiEndpoint endpoint, Map<String, String> queryParameters) {
    for (String required : endpoint.requiredQueryParameters()) {
      if (!queryParameters.containsKey(required)) {
        throw new IllegalArgumentException(
            "Endpoint " + endpoint.name() + " requires query parameter: " + required);
      }
    }
  }

  private static String expandPath(String template, Map<String, String> pathVariables) {
    Matcher matcher = PATH_VARIABLE.matcher(template);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      String name = matcher.group(1);
      String value = pathVariables.get(name);
      if (value == null) {
        throw new IllegalArgumentException(
            "Path template " + template + " requires path variable: " + name);
      }
      validatePathVariable(name, value);
      matcher.appendReplacement(result, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  private static void validateCallerHeader(MinioApiEndpoint endpoint, String name) {
    if (name == null) {
      throw new IllegalArgumentException("header name must not be null");
    }
    String lower = name.trim().toLowerCase(java.util.Locale.US);
    if ("host".equals(lower)
        || ("authorization".equals(lower) && !"bearer".equals(endpoint.authScheme()))
        || "x-amz-date".equals(lower)
        || "x-amz-content-sha256".equals(lower)
        || "x-amz-security-token".equals(lower)) {
      throw new IllegalArgumentException(
          "header is managed by the signer and cannot be caller supplied: " + name);
    }
  }

  private static void validatePathVariable(String name, String value) {
    if (value.indexOf('/') >= 0 && !allowsSlash(name)) {
      throw new IllegalArgumentException(
          "path variable " + name + " must not contain '/' because it maps to one path segment");
    }
    if (!allowsSlash(name) && (".".equals(value) || "..".equals(value))) {
      throw new IllegalArgumentException("path variable " + name + " must not be a dot segment");
    }
  }

  private static boolean allowsSlash(String name) {
    return "object".equals(name) || "prefix".equals(name) || "pathComps".equals(name);
  }

  private static String signingService(MinioApiEndpoint endpoint) {
    return "sts".equals(endpoint.family()) ? "sts" : "s3";
  }

  private static HttpMethod toHttpMethod(String method) {
    try {
      return HttpMethod.valueOf(method);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unsupported HTTP method: " + method, e);
    }
  }

  private static Map<String, String> safe(Map<String, String> values) {
    return values == null ? Collections.<String, String>emptyMap() : values;
  }

  public static final class Builder {
    private String endpoint;
    private String region;
    private ReactiveCredentialsProvider credentialsProvider;
    private WebClient webClient;

    private Builder() {}

    public Builder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    public Builder credentials(String accessKey, String secretKey) {
      this.credentialsProvider = new StaticCredentialsProvider(accessKey, secretKey);
      return this;
    }

    public Builder credentials(String accessKey, String secretKey, String sessionToken) {
      this.credentialsProvider = new StaticCredentialsProvider(accessKey, secretKey, sessionToken);
      return this;
    }

    public Builder credentialsProvider(ReactiveCredentialsProvider credentialsProvider) {
      this.credentialsProvider = credentialsProvider;
      return this;
    }

    public Builder webClient(WebClient webClient) {
      this.webClient = webClient;
      return this;
    }

    public ReactiveMinioRawClient build() {
      ReactiveMinioClientConfig config = ReactiveMinioClientConfig.of(endpoint, region);
      WebClient actualWebClient =
          webClient != null ? webClient : WebClient.builder().baseUrl(config.endpoint()).build();
      ReactiveCredentialsProvider actualProvider =
          credentialsProvider != null ? credentialsProvider : ReactiveCredentialsProvider.anonymous();
      return new ReactiveMinioRawClient(
          config, actualProvider, new ReactiveHttpClient(actualWebClient, config), new S3RequestSigner());
    }
  }
}
