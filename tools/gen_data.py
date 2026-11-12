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
    fronts = lambda top_z: [el(1, 1, 0, 15, 5, 1, "planks"), el(1, 6, 0, 15, 10, 1, "planks"),
                            el(1, 5, 0, 15, 6, 1, "log"), el(1, 10, 0, 15, 11, 1, "log"),
                            el(7, 2.5, -1, 9, 3.5, 0, "log"), el(7, 7.5, -1, 9, 8.5, 0, "log")]
    template("drawer", [body] + frame + fronts(0) + [el(1, 11, 0, 15, 15, 1, "planks"), el(7, 12.5, -1, 9, 13.5, 0, "log")], "planks")
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
    rel = f"assets/{MOD}/models/block/template/{tmpl}.json"
    assert os.path.exists(os.path.join(RES, rel)), f"falta o template {tmpl} (correr tools/blender/export_templates.py)"
    written.add(rel)
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
        # armário e gaveteiro: frente para o jogador; aberto ou a mexer, o corpo (as portas/gaveta são do renderer)
        for k in ("cabinet", "drawer"):
            child(f"{wid}_{k}", k, tex)
            write(f"assets/{MOD}/blockstates/{wid}_{k}.json", {"variants": anim_variants(None, f"{wid}_{k}_body", f"{wid}_{k}")})
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
    return out + all_blocks_v020()


