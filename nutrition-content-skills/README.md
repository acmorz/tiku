# 营养科普话稿创作工作台 (Nutrition Content Skills)

> 面向 **18-30 岁青年女性** 的营养科普内容生产工具集 —— 写稿 / 营养支撑 / 审稿 一站式
> 主题覆盖：**减肥、饮食、喝水、作息**

---

## 目录结构

```
nutrition-content-skills/
├── README.md                         ← 你正在看的这份
├── INSTALL.md                        ← 一键投喂给 Claude 的安装清单
├── scripts/
│   └── install.sh                    ← Bash 一键安装脚本
├── writing-assistant/                ← writing-assistant-skill 的人设和受众配置
│   ├── about-me.md                   ← 你的创作者人设（已填好，可微调）
│   └── user-personas.md              ← 3 类目标用户画像（已填好）
└── nutrition-facts/                  ← 本地营养事实库（你只放 PDF）
    ├── README.md                     ← 告诉你 PDF 怎么放、去哪下、怎么命名
    ├── 00-索引.md                    ← Claude 入口（已写好，无需改）
    ├── 01-权威指南/                   ← 把官方 PDF 丢这里
    └── 02-补充资料/                   ← 教材、笔记、补充读物
```

---

## 三步上手

### 第 1 步：装 Skill

把 `INSTALL.md` 整段提示词粘给 Claude（Claude Code / Cursor 都行），它会按顺序帮你把 8 个 Skill 全装到 `~/.claude/skills/`。也可以直接执行：

```bash
bash nutrition-content-skills/scripts/install.sh
```

脚本会自动把项目里的 `about-me.md` 和 `user-personas.md` 覆盖到 `~/.claude/skills/writing-assistant/` 下。

### 第 2 步：放指南 PDF（事实库）

进入 `nutrition-content-skills/nutrition-facts/`，按 `README.md` 里的命名表把 PDF 丢进去：

| 文件名 | 必须放？ |
|---|---|
| `01-权威指南/中国居民膳食指南2022.pdf` | ✅ 强烈建议 |
| `01-权威指南/中国2型糖尿病防治指南2020.pdf` | ✅ 强烈建议 |
| `01-权威指南/中国居民膳食营养素参考摄入量2023.pdf` | ⭕ 推荐 |
| `01-权威指南/WHO-Healthy-Diet.pdf` | ⭕ 推荐 |
| `02-补充资料/Basic-Health-Knowledge.pdf` 或目录 | ⭕ 可选 |

下载入口和命名规范都在 `nutrition-facts/README.md` 里。

### 第 3 步：让 Claude 优先从本地查

把整个 `nutrition-facts/` 目录复制（或软链接）到你日常做内容创作的工程根目录，然后在该工程的 `CLAUDE.md` 加这段：

```markdown
## 知识源优先级（营养科普话稿）
做营养、减肥、饮食、喝水、作息相关话稿时：
1. 先读 `./nutrition-facts/00-索引.md`
2. 按主题打开对应 PDF（用 Read 工具直接读，Claude 支持 PDF）
3. 本地未覆盖才联网或调用 pubmed-search
4. 引用时格式：`来源：本地事实库 / <文件名>` + 原始出处
```

或者直接放到全局 `~/.claude/`，让所有项目都能用：

```bash
cp -r nutrition-content-skills/nutrition-facts ~/.claude/
echo '## 营养事实库
做营养科普时优先读 ~/.claude/nutrition-facts/00-索引.md。' >> ~/.claude/CLAUDE.md
```

---

## 推荐工作流

```
[选题]   /learn (Waza)  →  content-strategy
              ↓
[备料]   读 nutrition-facts/00-索引.md  →  打开对应 PDF  →  food-database-query  →  pubmed-search
              ↓
[出稿]   douyin-script / wechat-article / xiaohongshu-writer
              ↓ (套用 writing-assistant 的人设和画像)
[润色]   /write (Waza)  把 AI 腔改成口语
              ↓
[审稿]   fact-check-skill  →  pubmed-search 验 PMID  →  writing-assistant 检查清单 ≥80 分
              ↓
[终审]   /check (Waza)  →  发布
```

---

## 适用场景示例

| 你说 | Claude 会做什么 |
|---|---|
| "帮我写一个 60 秒口播：奶茶到底有多少糖" | 加载 `douyin-script` + 打开 `中国居民膳食指南2022.pdf` 控糖部分 + `WHO-Healthy-Diet.pdf` 游离糖 + 套用画像 1（精致打工女） |
| "审一下这篇'断食 16 小时暴瘦'的稿子" | 加载 `fact-check-skill` + 读《膳食指南》《2 型糖尿病指南》交叉验证 |
| "下周 5 篇小红书选题" | 加载 `content-strategy` + 用户画像 + 月度热点（经期、节假日饮食等） |

---

## 维护建议

- 每季度刷新一次 `nutrition-facts/01-权威指南/` PDF（指南更新频率约 2-5 年）
- 被读者纠错的点 → 新建 `nutrition-facts/02-补充资料/我的纠错笔记.md`
- `about-me.md` 的"禁区"部分至少每两个月复盘一次
