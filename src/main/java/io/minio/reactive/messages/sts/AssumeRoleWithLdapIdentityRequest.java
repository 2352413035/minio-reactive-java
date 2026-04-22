package io.minio.reactive.messages.sts;

/** 使用 LDAP 用户名和密码申请临时凭证的请求对象。 */
public final class AssumeRoleWithLdapIdentityRequest {
  private final String username;
  private final String password;

  private AssumeRoleWithLdapIdentityRequest(String username, String password) {
    if (username == null || username.trim().isEmpty()) {
      throw new IllegalArgumentException("username 不能为空");
    }
    if (password == null || password.trim().isEmpty()) {
      throw new IllegalArgumentException("password 不能为空");
    }
    this.username = username;
    this.password = password;
  }

  public static AssumeRoleWithLdapIdentityRequest of(String username, String password) {
    return new AssumeRoleWithLdapIdentityRequest(username, password);
  }

  public String username() {
    return username;
  }

  public String password() {
    return password;
  }
}
