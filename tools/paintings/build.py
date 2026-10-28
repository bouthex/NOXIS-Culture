# Genera las texturas, modelos y blockstates de las pinturas, el atril y los documentos de Noxis Culture.
# Uso (desde la raíz del proyecto): python3 tools/paintings/build.py   (necesita Pillow)
import json, os, sys, shutil
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from PIL import Image
import materials as M
from arts import ART
from pal import rgb

A = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'noxis_culture')
T = os.path.join(A, 'textures')
MB = os.path.join(A, 'models', 'block')
N = len(ART)
NS = 'noxis_culture:'

def save_png(im, *path):
    p = os.path.join(T, *path); os.makedirs(os.path.dirname(p), exist_ok=True); im.save(p)

def save_json(obj, *path):
    p = os.path.join(A, *path); os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w') as f: json.dump(obj, f, indent=1)

def face(tex, uv=None, cull=None, rot=None):
    f = {'texture': tex}
    if uv is not None: f['uv'] = uv
    if cull: f['cullface'] = cull
    if rot: f['rotation'] = rot
    return f

def el(fr, to, faces, rot=None, name=None):
    e = {'from': fr, 'to': to, 'faces': faces}
    if rot: e['rotation'] = rot
    if name: e['name'] = name
    return e

# ------------------------------------------------------------------ texturas de las obras
FR = [(0x2a, 0x1c, 0x26), (0x46, 0x30, 0x3e), (0x6e, 0x4c, 0x5e)]
def art_texture(rows):
    im = Image.new('RGBA', (16, 16), FR[1] + (255,))
    for x in range(16):
        im.putpixel((x, 0), FR[2] + (255,)); im.putpixel((0, x), FR[2] + (255,))
        im.putpixel((x, 15), FR[0] + (255,)); im.putpixel((15, x), FR[0] + (255,))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r): im.putpixel((x + 1, y + 1), rgb(ch) + (255,))
    return im

def build_textures():
    d = os.path.join(T, 'block', 'noxis_painting')
    for f in os.listdir(d):
        if f.startswith('art_') or f == 'frame.png': os.remove(os.path.join(d, f))
    for i in range(N):
        save_png(art_texture(ART[i][2]), 'block', 'noxis_painting', 'art_%02d.png' % i)
    save_png(M.frame_wood(), 'block', 'noxis_painting', 'frame_wood.png')
    save_png(M.frame_gilt(), 'block', 'noxis_painting', 'frame_gilt.png')
    save_png(M.easel_wood(), 'block', 'noxis_easel', 'wood.png')
    save_png(M.canvas_linen(), 'block', 'noxis_easel', 'canvas_linen.png')
    save_png(M.canvas_back(), 'block', 'noxis_easel', 'canvas_back.png')
    save_png(M.canvas_sketch(), 'block', 'noxis_easel', 'canvas_sketch.png')
    save_png(M.canvas_unfinished(), 'block', 'noxis_easel', 'canvas_unfinished.png')
    save_png(M.painting_item(), 'item', 'noxis_painting.png')
    dd = os.path.join(T, 'block', 'noxis_document')
    for f in os.listdir(dd): os.remove(os.path.join(dd, f))
    save_png(M.book_cover(), 'block', 'noxis_document', 'book_cover.png')
    save_png(M.book_spine(), 'block', 'noxis_document', 'book_spine.png')
    save_png(M.book_pages(), 'block', 'noxis_document', 'pages.png')
    save_png(M.notebook_cover(), 'block', 'noxis_document', 'notebook_cover.png')
    save_png(M.notebook_ring(), 'block', 'noxis_document', 'notebook_ring.png')
    save_png(M.sheet_paper(), 'block', 'noxis_document', 'sheet.png')
    save_png(M.gui_document(), 'gui', 'noxis_document.png')