def gen_loot_tags():
    for b in all_blocks()[:-len(all_blocks_v020())]:
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
      ("Guia dos Móveis", "Cadeiras, mesas, armários, sofás, camas, lâmpadas e a casa toda: cozinha, quarto, banheiro e jardim.\n\nOs móveis de madeira existem em todas as madeiras do jogo: troque as tábuas e a laje da receita."),
      ("Guia dos Móveis", "Cadeiras, mesas, armários, sofás, camas, candeeiros e a casa toda: cozinha, quarto, casa de banho e jardim.\n\nOs móveis de madeira existem em todas as madeiras do jogo: troca as tábuas e a laje da receita."),
      ("Furniture Guide", "Chairs, tables, cabinets, sofas, beds, lamps and the whole house: kitchen, bedroom, bathroom and garden.\n\nWooden furniture comes in every wood of the game: swap the planks and the slab in the recipe.")),
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
    # ---------------- 0.2.0
    P("house", None,
      ("A casa toda", "Cozinha, quarto, banheiro, sala e jardim.\n\nPortas, gavetas, tampas, cortinas e persianas abrem devagar, como a tampa do baú, e fecham sozinhas quando você sai do inventário."),
      ("A casa toda", "Cozinha, quarto, casa de banho, sala e jardim.\n\nPortas, gavetas, tampas, cortinas e portadas abrem devagar, como a tampa do baú, e fecham sozinhas quando sais do inventário."),
      ("The whole house", "Kitchen, bedroom, bathroom, living room and garden.\n\nDoors, drawers, lids, curtains and shutters open smoothly, like the chest lid, and close by themselves when you leave the inventory.")),
    P("counter", "oak_counter",
      ("Bancada", "Tampo de pedra e armário de 9 espaços. Em L, o canto se ajeita sozinho. Dá 2."),
      ("Bancada", "Tampo de pedra e armário de 9 espaços. Em L, o canto ajeita-se sozinho. Dá 2."),
      ("Counter", "Stone top and a 9-slot cupboard. In an L, the corner sorts itself out. Makes 2.")),
    P("sink", "sink",
      ("Pia", "Encha com balde de água ou cano de fluidos (1 balde). Tire com balde ou frasco. Lava não entra."),
      ("Lava-Loiça", "Enche com balde de água ou cano de fluidos (1 balde). Tira com balde ou frasco. Lava não entra."),
      ("Kitchen Sink", "Fill it with a water bucket or a fluid pipe (1 bucket). Take water with a bucket or bottle. No lava.")),
    P("trash_can", "trash_can",
      ("Lixeira", "⚠ Os itens somem para sempre: ao fechar a lixeira, ou na hora se vierem de um funil."),
      ("Caixote do Lixo", "⚠ Os itens desaparecem para sempre: ao fechar, ou logo se vierem de um funil."),
      ("Trash Can", "⚠ Items are gone forever: when you close it, or right away if a hopper puts them in.")),
    P("nightstand", "oak_nightstand",
      ("Criado-Mudo", "9 espaços. A gaveta de cima desliza enquanto o inventário está aberto."),
      ("Mesa de Cabeceira", "9 espaços. A gaveta de cima desliza enquanto o inventário está aberto."),
      ("Nightstand", "9 slots. The top drawer slides out while the inventory is open.")),
    P("wardrobe", "oak_wardrobe",
      ("Guarda-Roupa", "2 armários da mesma madeira. 2 blocos de altura e 36 espaços."),
      ("Roupeiro", "2 armários da mesma madeira. 2 blocos de altura e 36 espaços."),
      ("Wardrobe", "2 cabinets of the same wood. 2 blocks tall, 36 slots.")),
    P("curtain", "white_curtain",
      ("Cortina", "Clique para abrir ou fechar. Cortinas vizinhas viradas para o mesmo lado abrem juntas. Dá 2."),
      ("Cortina", "Clica para abrir ou fechar. Cortinas vizinhas viradas para o mesmo lado abrem juntas. Dá 2."),
      ("Curtain", "Click to open or close. Neighbouring curtains facing the same way move together. Makes 2.")),
    P("shutters", "oak_shutters",
      ("Persiana", "Clique para abrir ou fechar as folhas. Dá 2."),
      ("Portada", "Clica para abrir ou fechar as folhas. Dá 2."),
      ("Shutters", "Click to swing them open or shut. Makes 2.")),
    P("mirror", "mirror",
      ("Espelho", "Decoração de parede: clique na face de um bloco."),
      ("Espelho", "Decoração de parede: clica na face de um bloco."),
      ("Mirror", "Wall decoration: click the side of a block.")),
    P("wall_clock", "wall_clock",
      ("Relógio", "Na parede. Os ponteiros mostram a hora do mundo, como o relógio de bolso."),
      ("Relógio", "Na parede. Os ponteiros mostram a hora do mundo, como o relógio de bolso."),
      ("Wall Clock", "On a wall. The hands show the time of the world, like the pocket clock.")),
    P("bunk_bed", "white_bunk_bed",
      ("Beliche", "Escada e 2 camas da mesma cor. São 2 camas de verdade: cada uma dorme uma pessoa."),
      ("Beliche", "Escada e 2 camas da mesma cor. São 2 camas a sério: cada uma dorme uma pessoa."),
      ("Bunk Bed", "A ladder and 2 beds of one color. Two real beds: each one sleeps one player.")),
    P("toilet", "toilet",
      ("Vaso Sanitário", "Clique para se sentar. Shift para levantar."),
      ("Sanita", "Clica para te sentares. Shift para te levantares."),
      ("Toilet", "Click to sit down. Sneak to stand up.")),
    P("bathtub", "bathtub",
      ("Banheira", "2 blocos. Encha com baldes ou cano (até 3 baldes). Entrar na água apaga o fogo."),
      ("Banheira", "2 blocos. Enche com baldes ou cano (até 3 baldes). Entrar na água apaga o fogo."),
      ("Bathtub", "2 blocks. Fill it with buckets or a pipe (up to 3 buckets). Getting in the water puts out fire.")),
    P("fireplace", "fireplace",
      ("Lareira", "Ponha lenha (paus ou troncos) e acenda com a Pederneira ou o Isqueiro do Clima. A pá apaga."),
      ("Lareira", "Põe lenha (paus ou troncos) e acende com o Isqueiro (do jogo ou do Clima). A pá apaga."),
      ("Fireplace", "Add firewood (sticks or logs) and light it with Flint and Steel or the Clima Lighter. A shovel puts it out.")),
    P("fireplace_use", None,
      ("Lenha e calor", "Um tronco dura 1 minuto; um pau, 20 segundos. Sem lenha, apaga.\n\nCom o mod Clima, aquece a sala (6 blocos à volta).\n\n⚠ Queima quem encosta no fogo."),
      ("Lenha e calor", "Um tronco dura 1 minuto; um pau, 20 segundos. Sem lenha, apaga.\n\nCom o mod Clima, aquece a sala (6 blocos à volta).\n\n⚠ Queima quem encosta ao fogo."),
      ("Firewood and heat", "A log lasts 1 minute; a stick, 20 seconds. Without firewood it goes out.\n\nWith the Clima mod it warms the room (6 blocks around).\n\n⚠ Burns whoever touches the fire.")),
    P("ceiling_fan", "ceiling_fan",
      ("Ventilador", "No teto. Gira com redstone (mais sinal, mais rápido) ou com energia do Industrial."),
      ("Ventoinha", "No teto. Gira com redstone (mais sinal, mais rápido) ou com energia do Industrial."),
      ("Ceiling Fan", "On the ceiling. Spins with redstone (stronger signal, faster) or Industrial energy.")),
    P("fan_climate", None,
      ("Ventilador e Clima", "Com o mod Clima, o ventilador a girar refresca a sala no verão (5 blocos à volta).\n\nA luz do ventilador acende junto."),
      ("Ventoinha e Clima", "Com o mod Clima, a ventoinha a girar refresca a sala no verão (5 blocos à volta).\n\nA luz da ventoinha acende junto."),
      ("Fan and climate", "With the Clima mod, a spinning fan cools the room in summer (5 blocks around).\n\nThe fan light turns on with it.")),
    P("mailbox", "mailbox",
      ("Caixa de Correio", "É de quem a põe. Qualquer um deixa itens; só o dono tira. A bandeirinha levanta com correio."),
      ("Caixa do Correio", "É de quem a põe. Qualquer um deixa itens; só o dono tira. A bandeirinha levanta com correio."),
      ("Mailbox", "It belongs to whoever places it. Anyone can drop items; only the owner takes them. The flag goes up with mail.")),
    P("mail_use", None,
      ("Mandar correio", "Para mandar itens a outro jogador, ponha-os na caixa de correio dele.\n\nSe ele estiver no mundo, recebe um aviso na hora; senão, quando entrar."),
      ("Mandar correio", "Para mandar itens a outro jogador, põe-nos na caixa do correio dele.\n\nSe ele estiver no mundo, recebe um aviso logo; senão, quando entrar."),
      ("Sending mail", "To send items to another player, put them in their mailbox.\n\nIf they are online they get a message right away; otherwise when they join.")),
    P("desk", "oak_desk",
      ("Escrivaninha", "3 gavetas, 9 espaços. Lado a lado, juntam-se num tampo só."),
      ("Secretária", "3 gavetas, 9 espaços. Lado a lado, juntam-se num tampo só."),
      ("Desk", "3 drawers, 9 slots. Side by side, desks join into one long top.")),
    P("coffee_table", "oak_coffee_table",
      ("Mesa de Centro", "Clique com um item para o pôr em cima; com a mão vazia, tire."),
      ("Mesa de Centro", "Clica com um item para o pores em cima; com a mão vazia, tira-o."),
      ("Coffee Table", "Click with an item to put it on top; an empty hand takes it back.")),
    P("side_table", "oak_side_table",
      ("Mesa Lateral", "Para o lado do sofá. Também mostra o item que puser em cima."),
      ("Mesa de Apoio", "Para o lado do sofá. Também mostra o item que puseres em cima."),
      ("Side Table", "For next to the sofa. It also shows the item you put on top.")),
    P("garden_bench", "oak_garden_bench",
      ("Banco de Jardim", "Para sentar ao ar livre: clique, e Shift para levantar."),
      ("Banco de Jardim", "Para sentar ao ar livre: clica, e Shift para te levantares."),
      ("Garden Bench", "To sit outdoors: click, and sneak to stand up.")),
    P("planter_box", "oak_planter_box",
      ("Floreira", "Flores, mudas e sementes crescem nela como na terra arada molhada."),
      ("Floreira", "Flores, rebentos e sementes crescem nela como na terra lavrada molhada."),
      ("Planter Box", "Flowers, saplings and seeds grow in it like in wet farmland.")),
    P("wine_rack", "oak_wine_rack",
      ("Adega", "9 casas só para frascos e poções. Clique numa casa com o frasco; a mão vazia tira."),
      ("Garrafeira", "9 casas só para frascos e poções. Clica numa casa com o frasco; a mão vazia tira-o."),
      ("Wine Rack", "9 cells, bottles and potions only. Click a cell with a bottle; an empty hand takes it.")),
    P("floor_lamp", "floor_lamp",
      ("Luminária de Chão", "2 blocos de altura. Acende com redstone ou energia do Industrial."),
      ("Candeeiro de Pé", "2 blocos de altura. Acende com redstone ou energia do Industrial."),
      ("Floor Lamp", "2 blocks tall. Lights up with redstone or Industrial energy.")),
    P("piano", "piano",
      ("Piano", "2 blocos. Cada tecla toca uma nota: as graves à esquerda, as agudas à direita."),
      ("Piano", "2 blocos. Cada tecla toca uma nota: as graves à esquerda, as agudas à direita."),
      ("Piano", "2 blocks wide. Each key plays a note: low ones on the left, high ones on the right.")),
    P("safe", "safe",
      ("Cofre-Forte", "27 espaços. Só o dono abre e parte. Explosões, funis e outros jogadores não o abrem."),
      ("Cofre-Forte", "27 espaços. Só o dono abre e parte. Explosões, funis e outros jogadores não o abrem."),
      ("Safe", "27 slots. Only the owner opens and breaks it. Blasts, hoppers and other players can't.")),
    P("awning", "white_awning",
      ("Toldo", "Decoração de parede, por cima de janelas e portas. Dá 2."),
      ("Toldo", "Decoração de parede, por cima de janelas e portas. Dá 2."),
      ("Awning", "Wall decoration, over windows and doors. Makes 2.")),
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


# ------------------------------------------------------------------ 0.2.0: cozinha, quarto, banheiro, sala e rua

