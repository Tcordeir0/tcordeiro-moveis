#!/usr/bin/env python3
"""Móveis 0.3.0 — madeira e verniz. Chamado pelo tools/gen_data.py com UMA linha (run(globals())), depois das receitas e antes do
guia; usa as funções e os dados dele (write, tr, shaped, GUIDE, WOODS, LANG…). Gera:

  - texturas (a partir das tábuas e troncos descascados do jar 26.3, com tools/madeira/madeira_textures.py):
      textures/block/finish/<madeira>/<superfície>[_<acabamento>].png e textures/item/varnish[_brush]_<tom>.png;
  - tábuas, escadas e lajes envernizadas das madeiras do jogo (um bloco por madeira × forma × tom × polido);
  - os blocos novos (parquet, espinha de peixe, assoalho, lambri, viga) com a propriedade «finish» (crua, gasta 1–3, 7 tons, 7 polidos);
  - a propriedade «finish» nos móveis de madeira (FURNITURE): reescreve os blockstates, itens e loot que o gen_data fez;
  - a Serraria (modelo de tools/madeira/pecas_madeira.py), as receitas dela (tipo tmoveis:sawing), pincéis e vernizes;
  - traduções, tags, páginas do guia e finish/FinishFurniture.java.

Uso: python3 tools/gen_data.py   (este ficheiro não corre sozinho)
"""
import copy
import io
import json
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "madeira"))
import madeira_textures as mt  # noqa: E402
import brush_textures as bt  # noqa: E402
import pecas_madeira  # noqa: E402

# ------------------------------------------------------------------ dados

# Os móveis de madeira que levam verniz (propriedade «finish»). LISTA ÚNICA: os móveis novos acrescentam-se aqui (o Java lê-a de
# finish/FinishFurniture.java, gerado). Cada um tem de ter <madeira>_<sufixo>, e as classes dos blocos acrescentam FinishState.FINISH.
FURNITURE = ["chair", "stool", "table", "cabinet", "drawer",
             # 0.2.0 (todos os móveis de madeira; a bancada de canto é o estado corner=true da bancada)
             "counter", "nightstand", "wardrobe", "desk", "coffee_table", "side_table", "garden_bench", "planter_box", "shutters",
             "wine_rack"]

# tom: (id da textura em madeira_textures, pt_br, pt_pt, en, polido pt_br, polido pt_pt, polido en)
TONES = {
    "natural": ("natural", "Natural", "Natural", "Natural", "Natural Polido", "Natural Polido", "Polished Natural"),
    "honey": ("mel", "Mel", "Mel", "Honey", "Mel Polido", "Mel Polido", "Polished Honey"),
    "walnut": ("nogueira", "Nogueira", "Nogueira", "Walnut", "Nogueira Polida", "Nogueira Polida", "Polished Walnut"),
    "mahogany": ("mogno", "Mogno", "Mogno", "Mahogany", "Mogno Polido", "Mogno Polido", "Polished Mahogany"),
    "ebony": ("ebano", "Ébano", "Ébano", "Ebony", "Ébano Polido", "Ébano Polido", "Polished Ebony"),
    "white_lacquer": ("laca_branca", "Laca Branca", "Laca Branca", "White Lacquer", "Laca Branca Polida", "Laca Branca Polida",
                      "Polished White Lacquer"),
    "black_lacquer": ("laca_preta", "Laca Preta", "Laca Preta", "Black Lacquer", "Laca Preta Polida", "Laca Preta Polida",
                      "Polished Black Lacquer"),
}
WORN = {1: ("Desgaste Leve", "Desgaste Leve", "Lightly Weathered"), 2: ("Desgaste Médio", "Desgaste Médio", "Weathered"),
        3: ("Desgaste Forte", "Desgaste Forte", "Heavily Weathered")}
# Ordem = a do enum Finish em Java (a primeira é a de omissão).
VARNISHED = [t + p for t in TONES for p in ("", "_polished")]
FINISHES = ["raw", "worn_1", "worn_2", "worn_3"] + VARNISHED

# O verniz: frasco + favo de mel (cera) + o que dá a cor do tom.
VARNISH_RECIPES = {
    "natural": ["minecraft:glass_bottle", "minecraft:honeycomb", "minecraft:honeycomb"],
    "honey": ["minecraft:glass_bottle", "minecraft:honeycomb", "minecraft:honey_bottle"],  # a garrafa de mel devolve o frasco dela
    "walnut": ["minecraft:glass_bottle", "minecraft:honeycomb", "minecraft:brown_dye"],
    "mahogany": ["minecraft:glass_bottle", "minecraft:honeycomb", "minecraft:red_dye", "minecraft:brown_dye"],
    "ebony": ["minecraft:glass_bottle", "minecraft:honeycomb", "minecraft:charcoal"],
    "white_lacquer": ["minecraft:glass_bottle", "minecraft:honeycomb", "minecraft:white_dye"],
    "black_lacquer": ["minecraft:glass_bottle", "minecraft:honeycomb", "minecraft:black_dye"],
}

