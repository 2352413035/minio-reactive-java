#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""审计 Maven POM 的正式发布元数据，不修改 pom.xml。"""

import argparse
import json
import xml.etree.ElementTree as ET
from pathlib import Path

NS = {'m': 'http://maven.apache.org/POM/4.0.0'}

BASIC_FIELDS = [
    ('groupId', 'groupId'),
    ('artifactId', 'artifactId'),
    ('version', 'version'),
    ('name', 'name'),
    ('description', 'description'),
]

PUBLICATION_FIELDS = [
    ('url', 'url'),
    ('licenses', 'licenses/license'),
    ('scm', 'scm'),
    ('scm.url', 'scm/url'),
    ('scm.connection', 'scm/connection'),
    ('developers', 'developers/developer'),
    ('issueManagement', 'issueManagement/url'),
    ('organization', 'organization/name'),
    ('distributionManagement', 'distributionManagement'),
]

RELEASE_PLUGINS = [
    ('maven-source-plugin', '源码包插件'),
    ('maven-javadoc-plugin', 'javadoc 包插件'),
    ('maven-gpg-plugin', '签名插件'),
    ('cyclonedx-maven-plugin', 'SBOM 插件'),
]


def text_at(root, path):
    node = root.find('m:' + path.replace('/', '/m:'), NS)
    if node is None or node.text is None:
        return ''
    return node.text.strip()


def exists_at(root, path):
    return root.find('m:' + path.replace('/', '/m:'), NS) is not None


def plugin_artifacts(root):
    artifacts = set()
    for plugin in root.findall('.//m:plugin', NS):
        artifact = plugin.find('m:artifactId', NS)
        if artifact is not None and artifact.text:
            artifacts.add(artifact.text.strip())
    return artifacts


def audit_worktree(worktree):
    pom = Path(worktree) / 'pom.xml'
    tree = ET.parse(pom)
    root = tree.getroot()
    plugins = plugin_artifacts(root)

    basic = []
    for label, path in BASIC_FIELDS:
        value = text_at(root, path)
        basic.append({'name': label, 'present': bool(value)})

    publication = []
    for label, path in PUBLICATION_FIELDS:
        publication.append({'name': label, 'present': exists_at(root, path)})

    release_plugins = []
    for artifact, label in RELEASE_PLUGINS:
        release_plugins.append({'name': artifact, 'label': label, 'present': artifact in plugins})

    missing_publication = [item['name'] for item in publication if not item['present']]
    missing_plugins = [item['name'] for item in release_plugins if not item['present']]
    publish_ready = not missing_publication and not missing_plugins

    return {
        'worktree': str(worktree),
        'pom': str(pom),
        'basic': basic,
        'publication': publication,
        'releasePlugins': release_plugins,
        'publishReady': publish_ready,
        'missingPublicationMetadata': missing_publication,
        'missingReleasePlugins': missing_plugins,
    }


def yes_no(value):
    return '是' if value else '否'


def render_markdown(results):
    lines = ['# Maven 发布元数据预检报告', '']
    lines.append('本报告只审计发布元数据是否齐全，不代表可以发布 Maven，也不会修改 `pom.xml`。')
    lines.append('')
    for result in results:
        lines.append(f"## {result['worktree']}")
        lines.append('')
        lines.append(f"- POM：`{result['pom']}`")
        lines.append(f"- 是否具备正式发布元数据：{yes_no(result['publishReady'])}")
        lines.append('')
        lines.append('| 基础字段 | 是否存在 |')
        lines.append('| --- | --- |')
        for item in result['basic']:
            lines.append(f"| `{item['name']}` | {yes_no(item['present'])} |")
        lines.append('')
        lines.append('| 发布元数据 | 是否存在 |')
        lines.append('| --- | --- |')
        for item in result['publication']:
            lines.append(f"| `{item['name']}` | {yes_no(item['present'])} |")
        lines.append('')
        lines.append('| 发布插件 | 用途 | 是否存在 |')
        lines.append('| --- | --- | --- |')
        for item in result['releasePlugins']:
            lines.append(f"| `{item['name']}` | {item['label']} | {yes_no(item['present'])} |")
        lines.append('')
        if result['missingPublicationMetadata'] or result['missingReleasePlugins']:
            lines.append('缺失项：')
            for name in result['missingPublicationMetadata']:
                lines.append(f"- 发布元数据：`{name}`")
            for name in result['missingReleasePlugins']:
                lines.append(f"- 发布插件：`{name}`")
        else:
            lines.append('缺失项：无。')
        lines.append('')
    return '\n'.join(lines).rstrip() + '\n'


def main():
    parser = argparse.ArgumentParser(description='审计 Maven POM 发布元数据。')
    parser.add_argument('--worktree', action='append', required=True, help='需要审计的工作区路径，可重复。')
    parser.add_argument('--format', choices=['markdown', 'json'], required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()

    results = [audit_worktree(Path(path)) for path in args.worktree]
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    if args.format == 'json':
        output.write_text(json.dumps(results, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    else:
        output.write_text(render_markdown(results), encoding='utf-8')


if __name__ == '__main__':
    main()
