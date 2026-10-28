# Texturas de materiales de Noxis Culture (marco, atril, lienzo, ícono de pintura, documentos).
# Pixel art dibujado con reglas simples y paletas cortas; se revisa a escala y dentro del juego.
from PIL import Image
import random

def hexrgb(h, a=255):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

def img(w=16, h=16):
    return Image.new('RGBA', (w, h), (0, 0, 0, 0))

def grid(rows, pal):
    """rows: lista de strings; pal: char -> hex ('.' transparente)."""
    h = len(rows); w = max(len(r) for r in rows)
    im = img(w, h)
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in pal: im.putpixel((x, y), hexrgb(pal[ch]))
    return im

def wood(tones, seed, knot=True, vertical=False):
    """Madera con vetas horizontales: tones = [oscuro, base, medio, claro]."""
    rnd = random.Random(seed)
    im = img()
    rows = []
    for y in range(16):
        base = 1 if y % 4 in (0, 1) else 2
        if y % 8 == 3: base = 0 if rnd.random() < 0.5 else 1
        row = [base] * 16
        x = rnd.randrange(16)
        for _ in range(2):
            L = rnd.randint(3, 7); t = rnd.choice([0, 2, 3]) if base != 0 else 1
            for i in range(L): row[(x + i) % 16] = t
            x = (x + L + rnd.randint(2, 6)) % 16
        rows.append(row)
    if knot:
        kx, ky = rnd.randrange(3, 13), rnd.randrange(3, 13)
        for dx, dy, t in [(0, 0, 0), (1, 0, 0), (-1, 0, 1), (2, 0, 1), (0, -1, 2), (1, -1, 2), (0, 1, 1), (1, 1, 1)]:
            rows[(ky + dy) % 16][(kx + dx) % 16] = t
    for y in range(16):
        for x in range(16):
            im.putpixel((y, x) if vertical else (x, y), hexrgb(tones[rows[y][x]]))
    return im

FRAME_TONES = ['2a1a26', '3d2735', '523447', '6a4660']       # nogal oscuro con tinte violeta (marco)
EASEL_TONES = ['3e2c25', '5a4032', '76563f', '93714f']       # madera tibia (atril)

def frame_wood():
    return wood(FRAME_TONES, 11)

def frame_gilt():
    """Filete dorado del borde interior del marco."""
    im = img()
    for y in range(16):
        for x in range(16):
            t = ['8a5a14', 'c08a2a', 'e0b04a', 'f4d878'][[1, 2, 3, 2][y % 4] if x % 7 else 0]
            im.putpixel((x, y), hexrgb(t))
    return im

def easel_wood():
    return wood(EASEL_TONES, 5, knot=False, vertical=True)

LINEN = ['e8dfca', 'ddd2b8', 'cfc2a4', 'f2ecdc']

def canvas_linen():
    im = img()
    for y in range(16):
        for x in range(16):
            t = 0
            if (x + y) % 4 == 0: t = 1
            if (x * 3 + y * 5) % 11 == 0: t = 3
            if (x * 7 + y) % 13 == 0: t = 2
            im.putpixel((x, y), hexrgb(LINEN[t]))
    return im

def canvas_back():
    im = canvas_linen()
    dark = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            r, g, b, a = im.getpixel((x, y))
            im.putpixel((x, y), (int(r * 0.86), int(g * 0.84), int(b * 0.8), 255))
    w = easel_wood()
    for i in range(16):
        for d in (0, 1):
            im.putpixel((i, d), w.getpixel((i, d)))
            im.putpixel((i, 15 - d), w.getpixel((i, 15 - d)))
            im.putpixel((d, i), w.getpixel((d, i)))
            im.putpixel((15 - d, i), w.getpixel((15 - d, i)))
            im.putpixel((i, 7 + d), w.getpixel((i, 7 + d)))
    return im

def canvas_sketch():
    """Boceto a carbonilla (transparente donde no hay trazo): horizonte, una forma y unas líneas guía."""
    rows = [
        '................',
        '................',
        '..........aa....',
        '.........a..a...',
        '.........a..a...',
        '..........aa....',
        '................',
        '..bb......b.....',
        '....bbb..b.bb...',
        'aaa....bb....bba',
        '...........b....',
        '..b......b......',
        '.....bb.....b...',
        '................',
        '................',
        '................']
    return grid(rows, {'a': '4a4148', 'b': '6e6670'})