# blocos novos: (pt_br, pt_pt, en, género pt m/f, palavra das escadas/lajes em en, tem escadas e lajes, superfície)
KINDS = {
    "parquet": ("Parquet", "Parquet", "Parquet", "m", "Parquet", True),
    "herringbone": ("Espinha de Peixe", "Espinha de Peixe", "Herringbone", "f", "Herringbone", True),
    "floorboards": ("Assoalho", "Soalho", "Floorboards", "m", "Floorboard", True),
    "paneling": ("Lambri", "Lambri", "Paneling", "m", None, False),
    "beam": ("Viga", "Viga", "Beam", "f", None, False),
}
STAIRS_ID = {"parquet": "parquet", "herringbone": "herringbone", "floorboards": "floorboard"}
# superfícies das texturas: nome → função que a faz a partir das tábuas (None = a própria textura do jogo)
SURFACES = {"parquet": mt.parquet, "herringbone": mt.herringbone, "floorboards": mt.floorboards, "paneling": mt.paneling,
            "beam_side": mt.beam_side, "beam_end": mt.beam_top}

g = {}  # globais do gen_data
MOD = "tmoveis"


def sfx(f):
    return "" if f == "raw" else "_" + f


def finish_image(base, f):
    if f == "raw":
        return base
    if f.startswith("worn_"):
        return mt.weather(base, int(f[-1]))
    tone = f.removesuffix("_polished")
    v = mt.varnish(base, TONES[tone][0])
    return mt.polish(v) if f.endswith("_polished") else v


def save_png(rel, im):
    path = os.path.join(g["RES"], rel)
    buf = io.BytesIO()
    im.save(buf, "PNG")
    data = buf.getvalue()
    if os.path.exists(path) and open(path, "rb").read() == data:
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(data)


def jar_png(path):
    return Image.open(io.BytesIO(g["zjar"].read(path))).convert("RGBA")


def tex(w, surface, f):
    """Referência de textura de uma superfície com um acabamento (crua das tábuas/tronco = a do jogo)."""
    if f == "raw" and surface == "planks":
        return f"minecraft:block/{w['planks']}"
    if f == "raw" and surface == "log":
        return f"minecraft:block/{w['log']}"
    return f"{MOD}:block/finish/{w['id']}/{surface}{sfx(f)}"


TEXTURES = 0


def gen_textures():
    global TEXTURES
    n = 0
    for w in g["WOODS"]:
        planks = jar_png(f"assets/minecraft/textures/block/{w['planks']}.png")
        log = jar_png(f"assets/minecraft/textures/block/{w['log']}.png")
        bases = {"planks": planks, "log": log}
        bases.update({s: fn(planks) for s, fn in SURFACES.items()})
        for s, base in bases.items():
            for f in FINISHES:
                if f == "raw" and s in ("planks", "log"):
                    continue
                save_png(f"assets/{MOD}/textures/block/finish/{w['id']}/{s}{sfx(f)}.png", finish_image(base, f))
                n += 1
    bottle = jar_png("assets/minecraft/textures/item/honey_bottle.png")
    for t, (tid, *_r) in TONES.items():
        save_png(f"assets/{MOD}/textures/item/varnish_brush_{t}.png", bt.brush(tid))
        save_png(f"assets/{MOD}/textures/item/varnish_{t}.png", varnish_bottle(bottle, tid))
        n += 2
    TEXTURES = n


def varnish_bottle(bottle, tid):
    """A garrafa de mel do jogo com o líquido na cor do tom (os píxeis quentes = mel)."""
    dark, light, _ = mt.TONES[tid]
    out = bottle.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, gg, b, a = px[x, y]
            if a and r > b + 40 and r >= gg:
                l = (0.299 * r + 0.587 * gg + 0.114 * b) / 255
                px[x, y] = mt.lerp(dark, light, min(1.0, l * 1.2)) + (a,)
    return out


# ------------------------------------------------------------------ modelos

def model(rel, data):
    g["write"](f"assets/{MOD}/models/block/{rel}.json", data)
    return f"{MOD}:block/{rel}"


def cube_models(w, name, side, top=None, bottom=None):
    """bloco cheio + escadas + lajes com estas texturas; devolve os ids dos modelos."""
    top = top or side
    bottom = bottom or side
    t = {"bottom": bottom, "top": top, "side": side}
    return {
        "block": model(f"finish/{w}/{name}", {"parent": "minecraft:block/cube_all", "textures": {"all": side}}),
        "stairs": model(f"finish/{w}/{name}_stairs", {"parent": "minecraft:block/stairs", "textures": t}),
        "stairs_inner": model(f"finish/{w}/{name}_stairs_inner", {"parent": "minecraft:block/inner_stairs", "textures": t}),
        "stairs_outer": model(f"finish/{w}/{name}_stairs_outer", {"parent": "minecraft:block/outer_stairs", "textures": t}),
        "slab": model(f"finish/{w}/{name}_slab", {"parent": "minecraft:block/slab", "textures": t}),
        "slab_top": model(f"finish/{w}/{name}_slab_top", {"parent": "minecraft:block/slab_top", "textures": t}),
    }


def vanilla_stairs_variants(models):
    """As variantes das escadas do jogo (oak_stairs), com os nossos modelos."""
    vs = g["jar_json"]("assets/minecraft/blockstates/oak_stairs.json")["variants"]
    out = {}
    for k, v in vs.items():
        v = dict(v)
        m = v["model"]
        v["model"] = models["stairs_inner"] if m.endswith("_inner") else models["stairs_outer"] if m.endswith("_outer") else models["stairs"]
        out[k] = v
    return out


def slab_variants(models, full):
    return {"type=bottom": {"model": models["slab"]}, "type=top": {"model": models["slab_top"]}, "type=double": {"model": full}}


def with_finish(variants, f):
    return {(f"finish={f}," + k) if k else f"finish={f}": v for k, v in variants.items()}


