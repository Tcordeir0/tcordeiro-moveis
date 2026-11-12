# Peças dos Móveis em cubos (unidades = pixels; 16 = 1 bloco). Frente da peça = norte (z = 0).
# Cada cubo: (x1, y1, z1, x2, y2, z2, variável_de_textura)

def legs4(x0, x1, z0, z1, top, w=2, var="#planks"):
    return [(x0, 0, z0, x0 + w, top, z0 + w, var), (x1 - w, 0, z0, x1, top, z0 + w, var),
            (x0, 0, z1 - w, x0 + w, top, z1, var), (x1 - w, 0, z1 - w, x1, top, z1, var)]

PIECES = {}

PIECES["chair"] = legs4(2, 14, 2, 14, 8) + [
    (4, 3, 2.5, 12, 4, 3.5, "#planks"), (4, 3, 12.5, 12, 4, 13.5, "#planks"),   # travessas
    (1, 8, 1, 15, 10, 15, "#planks"),                                           # assento
    (2, 10, 12, 4, 24, 14, "#planks"), (12, 10, 12, 14, 24, 14, "#planks"),     # pés do encosto
    (4, 13, 12.5, 12, 15, 13.5, "#planks"), (4, 17, 12.5, 12, 19, 13.5, "#planks"),
    (1.5, 22, 11.5, 14.5, 24.5, 14.5, "#planks"),                               # topo do encosto
]

PIECES["stool"] = legs4(4, 12, 4, 12, 9) + [
    (6, 4, 4.5, 10, 5, 5.5, "#planks"), (6, 4, 10.5, 10, 5, 11.5, "#planks"),
    (4.5, 4, 6, 5.5, 5, 10, "#planks"), (10.5, 4, 6, 11.5, 5, 10, "#planks"),
    (3, 9, 3, 13, 11, 13, "#planks"), (4, 11, 4, 12, 11.5, 12, "#wool"),        # assento + almofada
]

# Mesa em partes (multipart): tampo sempre; uma perna por canto só quando esse canto não tem vizinho.
PIECES["table_top"] = [(0, 14, 0, 16, 16, 16, "#planks")]
PIECES["table_leg_nw"] = [(1, 0, 1, 3, 14, 3, "#planks"), (3, 12, 1.5, 8, 14, 2.5, "#planks"), (1.5, 12, 3, 2.5, 14, 8, "#planks")]
PIECES["table_single"] = PIECES["table_top"] + [c for c in PIECES["table_leg_nw"]] + [
    (13, 0, 1, 15, 14, 3, "#planks"), (1, 0, 13, 3, 14, 15, "#planks"), (13, 0, 13, 15, 14, 15, "#planks"),
    (3, 12, 1.5, 13, 14, 2.5, "#planks"), (3, 12, 13.5, 13, 14, 14.5, "#planks"),
    (1.5, 12, 3, 2.5, 14, 13, "#planks"), (13.5, 12, 3, 14.5, 14, 13, "#planks")]

PIECES["cabinet"] = [
    (0, 0, 0, 16, 1, 16, "#planks"), (1, 1, 1, 15, 15, 15, "#planks"), (0, 15, 0, 16, 16, 16, "#planks"),
    (1.5, 2, 0.5, 7.5, 14, 1, "#planks"), (8.5, 2, 0.5, 14.5, 14, 1, "#planks"),   # portas
    (6, 7, 0, 7, 10, 0.5, "#metal"), (9, 7, 0, 10, 10, 0.5, "#metal"),           # puxadores
]

PIECES["drawer"] = [
    (0, 0, 0, 16, 1, 16, "#planks"), (1, 1, 1, 15, 15, 15, "#planks"), (0, 15, 0, 16, 16, 16, "#planks"),
] + [c for i, y in enumerate((1.5, 6, 10.5)) for c in (
    (2, y, 0.5, 14, y + 4, 1, "#planks"), (7, y + 1.5, 0, 9, y + 2.5, 0.5, "#metal"))]