import sys as _sys
_sys.path.insert(0, os.path.join(ROOT, "tools", "blender"))
import pecas as PECAS  # noqa: E402  (as partes móveis e os seus pivôs vêm do mesmo sítio que os cubos)

# móveis de madeira novos (13 madeiras): (pt_br, pt_pt, en, género em pt: m/f/mf = m no Brasil e f em Portugal)
FURN2 = {
    "counter": ("Bancada", "Bancada", "Counter", "f"),
    "nightstand": ("Criado-Mudo", "Mesa de Cabeceira", "Nightstand", "mf"),
    "wardrobe": ("Guarda-Roupa", "Roupeiro", "Wardrobe", "m"),
    "desk": ("Escrivaninha", "Secretária", "Desk", "f"),
    "coffee_table": ("Mesa de Centro", "Mesa de Centro", "Coffee Table", "f"),
    "side_table": ("Mesa Lateral", "Mesa de Apoio", "Side Table", "f"),
    "garden_bench": ("Banco de Jardim", "Banco de Jardim", "Garden Bench", "m"),
    "planter_box": ("Floreira", "Floreira", "Planter Box", "f"),
    "shutters": ("Persiana", "Portada", "Shutters", "f"),
    "wine_rack": ("Adega", "Garrafeira", "Wine Rack", "f"),
}
# 16 cores: (pt_br, pt_pt, en, género)
COLORED = {
    "curtain": ("Cortina", "Cortina", "Curtain", "f"),
    "awning": ("Toldo", "Toldo", "Awning", "m"),
    "bunk_bed": ("Beliche", "Beliche", "Bunk Bed", "m"),
}
# uma variante só (metal, porcelana, tijolo, instrumento, peças pequenas de decoração)
SINGLE = {
    "sink": ("Pia", "Lava-Loiça", "Kitchen Sink"),
    "trash_can": ("Lixeira", "Caixote do Lixo", "Trash Can"),
    "mirror": ("Espelho", "Espelho", "Mirror"),
    "wall_clock": ("Relógio de Parede", "Relógio de Parede", "Wall Clock"),
    "toilet": ("Vaso Sanitário", "Sanita", "Toilet"),
    "bathtub": ("Banheira", "Banheira", "Bathtub"),
    "fireplace": ("Lareira", "Lareira", "Fireplace"),
    "ceiling_fan": ("Ventilador de Teto", "Ventoinha de Teto", "Ceiling Fan"),
    "mailbox": ("Caixa de Correio", "Caixa do Correio", "Mailbox"),
    "floor_lamp": ("Luminária de Chão", "Candeeiro de Pé", "Floor Lamp"),
    "piano": ("Piano", "Piano", "Piano"),
    "safe": ("Cofre-Forte", "Cofre-Forte", "Safe"),
}
FACES4 = list(YROT)


def gen_lang_v020():
    for w in WOODS:
        n = wood_names(w)
        for f, (br, pt, en, g) in FURN2.items():
            gbr = 1 if g == "f" else 0
            gpt = 1 if g in ("f", "mf") else 0
            tr(f"block.{MOD}.{w['id']}_{f}", f"{br} {n['br'][gbr]}", f"{pt} {n['pt'][gpt]}", f"{n['en']} {en}")
    for c in DYES:
        n = color_names(c)
        for f, (br, pt, en, g) in COLORED.items():
            i = 1 if g == "f" else 0
            tr(f"block.{MOD}.{c}_{f}", f"{br} {n['br'][i]}", f"{pt} {n['pt'][i]}", f"{n['en']} {en}")
    for k, (br, pt, en) in SINGLE.items():
        tr(f"block.{MOD}.{k}", br, pt, en)
    T = lambda k, br, pt, en: tr(f"message.{MOD}.{k}", br, pt, en)
    T("not_owner", "Isto é de %s.", "Isto é de %s.", "This belongs to %s.")
    T("mail_arrived", "Chegou correio na sua caixa (%s).", "Chegou correio à tua caixa (%s).", "Mail arrived in your mailbox (%s).")
    T("mail_waiting", "Você tem correio à espera: %s caixa(s), a primeira em %s.", "Tens correio à espera: %s caixa(s), a primeira em %s.",
      "You have mail waiting: %s mailbox(es), the first at %s.")
    T("fireplace_fuel", "Lenha: %s", "Lenha: %s", "Firewood: %s")
    T("fireplace_no_fuel", "Ponha lenha primeiro (paus ou troncos).", "Põe lenha primeiro (paus ou troncos).", "Add firewood first (sticks or logs).")
    T("water", "Água: %s de %s baldes", "Água: %s de %s baldes", "Water: %s of %s buckets")
    tr(f"tooltip.{MOD}.trash_can", "⚠ Os itens somem para sempre ao fechar.", "⚠ Os itens desaparecem para sempre ao fechar.",
       "⚠ Items are gone forever when you close it.")
    tr(f"tooltip.{MOD}.fireplace", "⚠ Queima quem encosta no fogo.", "⚠ Queima quem encosta ao fogo.", "⚠ Burns whoever touches the fire.")
    tr(f"tooltip.{MOD}.safe", "Só o dono abre e parte. Resiste a explosões.", "Só o dono abre e parte. Resiste a explosões.",
       "Only the owner opens and breaks it. Blast-proof.")
    tr(f"tooltip.{MOD}.mailbox", "Qualquer um põe; só o dono tira.", "Qualquer um põe; só o dono tira.", "Anyone can put in; only the owner takes out.")


# ---------------- texturas de cada bloco (as variáveis dos templates)

OAK, DARK = "minecraft:block/oak_planks", "minecraft:block/dark_oak_planks"


def wood_tex(w):
    return {"planks": f"minecraft:block/{w['planks']}", "log": f"minecraft:block/{w['log']}", "particle": f"minecraft:block/{w['planks']}"}


def color_tex(kind, c):
    t = {"wool": f"minecraft:block/{c}_wool", "particle": f"minecraft:block/{c}_wool"}
    if kind == "bunk_bed":
        t["planks"] = OAK
    return t


SINGLE_TEX = {
    "sink": {"planks": OAK, "log": "minecraft:block/stripped_oak_log", "particle": "minecraft:block/smooth_stone"},
    "trash_can": {"particle": "minecraft:block/iron_block"},
    "mirror": {"planks": DARK, "particle": DARK},
    "wall_clock": {"planks": DARK, "pillow": "minecraft:block/white_concrete", "metal": "minecraft:block/gold_block", "particle": DARK},
    "toilet": {"planks": "minecraft:block/birch_planks", "particle": "minecraft:block/quartz_block_side"},
    "bathtub": {"particle": "minecraft:block/quartz_block_side"},
    "fireplace": {"planks": "minecraft:block/spruce_planks", "log": "minecraft:block/oak_log", "particle": "minecraft:block/bricks"},
    "ceiling_fan": {"planks": DARK, "metal": "minecraft:block/copper_block", "particle": "minecraft:block/copper_block"},
    "mailbox": {"planks": OAK, "particle": "minecraft:block/iron_block"},
    "floor_lamp": {"metal": "minecraft:block/copper_block", "particle": "minecraft:block/copper_block"},
    "piano": {"planks": DARK, "pillow": "minecraft:block/white_concrete", "particle": DARK},
    "safe": {"pillow": "minecraft:block/white_concrete", "metal": "minecraft:block/iron_block", "particle": "minecraft:block/iron_block"},
}
LIGHT = {False: "minecraft:block/redstone_lamp", True: "minecraft:block/redstone_lamp_on"}

