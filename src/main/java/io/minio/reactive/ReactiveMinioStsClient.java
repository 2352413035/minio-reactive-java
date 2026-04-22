package io.minio.reactive;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * 临时凭证 STS 专用客户端。
 *
 * <p>这个客户端只提供STS相关目录接口的命名入口；响应体先以原始文本返回。
 * 如果某个接口需要二进制、流式或只读响应头，可以通过 `rawClient()` 使用底层兜底调用器。
 */
public final class ReactiveMinioStsClient extends ReactiveMinioCatalogClientSupport {
  ReactiveMinioStsClient(ReactiveMinioRawClient rawClient) {
    super(rawClient);
  }

  /** 调用目录接口 `STS_ASSUME_ROLE_FORM`，返回原始文本响应。 */
  public Mono<String> assumeRoleForm(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("STS_ASSUME_ROLE_FORM", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `STS_ASSUME_ROLE_SSO_FORM`，返回原始文本响应。 */
  public Mono<String> assumeRoleSsoForm(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("STS_ASSUME_ROLE_SSO_FORM", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `STS_ASSUME_ROLE_WITH_CLIENT_GRANTS`，返回原始文本响应。 */
  public Mono<String> assumeRoleWithClientGrants(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("STS_ASSUME_ROLE_WITH_CLIENT_GRANTS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `STS_ASSUME_ROLE_WITH_WEB_IDENTITY`，返回原始文本响应。 */
  public Mono<String> assumeRoleWithWebIdentity(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("STS_ASSUME_ROLE_WITH_WEB_IDENTITY", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `STS_ASSUME_ROLE_WITH_LDAP_IDENTITY`，返回原始文本响应。 */
  public Mono<String> assumeRoleWithLdapIdentity(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("STS_ASSUME_ROLE_WITH_LDAP_IDENTITY", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `STS_ASSUME_ROLE_WITH_CERTIFICATE`，返回原始文本响应。 */
  public Mono<String> assumeRoleWithCertificate(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("STS_ASSUME_ROLE_WITH_CERTIFICATE", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `STS_ASSUME_ROLE_WITH_CUSTOM_TOKEN`，返回原始文本响应。 */
  public Mono<String> assumeRoleWithCustomToken(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("STS_ASSUME_ROLE_WITH_CUSTOM_TOKEN", pathVariables, queryParameters, headers, body, contentType);
  }

}
