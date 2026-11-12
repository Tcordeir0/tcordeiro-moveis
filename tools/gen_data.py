#!/usr/bin/env python3
"""Gera os dados do mod Móveis do Tcordeiro (tmoveis) a partir do jar do Minecraft 26.3:

  - a lista de madeiras (WoodTypes.java) — lida do jogo (tag minecraft:planks), não inventada;
  - modelos-filho (por madeira/cor), blockstates, definições de itens, loot tables, receitas, conquistas das receitas, tags;
  - traduções (pt_br, pt_pt, en_us) com os nomes das madeiras e cores tirados das traduções do próprio jogo;
  - o guia: traduções guide.tmoveis.*, o livro escrito (receita tmoveis:guide, em pt_br) e MoveisGuide.java (separador «Guias»
    do Receitário).

Os modelos-PAI ficam em assets/tmoveis/models/block/template/<peça>.json, com variáveis de textura (#planks, #log, #wool, #metal,
#glass, #light, #particle). Este script só os cria se ainda NÃO existirem (são provisórios, em cubos; os definitivos vêm do Blender
e substituem estes ficheiros sem mexer em mais nada). Para os recriar, apagar o ficheiro e correr de novo.

Uso: python3 tools/gen_data.py
"""
import json
import os
import zipfile

HOME = os.path.expanduser("~")
MC = os.path.join(HOME, "Library", "Application Support", "horizon-launcher", "minecraft")
JAR = os.path.join(MC, "versions", "26.3", "26.3.jar")
ASSET_INDEX = os.path.join(MC, "assets", "indexes", "34.json")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src", "main", "resources")
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "tcordeiro", "moveis")
MOD = "tmoveis"

DYES = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown",
        "green", "red", "black"]
SOFA_FRAME = "minecraft:block/oak_planks"  # a estrutura do sofá e da cama é de madeira genérica (receita aceita qualquer tábua)
PILLOW = "minecraft:block/white_wool"

written = set()


def write(rel, data):
    path = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")
    written.add(rel)


# ------------------------------------------------------------------ o jogo

zjar = zipfile.ZipFile(JAR)


def jar_json(path):
    return json.load(zjar.open(path))


def jar_has(path):
    try:
        zjar.getinfo(path)
        return True
    except KeyError:
        return False


def asset_lang(code):
    idx = json.load(open(ASSET_INDEX))["objects"]
    h = idx[f"minecraft/lang/{code}.json"]["hash"]
    return json.load(open(os.path.join(MC, "assets", "objects", h[:2], h)))


LANG = {"en": jar_json("assets/minecraft/lang/en_us.json"), "br": asset_lang("pt_br"), "pt": asset_lang("pt_pt")}
NON_FLAMMABLE = set(jar_json("data/minecraft/tags/item/non_flammable_wood.json")["values"])


def woods():
    """As madeiras do jogo, pela ordem da tag minecraft:planks. Para cada uma: tábuas, laje e textura do tronco descascado."""
    out = []
    for planks in jar_json("data/minecraft/tags/item/planks.json")["values"]:
        assert planks.startswith("minecraft:") and planks.endswith("_planks"), planks
        wid = planks[len("minecraft:"):-len("_planks")]
        slab = f"{wid}_slab"
        assert jar_has(f"data/minecraft/recipe/{slab}.json"), f"sem laje: {wid}"
        log = None
        for cand in (f"stripped_{wid}_log", f"stripped_{wid}_stem", f"stripped_{wid}_block"):
            if jar_has(f"assets/minecraft/textures/block/{cand}.png"):
                log = cand
                break
        assert log, f"sem tronco descascado: {wid}"
        out.append({"id": wid, "planks": f"{wid}_planks", "slab": slab, "log": log, "flammable": planks not in NON_FLAMMABLE})
    return out


WOODS = woods()


def wood_names(w):
    """Nome da madeira em cada língua, a partir de «Tábuas de X» / «X Planks»: (masculino, feminino) em pt; en simples."""
    out = {"en": LANG["en"][f"block.minecraft.{w['planks']}"].removesuffix(" Planks")}
    for lg in ("br", "pt"):
        rest = LANG[lg][f"block.minecraft.{w['planks']}"].removeprefix("Tábuas ")
        if rest.split(" ")[0] in ("de", "da", "do", "das", "dos"):
            out[lg] = (rest, rest)
        elif rest.endswith("as"):  # adjetivo no feminino plural (Distorcidas)
            out[lg] = (rest[:-2] + "o", rest[:-1])
        elif rest.endswith("ins"):  # plural de «-im» (Carmesins em pt_pt)
            out[lg] = (rest[:-3] + "im", rest[:-3] + "im")
        else:  # invariável (Carmesim)
            out[lg] = (rest, rest)
    return out


def color_names(c):
    """Cor: masculino (do tapete), feminino (da cama) em pt; en simples."""
    return {"en": LANG["en"][f"block.minecraft.{c}_carpet"].removesuffix(" Carpet"),
            "br": (LANG["br"][f"block.minecraft.{c}_carpet"].removeprefix("Tapete "), LANG["br"][f"block.minecraft.{c}_bed"].removeprefix("Cama ")),
            "pt": (LANG["pt"][f"block.minecraft.{c}_carpet"].removeprefix("Tapete "), LANG["pt"][f"block.minecraft.{c}_bed"].removeprefix("Cama "))}


# ------------------------------------------------------------------ traduções

TR = {"br": {}, "pt": {}, "en": {}}


