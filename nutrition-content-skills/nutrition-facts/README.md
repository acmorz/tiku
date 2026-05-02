# 营养事实库（你的 PDF 放这里）

> **不需要写任何内容**。你只要把 PDF / 电子书 / 指南文档丢进对应子目录，
> Claude 会根据 `00-索引.md` 自动优先从这里查证。

---

## 一、目录怎么放

```
nutrition-facts/
├── 00-索引.md                   ← Claude 入口，已写好
├── README.md                    ← 你正在看的这份说明
├── 01-权威指南/                 ← 把官方 PDF 丢这里
│   ├── 中国居民膳食指南2022.pdf            ← 你去放
│   ├── 中国2型糖尿病防治指南2020.pdf       ← 你去放
│   ├── 中国居民膳食营养素参考摄入量2023.pdf ← 你去放（可选）
│   └── WHO-Healthy-Diet.pdf                ← 你去放（可选）
└── 02-补充资料/                 ← 任何参考书 / 课件 / 教材丢这里
    ├── Basic-Health-Knowledge.pdf          ← 你去放
    └── 自己整理的笔记.md                    ← 想加什么加什么
```

---

## 二、PDF 命名规范（重要，Claude 靠文件名识别）

| 你下载的文件 | **请重命名为** |
|---|---|
| 任何"中国居民膳食指南"PDF | `中国居民膳食指南2022.pdf` |
| 任何"中国 2 型糖尿病防治指南"PDF | `中国2型糖尿病防治指南2020.pdf` |
| 任何"DRIs / 营养素参考摄入量"PDF | `中国居民膳食营养素参考摄入量2023.pdf` |
| WHO Healthy Diet 文档 | `WHO-Healthy-Diet.pdf` |
| 美国膳食指南 | `美国居民膳食指南2020-2025.pdf` |
| 其他任何指南 | `<主题>-<机构>-<年份>.pdf`，例如 `肥胖防治-中华医学会-2022.pdf` |

> 命名一致是为了让 `00-索引.md` 里的引用规则能直接生效。

---

## 三、PDF 去哪里下载（合法来源）

| 文件 | 官方下载入口 |
|---|---|
| 中国居民膳食指南 2022 | 中国营养学会官网 cnsoc.org（注册可下载摘要版）；正式书需购买《中国居民膳食指南（2022）》 人民卫生出版社 |
| 中国 2 型糖尿病防治指南 2020 | 中华医学会糖尿病学分会官网 diab.cma.org.cn；中华糖尿病杂志 2021 年第 4 期免费全文 |
| 中国居民膳食营养素参考摄入量（DRIs 2023） | 中国营养学会官网；纸质书《中国居民膳食营养素参考摄入量（2023 版）》 |
| WHO Healthy Diet | https://www.who.int/news-room/fact-sheets/detail/healthy-diet 直接保存为 PDF |
| WHO 糖摄入指南 | https://www.who.int/publications/i/item/9789241549028 |
| Basic Health Knowledge | https://github.com/quanbinn/Basic-Health-Knowledge-We-Need-To-Learn → Code → Download ZIP |

---

## 四、放好之后，让 Claude "看到" 它（一次性配置）

### 方法 A：项目根目录配置（推荐）

把 `nutrition-facts/` 目录复制（或软链接）到你日常做内容创作的工程根目录。然后在该工程根目录的 `CLAUDE.md` 里加这一段：

```markdown
## 知识源优先级（营养科普话稿）
做营养、减肥、饮食、喝水、作息相关话稿时：
1. 先读 `./nutrition-facts/00-索引.md`
2. 按主题打开对应子目录（01-权威指南 / 02-补充资料）
3. PDF 用 Read 工具直接读取（Claude 支持 PDF）
4. 本地未覆盖的内容才去联网或调用 pubmed-search
5. 引用时格式：`来源：本地事实库 / <文件名>`，并补充原始出处
```

### 方法 B：直接放进 Claude Code 全局 skills（更快）

```bash
# 把整个目录搬到 Claude 全局可见的位置
cp -r nutrition-content-skills/nutrition-facts ~/.claude/

# 在 ~/.claude/CLAUDE.md 里追加
echo '## 营养事实库
做营养科普时优先读 ~/.claude/nutrition-facts/00-索引.md。' >> ~/.claude/CLAUDE.md
```

---

## 五、验证 Claude 真的优先读了本地

放好 PDF 之后，新开一个对话问：

> "查一下《中国居民膳食指南 2022》对每日添加糖摄入的建议是多少？"

如果 Claude 回答时**先告诉你它在读哪个本地文件**（比如 "我从 nutrition-facts/01-权威指南/中国居民膳食指南2022.pdf 第 X 页查到……"），就说明配置成功了。

如果它直接联网搜索或者凭印象回答，去 `CLAUDE.md` 里检查那段提示是否生效。

---

## 六、维护建议

- 每季度看一下指南有没有新版本（一般 2-5 年更新一次）
- 自己被读者纠错的点，新建一个 `02-补充资料/我的纠错笔记.md` 记下来
- 接广 / 写到具体品牌时，把对应的成分检测报告 PDF 也放到 `02-补充资料/`，让 Claude 写稿时能交叉验证

---

## 七、暂时没找到 PDF 怎么办

不要紧。`00-索引.md` 里已经预置了引用兜底顺序：

```
本地事实库 → pubmed-search → CDC/WHO/NIH/中国营养学会官网 → 学会共识/教科书
```

Claude 会自动 fallback。但**长期建议你把上面 4 份核心 PDF 补齐**，本地查比联网快、准、不幻觉。