def sofa(left_arm, right_arm):
    c = [(0, 0, 0, 16, 3, 16, "#planks"), (0, 0, 0, 1, 1, 1, "#planks"),
         (1 if left_arm else 0, 3, 1, 15 if right_arm else 16, 7, 12, "#wool"),     # assento
         (0, 3, 12, 16, 15, 16, "#wool")]                                           # encosto
    if left_arm: c.append((0, 3, 0, 2, 11, 12, "#wool"))
    if right_arm: c.append((14, 3, 0, 16, 11, 12, "#wool"))
    return c

PIECES["sofa_single"] = sofa(True, True)
PIECES["sofa_left"] = sofa(True, False)
PIECES["sofa_right"] = sofa(False, True)
PIECES["sofa_middle"] = sofa(False, False)
PIECES["sofa_corner"] = [(0, 0, 0, 16, 3, 16, "#planks"), (0, 3, 0, 12, 7, 12, "#wool"),
                         (0, 3, 12, 16, 15, 16, "#wool"), (12, 3, 0, 16, 15, 12, "#wool")]

# Cama de casal desenhada inteira (32×32, cabeceira a norte) e cortada em 4 quartos de 1 bloco.
BED = [
    (0, 0, 0, 32, 18, 2, "#planks"),                                   # cabeceira
    (1, 18, 0.5, 31, 19, 1.5, "#planks"),
    (0, 0, 30, 32, 8, 32, "#planks"),                                  # pés da cama
    (0, 0, 2, 2, 3, 30, "#planks"), (30, 0, 2, 32, 3, 30, "#planks"),   # laterais
    (0, 0, 2, 32, 2, 30, "#planks"),
    (1, 3, 2, 31, 8, 30, "#sheet"),                                    # colchão
    (0.5, 6, 11, 31.5, 9, 30, "#wool"),                                # cobertor
    (3, 8, 3, 14, 10.5, 9, "#sheet"), (18, 8, 3, 29, 10.5, 9, "#sheet"),  # almofadas
]

def clip(boxes, ox, oz):
    out = []
    for (x1, y1, z1, x2, y2, z2, v) in boxes:
        a, b = max(x1, ox), min(x2, ox + 16)
        c, d = max(z1, oz), min(z2, oz + 16)
        if a < b and c < d:
            out.append((a - ox, y1, c - oz, b - ox, y2, d - oz, v))
    return out

PIECES["double_bed_head_left"] = clip(BED, 0, 0)
PIECES["double_bed_head_right"] = clip(BED, 16, 0)
PIECES["double_bed_foot_left"] = clip(BED, 0, 16)
PIECES["double_bed_foot_right"] = clip(BED, 16, 16)

PIECES["chandelier"] = [
    (7.5, 11, 7.5, 8.5, 16, 8.5, "#metal"), (6, 9, 6, 10, 11, 10, "#metal"),
    (1, 9.5, 7.5, 15, 10.5, 8.5, "#metal"), (7.5, 9.5, 1, 8.5, 10.5, 15, "#metal"),
] + [c for (x, z) in ((1.5, 8), (14.5, 8), (8, 1.5), (8, 14.5)) for c in (
    (x - 1.5, 10.5, z - 1.5, x + 1.5, 11, z + 1.5, "#metal"),
    (x - 1, 11, z - 1, x + 1, 13, z + 1, "#light"))]

PIECES["wall_lamp"] = [  # presa à parede de trás (sul)
    (6, 3, 15, 10, 11, 16, "#metal"), (7.5, 9, 10, 8.5, 10, 15, "#metal"),
    (6.5, 6, 6.5, 9.5, 9, 9.5, "#light"),
    (5.5, 9, 5.5, 10.5, 10, 10.5, "#metal"), (5.5, 5, 5.5, 10.5, 6, 10.5, "#metal"),
    (5.5, 6, 5.5, 10.5, 9, 10.5, "#glass"),
]

PIECES["table_lamp"] = [
    (5, 0, 5, 11, 1, 11, "#metal"), (6, 1, 6, 10, 1.5, 10, "#metal"),
    (7.5, 1.5, 7.5, 8.5, 9, 8.5, "#metal"), (6.5, 8, 6.5, 9.5, 10, 9.5, "#light"),
    (4, 9, 4, 12, 14, 12, "#shade"),
]

