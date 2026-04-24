#!/usr/bin/env python3
import argparse
import json
import re
from pathlib import Path

FAMILY_FILES = {
    's3': 'src/main/java/io/minio/reactive/ReactiveMinioClient.java',
    'admin': 'src/main/java/io/minio/reactive/ReactiveMinioAdminClient.java',
    'kms': 'src/main/java/io/minio/reactive/ReactiveMinioKmsClient.java',
    'sts': 'src/main/java/io/minio/reactive/ReactiveMinioStsClient.java',
    'metrics': 'src/main/java/io/minio/reactive/ReactiveMinioMetricsClient.java',
    'health': 'src/main/java/io/minio/reactive/ReactiveMinioHealthClient.java',
}

TYPED_METHODS = {
    's3': {
        'listBuckets','bucketExists','makeBucket','removeBucket','getBucketLocation',
        'listObjects','listObjectsPage','getObject','getObjectRange','getObjectAsBytes','getObjectAsString',
        'putObject','copyObject','removeObject','removeObjects','statObject','getObjectTags','setObjectTags','deleteObjectTags',
        'getBucketTags','setBucketTags','deleteBucketTags','getBucketPolicy','setBucketPolicy','deleteBucketPolicy',
        'getBucketLifecycle','setBucketLifecycle','deleteBucketLifecycle','getBucketVersioning','setBucketVersioning',
        'getBucketVersioningConfiguration','setBucketVersioningConfiguration','setBucketVersioningEnabled',
        'getBucketNotification','setBucketNotification','getBucketEncryption','setBucketEncryption','deleteBucketEncryption',
        'getBucketObjectLockConfiguration','setBucketObjectLockConfiguration','getBucketReplication','setBucketReplication','deleteBucketReplication',
        'getPresignedObjectUrl','getPresignedGetObjectUrl','listObjectVersions','listObjectVersionsPage',
        'createMultipartUpload','listMultipartUploads','listMultipartUploadsPage','uploadPart','listParts',
        'completeMultipartUpload','abortMultipartUpload','uploadMultipartObject',
        'getObjectAttributes','getObjectRetention','setObjectRetention',
        'getObjectLegalHold','setObjectLegalHold','restoreObject',
        'getBucketCorsConfiguration','setBucketCorsConfiguration','deleteBucketCorsConfiguration',
        'getBucketWebsiteConfiguration','deleteBucketWebsiteConfiguration',
        'getBucketLoggingConfiguration','getBucketPolicyStatus',
        'getBucketAccelerateConfiguration','getBucketRequestPaymentConfiguration',
        'getObjectAcl','setObjectCannedAcl','getBucketAcl','setBucketCannedAcl',
        'selectObjectContent','getBucketNotificationConfiguration',
        'setBucketNotificationConfiguration','getBucketReplicationMetrics',
        'getBucketReplicationMetricsV2','listenBucketNotification','listenRootNotification'
    },
    'admin': {
        'addUser','setConfigKvText','setConfigText','getServerInfo','getStorageInfo','getDataUsageInfo','getAccountInfo',
        'getStorageSummary','getDataUsageSummary','getAccountSummary','getConfigHelp',
        'getBackgroundHealStatus','listPoolsInfo','getPoolStatus','getRebalanceStatus',
        'getTierStats','getSiteReplicationInfo','getSiteReplicationStatus',
        'getTopLocksInfo','getObdInfo','getHealthInfo',
        'getUserInfo','deleteUser','setUserEnabled','listPolicies','getPolicy','getPolicyV2','putPolicy','deletePolicy',
        'setUserPolicy','setGroupPolicy','listUsersEncrypted','listGroupsTyped','getGroupInfo','setGroupEnabled','updateGroupMembers',
        'createServiceAccount','getServiceAccountInfoEncrypted','listServiceAccountsEncrypted','deleteServiceAccountTyped','addServiceAccount',
        'getBucketQuotaInfo','listTiers','listPolicyEntities','listIdpConfigs','getIdpConfigInfo',
        'listRemoteTargetsInfo','listBatchJobsInfo','getBatchJobStatusInfo','describeBatchJobInfo',
        'getSiteReplicationMetainfo','traceStream','logStream'
    },
    'kms': {'getStatus','getApis','getVersion','listKeys','createKey','getKeyStatus','scrapeMetrics'},
    'sts': {
        'assumeRoleCredentials','assumeRoleWithWebIdentityCredentials',
        'assumeRoleWithClientGrantsCredentials','assumeRoleWithLdapCredentials',
        'assumeRoleSsoCredentials','assumeRoleWithCertificateCredentials',
        'assumeRoleWithCustomTokenCredentials'
    },
    'metrics': {'scrapeClusterMetrics','scrapeNodeMetrics','scrapeBucketMetrics','scrapeResourceMetrics','scrapeV3','scrapeLegacyMetrics'},
    'health': {'checkLiveness','isLive','checkReadiness','isReady','checkCluster','checkClusterRead','clusterGet','clusterHead','clusterReadGet','clusterReadHead','liveGet','liveHead','readyGet','readyHead'},
}

ENCRYPTED_BLOCKED = {
    'admin': {
        'ADMIN_LIST_USERS','ADMIN_ADD_SERVICE_ACCOUNT','ADMIN_INFO_SERVICE_ACCOUNT','ADMIN_LIST_SERVICE_ACCOUNTS',
        'ADMIN_GET_CONFIG','ADMIN_GET_CONFIG_KV','ADMIN_LIST_CONFIG_HISTORY_KV',
        'ADMIN_INFO_ACCESS_KEY','ADMIN_LIST_ACCESS_KEYS_BULK'
    }
}

