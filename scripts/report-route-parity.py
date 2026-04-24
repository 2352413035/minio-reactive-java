#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 MinIO 服务端路由与 SDK catalog 的对照报告。"""

import argparse
import json
import re
from collections import Counter
from pathlib import Path

HTTP_METHODS = {
    'http.MethodGet': 'GET',
    'http.MethodPost': 'POST',
    'http.MethodPut': 'PUT',
    'http.MethodDelete': 'DELETE',
    'http.MethodHead': 'HEAD',
}

ROUTER_FILES = [
    'cmd/api-router.go',
    'cmd/admin-router.go',
    'cmd/kms-router.go',
    'cmd/sts-handlers.go',
    'cmd/metrics-router.go',
    'cmd/healthcheck-router.go',
]


def quoted_strings(text):
    return re.findall(r'"([^"\\]*(?:\\.[^"\\]*)*)"', text)


def normalize_path(path):
    normalized = re.sub(r'\{([^}:]+):[^}]+\}', r'{\1}', path)
    return normalized if normalized == '//' else normalized.replace('//', '/')


def strip_string_literals(text):
    return re.sub(r'"[^"\\]*(?:\\.[^"\\]*)*"', '""', text)


def paren_balance(text):
    cleaned = strip_string_literals(text)
    return cleaned.count('(') - cleaned.count(')')


def collect_method_chains(text):
    lines = text.splitlines()
    chains = []
    index = 0
    while index < len(lines):
        line = lines[index]
        if '.Methods(' in line:
            statement = line.strip()
            saw_handler = 'HandlerFunc' in line or '.Handler(' in line
            balance = paren_balance(statement)
            while True:
                if saw_handler and balance <= 0 and not statement.rstrip().endswith('.'):
                    break
                index += 1
                if index >= len(lines):
                    break
                next_line = lines[index].strip()
                statement += ' ' + next_line
                balance += paren_balance(next_line)
                if 'HandlerFunc' in next_line or '.Handler(' in next_line:
                    saw_handler = True
            chains.append(statement)
        index += 1
    return chains


def method_list(method_expression):
    if 'r.methods' in method_expression:
        return []
    return [HTTP_METHODS[token] for token in re.findall(r'http\.Method[A-Za-z]+', method_expression) if token in HTTP_METHODS]


def path_expression(statement):
    match = re.search(r'\.\s*Path\((.*?)\)', statement)
    return match.group(1).strip() if match else None


def concat_expression(expression, constants):
    result = ''
    for part in [value.strip() for value in expression.split('+')]:
        if part.startswith('"'):
            result += re.match(r'"(.*)"', part).group(1)
        elif part == 'SlashSeparator':
            result += '/'
        elif part in constants:
            result += constants[part]
        else:
            result += '{' + part + '}'
    return normalize_path(result)


def parse_queries(statement):
    defaults = {}
    required = []
    for query_expression in re.findall(r'\.\s*Queries\((.*?)\)', statement):
        values = quoted_strings(query_expression)
        for index in range(0, len(values) - 1, 2):
            key = values[index]
            value = values[index + 1]
            if value.startswith('{'):
                required.append(key)
            else:
                defaults[key] = value
    return defaults, sorted(required)


def route_record(family, method, path, auth_scheme='sigv4', defaults=None, required=None, source='', condition=''):
    return {
        'family': family,
        'method': method,
        'pathTemplate': normalize_path(path),
        'authScheme': auth_scheme,
        'defaultQueryParameters': defaults or {},
        'requiredQueryParameters': sorted(required or []),
        'source': source,
        'condition': condition,
    }


def signature(route):
    return (
        route['family'],
        route['method'],
        route['pathTemplate'],
        route['authScheme'],
        tuple(sorted(route.get('defaultQueryParameters', {}).items())),
        tuple(sorted(route.get('requiredQueryParameters', []))),
    )