def item_select(name, cases):
    """Item com o modelo do acabamento guardado no componente block_state (os blocos partidos guardam o acabamento)."""
    g["write"](f"assets/{MOD}/items/{name}.json", {"model": {
        "type": "minecraft:select", "property": "minecraft:block_state", "block_state_property": "finish",
        "cases": [{"when": f, "model": m} for f, m in cases.items() if f != "raw"], "fallback": cases["raw"]}})


def plain(m):
    return {"type": "minecraft:model", "model": m}


# ------------------------------------------------------------------ loot

def finish_modifiers(block_id):
    """Ao partir: guarda o acabamento (só se não for cru, para empilhar com os novos) e põe-no no nome: «Parquet de Carvalho (Mel)»."""
    mods = [{"type": "minecraft:copy_state", "block": f"{MOD}:{block_id}", "properties": ["finish"],
             "condition": {"type": "minecraft:inverted", "term": {"type": "minecraft:match_block", "blocks": f"{MOD}:{block_id}",
                                                                  "state": {"finish": "raw"}}}}]
    for f in FINISHES[1:]:
        mods.append({"type": "minecraft:set_name", "target": "item_name",
                     "name": {"translate": f"finish.{MOD}.named", "with": [{"translate": f"block.{MOD}.{block_id}"},
                                                                         {"translate": f"finish.{MOD}.{f}"}]},
                     "condition": {"type": "minecraft:match_block", "blocks": f"{MOD}:{block_id}", "state": {"finish": f}}})
    return mods


def loot(block_id, slab=False, finish=False):
    mods = []
    if slab:
        mods.append({"type": "minecraft:set_count", "count": 2,
                     "condition": {"type": "minecraft:match_block", "blocks": f"{MOD}:{block_id}", "state": {"type": "double"}}})
    if finish:
        mods += finish_modifiers(block_id)
    mods.append({"type": "minecraft:explosion_decay"})
    g["write"](f"data/{MOD}/loot_table/blocks/{block_id}.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{block_id}", "modifier": mods}]}],
        "random_sequence": f"{MOD}:blocks/{block_id}"})


# ------------------------------------------------------------------ blocos

BLOCKS = []        # todos os blocos deste gerador (ids)
FINISH_BLOCKS = []  # os que têm a propriedade finish
STAIRS, SLABS = [], []


def tone_name(f, lg):
    i = {"br": 0, "pt": 1, "en": 2}[lg]
    if f.startswith("worn_"):
        return WORN[int(f[-1])][i]
    t = f.removesuffix("_polished")
    return TONES[t][4 + i] if f.endswith("_polished") else TONES[t][1 + i]


def gen_varnished_vanilla():
    """Tábuas, escadas e lajes do jogo envernizadas: um bloco por madeira × forma × acabamento (14)."""
    L = g["LANG"]
    for w in g["WOODS"]:
        wid = w["id"]
        for f in VARNISHED:
            pt = tex(w, "planks", f)
            ms = cube_models(wid, f"planks{sfx(f)}", pt)
            planks, stairs, slab = f"{wid}_planks{sfx(f)}", f"{wid}_stairs{sfx(f)}", f"{wid}_slab{sfx(f)}"
            g["write"](f"assets/{MOD}/blockstates/{planks}.json", {"variants": {"": {"model": ms["block"]}}})
            g["write"](f"assets/{MOD}/blockstates/{stairs}.json", {"variants": vanilla_stairs_variants(ms)})
            g["write"](f"assets/{MOD}/blockstates/{slab}.json", {"variants": slab_variants(ms, ms["block"])})
            for b, m in ((planks, ms["block"]), (stairs, ms["stairs"]), (slab, ms["slab"])):
                g["write"](f"assets/{MOD}/items/{b}.json", {"model": plain(m)})
            loot(planks)
            loot(stairs)
            loot(slab, slab=True)
            for b, vk in ((planks, f"{wid}_planks"), (stairs, f"{wid}_stairs"), (slab, f"{wid}_slab")):
                g["tr"](f"block.{MOD}.{b}", *(f"{L[lg]['block.minecraft.' + vk]} ({tone_name(f, lg)})" for lg in ("br", "pt", "en")))
            BLOCKS.extend([planks, stairs, slab])
            STAIRS.append(stairs)
            SLABS.append(slab)


