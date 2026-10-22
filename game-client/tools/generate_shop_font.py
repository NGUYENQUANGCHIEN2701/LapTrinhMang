"""Bake the Builds shop font for reflection-free desktop/GWT BitmapFont loading.
Run with Python 3 + Pillow; source font is copied unchanged from Builds/docs/html5.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
root = Path(__file__).resolve().parents[1] / 'assets' / 'fonts'
font = ImageFont.truetype(str(root / 'RobotoCondensed_Regular.ttf'), 48)
ascent, descent = font.getmetrics()
atlas = Image.new('RGBA', (1024, 512))
draw = ImageDraw.Draw(atlas)
lines = ['info face="Roboto Condensed" size=48 bold=0 italic=0 charset="" unicode=1 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=1,1',
         f'common lineHeight={ascent + descent} base={ascent} scaleW=1024 scaleH=512 pages=1 packed=0',
         'page id=0 file="shop-font.png"', 'chars count=95']
for index, code in enumerate(range(32, 127)):
    char = chr(code)
    left, top, right, bottom = font.getbbox(char, anchor='ls')
    x, y = (index % 16) * 64, (index // 16) * 80
    draw.text((x-left, y-top), char, font=font, fill='white', anchor='ls')
    advance = round(font.getlength(char) * (2 if char == ' ' else 1))
    lines.append(f'char id={code} x={x} y={y} width={right-left} height={bottom-top} xoffset={left} yoffset={ascent+top} xadvance={advance} page=0 chnl=15')
atlas.save(root / 'shop-font.png')
(root / 'shop-font.fnt').write_text('\n'.join(lines) + '\n')