# texturas das prévias (o mod troca estas por madeira/cor)
PREVIEW_TEX = {"#planks": "oak_planks", "#log": "oak_log", "#wool": "red_wool", "#sheet": "white_wool",
               "#metal": "iron_block", "#glass": "glass", "#light": "glowstone", "#shade": "white_wool"}

# ---------------- casa: cozinha, quarto, banheiro, sala ----------------
def counter_body(doors=True):
    c = [(0, 0, 2, 16, 1, 16, "#planks"), (0, 1, 1, 16, 14, 16, "#planks"), (0, 14, 0, 16, 16, 16, "#counter")]
    if doors:
        c += [(1, 2, 0.5, 7.5, 13, 1, "#planks"), (8.5, 2, 0.5, 15, 13, 1, "#planks"),
              (6.5, 9, 0, 7, 12, 0.5, "#metal"), (9, 9, 0, 9.5, 12, 0.5, "#metal")]
    return c

PIECES["counter"] = counter_body()
PIECES["counter_corner"] = [(0, 0, 2, 16, 1, 16, "#planks"), (0, 1, 1, 16, 14, 16, "#planks"), (0, 14, 0, 16, 16, 16, "#counter"),
                            (1, 2, 0.5, 15, 13, 1, "#planks"), (13, 9, 0, 13.5, 12, 0.5, "#metal")]
PIECES["sink"] = [
    (0, 0, 2, 16, 1, 16, "#planks"), (0, 1, 1, 16, 11, 16, "#planks"),
    (1, 2, 0.5, 7.5, 10, 1, "#planks"), (8.5, 2, 0.5, 15, 10, 1, "#planks"),
    (6.5, 6, 0, 7, 9, 0.5, "#metal"), (9, 6, 0, 9.5, 9, 0.5, "#metal"),
    (0, 11, 1, 3, 16, 16, "#counter"), (13, 11, 1, 16, 16, 16, "#counter"),
    (3, 11, 1, 13, 16, 3, "#counter"), (3, 11, 13, 13, 16, 16, "#counter"), (0, 14, 0, 16, 16, 1, "#counter"),
    (3, 11, 3, 13, 12, 13, "#metal"),                                           # fundo da cuba
    (7, 16, 13.5, 9, 20, 15.5, "#metal"), (7, 18.5, 10, 9, 20, 13.5, "#metal"),  # torneira
    (5, 16, 14, 6.5, 17, 15, "#metal"), (9.5, 16, 14, 11, 17, 15, "#metal"),
]
PIECES["sink_water"] = [(3, 12, 3, 13, 14.5, 13, "#water")]                     # camada de água (cheia)

PIECES["nightstand"] = [(1, 0, 1, 3, 2, 3, "#planks"), (13, 0, 1, 15, 2, 3, "#planks"), (1, 0, 13, 3, 2, 15, "#planks"),
                        (13, 0, 13, 15, 2, 15, "#planks"), (1, 2, 1, 15, 12, 15, "#planks"), (0.5, 12, 0.5, 15.5, 13, 15.5, "#planks"),
                        (2.5, 7, 0.5, 13.5, 11, 1, "#planks"), (7.5, 8.5, 0, 8.5, 9.5, 0.5, "#metal"),
                        (2.5, 2.5, 0.5, 13.5, 6.5, 1, "#planks"), (7.5, 4, 0, 8.5, 5, 0.5, "#metal")]

PIECES["wardrobe_bottom"] = [(0, 0, 0, 16, 1, 16, "#planks"), (1, 1, 1, 15, 16, 15, "#planks"),
                             (1.5, 1.5, 0.5, 7.75, 16, 1, "#planks"), (8.25, 1.5, 0.5, 14.5, 16, 1, "#planks")]
PIECES["wardrobe_top"] = [(1, 0, 1, 15, 15, 15, "#planks"), (0, 15, 0, 16, 16, 16, "#planks"),
                          (1.5, 0, 0.5, 7.75, 14, 1, "#planks"), (8.25, 0, 0.5, 14.5, 14, 1, "#planks"),
                          (6.5, 0, 0, 7.25, 5, 0.5, "#metal"), (8.75, 0, 0, 9.5, 5, 0.5, "#metal")]

