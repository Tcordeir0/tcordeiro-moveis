# blender --background --python blender_moveis.py -- <pasta>
# Monta as peças de pecas.py em cubos, exporta cada uma como modelo JSON do Minecraft e renderiza prévias.
import json, math, os, sys
import bpy, bmesh

BASE = sys.argv[sys.argv.index("--") + 1]
sys.path.insert(0, BASE)
from pecas import PIECES, PREVIEW_TEX

OUT = os.path.join(BASE, "out"); REN = os.path.join(BASE, "render"); TEX = os.path.join(BASE, "tex")
os.makedirs(OUT, exist_ok=True); os.makedirs(REN, exist_ok=True)

# ---------- 1) exportar JSON (fonte única: os cubos) ----------
def r(v):
    return round(v, 3) if v != int(v) else int(v)

for name, boxes in PIECES.items():
    vars_ = sorted({b[6] for b in boxes})
    elements = []
    for (x1, y1, z1, x2, y2, z2, v) in boxes:
        assert -16 <= min(x1, y1, z1) and max(x2, y2, z2) <= 32 and x1 < x2 and y1 < y2 and z1 < z2, (name, x1, y1, z1)
        elements.append({"from": [r(x1), r(y1), r(z1)], "to": [r(x2), r(y2), r(z2)],
                         "faces": {f: {"texture": v} for f in ("north", "south", "east", "west", "up", "down")}})
    model = {"parent": "block/block", "textures": {"particle": "#planks" if "#planks" in vars_ else vars_[0]},
             "elements": elements}
    with open(os.path.join(OUT, name + ".json"), "w") as fh:
        json.dump(model, fh, indent=1)
print("JSON:", len(PIECES))

# ---------- 2) cena ----------
bpy.ops.wm.read_factory_settings(use_empty=True)
scn = bpy.context.scene

mats = {}
def material(var):
    if var in mats: return mats[var]
    m = bpy.data.materials.new(var.strip("#")); m.use_nodes = True
    nt = m.node_tree
    bsdf = next(n for n in nt.nodes if n.type == "BSDF_PRINCIPLED")
    img = bpy.data.images.load(os.path.join(TEX, PREVIEW_TEX[var] + ".png"))
    tex = nt.nodes.new("ShaderNodeTexImage"); tex.image = img; tex.interpolation = "Closest"
    nt.links.new(tex.outputs["Color"], bsdf.inputs["Base Color"])
    bsdf.inputs["Roughness"].default_value = 0.8
    if var == "#glass":
        nt.links.new(tex.outputs["Alpha"], bsdf.inputs["Alpha"])
        try: m.surface_render_method = "DITHERED"
        except Exception: pass
    if var == "#light":
        bsdf.inputs["Emission Strength"].default_value = 2.0
        nt.links.new(tex.outputs["Color"], bsdf.inputs["Emission Color"])
    mats[var] = m
    return m

def build(name, boxes, ox, oz):
    me = bpy.data.meshes.new(name); bm = bmesh.new()
    uv = bm.loops.layers.uv.new()
    vars_ = []
    for (x1, y1, z1, x2, y2, z2, v) in boxes:
        if v not in vars_: vars_.append(v)
        mi = vars_.index(v)
        # Minecraft: x leste, y cima, z sul  ->  Blender: x, -z, y
        P = lambda x, y, z: bm.verts.new(((x) / 16, -(z) / 16, y / 16))
        c = {(i, j, k): P((x1, x2)[i], (y1, y2)[j], (z1, z2)[k]) for i in (0, 1) for j in (0, 1) for k in (0, 1)}
        faces = {  # (cantos, eixos para o UV automático do Minecraft)
            "north": [(1, 0, 0), (0, 0, 0), (0, 1, 0), (1, 1, 0)], "south": [(0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)],
            "west": [(0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0)], "east": [(1, 0, 1), (1, 0, 0), (1, 1, 0), (1, 1, 1)],
            "up": [(0, 1, 1), (1, 1, 1), (1, 1, 0), (0, 1, 0)], "down": [(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)]}
        X, Y, Z = (x1, x2), (y1, y2), (z1, z2)
        for fname, corners in faces.items():
            f = bm.faces.new([c[k] for k in corners]); f.material_index = mi
            for loop, (i, j, k) in zip(f.loops, corners):
                if fname in ("north", "south"): u, vv = X[i], Y[j]
                elif fname in ("east", "west"): u, vv = Z[k], Y[j]
                else: u, vv = X[i], Z[k]
                loop[uv].uv = (u / 16, vv / 16)
    bm.to_mesh(me); bm.free()
    ob = bpy.data.objects.new(name, me); scn.collection.objects.link(ob)
    for v in vars_: me.materials.append(material(v))
    ob.location = (ox, oy_from(oz), 0)
    return ob

