# Colección de pinturas de Noxis Culture: 53 obras originales (14x14 dentro del marco).
# Índice = número de obra (nunca se reordena). Familias: 0 tiernos, 1 Veyra, 2 expediciones,
# 3 arquitectura, 4 inquietantes, 5 ceremonial. Ninguna obra contiene escritura ni glifos.
import math, sys
sys.path.insert(0, '/home/claude/e3/art')
from cv import C, line
ART = {}
def art(i, fam, name):
    def deco(fn):
        c = fn()
        ART[i] = (fam, name, c.rows())
        return fn
    return deco

FLOWER_Y = [' i ', 'hfh', ' g ']

# ============================================================ 0 · recuerdos tiernos
@art(0, 0, 'prado de flores amarillas')
def _():
    c = C()
    c.bands([(1, 'p'), (3, 'q'), (6, 'r')])
    c.stamp(10, 1, ['ii', 'ii']); c.px(9, 1, 'r'); c.px(12, 2, 'r'); c.px(10, 0, 'r')
    c.ground([6, 6, 5, 5, 5, 5, 6, 7, 7, 6, 6, 5, 5, 6], 'n')
    c.ground([9, 8, 8, 7, 7, 8, 8, 8, 8, 7, 7, 7, 8, 9], 'm', top='n')
    c.ground([10, 10, 10, 11, 11, 10, 10, 10, 10, 11, 11, 11, 10, 10], 'l')
    c.ground([12] * 14, 'k')
    for x, y in [(2, 9), (5, 8), (8, 9), (12, 8), (10, 9)]: c.px(x, y, 'h')
    for x, y in [(1, 12), (4, 13), (6, 12), (9, 13), (13, 12)]: c.px(x, y, 'j')
    for x, y in [(0, 11), (6, 11), (8, 11), (12, 11)]: c.px(x, y, 'm')
    for x0, y0 in [(2, 9), (9, 10)]:
        c.stamp(x0, y0, FLOWER_Y); c.px(x0 + 1, y0 + 3, 'l'); c.px(x0 + 1, y0 + 4, 'l'); c.px(x0 + 2, y0 + 4, 'm')
    return c

@art(1, 0, 'flor violeta en la ventana')
def _():
    c = C('Z')
    c.rect(1, 0, 12, 8, 'y')
    c.rect(2, 1, 5, 7, 'q'); c.rect(8, 1, 11, 7, 'q')
    for x, y in [(3, 2), (4, 2), (2, 3), (3, 3), (4, 3), (5, 3), (9, 4), (10, 4), (11, 4), (10, 5)]: c.px(x, y, 'r')
    c.rect(1, 0, 12, 0, 'z'); c.rect(1, 8, 12, 8, 'x')
    c.rect(0, 9, 13, 9, 'z'); c.rect(0, 10, 13, 10, 'y'); c.rect(0, 11, 13, 11, 'w')
    # maceta de barro
    c.rect(4, 6, 9, 6, 'v'); c.rect(5, 7, 8, 8, 'u'); c.px(8, 7, 't'); c.px(8, 8, 't'); c.px(5, 7, 'v')
    # tallos y hojas
    for x, y in [(6, 5), (7, 5), (6, 4), (7, 3), (5, 4), (8, 4)]: c.px(x, y, 'l')
    for x, y in [(4, 5), (9, 5), (4, 4), (9, 3)]: c.px(x, y, 'm')
    # flores violetas
    for x0, y0 in [(2, 2), (5, 0), (8, 1)]:
        c.stamp(x0, y0, [' 5 ', '545', ' 3 '])
    return c

@art(2, 0, 'una flor para alguien')
def _():
    c = C('1')
    for y in range(14):
        for x in range(14):
            d = math.hypot(x + 0.5 - 7, y + 0.5 - 6.5)
            c.px(x, y, '2' if d < 4.5 else ('2' if (d < 5.5 and (x + y) % 2) else '1') if d < 7.5 else 'a')
    c.rect(6, 9, 6, 13, 'l'); c.px(7, 13, 'k')
    c.stamp(3, 10, ['mm  ', ' mml']); c.stamp(7, 11, [' mm', 'll '])
    for y in range(14):
        for x in range(14):
            dx, dy = x + 0.5 - 7, y + 0.5 - 5.5
            d = math.hypot(dx, dy); th = math.atan2(dy, dx) + math.pi / 2
            r = 3.6 + 1.0 * math.cos(5 * th)
            if 1.0 < d <= r:
                lit = -dx * 0.8 - dy
                c.px(x, y, 'i' if lit > 2.4 else 'h' if lit > -1.2 else 'g')
    c.stamp(6, 4, ['ff', 'fe'])
    return c

@art(3, 0, 'bichitos de luz al anochecer')
def _():
    c = C()
    c.bands([(3, 'b'), (6, 'c'), (8, 'd'), (9, '2'), (10, 's')])
    c.ground([10, 10, 10, 11, 11, 11, 10, 10, 11, 11, 11, 10, 10, 10], 'L')
    c.ground([12, 12, 11, 12, 12, 12, 12, 11, 12, 12, 12, 12, 11, 12], 'K')
    for x, y in [(1, 9), (4, 10), (8, 10), (11, 9), (13, 9)]: c.px(x, y, 'L')
    c.stamp(0, 2, ['KKK ', 'KKKK', 'KKKK', 'KKK ', ' K  ', ' K  ', ' K  ', ' K  '])
    c.stamp(10, 4, [' KKK', 'KKKK', 'KKKK', ' KKK', '  K ', '  K '])
    for x, y in [(5, 3), (8, 5), (4, 7), (10, 9), (7, 9), (12, 2), (2, 11), (6, 11)]:
        c.px(x, y, 'h')
    c.px(4, 7, 'i'); c.px(8, 5, 'i'); c.px(7, 9, 'i')
    for x, y in [(5, 4), (9, 5), (4, 8), (7, 10)]: c.px(x, y, 'e' if y > 6 else 'f')
    return c

