# RP Psyche — указатель кода

Создано `python _family/tools/mapgen.py` 15.11.2026, версия 0.5.1-1.20.1-forge, коммит `3956dde`.
**Руками не править**: пересоздаётся скриптом. Смысл, потоки работы и рецепты — в `docs/MAP.md`.
Разбор по шаблонам, без компиляции: необычно оформленная регистрация может отсутствовать в списках.

Всего: 31 файлов, 3272 строк Java.

## 1. Пакеты

| Пакет | Файлов | Строк |
|---|---:|---:|
| `faygolover.rppsyche` | 1 | 117 |
| `faygolover.rppsyche.capability` | 1 | 135 |
| `faygolover.rppsyche.client` | 7 | 589 |
| `faygolover.rppsyche.config` | 1 | 272 |
| `faygolover.rppsyche.core` | 4 | 503 |
| `faygolover.rppsyche.integration` | 4 | 201 |
| `faygolover.rppsyche.integration.voice` | 2 | 131 |
| `faygolover.rppsyche.item` | 1 | 47 |
| `faygolover.rppsyche.network` | 2 | 68 |
| `faygolover.rppsyche.registry` | 2 | 50 |
| `faygolover.rppsyche.server` | 6 | 1159 |

## 2. Классы

### `faygolover.rppsyche`
- `RpPsyche`, 117 — RP Psyche — стресс и рассудок, аддон к RP Medicine (план — docs/00_plan.md).

### `faygolover.rppsyche.capability`
- `PsycheData`, 135 — Психика игрока (сохраняется) и служебное состояние сервера (не сохраняется).

### `faygolover.rppsyche.client`
- `ClientPsyche`, 92 — Своя психика на клиенте: то, что прислал сервер, и плавные значения для эффектов (переходы без скачков).
- `ClientSetup`, 45 — Клиент: экран, звуки, мысли, дрожь рук.
- `LoopSound`, 38 — Звук «в голове» по кругу (сердцебиение, гул): у самого слушателя, громкость меняется плавно.
- `PhantomSounds`, 137 — Звуки, которых нет: только у своего клиента, никто больше их не слышит.
- `PsycheFx`, 109 — Что видно: мир выцветает и становится контрастнее (стресс, плохой рассудок), цвета расходятся к краям (с Помешательства и на панике), края темнеют тоннелем (паника).
- `Thoughts`, 126 — Внутренний голос: мысли посреди экрана, только свои.
- `Tremor`, 42 — Дрожь рук от стресса: взгляд (и прицел) мелко дрожит, на панике ещё и медленно «уводит».

### `faygolover.rppsyche.config`
- `PsycheConfig`, 272 — rppsyche-server.toml: все числа Settings.

### `faygolover.rppsyche.core`
- `Psyche`, 113 — Шаг психики за секунду (без Minecraft).
- `PsycheState`, 58 — Психика игрока: то, что сохраняется.
- `Settings`, 277 — Все числа психики (черновик плана, docs/00_plan.md).
- `Stutter`, 55 — Заикание от стресса: начало слова повторяется через дефис («п-п-подожди»), иногда — обрыв «...».

### `faygolover.rppsyche.integration`
- `CuriosWorn`, 62 — Надетое в слоты Curios (налобный фонарь, лампа на поясе — RP Lights).
- `MedicineCompat`, 59 — RP Medicine (обязательный): боль, лежачие, опьянение, седация, ломка.
- `TaczCompat`, 57 — TaCZ (необязательный): выстрел слышат все вокруг, а у кого пуля прошла у самой головы — подавление.
- `ZaCompat`, 23 — Zone Artifacts (необязательный): в Пси-зоне ли игрок.

### `faygolover.rppsyche.integration.voice`
- `PsycheVoicePlugin`, 49 — Плагин Simple Voice Chat: только отмечает, кто и когда говорит и кому его голос реально ушёл (рядом, в группе; шёпот и дальность считает сам Simple Voice Chat).
- `VoiceTracker`, 82 — Кто говорит в голосовой чат.

### `faygolover.rppsyche.item`
- `PsychologistJournalItem`, 47 — Журнал психолога: ПКМ по игроку — беседа (3 минуты рядом), после неё рассудок пациента +, стресс −.

### `faygolover.rppsyche.network`
- `Net`, 31 — Сеть мода: своя психика — только своему клиенту.
- `SyncPacket` (record), 37 — Своя психика клиенту: стресс, рассудок и что сейчас вокруг (флаги).

### `faygolover.rppsyche.registry`
- `PsycheItems`, 25 — Предметы психики.
- `PsycheSounds`, 25 — Звуки психики: сердцебиение, гул, шорох; далёкие выстрелы (из пака TaCZ по умолчанию).

