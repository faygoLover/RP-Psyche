# RP Psyche

Мод Minecraft Forge 1.20.1 (Java 17): стресс и рассудок, аддон к RP Medicine. Замена Sanity: Descent Into Madness
(`sanitydim`). Автор — faygoLover (Alex). **С автором общаемся по-русски.**

Мод `rppsyche`, «RP Psyche», пакет `faygolover.rppsyche`, MIT, языки ru и en. Требует **RP Core 0.11.0+** и **RP
Medicine 0.8.1+**; RP Perks (черты), RP PDA (речь), TaCZ, Simple Voice Chat, Zone Artifacts — мягко.

## Состояние
- План, решения автора и этапы — `docs/00_plan.md` (§12 — решения, §11 — этапы, «Этап 1 — как сделано»).
- **20.10.2026, 0.1.0 — этап 1 (ядро).** В игре не проверено.
- **06.11.2026, 0.3.0 — баланс (одиночество с потолком, равновесие стресса, честный разговор с собой) и признаки в
  осмотре Medicine.** Разбор — `docs/01_review_2026-11-06.md`. В игре не проверено, в сборку не положено.
- Разбор Sanity (декомпиляция) — `_mcref/mods/sanitydim-1.1.0`.

## Сборка
JDK 17: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.14.7-hotspot"`, затем `gradlew.bat build` → `build/libs`.
- `libs/`: jar RP Core, Medicine, Perks, PDA, Zone Artifacts (обновлять копией свежих сборок). ForgeGradle кэширует
  jar по версии — после пересборки соседа с той же версией удалить
  `~/.gradle/caches/forge_gradle/deobf_dependencies/blank/<мод>`.
- Клиент разработки: `gradlew.bat runClient` (с Perks и PDA; `-Pno_perks=true`, `-Pno_pda=true` — без них,
  `-Pwith_za=true` — с Zone Artifacts, `-Pwith_pack=true` — jar из `libs/pack/*-1.jar`: TaCZ, Simple Voice Chat…).
- Переводы — `python scripts/gen_lang.py` (руками JSON в `lang/` не править).

## Устройство
- `core/` — без Minecraft: `Settings` (все числа), `PsycheState` (что сохраняется), `Psyche.step` (шаг за секунду:
  стресс с толчками и полом от рассудка, рассудок от стресса, Пси-зоны, разовых ударов), `Stutter` (заикание).
- `config/PsycheConfig` — `rppsyche-server.toml`: поля `Settings` по имени (camelCase → snake_case), комментарии
  по-русски. Новое число — поле в `Settings` + строка `bind(...)` здесь.
- `capability/PsycheData` — психика игрока (NBT) и служебное (толчки до шага, голос, что ушло клиенту).
- `server/PsycheService` — раз в секунду: тело (боль, ломка, седация, опьянение, клиническая смерть — через
  `integration/MedicineCompat`), голод и жажда (Core `NutritionProvider`), полная тьма (`Lights`), люди вокруг
  (одиночество, компания, заразность, двусторонний разговор), лежачие рядом, Пси-зона, разговор с собой; события —
  урон, взрыв, сон, еда; синхронизация своему клиенту; ощущения в строке Medicine (`BodyProvider.sensation`); фильтр
  речи (заикание) для Core → PDA.
- `server/PsycheCommands` — `/rppsyche get|set|add` (уровень прав 2).
- `integration/` — `TaczCompat` (выстрелы и пули рядом лучом от стрелка), `ZaCompat` (Пси-зона),
  `voice/PsycheVoicePlugin` + `VoiceTracker` (кто говорит; слышит ли кто — решает сервер).
- `client/` — `PsycheFx` (шейдер `assets/minecraft/shaders/program/rppsyche_psyche.*`, прожилки), `PhantomSounds`
  (сердцебиение, гул, шаги, звуки рядом, далёкие выстрелы), `Thoughts` (мысли), `Tremor` (дрожь взгляда).
- Ассеты из Sanity (звуки сердцебиения, гула, шороха, текстура прожилок, шейдеры) и TaCZ (далёкие выстрелы).

## Правила работы
Как в остальных RP-модах: делаю только согласованное (план), сам тесты и игру не запускаю — собираю, даю автору план
проверки, он смотрит в игре. Комментарии и строки конфига — по-русски. Коммиты — только по просьбе автора.
