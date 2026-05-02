# 一键投喂给 Claude 的安装清单

> 直接把整份内容粘到 Claude Code / Cursor 的对话框里，让它按顺序执行。
> 也可以直接 `bash nutrition-content-skills/scripts/install.sh`。

---

## 给 Claude 的提示词（直接复制下面这一整块）

```
你好 Claude，请帮我安装以下 8 个用于"营养科普话稿创作"的 Skill。
我的环境是 Claude Code。请按顺序执行，每装完一个简短确认一下，遇到失败先继续后面的，最后汇总。

=== 第一组：写稿模板（最核心）===

1. 5tldr/claude-skills（中文短视频/公众号/小红书脚本套件）
   执行：/plugin add https://github.com/5tldr/claude-skills
   关键 Skill：douyin-script、wechat-article、xiaohongshu-writer

2. lidazhou/writing-assistant-skill（自媒体写作五步法 + 检查清单）
   执行：
   git clone https://github.com/lidazhou/writing-assistant-skill /tmp/writing-assistant
   mkdir -p ~/.claude/skills/writing-assistant
   cp -r /tmp/writing-assistant/* ~/.claude/skills/writing-assistant/
   然后把我项目里 nutrition-content-skills/writing-assistant/about-me.md 和 user-personas.md
   覆盖到 ~/.claude/skills/writing-assistant/ 同名文件。

3. alirezarezvani/claude-skills 中的 content-production 和 content-strategy
   执行：
   git clone --depth 1 https://github.com/alirezarezvani/claude-skills /tmp/arz-skills
   mkdir -p ~/.claude/skills/content-production ~/.claude/skills/content-strategy
   cp -r /tmp/arz-skills/marketing-skill/content-production/* ~/.claude/skills/content-production/
   cp -r /tmp/arz-skills/marketing-skill/content-strategy/*   ~/.claude/skills/content-strategy/

=== 第二组：写作辅助（口语化 + 研究流程）===

4. tw93/Waza（/write 把 AI 腔改成自然中文；/learn 六步研究；/check 终审；/hunt 排查）
   执行：npx skills add tw93/Waza -a claude-code -g -y
   如果上面命令失败：
   npx skills add tw93/Waza -a claude-code -s write -y
   npx skills add tw93/Waza -a claude-code -s learn -y
   npx skills add tw93/Waza -a claude-code -s check -y

=== 第三组：营养专业支撑 ===

5. huifer/WellAlly-health 的 food-database-query（50 种食物 × 30+ 营养素）
   执行：
   git clone --depth 1 https://github.com/huifer/WellAlly-health /tmp/wellally
   mkdir -p ~/.claude/skills/food-database-query
   cp -r /tmp/wellally/skills/food-database-query/* ~/.claude/skills/food-database-query/

6. Yungho/family-doctor 的 nutrition-advisor + pubmed-search
   执行：
   git clone --depth 1 https://github.com/Yungho/family-doctor /tmp/family-doctor
   mkdir -p ~/.claude/skills/nutrition-advisor ~/.claude/skills/pubmed-search
   cp -r /tmp/family-doctor/skills/nutrition-advisor/* ~/.claude/skills/nutrition-advisor/ 2>/dev/null || true
   cp -r /tmp/family-doctor/skills/pubmed-search/*    ~/.claude/skills/pubmed-search/ 2>/dev/null || true
   （目录结构如有变动，请在 family-doctor 仓库内自行定位 SKILL.md 后拷贝）

=== 第四组：审稿与事实核查 ===

7. petar-nauka/fact-check-skill（11 步事实核查 + HTML 报告）
   执行：
   mkdir -p ~/.claude/skills/fact-check
   git clone --depth 1 https://github.com/petar-nauka/fact-check-skill /tmp/fact-check
   cp /tmp/fact-check/SKILL.md             ~/.claude/skills/fact-check/
   cp /tmp/fact-check/educational-tips.md  ~/.claude/skills/fact-check/

8. daymade/claude-code-skills 的 fact-checker（文档级事实核查）
   执行：
   claude plugin marketplace add https://github.com/daymade/claude-code-skills
   claude plugin install fact-checker@daymade-skills

=== 收尾 ===

请确认：
- 所有 Skill 都已落到 ~/.claude/skills/ 下面
- 列出我现在已经装的 Skill 名称（用 ls ~/.claude/skills/）
- 把我项目里的 nutrition-content-skills/nutrition-facts/ 路径加进 CLAUDE.md，
  说明"做营养科普话稿时优先从这里查证"
```

---

## 验证安装

```bash
ls ~/.claude/skills/
# 应该至少包含：
# douyin-script  wechat-article  xiaohongshu-writer
# writing-assistant  content-production  content-strategy
# food-database-query  nutrition-advisor  pubmed-search
# fact-check  fact-checker
```

---

## 备选 Skill（按需追加，不是必装）

| Skill | 何时再装 | 一行安装 |
|---|---|---|
| `K-Dense-AI/scientific-agent-skills` | 要画营养信息图 / 引用管理 | `git clone https://github.com/K-Dense-AI/scientific-agent-skills` |
| MedSci Skills 的 `search-lit` | 要严格反幻觉验 PubMed PMID | 见 awesome-claude-code Issue #1518 |
| `ceeon/videocut-skills` | 后期要自动剪口播视频 | `git clone https://github.com/ceeon/videocut-skills` |
| `quanbinn/Basic-Health-Knowledge-We-Need-To-Learn` | 想加更多基础知识进事实库 | `git clone https://github.com/quanbinn/Basic-Health-Knowledge-We-Need-To-Learn` |
