"""Archive the in-game integration of approved gilding without overwriting approved concept screenshots."""
from pathlib import Path
import shutil

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'run-preview/screenshots'
OUT = ROOT / 'docs/gilded-trim-2026-10-25'


def main():
    result = (SOURCE.parent / 'art-preview-result.txt').read_text(encoding='utf-8').strip()
    if result != 'PASS structures 31':
        raise RuntimeError('Cannot archive an incomplete scene: ' + result)
    shots = [
        ('casino_lounge_angle', 'Лаунж — дневной фасад'),
        ('casino_hall_angle', 'Большой зал — дневной фасад'),
        ('casino_night_lounge', 'Лаунж — ночь'),
        ('casino_night_hall', 'Большой зал — ночь'),
        ('casino_lounge_hall', 'Лаунж — отделка интерьера'),
        ('casino_hall_gallery', 'Большой зал — галерея'),
    ]
    (OUT / 'screenshots').mkdir(parents=True, exist_ok=True)
    canvas = Image.new('RGB', (1310, 1265), (25, 20, 24))
    draw = ImageDraw.Draw(canvas)
    title = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 28)
    font = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 22)
    color = (242, 210, 122)
    draw.text((15, 12), 'Утверждённая позолота в новых казино — реальные кадры игры', font=title, fill=color)
    for i, (name, label) in enumerate(shots):
        path = SOURCE / (name + '.png')
        shutil.copy2(path, OUT / 'screenshots' / path.name)
        picture = Image.open(path).convert('RGB').resize((640, 360), Image.Resampling.LANCZOS)
        x, y = 10 + i % 2 * 650, 65 + i // 2 * 400
        canvas.paste(picture, (x, y))
        draw.text((x, y + 363), label, font=font, fill=color)
    canvas.save(OUT / 'gilded-in-game.png')
    print(OUT)


if __name__ == '__main__':
    main()
