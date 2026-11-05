"""Builds every neon block of the mod from the hand-drawn tubes in art/neon_font.txt and art/neon_signs.txt.

    python art/neon.py [preview.png]

A neon sign is glass tube bent into shape and fixed a little off the wall, and the light it throws paints a halo
on the wall behind it. So every tube pixel becomes a small glowing cube one pixel in front of the wall (runs of
pixels are merged into longer cubes), and a flat plane against the wall carries the halo, one pixel round the
tube in the tube's darker shade. Both glow at full brightness whatever the light around them.

Writes, under src/main/resources/assets/boltcraft_casino:
    textures/block/neon/<glyph>.png        the letter's halo and tube in white and grey; the block tints them
    textures/block/neon/tube.png           tube colours: front, side and underside shades and the halo shade
    textures/block/neon/sign_<name>.png    an icon sign's halo and tubes, also its item icon
    textures/block/neon/panel_*.png        panel boards, their halos, and the whole panel for the item
    models/block/neon/*.json, models/item/neon_*.json
and src/main/java/com/mbolt/casino/block/decor/NeonGlyph.java, the glyph list in font order.

Nothing here invents pixels: the script only places drawn tubes and the drawn board tiles and rings each tube
with its halo.
"""
from pathlib import Path
import json
import sys

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/boltcraft_casino'
TEXTURES = ASSETS / 'textures/block/neon'
MODELS = ASSETS / 'models/block/neon'
ITEM_MODELS = ASSETS / 'models/item'
JAVA = ROOT / 'src/main/java/com/mbolt/casino/block/decor/NeonGlyph.java'
SIZE = 16
LETTER_TOP = 2
PANEL_LETTER_TOP = 4

# Neon colours: tube and halo. '#' is the white tube the block tints with the sign's colour.
NEON = {
    '#': ((255, 255, 255), (140, 140, 140)),
    'M': ((255, 62, 165), (184, 40, 122)),
    'T': ((46, 230, 255), (26, 159, 181)),
    'Y': ((255, 225, 77), (201, 164, 0)),
    'V': ((77, 255, 136), (37, 168, 82)),
    'P': ((255, 59, 59), (168, 30, 30)),
    'U': ((180, 92, 255), (110, 47, 176)),
    'W': ((244, 244, 255), (140, 140, 153)),
}
# Rows of tube.png, two pixels each; columns: front, side, underside, halo.
TUBE_ROWS = list(NEON)
SIDE_SHADE = 0.86
UNDER_SHADE = 0.68
SYMBOLS = {'star', 'diamond', 'heart', 'spade', 'club', 'exclamation', 'question', 'ampersand'}
FULL_BRIGHT = {'block_light': 15, 'sky_light': 15}

# Depths in the north-facing model, which hangs on the wall at z = 16.
WALL_HALO_Z = 15.8
WALL_TUBE = (14.8, 15.8)
TUBE_BLOCK = (13.8, 15.8)
PANEL_BOARD = (14, 16)
PANEL_HALO_Z = 13.9
PANEL_TUBE = (12.9, 13.9)


def rgba(color, alpha=255):
    return (*color, alpha)


def shade(color, factor):
    return tuple(round(c * factor) for c in color)


# ---------------------------------------------------------------- reading the drawings

def read_blocks(path, header_parts):
    """Splits a drawing file into "= kind name ..." blocks of rows."""
    blocks = []
    for raw in path.read_text(encoding='utf-8').splitlines():
        line = raw.rstrip()
        if not line or line == '#' or line.startswith('#') and not set(line) <= {'#', '.'}:
            continue
        if line.startswith('= '):
            blocks.append({'head': line[2:].split(' ', header_parts - 1), 'rows': []})
        else:
            blocks[-1]['rows'].append(line)
    return blocks


def read_font():
    glyphs = []
    for block in read_blocks(ROOT / 'art/neon_font.txt', 2):
        glyph_id, character = block['head']
        rows = block['rows']
        if len({len(row) for row in rows}) != 1 or len(rows) != 11:
            raise ValueError(f'Glyph {glyph_id} must be 11 rows of one width')
        glyphs.append({'id': glyph_id, 'char': character, 'rows': rows})
    return glyphs


