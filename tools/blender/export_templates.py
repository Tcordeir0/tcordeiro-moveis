"""Escreve os templates de modelo dos Móveis a partir das peças desenhadas para o Blender (tools/blender/pecas.py).

Uso: python3 tools/blender/export_templates.py   (a partir da raiz do repo)
Fonte única = pecas.py (cubos). O Blender (blender_moveis.py) usa as mesmas peças para as prévias.
Só toca nos templates listados em MAP; mantém o bloco "display" que já exista (ex.: cama).
"""
import json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
from pecas import PIECES, BED

OUT = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "tmoveis", "models", "block", "template")
# variáveis do Blender -> variáveis do mod (o que o filho não define vem de DEFAULTS no próprio template)
RENAME = {"#sheet": "#pillow"}
DEFAULTS = {"#metal": "minecraft:block/iron_block", "#pillow": "minecraft:block/white_wool", "#shade": "minecraft:block/white_wool",
            "#log": None}  # #log: os filhos já definem (stripped log)

def bed_quarter(x0, head):
    """Cama do mod: cabeceira a SUL (como a vanilla). pecas.BED tem a cabeceira a norte → espelha z (z' = 32 - z)."""
    out = []
    for (x1, y1, z1, x2, y2, z2, v) in BED:
        z1, z2 = 32 - z2, 32 - z1
        oz = 16 if head else 0
        a, b = max(x1, x0), min(x2, x0 + 16); c, d = max(z1, oz), min(z2, oz + 16)
        if a < b and c < d: out.append((a - x0, y1, c - oz, b - x0, y2, d - oz, v))
    return out

MAP = {n: PIECES[n] for n in ["chair", "stool", "table_top", "table_leg", "cabinet", "cabinet_open", "drawer", "drawer_open",
                              "sofa_single", "sofa_left", "sofa_right", "sofa_middle", "sofa_corner_inner", "sofa_corner_outer",
                              "chandelier", "wall_lamp", "table_lamp"]}
# lado «right» do mod = poste exterior em x = 0 → metade x 0..16 do desenho; «left» = metade 16..32
MAP["double_bed_head_right"] = bed_quarter(0, True); MAP["double_bed_head_left"] = bed_quarter(16, True)
MAP["double_bed_foot_right"] = bed_quarter(0, False); MAP["double_bed_foot_left"] = bed_quarter(16, False)
# o banco do mod não tem variável de lã: a almofada passa a madeira descascada
MAP["stool"] = [b[:6] + ("#log",) if b[6] == "#wool" else b for b in MAP["stool"]]
# a cadeira: pernas e pés do encosto em madeira descascada, como o provisório
MAP["chair"] = [b[:6] + ("#log",) if (b[4] - b[1] >= 8 and b[3] - b[0] <= 2) else b for b in MAP["chair"]]

def r(v): return round(v, 3) if v != int(v) else int(v)

for name, boxes in MAP.items():
    path = os.path.join(OUT, name + ".json")
    old = json.load(open(path)) if os.path.exists(path) else {}
    els, used = [], set()
    for (x1, y1, z1, x2, y2, z2, v) in boxes:
        v = RENAME.get(v, v); used.add(v)
        els.append({"from": [r(x1), r(y1), r(z1)], "to": [r(x2), r(y2), r(z2)],
                    "faces": {f: {"texture": v} for f in ("north", "south", "east", "west", "up", "down")}})
    tex = dict(old.get("textures", {}))
    for v in used:
        if DEFAULTS.get(v) and v[1:] not in tex: tex[v[1:]] = DEFAULTS[v]
    model = {}
    if "display" in old: model["display"] = old["display"]
    else: model["parent"] = "block/block"
    model["textures"] = tex; model["elements"] = els
    json.dump(model, open(path, "w"), indent=1)
    print(f"{name}: {len(els)} cubos, variáveis {sorted(used)}")
