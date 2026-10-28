# Bloque de Noxus: se conserva su identidad (marco biselado violeta, cuatro facetas que convergen en el
# "ojo" de luz del centro) y se refina el material: facetas con luz coherente (arriba a la izquierda),
# aristas marcadas, vetas finas con inclusiones, y un ojo más contenido con halo suave.
from PIL import Image
import math

P = {'0': '1c0f2e', '1': '2e1a4b', '2': '402463', '3': '543083', '4': '6b41a3', '5': '885bc2',
     '6': 'a985de', '7': 'd7c6f2', 'e': 'e682e0', 'f': 'a65ccc', 'w': 'fbeefc'}

def rgb(c):
    h = P[c]; return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)

def side(cracks, inclusions):
    g = [['3'] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            cx, cy = x - 7.5, y - 7.5
            # faceta según el triángulo (arriba / izquierda / derecha / abajo)
            if abs(cy) >= abs(cx): f = '5' if cy < 0 else '2'
            else: f = '4' if cx < 0 else '3'
            g[y][x] = f
            # aristas diagonales
            if abs(abs(cx) - abs(cy)) < 0.6:
                g[y][x] = '6' if (cx < 0 and cy < 0) else ('1' if (cx > 0 and cy > 0) else ('5' if cy < 0 else '2'))
    # halo alrededor del ojo
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 2.2 < d < 3.4: g[y][x] = str(min(7, int(g[y][x]) + 1)) if g[y][x].isdigit() else g[y][x]
    # ojo
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 1.2: g[y][x] = 'e'
            elif d < 2.2: g[y][x] = 'f' if (x + y) % 2 else '6'
    g[7][7] = 'w'
    # vetas e inclusiones
    for (x, y) in cracks:
        if g[y][x] not in 'efw': g[y][x] = '1' if g[y][x] in '2345' else '2'
    for (x, y) in inclusions: g[y][x] = '7'
    # bisel exterior: luz arriba/izquierda, sombra abajo/derecha
    for i in range(16):
        g[0][i] = '6'; g[i][0] = '6'
        g[15][i] = '0'; g[i][15] = '0'
        if 0 < i < 15:
            g[1][i] = '5' if g[1][i] != '6' else '6'; g[i][1] = '5' if g[i][1] != '6' else '6'
            g[14][i] = '1'; g[i][14] = '1'
    g[0][15] = '3'; g[15][0] = '3'
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16): im.putpixel((x, y), rgb(g[y][x]))
    return im

def top():
    g = [['3'] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            k = min(x, y, 15 - x, 15 - y)
            lit = x + y < 15
            if k == 0: c = '6' if lit else '0'
            elif k == 1: c = '5' if lit else '1'
            elif k == 2: c = '3'
            elif k == 3: c = '2' if lit else '5'
            elif k == 4: c = '4' if lit else '3'
            else: c = '4' if (x + y) % 7 else '5'
            g[y][x] = c
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 1.2: g[y][x] = 'e'
            elif d < 2.2: g[y][x] = 'f' if (x + y) % 2 else '6'
    g[7][7] = 'w'
    for (x, y) in [(5, 10), (10, 5), (11, 11)]: g[y][x] = '6'
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16): im.putpixel((x, y), rgb(g[y][x]))
    return im

CRACKS_A = [(3, 4), (4, 5), (4, 6), (11, 10), (12, 11), (12, 12), (10, 3), (9, 4)]
INCL_A = [(5, 3), (12, 6), (3, 11)]
CRACKS_B = [(4, 11), (5, 12), (5, 13), (10, 4), (11, 4), (12, 3), (3, 7), (2, 8)]
INCL_B = [(11, 2), (4, 4), (12, 12)]
