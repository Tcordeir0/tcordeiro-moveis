"""Verifica um livro escrito (receita <mod>:guide com minecraft:written_book_content) com as larguras reais da fonte do Minecraft.
Regras do ecrã do livro do jogo: 114 px de largura, 14 linhas por página, no máximo 100 páginas; «§l…§r» é negrito (+1 px por letra).
Também acusa fórmulas e contas («=», «×»).
Uso: python3 check_written_book.py <client.jar> <recipe/guide.json>
Sai com código 1 se houver problemas.
"""
import io
import json
import re
import sys
import zipfile

from PIL import Image

JAR, BOOK = sys.argv[1:3]
TW, LINES, MAX_PAGES = 114, 14, 100

with zipfile.ZipFile(JAR) as z:
    font = json.load(z.open("assets/minecraft/font/include/default.json"))
    prov = next(p for p in font["providers"] if p.get("file", "").endswith("ascii.png"))
    img = Image.open(io.BytesIO(z.read("assets/minecraft/textures/font/ascii.png"))).convert("RGBA")
cw, ch = img.width // 16, img.height // 16
widths = {}
for row, chars in enumerate(prov["chars"]):
    for col, c in enumerate(chars):
        last = -1
        for x in range(cw):
            if any(img.getpixel((col * cw + x, row * ch + y))[3] > 0 for y in range(ch)):
                last = x
        widths[c] = (last + 2) if last >= 0 else 4
widths[" "] = 4


def segments(text):
    """(letra, negrito?) sem os códigos §."""
    bold, out, i = False, [], 0
    while i < len(text):
        if text[i] == "§" and i + 1 < len(text):
            code = text[i + 1].lower()
            bold = True if code == "l" else False if code in "r0123456789abcdef" else bold
            i += 2
            continue
        out.append((text[i], bold))
        i += 1
    return out


def count_lines(text):
    n = 0
    chars = segments(text)
    paras, cur = [], []
    for c, b in chars:
        if c == "\n":
            paras.append(cur)
            cur = []
        else:
            cur.append((c, b))
    paras.append(cur)
    for para in paras:
        words, word = [], []
        for c, b in para:
            if c == " ":
                words.append(word)
                word = []
            else:
                word.append((c, b))
        words.append(word)
        line_w, lines = 0, 1
        for wd in words:
            ww = sum(widths.get(c, 6) + (1 if b else 0) for c, b in wd)
            cand = ww if line_w == 0 else line_w + 4 + ww
            if cand <= TW or line_w == 0:
                line_w = cand
            else:
                lines += 1
                line_w = ww
        n += lines
    return n


recipe = json.load(open(BOOK))
content = recipe["result"]["components"]["minecraft:written_book_content"]
pages = [p["raw"] if isinstance(p, dict) else p for p in content["pages"]]
problems = 0
if len(pages) > MAX_PAGES:
    print(f"  ✗ {len(pages)} páginas (máximo {MAX_PAGES})")
    problems += 1
for i, p in enumerate(pages, 1):
    n = count_lines(p)
    title = p.split("\n", 1)[0]
    if n > LINES:
        print(f"  ✗ página {i} ({re.sub('§.', '', title)}): {n} linhas (máximo {LINES})")
        problems += 1
    for bad in ("=", "×"):
        if bad in p:
            print(f"  ✗ página {i} ({re.sub('§.', '', title)}): tem «{bad}» (sem fórmulas nem contas)")
            problems += 1
print(f"{BOOK.split('/')[-1]}: {len(pages)} páginas, {problems} problemas")
sys.exit(1 if problems else 0)
