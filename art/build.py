"""Assembles hand-drawn pixel tiles into the mod's PNG textures.

Every pixel is written by hand in art/textures/*.txt; this script only looks colors up in art/palette.txt
and places tiles where the file says. It never generates pixels on its own.

Tile file format:
    @texture <output path relative to the repository> <width> <height>
    @tile <name> <u> <v>        rows of palette characters follow, one character per pixel
    @tile <name> <u> <v> rot180 the same, placed turned half a turn: tops of models are drawn the way a
                                player standing at the front sees them, and stored the way Blockbench maps
                                up faces (first row at the front, first column on the player's right)
    @copy <name> <u> <v>        places an already defined tile again
    @flip <name> <u> <v>        places a tile mirrored left to right
    @recolor <name> <u> <v> <from> <to>
                                places a tile again with every color in <from> swapped for the one at the
                                same position in <to>, for palette swaps of one drawing
Lines starting with # are comments. '.' is transparent. A file may hold several @texture blocks, for
example the frames of a particle; tiles stay usable in the blocks after the one that defines them.

Usage: python art/build.py art/textures/vault.txt [more files...]
"""
from pathlib import Path
import sys

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]


def load_palette():
    palette = {'.': (0, 0, 0, 0)}
    for line in (ROOT / 'art' / 'palette.txt').read_text(encoding='utf-8').splitlines():
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        char, color = line.split()[:2]
        hex_digits = color.lstrip('#')
        value = int(hex_digits, 16)
        if len(hex_digits) == 8:
            palette[char] = (value >> 24 & 255, value >> 16 & 255, value >> 8 & 255, value & 255)
        else:
            palette[char] = (value >> 16 & 255, value >> 8 & 255, value & 255, 255)
    return palette


def build(path, palette):
    image = None
    output = None
    tiles = {}
    current = None

    def finish():
        if current is None:
            return
        name, u, v, rows, turned = current
        widths = {len(row) for row in rows}
        if len(widths) != 1:
            raise ValueError(f'{path}: tile {name} has rows of different widths {sorted(widths)}')
        if turned:
            rows = [row[::-1] for row in reversed(rows)]
        tiles[name] = rows
        paste(rows, u, v, name)

    def paste(rows, u, v, name):
        for y, row in enumerate(rows):
            for x, char in enumerate(row):
                if char not in palette:
                    raise ValueError(f'{path}: tile {name} uses unknown color {char!r}')
                if char != '.':
                    image.putpixel((u + x, v + y), palette[char])

    def save():
        if image is None:
            return
        output.parent.mkdir(parents=True, exist_ok=True)
        image.save(output)
        print(f'{path} -> {output.relative_to(ROOT)}')

    for raw in Path(path).read_text(encoding='utf-8').splitlines():
        line = raw.strip()
        if not line or line.startswith('#'):
            continue
        if line.startswith('@'):
            finish()
            current = None
            parts = line.split()
            if parts[0] == '@texture':
                save()
                output = ROOT / parts[1]
                image = Image.new('RGBA', (int(parts[2]), int(parts[3])), (0, 0, 0, 0))
            elif parts[0] == '@tile':
                turned = len(parts) > 4 and parts[4] == 'rot180'
                current = (parts[1], int(parts[2]), int(parts[3]), [], turned)
            elif parts[0] in ('@copy', '@flip'):
                rows = tiles[parts[1]]
                if parts[0] == '@flip':
                    rows = [row[::-1] for row in rows]
                paste(rows, int(parts[2]), int(parts[3]), parts[1])
            elif parts[0] == '@recolor':
                swap = str.maketrans(parts[4], parts[5])
                rows = [row.translate(swap) for row in tiles[parts[1]]]
                paste(rows, int(parts[2]), int(parts[3]), parts[1])
            else:
                raise ValueError(f'{path}: unknown directive {parts[0]}')
            continue
        current[3].append(line)
    finish()
    save()


def main():
    palette = load_palette()
    for path in sys.argv[1:]:
        build(path, palette)


if __name__ == '__main__':
    main()
