# 阶段 114：破坏性边界机器报告

## 目标

阶段 113 已用文档解释 `destructive-blocked = 29` 的风险分类。阶段 114 将这套分类固化为脚本，方便后续每次发布复审生成稳定证据。

## 新增脚本

```bash
scripts/report-destructive-boundary.py \
  --worktree . \
  --format markdown \
  --output .omx/reports/destructive-boundary-jdk8.md
```

也支持 JSON：

```bash
scripts/report-destructive-boundary.py \
  --worktree . \
  --format json \
  --output .omx/reports/destructive-boundary-jdk8.json
```

脚本只输出静态分类，不连接 MinIO，也不执行任何写入。

## 当前分类

| 分类 | 数量 | 含义 |
| --- | ---: | --- |
| 已有独立 lab 证据 | 11 | typed/raw 已在一次性 Docker lab 或双容器 lab 证明。 |
| 可回滚候选 | 1 | 有机会继续设计独立 lab，但当前还缺恢复策略。 |
| 拓扑或身份提供方 | 7 | 需要独立 IDP、多站点或复制拓扑。 |
| 维护窗口 | 5 | 会影响服务可用性、升级或锁语义。 |
| 资源压测 | 5 | 会消耗对象、磁盘、网络或站点复制资源。 |

## 发布门禁变化

后续发布复审除了 capability matrix，还应生成 destructive boundary 报告。能力矩阵回答“还有多少高风险边界”，本脚本回答“这些边界为什么仍然存在”。

## 不变边界

- 本阶段不降低 `destructive-blocked`。
- 不在共享 MinIO 执行破坏性操作。
- 不把维护窗口级操作伪装成普通集成测试。
