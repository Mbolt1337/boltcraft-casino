"""Builds the large wall boards of art/boards.txt: the CASINO marquee signs, the giant chips and the banner.

    python art/boards.py [preview.png]

Every board is a grid of blocks, part 0 at the bottom left as seen from the front. For each part this writes
textures/block/board/<board>_<part>.png and models/block/board/<board>_<part>.json, plus an item model of the
whole board shrunk to an inventory slot.

- Marquee signs: the board is nine-sliced from the drawn frame tiles. Each letter cell becomes a raised gold
  stroke with a bulb in it, and bulbs run round the frame, lit two in four and stepping clockwise, using the
  animated marquee bulbs.
- Chips: the drawn disc is cut into one slab per pixel row, two pixels thick, so the rim shows from the side
  in red and white stripes. The back shows the same face, the right way round.
- Banner: the drawn cloth hangs one pixel off the wall under a gold rod; the bolt is glowing tube with a halo,
  made the way art/neon.py makes neon.

The script only cuts, places and merges what is drawn; it never draws a pixel of its own.
"""
from pathlib import Path
import json
import sys

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/boltcraft_casino'
TEXTURES = ASSETS / 'textures/block/board'
MODELS = ASSETS / 'models/block/board'
ITEM_MODELS = ASSETS / 'models/item'
SIZE = 16
CELL = 4
FRAME = 6
FULL_BRIGHT = {'block_light': 15, 'sky_light': 15}
CHIPS = {
    'red': {},
    'blue': {'P': 'L', 'R': 'B', 'Q': 'N', 'q': 'n'},
    'black': {'P': '4', 'R': '2', 'Q': '1', 'q': '0'},
}
# Rim stripes seen from the side, one pixel row each: body colour with the white of the edge spots.
CHIP_EDGE = 'RRRWWRRRRRWWRRRR'
TUBE_ROW_Y = 3  # the neon yellow row of textures/block/neon/tube.png
HALO = (201, 164, 0)


def tex(path):
    return f'boltcraft_casino:block/{path}'


def load_palette():
    palette = {'.': (0, 0, 0, 0)}
    for line in (ROOT / 'art/palette.txt').read_text(encoding='utf-8').splitlines():
        line = line.strip()
        if line and not line.startswith('#'):
            char, color = line.split()[:2]
            value = int(color.lstrip('#')[:6], 16)
            palette[char] = (value >> 16 & 255, value >> 8 & 255, value & 255, 255)
    return palette


def read_boards():
    tiles, fonts, drawings, signs = {}, {}, {}, []
    current = None
    for raw in (ROOT / 'art/boards.txt').read_text(encoding='utf-8').splitlines():
        line = raw.rstrip()
        if not line or line.startswith('#') and not set(line) <= {'#', '.'} or line == '#':
            continue
        if line.startswith('= '):
            head = line[2:].split()
            kind = head[0]
            if kind == 'tile':
                current = tiles.setdefault(head[1], [])
            elif kind == 'font':
                current = fonts.setdefault(head[1], {}).setdefault(head[2], [])
            elif kind == 'drawing':
                current = drawings.setdefault(head[1], [])
            elif kind == 'sign':
                signs.append({'name': head[1], 'width': int(head[2]), 'height': int(head[3]), 'font': head[4],
                              'text': head[5]})
                current = None
            continue
        current.append(line)
    for name, rows in list(tiles.items()) + list(drawings.items()):
        if len({len(row) for row in rows}) != 1:
            raise ValueError(f'{name} has rows of different widths')
    return tiles, fonts, drawings, signs


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=1) + '\n', encoding='utf-8')


