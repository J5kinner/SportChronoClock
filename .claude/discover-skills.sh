#!/bin/bash
# Injects project skill descriptions into Claude's context at session start.
# Runs from the project root; outputs hookSpecificOutput.additionalContext JSON.
[ -d ".claude/skills" ] && ls .claude/skills/*.md 2>/dev/null | grep -q . || exit 0

ctx="Project skills found in .claude/skills/ — read these files with the Read tool before doing relevant work:"
for f in .claude/skills/*.md; do
  name=$(awk '/^name:/{sub(/^name:[[:space:]]*/,"");print;exit}' "$f")
  desc=$(awk '/^description:/{sub(/^description:[[:space:]]*/,"");print;exit}' "$f")
  [ -n "$name" ] && ctx="${ctx}\n- ${name} (read .claude/skills/$(basename "$f")): ${desc}"
done

jq -n --arg ctx "$(printf "%b" "$ctx")" \
  '{"hookSpecificOutput":{"hookEventName":"SessionStart","additionalContext":$ctx}}'
