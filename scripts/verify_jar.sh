#!/usr/bin/env bash
# Verify that the built jar contains a valid neoforge.mods.toml with the expected modid.
# Usage: verify_jar.sh <path-to-jar> [<expected-modid>]
# If <expected-modid> is omitted, reads it from ./gradle.properties (mod_id=...).
set -eu
JAR="${1:?usage: verify_jar.sh <path-to-jar> [<expected-modid>]}"

EXPECTED_MODID="${2:-}"
if [[ -z "$EXPECTED_MODID" ]] && [[ -f gradle.properties ]]; then
  EXPECTED_MODID="$(grep -E '^mod_id=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')"
fi
if [[ -z "$EXPECTED_MODID" ]]; then
  echo "[verify] ❌ Не удалось определить ожидаемый modid"; exit 1
fi

if [[ ! -f "$JAR" ]]; then
  echo "[verify] ❌ Jar не найден: $JAR"; exit 1
fi

SIZE=$(stat -c%s "$JAR" 2>/dev/null || stat -f%z "$JAR" 2>/dev/null || wc -c < "$JAR")
if [[ "$SIZE" -lt 5120 ]]; then
  echo "[verify] ❌ Jar слишком маленький ($SIZE байт) — возможно, сборка повреждена"; exit 1
fi

if ! command -v unzip >/dev/null 2>&1; then
  echo "[verify] ⚠️  unzip недоступен — пропускаю проверку содержимого"
  exit 0
fi

TOML_CONTENT="$(unzip -p "$JAR" META-INF/neoforge.mods.toml 2>/dev/null || true)"
if [[ -z "$TOML_CONTENT" ]]; then
  echo "[verify] ❌ В jar нет META-INF/neoforge.mods.toml"; exit 1
fi
if ! echo "$TOML_CONTENT" | grep -qE "modId\\s*=\\s*\"$EXPECTED_MODID\""; then
  echo "[verify] ❌ В neoforge.mods.toml не найден modId=\"$EXPECTED_MODID\""
  echo "$TOML_CONTENT" | head -20
  exit 1
fi

echo "[verify] ✅ Jar валиден ($SIZE байт, modId=$EXPECTED_MODID)"