def tr(key, br, pt, en):
    TR["br"][key], TR["pt"][key], TR["en"][key] = br, pt, en


# nomes dos móveis de madeira: (pt_br, pt_pt, en, género em pt: m/f)
FURN = {
    "chair": ("Cadeira", "Cadeira", "Chair", "f"),
    "stool": ("Banco", "Banco", "Stool", "m"),
    "table": ("Mesa", "Mesa", "Table", "f"),
    "cabinet": ("Armário", "Armário", "Cabinet", "m"),
    "drawer": ("Gaveteiro", "Cómoda", "Chest of Drawers", "mf"),  # gaveteiro (m) no Brasil, cómoda (f) em Portugal
}
LAMPS = {
    "chandelier": ("Lustre", "Lustre", "Chandelier", 15),
    "wall_lamp": ("Arandela", "Arandela", "Wall Sconce", 14),
    "table_lamp": ("Luminária de Mesa", "Candeeiro de Mesa", "Table Lamp", 13),
}


def gen_lang_names():
    for w in WOODS:
        n = wood_names(w)
        for f, (br, pt, en, g) in FURN.items():
            gbr = 1 if g in ("f",) else 0
            gpt = 1 if g in ("f", "mf") else 0
            tr(f"block.{MOD}.{w['id']}_{f}", f"{br} {n['br'][gbr]}", f"{pt} {n['pt'][gpt]}", f"{n['en']} {en}")
    for c in DYES:
        n = color_names(c)
        tr(f"block.{MOD}.{c}_sofa", f"Sofá {n['br'][0]}", f"Sofá {n['pt'][0]}", f"{n['en']} Sofa")
        tr(f"block.{MOD}.{c}_double_bed", f"Cama de Casal {n['br'][1]}", f"Cama de Casal {n['pt'][1]}", f"{n['en']} Double Bed")
    for k, (br, pt, en, _) in LAMPS.items():
        tr(f"block.{MOD}.{k}", br, pt, en)
    tr(f"itemGroup.{MOD}.furniture", "Móveis do Tcordeiro", "Móveis do Tcordeiro", "Tcordeiro's Furniture")
    tr(f"entity.{MOD}.seat", "Assento", "Assento", "Seat")
    tr(f"message.{MOD}.occupied", "Alguém já está sentado aqui.", "Já está alguém sentado aqui.", "Someone is already sitting here.")


# ------------------------------------------------------------------ modelos-pai (templates provisórios)

FACES = ("north", "south", "east", "west", "up", "down")


def el(x0, y0, z0, x1, y1, z1, tex):
    return {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": {f: {"texture": f"#{tex}"} for f in FACES}}


def template(name, elements, particle, extra=None):
    """Escreve template/<name>.json só se ainda não existir (os definitivos vêm do Blender)."""
    rel = f"assets/{MOD}/models/block/template/{name}.json"
    written.add(rel)
    if os.path.exists(os.path.join(RES, rel)):
        return
    data = {"textures": {"particle": f"#{particle}"}, "elements": elements}
    if extra:
        data.update(extra)
    write(rel, data)


BED_DISPLAY = {"display": {
    "gui": {"rotation": [30, 160, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.2, 0.2, 0.2]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.25, 0.25, 0.25]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]}}}


