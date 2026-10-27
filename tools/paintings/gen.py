# Colección de pinturas Noxis: 53 obras originales de 16x16 (marco de 1 px + lienzo de 14x14).
import random, math, os
from PIL import Image
OUT=os.path.join(os.path.dirname(os.path.abspath(__file__)),'..','..')+'/src/main/resources/assets/noxis_culture/textures/block/noxis_painting/'
import os; os.makedirs(OUT,exist_ok=True)
W=14
def C(h): return tuple(int(h[i:i+2],16) for i in (0,2,4))
class Cv:
    def __init__(s,bg=(0,0,0)): s.p=[[bg for _ in range(W)] for _ in range(W)]
    def px(s,x,y,c):
        if 0<=x<W and 0<=y<W: s.p[y][x]=c
    def rect(s,x0,y0,x1,y1,c):
        for y in range(y0,y1+1):
            for x in range(x0,x1+1): s.px(x,y,c)
    def vgrad(s,cols,y0=0,y1=W-1):
        n=y1-y0+1
        for y in range(y0,y1+1):
            t=(y-y0)/max(1,n-1)*(len(cols)-1); i=min(int(t),len(cols)-2); f=t-i
            # sin mezclas suaves: bandas (pixel art)
            c=cols[i] if f<0.5 else cols[i+1]
            for x in range(W): s.p[y][x]=c
    def hill(s,h_fn,c,shade=None):
        for x in range(W):
            top=int(round(h_fn(x)))
            for y in range(max(0,top),W):
                s.px(x,y,c)
            if shade and 0<=top<W: s.px(x,top,shade)
    def circle(s,cx,cy,r,c):
        for y in range(W):
            for x in range(W):
                if (x-cx)**2+(y-cy)**2<=r*r+0.3: s.px(x,y,c)
    def get(s,x,y): return s.p[y][x]
FRAME_D=C('2a1c26'); FRAME=C('46303e'); FRAME_L=C('6e4c5e')
def save(cv,i):
    im=Image.new('RGBA',(16,16))
    for y in range(16):
        for x in range(16):
            if x in (0,15) or y in (0,15):
                c=FRAME_L if (x==0 or y==0) and not (x==15 or y==15) else FRAME_D if (x==15 or y==15) else FRAME
                im.putpixel((x,y),c+(255,))
            else:
                im.putpixel((x,y),cv.p[y-1][x-1]+(255,))
    im.save(OUT+'art_%02d.png'%i)
    return im
R=random.Random(7)
designs=[]  # (familia, función)
def design(fam):
    def deco(f): designs.append((fam,f)); return f
    return deco
# ---------------- paleta compartida
SKY_D=[C('2b3d74'),C('3d5aa0'),C('6f8fd0'),C('a6c3ef')]
NIGHT=[C('0d0b1c'),C('16142e'),C('221d44')]
GRASS=C('5d9c3a'); GRASS_D=C('3f7428'); DIRT=C('6b4a2e'); STONE=C('7c7c84'); STONE_D=C('5a5a62')
VIO=C('8a4fd0'); VIO_L=C('c79bf0'); VIO_D=C('4b2a7a'); YEL=C('f0c840'); YEL_L=C('fff3a0')
WOOD=C('7a5232'); WOOD_D=C('4e3420'); WATER=C('3a64b8'); WATER_L=C('6f98e0'); SAND=C('d9c78a'); SNOW=C('eef2f8')
BLACK=C('17121d'); NOX=C('1d1724'); EYE=C('f6d24a')
# ======================= 0: recuerdos tiernos (flores, bichitos, luces cálidas)
@design(0)
def flowers_yellow(cv):
    cv.vgrad([C('9fc7ef'),C('b7d6f2')],0,7); cv.rect(0,8,13,13,GRASS); cv.rect(0,8,13,8,GRASS_D)
    for x,h in [(2,4),(6,3),(10,5)]:
        cv.rect(x,13-h,x,12,GRASS_D)
        for dx,dy in [(-1,0),(1,0),(0,-1),(0,1)]: cv.px(x+dx,13-h-1+dy,YEL)
        cv.px(x,13-h-1,C('b8862a'))
