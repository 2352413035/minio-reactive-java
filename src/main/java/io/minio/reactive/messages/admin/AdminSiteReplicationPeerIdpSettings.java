package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 站点复制 peer 的 IDP 设置摘要。
 *
 * <p>MinIO 的 peer IDP 设置用于确认各站点身份源是否一致，响应里可能出现 OIDC
 * provider 的客户端标识或哈希密钥字段。为避免把这类敏感或半敏感字段带进业务日志，本模型只保留
 * LDAP/OpenID 是否启用、LDAP 搜索条件、OpenID 区域、角色数量和角色名称，不保存 raw JSON。
 */
public final class AdminSiteReplicationPeerIdpSettings {
  private final boolean ldapEnabled;
  private final String ldapUserDnSearchBase;
  private final String ldapUserDnSearchFilter;
  private final String ldapGroupSearchBase;
  private final String ldapGroupSearchFilter;
  private final boolean openidEnabled;
  private final String openidRegion;
  private final List<String> openidRoleNames;
  private final boolean claimProviderConfigured;

  private AdminSiteReplicationPeerIdpSettings(
      boolean ldapEnabled,
      String ldapUserDnSearchBase,
      String ldapUserDnSearchFilter,
      String ldapGroupSearchBase,
      String ldapGroupSearchFilter,
      boolean openidEnabled,
      String openidRegion,
      List<String> openidRoleNames,
      boolean claimProviderConfigured) {
    this.ldapEnabled = ldapEnabled;
    this.ldapUserDnSearchBase = ldapUserDnSearchBase == null ? "" : ldapUserDnSearchBase;
    this.ldapUserDnSearchFilter = ldapUserDnSearchFilter == null ? "" : ldapUserDnSearchFilter;
    this.ldapGroupSearchBase = ldapGroupSearchBase == null ? "" : ldapGroupSearchBase;
    this.ldapGroupSearchFilter = ldapGroupSearchFilter == null ? "" : ldapGroupSearchFilter;
    this.openidEnabled = openidEnabled;
    this.openidRegion = openidRegion == null ? "" : openidRegion;
    this.openidRoleNames = Collections.unmodifiableList(new ArrayList<String>(openidRoleNames));
    this.claimProviderConfigured = claimProviderConfigured;
  }

  public static AdminSiteReplicationPeerIdpSettings parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    JsonNode ldap = JsonSupport.child(root, "LDAP", "ldap");
    if (ldap == null || ldap.isNull()) {
      // 兼容旧版 MinIO：旧响应直接返回 LDAPSettings，而不是外层 IDPSettings。
      ldap = root;
    }
    JsonNode openid = JsonSupport.child(root, "OpenID", "openID", "openid");
    List<String> roleNames = collectRoleNames(JsonSupport.child(openid, "Roles", "roles"));
    return new AdminSiteReplicationPeerIdpSettings(
        JsonSupport.booleanAny(ldap, "IsLDAPEnabled", "isLDAPEnabled", "ldapEnabled", "enabled"),
        JsonSupport.textAny(ldap, "LDAPUserDNSearchBase", "ldapUserDNSearchBase", "userDNSearchBase"),
        JsonSupport.textAny(
            ldap, "LDAPUserDNSearchFilter", "ldapUserDNSearchFilter", "userDNSearchFilter"),
        JsonSupport.textAny(ldap, "LDAPGroupSearchBase", "ldapGroupSearchBase", "groupSearchBase"),
        JsonSupport.textAny(
            ldap, "LDAPGroupSearchFilter", "ldapGroupSearchFilter", "groupSearchFilter"),
        JsonSupport.booleanAny(openid, "Enabled", "enabled"),
        JsonSupport.textAny(openid, "Region", "region"),
        roleNames,
        hasConfiguredClaimProvider(JsonSupport.child(openid, "ClaimProvider", "claimProvider")));
  }

  public boolean ldapEnabled() {
    return ldapEnabled;
  }

  public String ldapUserDnSearchBase() {
    return ldapUserDnSearchBase;
  }

  public String ldapUserDnSearchFilter() {
    return ldapUserDnSearchFilter;
  }

  public String ldapGroupSearchBase() {
    return ldapGroupSearchBase;
  }

  public String ldapGroupSearchFilter() {
    return ldapGroupSearchFilter;
  }

  public boolean openidEnabled() {
    return openidEnabled;
  }

  public String openidRegion() {
    return openidRegion;
  }

  public List<String> openidRoleNames() {
    return openidRoleNames;
  }

  public int openidRoleCount() {
    return openidRoleNames.size();
  }

  public boolean claimProviderConfigured() {
    return claimProviderConfigured;
  }

  public boolean identityProviderConfigured() {
    return ldapEnabled || openidEnabled || claimProviderConfigured || !openidRoleNames.isEmpty();
  }

  private static List<String> collectRoleNames(JsonNode roles) {
    List<String> roleNames = new ArrayList<String>();
    if (roles == null || roles.isNull()) {
      return roleNames;
    }
    if (roles.isObject()) {
      Iterator<String> names = roles.fieldNames();
      while (names.hasNext()) {
        roleNames.add(names.next());
      }
      return roleNames;
    }
    if (roles.isArray()) {
      for (JsonNode role : roles) {
        String name = JsonSupport.textAny(role, "name", "Name", "role", "Role", "arn", "Arn");
        if (!name.isEmpty()) {
          roleNames.add(name);
        }
      }
    }
    return roleNames;
  }

  private static boolean hasConfiguredClaimProvider(JsonNode claimProvider) {
    if (claimProvider == null || claimProvider.isNull()) {
      return false;
    }
    if (claimProvider.isObject()) {
      Iterator<JsonNode> values = claimProvider.elements();
      while (values.hasNext()) {
        JsonNode value = values.next();
        if (value == null || value.isNull()) {
          continue;
        }
        if (value.isBoolean() && value.asBoolean()) {
          return true;
        }
        if (value.isNumber() && value.asLong() != 0L) {
          return true;
        }
        if (value.isTextual() && !value.asText("").trim().isEmpty()) {
          return true;
        }
      }
      return false;
    }
    return !claimProvider.asText("").trim().isEmpty();
  }
}