DESTRUCTIVE_BLOCKED = {
    'admin': {
        'ADMIN_SET_CONFIG_KV','ADMIN_SET_CONFIG','ADMIN_ADD_IDP_CONFIG','ADMIN_UPDATE_IDP_CONFIG','ADMIN_DELETE_IDP_CONFIG',
        'ADMIN_SET_BUCKET_QUOTA','ADMIN_SET_REMOTE_TARGET','ADMIN_REMOVE_REMOTE_TARGET','ADMIN_REPLICATION_DIFF',
        'ADMIN_START_BATCH_JOB','ADMIN_CANCEL_BATCH_JOB','ADMIN_ADD_TIER','ADMIN_EDIT_TIER','ADMIN_REMOVE_TIER',
        'ADMIN_SITE_REPLICATION_ADD','ADMIN_SITE_REPLICATION_REMOVE','ADMIN_SITE_REPLICATION_EDIT','ADMIN_SR_PEER_EDIT',
        'ADMIN_SR_PEER_REMOVE','ADMIN_SERVICE','ADMIN_SERVICE_V2','ADMIN_SERVER_UPDATE','ADMIN_SERVER_UPDATE_V2','ADMIN_FORCE_UNLOCK',
        'ADMIN_SPEEDTEST','ADMIN_SPEEDTEST_OBJECT','ADMIN_SPEEDTEST_DRIVE','ADMIN_SPEEDTEST_NET','ADMIN_SPEEDTEST_SITE'
    }
}


def parse_catalog(java_file: Path):
    text = java_file.read_text(encoding='utf-8')
    families = {}
    endpoints = {}
    for name, family in re.findall(r'endpoints\.add\((?:e|eAuth)\("([A-Z0-9_]+)",\s*"([a-z0-9]+)"', text):
        families[family] = families.get(family, 0) + 1
        endpoints.setdefault(family, set()).add(name)
    return families, endpoints


def public_methods(java_file: Path):
    text = java_file.read_text(encoding='utf-8')
    methods = []
    for ret, name in re.findall(r'public\s+([A-Za-z0-9_<>\.? ,\[\]]+)\s+([a-zA-Z0-9_]+)\s*\(', text):
        methods.append((ret.strip(), name))
    return methods, text


def advanced_count(family, methods, text):
    if family == 's3':
        return len({name for _, name in methods if name.startswith('s3')})
    if family == 'admin':
        return len({name for ret, name in methods if 'Mono<String>' in ret})
    if family in ('kms','sts','metrics'):
        return len({name for ret, name in methods if 'Mono<String>' in ret})
    return 0


def typed_count(family, methods, catalog_count):
    names = {name for _, name in methods}
    # 这里统计的是产品化能力覆盖，不是公开方法数量；健康检查同时有 GET/HEAD 和业务便捷方法，按 route-catalog 上限收敛。
    return min(len(names & TYPED_METHODS.get(family, set())), catalog_count)


def raw_fallback_count(route_catalog, adv, typed):
    # advanced 与 typed 不是互斥集合；兜底缺口按两者中覆盖更广的一侧扣减，避免把已覆盖的 Health HEAD 误报成 raw fallback 缺口。
    return max(route_catalog - max(adv, typed), 0)


def build_matrix(worktree: Path):
    catalog_file = worktree / 'src/main/java/io/minio/reactive/catalog/MinioApiCatalog.java'
    catalog_counts, endpoint_names = parse_catalog(catalog_file)
    rows = []
    for family, rel in FAMILY_FILES.items():
        methods, text = public_methods(worktree / rel)
        route_catalog = catalog_counts.get(family, 0)
        adv = advanced_count(family, methods, text)
        typed = typed_count(family, methods, route_catalog)
        rows.append({
            'family': family,
            'route-catalog': route_catalog,
            'product-typed': typed,
            'advanced-compatible': adv,
            'raw-fallback': raw_fallback_count(route_catalog, adv, typed),
            'encrypted-blocked': len(ENCRYPTED_BLOCKED.get(family, set())),
            'destructive-blocked': len(DESTRUCTIVE_BLOCKED.get(family, set())),
        })
    return {'worktree': str(worktree), 'rows': rows}


def render_markdown(matrices):
    parts = ["<!-- counts distinguish route-catalog, product-typed, advanced-compatible, raw-fallback, and blocked-risk gates. -->", ""]
    for matrix in matrices:
        parts.append(f"## {matrix['worktree']}")
        parts.append('| family | route-catalog | product-typed | advanced-compatible | raw-fallback | encrypted-blocked | destructive-blocked |')
        parts.append('| --- | ---: | ---: | ---: | ---: | ---: | ---: |')
        for row in matrix['rows']:
            parts.append(f"| {row['family']} | {row['route-catalog']} | {row['product-typed']} | {row['advanced-compatible']} | {row['raw-fallback']} | {row['encrypted-blocked']} | {row['destructive-blocked']} |")
        parts.append('')
    return '\n'.join(parts).strip() + '\n'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--worktree', action='append', required=True)
    parser.add_argument('--format', choices=['markdown','json'], required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()

    matrices = [build_matrix(Path(w)) for w in args.worktree]
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    if args.format == 'json':
        out.write_text(json.dumps(matrices, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    else:
        out.write_text(render_markdown(matrices), encoding='utf-8')

if __name__ == '__main__':
    main()