# ------------------------------------------------------------------ pintura en la pared
def wall_model(i):
    """Mira al norte (la pared queda al sur). Lienzo hundido 1 px detrás de un marco de 2 px de profundidad."""
    art = NS + 'block/noxis_painting/art_%02d' % i
    W = '#wood'; G = '#gilt'
    els = [el([1, 1, 15], [15, 15, 16], {'north': face('#art', [1, 1, 15, 15])}, name='lienzo')]
    # barras del marco (z 13.5..16): frente de madera, canto interior dorado, cantos exteriores de madera
    bars = [
        ([0, 15, 13.5], [16, 16, 16], 'down'),    # arriba
        ([0, 0, 13.5], [16, 1, 16], 'up'),        # abajo
        ([0, 1, 13.5], [1, 15, 16], 'east'),      # izquierda (vista desde el frente: x bajo)
        ([15, 1, 13.5], [16, 15, 16], 'west'),    # derecha
    ]
    for fr, to, inner in bars:
        horiz = (to[0] - fr[0]) > 1
        faces = {}
        for d in ['north', 'up', 'down', 'east', 'west']:
            if d == inner:
                faces[d] = face(G, [0, 0, 16 if horiz else 2.5, 2.5 if horiz else 14])
            elif d == 'north':
                faces[d] = face(W, [fr[0], 16 - to[1], to[0], 16 - fr[1]], rot=None)
            else:
                faces[d] = face(W, [0, 0, 2.5, 16] if d in ('east', 'west') else [0, 0, 16, 2.5])
        els.append(el(fr, to, faces, name='marco'))
    return {'parent': 'minecraft:block/block', 'ambientocclusion': True,
            'textures': {'particle': art, 'art': art, 'wood': NS + 'block/noxis_painting/frame_wood',
                         'gilt': NS + 'block/noxis_painting/frame_gilt'},
            'elements': els}

FACING_Y = {'north': 0, 'east': 90, 'south': 180, 'west': 270}

def build_wall():
    d = os.path.join(MB, 'noxis_painting')
    if os.path.isdir(d): shutil.rmtree(d)
    for i in range(N):
        save_json(wall_model(i), 'models', 'block', 'noxis_painting', 'wall_%02d.json' % i)
    variants = {}
    for i in range(N):
        for f, y in FACING_Y.items():
            v = {'model': NS + 'block/noxis_painting/wall_%02d' % i}
            if y: v['y'] = y
            variants['art=%d,facing=%s' % (i, f)] = v
    save_json({'variants': variants}, 'blockstates', 'noxis_painting.json')
    idir = os.path.join(A, 'models', 'item', 'noxis_painting')
    if os.path.isdir(idir): shutil.rmtree(idir)
    save_json({'parent': 'minecraft:item/generated', 'textures': {'layer0': NS + 'item/noxis_painting'}},
              'models', 'item', 'noxis_painting.json')
    save_json({'model': {'type': 'minecraft:model', 'model': NS + 'item/noxis_painting'}}, 'items', 'noxis_painting.json')

# ------------------------------------------------------------------ atril
LEAN = -12.0            # el bastidor delantero se apoya hacia atrás (arriba hacia +z)
PIVOT = [8, 0, 8]

def frame_rot():
    return {'angle': LEAN, 'axis': 'x', 'origin': PIVOT}

def wood_box(fr, to, rot=None, skip=()):
    faces = {}
    for d in ['north', 'south', 'east', 'west', 'up', 'down']:
        if d in skip: continue
        if d in ('up', 'down'): uv = [fr[0], fr[2], to[0], to[2]]
        elif d in ('north', 'south'): uv = [fr[0], 16 - to[1], to[0], 16 - fr[1]]
        else: uv = [fr[2], 16 - to[1], to[2], 16 - fr[1]]
        faces[d] = face('#wood', uv)
    return el(fr, to, faces, rot)

