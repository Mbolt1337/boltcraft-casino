"""Copy unedited game evidence and compose labelled review sheets. Does not generate game artwork."""
from pathlib import Path
import shutil

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/concepts-2026-10-25'
SOURCE = ROOT / 'run-preview/screenshots'
FONT = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 22)
TITLE = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 28)


def sheet(name, title, entries):
    width, height = 640, 360
    result = Image.new('RGB', (1310, 65 + len(entries) // 2 * 400), (25, 20, 24))
    draw = ImageDraw.Draw(result)
    draw.text((15, 12), title, font=TITLE, fill=(242, 210, 122))
    for i, (shot, label) in enumerate(entries):
        source = SOURCE / (shot + '.png')
        target = OUT / 'screenshots' / source.name
        shutil.copy2(source, target)
        picture = Image.open(source).convert('RGB').resize((width, height), Image.Resampling.LANCZOS)
        x, y = 10 + i % 2 * 650, 65 + i // 2 * 400
        result.paste(picture, (x, y))
        draw.text((x, y + height + 3), label, font=FONT, fill=(242, 210, 122))
    result.save(OUT / name)


def main():
    result = (SOURCE.parent / 'art-preview-result.txt').read_text(encoding='utf-8').strip()
    if result != 'PASS structures 31':
        raise RuntimeError('Cannot publish screenshots from an incomplete preview: ' + result)
    (OUT / 'screenshots').mkdir(parents=True, exist_ok=True)
    sheet('exteriors.png', 'Новые казино — реальные кадры Minecraft, день и ночь', [
        ('casino_lounge_angle', 'Лаунж: вход и боковой фасад'),
        ('casino_hall_angle', 'Большой зал: балкон и фонтан'),
        ('casino_lounge_rear', 'Лаунж: задний фасад'),
        ('casino_hall_side', 'Большой зал: боковые окна и пояса'),
        ('casino_night_lounge', 'Лаунж ночью'),
        ('casino_night_hall', 'Большой зал ночью'),
    ])
    sheet('interiors.png', 'Наши игровые блоки и декорации в новых планировках', [
        ('casino_lounge_hall', 'Лаунж: рулетка и автоматы'),
        ('casino_lounge_cashier', 'Лаунж: касса и служебная дверь'),
        ('casino_hall_portico', 'Большой зал: вход в атриум'),
        ('casino_hall_slots', 'Зона автоматов'),
        ('casino_hall_roulette', 'Рулетка и бар'),
        ('casino_hall_gallery', 'Второй этаж: VIP и галерея'),
        ('casino_hall_stairs', 'Лестница на второй этаж'),
        ('casino_hall_vault', 'Комната сейфа'),
    ])
    for source in SOURCE.glob('casino_*.png'):
        # Archive only this session's shots, avoiding the obsolete 24 October scenes.
        if source.name not in {'casino_placed.png', 'casino_night_atrium.png', 'casino_night_lounge_hall.png'}:
            shutil.copy2(source, OUT / 'screenshots' / source.name)
    print(OUT)


if __name__ == '__main__':
    main()