# as partes móveis (pecas.ANIM) que cada tipo de bloco desenha com o renderer
PART_KINDS = {"cabinet": ["cabinet"], "drawer": ["drawer"], "counter": ["counter", "counter_corner"], "nightstand": ["nightstand"],
              "wardrobe": ["wardrobe_bottom", "wardrobe_top"], "desk": ["desk"], "shutters": ["shutters"], "curtain": ["curtain"],
              "safe": ["safe"], "trash_can": ["trash_can"], "mailbox": ["mailbox"], "ceiling_fan": ["ceiling_fan"], "wall_clock": ["wall_clock"]}
EXTRA_PARTS = {"piano": ["piano_white_key", "piano_black_key"], "wine_rack": ["wine_rack_bottle"]}


def parts_for(block, kind, tex):
    """Modelos das partes móveis deste bloco: models/block/part/<bloco>/<template>.json e o item part/<bloco>/<template> (o renderer
    desenha-as como itens com o componente item_model, no espaço do bloco)."""
    templates = [f"{k}_{p[0]}" for k2 in PART_KINDS.get(kind, []) for k in [k2] for p in PECAS.ANIM[k]] + EXTRA_PARTS.get(kind, [])
    for t in templates:
        child(f"part/{block}/{t}", t, tex)
        item_model(f"part/{block}/{t}", f"part/{block}/{t}")


def bs(name, variants):
    write(f"assets/{MOD}/blockstates/{name}.json", {"variants": variants})


def mp(name, parts):
    write(f"assets/{MOD}/blockstates/{name}.json", {"multipart": parts})


def comp(name, parts):
    composite_item(name, parts)


def anim_variants(prefix, body, full, extra_open=None):
    """facing × open × moving: parado e fechado = modelo inteiro; aberto ou a mexer = só o corpo (o renderer desenha as partes)."""
    v = {}
    for f, y in YROT.items():
        for o in ("false", "true"):
            for m in ("false", "true"):
                model = full if (o == "false" and m == "false") else (extra_open if (extra_open and o == "true" and m == "false") else body)
                v[f"facing={f},moving={m},open={o}"] = var(model, y)
    return v


def gen_wood_v020():
    for w in WOODS:
        wid, tex = w["id"], wood_tex(w)
        p = lambda t: f"{wid}_{t}"
        # bancada: reta ou canto de dentro
        for t in ("counter", "counter_body", "counter_corner", "counter_corner_body"):
            child(p(t), t, tex)
        v = {}
        for f, y in YROT.items():
            for c in ("false", "true"):
                k = "counter_corner" if c == "true" else "counter"
                for o in ("false", "true"):
                    for m in ("false", "true"):
                        v[f"corner={c},facing={f},moving={m},open={o}"] = var(p(k if o == m == "false" else k + "_body"), y)
        bs(p("counter"), v)
        item_model(p("counter"), p("counter"))
        parts_for(p("counter"), "counter", tex)
        # mesa de cabeceira
        for t in ("nightstand", "nightstand_body"):
            child(p(t), t, tex)
        bs(p("nightstand"), anim_variants(p("nightstand"), p("nightstand_body"), p("nightstand")))
        item_model(p("nightstand"), p("nightstand"))
        parts_for(p("nightstand"), "nightstand", tex)
        # guarda-roupa (2 de altura)
        for t in ("wardrobe_bottom", "wardrobe_bottom_body", "wardrobe_top", "wardrobe_top_body"):
            child(p(t), t, tex)
        v = {}
        for half, t in (("lower", "wardrobe_bottom"), ("upper", "wardrobe_top")):
            for key, model in anim_variants(None, p(t + "_body"), p(t)).items():
                v[f"half={half}," + key] = model
        bs(p("wardrobe"), v)
        comp(p("wardrobe"), [(p("wardrobe_bottom"), 0, [0, -0.5, 0]), (p("wardrobe_top"), 0, [0, 0.5, 0])])
        parts_for(p("wardrobe"), "wardrobe", tex)
        # escrivaninha: perna da esquerda só sem vizinha desse lado
        for t in ("desk", "desk_body", "desk_leg"):
            child(p(t), t, tex)
        parts = []
        for f, y in YROT.items():
            parts.append({"when": {"facing": f, "open": "false", "moving": "false"}, "apply": var(p("desk"), y)})
            parts.append({"when": {"AND": [{"facing": f}, {"OR": [{"open": "true"}, {"moving": "true"}]}]}, "apply": var(p("desk_body"), y)})
            parts.append({"when": {"facing": f, "joined": "false"}, "apply": var(p("desk_leg"), y)})
        mp(p("desk"), parts)
        comp(p("desk"), [(p("desk"), 0, [0, 0, 0]), (p("desk_leg"), 0, [0, 0, 0])])
        parts_for(p("desk"), "desk", tex)
        # mesas de centro e lateral, banco de jardim, adega: viradas para o jogador
        for t in ("coffee_table", "garden_bench", "wine_rack_body", "wine_rack"):
            child(p(t), t, tex)
        for t, model in (("coffee_table", "coffee_table"), ("garden_bench", "garden_bench"), ("wine_rack", "wine_rack_body")):
            bs(p(t), {f"facing={f}": var(p(model), y) for f, y in YROT.items()})
        for t in ("coffee_table", "garden_bench", "wine_rack"):
            item_model(p(t), p(t))
        parts_for(p("wine_rack"), "wine_rack", tex)
        child(p("side_table"), "side_table", tex)
        bs(p("side_table"), {"": var(p("side_table"))})
        item_model(p("side_table"), p("side_table"))
        # floreira (sempre regada: moisture=7, como a terra arada molhada)
        child(p("planter_box"), "planter_box", tex)
        bs(p("planter_box"), {f"moisture={i}": var(p("planter_box")) for i in range(8)})
        item_model(p("planter_box"), p("planter_box"))
        # persianas
        for t in ("shutters_closed", "shutters_open", "shutters_body"):
            child(p(t), t, tex)
        bs(p("shutters"), anim_variants(None, p("shutters_body"), p("shutters_closed"), p("shutters_open")))
        item_model(p("shutters"), p("shutters_closed"))
        parts_for(p("shutters"), "shutters", tex)


