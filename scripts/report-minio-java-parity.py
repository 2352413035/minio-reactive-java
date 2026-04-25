#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 minio-java 与当前响应式 SDK 的公开 API 对标报告。

脚本只做静态源码扫描，不编译、不联网、不读取凭证。它的目的不是证明协议可用，
而是把“Java SDK 使用体验”与“服务端路由覆盖”分开统计，避免再次混淆基准。
"""

from __future__ import print_function

import argparse
import json
import os
import re
from pathlib import Path

# 这些是客户端构造、运行时配置或迭代器辅助方法，不属于用户业务 API 覆盖口径。
OBJECT_METHOD_EXCLUDES = set(
    [
        "baseUrl",
        "build",
        "builder",
        "close",
        "closeableIterator",
        "credentials",
        "credentialsProvider",
        "disableDualStackEndpoint",
        "disableVirtualStyleEndpoint",
        "enableDualStackEndpoint",
        "enableVirtualStyleEndpoint",
        "endpoint",
        "hasNext",
        "httpClient",
        "ignoreCertCheck",
        "iterator",
        "next",
        "populate",
        "region",
        "remove",
        "setAppInfo",
        "setAwsS3Prefix",
        "setTimeout",
        "throwMinioException",
        "traceOff",
        "traceOn",
    ]
)

ADMIN_METHOD_EXCLUDES = set(
    [
        "build",
        "builder",
        "credentials",
        "credentialsProvider",
        "endpoint",
        "httpClient",
        "ignoreCertCheck",
        "region",
        "setAppInfo",
        "setTimeout",
        "toString",
        "traceOff",
        "traceOn",
    ]
)

# 功能已接近但还没有采用 minio-java 同名入口时，列为 alias-or-partial。
OBJECT_ALIASES = {
    "deleteBucketCors": ["deleteBucketCorsConfiguration", "s3DeleteBucketCors"],
    "getBucketCors": ["getBucketCorsConfiguration", "s3GetBucketCors"],
    "setBucketCors": ["setBucketCorsConfiguration", "s3PutBucketCors"],
    "deleteBucketNotification": ["s3PutBucketNotification"],
    "deleteObjectLockConfiguration": ["s3PutBucketObjectLock"],
    "disableObjectLegalHold": ["setObjectLegalHold", "s3PutObjectLegalHold"],
    "enableObjectLegalHold": ["setObjectLegalHold", "s3PutObjectLegalHold"],
    "getObjectLockConfiguration": ["getBucketObjectLockConfiguration", "s3GetBucketObjectLock"],
    "isObjectLegalHoldEnabled": ["getObjectLegalHold", "s3GetObjectLegalHold"],
    "setObjectLockConfiguration": ["setBucketObjectLockConfiguration", "s3PutBucketObjectLock"],
}

ADMIN_ALIASES = {
    "addUpdateGroup": ["updateGroupMembers"],
    "attachPolicy": ["attachBuiltinPolicy", "attachLdapPolicy", "attachDetachBuiltinPolicy"],
    "clearBucketQuota": ["setBucketQuota", "setBucketQuotaConfig"],
    "detachPolicy": ["detachBuiltinPolicy", "detachLdapPolicy", "attachDetachBuiltinPolicy"],
    "getServiceAccountInfo": ["getServiceAccountInfoEncrypted", "infoServiceAccount"],
    "listServiceAccount": ["listServiceAccounts", "listServiceAccountsEncrypted"],
    "removeGroup": ["updateGroupMembers"],
    "setPolicy": ["setUserPolicy", "setGroupPolicy", "setUserOrGroupPolicy"],
}


def read_text(path):
    try:
        return Path(path).read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return Path(path).read_text(errors="ignore")


def public_methods(java_file):
    if not Path(java_file).is_file():
        return set()
    text = read_text(java_file)
    text = re.sub(r"/\*.*?\*/", " ", text, flags=re.S)
    text = re.sub(r"//.*", " ", text)
    pattern = re.compile(
        r"(?m)^\s*public\s+"
        r"(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?"
        r"(?:<[^;{}()]+>\s+)?"
        r"(?:[\w\[\]<>?,.]+\s+)+"
        r"(?P<name>[A-Za-z_]\w*)\s*\("
    )
    return set(m.group("name") for m in pattern.finditer(text))


def class_names(root, relative_dir, suffix=".java"):
    base = Path(root) / relative_dir
    if not base.is_dir():
        return []
    return sorted(p.stem for p in base.glob("*" + suffix))


def args_names(root):
    return sorted(p.stem for p in (Path(root) / "api/src/main/java/io/minio").glob("*Args.java"))


def classify(reference, actual, aliases):
    exact = []
    alias_or_partial = []
    missing = []
    for name in sorted(reference):
        if name in actual:
            exact.append(name)
            continue
        matched_aliases = [alias for alias in aliases.get(name, []) if alias in actual]
        if matched_aliases:
            alias_or_partial.append({"name": name, "matchedBy": matched_aliases})
        else:
            missing.append(name)
    return {"exact": exact, "aliasOrPartial": alias_or_partial, "missing": missing}


def build_report(minio_java_root, worktree):
    minio_java_root = Path(minio_java_root)
    worktree = Path(worktree)

    minio_object_methods = (
        public_methods(minio_java_root / "api/src/main/java/io/minio/MinioClient.java")
        | public_methods(minio_java_root / "api/src/main/java/io/minio/MinioAsyncClient.java")
    ) - OBJECT_METHOD_EXCLUDES
    our_object_methods = public_methods(worktree / "src/main/java/io/minio/reactive/ReactiveMinioClient.java")

    minio_admin_methods = public_methods(
        minio_java_root / "adminapi/src/main/java/io/minio/admin/MinioAdminClient.java"
    ) - ADMIN_METHOD_EXCLUDES
    our_admin_methods = public_methods(worktree / "src/main/java/io/minio/reactive/ReactiveMinioAdminClient.java")

    minio_credentials = class_names(minio_java_root, "api/src/main/java/io/minio/credentials")
    our_credentials = class_names(worktree, "src/main/java/io/minio/reactive/credentials")

    minio_args = args_names(minio_java_root)
    our_args = class_names(worktree, "src/main/java/io/minio/reactive")
    our_message_args = class_names(worktree, "src/main/java/io/minio/reactive/messages")
    all_our_args = sorted(set([name for name in our_args + our_message_args if name.endswith("Args")]))

    return {
        "minioJavaRoot": str(minio_java_root),
        "worktree": str(worktree),
        "objectApi": classify(minio_object_methods, our_object_methods, OBJECT_ALIASES),
        "adminApi": classify(minio_admin_methods, our_admin_methods, ADMIN_ALIASES),
        "credentials": {
            "minioJava": minio_credentials,
            "reactive": our_credentials,
            "missingByName": [name for name in minio_credentials if name not in our_credentials],
        },
        "args": {
            "minioJavaCount": len(minio_args),
            "reactiveCount": len(all_our_args),
            "minioJava": minio_args,
            "reactive": all_our_args,
        },
    }


def render_list(values):
    if not values:
        return "无"
    return "、".join("`%s`" % value for value in values)


def render_aliases(items):
    if not items:
        return "无"
    lines = []
    for item in items:
        lines.append("- `%s`：当前由 %s 覆盖或部分覆盖" % (item["name"], render_list(item["matchedBy"])))
    return "\n".join(lines)


def markdown(report):
    object_api = report["objectApi"]
    admin_api = report["adminApi"]
    lines = []
    lines.append("# minio-java 对标报告")
    lines.append("")
    lines.append("- minio-java：`%s`" % report["minioJavaRoot"])
    lines.append("- worktree：`%s`" % report["worktree"])
    lines.append("")
    lines.append("## 对象存储 API")
    lines.append("")
    lines.append("| 口径 | 数量 |")
    lines.append("| --- | ---: |")
    total_object = len(object_api["exact"]) + len(object_api["aliasOrPartial"]) + len(object_api["missing"])
    lines.append("| minio-java 核心对象 API | %d |" % total_object)
    lines.append("| 精确同名 | %d |" % len(object_api["exact"]))
    lines.append("| 别名或部分覆盖 | %d |" % len(object_api["aliasOrPartial"]))
    lines.append("| 缺失 | %d |" % len(object_api["missing"]))
    lines.append("")
    lines.append("### 对象存储缺失")
    lines.append("")
    lines.append(render_list(object_api["missing"]))
    lines.append("")
    lines.append("### 对象存储别名或部分覆盖")
    lines.append("")
    lines.append(render_aliases(object_api["aliasOrPartial"]))
    lines.append("")
    lines.append("## Admin API")
    lines.append("")
    lines.append("| 口径 | 数量 |")
    lines.append("| --- | ---: |")
    total_admin = len(admin_api["exact"]) + len(admin_api["aliasOrPartial"]) + len(admin_api["missing"])
    lines.append("| minio-java adminapi 核心 API | %d |" % total_admin)
    lines.append("| 精确同名 | %d |" % len(admin_api["exact"]))
    lines.append("| 别名或部分覆盖 | %d |" % len(admin_api["aliasOrPartial"]))
    lines.append("| 缺失 | %d |" % len(admin_api["missing"]))
    lines.append("")
    lines.append("### Admin 缺失")
    lines.append("")
    lines.append(render_list(admin_api["missing"]))
    lines.append("")
    lines.append("### Admin 别名或部分覆盖")
    lines.append("")
    lines.append(render_aliases(admin_api["aliasOrPartial"]))
    lines.append("")
    lines.append("## Args 与凭证体系")
    lines.append("")
    lines.append("- minio-java `*Args` 数量：`%d`" % report["args"]["minioJavaCount"])
    lines.append("- 当前 reactive 同名 `*Args` 数量：`%d`" % report["args"]["reactiveCount"])
    lines.append("- minio-java credentials 类：%s" % render_list(report["credentials"]["minioJava"]))
    lines.append("- 当前 reactive credentials 类：%s" % render_list(report["credentials"]["reactive"]))
    lines.append("- 按类名缺失的 credentials：%s" % render_list(report["credentials"]["missingByName"]))
    lines.append("")
    lines.append("## 结论")
    lines.append("")
    object_done = (
        len(object_api["missing"]) == 0
        and len(object_api["aliasOrPartial"]) == 0
    )
    admin_done = (
        len(admin_api["missing"]) == 0
        and len(admin_api["aliasOrPartial"]) == 0
    )
    if object_done and admin_done:
        lines.append(
            "服务端 route catalog 覆盖不能替代 minio-java SDK 对标。当前对象存储核心 API 与 Admin 核心 API 已达到精确同名收口，后续重点转向 `*Args` builder、凭证 Provider、Admin Crypto 自动解密和正式发布工程。"
        )
    else:
        lines.append(
            "服务端 route catalog 覆盖不能替代 minio-java SDK 对标。后续应优先补齐 minio-java API 同名缺口，然后继续推进 `*Args` builder、凭证 Provider 与 Admin Crypto 自动解密。"
        )
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description="生成 minio-java 与响应式 SDK 的 API 对标报告。")
    parser.add_argument("--minio-java-root", required=True)
    parser.add_argument("--worktree", required=True)
    parser.add_argument("--format", choices=["markdown", "json"], default="markdown")
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    report = build_report(args.minio_java_root, args.worktree)
    output = json.dumps(report, ensure_ascii=False, indent=2) + "\n" if args.format == "json" else markdown(report)
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(output, encoding="utf-8")


if __name__ == "__main__":
    main()