def read_signs():
    icons, tiles, letters, panels = {}, {}, {}, {}
    for block in read_blocks(ROOT / 'art/neon_signs.txt', 3):
        kind, name, *rest = block['head']
        rows = block['rows']
        if kind != 'panel' and len({len(row) for row in rows}) != 1:
            raise ValueError(f'{kind} {name} has rows of different widths')
        if kind == 'icon':
            if len(rows) != SIZE or len(rows[0]) != SIZE:
                raise ValueError(f'Icon {name} must be 16x16')
            icons[name] = rows
        elif kind == 'tile':
            tiles[name] = rows
        elif kind == 'letter':
            letters[name] = rows
        elif kind == 'panel':
            panels[name] = {'blocks': int(rest[0]), 'layout': rows[0].split()}
    return icons, tiles, letters, panels


def load_palette():
    palette = {}
    for line in (ROOT / 'art/palette.txt').read_text(encoding='utf-8').splitlines():
        line = line.strip()
        if line and not line.startswith('#'):
            char, color = line.split()[:2]
            value = int(color.lstrip('#')[:6], 16)
            palette[char] = (value >> 16 & 255, value >> 8 & 255, value & 255)
    return palette


# ---------------------------------------------------------------- pixels to textures and cubes

def halo_pixels(tubes, allowed=None):
    """The halo round {(x, y): colour}: every free pixel touching a tube, coloured like the tube it touches."""
    halo = {}
    for (x, y), char in sorted(tubes.items()):
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                spot = (x + dx, y + dy)
                if spot not in tubes and (allowed is None or spot in allowed):
                    halo.setdefault(spot, char)
    return halo


def paint(image, pixels, which, offset=(0, 0)):
    """which: 0 the tube colour, 1 the halo colour."""
    for (x, y), char in pixels.items():
        image.putpixel((x - offset[0], y - offset[1]), rgba(NEON[char][which]))


def rectangles(pixels):
    """Merges same-coloured pixels into as few rectangles as rows and columns allow: (x0, y0, x1, y1, char)."""
    runs = []
    for char in sorted(set(pixels.values())):
        rows = {}
        for (x, y), c in pixels.items():
            if c == char:
                rows.setdefault(y, []).append(x)
        for y, xs in rows.items():
            xs.sort()
            start = xs[0]
            for a, b in zip(xs, xs[1:] + [None]):
                if b != a + 1:
                    runs.append([start, y, a + 1, y + 1, char])
                    start = b
    runs.sort(key=lambda r: (r[4], r[0], r[2], r[1]))
    merged = []
    for run in runs:
        last = merged[-1] if merged else None
        if last and last[4] == run[4] and last[0] == run[0] and last[2] == run[2] and last[3] == run[1]:
            last[3] = run[3]
        else:
            merged.append(run)
    return merged


def tube_uv(char, column):
    row = TUBE_ROWS.index(char)
    return [column * 2, row * 2, column * 2 + 2, row * 2 + 2]


def tube_element(x0, y0, x1, y1, char, depth, origin=(0, 0)):
    """A glowing cube for tube pixels x0..x1, y0..y1 counted from the top left as the viewer sees the front."""
    ox, oy = origin
    face = lambda column: {'texture': '#tube', 'uv': tube_uv(char, column), **tint(char)}
    return {
        'from': [SIZE - (x1 - ox), SIZE - (y1 - oy), depth[0]],
        'to': [SIZE - (x0 - ox), SIZE - (y0 - oy), depth[1]],
        'shade': False,
        'neoforge_data': FULL_BRIGHT,
        'faces': {'north': face(0), 'up': face(0), 'east': face(1), 'west': face(1), 'down': face(2)},
    }


def tint(char):
    return {'tintindex': 0} if char == '#' else {}


def plane(texture, z, tinted, rect=(0, 0, 16, 16)):
    """A glowing flat plane facing the viewer, rect = (x0, y0, x1, y1) as the viewer sees it."""
    x0, y0, x1, y1 = rect
    return {
        'from': [SIZE - x1, SIZE - y1, z],
        'to': [SIZE - x0, SIZE - y0, z],
        'shade': False,
        'neoforge_data': FULL_BRIGHT,
        'faces': {'north': {'texture': texture, 'uv': [x0, y0, x1, y1], **({'tintindex': 0} if tinted else {})}},
    }