def gen_colored_v020():
    for c in DYES:
        # cortina
        tex = color_tex("curtain", c)
        for t in ("curtain_closed", "curtain_open", "curtain_body"):
            child(f"{c}_{t}", t, tex)
        bs(f"{c}_curtain", anim_variants(None, f"{c}_curtain_body", f"{c}_curtain_closed", f"{c}_curtain_open"))
        item_model(f"{c}_curtain", f"{c}_curtain_closed")
        parts_for(f"{c}_curtain", "curtain", tex)
        # toldo
        child(f"{c}_awning", "awning", color_tex("awning", c))
        bs(f"{c}_awning", {f"facing={f}": var(f"{c}_awning", y) for f, y in YROT.items()})
        item_model(f"{c}_awning", f"{c}_awning")
        # beliche: 2 de altura × 2 de comprimento (cabeceira a norte no desenho = FACING, como nas camas: pés → cabeceira)
        tex = color_tex("bunk_bed", c)
        v = {}
        for half, tier in (("lower", "bottom"), ("upper", "top")):
            for part in ("head", "foot"):
                child(f"{c}_bunk_bed_{tier}_{part}", f"bunk_bed_{tier}_{part}", tex)
                for f, y in YROT.items():
                    for occ in ("false", "true"):
                        v[f"facing={f},half={half},occupied={occ},part={part}"] = var(f"{c}_bunk_bed_{tier}_{part}", y)
        bs(f"{c}_bunk_bed", v)
        comp(f"{c}_bunk_bed", [(f"{c}_bunk_bed_bottom_head", 0, [0, -0.5, -0.5]), (f"{c}_bunk_bed_bottom_foot", 0, [0, -0.5, 0.5]),
                               (f"{c}_bunk_bed_top_head", 0, [0, 0.5, -0.5]), (f"{c}_bunk_bed_top_foot", 0, [0, 0.5, 0.5])])


def gen_single_v020():
    T = SINGLE_TEX
    # pia: camada de água quando tem água
    child("sink", "sink", T["sink"])
    child("sink_water", "sink_water", T["sink"])
    parts = []
    for f, y in YROT.items():
        parts.append({"when": {"facing": f}, "apply": var("sink", y)})
        parts.append({"when": {"facing": f, "water": "true"}, "apply": var("sink_water", y)})
    mp("sink", parts)
    item_model("sink", "sink")
    # lixeira
    for t in ("trash_can", "trash_can_body"):
        child(t, t, T["trash_can"])
    bs("trash_can", anim_variants(None, "trash_can_body", "trash_can"))
    item_model("trash_can", "trash_can")
    parts_for("trash_can", "trash_can", T["trash_can"])
    # espelho, vaso sanitário, toldo: virados para fora da parede / para o jogador
    for t in ("mirror", "toilet"):
        child(t, t, T[t])
        bs(t, {f"facing={f}": var(t, y) for f, y in YROT.items()})
        item_model(t, t)
    # relógio: o bloco só tem o mostrador; os ponteiros são do renderer (o item mostra-os)
    for t in ("wall_clock", "wall_clock_body"):
        child(t, t, T["wall_clock"])
    bs("wall_clock", {f"facing={f}": var("wall_clock_body", y) for f, y in YROT.items()})
    item_model("wall_clock", "wall_clock")
    parts_for("wall_clock", "wall_clock", T["wall_clock"])
    # banheira: 2 blocos (pés → cabeceira = FACING; as torneiras na cabeceira)
    parts = []
    for part in ("head", "foot"):
        child(f"bathtub_{part}", f"bathtub_{part}", T["bathtub"])
        child(f"bathtub_water_{part}", f"bathtub_water_{part}", T["bathtub"])
        for f, y in YROT.items():
            parts.append({"when": {"facing": f, "part": part}, "apply": var(f"bathtub_{part}", y)})
            parts.append({"when": {"facing": f, "part": part, "water": "true"}, "apply": var(f"bathtub_water_{part}", y)})
    mp("bathtub", parts)
    comp("bathtub", [("bathtub_head", 0, [0, 0, -0.5]), ("bathtub_foot", 0, [0, 0, 0.5])])
    # lareira
    child("fireplace", "fireplace", T["fireplace"])
    child("fireplace_unlit", "fireplace_unlit", T["fireplace"])
    bs("fireplace", {f"facing={f},lit={l}": var("fireplace" if l == "true" else "fireplace_unlit", y) for f, y in YROT.items()
                     for l in ("false", "true")})
    item_model("fireplace", "fireplace_unlit")
    # ventilador de teto: pás no modelo só parado; a luz acende com ele
    for lit in (False, True):
        sfx = "_lit" if lit else ""
        child("ceiling_fan" + sfx, "ceiling_fan", {**T["ceiling_fan"], "light": LIGHT[lit]})
        child("ceiling_fan_body" + sfx, "ceiling_fan_body", {**T["ceiling_fan"], "light": LIGHT[lit]})
    mp("ceiling_fan", [
        {"when": {"lit": "false", "moving": "false"}, "apply": var("ceiling_fan")},
        {"when": {"lit": "false", "moving": "true"}, "apply": var("ceiling_fan_body")},
        {"when": {"lit": "true"}, "apply": var("ceiling_fan_body_lit")}])
    item_model("ceiling_fan", "ceiling_fan")
    parts_for("ceiling_fan", "ceiling_fan", {**T["ceiling_fan"], "light": LIGHT[False]})
    # caixa de correio: bandeirinha em baixo (vazia) ou em cima (com correio); a mexer, o renderer
    for t in ("mailbox", "mailbox_full", "mailbox_body"):
        child(t, t, T["mailbox"])
    bs("mailbox", {f"facing={f},has_mail={h},moving={m}": var("mailbox_body" if m == "true" else ("mailbox_full" if h == "true" else "mailbox"), y)
                   for f, y in YROT.items() for h in ("false", "true") for m in ("false", "true")})
    item_model("mailbox", "mailbox_full")
    parts_for("mailbox", "mailbox", T["mailbox"])
    # luminária de chão (2 de altura; a luz é a de cima)
    for lit in (False, True):
        sfx = "_lit" if lit else ""
        for half in ("bottom", "top"):
            child(f"floor_lamp_{half}{sfx}", f"floor_lamp_{half}", {**T["floor_lamp"], "light": LIGHT[lit]})
    bs("floor_lamp", {f"half={h},lit={l}": var(f"floor_lamp_{'bottom' if h == 'lower' else 'top'}" + ("_lit" if l == "true" else ""))
                      for h in ("lower", "upper") for l in ("false", "true")})
    comp("floor_lamp", [("floor_lamp_bottom", 0, [0, -0.5, 0]), ("floor_lamp_top", 0, [0, 0.5, 0])])
    # piano: 2 de largura; a tocar, as teclas são do renderer
    for side in ("left", "right"):
        child(f"piano_{side}", f"piano_{side}", T["piano"])
        child(f"piano_{side}_body", f"piano_{side}_body", T["piano"])
    bs("piano", {f"facing={f},playing={pl},side={s}": var(f"piano_{s}" + ("_body" if pl == "true" else ""), y)
                 for f, y in YROT.items() for pl in ("false", "true") for s in ("left", "right")})
    comp("piano", [("piano_left", 0, [-0.5, 0, 0]), ("piano_right", 0, [0.5, 0, 0])])
    parts_for("piano", "piano", T["piano"])
    # cofre
    for t in ("safe", "safe_body"):
        child(t, t, T["safe"])
    bs("safe", anim_variants(None, "safe_body", "safe"))
    item_model("safe", "safe")
    parts_for("safe", "safe", T["safe"])