PIECES["curtain_closed"] = [(0, 15, 14, 16, 16, 15, "#metal")] + [
    (x, 0, 14.5 + (0.6 if i % 2 else 0), x + 2, 15, 15.3 + (0.6 if i % 2 else 0), "#wool") for i, x in enumerate(range(0, 16, 2))]
PIECES["curtain_open"] = [(0, 15, 14, 16, 16, 15, "#metal"), (0, 0, 14, 3, 15, 16, "#wool"), (13, 0, 14, 16, 15, 16, "#wool"),
                          (0, 8, 13.5, 3, 9, 16, "#wool"), (13, 8, 13.5, 16, 9, 16, "#wool")]

PIECES["toilet"] = [(5, 0, 5, 11, 6, 12, "#porcelain"), (3, 6, 1, 13, 9, 12, "#porcelain"), (3.5, 9, 1.5, 12.5, 9.5, 11.5, "#planks"),
                    (3, 6, 12, 13, 16, 16, "#porcelain"), (2.5, 16, 11.5, 13.5, 17, 16, "#porcelain"), (11, 14, 11, 12.5, 14.5, 12, "#metal")]

TUB = [(0, 0, 0, 16, 2, 32, "#porcelain"),
       (0, 2, 0, 16, 10, 1.5, "#porcelain"), (0, 2, 30.5, 16, 10, 32, "#porcelain"),
       (0, 2, 1.5, 1.5, 10, 30.5, "#porcelain"), (14.5, 2, 1.5, 16, 10, 30.5, "#porcelain"),
       (7, 10, 0.5, 9, 13, 2, "#metal"), (7, 12, 2, 9, 13, 4.5, "#metal"), (4.5, 10, 0.5, 6, 11, 1.5, "#metal"), (10, 10, 0.5, 11.5, 11, 1.5, "#metal")]
def clipz(boxes, oz):
    return [(x1, y1, max(z1, oz) - oz, x2, y2, min(z2, oz + 16) - oz, v) for (x1, y1, z1, x2, y2, z2, v) in boxes if max(z1, oz) < min(z2, oz + 16)]
PIECES["bathtub_head"] = clipz(TUB, 0)
PIECES["bathtub_foot"] = clipz(TUB, 16)
PIECES["bathtub_water_head"] = [(1.5, 2, 1.5, 14.5, 8, 16, "#water")]
PIECES["bathtub_water_foot"] = [(1.5, 2, 0, 14.5, 8, 14.5, "#water")]

PIECES["fireplace"] = [
    (0, 0, 10, 16, 16, 16, "#bricks"), (0, 0, 0, 3, 13, 10, "#bricks"), (13, 0, 0, 16, 13, 10, "#bricks"),
    (0, 13, 0, 16, 16, 10, "#bricks"), (-0.5, 13, -1, 16.5, 14, 11, "#planks"),      # cornija de madeira
    (3, 0, 0, 13, 1, 10, "#stone"), (4, 1, 4, 12, 2.5, 6, "#log"), (5, 1, 6.5, 11, 2.5, 8.5, "#log"),
    (4.5, 2.5, 5, 11.5, 10, 8, "#fire"),
]

PIECES["ceiling_fan"] = [(7.5, 12, 7.5, 8.5, 16, 8.5, "#metal"), (6, 9, 6, 10, 12, 10, "#metal"), (6.5, 8, 6.5, 9.5, 9, 9.5, "#light"),
                         (10, 10, 7, 16, 10.5, 9, "#planks"), (0, 10, 7, 6, 10.5, 9, "#planks"),
                         (7, 10, 10, 9, 10.5, 16, "#planks"), (7, 10, 0, 9, 10.5, 6, "#planks")]

PIECES["trash_can"] = [(4, 0, 4, 12, 11, 12, "#metal"), (3.5, 11, 3.5, 12.5, 12, 12.5, "#metal"), (7, 12, 6.5, 9, 13, 9.5, "#metal")]