def signature_text(route_or_signature):
    if isinstance(route_or_signature, tuple):
        family, method, path, auth, defaults, required = route_or_signature
    else:
        family, method, path, auth, defaults, required = signature(route_or_signature)
    defaults_text = ','.join('%s=%s' % item for item in defaults) or '-'
    required_text = ','.join(required) or '-'
    return '%s %s %s auth=%s defaultQuery=%s requiredQuery=%s' % (
        family,
        method,
        path,
        auth,
        defaults_text,
        required_text,
    )


def parse_catalog(worktree):
    catalog_file = worktree / 'src/main/java/io/minio/reactive/catalog/MinioApiCatalog.java'
    text = catalog_file.read_text(encoding='utf-8')
    records = []
    for kind, args in re.findall(r'endpoints\.add\((e|eAuth)\((.*?)\)\);', text):
        values = quoted_strings(args)
        if kind == 'eAuth':
            name, family, method, path, auth_scheme = values[:5]
        else:
            name, family, method, path = values[:4]
            auth_scheme = 'sigv4' if re.search(r',\s*true\s*,', args) else 'none'
        query_values = quoted_strings(re.search(r'q\((.*?)\)', args).group(1))
        required_values = quoted_strings(re.search(r'req\((.*?)\)', args).group(1))
        defaults = {}
        for index in range(0, len(query_values) - 1, 2):
            defaults[query_values[index]] = query_values[index + 1]
        record = route_record(family, method, path, auth_scheme, defaults, required_values, source='MinioApiCatalog.java')
        record['name'] = name
        records.append(record)
    return records


def parse_s3_routes(minio_root):
    text = (minio_root / 'cmd/api-router.go').read_text(encoding='utf-8')
    routes = []
    for statement in collect_method_chains(text):
        if 'r.methods' in statement or 'notImplementedHandler' in statement or 'errorResponseHandler' in statement:
            continue
        if 'router.Methods' not in statement and 'apiRouter.Methods' not in statement:
            continue
        methods = method_list(re.search(r'\.\s*Methods\((.*?)\)', statement).group(1))
        path_expr = path_expression(statement)
        if 'apiRouter.Methods' in statement:
            path = concat_expression(path_expr or 'SlashSeparator', {})
            if path == '//':
                continue
        else:
            path = '/{bucket}/{object}' if path_expr and 'object' in path_expr else '/{bucket}'
        defaults, required = parse_queries(statement)
        for method in methods:
            routes.append(route_record('s3', method, path, 'sigv4', defaults, required, 'cmd/api-router.go'))
    return routes


def parse_admin_routes(minio_root):
    text = (minio_root / 'cmd/admin-router.go').read_text(encoding='utf-8')
    constants = {
        'adminVersion': '/v3',
        'adminAPISiteReplicationDevNull': '/site-replication/devnull',
        'adminAPISiteReplicationNetPerf': '/site-replication/netperf',
        'adminAPIClientDevNull': '/speedtest/client/devnull',
        'adminAPIClientDevExtraTime': '/speedtest/client/devnull/extratime',
    }
    routes = []
    for statement in collect_method_chains(text):
        if 'adminRouter.Methods' not in statement:
            continue
        methods = method_list(re.search(r'\.\s*Methods\((.*?)\)', statement).group(1))
        path = '/minio/admin' + concat_expression(path_expression(statement), constants)
        defaults, required = parse_queries(statement)
        condition = ''
        if any(token in path for token in ('/heal', '/pools/', '/rebalance/')):
            condition = '分布式或纠删码模式才注册'
        elif any(token in path for token in ('/config', '-config-', '/set-config-kv', '/del-config-kv')):
            condition = 'enableConfigOps 控制'
        for method in methods:
            routes.append(route_record('admin', method, path, 'sigv4', defaults, required, 'cmd/admin-router.go', condition))
    return routes


