# Prévia (Blender SEM janela) dos modelos REAIS do mod: blockstate → modelo → pais → texturas do mod e do jar 26.3.
# Uso: /Applications/Blender.app/Contents/MacOS/Blender --background --python tools/madeira/render_preview.py -- <saída.png>
import bpy, bmesh, json, math, os, sys, zipfile
from mathutils import Vector
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources", "assets", "tmoveis")
JAR = os.path.expanduser("~/Library/Application Support/horizon-launcher/minecraft/versions/26.3/26.3.jar")
OUT = sys.argv[sys.argv.index("--") + 1]
import tempfile; TMP = os.path.join(tempfile.gettempdir(), "tmoveis_preview_tex"); os.makedirs(TMP, exist_ok=True)
z = zipfile.ZipFile(JAR); cache = {}
def split(ref):
    return ref.split(":", 1) if ":" in ref else ("minecraft", ref)
def load_model(ref):
    ns, p = split(ref)
    if ns == "tmoveis": return json.load(open(os.path.join(ROOT, "models", p + ".json")))
    return json.load(z.open(f"assets/minecraft/models/{p}.json"))
def img(ref):
    if ref in cache: return cache[ref]
    ns, p = split(ref)
    if ns == "tmoveis": path = os.path.join(ROOT, "textures", p + ".png")
    else:
        path = os.path.join(TMP, p.replace("/", "_") + ".png"); open(path, "wb").write(z.read(f"assets/minecraft/textures/{p}.png"))
    im = bpy.data.images.load(path); cache[ref] = im; return im
def resolve(ref):
    chain = [load_model(ref)]
    while "parent" in chain[-1] and not chain[-1]["parent"].startswith("builtin") and chain[-1]["parent"] not in ("minecraft:block/block", "block/block"):
        chain.append(load_model(chain[-1]["parent"]))
    tex = {}
    for m in reversed(chain): tex.update(m.get("textures", {}))
    els = next((m["elements"] for m in chain if "elements" in m), [])
    def get(v):
        while v.startswith("#"): v = tex[v[1:]]
        return v
    return els, get
mats = {}
def mat(tex):
    if tex in mats: return mats[tex]
    m = bpy.data.materials.new(tex); m.use_nodes = True; nt = m.node_tree
    b = next(n for n in nt.nodes if n.type == "BSDF_PRINCIPLED"); ti = nt.nodes.new("ShaderNodeTexImage")
    ti.image = img(tex); ti.interpolation = "Closest"; nt.links.new(ti.outputs["Color"], b.inputs["Base Color"])
    b.inputs["Roughness"].default_value = 0.35 if "polished" in tex else 0.8
    mats[tex] = m; return m
FACES = {"north": [(1,0,0),(0,0,0),(0,1,0),(1,1,0)], "south": [(0,0,1),(1,0,1),(1,1,1),(0,1,1)], "west": [(0,0,0),(0,0,1),(0,1,1),(0,1,0)],
         "east": [(1,0,1),(1,0,0),(1,1,0),(1,1,1)], "up": [(0,1,1),(1,1,1),(1,1,0),(0,1,0)], "down": [(0,0,0),(1,0,0),(1,0,1),(0,0,1)]}
def place(model, bx, bz, roty=0, by=0):
    els, get = resolve(model)
    me = bpy.data.meshes.new(model); bm = bmesh.new(); uv = bm.loops.layers.uv.new(); ml = []
    for e in els:
        (x1, y1, z1), (x2, y2, z2) = e["from"], e["to"]
        c = {(i, j, k): bm.verts.new(((x1, x2)[i] / 16 - .5, -((z1, z2)[k] / 16 - .5), (y1, y2)[j] / 16)) for i in (0, 1) for j in (0, 1) for k in (0, 1)}
        X, Y, Z = (x1, x2), (y1, y2), (z1, z2)
        for fn, face in e["faces"].items():
            tx = get(face["texture"])
            if tx not in ml: ml.append(tx)
            cs = FACES[fn]; f = bm.faces.new([c[q] for q in cs]); f.material_index = ml.index(tx)
            for lp, (i, j, k) in zip(f.loops, cs):
                u, v = (X[i], Y[j]) if fn in ("north", "south") else ((Z[k], Y[j]) if fn in ("east", "west") else (X[i], 16 - Z[k]))
                lp[uv].uv = (u / 16, v / 16)
    bm.to_mesh(me); bm.free(); ob = bpy.data.objects.new(model, me); bpy.context.scene.collection.objects.link(ob)
    for t in ml: me.materials.append(mat(t))
    ob.location = (bx + .5, -(bz + .5), by); ob.rotation_euler = (0, 0, math.radians(-roty)); return ob
