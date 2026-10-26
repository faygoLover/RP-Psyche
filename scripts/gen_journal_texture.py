# Текстура «Журнала психолога»: медкарта RP Medicine, перекрашенная в синий, со знаком Ψ на лицевой стороне.
# Запуск: python scripts/gen_journal_texture.py (нужен Pillow; берёт medcard.png из соседней папки RP Medicine).
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "..", "RP Medicine (Health revork)", "src", "main", "resources", "assets", "rpmedicine",
                   "textures", "item", "medcard.png")
DST = os.path.join(HERE, "..", "src", "main", "resources", "assets", "rppsyche", "textures", "item", "psychologist_journal.png")

PSI = ["#...#...#",
       "#...#...#",
       "#...#...#",
       ".#..#..#.",
       "..#####..",
       "....#....",
       "....#....",
       "....#....",
       "...###..."]

im = Image.open(SRC).convert("RGBA")
w, h = im.size
px = im.load()
# Синий тон: яркость исходника × оттенок.
for y in range(h):
    for x in range(w):
        r, g, b, a = px[x, y]
        if a == 0:
            continue
        l = (0.3 * r + 0.59 * g + 0.11 * b) / 255
        px[x, y] = (int(40 + 70 * l), int(70 + 110 * l), int(140 + 115 * l), a)
# Лицевую сторону под знаком — ровным синим, знак — светлым.
for y in range(9, 19):
    for x in range(8, 23):
        if px[x, y][3] > 0:
            px[x, y] = (38, 64, 132, 255)
for dy, row in enumerate(PSI):
    for dx, ch in enumerate(row):
        if ch == "#":
            px[11 + dx, 9 + dy] = (225, 235, 255, 255)
os.makedirs(os.path.dirname(DST), exist_ok=True)
im.save(DST)
print(DST)