@art(4, 0, 'hongo y caracol')
def _():
    c = C()
    c.bands([(3, 'j'), (8, 'k'), (13, 'l')])
    for x, y in [(1, 1), (9, 0), (12, 2), (3, 5)]: c.px(x, y, 'k')
    c.ground([11, 11, 10, 10, 11, 11, 11, 11, 11, 11, 10, 11, 11, 12], 'l', top='m')
    c.ground([13] * 14, 'k')
    c.rect(5, 7, 7, 10, 'I'); c.px(7, 8, 'H'); c.px(7, 9, 'H'); c.px(7, 10, 'H')
    c.ellipse(6, 5, 4.6, 2.6, 'G'); c.rect(2, 6, 10, 6, 'F'); c.rect(3, 7, 9, 7, 'H')
    for x, y in [(4, 4), (8, 3), (6, 5), (9, 5), (3, 5)]: c.px(x, y, 'I')
    c.px(5, 3, 'u'); c.px(6, 3, 'u')
    c.stamp(10, 9, [' zy', 'zxz', 'NNN']); c.px(13, 10, 'N'); c.px(13, 9, 'N')
    return c

@art(5, 0, 'un farol en la oscuridad')
def _():
    c = C('6')
    for y in range(14):
        for x in range(14):
            d = math.hypot(x + 0.5 - 7.5, y + 0.5 - 7.5) + (0.6 if (x * 7 + y * 3) % 5 == 0 else 0)
            chk = (x + y) % 2
            if d < 3.0: v = 'y'
            elif d < 3.8: v = 'y' if chk else 'x'
            elif d < 4.8: v = 'x'
            elif d < 5.6: v = 'x' if chk else 'Z'
            elif d < 6.6: v = 'Z'
            elif d < 7.4: v = 'Z' if chk else '7'
            else: v = '7' if d < 9 else '6'
            c.px(x, y, v)
    for x, y in [(0, 4), (1, 4), (2, 4), (9, 12), (10, 12), (11, 12), (12, 5), (13, 5)]:
        c.px(x, y, 'w')
    c.rect(7, 0, 7, 2, '8'); c.px(7, 1, '9')
    c.stamp(5, 3, [' 898 ', '88888', '8ghg8', '8hih8', '8hRh8', '8ghg8', '88888', ' 878 '])
    return c

@art(6, 0, 'durmiendo en la pecera')
def _():
    c = C('b')
    for x, y in [(1, 1), (12, 2), (10, 0), (2, 5)]: c.px(x, y, 'd')
    c.px(12, 2, 'r')
    for y in range(14):
        for x in range(14):
            d = math.hypot(x + 0.5 - 6, (y + 0.5 - 7.4) * 1.05)
            if d < 4.3: c.px(x, y, 'c')
            elif d < 5.1: c.px(x, y, 'q' if (x < 5 and y < 8) else 'o')
    c.rect(3, 2, 8, 2, 'o'); c.rect(4, 2, 7, 2, 'D')
    c.px(3, 4, 'r'); c.px(2, 5, 'r'); c.px(2, 6, 'q')
    c.ellipse(6.3, 9.8, 3.0, 1.4, 'K')
    c.stamp(3, 7, ['K K', 'KKK', 'KKKK'])
    c.px(4, 9, 'e'); c.px(5, 9, 'e')
    c.px(9, 8, 'L'); c.px(9, 9, 'L')
    c.rect(1, 12, 10, 12, 'y'); c.rect(2, 13, 9, 13, 'x'); c.px(1, 12, 'z')
    c.rect(11, 8, 12, 11, 'K'); c.rect(11, 8, 11, 10, 'L'); c.rect(10, 12, 13, 12, 'K'); c.rect(11, 11, 12, 11, '3'); c.px(12, 11, '5')
    return c

@art(7, 0, 'mariposas')
def _():
    c = C()
    c.bands([(3, 'q'), (8, 'r')])
    c.ground([10, 10, 9, 9, 10, 10, 10, 9, 9, 9, 10, 10, 9, 9], 'm', top='n')
    c.ground([12] * 14, 'l')
    for x, y in [(1, 10), (5, 11), (9, 10), (12, 11), (3, 12), (11, 13)]: c.px(x, y, '4')
    for x, y in [(1, 9), (9, 9)]: c.px(x, y, '5')
    c.stamp(2, 2, ['44 44', '453 54', ' 4L4 '][:2]); c.stamp(2, 2, ['44.44', '45L54', '.3L3.'])
    c.stamp(8, 5, ['hh.hh', 'hiLih', '.gLg.'])
    c.px(6, 1, 'L'); c.px(12, 4, 'L')
    return c

@art(48, 0, 'retrato de un Noxis')
def _():
    c = C('3')
    for y in range(14):
        for x in range(14):
            d = math.hypot(x - 4, y - 3)
            c.px(x, y, '4' if d < 3 else '3' if d < 9 else '2')
    c.rect(4, 0, 10, 4, 'K'); c.rect(5, 0, 5, 3, 'L')
    c.rect(4, 4, 10, 4, '3'); c.px(7, 4, '5'); c.px(8, 4, '4')
    c.rect(2, 5, 12, 5, 'K')
    c.stamp(2, 5, ['K  ', 'KK ', 'K2K'][:0])
    c.ellipse(7, 9.5, 4.8, 3.6, 'L')
    c.stamp(2, 6, ['L', 'LL']); c.stamp(11, 6, [' L', 'LL'])
    c.px(2, 7, '2'); c.px(12, 7, '2')
    for y in range(10, 14):
        for x in range(14):
            if c.get(x, y) == 'L' and (x > 9 or y > 11): c.px(x, y, 'K')
    c.rect(1, 13, 12, 13, 'K')
    c.stamp(4, 8, ['hh', 'gh']); c.stamp(8, 8, ['hh', 'hg'])
    c.px(5, 8, 'e'); c.px(8, 8, 'e')
    c.px(6, 11, 'K'); c.px(7, 11, 'K')
    return c