oy_from = lambda oz: -oz

# disposição: peças lado a lado; a cama inteira com os 4 quartos juntos
layout = [["chair", "stool", "table_single", "cabinet", "drawer"],
          ["sofa_left", "sofa_middle", "sofa_right", "sofa_corner", "sofa_single"],
          ["chandelier", "wall_lamp", "table_lamp"]]
objs = {}
SOFA_X = {"sofa_left": 0, "sofa_middle": 1, "sofa_right": 2, "sofa_corner": 3.7, "sofa_single": 5.4}
for row, names in enumerate(layout):
    for col, n in enumerate(names):
        x = SOFA_X[n] if n in SOFA_X else col * 1.5
        objs[n] = build(n, PIECES[n], x, row * 2.2)
bx, bz = 7.8, 2.2
for n, (dx, dz) in {"double_bed_head_left": (0, 0), "double_bed_head_right": (1, 0),
                    "double_bed_foot_left": (0, 1), "double_bed_foot_right": (1, 1)}.items():
    objs[n] = build(n, PIECES[n], bx + dx, bz + dz)

# chão, luz, câmara
bpy.ops.mesh.primitive_plane_add(size=80, location=(5, -10, 0))
floor = bpy.context.object; fm = bpy.data.materials.new("chao"); fm.use_nodes = True
next(n for n in fm.node_tree.nodes if n.type == "BSDF_PRINCIPLED").inputs["Base Color"].default_value = (0.55, 0.55, 0.58, 1)
floor.data.materials.append(fm)
sun = bpy.data.objects.new("sol", bpy.data.lights.new("sol", "SUN")); scn.collection.objects.link(sun)
sun.data.energy = 3.5; sun.rotation_euler = (math.radians(50), 0, math.radians(200))
world = bpy.data.worlds.new("w"); scn.world = world; world.use_nodes = True
world.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.9

cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam")); scn.collection.objects.link(cam); scn.camera = cam
cam.data.type = "ORTHO"

engines = [i.identifier for i in bpy.types.RenderSettings.bl_rna.properties["engine"].enum_items]
scn.render.engine = "BLENDER_EEVEE_NEXT" if "BLENDER_EEVEE_NEXT" in engines else ("BLENDER_EEVEE" if "BLENDER_EEVEE" in engines else engines[0])
scn.render.film_transparent = False
scn.render.image_settings.file_format = "PNG"

def shoot(fname, center, scale, w=1400, h=900, az=150, el=30):
    scn.render.resolution_x, scn.render.resolution_y = w, h
    cam.data.ortho_scale = scale
    d = 30; a, e = math.radians(az), math.radians(el)
    cx, cy, cz = center
    cam.location = (cx + d * math.sin(a) * math.cos(e) * -1, cy - d * math.cos(a) * math.cos(e), cz + d * math.sin(e))
    dirv = (cx - cam.location[0], cy - cam.location[1], cz - cam.location[2])
    from mathutils import Vector
    cam.rotation_euler = Vector(dirv).to_track_quat("-Z", "Y").to_euler()
    scn.render.filepath = os.path.join(REN, fname); bpy.ops.render.render(write_still=True)