def easel_model():
    els = [
        wood_box([3, 0, 7.5], [4.5, 16.5, 9], frame_rot()),          # pata delantera izquierda
        wood_box([11.5, 0, 7.5], [13, 16.5, 9], frame_rot()),        # pata delantera derecha
        wood_box([2.5, 15, 7], [13.5, 16, 9.5], frame_rot()),        # travesaño de arriba (sujeta el lienzo)
        wood_box([2, 2.5, 5], [14, 3.5, 9.5], frame_rot()),          # bandeja donde apoya el lienzo
        wood_box([4.5, 7, 8], [11.5, 8, 9], frame_rot()),            # travesaño del medio
        wood_box([7.25, 0, 13.5], [8.75, 15.5, 15], {'angle': 23.0, 'axis': 'x', 'origin': [8, 0, 14.25]}),   # pata trasera
    ]
    return {'parent': 'minecraft:block/block', 'ambientocclusion': False,
            'textures': {'particle': NS + 'block/noxis_easel/wood', 'wood': NS + 'block/noxis_easel/wood'},
            'elements': els,
            'display': {
                'gui': {'rotation': [25, 160, 0], 'translation': [0, -1, 0], 'scale': [0.62, 0.62, 0.62]},
                'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.4, 0.4, 0.4]},
                'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [0.6, 0.6, 0.6]},
                'thirdperson_righthand': {'rotation': [75, 135, 0], 'translation': [0, 1.5, 1.5], 'scale': [0.375, 0.375, 0.375]},
                'thirdperson_lefthand': {'rotation': [75, 135, 0], 'translation': [0, 1.5, 1.5], 'scale': [0.375, 0.375, 0.375]},
                'firstperson_righthand': {'rotation': [0, 135, 0], 'translation': [0, 2, 0], 'scale': [0.4, 0.4, 0.4]},
                'firstperson_lefthand': {'rotation': [0, 135, 0], 'translation': [0, 2, 0], 'scale': [0.4, 0.4, 0.4]}}}

CANVAS = ([2.5, 3.5, 6.5], [13.5, 14.5, 7.5])   # 11x11 sobre la bandeja, delante de las patas

def canvas_model(front_tex, front_uv, extra_tex=None):
    fr, to = CANVAS
    faces = {'north': face('#front', front_uv), 'south': face('#back', [0, 0, 16, 16]),
             'up': face('#linen', [0, 0, 11, 1]), 'down': face('#linen', [0, 0, 11, 1]),
             'east': face('#linen', [0, 0, 1, 11]), 'west': face('#linen', [0, 0, 1, 11])}
    els = [el(fr, to, faces, frame_rot(), name='lienzo')]
    tex = {'particle': front_tex, 'front': front_tex, 'back': NS + 'block/noxis_easel/canvas_back',
           'linen': NS + 'block/noxis_easel/canvas_linen'}
    if extra_tex:
        tex['over'] = extra_tex
        els.append(el([fr[0], fr[1], fr[2] - 0.05], [to[0], to[1], fr[2] - 0.05],
                      {'north': face('#over', [1, 1, 15, 15])}, frame_rot(), name='capa'))
    return {'ambientocclusion': False, 'textures': tex, 'elements': els}

