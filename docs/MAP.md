# RP Psyche — карта

Версия на момент написания: 0.5.4 (16.11.2026). Факты по классам, событиям, пакетам — `docs/INDEX.md`.

## 1. Что делает и чего не делает

Психика персонажа: **стресс** (0–100, гибкий — растёт за десятки секунд, спадает за минуты) и **рассудок**
(0–100, инертный — портится за часы, лечится днями). Давят: полная тьма, одиночество, бой (выстрелы, пули
рядом, взрывы, свой урон), боль и ломка, лежачие рядом, голод и жажда, недосып, Пси-зона, чужая паника.
Успокаивают: компания и разговор, сон, еда, курение и выпивка, досуг (музыка, настольные игры, дневник,
животное), беседа с психологом, антипсихотик. Проявления — у самого игрока: выцветший мир, звуки, которых нет,
мысли, дрожь рук, заикание в речи. Замена мода Sanity.

За этим — не сюда:
- сами боль, ломка, опьянение, нокдаун — мод тела (здесь только чтение через `integration/BodyCompat`);
- фонари — RP Lights; здесь только вопрос «несёт ли свет» (`RpCoreAPI.carriesLight`);
- сон и недосып — RP Needs; еда — RP Culinary (сюда приходят события);
- давить на психику из другого мода — не импортом: `RpCoreAPI.stressImpulse`, `sanityHit`, `RpMoodEvent`.

## 2. Как течёт работа

**Шаг раз в секунду.** `server/PsycheService.onPlayerTick` → `step(игрок, данные, настройки)`: собирает входы
(`core/Psyche.Inputs`) строго по порядку — черты → `bodyStep` (тело) → `needsStep` (голод, жажда, недосып,
температура) → `darknessStep` (`server/Lights.inTotalDarkness`) → `peopleStep` (одиночество, компания, заразность
паники, двусторонний разговор) → `downedStep` (лежачие в поле зрения) → `psiStep` (Пси-зона) → голос
(`voiceStep`: разговор с собой) → досуг (`leisure`, `diary`) → накопленные толчки (`impulse`, `relief`,
`sanityHit`, `sanityBoost`) → **ядро** `core/Psyche.step(состояние, входы, настройки, 1 с)` — чистая формула без
Minecraft → пик паники → `sync` клиенту → `sensations` (строка ощущений мода тела).

**Разовые события** копят толчок до ближайшего шага: урон (`onHurt`), взрыв (`onExplosion`), выстрел и пуля у
головы (`integration/TaczCompat`), сон (`onWake`), еда (`onFoodEaten`), курение и выпивка (`onConsume` →
`substances`), настроение от других модов (`onMood` ← `RpMoodEvent`), вред другому игроку (`onHarm` ←
`RpHarmEvent`), вызовы через RP Core (`impulse`, `sanityHit` ← `MindProvider`).

**Клиенту.** только свои цифры своему клиенту: `network/SyncPacket` (стресс, рассудок, флаги «тьма», «один»,
«Пси», «приглушено») → `client/ClientPsyche` (плавные значения) → `PsycheFx` (шейдер), `PhantomSounds`,
`Thoughts`, `Tremor`. Чужую психику клиент не знает.

**Речь.** `RpCoreAPI.registerSpeechFilter` → `PsycheService.filterSpeech` → `core/Stutter` (заикание со стресса
`stutter_from`); применяет тот, кто ведёт чат (RP PDA).

**Беседа с психологом.** `item/PsychologistJournalItem` (ПКМ по игроку) → `server/Therapy`: таймер, проверка, что
оба рядом, итог — рассудок и стресс.

**Данные игрока.** `capability/PsycheData.register()` → `PlayerStore` RP Core: психика переживает и смерть, и
возврат из Края; `resync` сбрасывает «что уже отправлено», `forget` чистит служебное.

**Настройки.** `core/Settings` (поля) ↔ `config/PsycheConfig` (`SettingsBinder` RP Core) → `PsycheConfig.bake()` на
загрузке и перечитывании конфига → `Settings.set`.

## 3. Карта пакетов