bpy.ops.wm.read_factory_settings(use_empty=True); scn = bpy.context.scene
TONES = ["natural", "honey", "walnut", "mahogany", "ebony", "white_lacquer", "black_lacquer"]
# linha 1: tábuas de carvalho: crua (do jogo) + 7 tons + mel polido
place("minecraft:block/oak_planks", -1.4, 0)  # crua (do jogo)
for i, t in enumerate(TONES): place(f"tmoveis:block/finish/oak/planks_{t}", i * 1.2, 0)
place("tmoveis:block/finish/oak/planks_honey_polished", 7 * 1.2, 0)
place("tmoveis:block/finish/oak/planks_walnut_polished", 8 * 1.2, 0)
# linha 2: blocos novos crus (carvalho) + escadas/laje + desgaste
for i, m in enumerate(["parquet", "herringbone", "floorboards", "paneling", "beam", "parquet_stairs", "herringbone_slab",
                       "floorboards_worn_1", "floorboards_worn_2", "floorboards_worn_3"]):
    place(f"tmoveis:block/finish/oak/{m}", i * 1.2, 2.3, roty=180 if "stairs" in m else 0)
# linha 3: móveis com acabamento + Serraria
place("tmoveis:block/oak_chair", 0, 4.6, 180); place("tmoveis:block/finish/oak/chair_honey", 1.2, 4.6, 180)
place("tmoveis:block/finish/oak/chair_walnut_polished", 2.4, 4.6, 180)
place("tmoveis:block/finish/spruce/table_top_mahogany", 3.6, 4.6)
for r in (0, 90, 180, 270): place("tmoveis:block/finish/spruce/table_leg_mahogany", 3.6, 4.6, r)
place("tmoveis:block/finish/birch/cabinet_white_lacquer", 4.8, 4.6, 180); place("tmoveis:block/finish/cherry/drawer_black_lacquer", 6.0, 4.6, 180)
place("tmoveis:block/finish/oak/stool_ebony", 7.2, 4.6); place("tmoveis:block/finish/oak/chair_worn_3", 8.4, 4.6, 180)
place("tmoveis:block/sawmill", 9.8, 4.6, 180); place("tmoveis:block/sawmill_blade", 9.8, 4.6, 180)
# linha 4: outras madeiras
for i, (w, m) in enumerate([("spruce", "planks_honey"), ("cherry", "planks_ebony"), ("crimson", "herringbone_mahogany"), ("warped", "parquet_natural"),
                            ("dark_oak", "herringbone"), ("birch", "parquet_white_lacquer_polished"), ("bamboo", "floorboards_honey"),
                            ("mangrove", "beam_walnut"), ("pale_oak", "paneling_black_lacquer"), ("jungle", "floorboards_worn_2")]):
    place(f"tmoveis:block/finish/{w}/{m}", i * 1.2, 6.9)
bpy.ops.mesh.primitive_plane_add(size=80, location=(5, -3, 0))
pl = bpy.context.object; pm = bpy.data.materials.new("chao"); pm.use_nodes = True
next(n for n in pm.node_tree.nodes if n.type == "BSDF_PRINCIPLED").inputs["Base Color"].default_value = (0.55, 0.57, 0.6, 1); pl.data.materials.append(pm)
sun = bpy.data.objects.new("s", bpy.data.lights.new("s", "SUN")); scn.collection.objects.link(sun); sun.data.energy = 3.2
sun.rotation_euler = (math.radians(50), 0, math.radians(200))
w = bpy.data.worlds.new("w"); scn.world = w; w.use_nodes = True; w.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.9
cam = bpy.data.objects.new("c", bpy.data.cameras.new("c")); scn.collection.objects.link(cam); scn.camera = cam
cam.data.type = "ORTHO"; cam.data.ortho_scale = 15.5
ctr = Vector((5.6, -4.0, 0.4)); a, e = math.radians(18), math.radians(35)
cam.location = ctr + Vector((-30 * math.sin(a) * math.cos(e), -30 * math.cos(a) * math.cos(e), 30 * math.sin(e)))
cam.rotation_euler = (ctr - cam.location).to_track_quat("-Z", "Y").to_euler()
eng = [i.identifier for i in bpy.types.RenderSettings.bl_rna.properties["engine"].enum_items]
try:
    scn.render.engine = "BLENDER_EEVEE_NEXT" if "BLENDER_EEVEE_NEXT" in eng else "BLENDER_EEVEE"
except TypeError:
    pass
scn.render.resolution_x, scn.render.resolution_y = 1600, 1000; scn.render.filepath = OUT; bpy.ops.render.render(write_still=True)