def gen_templates():
    # cadeira virada a norte (quem se senta olha para norte; encosto a sul)
    legs = lambda a, b, h: [el(a, 0, a, a + 2, h, a + 2, "log"), el(b, 0, a, b + 2, h, a + 2, "log"),
                            el(a, 0, b, a + 2, h, b + 2, "log"), el(b, 0, b, b + 2, h, b + 2, "log")]
    template("chair", legs(2, 12, 8) + [el(2, 8, 2, 14, 10, 14, "planks"), el(2, 10, 12, 4, 24, 14, "log"), el(12, 10, 12, 14, 24, 14, "log"),
                                        el(4, 14, 12.5, 12, 16, 13.5, "planks"), el(4, 19, 12.5, 12, 23, 13.5, "planks")], "planks")
    template("stool", legs(3, 11, 8) + [el(2, 8, 2, 14, 10, 14, "planks")], "planks")
    # mesa: tampo + uma perna no canto noroeste (o blockstate roda-a para os outros cantos)
    template("table_top", [el(0, 14, 0, 16, 16, 16, "planks")], "planks")
    template("table_leg", [el(1, 0, 1, 3, 14, 3, "log")], "log")
    # armário e gaveteiro: frente a norte; o fundo por trás da frente usa #log (é o «interior» que se vê aberto)
    frame = [el(0, 0, 0, 16, 1, 1, "log"), el(0, 15, 0, 16, 16, 1, "log"), el(0, 1, 0, 1, 15, 1, "log"), el(15, 1, 0, 16, 15, 1, "log")]
    body = el(0, 0, 1, 16, 16, 16, "planks")
    body["faces"]["north"]["texture"] = "#log"
    template("cabinet", [body] + frame + [el(1, 1, 0, 8, 15, 1, "planks"), el(8, 1, 0, 15, 15, 1, "planks"),
                                          el(6.5, 7, -0.5, 7.5, 9, 0, "log"), el(8.5, 7, -0.5, 9.5, 9, 0, "log")], "planks")
    template("cabinet_open", [body] + frame + [el(0, 1, -6, 1, 15, 0, "planks"), el(15, 1, -6, 16, 15, 0, "planks")], "planks")
    fronts = lambda top_z: [el(1, 1, 0, 15, 5, 1, "planks"), el(1, 6, 0, 15, 10, 1, "planks"),
                            el(1, 5, 0, 15, 6, 1, "log"), el(1, 10, 0, 15, 11, 1, "log"),
                            el(7, 2.5, -1, 9, 3.5, 0, "log"), el(7, 7.5, -1, 9, 8.5, 0, "log")]
    template("drawer", [body] + frame + fronts(0) + [el(1, 11, 0, 15, 15, 1, "planks"), el(7, 12.5, -1, 9, 13.5, 0, "log")], "planks")
    template("drawer_open", [body] + frame + fronts(0) + [el(1, 11, -6, 15, 15, -5, "planks"), el(7, 12.5, -7, 9, 13.5, -6, "log"),
                                                         el(1, 11, -5, 2, 14, 0, "planks"), el(14, 11, -5, 15, 14, 0, "planks"),
                                                         el(2, 11, -5, 14, 12, 0, "planks")], "planks")
    # sofá virado a norte (encosto a sul; esquerda de quem se senta = oeste)
    base = el(0, 0, 0, 16, 3, 16, "planks")
    back = el(0, 3, 12, 16, 15, 16, "wool")
    arm_l, arm_r = el(0, 3, 0, 3, 11, 12, "wool"), el(13, 3, 0, 16, 11, 12, "wool")
    cush = lambda a, b: el(a, 3, 0, b, 8, 12, "wool")
    template("sofa_single", [base, back, arm_l, arm_r, cush(3, 13)], "wool")
    template("sofa_left", [base, back, arm_l, cush(3, 16)], "wool")
    template("sofa_right", [base, back, arm_r, cush(0, 13)], "wool")
    template("sofa_middle", [base, back, cush(0, 16)], "wool")
    template("sofa_corner_inner", [base, back, el(0, 3, 0, 4, 15, 12, "wool"), cush(4, 16)], "wool")
    template("sofa_corner_outer", [base, el(0, 3, 12, 4, 15, 16, "wool"), cush(0, 16), el(4, 3, 12, 16, 8, 16, "wool")], "wool")
    # cama de casal virada a sul (como as camas vanilla: cabeceira a +z); a coluna LEFT tem o lado de fora a este, a RIGHT a oeste
    pillow = {"from": [2, 9, 6], "to": [14, 11, 12], "faces": {f: {"texture": "#pillow"} for f in FACES}}
    bed = lambda els: (els, "wool", {"textures": {"particle": "#wool", "pillow": PILLOW}, **BED_DISPLAY})
    for name, els in {
        "double_bed_foot_left": [el(0, 0, 0, 14, 11, 2, "planks"), el(14, 0, 0, 16, 13, 2, "log"), el(0, 0, 2, 16, 3, 16, "planks"),
                                 el(0, 3, 2, 16, 9, 16, "wool")],
        "double_bed_foot_right": [el(2, 0, 0, 16, 11, 2, "planks"), el(0, 0, 0, 2, 13, 2, "log"), el(0, 0, 2, 16, 3, 16, "planks"),
                                  el(0, 3, 2, 16, 9, 16, "wool")],
        "double_bed_head_left": [el(0, 0, 14, 14, 16, 16, "planks"), el(14, 0, 14, 16, 18, 16, "log"), el(0, 0, 0, 16, 3, 14, "planks"),
                                 el(0, 3, 0, 16, 9, 14, "wool"), pillow],
        "double_bed_head_right": [el(2, 0, 14, 16, 16, 16, "planks"), el(0, 0, 14, 2, 18, 16, "log"), el(0, 0, 0, 16, 3, 14, "planks"),
                                  el(0, 3, 0, 16, 9, 14, "wool"), pillow],
    }.items():
        els_, part, extra = bed(els)
        rel = f"assets/{MOD}/models/block/template/{name}.json"
        written.add(rel)
        if not os.path.exists(os.path.join(RES, rel)):
            write(rel, {**extra, "elements": els_})
    # lâmpadas: #metal (estrutura), #glass (cúpula), #light (lâmpada: muda de textura quando acende)
    bulbs = [(2, 7), (12, 7), (7, 2), (7, 12)]
    template("chandelier", [el(7.5, 11, 7.5, 8.5, 16, 8.5, "metal"), el(6, 8, 6, 10, 11, 10, "metal"), el(2, 9, 7.5, 14, 10, 8.5, "metal"),
                            el(7.5, 9, 2, 8.5, 10, 14, "metal")]
             + [el(x, 10, z, x + 2, 13, z + 2, "light") for x, z in bulbs]
             + [el(x - 1, 10, z - 1, x + 3, 14, z + 3, "glass") for x, z in bulbs], "metal")
    template("wall_lamp", [el(5, 3, 15, 11, 13, 16, "metal"), el(7.5, 7, 11, 8.5, 8, 15, "metal"), el(6.5, 6, 6.5, 9.5, 10, 9.5, "light"),
                           el(5, 5, 5, 11, 13, 11, "glass")], "metal")
    template("table_lamp", [el(5, 0, 5, 11, 1, 11, "metal"), el(7.5, 1, 7.5, 8.5, 7, 8.5, "metal"), el(6.5, 7, 6.5, 9.5, 10, 9.5, "light"),
                            el(4, 7, 4, 12, 13, 12, "glass")], "metal")


# ------------------------------------------------------------------ modelos-filho, blockstates, itens

YROT = {"north": 0, "east": 90, "south": 180, "west": 270}
BED_YROT = {"south": 0, "west": 90, "north": 180, "east": 270}  # os modelos das camas estão virados a sul
CCW = {"north": "west", "west": "south", "south": "east", "east": "north"}


