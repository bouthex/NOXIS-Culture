import math
W=14
class C:
    def __init__(s, bg='a'): s.g=[[bg]*W for _ in range(W)]
    def px(s,x,y,c):
        if 0<=x<W and 0<=y<W and c not in ' .': s.g[y][x]=c
    def get(s,x,y): return s.g[y][x]
    def rect(s,x0,y0,x1,y1,c):
        for y in range(y0,y1+1):
            for x in range(x0,x1+1): s.px(x,y,c)
    def bands(s,spec):
        # spec: lista de (fila_hasta_inclusive, color)
        y=0
        for yy,c in spec:
            while y<=yy and y<W:
                for x in range(W): s.g[y][x]=c
                y+=1
    def dither_row(s,y,c1,c2,phase=0):
        for x in range(W): s.px(x,y,c1 if (x+phase)%2 else c2)
    def ellipse(s,cx,cy,rx,ry,c):
        for y in range(W):
            for x in range(W):
                if ((x-cx)/rx)**2+((y-cy)/ry)**2<=1.0: s.px(x,y,c)
    def ring(s,cx,cy,r0,r1,c):
        for y in range(W):
            for x in range(W):
                d=math.hypot(x-cx,y-cy)
                if r0<=d<r1: s.px(x,y,c)
    def ground(s,heights,c,top=None,below=None):
        # heights: alto (fila superior) por columna
        for x,h in enumerate(heights):
            for y in range(h,W if below is None else below): s.px(x,y,c)
            if top: s.px(x,h,top)
    def stamp(s,x0,y0,pat):
        for dy,row in enumerate(pat):
            for dx,ch in enumerate(row): s.px(x0+dx,y0+dy,ch)
    def rows(s): return [''.join(r) for r in s.g]

def line(c, x0, y0, x1, y1, ch):
    dx, dy = abs(x1 - x0), -abs(y1 - y0); sx = 1 if x0 < x1 else -1; sy = 1 if y0 < y1 else -1; err = dx + dy
    while True:
        c.px(x0, y0, ch)
        if x0 == x1 and y0 == y1: break
        e2 = 2 * err
        if e2 >= dy: err += dy; x0 += sx
        if e2 <= dx: err += dx; y0 += sy