def paint(rows, palette, recolor=None):
    image = Image.new('RGBA', (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, char in enumerate(row):
            if recolor:
                char = recolor.get(char, char)
            image.putpixel((x, y), palette[char])
    return image


def merge(cells):
    """Same-row runs of (x, y) cells merged, then stacked runs of equal span: [x0, y0, x1, y1] in cells."""
    rows = {}
    for x, y in cells:
        rows.setdefault(y, []).append(x)
    runs = []
    for y in sorted(rows):
        xs = sorted(rows[y])
        start = xs[0]
        for a, b in zip(xs, xs[1:] + [None]):
            if b != a + 1:
                runs.append([start, y, a + 1, y + 1])
                start = b
    merged = []
    for run in sorted(runs, key=lambda r: (r[0], r[2], r[1])):
        last = merged[-1] if merged else None
        if last and last[0] == run[0] and last[2] == run[2] and last[3] == run[1]:
            last[3] = run[3]
        else:
            merged.append(run)
    return merged


class Board:
    """A board of width x height blocks; geometry is given in pixels from the top left as seen from the front."""

    def __init__(self, name, width, height):
        self.name = name
        self.width = width
        self.height = height
        self.parts = {(c, r): [] for c in range(width) for r in range(height)}

    def part_of(self, column, row_from_top):
        return column, self.height - 1 - row_from_top

    def add(self, x0, y0, x1, y1, z0, z1, faces, glow=False):
        """A box over board pixels [x0, x1) x [y0, y1), cut at block edges. faces(part, local rect) -> faces."""
        for column in range(int(x0 // SIZE), int((x1 - 1e-6) // SIZE) + 1):
            for row in range(int(y0 // SIZE), int((y1 - 1e-6) // SIZE) + 1):
                lx0 = max(x0, column * SIZE) - column * SIZE
                lx1 = min(x1, (column + 1) * SIZE) - column * SIZE
                ly0 = max(y0, row * SIZE) - row * SIZE
                ly1 = min(y1, (row + 1) * SIZE) - row * SIZE
                if lx1 <= lx0 or ly1 <= ly0:
                    continue
                part = self.part_of(column, row)
                element = {'from': [SIZE - lx1, SIZE - ly1, z0], 'to': [SIZE - lx0, SIZE - ly0, z1],
                           'faces': faces(part, (lx0, ly0, lx1, ly1), (column * SIZE + lx0, row * SIZE + ly0,
                                                                        column * SIZE + lx1, row * SIZE + ly1))}
                if glow:
                    element['shade'] = False
                    element['neoforge_data'] = FULL_BRIGHT
                self.parts[part].append(element)

    def index(self, part):
        column, row = part
        return row * self.width + column

    def write(self, textures_of, render_type='minecraft:cutout'):
        for part, elements in self.parts.items():
            write_json(MODELS / f'{self.name}_{self.index(part)}.json', {
                'parent': 'block/block', 'render_type': render_type, 'textures': textures_of(part),
                'elements': elements})

    def item(self, texture):
        """The whole board, flat, shrunk to fit an inventory slot: model elements may not reach past -16..32."""
        fit = 16 / max(self.width, self.height)
        width = self.width * fit
        height = self.height * fit
        scale = .95
        write_json(ITEM_MODELS / f'{self.name}.json', {
            'parent': 'minecraft:block/block',
            'textures': {'board': texture, 'particle': texture},
            'elements': [{'from': [8 - width / 2, 8 - height / 2, 7.5], 'to': [8 + width / 2, 8 + height / 2, 8.5],
                          'faces': {'north': {'texture': '#board', 'uv': [0, 0, 16, 16]},
                                    'south': {'texture': '#board', 'uv': [0, 0, 16, 16]}}}],
            'display': {
                'gui': {'rotation': [0, 180, 0], 'scale': [scale] * 3},
                'fixed': {'rotation': [0, 180, 0], 'scale': [scale] * 3},
                'ground': {'translation': [0, 2, 0], 'scale': [scale * .5] * 3},
                'thirdperson_righthand': {'rotation': [0, 180, 0], 'translation': [0, 2.5, 0],
                                          'scale': [scale * .4] * 3},
                'firstperson_righthand': {'rotation': [0, 135, 0], 'scale': [scale * .4] * 3},
                'firstperson_lefthand': {'rotation': [0, 135, 0], 'scale': [scale * .4] * 3},
            }})


def uv_of(rect):
    return [rect[0], rect[1], rect[2], rect[3]]


# ---------------------------------------------------------------------------------------------- marquee signs

def tile_for(column, row_from_top, width, height):
    vertical = 't' if row_from_top == 0 else 'b' if row_from_top == height - 1 else ''
    horizontal = 'l' if column == 0 else 'r' if column == width - 1 else ''
    return 'marquee_' + (vertical + horizontal or 'm')


def frame_bulbs(width_px, height_px):
    """Bulb top-left corners round the frame, clockwise from the top left, every four pixels."""
    spots = []
    left, top, right, bottom = 2, 2, width_px - 4, height_px - 4
    for x in range(left, right, CELL):
        spots.append((x, top))
    for y in range(top, bottom, CELL):
        spots.append((right, y))
    for x in range(right, left, -CELL):
        spots.append((x, bottom))
    for y in range(bottom, top, -CELL):
        spots.append((left, y))
    return spots


def build_sign(sign, tiles, fonts, palette):
    name, width, height = sign['name'], sign['width'], sign['height']
    w, h = width * SIZE, height * SIZE
    board_rows = []
    for row in range(height):
        for y in range(SIZE):
            board_rows.append(''.join(tiles[tile_for(column, row, width, height)][y] for column in range(width)))
    board = paint(board_rows, palette)

    glyphs = fonts[sign['font']]
    gap = 2 if sign['font'] == 'large' else 1
    columns_used = sum(len(glyphs[c][0]) for c in sign['text']) + gap * (len(sign['text']) - 1)
    rows_used = len(glyphs[sign['text'][0]])
    origin_x = (w - columns_used * CELL) // 2
    origin_y = (h - rows_used * CELL) // 2
    # Letter cells on a grid of four-pixel cells starting at the origin.
    grid = []
    left = 0
    for char in sign['text']:
        glyph = glyphs[char]
        for gy, line in enumerate(glyph):
            for gx, mark in enumerate(line):
                if mark == '#':
                    grid.append((left + gx, gy))
        left += len(glyph[0]) + gap
    cells = [(origin_x + cx * CELL, origin_y + cy * CELL) for cx, cy in grid]

    b = Board(name, width, height)
    gold = {d: {'texture': '#gold', 'uv': [6, 6, 9, 9]} for d in ('north', 'east', 'west', 'up', 'down')}
    flame = {d: {'texture': '#bulb', 'uv': [4, 4, 6, 6]} for d in ('north', 'east', 'west', 'up', 'down')}

    def board_faces(part, local, whole):
        column, row = part
        faces = {'north': {'texture': '#board', 'uv': [0, 0, 16, 16]},
                 'south': {'texture': '#back', 'uv': [0, 0, 16, 16], 'cullface': 'south'}}
        if column == 0:
            faces['east'] = {'texture': '#gold', 'uv': [6, 0, 9, 16]}
        if column == width - 1:
            faces['west'] = {'texture': '#gold', 'uv': [6, 0, 9, 16]}
        if row == height - 1:
            faces['up'] = {'texture': '#gold', 'uv': [0, 6, 16, 9]}
        if row == 0:
            faces['down'] = {'texture': '#gold', 'uv': [0, 6, 16, 9]}
        return faces

    for column in range(width):
        for row in range(height):
            b.add(column * SIZE, row * SIZE, (column + 1) * SIZE, (row + 1) * SIZE, 13, 16, board_faces)
    for x0, y0, x1, y1 in merge(grid):
        b.add(origin_x + x0 * CELL, origin_y + y0 * CELL, origin_x + x1 * CELL, origin_y + y1 * CELL, 12, 13,
              lambda p, l, g: dict(gold))
    for x, y in cells:
        b.add(x + 1, y + 1, x + 3, y + 3, 11.5, 12, lambda p, l, g: dict(flame), glow=True)
    for i, (x, y) in enumerate(frame_bulbs(w, h)):
        slot = i % 4
        uv = [1 + 4 * slot, 7, 3 + 4 * slot, 9]
        b.add(x, y, x + 2, y + 2, 12.5, 13,
              lambda p, l, g, uv=uv: {d: {'texture': '#bulbs', 'uv': uv} for d in ('north', 'east', 'west', 'up',
                                                                                  'down')}, glow=True)

    TEXTURES.mkdir(parents=True, exist_ok=True)
    for (column, row), _ in b.parts.items():
        part_index = b.index((column, row))
        row_from_top = height - 1 - row
        board.crop((column * SIZE, row_from_top * SIZE, (column + 1) * SIZE, (row_from_top + 1) * SIZE)).save(
            TEXTURES / f'{name}_{part_index}.png')
    b.write(lambda part: {'board': tex(f'board/{name}_{b.index(part)}'), 'back': tex('decor/dark_wood'),
                          'gold': tex('decor/gold'), 'bulb': tex('decor/flame'), 'bulbs': tex('decor/marquee_bulbs'),
                          'particle': tex(f'board/{name}_{b.index(part)}')})

    # The item shows the board with its letters and bulbs painted flat.
    whole = board.copy()
    for x, y in cells:
        for dx in range(CELL):
            for dy in range(CELL):
                whole.putpixel((x + dx, y + dy), palette['e' if dx == 0 or dy == 0 else 'd'])
        for dx in (1, 2):
            for dy in (1, 2):
                whole.putpixel((x + dx, y + dy), palette['g'])
    for i, (x, y) in enumerate(frame_bulbs(w, h)):
        for dx in (0, 1):
            for dy in (0, 1):
                whole.putpixel((x + dx, y + dy), palette['g' if i % 4 < 2 else 'c'])
    whole.save(TEXTURES / f'{name}.png')
    b.item(tex(f'board/{name}'))
    return whole


# ---------------------------------------------------------------------------------------------- giant chips

def build_chip(color, drawing, palette):
    name = f'giant_chip_{color}'
    recolor = CHIPS[color]
    face = paint(drawing, palette, recolor)
    size = len(drawing)
    blocks = size // SIZE
    b = Board(name, blocks, blocks)
    edge_rows = [CHIP_EDGE[y % len(CHIP_EDGE)] * SIZE for y in range(SIZE)]
    paint(edge_rows, palette, recolor).save(TEXTURES / f'{name}_edge.png')

    for y, row in enumerate(drawing):
        xs = [x for x, char in enumerate(row) if char != '.']
        if not xs:
            continue
        x0, x1 = min(xs), max(xs) + 1

        def faces(part, local, whole, y=y):
            column, part_row = part
            lx0, ly0, lx1, ly1 = local
            gx0, gy0, gx1, gy1 = whole
            # From behind, this slab shows the front's mirror-image position, drawn the right way round.
            mirror_column = b.width - 1 - column
            bx0 = size - gx1 - mirror_column * SIZE
            bx1 = size - gx0 - mirror_column * SIZE
            edge_v = [0, y % SIZE, 2, y % SIZE + 1]
            return {
                'north': {'texture': '#front', 'uv': [lx0, ly0, lx1, ly1]},
                'south': {'texture': '#back', 'uv': [bx0, ly0, bx1, ly1]},
                'east': {'texture': '#edge', 'uv': edge_v},
                'west': {'texture': '#edge', 'uv': edge_v},
                'up': {'texture': '#edge', 'uv': [lx0, 0, lx1, 2]},
                'down': {'texture': '#edge', 'uv': [lx0, 0, lx1, 2]},
            }

        b.add(x0, y, x1, y + 1, 14, 16, faces)
    for (column, row) in b.parts:
        row_from_top = blocks - 1 - row
        face.crop((column * SIZE, row_from_top * SIZE, (column + 1) * SIZE, (row_from_top + 1) * SIZE)).save(
            TEXTURES / f'{name}_{b.index((column, row))}.png')

    def textures(part):
        column, row = part
        mirror = (b.width - 1 - column, row)
        return {'front': tex(f'board/{name}_{b.index(part)}'), 'back': tex(f'board/{name}_{b.index(mirror)}'),
                'edge': tex(f'board/{name}_edge'), 'particle': tex(f'board/{name}_{b.index(part)}')}

    b.write(textures)
    face.save(TEXTURES / f'{name}.png')
    b.item(tex(f'board/{name}'))
    return face


# ---------------------------------------------------------------------------------------------- banner

def build_banner(drawing, palette):
    name = 'boltcraft_banner'
    w, h = len(drawing[0]), len(drawing)
    tubes = {(x, y) for y, row in enumerate(drawing) for x, char in enumerate(row) if char == 'Y'}
    # Under the tube the cloth carries on in plain velvet.
    cloth = paint([''.join('t' if char == 'Y' else char for char in row) for row in drawing], palette)
    halo = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    for x, y in tubes:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if (x + dx, y + dy) not in tubes and 0 <= x + dx < w and 0 <= y + dy < h:
                    halo.putpixel((x + dx, y + dy), (*HALO, 255))
    b = Board(name, w // SIZE, h // SIZE)
    b.add(0, 0, w, h, 15, 16, lambda part, local, whole: {
        'north': {'texture': '#cloth', 'uv': list(local)},
        'south': {'texture': '#cloth', 'uv': list(local), 'cullface': 'south'}})
    b.add(0, 1, w, 4, 14, 15.99, lambda part, local, whole: {
        d: {'texture': '#gold', 'uv': [6, 6, 9, 9]} for d in ('north', 'east', 'west', 'up', 'down')})
    b.add(0, 0, w, h, 14.9, 14.9, lambda part, local, whole: {'north': {'texture': '#halo', 'uv': list(local)}},
          glow=True)
    tube_uv = [0, TUBE_ROW_Y * 2, 2, TUBE_ROW_Y * 2 + 2]
    side_uv = [2, TUBE_ROW_Y * 2, 4, TUBE_ROW_Y * 2 + 2]
    under_uv = [4, TUBE_ROW_Y * 2, 6, TUBE_ROW_Y * 2 + 2]
    for x0, y0, x1, y1 in merge(tubes):
        b.add(x0, y0, x1, y1, 14, 14.9, lambda part, local, whole: {
            'north': {'texture': '#tube', 'uv': tube_uv}, 'up': {'texture': '#tube', 'uv': tube_uv},
            'east': {'texture': '#tube', 'uv': side_uv}, 'west': {'texture': '#tube', 'uv': side_uv},
            'down': {'texture': '#tube', 'uv': under_uv}}, glow=True)
    for (column, row) in b.parts:
        row_from_top = b.height - 1 - row
        box = (column * SIZE, row_from_top * SIZE, (column + 1) * SIZE, (row_from_top + 1) * SIZE)
        cloth.crop(box).save(TEXTURES / f'{name}_{b.index((column, row))}.png')
        halo.crop(box).save(TEXTURES / f'{name}_halo_{b.index((column, row))}.png')
    b.write(lambda part: {'cloth': tex(f'board/{name}_{b.index(part)}'),
                          'halo': tex(f'board/{name}_halo_{b.index(part)}'), 'gold': tex('decor/gold'),
                          'tube': tex('neon/tube'), 'particle': tex(f'board/{name}_{b.index(part)}')})
    whole = cloth.copy()
    whole.alpha_composite(halo)
    for x, y in tubes:
        whole.putpixel((x, y), palette['Y'])
    whole.save(TEXTURES / f'{name}.png')
    b.item(tex(f'board/{name}'))
    return whole


def main():
    palette = load_palette()
    tiles, fonts, drawings, signs = read_boards()
    TEXTURES.mkdir(parents=True, exist_ok=True)
    pictures = [build_sign(sign, tiles, fonts, palette) for sign in signs]
    pictures += [build_chip(color, drawings['chip'], palette) for color in CHIPS]
    pictures.append(build_banner(drawings['banner'], palette))
    print(f'-> {len(signs)} signs, {len(CHIPS)} chips, the banner')
    scale = 3
    width = max(p.width for p in pictures) * scale
    sheet = Image.new('RGBA', (width, sum(p.height * scale + 8 for p in pictures)), (30, 26, 32, 255))
    y = 0
    for picture in pictures:
        sheet.alpha_composite(picture.resize((picture.width * scale, picture.height * scale), Image.NEAREST), (0, y))
        y += picture.height * scale + 8
    return sheet


if __name__ == '__main__':
    preview = main()
    if len(sys.argv) > 1:
        preview.save(sys.argv[1])
