$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assetRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../assets/fonts'))
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.IO;
using System.Text;
using System.Drawing;
using System.Drawing.Text;
using System.Drawing.Imaging;
using System.Collections.Generic;

public static class TankzorsFontAtlas {
    public static void Generate(string root, string name, int size) {
        var codes = new List<int>();
        for (int c = 32; c <= 126; c++) codes.Add(c);
        for (int c = 192; c <= 255; c++) codes.Add(c);
        codes.AddRange(new int[]{258,259,272,273,296,297,360,361,416,417,431,432});
        for (int c = 7840; c <= 7929; c++) codes.Add(c);
        codes.AddRange(new int[]{8211,8212,8226,8592,8593,8594,8595});
        int cellW = 64, cellH = size + 24, columns = 16, height = 1;
        while (height < ((codes.Count + columns - 1) / columns) * cellH) height *= 2;
        using (var family = new PrivateFontCollection()) {
            family.AddFontFile(Path.Combine(root, "RobotoCondensed_Regular.ttf"));
            using (var font = new Font(family.Families[0], size, FontStyle.Regular, GraphicsUnit.Pixel))
            using (var bitmap = new Bitmap(1024, height, PixelFormat.Format32bppArgb))
            using (var graphics = Graphics.FromImage(bitmap))
            using (var format = (StringFormat)StringFormat.GenericTypographic.Clone()) {
                graphics.Clear(Color.Transparent);
                graphics.TextRenderingHint = TextRenderingHint.AntiAliasGridFit;
                format.FormatFlags |= StringFormatFlags.MeasureTrailingSpaces;
                int ascent = (int)Math.Ceiling(size * (double)font.FontFamily.GetCellAscent(FontStyle.Regular) / font.FontFamily.GetEmHeight(FontStyle.Regular));
                int lineHeight = (int)Math.Ceiling(font.GetHeight(graphics));
                var lines = new List<string>();
                lines.Add("info face=\"Roboto Condensed Vietnamese\" size=" + size + " bold=0 italic=0 charset=\"\" unicode=1 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=1,1");
                lines.Add("common lineHeight=" + lineHeight + " base=" + ascent + " scaleW=1024 scaleH=" + height + " pages=1 packed=0");
                lines.Add("page id=0 file=\"" + name + ".png\"");
                lines.Add("chars count=" + codes.Count);
                for (int i = 0; i < codes.Count; i++) {
                    string ch = Char.ConvertFromUtf32(codes[i]);
                    int x = (i % columns) * cellW, y = (i / columns) * cellH;
                    int advance = (int)Math.Ceiling(graphics.MeasureString(ch, font, cellW, format).Width);
                    graphics.DrawString(ch, font, Brushes.White, x + 4, y, format);
                    int left = cellW, top = cellH, right = -1, bottom = -1;
                    for (int py = 0; py < cellH; py++) for (int px = 0; px < cellW; px++) {
                        if (bitmap.GetPixel(x + px, y + py).A == 0) continue;
                        left = Math.Min(left, px); right = Math.Max(right, px);
                        top = Math.Min(top, py); bottom = Math.Max(bottom, py);
                    }
                    if (right < 0) { left = 4; top = ascent; }
                    int w = right < 0 ? 0 : right - left + 1, h = bottom < 0 ? 0 : bottom - top + 1;
                    lines.Add("char id=" + codes[i] + " x=" + (x + left) + " y=" + (y + top) + " width=" + w + " height=" + h + " xoffset=" + (left - 4) + " yoffset=" + top + " xadvance=" + advance + " page=0 chnl=15");
                }
                bitmap.Save(Path.Combine(root, name + ".png"), ImageFormat.Png);
                File.WriteAllLines(Path.Combine(root, name + ".fnt"), lines, new UTF8Encoding(false));
            }
        }
    }
}
'@
[TankzorsFontAtlas]::Generate($assetRoot, 'game-font', 16)
[TankzorsFontAtlas]::Generate($assetRoot, 'vietnamese-font', 32)
[TankzorsFontAtlas]::Generate($assetRoot, 'shop-font', 48)