def cabinet_parts_v020():
    """As portas do armário e a gaveta do gaveteiro (0.1.0) também passam a animar."""
    for w in WOODS:
        wid, tex = w["id"], wood_tex(w)
        for k in ("cabinet", "drawer"):
            child(f"{wid}_{k}_body", f"{k}_body", tex)
            parts_for(f"{wid}_{k}", k, tex)


def all_blocks_v020():
    out = []
    for f in FURN2:
        out += [f"{w['id']}_{f}" for w in WOODS]
    for f in COLORED:
        out += [f"{c}_{f}" for c in DYES]
    return out + list(SINGLE)


# a parte que dá o item nos móveis de vários blocos (a que se põe com o item)
MAIN_PART = {"wardrobe": {"half": "lower"}, "floor_lamp": {"half": "lower"}, "bathtub": {"part": "foot"},
             "bunk_bed": {"half": "lower", "part": "foot"}, "piano": {"side": "right"}}
AXE2 = ["counter", "nightstand", "wardrobe", "desk", "coffee_table", "side_table", "garden_bench", "planter_box", "shutters", "wine_rack"]
PICKAXE2 = ["sink", "trash_can", "toilet", "bathtub", "fireplace", "ceiling_fan", "mailbox", "floor_lamp", "safe"]
AXE2_SINGLE = ["wall_clock", "piano", "mirror"]


def loot_v020():
    for b in all_blocks_v020():
        kind = next((k for k in MAIN_PART if b == k or b.endswith("_" + k)), None)
        entry = {"type": "minecraft:item", "name": f"{MOD}:{b}"}
        if kind:
            entry["condition"] = {"type": "minecraft:match_block", "blocks": f"{MOD}:{b}", "state": MAIN_PART[kind]}
        write(f"data/{MOD}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [
            {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [entry]}], "random_sequence": f"{MOD}:blocks/{b}"})
    plural = {"garden_bench": "garden_benches", "planter_box": "planter_boxes", "shutters": "shutters"}
    for f in FURN2:
        write(f"data/{MOD}/tags/item/{plural.get(f, f + 's')}.json", {"values": [f"{MOD}:{w['id']}_{f}" for w in WOODS]})
    for f in COLORED:
        write(f"data/{MOD}/tags/item/{f}s.json", {"values": [f"{MOD}:{c}_{f}" for c in DYES]})
    planters = [f"{MOD}:{w['id']}_planter_box" for w in WOODS]
    for tag in ("supports_crops", "supports_vegetation", "grows_crops", "supports_stem_crops", "cannot_replace_below_tree_trunk"):
        write(f"data/minecraft/tags/block/{tag}.json", {"replace": False, "values": planters})


def gen_loot_tags_merged():
    """mineable/axe e pickaxe com o que já havia (0.1.0) e o novo."""
    wooden = [f"{MOD}:{w['id']}_{f}" for f in list(FURN) + AXE2 for w in WOODS]
    write("data/minecraft/tags/block/mineable/axe.json", {"replace": False, "values": wooden + [f"{MOD}:{c}_sofa" for c in DYES]
                                                           + [f"{MOD}:{k}" for k in AXE2_SINGLE]})
    write("data/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": [f"{MOD}:{k}" for k in list(LAMPS) + PICKAXE2]})


def recipes_v020():
    stick, nug, iron, copper = "minecraft:stick", "minecraft:iron_nugget", "minecraft:iron_ingot", "minecraft:copper_ingot"
    planks, clay = "#minecraft:planks", "minecraft:clay_ball"
    for w in WOODS:
        P, S, wid = f"minecraft:{w['planks']}", f"minecraft:{w['slab']}", w["id"]
        shaped(f"{wid}_counter", ["CCC", "P|P", "PPP"], {"C": "minecraft:smooth_stone_slab", "P": P, "|": stick}, f"{wid}_counter", 2, "counter", [P])
        shaped(f"{wid}_nightstand", ["PSP", "P P"], {"P": P, "S": S}, f"{wid}_nightstand", 1, "nightstand", [P])
        shapeless(f"{wid}_wardrobe", [f"{MOD}:{wid}_cabinet", f"{MOD}:{wid}_cabinet"], f"{wid}_wardrobe", 1, "wardrobe")
        shaped(f"{wid}_desk", ["SSS", "| P", "| P"], {"S": S, "P": P, "|": stick}, f"{wid}_desk", 1, "desk", [P])
        shaped(f"{wid}_coffee_table", ["PPP", "| |"], {"P": P, "|": stick}, f"{wid}_coffee_table", 1, "coffee_table", [P])
        shaped(f"{wid}_side_table", ["S", "|", "S"], {"S": S, "|": stick}, f"{wid}_side_table", 1, "side_table", [P, S])
        shaped(f"{wid}_garden_bench", ["SSS", "N N"], {"S": S, "N": nug}, f"{wid}_garden_bench", 1, "garden_bench", [P, S])
        shaped(f"{wid}_planter_box", ["P P", "PDP"], {"P": P, "D": "minecraft:dirt"}, f"{wid}_planter_box", 1, "planter_box", [P])
        shaped(f"{wid}_shutters", ["S S", "S S"], {"S": S}, f"{wid}_shutters", 2, "shutters", [P, S])
        shaped(f"{wid}_wine_rack", ["P P", " P ", "P P"], {"P": P}, f"{wid}_wine_rack", 1, "wine_rack", [P])
    for c in DYES:
        W = f"minecraft:{c}_wool"
        shaped(f"{c}_curtain", ["|", "W", "W"], {"W": W, "|": stick}, f"{c}_curtain", 2, "curtain", [W])
        shaped(f"{c}_awning", ["WWW", "| |"], {"W": W, "|": stick}, f"{c}_awning", 2, "awning", [W])
        shaped(f"{c}_bunk_bed", ["L", "B", "B"], {"L": "minecraft:ladder", "B": f"minecraft:{c}_bed"}, f"{c}_bunk_bed", 1, "bunk_bed",
               [f"minecraft:{c}_bed"])
    shaped("sink", [" I ", "C C", "PPP"], {"I": iron, "C": "minecraft:smooth_stone_slab", "P": planks}, "sink", 1, None, [iron])
    shaped("trash_can", [" N ", "I I", " I "], {"N": nug, "I": iron}, "trash_can", 1, None, [iron])
    shaped("mirror", ["PGP"], {"P": planks, "G": "minecraft:glass_pane"}, "mirror", 1, None, ["minecraft:glass_pane"])
    shaped("wall_clock", [" P ", "PCP", " P "], {"P": planks, "C": "minecraft:clock"}, "wall_clock", 1, None, ["minecraft:clock"])
    shaped("toilet", ["C  ", "CCC", " C "], {"C": clay}, "toilet", 1, None, [clay])
    shaped("bathtub", ["C C", "CCC"], {"C": clay}, "bathtub", 1, None, [clay])
    shaped("fireplace", ["BBB", "BCB"], {"B": "minecraft:bricks", "C": "minecraft:campfire"}, "fireplace", 1, None, ["minecraft:campfire"])
    shaped("ceiling_fan", [" I ", "PRP"], {"I": iron, "P": planks, "R": "minecraft:redstone"}, "ceiling_fan", 1, None, ["minecraft:redstone"])
    shaped("mailbox", ["III", " | "], {"I": iron, "|": stick}, "mailbox", 1, None, [iron])
    shaped("floor_lamp", ["L", "|", "C"], {"L": f"{MOD}:table_lamp", "|": stick, "C": copper}, "floor_lamp", 1, None, [f"{MOD}:table_lamp"])
    shaped("piano", ["PPP", "PNP"], {"P": planks, "N": "minecraft:note_block"}, "piano", 1, None, ["minecraft:note_block"])
    shaped("safe", ["ITI", "ICI", "III"], {"I": iron, "T": "minecraft:tripwire_hook", "C": "minecraft:chest"}, "safe", 1, None, [iron])


