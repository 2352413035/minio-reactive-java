#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""汇总发布候选与正式发布门禁状态。"""

import argparse
import importlib.util
import json
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent


def load_report_module(module_name, script_name):
    """从同目录脚本按文件名加载报告模块，避免复制已有审计逻辑。"""
    script = SCRIPT_DIR / script_name
    spec = importlib.util.spec_from_file_location(module_name, script)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


PARITY = load_report_module('minio_java_parity_report', 'report-minio-java-parity.py')
SIGNATURE = load_report_module('minio_java_signature_report', 'report-minio-java-signature-parity.py')
ROUTE = load_report_module('route_parity_report', 'report-route-parity.py')
CAPABILITY = load_report_module('capability_matrix_report', 'report-capability-matrix.py')
POM = load_report_module('pom_release_metadata_report', 'report-pom-release-metadata.py')
DESTRUCTIVE = load_report_module('destructive_boundary_report', 'report-destructive-boundary.py')


def yes_no(value):
    return '是' if value else '否'


def count_status(rows, status):
    return sum(1 for row in rows if row.get('status') == status)


def less_or_missing_overloads(rows):
    return [row for row in rows if row.get('status') in ('缺失', '重载较少')]


def args_problem_rows(rows):
    return [row for row in rows if row.get('status') == '缺失']


def capability_totals(matrix):
    totals = {
        'routeCatalog': 0,
        'productTyped': 0,
        'advancedCompatible': 0,
        'rawFallback': 0,
        'encryptedBlocked': 0,
        'destructiveBlocked': 0,
    }
    for row in matrix['rows']:
        totals['routeCatalog'] += row['route-catalog']
        totals['productTyped'] += row['product-typed']
        totals['advancedCompatible'] += row['advanced-compatible']
        totals['rawFallback'] += row['raw-fallback']
        totals['encryptedBlocked'] += row['encrypted-blocked']
        totals['destructiveBlocked'] += row['destructive-blocked']
    return totals