def build_easel():
    d = os.path.join(MB, 'noxis_easel')
    if os.path.isdir(d): shutil.rmtree(d)
    save_json(easel_model(), 'models', 'block', 'noxis_easel', 'easel.json')
    lin = NS + 'block/noxis_easel/canvas_linen'
    save_json(canvas_model(lin, [0, 0, 11, 11]), 'models', 'block', 'noxis_easel', 'canvas_blank.json')
    save_json(canvas_model(lin, [0, 0, 11, 11], NS + 'block/noxis_easel/canvas_sketch'), 'models', 'block', 'noxis_easel', 'canvas_sketch.json')
    for i in range(N):
        art = NS + 'block/noxis_painting/art_%02d' % i
        save_json({'parent': NS + 'block/noxis_easel/canvas_art', 'textures': {'particle': art, 'front': art}},
                  'models', 'block', 'noxis_easel', 'canvas_%02d.json' % i)
    save_json(canvas_model('#front', [1, 1, 15, 15]), 'models', 'block', 'noxis_easel', 'canvas_art.json')
    # capa "sin terminar" (etapa de color), sola: se aplica encima de la obra
    fr, to = CANVAS
    save_json({'ambientocclusion': False, 'textures': {'particle': lin, 'over': NS + 'block/noxis_easel/canvas_unfinished'},
               'elements': [el([fr[0], fr[1], fr[2] - 0.05], [to[0], to[1], fr[2] - 0.05],
                               {'north': face('#over', [1, 1, 15, 15])}, frame_rot())]},
              'models', 'block', 'noxis_easel', 'canvas_unfinished.json')
    parts = []
    for f, y in FACING_Y.items():
        def ap(model):
            a = {'model': NS + 'block/noxis_easel/' + model}
            if y: a['y'] = y
            return a
        parts.append({'when': {'facing': f}, 'apply': ap('easel')})
        parts.append({'when': {'facing': f, 'canvas': '1'}, 'apply': ap('canvas_blank')})
        parts.append({'when': {'facing': f, 'canvas': '2'}, 'apply': ap('canvas_sketch')})
        parts.append({'when': {'facing': f, 'canvas': '3'}, 'apply': ap('canvas_unfinished')})
        for i in range(N):
            parts.append({'when': {'facing': f, 'canvas': '3|4', 'art': str(i)}, 'apply': ap('canvas_%02d' % i)})
    save_json({'multipart': parts}, 'blockstates', 'noxis_easel.json')
    save_json({'model': {'type': 'minecraft:model', 'model': NS + 'block/noxis_easel/easel'}}, 'items', 'noxis_easel.json')

# ------------------------------------------------------------------ documentos
DOC_DISPLAY = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 2.5, 0], 'scale': [0.9, 0.9, 0.9]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'rotation': [-90, 0, 0], 'translation': [0, 0, -7], 'scale': [0.9, 0.9, 0.9]},
    'thirdperson_righthand': {'rotation': [0, 90, -60], 'translation': [0, 1, 1], 'scale': [0.55, 0.55, 0.55]},
    'thirdperson_lefthand': {'rotation': [0, 90, -60], 'translation': [0, 1, 1], 'scale': [0.55, 0.55, 0.55]},
    'firstperson_righthand': {'rotation': [-20, 80, -10], 'translation': [1, 3, 0], 'scale': [0.6, 0.6, 0.6]},
    'firstperson_lefthand': {'rotation': [-20, 80, -10], 'translation': [1, 3, 0], 'scale': [0.6, 0.6, 0.6]},
}

def book_model():
    """Libro cerrado, acostado. Frente al norte: el lomo a la izquierda (x bajo). 9 x 12 x 4 px."""
    x0, x1, z0, z1 = 3.5, 12.5, 2, 14
    C = '#cover'
    def cover(y0, y1, top):
        return el([x0, y0, z0], [x1, y1, z1], {
            'up' if top else 'down': face(C, [0, 0, 9, 12]),
            'down' if top else 'up': face(C, [0, 0, 9, 12]),
            'north': face(C, [0, 14, 9, 15]), 'south': face(C, [0, 14, 9, 15]),
            'east': face(C, [0, 14, 12, 15])}, name='tapa')
    pages = el([x0 + 1, 1, z0 + 0.5], [x1 - 0.5, 3, z1 - 0.5], {
        'north': face('#pages', [0, 0, 7.5, 2]), 'south': face('#pages', [0, 0, 7.5, 2]),
        'east': face('#pages', [0, 0, 11, 2])}, name='páginas')
    spine = el([x0 - 0.25, 0, z0], [x0 + 1, 4, z1], {
        'west': face('#spine', [2, 0, 14, 16], rot=90), 'up': face('#spine', [0, 0, 1.25, 12]),
        'down': face('#spine', [0, 0, 1.25, 12]), 'north': face('#spine', [0, 0, 1.25, 4]),
        'south': face('#spine', [0, 0, 1.25, 4]), 'east': face('#spine', [0, 0, 12, 4])}, name='lomo')
    return {'parent': 'minecraft:block/block', 'ambientocclusion': False,
            'textures': {'particle': NS + 'block/noxis_document/book_cover', 'cover': NS + 'block/noxis_document/book_cover',
                         'spine': NS + 'block/noxis_document/book_spine', 'pages': NS + 'block/noxis_document/pages'},
            'elements': [cover(0, 1, False), pages, cover(3, 4, True), spine], 'display': DOC_DISPLAY}