def model(textures, elements, parent='block/block'):
    return {
        'parent': parent,
        'render_type': 'minecraft:cutout',
        'ambientocclusion': False,
        'textures': textures,
        'elements': elements,
    }


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')


def tex(name):
    return f'boltcraft_casino:block/neon/{name}'


# ---------------------------------------------------------------- the parts

def tube_texture():
    image = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    for row, char in enumerate(TUBE_ROWS):
        tube, halo = NEON[char]
        for column, color in enumerate([tube, shade(tube, SIDE_SHADE), shade(tube, UNDER_SHADE), halo]):
            for dx in (0, 1):
                for dy in (0, 1):
                    image.putpixel((column * 2 + dx, row * 2 + dy), rgba(color))
    image.save(TEXTURES / 'tube.png')


def glyph_tubes(glyph):
    """The glyph's tube pixels placed on the 16x16 face: letters on a common baseline, symbols centred."""
    rows = glyph['rows']
    width = len(rows[0])
    drawn = [(x, y) for y, row in enumerate(rows) for x, c in enumerate(row) if c == '#']
    top = LETTER_TOP
    if glyph['id'] in SYMBOLS:
        ys = [y for _, y in drawn]
        top = (SIZE - (max(ys) - min(ys) + 1)) // 2 - min(ys)
    left = (SIZE - width) // 2
    return {(left + x, top + y): '#' for x, y in drawn}


def build_letters(glyphs):
    for glyph in glyphs:
        tubes = glyph_tubes(glyph)
        image = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
        paint(image, halo_pixels(tubes), 1)
        paint(image, tubes, 0)
        image.save(TEXTURES / f"{glyph['id']}.png")
        elements = [plane('#halo', WALL_HALO_Z, True)]
        elements += [tube_element(*r, depth=WALL_TUBE) for r in rectangles(tubes)]
        write_json(MODELS / f"letter_{glyph['id']}.json",
                   model({'halo': tex(glyph['id']), 'tube': tex('tube'), 'particle': tex('tube_halo')}, elements))
    write_json(ITEM_MODELS / 'neon_letter.json',
               {'parent': 'minecraft:item/generated', 'textures': {'layer0': tex('a')}})


def build_icons(icons):
    for name, rows in icons.items():
        tubes = {(x, y): c for y, row in enumerate(rows) for x, c in enumerate(row) if c != '.'}
        image = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
        paint(image, halo_pixels(tubes), 1)
        paint(image, tubes, 0)
        image.save(TEXTURES / f'sign_{name}.png')
        elements = [plane('#halo', WALL_HALO_Z, False)]
        elements += [tube_element(*r, depth=WALL_TUBE) for r in rectangles(tubes)]
        write_json(MODELS / f'sign_{name}.json',
                   model({'halo': tex(f'sign_{name}'), 'tube': tex('tube'), 'particle': tex(f'sign_{name}')},
                         elements))
        write_json(ITEM_MODELS / f'neon_sign_{name}.json',
                   {'parent': 'minecraft:item/generated', 'textures': {'layer0': tex(f'sign_{name}')}})


def layout_tubes(layout, letters, board_width):
    """Places the panel's tokens side by side, centred on the board, letters on PANEL_LETTER_TOP."""
    placed = []
    width = 0
    for token in layout:
        if token.isdigit():
            width += int(token)
            continue
        name, color = token.split(':')
        rows = letters[name]
        placed.append((width, rows, color))
        width += len(rows[0])
    left = 3 + (board_width - 6 - width) // 2
    tubes = {}
    for x0, rows, color in placed:
        top = PANEL_LETTER_TOP + (7 - len(rows)) // 2
        for y, row in enumerate(rows):
            for x, c in enumerate(row):
                if c == '#':
                    tubes[(left + x0 + x, top + y)] = color
    if width > board_width - 6:
        raise ValueError(f'Panel lettering is {width} px, the board only has {board_width - 6}')
    return tubes