def canvas_unfinished():
    """Lo que falta pintar en la etapa de color: zonas de lienzo con bordes de pincelada."""
    rows = [
        '................',
        '.......LLLLLLLL.',
        '........LLLLLLL.',
        '..........LLLLL.',
        '...........LLLL.',
        '............LLL.',
        '..............L.',
        '................',
        '................',
        '................',
        '.LL.............',
        '.LLLL...........',
        '.LLLLLL......LL.',
        '.LLLLLLLL..LLLL.',
        '.LLLLLLLLLLLLLL.',
        '................']
    im = grid(rows, {'L': LINEN[0]})
    lin = canvas_linen()
    for y in range(16):
        for x in range(16):
            if im.getpixel((x, y))[3]: im.putpixel((x, y), lin.getpixel((x, y)))
    return im

def painting_item():
    """Ícono de la pintura: un cuadro envuelto en tela y atado con una cinta violeta (la obra no se ve)."""
    rows = [
        '................',
        '.FFFFFFFFRrFFFF.',
        '.FgggggggRrgggF.',
        '.FgCCCCCcRrkCfF.',
        '.FgCCCcckRrCkfF.',
        '.FgCCcckCRrCCfF.',
        '.RRRRRRRhRhRRRR.',
        '.rrrrrrrhRhrrrr.',
        '.FgCkcCCrRrCCfF.',
        '.FgckCCCCRrCcfF.',
        '.FgkCCCcCRrckfF.',
        '.FgCCCckCRrCkfF.',
        '.FfffffffRrfffF.',
        '.FFFFFFFFRrFFFF.',
        '.........r......',
        '................']
    return grid(rows, {'F': '2a1a26', 'f': '3d2735', 'g': '6a4660', 'C': 'efe6d2', 'c': 'd9cdb1',
                       'k': 'b9aa8c', 'R': '8a4fd0', 'r': '5a2f9a', 'h': 'c79bf0'})


# ------------------------------------------------------------------ documentos
LEATHER = {'o': '2a1545', 'd': '3a1f62', 'b': '4b2a7a', 'k': '55308a', 'l': '6a3fa0', 's': '2e184f',
           'g': 'e0a83a', 'G': 'a8761c', '5': 'd8b4ff', '4': 'a774e0', '3': '7444b0', '2': '4b2a7a'}

def book_cover():
    """Tapa del libro (zona 9x12 arriba a la izquierda) y canto de la tapa (fila 14)."""
    rows = [
        'gGddddddGg......',
        'Gdbbbbbbbd......',
        'dbllllllbsd.....',
        'dblbbkbbls......',
        'dbkbb5bbls......',
        'dblb545bls......',
        'dblb443bls......',
        'dbkbb3bbls......',
        'dblbbbkbls......',
        'dblbkbbbls......',
        'dbbssssssd......',
        'Gdbbbbbbdd......',
        'gGddddddGg......',
        '................',
        'oddddddddddddo..',
        '................']
    rows = [r[:16].ljust(16, '.') for r in rows]
    # La fila 2 tiene un caracter de más a propósito: se recorta a 16.
    return grid(rows, LEATHER)

def book_spine():
    """Lomo del libro: cuero oscuro con dos nervios dorados (se usa en franjas)."""
    rows = []
    for y in range(16):
        if y in (3, 4, 11, 12): rows.append('G' + 'g' * 14 + 'G' if y in (3, 11) else 'G' * 16)
        elif y in (0, 15): rows.append('o' * 16)
        else: rows.append(('db' * 8) if y % 3 else ('bd' * 8))
    return grid(rows, LEATHER)

def book_pages():
    """Canto de las páginas: líneas finas de papel."""
    im = img()
    for y in range(16):
        for x in range(16):
            t = 'efe8d4' if y % 2 == 0 else 'd8cfb8'
            if x in (0, 15): t = 'c9bf9f'
            im.putpixel((x, y), hexrgb(t))
    return im

def notebook_cover():
    """Tapa del cuaderno: negro violáceo, con banda elástica amarilla y canto."""
    rows = []
    for y in range(16):
        r = ''
        for x in range(16):
            if x >= 9: r += '.'
            elif x == 7: r += 'g'
            elif x == 6: r += 'G'
            elif y in (0, 9) or x == 0 or x == 8: r += 'o'
            else: r += 'L' if (x + y) % 5 else 'K'
        rows.append(r)
    rows[14] = 'oKKKKKKKKKKKKo..'
    return grid(rows, {'o': '0f0d14', 'L': '231d2e', 'K': '2c2538', 'g': 'f6d860', 'G': 'c9a030'})