def notebook_model():
    """Cuaderno: tapas finas, una hoja gruesa de páginas, anillos en el lomo y banda elástica."""
    x0, x1, z0, z1 = 4, 12.5, 3, 13
    C = '#cover'
    def cover(y0, y1, top):
        return el([x0, y0, z0], [x1, y1, z1], {
            'up' if top else 'down': face(C, [0, 0, 8.5, 10]), 'down' if top else 'up': face(C, [0, 0, 8.5, 10]),
            'north': face(C, [0, 14, 8.5, 14.5]), 'south': face(C, [0, 14, 8.5, 14.5]),
            'east': face(C, [0, 14, 10, 14.5]), 'west': face(C, [0, 14, 10, 14.5])}, name='tapa')
    pages = el([x0 + 0.25, 0.5, z0 + 0.25], [x1 - 0.25, 1.5, z1 - 0.25], {
        'north': face('#pages', [0, 0, 8, 1]), 'south': face('#pages', [0, 0, 8, 1]),
        'east': face('#pages', [0, 0, 9.5, 1]), 'west': face('#pages', [0, 0, 9.5, 1])}, name='páginas')
    els = [cover(0, 0.5, False), pages, cover(1.5, 2, True)]
    for i, z in enumerate([4, 6.25, 8.5, 10.75]):
        els.append(el([x0 - 0.75, -0.01, z], [x0 + 0.75, 2.25, z + 1.25], {
            d: face('#ring', [0, 0, 1.5, 2.25]) for d in ['north', 'south', 'east', 'west', 'up', 'down']}, name='anillo'))
    return {'parent': 'minecraft:block/block', 'ambientocclusion': False,
            'textures': {'particle': NS + 'block/noxis_document/notebook_cover', 'cover': NS + 'block/noxis_document/notebook_cover',
                         'pages': NS + 'block/noxis_document/pages', 'ring': NS + 'block/noxis_document/notebook_ring'},
            'elements': els, 'display': DOC_DISPLAY}

SHEET_ANGLES = [0, 4.5, -3.5, 2.5, -5, 1.5, -2.5, 5]

def sheets_model(n):
    els = []
    for i in range(n):
        y0, y1 = i, i + 1
        a = SHEET_ANGLES[i]
        e = el([3, y0, 3], [13, y1, 13], {
            'up': face('#paper', [3, 3, 13, 13]), 'down': face('#paper', [3, 3, 13, 13]),
            'north': face('#paper', [0, 15, 10, 16]), 'south': face('#paper', [0, 15, 10, 16]),
            'east': face('#paper', [0, 15, 10, 16]), 'west': face('#paper', [0, 15, 10, 16])}, name='hoja')
        if a: e['rotation'] = {'angle': a, 'axis': 'y', 'origin': [8, y0, 8]}
        els.append(e)
    m = {'parent': 'minecraft:block/block', 'ambientocclusion': False,
         'textures': {'particle': NS + 'block/noxis_document/sheet', 'paper': NS + 'block/noxis_document/sheet'},
         'elements': els}
    if n == 1: m['display'] = DOC_DISPLAY
    return m

