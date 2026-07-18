#!/bin/sh
# git/GitHub 쓰기 위임(D-026)의 로컬 가드 — PreToolUse(Bash) 훅
# 형식·안전 규칙을 기억이 아니라 장치로 강제한다 (D-023 철학).
# 차단 시 exit 2 + stderr 사유 (에이전트가 읽고 수정 후 재시도).

input=$(cat)
cmd=$(printf '%s' "$input" | jq -r '.tool_input.command // empty' 2>/dev/null)
[ -z "$cmd" ] && exit 0

block() { printf '차단(D-026 가드): %s\n' "$1" >&2; exit 2; }

# 따옴표 인자 추출: extract_arg <옵션1> [옵션2 ...] → 먼저 매치되는 옵션의 따옴표 내부 값
extract_arg() {
  for opt in "$@"; do
    v=$(printf '%s' "$cmd" | sed -nE "s/.*$opt[[:space:]]+\"([^\"]+)\".*/\1/p" | head -1)
    [ -z "$v" ] && v=$(printf '%s' "$cmd" | sed -nE "s/.*$opt[[:space:]]+'([^']+)'.*/\1/p" | head -1)
    if [ -n "$v" ]; then printf '%s' "$v"; return; fi
  done
}

# ── 1) git push 가드 ─────────────────────────────────────────
if printf '%s' "$cmd" | grep -qE '(^|[;&|][[:space:]]*)git([[:space:]]+-C[[:space:]]+[^[:space:]]+)?[[:space:]]+push'; then
  printf '%s' "$cmd" | grep -qE 'push[^;&|]*([[:space:]]|:)main([[:space:]]|$)' \
    && block "main 직접 push 금지 — develop 대상 PR로 병합 (D-025)"
  printf '%s' "$cmd" | grep -qE '(--force|[[:space:]]-f)([[:space:]]|$)' \
    && block "--force push 금지 (--force-with-lease만 허용)"
  printf '%s' "$cmd" | grep -qE 'push[^;&|]*[[:space:]]origin[[:space:]]+[^-[:space:]]' \
    || block "push 대상 명시 필수 — 예: git push -u origin feature/12-rss-feed-adapter"
fi

# ── 2) git commit 메시지 가드 ────────────────────────────────
if printf '%s' "$cmd" | grep -qE '(^|[;&|][[:space:]]*)git([[:space:]]+-C[[:space:]]+[^[:space:]]+)?[[:space:]]+commit'; then
  printf '%s' "$cmd" | grep -qiE 'co-authored-by|generated with' \
    && block "커밋 트레일러·에이전트 서명 금지 — 기록은 사용자 명의 (D-026)"
  if ! printf '%s' "$cmd" | grep -qE -- '--amend'; then
    msg=$(extract_arg '-m')
    [ -z "$msg" ] && block "커밋 메시지는 -m \"...\" 한 줄로 — 형식: {type}: {한국어 요약}"
    printf '%s' "$msg" | grep -qE '^(feat|fix|refactor|test|chore|build|docs): .+' \
      || block "커밋 메시지 형식 위반 — {feat|fix|refactor|test|chore|build|docs}: {한국어 요약} (D-014 형식 유지)"
  fi
fi

# ── 3) gh issue/pr create 가드 ───────────────────────────────
if printf '%s' "$cmd" | grep -qE 'gh[[:space:]]+(issue|pr)[[:space:]]+create'; then
  title=$(extract_arg '--title' '-t')
  [ -z "$title" ] && block "제목 명시 필수 — --title \"[FEAT] ...\" 형식 (템플릿 관행)"
  printf '%s' "$title" | grep -qE '^\[(FEAT|CHORE|FIX|REFACTOR|DOCS|feat|chore|fix|refactor|docs)\] ' \
    || block "제목 접두어 위반 — [FEAT|CHORE|FIX|REFACTOR|docs] 요약 형식 (이슈 #10·#12 관행)"

  if printf '%s' "$cmd" | grep -qE 'gh[[:space:]]+pr[[:space:]]+create'; then
    bodyfile=$(extract_arg '--body-file' '-F')
    if [ -n "$bodyfile" ] && [ -f "$bodyfile" ]; then
      grep -qiE 'close[sd]? #[0-9]+' "$bodyfile" \
        || block "PR 본문에 'close #{이슈번호}' 필요 (PR 템플릿 관행)"
    else
      printf '%s' "$cmd" | grep -qiE 'close[sd]? #[0-9]+' \
        || block "PR 본문에 'close #{이슈번호}' 필요 (PR 템플릿 관행)"
    fi
  fi
fi

exit 0