def child(name, tmpl, textures):
    write(f"assets/{MOD}/models/block/{name}.json", {"parent": f"{MOD}:block/template/{tmpl}", "textures": textures})


def var(model, y=0):
    v = {"model": f"{MOD}:block/{model}"}
    if y:
        v["y"] = y
    return v


def item_model(name, model):
    write(f"assets/{MOD}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/{model}"}})


def composite_item(name, parts):
    """Item feito de vários modelos (mesa com as 4 pernas, cama com as 4 partes). parts: (modelo, rotação y em graus, translação)."""
    q = {0: [0, 0, 0, 1], 90: [0, -0.70710677, 0, 0.70710677], 180: [0, 1, 0, 0], 270: [0, 0.70710677, 0, 0.70710677]}
    models = []
    for model, y, t in parts:
        m = {"type": "minecraft:model", "model": f"{MOD}:block/{model}"}
        if y or any(t):
            m["transformation"] = {"left_rotation": q[y], "right_rotation": [0, 0, 0, 1], "scale": [1, 1, 1], "translation": t}
        models.append(m)
    write(f"assets/{MOD}/items/{name}.json", {"model": {"type": "minecraft:composite", "models": models}})


def gen_wooden():
    for w in WOODS:
        tex = {"planks": f"minecraft:block/{w['planks']}", "log": f"minecraft:block/{w['log']}", "particle": f"minecraft:block/{w['planks']}"}
        wid = w["id"]
        # cadeira: vira para onde o jogador olha
        child(f"{wid}_chair", "chair", tex)
        write(f"assets/{MOD}/blockstates/{wid}_chair.json", {"variants": {f"facing={f}": var(f"{wid}_chair", y) for f, y in YROT.items()}})
        item_model(f"{wid}_chair", f"{wid}_chair")
        # banco
        child(f"{wid}_stool", "stool", tex)
        write(f"assets/{MOD}/blockstates/{wid}_stool.json", {"variants": {"": var(f"{wid}_stool")}})
        item_model(f"{wid}_stool", f"{wid}_stool")
        # mesa: tampo sempre; perna em cada canto cujos dois lados não têm mesa (a perna-modelo está a noroeste)
        child(f"{wid}_table_top", "table_top", tex)
        child(f"{wid}_table_leg", "table_leg", tex)
        corners = [("north", "west", 0), ("north", "east", 90), ("south", "east", 180), ("south", "west", 270)]
        parts = [{"apply": var(f"{wid}_table_top")}]
        for a, b, y in corners:
            parts.append({"when": {a: "false", b: "false"}, "apply": var(f"{wid}_table_leg", y)})
        write(f"assets/{MOD}/blockstates/{wid}_table.json", {"multipart": parts})
        composite_item(f"{wid}_table", [(f"{wid}_table_top", 0, [0, 0, 0]), (f"{wid}_table_leg", 0, [0, 0, 0]),
                                        (f"{wid}_table_leg", 90, [1, 0, 0]), (f"{wid}_table_leg", 180, [1, 0, 1]),
                                        (f"{wid}_table_leg", 270, [0, 0, 1])])
        # armário e gaveteiro: frente para o jogador, OPEN enquanto alguém tem o inventário aberto
        for k in ("cabinet", "drawer"):
            child(f"{wid}_{k}", k, tex)
            child(f"{wid}_{k}_open", f"{k}_open", tex)
            write(f"assets/{MOD}/blockstates/{wid}_{k}.json", {"variants": {
                f"facing={f},open={o}": var(f"{wid}_{k}" + ("_open" if o == "true" else ""), y) for f, y in YROT.items() for o in ("false", "true")}})
            item_model(f"{wid}_{k}", f"{wid}_{k}")


SOFA_MODELS = {"single": "sofa_single", "left": "sofa_left", "right": "sofa_right", "middle": "sofa_middle",
               "inner_left": "sofa_corner_inner", "inner_right": "sofa_corner_inner", "outer_left": "sofa_corner_outer",
               "outer_right": "sofa_corner_outer"}


def gen_sofas_beds():
    for c in DYES:
        tex = {"wool": f"minecraft:block/{c}_wool", "planks": SOFA_FRAME, "particle": f"minecraft:block/{c}_wool"}
        for t in sorted(set(SOFA_MODELS.values())):
            child(f"{c}_{t}", t, tex)
        variants = {}
        for f in YROT:
            for shape, t in SOFA_MODELS.items():
                # cantos «left»: o modelo de canto rodado 90° para a esquerda (SofaBlock.modelFacing)
                mf = CCW[f] if shape in ("inner_left", "outer_left") else f
                variants[f"facing={f},shape={shape}"] = var(f"{c}_{t}", YROT[mf])
        write(f"assets/{MOD}/blockstates/{c}_sofa.json", {"variants": variants})
        item_model(f"{c}_sofa", f"{c}_sofa_single")
        # cama de casal
        btex = {"wool": f"minecraft:block/{c}_wool", "planks": SOFA_FRAME, "log": "minecraft:block/stripped_oak_log",
                "particle": f"minecraft:block/{c}_wool"}
        for part in ("head", "foot"):
            for side in ("left", "right"):
                child(f"{c}_double_bed_{part}_{side}", f"double_bed_{part}_{side}", btex)
        write(f"assets/{MOD}/blockstates/{c}_double_bed.json", {"variants": {
            f"facing={f},part={p},side={s}": var(f"{c}_double_bed_{p}_{s}", y) for f, y in BED_YROT.items() for p in ("head", "foot")
            for s in ("left", "right")}})
        # item: as 4 partes, centradas (a coluna direita fica a oeste, a cabeceira a sul)
        composite_item(f"{c}_double_bed", [(f"{c}_double_bed_foot_left", 0, [0.5, 0, -0.5]), (f"{c}_double_bed_head_left", 0, [0.5, 0, 0.5]),
                                           (f"{c}_double_bed_foot_right", 0, [-0.5, 0, -0.5]), (f"{c}_double_bed_head_right", 0, [-0.5, 0, 0.5])])


