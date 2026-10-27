import json, os
from PIL import Image
A=os.path.join(os.path.dirname(os.path.abspath(__file__)),'..','..')+'/src/main/resources/assets/noxis_culture/'
N=53
T=A+'textures/block/noxis_painting/'
# textura de madera del marco y del caballete
im=Image.new('RGBA',(16,16))
for y in range(16):
    for x in range(16):
        c=(70,48,62) if (x+y*3)%7 else (54,36,48)
        if y%5==0: c=(86,60,76)
        im.putpixel((x,y),c+(255,))
im.save(T+'frame.png')
os.makedirs(A+'models/block/noxis_painting',exist_ok=True)
os.makedirs(A+'models/item/noxis_painting',exist_ok=True)
def face(tex,uv=None,cull=None):
    f={"texture":tex}
    if uv: f["uv"]=uv
    if cull: f["cullface"]=cull
    return f
for i in range(N):
    art="noxis_culture:block/noxis_painting/art_%02d"%i
    wall={"parent":"minecraft:block/block","textures":{"particle":art,"art":art,"frame":"noxis_culture:block/noxis_painting/frame"},
      "elements":[{"from":[0,0,15],"to":[16,16,16],"faces":{
        "north":face("#art",[0,0,16,16]),"south":face("#frame",[0,0,16,16],"south"),
        "east":face("#frame",[0,0,1,16]),"west":face("#frame",[0,0,1,16]),"up":face("#frame",[0,0,16,1]),"down":face("#frame",[0,0,16,1])}}]}
    json.dump(wall,open(A+'models/block/noxis_painting/wall_%02d.json'%i,'w'),indent=1)
    stand={"parent":"minecraft:block/block","textures":{"particle":art,"art":art,"frame":"noxis_culture:block/noxis_painting/frame"},
      "elements":[
        {"from":[3,0,6],"to":[4,11,7],"rotation":{"angle":-22.5,"axis":"x","origin":[3.5,0,6.5]},"faces":{k:face("#frame",[0,0,1,11]) for k in ["north","south","east","west"]}},
        {"from":[12,0,6],"to":[13,11,7],"rotation":{"angle":-22.5,"axis":"x","origin":[12.5,0,6.5]},"faces":{k:face("#frame",[0,0,1,11]) for k in ["north","south","east","west"]}},
        {"from":[7.5,0,10],"to":[8.5,12,11],"rotation":{"angle":22.5,"axis":"x","origin":[8,0,10.5]},"faces":{k:face("#frame",[0,0,1,12]) for k in ["north","south","east","west"]}},
        {"from":[2,3,4],"to":[14,4,6],"faces":{k:face("#frame",[0,0,12,1]) for k in ["north","south","up","down"]}|{"east":face("#frame",[0,0,2,1]),"west":face("#frame",[0,0,2,1])}},
        {"from":[2,4,5],"to":[14,16,6],"rotation":{"angle":-22.5,"axis":"x","origin":[8,4,5.5]},"faces":{
            "north":face("#art",[0,0,16,16]),"south":face("#frame",[0,0,12,12]),"east":face("#frame",[0,0,1,12]),"west":face("#frame",[0,0,1,12]),"up":face("#frame",[0,0,12,1]),"down":face("#frame",[0,0,12,1])}}]}
    json.dump(stand,open(A+'models/block/noxis_painting/standing_%02d.json'%i,'w'),indent=1)
    json.dump({"parent":"minecraft:item/generated","textures":{"layer0":art}},open(A+'models/item/noxis_painting/art_%02d.json'%i,'w'),indent=1)
# blockstate
rot={"north":0,"east":90,"south":180,"west":270}
variants={}
for i in range(N):
    for f,r in rot.items():
        for st in ("false","true"):
            m="noxis_culture:block/noxis_painting/%s_%02d"%("standing" if st=="true" else "wall",i)
            v={"model":m}
            if r: v["y"]=r
            variants["art=%d,facing=%s,standing=%s"%(i,f,st)]=v
json.dump({"variants":variants},open(A+'blockstates/noxis_painting.json','w'),indent=1)
# definición del ítem: dibujo según custom_model_data
entries=[{"threshold":float(i),"model":{"type":"minecraft:model","model":"noxis_culture:item/noxis_painting/art_%02d"%i}} for i in range(N)]
json.dump({"model":{"type":"minecraft:range_dispatch","property":"minecraft:custom_model_data","index":0,
  "entries":entries,"fallback":{"type":"minecraft:model","model":"noxis_culture:item/noxis_painting/art_00"}}},
  open(A+'items/noxis_painting.json','w'),indent=1)
print('ok',len(variants))