PIECES["wall_clock"] = [(3, 3, 15, 13, 13, 16, "#planks"), (4, 4, 14.5, 12, 12, 15, "#sheet"),
                        (7.5, 7.5, 14, 8.5, 11, 14.5, "#black"), (8.5, 7.5, 14, 11, 8.5, 14.5, "#black"), (7.5, 7.5, 13.8, 8.5, 8.5, 14, "#metal")]

PIECES["mailbox"] = [(7, 0, 7, 9, 10, 9, "#planks"), (4, 10, 3, 12, 15, 13, "#metal"), (4.5, 15, 3, 11.5, 16, 13, "#metal"),
                     (5, 11, 2.5, 11, 14, 3, "#black"), (12, 12, 9, 12.5, 16, 10, "#flag"), (12, 15, 10, 12.5, 16, 12, "#flag")]

PIECES["mirror"] = [(2, 1, 15, 14, 15, 16, "#planks"), (3, 2, 14.5, 13, 14, 15, "#mirror")]

PREVIEW_TEX.update({"#counter": "smooth_stone", "#porcelain": "quartz_block_side", "#water": "water_still", "#fire": "fire_0",
                    "#bricks": "bricks", "#black": "black_wool", "#flag": "red_wool", "#mirror": "light_blue_stained_glass",
                    "#stone": "stone"})

# ---------------- mais peças (rodada 3) ----------------
def clip3(boxes, ox, oy, oz):
    out = []
    for (x1, y1, z1, x2, y2, z2, v) in boxes:
        a, b = max(x1, ox), min(x2, ox + 16); c, d = max(y1, oy), min(y2, oy + 16); e, f = max(z1, oz), min(z2, oz + 16)
        if a < b and c < d and e < f: out.append((a - ox, c - oy, e - oz, b - ox, d - oy, f - oz, v))
    return out

PIECES["desk"] = [(0, 14, 0, 16, 16, 16, "#planks"), (0.5, 0, 1, 2.5, 14, 15, "#planks"), (10, 0, 1, 15.5, 14, 15, "#planks"),
                  (2.5, 6, 14, 10, 14, 15, "#planks")] + [c for y in (9.5, 5, 0.5) for c in (
                  (10.5, y, 0.5, 15, y + 4, 1, "#planks"), (12.25, y + 1.5, 0, 13.25, y + 2.5, 0.5, "#metal"))]
PIECES["coffee_table"] = [(0, 6, 2, 16, 8, 14, "#planks"), (1, 2, 3, 15, 3, 13, "#planks"),
                          (1, 0, 3, 3, 6, 5, "#planks"), (13, 0, 3, 15, 6, 5, "#planks"), (1, 0, 11, 3, 6, 13, "#planks"), (13, 0, 11, 15, 6, 13, "#planks")]
PIECES["side_table"] = [(3, 12, 3, 13, 13, 13, "#planks"), (2, 12, 4, 14, 13, 12, "#planks"), (4, 12, 2, 12, 13, 14, "#planks"),
                        (7, 1, 7, 9, 12, 9, "#planks"), (5, 0, 5, 11, 1, 11, "#planks"), (4, 0, 7, 12, 0.5, 9, "#planks")]
PIECES["garden_bench"] = [(0, 7, z, 16, 8, z + 2, "#planks") for z in (2, 5, 8, 11)] + [
    (0, 10, 13, 16, 11.5, 14, "#planks"), (0, 13, 13, 16, 14.5, 14, "#planks"),
    (0, 0, 2, 1.5, 7, 3, "#metal"), (14.5, 0, 2, 16, 7, 3, "#metal"), (0, 0, 13.5, 1.5, 15, 14.5, "#metal"), (14.5, 0, 13.5, 16, 15, 14.5, "#metal"),
    (0, 10, 2, 1.5, 11, 13.5, "#metal"), (14.5, 10, 2, 16, 11, 13.5, "#metal"), (0, 7, 2, 1.5, 10, 3, "#metal"), (14.5, 7, 2, 16, 10, 3, "#metal")]
