# Menas de Noxus: cúmulos de cristales violetas incrustados en la roca, dibujados a mano (prismas con
# cara iluminada a la izquierda, cara en sombra a la derecha, punta clara y contorno oscuro) y con la
# cavidad de la roca alrededor. Es una capa sobre la piedra / pizarra profunda de Minecraft.
from PIL import Image

PAL = {'O': (0x24, 0x10, 0x3c), 'D': (0x45, 0x21, 0x72), 'M': (0x6c, 0x36, 0xab), 'L': (0x99, 0x5f, 0xdb),
       'H': (0xcf, 0xab, 0xff), 'G': (0xf4, 0xe8, 0xff)}
CAVITY = {'stone': {'c': (0x5a, 0x5a, 0x5e), 'e': (0x6a, 0x6a, 0x6c)},
          'deepslate': {'c': (0x24, 0x23, 0x2a), 'e': (0x30, 0x2f, 0x37)}}

# Cúmulos dibujados a mano ('.' = roca intacta). c/e = cavidad oscura / borde de la cavidad.
BIG = [
    '....OO...',
    '...OHLO..',
    '...OLMO..',
    '.OOOLMDO.',
    'OHLOLMDO.',
    'OLMOLDDOO',
    'OLDOMDOHO',
    'cOOOOOOLO',
    '.ccccOODO',
    '.....ccOc']
MED = [
    '..OO...',
    '.OHLO..',
    '.OLMDO.',
    'OOLMDOO',
    'OHOMDOL',
    'cOOOODO',
    '.cccOOc']
SMALL = [
    '.O..',
    'OHO.',
    'OLDO',
    'OLDO',
    'cOOc']
TINY = [
    '.O.',
    'OHO',
    'OMO',
    '.c.']
LEAN = [
    '.....OO',
    '....OHO',
    '...OLDO',
    '..OLMO.',
    '.OLMDO.',
    'OLMDO..',
    'cOOOc..']

def flip(stamp):
    return [r[::-1].replace('L', '#').replace('D', 'L').replace('#', 'D') for r in stamp]

VARIANTS = [
    [(BIG, 1, 5), (SMALL, 11, 2), (TINY, 12, 12), (TINY, 7, 1)],
    [(MED, 7, 7), (LEAN, 1, 1), (SMALL, 12, 1), (TINY, 2, 12)],
    [(LEAN, 8, 8), (MED, 1, 2), (TINY, 12, 3), (SMALL, 3, 11)],
    [(flip(BIG), 6, 1), (SMALL, 1, 9), (TINY, 3, 3), (MED, 9, 10)],
]

def overlay(variant, base):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cav = CAVITY[base]
    for stamp, x0, y0 in variant:
        for dy, row in enumerate(stamp):
            for dx, ch in enumerate(row):
                x, y = x0 + dx, y0 + dy
                if ch == '.' or not (0 <= x < 16 and 0 <= y < 16): continue
                if ch in cav:
                    if im.getpixel((x, y))[3] == 0: im.putpixel((x, y), cav[ch] + (255,))
                else:
                    im.putpixel((x, y), PAL[ch] + (255,))
    return im