def parse_kms_routes(minio_root):
    text = (minio_root / 'cmd/kms-router.go').read_text(encoding='utf-8')
    routes = []
    for statement in collect_method_chains(text):
        if 'kmsRouter.Methods' not in statement:
            continue
        methods = method_list(re.search(r'\.\s*Methods\((.*?)\)', statement).group(1))
        path = '/minio/kms' + concat_expression(path_expression(statement), {'version': '/v1'})
        defaults, required = parse_queries(statement)
        for method in methods:
            routes.append(route_record('kms', method, path, 'sigv4', defaults, required, 'cmd/kms-router.go'))
    return routes


def parse_health_routes(minio_root):
    text = (minio_root / 'cmd/healthcheck-router.go').read_text(encoding='utf-8')
    constants = {
        'healthCheckClusterPath': '/cluster',
        'healthCheckClusterReadPath': '/cluster/read',
        'healthCheckLivenessPath': '/live',
        'healthCheckReadinessPath': '/ready',
    }
    routes = []
    for statement in collect_method_chains(text):
        if 'healthRouter.Methods' not in statement:
            continue
        methods = method_list(re.search(r'\.\s*Methods\((.*?)\)', statement).group(1))
        path = '/minio/health' + concat_expression(path_expression(statement), constants)
        for method in methods:
            routes.append(route_record('health', method, path, 'none', source='cmd/healthcheck-router.go'))
    return routes


def parse_metrics_routes(minio_root):
    text = (minio_root / 'cmd/metrics-router.go').read_text(encoding='utf-8')
    routes = []
    for path in [
        '/prometheus/metrics',
        '/v2/metrics/cluster',
        '/v2/metrics/bucket',
        '/v2/metrics/node',
        '/v2/metrics/resource',
    ]:
        if path in text:
            routes.append(route_record('metrics', 'GET', '/minio' + path, 'bearer', source='cmd/metrics-router.go'))
    if 'metricsV3Path + "{pathComps:.*}"' in text:
        routes.append(route_record('metrics', 'GET', '/minio/metrics/v3{pathComps}', 'bearer', source='cmd/metrics-router.go'))
    return routes


def parse_sts_routes(minio_root):
    text = (minio_root / 'cmd/sts-handlers.go').read_text(encoding='utf-8')
    routes = []
    if 'sts.AssumeRole)' in text:
        routes.append(route_record('sts', 'POST', '/', 'sigv4', source='cmd/sts-handlers.go', condition='表单请求且 SigV4'))
    if 'sts.AssumeRoleWithSSO)' in text:
        routes.append(route_record('sts', 'POST', '/', 'none', source='cmd/sts-handlers.go', condition='表单请求且无 query'))
    sts_actions = [
        ('AssumeRoleWithClientGrants', ['Token']),
        ('AssumeRoleWithWebIdentity', ['WebIdentityToken']),
        ('AssumeRoleWithLDAPIdentity', ['LDAPUsername', 'LDAPPassword']),
        ('AssumeRoleWithCertificate', []),
        ('AssumeRoleWithCustomToken', []),
    ]
    for action, required in sts_actions:
        if action in text:
            routes.append(
                route_record(
                    'sts',
                    'POST',
                    '/',
                    'none',
                    {'Action': action, 'Version': '2011-06-15'},
                    required,
                    'cmd/sts-handlers.go',
                )
            )
    return routes


def parse_minio_routes(minio_root):
    return (
        parse_s3_routes(minio_root)
        + parse_admin_routes(minio_root)
        + parse_kms_routes(minio_root)
        + parse_health_routes(minio_root)
        + parse_metrics_routes(minio_root)
        + parse_sts_routes(minio_root)
    )