PIECES["planter_box"] = [(0, 0, 0, 16, 10, 1.5, "#planks"), (0, 0, 14.5, 16, 10, 16, "#planks"), (0, 0, 1.5, 1.5, 10, 14.5, "#planks"),
                         (14.5, 0, 1.5, 16, 10, 14.5, "#planks"), (1.5, 0, 1.5, 14.5, 9, 14.5, "#soil")]
def shutter_panel(x0, x1, z0, z1):
    return [(x0, 0, z0, x1, 16, z1, "#planks")] + [(x0 + 0.5, y, z0 - 0.4, x1 - 0.5, y + 1, z0, "#planks") for y in range(2, 15, 3)]
PIECES["shutters_closed"] = shutter_panel(0, 7.8, 15, 16) + shutter_panel(8.2, 16, 15, 16)
PIECES["shutters_open"] = [(0, 0, 8, 1, 16, 16, "#planks"), (15, 0, 8, 16, 16, 16, "#planks")] + \
    [(-0.4, y, 8.5, 0, y + 1, 15.5, "#planks") for y in range(2, 15, 3)] + [(16, y, 8.5, 16.4, y + 1, 15.5, "#planks") for y in range(2, 15, 3)]
PIECES["wine_rack"] = [(0, 0, 15, 16, 16, 16, "#planks"), (0, 0, 0, 16, 1, 15, "#planks"), (0, 15, 0, 16, 16, 15, "#planks"),
                       (0, 1, 0, 1, 15, 15, "#planks"), (15, 1, 0, 16, 15, 15, "#planks"), (5.3, 1, 0, 5.8, 15, 15, "#planks"),
                       (10.2, 1, 0, 10.7, 15, 15, "#planks"), (1, 5.3, 0, 15, 5.8, 15, "#planks"), (1, 10.2, 0, 15, 10.7, 15, "#planks")] + [
    c for x in (1.2, 6, 10.9) for y in (1.2, 6, 10.9) for c in ((x + 0.4, y + 0.4, 2, x + 3.6, y + 3.6, 14, "#bottle"), (x + 1.5, y + 1.5, 0.5, x + 2.5, y + 2.5, 2, "#planks"))]
PIECES["floor_lamp_bottom"] = [(4, 0, 4, 12, 1, 12, "#metal"), (5, 1, 5, 11, 1.5, 11, "#metal"), (7.5, 1.5, 7.5, 8.5, 16, 8.5, "#metal")]
PIECES["floor_lamp_top"] = [(7.5, 0, 7.5, 8.5, 6, 8.5, "#metal"), (6.5, 5, 6.5, 9.5, 8, 9.5, "#light"), (3, 6, 3, 13, 14, 13, "#shade")]
BUNK = [(0, 0, 0, 2, 30, 2, "#planks"), (14, 0, 0, 16, 30, 2, "#planks"), (0, 0, 30, 2, 30, 32, "#planks"), (14, 0, 30, 16, 30, 32, "#planks"),
        (0, 2, 0, 16, 4, 32, "#planks"), (2, 4, 2, 14, 7, 30, "#sheet"), (1.5, 5, 11, 14.5, 7.5, 30, "#wool"), (3, 7, 3, 13, 9, 8, "#sheet"),
        (0, 17, 0, 16, 19, 32, "#planks"), (2, 19, 2, 14, 22, 30, "#sheet"), (1.5, 20, 11, 14.5, 22.5, 30, "#wool"), (3, 22, 3, 13, 24, 8, "#sheet"),
        (0, 23, 9, 1, 25, 30, "#planks"), (15, 23, 9, 16, 25, 30, "#planks")] + [(2, y, 30.5, 14, y + 1, 31.5, "#planks") for y in (8, 12, 16)]
for (nm, oy, oz) in (("bunk_bed_bottom_head", 0, 0), ("bunk_bed_bottom_foot", 0, 16), ("bunk_bed_top_head", 16, 0), ("bunk_bed_top_foot", 16, 16)):
    PIECES[nm] = clip3(BUNK, 0, oy, oz)