@design(0)
def flowers_violet(cv):
    cv.vgrad([C('e6d6f2'),C('f2e6f8')],0,6); cv.rect(0,7,13,13,C('6aa84a'))
    for x,y in [(3,6),(8,5),(11,7),(5,9)]:
        cv.rect(x,y+1,x,12,GRASS_D); cv.px(x,y,VIO_L); cv.px(x-1,y,VIO); cv.px(x+1,y,VIO); cv.px(x,y-1,VIO); cv.px(x,y+1,VIO_D)
@design(0)
def single_flower(cv):
    cv.vgrad([C('f3e3c6'),C('f8eedb')]); cv.rect(6,6,7,13,GRASS_D); cv.px(5,9,GRASS); cv.px(4,8,GRASS); cv.px(8,10,GRASS); cv.px(9,9,GRASS)
    for dx,dy in [(-2,0),(2,0),(0,-2),(0,2),(-1,-1),(1,-1),(-1,1),(1,1)]: cv.px(6+dx,4+dy,YEL); cv.px(7+dx,4+dy,YEL)
    cv.rect(6,3,7,4,C('b07020'))
@design(0)
def fireflies(cv):
    cv.vgrad([C('101b30'),C('142640'),C('1b3048')]); cv.hill(lambda x:10+math.sin(x*0.6)*0.8,C('1f3a26'),C('2b4d33'))
    for x,y in [(2,4),(5,7),(9,3),(11,6),(7,5),(3,9),(12,9)]:
        cv.px(x,y,YEL_L); cv.px(x,y+1,C('8a8a30'))
@design(0)
def mushroom(cv):
    cv.vgrad([C('c9e0c0'),C('dcebd2')],0,8); cv.rect(0,9,13,13,C('4f8a36'))
    cv.rect(4,4,9,6,C('c33a2e')); cv.rect(3,6,10,6,C('c33a2e')); cv.px(5,5,SNOW); cv.px(8,4,SNOW); cv.px(4,6,SNOW)
    cv.rect(6,7,7,10,C('eadcc0'))
@design(0)
def lantern(cv):
    cv.vgrad(NIGHT); cv.rect(6,1,7,2,STONE_D); cv.rect(5,3,8,3,BLACK); cv.rect(5,4,8,8,C('e8a640')); cv.rect(6,5,7,7,YEL_L); cv.rect(5,9,8,9,BLACK)
    for x,y in [(3,5),(10,6),(4,9),(9,10)]: cv.px(x,y,C('5a4020'))
@design(0)
def cat_window(cv):
    cv.rect(0,0,13,13,C('6a4a3a')); cv.rect(2,2,11,9,C('8ab4e8')); cv.rect(2,6,11,6,C('6a4a3a')); cv.rect(6,2,7,9,C('6a4a3a'))
    cv.rect(0,10,13,13,C('4a3228'))
    cv.rect(4,8,8,10,BLACK); cv.px(4,7,BLACK); cv.px(8,7,BLACK); cv.px(5,8,EYE); cv.px(7,8,EYE); cv.rect(9,10,10,10,BLACK)
@design(0)
def butterflies(cv):
    cv.vgrad([C('fbe9d0'),C('f4dcc0')]); 
    for x,y,c in [(3,4,VIO),(9,3,YEL),(6,9,C('e07090')),(11,10,VIO_L)]:
        cv.px(x-1,y,c); cv.px(x+1,y,c); cv.px(x-1,y+1,c); cv.px(x+1,y+1,c); cv.px(x,y,BLACK); cv.px(x,y+1,BLACK)
# ======================= 1: paisajes grises y agotados (Veyra: gris, gastado, restos de luz)
G1=[C('3a3a40'),C('4a4a52'),C('5e5e66'),C('74747c')]
@design(1)
def grey_hills(cv):
    cv.vgrad([C('8a8a90'),C('9c9ca2')],0,7); cv.hill(lambda x:7+math.sin(x*0.5)*1.5,G1[2],G1[3]); cv.hill(lambda x:10+math.cos(x*0.7),G1[1],G1[2])
@design(1)
def faint_light(cv):
    cv.vgrad([C('3c3c44'),C('4a4a52'),C('56565e')]); cv.hill(lambda x:11,G1[0],G1[1]); cv.px(7,9,C('c8b0f0')); cv.px(7,10,C('8a70b8')); cv.px(6,10,C('6a5a88')); cv.px(8,10,C('6a5a88'))
@design(1)
def broken_arch(cv):
    cv.vgrad([C('7a7a82'),C('8c8c94')]); cv.rect(0,11,13,13,G1[1])
    cv.rect(3,4,4,10,G1[0]); cv.rect(9,6,10,10,G1[0]); cv.rect(4,3,7,3,G1[0]); cv.rect(5,2,6,2,G1[0]); cv.px(10,5,G1[0])
