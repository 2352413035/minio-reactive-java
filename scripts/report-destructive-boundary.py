#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 MinIO Admin 破坏性边界分类报告。

该脚本只输出静态分类，不连接 MinIO，也不执行任何写入。分类用于发布复审时解释
`destructive-blocked`：它是高风险操作边界，不是 SDK 功能缺口。
"""

import argparse
import json
from pathlib import Path

ROUTES = [
    {
        'route': 'ADMIN_SET_CONFIG_KV',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw 写入恢复已验证',
        'evidence': '阶段 84/104：config KV typed/raw 写入、探测、恢复。',
    },
    {
        'route': 'ADMIN_SET_CONFIG',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw 原样写回与恢复已验证',
        'evidence': '阶段 117：full config 原样写回 typed/raw 独立 Docker lab。',
    },
    {
        'route': 'ADMIN_ADD_IDP_CONFIG',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw OpenID add 已验证',
        'evidence': '阶段 123：独立 Docker MinIO + 临时 OIDC discovery/JWKS 夹具，按非动态配置重启语义验证 typed/raw add。',
    },
    {
        'route': 'ADMIN_UPDATE_IDP_CONFIG',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw OpenID update 已验证',
        'evidence': '阶段 123：独立 Docker MinIO + 临时 OIDC discovery/JWKS 夹具，验证 IDP update 需要目标配置已加载。',
    },
    {
        'route': 'ADMIN_DELETE_IDP_CONFIG',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw OpenID delete 已验证',
        'evidence': '阶段 123：独立 Docker MinIO + 临时 OIDC discovery/JWKS 夹具，验证 raw delete 与 finally typed delete 恢复路径。',
    },
    {
        'route': 'ADMIN_SET_BUCKET_QUOTA',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw 写入恢复已验证',
        'evidence': '阶段 84/104：bucket quota typed/raw 写入、读取、恢复。',
    },
    {
        'route': 'ADMIN_SET_REMOTE_TARGET',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw set/remove 已验证',
        'evidence': '阶段 106：remote target set/remove typed/raw 双容器 lab。',
    },
    {
        'route': 'ADMIN_REMOVE_REMOTE_TARGET',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw set/remove 已验证',
        'evidence': '阶段 106：remote target set/remove typed/raw 双容器 lab。',
    },
    {
        'route': 'ADMIN_REPLICATION_DIFF',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw replication diff 已验证',
        'evidence': '阶段 122：双 MinIO Docker bucket replication 拓扑中验证 typed/raw ADMIN_REPLICATION_DIFF。',
    },
    {
        'route': 'ADMIN_START_BATCH_JOB',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw start/status/cancel 已验证',
        'evidence': '阶段 108：batch job 双容器 lab。',
    },
    {
        'route': 'ADMIN_CANCEL_BATCH_JOB',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw start/status/cancel 已验证',
        'evidence': '阶段 108：batch job 双容器 lab。',
    },
    {
        'route': 'ADMIN_ADD_TIER',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw add/remove 已验证',
        'evidence': '阶段 107：tier add/remove 双容器 lab。',
    },
    {
        'route': 'ADMIN_EDIT_TIER',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw edit/remove 已验证',
        'evidence': '阶段 110：tier edit 双容器 lab。',
    },
    {
        'route': 'ADMIN_REMOVE_TIER',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw add/edit/remove 已验证',
        'evidence': '阶段 107/110：tier 恢复矩阵。',
    },
    {
        'route': 'ADMIN_SITE_REPLICATION_ADD',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw add/remove 已验证',
        'evidence': '阶段 109：site replication 双容器 lab。',
    },
    {
        'route': 'ADMIN_SITE_REPLICATION_REMOVE',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw add/remove 已验证',
        'evidence': '阶段 109：site replication 双容器 lab。',
    },
    {
        'route': 'ADMIN_SITE_REPLICATION_EDIT',
        'category': '拓扑或身份提供方',
        'status': '需要真实 deploymentID/endpoint 变更',
        'evidence': '已有入口；真实拓扑变更未执行。',
    },
    {
        'route': 'ADMIN_SR_PEER_EDIT',
        'category': '拓扑或身份提供方',
        'status': '需要多站点维护窗口',
        'evidence': 'peer 级变更会影响复制拓扑。',
    },
    {
        'route': 'ADMIN_SR_PEER_REMOVE',
        'category': '拓扑或身份提供方',
        'status': '需要多站点维护窗口',
        'evidence': 'peer 删除会影响复制拓扑。',
    },
    {
        'route': 'ADMIN_SERVICE',
        'category': '维护窗口',
        'status': '需要可用性维护窗口',
        'evidence': 'restart/stop 类操作会影响服务可用性。',
    },
    {
        'route': 'ADMIN_SERVICE_V2',
        'category': '维护窗口',
        'status': '需要可用性维护窗口',
        'evidence': 'restart/stop 类操作会影响服务可用性。',
    },
    {
        'route': 'ADMIN_SERVER_UPDATE',
        'category': '维护窗口',
        'status': '需要升级和回滚计划',
        'evidence': '服务端升级不能在普通 lab 中默认执行。',
    },
    {
        'route': 'ADMIN_SERVER_UPDATE_V2',
        'category': '维护窗口',
        'status': '需要升级和回滚计划',
        'evidence': '服务端升级不能在普通 lab 中默认执行。',
    },
    {
        'route': 'ADMIN_FORCE_UNLOCK',
        'category': '维护窗口',
        'status': '需要锁语义风险评估',
        'evidence': '强制解锁可能破坏正在进行的写入或锁状态。',
    },
    {
        'route': 'ADMIN_SPEEDTEST',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw bounded cluster speedtest 已验证',
        'evidence': '阶段 120：独立 Docker lab 使用 1MiB、并发 1、2 秒窗口验证 typed/raw ADMIN_SPEEDTEST；强类型无参入口已禁用。',
    },
    {
        'route': 'ADMIN_SPEEDTEST_OBJECT',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw bounded object speedtest 已验证',
        'evidence': '阶段 120：独立 Docker lab 使用 1MiB、并发 1、2 秒窗口验证 typed/raw ADMIN_SPEEDTEST_OBJECT；强类型无参入口已禁用。',
    },
    {
        'route': 'ADMIN_SPEEDTEST_DRIVE',
        'category': '已有独立 lab 证据',
        'status': 'typed/raw bounded drive speedtest 已验证',
        'evidence': '阶段 121：独立 Docker lab 使用 serial=true、blocksize=4096、filesize=8192 验证 typed/raw ADMIN_SPEEDTEST_DRIVE；强类型无参入口已禁用。',
    },
    {
        'route': 'ADMIN_SPEEDTEST_NET',
        'category': '资源压测',
        'status': '单节点独立 lab 返回 NotImplemented，仍需服务端实现与专用压测窗口',
        'evidence': '阶段 124：typed/raw 有界 net speedtest 在独立 Docker lab 均返回 HTTP 501 NotImplemented；预期失败采证通过。',
    },
    {
        'route': 'ADMIN_SPEEDTEST_SITE',
        'category': '资源压测',
        'status': '单节点独立 lab 返回 NotImplemented，仍需 site replication 拓扑与压测窗口',
        'evidence': '阶段 124：typed/raw 有界 site speedtest 在独立 Docker lab 均返回 HTTP 501 NotImplemented；预期失败采证通过。',
    },
]


def summarize():
    counts = {}
    for item in ROUTES:
        counts[item['category']] = counts.get(item['category'], 0) + 1
    return counts


def render_markdown(worktree):
    counts = summarize()
    lines = [
        '# 破坏性 Admin 边界分类报告',
        '',
        '本报告只解释 `destructive-blocked` 风险分类，不连接 MinIO，也不执行任何写入。',
        '',
        f'- 工作区：`{worktree}`',
        f'- 路由总数：`{len(ROUTES)}`',
        '',
        '## 分类汇总',
        '',
        '| 分类 | 数量 |',
        '| --- | ---: |',
    ]
    for category in sorted(counts):
        lines.append(f'| {category} | {counts[category]} |')
    lines.extend(['', '## 路由明细', '', '| route | 分类 | 当前状态 | 证据/原因 |', '| --- | --- | --- | --- |'])
    for item in ROUTES:
        lines.append(f"| `{item['route']}` | {item['category']} | {item['status']} | {item['evidence']} |")
    lines.extend([
        '',
        '## 结论',
        '',
        '`destructive-blocked` 不是功能缺失计数。降低该计数必须附独立 lab 报告或维护窗口报告；不能只靠新增 SDK 方法、mock 测试或 route parity 证明。',
    ])
    return '\n'.join(lines) + '\n'


def main():
    parser = argparse.ArgumentParser(description='生成破坏性 Admin 边界分类报告。')
    parser.add_argument('--worktree', default='.', help='报告中显示的工作区名称。')
    parser.add_argument('--format', choices=['markdown', 'json'], required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()

    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    if args.format == 'json':
        output.write_text(json.dumps({'worktree': args.worktree, 'summary': summarize(), 'routes': ROUTES}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    else:
        output.write_text(render_markdown(args.worktree), encoding='utf-8')


if __name__ == '__main__':
    main()