def build_worktree_report(worktree, minio_java_root, minio_root):
    """生成单个工作区的聚合发布状态。"""
    parity = PARITY.build_report(minio_java_root, worktree)
    signature = SIGNATURE.build_report(minio_java_root, worktree)
    route = ROUTE.build_report(minio_root, worktree)
    capability = CAPABILITY.build_matrix(worktree)
    pom = POM.audit_worktree(worktree)
    destructive_summary = DESTRUCTIVE.summarize()

    object_missing = len(parity['objectApi']['missing'])
    object_alias = len(parity['objectApi']['aliasOrPartial'])
    admin_missing = len(parity['adminApi']['missing'])
    admin_alias = len(parity['adminApi']['aliasOrPartial'])
    credentials_missing = len(parity['credentials']['missingByName'])
    args_same_count = parity['args']['minioJavaCount'] == parity['args']['reactiveCount']

    object_overload_gaps = less_or_missing_overloads(signature['objectOverloads'])
    admin_overload_gaps = less_or_missing_overloads(signature['adminOverloads'])
    args_gaps = args_problem_rows(signature['args'])

    route_summary = route['summary']
    route_ready = route_summary['missingFromCatalog'] == 0 and route_summary['extraInCatalog'] == 0

    totals = capability_totals(capability)
    capability_ready = totals['rawFallback'] == 0 and totals['encryptedBlocked'] == 0
    crypto_gate_pass = CAPABILITY.crypto_gate_passed(worktree)

    lab_evidence = destructive_summary.get('已有独立 lab 证据', 0)
    destructive_total = sum(destructive_summary.values())
    destructive_without_full_evidence = destructive_total - lab_evidence

    sdk_candidate_ready = all([
        object_missing == 0,
        object_alias == 0,
        admin_missing == 0,
        admin_alias == 0,
        credentials_missing == 0,
        args_same_count,
        not object_overload_gaps,
        not admin_overload_gaps,
        not args_gaps,
        route_ready,
        capability_ready,
        crypto_gate_pass,
    ])

    formal_release_ready = all([
        sdk_candidate_ready,
        pom['publishReady'],
        destructive_without_full_evidence == 0,
    ])

    blockers = []
    if not sdk_candidate_ready:
        blockers.append('SDK 发布候选门禁仍有功能或报告缺口')
    if not pom['publishReady']:
        blockers.append('POM 发布元数据或发布插件缺负责人确认')
    if destructive_without_full_evidence:
        blockers.append('仍有破坏性 Admin 操作需要独立 lab 或维护窗口证据')

    return {
        'worktree': str(worktree),
        'sdkCandidateReady': sdk_candidate_ready,
        'formalReleaseReady': formal_release_ready,
        'parity': {
            'objectExact': len(parity['objectApi']['exact']),
            'objectMissing': object_missing,
            'objectAliasOrPartial': object_alias,
            'adminExact': len(parity['adminApi']['exact']),
            'adminMissing': admin_missing,
            'adminAliasOrPartial': admin_alias,
            'argsMinioJava': parity['args']['minioJavaCount'],
            'argsReactive': parity['args']['reactiveCount'],
            'credentialsMissing': credentials_missing,
        },
        'signature': {
            'objectLessOrMissing': len(object_overload_gaps),
            'adminLessOrMissing': len(admin_overload_gaps),
            'argsMissing': len(args_gaps),
            'argsInternalBoundaries': count_status(signature['args'], '响应式内部边界'),
        },
        'route': {
            'serverRouteCount': route_summary['serverRouteCount'],
            'catalogRouteCount': route_summary['catalogRouteCount'],
            'missingFromCatalog': route_summary['missingFromCatalog'],
            'extraInCatalog': route_summary['extraInCatalog'],
        },
        'capability': totals,
        'pom': {
            'publishReady': pom['publishReady'],
            'missingPublicationMetadata': pom['missingPublicationMetadata'],
            'missingReleasePlugins': pom['missingReleasePlugins'],
        },
        'destructive': {
            'total': destructive_total,
            'withIndependentLabEvidence': lab_evidence,
            'withoutFullEvidence': destructive_without_full_evidence,
            'categories': destructive_summary,
        },
        'cryptoGatePass': crypto_gate_pass,
        'blockers': blockers,
    }