PIANO = [(0, 0, 8, 32, 22, 16, "#planks"), (0, 22, 7, 32, 23, 16, "#planks"), (0, 11, 1, 32, 12, 9, "#planks"),
         (1, 0, 2, 3, 11, 4, "#planks"), (29, 0, 2, 31, 11, 4, "#planks"), (1, 12, 2, 31, 13.5, 8, "#sheet"),
         (4, 17, 7, 28, 20, 8, "#planks"), (0, 12, 1, 1, 15, 8, "#planks"), (31, 12, 1, 32, 15, 8, "#planks")] + [
    (1.5 + i * 2.1, 13.5, 4.5, 2.5 + i * 2.1, 14.3, 8, "#black") for i in range(14) if i % 7 not in (2, 6)]
PIECES["piano_left"] = clip3(PIANO, 0, 0, 0)
PIECES["piano_right"] = clip3(PIANO, 16, 0, 0)
PIECES["safe"] = [(1, 0, 1, 15, 15, 15, "#metal"), (0.5, 15, 0.5, 15.5, 16, 15.5, "#metal"), (2, 1, 0.5, 14, 14, 1, "#metal"),
                  (6, 6, 0, 10, 10, 0.5, "#black"), (7.5, 9, -0.2, 8.5, 9.8, 0, "#sheet"), (11, 6.5, 0, 12.5, 9.5, 0.5, "#black")]
PIECES["awning"] = [(0, 12, 12, 16, 13, 16, "#wool"), (0, 11, 8, 16, 12, 12, "#wool"), (0, 10, 4, 16, 11, 8, "#wool"),
                    (0, 9, 0, 16, 10, 4, "#wool"), (0, 7, 0, 16, 9, 0.5, "#wool"), (0, 13, 15, 16, 14, 16, "#metal")]
PREVIEW_TEX.update({"#soil": "dirt", "#bottle": "green_stained_glass"})

# ---------------- peças que o mod 0.1.0 pediu (estados) ----------------
_CAB_BODY = [(0, 0, 0, 16, 1, 16, "#planks"), (1, 1, 1, 15, 15, 15, "#planks"), (0, 15, 0, 16, 16, 16, "#planks")]
PIECES["cabinet_open"] = _CAB_BODY + [(1, 2, 0.6, 15, 14, 1, "#log"),                       # interior escuro
    (1.5, 2, -5.5, 2, 14, 0.5, "#planks"), (14, 2, -5.5, 14.5, 14, 0.5, "#planks"),          # portas abertas
    (1, 7, -5, 1.5, 10, -4, "#metal"), (14.5, 7, -5, 15, 10, -4, "#metal")]
PIECES["drawer_open"] = _CAB_BODY + [(2, 1.5, 0.5, 14, 5.5, 1, "#planks"), (7, 3, 0, 9, 4, 0.5, "#metal"),
    (2, 6, 0.5, 14, 10, 1, "#planks"), (7, 7.5, 0, 9, 8.5, 0.5, "#metal"),
    (2.5, 10.5, -5, 13.5, 11, 1, "#planks"), (2.5, 11, -5, 3, 14.5, 1, "#planks"), (13, 11, -5, 13.5, 14.5, 1, "#planks"),
    (2, 10.5, -5.5, 14, 14.5, -5, "#planks"), (7, 12, -6, 9, 13, -5.5, "#metal")]           # gaveta de cima puxada
PIECES["sofa_corner_inner"] = [(0, 0, 0, 16, 3, 16, "#planks"), (4, 3, 0, 16, 7, 12, "#wool"),
                               (0, 3, 12, 16, 15, 16, "#wool"), (0, 3, 0, 4, 15, 12, "#wool")]
PIECES["sofa_corner_outer"] = [(0, 0, 0, 16, 3, 16, "#planks"), (0, 3, 0, 16, 7, 12, "#wool"), (4, 3, 12, 16, 7, 16, "#wool"),
                               (0, 3, 12, 4, 15, 16, "#wool")]
PIECES["table_leg"] = [(1, 0, 1, 3, 14, 3, "#log"), (3, 12, 1.5, 8, 14, 2.5, "#planks"), (1.5, 12, 3, 2.5, 14, 8, "#planks")]
