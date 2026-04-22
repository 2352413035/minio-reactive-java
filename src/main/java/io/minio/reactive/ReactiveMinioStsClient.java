package io.minio.reactive;

import reactor.core.publisher.Mono;

/**
 * 临时凭证 STS 专用客户端。
 *
 * <p>这个客户端按STS接口的业务名称提供方法，调用者不需要直接查目录或拼 Map。
 * 如果遇到尚未补充业务模型的特殊场景，可以回退使用 `ReactiveMinioRawClient`。
 */
public final class ReactiveMinioStsClient extends ReactiveMinioCatalogClientSupport {
  ReactiveMinioStsClient(ReactiveMinioEndpointExecutor executor) {
    super(executor);
  }

  /** 调用 `STS_ASSUME_ROLE_FORM`。 */
  public Mono<String> assumeRoleForm(byte[] body, String contentType) {
    return executeToString("STS_ASSUME_ROLE_FORM", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `STS_ASSUME_ROLE_FORM`，不携带请求体。 */
  public Mono<String> assumeRoleForm() {
    return assumeRoleForm(null, null);
  }

  /** 调用 `STS_ASSUME_ROLE_SSO_FORM`。 */
  public Mono<String> assumeRoleSsoForm(byte[] body, String contentType) {
    return executeToString("STS_ASSUME_ROLE_SSO_FORM", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `STS_ASSUME_ROLE_SSO_FORM`，不携带请求体。 */
  public Mono<String> assumeRoleSsoForm() {
    return assumeRoleSsoForm(null, null);
  }

  /** 调用 `STS_ASSUME_ROLE_WITH_CLIENT_GRANTS`。 */
  public Mono<String> assumeRoleWithClientGrants(String token, byte[] body, String contentType) {
    return executeToString("STS_ASSUME_ROLE_WITH_CLIENT_GRANTS", emptyMap(), map("Token", token), emptyMap(), body, contentType);
  }

  /** 调用 `STS_ASSUME_ROLE_WITH_CLIENT_GRANTS`，不携带请求体。 */
  public Mono<String> assumeRoleWithClientGrants(String token) {
    return assumeRoleWithClientGrants(token, null, null);
  }

  /** 调用 `STS_ASSUME_ROLE_WITH_WEB_IDENTITY`。 */
  public Mono<String> assumeRoleWithWebIdentity(String webIdentityToken, byte[] body, String contentType) {
    return executeToString("STS_ASSUME_ROLE_WITH_WEB_IDENTITY", emptyMap(), map("WebIdentityToken", webIdentityToken), emptyMap(), body, contentType);
  }

  /** 调用 `STS_ASSUME_ROLE_WITH_WEB_IDENTITY`，不携带请求体。 */
  public Mono<String> assumeRoleWithWebIdentity(String webIdentityToken) {
    return assumeRoleWithWebIdentity(webIdentityToken, null, null);
  }

  /** 调用 `STS_ASSUME_ROLE_WITH_LDAP_IDENTITY`。 */
  public Mono<String> assumeRoleWithLdapIdentity(String lDAPUsername, String lDAPPassword, byte[] body, String contentType) {
    return executeToString("STS_ASSUME_ROLE_WITH_LDAP_IDENTITY", emptyMap(), map("LDAPUsername", lDAPUsername, "LDAPPassword", lDAPPassword), emptyMap(), body, contentType);
  }

  /** 调用 `STS_ASSUME_ROLE_WITH_LDAP_IDENTITY`，不携带请求体。 */
  public Mono<String> assumeRoleWithLdapIdentity(String lDAPUsername, String lDAPPassword) {
    return assumeRoleWithLdapIdentity(lDAPUsername, lDAPPassword, null, null);
  }

  /** 调用 `STS_ASSUME_ROLE_WITH_CERTIFICATE`。 */
  public Mono<String> assumeRoleWithCertificate(byte[] body, String contentType) {
    return executeToString("STS_ASSUME_ROLE_WITH_CERTIFICATE", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `STS_ASSUME_ROLE_WITH_CERTIFICATE`，不携带请求体。 */
  public Mono<String> assumeRoleWithCertificate() {
    return assumeRoleWithCertificate(null, null);
  }

  /** 调用 `STS_ASSUME_ROLE_WITH_CUSTOM_TOKEN`。 */
  public Mono<String> assumeRoleWithCustomToken(byte[] body, String contentType) {
    return executeToString("STS_ASSUME_ROLE_WITH_CUSTOM_TOKEN", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `STS_ASSUME_ROLE_WITH_CUSTOM_TOKEN`，不携带请求体。 */
  public Mono<String> assumeRoleWithCustomToken() {
    return assumeRoleWithCustomToken(null, null);
  }

}
