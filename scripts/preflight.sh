#!/usr/bin/env bash
# Pre-flight checks for any NeoForge mod from the studio template.
# Run from the mod root (where build.gradle and gradle.properties live).
# Exits 0 if all green, 1 on hard fail, 2 on warnings only.
set -u

ROOT="$(pwd)"
WARN=0
FAIL=0

say()  { echo "[preflight] $*"; }
warn() { echo "[preflight] ⚠️  $*"; WARN=1; }
fail() { echo "[preflight] ❌ $*"; FAIL=1; }
ok()   { echo "[preflight] ✅ $*"; }

# 1. Mod root
if [[ ! -f "build.gradle" ]] || [[ ! -f "gradle.properties" ]]; then
  fail "build.gradle / gradle.properties не найдены — запусти из корня мод-папки"
  exit 1
fi
ok "Корень мод-проекта найден"

# 2. mod_id + mod_version from gradle.properties
MODID="$(grep -E '^mod_id=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')"
VERSION="$(grep -E '^mod_version=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')"
if [[ -z "$MODID" ]]; then fail "mod_id не задан в gradle.properties"; exit 1; fi
if [[ -z "$VERSION" ]]; then fail "mod_version не задан в gradle.properties"; exit 1; fi
ok "Mod: $MODID  Version: $VERSION"
echo "MODID=$MODID" > "$ROOT/.mod-build.env"
echo "MOD_VERSION=$VERSION" >> "$ROOT/.mod-build.env"

# 3. Minecraft mods folder
MODS_DIR="${APPDATA:-}/.minecraft/mods"
MODS_DIR_UNIX="$(echo "$MODS_DIR" | sed 's|\\|/|g')"
if [[ -z "${APPDATA:-}" ]]; then
  fail "APPDATA не определена"; exit 1
fi
if [[ ! -d "$MODS_DIR_UNIX" ]]; then
  fail "Папка mods не найдена: $MODS_DIR — запусти Minecraft хотя бы раз"
  exit 1
fi
ok "Папка mods: $MODS_DIR_UNIX"
echo "MODS_DIR=$MODS_DIR_UNIX" >> "$ROOT/.mod-build.env"

# 4. JDK 25 (NeoForge 26.1 requirement)
if ! command -v java >/dev/null 2>&1; then
  fail "java не найден в PATH"; exit 1
fi
JAVA_VER="$(java -version 2>&1 | head -n1 | grep -oE '"[0-9]+' | tr -d '"')"
if [[ "$JAVA_VER" != "25" ]]; then
  warn "JDK $JAVA_VER (NeoForge 26.1 требует JDK 25 — https://docs.neoforged.net/docs/gettingstarted/)"
else
  ok "JDK 25"
fi

# 5. Lang files consistency (auto-detected locales)
LANG_DIR="src/main/resources/assets/$MODID/lang"
if [[ -d "$LANG_DIR" ]]; then
  mapfile -t LOCALES < <(find "$LANG_DIR" -maxdepth 1 -type f -name '*.json' -exec basename {} .json \; | sort)
  if [[ ${#LOCALES[@]} -eq 0 ]]; then
    warn "В $LANG_DIR нет .json файлов локализации"
  else
    TMP="$(mktemp -d)"
    for L in "${LOCALES[@]}"; do
      grep -oE '"[^"]+":' "$LANG_DIR/$L.json" | sort -u > "$TMP/$L.keys"
    done
    BASE="${LOCALES[0]}"
    DIFF_OUT=""
    for L in "${LOCALES[@]}"; do
      [[ "$L" == "$BASE" ]] && continue
      D="$(diff "$TMP/$BASE.keys" "$TMP/$L.keys" 2>/dev/null)"
      if [[ -n "$D" ]]; then
        DIFF_OUT+="[$BASE vs $L]"$'\n'"$D"$'\n'
      fi
    done
    if [[ -n "$DIFF_OUT" ]]; then
      warn "Lang-файлы расходятся:"
      echo "$DIFF_OUT" | sed 's/^/    /'
    else
      ok "Lang-файлы консистентны (${#LOCALES[@]} локалей: $(IFS=, ; echo "${LOCALES[*]}"))"
    fi
    rm -rf "$TMP"
  fi
else
  warn "Каталог lang не найден: $LANG_DIR"
fi

if [[ $FAIL -ne 0 ]]; then exit 1; fi
if [[ $WARN -ne 0 ]]; then exit 2; fi
exit 0
