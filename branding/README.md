# Branding — AlaAggro

Visual assets for CurseForge, GitHub, and social platforms.

> **In-game textures, models, and sounds live in `src/main/resources/assets/alaaggro/` — that's NeoForge convention. This folder is for OUTSIDE-game branding only.**
>
> Полное руководство: `../_docs/12_BRANDING_GUIDE.md` (или `../../_docs/12_BRANDING_GUIDE.md` если studio root доступен).

## Структура

| Папка | Что |
|---|---|
| `icons/` | Квадратные иконки мода (`icon-source`, `icon-512`, `icon-400`, `icon-256`, `icon-128`, `icon-64`) |
| `banners/` | Широкоформатные баннеры (`banner-1280x720` для CF cover, `banner-1920x1080` для соцсетей) |
| `logos/` | Текстовые логотипы с названием мода (light/dark вариант) — опц. |
| `screenshots/` | In-game скриншоты для CF gallery (`01-overview`, `02-config`, …) |
| `source/` | Исходники в Photoshop / Affinity / Figma / Inkscape — опц. |

## Минимум для первого release

- [ ] `icons/icon-400.png` — 400×400 квадрат, для CF page (обязательно)
- [ ] `icons/icon-source.png` — ≥1024×1024, source для resize
- [ ] `banners/banner-1280x720.png` — 16:9, для CF cover
- [ ] `../src/main/resources/logo.png` — копия `icons/icon-256.png` (для in-game Mods screen)
- [ ] EXIF/metadata очищены: `exiftool -all= icons/*.png banners/*.png`

## Опционально (повышает качество страницы)

- [ ] `screenshots/01-overview.png`, `02-…`, `03-…` — 3+ скриншотов с фичами мода
- [ ] `icons/icon-512.png` — для GitHub social preview
- [ ] `logos/logo-light.png` + `logo-dark.png` — текстовый логотип для разных фонов

## ⚠️ NO AI TRACES в изображениях

AI-генераторы (Midjourney, DALL-E, SD) встраивают signatures в EXIF. Перед коммитом:
```bash
exiftool -all= branding/icons/*.png branding/banners/*.png branding/screenshots/*.png
```

Или re-save через любой image editor (GIMP/Photoshop/Affinity не сохраняют чужой EXIF).

`_scripts/audit-no-ai-traces.sh <mod>` проверяет EXIF (если установлен exiftool).

## Текущая ревизия

- Дата создания: <fill in YYYY-MM-DD>
- Source: <ссылка на облачный проект, если есть>
- Designer: <автор иконок>