def gen_lamps():
    base = {"metal": "minecraft:block/copper_block", "glass": "minecraft:block/glass", "particle": "minecraft:block/copper_block"}
    for k in LAMPS:
        child(k, k, {**base, "light": "minecraft:block/redstone_lamp"})
        child(f"{k}_lit", k, {**base, "light": "minecraft:block/redstone_lamp_on"})
        item_model(k, k)
    write(f"assets/{MOD}/blockstates/chandelier.json", {"variants": {"lit=false": var("chandelier"), "lit=true": var("chandelier_lit")}})
    write(f"assets/{MOD}/blockstates/table_lamp.json", {"variants": {"lit=false": var("table_lamp"), "lit=true": var("table_lamp_lit")}})
    write(f"assets/{MOD}/blockstates/wall_lamp.json", {"variants": {
        f"facing={f},lit={l}": var("wall_lamp" + ("_lit" if l == "true" else ""), y) for f, y in YROT.items() for l in ("false", "true")}})


# ------------------------------------------------------------------ loot, tags

def all_blocks():
    out = []
    for f in FURN:
        out += [f"{w['id']}_{f}" for w in WOODS]
    out += [f"{c}_sofa" for c in DYES] + [f"{c}_double_bed" for c in DYES] + list(LAMPS)
    return out


def gen_loot_tags():
    for b in all_blocks():
        entry = {"type": "minecraft:item", "name": f"{MOD}:{b}"}
        if b.endswith("_double_bed"):  # só a cabeceira esquerda dá o item (como a cabeceira das camas vanilla)
            entry["condition"] = {"type": "minecraft:match_block", "blocks": f"{MOD}:{b}", "state": {"part": "head", "side": "left"}}
        write(f"data/{MOD}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [
            {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [entry]}], "random_sequence": f"{MOD}:blocks/{b}"})
    wooden = [f"{MOD}:{w['id']}_{f}" for f in FURN for w in WOODS]
    write("data/minecraft/tags/block/mineable/axe.json", {"replace": False, "values": wooden + [f"{MOD}:{c}_sofa" for c in DYES]})
    write("data/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": [f"{MOD}:{k}" for k in LAMPS]})
    # tags próprias (úteis a outros mods / ao Receitário)
    for f in FURN:
        write(f"data/{MOD}/tags/item/{f}s.json", {"values": [f"{MOD}:{w['id']}_{f}" for w in WOODS]})
    write(f"data/{MOD}/tags/item/sofas.json", {"values": [f"{MOD}:{c}_sofa" for c in DYES]})
    write(f"data/{MOD}/tags/item/double_beds.json", {"values": [f"{MOD}:{c}_double_bed" for c in DYES]})


# ------------------------------------------------------------------ receitas (bancada; baratas)

RECIPES = {}  # nome → receita (para o guia)


def shaped(name, pattern, key, result, count=1, group=None, unlock=None):
    r = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": key, "result": {"id": f"{MOD}:{result}"}}
    if count > 1:
        r["result"]["count"] = count
    if group:
        r["group"] = f"{MOD}:{group}"
    write(f"data/{MOD}/recipe/{name}.json", r)
    RECIPES[name] = r
    advancement(name, unlock or sorted(set(v for v in key.values())))


def shapeless(name, ingredients, result, count=1, group=None):
    r = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ingredients, "result": {"id": f"{MOD}:{result}"}}
    if count > 1:
        r["result"]["count"] = count
    if group:
        r["group"] = f"{MOD}:{group}"
    write(f"data/{MOD}/recipe/{name}.json", r)
    RECIPES[name] = r
    advancement(name, sorted(set(ingredients)))


