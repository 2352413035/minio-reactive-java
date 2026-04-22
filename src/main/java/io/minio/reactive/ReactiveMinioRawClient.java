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
 * MinIO 公开 HTTP 接口目录的原始响应式执行器。
 *
 * <p>这个类刻意保持在“协议执行层”：它负责把目录条目、路径变量、query、header、body
 * 合成一个可签名的请求，并复用统一 HTTP 层发送。这样即使某个管理端接口还没有强类型
 * 请求/响应模型，SDK 也不会缺失它的调用入口。
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

  /** 执行不带额外参数的接口，并只返回 HTTP 状态码。 */
  public Mono<Integer> executeToStatus(MinioApiEndpoint endpoint) {
    return executeToStatus(
        endpoint,
        Collections.<String, String>emptyMap(),
        Collections.<String, String>emptyMap(),
        Collections.<String, String>emptyMap(),
        null,
        null);
  }

  /** 执行接口并只关心状态码，适合 HEAD、健康检查或只需确认成功的操作。 */
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

  /** 执行接口并把响应体整体读取为字符串，适合 XML、JSON、文本类管理接口。 */
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

  /** 执行接口并把响应体整体读取为字节数组，适合导出、下载等小到中等体积响应。 */
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

  /** 执行接口并以分块字节流返回响应体，适合下载、日志、trace 等流式场景。 */
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

  /** 执行接口并返回响应头，适合 stat、HEAD 或只关心元数据的操作。 */
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

  /** 执行接口并释放响应体，适合删除、设置配置等无返回体操作。 */
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

  /**
   * 根据目录条目构造内部请求模型。
   *
   * <p>这一步只做请求语义合成，不直接发送网络请求。把它拆出来是为了让测试能直接验证
   * 路径模板展开、query 合并、header 过滤、body 长度和签名 service 的选择是否正确。
   */
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

    // 目录里的固定 query 先进入，再用调用方传入的动态 query 覆盖或补齐。
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

  /** 按认证方案决定是否执行 SigV4 签名；Bearer 和无认证接口不能被错误地 S3 签名。 */
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

  /** 必填 query 缺失时尽早失败，避免把错误请求发到 MinIO 后才排查。 */
  private static void validateRequiredQuery(
      MinioApiEndpoint endpoint, Map<String, String> queryParameters) {
    for (String required : endpoint.requiredQueryParameters()) {
      if (!queryParameters.containsKey(required)) {
        throw new IllegalArgumentException(
            "Endpoint " + endpoint.name() + " requires query parameter: " + required);
      }
    }
  }

  /** 展开路径模板；展开后的路径仍会由 S3Escaper 做 canonical 编码。 */
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

  /** 防止调用方覆盖签名器管理的关键头，Bearer 认证接口除 Authorization 外仍受保护。 */
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

  /** 限制单段路径变量，避免把一个模板段扩展成多个路径段或点段。 */
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

  /** STS 签名必须使用 sts 服务范围，其它 MinIO/S3 路由沿用 s3 服务范围。 */
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