### `faygolover.rppsyche.server`
- `Lights`, 82 — Полная тьма: свет у глаз почти ноль и ни у тебя, ни у игрока рядом нет света в руках или надетого в Curios (налобный фонарь, лампа на поясе).
- `PsycheCommands`, 114 — /rppsyche: цифры психики — только ГМу (уровень прав 2).
- `PsycheService`, 714 — Психика на сервере.
- `PsycheStatus`, 57 — Психика в осмотре RP Medicine (строки RP Core).
- `Radios`, 37 — Рация в руке (Settings#voiceRadioItems, Simple Radio): голос уходит через неё, а не напрямую игрокам, и Simple Voice Chat не знает, кто услышал.
- `Therapy`, 155 — Беседа с психологом (журнал психолога).

Классов без описания: 0 из 31.

## 3. Подписки на события

| Событие | Кто слушает |
|---|---|
| `AttachCapabilitiesEvent` | `RpPsyche.attachCaps` |
| `BuildCreativeModeTabContentsEvent` | `PsycheItems.onCreativeTab` |
| `ClientPlayerNetworkEvent.LoggingOut` | `ClientSetup.onLogout` |
| `ExplosionEvent.Detonate` | `PsycheService.onExplosion` |
| `FMLCommonSetupEvent` | `RpPsyche.commonSetup` |
| `FoodEatenEvent` | `PsycheService.onFoodEaten` |
| `GunShootEvent` | `TaczCompat.onShoot` |
| `LivingEntityUseItemEvent.Finish` | `PsycheService.onUseFinish` |
| `LivingHurtEvent` | `PsycheService.onHurt` |
| `MicrophonePacketEvent` | `PsycheVoicePlugin.onMicrophone` |
| `ModConfigEvent.Loading` | `RpPsyche.onConfig` |
| `ModConfigEvent.Reloading` | `RpPsyche.onConfigReload` |
| `PlayerEvent.Clone` | `PsycheService.onClone` |
| `PlayerEvent.PlayerChangedDimensionEvent` | `PsycheService.onChangeDimension` |
| `PlayerEvent.PlayerLoggedInEvent` | `PsycheService.onLogin` |
| `PlayerEvent.PlayerLoggedOutEvent` | `PsycheService.onLogout`, `Therapy.onLogout` |
| `PlayerEvent.PlayerRespawnEvent` | `PsycheService.onRespawn` |
| `PlayerWakeUpEvent` | `PsycheService.onWake` |
| `RegisterCapabilitiesEvent` | `RpPsyche.registerCaps` |
| `RegisterCommandsEvent` | `PsycheCommands.register` |
| `RegisterGuiOverlaysEvent` | `ClientSetup.overlays` |
| `RenderGuiEvent.Pre` | `ClientSetup.onGuiPre` |
| `ServerStoppedEvent` | `PsycheService.onServerStopped` |
| `SoundPacketEvent` | `PsycheVoicePlugin.onSound` |
| `TickEvent.ClientTickEvent` | `ClientSetup.onClientTick` |
| `TickEvent.PlayerTickEvent` | `PsycheService.onPlayerTick` |
| `TickEvent.RenderTickEvent` | `ClientSetup.onRenderTick` |
| `TickEvent.ServerTickEvent` | `Therapy.onServerTick` |
| `faygolover.rpcore.api.event.RpCharacterWipeEvent` | `PsycheService.onWipe` |
| `faygolover.rpcore.api.event.RpConsumeEvent` | `PsycheService.onConsume` |
| `faygolover.rpcore.api.event.RpHarmEvent` | `PsycheService.onHarm` |
| `faygolover.rpcore.api.event.RpMoodEvent` | `PsycheService.onMood` |
| `faygolover.rpcore.api.event.RpTimeSkipEvent` | `PsycheService.onTimeSkip` |
| `net.minecraftforge.event.entity.living.MobEffectEvent.Added` | `PsycheService.onEffectAdded` |
| `net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract` | `PsycheService.onEntityInteract` |
| `net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem` | `PsycheService.onRightClick` |

## 4. Сеть

Канал: `faygolover/rppsyche/network/Net.java`, имя `main`, версия протокола `2`.
- `SyncPacket` (направление не указано в регистрации)

Классы с обработчиками пакетов (1): `SyncPacket`.

## 5. Команды

- `faygolover/rppsyche/server/PsycheCommands.java`: `rppsyche` `get` `pause` `resume`

## 6. Данные

- capability: `PsycheData`, ключ `rppsyche:psyche`

## 7. Конфиги

- server: `PsycheConfig`, вызовов define 4, привязок по имени поля 137

## 8. Что берёт из RP Core и что ему даёт

Методы `RpCoreAPI` (сколько вызовов): `MISC_TAB` 1, `SpeechChannel` 1, `body` 3, `bodyScale` 1, `dayLengthSeconds` 1, `hasTrait` 8, `isSkillManaged` 1, `nutrition` 2, `registerCommand` 5, `registerModule` 1, `registerSanity` 1, `registerSpeechFilter` 1, `registerStress` 1, `skill` 1, `sleepStage` 1, `timeRunning` 1.

Реализует розетки: Source ← `PsycheStatus`.

События Core: `FoodEatenEvent`, `RpCharacterWipeEvent`, `RpConsumeEvent`, `RpHarmEvent`, `RpMoodEvent`, `RpTimeSkipEvent`.

## 9. Связи с другими модами

Проверки `isLoaded`: `curios` (CuriosWorn); `fl_zone_arts` (PsycheService); `immersive_melodies` (PsycheService); `rpneeds` (PsycheService); `table_top_craft` (PsycheService); `tacz` (RpPsyche); `voicechat` (PsycheService).

Отражение (`Class.forName`):
- `CuriosWorn` → `top.theillusivec4.curios.api.CuriosApi`
- `CuriosWorn` → `top.theillusivec4.curios.api.type.capability.ICuriosItemHandler`
- `CuriosWorn` → `net.minecraftforge.items.IItemHandlerModifiable`

Пакеты других модов семейства в коде: `rpcore` — 6 файлов; `rpmedicine` — 1 файлов; `zoneartifacts` — 1 файлов.

## 10. Зарегистрированные id

- `faygolover/rppsyche/registry/PsycheItems.java` (1): `psychologist_journal`