# ============================================================ 1 · paisajes de Veyra (gris, agotado, restos de luz)
@art(8, 1, 'colinas grises de Veyra')
def _():
    c = C()
    c.bands([(2, '0'), (5, '-'), (6, '0')])
    c.ellipse(9.5, 3.5, 1.6, 1.6, 'H'); c.px(9, 3, 'I')
    c.ground([6, 6, 5, 5, 6, 7, 7, 6, 5, 5, 5, 6, 7, 7], '9')
    c.ground([9, 8, 8, 8, 8, 9, 9, 9, 8, 7, 7, 8, 8, 9], '8', top='9')
    c.ground([11, 11, 10, 10, 11, 12, 12, 11, 11, 11, 10, 10, 11, 11], '7', top='8')
    c.ground([13] * 14, '6')
    for x, y in [(3, 12), (9, 12), (12, 12)]: c.px(x, y, '6')
    return c

@art(9, 1, 'luz tenue entre ruinas')
def _():
    c = C('7')
    c.bands([(4, '8'), (8, '7'), (13, '6')])
    c.rect(0, 3, 3, 13, '9'); c.rect(1, 2, 2, 2, '9'); c.rect(0, 4, 0, 13, '0')
    c.rect(10, 5, 13, 13, '9'); c.rect(11, 4, 12, 4, '9'); c.rect(13, 6, 13, 13, '8')
    c.rect(4, 11, 9, 13, '8'); c.rect(4, 11, 9, 11, '9')
    for y in range(14):
        for x in range(4, 10):
            d = math.hypot(x + 0.5 - 7, y + 0.5 - 9)
            if d < 1.2: c.px(x, y, '5')
            elif d < 2.2 and c.get(x, y) in '76': c.px(x, y, '3')
            elif d < 3.4 and c.get(x, y) in '76': c.px(x, y, '2')
    c.px(6, 8, '4'); c.px(7, 10, '4'); c.px(8, 9, '4')
    for x, y in [(1, 6), (2, 9), (11, 8), (12, 11)]: c.px(x, y, '8')
    return c

@art(10, 1, 'arco roto')
def _():
    c = C()
    c.bands([(5, '-'), (9, '0'), (10, '9')])
    c.ground([11] * 14, '8', top='9'); c.ground([13] * 14, '7')
    c.stamp(1, 1, [
        '....9998....',
        '..99887.....',
        '.9887.......',
        '.987........',
        '987......7..',
        '987......87.',
        '987......887',
        '987......887',
        '987......887',
        '987......887',
        '887......777'])
    c.stamp(6, 10, ['98', '877']); c.px(9, 11, '7')
    return c

@art(11, 1, 'árbol seco con una hoja')
def _():
    c = C()
    c.bands([(3, '0'), (8, '-'), (9, '0')])
    c.ground([10, 10, 11, 11, 10, 10, 10, 10, 11, 11, 11, 10, 10, 10], '9')
    c.ground([12] * 14, '8')
    c.rect(6, 6, 7, 11, '7'); c.rect(7, 6, 7, 11, '6'); c.px(5, 11, '7'); c.px(8, 11, '6')
    for a in [(6, 6, 3, 3), (3, 3, 1, 1), (4, 4, 2, 5), (7, 6, 10, 3), (10, 3, 12, 1), (9, 4, 12, 5), (6, 5, 6, 2), (6, 2, 5, 0), (10, 3, 10, 1)]:
        line(c, *a, '6')
    c.px(12, 6, '4'); c.px(12, 7, '3')
    return c

@art(12, 1, 'llanura de polvo y aguja lejana')
def _():
    c = C()
    c.bands([(3, '0'), (6, '-'), (7, '0'), (9, '9'), (11, '0'), (13, '9')])
    c.rect(9, 2, 9, 7, '8'); c.rect(10, 4, 10, 7, '8'); c.px(9, 1, '8'); c.px(8, 6, '8'); c.px(8, 7, '8')
    c.px(9, 3, '3')
    for x, y in [(1, 5), (2, 5), (4, 4), (12, 5), (13, 5), (3, 10), (4, 10), (10, 12), (11, 12)]: c.px(x, y, '-')
    for x, y in [(6, 9), (7, 9), (12, 9), (0, 12), (1, 12), (6, 13)]: c.px(x, y, '8')
    return c

@art(13, 1, 'el último cristal')
def _():
    c = C('6')
    c.bands([(6, '6'), (9, '7'), (13, '8')])
    for y in range(14):
        for x in range(14):
            d = math.hypot(x + 0.5 - 7, y + 0.5 - 7)
            if d < 4.5 and c.get(x, y) == '6': c.px(x, y, '1')
            elif d < 4.5 and c.get(x, y) == '7': c.px(x, y, '2')
    c.ground([10, 10, 9, 9, 10, 10, 11, 11, 10, 9, 9, 10, 10, 11], '8', top='9')
    c.stamp(6, 3, [' 5', '54', '543', '433', '432', '332'])
    c.stamp(5, 6, ['4 ', '3 ', '32'])
    c.px(9, 7, '3'); c.px(9, 8, '2')
    c.px(4, 9, '7'); c.px(10, 9, '7')
    return c

@art(14, 1, 'lago gris')
def _():
    c = C()
    c.bands([(4, '0'), (6, '-'), (7, '9'), (13, '8')])
    c.ellipse(4.5, 3.5, 1.4, 1.4, 'H')
    c.ground([7, 7, 6, 6, 6, 7, 7, 7, 7, 7, 6, 6, 7, 7], '9')
    for y in (8, 9, 10, 12):
        for x in range(14):
            if (x * 3 + y * 5) % 7 == 0: c.px(x, y, '9')
    c.rect(4, 8, 5, 8, 'H'); c.px(4, 9, '-'); c.rect(3, 10, 5, 10, '-'); c.px(5, 11, '0')
    c.ground([12, 12, 13, 13, 13, 13, 13, 13, 13, 13, 12, 12, 11, 11], '7')
    return c