def build_documents():
    for f in ['noxis_book.json', 'noxis_notebook.json', 'noxis_sheet.json']:
        p = os.path.join(MB, f)
        if os.path.exists(p): os.remove(p)
    d = os.path.join(MB, 'noxis_document')
    if os.path.isdir(d): shutil.rmtree(d)
    save_json(book_model(), 'models', 'block', 'noxis_document', 'book.json')
    save_json(notebook_model(), 'models', 'block', 'noxis_document', 'notebook.json')
    for n in range(1, 9):
        save_json(sheets_model(n), 'models', 'block', 'noxis_document', 'sheets_%d.json' % n)
    variants = {}
    names = ['book', 'notebook']
    for f, y in FACING_Y.items():
        for kind in range(3):
            for layers in range(1, 9):
                if kind < 2 and layers > 1:
                    model = names[kind]
                elif kind < 2:
                    model = names[kind]
                else:
                    model = 'sheets_%d' % layers
                v = {'model': NS + 'block/noxis_document/' + model}
                if y: v['y'] = y
                variants['facing=%s,kind=%d,layers=%d' % (f, kind, layers)] = v
    save_json({'variants': variants}, 'blockstates', 'noxis_document.json')
    def m(model): return {'type': 'minecraft:model', 'model': NS + 'block/noxis_document/' + model}
    save_json({'model': {'type': 'minecraft:range_dispatch', 'property': 'minecraft:custom_model_data', 'index': 0,
                         'entries': [{'threshold': 0.0, 'model': m('book')}, {'threshold': 1.0, 'model': m('notebook')},
                                     {'threshold': 2.0, 'model': m('sheets_1')}],
                         'fallback': m('book')}}, 'items', 'noxis_document.json')

# ------------------------------------------------------------------ menas de Noxus
def build_ores():
    import ores
    for base, prefix, tex in [('stone', 'noxus_ore', 'minecraft:block/stone'), ('deepslate', 'deepslate_noxus_ore', 'minecraft:block/deepslate')]:
        for f in os.listdir(os.path.join(T, 'block')):
            if f.startswith(prefix + '_overlay'): os.remove(os.path.join(T, 'block', f))
        for f in os.listdir(MB):
            if f.startswith(prefix) and f.endswith('.json'): os.remove(os.path.join(MB, f))
        entries = []
        for i, v in enumerate(ores.VARIANTS):
            suffix = '' if i == 0 else '_%d' % (i + 1)
            save_png(ores.overlay(v, base), 'block', prefix + '_overlay' + suffix + '.png')
            save_json({'parent': NS + 'block/ore_overlay',
                       'textures': {'base': tex, 'overlay': NS + 'block/' + prefix + '_overlay' + suffix}},
                      'models', 'block', prefix + suffix + '.json')
            for y in (0, 90, 180, 270):
                e = {'model': NS + 'block/' + prefix + suffix}
                if y: e['y'] = y
                entries.append(e)
        save_json({'variants': {'': entries}}, 'blockstates', prefix + '.json')

def build_noxus_block():
    import noxus_block as NB
    save_png(NB.side(NB.CRACKS_A, NB.INCL_A), 'block', 'noxus_block.png')
    save_png(NB.side(NB.CRACKS_B, NB.INCL_B), 'block', 'noxus_block_2.png')
    save_png(NB.top(), 'block', 'noxus_block_top.png')
    entries = []
    for m in ('noxus_block', 'noxus_block_2'):
        for y in (0, 90, 180, 270):
            e = {'model': NS + 'block/' + m}
            if y: e['y'] = y
            entries.append(e)
    save_json({'variants': {'': entries}}, 'blockstates', 'noxus_block.json')

if __name__ == '__main__':
    build_textures(); build_wall(); build_easel(); build_documents(); build_ores(); build_noxus_block()
    print('ok:', N, 'obras')
