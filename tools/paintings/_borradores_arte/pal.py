# Paleta común de Noxis Culture (rampas con sombras hacia el violeta/azul y luces hacia el amarillo).
PAL = {
 'a':'0b0a17','b':'16142e','c':'232050','d':'34327a',
 '1':'2a1745','2':'4b2a7a','3':'7444b0','4':'a774e0','5':'d8b4ff',
 'e':'6b4a12','f':'a8761c','g':'e0a83a','h':'f6d860','i':'fff4b8',
 'j':'1c3324','k':'2d5236','l':'467a3e','m':'6fa04c','n':'a3c867',
 'o':'3e5e9c','p':'5d82c4','q':'8fb0e0','r':'c4d8f0',
 's':'5a2440','t':'a03a4a','u':'e0704a','v':'f4a860',
 'w':'2e1e1a','x':'4e3424','y':'7a5234','z':'a8784a',
 '6':'1e1d22','7':'34333a','8':'4e4c55','9':'6e6b74','0':'96929a','-':'c2bec4',
 'A':'1a2e5a','B':'2a4f8e','C':'4f7fc4','D':'8fbfe8',
 'E':'3d1018','F':'7a1f2a','G':'c0392b',
 'H':'d8cfb8','I':'efe8d4','J':'fbf8f0',
 'K':'0f0d14','L':'231d2e',
 'M':'b8955a','N':'e3cf96',
 'O':'3a0f12','P':'6e1e1e','Q':'ff8a1e','R':'ffd060',
 'S':'d9d8a4','T':'aaa872','U':'1c1028',
 'V':'b0408c','W':'f08ad0','X':'1e4a4e','Y':'3e8a7e','Z':'3b2a20',
}
def rgb(ch): h=PAL[ch]; return tuple(int(h[i:i+2],16) for i in (0,2,4))