@design(1)
def dead_tree(cv):
    cv.vgrad([C('6c6c74'),C('80808a')]); cv.rect(0,11,13,13,G1[0]); cv.rect(6,4,7,10,C('3a3236')); cv.px(5,5,C('3a3236')); cv.px(4,4,C('3a3236')); cv.px(8,6,C('3a3236')); cv.px(9,5,C('3a3236')); cv.px(9,4,C('3a3236'))
@design(1)
def dust_plain(cv):
    cv.vgrad([C('9a948c'),C('a8a298')],0,8); cv.rect(0,9,13,13,C('7a746c')); 
    for x in range(0,14,3): cv.px(x,10+(x%2),C('6a645c'))
    cv.px(11,3,C('d8d2c8'))
@design(1)
def last_crystal(cv):
    cv.vgrad([C('2e2e36'),C('3a3a42')]); cv.hill(lambda x:10+abs(x-7)*0.4,G1[1],G1[2]); cv.rect(6,6,7,9,VIO_D); cv.px(6,6,VIO); cv.px(7,7,VIO_L); cv.px(6,5,VIO_D)
@design(1)
def grey_lake(cv):
    cv.vgrad([C('7c7c86'),C('8c8c96')],0,6); cv.hill(lambda x:6+math.sin(x*0.9)*0.7,G1[1]); cv.rect(0,9,13,13,C('5c6470')); cv.rect(2,10,5,10,C('6c7480')); cv.rect(8,12,11,12,C('6c7480'))
@design(1)
def worn_stairs(cv):
    cv.vgrad([C('5a5a62'),C('686870')]); 
    for i in range(5): cv.rect(1+i*2,12-i*2,13,13-i*2+1,G1[i%2+1])
    cv.px(12,2,C('b8a0e0'))
# ======================= 2: paisajes y expediciones
@design(2)
def green_hills_sun(cv):
    cv.vgrad(SKY_D[::-1][:3][::-1]+[SKY_D[3]],0,8); cv.circle(10,3,1.6,YEL); cv.hill(lambda x:8+math.sin(x*0.5)*1.4,GRASS,C('7ab84c')); cv.hill(lambda x:11+math.cos(x*0.6)*0.8,GRASS_D,GRASS)
@design(2)
def mountains_lake(cv):
    cv.vgrad([C('5a7ac8'),C('8aa8e0'),C('b8cdee')],0,8); cv.hill(lambda x:3+abs(x-5)*1.2,STONE,SNOW); cv.hill(lambda x:5+abs(x-11)*1.3,STONE_D,SNOW); cv.rect(0,9,13,13,WATER); cv.rect(2,10,6,10,WATER_L); cv.rect(8,12,12,12,WATER_L)
@design(2)
def forest(cv):
    cv.vgrad([C('9cc8e8'),C('b8daf0')],0,7); cv.rect(0,11,13,13,GRASS_D)
    for x in [1,4,7,10,12]:
        h=R.randint(4,6); cv.rect(x,11-h,x,11,WOOD_D)
        cv.rect(x-1,11-h-2,x+1,11-h+1,C('2f6e2a')); cv.px(x,11-h-3,C('2f6e2a')); cv.px(x-1,11-h-2,C('48913c'))
@design(2)
def sunset_sea(cv):
    cv.vgrad([C('3a2a6a'),C('a04a7a'),C('f08a50'),C('f8c060')],0,8); cv.rect(4,7,9,8,C('fde08a')); cv.rect(0,9,13,13,C('2a3a78')); cv.rect(3,10,10,10,C('c86a6a')); cv.rect(5,12,8,12,C('8a5a8a'))
@design(2)
def desert(cv):
    cv.vgrad([C('8ec4f0'),C('c0e0f8')],0,7); cv.hill(lambda x:8+math.sin(x*0.45)*1.5,SAND,C('ece0a8')); cv.rect(10,5,10,9,C('4a8a3a')); cv.px(9,7,C('4a8a3a')); cv.px(11,6,C('4a8a3a'))
@design(2)
def nether(cv):
    cv.vgrad([C('3a0c10'),C('5a1418'),C('7a2018')]); cv.hill(lambda x:9+math.sin(x*0.8),C('6a2a2a'),C('8a3a32')); cv.rect(0,12,13,13,C('e86a18')); cv.rect(3,12,6,12,C('f8b040')); cv.rect(10,2,10,8,C('f8b040'))