def gen_new_blocks():
    """Parquet, espinha de peixe e assoalho (com escadas e lajes), lambri e viga, em todas as madeiras, com «finish»."""
    L = g["LANG"]
    for w in g["WOODS"]:
        wid = w["id"]
        wn = g["wood_names"](w)
        for kind, (br, pt, en, gender, en_st, has_st) in KINDS.items():
            gi = 1 if gender == "f" else 0
            base = f"{wid}_{kind}"
            full = {"br": f"{br} {wn['br'][gi]}", "pt": f"{pt} {wn['pt'][gi]}", "en": f"{wn['en']} {en}"}
            g["tr"](f"block.{MOD}.{base}", full["br"], full["pt"], full["en"])
            BLOCKS.append(base)
            FINISH_BLOCKS.append(base)
            if kind == "beam":
                vs, cases = {}, {}
                for f in FINISHES:
                    side, end = tex(w, "beam_side", f), tex(w, "beam_end", f)
                    mv = model(f"finish/{wid}/beam{sfx(f)}", {"parent": "minecraft:block/cube_column", "textures": {"end": end, "side": side}})
                    mh = model(f"finish/{wid}/beam_horizontal{sfx(f)}",
                               {"parent": "minecraft:block/cube_column_horizontal", "textures": {"end": end, "side": side}})
                    vs[f"axis=x,finish={f}"] = {"model": mh, "x": 90, "y": 90}
                    vs[f"axis=y,finish={f}"] = {"model": mv}
                    vs[f"axis=z,finish={f}"] = {"model": mh, "x": 90}
                    cases[f] = plain(mv)
                g["write"](f"assets/{MOD}/blockstates/{base}.json", {"variants": vs})
                item_select(base, cases)
                loot(base, finish=True)
                continue
            if kind == "paneling":
                vs, cases = {}, {}
                for f in FINISHES:
                    m = model(f"finish/{wid}/paneling{sfx(f)}", {"parent": "minecraft:block/cube_column",
                                                               "textures": {"end": tex(w, "planks", f), "side": tex(w, "paneling", f)}})
                    vs[f"finish={f}"] = {"model": m}
                    cases[f] = plain(m)
                g["write"](f"assets/{MOD}/blockstates/{base}.json", {"variants": vs})
                item_select(base, cases)
                loot(base, finish=True)
                continue
            st, sl = f"{wid}_{STAIRS_ID[kind]}_stairs", f"{wid}_{STAIRS_ID[kind]}_slab"
            bv, sv, lv = {}, {}, {}
            bc, sc, lc = {}, {}, {}
            for f in FINISHES:
                ms = cube_models(wid, f"{kind}{sfx(f)}", tex(w, kind, f))
                bv[f"finish={f}"] = {"model": ms["block"]}
                sv.update(with_finish(vanilla_stairs_variants(ms), f))
                lv.update(with_finish(slab_variants(ms, ms["block"]), f))
                bc[f], sc[f], lc[f] = plain(ms["block"]), plain(ms["stairs"]), plain(ms["slab"])
            g["write"](f"assets/{MOD}/blockstates/{base}.json", {"variants": bv})
            g["write"](f"assets/{MOD}/blockstates/{st}.json", {"variants": sv})
            g["write"](f"assets/{MOD}/blockstates/{sl}.json", {"variants": lv})
            item_select(base, bc)
            item_select(st, sc)
            item_select(sl, lc)
            loot(base, finish=True)
            loot(st, finish=True)
            loot(sl, slab=True, finish=True)
            stairs_word = {"br": f"Escadas de {br} {wn['br'][gi]}", "pt": f"Escadas de {pt} {wn['pt'][gi]}",
                           "en": f"{wn['en']} {en_st} Stairs"}
            # laje: «Laje» no Brasil, «Degrau» em Portugal (as palavras do próprio jogo)
            slab_br = L["br"]["block.minecraft.oak_slab"].split(" ")[0]
            slab_pt = L["pt"]["block.minecraft.oak_slab"].split(" ")[0]
            g["tr"](f"block.{MOD}.{st}", stairs_word["br"], stairs_word["pt"], stairs_word["en"])
            g["tr"](f"block.{MOD}.{sl}", f"{slab_br} de {br} {wn['br'][gi]}", f"{slab_pt} de {pt} {wn['pt'][gi]}", f"{wn['en']} {en_st} Slab")
            BLOCKS.extend([st, sl])
            FINISH_BLOCKS.extend([st, sl])
            STAIRS.append(st)
            SLABS.append(sl)


def remap(obj, w, f):
    """Troca, numa definição de item/blockstate, os modelos tmoveis:block/<madeira>_X pelos do acabamento (e cria-os)."""
    if isinstance(obj, dict):
        out = {}
        for k, v in obj.items():
            if k == "model" and isinstance(v, str) and v.startswith(f"{MOD}:block/{w['id']}_"):
                out[k] = furniture_model(w, v.split("block/", 1)[1], f)
            else:
                out[k] = remap(v, w, f)
        return out
    if isinstance(obj, list):
        return [remap(v, w, f) for v in obj]
    return obj


def furniture_model(w, raw_name, f):
    """Modelo-filho do móvel com as texturas de tábuas e tronco trocadas pelas do acabamento."""
    if f == "raw":
        return f"{MOD}:block/{raw_name}"
    src = json.load(open(os.path.join(g["RES"], f"assets/{MOD}/models/block/{raw_name}.json")))
    swap = {f"minecraft:block/{w['planks']}": tex(w, "planks", f), f"minecraft:block/{w['log']}": tex(w, "log", f)}
    textures = src.get("textures", {})
    if not any(v in swap for v in textures.values()):
        return f"{MOD}:block/{raw_name}"  # sem madeira (ex.: a garrafa da adega): o mesmo modelo em todos os acabamentos
    src["textures"] = {k: swap.get(v, v) for k, v in textures.items()}
    if raw_name.startswith("part/"):  # part/<madeira>_<móvel>/<parte> → finish/<madeira>/part/<móvel>/<parte>_<acabamento>
        _, block, part = raw_name.split("/")
        return model(f"finish/{w['id']}/part/{block[len(w['id']) + 1:]}/{part}{sfx(f)}", src)
    return model(f"finish/{w['id']}/{raw_name[len(w['id']) + 1:]}{sfx(f)}", src)


PART_MODELS = 0


