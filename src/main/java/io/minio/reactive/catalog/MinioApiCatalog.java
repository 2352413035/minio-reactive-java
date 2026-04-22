package io.minio.reactive.catalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MinIO 服务端公开 HTTP 接口目录。
 *
 * <p>这里的条目对照本地 `minio` 项目的公开路由文件整理，用于保证 SDK 至少能以
 * 原始响应式请求的形式访问每个公开接口。强类型客户端后续可以建立在这些目录条目之上。
 */
public final class MinioApiCatalog {
  private static final List<MinioApiEndpoint> ENDPOINTS = build();

  private MinioApiCatalog() {}

  /** 返回当前已登记的全部公开 MinIO 接口。 */
  public static List<MinioApiEndpoint> all() { return ENDPOINTS; }

  /** 按稳定名称查找接口；名称不存在时主动失败，避免静默调用错误路径。 */
  public static MinioApiEndpoint byName(String name) {
    for (MinioApiEndpoint endpoint : ENDPOINTS) {
      if (endpoint.name().equals(name)) { return endpoint; }
    }
    throw new IllegalArgumentException("Unknown MinIO API endpoint: " + name);
  }

  /** 按接口分组筛选目录，例如 s3、admin、kms、health、metrics、sts。 */
  public static List<MinioApiEndpoint> byFamily(String family) {
    List<MinioApiEndpoint> result = new ArrayList<MinioApiEndpoint>();
    for (MinioApiEndpoint endpoint : ENDPOINTS) {
      if (endpoint.family().equals(family)) { result.add(endpoint); }
    }
    return Collections.unmodifiableList(result);
  }