def advancement(recipe, items):
    """Desbloqueia a receita no livro de receitas quando se apanha um dos ingredientes (como as conquistas de receitas vanilla)."""
    crit = {"has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipes": f"{MOD}:{recipe}"}}}
    for i, it in enumerate(items):
        crit[f"has_item_{i}"] = {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": it}]}}
    write(f"data/{MOD}/advancement/recipes/{recipe}.json", {"parent": "minecraft:recipes/root", "criteria": crit,
                                                            "requirements": [list(crit)], "rewards": {"recipes": [f"{MOD}:{recipe}"]}})


def gen_recipes():
    for w in WOODS:
        P, S, wid = f"minecraft:{w['planks']}", f"minecraft:{w['slab']}", w["id"]
        shaped(f"{wid}_chair", ["P  ", "PPP", "| |"], {"P": P, "|": "minecraft:stick"}, f"{wid}_chair", 2, "chair", [P])
        shaped(f"{wid}_stool", ["SSS", "| |"], {"S": S, "|": "minecraft:stick"}, f"{wid}_stool", 2, "stool", [P, S])
        shaped(f"{wid}_table", ["PPP", "| |", "| |"], {"P": P, "|": "minecraft:stick"}, f"{wid}_table", 1, "table", [P])
        shaped(f"{wid}_cabinet", ["PPP", "P|P", "PPP"], {"P": P, "|": "minecraft:stick"}, f"{wid}_cabinet", 1, "cabinet", [P])
        shaped(f"{wid}_drawer", ["PSP", "P|P", "PSP"], {"P": P, "S": S, "|": "minecraft:stick"}, f"{wid}_drawer", 1, "drawer", [P])
    for c in DYES:
        W = f"minecraft:{c}_wool"
        shaped(f"{c}_sofa", ["W W", "WWW", "SSS"], {"W": W, "S": "#minecraft:wooden_slabs"}, f"{c}_sofa", 2, "sofa", [W])
        shapeless(f"{c}_double_bed", [f"minecraft:{c}_bed", f"minecraft:{c}_bed"], f"{c}_double_bed", 1, "double_bed")
    C, G, R = "minecraft:copper_ingot", "minecraft:glass", "minecraft:redstone"
    shaped("chandelier", [" C ", "GRG", "G G"], {"C": C, "G": G, "R": R}, "chandelier", 1, None, [C, R])
    shaped("wall_lamp", ["CG", "CR"], {"C": C, "G": G, "R": R}, "wall_lamp", 2, None, [C, R])
    shaped("table_lamp", ["G", "R", "C"], {"C": C, "G": G, "R": R}, "table_lamp", 1, None, [C, R])


# ------------------------------------------------------------------ guia

def P(key, recipe, br, pt, en):
    return {"key": key, "recipe": recipe, "br": br, "pt": pt, "en": en}


GUIDE = [
    P("cover", None,
      ("Guia dos Móveis", "Cadeiras, bancos, mesas, armários, gaveteiros, sofás, camas de casal e lâmpadas.\n\nOs móveis de madeira existem em todas as madeiras do jogo: troque as tábuas e a laje da receita."),
      ("Guia dos Móveis", "Cadeiras, bancos, mesas, armários, cómodas, sofás, camas de casal e candeeiros.\n\nOs móveis de madeira existem em todas as madeiras do jogo: troca as tábuas e a laje da receita."),
      ("Furniture Guide", "Chairs, stools, tables, cabinets, chests of drawers, sofas, double beds and lamps.\n\nWooden furniture comes in every wood of the game: swap the planks and the slab in the recipe.")),
    P("sitting", None,
      ("Sentar", "Clique direito numa cadeira, banco ou sofá para se sentar. Shift para se levantar.\n\nSó cabe uma pessoa por bloco. Partir o móvel levanta quem lá estiver."),
      ("Sentar", "Clique direito numa cadeira, banco ou sofá para te sentares. Shift para te levantares.\n\nSó cabe uma pessoa por bloco. Partir o móvel levanta quem lá estiver."),
      ("Sitting", "Right-click a chair, stool or sofa to sit down. Sneak to stand up.\n\nOne person per block. Breaking the furniture stands the sitter up.")),
    P("chair", "oak_chair",
      ("Cadeira", "Fica virada para onde você está olhando: sentado, você olha para o mesmo lado. Dá 2."),
      ("Cadeira", "Fica virada para onde estás a olhar: sentado, olhas para o mesmo lado. Dá 2."),
      ("Chair", "Faces the way you look when you place it: seated, you look the same way. Makes 2.")),
    P("stool", "oak_stool",
      ("Banco", "Sem encosto: senta-se virado para qualquer lado. Dá 2."),
      ("Banco", "Sem encosto: senta-se virado para qualquer lado. Dá 2."),
      ("Stool", "No backrest: sit facing any way. Makes 2.")),
    P("table", "oak_table",
      ("Mesa", "Mesas lado a lado juntam-se num tampo só. As pernas ficam só nos cantos de fora."),
      ("Mesa", "Mesas lado a lado juntam-se num tampo só. As pernas ficam só nos cantos de fora."),
      ("Table", "Tables side by side join into one top. Legs stay only on the outer corners.")),
    P("cabinet", "oak_cabinet",
      ("Armário", "27 espaços, como um baú. As portas abrem enquanto o inventário está aberto."),
      ("Armário", "27 espaços, como um baú. As portas abrem enquanto o inventário está aberto."),
      ("Cabinet", "27 slots, like a chest. The doors open while the inventory is open.")),
    P("drawer", "oak_drawer",
      ("Gaveteiro", "27 espaços, como um baú. A gaveta de cima abre enquanto o inventário está aberto."),
      ("Cómoda", "27 espaços, como um baú. A gaveta de cima abre enquanto o inventário está aberto."),
      ("Chest of Drawers", "27 slots, like a chest. The top drawer opens while the inventory is open.")),
    P("storage_use", None,
      ("Guardar", "Armário e gaveteiro funcionam com funis (põem por cima e pelos lados, tiram por baixo) e comparadores.\n\nAo partir, o conteúdo cai no chão."),
      ("Guardar", "Armário e cómoda funcionam com funis (põem por cima e pelos lados, tiram por baixo) e comparadores.\n\nAo partir, o conteúdo cai no chão."),
      ("Storage", "Cabinets and chests of drawers work with hoppers (in from the top and sides, out from the bottom) and comparators.\n\nBreaking one drops its contents.")),
    P("sofa", "white_sofa",
      ("Sofá", "Lã de qualquer cor e lajes de qualquer madeira. Dá 2. A frente fica virada para você."),
      ("Sofá", "Lã de qualquer cor e lajes de qualquer madeira. Dá 2. A frente fica virada para ti."),
      ("Sofa", "Wool of any color and slabs of any wood. Makes 2. The front faces you.")),
    P("sofa_shapes", None,
      ("Sofás juntos", "Sofás lado a lado viram um sofá comprido: os braços ficam só nas pontas.\n\nEm L fazem canto, como as escadas. Cores diferentes também se juntam."),
      ("Sofás juntos", "Sofás lado a lado viram um sofá comprido: os braços ficam só nas pontas.\n\nEm L fazem canto, como as escadas. Cores diferentes também se juntam."),
      ("Joined sofas", "Sofas side by side become one long sofa: armrests only at the ends.\n\nIn an L they make a corner, like stairs. Different colors join too.")),
    P("double_bed", "white_double_bed",
      ("Cama de Casal", "2 camas da mesma cor. Ocupa 2×2 blocos: precisa de espaço à direita e à frente."),
      ("Cama de Casal", "2 camas da mesma cor. Ocupa 2×2 blocos: precisa de espaço à direita e à frente."),
      ("Double Bed", "2 beds of the same color. Takes 2×2 blocks: needs room to the right and ahead.")),
    P("double_bed_use", None,
      ("Dormir", "Funciona como uma cama normal: passa a noite e marca onde você renasce. Cabem duas pessoas, uma de cada lado.\n\nNo Nether e no Fim explode!"),
      ("Dormir", "Funciona como uma cama normal: passa a noite e marca onde renasces. Cabem duas pessoas, uma de cada lado.\n\nNo Nether e no Fim explode!"),
      ("Sleeping", "Works like a normal bed: skips the night and sets your respawn point. Two people fit, one on each side.\n\nIn the Nether and the End it explodes!")),
    P("lamps", None,
      ("Lâmpadas", "Lustre, arandela e luminária de mesa acendem com redstone, como a Lâmpada de Redstone.\n\nCom o mod Industrial também acendem com energia."),
      ("Candeeiros", "Lustre, arandela e candeeiro de mesa acendem com redstone, como a Lâmpada de Redstone.\n\nCom o mod Industrial também acendem com energia."),
      ("Lamps", "Chandelier, wall sconce and table lamp light up with redstone, like the Redstone Lamp.\n\nWith the Industrial mod they also run on energy.")),
    P("chandelier", "chandelier",
      ("Lustre", "Pendurado no teto: precisa de um bloco por cima. A luz mais forte."),
      ("Lustre", "Pendurado no teto: precisa de um bloco por cima. A luz mais forte."),
      ("Chandelier", "Hangs from the ceiling: needs a block above it. The brightest light.")),
    P("wall_lamp", "wall_lamp",
      ("Arandela", "Presa numa parede: clique na face do bloco. Dá 2."),
      ("Arandela", "Presa numa parede: clica na face do bloco. Dá 2."),
      ("Wall Sconce", "Fixed to a wall: click the side of a block. Makes 2.")),
    P("table_lamp", "table_lamp",
      ("Luminária de Mesa", "Em cima de uma mesa, armário ou qualquer bloco."),
      ("Candeeiro de Mesa", "Em cima de uma mesa, armário ou qualquer bloco."),
      ("Table Lamp", "On top of a table, cabinet or any block.")),
    P("lamp_energy", None,
      ("Com energia", "Com o Industrial: ligue um Fio de Cobre ou cabo à lâmpada, ou encoste-a a uma bateria ou painel.\n\nGasta 1 J/t: um fio de 32 J/t dá para 32 lâmpadas."),
      ("Com energia", "Com o Industrial: liga um Fio de Cobre ou cabo à lâmpada, ou encosta-a a uma bateria ou painel.\n\nGasta 1 J/t: um fio de 32 J/t dá para 32 candeeiros."),
      ("With energy", "With Industrial: connect a Copper Wire or cable to the lamp, or place it against a battery or panel.\n\nUses 1 J/t: a 32 J/t wire runs 32 lamps.")),
]


def _grid(r):
    if r["type"] == "minecraft:crafting_shaped":
        rows = [row.ljust(3) for row in r["pattern"]] + ["   "] * (3 - len(r["pattern"]))
        return [[None if ch == " " else r["key"][ch] for ch in row[:3]] for row in rows[:3]]
    ing = r["ingredients"]
    return [[ing[i * 3 + j] if i * 3 + j < len(ing) else None for j in range(3)] for i in range(3)]


def gen_guide():
    keys = [g["key"] for g in GUIDE]
    assert len(keys) == len(set(keys))
    woods_br = ", ".join(wood_names(w)["br"][1].removeprefix("de ").removeprefix("da ") for w in WOODS)
    for g in GUIDE:
        for part, i in (("title", 0), ("text", 1)):
            tr(f"guide.{MOD}.{g['key']}.{part}", g["br"][i], g["pt"][i], g["en"][i])
    book = [f"§l{g['br'][0]}§r\n\n{g['br'][1]}" for g in GUIDE]
    book.append(f"§lMadeiras§r\n\n{woods_br}.")
    assert len(book) <= 100, f"livro escrito com {len(book)} páginas (máximo 100)"
    write(f"data/{MOD}/recipe/guide.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                             "ingredients": ["minecraft:book", "#minecraft:planks"],
                                             "result": {"id": "minecraft:written_book", "components": {"minecraft:written_book_content": {
                                                 "title": {"raw": "Guia dos Móveis"}, "author": "Tcordeiro",
                                                 "pages": [{"raw": p} for p in book], "resolved": True}}}})
    advancement("guide", ["#minecraft:planks"])

    def js(v):
        return "null" if v is None else json.dumps(v, ensure_ascii=False)

    consts, lines = [], []
    for g in GUIDE:
        if g["recipe"] is None:
            lines.append(f'new Page("{g["key"]}", null, List.of())')
            continue
        r = RECIPES[g["recipe"]]
        const = g["recipe"].upper()
        rows = ", ".join("{" + ", ".join(js(c) for c in row) + "}" for row in _grid(r))
        consts.append(f'\tstatic final Recipe {const} = new Recipe("{MOD}:{g["recipe"]}", "{r["result"]["id"]}", {r["result"].get("count", 1)},\n'
                      f'\t\t\tnew String[][] {{{rows}}});')
        lines.append(f'new Page("{g["key"]}", null, List.of({const}))')
    java = f"""package com.tcordeiro.moveis.guide;

import java.util.List;

/**
 * Páginas do guia dos Móveis, lidas pelo separador «Guias» do Receitário por reflexão ({{@code PAGES}}; cada página com
 * {{@code key()}}, {{@code image()}} e {{@code recipes()}}; cada receita com {{@code result()}}, {{@code count()}} e {{@code cell(linha, coluna)}}).
 * Os textos estão nas traduções: guide.{MOD}.&lt;página&gt;.title / .text.
 * <p>
 * GERADO por tools/gen_data.py: não editar à mão.
 */
public final class MoveisGuide {{
	private MoveisGuide() {{}}

	/** Receita numa página: id, resultado, quantidade e 3 linhas de 3 casas (id de item, «#tag» ou null). */
	public record Recipe(String id, String result, int count, String[][] grid) {{
		public String cell(int row, int col) {{
			return row >= 0 && row < grid.length && col >= 0 && col < grid[row].length ? grid[row][col] : null;
		}}
	}}

	/** Página: chave de tradução, imagem opcional (textures/gui/guide/&lt;img&gt;.png) e receitas. */
	public record Page(String key, String image, List<Recipe> recipes) {{}}

{chr(10).join(consts)}

	public static final List<Page> PAGES = List.of(
			{(","+chr(10)+chr(9)+chr(9)+chr(9)).join(lines)});
}}
"""
    path = os.path.join(JAVA, "guide", "MoveisGuide.java")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(java)


