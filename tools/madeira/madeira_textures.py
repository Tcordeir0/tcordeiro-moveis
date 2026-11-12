"""Madeira e verniz (Móveis 0.3.0): texturas geradas a partir das tábuas do Minecraft 26.3.

Uso: python3 madeira_textures.py <pasta com <madeira>_planks.png> <pasta de saída>
Para cada madeira gera:
  verniz:   <madeira>_<tom>.png        tons: natural, mel, nogueira, mogno, ebano, laca_branca, laca_preta
  polido:   <madeira>_<tom>_polido.png (brilho: realce nas arestas de cima e reflexo)
  desgaste: <madeira>_gasta_<1..3>.png (sem verniz ao ar livre: 1 gasta, 2 acinzentada, 3 rachada)
  blocos:   <madeira>_parquet.png, _espinha.png, _lambri.png, _assoalho.png, _viga_lado.png, _viga_topo.png
O veio (luminância) vem sempre da textura original, por isso cada madeira continua reconhecível.
"""
import os, sys
from PIL import Image

TONES = {  # (cor escura, cor clara, força)  — força 1 = só a cor do verniz com o veio da madeira
    "natural": ((92, 62, 30), (214, 170, 110), 0.25),
    "mel": ((120, 66, 18), (236, 170, 74), 0.45),
    "nogueira": ((52, 32, 18), (140, 96, 58), 0.55),
    "mogno": ((70, 22, 14), (168, 74, 46), 0.55),
    "ebano": ((18, 14, 12), (74, 58, 50), 0.72),
    "laca_branca": ((176, 172, 162), (246, 244, 236), 0.92),
    "laca_preta": ((10, 10, 12), (58, 56, 60), 0.95),
}

def lum(p):
    r, g, b = p[:3]
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255

def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))

def normalized_lums(im):
    ls = [lum(im.getpixel((x, y))) for y in range(16) for x in range(16)]
    lo, hi = min(ls), max(ls)
    return lambda p: (lum(p) - lo) / max(1e-6, hi - lo)

def varnish(im, tone):
    dark, light, k = TONES[tone]
    nl = normalized_lums(im); out = im.copy()
    for y in range(16):
        for x in range(16):
            p = im.getpixel((x, y)); c = lerp(dark, light, nl(p))
            out.putpixel((x, y), lerp(p[:3], c, k) + (255,))
    return out

def plank_rows(im):
    """Linhas de junta das tábuas (as mais escuras de cada faixa horizontal) — as tábuas vanilla têm 4 faixas de 4 px."""
    rows = [sum(lum(im.getpixel((x, y))) for x in range(16)) for y in range(16)]
    return sorted(range(16), key=lambda y: rows[y])[:4]

def polish(im):
    out = im.copy(); joints = set(plank_rows(im))
    for y in range(16):
        for x in range(16):
            r, g, b, a = im.getpixel((x, y))
            f = 1.08
            if (y - 1) in joints: f = 1.28                # aresta de cima de cada tábua apanha a luz
            if 3 <= (x + y) % 16 <= 4 and y not in joints: f = 1.18  # reflexo largo e suave em diagonal
            out.putpixel((x, y), (min(255, int(r * f)), min(255, int(g * f)), min(255, int(b * f)), a))
    return out