@design(2)
def the_end(cv):
    cv.vgrad([C('120c1a'),C('1a1226')]); cv.hill(lambda x:10+abs(x-7)*0.3,C('dcd8a0'),C('ece8b8')); cv.rect(3,4,4,10,C('2a1a38')); cv.rect(9,6,10,10,C('2a1a38')); 
    for x,y in [(1,2),(7,1),(12,4),(6,6)]: cv.px(x,y,C('b8a0e0'))
@design(2)
def island(cv):
    cv.vgrad([C('7ab8ee'),C('a8d4f6')],0,7); cv.rect(0,8,13,13,WATER); cv.hill(lambda x:7+abs(x-7)*0.8 if 3<=x<=11 else 20,SAND,GRASS); cv.rect(7,3,7,6,WOOD); cv.rect(5,2,9,3,C('3a8a3a')); cv.rect(2,11,4,11,WATER_L)
# ======================= 3: arquitectura y lugares
@design(3)
def tower(cv):
    cv.vgrad([C('9ab8e0'),C('b8d0ec')]); cv.rect(0,12,13,13,GRASS); cv.rect(5,3,8,11,STONE); cv.rect(4,2,9,3,STONE_D); cv.px(4,1,STONE_D); cv.px(6,1,STONE_D); cv.px(9,1,STONE_D); cv.rect(6,5,7,6,YEL); cv.rect(6,9,7,11,WOOD_D)
@design(3)
def bridge(cv):
    cv.vgrad([C('8ab0e0'),C('a8c8ec')],0,7); cv.rect(0,10,13,13,WATER); cv.rect(0,6,13,6,WOOD); cv.rect(0,7,13,7,WOOD_D)
    for x in [2,6,10]: cv.rect(x,8,x,11,WOOD_D)
@design(3)
def house_lit(cv):
    cv.vgrad(NIGHT); cv.rect(0,11,13,13,C('1f3020')); cv.rect(3,6,10,11,WOOD); cv.rect(2,5,11,5,C('5a2e2a')); cv.rect(3,4,10,4,C('5a2e2a')); cv.rect(5,3,8,3,C('5a2e2a')); cv.rect(5,7,6,8,YEL); cv.rect(8,9,9,11,WOOD_D)
    for x,y in [(1,1),(12,2),(9,0)]: cv.px(x,y,SNOW)
@design(3)
def pillars(cv):
    cv.vgrad([C('d8cfe8'),C('e8e0f2')]); cv.rect(0,12,13,13,C('a89cb8'))
    for x in [2,6,10]: cv.rect(x,3,x+1,11,C('f2eef8')); cv.rect(x-1,2,x+2,2,C('c8bcd8')); cv.px(x+1,4,C('c8bcd8'))
@design(3)
def stone_gate(cv):
    cv.vgrad([C('5c7aa8'),C('7a98c4')]); cv.rect(0,12,13,13,GRASS_D); cv.rect(2,3,11,11,STONE_D); cv.rect(4,5,9,11,C('1a1622')); cv.rect(5,4,8,4,C('1a1622')); cv.px(6,7,VIO); cv.px(7,8,VIO_L)
@design(3)
def windmill(cv):
    cv.vgrad([C('a8d0f0'),C('c8e2f8')]); cv.rect(0,12,13,13,GRASS); cv.rect(5,6,8,11,C('e8dcc0')); cv.rect(6,9,7,11,WOOD_D)
    for (dx,dy) in [(0,-1),(1,0),(0,1),(-1,0)]:
        for k in range(1,4): cv.px(6+dx*k+(1 if dx>0 else 0),5+dy*k,WOOD)
@design(3)
def fishbowl_home(cv):
    cv.vgrad([C('f0e2c8'),C('f6ead4')]); cv.rect(0,11,13,13,WOOD); cv.rect(3,5,10,10,C('b8d8e8')); cv.rect(3,4,10,4,WOOD); cv.rect(2,10,11,10,WOOD_D)
    cv.rect(5,7,8,9,NOX); cv.px(6,7,EYE); cv.px(8,7,EYE)