# ------------------------------------------------------------------ WoodTypes.java

def gen_wood_types():
    rows = ",\n".join(f'\t\t\tnew Wood("{w["id"]}", "{w["planks"]}", "{w["slab"]}", "{w["log"]}", {str(w["flammable"]).lower()})' for w in WOODS)
    java = f"""package com.tcordeiro.moveis.wood;

import java.util.List;

/**
 * As madeiras dos móveis: TODAS as do Minecraft 26.3, pela ordem da tag minecraft:planks (lidas do jar do jogo por
 * tools/gen_data.py — GERADO, não editar à mão; para acrescentar madeiras ou «acabamentos» (verniz), mudar o gerador).
 * Cada uma: id, tábuas, laje, tronco descascado (textura das pernas) e se arde (as do Nether não).
 */
public final class WoodTypes {{
	private WoodTypes() {{}}

	public record Wood(String id, String planks, String slab, String log, boolean flammable) {{}}

	public static final List<Wood> ALL = List.of(
{rows});

	public static Wood byId(String id) {{
		for (Wood w : ALL) if (w.id().equals(id)) return w;
		return null;
	}}
}}
"""
    path = os.path.join(JAVA, "wood", "WoodTypes.java")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(java)


# ------------------------------------------------------------------ main

def gen_lang():
    for lg, code in (("br", "pt_br"), ("pt", "pt_pt"), ("en", "en_us")):
        write(f"assets/{MOD}/lang/{code}.json", dict(sorted(TR[lg].items())))