shoot("00_todos.png", (4.8, -2.8, 0.6), 11.5)
for n in ["chair", "stool", "table_single", "cabinet", "drawer", "chandelier", "wall_lamp", "table_lamp"]:
    o = objs[n]; shoot(n + ".png", (o.location.x + 0.5, o.location.y - 0.5, 0.6), 2.3, 700, 700)
shoot("sofas.png", (2.5, -2.7, 0.5), 6.0, 1100, 650)
shoot("double_bed.png", (bx + 1, -(bz + 1), 0.5), 3.4, 800, 700)
# ---------- 3) casa: cozinha, quarto, banheiro, sala (mais atrás na cena) ----------
Z0 = 7.0
casa = [  # (peça, x, z, altura)
    ("counter", 0, 0, 0), ("sink", 1, 0, 0), ("sink_water", 1, 0, 0), ("counter", 2, 0, 0), ("counter_corner", 3, 0, 0),
    ("fireplace", 5, 0, 0), ("trash_can", 6.5, 0, 0), ("mailbox", 7.8, 0, 0),
    ("nightstand", 0, 2.2, 0), ("wardrobe_bottom", 1.5, 2.2, 0), ("wardrobe_top", 1.5, 2.2, 1),
    ("curtain_closed", 3, 2.2, 0), ("curtain_open", 4.5, 2.2, 0), ("mirror", 6, 2.2, 0), ("wall_clock", 7.3, 2.2, 0.6),
    ("toilet", 0, 4.4, 0), ("bathtub_head", 1.5, 4.4, 0), ("bathtub_foot", 1.5, 5.4, 0),
    ("bathtub_water_head", 1.5, 4.4, 0), ("bathtub_water_foot", 1.5, 5.4, 0), ("ceiling_fan", 3.3, 4.4, 1.2),
]
for i, (n, x, z, h) in enumerate(casa):
    o = build(f"{n}.{i}", PIECES[n], x, Z0 + z); o.location.z = h; objs[n] = o
shoot("01_casa.png", (4.0, -(Z0 + 3.0), 0.7), 10.5)
for n, (cx, cz, s) in {"cozinha": (2.5, 0.5, 5.5), "quarto": (4.0, 2.7, 6.5), "banheiro": (2.2, 5.0, 4.8)}.items():
    shoot(f"02_{n}.png", (cx, -(Z0 + cz), 0.7), s, 1000, 700)

# ---------- 4) rodada 3 ----------
Z1 = 14.5
r3 = [("desk", 0, 0, 0), ("chair", 0, 0.95, 0), ("coffee_table", 1.6, 0, 0), ("side_table", 3.0, 0, 0), ("garden_bench", 4.4, 0, 0),
      ("planter_box", 5.9, 0, 0), ("shutters_closed", 7.3, 0, 0), ("shutters_open", 8.7, 0, 0),
      ("wine_rack", 0, 2.3, 0), ("floor_lamp_bottom", 1.5, 2.3, 0), ("floor_lamp_top", 1.5, 2.3, 1),
      ("bunk_bed_bottom_head", 3, 2.3, 0), ("bunk_bed_bottom_foot", 3, 3.3, 0), ("bunk_bed_top_head", 3, 2.3, 1), ("bunk_bed_top_foot", 3, 3.3, 1),
      ("piano_left", 4.6, 2.3, 0), ("piano_right", 5.6, 2.3, 0), ("safe", 7.2, 2.3, 0), ("awning", 8.6, 2.3, 0.8)]
for i, (n, x, z, h) in enumerate(r3):
    o = build(f"{n}.r3.{i}", PIECES[n], x, Z1 + z); o.location.z = h
r3objs = {o.name for o in scn.objects if ".r3." in o.name}
for o in scn.objects:
    if o.type == "MESH" and o.name != "Plane" and o.name not in r3objs: o.hide_render = True
shoot("03_mais_moveis.png", (4.5, -(Z1 + 1.8), 0.8), 11.0)
for o in scn.objects: o.hide_render = False
bpy.ops.wm.save_as_mainfile(filepath=os.path.join(BASE, "moveis.blend"))
print("OK")
