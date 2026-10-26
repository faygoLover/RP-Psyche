import json, sys, os
# Переводы RP Psyche: python scripts/gen_lang.py (пишет assets/rppsyche/lang). Руками JSON не править.
out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "rppsyche", "lang")
ru = {
 "rppsyche.sense.anxious": "Тревожно",
 "rppsyche.sense.tremor": "Руки дрожат",
 "rppsyche.sense.seeing": "Мерещится",
 "rppsyche.sense.someone": "Кто-то рядом",
 "rppsyche.stage.0": "норма", "rppsyche.stage.1": "надломлен", "rppsyche.stage.2": "неустойчив",
 "rppsyche.stage.3": "помешательство", "rppsyche.stage.4": "безумие",
 "rppsyche.cmd.get": "стресс и рассудок игрока (цифры)",
 "rppsyche.cmd.set": "выставить стресс или рассудок (0–100)",
 "rppsyche.cmd.add": "прибавить к стрессу или рассудку",
 "rppsyche.cmd.get.result": "%s: стресс %s, рассудок %s (%s); во тьме %s с, один %s мин",
 "rppsyche.cmd.changed": "Психика изменена: %s",
 "rppsyche.cmd.pause": "пауза психики (ГМ-пауза, по умолчанию 10 минут): стоит, эффекты выключены",
 "rppsyche.cmd.resume": "снять паузу психики",
 "rppsyche.cmd.paused": "Психика на паузе у %s игроков на %s мин",
 "rppsyche.cmd.resumed": "Пауза психики снята у %s игроков",
 "item.rppsyche.psychologist_journal": "Журнал психолога",
 "item.rppsyche.psychologist_journal.desc": "ПКМ по игроку — беседа (3 минуты рядом): рассудок +, стресс −. Медицина 4+",
 "item.rppsyche.psychologist_journal.cooldown": "Снова можно через %s мин",
 "rppsyche.therapy.busy": "Беседа уже идёт",
 "rppsyche.therapy.skill": "Нужна медицина не ниже %s",
 "rppsyche.therapy.journal_cooldown": "Журнал снова можно использовать через %s мин",
 "rppsyche.therapy.patient_cooldown": "%s недавно уже был на беседе",
 "rppsyche.therapy.started": "Беседа с %s началась — оставайтесь рядом",
 "rppsyche.therapy.started_patient": "%s начал(а) с вами беседу — оставайтесь рядом",
 "rppsyche.therapy.broken": "Беседа сорвалась",
 "rppsyche.therapy.done": "Беседа окончена",
 "rppsyche.therapy.done_patient": "После разговора стало легче",
 "rppsyche.therapy.progress": "Беседа %s %s",
 "subtitles.rppsyche.whoosh": "Шорох",
 "subtitles.rppsyche.distant_shot": "Далёкий выстрел",
}
en = {
 "rppsyche.sense.anxious": "Anxious",
 "rppsyche.sense.tremor": "Hands are shaking",
 "rppsyche.sense.seeing": "Seeing things",
 "rppsyche.sense.someone": "Someone is near",
 "rppsyche.stage.0": "sound", "rppsyche.stage.1": "cracked", "rppsyche.stage.2": "unstable",
 "rppsyche.stage.3": "deranged", "rppsyche.stage.4": "insane",
 "rppsyche.cmd.get": "player's stress and sanity (numbers)",
 "rppsyche.cmd.set": "set stress or sanity (0–100)",
 "rppsyche.cmd.add": "add to stress or sanity",
 "rppsyche.cmd.get.result": "%s: stress %s, sanity %s (%s); in the dark %s s, alone %s min",
 "rppsyche.cmd.changed": "Psyche changed: %s",
 "rppsyche.cmd.pause": "pause the psyche (GM pause, 10 minutes by default): frozen, effects off",
 "rppsyche.cmd.resume": "lift the psyche pause",
 "rppsyche.cmd.paused": "Psyche paused for %s players for %s min",
 "rppsyche.cmd.resumed": "Psyche pause lifted for %s players",
 "item.rppsyche.psychologist_journal": "Psychologist's journal",
 "item.rppsyche.psychologist_journal.desc": "Right-click a player — a talk (3 minutes nearby): sanity up, stress down. Medicine 4+",
 "item.rppsyche.psychologist_journal.cooldown": "Ready again in %s min",
 "rppsyche.therapy.busy": "A talk is already going on",
 "rppsyche.therapy.skill": "Needs Medicine %s or higher",
 "rppsyche.therapy.journal_cooldown": "The journal is ready again in %s min",
 "rppsyche.therapy.patient_cooldown": "%s has had a talk recently",
 "rppsyche.therapy.started": "The talk with %s has started — stay close",
 "rppsyche.therapy.started_patient": "%s started a talk with you — stay close",
 "rppsyche.therapy.broken": "The talk broke off",
 "rppsyche.therapy.done": "The talk is over",
 "rppsyche.therapy.done_patient": "You feel better after the talk",
 "rppsyche.therapy.progress": "Talk %s %s",
 "subtitles.rppsyche.whoosh": "Rustle",
 "subtitles.rppsyche.distant_shot": "Distant gunshot",
}
thoughts_ru = {
 "stress": ["Спокойно. Дыши.", "Надо уходить отсюда", "Сердце колотится", "Руки не слушаются", "Только не сейчас...", "Соберись"],
 "panic": ["БЕГИ", "Не могу... не могу...", "Я сейчас умру", "ГДЕ ВЫХОД", "Нет-нет-нет-нет", "Дыши! ДЫШИ!"],
 "dark": ["Темно... где фонарь?", "Ничего не видно", "Там что-то есть, в темноте", "Не люблю темноту"],
 "alone": ["Где все?", "Я тут совсем один", "Давно никого не слышал", "Хоть бы кто-нибудь"],
 "s1": ["Показалось?", "Здесь кто-то был", "Странно...", "Я это уже видел?", "Что-то не так", "Это был я?"],
 "s2": ["Они переглядываются", "Почему он так смотрит?", "Я слышал своё имя", "Не оборачивайся", "Это не моя рука... моя?", "Кто это сказал?"],
 "s3": ["ОНИ ЗНАЮТ", "Не верь им", "Тише... тише... тише...", "Я не спал. Я НЕ СПАЛ.", "Оно идёт за мной", "Почему никто не слышит?"],
 "s4": ["ВСЁ ЭТО НЕ НАСТОЯЩЕЕ", "Они смеются надо мной", "ЗАМОЛЧИ", "Раз. Два. Три. Раз. Два. Три.", "Я ЗДЕСЬ. Я ЗДЕСЬ. Я ЗДЕСЬ.", "ВЫЙДИ ИЗ МОЕЙ ГОЛОВЫ"],
}
thoughts_en = {
 "stress": ["Easy. Breathe.", "Got to get out of here", "My heart is pounding", "My hands won't listen", "Not now...", "Pull yourself together"],
 "panic": ["RUN", "I can't... I can't...", "I'm going to die", "WHERE'S THE WAY OUT", "No-no-no-no", "Breathe! BREATHE!"],
 "dark": ["Dark... where's the light?", "I can't see a thing", "Something's there, in the dark", "I hate the dark"],
 "alone": ["Where is everyone?", "I'm all alone here", "Haven't heard anyone in ages", "If only someone was here"],
 "s1": ["Did I imagine that?", "Someone was here", "Strange...", "Have I seen this before?", "Something's wrong", "Was that me?"],
 "s2": ["They're exchanging looks", "Why is he staring like that?", "I heard my name", "Don't turn around", "That's not my hand... is it?", "Who said that?"],
 "s3": ["THEY KNOW", "Don't trust them", "Quiet... quiet... quiet...", "I wasn't asleep. I WASN'T.", "It's following me", "Why can't anyone hear it?"],
 "s4": ["NONE OF THIS IS REAL", "They're laughing at me", "SHUT UP", "One. Two. Three. One. Two. Three.", "I'M HERE. I'M HERE. I'M HERE.", "GET OUT OF MY HEAD"],
}
for d, t in ((ru, thoughts_ru), (en, thoughts_en)):
    for g, lines in t.items():
        for i, l in enumerate(lines):
            d[f"rppsyche.thought.{g}.{i}"] = l
for name, d in (("ru_ru.json", ru), ("en_us.json", en)):
    with open(os.path.join(out, name), "w", encoding="utf-8", newline="\n") as f:
        json.dump(d, f, ensure_ascii=False, indent=2)
        f.write("\n")
print(len(ru), len(en))
