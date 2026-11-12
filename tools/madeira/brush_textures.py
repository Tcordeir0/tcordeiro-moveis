"""Pincéis de verniz (item 2D 16×16): cabo de madeira, virola de ferro, cerdas na cor do tom (TONES de madeira_textures).
Uso: python3 tools/madeira/brush_textures.py <pasta de saída>  → varnish_brush_<tom>.png"""
import os, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import madeira_textures as mt

def brush(tone):
    dark, light, _ = mt.TONES[tone]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); px = im.load()
    for i in range(7):
        px[1 + i, 14 - i] = (122, 86, 48, 255); px[2 + i, 14 - i] = (88, 58, 30, 255)
    for (x, y) in ((8, 7), (9, 7), (9, 6), (8, 6), (10, 6), (9, 8), (7, 7)): px[x, y] = (186, 186, 192, 255)
    for y in range(1, 7):
        for x in range(9, 16):
            if 13 <= x + y <= 17 and (x - y) >= 4:
                px[x, y] = mt.lerp(dark, light, 0.25 + 0.6 * (16 - x) / 7) + (255,)
    return im

if __name__ == "__main__":
    os.makedirs(sys.argv[1], exist_ok=True)
    for t in mt.TONES: brush(t).save(os.path.join(sys.argv[1], f"varnish_brush_{t}.png"))
