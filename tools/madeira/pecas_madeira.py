# Peças da Móveis 0.3.0 (madeira e verniz) — mesmo formato do tools/blender/pecas.py: (x1,y1,z1,x2,y2,z2,var), frente a norte.
PIECES = {}
PIECES["sawmill"] = [  # Serraria: bancada com serra circular a sair do tampo
    (1, 0, 1, 3, 11, 3, "#log"), (13, 0, 1, 15, 11, 3, "#log"), (1, 0, 13, 3, 11, 15, "#log"), (13, 0, 13, 15, 11, 15, "#log"),
    (2, 3, 2, 14, 4, 14, "#planks"), (0, 11, 0, 16, 14, 16, "#planks"),
    (4.5, 14, 7.6, 11.5, 17, 8.4, "#blade"), (5.5, 13.5, 7.6, 10.5, 18.5, 8.4, "#blade"), (6.5, 13.5, 7.6, 9.5, 19.5, 8.4, "#blade"),
    (7.5, 15.5, 7.4, 8.5, 16.5, 8.6, "#metal"),                       # eixo
    (0.5, 14, 2, 15.5, 14.5, 3, "#metal"), (0.5, 14, 13, 15.5, 14.5, 14, "#metal"),   # guias
    (14, 6, 0.5, 15.5, 9, 1, "#metal"),                                # manivela/alavanca
]
PIECES["varnish_brush"] = []  # item (pincel) é 2D: textura item/varnish_brush_<tom>.png
PREVIEW_TEX = {"#planks": "oak_planks", "#log": "oak_log", "#metal": "iron_block", "#blade": "iron_block"}