@design(3)
def lighthouse(cv):
    cv.vgrad([C('1a2448'),C('2a3a68')]); cv.rect(0,11,13,13,C('20406a')); cv.rect(6,4,7,10,SNOW); cv.rect(6,6,7,6,C('c83a3a')); cv.rect(6,8,7,8,C('c83a3a')); cv.rect(5,2,8,3,YEL); cv.rect(9,2,13,2,C('f8e8a0')); cv.rect(0,3,4,3,C('f8e8a0'))
# ======================= 4: formas inquietantes (sombras, figuras sin rasgos, grietas de luz)
@design(4)
def eyes_dark(cv):
    cv.rect(0,0,13,13,C('0a080e')); 
    for x,y in [(3,4),(9,6),(5,10)]: cv.px(x,y,EYE); cv.px(x+2,y,EYE)
@design(4)
def faceless(cv):
    cv.vgrad([C('3a3444'),C('2a2434')]); cv.rect(5,3,8,6,C('121016')); cv.rect(4,7,9,13,C('121016')); cv.px(3,9,C('121016')); cv.px(10,9,C('121016'))
@design(4)
def crack_light(cv):
    cv.rect(0,0,13,13,C('1a1620')); 
    pts=[(7,0),(7,2),(6,4),(7,6),(8,8),(7,10),(6,12),(6,13)]
    for x,y in pts: cv.px(x,y,VIO_L); cv.px(x+1,y,VIO)
@design(4)
def spiral(cv):
    cv.rect(0,0,13,13,C('241c30')); 
    for t in range(0,60):
        a=t*0.32; r=t*0.1; x=int(round(6.5+math.cos(a)*r)); y=int(round(6.5+math.sin(a)*r)); cv.px(x,y,C('8a6ab8'))
