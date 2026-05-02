#!/usr/bin/env bash
# 营养科普话稿创作 Skill 一键安装脚本
# 用法：bash nutrition-content-skills/scripts/install.sh
set -u

SKILLS_DIR="${HOME}/.claude/skills"
TMP="${TMPDIR:-/tmp}/nutrition-skills-install"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

mkdir -p "${SKILLS_DIR}" "${TMP}"

ok()    { printf "\033[32m[OK]\033[0m   %s\n" "$*"; }
warn()  { printf "\033[33m[WARN]\033[0m %s\n" "$*"; }
info()  { printf "\033[36m[..]\033[0m   %s\n" "$*"; }
fail()  { printf "\033[31m[FAIL]\033[0m %s\n" "$*"; }

clone_skill_subdir() {
  # $1 repo_url, $2 local tmp name, $3 source subpath, $4 dest skill name
  local repo="$1" name="$2" sub="$3" dest="$4"
  info "克隆 ${repo}"
  rm -rf "${TMP}/${name}"
  if git clone --depth 1 "${repo}" "${TMP}/${name}" >/dev/null 2>&1; then
    if [[ -d "${TMP}/${name}/${sub}" ]]; then
      mkdir -p "${SKILLS_DIR}/${dest}"
      cp -R "${TMP}/${name}/${sub}/." "${SKILLS_DIR}/${dest}/"
      ok "已安装 ${dest}"
    else
      warn "${repo} 中未找到 ${sub}，请进入 ${TMP}/${name} 手动定位 SKILL.md"
    fi
  else
    fail "克隆 ${repo} 失败"
  fi
}

echo "================================================================"
echo " 营养科普话稿创作 Skill 安装器"
echo " 目标目录：${SKILLS_DIR}"
echo "================================================================"

# 1. 5tldr/claude-skills（短视频/公众号/小红书）
for s in douyin-script wechat-article xiaohongshu-writer; do
  clone_skill_subdir "https://github.com/5tldr/claude-skills" "5tldr-skills" "skills/${s}" "${s}"
done

# 2. lidazhou/writing-assistant-skill
info "安装 writing-assistant"
rm -rf "${TMP}/writing-assistant"
if git clone --depth 1 https://github.com/lidazhou/writing-assistant-skill "${TMP}/writing-assistant" >/dev/null 2>&1; then
  mkdir -p "${SKILLS_DIR}/writing-assistant"
  cp -R "${TMP}/writing-assistant/." "${SKILLS_DIR}/writing-assistant/"
  # 用项目里写好的 about-me / user-personas 覆盖
  if [[ -f "${PROJECT_DIR}/writing-assistant/about-me.md" ]]; then
    cp "${PROJECT_DIR}/writing-assistant/about-me.md"      "${SKILLS_DIR}/writing-assistant/about-me.md"
    cp "${PROJECT_DIR}/writing-assistant/user-personas.md" "${SKILLS_DIR}/writing-assistant/user-personas.md"
    ok "已覆盖人设和用户画像（about-me.md / user-personas.md）"
  fi
  ok "已安装 writing-assistant"
else
  fail "克隆 writing-assistant-skill 失败"
fi

# 3. alirezarezvani/claude-skills
for s in content-production content-strategy; do
  clone_skill_subdir "https://github.com/alirezarezvani/claude-skills" "arz-skills" "marketing-skill/${s}" "${s}"
done

# 4. tw93/Waza
info "安装 tw93/Waza（write/learn/check/hunt）"
if command -v npx >/dev/null 2>&1; then
  npx -y skills add tw93/Waza -a claude-code -g -y || warn "Waza 全量安装失败，试试单装：npx skills add tw93/Waza -a claude-code -s write -y"
  ok "Waza 安装尝试完成"
else
  warn "未检测到 npx，跳过 Waza。请先安装 Node.js 18+ 再手动执行：npx skills add tw93/Waza -a claude-code -g -y"
