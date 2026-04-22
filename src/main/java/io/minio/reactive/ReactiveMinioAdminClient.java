package io.minio.reactive;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * 管理端专用客户端。
 *
 * <p>这个客户端只提供管理端相关目录接口的命名入口；响应体先以原始文本返回。
 * 如果某个接口需要二进制、流式或只读响应头，可以通过 `rawClient()` 使用底层兜底调用器。
 */
public final class ReactiveMinioAdminClient extends ReactiveMinioCatalogClientSupport {
  ReactiveMinioAdminClient(ReactiveMinioRawClient rawClient) {
    super(rawClient);
  }

  /** 调用目录接口 `ADMIN_SERVICE_V2`，返回原始文本响应。 */
  public Mono<String> serviceV2(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SERVICE_V2", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SERVICE`，返回原始文本响应。 */
  public Mono<String> service(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SERVICE", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SERVER_UPDATE_V2`，返回原始文本响应。 */
  public Mono<String> serverUpdateV2(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SERVER_UPDATE_V2", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SERVER_UPDATE`，返回原始文本响应。 */
  public Mono<String> serverUpdate(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SERVER_UPDATE", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SERVER_INFO`，返回原始文本响应。 */
  public Mono<String> serverInfo(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SERVER_INFO", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_INSPECT_DATA_GET`，返回原始文本响应。 */
  public Mono<String> inspectDataGet(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_INSPECT_DATA_GET", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_INSPECT_DATA_POST`，返回原始文本响应。 */
  public Mono<String> inspectDataPost(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_INSPECT_DATA_POST", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_STORAGE_INFO`，返回原始文本响应。 */
  public Mono<String> storageInfo(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_STORAGE_INFO", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_DATA_USAGE_INFO`，返回原始文本响应。 */
  public Mono<String> dataUsageInfo(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_DATA_USAGE_INFO", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_METRICS`，返回原始文本响应。 */
  public Mono<String> metrics(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_METRICS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_HEAL_ROOT`，返回原始文本响应。 */
  public Mono<String> healRoot(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_HEAL_ROOT", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_HEAL_BUCKET`，返回原始文本响应。 */
  public Mono<String> healBucket(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_HEAL_BUCKET", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_HEAL_PREFIX`，返回原始文本响应。 */
  public Mono<String> healPrefix(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_HEAL_PREFIX", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_BACKGROUND_HEAL_STATUS`，返回原始文本响应。 */
  public Mono<String> backgroundHealStatus(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_BACKGROUND_HEAL_STATUS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LIST_POOLS`，返回原始文本响应。 */
  public Mono<String> listPools(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LIST_POOLS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_POOL_STATUS`，返回原始文本响应。 */
  public Mono<String> poolStatus(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_POOL_STATUS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_START_DECOMMISSION`，返回原始文本响应。 */
  public Mono<String> startDecommission(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_START_DECOMMISSION", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_CANCEL_DECOMMISSION`，返回原始文本响应。 */
  public Mono<String> cancelDecommission(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_CANCEL_DECOMMISSION", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_REBALANCE_START`，返回原始文本响应。 */
  public Mono<String> rebalanceStart(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_REBALANCE_START", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_REBALANCE_STATUS`，返回原始文本响应。 */
  public Mono<String> rebalanceStatus(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_REBALANCE_STATUS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_REBALANCE_STOP`，返回原始文本响应。 */
  public Mono<String> rebalanceStop(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_REBALANCE_STOP", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_PROFILING_START`，返回原始文本响应。 */
  public Mono<String> profilingStart(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_PROFILING_START", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_PROFILING_DOWNLOAD`，返回原始文本响应。 */
  public Mono<String> profilingDownload(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_PROFILING_DOWNLOAD", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_PROFILE`，返回原始文本响应。 */
  public Mono<String> profile(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_PROFILE", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_GET_CONFIG_KV`，返回原始文本响应。 */
  public Mono<String> getConfigKv(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_GET_CONFIG_KV", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SET_CONFIG_KV`，返回原始文本响应。 */
  public Mono<String> setConfigKv(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SET_CONFIG_KV", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_DELETE_CONFIG_KV`，返回原始文本响应。 */
  public Mono<String> deleteConfigKv(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_DELETE_CONFIG_KV", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_HELP_CONFIG_KV`，返回原始文本响应。 */
  public Mono<String> helpConfigKv(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_HELP_CONFIG_KV", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LIST_CONFIG_HISTORY_KV`，返回原始文本响应。 */
  public Mono<String> listConfigHistoryKv(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LIST_CONFIG_HISTORY_KV", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_CLEAR_CONFIG_HISTORY_KV`，返回原始文本响应。 */
  public Mono<String> clearConfigHistoryKv(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_CLEAR_CONFIG_HISTORY_KV", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_RESTORE_CONFIG_HISTORY_KV`，返回原始文本响应。 */
  public Mono<String> restoreConfigHistoryKv(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_RESTORE_CONFIG_HISTORY_KV", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_GET_CONFIG`，返回原始文本响应。 */
  public Mono<String> getConfig(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_GET_CONFIG", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SET_CONFIG`，返回原始文本响应。 */
  public Mono<String> setConfig(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SET_CONFIG", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_ADD_CANNED_POLICY`，返回原始文本响应。 */
  public Mono<String> addCannedPolicy(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_ADD_CANNED_POLICY", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_ACCOUNT_INFO`，返回原始文本响应。 */
  public Mono<String> accountInfo(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_ACCOUNT_INFO", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_ADD_USER`，返回原始文本响应。 */
  public Mono<String> addUser(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_ADD_USER", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SET_USER_STATUS`，返回原始文本响应。 */
  public Mono<String> setUserStatus(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SET_USER_STATUS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_ADD_SERVICE_ACCOUNT`，返回原始文本响应。 */
  public Mono<String> addServiceAccount(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_ADD_SERVICE_ACCOUNT", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_UPDATE_SERVICE_ACCOUNT`，返回原始文本响应。 */
  public Mono<String> updateServiceAccount(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_UPDATE_SERVICE_ACCOUNT", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_INFO_SERVICE_ACCOUNT`，返回原始文本响应。 */
  public Mono<String> infoServiceAccount(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_INFO_SERVICE_ACCOUNT", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LIST_SERVICE_ACCOUNTS`，返回原始文本响应。 */
  public Mono<String> listServiceAccounts(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LIST_SERVICE_ACCOUNTS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_DELETE_SERVICE_ACCOUNT`，返回原始文本响应。 */
  public Mono<String> deleteServiceAccount(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_DELETE_SERVICE_ACCOUNT", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_TEMPORARY_ACCOUNT_INFO`，返回原始文本响应。 */
  public Mono<String> temporaryAccountInfo(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_TEMPORARY_ACCOUNT_INFO", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LIST_ACCESS_KEYS_BULK`，返回原始文本响应。 */
  public Mono<String> listAccessKeysBulk(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LIST_ACCESS_KEYS_BULK", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_INFO_ACCESS_KEY`，返回原始文本响应。 */
  public Mono<String> infoAccessKey(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_INFO_ACCESS_KEY", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_INFO_CANNED_POLICY`，返回原始文本响应。 */
  public Mono<String> infoCannedPolicy(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_INFO_CANNED_POLICY", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LIST_BUCKET_POLICIES`，返回原始文本响应。 */
  public Mono<String> listBucketPolicies(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LIST_BUCKET_POLICIES", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LIST_CANNED_POLICIES`，返回原始文本响应。 */
  public Mono<String> listCannedPolicies(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LIST_CANNED_POLICIES", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LIST_BUILTIN_POLICY_ENTITIES`，返回原始文本响应。 */
  public Mono<String> listBuiltinPolicyEntities(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LIST_BUILTIN_POLICY_ENTITIES", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_REMOVE_CANNED_POLICY`，返回原始文本响应。 */
  public Mono<String> removeCannedPolicy(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_REMOVE_CANNED_POLICY", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SET_USER_OR_GROUP_POLICY`，返回原始文本响应。 */
  public Mono<String> setUserOrGroupPolicy(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SET_USER_OR_GROUP_POLICY", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_ATTACH_DETACH_BUILTIN_POLICY`，返回原始文本响应。 */
  public Mono<String> attachDetachBuiltinPolicy(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_ATTACH_DETACH_BUILTIN_POLICY", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_REMOVE_USER`，返回原始文本响应。 */
  public Mono<String> removeUser(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_REMOVE_USER", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LIST_BUCKET_USERS`，返回原始文本响应。 */
  public Mono<String> listBucketUsers(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LIST_BUCKET_USERS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LIST_USERS`，返回原始文本响应。 */
  public Mono<String> listUsers(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LIST_USERS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_USER_INFO`，返回原始文本响应。 */
  public Mono<String> userInfo(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_USER_INFO", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_UPDATE_GROUP_MEMBERS`，返回原始文本响应。 */
  public Mono<String> updateGroupMembers(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_UPDATE_GROUP_MEMBERS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_GET_GROUP`，返回原始文本响应。 */
  public Mono<String> getGroup(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_GET_GROUP", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LIST_GROUPS`，返回原始文本响应。 */
  public Mono<String> listGroups(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LIST_GROUPS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SET_GROUP_STATUS`，返回原始文本响应。 */
  public Mono<String> setGroupStatus(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SET_GROUP_STATUS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_EXPORT_IAM`，返回原始文本响应。 */
  public Mono<String> exportIam(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_EXPORT_IAM", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_IMPORT_IAM`，返回原始文本响应。 */
  public Mono<String> importIam(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_IMPORT_IAM", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_IMPORT_IAM_V2`，返回原始文本响应。 */
  public Mono<String> importIamV2(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_IMPORT_IAM_V2", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_ADD_IDP_CONFIG`，返回原始文本响应。 */
  public Mono<String> addIdpConfig(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_ADD_IDP_CONFIG", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_UPDATE_IDP_CONFIG`，返回原始文本响应。 */
  public Mono<String> updateIdpConfig(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_UPDATE_IDP_CONFIG", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LIST_IDP_CONFIG`，返回原始文本响应。 */
  public Mono<String> listIdpConfig(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LIST_IDP_CONFIG", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_GET_IDP_CONFIG`，返回原始文本响应。 */
  public Mono<String> getIdpConfig(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_GET_IDP_CONFIG", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_DELETE_IDP_CONFIG`，返回原始文本响应。 */
  public Mono<String> deleteIdpConfig(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_DELETE_IDP_CONFIG", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LDAP_ADD_SERVICE_ACCOUNT`，返回原始文本响应。 */
  public Mono<String> ldapAddServiceAccount(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LDAP_ADD_SERVICE_ACCOUNT", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LDAP_LIST_ACCESS_KEYS`，返回原始文本响应。 */
  public Mono<String> ldapListAccessKeys(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LDAP_LIST_ACCESS_KEYS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LDAP_LIST_ACCESS_KEYS_BULK`，返回原始文本响应。 */
  public Mono<String> ldapListAccessKeysBulk(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LDAP_LIST_ACCESS_KEYS_BULK", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LDAP_POLICY_ENTITIES`，返回原始文本响应。 */
  public Mono<String> ldapPolicyEntities(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LDAP_POLICY_ENTITIES", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LDAP_ATTACH_DETACH_POLICY`，返回原始文本响应。 */
  public Mono<String> ldapAttachDetachPolicy(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LDAP_ATTACH_DETACH_POLICY", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_OPENID_LIST_ACCESS_KEYS_BULK`，返回原始文本响应。 */
  public Mono<String> openidListAccessKeysBulk(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_OPENID_LIST_ACCESS_KEYS_BULK", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_GET_BUCKET_QUOTA`，返回原始文本响应。 */
  public Mono<String> getBucketQuota(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_GET_BUCKET_QUOTA", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SET_BUCKET_QUOTA`，返回原始文本响应。 */
  public Mono<String> setBucketQuota(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SET_BUCKET_QUOTA", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LIST_REMOTE_TARGETS`，返回原始文本响应。 */
  public Mono<String> listRemoteTargets(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LIST_REMOTE_TARGETS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SET_REMOTE_TARGET`，返回原始文本响应。 */
  public Mono<String> setRemoteTarget(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SET_REMOTE_TARGET", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_REMOVE_REMOTE_TARGET`，返回原始文本响应。 */
  public Mono<String> removeRemoteTarget(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_REMOVE_REMOTE_TARGET", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_REPLICATION_DIFF`，返回原始文本响应。 */
  public Mono<String> replicationDiff(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_REPLICATION_DIFF", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_REPLICATION_MRF`，返回原始文本响应。 */
  public Mono<String> replicationMrf(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_REPLICATION_MRF", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_START_BATCH_JOB`，返回原始文本响应。 */
  public Mono<String> startBatchJob(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_START_BATCH_JOB", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LIST_BATCH_JOBS`，返回原始文本响应。 */
  public Mono<String> listBatchJobs(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LIST_BATCH_JOBS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_BATCH_JOB_STATUS`，返回原始文本响应。 */
  public Mono<String> batchJobStatus(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_BATCH_JOB_STATUS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_DESCRIBE_BATCH_JOB`，返回原始文本响应。 */
  public Mono<String> describeBatchJob(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_DESCRIBE_BATCH_JOB", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_CANCEL_BATCH_JOB`，返回原始文本响应。 */
  public Mono<String> cancelBatchJob(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_CANCEL_BATCH_JOB", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_EXPORT_BUCKET_METADATA`，返回原始文本响应。 */
  public Mono<String> exportBucketMetadata(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_EXPORT_BUCKET_METADATA", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_IMPORT_BUCKET_METADATA`，返回原始文本响应。 */
  public Mono<String> importBucketMetadata(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_IMPORT_BUCKET_METADATA", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_ADD_TIER`，返回原始文本响应。 */
  public Mono<String> addTier(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_ADD_TIER", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_EDIT_TIER`，返回原始文本响应。 */
  public Mono<String> editTier(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_EDIT_TIER", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LIST_TIER`，返回原始文本响应。 */
  public Mono<String> listTier(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LIST_TIER", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_REMOVE_TIER`，返回原始文本响应。 */
  public Mono<String> removeTier(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_REMOVE_TIER", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_VERIFY_TIER`，返回原始文本响应。 */
  public Mono<String> verifyTier(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_VERIFY_TIER", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_TIER_STATS`，返回原始文本响应。 */
  public Mono<String> tierStats(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_TIER_STATS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SITE_REPLICATION_ADD`，返回原始文本响应。 */
  public Mono<String> siteReplicationAdd(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SITE_REPLICATION_ADD", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SITE_REPLICATION_REMOVE`，返回原始文本响应。 */
  public Mono<String> siteReplicationRemove(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SITE_REPLICATION_REMOVE", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SITE_REPLICATION_INFO`，返回原始文本响应。 */
  public Mono<String> siteReplicationInfo(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SITE_REPLICATION_INFO", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SITE_REPLICATION_METAINFO`，返回原始文本响应。 */
  public Mono<String> siteReplicationMetainfo(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SITE_REPLICATION_METAINFO", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SITE_REPLICATION_STATUS`，返回原始文本响应。 */
  public Mono<String> siteReplicationStatus(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SITE_REPLICATION_STATUS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SITE_REPLICATION_DEVNULL`，返回原始文本响应。 */
  public Mono<String> siteReplicationDevnull(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SITE_REPLICATION_DEVNULL", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SITE_REPLICATION_NETPERF`，返回原始文本响应。 */
  public Mono<String> siteReplicationNetperf(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SITE_REPLICATION_NETPERF", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SR_PEER_JOIN`，返回原始文本响应。 */
  public Mono<String> srPeerJoin(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SR_PEER_JOIN", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SR_PEER_BUCKET_OPS`，返回原始文本响应。 */
  public Mono<String> srPeerBucketOps(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SR_PEER_BUCKET_OPS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SR_PEER_IAM_ITEM`，返回原始文本响应。 */
  public Mono<String> srPeerIamItem(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SR_PEER_IAM_ITEM", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SR_PEER_BUCKET_META`，返回原始文本响应。 */
  public Mono<String> srPeerBucketMeta(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SR_PEER_BUCKET_META", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SR_PEER_IDP_SETTINGS`，返回原始文本响应。 */
  public Mono<String> srPeerIdpSettings(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SR_PEER_IDP_SETTINGS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SITE_REPLICATION_EDIT`，返回原始文本响应。 */
  public Mono<String> siteReplicationEdit(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SITE_REPLICATION_EDIT", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SR_PEER_EDIT`，返回原始文本响应。 */
  public Mono<String> srPeerEdit(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SR_PEER_EDIT", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SR_PEER_REMOVE`，返回原始文本响应。 */
  public Mono<String> srPeerRemove(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SR_PEER_REMOVE", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SITE_REPLICATION_RESYNC_OP`，返回原始文本响应。 */
  public Mono<String> siteReplicationResyncOp(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SITE_REPLICATION_RESYNC_OP", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SR_STATE_EDIT`，返回原始文本响应。 */
  public Mono<String> srStateEdit(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SR_STATE_EDIT", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_TOP_LOCKS`，返回原始文本响应。 */
  public Mono<String> topLocks(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_TOP_LOCKS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_FORCE_UNLOCK`，返回原始文本响应。 */
  public Mono<String> forceUnlock(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_FORCE_UNLOCK", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SPEEDTEST`，返回原始文本响应。 */
  public Mono<String> speedtest(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SPEEDTEST", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SPEEDTEST_OBJECT`，返回原始文本响应。 */
  public Mono<String> speedtestObject(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SPEEDTEST_OBJECT", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SPEEDTEST_DRIVE`，返回原始文本响应。 */
  public Mono<String> speedtestDrive(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SPEEDTEST_DRIVE", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SPEEDTEST_NET`，返回原始文本响应。 */
  public Mono<String> speedtestNet(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SPEEDTEST_NET", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_SPEEDTEST_SITE`，返回原始文本响应。 */
  public Mono<String> speedtestSite(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_SPEEDTEST_SITE", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_CLIENT_DEVNULL`，返回原始文本响应。 */
  public Mono<String> clientDevnull(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_CLIENT_DEVNULL", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_CLIENT_DEVNULL_EXTRA_TIME`，返回原始文本响应。 */
  public Mono<String> clientDevnullExtraTime(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_CLIENT_DEVNULL_EXTRA_TIME", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_TRACE`，返回原始文本响应。 */
  public Mono<String> trace(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_TRACE", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_LOG`，返回原始文本响应。 */
  public Mono<String> log(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_LOG", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_KMS_STATUS`，返回原始文本响应。 */
  public Mono<String> kmsStatus(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_KMS_STATUS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_KMS_KEY_CREATE`，返回原始文本响应。 */
  public Mono<String> kmsKeyCreate(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_KMS_KEY_CREATE", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_KMS_KEY_STATUS`，返回原始文本响应。 */
  public Mono<String> kmsKeyStatus(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_KMS_KEY_STATUS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_OBD_INFO`，返回原始文本响应。 */
  public Mono<String> obdInfo(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_OBD_INFO", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_HEALTH_INFO`，返回原始文本响应。 */
  public Mono<String> healthInfo(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_HEALTH_INFO", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `ADMIN_REVOKE_TOKENS`，返回原始文本响应。 */
  public Mono<String> revokeTokens(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("ADMIN_REVOKE_TOKENS", pathVariables, queryParameters, headers, body, contentType);
  }

}