def gen_furniture_parts(w, bid):
    """As partes móveis (portas, gavetas, persianas…) que o BlockEntityRenderer desenha: o item part/<bloco>/<parte> passa a escolher
    o modelo pelo componente block_state «finish» (o renderer põe-lhe o acabamento do bloco; ver PartStacks/FurnitureParts.partStack).
    Mesmo número de definições de itens; só os modelos crescem (um por parte com madeira × 17 acabamentos)."""
    global PART_MODELS
    prefix = f"assets/{MOD}/items/part/{bid}/"
    for rel in sorted(r for r in g["written"] if r.startswith(prefix)):
        item = json.load(open(os.path.join(g["RES"], rel)))
        raw = item["model"]["model"].split("block/", 1)[1]  # part/<bloco>/<parte>
        cases = {f: plain(furniture_model(w, raw, f)) for f in FINISHES}
        if len({c["model"] for c in cases.values()}) == 1:
            continue
        PART_MODELS += len(FINISHES) - 1
        g["write"](rel, {"model": {
            "type": "minecraft:select", "property": "minecraft:block_state", "block_state_property": "finish",
            "cases": [{"when": f, "model": m} for f, m in cases.items() if f != "raw"], "fallback": cases["raw"]}})


def gen_furniture():
    """Os móveis de madeira (FURNITURE) ganham «finish»: os blockstates, itens e loot que o gen_data escreveu são reescritos."""
    res = g["RES"]
    for w in g["WOODS"]:
        for suffix in FURNITURE:
            bid = f"{w['id']}_{suffix}"
            bs_path = os.path.join(res, f"assets/{MOD}/blockstates/{bid}.json")
            bs = json.load(open(bs_path))
            if "variants" in bs:
                out = {}
                for f in FINISHES:
                    out.update(with_finish(remap(bs["variants"], w, f), f))
                new = {"variants": out}
            else:
                parts = []
                for f in FINISHES:
                    for p in bs["multipart"]:
                        p2 = remap(p, w, f)
                        when = p.get("when")
                        if when is None:
                            p2["when"] = {"finish": f}
                        elif "OR" in when or "AND" in when:
                            p2["when"] = {"AND": [{"finish": f}, when]}
                        else:
                            p2["when"] = {**when, "finish": f}
                        parts.append(p2)
                new = {"multipart": parts}
            g["write"](f"assets/{MOD}/blockstates/{bid}.json", new)
            item = json.load(open(os.path.join(res, f"assets/{MOD}/items/{bid}.json")))
            item_select(bid, {f: remap(item["model"], w, f) for f in FINISHES})
            lt = json.load(open(os.path.join(res, f"data/{MOD}/loot_table/blocks/{bid}.json")))
            for pool in lt["pools"]:
                for e in pool["entries"]:
                    if e.get("name") == f"{MOD}:{bid}":
                        e["modifier"] = e.get("modifier", []) + finish_modifiers(bid)
            g["write"](f"data/{MOD}/loot_table/blocks/{bid}.json", lt)
            gen_furniture_parts(w, bid)


# ------------------------------------------------------------------ Serraria

def els(boxes):
    def r(v):
        return round(v, 3) if v != int(v) else int(v)
    return [{"from": [r(b[0]), r(b[1]), r(b[2])], "to": [r(b[3]), r(b[4]), r(b[5])],
             "faces": {f: {"texture": b[6]} for f in ("north", "south", "east", "west", "up", "down")}} for b in boxes]


def gen_sawmill():
    boxes = pecas_madeira.PIECES["sawmill"]
    spin = [b for b in boxes if b[6] == "#blade" or (b[0], b[1]) == (7.5, 15.5)]  # disco + eixo: o BlockEntityRenderer roda-os
    still = [b for b in boxes if b not in spin]
    model("template/sawmill", {"parent": "minecraft:block/block", "textures": {"particle": "#planks"}, "elements": els(still)})
    model("template/sawmill_blade", {"textures": {"particle": "#blade"}, "elements": els(spin)})
    m = model("sawmill", {"parent": f"{MOD}:block/template/sawmill", "textures": {
        "planks": "minecraft:block/spruce_planks", "log": "minecraft:block/stripped_spruce_log", "metal": "minecraft:block/iron_block"}})
    mb = model("sawmill_blade", {"parent": f"{MOD}:block/template/sawmill_blade", "textures": {
        "blade": "minecraft:block/iron_block", "metal": "minecraft:block/anvil"}})
    g["write"](f"assets/{MOD}/blockstates/sawmill.json", {"variants": {
        f"facing={d}": ({"model": m, "y": y} if y else {"model": m}) for d, y in g["YROT"].items()}})
    g["write"](f"assets/{MOD}/items/sawmill.json", {"model": {"type": "minecraft:composite", "models": [plain(m), plain(mb)]}})
    g["write"](f"assets/{MOD}/items/sawmill_blade.json", {"model": plain(mb)})  # o disco que o renderer desenha a rodar
    loot("sawmill")
    BLOCKS.append("sawmill")
    g["tr"](f"block.{MOD}.sawmill", "Serraria", "Serração", "Sawmill")
    g["tr"](f"container.{MOD}.sawmill", "Serraria", "Serração", "Sawmill")
    g["shaped"]("sawmill", [" I ", "PPP", "| |"], {"I": "minecraft:iron_ingot", "P": "#minecraft:planks", "|": "minecraft:stick"},
                "sawmill", 1, None, ["minecraft:iron_ingot", "#minecraft:logs"])


SAWING = []


def sawing(name, ingredient, result, count=1):
    r = {"type": f"{MOD}:sawing", "ingredient": ingredient, "result": {"id": result}}
    if count > 1:
        r["result"]["count"] = count
    g["write"](f"data/{MOD}/recipe/sawing/{name}.json", r)
    SAWING.append(name)