| Пакет | За что отвечает | С чего начать читать |
|---|---|---|
| `rppsyche` | вход мода, подписки, регистрация в Core | `RpPsyche` |
| `core` | формулы без Minecraft, проверяются без игры | `Psyche.step`, `PsycheState` (что сохраняется), `Settings` (все числа), `Stutter` |
| `config` | привязка `Settings` к `rppsyche-server.toml` | `PsycheConfig` |
| `capability` | данные игрока: сохраняемое и служебное | `PsycheData` |
| `server` | серверная механика | `PsycheService` (шаг и события), `Lights`, `Therapy`, `PsycheStatus` (строки осмотра), `Radios`, `PsycheCommands` |
| `integration` | соседи и чужие моды, мягко | `BodyCompat` (мод тела через Core), `TaczCompat`, `ZaCompat` |
| `integration.voice` | Simple Voice Chat | `PsycheVoicePlugin`, `VoiceTracker` |
| `network` | один пакет клиенту | `Net`, `SyncPacket` |
| `client` | всё, что игрок видит и слышит | `ClientSetup`, `ClientPsyche`, затем нужный эффект |
| `item`, `registry` | журнал психолога, звуки | `PsycheItems`, `PsycheSounds` |
| `gametest` | игровые тесты | `PsycheEdgeTests` |

## 4. Хочу добавить… — рецепты

| Что | Где | Образец | Не забыть |
|---|---|---|---|
| новое число (настройку) | поле в `core/Settings` + строка `S.bind("имяПоля", "описание")` в нужном разделе `config/PsycheConfig` | `stressDecay` | ключ в файле получится сам (`stress_decay`); типы: `double`, `int`, `long`, `List<String>`; описание по-русски |
| новый постоянный источник стресса | метод-шаг в `PsycheService` (или строка в подходящем `…Step`), вызов из `step` **в конец перед голосом** | `needsStep` (голод: `in.stressRate += …`) | порядок шагов влияет на результат — существующие не переставлять |
| разовый толчок от события этого мода | обработчик в `PsycheService` → `impulse(игрок, сила)` или `d.relief +=` | `onExplosion` | `affected(игрок)`: креатив и наблюдатель не затрагиваются |
| толчок из другого мода семейства | в том моде: `RpCoreAPI.stressImpulse` / `sanityHit` или `RpMoodEvent` | RP Culinary `ExoticEffects.Psyche` | здесь править ничего не надо; новый вид настроения — ветка в `PsycheService.onMood` |
| новое проявление у игрока | клиент: класс в `client/`, подключить в `ClientSetup`; нужен новый признак — флаг в `SyncPacket` | `Tremor`; флаг `SyncPacket.DARK` | только своему клиенту; новые флаги — следующим битом |
| светящий предмет чужого мода | конфиг: строка в `light_items` (`id` или `id{ключNBT}`) или тег предметов `rppsyche:light_sources` | значения по умолчанию в `Settings.lightItems` | кода не нужно; фонари RP Lights видны и так — через RP Core |
| новое сохраняемое поле | `core/PsycheState` + запись и чтение в `capability/PsycheData.save/load` + `PsycheState.copyFrom` | `aloneSeconds` | тест `PsycheEdgeTests` заполняет все поля `PsycheState` сам и найдёт несохранённое |
| что тело говорит психике | поле в `BodyCompat.Body` + чтение из `BodyProvider`; нет нужного вопроса — добавить его в розетку RP Core | `craving` | `faygolover.rpmedicine` не импортировать |
| досуг (что держит рассудок) | `PsycheService.leisure` | музыка, настольная игра | чужой мод — по id предметов и блоков, без его классов |
| команду ГМа | `server/PsycheCommands` | `/rppsyche set` | `PsycheCommands.describe` (список команд Core) |

## 5. Данные

| Что | Где хранится | Ключ | Что при смерти | Как попадает клиенту |
|---|---|---|---|---|
| психика игрока (`PsycheState` + `wasClinical`) | у игрока, `PlayerStore` RP Core | `rppsyche:psyche` | остаётся целиком | `SyncPacket` своему клиенту |
| толчки до шага, голос, пауза ГМа, «что отправлено» | память, поля `PsycheData` | — | не переносятся | — |
| беседы психолога | память, `Therapy` | — | — | — |