@design(4)
def long_shadow(cv):
    cv.vgrad([C('c8b8a0'),C('b0a088')]); cv.rect(0,10,13,13,C('8a7a66')); cv.rect(2,6,3,10,NOX); cv.rect(2,5,3,5,NOX); 
    for i in range(10): cv.px(3+i,11+(i//4),C('4a4038'))
@design(4)
def deep_hole(cv):
    cv.vgrad([C('5a5a62'),C('4a4a52')]); cv.circle(6.5,7,4.2,C('0e0c12')); cv.circle(6.5,7,2.2,C('060508')); cv.px(6,7,VIO_D)
@design(4)
def red_moon(cv):
    cv.vgrad([C('120a10'),C('1e1018')]); cv.circle(7,5,2.6,C('9a2a2a')); cv.px(6,4,C('c84a40')); cv.hill(lambda x:11+math.sin(x)*0.6,C('0a0608'))
@design(4)
def hands(cv):
    cv.rect(0,0,13,13,C('2a2232')); 
    for x in [2,5,8,11]: cv.rect(x,6,x,13,C('0e0c12')); cv.px(x-1,6,C('0e0c12')); cv.px(x+1,7,C('0e0c12'))
# ======================= 5: ceremonial (simetría, formas cristalinas, gema en lágrima)
def sym(cv,f):
    for y in range(W):
        for x in range(7): c=f(x,y); cv.px(x,y,c); cv.px(13-x,y,c)
@design(5)
def mandala(cv):
    sym(cv,lambda x,y: VIO_L if abs(x-6.5)+abs(y-6.5)<2.5 else VIO if abs(x-6.5)+abs(y-6.5)<5 else VIO_D if (x+y)%3==0 else C('241a36'))
@design(5)
def teardrop_gem(cv):
    cv.rect(0,0,13,13,C('1c1428'))
    shape=["......X.......","......XX......",".....XXX......",".....XXXX.....","....XXXXX.....","....XXXXXX....","...XXXXXXX....","...XXXXXXXX...","...XXXXXXXX...","....XXXXXX....",".....XXXX....."]
    for j,row in enumerate(shape):
        for i,ch in enumerate(row):
            if ch=='X': cv.px(i,j+1,VIO if i<7 else VIO_D)
    cv.px(5,6,VIO_L); cv.px(5,7,VIO_L); cv.px(4,8,VIO_L); cv.px(6,3,C('e8d0ff'))
@design(5)
def diamonds(cv):
    sym(cv,lambda x,y: C('e8d8a8') if (x+y)%4==0 or (x-y)%4==0 else C('3a2a54'))
@design(5)
def crystal_cluster(cv):
    cv.vgrad([C('201830'),C('2a2040')]); 
    for x,h in [(3,4),(5,7),(7,9),(9,6),(11,4)]: cv.rect(x,13-h,x+1,12,VIO_D); cv.rect(x,13-h,x,12,VIO); cv.px(x,13-h,VIO_L)
@design(5)
def concentric(cv):
    for y in range(W):
        for x in range(W):
            d=max(abs(x-6.5),abs(y-6.5)); cv.px(x,y,[C('f0d860'),C('6a3aa8'),C('241a36'),C('8a5ad0'),C('1a1228'),C('c8a0f0'),C('120c1c')][int(d)%7])
@design(5)
def star_sign(cv):
    cv.rect(0,0,13,13,C('120e20'))
    for i in range(-5,6): cv.px(6+i,6,YEL); cv.px(6,6+i,YEL); cv.px(7+i,7,YEL); cv.px(7,7+i,YEL)
    cv.rect(5,5,8,8,YEL_L)
@design(5)
def noxus_ore_study(cv):
    cv.rect(0,0,13,13,C('6c6c74'))
    for x,y in [(2,3),(8,2),(5,8),(10,9),(3,11)]: cv.px(x,y,VIO); cv.px(x+1,y,VIO_D); cv.px(x,y+1,VIO_D); cv.px(x,y-1,VIO_L)
@design(5)
def twin_lights(cv):
    sym(cv,lambda x,y: YEL_L if (x-3)**2+(y-6)**2<3 else C('e0a840') if (x-3)**2+(y-6)**2<7 else C('1a1424') if y<10 else C('2a2034'))
# ======================= extras: cielos y Noxis (se reparten entre familias)
@design(0)
def noxis_portrait(cv):
    cv.vgrad([C('d8c8f0'),C('c8b4ea')]); cv.rect(3,6,10,12,NOX); cv.rect(4,13,9,13,NOX); cv.rect(5,1,8,5,C('1a1222')); cv.rect(3,5,10,5,C('1a1222')); cv.rect(5,4,8,4,C('7a3ab8')); cv.px(6,4,C('d8b0ff'))
    cv.px(5,8,EYE); cv.px(8,8,EYE); cv.px(5,9,C('9a7a20')); cv.px(8,9,C('9a7a20')); cv.px(2,6,NOX); cv.px(11,6,NOX)
@design(2)
def night_moon(cv):
    cv.vgrad(NIGHT); cv.circle(9,3,1.7,C('f2f0d8')); cv.px(10,3,C('d8d4b8')); cv.hill(lambda x:10+math.sin(x*0.5),C('14261a'),C('1e3524'))
    for x,y in [(1,1),(3,4),(6,2),(12,6),(5,6),(2,7)]: cv.px(x,y,SNOW)
@design(2)
def starry_noxis(cv):
    cv.vgrad(NIGHT); cv.rect(0,11,13,13,C('14261a'))
    for x,y in [(1,1),(4,2),(8,1),(11,3),(6,4),(2,5),(12,0)]: cv.px(x,y,SNOW)
    for bx in [3,8]:
        cv.rect(bx,8,bx+2,10,NOX); cv.rect(bx,6,bx+2,7,C('1a1222')); cv.px(bx,9,EYE); cv.px(bx+2,9,EYE)
@design(4)
def watching_from_dark(cv):
    cv.vgrad([C('0e0c14'),C('16121e')]); cv.rect(0,12,13,13,C('1e1a26')); cv.px(10,8,EYE); cv.px(12,8,EYE); cv.rect(10,6,12,6,C('241c30'))
@design(3)
def bowl_under_stars(cv):
    cv.vgrad(NIGHT); cv.rect(0,11,13,13,WOOD); cv.rect(4,6,9,10,C('6a8aa8')); cv.rect(4,5,9,5,WOOD); cv.rect(6,8,7,9,NOX); 
    for x,y in [(1,1),(11,2),(7,0),(2,4)]: cv.px(x,y,SNOW)
print("diseños:",len(designs))
fams=[f for f,_ in designs]
ims=[]
for i,(fam,fn) in enumerate(designs):
    cv=Cv(); fn(cv); ims.append(save(cv,i))
open('families.txt','w').write(','.join(map(str,fams)))
print(fams)
sheet=Image.new("RGBA",(12*18,5*18),(40,40,40,255))
for i,im in enumerate(ims): sheet.paste(im,((i%12)*18,(i//12)*18))
sheet.resize((12*18*5,5*18*5),Image.NEAREST).save('sheet.png')