def notebook_ring():
    return grid(['9a' * 8] * 16, {'9': 'b8b4c0', 'a': '6e6a78'})

def sheet_paper():
    """Hoja suelta: papel crema con fibras suaves (sin escritura). Canto en la fila 15."""
    im = img()
    for y in range(16):
        for x in range(16):
            t = 'efe8d4'
            if (x * 5 + y * 3) % 17 == 0: t = 'e4dcc4'
            if (x + y * 7) % 23 == 0: t = 'f7f3e6'
            if y == 15: t = 'cfc4a6' if x % 2 else 'dcd2b6'
            im.putpixel((x, y), hexrgb(t))
    return im

# ------------------------------------------------------------------ pantalla de lectura (256x256)
def gui_document():
    """Libro abierto (0,0,232,152), hoja de cuaderno (0,152,116,104) y hoja suelta (120,152,96,104). Sin texto."""
    W = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    px = W.putpixel
    def rect(x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1): px((x, y), hexrgb(c))
    # --- libro: tapa de cuero
    rect(0, 2, 231, 151, '3a1f62')
    rect(1, 3, 230, 150, '4b2a7a')
    for x in range(2, 230):
        for y in range(4, 150):
            if (x * 7 + y * 13) % 29 == 0: px((x, y), hexrgb('55308a'))
    rect(1, 150, 230, 151, '2a1545')
    for cx, cy in [(1, 3), (226, 3), (1, 146), (226, 146)]:
        rect(cx, cy, cx + 4, cy + 4, 'a8761c'); rect(cx + 1, cy + 1, cx + 3, cy + 3, 'e0a83a'); px((cx + 1, cy + 1), hexrgb('f6d860'))
    # --- canto de las hojas (abajo y afuera)
    for i, c in enumerate(['d8cfb8', 'efe8d4', 'cfc4a6', 'efe8d4']):
        rect(8 - i // 2, 140 + i, 223 + i // 2, 140 + i, c)
    # --- páginas
    def page(x0, x1, gutter_right):
        for x in range(x0, x1 + 1):
            for y in range(8, 140):
                d_edge = (x - x0) if gutter_right else (x1 - x)        # distancia al borde exterior
                d_gut = (x1 - x) if gutter_right else (x - x0)          # distancia al lomo
                c = 'efe8d4'
                if d_edge < 2 or y < 10: c = 'e3dbc2'
                if d_gut < 6: c = 'e6dec6'
                if d_gut < 3: c = 'd8cfb8'
                if d_gut < 1: c = 'c4b994'
                if (x * 5 + y * 11) % 37 == 0 and c == 'efe8d4': c = 'f5f0e2'
                px((x, y), hexrgb(c))
    page(10, 115, True)
    page(116, 221, False)
    # --- cinta señaladora violeta
    for y in range(6, 152):
        px((118, y), hexrgb('8a4fd0')); px((119, y), hexrgb('5a2f9a'))
    px((118, 152), hexrgb('8a4fd0')); px((119, 153), hexrgb('5a2f9a')); px((117, 153), hexrgb('5a2f9a'))
    # --- hoja de cuaderno (0,152): tapa detrás, hoja rayada, anillos
    rect(0, 152, 115, 255, '0f0d14')
    rect(1, 153, 114, 254, '231d2e')
    rect(12, 156, 111, 251, 'efe8d4')
    for y in range(168, 250, 8): rect(14, y, 109, y, 'b9bed6')
    rect(24, 156, 24, 251, 'd9a3b4')
    rect(12, 251, 111, 251, 'cfc4a6')
    for y in range(160, 250, 10):
        rect(4, y, 15, y + 2, '6e6a78'); rect(5, y, 14, y, 'b8b4c0'); rect(16, y, 17, y + 2, '9f9a8a')
    # --- hoja suelta (120,152): papel con borde irregular y alguna mancha suave
    import random as _r
    rnd = _r.Random(3)
    for y in range(152, 256):
        for x in range(120, 216):
            ex = min(x - 120, 215 - x, y - 152, 255 - y)
            if ex < 2 and rnd.random() < 0.55: continue
            c = 'efe6cc'
            if ex < 3: c = 'e2d6b4'
            if (x * 3 + y * 7) % 41 == 0: c = 'f6f0de'
            px((x, y), hexrgb(c))
    return W