def compare_routes(catalog_routes, server_routes):
    catalog_counter = Counter(signature(route) for route in catalog_routes)
    server_counter = Counter(signature(route) for route in server_routes)
    missing = sorted((server_counter - catalog_counter).elements(), key=signature_text)
    extra = sorted((catalog_counter - server_counter).elements(), key=signature_text)
    return {
        'missingFromCatalog': [signature_text(item) for item in missing],
        'extraInCatalog': [signature_text(item) for item in extra],
    }


def family_counts(routes):
    counts = Counter(route['family'] for route in routes)
    return dict(sorted(counts.items()))


def build_report(minio_root, worktree):
    catalog_routes = parse_catalog(worktree)
    server_routes = parse_minio_routes(minio_root)
    diff = compare_routes(catalog_routes, server_routes)
    return {
        'minioRoot': str(minio_root),
        'worktree': str(worktree),
        'sourceFiles': ROUTER_FILES,
        'summary': {
            'serverRouteCount': len(server_routes),
            'catalogRouteCount': len(catalog_routes),
            'serverFamilyCounts': family_counts(server_routes),
            'catalogFamilyCounts': family_counts(catalog_routes),
            'missingFromCatalog': len(diff['missingFromCatalog']),
            'extraInCatalog': len(diff['extraInCatalog']),
        },
        'diff': diff,
        'serverRoutes': server_routes,
        'catalogRoutes': catalog_routes,
    }


def render_markdown(report):
    summary = report['summary']
    lines = [
        '# MinIO 路由对照报告',
        '',
        '## 输入',
        '',
        '- MinIO 源码：`%s`' % report['minioRoot'],
        '- SDK 工作区：`%s`' % report['worktree'],
        '',
        '## 汇总',
        '',
        '| 项目 | 数量 |',
        '| --- | ---: |',
        '| 服务端路由 | %d |' % summary['serverRouteCount'],
        '| SDK catalog | %d |' % summary['catalogRouteCount'],
        '| catalog 缺失 | %d |' % summary['missingFromCatalog'],
        '| catalog 额外 | %d |' % summary['extraInCatalog'],
        '',
        '## 分组数量',
        '',
        '| family | 服务端 | catalog |',
        '| --- | ---: | ---: |',
    ]
    families = sorted(set(summary['serverFamilyCounts']) | set(summary['catalogFamilyCounts']))
    for family in families:
        lines.append('| %s | %d | %d |' % (family, summary['serverFamilyCounts'].get(family, 0), summary['catalogFamilyCounts'].get(family, 0)))
    lines.extend(['', '## 差异', ''])
    if summary['missingFromCatalog'] == 0 and summary['extraInCatalog'] == 0:
        lines.append('服务端 router 与 SDK catalog 的 family/method/path/query/auth 语义对照通过。')
    else:
        lines.append('### 服务端有但 catalog 缺失')
        lines.extend('- `%s`' % item for item in report['diff']['missingFromCatalog'])
        lines.append('')
        lines.append('### catalog 有但服务端未匹配')
        lines.extend('- `%s`' % item for item in report['diff']['extraInCatalog'])
    lines.extend(['', '## 说明', '', '- 条件路由、dummy/rejected route 会保留在报告字段或在抽取时排除，避免把不可用路由误算为产品 API。'])
    return '\n'.join(lines).strip() + '\n'


def baseline_json(report):
    return {
        'source': 'minio-router-baseline',
        'minioRoot': Path(report['minioRoot']).name,
        'sourceFiles': report['sourceFiles'],
        'routes': report['serverRoutes'],
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--minio-root', required=True)
    parser.add_argument('--worktree', required=True)
    parser.add_argument('--format', choices=['markdown', 'json', 'baseline-json'], required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()

    report = build_report(Path(args.minio_root), Path(args.worktree))
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    if args.format == 'markdown':
        output.write_text(render_markdown(report), encoding='utf-8')
    elif args.format == 'baseline-json':
        output.write_text(json.dumps(baseline_json(report), ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    else:
        output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

    if report['summary']['missingFromCatalog'] or report['summary']['extraInCatalog']:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