# ---------------- receitas que não podem colidir (vanilla e mods Tcordeiro)

def _expand_tags():
    tags = {}
    for n in zjar.namelist():
        if n.startswith("data/minecraft/tags/item/") and n.endswith(".json"):
            tags["#minecraft:" + n[len("data/minecraft/tags/item/"):-5]] = json.load(zjar.open(n))["values"]
    for dp, _, fs in os.walk(os.path.join(RES, "data", MOD, "tags", "item")):
        for fn in fs:
            tags[f"#{MOD}:" + fn[:-5]] = json.load(open(os.path.join(dp, fn)))["values"]
    cache = {}

    def items(ing, depth=0):
        if isinstance(ing, list):
            return set().union(*[items(i, depth) for i in ing])
        if isinstance(ing, dict):
            ing = ing.get("item") or ing.get("tag") and "#" + ing["tag"] or ""
        if not ing.startswith("#"):
            return {ing}
        if ing in cache:
            return cache[ing]
        out = set()
        for v in tags.get(ing, []):
            v = v["id"] if isinstance(v, dict) else v
            if depth < 8:
                out |= items(v, depth + 1)
        cache[ing] = out or {ing}
        return cache[ing]
    return items


def _grid_sets(r, items):
    if r.get("type") == "minecraft:crafting_shaped":
        rows = r["pattern"]
        width = max(len(x) for x in rows)
        return ("shaped", [[items(r["key"][ch]) if ch != " " else None for ch in row.ljust(width)] for row in rows])
    if r.get("type") == "minecraft:crafting_shapeless":
        return ("shapeless", [items(i) for i in r["ingredients"]])
    return None


def _overlap(a, b):
    if a[0] != b[0]:
        return False
    if a[0] == "shaped":
        A, B = a[1], b[1]
        if len(A) != len(B) or len(A[0]) != len(B[0]):
            return False
        for mirror in (False, True):
            ok = True
            for ra, rb in zip(A, B):
                rb = list(reversed(rb)) if mirror else rb
                for ca, cb in zip(ra, rb):
                    if (ca is None) != (cb is None) or (ca is not None and not (ca & cb)):
                        ok = False
                        break
                if not ok:
                    break
            if ok:
                return True
        return False
    A, B = a[1], b[1]
    if len(A) != len(B):
        return False
    used = [False] * len(B)

    def match(i):  # emparelhamento simples (as receitas sem forma têm poucos ingredientes)
        if i == len(A):
            return True
        for j in range(len(B)):
            if not used[j] and A[i] & B[j]:
                used[j] = True
                if match(i + 1):
                    return True
                used[j] = False
        return False
    return match(0)


def check_recipe_collisions():
    """Nenhuma receita do mod pode servir também a outra (do jogo, dos outros mods Tcordeiro ou nossa)."""
    items = _expand_tags()
    others = []
    for n in zjar.namelist():
        if n.startswith("data/minecraft/recipe/") and n.endswith(".json"):
            others.append((n, json.load(zjar.open(n))))
    cods = os.path.dirname(ROOT)
    for d in sorted(os.listdir(cods)):
        if d.startswith("tcordeiro-moveis") or not (d.startswith("tcordeiro-") or d in ("cubcoin", "teletransporter", "receitario")):
            continue
        for dp, _, fs in os.walk(os.path.join(cods, d, "src", "main", "resources", "data")):
            if "/recipe" not in dp:
                continue
            for fn in fs:
                if fn.endswith(".json"):
                    try:
                        others.append((f"{d}:{fn}", json.load(open(os.path.join(dp, fn)))))
                    except (json.JSONDecodeError, UnicodeDecodeError):
                        pass
    ours = {k: _grid_sets(r, items) for k, r in RECIPES.items()}
    theirs = [(n, _grid_sets(r, items)) for n, r in others]
    bad = []
    keys = sorted(ours)
    for i, k in enumerate(keys):
        if ours[k] is None:
            continue
        for n, g in theirs:
            if g and _overlap(ours[k], g):
                bad.append(f"{k} ↔ {n}")
        for k2 in keys[i + 1:]:
            if ours[k2] and _overlap(ours[k], ours[k2]):
                bad.append(f"{k} ↔ {k2}")
    assert not bad, "receitas que colidem:\n  " + "\n  ".join(bad)
    return len(theirs)


# ---------------- FurnitureParts.java (pivôs das partes móveis, das teclas do piano e das casas da adega)