def render_markdown(reports):
    lines = [
        '# 发布就绪总览报告',
        '',
        '本报告聚合 minio-java 对标、签名级差异、route parity、能力矩阵、POM 元数据、Crypto Gate 与破坏性边界。',
        '它只读取源码和报告脚本，不连接 MinIO，不执行写入，不发布 Maven，也不修改 `pom.xml`。',
        '',
        '## 总览',
        '',
        '| 工作区 | SDK 发布候选就绪 | 正式 Maven/tag 发布就绪 | 主要阻塞 |',
        '| --- | --- | --- | --- |',
    ]
    for report in reports:
        blockers = '；'.join(report['blockers']) if report['blockers'] else '无'
        lines.append(
            f"| `{report['worktree']}` | {yes_no(report['sdkCandidateReady'])} | "
            f"{yes_no(report['formalReleaseReady'])} | {blockers} |"
        )

    for report in reports:
        lines.extend(['', f"## {report['worktree']}", ''])
        p = report['parity']
        s = report['signature']
        r = report['route']
        c = report['capability']
        pom = report['pom']
        d = report['destructive']
        lines.extend([
            '| 门禁 | 当前值 | 结论 |',
            '| --- | --- | --- |',
            f"| minio-java 对象 API | 精确同名 {p['objectExact']}，缺失 {p['objectMissing']}，别名/部分 {p['objectAliasOrPartial']} | {'通过' if p['objectMissing'] == 0 and p['objectAliasOrPartial'] == 0 else '需处理'} |",
            f"| minio-java Admin API | 精确同名 {p['adminExact']}，缺失 {p['adminMissing']}，别名/部分 {p['adminAliasOrPartial']} | {'通过' if p['adminMissing'] == 0 and p['adminAliasOrPartial'] == 0 else '需处理'} |",
            f"| Args / credentials | Args {p['argsReactive']} / {p['argsMinioJava']}，credentials 缺失 {p['credentialsMissing']} | {'通过' if p['argsReactive'] == p['argsMinioJava'] and p['credentialsMissing'] == 0 else '需处理'} |",
            f"| 签名级差异 | 对象缺失/重载较少 {s['objectLessOrMissing']}，Admin 缺失/重载较少 {s['adminLessOrMissing']}，Args 缺失 {s['argsMissing']}，内部边界 {s['argsInternalBoundaries']} | {'通过' if s['objectLessOrMissing'] == 0 and s['adminLessOrMissing'] == 0 and s['argsMissing'] == 0 else '需处理'} |",
            f"| route parity | 服务端 {r['serverRouteCount']}，catalog {r['catalogRouteCount']}，缺失 {r['missingFromCatalog']}，额外 {r['extraInCatalog']} | {'通过' if r['missingFromCatalog'] == 0 and r['extraInCatalog'] == 0 else '需处理'} |",
            f"| capability matrix | product-typed {c['productTyped']} / route {c['routeCatalog']}，raw-fallback {c['rawFallback']}，encrypted-blocked {c['encryptedBlocked']}，destructive-blocked {c['destructiveBlocked']} | {'通过' if c['rawFallback'] == 0 and c['encryptedBlocked'] == 0 else '需处理'} |",
            f"| Crypto Gate | {'Pass' if report['cryptoGatePass'] else 'Fail'} | {'通过' if report['cryptoGatePass'] else '需处理'} |",
            f"| POM 发布元数据 | 缺元数据 {len(pom['missingPublicationMetadata'])}，缺插件 {len(pom['missingReleasePlugins'])} | {'通过' if pom['publishReady'] else '需负责人输入'} |",
            f"| 破坏性边界 | 总数 {d['total']}，已有独立 lab 证据 {d['withIndependentLabEvidence']}，仍需证据 {d['withoutFullEvidence']} | {'通过' if d['withoutFullEvidence'] == 0 else '需 lab/维护窗口'} |",
        ])
        if pom['missingPublicationMetadata'] or pom['missingReleasePlugins']:
            lines.extend(['', '### POM 待负责人确认项', ''])
            for name in pom['missingPublicationMetadata']:
                lines.append(f'- 发布元数据：`{name}`')
            for name in pom['missingReleasePlugins']:
                lines.append(f'- 发布插件：`{name}`')
        lines.extend(['', '### 破坏性边界分类', '', '| 分类 | 数量 |', '| --- | ---: |'])
        for category, count in sorted(d['categories'].items()):
            lines.append(f'| {category} | {count} |')

    lines.extend([
        '',
        '## 结论',
        '',
        '- SDK 功能覆盖层面可以按“发布候选就绪”管理；正式发布不能只看 route parity 或 product-typed。',
        '- Crypto Gate 已进入 Pass 回归项，不能再写成未实现阻塞。',
        '- 正式 Maven/tag 发布仍需要负责人提供 POM 元数据、签名、SBOM、发布仓库和回滚策略。',
        '- `destructive-blocked` 是真实运维风险证据门禁，降低它必须附独立 lab 或维护窗口报告。',
    ])
    return '\n'.join(lines).rstrip() + '\n'


def main():
    parser = argparse.ArgumentParser(description='生成发布就绪总览报告。')
    parser.add_argument('--worktree', action='append', required=True, help='需要审计的 SDK 工作区，可重复。')
    parser.add_argument('--minio-java-root', required=True, help='minio-java 对标项目路径。')
    parser.add_argument('--minio-root', required=True, help='MinIO 服务端源码路径，用于 route parity。')
    parser.add_argument('--format', choices=['markdown', 'json'], required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()

    reports = [
        build_worktree_report(Path(worktree), Path(args.minio_java_root), Path(args.minio_root))
        for worktree in args.worktree
    ]
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    if args.format == 'json':
        output.write_text(json.dumps(reports, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    else:
        output.write_text(render_markdown(reports), encoding='utf-8')


if __name__ == '__main__':
    main()