def weather(im, stage):
    grey = (128, 126, 120); out = im.copy(); joints = plank_rows(im)
    t = (0.25, 0.55, 0.75)[stage - 1]
    for y in range(16):
        for x in range(16):
            p = im.getpixel((x, y)); l = lum(p)
            g = tuple(int(v * (0.7 + 0.6 * l)) for v in grey)
            out.putpixel((x, y), lerp(p[:3], g, t) + (255,))
    if stage == 3:  # rachas
        for (x0, y0, n) in ((2, joints[0] + 2, 5), (9, joints[1] + 1, 4), (5, joints[2] + 2, 6)):
            for i in range(n):
                x, y = (x0 + i) % 16, (y0 + (i // 3)) % 16
                p = out.getpixel((x, y)); out.putpixel((x, y), tuple(int(v * 0.55) for v in p[:3]) + (255,))
    return out

def strip(im, y0, h=4):
    return im.crop((0, y0, 16, y0 + h))

def parquet(im):
    """Quadrados 8×8 alternados: tábuas deitadas / em pé (veio rodado)."""
    out = Image.new("RGBA", (16, 16)); tile = im.crop((0, 0, 8, 8)); rot = tile.rotate(90)
    for i, (x, y) in enumerate(((0, 0), (8, 0), (0, 8), (8, 8))):
        out.paste(tile if (i in (0, 3)) else rot, (x, y))
    return out

def herringbone(im):
    """Espinha de peixe de verdade: réguas 4×2 (deitadas) e 2×4 (em pé) em escada. Período 16 px (repete sem emenda).
    Cada régua leva o veio no seu sentido; a primeira linha/coluna da régua é a junta (mais escura)."""
    own = {}
    for k in range(-16, 16):
        for j in range(-8, 8):
            ox, oy = 2 * k + 4 * j, 2 * k - 4 * j; sid = (k * 7 + j * 13) % 5
            for dx in range(4):
                for dy in range(2): own[((ox + dx) % 16, (oy + dy) % 16)] = ("H", dx, dy, sid)
            for dx in range(2):
                for dy in range(4): own[((ox + dx) % 16, (oy + 2 + dy) % 16)] = ("V", dy, dx, sid)
    out = Image.new("RGBA", (16, 16)); px = out.load()
    for y in range(16):
        for x in range(16):
            kind, along, across, sid = own[(x, y)]
            sx, sy = (along * 3 + sid * 2) % 16, (sid * 3 + 1) % 16
            p = im.getpixel((sx, sy))
            f = (0.94, 1.0, 1.06, 0.97, 1.03)[sid]
            if along == 0: f *= 0.72                     # junta no topo da régua
            elif across == 0: f *= 1.08                  # aresta clara
            px[x, y] = tuple(min(255, int(v * f)) for v in p[:3]) + (255,)
    return out

def paneling(im):
    """Lambri: tábuas em pé de 4 px com sulco suave (sem friso, para não riscar a parede a cada bloco)."""
    out = im.rotate(90); px = out.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            f = 0.78 if x % 4 == 0 else (1.07 if x % 4 == 1 else 1.0)
            px[x, y] = (min(255, int(r * f)), min(255, int(g * f)), min(255, int(b * f)), a)
    return out

def floorboards(im):
    """Assoalho: tábuas compridas com as juntas desencontradas."""
    out = im.copy(); joints = plank_rows(im); px = out.load()
    cuts = {joints[0]: 5, joints[1]: 12, joints[2]: 2, joints[3]: 9}
    for jy, cx in cuts.items():
        for dy in range(1, 4):
            y = (jy + dy) % 16
            r, g, b, a = px[cx, y]; px[cx, y] = (int(r * 0.55), int(g * 0.55), int(b * 0.55), a)
    return out

def beam_side(im):
    out = im.rotate(90).copy(); px = out.load()
    for y in range(16):
        for x in (0, 15):
            r, g, b, a = px[x, y]; px[x, y] = (int(r * 0.6), int(g * 0.6), int(b * 0.6), a)
    return out

def beam_top(im):
    out = Image.new("RGBA", (16, 16)); px = out.load()
    for y in range(16):
        for x in range(16):
            ring = max(abs(x - 7.5), abs(y - 7.5))
            p = im.getpixel(((x * 3) % 16, (y * 5) % 16))
            f = 0.6 if ring > 7 else (0.85 if int(ring) % 3 == 0 else 1.0)
            px[x, y] = tuple(int(v * f) for v in p[:3]) + (255,)
    return out

def main(src, dst):
    os.makedirs(dst, exist_ok=True); n = 0
    for f in sorted(os.listdir(src)):
        if not f.endswith("_planks.png"): continue
        wood = f[:-len("_planks.png")]; im = Image.open(os.path.join(src, f)).convert("RGBA")
        for tone in TONES:
            v = varnish(im, tone); v.save(os.path.join(dst, f"{wood}_{tone}.png")); polish(v).save(os.path.join(dst, f"{wood}_{tone}_polido.png")); n += 2
        for s in (1, 2, 3): weather(im, s).save(os.path.join(dst, f"{wood}_gasta_{s}.png")); n += 1
        for name, fn in (("parquet", parquet), ("espinha", herringbone), ("lambri", paneling), ("assoalho", floorboards),
                         ("viga_lado", beam_side), ("viga_topo", beam_top)):
            fn(im).save(os.path.join(dst, f"{wood}_{name}.png")); n += 1
    print(n, "texturas")

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