  private static List<MinioApiEndpoint> build() {
    // 这里故意集中登记路由元数据，方便和 MinIO router 文件逐项核对。
    List<MinioApiEndpoint> endpoints = new ArrayList<MinioApiEndpoint>();
    endpoints.add(e("S3_HEAD_OBJECT", "s3", "HEAD", "/{bucket}/{object}", true, q(), req()));
    endpoints.add(e("S3_GET_OBJECT_ATTRIBUTES", "s3", "GET", "/{bucket}/{object}", true, q("attributes", ""), req()));
    endpoints.add(e("S3_COPY_OBJECT_PART", "s3", "PUT", "/{bucket}/{object}", true, q(), req("partNumber", "uploadId")));
    endpoints.add(e("S3_PUT_OBJECT_PART", "s3", "PUT", "/{bucket}/{object}", true, q(), req("partNumber", "uploadId")));
    endpoints.add(e("S3_LIST_OBJECT_PARTS", "s3", "GET", "/{bucket}/{object}", true, q(), req("uploadId")));
    endpoints.add(e("S3_COMPLETE_MULTIPART_UPLOAD", "s3", "POST", "/{bucket}/{object}", true, q(), req("uploadId")));
    endpoints.add(e("S3_CREATE_MULTIPART_UPLOAD", "s3", "POST", "/{bucket}/{object}", true, q("uploads", ""), req()));
    endpoints.add(e("S3_ABORT_MULTIPART_UPLOAD", "s3", "DELETE", "/{bucket}/{object}", true, q(), req("uploadId")));
    endpoints.add(e("S3_GET_OBJECT_ACL", "s3", "GET", "/{bucket}/{object}", true, q("acl", ""), req()));
    endpoints.add(e("S3_PUT_OBJECT_ACL", "s3", "PUT", "/{bucket}/{object}", true, q("acl", ""), req()));
    endpoints.add(e("S3_GET_OBJECT_TAGGING", "s3", "GET", "/{bucket}/{object}", true, q("tagging", ""), req()));
    endpoints.add(e("S3_PUT_OBJECT_TAGGING", "s3", "PUT", "/{bucket}/{object}", true, q("tagging", ""), req()));
    endpoints.add(e("S3_DELETE_OBJECT_TAGGING", "s3", "DELETE", "/{bucket}/{object}", true, q("tagging", ""), req()));
    endpoints.add(e("S3_SELECT_OBJECT_CONTENT", "s3", "POST", "/{bucket}/{object}", true, q("select", "", "select-type", "2"), req()));
    endpoints.add(e("S3_GET_OBJECT_RETENTION", "s3", "GET", "/{bucket}/{object}", true, q("retention", ""), req()));
    endpoints.add(e("S3_GET_OBJECT_LEGAL_HOLD", "s3", "GET", "/{bucket}/{object}", true, q("legal-hold", ""), req()));
    endpoints.add(e("S3_GET_OBJECT_LAMBDA", "s3", "GET", "/{bucket}/{object}", true, q(), req("lambdaArn")));
    endpoints.add(e("S3_GET_OBJECT", "s3", "GET", "/{bucket}/{object}", true, q(), req()));
    endpoints.add(e("S3_COPY_OBJECT", "s3", "PUT", "/{bucket}/{object}", true, q(), req()));
    endpoints.add(e("S3_PUT_OBJECT_RETENTION", "s3", "PUT", "/{bucket}/{object}", true, q("retention", ""), req()));
    endpoints.add(e("S3_PUT_OBJECT_LEGAL_HOLD", "s3", "PUT", "/{bucket}/{object}", true, q("legal-hold", ""), req()));
    endpoints.add(e("S3_PUT_OBJECT_EXTRACT", "s3", "PUT", "/{bucket}/{object}", true, q(), req()));
    endpoints.add(e("S3_PUT_OBJECT", "s3", "PUT", "/{bucket}/{object}", true, q(), req()));
    endpoints.add(e("S3_DELETE_OBJECT", "s3", "DELETE", "/{bucket}/{object}", true, q(), req()));
    endpoints.add(e("S3_POST_RESTORE_OBJECT", "s3", "POST", "/{bucket}/{object}", true, q("restore", ""), req()));
    endpoints.add(e("S3_GET_BUCKET_LOCATION", "s3", "GET", "/{bucket}", true, q("location", ""), req()));
    endpoints.add(e("S3_GET_BUCKET_POLICY", "s3", "GET", "/{bucket}", true, q("policy", ""), req()));
    endpoints.add(e("S3_GET_BUCKET_LIFECYCLE", "s3", "GET", "/{bucket}", true, q("lifecycle", ""), req()));
    endpoints.add(e("S3_GET_BUCKET_ENCRYPTION", "s3", "GET", "/{bucket}", true, q("encryption", ""), req()));
    endpoints.add(e("S3_GET_BUCKET_OBJECT_LOCK", "s3", "GET", "/{bucket}", true, q("object-lock", ""), req()));
    endpoints.add(e("S3_GET_BUCKET_REPLICATION", "s3", "GET", "/{bucket}", true, q("replication", ""), req()));
    endpoints.add(e("S3_GET_BUCKET_VERSIONING", "s3", "GET", "/{bucket}", true, q("versioning", ""), req()));
    endpoints.add(e("S3_GET_BUCKET_NOTIFICATION", "s3", "GET", "/{bucket}", true, q("notification", ""), req()));
    endpoints.add(e("S3_LISTEN_BUCKET_NOTIFICATION", "s3", "GET", "/{bucket}", true, q(), req("events")));
    endpoints.add(e("S3_RESET_BUCKET_REPLICATION_STATUS", "s3", "GET", "/{bucket}", true, q("replication-reset-status", ""), req()));
    endpoints.add(e("S3_GET_BUCKET_ACL", "s3", "GET", "/{bucket}", true, q("acl", ""), req()));
    endpoints.add(e("S3_PUT_BUCKET_ACL", "s3", "PUT", "/{bucket}", true, q("acl", ""), req()));
    endpoints.add(e("S3_GET_BUCKET_CORS", "s3", "GET", "/{bucket}", true, q("cors", ""), req()));
    endpoints.add(e("S3_PUT_BUCKET_CORS", "s3", "PUT", "/{bucket}", true, q("cors", ""), req()));
    endpoints.add(e("S3_DELETE_BUCKET_CORS", "s3", "DELETE", "/{bucket}", true, q("cors", ""), req()));
    endpoints.add(e("S3_GET_BUCKET_WEBSITE", "s3", "GET", "/{bucket}", true, q("website", ""), req()));
    endpoints.add(e("S3_GET_BUCKET_ACCELERATE", "s3", "GET", "/{bucket}", true, q("accelerate", ""), req()));
    endpoints.add(e("S3_GET_BUCKET_REQUEST_PAYMENT", "s3", "GET", "/{bucket}", true, q("requestPayment", ""), req()));
    endpoints.add(e("S3_GET_BUCKET_LOGGING", "s3", "GET", "/{bucket}", true, q("logging", ""), req()));
    endpoints.add(e("S3_GET_BUCKET_TAGGING", "s3", "GET", "/{bucket}", true, q("tagging", ""), req()));
    endpoints.add(e("S3_DELETE_BUCKET_WEBSITE", "s3", "DELETE", "/{bucket}", true, q("website", ""), req()));
    endpoints.add(e("S3_DELETE_BUCKET_TAGGING", "s3", "DELETE", "/{bucket}", true, q("tagging", ""), req()));
    endpoints.add(e("S3_LIST_MULTIPART_UPLOADS", "s3", "GET", "/{bucket}", true, q("uploads", ""), req()));
    endpoints.add(e("S3_LIST_OBJECTS_V2_WITH_METADATA", "s3", "GET", "/{bucket}", true, q("list-type", "2", "metadata", "true"), req()));
    endpoints.add(e("S3_LIST_OBJECTS_V2", "s3", "GET", "/{bucket}", true, q("list-type", "2"), req()));
    endpoints.add(e("S3_LIST_OBJECT_VERSIONS_WITH_METADATA", "s3", "GET", "/{bucket}", true, q("versions", "", "metadata", "true"), req()));
    endpoints.add(e("S3_LIST_OBJECT_VERSIONS", "s3", "GET", "/{bucket}", true, q("versions", ""), req()));
    endpoints.add(e("S3_GET_BUCKET_POLICY_STATUS", "s3", "GET", "/{bucket}", true, q("policyStatus", ""), req()));
    endpoints.add(e("S3_PUT_BUCKET_LIFECYCLE", "s3", "PUT", "/{bucket}", true, q("lifecycle", ""), req()));
    endpoints.add(e("S3_PUT_BUCKET_REPLICATION", "s3", "PUT", "/{bucket}", true, q("replication", ""), req()));
    endpoints.add(e("S3_PUT_BUCKET_ENCRYPTION", "s3", "PUT", "/{bucket}", true, q("encryption", ""), req()));
    endpoints.add(e("S3_PUT_BUCKET_POLICY", "s3", "PUT", "/{bucket}", true, q("policy", ""), req()));
    endpoints.add(e("S3_PUT_BUCKET_OBJECT_LOCK", "s3", "PUT", "/{bucket}", true, q("object-lock", ""), req()));
    endpoints.add(e("S3_PUT_BUCKET_TAGGING", "s3", "PUT", "/{bucket}", true, q("tagging", ""), req()));
    endpoints.add(e("S3_PUT_BUCKET_VERSIONING", "s3", "PUT", "/{bucket}", true, q("versioning", ""), req()));
    endpoints.add(e("S3_PUT_BUCKET_NOTIFICATION", "s3", "PUT", "/{bucket}", true, q("notification", ""), req()));
    endpoints.add(e("S3_RESET_BUCKET_REPLICATION_START", "s3", "PUT", "/{bucket}", true, q("replication-reset", ""), req()));
    endpoints.add(e("S3_PUT_BUCKET", "s3", "PUT", "/{bucket}", true, q(), req()));
    endpoints.add(e("S3_HEAD_BUCKET", "s3", "HEAD", "/{bucket}", true, q(), req()));
    endpoints.add(e("S3_POST_POLICY_BUCKET", "s3", "POST", "/{bucket}", true, q(), req()));
    endpoints.add(e("S3_DELETE_MULTIPLE_OBJECTS", "s3", "POST", "/{bucket}", true, q("delete", ""), req()));
    endpoints.add(e("S3_DELETE_BUCKET_POLICY", "s3", "DELETE", "/{bucket}", true, q("policy", ""), req()));
    endpoints.add(e("S3_DELETE_BUCKET_REPLICATION", "s3", "DELETE", "/{bucket}", true, q("replication", ""), req()));
    endpoints.add(e("S3_DELETE_BUCKET_LIFECYCLE", "s3", "DELETE", "/{bucket}", true, q("lifecycle", ""), req()));
    endpoints.add(e("S3_DELETE_BUCKET_ENCRYPTION", "s3", "DELETE", "/{bucket}", true, q("encryption", ""), req()));
    endpoints.add(e("S3_DELETE_BUCKET", "s3", "DELETE", "/{bucket}", true, q(), req()));
    endpoints.add(e("S3_GET_BUCKET_REPLICATION_METRICS_V2", "s3", "GET", "/{bucket}", true, q("replication-metrics", "2"), req()));
    endpoints.add(e("S3_GET_BUCKET_REPLICATION_METRICS", "s3", "GET", "/{bucket}", true, q("replication-metrics", ""), req()));
    endpoints.add(e("S3_VALIDATE_BUCKET_REPLICATION_CREDS", "s3", "GET", "/{bucket}", true, q("replication-check", ""), req()));
    endpoints.add(e("S3_LIST_OBJECTS_V1", "s3", "GET", "/{bucket}", true, q(), req()));
    endpoints.add(e("S3_LISTEN_ROOT_NOTIFICATION", "s3", "GET", "/", true, q(), req("events")));
    endpoints.add(e("S3_LIST_BUCKETS", "s3", "GET", "/", true, q(), req()));
    endpoints.add(e("ADMIN_SERVICE_V2", "admin", "POST", "/minio/admin/v3/service", true, q("type", "2"), req("action")));
    endpoints.add(e("ADMIN_SERVICE", "admin", "POST", "/minio/admin/v3/service", true, q(), req("action")));
    endpoints.add(e("ADMIN_SERVER_UPDATE_V2", "admin", "POST", "/minio/admin/v3/update", true, q("type", "2"), req("updateURL")));
    endpoints.add(e("ADMIN_SERVER_UPDATE", "admin", "POST", "/minio/admin/v3/update", true, q(), req("updateURL")));
    endpoints.add(e("ADMIN_SERVER_INFO", "admin", "GET", "/minio/admin/v3/info", true, q(), req()));
    endpoints.add(e("ADMIN_INSPECT_DATA_GET", "admin", "GET", "/minio/admin/v3/inspect-data", true, q(), req()));
    endpoints.add(e("ADMIN_INSPECT_DATA_POST", "admin", "POST", "/minio/admin/v3/inspect-data", true, q(), req()));
    endpoints.add(e("ADMIN_STORAGE_INFO", "admin", "GET", "/minio/admin/v3/storageinfo", true, q(), req()));
    endpoints.add(e("ADMIN_DATA_USAGE_INFO", "admin", "GET", "/minio/admin/v3/datausageinfo", true, q(), req()));
    endpoints.add(e("ADMIN_METRICS", "admin", "GET", "/minio/admin/v3/metrics", true, q(), req()));
    endpoints.add(e("ADMIN_HEAL_ROOT", "admin", "POST", "/minio/admin/v3/heal/", true, q(), req()));
    endpoints.add(e("ADMIN_HEAL_BUCKET", "admin", "POST", "/minio/admin/v3/heal/{bucket}", true, q(), req()));
    endpoints.add(e("ADMIN_HEAL_PREFIX", "admin", "POST", "/minio/admin/v3/heal/{bucket}/{prefix}", true, q(), req()));
    endpoints.add(e("ADMIN_BACKGROUND_HEAL_STATUS", "admin", "POST", "/minio/admin/v3/background-heal/status", true, q(), req()));
    endpoints.add(e("ADMIN_LIST_POOLS", "admin", "GET", "/minio/admin/v3/pools/list", true, q(), req()));
    endpoints.add(e("ADMIN_POOL_STATUS", "admin", "GET", "/minio/admin/v3/pools/status", true, q(), req("pool")));
    endpoints.add(e("ADMIN_START_DECOMMISSION", "admin", "POST", "/minio/admin/v3/pools/decommission", true, q(), req("pool")));
    endpoints.add(e("ADMIN_CANCEL_DECOMMISSION", "admin", "POST", "/minio/admin/v3/pools/cancel", true, q(), req("pool")));
    endpoints.add(e("ADMIN_REBALANCE_START", "admin", "POST", "/minio/admin/v3/rebalance/start", true, q(), req()));
    endpoints.add(e("ADMIN_REBALANCE_STATUS", "admin", "GET", "/minio/admin/v3/rebalance/status", true, q(), req()));
    endpoints.add(e("ADMIN_REBALANCE_STOP", "admin", "POST", "/minio/admin/v3/rebalance/stop", true, q(), req()));
    endpoints.add(e("ADMIN_PROFILING_START", "admin", "POST", "/minio/admin/v3/profiling/start", true, q(), req("profilerType")));
    endpoints.add(e("ADMIN_PROFILING_DOWNLOAD", "admin", "GET", "/minio/admin/v3/profiling/download", true, q(), req()));
    endpoints.add(e("ADMIN_PROFILE", "admin", "POST", "/minio/admin/v3/profile", true, q(), req()));
    endpoints.add(e("ADMIN_GET_CONFIG_KV", "admin", "GET", "/minio/admin/v3/get-config-kv", true, q(), req("key")));
    endpoints.add(e("ADMIN_SET_CONFIG_KV", "admin", "PUT", "/minio/admin/v3/set-config-kv", true, q(), req()));
    endpoints.add(e("ADMIN_DELETE_CONFIG_KV", "admin", "DELETE", "/minio/admin/v3/del-config-kv", true, q(), req()));
    endpoints.add(e("ADMIN_HELP_CONFIG_KV", "admin", "GET", "/minio/admin/v3/help-config-kv", true, q(), req("subSys", "key")));
    endpoints.add(e("ADMIN_LIST_CONFIG_HISTORY_KV", "admin", "GET", "/minio/admin/v3/list-config-history-kv", true, q(), req("count")));
    endpoints.add(e("ADMIN_CLEAR_CONFIG_HISTORY_KV", "admin", "DELETE", "/minio/admin/v3/clear-config-history-kv", true, q(), req("restoreId")));
    endpoints.add(e("ADMIN_RESTORE_CONFIG_HISTORY_KV", "admin", "PUT", "/minio/admin/v3/restore-config-history-kv", true, q(), req("restoreId")));
    endpoints.add(e("ADMIN_GET_CONFIG", "admin", "GET", "/minio/admin/v3/config", true, q(), req()));
    endpoints.add(e("ADMIN_SET_CONFIG", "admin", "PUT", "/minio/admin/v3/config", true, q(), req()));
    endpoints.add(e("ADMIN_ADD_CANNED_POLICY", "admin", "PUT", "/minio/admin/v3/add-canned-policy", true, q(), req("name")));
    endpoints.add(e("ADMIN_ACCOUNT_INFO", "admin", "GET", "/minio/admin/v3/accountinfo", true, q(), req()));
    endpoints.add(e("ADMIN_ADD_USER", "admin", "PUT", "/minio/admin/v3/add-user", true, q(), req("accessKey")));
    endpoints.add(e("ADMIN_SET_USER_STATUS", "admin", "PUT", "/minio/admin/v3/set-user-status", true, q(), req("accessKey", "status")));
    endpoints.add(e("ADMIN_ADD_SERVICE_ACCOUNT", "admin", "PUT", "/minio/admin/v3/add-service-account", true, q(), req()));
    endpoints.add(e("ADMIN_UPDATE_SERVICE_ACCOUNT", "admin", "POST", "/minio/admin/v3/update-service-account", true, q(), req("accessKey")));
    endpoints.add(e("ADMIN_INFO_SERVICE_ACCOUNT", "admin", "GET", "/minio/admin/v3/info-service-account", true, q(), req("accessKey")));
    endpoints.add(e("ADMIN_LIST_SERVICE_ACCOUNTS", "admin", "GET", "/minio/admin/v3/list-service-accounts", true, q(), req()));
    endpoints.add(e("ADMIN_DELETE_SERVICE_ACCOUNT", "admin", "DELETE", "/minio/admin/v3/delete-service-account", true, q(), req("accessKey")));
    endpoints.add(e("ADMIN_TEMPORARY_ACCOUNT_INFO", "admin", "GET", "/minio/admin/v3/temporary-account-info", true, q(), req("accessKey")));
    endpoints.add(e("ADMIN_LIST_ACCESS_KEYS_BULK", "admin", "GET", "/minio/admin/v3/list-access-keys-bulk", true, q(), req("listType")));
    endpoints.add(e("ADMIN_INFO_ACCESS_KEY", "admin", "GET", "/minio/admin/v3/info-access-key", true, q(), req("accessKey")));
    endpoints.add(e("ADMIN_INFO_CANNED_POLICY", "admin", "GET", "/minio/admin/v3/info-canned-policy", true, q(), req("name")));
    endpoints.add(e("ADMIN_LIST_BUCKET_POLICIES", "admin", "GET", "/minio/admin/v3/list-canned-policies", true, q(), req("bucket")));
    endpoints.add(e("ADMIN_LIST_CANNED_POLICIES", "admin", "GET", "/minio/admin/v3/list-canned-policies", true, q(), req()));
    endpoints.add(e("ADMIN_LIST_BUILTIN_POLICY_ENTITIES", "admin", "GET", "/minio/admin/v3/idp/builtin/policy-entities", true, q(), req()));
    endpoints.add(e("ADMIN_REMOVE_CANNED_POLICY", "admin", "DELETE", "/minio/admin/v3/remove-canned-policy", true, q(), req("name")));
    endpoints.add(e("ADMIN_SET_USER_OR_GROUP_POLICY", "admin", "PUT", "/minio/admin/v3/set-user-or-group-policy", true, q(), req("policyName", "userOrGroup", "isGroup")));
    endpoints.add(e("ADMIN_ATTACH_DETACH_BUILTIN_POLICY", "admin", "POST", "/minio/admin/v3/idp/builtin/policy/{operation}", true, q(), req()));
    endpoints.add(e("ADMIN_REMOVE_USER", "admin", "DELETE", "/minio/admin/v3/remove-user", true, q(), req("accessKey")));
    endpoints.add(e("ADMIN_LIST_BUCKET_USERS", "admin", "GET", "/minio/admin/v3/list-users", true, q(), req("bucket")));
    endpoints.add(e("ADMIN_LIST_USERS", "admin", "GET", "/minio/admin/v3/list-users", true, q(), req()));
    endpoints.add(e("ADMIN_USER_INFO", "admin", "GET", "/minio/admin/v3/user-info", true, q(), req("accessKey")));
    endpoints.add(e("ADMIN_UPDATE_GROUP_MEMBERS", "admin", "PUT", "/minio/admin/v3/update-group-members", true, q(), req()));
    endpoints.add(e("ADMIN_GET_GROUP", "admin", "GET", "/minio/admin/v3/group", true, q(), req("group")));
    endpoints.add(e("ADMIN_LIST_GROUPS", "admin", "GET", "/minio/admin/v3/groups", true, q(), req()));
    endpoints.add(e("ADMIN_SET_GROUP_STATUS", "admin", "PUT", "/minio/admin/v3/set-group-status", true, q(), req("group", "status")));
    endpoints.add(e("ADMIN_EXPORT_IAM", "admin", "GET", "/minio/admin/v3/export-iam", true, q(), req()));
    endpoints.add(e("ADMIN_IMPORT_IAM", "admin", "PUT", "/minio/admin/v3/import-iam", true, q(), req()));
    endpoints.add(e("ADMIN_IMPORT_IAM_V2", "admin", "PUT", "/minio/admin/v3/import-iam-v2", true, q(), req()));
    endpoints.add(e("ADMIN_ADD_IDP_CONFIG", "admin", "PUT", "/minio/admin/v3/idp-config/{type}/{name}", true, q(), req()));
    endpoints.add(e("ADMIN_UPDATE_IDP_CONFIG", "admin", "POST", "/minio/admin/v3/idp-config/{type}/{name}", true, q(), req()));
    endpoints.add(e("ADMIN_LIST_IDP_CONFIG", "admin", "GET", "/minio/admin/v3/idp-config/{type}", true, q(), req()));
    endpoints.add(e("ADMIN_GET_IDP_CONFIG", "admin", "GET", "/minio/admin/v3/idp-config/{type}/{name}", true, q(), req()));
    endpoints.add(e("ADMIN_DELETE_IDP_CONFIG", "admin", "DELETE", "/minio/admin/v3/idp-config/{type}/{name}", true, q(), req()));
    endpoints.add(e("ADMIN_LDAP_ADD_SERVICE_ACCOUNT", "admin", "PUT", "/minio/admin/v3/idp/ldap/add-service-account", true, q(), req()));
    endpoints.add(e("ADMIN_LDAP_LIST_ACCESS_KEYS", "admin", "GET", "/minio/admin/v3/idp/ldap/list-access-keys", true, q(), req("userDN", "listType")));
    endpoints.add(e("ADMIN_LDAP_LIST_ACCESS_KEYS_BULK", "admin", "GET", "/minio/admin/v3/idp/ldap/list-access-keys-bulk", true, q(), req("listType")));
    endpoints.add(e("ADMIN_LDAP_POLICY_ENTITIES", "admin", "GET", "/minio/admin/v3/idp/ldap/policy-entities", true, q(), req()));
    endpoints.add(e("ADMIN_LDAP_ATTACH_DETACH_POLICY", "admin", "POST", "/minio/admin/v3/idp/ldap/policy/{operation}", true, q(), req()));
    endpoints.add(e("ADMIN_OPENID_LIST_ACCESS_KEYS_BULK", "admin", "GET", "/minio/admin/v3/idp/openid/list-access-keys-bulk", true, q(), req("listType")));
    endpoints.add(e("ADMIN_GET_BUCKET_QUOTA", "admin", "GET", "/minio/admin/v3/get-bucket-quota", true, q(), req("bucket")));
    endpoints.add(e("ADMIN_SET_BUCKET_QUOTA", "admin", "PUT", "/minio/admin/v3/set-bucket-quota", true, q(), req("bucket")));
    endpoints.add(e("ADMIN_LIST_REMOTE_TARGETS", "admin", "GET", "/minio/admin/v3/list-remote-targets", true, q(), req("bucket", "type")));
    endpoints.add(e("ADMIN_SET_REMOTE_TARGET", "admin", "PUT", "/minio/admin/v3/set-remote-target", true, q(), req("bucket")));
    endpoints.add(e("ADMIN_REMOVE_REMOTE_TARGET", "admin", "DELETE", "/minio/admin/v3/remove-remote-target", true, q(), req("bucket", "arn")));
    endpoints.add(e("ADMIN_REPLICATION_DIFF", "admin", "POST", "/minio/admin/v3/replication/diff", true, q(), req("bucket")));
    endpoints.add(e("ADMIN_REPLICATION_MRF", "admin", "GET", "/minio/admin/v3/replication/mrf", true, q(), req("bucket")));
    endpoints.add(e("ADMIN_START_BATCH_JOB", "admin", "POST", "/minio/admin/v3/start-job", true, q(), req()));
    endpoints.add(e("ADMIN_LIST_BATCH_JOBS", "admin", "GET", "/minio/admin/v3/list-jobs", true, q(), req()));
    endpoints.add(e("ADMIN_BATCH_JOB_STATUS", "admin", "GET", "/minio/admin/v3/status-job", true, q(), req()));
    endpoints.add(e("ADMIN_DESCRIBE_BATCH_JOB", "admin", "GET", "/minio/admin/v3/describe-job", true, q(), req()));
    endpoints.add(e("ADMIN_CANCEL_BATCH_JOB", "admin", "DELETE", "/minio/admin/v3/cancel-job", true, q(), req()));
    endpoints.add(e("ADMIN_EXPORT_BUCKET_METADATA", "admin", "GET", "/minio/admin/v3/export-bucket-metadata", true, q(), req()));
    endpoints.add(e("ADMIN_IMPORT_BUCKET_METADATA", "admin", "PUT", "/minio/admin/v3/import-bucket-metadata", true, q(), req()));
    endpoints.add(e("ADMIN_ADD_TIER", "admin", "PUT", "/minio/admin/v3/tier", true, q(), req()));
    endpoints.add(e("ADMIN_EDIT_TIER", "admin", "POST", "/minio/admin/v3/tier/{tier}", true, q(), req()));
    endpoints.add(e("ADMIN_LIST_TIER", "admin", "GET", "/minio/admin/v3/tier", true, q(), req()));
    endpoints.add(e("ADMIN_REMOVE_TIER", "admin", "DELETE", "/minio/admin/v3/tier/{tier}", true, q(), req()));
    endpoints.add(e("ADMIN_VERIFY_TIER", "admin", "GET", "/minio/admin/v3/tier/{tier}", true, q(), req()));
    endpoints.add(e("ADMIN_TIER_STATS", "admin", "GET", "/minio/admin/v3/tier-stats", true, q(), req()));
    endpoints.add(e("ADMIN_SITE_REPLICATION_ADD", "admin", "PUT", "/minio/admin/v3/site-replication/add", true, q(), req()));
    endpoints.add(e("ADMIN_SITE_REPLICATION_REMOVE", "admin", "PUT", "/minio/admin/v3/site-replication/remove", true, q(), req()));
    endpoints.add(e("ADMIN_SITE_REPLICATION_INFO", "admin", "GET", "/minio/admin/v3/site-replication/info", true, q(), req()));
    endpoints.add(e("ADMIN_SITE_REPLICATION_METAINFO", "admin", "GET", "/minio/admin/v3/site-replication/metainfo", true, q(), req()));
    endpoints.add(e("ADMIN_SITE_REPLICATION_STATUS", "admin", "GET", "/minio/admin/v3/site-replication/status", true, q(), req()));
    endpoints.add(e("ADMIN_SITE_REPLICATION_DEVNULL", "admin", "POST", "/minio/admin/v3/site-replication/devnull", true, q(), req()));
    endpoints.add(e("ADMIN_SITE_REPLICATION_NETPERF", "admin", "POST", "/minio/admin/v3/site-replication/netperf", true, q(), req()));
    endpoints.add(e("ADMIN_SR_PEER_JOIN", "admin", "PUT", "/minio/admin/v3/site-replication/peer/join", true, q(), req()));
    endpoints.add(e("ADMIN_SR_PEER_BUCKET_OPS", "admin", "PUT", "/minio/admin/v3/site-replication/peer/bucket-ops", true, q(), req("bucket", "operation")));
    endpoints.add(e("ADMIN_SR_PEER_IAM_ITEM", "admin", "PUT", "/minio/admin/v3/site-replication/peer/iam-item", true, q(), req()));
    endpoints.add(e("ADMIN_SR_PEER_BUCKET_META", "admin", "PUT", "/minio/admin/v3/site-replication/peer/bucket-meta", true, q(), req()));
    endpoints.add(e("ADMIN_SR_PEER_IDP_SETTINGS", "admin", "GET", "/minio/admin/v3/site-replication/peer/idp-settings", true, q(), req()));
    endpoints.add(e("ADMIN_SITE_REPLICATION_EDIT", "admin", "PUT", "/minio/admin/v3/site-replication/edit", true, q(), req()));
    endpoints.add(e("ADMIN_SR_PEER_EDIT", "admin", "PUT", "/minio/admin/v3/site-replication/peer/edit", true, q(), req()));
    endpoints.add(e("ADMIN_SR_PEER_REMOVE", "admin", "PUT", "/minio/admin/v3/site-replication/peer/remove", true, q(), req()));
    endpoints.add(e("ADMIN_SITE_REPLICATION_RESYNC_OP", "admin", "PUT", "/minio/admin/v3/site-replication/resync/op", true, q(), req("operation")));
    endpoints.add(e("ADMIN_SR_STATE_EDIT", "admin", "PUT", "/minio/admin/v3/site-replication/state/edit", true, q(), req()));
    endpoints.add(e("ADMIN_TOP_LOCKS", "admin", "GET", "/minio/admin/v3/top/locks", true, q(), req()));
    endpoints.add(e("ADMIN_FORCE_UNLOCK", "admin", "POST", "/minio/admin/v3/force-unlock", true, q(), req("paths")));
    endpoints.add(e("ADMIN_SPEEDTEST", "admin", "POST", "/minio/admin/v3/speedtest", true, q(), req()));
    endpoints.add(e("ADMIN_SPEEDTEST_OBJECT", "admin", "POST", "/minio/admin/v3/speedtest/object", true, q(), req()));
    endpoints.add(e("ADMIN_SPEEDTEST_DRIVE", "admin", "POST", "/minio/admin/v3/speedtest/drive", true, q(), req()));
    endpoints.add(e("ADMIN_SPEEDTEST_NET", "admin", "POST", "/minio/admin/v3/speedtest/net", true, q(), req()));
    endpoints.add(e("ADMIN_SPEEDTEST_SITE", "admin", "POST", "/minio/admin/v3/speedtest/site", true, q(), req()));
    endpoints.add(e("ADMIN_CLIENT_DEVNULL", "admin", "POST", "/minio/admin/v3/speedtest/client/devnull", true, q(), req()));
    endpoints.add(e("ADMIN_CLIENT_DEVNULL_EXTRA_TIME", "admin", "POST", "/minio/admin/v3/speedtest/client/devnull/extratime", true, q(), req()));
    endpoints.add(e("ADMIN_TRACE", "admin", "GET", "/minio/admin/v3/trace", true, q(), req()));
    endpoints.add(e("ADMIN_LOG", "admin", "GET", "/minio/admin/v3/log", true, q(), req()));
    endpoints.add(e("ADMIN_KMS_STATUS", "admin", "POST", "/minio/admin/v3/kms/status", true, q(), req()));
    endpoints.add(e("ADMIN_KMS_KEY_CREATE", "admin", "POST", "/minio/admin/v3/kms/key/create", true, q(), req("key-id")));
    endpoints.add(e("ADMIN_KMS_KEY_STATUS", "admin", "GET", "/minio/admin/v3/kms/key/status", true, q(), req()));
    endpoints.add(e("ADMIN_OBD_INFO", "admin", "GET", "/minio/admin/v3/obdinfo", true, q(), req()));
    endpoints.add(e("ADMIN_HEALTH_INFO", "admin", "GET", "/minio/admin/v3/healthinfo", true, q(), req()));
    endpoints.add(e("ADMIN_REVOKE_TOKENS", "admin", "POST", "/minio/admin/v3/revoke-tokens/{userProvider}", true, q(), req()));
    endpoints.add(e("KMS_STATUS", "kms", "GET", "/minio/kms/v1/status", true, q(), req()));
    endpoints.add(e("KMS_METRICS", "kms", "GET", "/minio/kms/v1/metrics", true, q(), req()));
    endpoints.add(e("KMS_APIS", "kms", "GET", "/minio/kms/v1/apis", true, q(), req()));
    endpoints.add(e("KMS_VERSION", "kms", "GET", "/minio/kms/v1/version", true, q(), req()));
    endpoints.add(e("KMS_KEY_CREATE", "kms", "POST", "/minio/kms/v1/key/create", true, q(), req("key-id")));
    endpoints.add(e("KMS_KEY_LIST", "kms", "GET", "/minio/kms/v1/key/list", true, q(), req("pattern")));
    endpoints.add(e("KMS_KEY_STATUS", "kms", "GET", "/minio/kms/v1/key/status", true, q(), req()));
    endpoints.add(e("HEALTH_CLUSTER_GET", "health", "GET", "/minio/health/cluster", false, q(), req()));
    endpoints.add(e("HEALTH_CLUSTER_HEAD", "health", "HEAD", "/minio/health/cluster", false, q(), req()));
    endpoints.add(e("HEALTH_CLUSTER_READ_GET", "health", "GET", "/minio/health/cluster/read", false, q(), req()));
    endpoints.add(e("HEALTH_CLUSTER_READ_HEAD", "health", "HEAD", "/minio/health/cluster/read", false, q(), req()));
    endpoints.add(e("HEALTH_LIVE_GET", "health", "GET", "/minio/health/live", false, q(), req()));
    endpoints.add(e("HEALTH_LIVE_HEAD", "health", "HEAD", "/minio/health/live", false, q(), req()));
    endpoints.add(e("HEALTH_READY_GET", "health", "GET", "/minio/health/ready", false, q(), req()));
    endpoints.add(e("HEALTH_READY_HEAD", "health", "HEAD", "/minio/health/ready", false, q(), req()));
    endpoints.add(eAuth("METRICS_PROMETHEUS_LEGACY", "metrics", "GET", "/minio/prometheus/metrics", "bearer", q(), req()));
    endpoints.add(eAuth("METRICS_V2_CLUSTER", "metrics", "GET", "/minio/v2/metrics/cluster", "bearer", q(), req()));
    endpoints.add(eAuth("METRICS_V2_BUCKET", "metrics", "GET", "/minio/v2/metrics/bucket", "bearer", q(), req()));
    endpoints.add(eAuth("METRICS_V2_NODE", "metrics", "GET", "/minio/v2/metrics/node", "bearer", q(), req()));
    endpoints.add(eAuth("METRICS_V2_RESOURCE", "metrics", "GET", "/minio/v2/metrics/resource", "bearer", q(), req()));
    endpoints.add(eAuth("METRICS_V3", "metrics", "GET", "/minio/metrics/v3{pathComps}", "bearer", q(), req()));
    endpoints.add(e("STS_ASSUME_ROLE_FORM", "sts", "POST", "/", true, q(), req()));
    endpoints.add(e("STS_ASSUME_ROLE_SSO_FORM", "sts", "POST", "/", false, q(), req()));
    endpoints.add(e("STS_ASSUME_ROLE_WITH_CLIENT_GRANTS", "sts", "POST", "/", false, q("Action", "AssumeRoleWithClientGrants", "Version", "2011-06-15"), req("Token")));
    endpoints.add(e("STS_ASSUME_ROLE_WITH_WEB_IDENTITY", "sts", "POST", "/", false, q("Action", "AssumeRoleWithWebIdentity", "Version", "2011-06-15"), req("WebIdentityToken")));
    endpoints.add(e("STS_ASSUME_ROLE_WITH_LDAP_IDENTITY", "sts", "POST", "/", false, q("Action", "AssumeRoleWithLDAPIdentity", "Version", "2011-06-15"), req("LDAPUsername", "LDAPPassword")));
    endpoints.add(e("STS_ASSUME_ROLE_WITH_CERTIFICATE", "sts", "POST", "/", false, q("Action", "AssumeRoleWithCertificate", "Version", "2011-06-15"), req()));
    endpoints.add(e("STS_ASSUME_ROLE_WITH_CUSTOM_TOKEN", "sts", "POST", "/", false, q("Action", "AssumeRoleWithCustomToken", "Version", "2011-06-15"), req()));
    return Collections.unmodifiableList(endpoints);
  }

  private static MinioApiEndpoint eAuth(String name, String family, String method, String pathTemplate, String authScheme, Map<String, String> defaults, List<String> required) {
    return new MinioApiEndpoint(name, family, method, pathTemplate, authScheme, defaults, required);
  }

  private static MinioApiEndpoint e(String name, String family, String method, String pathTemplate, boolean authRequired, Map<String, String> defaults, List<String> required) {
    return new MinioApiEndpoint(name, family, method, pathTemplate, authRequired, defaults, required);
  }

  private static Map<String, String> q(String... keyValues) {
    Map<String, String> map = new LinkedHashMap<String, String>();
    for (int i = 0; i < keyValues.length; i += 2) { map.put(keyValues[i], keyValues[i + 1]); }
    return map;
  }

  private static List<String> req(String... names) { return Arrays.asList(names); }
}
