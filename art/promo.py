"""Pictures and animations for the public project page, made only from real screenshots of the game.

    gradlew runClient -PartPreview -PpreviewScene=promo -PpreviewLanguage=en_us
    gradlew runClient -PartPreview -PpreviewScene=promo_world
    gradlew runClient -PartPreview -PpreviewScene=games -PpreviewLanguage=en_us
    python art/promo.py

Writes docs/media/: the banner and the social card, four animations and the gallery. The logo is the mod's own
CASINO marquee board and the lettering its neon font, both drawn by hand for the game; the photographs are game
frames, cropped and scaled, never painted over.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
SHOTS = ROOT / 'run-preview/screenshots'
NATURAL = ROOT / 'build/worldgen-client/screenshots'
OUT = ROOT / 'docs/media'
MARQUEE = ROOT / 'src/main/resources/assets/boltcraft_casino/textures/block/board/casino_marquee.png'
GOLD = (255, 214, 92)
CREAM = (255, 240, 206)
RED_NEON = (255, 82, 82)


def glyphs():
    table, name, rows = {}, None, []
    for line in (ROOT / 'art/neon_font.txt').read_text(encoding='utf-8').splitlines():
        if line.startswith('= '):
            if name:
                table[name] = rows
            name, rows = line.split(' ', 2)[2], []
        elif name and line:
            # Inside a glyph '#' is a lit pixel, not a comment.
            rows.append(line)
        elif name and not line and rows:
            table[name], name, rows = rows, None, []
    if name:
        table[name] = rows
    return table


GLYPHS = glyphs()


def lettering(text, scale, colour, glow=None):
    """Text in the neon font, each pixel a square of `scale`, with a soft halo when asked."""
    gap, space = 2, 4
    width = sum(space if char == ' ' else len(GLYPHS[char][0]) + gap for char in text)
    height = max(len(rows) for rows in GLYPHS.values())
    pad = scale * 4
    picture = Image.new('RGBA', (width * scale + pad * 2, height * scale + pad * 2), (0, 0, 0, 0))
    draw = ImageDraw.Draw(picture)
    x = 0
    for char in text:
        if char == ' ':
            x += space
            continue
        rows = GLYPHS[char]
        for y, row in enumerate(rows):
            for dx, cell in enumerate(row):
                if cell == '#':
                    left, top = pad + (x + dx) * scale, pad + y * scale
                    draw.rectangle([left, top, left + scale - 1, top + scale - 1], fill=colour + (255,))
        x += len(rows[0]) + gap
    if not glow:
        return picture
    halo = Image.new('RGBA', picture.size, glow + (0,))
    halo.putalpha(picture.getchannel('A').filter(ImageFilter.GaussianBlur(scale * 1.6)).point(lambda a: a * 0.9))
    return Image.alpha_composite(halo, picture)


def shot(name, folder=SHOTS):
    return Image.open(folder / f'{name}.png').convert('RGB')


def cover(picture, width, height, focus_y=0.5, zoom=1.0, focus_x=0.5):
    """Scales to fill width x height, enlarged by `zoom`, and crops the rest, keeping the given fractions of the
    spare width and height on the left and top."""
    scale = max(width / picture.width, height / picture.height) * zoom
    picture = picture.resize((round(picture.width * scale), round(picture.height * scale)), Image.LANCZOS)
    left = round((picture.width - width) * focus_x)
    top = round((picture.height - height) * focus_y)
    return picture.crop((left, top, left + width, top + height))


def shade_left(picture, reach):
    """Darkens the left side so the title reads over the photograph, fading out by `reach` of the width."""
    mask = Image.new('L', picture.size)
    draw = ImageDraw.Draw(mask)
    edge = int(picture.width * reach)
    for x in range(edge):
        draw.line([(x, 0), (x, picture.height)], fill=int(215 * (1 - x / edge) ** 1.4))
    return Image.composite(Image.new('RGB', picture.size, (14, 8, 14)), picture, mask)


def title_card(width, height, background, focus_y, zoom, scale):
    """The title on the left over the shaded sky, the building on the right: the background is enlarged and
    cropped from its left so that the building, in the middle of the frame, moves right. The marquee board is
    drawn `scale` times its size; the lettering keeps the same proportions to it."""
    picture = shade_left(cover(background, width, height, focus_y, zoom, focus_x=0.0), 0.62).convert('RGBA')
    margin = width // 22
    name = lettering('BOLTCRAFT', scale * 2, GOLD, glow=(255, 160, 40))
    marquee = Image.open(MARQUEE).convert('RGBA')
    marquee = marquee.resize((marquee.width * scale, marquee.height * scale), Image.NEAREST)
    tagline = lettering('SLOTS ★ ROULETTE ★ CASINOS TO CLAIM', scale, CREAM, glow=(255, 120, 120))
    block = name.height + marquee.height + tagline.height
    y = (height - block) // 2
    picture.alpha_composite(name, (margin - name.height // 12, y))
    y += name.height - height // 40
    picture.alpha_composite(marquee, (margin, y))
    y += marquee.height + height // 60
    picture.alpha_composite(tagline, (margin - tagline.height // 12, y))
    return picture.convert('RGB')


def animation(names, path, size, crop=None, step=1):
    """A looping GIF at ten frames a second from frames two game ticks apart, one shared palette, no dither."""
    frames = []
    for name in names[::step]:
        picture = shot(name)
        if crop:
            picture = picture.crop(crop)
        frames.append(picture.resize(size, Image.LANCZOS))
    sample = Image.new('RGB', (size[0], size[1] * 4))
    for i, frame in enumerate(frames[::max(1, len(frames) // 4)][:4]):
        sample.paste(frame, (0, size[1] * i))
    palette = sample.quantize(colors=255, method=Image.Quantize.MEDIANCUT)
    frames = [frame.quantize(palette=palette, dither=Image.Dither.NONE) for frame in frames]
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=100 * step, loop=0, optimize=True,
                   disposal=1)
    print(f'{path.relative_to(ROOT)}: {len(frames)} frames, {path.stat().st_size // 1024} KB')


def series(prefix):
    return sorted(path.stem for path in SHOTS.glob(f'{prefix}_[0-9][0-9][0-9].png'))


GALLERY = [
    ('hall-facade', 'casino_hall_facade', SHOTS),
    ('hall-night', 'promo_night', SHOTS),
    ('lounge-night', 'promo_night_lounge', SHOTS),
    ('hall-atrium', 'casino_hall_front_gallery', SHOTS),
    ('lounge-hall', 'casino_lounge_hall', SHOTS),
    ('hall-natural', 'natural_grand_hall', NATURAL),
    ('lounge-natural', 'natural_neon_lounge', NATURAL),
    ('slot-machine', 'slot_jackpot_pile', SHOTS),
    ('roulette', 'roulette_announce', SHOTS),
    ('gui-roulette', 'gui_roulette_betting', SHOTS),
    ('gui-cashier', 'gui_cashier_exchanged', SHOTS),
    ('gui-vault', 'gui_vault_journal', SHOTS),
    ('decor', 'decor_overview', SHOTS),
    ('guide-book', 'gui_guide_2', SHOTS),
]


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / 'gallery').mkdir(exist_ok=True)
    dusk = shot('promo_dusk')
    title_card(1280, 420, dusk, 0.3, 1.4, 3).save(OUT / 'banner.png', optimize=True)
    title_card(1280, 640, dusk, 0.42, 1.75, 3).save(OUT / 'social-preview.png', optimize=True)
    print('docs/media/banner.png, docs/media/social-preview.png')
    animation(series('promo_jackpot'), OUT / 'jackpot.gif', (640, 360))
    animation(series('promo_roulette'), OUT / 'roulette.gif', (640, 360))
    animation(series('promo_flyover'), OUT / 'flyover.gif', (560, 315))
    # The slot machine screen, gold frame and all, at GUI scale 3 in a 1280 x 720 window.
    animation(series('promo_gui_spin'), OUT / 'slot-screen.gif', (480, 439), crop=(250, 4, 1022, 710))
    for target, name, folder in GALLERY:
        source = folder / f'{name}.png'
        if not source.exists():
            print(f'missing {source.relative_to(ROOT)}')
            continue
        shot(name, folder).save(OUT / 'gallery' / f'{target}.jpg', quality=90, optimize=True)
    print(f'docs/media/gallery: {len(list((OUT / "gallery").glob("*.jpg")))} pictures')


if __name__ == '__main__':
    main()
