"""Compose real MCP views and enlarged original pixels for review. Does not draw game artwork."""
from pathlib import Path
import json
import shutil

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'models/previews/hospitality'
FONT = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 22)
TITLE = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 28)
BACKGROUND = (25, 20, 24)
GOLD = (242, 210, 122)
NAMES = ['dining_chair', 'serving_tray', 'table_lamp', 'bottle_shelf']
LABELS = ['Обеденный стул', 'Сервировочный поднос', 'Настольная лампа', 'Полка с бутылками']


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    results = json.loads((ROOT / 'build/diagnostics/hospitality-review-results.json').read_text(encoding='utf-8'))
    validation = []
    for index, (name, label) in enumerate(zip(NAMES, LABELS)):
        summary = json.loads(results[index * 3]['content'][0]['text'])['summary']
        validation.append(dict(model=name, **summary))
        views = []
        for content in results[index * 3 + 1]['content']:
            if content['type'] == 'text' and ': ' in content['text']:
                view, source = content['text'].split(': ', 1)
                path = Path(source.strip())
                if path.is_file():
                    target = OUT / f'{name}-{view}.png'
                    shutil.copy2(path, target)
                    views.append((view, target))
        sheet = Image.new('RGB', (1120, 900), BACKGROUND)
        draw = ImageDraw.Draw(sheet)
        draw.text((20, 12), label + ' — шесть ракурсов MCP', fill=GOLD, font=TITLE)
        for i, (view, path) in enumerate(views):
            x, y = 15 + i % 3 * 370, 60 + i // 3 * 400
            picture = Image.open(path).convert('RGB')
            sheet.paste(picture, (x, y))
            draw.text((x + 5, y + 362), view, fill=GOLD, font=FONT)
        sheet.save(OUT / f'{name}-six-views.png')
    (OUT / 'validation.json').write_text(json.dumps(validation, indent=2) + '\n', encoding='utf-8')
    atlas = Image.open(ROOT / 'models/hospitality.png')
    atlas.resize((768, 768), Image.Resampling.NEAREST).save(OUT / 'texture-12x.png')
    foods = [('canape', 'Канапе'), ('club_sandwich', 'Сэндвич'), ('berry_tart', 'Ягодный тарт'),
             ('chocolate_truffle', 'Трюфель'), ('berry_fizz', 'Ягодная газировка'),
             ('spiced_cocoa', 'Какао'), ('ceramic_cup', 'Керамическая чашка')]
    sheet = Image.new('RGB', (1120, 550), BACKGROUND)
    draw = ImageDraw.Draw(sheet)
    draw.text((20, 12), 'Меню бара — ручные пиксели 16×16, увеличение ×10', font=TITLE, fill=GOLD)
    for i, (name, label) in enumerate(foods):
        x, y = 20 + i % 4 * 275, 70 + i // 4 * 240
        icon = Image.open(ROOT / f'src/main/resources/assets/boltcraft_casino/textures/item/{name}.png')
        icon = icon.resize((160, 160), Image.Resampling.NEAREST)
        sheet.paste(icon, (x + 40, y), icon)
        draw.text((x, y + 172), label, font=FONT, fill=GOLD)
    sheet.save(OUT / 'bar-menu.png')
    print(json.dumps(validation))
    print(OUT)


if __name__ == '__main__':
    main()