def gen_sawing():
    """Receitas da Serraria: tronco → tábuas (mais 50% do que a bancada), tábuas → blocos novos, escadas, lajes ×2, paus."""
    for w in g["WOODS"]:
        wid, P = w["id"], f"minecraft:{w['planks']}"
        planks_recipe = g["jar_json"](f"data/minecraft/recipe/{w['planks']}.json")
        bench = planks_recipe["result"].get("count", 1)
        sawing(f"{w['planks']}", planks_recipe["ingredients"][0], P, bench * 3 // 2)
        for kind in KINDS:
            sawing(f"{wid}_{kind}", P, f"{MOD}:{wid}_{kind}")
            if kind in STAIRS_ID:
                st = STAIRS_ID[kind]
                sawing(f"{wid}_{st}_stairs", f"{MOD}:{wid}_{kind}", f"{MOD}:{wid}_{st}_stairs")
                sawing(f"{wid}_{st}_slab", f"{MOD}:{wid}_{kind}", f"{MOD}:{wid}_{st}_slab", 2)
        sawing(f"{wid}_stairs", P, f"minecraft:{wid}_stairs")
        sawing(f"{wid}_slab", P, f"minecraft:{w['slab']}", 2)
        sawing(f"{wid}_sticks", P, "minecraft:stick", 2)


# ------------------------------------------------------------------ itens: verniz e pincéis

def gen_items():
    L = g["LANG"]
    for t, (tid, br, pt, en, *_p) in TONES.items():
        g["write"](f"assets/{MOD}/models/item/varnish_{t}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/varnish_{t}"}})
        g["write"](f"assets/{MOD}/items/varnish_{t}.json", {"model": plain(f"{MOD}:item/varnish_{t}")})
        g["write"](f"assets/{MOD}/models/item/varnish_brush_{t}.json",
                   {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MOD}:item/varnish_brush_{t}"}})
        g["write"](f"assets/{MOD}/items/varnish_brush_{t}.json", {"model": plain(f"{MOD}:item/varnish_brush_{t}")})
        g["tr"](f"item.{MOD}.varnish_{t}", f"Verniz {br}" if not br.startswith("Laca") else br, f"Verniz {pt}" if not pt.startswith("Laca") else pt,
                f"{en} Varnish" if not en.endswith("Lacquer") else en)
        g["tr"](f"item.{MOD}.varnish_brush_{t}", f"Pincel de Verniz ({br})", f"Pincel de Verniz ({pt})", f"Varnish Brush ({en})")
        g["shapeless"](f"varnish_{t}", VARNISH_RECIPES[t], f"varnish_{t}", 1, "varnish")
        g["shaped"](f"varnish_brush_{t}", ["W", "V", "|"], {"W": "#minecraft:wool", "V": f"{MOD}:varnish_{t}", "|": "minecraft:stick"},
                    f"varnish_brush_{t}", 1, "varnish_brush", [f"{MOD}:varnish_{t}"])
    for f in FINISHES[1:]:
        g["tr"](f"finish.{MOD}.{f}", tone_name(f, "br"), tone_name(f, "pt"), tone_name(f, "en"))
    g["tr"](f"finish.{MOD}.raw", "Madeira Crua", "Madeira Crua", "Raw Wood")
    g["tr"](f"finish.{MOD}.named", "%s (%s)", "%s (%s)", "%s (%s)")
    g["tr"](f"itemGroup.{MOD}.wood", "Madeira e Verniz", "Madeira e Verniz", "Wood and Varnish")
    g["tr"](f"tooltip.{MOD}.brush", "Botão direito: envernizar. De novo: polir.", "Clique direito: envernizar. De novo: polir.",
            "Right-click: varnish. Again: polish.")


# ------------------------------------------------------------------ tags

def gen_tags():
    res = g["RES"]
    axe = f"data/minecraft/tags/block/mineable/axe.json"
    data = json.load(open(os.path.join(res, axe)))
    data["values"] += [f"{MOD}:{b}" for b in BLOCKS]
    g["write"](axe, data)
    for kind in ("block", "item"):
        g["write"](f"data/minecraft/tags/{kind}/wooden_stairs.json", {"replace": False, "values": [f"{MOD}:{b}" for b in STAIRS]})
        g["write"](f"data/minecraft/tags/{kind}/wooden_slabs.json", {"replace": False, "values": [f"{MOD}:{b}" for b in SLABS]})
    g["write"](f"data/{MOD}/tags/item/varnish_brushes.json", {"values": [f"{MOD}:varnish_brush_{t}" for t in TONES]})
    g["write"](f"data/{MOD}/tags/item/varnishes.json", {"values": [f"{MOD}:varnish_{t}" for t in TONES]})
    g["write"](f"data/{MOD}/tags/block/finishable.json", {"values": [f"{MOD}:{b}" for b in FINISH_BLOCKS]
                                                          + [f"{MOD}:{w['id']}_{s}" for s in FURNITURE for w in g["WOODS"]]})


# ------------------------------------------------------------------ guia

def gen_guide_pages():
    P = g["P"]
    pages = [
        P("finish", None,
          ("Verniz", "Muda a cor da madeira e a protege da chuva.\n\n7 tons: Natural, Mel, Nogueira, Mogno, Ébano, Laca Branca e Laca Preta.\n\nServe em tábuas, escadas, lajes, blocos novos e móveis."),
          ("Verniz", "Muda a cor da madeira e protege-a da chuva.\n\n7 tons: Natural, Mel, Nogueira, Mogno, Ébano, Laca Branca e Laca Preta.\n\nServe em tábuas, escadas, degraus, blocos novos e móveis."),
          ("Varnish", "Changes the color of wood and protects it from rain.\n\n7 tones: Natural, Honey, Walnut, Mahogany, Ebony, White and Black Lacquer.\n\nFor planks, stairs, slabs, new blocks, furniture.")),
        P("varnish", "varnish_honey",
          ("Fazer verniz", "Frasco de vidro, favo de mel e o que dá a cor do tom (aqui: garrafa de mel)."),
          ("Fazer verniz", "Frasco de vidro, favo de mel e o que dá a cor do tom (aqui: garrafa de mel)."),
          ("Making varnish", "Glass bottle, honeycomb and what gives the tone its color (here: a honey bottle).")),
        P("varnish_tones", None,
          ("Os 7 tons", "Frasco, favo de mel e:\nNatural: outro favo\nMel: garrafa de mel\nNogueira: marrom\nMogno: marrom e vermelho\nÉbano: carvão vegetal\nLaca Branca: branco\nLaca Preta: preto\n\nAs cores são corantes."),
          ("Os 7 tons", "Frasco, favo de mel e:\nNatural: outro favo\nMel: garrafa de mel\nNogueira: castanho\nMogno: castanho e vermelho\nÉbano: carvão vegetal\nLaca Branca: branco\nLaca Preta: preto\n\nAs cores são corantes."),
          ("The 7 tones", "Bottle, honeycomb and:\nNatural: more honeycomb\nHoney: honey bottle\nWalnut: brown\nMahogany: brown and red\nEbony: charcoal\nWhite Lacquer: white\nBlack Lacquer: black\n\nThe colors are dyes.")),
        P("varnish_brush", "varnish_brush_honey",
          ("Pincel de Verniz", "Lã, verniz e um pau. Um pincel por tom; dá 64 demãos. Devolve o frasco."),
          ("Pincel de Verniz", "Lã, verniz e um pau. Um pincel por tom; dá 64 demãos. Devolve o frasco."),
          ("Varnish Brush", "Wool, varnish and a stick. One brush per tone; 64 coats. Gives the bottle back.")),
        P("brushing", None,
          ("Envernizar", "Clique com o botão direito, com o pincel, em tábuas, escadas, lajes, blocos novos ou móveis: ficam da cor do verniz.\n\nO carvalho continua carvalho, virado para o mesmo lado. Portas, cercas e alçapões não."),
          ("Envernizar", "Clica com o direito, com o pincel, em tábuas, escadas, degraus, blocos novos ou móveis: ficam da cor do verniz.\n\nO carvalho continua carvalho, virado para o mesmo lado. Portas, cercas e alçapões não."),
          ("Varnishing", "Right-click planks, stairs, slabs, new blocks or furniture with the brush: they take the varnish color.\n\nOak stays oak, facing the same way. Not doors, fences or trapdoors.")),
        P("polish", None,
          ("Polir", "Passe o mesmo pincel outra vez: a madeira fica polida, com mais brilho.\n\nUm pincel de outro tom troca a cor (sem polimento)."),
          ("Polir", "Passa o mesmo pincel outra vez: a madeira fica polida, com mais brilho.\n\nUm pincel de outro tom troca a cor (sem polimento)."),
          ("Polishing", "Use the same brush again: the wood gets polished, with more shine.\n\nA brush of another tone changes the color (not polished).")),
        P("remove_varnish", None,
          ("Tirar o verniz", "Clique com o botão direito segurando um machado, como na cera do cobre:\n\npolida → envernizada → madeira crua.\n\nCada raspagem gasta o machado."),
          ("Tirar o verniz", "Clica com o direito com um machado, como na cera do cobre:\n\npolida → envernizada → madeira crua.\n\nCada raspagem gasta o machado."),
          ("Removing varnish", "Right-click with an axe, like copper wax:\n\npolished → varnished → raw wood.\n\nEach scrape uses the axe.")),
        P("weathering", None,
          ("Desgaste", "A madeira crua dos Móveis (blocos novos e móveis) se desgasta na chuva, com o céu por cima: 3 estágios, cada um cerca de 9 dias de jogo.\n\nA madeira do jogo e as casas que já existem nunca mudam."),
          ("Desgaste", "A madeira crua dos Móveis (blocos novos e móveis) gasta-se à chuva, com o céu por cima: 3 estágios, cada um cerca de 9 dias de jogo.\n\nA madeira do jogo e as casas que já existem nunca mudam."),
          ("Weathering", "Raw wood from this mod (new blocks and furniture) weathers in the rain under open sky: 3 stages, about 9 game days each.\n\nVanilla wood and existing houses never change.")),
        P("protect", None,
          ("Proteger", "Madeira envernizada nunca se desgasta.\n\nJá gasta? O machado raspa um estágio. O pincel numa madeira gasta repõe a cor e a deixa protegida."),
          ("Proteger", "Madeira envernizada nunca se gasta.\n\nJá gasta? O machado raspa um estágio. O pincel numa madeira gasta repõe a cor e deixa-a protegida."),
          ("Protecting", "Varnished wood never weathers.\n\nAlready weathered? The axe scrapes one stage off. A brush on weathered wood restores the color and protects it.")),
        P("sawmill", "sawmill",
          ("Serraria", "Corta madeira como o cortador de pedras. A serra gira enquanto a tela está aberta."),
          ("Serração", "Corta madeira como o cortador de pedras. A serra roda enquanto o ecrã está aberto."),
          ("Sawmill", "Cuts wood like the stonecutter. The saw spins while the screen is open.")),
        P("sawmill_use", None,
          ("Na Serraria", "Tronco → 6 tábuas (a bancada dá 4).\n\nTábua → parquet, espinha, assoalho, lambri, viga, escadas, 2 lajes ou 2 paus.\n\nParquet, espinha, assoalho → escadas ou 2 lajes."),
          ("Na Serração", "Tronco → 6 tábuas (a bancada dá 4).\n\nTábua → parquet, espinha, soalho, lambri, viga, escadas, 2 degraus ou 2 paus.\n\nParquet, espinha, soalho → escadas ou 2 degraus."),
          ("In the Sawmill", "Log → 6 planks (crafting gives 4).\n\nPlank → parquet, herringbone, floorboards, paneling, beam, stairs, 2 slabs or 2 sticks.\n\nParquet, herringbone, floorboards → stairs or 2 slabs.")),
        P("parquet", "sawing/oak_parquet",
          ("Parquet", "Quadrados de tábuas deitadas e em pé, alternados. Feito na Serraria, com 1 tábua. Tem escadas e lajes."),
          ("Parquet", "Quadrados de tábuas deitadas e em pé, alternados. Faz-se na Serração, de 1 tábua. Tem escadas e degraus."),
          ("Parquet", "Squares of lying and standing boards, alternating. Made in the Sawmill from 1 plank. Has stairs and slabs.")),
        P("herringbone", "sawing/oak_herringbone",
          ("Espinha de Peixe", "Réguas curtas em ziguezague, como nos salões antigos. Feita na Serraria, com 1 tábua. Tem escadas e lajes."),
          ("Espinha de Peixe", "Réguas curtas em ziguezague, como nos salões antigos. Faz-se na Serração, de 1 tábua. Tem escadas e degraus."),
          ("Herringbone", "Short boards in a zigzag, like in old halls. Made in the Sawmill from 1 plank. Has stairs and slabs.")),
        P("floorboards", "sawing/oak_floorboards",
          ("Assoalho", "Tábuas compridas com as juntas desencontradas, para pisos. Feito na Serraria, com 1 tábua. Tem escadas e lajes."),
          ("Soalho", "Tábuas compridas e desencontradas, para pisos. Faz-se na Serração, de 1 tábua. Tem escadas e degraus."),
          ("Floorboards", "Long boards with staggered joints, for floors. Made in the Sawmill from 1 plank. Has stairs and slabs.")),
        P("paneling", "sawing/oak_paneling",
          ("Lambri", "Tábuas em pé com sulcos, para forrar paredes. Feito na Serraria, com 1 tábua. Sem escadas nem lajes."),
          ("Lambri", "Tábuas em pé com sulcos, para forrar paredes. Faz-se na Serração, de 1 tábua. Sem escadas nem degraus."),
          ("Paneling", "Standing grooved boards, to line walls. Made in the Sawmill from 1 plank. No stairs or slabs.")),
        P("beam", "sawing/oak_beam",
          ("Viga", "Pilar de madeira serrada. Fica deitada para o lado em que se clica, como os troncos. Feita na Serraria, com 1 tábua."),
          ("Viga", "Pilar de madeira serrada. Deita-se para o lado em que se clica, como os troncos. Faz-se na Serração, de 1 tábua."),
          ("Beam", "A sawn wooden pillar. Lies along the side you click, like logs. Made in the Sawmill from 1 plank.")),
    ]
    g["GUIDE"].extend(pages)


# ------------------------------------------------------------------ Java

def gen_java():
    items = ", ".join(f'"{s}"' for s in FURNITURE)
    java = f"""package com.tcordeiro.moveis.finish;

import java.util.List;

/**
 * Os móveis de madeira que levam verniz (propriedade {{@link FinishState#FINISH}}): sufixos dos ids &lt;madeira&gt;_&lt;sufixo&gt;.
 * GERADO por tools/gen_finishes.py (lista FURNITURE): não editar à mão. Um móvel novo entra lá e a classe do bloco acrescenta
 * FinishState.FINISH ao blockstate (mais o desgaste: isRandomlyTicking/randomTick → {{@link Weathering}}).
 */
public final class FinishFurniture {{
	private FinishFurniture() {{}}

	public static final List<String> SUFFIXES = List.of({items});
}}
"""
    path = os.path.join(g["JAVA"], "finish", "FinishFurniture.java")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(java)


# ------------------------------------------------------------------ entrada

def run(globs):
    g.update(globs)
    gen_textures()
    gen_varnished_vanilla()
    gen_new_blocks()
    gen_furniture()
    gen_sawmill()
    gen_sawing()
    gen_items()
    gen_tags()
    gen_guide_pages()
    gen_java()
    n_models = sum(1 for r in g["written"] if r.startswith(f"assets/{MOD}/models/"))
    print(f"verniz: {len(BLOCKS)} blocos ({len(FINISH_BLOCKS)} com «finish»), {len(FURNITURE) * len(g['WOODS'])} móveis com «finish» "
          f"({PART_MODELS} modelos de partes móveis), {len(SAWING)} receitas da Serraria, "
          f"{TEXTURES} texturas, {n_models} modelos no total do mod")