def gen_parts_java():
    def f(v):
        return f"{float(v)}f"
    ops = {"rot": "ROT", "move": "MOVE", "scalex": "SCALEX", "spin": "SPIN", "clock_hour": "CLOCK_HOUR", "clock_minute": "CLOCK_MINUTE"}
    rows = []
    for kind in sorted(PECAS.ANIM):
        plist = []
        for p in PECAS.ANIM[kind]:
            name, op = p[0], p[1]
            if op == "move":
                ax, piv, amt = "x", p[2], 1
            elif op == "scalex":
                ax, piv, amt = "x", (p[2], 0, 0), p[3]
            elif op in ("spin", "clock_hour", "clock_minute"):
                ax, piv, amt = p[2], p[3], 1
            else:
                ax, piv, amt = p[2], p[3], p[4]
            plist.append(f'new Part("{kind}_{name}", Op.{ops[op]}, \'{ax}\', {f(piv[0])}, {f(piv[1])}, {f(piv[2])}, {f(amt)})')
        rows.append(f'\t\t\tMap.entry("{kind}", List.of({", ".join(plist)}))')
    keys = ",\n".join(f"\t\t\t{{{1 if b else 0}, {f(x1)}, {f(x2)}}}" for b, x1, x2 in PECAS.PIANO_KEYS)
    cells = ", ".join(f"{{{f(x)}, {f(y)}}}" for x, y in PECAS.WINE_CELLS)
    java = f"""package com.tcordeiro.moveis.anim;

import java.util.List;
import java.util.Map;

/**
 * As partes móveis dos móveis animados e onde rodam/deslizam (px; 16 = 1 bloco; modelo virado a norte). Vêm de
 * tools/blender/pecas.py (ANIM, PIANO_KEYS, WINE_CELLS): GERADO por tools/gen_data.py, não editar à mão.
 * Cada parte é desenhada pelo renderer com o modelo {{@code tmoveis:part/<bloco>/<parte>}} (o mesmo template do bloco).
 */
public final class FurnitureParts {{
	private FurnitureParts() {{}}

	/** ROT: roda {{@code amount}} graus (aberta) à volta do eixo no pivô; MOVE: desliza (px, py, pz); SCALEX: encolhe em x para px até
	 *  {{@code amount}}; SPIN: gira sem parar; CLOCK_*: ponteiros do relógio. */
	public enum Op {{ ROT, MOVE, SCALEX, SPIN, CLOCK_HOUR, CLOCK_MINUTE }}

	public record Part(String model, Op op, char axis, float px, float py, float pz, float amount) {{}}

	/** Tipo de peça → partes (pela ordem de pecas.ANIM). */
	public static final Map<String, List<Part>> BY_KIND = Map.ofEntries(
{(","+chr(10)).join(rows)});

	/** Teclas do piano pela ordem cromática (Sol3 = tecla 0 = nota 1 do bloco musical): {{preta?, x1, x2}} na largura de 32 px. */
	public static final float[][] PIANO_KEYS = {{
{keys}}};
	public static final float WHITE_Y1 = {f(PECAS.WHITE_Y[0])}, WHITE_Y2 = {f(PECAS.WHITE_Y[1])}, BLACK_Y1 = {f(PECAS.BLACK_Y[0])}, BLACK_Y2 = {f(PECAS.BLACK_Y[1])};
	public static final float WHITE_Z1 = {f(PECAS.WHITE_Z[0])}, WHITE_Z2 = {f(PECAS.WHITE_Z[1])}, BLACK_Z1 = {f(PECAS.BLACK_Z[0])}, BLACK_Z2 = {f(PECAS.BLACK_Z[1])};
	public static final float WHITE_SINK = {f(PECAS.PIANO_SINK[False])}, BLACK_SINK = {f(PECAS.PIANO_SINK[True])};

	/** Deslocamento (px) da garrafa da casa 0 para cada uma das 9 casas da adega (0 = em cima à esquerda, vista de frente). */
	public static final float[][] WINE_CELLS = {{{cells}}};
}}
"""
    path = os.path.join(JAVA, "anim", "FurnitureParts.java")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        fh.write(java)


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


def check_textures():
    """Cada textura usada pelos modelos (templates e filhos) existe no jogo, e cada variável usada está definida."""
    base = os.path.join(RES, "assets", MOD, "models", "block")
    bad = []
    for dp, _, fs in os.walk(base):
        for fn in fs:
            if not fn.endswith(".json"):
                continue
            m = json.load(open(os.path.join(dp, fn)))
            tex = dict(m.get("textures", {}))
            parent = m.get("parent", "")
            if parent.startswith(f"{MOD}:"):
                t = json.load(open(os.path.join(RES, "assets", MOD, "models", parent.split(":")[1] + ".json")))
                tex = {**t.get("textures", {}), **tex}
                els = t.get("elements", [])
            else:
                els = m.get("elements", [])
                if "template" not in dp:
                    continue
                continue  # os templates sozinhos têm variáveis por preencher (os filhos dão-nas)
            for v in tex.values():
                if v.startswith("minecraft:") and not jar_has(f"assets/minecraft/textures/{v.split(':')[1]}.png"):
                    bad.append(f"{fn}: {v}")
            for e in els:
                for f in e["faces"].values():
                    var = f["texture"].lstrip("#")
                    seen = set()
                    while var in tex and var not in seen and tex[var].startswith("#"):
                        seen.add(var)
                        var = tex[var].lstrip("#")
                    if var not in tex:
                        bad.append(f"{os.path.relpath(os.path.join(dp, fn), base)}: falta #{var}")
    assert not bad, "texturas em falta:\n  " + "\n  ".join(sorted(set(bad))[:40])


def check_refs():
    """Verificador offline de referências: blockstates e itens → modelos (e os pais) → texturas, tudo tem de existir (no mod ou no jar)."""
    A = os.path.join(RES, "assets", MOD)
    bad, seen = [], {}

    def tex_ok(ref):
        ns, p = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
        if ns == MOD:
            return os.path.exists(os.path.join(A, "textures", p + ".png"))
        return jar_has(f"assets/minecraft/textures/{p}.png")

    def model_ok(ref, where):
        if ref in seen:
            return seen[ref]
        ns, p = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
        if ns != MOD:
            ok = p.startswith("builtin/") or jar_has(f"assets/minecraft/models/{p}.json")
        else:
            path = os.path.join(A, "models", p + ".json")
            ok = os.path.exists(path)
            if ok:
                m = json.load(open(path))
                if "parent" in m and not model_ok(m["parent"], ref):
                    ok = False
                for v in m.get("textures", {}).values():
                    if not v.startswith("#") and not tex_ok(v):
                        bad.append(f"{ref}: textura em falta {v}")
        seen[ref] = ok
        if not ok:
            bad.append(f"{where}: modelo em falta {ref}")
        return ok

    def walk(o, where):
        if isinstance(o, dict):
            for k, v in o.items():
                if k == "model" and isinstance(v, str):
                    model_ok(v, where)
                else:
                    walk(v, where)
        elif isinstance(o, list):
            for v in o:
                walk(v, where)

    n = 0
    for d in ("blockstates", "items"):
        for dp, _, fs in os.walk(os.path.join(A, d)):
            for fn in fs:
                if fn.endswith(".json"):
                    walk(json.load(open(os.path.join(dp, fn))), os.path.relpath(os.path.join(dp, fn), A))
                    n += 1
    assert not bad, f"{len(bad)} referências em falta:\n  " + "\n  ".join(sorted(set(bad))[:40])
    return n, len(seen)


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
    gen_lang_v020()
    gen_templates()
    gen_wooden()
    cabinet_parts_v020()
    gen_sofas_beds()
    gen_lamps()
    gen_wood_v020()
    gen_colored_v020()
    gen_single_v020()
    gen_loot_tags()
    loot_v020()
    gen_loot_tags_merged()
    gen_recipes()
    recipes_v020()
    __import__("gen_finishes").run(globals())  # 0.3.0: madeira e verniz (tools/gen_finishes.py)
    n_other = check_recipe_collisions()
    gen_guide()
    gen_wood_types()
    gen_parts_java()
    check_names()
    gen_lang()
    clean_stale()
    check_textures()
    n_files, n_models = check_refs()
    print(f"referências: {n_files} blockstates/itens → {n_models} modelos → texturas, nada em falta")
    print(f"receitas conferidas contra {n_other} receitas do jogo e dos mods Tcordeiro: sem colisões")
    print(f"{len(WOODS)} madeiras ({', '.join(w['id'] for w in WOODS)}), {len(DYES)} cores, {len(all_blocks())} blocos, "
          f"{len(RECIPES)} receitas, {len(GUIDE) + 1} páginas no livro, {len(written)} ficheiros")


if __name__ == "__main__":
    main()
