package io.minio.reactive;

import io.minio.reactive.credentials.ReactiveCredentialsProvider;
import io.minio.reactive.credentials.StaticCredentialsProvider;
import io.minio.reactive.http.ReactiveHttpClient;
import io.minio.reactive.signer.S3RequestSigner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 管理端专用客户端。
 *
 * <p>这个客户端按管理端接口的业务名称提供方法，调用者不需要直接查目录或拼 Map。
 * 如果遇到尚未补充业务模型的特殊场景，可以回退使用 `ReactiveMinioRawClient`。
 */
public final class ReactiveMinioAdminClient extends ReactiveMinioCatalogClientSupport {
  ReactiveMinioAdminClient(ReactiveMinioEndpointExecutor executor) {
    super(executor);
  }

  public static Builder builder() {
    return new Builder();
  }



  /** 新增内部用户，自动生成 MinIO madmin 兼容加密载荷。 */
  public Mono<Void> addUser(io.minio.reactive.messages.admin.AddUserRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request 不能为空");
    }
    return executeEncryptedJsonToVoid(
        "ADMIN_ADD_USER", emptyMap(), map("accessKey", request.accessKey()), request.toPayload());
  }

  /** 新增启用状态的内部用户。 */
  public Mono<Void> addUser(String accessKey, String secretKey) {
    return addUser(io.minio.reactive.messages.admin.AddUserRequest.of(accessKey, secretKey));
  }


  /**
   * 设置单条或多条配置 KV 文本。
   *
   * <p>这是破坏性管理端写操作，只负责生成 madmin 兼容加密载荷并发送；调用方需要确认配置内容正确。
   */
  public Mono<Void> setConfigKvText(String kvText) {
    requireText("kvText", kvText);
    return executeEncryptedBytesToVoid(
        "ADMIN_SET_CONFIG_KV",
        emptyMap(),
        emptyMap(),
        kvText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  /**
   * 设置完整 server config 文本。
   *
   * <p>这是高风险管理端写操作，只提供强类型入口和加密载荷生成，不在集成测试中直接修改环境。
   */
  public Mono<Void> setConfigText(String configText) {
    requireText("configText", configText);
    return executeEncryptedBytesToVoid(
        "ADMIN_SET_CONFIG",
        emptyMap(),
        emptyMap(),
        configText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  /**
   * 删除配置 KV 条目。
   *
   * <p>这是高风险配置变更，调用方必须传入 MinIO madmin 兼容请求体，并在执行前完成配置备份。
   */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> deleteConfigKvEntry(
      byte[] body, String contentType) {
    requireBytes("body", body);
    return deleteConfigKv(body, contentType)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("config-kv-delete", text));
  }

  /** 删除配置 KV 条目，默认使用 text/plain。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> deleteConfigKvEntry(
      byte[] body) {
    return deleteConfigKvEntry(body, "text/plain");
  }

  /** 清理指定配置历史；这是高风险维护操作，不在共享 live 中真实执行。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> clearConfigHistoryEntry(
      String restoreId, byte[] body, String contentType) {
    requireText("restoreId", restoreId);
    return clearConfigHistoryKv(restoreId, body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of(
                    "config-history-clear", text));
  }

  /** 清理指定配置历史，不携带请求体。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> clearConfigHistoryEntry(
      String restoreId) {
    return clearConfigHistoryEntry(restoreId, null, null);
  }

  /** 恢复指定配置历史；调用方必须先确认备份、回滚窗口和 restoreId 来源。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> restoreConfigHistoryEntry(
      String restoreId, byte[] body, String contentType) {
    requireText("restoreId", restoreId);
    return restoreConfigHistoryKv(restoreId, body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of(
                    "config-history-restore", text));
  }

  /** 恢复指定配置历史，不携带请求体。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> restoreConfigHistoryEntry(
      String restoreId) {
    return restoreConfigHistoryEntry(restoreId, null, null);
  }

  /** 获取服务端信息摘要，返回强类型稳定字段并保留原始 JSON。 */
  public Mono<io.minio.reactive.messages.admin.AdminServerInfo> getServerInfo() {
    return serverInfo().map(io.minio.reactive.messages.admin.AdminServerInfo::parse);
  }

  /** 获取存储信息，当前以通用 JSON 结果保留全部字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> getStorageInfo() {
    return storageInfo().map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /** 获取存储信息摘要，提取磁盘在线/离线/修复状态并保留原始 JSON。 */
  public Mono<io.minio.reactive.messages.admin.AdminStorageSummary> getStorageSummary() {
    return storageInfo().map(io.minio.reactive.messages.admin.AdminStorageSummary::parse);
  }

  /** 获取数据使用量信息，当前以通用 JSON 结果保留全部字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> getDataUsageInfo() {
    return dataUsageInfo().map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /** 获取数据使用量摘要，提取对象数、桶数和容量字段并保留原始 JSON。 */
  public Mono<io.minio.reactive.messages.admin.AdminDataUsageSummary> getDataUsageSummary() {
    return dataUsageInfo().map(io.minio.reactive.messages.admin.AdminDataUsageSummary::parse);
  }

  /** 获取当前账号信息，当前以通用 JSON 结果保留全部字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> getAccountInfo() {
    return accountInfo().map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /** 获取当前账号摘要，提取账号名、可读写 bucket 数和策略原文。 */
  public Mono<io.minio.reactive.messages.admin.AdminAccountSummary> getAccountSummary() {
    return accountInfo().map(io.minio.reactive.messages.admin.AdminAccountSummary::parse);
  }

  /**
   * 导出 IAM 备份包。
   *
   * <p>IAM 导出可能包含用户、策略、服务账号等敏感配置，产品入口只保留二进制边界，
   * 不把内容解码成字符串，也不建议调用方写入普通日志。
   */
  public Mono<io.minio.reactive.messages.admin.AdminBinaryResult> exportIamData() {
    return executeToBytes("ADMIN_EXPORT_IAM", emptyMap(), emptyMap(), emptyMap(), null, null)
        .map(bytes -> io.minio.reactive.messages.admin.AdminBinaryResult.of("export-iam", bytes));
  }

  /**
   * 导出 bucket 元数据备份包。
   *
   * <p>bucket 元数据导出可能包含策略、复制、通知等环境配置，产品入口只保留二进制边界，
   * 由调用方自行决定安全存储位置。
   */
  public Mono<io.minio.reactive.messages.admin.AdminBinaryResult> exportBucketMetadataData() {
    return executeToBytes(
            "ADMIN_EXPORT_BUCKET_METADATA", emptyMap(), emptyMap(), emptyMap(), null, null)
        .map(
            bytes ->
                io.minio.reactive.messages.admin.AdminBinaryResult.of(
                    "export-bucket-metadata", bytes));
  }

  /**
   * 导入 IAM 备份包。
   *
   * <p>这是破坏性恢复操作，必须只在独立可回滚 lab 或维护窗口中执行；SDK 只固定请求体
   * 和响应文本边界，不记录备份包内容。
   */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> importIamArchive(
      byte[] archiveBytes, String contentType) {
    requireBytes("archiveBytes", archiveBytes);
    return importIam(archiveBytes, contentType)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("import-iam", text));
  }

  /** 导入 IAM 备份包，默认使用 application/octet-stream。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> importIamArchive(
      byte[] archiveBytes) {
    return importIamArchive(archiveBytes, "application/octet-stream");
  }

  /** 导入 IAM v2 备份包；同样只应在独立 lab 或维护窗口执行。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> importIamV2Archive(
      byte[] archiveBytes, String contentType) {
    requireBytes("archiveBytes", archiveBytes);
    return importIamV2(archiveBytes, contentType)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("import-iam-v2", text));
  }

  /** 导入 IAM v2 备份包，默认使用 application/octet-stream。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> importIamV2Archive(
      byte[] archiveBytes) {
    return importIamV2Archive(archiveBytes, "application/octet-stream");
  }

  /** 导入 bucket metadata 备份包；该操作可能覆盖 bucket 策略、复制、通知等配置。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> importBucketMetadataArchive(
      byte[] archiveBytes, String contentType) {
    requireBytes("archiveBytes", archiveBytes);
    return importBucketMetadata(archiveBytes, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of(
                    "import-bucket-metadata", text));
  }

  /** 导入 bucket metadata 备份包，默认使用 application/octet-stream。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> importBucketMetadataArchive(
      byte[] archiveBytes) {
    return importBucketMetadataArchive(archiveBytes, "application/octet-stream");
  }

  /** 获取后台 heal 状态，先以通用 JSON 结果保留全部字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> getBackgroundHealStatus() {
    return backgroundHealStatus().map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /** 列出 pool 信息，先以通用 JSON 结果保留全部字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> listPoolsInfo() {
    return listPools().map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /** 获取指定 pool 状态，先以通用 JSON 结果保留全部字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> getPoolStatus(String pool) {
    requireText("pool", pool);
    return poolStatus(pool).map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /** 获取 rebalance 状态，先以通用 JSON 结果保留全部字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> getRebalanceStatus() {
    return rebalanceStatus().map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /**
   * 启动 root heal 维护任务。
   *
   * <p>heal 可能消耗集群资源，调用方应在维护窗口中执行；产品入口只固定响应文本边界。
   */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> startRootHeal(
      byte[] body, String contentType) {
    return healRoot(body, contentType)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("heal-root", text));
  }

  /** 启动不带请求体的 root heal 维护任务。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> startRootHeal() {
    return startRootHeal(null, null);
  }

  /** 启动指定 bucket 的 heal 维护任务；共享环境不应默认执行。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> startBucketHeal(
      String bucket, byte[] body, String contentType) {
    requireText("bucket", bucket);
    return healBucket(bucket, body, contentType)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("heal-bucket", text));
  }

  /** 启动不带请求体的 bucket heal 维护任务。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> startBucketHeal(String bucket) {
    return startBucketHeal(bucket, null, null);
  }

  /** 启动指定前缀的 heal 维护任务；prefix 为空时应改用 bucket heal。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> startPrefixHeal(
      String bucket, String prefix, byte[] body, String contentType) {
    requireText("bucket", bucket);
    requireText("prefix", prefix);
    return healPrefix(bucket, prefix, body, contentType)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("heal-prefix", text));
  }

  /** 启动不带请求体的 prefix heal 维护任务。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> startPrefixHeal(
      String bucket, String prefix) {
    return startPrefixHeal(bucket, prefix, null, null);
  }

  /** 启动 pool decommission；这是维护窗口操作，不在共享 live 测试中真实执行。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> startPoolDecommission(
      String pool, byte[] body, String contentType) {
    requireText("pool", pool);
    return startDecommission(pool, body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of(
                    "pool-decommission-start", text));
  }

  /** 启动不带请求体的 pool decommission。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> startPoolDecommission(
      String pool) {
    return startPoolDecommission(pool, null, null);
  }

  /** 取消 pool decommission；调用方应确认 MinIO 当前维护状态。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> cancelPoolDecommission(
      String pool, byte[] body, String contentType) {
    requireText("pool", pool);
    return cancelDecommission(pool, body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of(
                    "pool-decommission-cancel", text));
  }

  /** 取消不带请求体的 pool decommission。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> cancelPoolDecommission(
      String pool) {
    return cancelPoolDecommission(pool, null, null);
  }

  /** 启动 rebalance 维护任务；状态读取继续使用 getRebalanceStatus()。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> startRebalance(
      byte[] body, String contentType) {
    return rebalanceStart(body, contentType)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("rebalance-start", text));
  }

  /** 启动不带请求体的 rebalance 维护任务。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> startRebalance() {
    return startRebalance(null, null);
  }

  /** 停止 rebalance 维护任务；调用方应先确认当前 rebalance 状态。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> stopRebalance(
      byte[] body, String contentType) {
    return rebalanceStop(body, contentType)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("rebalance-stop", text));
  }

  /** 停止不带请求体的 rebalance 维护任务。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> stopRebalance() {
    return stopRebalance(null, null);
  }

  /** 获取 tier 统计信息，先以通用 JSON 结果保留全部字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> getTierStats() {
    return tierStats().map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /** 获取站点复制信息，先以通用 JSON 结果保留全部字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> getSiteReplicationInfo() {
    return siteReplicationInfo().map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /** 获取站点复制状态，先以通用 JSON 结果保留全部字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> getSiteReplicationStatus() {
    return siteReplicationStatus().map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /** 获取站点复制元信息，先以通用 JSON 结果保留全部字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> getSiteReplicationMetainfo() {
    return siteReplicationMetainfo().map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /**
   * 获取站点复制 peer 的 IDP 设置摘要。
   *
   * <p>该接口用于对比站点复制 peer 的身份源配置；响应里可能包含 OIDC provider 的哈希密钥字段，
   * 因此产品模型只提取安全摘要，不保留 raw JSON。
   */
  public Mono<io.minio.reactive.messages.admin.AdminSiteReplicationPeerIdpSettings>
      getSiteReplicationPeerIdpSettings() {
    return srPeerIdpSettings()
        .map(io.minio.reactive.messages.admin.AdminSiteReplicationPeerIdpSettings::parse);
  }

  /**
   * 加入站点复制 peer。
   *
   * <p>这是站点复制写入操作，只应在独立 lab 或维护窗口执行；专用入口固定 madmin 的 api-version=1。
   */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> joinSiteReplicationPeer(
      byte[] body, String contentType) {
    requireBytes("body", body);
    return srPeerJoin(body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of(
                    "site-replication-peer-join", text));
  }

  /** 加入站点复制 peer，默认使用 application/json。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> joinSiteReplicationPeer(
      byte[] body) {
    return joinSiteReplicationPeer(body, "application/json");
  }

  /** 执行站点复制 peer bucket 操作；operation 必须由调用方显式确认。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> applySiteReplicationPeerBucketOperation(
      String bucket, String operation, byte[] body, String contentType) {
    requireText("bucket", bucket);
    requireText("operation", operation);
    requireBytes("body", body);
    return srPeerBucketOps(bucket, operation, body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of(
                    "site-replication-peer-bucket-ops", text));
  }

  /** 执行站点复制 peer bucket 操作，默认使用 application/json。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> applySiteReplicationPeerBucketOperation(
      String bucket, String operation, byte[] body) {
    return applySiteReplicationPeerBucketOperation(bucket, operation, body, "application/json");
  }

  /** 同步站点复制 peer IAM 项；请求体可能包含 IAM 变更信息，SDK 不解析或记录。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> applySiteReplicationPeerIamItem(
      byte[] body, String contentType) {
    requireBytes("body", body);
    return srPeerIamItem(body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of(
                    "site-replication-peer-iam-item", text));
  }

  /** 同步站点复制 peer IAM 项，默认使用 application/json。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> applySiteReplicationPeerIamItem(
      byte[] body) {
    return applySiteReplicationPeerIamItem(body, "application/json");
  }

  /** 同步站点复制 peer bucket metadata；请求体由调用方控制并自行审计。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> applySiteReplicationPeerBucketMetadata(
      byte[] body, String contentType) {
    requireBytes("body", body);
    return srPeerBucketMeta(body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of(
                    "site-replication-peer-bucket-meta", text));
  }

  /** 同步站点复制 peer bucket metadata，默认使用 application/json。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> applySiteReplicationPeerBucketMetadata(
      byte[] body) {
    return applySiteReplicationPeerBucketMetadata(body, "application/json");
  }

  /** 执行站点复制 resync 操作；operation 必须明确传入，避免误触发。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runSiteReplicationResyncOperation(
      String operation, byte[] body, String contentType) {
    requireText("operation", operation);
    requireBytes("body", body);
    return siteReplicationResyncOp(operation, body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of(
                    "site-replication-resync-op", text));
  }

  /** 执行站点复制 resync 操作，默认使用 application/json。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runSiteReplicationResyncOperation(
      String operation, byte[] body) {
    return runSiteReplicationResyncOperation(operation, body, "application/json");
  }

  /** 编辑站点复制状态；这是 lab-only 高风险写入边界。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> editSiteReplicationState(
      byte[] body, String contentType) {
    requireBytes("body", body);
    return srStateEdit(body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of(
                    "site-replication-state-edit", text));
  }

  /** 编辑站点复制状态，默认使用 application/json。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> editSiteReplicationState(
      byte[] body) {
    return editSiteReplicationState(body, "application/json");
  }

  /** 获取锁热点信息，先以通用 JSON 结果保留全部字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> getTopLocksInfo() {
    return topLocks().map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /** 获取 OBD 诊断信息，先以通用 JSON 结果保留全部字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> getObdInfo() {
    return obdInfo().map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /** 获取 Admin health info，先以通用 JSON 结果保留全部字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> getHealthInfo() {
    return healthInfo().map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /** 拉取 Admin metrics，并按 Prometheus 文本格式包装；原文仍可通过 text() 读取。 */
  public Mono<io.minio.reactive.messages.metrics.PrometheusMetrics> scrapeAdminMetrics() {
    return metrics()
        .map(text -> new io.minio.reactive.messages.metrics.PrometheusMetrics("admin", text));
  }

  /**
   * 下载 inspect-data 诊断包。
   *
   * <p>该接口可能返回压缩包或二进制诊断文件，因此产品入口使用二进制包装，不做字符串解码。
   */
  public Mono<io.minio.reactive.messages.admin.AdminBinaryResult> downloadInspectData() {
    return executeToBytes("ADMIN_INSPECT_DATA_GET", emptyMap(), emptyMap(), emptyMap(), null, null)
        .map(bytes -> io.minio.reactive.messages.admin.AdminBinaryResult.of("inspect-data-get", bytes));
  }

  /**
   * 提交 inspect-data 请求并下载诊断包。
   *
   * <p>请求体结构由 MinIO 版本决定；SDK 只负责固定响应式二进制边界和保留原始字节。
   */
  public Mono<io.minio.reactive.messages.admin.AdminBinaryResult> downloadInspectData(
      byte[] body, String contentType) {
    return executeToBytes(
            "ADMIN_INSPECT_DATA_POST", emptyMap(), emptyMap(), emptyMap(), body, contentType)
        .map(bytes -> io.minio.reactive.messages.admin.AdminBinaryResult.of("inspect-data-post", bytes));
  }

  /** 启动指定类型的 profiling，并保留服务端返回的诊断文本。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> startProfiling(
      String profilerType) {
    requireText("profilerType", profilerType);
    return profilingStart(profilerType)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("profiling-start", text));
  }

  /** 下载 profiling 结果；通常是压缩包或 pprof 相关二进制内容。 */
  public Mono<io.minio.reactive.messages.admin.AdminBinaryResult> downloadProfilingData() {
    return executeToBytes(
            "ADMIN_PROFILING_DOWNLOAD", emptyMap(), emptyMap(), emptyMap(), null, null)
        .map(
            bytes ->
                io.minio.reactive.messages.admin.AdminBinaryResult.of(
                    "profiling-download", bytes));
  }

  /** 执行 profile 诊断请求，并把响应作为文本诊断结果保留。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> getProfileResult(
      byte[] body, String contentType) {
    return profile(body, contentType)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("profile", text));
  }

  /** 执行不带请求体的 profile 诊断请求。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> getProfileResult() {
    return getProfileResult(null, null);
  }

  /** 运行 client devnull 探测；该接口可能消耗集群资源，共享环境不应默认执行。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runClientDevnull(
      byte[] body, String contentType) {
    return clientDevnull(body, contentType)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("client-devnull", text));
  }

  /** 运行不带请求体的 client devnull 探测。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runClientDevnull() {
    return runClientDevnull(null, null);
  }

  /** 运行 client devnull extra-time 探测；调用方应在独立诊断窗口中执行。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runClientDevnullExtraTime(
      byte[] body, String contentType) {
    return clientDevnullExtraTime(body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of(
                    "client-devnull-extra-time", text));
  }

  /** 运行不带请求体的 client devnull extra-time 探测。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runClientDevnullExtraTime() {
    return runClientDevnullExtraTime(null, null);
  }

  /** 运行 site replication devnull 探测；只包装响应文本，不在共享 live 测试中真实执行。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runSiteReplicationDevnull(
      byte[] body, String contentType) {
    return siteReplicationDevnull(body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of(
                    "site-replication-devnull", text));
  }

  /** 运行不带请求体的 site replication devnull 探测。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runSiteReplicationDevnull() {
    return runSiteReplicationDevnull(null, null);
  }

  /** 运行 site replication netperf 探测；调用方应确认目标集群和负载窗口。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runSiteReplicationNetperf(
      byte[] body, String contentType) {
    return siteReplicationNetperf(body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of(
                    "site-replication-netperf", text));
  }

  /** 运行不带请求体的 site replication netperf 探测。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runSiteReplicationNetperf() {
    return runSiteReplicationNetperf(null, null);
  }

  /** 运行集群 speedtest；这是高资源诊断接口，不能在共享环境默认执行。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runSpeedtest(
      byte[] body, String contentType) {
    return speedtest(body, contentType)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("speedtest", text));
  }

  /** 运行不带请求体的集群 speedtest。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runSpeedtest() {
    return runSpeedtest(null, null);
  }

  /** 运行对象层 speedtest。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runObjectSpeedtest(
      byte[] body, String contentType) {
    return speedtestObject(body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of("speedtest-object", text));
  }

  /** 运行不带请求体的对象层 speedtest。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runObjectSpeedtest() {
    return runObjectSpeedtest(null, null);
  }

  /** 运行磁盘层 speedtest。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runDriveSpeedtest(
      byte[] body, String contentType) {
    return speedtestDrive(body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of("speedtest-drive", text));
  }

  /** 运行不带请求体的磁盘层 speedtest。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runDriveSpeedtest() {
    return runDriveSpeedtest(null, null);
  }

  /** 运行网络层 speedtest。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runNetworkSpeedtest(
      byte[] body, String contentType) {
    return speedtestNet(body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of("speedtest-net", text));
  }

  /** 运行不带请求体的网络层 speedtest。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runNetworkSpeedtest() {
    return runNetworkSpeedtest(null, null);
  }

  /** 运行站点层 speedtest。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runSiteSpeedtest(
      byte[] body, String contentType) {
    return speedtestSite(body, contentType)
        .map(
            text ->
                io.minio.reactive.messages.admin.AdminTextResult.of("speedtest-site", text));
  }

  /** 运行不带请求体的站点层 speedtest。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> runSiteSpeedtest() {
    return runSiteSpeedtest(null, null);
  }

  /**
   * 通过 Admin 路由读取 KMS 状态。
   *
   * <p>普通 KMS 场景优先使用 `ReactiveMinioKmsClient#getStatus()`；该方法用于需要明确走
   * madmin 兼容路径的集成环境。
   */
  public Mono<io.minio.reactive.messages.kms.KmsJsonResult> getAdminKmsStatus() {
    return kmsStatus().map(io.minio.reactive.messages.kms.KmsJsonResult::parse);
  }

  /**
   * 通过 Admin 路由创建 KMS key。
   *
   * <p>普通 KMS 场景优先使用 `ReactiveMinioKmsClient#createKey(...)`；该方法只保留
   * madmin 兼容路径的强类型入口。
   */
  public Mono<Void> createAdminKmsKey(String keyId) {
    requireText("keyId", keyId);
    return executeToVoid("ADMIN_KMS_KEY_CREATE", emptyMap(), map("key-id", keyId), emptyMap(), null, null);
  }

  /** 通过 Admin 路由获取默认 KMS key 状态；普通场景优先使用 `ReactiveMinioKmsClient`。 */
  public Mono<io.minio.reactive.messages.kms.KmsKeyStatus> getAdminKmsKeyStatus() {
    return kmsKeyStatus().map(io.minio.reactive.messages.kms.KmsKeyStatus::parse);
  }

  /** 通过 Admin 路由获取指定 KMS key 状态；普通场景优先使用 `ReactiveMinioKmsClient`。 */
  public Mono<io.minio.reactive.messages.kms.KmsKeyStatus> getAdminKmsKeyStatus(String keyId) {
    requireText("keyId", keyId);
    return executeToString(
            "ADMIN_KMS_KEY_STATUS", emptyMap(), map("key-id", keyId), emptyMap(), null, null)
        .map(io.minio.reactive.messages.kms.KmsKeyStatus::parse);
  }

  /**
   * 读取 Admin trace 输出流。
   *
   * <p>trace 可能是持续输出的诊断流，不能把它当成普通 JSON 摘要。调用方应主动设置超时、过滤条件和取消策略。
   */
  public Flux<byte[]> traceStream() {
    return executeToBody("ADMIN_TRACE", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /**
   * 读取 Admin 日志输出流。
   *
   * <p>日志接口可能长时间保持连接，SDK 只固定响应式流式边界，不在这里猜测日志行格式。
   */
  public Flux<byte[]> logStream() {
    return executeToBody("ADMIN_LOG", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 获取配置帮助信息；这是明文安全只读接口，不读取真实配置值。 */
  public Mono<io.minio.reactive.messages.admin.AdminConfigHelp> getConfigHelp(
      String subSys, String key) {
    requireText("subSys", subSys);
    return helpConfigKv(subSys, key == null ? "" : key)
        .map(io.minio.reactive.messages.admin.AdminConfigHelp::parse);
  }

  /** 获取指定配置子系统的帮助信息。 */
  public Mono<io.minio.reactive.messages.admin.AdminConfigHelp> getConfigHelp(String subSys) {
    return getConfigHelp(subSys, "");
  }

  /** 获取单个用户信息，当前以通用 JSON 结果保留全部字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminUserInfo> getUserInfo(String accessKey) {
    requireText("accessKey", accessKey);
    return userInfo(accessKey).map(io.minio.reactive.messages.admin.AdminUserInfo::parse);
  }

  /** 删除内部用户。 */
  public Mono<Void> deleteUser(String accessKey) {
    requireText("accessKey", accessKey);
    return executeToVoid("ADMIN_REMOVE_USER", emptyMap(), map("accessKey", accessKey), emptyMap(), null, null);
  }

  /** 设置用户启用或禁用状态。 */
  public Mono<Void> setUserEnabled(String accessKey, boolean enabled) {
    requireText("accessKey", accessKey);
    return executeToVoid(
        "ADMIN_SET_USER_STATUS",
        emptyMap(),
        map("accessKey", accessKey, "status", enabled ? "enabled" : "disabled"),
        emptyMap(),
        null,
        null);
  }


  /** 列出全部内置策略，返回通用 JSON 结果。 */
  public Mono<io.minio.reactive.messages.admin.AdminPolicyList> listPolicies() {
    return listCannedPolicies().map(io.minio.reactive.messages.admin.AdminPolicyList::parse);
  }

  /** 列出指定 bucket 可用的内置策略，返回通用 JSON 结果。 */
  public Mono<io.minio.reactive.messages.admin.AdminPolicyList> listPolicies(String bucket) {
    requireText("bucket", bucket);
    return listBucketPolicies(bucket).map(io.minio.reactive.messages.admin.AdminPolicyList::parse);
  }

  /** 获取策略内容，返回通用 JSON 结果。 */
  public Mono<io.minio.reactive.messages.admin.AdminPolicyInfo> getPolicy(String name) {
    requireText("name", name);
    return infoCannedPolicy(name).map(io.minio.reactive.messages.admin.AdminPolicyInfo::parse);
  }

  /** 获取策略内容和时间戳信息，返回通用 JSON 结果。 */
  public Mono<io.minio.reactive.messages.admin.AdminPolicyInfo> getPolicyV2(String name) {
    requireText("name", name);
    return executeToString("ADMIN_INFO_CANNED_POLICY", emptyMap(), map("name", name, "v", "2"), emptyMap(), null, null)
        .map(io.minio.reactive.messages.admin.AdminPolicyInfo::parse);
  }

  /** 新增或更新内置策略。 */
  public Mono<Void> putPolicy(String name, String policyJson) {
    requireText("name", name);
    requireText("policyJson", policyJson);
    byte[] body = policyJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    return executeToVoid("ADMIN_ADD_CANNED_POLICY", emptyMap(), map("name", name), emptyMap(), body, "application/json");
  }

  /** 删除内置策略。 */
  public Mono<Void> deletePolicy(String name) {
    requireText("name", name);
    return executeToVoid("ADMIN_REMOVE_CANNED_POLICY", emptyMap(), map("name", name), emptyMap(), null, null);
  }

  /** 给用户绑定策略。 */
  public Mono<Void> setUserPolicy(String policyName, String accessKey) {
    requireText("policyName", policyName);
    requireText("accessKey", accessKey);
    return executeToVoid(
        "ADMIN_SET_USER_OR_GROUP_POLICY",
        emptyMap(),
        map("policyName", policyName, "userOrGroup", accessKey, "isGroup", "false"),
        emptyMap(),
        null,
        null);
  }

  /** 给用户组绑定策略。 */
  public Mono<Void> setGroupPolicy(String policyName, String groupName) {
    requireText("policyName", policyName);
    requireText("groupName", groupName);
    return executeToVoid(
        "ADMIN_SET_USER_OR_GROUP_POLICY",
        emptyMap(),
        map("policyName", policyName, "userOrGroup", groupName, "isGroup", "true"),
        emptyMap(),
        null,
        null);
  }

  /** 列出内置策略绑定实体摘要。 */
  public Mono<io.minio.reactive.messages.admin.AdminPolicyEntities> listPolicyEntities() {
    return listBuiltinPolicyEntities()
        .map(io.minio.reactive.messages.admin.AdminPolicyEntities::parse);
  }

  /**
   * 绑定内置策略实体。
   *
   * <p>请求体结构与 MinIO madmin 保持一致；这是权限变更操作，调用方应在受控窗口中执行。
   */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> attachBuiltinPolicy(
      byte[] body, String contentType) {
    requireBytes("body", body);
    return attachDetachBuiltinPolicy("attach", body, contentType)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("builtin-policy-attach", text));
  }

  /** 绑定内置策略实体，默认使用 application/json。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> attachBuiltinPolicy(
      byte[] body) {
    return attachBuiltinPolicy(body, "application/json");
  }

  /** 解绑内置策略实体；调用方应确认请求体只包含目标实体。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> detachBuiltinPolicy(
      byte[] body, String contentType) {
    requireBytes("body", body);
    return attachDetachBuiltinPolicy("detach", body, contentType)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("builtin-policy-detach", text));
  }

  /** 解绑内置策略实体，默认使用 application/json。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> detachBuiltinPolicy(
      byte[] body) {
    return detachBuiltinPolicy(body, "application/json");
  }

  /** 列出 LDAP 策略绑定实体摘要；只统计映射数量并保留原始策略实体 JSON。 */
  public Mono<io.minio.reactive.messages.admin.AdminPolicyEntities> listLdapPolicyEntities() {
    return ldapPolicyEntities().map(io.minio.reactive.messages.admin.AdminPolicyEntities::parse);
  }


  /** 绑定 LDAP 策略实体；该操作会改变身份源策略映射，必须使用受控请求体。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> attachLdapPolicy(
      byte[] body, String contentType) {
    requireBytes("body", body);
    return ldapAttachDetachPolicy("attach", body, contentType)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("ldap-policy-attach", text));
  }

  /** 绑定 LDAP 策略实体，默认使用 application/json。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> attachLdapPolicy(byte[] body) {
    return attachLdapPolicy(body, "application/json");
  }

  /** 解绑 LDAP 策略实体；不会保存请求体中的用户、组或策略名称。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> detachLdapPolicy(
      byte[] body, String contentType) {
    requireBytes("body", body);
    return ldapAttachDetachPolicy("detach", body, contentType)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("ldap-policy-detach", text));
  }

  /** 解绑 LDAP 策略实体，默认使用 application/json。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> detachLdapPolicy(byte[] body) {
    return detachLdapPolicy(body, "application/json");
  }


  /** 列出全部用户的加密响应；默认响应解密能力完成前不伪装成明文用户列表。 */
  public Mono<io.minio.reactive.messages.admin.EncryptedAdminResponse> listUsersEncrypted() {
    return executeToBytes("ADMIN_LIST_USERS", emptyMap(), emptyMap(), emptyMap(), null, null)
        .map(io.minio.reactive.messages.admin.EncryptedAdminResponse::new);
  }

  /** 获取配置 KV 的加密响应；配置读取由 MinIO 服务端加密返回，不能伪装成明文 typed 模型。 */
  public Mono<io.minio.reactive.messages.admin.EncryptedAdminResponse> getConfigKvEncrypted(
      String key) {
    requireText("key", key);
    return executeToBytes("ADMIN_GET_CONFIG_KV", emptyMap(), map("key", key), emptyMap(), null, null)
        .map(io.minio.reactive.messages.admin.EncryptedAdminResponse::new);
  }

  /** 列出配置历史的加密响应；解密 Gate 通过前只暴露边界对象。 */
  public Mono<io.minio.reactive.messages.admin.EncryptedAdminResponse>
      listConfigHistoryKvEncrypted(int count) {
    if (count < 0) {
      throw new IllegalArgumentException("count 不能小于 0");
    }
    return executeToBytes(
            "ADMIN_LIST_CONFIG_HISTORY_KV",
            emptyMap(),
            map("count", String.valueOf(count)),
            emptyMap(),
            null,
            null)
        .map(io.minio.reactive.messages.admin.EncryptedAdminResponse::new);
  }

  /** 获取完整配置的加密响应；这是 crypto boundary，不进入明文 typed 完成口径。 */
  public Mono<io.minio.reactive.messages.admin.EncryptedAdminResponse> getConfigEncrypted() {
    return executeToBytes("ADMIN_GET_CONFIG", emptyMap(), emptyMap(), emptyMap(), null, null)
        .map(io.minio.reactive.messages.admin.EncryptedAdminResponse::new);
  }

  /** 列出全部用户组，返回强类型用户组名称列表。 */
  public Mono<io.minio.reactive.messages.admin.AdminGroupList> listGroupsTyped() {
    return listGroups().map(io.minio.reactive.messages.admin.AdminGroupList::parse);
  }

  /** 列出指定 bucket 相关用户摘要；这是明文只读 IAM 查询，不返回 secret。 */
  public Mono<io.minio.reactive.messages.admin.AdminUserList> listBucketUsersInfo(
      String bucket) {
    requireText("bucket", bucket);
    return listBucketUsers(bucket).map(io.minio.reactive.messages.admin.AdminUserList::parse);
  }

  /** 获取单个用户组信息。 */
  public Mono<io.minio.reactive.messages.admin.AdminGroupInfo> getGroupInfo(String group) {
    requireText("group", group);
    return getGroup(group).map(io.minio.reactive.messages.admin.AdminGroupInfo::parse);
  }

  /** 设置用户组启用或禁用状态。 */
  public Mono<Void> setGroupEnabled(String group, boolean enabled) {
    requireText("group", group);
    return executeToVoid(
        "ADMIN_SET_GROUP_STATUS",
        emptyMap(),
        map("group", group, "status", enabled ? "enabled" : "disabled"),
        emptyMap(),
        null,
        null);
  }

  /** 更新用户组成员，使用 madmin-go 对齐的 JSON 请求对象。 */
  public Mono<Void> updateGroupMembers(
      io.minio.reactive.messages.admin.UpdateGroupMembersRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request 不能为空");
    }
    return executeToVoid(
        "ADMIN_UPDATE_GROUP_MEMBERS",
        emptyMap(),
        emptyMap(),
        emptyMap(),
        io.minio.reactive.util.JsonSupport.toJsonBytes(request.toPayload()),
        "application/json");
  }

  /**
   * 创建服务账号，返回可解释的创建结果。
   *
   * <p>当前阶段尚不能解密服务端默认 Argon2id/ChaCha20 响应，因此结果可能只包含加密响应原文。
   */
  public Mono<io.minio.reactive.messages.admin.ServiceAccountCreateResult> createServiceAccount(
      io.minio.reactive.messages.admin.AddServiceAccountRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request 不能为空");
    }
    return executeEncryptedJsonToBytes(
            "ADMIN_ADD_SERVICE_ACCOUNT", emptyMap(), emptyMap(), request.toPayload())
        .map(io.minio.reactive.messages.admin.ServiceAccountCreateResult::fromResponseBytes);
  }

  /** 获取服务账号信息的加密响应；默认响应解密能力完成前不伪装成明文模型。 */
  public Mono<io.minio.reactive.messages.admin.EncryptedAdminResponse> getServiceAccountInfoEncrypted(
      String accessKey) {
    requireText("accessKey", accessKey);
    return executeToBytes(
            "ADMIN_INFO_SERVICE_ACCOUNT", emptyMap(), map("accessKey", accessKey), emptyMap(), null, null)
        .map(io.minio.reactive.messages.admin.EncryptedAdminResponse::new);
  }

  /** 列出当前用户的服务账号加密响应；默认响应解密能力完成前不伪装成明文模型。 */
  public Mono<io.minio.reactive.messages.admin.EncryptedAdminResponse> listServiceAccountsEncrypted() {
    return executeToBytes(
            "ADMIN_LIST_SERVICE_ACCOUNTS", emptyMap(), emptyMap(), emptyMap(), null, null)
        .map(io.minio.reactive.messages.admin.EncryptedAdminResponse::new);
  }

  /** 删除服务账号，返回空结果而不是兼容入口的字符串。 */
  public Mono<Void> deleteServiceAccountTyped(String accessKey) {
    requireText("accessKey", accessKey);
    return executeToVoid(
        "ADMIN_DELETE_SERVICE_ACCOUNT", emptyMap(), map("accessKey", accessKey), emptyMap(), null, null);
  }


  /**
   * 创建服务账号，返回服务端加密响应载荷。
   *
   * <p>MinIO 服务端会用 madmin 默认加密算法返回服务账号凭证。当前 Java 端尚不能解密默认
   * Argon2id/ChaCha20 响应，因此这里返回 `EncryptedAdminResponse`，不伪装成已解析凭证。
   */
  public Mono<io.minio.reactive.messages.admin.EncryptedAdminResponse> addServiceAccount(
      io.minio.reactive.messages.admin.AddServiceAccountRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request 不能为空");
    }
    return executeEncryptedJsonToBytes(
            "ADMIN_ADD_SERVICE_ACCOUNT", emptyMap(), emptyMap(), request.toPayload())
        .map(io.minio.reactive.messages.admin.EncryptedAdminResponse::new);
  }

  /** 调用 `ADMIN_SERVICE_V2`。 */
  public Mono<String> serviceV2(String action, byte[] body, String contentType) {
    return executeToString("ADMIN_SERVICE_V2", emptyMap(), map("action", action), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SERVICE_V2`，不携带请求体。 */
  public Mono<String> serviceV2(String action) {
    return serviceV2(action, null, null);
  }

  /** 调用 `ADMIN_SERVICE`。 */
  public Mono<String> service(String action, byte[] body, String contentType) {
    return executeToString("ADMIN_SERVICE", emptyMap(), map("action", action), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SERVICE`，不携带请求体。 */
  public Mono<String> service(String action) {
    return service(action, null, null);
  }

  /** 调用 `ADMIN_SERVER_UPDATE_V2`。 */
  public Mono<String> serverUpdateV2(String updateURL, byte[] body, String contentType) {
    return executeToString("ADMIN_SERVER_UPDATE_V2", emptyMap(), map("updateURL", updateURL), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SERVER_UPDATE_V2`，不携带请求体。 */
  public Mono<String> serverUpdateV2(String updateURL) {
    return serverUpdateV2(updateURL, null, null);
  }

  /** 调用 `ADMIN_SERVER_UPDATE`。 */
  public Mono<String> serverUpdate(String updateURL, byte[] body, String contentType) {
    return executeToString("ADMIN_SERVER_UPDATE", emptyMap(), map("updateURL", updateURL), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SERVER_UPDATE`，不携带请求体。 */
  public Mono<String> serverUpdate(String updateURL) {
    return serverUpdate(updateURL, null, null);
  }

  /** 调用 `ADMIN_SERVER_INFO`。 */
  public Mono<String> serverInfo() {
    return executeToString("ADMIN_SERVER_INFO", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_INSPECT_DATA_GET`。 */
  public Mono<String> inspectDataGet() {
    return executeToString("ADMIN_INSPECT_DATA_GET", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_INSPECT_DATA_POST`。 */
  public Mono<String> inspectDataPost(byte[] body, String contentType) {
    return executeToString("ADMIN_INSPECT_DATA_POST", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_INSPECT_DATA_POST`，不携带请求体。 */
  public Mono<String> inspectDataPost() {
    return inspectDataPost(null, null);
  }

  /** 调用 `ADMIN_STORAGE_INFO`。 */
  public Mono<String> storageInfo() {
    return executeToString("ADMIN_STORAGE_INFO", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_DATA_USAGE_INFO`。 */
  public Mono<String> dataUsageInfo() {
    return executeToString("ADMIN_DATA_USAGE_INFO", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_METRICS`。 */
  public Mono<String> metrics() {
    return executeToString("ADMIN_METRICS", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_HEAL_ROOT`。 */
  public Mono<String> healRoot(byte[] body, String contentType) {
    return executeToString("ADMIN_HEAL_ROOT", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_HEAL_ROOT`，不携带请求体。 */
  public Mono<String> healRoot() {
    return healRoot(null, null);
  }

  /** 调用 `ADMIN_HEAL_BUCKET`。 */
  public Mono<String> healBucket(String bucket, byte[] body, String contentType) {
    return executeToString("ADMIN_HEAL_BUCKET", map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_HEAL_BUCKET`，不携带请求体。 */
  public Mono<String> healBucket(String bucket) {
    return healBucket(bucket, null, null);
  }

  /** 调用 `ADMIN_HEAL_PREFIX`。 */
  public Mono<String> healPrefix(String bucket, String prefix, byte[] body, String contentType) {
    return executeToString("ADMIN_HEAL_PREFIX", map("bucket", bucket, "prefix", prefix), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_HEAL_PREFIX`，不携带请求体。 */
  public Mono<String> healPrefix(String bucket, String prefix) {
    return healPrefix(bucket, prefix, null, null);
  }

  /** 调用 `ADMIN_BACKGROUND_HEAL_STATUS`。 */
  public Mono<String> backgroundHealStatus(byte[] body, String contentType) {
    return executeToString("ADMIN_BACKGROUND_HEAL_STATUS", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_BACKGROUND_HEAL_STATUS`，不携带请求体。 */
  public Mono<String> backgroundHealStatus() {
    return backgroundHealStatus(null, null);
  }

  /** 调用 `ADMIN_LIST_POOLS`。 */
  public Mono<String> listPools() {
    return executeToString("ADMIN_LIST_POOLS", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_POOL_STATUS`。 */
  public Mono<String> poolStatus(String pool) {
    return executeToString("ADMIN_POOL_STATUS", emptyMap(), map("pool", pool), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_START_DECOMMISSION`。 */
  public Mono<String> startDecommission(String pool, byte[] body, String contentType) {
    return executeToString("ADMIN_START_DECOMMISSION", emptyMap(), map("pool", pool), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_START_DECOMMISSION`，不携带请求体。 */
  public Mono<String> startDecommission(String pool) {
    return startDecommission(pool, null, null);
  }

  /** 调用 `ADMIN_CANCEL_DECOMMISSION`。 */
  public Mono<String> cancelDecommission(String pool, byte[] body, String contentType) {
    return executeToString("ADMIN_CANCEL_DECOMMISSION", emptyMap(), map("pool", pool), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_CANCEL_DECOMMISSION`，不携带请求体。 */
  public Mono<String> cancelDecommission(String pool) {
    return cancelDecommission(pool, null, null);
  }

  /** 调用 `ADMIN_REBALANCE_START`。 */
  public Mono<String> rebalanceStart(byte[] body, String contentType) {
    return executeToString("ADMIN_REBALANCE_START", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_REBALANCE_START`，不携带请求体。 */
  public Mono<String> rebalanceStart() {
    return rebalanceStart(null, null);
  }

  /** 调用 `ADMIN_REBALANCE_STATUS`。 */
  public Mono<String> rebalanceStatus() {
    return executeToString("ADMIN_REBALANCE_STATUS", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_REBALANCE_STOP`。 */
  public Mono<String> rebalanceStop(byte[] body, String contentType) {
    return executeToString("ADMIN_REBALANCE_STOP", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_REBALANCE_STOP`，不携带请求体。 */
  public Mono<String> rebalanceStop() {
    return rebalanceStop(null, null);
  }

  /** 调用 `ADMIN_PROFILING_START`。 */
  public Mono<String> profilingStart(String profilerType, byte[] body, String contentType) {
    return executeToString("ADMIN_PROFILING_START", emptyMap(), map("profilerType", profilerType), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_PROFILING_START`，不携带请求体。 */
  public Mono<String> profilingStart(String profilerType) {
    return profilingStart(profilerType, null, null);
  }

  /** 调用 `ADMIN_PROFILING_DOWNLOAD`。 */
  public Mono<String> profilingDownload() {
    return executeToString("ADMIN_PROFILING_DOWNLOAD", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_PROFILE`。 */
  public Mono<String> profile(byte[] body, String contentType) {
    return executeToString("ADMIN_PROFILE", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_PROFILE`，不携带请求体。 */
  public Mono<String> profile() {
    return profile(null, null);
  }

  /** 调用 `ADMIN_GET_CONFIG_KV`。 */
  public Mono<String> getConfigKv(String key) {
    return executeToString("ADMIN_GET_CONFIG_KV", emptyMap(), map("key", key), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_SET_CONFIG_KV`。 */
  public Mono<String> setConfigKv(byte[] body, String contentType) {
    return executeToString("ADMIN_SET_CONFIG_KV", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SET_CONFIG_KV`，不携带请求体。 */
  public Mono<String> setConfigKv() {
    return setConfigKv(null, null);
  }

  /** 调用 `ADMIN_DELETE_CONFIG_KV`。 */
  public Mono<String> deleteConfigKv(byte[] body, String contentType) {
    return executeToString("ADMIN_DELETE_CONFIG_KV", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_DELETE_CONFIG_KV`，不携带请求体。 */
  public Mono<String> deleteConfigKv() {
    return deleteConfigKv(null, null);
  }

  /** 调用 `ADMIN_HELP_CONFIG_KV`。 */
  public Mono<String> helpConfigKv(String subSys, String key) {
    return executeToString("ADMIN_HELP_CONFIG_KV", emptyMap(), map("subSys", subSys, "key", key), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_LIST_CONFIG_HISTORY_KV`。 */
  public Mono<String> listConfigHistoryKv(String count) {
    return executeToString("ADMIN_LIST_CONFIG_HISTORY_KV", emptyMap(), map("count", count), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_CLEAR_CONFIG_HISTORY_KV`。 */
  public Mono<String> clearConfigHistoryKv(String restoreId, byte[] body, String contentType) {
    return executeToString("ADMIN_CLEAR_CONFIG_HISTORY_KV", emptyMap(), map("restoreId", restoreId), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_CLEAR_CONFIG_HISTORY_KV`，不携带请求体。 */
  public Mono<String> clearConfigHistoryKv(String restoreId) {
    return clearConfigHistoryKv(restoreId, null, null);
  }

  /** 调用 `ADMIN_RESTORE_CONFIG_HISTORY_KV`。 */
  public Mono<String> restoreConfigHistoryKv(String restoreId, byte[] body, String contentType) {
    return executeToString("ADMIN_RESTORE_CONFIG_HISTORY_KV", emptyMap(), map("restoreId", restoreId), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_RESTORE_CONFIG_HISTORY_KV`，不携带请求体。 */
  public Mono<String> restoreConfigHistoryKv(String restoreId) {
    return restoreConfigHistoryKv(restoreId, null, null);
  }

  /** 调用 `ADMIN_GET_CONFIG`。 */
  public Mono<String> getConfig() {
    return executeToString("ADMIN_GET_CONFIG", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_SET_CONFIG`。 */
  public Mono<String> setConfig(byte[] body, String contentType) {
    return executeToString("ADMIN_SET_CONFIG", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SET_CONFIG`，不携带请求体。 */
  public Mono<String> setConfig() {
    return setConfig(null, null);
  }

  /** 调用 `ADMIN_ADD_CANNED_POLICY`。 */
  public Mono<String> addCannedPolicy(String name, byte[] body, String contentType) {
    return executeToString("ADMIN_ADD_CANNED_POLICY", emptyMap(), map("name", name), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_ADD_CANNED_POLICY`，不携带请求体。 */
  public Mono<String> addCannedPolicy(String name) {
    return addCannedPolicy(name, null, null);
  }

  /** 调用 `ADMIN_ACCOUNT_INFO`。 */
  public Mono<String> accountInfo() {
    return executeToString("ADMIN_ACCOUNT_INFO", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_ADD_USER`。 */
  public Mono<String> addUser(String accessKey, byte[] body, String contentType) {
    return executeToString("ADMIN_ADD_USER", emptyMap(), map("accessKey", accessKey), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_ADD_USER`，不携带请求体。 */
  public Mono<String> addUser(String accessKey) {
    return addUser(accessKey, null, null);
  }

  /** 调用 `ADMIN_SET_USER_STATUS`。 */
  public Mono<String> setUserStatus(String accessKey, String status, byte[] body, String contentType) {
    return executeToString("ADMIN_SET_USER_STATUS", emptyMap(), map("accessKey", accessKey, "status", status), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SET_USER_STATUS`，不携带请求体。 */
  public Mono<String> setUserStatus(String accessKey, String status) {
    return setUserStatus(accessKey, status, null, null);
  }



  /** 获取 access key 信息的加密响应；当前 MinIO 服务端返回 madmin 加密载荷。 */
  public Mono<io.minio.reactive.messages.admin.EncryptedAdminResponse> getAccessKeyInfoEncrypted(
      String accessKey) {
    requireText("accessKey", accessKey);
    return executeToBytes(
            "ADMIN_INFO_ACCESS_KEY",
            emptyMap(),
            map("accessKey", accessKey),
            emptyMap(),
            null,
            null)
        .map(io.minio.reactive.messages.admin.EncryptedAdminResponse::new);
  }

  /** 兼容保留：当前 MinIO 返回加密载荷，不能在 Crypto Gate Pass 前解析成明文模型。 */
  public Mono<io.minio.reactive.messages.admin.AdminAccessKeyInfo> getAccessKeyInfoTyped(
      String accessKey) {
    requireText("accessKey", accessKey);
    return Mono.error(
        new UnsupportedOperationException(
            "ADMIN_INFO_ACCESS_KEY 返回 madmin 加密载荷；请使用 getAccessKeyInfoEncrypted(...)，等 Crypto Gate Pass 后再使用明文模型"));
  }

  /** 列出 access key 批量信息的加密响应；当前 MinIO 服务端返回 madmin 加密载荷。 */
  public Mono<io.minio.reactive.messages.admin.EncryptedAdminResponse> listAccessKeysEncrypted(
      String listType) {
    requireText("listType", listType);
    return executeToBytes(
            "ADMIN_LIST_ACCESS_KEYS_BULK",
            emptyMap(),
            map("listType", listType),
            emptyMap(),
            null,
            null)
        .map(io.minio.reactive.messages.admin.EncryptedAdminResponse::new);
  }

  /** 兼容保留：当前 MinIO 返回加密载荷，不能在 Crypto Gate Pass 前解析成明文列表模型。 */
  public Mono<io.minio.reactive.messages.admin.AdminAccessKeyList> listAccessKeysTyped(
      String listType) {
    requireText("listType", listType);
    return Mono.error(
        new UnsupportedOperationException(
            "ADMIN_LIST_ACCESS_KEYS_BULK 返回 madmin 加密载荷；请使用 listAccessKeysEncrypted(...)，等 Crypto Gate Pass 后再使用明文模型"));
  }

  /** 获取临时账号信息；该只读接口不等同于 encrypted-blocked 的 access key 批量列表。 */
  public Mono<io.minio.reactive.messages.admin.AdminAccessKeyInfo> getTemporaryAccountInfo(
      String accessKey) {
    requireText("accessKey", accessKey);
    return temporaryAccountInfo(accessKey)
        .map(io.minio.reactive.messages.admin.AdminAccessKeyInfo::parse);
  }

  /** 列出指定 LDAP 用户 DN 的 access key 只读摘要；不会保留 secret 或 token 字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminAccessKeySummaryList>
      listLdapAccessKeySummaries(String userDN, String listType) {
    requireText("userDN", userDN);
    requireText("listType", listType);
    return ldapListAccessKeys(userDN, listType)
        .map(io.minio.reactive.messages.admin.AdminAccessKeySummaryList::parse);
  }

  /** 批量列出 LDAP access key 只读摘要；不会保留 secret 或 token 字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminAccessKeySummaryList>
      listLdapAccessKeySummaries(String listType) {
    requireText("listType", listType);
    return ldapListAccessKeysBulk(listType)
        .map(io.minio.reactive.messages.admin.AdminAccessKeySummaryList::parse);
  }

  /** 批量列出 OpenID access key 只读摘要；不会保留 secret 或 token 字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminAccessKeySummaryList>
      listOpenidAccessKeySummaries(String listType) {
    requireText("listType", listType);
    return openidListAccessKeysBulk(listType)
        .map(io.minio.reactive.messages.admin.AdminAccessKeySummaryList::parse);
  }

  /** 调用 `ADMIN_ADD_SERVICE_ACCOUNT`。 */
  public Mono<String> addServiceAccount(byte[] body, String contentType) {
    return executeToString("ADMIN_ADD_SERVICE_ACCOUNT", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_ADD_SERVICE_ACCOUNT`，不携带请求体。 */
  public Mono<String> addServiceAccount() {
    return addServiceAccount(null, null);
  }

  /** 调用 `ADMIN_UPDATE_SERVICE_ACCOUNT`。 */
  public Mono<String> updateServiceAccount(String accessKey, byte[] body, String contentType) {
    return executeToString("ADMIN_UPDATE_SERVICE_ACCOUNT", emptyMap(), map("accessKey", accessKey), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_UPDATE_SERVICE_ACCOUNT`，不携带请求体。 */
  public Mono<String> updateServiceAccount(String accessKey) {
    return updateServiceAccount(accessKey, null, null);
  }

  /** 调用 `ADMIN_INFO_SERVICE_ACCOUNT`。 */
  public Mono<String> infoServiceAccount(String accessKey) {
    return executeToString("ADMIN_INFO_SERVICE_ACCOUNT", emptyMap(), map("accessKey", accessKey), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_LIST_SERVICE_ACCOUNTS`。 */
  public Mono<String> listServiceAccounts() {
    return executeToString("ADMIN_LIST_SERVICE_ACCOUNTS", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_DELETE_SERVICE_ACCOUNT`。 */
  public Mono<String> deleteServiceAccount(String accessKey, byte[] body, String contentType) {
    return executeToString("ADMIN_DELETE_SERVICE_ACCOUNT", emptyMap(), map("accessKey", accessKey), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_DELETE_SERVICE_ACCOUNT`，不携带请求体。 */
  public Mono<String> deleteServiceAccount(String accessKey) {
    return deleteServiceAccount(accessKey, null, null);
  }

  /** 调用 `ADMIN_TEMPORARY_ACCOUNT_INFO`。 */
  public Mono<String> temporaryAccountInfo(String accessKey) {
    return executeToString("ADMIN_TEMPORARY_ACCOUNT_INFO", emptyMap(), map("accessKey", accessKey), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_LIST_ACCESS_KEYS_BULK`。 */
  public Mono<String> listAccessKeysBulk(String listType) {
    return executeToString("ADMIN_LIST_ACCESS_KEYS_BULK", emptyMap(), map("listType", listType), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_INFO_ACCESS_KEY`。 */
  public Mono<String> infoAccessKey(String accessKey) {
    return executeToString("ADMIN_INFO_ACCESS_KEY", emptyMap(), map("accessKey", accessKey), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_INFO_CANNED_POLICY`。 */
  public Mono<String> infoCannedPolicy(String name) {
    return executeToString("ADMIN_INFO_CANNED_POLICY", emptyMap(), map("name", name), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_LIST_BUCKET_POLICIES`。 */
  public Mono<String> listBucketPolicies(String bucket) {
    return executeToString("ADMIN_LIST_BUCKET_POLICIES", emptyMap(), map("bucket", bucket), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_LIST_CANNED_POLICIES`。 */
  public Mono<String> listCannedPolicies() {
    return executeToString("ADMIN_LIST_CANNED_POLICIES", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_LIST_BUILTIN_POLICY_ENTITIES`。 */
  @Deprecated
  public Mono<String> listBuiltinPolicyEntities() {
    return executeToString("ADMIN_LIST_BUILTIN_POLICY_ENTITIES", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_REMOVE_CANNED_POLICY`。 */
  public Mono<String> removeCannedPolicy(String name, byte[] body, String contentType) {
    return executeToString("ADMIN_REMOVE_CANNED_POLICY", emptyMap(), map("name", name), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_REMOVE_CANNED_POLICY`，不携带请求体。 */
  public Mono<String> removeCannedPolicy(String name) {
    return removeCannedPolicy(name, null, null);
  }

  /** 调用 `ADMIN_SET_USER_OR_GROUP_POLICY`。 */
  public Mono<String> setUserOrGroupPolicy(String policyName, String userOrGroup, String isGroup, byte[] body, String contentType) {
    return executeToString("ADMIN_SET_USER_OR_GROUP_POLICY", emptyMap(), map("policyName", policyName, "userOrGroup", userOrGroup, "isGroup", isGroup), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SET_USER_OR_GROUP_POLICY`，不携带请求体。 */
  public Mono<String> setUserOrGroupPolicy(String policyName, String userOrGroup, String isGroup) {
    return setUserOrGroupPolicy(policyName, userOrGroup, isGroup, null, null);
  }

  /** 调用 `ADMIN_ATTACH_DETACH_BUILTIN_POLICY`。 */
  public Mono<String> attachDetachBuiltinPolicy(String operation, byte[] body, String contentType) {
    return executeToString("ADMIN_ATTACH_DETACH_BUILTIN_POLICY", map("operation", operation), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_ATTACH_DETACH_BUILTIN_POLICY`，不携带请求体。 */
  public Mono<String> attachDetachBuiltinPolicy(String operation) {
    return attachDetachBuiltinPolicy(operation, null, null);
  }

  /** 调用 `ADMIN_REMOVE_USER`。 */
  public Mono<String> removeUser(String accessKey, byte[] body, String contentType) {
    return executeToString("ADMIN_REMOVE_USER", emptyMap(), map("accessKey", accessKey), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_REMOVE_USER`，不携带请求体。 */
  public Mono<String> removeUser(String accessKey) {
    return removeUser(accessKey, null, null);
  }

  /** 调用 `ADMIN_LIST_BUCKET_USERS`。 */
  public Mono<String> listBucketUsers(String bucket) {
    return executeToString("ADMIN_LIST_BUCKET_USERS", emptyMap(), map("bucket", bucket), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_LIST_USERS`。 */
  public Mono<String> listUsers() {
    return executeToString("ADMIN_LIST_USERS", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_USER_INFO`。 */
  public Mono<String> userInfo(String accessKey) {
    return executeToString("ADMIN_USER_INFO", emptyMap(), map("accessKey", accessKey), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_UPDATE_GROUP_MEMBERS`。 */
  public Mono<String> updateGroupMembers(byte[] body, String contentType) {
    return executeToString("ADMIN_UPDATE_GROUP_MEMBERS", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_UPDATE_GROUP_MEMBERS`，不携带请求体。 */
  public Mono<String> updateGroupMembers() {
    return updateGroupMembers(null, null);
  }

  /** 调用 `ADMIN_GET_GROUP`。 */
  public Mono<String> getGroup(String group) {
    return executeToString("ADMIN_GET_GROUP", emptyMap(), map("group", group), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_LIST_GROUPS`。 */
  public Mono<String> listGroups() {
    return executeToString("ADMIN_LIST_GROUPS", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_SET_GROUP_STATUS`。 */
  public Mono<String> setGroupStatus(String group, String status, byte[] body, String contentType) {
    return executeToString("ADMIN_SET_GROUP_STATUS", emptyMap(), map("group", group, "status", status), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SET_GROUP_STATUS`，不携带请求体。 */
  public Mono<String> setGroupStatus(String group, String status) {
    return setGroupStatus(group, status, null, null);
  }

  /** 调用 `ADMIN_EXPORT_IAM`。 */
  public Mono<String> exportIam() {
    return executeToString("ADMIN_EXPORT_IAM", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_IMPORT_IAM`。推荐优先使用 `importIamArchive(...)`，并且只在独立 lab 或维护窗口执行。 */
  @Deprecated
  public Mono<String> importIam(byte[] body, String contentType) {
    return executeToString("ADMIN_IMPORT_IAM", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_IMPORT_IAM`，不携带请求体。推荐优先使用 `importIamArchive(...)`。 */
  @Deprecated
  public Mono<String> importIam() {
    return importIam(null, null);
  }

  /** 调用 `ADMIN_IMPORT_IAM_V2`。推荐优先使用 `importIamV2Archive(...)`，并且只在独立 lab 或维护窗口执行。 */
  @Deprecated
  public Mono<String> importIamV2(byte[] body, String contentType) {
    return executeToString("ADMIN_IMPORT_IAM_V2", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_IMPORT_IAM_V2`，不携带请求体。推荐优先使用 `importIamV2Archive(...)`。 */
  @Deprecated
  public Mono<String> importIamV2() {
    return importIamV2(null, null);
  }

  /** 调用 `ADMIN_ADD_IDP_CONFIG`。 */
  public Mono<String> addIdpConfig(String type, String name, byte[] body, String contentType) {
    return executeToString("ADMIN_ADD_IDP_CONFIG", map("type", type, "name", name), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_ADD_IDP_CONFIG`，不携带请求体。 */
  public Mono<String> addIdpConfig(String type, String name) {
    return addIdpConfig(type, name, null, null);
  }

  /** 调用 `ADMIN_UPDATE_IDP_CONFIG`。 */
  public Mono<String> updateIdpConfig(String type, String name, byte[] body, String contentType) {
    return executeToString("ADMIN_UPDATE_IDP_CONFIG", map("type", type, "name", name), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_UPDATE_IDP_CONFIG`，不携带请求体。 */
  public Mono<String> updateIdpConfig(String type, String name) {
    return updateIdpConfig(type, name, null, null);
  }

  /** 调用 `ADMIN_LIST_IDP_CONFIG`。 */
  @Deprecated
  public Mono<String> listIdpConfig(String type) {
    return executeToString("ADMIN_LIST_IDP_CONFIG", map("type", type), emptyMap(), emptyMap(), null, null);
  }

  /** 列出指定类型的 IDP 配置名称；只读摘要保留原始 JSON。 */
  public Mono<io.minio.reactive.messages.admin.AdminIdpConfigList> listIdpConfigs(String type) {
    requireText("type", type);
    return listIdpConfig(type).map(raw -> io.minio.reactive.messages.admin.AdminIdpConfigList.parse(type, raw));
  }

  /** 调用 `ADMIN_GET_IDP_CONFIG`。 */
  @Deprecated
  public Mono<String> getIdpConfig(String type, String name) {
    return executeToString("ADMIN_GET_IDP_CONFIG", map("type", type, "name", name), emptyMap(), emptyMap(), null, null);
  }

  /** 获取单个 IDP 配置的通用 JSON 包装；字段可能随 MinIO 版本变化，因此保留原文。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> getIdpConfigInfo(String type, String name) {
    requireText("type", type);
    requireText("name", name);
    return getIdpConfig(type, name).map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /** 调用 `ADMIN_DELETE_IDP_CONFIG`。 */
  public Mono<String> deleteIdpConfig(String type, String name, byte[] body, String contentType) {
    return executeToString("ADMIN_DELETE_IDP_CONFIG", map("type", type, "name", name), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_DELETE_IDP_CONFIG`，不携带请求体。 */
  public Mono<String> deleteIdpConfig(String type, String name) {
    return deleteIdpConfig(type, name, null, null);
  }

  /** 调用 `ADMIN_LDAP_ADD_SERVICE_ACCOUNT`。 */
  public Mono<String> ldapAddServiceAccount(byte[] body, String contentType) {
    return executeToString("ADMIN_LDAP_ADD_SERVICE_ACCOUNT", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_LDAP_ADD_SERVICE_ACCOUNT`，不携带请求体。 */
  public Mono<String> ldapAddServiceAccount() {
    return ldapAddServiceAccount(null, null);
  }

  /** 调用 `ADMIN_LDAP_LIST_ACCESS_KEYS`。 */
  public Mono<String> ldapListAccessKeys(String userDN, String listType) {
    return executeToString("ADMIN_LDAP_LIST_ACCESS_KEYS", emptyMap(), map("userDN", userDN, "listType", listType), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_LDAP_LIST_ACCESS_KEYS_BULK`。 */
  public Mono<String> ldapListAccessKeysBulk(String listType) {
    return executeToString("ADMIN_LDAP_LIST_ACCESS_KEYS_BULK", emptyMap(), map("listType", listType), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_LDAP_POLICY_ENTITIES`。 */
  public Mono<String> ldapPolicyEntities() {
    return executeToString("ADMIN_LDAP_POLICY_ENTITIES", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_LDAP_ATTACH_DETACH_POLICY`。 */
  public Mono<String> ldapAttachDetachPolicy(String operation, byte[] body, String contentType) {
    return executeToString("ADMIN_LDAP_ATTACH_DETACH_POLICY", map("operation", operation), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_LDAP_ATTACH_DETACH_POLICY`，不携带请求体。 */
  public Mono<String> ldapAttachDetachPolicy(String operation) {
    return ldapAttachDetachPolicy(operation, null, null);
  }

  /** 调用 `ADMIN_OPENID_LIST_ACCESS_KEYS_BULK`。 */
  public Mono<String> openidListAccessKeysBulk(String listType) {
    return executeToString("ADMIN_OPENID_LIST_ACCESS_KEYS_BULK", emptyMap(), map("listType", listType), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_GET_BUCKET_QUOTA`。 */
  public Mono<String> getBucketQuota(String bucket) {
    return executeToString("ADMIN_GET_BUCKET_QUOTA", emptyMap(), map("bucket", bucket), emptyMap(), null, null);
  }

  /** 获取 bucket quota 配置摘要；这是 L3 只读候选，默认仅做单元/mock 验证。 */
  public Mono<io.minio.reactive.messages.admin.AdminBucketQuota> getBucketQuotaInfo(
      String bucket) {
    requireText("bucket", bucket);
    return getBucketQuota(bucket).map(io.minio.reactive.messages.admin.AdminBucketQuota::parse);
  }

  /** 调用 `ADMIN_SET_BUCKET_QUOTA`。 */
  public Mono<String> setBucketQuota(String bucket, byte[] body, String contentType) {
    return executeToString("ADMIN_SET_BUCKET_QUOTA", emptyMap(), map("bucket", bucket), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SET_BUCKET_QUOTA`，不携带请求体。 */
  public Mono<String> setBucketQuota(String bucket) {
    return setBucketQuota(bucket, null, null);
  }

  /** 调用 `ADMIN_LIST_REMOTE_TARGETS`。 */
  @Deprecated
  public Mono<String> listRemoteTargets(String bucket, String type) {
    return executeToString("ADMIN_LIST_REMOTE_TARGETS", emptyMap(), map("bucket", bucket, "type", type), emptyMap(), null, null);
  }

  /** 列出 bucket remote target 只读摘要；凭据字段只保留在原始 JSON，不提供普通 getter。 */
  public Mono<io.minio.reactive.messages.admin.AdminRemoteTargetList> listRemoteTargetsInfo(
      String bucket, String type) {
    requireText("bucket", bucket);
    requireText("type", type);
    return listRemoteTargets(bucket, type)
        .map(io.minio.reactive.messages.admin.AdminRemoteTargetList::parse);
  }

  /** 获取指定 bucket 的 replication MRF 信息，使用通用 JSON 结果保留版本差异字段。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> getReplicationMrfInfo(
      String bucket) {
    requireText("bucket", bucket);
    return replicationMrf(bucket).map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /** 调用 `ADMIN_SET_REMOTE_TARGET`。 */
  public Mono<String> setRemoteTarget(String bucket, byte[] body, String contentType) {
    return executeToString("ADMIN_SET_REMOTE_TARGET", emptyMap(), map("bucket", bucket), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SET_REMOTE_TARGET`，不携带请求体。 */
  public Mono<String> setRemoteTarget(String bucket) {
    return setRemoteTarget(bucket, null, null);
  }

  /** 调用 `ADMIN_REMOVE_REMOTE_TARGET`。 */
  public Mono<String> removeRemoteTarget(String bucket, String arn, byte[] body, String contentType) {
    return executeToString("ADMIN_REMOVE_REMOTE_TARGET", emptyMap(), map("bucket", bucket, "arn", arn), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_REMOVE_REMOTE_TARGET`，不携带请求体。 */
  public Mono<String> removeRemoteTarget(String bucket, String arn) {
    return removeRemoteTarget(bucket, arn, null, null);
  }

  /** 调用 `ADMIN_REPLICATION_DIFF`。 */
  public Mono<String> replicationDiff(String bucket, byte[] body, String contentType) {
    return executeToString("ADMIN_REPLICATION_DIFF", emptyMap(), map("bucket", bucket), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_REPLICATION_DIFF`，不携带请求体。 */
  public Mono<String> replicationDiff(String bucket) {
    return replicationDiff(bucket, null, null);
  }

  /** 调用 `ADMIN_REPLICATION_MRF`。 */
  public Mono<String> replicationMrf(String bucket) {
    return executeToString("ADMIN_REPLICATION_MRF", emptyMap(), map("bucket", bucket), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_START_BATCH_JOB`。 */
  public Mono<String> startBatchJob(byte[] body, String contentType) {
    return executeToString("ADMIN_START_BATCH_JOB", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_START_BATCH_JOB`，不携带请求体。 */
  public Mono<String> startBatchJob() {
    return startBatchJob(null, null);
  }

  /** 调用 `ADMIN_LIST_BATCH_JOBS`。 */
  @Deprecated
  public Mono<String> listBatchJobs() {
    return executeToString("ADMIN_LIST_BATCH_JOBS", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 列出 batch job 摘要。 */
  public Mono<io.minio.reactive.messages.admin.AdminBatchJobList> listBatchJobsInfo() {
    return listBatchJobs().map(io.minio.reactive.messages.admin.AdminBatchJobList::parse);
  }

  /** 调用 `ADMIN_BATCH_JOB_STATUS`。 */
  @Deprecated
  public Mono<String> batchJobStatus() {
    return executeToString("ADMIN_BATCH_JOB_STATUS", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 获取 batch job 状态通用 JSON 包装。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> getBatchJobStatusInfo() {
    return batchJobStatus().map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /** 调用 `ADMIN_DESCRIBE_BATCH_JOB`。 */
  @Deprecated
  public Mono<String> describeBatchJob() {
    return executeToString("ADMIN_DESCRIBE_BATCH_JOB", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 获取 batch job 详情通用 JSON 包装。 */
  public Mono<io.minio.reactive.messages.admin.AdminJsonResult> describeBatchJobInfo() {
    return describeBatchJob().map(io.minio.reactive.messages.admin.AdminJsonResult::parse);
  }

  /** 调用 `ADMIN_CANCEL_BATCH_JOB`。 */
  public Mono<String> cancelBatchJob(byte[] body, String contentType) {
    return executeToString("ADMIN_CANCEL_BATCH_JOB", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_CANCEL_BATCH_JOB`，不携带请求体。 */
  public Mono<String> cancelBatchJob() {
    return cancelBatchJob(null, null);
  }

  /** 调用 `ADMIN_EXPORT_BUCKET_METADATA`。 */
  public Mono<String> exportBucketMetadata() {
    return executeToString("ADMIN_EXPORT_BUCKET_METADATA", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_IMPORT_BUCKET_METADATA`。推荐优先使用 `importBucketMetadataArchive(...)`，并且只在独立 lab 或维护窗口执行。 */
  @Deprecated
  public Mono<String> importBucketMetadata(byte[] body, String contentType) {
    return executeToString("ADMIN_IMPORT_BUCKET_METADATA", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_IMPORT_BUCKET_METADATA`，不携带请求体。推荐优先使用 `importBucketMetadataArchive(...)`。 */
  @Deprecated
  public Mono<String> importBucketMetadata() {
    return importBucketMetadata(null, null);
  }

  /** 调用 `ADMIN_ADD_TIER`。 */
  public Mono<String> addTier(byte[] body, String contentType) {
    return executeToString("ADMIN_ADD_TIER", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_ADD_TIER`，不携带请求体。 */
  public Mono<String> addTier() {
    return addTier(null, null);
  }

  /** 调用 `ADMIN_EDIT_TIER`。 */
  public Mono<String> editTier(String tier, byte[] body, String contentType) {
    return executeToString("ADMIN_EDIT_TIER", map("tier", tier), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_EDIT_TIER`，不携带请求体。 */
  public Mono<String> editTier(String tier) {
    return editTier(tier, null, null);
  }

  /** 调用 `ADMIN_LIST_TIER`。 */
  public Mono<String> listTier() {
    return executeToString("ADMIN_LIST_TIER", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 列出远端 tier 配置摘要；凭据字段仍只保留在 MinIO 已脱敏的原始 JSON 中。 */
  public Mono<io.minio.reactive.messages.admin.AdminTierList> listTiers() {
    return listTier().map(io.minio.reactive.messages.admin.AdminTierList::parse);
  }

  /** 校验指定 tier 连接状态；该接口可能访问外部存储，只包装响应文本。 */
  public Mono<io.minio.reactive.messages.admin.AdminTextResult> verifyTierInfo(String tier) {
    requireText("tier", tier);
    return verifyTier(tier)
        .map(text -> io.minio.reactive.messages.admin.AdminTextResult.of("tier-verify", text));
  }

  /** 调用 `ADMIN_REMOVE_TIER`。 */
  public Mono<String> removeTier(String tier, byte[] body, String contentType) {
    return executeToString("ADMIN_REMOVE_TIER", map("tier", tier), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_REMOVE_TIER`，不携带请求体。 */
  public Mono<String> removeTier(String tier) {
    return removeTier(tier, null, null);
  }

  /** 调用 `ADMIN_VERIFY_TIER`。 */
  public Mono<String> verifyTier(String tier) {
    return executeToString("ADMIN_VERIFY_TIER", map("tier", tier), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_TIER_STATS`。 */
  public Mono<String> tierStats() {
    return executeToString("ADMIN_TIER_STATS", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_SITE_REPLICATION_ADD`。 */
  public Mono<String> siteReplicationAdd(byte[] body, String contentType) {
    return executeToString(
        "ADMIN_SITE_REPLICATION_ADD", emptyMap(), siteReplicationApiVersion(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SITE_REPLICATION_ADD`，不携带请求体。 */
  public Mono<String> siteReplicationAdd() {
    return siteReplicationAdd(null, null);
  }

  /** 调用 `ADMIN_SITE_REPLICATION_REMOVE`。 */
  public Mono<String> siteReplicationRemove(byte[] body, String contentType) {
    return executeToString(
        "ADMIN_SITE_REPLICATION_REMOVE", emptyMap(), siteReplicationApiVersion(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SITE_REPLICATION_REMOVE`，不携带请求体。 */
  public Mono<String> siteReplicationRemove() {
    return siteReplicationRemove(null, null);
  }

  /** 调用 `ADMIN_SITE_REPLICATION_INFO`。 */
  public Mono<String> siteReplicationInfo() {
    return executeToString(
        "ADMIN_SITE_REPLICATION_INFO", emptyMap(), siteReplicationApiVersion(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_SITE_REPLICATION_METAINFO`。 */
  public Mono<String> siteReplicationMetainfo() {
    return executeToString(
        "ADMIN_SITE_REPLICATION_METAINFO", emptyMap(), siteReplicationApiVersion(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_SITE_REPLICATION_STATUS`。 */
  public Mono<String> siteReplicationStatus() {
    return executeToString(
        "ADMIN_SITE_REPLICATION_STATUS", emptyMap(), siteReplicationApiVersion(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_SITE_REPLICATION_DEVNULL`。 */
  public Mono<String> siteReplicationDevnull(byte[] body, String contentType) {
    return executeToString("ADMIN_SITE_REPLICATION_DEVNULL", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SITE_REPLICATION_DEVNULL`，不携带请求体。 */
  public Mono<String> siteReplicationDevnull() {
    return siteReplicationDevnull(null, null);
  }

  /** 调用 `ADMIN_SITE_REPLICATION_NETPERF`。 */
  public Mono<String> siteReplicationNetperf(byte[] body, String contentType) {
    return executeToString("ADMIN_SITE_REPLICATION_NETPERF", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SITE_REPLICATION_NETPERF`，不携带请求体。 */
  public Mono<String> siteReplicationNetperf() {
    return siteReplicationNetperf(null, null);
  }

  /** 调用 `ADMIN_SR_PEER_JOIN`。 */
  public Mono<String> srPeerJoin(byte[] body, String contentType) {
    return executeToString(
        "ADMIN_SR_PEER_JOIN", emptyMap(), siteReplicationApiVersion(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SR_PEER_JOIN`，不携带请求体。 */
  public Mono<String> srPeerJoin() {
    return srPeerJoin(null, null);
  }

  /** 调用 `ADMIN_SR_PEER_BUCKET_OPS`。 */
  public Mono<String> srPeerBucketOps(String bucket, String operation, byte[] body, String contentType) {
    return executeToString(
        "ADMIN_SR_PEER_BUCKET_OPS",
        emptyMap(),
        map("bucket", bucket, "operation", operation, "api-version", "1"),
        emptyMap(),
        body,
        contentType);
  }

  /** 调用 `ADMIN_SR_PEER_BUCKET_OPS`，不携带请求体。 */
  public Mono<String> srPeerBucketOps(String bucket, String operation) {
    return srPeerBucketOps(bucket, operation, null, null);
  }

  /** 调用 `ADMIN_SR_PEER_IAM_ITEM`。 */
  public Mono<String> srPeerIamItem(byte[] body, String contentType) {
    return executeToString(
        "ADMIN_SR_PEER_IAM_ITEM", emptyMap(), siteReplicationApiVersion(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SR_PEER_IAM_ITEM`，不携带请求体。 */
  public Mono<String> srPeerIamItem() {
    return srPeerIamItem(null, null);
  }

  /** 调用 `ADMIN_SR_PEER_BUCKET_META`。 */
  public Mono<String> srPeerBucketMeta(byte[] body, String contentType) {
    return executeToString(
        "ADMIN_SR_PEER_BUCKET_META", emptyMap(), siteReplicationApiVersion(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SR_PEER_BUCKET_META`，不携带请求体。 */
  public Mono<String> srPeerBucketMeta() {
    return srPeerBucketMeta(null, null);
  }

  /** 调用 `ADMIN_SR_PEER_IDP_SETTINGS`。 */
  public Mono<String> srPeerIdpSettings() {
    return executeToString(
        "ADMIN_SR_PEER_IDP_SETTINGS", emptyMap(), siteReplicationApiVersion(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_SITE_REPLICATION_EDIT`。 */
  public Mono<String> siteReplicationEdit(byte[] body, String contentType) {
    return executeToString(
        "ADMIN_SITE_REPLICATION_EDIT", emptyMap(), siteReplicationApiVersion(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SITE_REPLICATION_EDIT`，不携带请求体。 */
  public Mono<String> siteReplicationEdit() {
    return siteReplicationEdit(null, null);
  }

  /** 调用 `ADMIN_SR_PEER_EDIT`。 */
  public Mono<String> srPeerEdit(byte[] body, String contentType) {
    return executeToString(
        "ADMIN_SR_PEER_EDIT", emptyMap(), siteReplicationApiVersion(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SR_PEER_EDIT`，不携带请求体。 */
  public Mono<String> srPeerEdit() {
    return srPeerEdit(null, null);
  }

  /** 调用 `ADMIN_SR_PEER_REMOVE`。 */
  public Mono<String> srPeerRemove(byte[] body, String contentType) {
    return executeToString(
        "ADMIN_SR_PEER_REMOVE", emptyMap(), siteReplicationApiVersion(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SR_PEER_REMOVE`，不携带请求体。 */
  public Mono<String> srPeerRemove() {
    return srPeerRemove(null, null);
  }

  /** 调用 `ADMIN_SITE_REPLICATION_RESYNC_OP`。 */
  public Mono<String> siteReplicationResyncOp(String operation, byte[] body, String contentType) {
    return executeToString(
        "ADMIN_SITE_REPLICATION_RESYNC_OP",
        emptyMap(),
        map("operation", operation, "api-version", "1"),
        emptyMap(),
        body,
        contentType);
  }

  /** 调用 `ADMIN_SITE_REPLICATION_RESYNC_OP`，不携带请求体。 */
  public Mono<String> siteReplicationResyncOp(String operation) {
    return siteReplicationResyncOp(operation, null, null);
  }

  /** 调用 `ADMIN_SR_STATE_EDIT`。 */
  public Mono<String> srStateEdit(byte[] body, String contentType) {
    return executeToString(
        "ADMIN_SR_STATE_EDIT", emptyMap(), siteReplicationApiVersion(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SR_STATE_EDIT`，不携带请求体。 */
  public Mono<String> srStateEdit() {
    return srStateEdit(null, null);
  }

  /** 调用 `ADMIN_TOP_LOCKS`。 */
  public Mono<String> topLocks() {
    return executeToString("ADMIN_TOP_LOCKS", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_FORCE_UNLOCK`。 */
  public Mono<String> forceUnlock(String paths, byte[] body, String contentType) {
    return executeToString("ADMIN_FORCE_UNLOCK", emptyMap(), map("paths", paths), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_FORCE_UNLOCK`，不携带请求体。 */
  public Mono<String> forceUnlock(String paths) {
    return forceUnlock(paths, null, null);
  }

  /** 调用 `ADMIN_SPEEDTEST`。 */
  public Mono<String> speedtest(byte[] body, String contentType) {
    return executeToString("ADMIN_SPEEDTEST", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SPEEDTEST`，不携带请求体。 */
  public Mono<String> speedtest() {
    return speedtest(null, null);
  }

  /** 调用 `ADMIN_SPEEDTEST_OBJECT`。 */
  public Mono<String> speedtestObject(byte[] body, String contentType) {
    return executeToString("ADMIN_SPEEDTEST_OBJECT", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SPEEDTEST_OBJECT`，不携带请求体。 */
  public Mono<String> speedtestObject() {
    return speedtestObject(null, null);
  }

  /** 调用 `ADMIN_SPEEDTEST_DRIVE`。 */
  public Mono<String> speedtestDrive(byte[] body, String contentType) {
    return executeToString("ADMIN_SPEEDTEST_DRIVE", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SPEEDTEST_DRIVE`，不携带请求体。 */
  public Mono<String> speedtestDrive() {
    return speedtestDrive(null, null);
  }

  /** 调用 `ADMIN_SPEEDTEST_NET`。 */
  public Mono<String> speedtestNet(byte[] body, String contentType) {
    return executeToString("ADMIN_SPEEDTEST_NET", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SPEEDTEST_NET`，不携带请求体。 */
  public Mono<String> speedtestNet() {
    return speedtestNet(null, null);
  }

  /** 调用 `ADMIN_SPEEDTEST_SITE`。 */
  public Mono<String> speedtestSite(byte[] body, String contentType) {
    return executeToString("ADMIN_SPEEDTEST_SITE", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_SPEEDTEST_SITE`，不携带请求体。 */
  public Mono<String> speedtestSite() {
    return speedtestSite(null, null);
  }

  /** 调用 `ADMIN_CLIENT_DEVNULL`。 */
  public Mono<String> clientDevnull(byte[] body, String contentType) {
    return executeToString("ADMIN_CLIENT_DEVNULL", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_CLIENT_DEVNULL`，不携带请求体。 */
  public Mono<String> clientDevnull() {
    return clientDevnull(null, null);
  }

  /** 调用 `ADMIN_CLIENT_DEVNULL_EXTRA_TIME`。 */
  public Mono<String> clientDevnullExtraTime(byte[] body, String contentType) {
    return executeToString("ADMIN_CLIENT_DEVNULL_EXTRA_TIME", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_CLIENT_DEVNULL_EXTRA_TIME`，不携带请求体。 */
  public Mono<String> clientDevnullExtraTime() {
    return clientDevnullExtraTime(null, null);
  }

  /** 调用 `ADMIN_TRACE`。 */
  public Mono<String> trace() {
    return executeToString("ADMIN_TRACE", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_LOG`。 */
  public Mono<String> log() {
    return executeToString("ADMIN_LOG", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_KMS_STATUS`。推荐优先使用 `getAdminKmsStatus()` 或 `ReactiveMinioKmsClient#getStatus()`。 */
  @Deprecated
  public Mono<String> kmsStatus(byte[] body, String contentType) {
    return executeToString("ADMIN_KMS_STATUS", emptyMap(), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_KMS_STATUS`，不携带请求体。推荐优先使用 `getAdminKmsStatus()`。 */
  @Deprecated
  public Mono<String> kmsStatus() {
    return kmsStatus(null, null);
  }

  /** 调用 `ADMIN_KMS_KEY_CREATE`。推荐优先使用 `createAdminKmsKey(...)` 或 `ReactiveMinioKmsClient#createKey(...)`。 */
  @Deprecated
  public Mono<String> kmsKeyCreate(String keyId, byte[] body, String contentType) {
    return executeToString("ADMIN_KMS_KEY_CREATE", emptyMap(), map("key-id", keyId), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_KMS_KEY_CREATE`，不携带请求体。推荐优先使用 `createAdminKmsKey(...)`。 */
  @Deprecated
  public Mono<String> kmsKeyCreate(String keyId) {
    return kmsKeyCreate(keyId, null, null);
  }

  /** 调用 `ADMIN_KMS_KEY_STATUS`。推荐优先使用 `getAdminKmsKeyStatus()` 或 `ReactiveMinioKmsClient#getKeyStatus()`。 */
  @Deprecated
  public Mono<String> kmsKeyStatus() {
    return executeToString("ADMIN_KMS_KEY_STATUS", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_OBD_INFO`。 */
  public Mono<String> obdInfo() {
    return executeToString("ADMIN_OBD_INFO", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_HEALTH_INFO`。 */
  public Mono<String> healthInfo() {
    return executeToString("ADMIN_HEALTH_INFO", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `ADMIN_REVOKE_TOKENS`。 */
  public Mono<String> revokeTokens(String userProvider, byte[] body, String contentType) {
    return executeToString("ADMIN_REVOKE_TOKENS", map("userProvider", userProvider), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `ADMIN_REVOKE_TOKENS`，不携带请求体。 */
  public Mono<String> revokeTokens(String userProvider) {
    return revokeTokens(userProvider, null, null);
  }


  /**
   * MinIO madmin-go 的 site replication 客户端会固定携带 api-version=1。
   *
   * <p>catalog 仍只描述服务端 router 上可见的 path/query 语义，专用客户端在这里补齐 madmin 协议版本，
   * 避免影响 route parity 报告。
   */
  private static java.util.Map<String, String> siteReplicationApiVersion() {
    return map("api-version", "1");
  }

  /** 校验导入备份包，避免误调用空恢复请求。 */
  private static void requireBytes(String name, byte[] value) {
    if (value == null || value.length == 0) {
      throw new IllegalArgumentException(name + " 不能为空");
    }
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

    public ReactiveMinioAdminClient build() {
      ReactiveMinioClientConfig config = ReactiveMinioClientConfig.of(endpoint, region);
      WebClient actualWebClient =
          webClient != null ? webClient : WebClient.builder().baseUrl(config.endpoint()).build();
      ReactiveCredentialsProvider actualProvider =
          credentialsProvider != null ? credentialsProvider : ReactiveCredentialsProvider.anonymous();
      ReactiveMinioEndpointExecutor executor =
          new ReactiveMinioEndpointExecutor(
              config,
              actualProvider,
              new ReactiveHttpClient(actualWebClient, config),
              new S3RequestSigner());
      return new ReactiveMinioAdminClient(executor);
    }
  }
}