Имена полей в `PsycheData.save/load` (`stress`, `sanity`, `dark_s`, `alone_s`, `was_clinical`…) — в файлах игроков, не менять.

## 6. Связи

- Берёт из Core: `body()` через `BodyCompat` (боль, «лежит», опьянение, седация, ломка, тяга, антипсихотик,
  температура), `nutrition()`, `sleepStage`, `carriesLight`, `hasTrait`, `bodyRate`, `Consumables`, `Worn`;
  события `FoodEatenEvent`, `RpConsumeEvent`, `RpMoodEvent`, `RpHarmEvent`, `RpTimeSkipEvent`,
  `RpCharacterWipeEvent`; `PlayerStore`, `SettingsBinder`.
- Даёт в Core: `MindProvider` (`registerMind`), чтение стресса и рассудка (`registerStress`, `registerSanity`),
  поставщик света `rppsyche:light_items` (`registerLight`), фильтр речи, строки осмотра (`StatusLines`).
- RP Medicine необязательна: без мода тела нет боли, лежачих, ломки — источников меньше, мод работает.
- Zone Artifacts — мягкая прямая связь (`integration/ZaCompat`, грузится при `isLoaded`): сила Пси-зоны.
  Разрешена в `links_allow.txt`; уйдёт с розеткой Core.
- Чужие: TaCZ (выстрелы), Simple Voice Chat (плагин), Immersive Melodies, Table Top Craft, Simple Radio — по id.

## 7. Конфиги

`serverconfig/rppsyche-server.toml` — 136 ключей в 18 разделах, все из `core/Settings` через `SettingsBinder`.
Границы: числа −1e9…1e9, целые от 0 — менять нельзя (файл на сервере). Перечитывается на ходу.

## 8. Сеть

Канал мода (`network/Net`), один пакет сервер → клиент `SyncPacket`. Клиент ничего не присылает.

## 9. Ловушки

- **Порядок шагов в `PsycheService.step` менять нельзя:** входы копятся умножением и сложением, результат зависит
  от порядка до последнего знака.
- **Голос приходит не из главного потока** (`PsycheVoicePlugin`): он только отмечает в `VoiceTracker`, кто говорил
  и дошёл ли голос; решения — в шаге.
- **Лучи видимости — дорогие:** `hasLineOfSight` вызывается последним и только для соседа, который что-то меняет.
- **Рассудок идёт по времени тела** (`in.sanityTime = RpCoreAPI.bodyRate()`): на паузе RP-времени стоит; стресс — по реальному.
- **Пауза ГМа** (`/rppsyche pause`) и креатив: `affected` и `paused` проверяются во всех входах, новые — тоже.
- **Тело вышедшего игрока** считается лежачим всегда, а клинически мёртвым — если у него нет состояния или оно
  такое (`BodyProvider.down`).
- `scripts/gen_lang.py` отстал от файлов lang на 18 ключей: **не запускать** — сотрёт переводы.
- Сборка: Medicine в `build.gradle` — только для запуска (`runtimeOnly`); проверка без неё — `-Pno_medicine=true`.

## 10. Тесты

`python ../_family/family.py verify --tests "RP Psyche"` — 7 игровых тестов (`PsycheEdgeTests`): запись и чтение
всех полей, смерть, возврат из Края, перезаход, связи через Core (`coreLinks`), свет через Core
(`lightThroughCore`). Формулы `core/Psyche` модульными тестами не покрыты.

## 11. Документы в docs/

| Файл | Состояние |
|---|---|
| `MAP.md`, `INDEX.md` | действуют |
| `00_plan.md` | действует: план и решения автора (§12), этапы (§11) |
| `01_review_2026-11-06.md` | история: разбор баланса 06.11, внесён в 0.3.0 |
| `CLAUDE_old.md` | прежняя памятка мода; сборка и запуск клиента оттуда ещё верны, устройство — здесь |