fi

# 5. food-database-query
clone_skill_subdir "https://github.com/huifer/WellAlly-health" "wellally" "skills/food-database-query" "food-database-query"

# 6. nutrition-advisor + pubmed-search（family-doctor 目录结构可能调整）
info "克隆 family-doctor，自动探测 nutrition-advisor / pubmed-search 位置"
rm -rf "${TMP}/family-doctor"
if git clone --depth 1 https://github.com/Yungho/family-doctor "${TMP}/family-doctor" >/dev/null 2>&1; then
  for s in nutrition-advisor pubmed-search; do
    found="$(find "${TMP}/family-doctor" -type d -name "${s}" -print -quit 2>/dev/null || true)"
    if [[ -n "${found}" ]]; then
      mkdir -p "${SKILLS_DIR}/${s}"
      cp -R "${found}/." "${SKILLS_DIR}/${s}/"
      ok "已安装 ${s}"
    else
      warn "未在 family-doctor 中找到 ${s}，请手动到 ${TMP}/family-doctor 查找"
    fi
  done
else
  fail "克隆 family-doctor 失败"
fi

# 7. fact-check
info "安装 petar-nauka/fact-check-skill"
rm -rf "${TMP}/fact-check"
if git clone --depth 1 https://github.com/petar-nauka/fact-check-skill "${TMP}/fact-check" >/dev/null 2>&1; then
  mkdir -p "${SKILLS_DIR}/fact-check"
  [[ -f "${TMP}/fact-check/SKILL.md" ]]            && cp "${TMP}/fact-check/SKILL.md"            "${SKILLS_DIR}/fact-check/"
  [[ -f "${TMP}/fact-check/educational-tips.md" ]] && cp "${TMP}/fact-check/educational-tips.md" "${SKILLS_DIR}/fact-check/"
  ok "已安装 fact-check"
else
  fail "克隆 fact-check-skill 失败"
fi

# 8. daymade fact-checker（通过 claude CLI）
info "安装 daymade/claude-code-skills 的 fact-checker"
if command -v claude >/dev/null 2>&1; then
  claude plugin marketplace add https://github.com/daymade/claude-code-skills 2>/dev/null || true
  claude plugin install fact-checker@daymade-skills 2>/dev/null || warn "claude plugin install 失败，可手动 git clone https://github.com/daymade/claude-code-skills 后拷贝 fact-checker/ 目录"
else
  warn "未检测到 claude CLI，跳过 daymade fact-checker。可改用 git clone https://github.com/daymade/claude-code-skills 然后 cp -r fact-checker ~/.claude/skills/"
fi

echo ""
echo "================================================================"
echo " 已安装的 Skill："
ls -1 "${SKILLS_DIR}" 2>/dev/null | sed 's/^/   - /'
echo "================================================================"
echo ""
ok "全部安装完成。下一步："
echo ""
echo "   1) 把指南 PDF 放进事实库（按命名表）："
echo "        ${PROJECT_DIR}/nutrition-facts/01-权威指南/"
echo "      详见 ${PROJECT_DIR}/nutrition-facts/README.md"
echo ""
echo "   2) 让 Claude 优先从本地查（二选一）："
echo "      A. 复制到你的工程根目录，在 CLAUDE.md 里加："
echo "         '做营养科普话稿时优先读 ./nutrition-facts/00-索引.md'"
echo "      B. 全局可见："
echo "         cp -r ${PROJECT_DIR}/nutrition-facts ~/.claude/"
echo "         echo '## 营养事实库" >&2
echo "         做营养科普时优先读 ~/.claude/nutrition-facts/00-索引.md。' >> ~/.claude/CLAUDE.md"
echo ""
echo "   3) 微调人设和画像："
echo "        ${SKILLS_DIR}/writing-assistant/about-me.md"
echo "        ${SKILLS_DIR}/writing-assistant/user-personas.md"
