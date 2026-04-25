#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 minio-java 与响应式 SDK 的签名级差异报告。

本脚本只做源码静态扫描：不编译、不联网、不读取凭证。它补充
report-minio-java-parity.py 的“名称是否存在”口径，用于发现重载数量、
credentials 构造器和 Args builder 入口等迁移体验差异。
"""

from __future__ import print_function

import argparse
import json
import re
from collections import Counter, OrderedDict
from pathlib import Path

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

INTENTIONAL_REACTIVE_BOUNDARY_TYPES = ["OkHttpClient", "HttpUrl", "Response", "Request"]


def read_text(path):
    try:
        return Path(path).read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return Path(path).read_text(errors="ignore")


def strip_comments(text):
    text = re.sub(r"/\*.*?\*/", " ", text, flags=re.S)
    return re.sub(r"//.*", " ", text)



def extract_parenthesized(text, open_index):
    depth = 0
    start = open_index + 1
    for index in range(open_index, len(text)):
        ch = text[index]
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return text[start:index]
    return ""

def split_params(params):
    params = params.strip()
    if not params:
        return []
    result = []
    current = []
    depth = 0
    for ch in params:
        if ch == "<":
            depth += 1
        elif ch == ">" and depth:
            depth -= 1
        if ch == "," and depth == 0:
            result.append("".join(current).strip())
            current = []
            continue
        current.append(ch)
    if current:
        result.append("".join(current).strip())
    return result


def normalize_param_type(param):
    param = re.sub(r"@\w+(?:\([^)]*\))?", " ", param)
    param = re.sub(r"\bfinal\b", " ", param)
    param = " ".join(param.split())
    if not param:
        return ""
    parts = param.split()
    if len(parts) == 1:
        return parts[0]
    return " ".join(parts[:-1])


def signature_from_params(params):
    types = [normalize_param_type(item) for item in split_params(params)]
    types = [item for item in types if item]
    return {"arity": len(types), "types": types}


def public_method_counts(java_file):
    path = Path(java_file)
    if not path.is_file():
        return Counter()
    text = strip_comments(read_text(path))
    pattern = re.compile(
        r"(?m)^\s*public\s+"
        r"(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?"
        r"(?:<[^;{}()]+>\s+)?"
        r"(?:[\w\[\]<>?,.]+\s+)+"
        r"(?P<name>[A-Za-z_]\w*)\s*\("
    )
    return Counter(m.group("name") for m in pattern.finditer(text))


def public_static_factories(java_file):
    path = Path(java_file)
    if not path.is_file():
        return []
    text = strip_comments(read_text(path))
    pattern = re.compile(
        r"(?m)^\s*public\s+static\s+(?:<[^;{}()]+>\s+)?"
        r"(?P<return>[\w\[\]<>?,.]+)\s+"
        r"(?P<name>[A-Za-z_]\w*)\s*\("
    )
    factories = []
    for match in pattern.finditer(text):
        params = extract_parenthesized(text, match.end() - 1)
        sig = signature_from_params(params)
        factories.append(
            {
                "name": match.group("name"),
                "returnType": match.group("return"),
                "arity": sig["arity"],
                "types": sig["types"],
            }
        )
    return factories


def public_constructors(java_file, class_name):
    path = Path(java_file)
    if not path.is_file():
        return []
    text = strip_comments(read_text(path))
    pattern = re.compile(r"(?m)^\s*public\s+" + re.escape(class_name) + r"\s*\(")
    constructors = []
    for match in pattern.finditer(text):
        params = extract_parenthesized(text, match.end() - 1)
        sig = signature_from_params(params)
        constructors.append({"arity": sig["arity"], "types": sig["types"]})
    has_public_class = re.search(
        r"(?m)^\s*public\s+(?:final\s+)?class\s+" + re.escape(class_name) + r"\b", text
    )
    is_abstract = re.search(
        r"(?m)^\s*public\s+abstract\s+class\s+" + re.escape(class_name) + r"\b", text
    )
    if not constructors and has_public_class and not is_abstract:
        constructors.append({"arity": 0, "types": []})
    return constructors


def class_names(root, relative_dir):
    base = Path(root) / relative_dir
    if not base.is_dir():
        return []
    return sorted(p.stem for p in base.glob("*.java"))


def args_path(root, class_name):
    root = Path(root)
    candidates = [
        root / "src/main/java/io/minio/reactive" / (class_name + ".java"),
        root / "src/main/java/io/minio/reactive/messages" / (class_name + ".java"),
    ]
    for candidate in candidates:
        if candidate.is_file():
            return candidate
    return None


def entry_methods(java_file):
    counts = public_method_counts(java_file)
    names = [name for name in ["builder", "of", "create"] if counts.get(name, 0) > 0]
    return names



def max_counts(*counters):
    result = Counter()
    for counter in counters:
        for name, count in counter.items():
            if count > result[name]:
                result[name] = count
    return result

def overloaded_deltas(reference_counts, actual_counts, excludes):
    rows = []
    for name in sorted(reference_counts):
        if name in excludes:
            continue
        ref = reference_counts[name]
        actual = actual_counts.get(name, 0)
        if actual == 0:
            status = "缺失"
        elif actual < ref:
            status = "重载较少"
        elif actual > ref:
            status = "响应式扩展"
        else:
            status = "一致"
        rows.append({"name": name, "minioJava": ref, "reactive": actual, "status": status})
    return rows


def credential_report(minio_java_root, worktree):
    result = []
    names = class_names(minio_java_root, "api/src/main/java/io/minio/credentials")
    for name in names:
        ref_file = Path(minio_java_root) / "api/src/main/java/io/minio/credentials" / (name + ".java")
        actual_file = Path(worktree) / "src/main/java/io/minio/reactive/credentials" / (name + ".java")
        ref_ctors = public_constructors(ref_file, name)
        actual_ctors = public_constructors(actual_file, name)
        factories = public_static_factories(actual_file)
        intentional = any(
            boundary in " ".join(sig.get("types", []))
            for sig in ref_ctors
            for boundary in INTENTIONAL_REACTIVE_BOUNDARY_TYPES
        )
        if not actual_file.is_file():
            status = "类缺失"
        elif len(ref_ctors) == 0:
            status = "无公开构造器要求"
        elif actual_ctors:
            status = "有响应式构造器"
        elif factories:
            status = "由静态工厂覆盖"
        elif intentional:
            status = "阻塞 HTTP 构造器有意不同"
        else:
            status = "需要设计"
        result.append(
            {
                "name": name,
                "minioJavaConstructors": ref_ctors,
                "reactiveConstructors": actual_ctors,
                "reactiveFactories": factories,
                "status": status,
                "intentionalReactiveBoundary": intentional,
            }
        )
    return result


def args_report(minio_java_root, worktree):
    result = []
    for ref_file in sorted((Path(minio_java_root) / "api/src/main/java/io/minio").glob("*Args.java")):
        name = ref_file.stem
        actual = args_path(worktree, name)
        result.append(
            {
                "name": name,
                "reactivePath": str(actual) if actual else None,
                "minioJavaEntryMethods": entry_methods(ref_file),
                "reactiveEntryMethods": entry_methods(actual) if actual else [],
                "status": "存在" if actual else "缺失",
            }
        )
    return result


def build_report(minio_java_root, worktree):
    minio_java_root = Path(minio_java_root)
    worktree = Path(worktree)
    ref_object_counts = max_counts(
        public_method_counts(minio_java_root / "api/src/main/java/io/minio/MinioClient.java"),
        public_method_counts(minio_java_root / "api/src/main/java/io/minio/MinioAsyncClient.java"),
    )
    actual_object_counts = public_method_counts(worktree / "src/main/java/io/minio/reactive/ReactiveMinioClient.java")
    ref_admin_counts = public_method_counts(
        minio_java_root / "adminapi/src/main/java/io/minio/admin/MinioAdminClient.java"
    )
    actual_admin_counts = public_method_counts(
        worktree / "src/main/java/io/minio/reactive/ReactiveMinioAdminClient.java"
    )
    return {
        "minioJavaRoot": str(minio_java_root),
        "worktree": str(worktree),
        "objectOverloads": overloaded_deltas(ref_object_counts, actual_object_counts, OBJECT_METHOD_EXCLUDES),
        "adminOverloads": overloaded_deltas(ref_admin_counts, actual_admin_counts, ADMIN_METHOD_EXCLUDES),
        "credentials": credential_report(minio_java_root, worktree),
        "args": args_report(minio_java_root, worktree),
    }


def sig_text(signatures):
    if not signatures:
        return "无"
    values = []
    for sig in signatures:
        if sig.get("types"):
            values.append("(%s)" % ", ".join(sig["types"]))
        else:
            values.append("()")
    return "<br>".join(values)


def factory_text(factories):
    if not factories:
        return "无"
    values = []
    for factory in factories:
        params = ", ".join(factory["types"])
        values.append("%s(%s)" % (factory["name"], params))
    return "<br>".join(values)


def overload_summary(rows):
    counter = Counter(row["status"] for row in rows)
    return OrderedDict((key, counter.get(key, 0)) for key in ["一致", "重载较少", "响应式扩展", "缺失"])


def markdown(report):
    lines = []
    lines.append("# minio-java 签名级差异报告")
    lines.append("")
    lines.append("- minio-java：`%s`" % report["minioJavaRoot"])
    lines.append("- worktree：`%s`" % report["worktree"])
    lines.append("")
    lines.append("## 方法重载数量摘要")
    lines.append("")
    for title, rows in [("对象存储 API", report["objectOverloads"]), ("Admin API", report["adminOverloads"] )]:
        summary = overload_summary(rows)
        lines.append("### %s" % title)
        lines.append("")
        lines.append("| 状态 | 数量 |")
        lines.append("| --- | ---: |")
        for key, value in summary.items():
            lines.append("| %s | %d |" % (key, value))
        lines.append("")
    lines.append("## 重载较少或缺失的方法")
    lines.append("")
    lines.append("### 对象存储")
    lines.append("")
    lines.extend(overload_rows(report["objectOverloads"]))
    lines.append("")
    lines.append("### Admin")
    lines.append("")
    lines.extend(overload_rows(report["adminOverloads"]))
    lines.append("")
    lines.append("## credentials provider 构造器/工厂")
    lines.append("")
    lines.append("| 类 | minio-java 构造器 | reactive 构造器 | reactive 静态工厂 | 状态 |")
    lines.append("| --- | --- | --- | --- | --- |")
    for item in report["credentials"]:
      lines.append(
          "| `%s` | %s | %s | %s | %s |"
          % (
              item["name"],
              sig_text(item["minioJavaConstructors"]),
              sig_text(item["reactiveConstructors"]),
              factory_text(item["reactiveFactories"]),
              item["status"],
          )
      )
    lines.append("")
    lines.append("## Args builder 入口")
    lines.append("")
    missing_args = [item for item in report["args"] if item["status"] != "存在"]
    no_entries = [
        item
        for item in report["args"]
        if item["status"] == "存在" and item["minioJavaEntryMethods"] and not item["reactiveEntryMethods"]
    ]
    lines.append("- Args 类缺失：%s" % ("无" if not missing_args else "、".join("`%s`" % item["name"] for item in missing_args)))
    lines.append("- 已存在但未扫描到 `builder/of/create` 入口的 Args：%s" % ("无" if not no_entries else "、".join("`%s`" % item["name"] for item in no_entries)))
    lines.append("")
    lines.append("## 结论")
    lines.append("")
    lines.append(
        "该报告用于后续阶段排查迁移体验差异。名称级报告已经证明核心 API、Admin API、Args 类名和 credentials 类名收口；签名级报告进一步揭示哪些差异是响应式 SDK 有意保留，哪些需要继续补重载或工厂。"
    )
    return "\n".join(lines) + "\n"


def overload_rows(rows):
    filtered = [row for row in rows if row["status"] in ("重载较少", "缺失")]
    if not filtered:
        return ["无"]
    result = ["| 方法 | minio-java 重载数 | reactive 重载数 | 状态 |", "| --- | ---: | ---: | --- |"]
    for row in filtered:
        result.append(
            "| `%s` | %d | %d | %s |"
            % (row["name"], row["minioJava"], row["reactive"], row["status"])
        )
    return result


def main():
    parser = argparse.ArgumentParser(description="生成 minio-java 与响应式 SDK 的签名级差异报告。")
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