@art(15, 1, 'escalera gastada hacia la niebla')
def _():
    c = C()
    c.bands([(6, '-'), (13, '0')])
    for i in range(6):
        x0 = 1 + 2 * i; top = 12 - 2 * i
        far = i >= 4
        c.rect(x0, top, 13, top, '0' if far else '9')
        c.rect(x0, top + 1, 13, 13, '9' if far else '8')
        c.px(x0, top, '-' if far else '0')
    for i in range(4):
        c.rect(1 + 2 * i, 13 - 2 * i, 13, 13 - 2 * i, '7')
    for x, y in [(5, 9), (10, 11), (8, 7)]: c.px(x, y, '7')
    return c

@art(16, 2, 'colinas verdes y sol')
def _():
    c = C()
    c.bands([(2, 'p'), (5, 'q'), (6, 'r')])
    c.ellipse(3.5, 2.5, 1.8, 1.8, 'h'); c.px(3, 2, 'i'); c.px(2, 2, 'i')
    c.stamp(8, 1, [' rr ', 'rrrr']); c.stamp(11, 3, ['rr'])
    c.ground([8, 7, 7, 6, 6, 7, 8, 8, 7, 6, 6, 6, 7, 8], 'm', top='n')
    c.ground([11, 10, 10, 10, 11, 11, 10, 10, 10, 11, 11, 11, 10, 10], 'l', top='m')
    c.ground([13] * 14, 'k')
    c.stamp(9, 3, [' l ', 'lll', 'lkl', ' y '])
    c.stamp(3, 7, [' k ', 'kkk', ' x '])
    return c

@art(17, 2, 'montañas y lago')
def _():
    c = C()
    c.bands([(3, 'q'), (5, 'r')])
    c.ground([6, 5, 4, 3, 2, 3, 4, 5, 4, 3, 3, 4, 5, 6], '9')
    c.ground([7] * 14, '8')
    for x, y in [(3, 3), (4, 2), (5, 3), (4, 3), (9, 3), (10, 3), (2, 4), (11, 4)]: c.px(x, y, 'J')
    for x in range(14):
        for y in range(4, 8):
            if c.get(x, y) == '9' and x > 4 and x < 9: c.px(x, y, '8')
    c.ground([8, 8, 7, 7, 8, 8, 8, 8, 8, 7, 7, 8, 8, 8], 'k', top='l')
    c.rect(0, 9, 13, 11, 'C'); c.rect(0, 9, 13, 9, 'B')
    for x, y in [(3, 10), (4, 10), (9, 10), (10, 11), (6, 11)]: c.px(x, y, 'D')
    c.ground([12, 12, 11, 12, 12, 12, 12, 12, 12, 12, 11, 11, 12, 12], 'l', top='m')
    return c

@art(18, 2, 'linde del bosque')
def _():
    c = C()
    c.bands([(4, 'q'), (6, 'r')])
    def pine(x, y, h, dark='k', lite='l'):
        for i in range(h):
            w = (i + 1) // 2
            c.rect(x - w, y + i, x + w, y + i, dark)
            c.px(x - w, y + i, lite)
        c.px(x, y + h, 'x')
    pine(2, 2, 7); pine(6, 4, 6, 'j', 'k'); pine(11, 1, 8); pine(8, 5, 5, 'j', 'k')
    c.ground([11, 11, 10, 10, 11, 11, 11, 11, 11, 10, 10, 11, 11, 11], 'l', top='m')
    c.ground([13] * 14, 'k')
    for x, y in [(4, 12), (9, 12)]: c.px(x, y, 'h')
    return c

@art(19, 2, 'atardecer en el mar')
def _():
    c = C()
    c.bands([(1, 's'), (3, 't'), (5, 'u'), (6, 'v')])
    c.ellipse(6.5, 7.2, 2.6, 2.4, 'h'); c.rect(5, 5, 7, 5, 'i')
    c.rect(0, 8, 13, 13, 'B'); c.rect(0, 8, 13, 8, 'o')
    for y, xs in [(9, (5, 8)), (10, (4, 9)), (11, (5, 8)), (12, (6, 7)), (13, (6, 7))]:
        for x in range(xs[0], xs[1] + 1):
            if (x + y) % 2 == 0 or y < 11: c.px(x, y, 'v' if y < 11 else 'u')
    c.stamp(1, 2, ['tt ', ' ttt']); c.stamp(9, 1, ['ss', ' sss'])
    for x, y in [(1, 11), (2, 11), (11, 10), (12, 10), (10, 12)]: c.px(x, y, 'A')
    return c

@art(20, 2, 'desierto y cactus')
def _():
    c = C()
    c.bands([(3, 'q'), (6, 'r')])
    c.ellipse(11, 2, 1.5, 1.5, 'i')
    c.ground([8, 8, 7, 7, 7, 8, 8, 9, 9, 8, 8, 7, 7, 8], 'M', top='N')
    c.ground([11, 11, 11, 10, 10, 10, 11, 11, 12, 12, 11, 11, 11, 11], 'N')
    c.stamp(9, 9, ['M', 'M', 'MM'][:0])
    c.rect(4, 4, 5, 11, 'l'); c.rect(5, 4, 5, 11, 'k')
    c.rect(2, 6, 3, 6, 'l'); c.rect(2, 4, 2, 6, 'l')
    c.rect(6, 7, 7, 7, 'l'); c.rect(7, 5, 7, 7, 'k')
    c.px(4, 12, 'M'); c.px(5, 12, 'M'); c.px(6, 12, 'M')
    for x, y in [(10, 12), (11, 13), (1, 12)]: c.px(x, y, 'M')
    return c

@art(21, 2, 'el Nether')
def _():
    c = C('O')
    c.bands([(3, 'E'), (8, 'O')])
    c.ground([9, 9, 8, 8, 9, 10, 10, 9, 9, 8, 8, 9, 9, 10], 'P', top='F')
    c.rect(0, 12, 13, 13, 'Q'); c.rect(0, 11, 13, 11, 'R')
    for x in range(14):
        if x % 3 == 0: c.px(x, 11, 'Q')
    c.rect(9, 0, 10, 10, 'Q'); c.rect(9, 0, 9, 10, 'R')
    c.rect(1, 3, 4, 7, 'E'); c.rect(1, 2, 1, 2, 'E'); c.rect(4, 2, 4, 2, 'E'); c.px(2, 5, 'Q'); c.px(3, 5, 'R')
    for x, y in [(6, 1), (12, 3), (7, 5)]: c.px(x, y, 'P')
    return c

