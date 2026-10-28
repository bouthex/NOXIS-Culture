import sys, importlib.util
from PIL import Image, ImageDraw
sys.path.insert(0,'/home/claude/e3/art')
from pal import rgb
def load():
    spec=importlib.util.spec_from_file_location('arts','/home/claude/e3/art/arts.py'); m=importlib.util.module_from_spec(spec); spec.loader.exec_module(m); return m.ART
FR=[(0x2a,0x1c,0x26),(0x46,0x30,0x3e),(0x6e,0x4c,0x5e)]
def tile(rows):
    assert len(rows)==14, len(rows)
    im=Image.new('RGB',(16,16),FR[1])
    for y,r in enumerate(rows):
        assert len(r)==14,(y,r,len(r))
        for x,ch in enumerate(r): im.putpixel((x+1,y+1),rgb(ch))
    return im
def sheet(idx, out, Z=12):
    A=load(); sel=[i for i in idx]
    W=len(sel)
    s=Image.new('RGB',(W*(16*Z+8)+8, 16*Z+16+40),(40,36,44)); d=ImageDraw.Draw(s)
    for k,i in enumerate(sel):
        f,name,rows=A[i]; t=tile(rows)
        x=8+k*(16*Z+8); s.paste(t.resize((16*Z,16*Z),Image.NEAREST),(x,8))
        s.paste(t.resize((32,32),Image.NEAREST),(x,16*Z+12)); s.paste(t,(x+40,16*Z+12))
        d.text((x+60,16*Z+14),f'{i} {name}',fill=(230,230,230))
    s.save(out)
if __name__=='__main__':
    a=sys.argv[1]; lo,hi=map(int,a.split('-')); sheet(range(lo,hi+1), sys.argv[2])