def build_panels(panels, tiles, letters, palette):
    for name, panel in panels.items():
        blocks = panel['blocks']
        width = blocks * SIZE
        board = Image.new('RGBA', (width, SIZE), (0, 0, 0, 0))
        for i in range(blocks):
            tile = tiles['panel_left' if i == 0 else 'panel_right' if i == blocks - 1 else 'panel_middle']
            for y, row in enumerate(tile):
                for x, c in enumerate(row):
                    board.putpixel((i * SIZE + x, y), rgba(palette[c]))
        tubes = layout_tubes(panel['layout'], letters, width)
        # The halo only falls on the board, not on the gold frame.
        inside = {(x, y) for x in range(2, width - 2) for y in range(2, SIZE - 2)}
        halo = halo_pixels(tubes, inside)
        glow = Image.new('RGBA', (width, SIZE), (0, 0, 0, 0))
        paint(glow, halo, 1)
        whole = board.copy()
        paint(whole, halo, 1)
        paint(whole, tubes, 0)
        whole.save(TEXTURES / f'panel_{name}.png')
        for i in range(blocks):
            box = (i * SIZE, 0, (i + 1) * SIZE, SIZE)
            board.crop(box).save(TEXTURES / f'panel_{name}_{i}.png')
            glow.crop(box).save(TEXTURES / f'panel_{name}_halo_{i}.png')
            part_tubes = {(x - i * SIZE, y): c for (x, y), c in tubes.items() if i * SIZE <= x < (i + 1) * SIZE}
            write_json(MODELS / f'panel_{name}_{i}.json', panel_part_model(name, i, blocks, part_tubes))
        write_json(ITEM_MODELS / f'neon_panel_{name}.json', panel_item_model(name, blocks))


def panel_part_model(name, part, blocks, tubes):
    board = tex(f'panel_{name}_{part}')
    # The part on the viewer's left is part 0. Its outer side is gold, inner sides are hidden by the next part.
    gold_side = {'texture': '#board', 'uv': [0, 0, 2, 16]}
    faces = {
        'north': {'texture': '#board', 'uv': [0, 0, 16, 16]},
        'up': {'texture': '#board', 'uv': [0, 0, 16, 2]},
        'down': {'texture': '#board', 'uv': [0, 14, 16, 16]},
        'south': {'texture': '#board', 'uv': [0, 0, 16, 16], 'cullface': 'south'},
    }
    if part == 0:
        faces['east'] = gold_side
    if part == blocks - 1:
        faces['west'] = {'texture': '#board', 'uv': [14, 0, 16, 16]}
    elements = [{'from': [0, 0, PANEL_BOARD[0]], 'to': [16, 16, PANEL_BOARD[1]], 'faces': faces},
                plane('#halo', PANEL_HALO_Z, False, (2, 2, 14, 14) if blocks == 1 else halo_rect(part, blocks))]
    elements += [tube_element(*r, depth=PANEL_TUBE) for r in rectangles(tubes)]
    m = model({'board': board, 'halo': tex(f'panel_{name}_halo_{part}'), 'tube': tex('tube'),
               'particle': board}, elements)
    m['ambientocclusion'] = True
    return m


def halo_rect(part, blocks):
    """The halo plane covers the board inside the frame, so it never shows over the gold."""
    x0 = 2 if part == 0 else 0
    x1 = 14 if part == blocks - 1 else 16
    return (x0, 2, x1, 14)


def panel_item_model(name, blocks):
    """The whole panel, flat, at half the block scale so it fits an inventory slot."""
    half = blocks * 8
    return {
        'parent': 'minecraft:block/block',
        'textures': {'panel': tex(f'panel_{name}'), 'particle': tex(f'panel_{name}')},
        'elements': [{
            'from': [8 - half / 2, 4, 7.5], 'to': [8 + half / 2, 12, 8.5],
            'faces': {'north': {'texture': '#panel', 'uv': [0, 0, 16, 16]},
                      'south': {'texture': '#panel', 'uv': [0, 0, 16, 16]}},
        }],
        'display': {
            'gui': {'rotation': [0, 180, 0], 'scale': [16 / (half * 1.0) * 0.95] * 3 if half > 16 else [1, 1, 1]},
            'ground': {'translation': [0, 2, 0], 'scale': [0.4, 0.4, 0.4]},
            'fixed': {'rotation': [0, 180, 0], 'scale': [16 / half * 0.95] * 3 if half > 16 else [1, 1, 1]},
            'thirdperson_righthand': {'rotation': [0, 180, 0], 'translation': [0, 2.5, 0],
                                      'scale': [0.375, 0.375, 0.375]},
            'firstperson_righthand': {'rotation': [0, 135, 0], 'scale': [0.4, 0.4, 0.4]},
            'firstperson_lefthand': {'rotation': [0, 135, 0], 'scale': [0.4, 0.4, 0.4]},
        },
    }