@art(22, 2, 'el End')
def _():
    c = C('U')
    for x, y in [(1, 1), (5, 2), (11, 1), (8, 4), (2, 6), (12, 6)]: c.px(x, y, '4')
    c.px(5, 2, '5'); c.px(11, 1, '5')
    c.ellipse(7, 10.5, 6.5, 2.0, 'S')
    c.rect(1, 10, 12, 10, 'S'); c.rect(2, 11, 11, 12, 'T'); c.rect(4, 13, 9, 13, 'T')
    for x0, h in [(3, 5), (7, 7), (10, 4)]:
        c.rect(x0, 9 - h, x0 + 1, 9, 'L'); c.px(x0, 9 - h, '3')
    c.px(4, 4, 'W')
    return c

@art(23, 2, 'isla')
def _():
    c = C()
    c.bands([(4, 'q'), (7, 'r'), (13, 'C')])
    c.rect(0, 8, 13, 8, 'D')
    c.ellipse(6.5, 10.5, 4.2, 1.5, 'N'); c.rect(3, 11, 10, 11, 'M')
    c.rect(7, 4, 7, 9, 'y'); c.px(6, 5, 'y')
    c.stamp(4, 2, ['  mm  ', ' mllm ', 'm l  m', '   l  '])
    c.px(9, 9, 'l'); c.px(10, 9, 'l')
    for x, y in [(1, 12), (2, 12), (11, 13), (12, 13)]: c.px(x, y, 'D')
    return c

@art(49, 2, 'noche de luna')
def _():
    c = C()
    c.bands([(3, 'a'), (7, 'b'), (9, 'c')])
    c.ellipse(9.5, 3, 2.2, 2.2, 'I'); c.px(10, 3, 'H'); c.px(9, 2, 'H'); c.px(11, 4, 'H')
    for x, y in [(1, 1), (3, 4), (6, 2), (13, 6), (5, 6), (2, 8)]: c.px(x, y, 'r')
    c.ground([10, 9, 9, 8, 8, 9, 10, 10, 9, 9, 10, 11, 11, 10], 'k', top='l')
    c.ground([12, 12, 12, 12, 13, 13, 12, 12, 12, 13, 13, 13, 12, 12], 'j')
    return c

@art(50, 2, 'dos Noxis bajo las estrellas')
def _():
    c = C()
    c.bands([(3, 'a'), (6, 'b'), (8, 'c'), (9, 'd'), (10, '2')])
    for x, y in [(1, 1), (4, 0), (7, 1), (10, 2), (12, 4), (2, 5), (11, 6), (13, 1), (0, 3)]: c.px(x, y, 'r')
    c.px(7, 1, 'J'); c.px(10, 2, 'h')
    c.ground([12, 12, 11, 11, 10, 10, 10, 10, 10, 10, 11, 11, 12, 12], 'j', top='k')
    for x0 in (3, 8):
        c.rect(x0 + 1, 3, x0 + 2, 5, 'K'); c.px(x0 + 1, 3, 'L'); c.rect(x0 + 1, 5, x0 + 2, 5, '3'); c.px(x0 + 2, 5, '5')
        c.rect(x0, 6, x0 + 3, 6, 'K')
        c.stamp(x0, 7, ['K  K', 'KKKK', 'KKKK', ' KK '])
        c.px(x0, 7, 'L'); c.px(x0 + 1, 8, 'h'); c.px(x0 + 2, 8, 'h')
    c.px(12, 10, 'K'); c.px(13, 9, 'K')
    return c


# ============================================================ 3 · arquitectura y lugares
@art(24, 3, 'torre al anochecer')
def _():
    c = C()
    c.bands([(3, 'c'), (6, '2'), (8, 's'), (9, 't')])
    c.ground([11, 11, 10, 10, 10, 11, 11, 11, 11, 11, 10, 10, 11, 11], 'L')
    c.ground([12] * 14, 'K')
    c.rect(5, 3, 8, 11, '7'); c.rect(5, 3, 5, 11, '8'); c.rect(8, 3, 8, 11, '6')
    c.rect(4, 2, 9, 2, '7'); c.px(4, 1, '7'); c.px(6, 1, '7'); c.px(9, 1, '7'); c.px(7, 1, '7')
    c.stamp(6, 5, ['h', 'g']); c.px(7, 8, 'h'); c.px(7, 9, 'g')
    c.rect(6, 10, 7, 11, 'K')
    for x, y in [(1, 1), (11, 2), (13, 0), (2, 4)]: c.px(x, y, 'd')
    return c

@art(25, 3, 'tres puentes')
def _():
    c = C()
    c.bands([(2, '7'), (5, '6'), (9, 'K'), (13, 'a')])
    c.rect(0, 0, 1, 13, '8'); c.rect(1, 0, 1, 13, '9'); c.rect(12, 0, 13, 13, '8'); c.rect(12, 0, 12, 13, '7')
    c.rect(2, 3, 11, 3, '0'); c.rect(2, 4, 11, 4, '8'); c.px(4, 5, '7'); c.px(9, 5, '7')
    c.rect(2, 7, 5, 7, '9'); c.rect(8, 7, 11, 7, '9'); c.rect(2, 8, 5, 8, '7'); c.rect(8, 8, 11, 8, '7')
    c.px(5, 9, '7'); c.px(8, 9, '6')
    c.rect(3, 10, 10, 10, '8'); c.rect(3, 11, 10, 11, '7'); c.px(2, 10, '7'); c.px(11, 10, '7')
    for x, y in [(6, 12), (7, 11), (6, 9), (7, 8), (6, 6), (7, 5)]: c.px(x, y, '7')
    c.px(7, 13, '2'); c.px(6, 13, '1')
    return c