def check_names():
    """Nenhum nome pode repetir um nome do jogo (nem entre si)."""
    for lg in TR:
        vanilla = {v for k, v in LANG[lg].items() if k.startswith(("block.", "item."))}
        names = [v for k, v in TR[lg].items() if k.startswith("block.")]
        assert len(names) == len(set(names)), f"{lg}: nomes repetidos"
        clash = [n for n in names if n in vanilla]
        assert not clash, f"{lg}: nomes iguais aos do jogo: {clash}"


def clean_stale():
    """Apaga ficheiros gerados antes que já não são gerados (ex.: madeira renomeada). Só nas pastas geridas por este script."""
    managed = [f"assets/{MOD}/blockstates", f"assets/{MOD}/items", f"assets/{MOD}/models/block", f"data/{MOD}/loot_table",
               f"data/{MOD}/recipe", f"data/{MOD}/advancement", f"data/{MOD}/tags"]
    for d in managed:
        for dp, _, fs in os.walk(os.path.join(RES, d)):
            for fn in fs:
                rel = os.path.relpath(os.path.join(dp, fn), RES)
                if fn.endswith(".json") and rel not in written:
                    os.remove(os.path.join(dp, fn))
                    print("  apagado:", rel)


def main():
    gen_lang_names()
    gen_templates()
    gen_wooden()
    gen_sofas_beds()
    gen_lamps()
    gen_loot_tags()
    gen_recipes()
    gen_guide()
    gen_wood_types()
    check_names()
    gen_lang()
    clean_stale()
    print(f"{len(WOODS)} madeiras ({', '.join(w['id'] for w in WOODS)}), {len(DYES)} cores, {len(all_blocks())} blocos, "
          f"{len(RECIPES)} receitas, {len(GUIDE) + 1} páginas no livro, {len(written)} ficheiros")


if __name__ == "__main__":
    main()