def build_tube_block():
    """The neon tube for outlines: a two pixel tube along the middle of the block, bending to its neighbours."""
    pieces = {
        'center': ((7, 7, 9, 9), (6, 6, 10, 10)),
        'up': ((7, 0, 9, 7), (6, 0, 10, 6)),
        'down': ((7, 9, 9, 16), (6, 10, 10, 16)),
        'left': ((0, 7, 7, 9), (0, 6, 6, 10)),
        'right': ((9, 7, 16, 9), (10, 6, 16, 10)),
    }
    textures = {'halo': tex('tube_halo'), 'tube': tex('tube'), 'particle': tex('tube_halo')}
    item = []
    for piece, (tube, halo) in pieces.items():
        elements = [plane('#halo', WALL_HALO_Z, True, halo), tube_element(*tube, '#', TUBE_BLOCK)]
        write_json(MODELS / f'tube_{piece}.json', model(textures, elements))
        if piece in ('center', 'left', 'right'):
            item += elements
    image = Image.new('RGBA', (SIZE, SIZE), rgba(NEON['#'][1]))
    image.save(TEXTURES / 'tube_halo.png')
    # In the inventory: a straight piece of tube, seen from the front.
    item_model = model(textures, item)
    item_model['display'] = {'gui': {'rotation': [0, 180, 0]}, 'fixed': {'rotation': [0, 180, 0]},
                             'ground': {'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]}}
    write_json(ITEM_MODELS / 'neon_tube.json', item_model)


def write_java(glyphs):
    constants = ',\n'.join(f'    {g["id"].upper()}("{g["id"]}", \'{g["char"]}\')' for g in glyphs)
    JAVA.write_text(f'''package com.mbolt.casino.block.decor;

/**
 * The characters a neon letter can show, in the order of art/neon_font.txt. Generated by art/neon.py from that
 * font; edit the font and run the script instead of editing this file.
 */
public enum NeonGlyph {{
{constants};

    private final String id;
    private final char character;

    NeonGlyph(String id, char character) {{
        this.id = id;
        this.character = character;
    }}

    /** Texture and model name, {{@code textures/block/neon/<id>.png}}. */
    public String id() {{
        return id;
    }}

    public char character() {{
        return character;
    }}
}}
''', encoding='utf-8')


def preview(glyphs, icons, panels):
    scale = 4
    sheet = Image.new('RGBA', (SIZE * 16 * scale, SIZE * 10 * scale), (34, 22, 30, 255))
    items = [Image.open(TEXTURES / f"{g['id']}.png") for g in glyphs]
    items += [Image.open(TEXTURES / f'sign_{name}.png') for name in icons]
    for index, image in enumerate(items):
        tile = image.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
        sheet.alpha_composite(tile, ((index % 16) * SIZE * scale, (index // 16) * SIZE * scale))
    y = (len(items) // 16 + 1) * SIZE * scale
    x = 0
    for name in panels:
        image = Image.open(TEXTURES / f'panel_{name}.png')
        tile = image.resize((image.width * scale, image.height * scale), Image.NEAREST)
        if x + tile.width > sheet.width:
            x, y = 0, y + tile.height + 8
        sheet.alpha_composite(tile, (x, y))
        x += tile.width + 8
    return sheet


def main():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    glyphs = read_font()
    icons, tiles, letters, panels = read_signs()
    tube_texture()
    build_letters(glyphs)
    build_icons(icons)
    build_panels(panels, tiles, letters, load_palette())
    build_tube_block()
    write_java(glyphs)
    print(f'-> {len(glyphs)} letters, {len(icons)} icon signs, {len(panels)} panels, tube pieces')
    return preview(glyphs, icons, panels)


if __name__ == '__main__':
    sheet = main()
    if len(sys.argv) > 1:
        sheet.save(sys.argv[1])
