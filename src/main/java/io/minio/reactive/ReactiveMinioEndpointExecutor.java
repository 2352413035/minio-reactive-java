package io.minio.reactive;

import io.minio.reactive.catalog.MinioApiEndpoint;
import io.minio.reactive.credentials.ReactiveCredentials;
import io.minio.reactive.credentials.ReactiveCredentialsProvider;
import io.minio.reactive.errors.ReactiveMinioProtocolException;
import io.minio.reactive.errors.ReactiveS3Exception;
import io.minio.reactive.http.ReactiveHttpClient;
import io.minio.reactive.http.S3Request;
import io.minio.reactive.signer.S3RequestSigner;
import io.minio.reactive.util.JsonSupport;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Catalog 接口的统一执行器。
 *
 * <p>这个类是 raw client 和各个专用客户端共同复用的底层能力。它只负责协议层动作：
 * 展开路径模板、合并 query、过滤危险 header、选择签名服务名、按认证方案签名，并调用 HTTP 层。
 * 专用客户端不依赖 raw client，而是和 raw client 一样依赖这个执行器。
 */
final class ReactiveMinioEndpointExecutor {
  private static final Pattern PATH_VARIABLE = Pattern.compile("\\{([^}/:]+)(?::[^}]*)?}");

  private final ReactiveMinioClientConfig config;
  private final ReactiveCredentialsProvider credentialsProvider;
  private final ReactiveHttpClient httpClient;
  private final S3RequestSigner signer;

  ReactiveMinioEndpointExecutor(
      ReactiveMinioClientConfig config,
      ReactiveCredentialsProvider credentialsProvider,
      ReactiveHttpClient httpClient,
      S3RequestSigner signer) {
    this.config = config;
    this.credentialsProvider = credentialsProvider;
    this.httpClient = httpClient;
    this.signer = signer;
  }

  Mono<Integer> executeToStatusAllowAll(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return prepare(endpoint, pathVariables, queryParameters, headers, body, contentType)
        .flatMap(httpClient::exchangeToStatusAllowAll);
  }

  Mono<Integer> executeToStatus(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return withProtocolErrors(
        endpoint,
        prepare(endpoint, pathVariables, queryParameters, headers, body, contentType)
            .flatMap(httpClient::exchangeToStatus));
  }

  Mono<String> executeToString(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return withProtocolErrors(
        endpoint,
        prepare(endpoint, pathVariables, queryParameters, headers, body, contentType)
            .flatMap(httpClient::exchangeToString));
  }

  Mono<byte[]> executeToBytes(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return withProtocolErrors(
        endpoint,
        prepare(endpoint, pathVariables, queryParameters, headers, body, contentType)
            .flatMap(httpClient::exchangeToByteArray));
  }

  Flux<byte[]> executeToBody(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return withProtocolErrors(
        endpoint,
        prepare(endpoint, pathVariables, queryParameters, headers, body, contentType)
            .flatMapMany(httpClient::exchangeToBody));
  }

  Mono<Map<String, List<String>>> executeToHeaders(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return withProtocolErrors(
        endpoint,
        prepare(endpoint, pathVariables, queryParameters, headers, body, contentType)
            .flatMap(httpClient::exchangeToHeaders));
  }

  Mono<Void> executeToVoid(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return withProtocolErrors(
        endpoint,
        prepare(endpoint, pathVariables, queryParameters, headers, body, contentType)
            .flatMap(httpClient::exchangeToVoid));
  }

  S3Request requestFor(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    Map<String, String> safePathVariables = safe(pathVariables);
    Map<String, String> safeQueryParameters = safe(queryParameters);
    Map<String, String> safeHeaders = safe(headers);

    // catalog 中的固定 query 代表协议要求，调用方 query 用于补齐必填项或覆盖可变项。
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


  /** 非 S3 协议族使用更贴近领域的异常，避免把 Admin/KMS/STS 错误都表现成 S3 XML 错误。 */
  private static <T> Mono<T> withProtocolErrors(MinioApiEndpoint endpoint, Mono<T> source) {
    return source.onErrorMap(ReactiveS3Exception.class, ex -> mapProtocolError(endpoint, ex));
  }

  /** 非 S3 协议族使用更贴近领域的异常，避免把 Admin/KMS/STS 错误都表现成 S3 XML 错误。 */
  private static <T> Flux<T> withProtocolErrors(MinioApiEndpoint endpoint, Flux<T> source) {
    return source.onErrorMap(ReactiveS3Exception.class, ex -> mapProtocolError(endpoint, ex));
  }

  private static RuntimeException mapProtocolError(MinioApiEndpoint endpoint, ReactiveS3Exception ex) {
    if ("s3".equals(endpoint.family())) {
      return ex;
    }
    String code = ex.errorCode();
    String message = ex.errorMessage();
    String requestId = ex.requestId();
    if ((requestId == null || requestId.isEmpty()) && ex.s3Error() != null) {
      requestId = ex.s3Error().requestId();
    }
    if ((code == null || code.isEmpty())
        && ex.responseBody() != null
        && !ex.responseBody().trim().isEmpty()) {
      Map<String, Object> values = safeJson(ex.responseBody());
      code = firstText(values, "code", "Code", "error", "Error");
      message = firstText(values, "message", "Message", "errorMessage", "ErrorMessage");
      requestId = firstText(values, "requestId", "requestID", "RequestId", "RequestID");
    }
    return new ReactiveMinioProtocolException(
        endpoint.family(), ex.statusCode(), code, message, requestId, ex.responseBody());
  }

  private static Map<String, Object> safeJson(String body) {
    try {
      return JsonSupport.parseMap(body);
    } catch (RuntimeException ignored) {
      return Collections.emptyMap();
    }
  }

  private static String firstText(Map<String, Object> values, String... names) {
    for (String name : names) {
      Object value = values.get(name);
      if (value != null) {
        String text = String.valueOf(value);
        if (!text.trim().isEmpty()) {
          return text;
        }
      }
    }
    return "";
  }

  /** 按认证方案决定是否签名，避免把 bearer 或无认证接口错误地做 SigV4 签名。 */
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

  /** 必填 query 缺失时在本地失败，避免把不完整请求发到 MinIO。 */
  private static void validateRequiredQuery(
      MinioApiEndpoint endpoint, Map<String, String> queryParameters) {
    for (String required : endpoint.requiredQueryParameters()) {
      if (!queryParameters.containsKey(required)) {
        throw new IllegalArgumentException(
            "Endpoint " + endpoint.name() + " requires query parameter: " + required);
      }
    }
  }

  /** 展开路径模板；展开结果后续仍会经过 S3Request 的规范化路径编码。 */
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

  /** 防止调用方覆盖签名器管理的关键头，bearer 接口只例外放行 Authorization。 */
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

  /** 单段路径变量不能扩展成多段，也不能使用可能引起路径归一化歧义的点段。 */
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

  /** STS 签名必须使用 sts 服务范围，其它公开 MinIO 路由沿用 s3 服务范围。 */
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
}
