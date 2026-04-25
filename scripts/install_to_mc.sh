#!/usr/bin/env bash
# Backup existing <modid>-*.jar in mods/ and copy the new one in.
# Usage: install_to_mc.sh <path-to-new-jar> [<modid>]
# If <modid> is omitted, reads from ./gradle.properties (mod_id=...) or .mod-build.env.
set -eu
JAR="${1:?usage: install_to_mc.sh <path-to-jar> [<modid>]}"

MODID="${2:-}"
if [[ -z "$MODID" ]] && [[ -f .mod-build.env ]]; then
  # shellcheck disable=SC1091
  source .mod-build.env
fi
if [[ -z "${MODID:-}" ]] && [[ -f gradle.properties ]]; then
  MODID="$(grep -E '^mod_id=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')"
fi
if [[ -z "${MODID:-}" ]]; then
  echo "[install] ❌ Не удалось определить modid"; exit 1
fi

if [[ ! -f "$JAR" ]]; then
  echo "[install] ❌ Jar не найден: $JAR"; exit 1
fi

MODS_DIR="${MODS_DIR:-${APPDATA:-}/.minecraft/mods}"
MODS_DIR="$(echo "$MODS_DIR" | sed 's|\\|/|g')"

if [[ ! -d "$MODS_DIR" ]]; then
  echo "[install] ❌ MODS_DIR не существует: $MODS_DIR"; exit 1
fi

BACKUP_DIR="$MODS_DIR/.${MODID}-backup"
mkdir -p "$BACKUP_DIR"

# Move all existing <modid>-*.jar to backup (only this mod's jars; never touch other mods)
TS="$(date +%Y%m%d-%H%M%S)"
MOVED=0
for OLD in "$MODS_DIR"/${MODID}-*.jar; do
  [[ -e "$OLD" ]] || continue
  NAME="$(basename "$OLD")"
  mv "$OLD" "$BACKUP_DIR/${NAME}.${TS}.bak"
  echo "[install] 📦 Backup: $NAME → .${MODID}-backup/${NAME}.${TS}.bak"
  MOVED=$((MOVED+1))
done

# Trim backup: keep last 5
if command -v ls >/dev/null 2>&1; then
  cd "$BACKUP_DIR"
  # shellcheck disable=SC2012
  ls -1t ${MODID}-*.jar.*.bak 2>/dev/null | tail -n +6 | while read -r OLDB; do
    rm -f "$OLDB"
    echo "[install] 🗑️  Старый backup удалён: $OLDB"
  done
  cd - >/dev/null
fi

DEST="$MODS_DIR/$(basename "$JAR")"
cp "$JAR" "$DEST"
SIZE=$(stat -c%s "$DEST" 2>/dev/null || stat -f%z "$DEST" 2>/dev/null || wc -c < "$DEST")
SIZE_KB=$((SIZE / 1024))

echo "[install] ✅ Установлено: $DEST (${SIZE_KB} KB)"
echo "[install]    Backup'ов перенесено: $MOVED  (modid: $MODID)"