@art(26, 3, 'casa iluminada')
def _():
    c = C()
    c.bands([(4, 'b'), (8, 'c'), (9, 'd')])
    for x, y in [(1, 1), (4, 3), (12, 2), (9, 0)]: c.px(x, y, 'r')
    c.ground([10] * 14, 'k', top='l'); c.ground([12] * 14, 'j')
    c.stamp(3, 4, ['   xx   ', '  xyyx  ', ' xyyyyx ', 'xxxxxxxx'])
    c.rect(4, 8, 9, 10, 'y'); c.rect(4, 8, 4, 10, 'z'); c.rect(9, 8, 9, 10, 'x')
    c.rect(5, 8, 6, 9, 'h'); c.px(5, 8, 'i'); c.rect(8, 9, 8, 10, 'w')
    c.rect(9, 3, 9, 5, '8'); c.px(10, 2, '9'); c.px(10, 1, '0'); c.px(11, 0, '0')
    c.rect(4, 11, 7, 11, 'f'); c.px(5, 12, 'e')
    return c

@art(27, 3, 'columnata desde un ángulo imposible')
def _():
    c = C()
    c.bands([(5, 'r'), (13, 'q')])
    c.rect(0, 11, 13, 13, '0'); c.rect(0, 11, 13, 11, '-')
    for x0, top in [(0, 1), (4, 3), (7, 5), (9, 6), (11, 7)]:
        w = 2 if x0 < 7 else 1
        c.rect(x0, top, x0 + w - 1, 11, 'I'); c.rect(x0 + w - 1, top, x0 + w - 1, 11, 'H')
        c.rect(x0, top - 1 if top > 0 else top, x0 + w, top - 1 if top > 0 else top, 'H')
    c.rect(0, 0, 13, 0, 'H')
    for x in range(14):
        if x % 2: c.px(x, 12, '9')
    c.px(12, 9, '3')
    return c

@art(28, 3, 'portón de piedra')
def _():
    c = C()
    c.bands([(5, '9'), (13, '8')])
    c.rect(1, 2, 12, 13, '7')
    for y in range(2, 14, 2):
        for x in range(1, 13):
            c.px(x, y, '8' if (x + y // 2 * 2) % 4 else '6')
    c.ellipse(6.5, 7, 3.2, 4.8, 'K'); c.rect(4, 7, 9, 13, 'K')
    c.ring(6.5, 7.2, 3.3, 4.2, '9')
    for y in range(4, 14):
        for x in range(4, 10):
            if c.get(x, y) == 'K':
                d = math.hypot(x + 0.5 - 7, y + 0.5 - 10)
                if d < 1.5: c.px(x, y, '4')
                elif d < 2.8: c.px(x, y, '2')
                elif d < 3.8: c.px(x, y, '1')
    c.px(6, 10, '5')
    c.rect(1, 1, 12, 1, '9')
    return c

@art(29, 3, 'el trono negro')
def _():
    c = C('6')
    for y in range(14):
        for x in range(14):
            d = math.hypot(x + 0.5 - 7, y + 0.5 - 5)
            c.px(x, y, '9' if d < 3 else '8' if d < 4.8 else '7' if d < 6.8 else '6')
    c.rect(0, 0, 1, 11, '7'); c.rect(12, 0, 13, 11, '7'); c.rect(1, 0, 1, 11, '6'); c.rect(12, 0, 12, 11, '6')
    c.rect(0, 11, 13, 13, '8'); c.rect(2, 11, 11, 11, '9'); c.rect(3, 12, 10, 12, '0')
    c.rect(5, 2, 8, 10, 'K'); c.px(5, 1, 'K'); c.px(8, 1, 'K'); c.px(6, 1, 'K'); c.px(7, 0, 'K')
    c.rect(4, 6, 9, 7, 'K'); c.rect(4, 8, 4, 10, 'K'); c.rect(9, 8, 9, 10, 'K')
    c.rect(5, 2, 5, 5, 'L'); c.px(4, 6, 'L'); c.px(4, 8, 'L')
    c.px(7, 1, '4'); c.px(6, 1, '2')
    c.px(9, 3, '8'); c.px(10, 9, 'K'); c.px(8, 2, '7')
    return c

@art(30, 3, 'peceras en el prado')
def _():
    c = C()
    c.bands([(4, 'q'), (7, 'r')])
    c.ground([9, 9, 8, 8, 8, 9, 9, 9, 9, 8, 8, 8, 9, 9], 'm', top='n')
    c.ground([12] * 14, 'l')
    for cx, cy, r in [(3.5, 8, 2.2), (10, 9, 2.6), (7, 4.8, 1.6)]:
        for y in range(14):
            for x in range(14):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d < r - 0.7: c.px(x, y, 'D')
                elif d < r: c.px(x, y, 'C')
        c.px(int(cx - r / 2), int(cy - r / 2), 'J')
        c.rect(int(cx - r + 0.5), int(cy + r - 0.3), int(cx + r - 0.5), int(cy + r - 0.3), 'y')
    c.px(10, 9, 'K'); c.px(11, 9, 'K'); c.px(10, 8, 'K')
    return c

@art(31, 3, 'arcos que no llevan a ningún lado')
def _():
    c = C('2')
    c.bands([(4, '3'), (9, '2'), (13, '1')])
    def arch(x0, y0, w, h, col, sh):
        c.rect(x0, y0 + 1, x0, y0 + h, col); c.rect(x0 + w - 1, y0 + 1, x0 + w - 1, y0 + h, sh)
        c.rect(x0 + 1, y0, x0 + w - 2, y0, col)
    arch(1, 6, 5, 6, '-', '0'); arch(5, 2, 5, 5, '-', '0'); arch(8, 7, 5, 5, '0', '9')
    arch(3, 10, 4, 3, '0', '9')
    c.rect(1, 12, 5, 12, '-')
    return c

@art(52, 3, 'pecera bajo las estrellas')
def _():
    c = C()
    c.bands([(5, 'a'), (9, 'b'), (10, 'c')])
    for x, y in [(1, 2), (3, 0), (6, 1), (10, 3), (12, 1), (13, 5), (2, 6), (8, 4)]: c.px(x, y, 'r')
    c.ground([11, 11, 11, 10, 10, 10, 10, 10, 10, 10, 11, 11, 12, 12], 'k', top='l')
    for y in range(14):
        for x in range(14):
            d = math.hypot(x + 0.5 - 7, y + 0.5 - 7.5)
            if d < 2.5: c.px(x, y, 'c')
            elif d < 3.2: c.px(x, y, 'o')
    c.px(5, 6, 'r'); c.px(6, 5, 'q')
    c.rect(5, 10, 9, 10, 'y'); c.rect(4, 4, 9, 4, '8')
    c.rect(6, 7, 7, 8, 'g'); c.px(7, 7, 'h')
    return c

# ============================================================ 4 · formas inquietantes
@art(32, 4, 'ojos en la oscuridad')
def _():
    c = C('a')
    for x, y in [(2, 3), (9, 2), (5, 7), (11, 9), (3, 11)]:
        c.px(x, y, 'h'); c.px(x + 2, y, 'h')
        c.px(x, y + 1, 'e'); c.px(x + 2, y + 1, 'e')
    c.rect(7, 5, 9, 5, 'b'); c.px(9, 2, 'i')
    return c

@art(33, 4, 'figura sin rostro en la niebla')
def _():
    c = C()
    c.bands([(3, '9'), (7, '8'), (13, '7')])
    for y in range(14):
        for x in range(14):
            if (x * 3 + y * 7) % 11 == 0: c.px(x, y, '9' if y > 3 else '0')
    c.ellipse(6.5, 3.5, 1.5, 1.8, 'K')
    c.rect(5, 5, 8, 12, 'K'); c.rect(4, 7, 4, 11, 'K'); c.rect(9, 7, 9, 11, 'K')
    c.rect(5, 13, 5, 13, 'K'); c.rect(8, 13, 8, 13, 'K')
    c.rect(0, 13, 13, 13, '6')
    return c

@art(34, 4, 'grieta de luz')
def _():
    c = C('6')
    for y in range(14):
        for x in range(14):
            c.px(x, y, '7' if (x // 4 + y // 3) % 2 else '6')
    pts = [(8, 0), (7, 2), (8, 4), (6, 6), (7, 8), (5, 10), (6, 13)]
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        line(c, x0, y0, x1, y1, 'W')
    for y in range(14):
        for x in range(14):
            if c.get(x, y) == 'W':
                for dx in (-1, 1):
                    if 0 <= x + dx < 14 and c.get(x + dx, y) in '67': c.px(x + dx, y, 'V' if dx < 0 else '2')
    c.px(7, 2, 'J'); c.px(6, 6, 'J'); c.px(5, 10, 'J')
    return c

@art(35, 4, 'figura sentada ante una luz')
def _():
    c = C('a')
    for y in range(14):
        for x in range(14):
            d = math.hypot(x + 0.5 - 10, y + 0.5 - 8)
            c.px(x, y, '3' if d < 1.6 else '2' if d < 3.2 else '1' if d < 5.6 else 'b' if d < 8.5 else 'a')
    c.rect(0, 11, 13, 13, 'b'); c.rect(2, 11, 13, 11, '1'); c.rect(7, 11, 12, 11, '2')
    c.px(10, 8, 'i'); c.px(10, 7, 'h'); c.px(11, 8, 'h'); c.px(9, 8, 'g'); c.px(10, 9, 'g'); c.px(10, 10, 'f')
    c.rect(3, 6, 5, 10, 'K'); c.rect(3, 4, 5, 5, 'K'); c.px(3, 3, 'K'); c.px(5, 3, 'K'); c.rect(2, 10, 6, 10, 'K')
    c.rect(6, 6, 6, 9, '2'); c.px(6, 4, '2'); c.px(5, 3, '2'); c.px(7, 10, '1')
    c.px(5, 4, 'h')
    return c

@art(36, 4, 'una sombra demasiado larga')
def _():
    c = C()
    c.bands([(2, 's'), (4, 't'), (5, 'u'), (6, 'v')])
    c.rect(0, 7, 13, 13, 'M'); c.rect(0, 7, 13, 7, 'N')
    c.rect(2, 4, 2, 6, 'K'); c.px(2, 3, 'K')
    for i in range(10):
        c.px(3 + i, 7 + (i * 6) // 10, 'Z'); c.px(3 + i, 8 + (i * 6) // 10, 'Z')
    c.px(12, 13, 'Z'); c.px(13, 13, 'Z'); c.rect(11, 12, 13, 12, 'Z')
    return c

@art(37, 4, 'el pozo')
def _():
    c = C('8')
    for y in range(14):
        for x in range(14):
            d = math.hypot((x + 0.5 - 7) / 1.0, (y + 0.5 - 7) / 1.0)
            v = '8' if d > 6.2 else '7' if d > 4.6 else '6' if d > 3.2 else 'K' if d > 1.6 else 'a'
            c.px(x, y, v)
    for x, y in [(3, 3), (10, 4), (4, 10), (11, 10), (7, 1), (1, 7), (12, 7)]: c.px(x, y, '9')
    c.px(7, 7, '2'); c.px(6, 6, '1')
    return c

@art(38, 4, 'luna roja')
def _():
    c = C('E')
    c.bands([(5, 'E'), (9, 'F'), (10, 'E')])
    c.ellipse(7, 4, 2.6, 2.6, 'G'); c.px(6, 3, 't'); c.px(8, 5, 'F'); c.px(7, 2, 'u')
    c.ground([10, 9, 9, 10, 11, 11, 10, 9, 8, 8, 9, 10, 10, 11], 'K')
    c.ground([9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9], 'K', below=0)
    for x in range(14):
        if c.get(x, 10) == 'E': c.px(x, 10, 'K')
    c.rect(0, 11, 13, 13, 'K'); c.px(10, 6, 'K'); c.px(10, 7, 'K'); c.px(11, 6, 'K')
    return c

@art(39, 4, 'alguien pequeño sostiene algo brillante')
def _():
    c = C('a')
    for y in range(14):
        for x in range(14):
            d = math.hypot(x + 0.5 - 7, y + 0.5 - 6.5)
            c.px(x, y, '2' if d < 2.2 else '1' if d < 4 else 'b' if d < 6.5 else 'a')
    c.rect(5, 9, 8, 12, 'K'); c.rect(5, 7, 8, 8, 'K'); c.px(5, 6, 'K'); c.px(8, 6, 'K')
    c.px(6, 8, 'h'); c.px(7, 8, 'h')
    c.rect(4, 10, 4, 11, 'K'); c.rect(9, 10, 9, 11, 'K')
    c.stamp(6, 3, [' 5', '54', '44']); c.px(6, 4, '5'); c.px(7, 3, 'J')
    c.rect(4, 13, 9, 13, 'b')
    return c

@art(51, 4, 'mirando desde la cueva')
def _():
    c = C('8')
    for y in range(14):
        for x in range(14):
            c.px(x, y, '9' if (x * 7 + y * 5) % 9 == 0 else '8')
    c.ellipse(7, 8.5, 5.2, 5.0, 'K'); c.rect(2, 9, 12, 13, 'K')
    c.ring(7, 8.5, 5.2, 6.0, '7')
    c.px(6, 9, 'h'); c.px(9, 9, 'h'); c.px(6, 10, 'e'); c.px(9, 10, 'e')
    c.rect(0, 13, 13, 13, '7')
    return c

# ============================================================ 5 · ceremonial
@art(40, 5, 'mandala violeta')
def _():
    c = C('1')
    for y in range(14):
        for x in range(14):
            dx, dy = x + 0.5 - 7, y + 0.5 - 7
            d = math.hypot(dx, dy); th = math.atan2(dy, dx)
            if d < 1.5: v = 'h'
            elif d < 2.6: v = '4'
            elif d < 4.2: v = '3' if math.cos(8 * th) > 0 else '2'
            elif d < 5.0: v = 'g'
            elif d < 6.6: v = '2' if math.cos(4 * th) > 0.3 else '1'
            else: v = '1'
            c.px(x, y, v)
    c.rect(6, 6, 7, 7, 'i')
    return c

@art(41, 5, 'la gema en forma de lágrima')
def _():
    c = C('1')
    for y in range(14):
        for x in range(14):
            d = math.hypot(x + 0.5 - 7, y + 0.5 - 8)
            c.px(x, y, '2' if d < 5.5 else '1')
    c.stamp(4, 1, [
        '..5...',
        '..54..',
        '.5443.',
        '.5433.',
        '54433.',
        '544332',
        '544332',
        '543332',
        '.4332.',
        '..32..'])
    c.px(6, 3, 'J'); c.px(5, 6, 'J')
    for x in (2, 11): c.px(x, 12, 'g')
    c.rect(3, 12, 10, 12, 'f')
    return c

@art(42, 5, 'rombos')
def _():
    c = C()
    for y in range(14):
        for x in range(14):
            a = abs((x + 1) % 6 - 3 + 0.5) + abs((y + 1) % 6 - 3 + 0.5)
            cell = ((x + 1) // 6 + (y + 1) // 6) % 2
            if a < 2.6: v = '3' if cell else '2'
            elif a < 3.6: v = '1'
            else: v = 'b'
            c.px(x, y, v)
            if a < 1.1: c.px(x, y, 'g' if cell else '4')
    return c

@art(43, 5, 'racimo de cristales')
def _():
    c = C('6')
    for y in range(14):
        for x in range(14):
            d = math.hypot(x + 0.5 - 7, y + 0.5 - 7)
            c.px(x, y, '1' if d < 6 else '6')
    c.ground([11, 11, 10, 10, 11, 11, 11, 11, 10, 10, 11, 11, 12, 12], '7', top='8')
    for x0, y0, h in [(3, 6, 5), (6, 2, 9), (9, 4, 7), (5, 7, 4), (10, 8, 3)]:
        c.rect(x0, y0 + 1, x0 + 1, y0 + h, '3'); c.rect(x0, y0 + 1, x0, y0 + h, '4'); c.px(x0, y0, '5')
        c.px(x0 + 1, y0 + h, '2')
    c.px(6, 3, 'J')
    return c

@art(44, 5, 'cuadrados concéntricos')
def _():
    c = C()
    cols = ['1', '3', '2', 'g', '1', '4', 'h']
    for y in range(14):
        for x in range(14):
            k = min(x, y, 13 - x, 13 - y)
            c.px(x, y, cols[k])
    return c

@art(45, 5, 'estrella de ocho puntas')
def _():
    c = C('b')
    for y in range(14):
        for x in range(14):
            dx, dy = abs(x + 0.5 - 7), abs(y + 0.5 - 7)
            if min(dx, dy) < 0.6 and max(dx, dy) < 6.5 or abs(dx - dy) < 0.6 and dx < 4.2:
                c.px(x, y, 'g')
            if dx + dy < 3.2: c.px(x, y, 'h')
    c.rect(6, 6, 7, 7, 'i')
    for x, y in [(1, 1), (12, 1), (1, 12), (12, 12)]: c.px(x, y, 'd')
    return c

@art(46, 5, 'estudio de mineral de Noxus')
def _():
    c = C('8')
    for y in range(14):
        for x in range(14):
            c.px(x, y, '9' if (x * 5 + y * 3) % 7 == 0 else '7' if (x * 3 + y * 5) % 8 == 0 else '8')
    for x, y in [(2, 2), (8, 3), (4, 8), (10, 9)]:
        c.stamp(x, y, [' 5 ', '543', ' 32'])
    return c

@art(47, 5, 'luces gemelas')
def _():
    c = C('1')
    for y in range(14):
        for x in range(14):
            for cx in (3.5, 10.5):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - 6)
                if d < 1.4: c.px(x, y, 'i')
                elif d < 2.4: c.px(x, y, 'h')
                elif d < 3.3 and c.get(x, y) == '1': c.px(x, y, 'f')
    c.rect(0, 11, 13, 13, 'b'); c.rect(3, 9, 3, 11, 'L'); c.rect(10, 9, 10, 11, 'L')
    c.rect(6, 4, 7, 9, '2'); c.px(6, 4, '4')
    return c
