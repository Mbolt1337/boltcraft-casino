"""Readable authoring recipe for Blockbench MCP; never writes or overwrites a .bbmodel itself.

    python art/build.py art/textures/hospitality.txt
    python art/hospitality_models.py

Run build/diagnostics/hospitality-create.json through the configured headless MCP, then its review request.
Recognition: a padded chair with a tall back; a pleated lamp on a brass stem; a shallow handled serving tray;
an open bottle shelf with a proper back and three different glass bottles. All faces retain vanilla UV density.
"""
from pathlib import Path
import base64
import json

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'build/diagnostics'
TILES = dict(wood=(0, 0), gold=(16, 0), velvet=(32, 0), porcelain=(48, 0),
             green_glass=(0, 16), red_glass=(16, 16), blue_glass=(32, 16), lampshade=(48, 16))
SIDES = ('north', 'east', 'south', 'west', 'up', 'down')


def cube(name, start, end, material, **faces):
    size = [end[i] - start[i] for i in range(3)]
    widths = dict(north=(size[0], size[1]), south=(size[0], size[1]),
                  east=(size[2], size[1]), west=(size[2], size[1]),
                  up=(size[0], size[2]), down=(size[0], size[2]))
    drawn = {}
    for side in SIDES:
        u, v = TILES[faces.get(side, material)]
        width, height = widths[side]
        if width > 16 or height > 16:
            raise ValueError(f'{name}/{side} would stretch its tile')
        # Narrow fittings show the brass body, with the brightest pixels reserved for upward edges.
        if faces.get(side, material) == 'gold' and side not in ('up', 'down'):
            u += min(2, 16 - width)
            v += min(2, 16 - height)
        if faces.get(side, material) == 'lampshade' and side not in ('up', 'down'):
            u += min(1, 16 - width)
            v += 3
        drawn[side] = dict(texture=0, uv=[u, v, u + width, v + height])
    return dict(op='add_cube', name=name, parent='assembly', **{'from': start, 'to': end}, faces=drawn)


def dining_chair():
    parts = []
    for x in (3, 11):
        for z in (3, 11):
            parts.append(cube(f'leg_{x}_{z}', [x, 0, z], [x + 2, 7, z + 2], 'wood', down='gold'))
            parts.append(cube(f'ferrule_{x}_{z}', [x - .1, 0, z - .1], [x + 2.1, 1, z + 2.1], 'gold'))
    parts.append(cube('seat_frame', [2, 6, 2], [14, 8, 14], 'wood', up='gold'))
    parts.append(cube('padded_seat', [3, 8, 3], [13, 9, 13], 'velvet'))
    for x in (2, 12):
        parts.append(cube(f'back_post_{x}', [x, 7, 13], [x + 2, 20, 15], 'wood'))
        parts.append(cube(f'back_finial_{x}', [x, 20, 13], [x + 2, 21, 15], 'gold'))
    parts.append(cube('back_frame', [4, 13, 13], [12, 20, 15], 'wood'))
    parts.append(cube('back_cushion', [4, 14, 12.5], [12, 19, 13], 'velvet'))
    parts.append(cube('back_crest', [4, 19, 12.5], [12, 20, 13], 'gold'))
    return parts


def serving_tray():
    parts = [cube('wood_base', [1, 0, 2], [15, 1, 14], 'wood'),
             cube('ceramic_inset', [2, 1, 3], [14, 1.5, 13], 'porcelain')]
    for x in (1, 14):
        parts.append(cube(f'long_rail_{x}', [x, 1, 2], [x + 1, 2, 14], 'gold'))
    for z in (2, 13):
        parts.append(cube(f'end_rail_{z}', [2, 1, z], [14, 2, z + 1], 'gold'))
    for x in (0, 15):
        parts.append(cube(f'handle_{x}', [x, 1, 6], [x + 1, 3, 10], 'gold'))
    return parts


def table_lamp():
    return [cube('brass_foot', [5, 0, 5], [11, 1, 11], 'gold'),
            cube('foot_step', [6, 1, 6], [10, 2, 10], 'gold'),
            cube('stem', [7, 2, 7], [9, 8, 9], 'gold'),
            cube('shade_lower_trim', [3, 7, 3], [13, 8, 13], 'gold', down='porcelain'),
            cube('shade_lower', [3.5, 8, 3.5], [12.5, 10, 12.5], 'lampshade'),
            cube('shade_upper', [4.5, 10, 4.5], [11.5, 12, 11.5], 'lampshade'),
            cube('shade_crown', [5.5, 12, 5.5], [10.5, 13, 10.5], 'gold'),
            cube('finial', [7, 13, 7], [9, 14, 9], 'gold')]


def bottle_shelf():
    parts = [cube('back_board', [0, 0, 15], [16, 16, 16], 'wood'),
             cube('shelf', [0, 0, 10], [16, 2, 15], 'wood', north='gold'),
             cube('cornice', [0, 15, 9], [16, 16, 15], 'wood', north='gold')]
    for x in (0, 15):
        parts.append(cube(f'side_{x}', [x, 2, 10], [x + 1, 15, 15], 'wood', north='gold'))
    for x, height, glass in [(2, 7, 'green_glass'), (7, 6, 'red_glass'), (12, 8, 'blue_glass')]:
        parts.append(cube(f'bottle_{x}', [x, 2, 11], [x + 2, height, 14], glass))
        parts.append(cube(f'neck_{x}', [x + .5, height, 12], [x + 1.5, height + 2, 13], glass))
        parts.append(cube(f'cap_{x}', [x + .5, height + 2, 12], [x + 1.5, height + 3, 13], 'gold'))
        parts.append(cube(f'label_{x}', [x, 3, 10.75], [x + 2, 5, 11], 'porcelain'))
    return parts


def main():
    texture = 'data:image/png;base64,' + base64.b64encode((ROOT / 'models/hospitality.png').read_bytes()).decode()
    create, review = [], []
    for name, recipe in [('dining_chair', dining_chair), ('serving_tray', serving_tray),
                         ('table_lamp', table_lamp), ('bottle_shelf', bottle_shelf)]:
        if (ROOT / 'models' / f'{name}.bbmodel').exists():
            raise SystemExit(f'{name}.bbmodel already exists: use MCP edits and keep its Git backup')
        file = name + '.bbmodel'
        create.append(dict(name='bbmodel_create', arguments=dict(file=file, name=name, format='java_block',
                      box_uv=False, resolution=dict(width=64, height=64))))
        operations = [dict(op='add_texture', name='hospitality.png', source=texture, width=64, height=64),
                      dict(op='add_group', name='assembly', origin=[8, 0, 8])] + recipe()
        create.append(dict(name='bbmodel_edit', arguments=dict(file=file, operations=operations)))
        review.append(dict(name='bbmodel_validate', arguments=dict(file=file)))
        review.append(dict(name='bbmodel_contact_sheet', arguments=dict(file=file, size=360,
                           orthographic=True, lighting='studio')))
        review.append(dict(name='bbmodel_export_java_block', arguments=dict(file=file, output=name + '.json',
                           version='1.9.0', overwrite=True)))
    OUT.mkdir(parents=True, exist_ok=True)
    for name, data in [('hospitality-create.json', create), ('hospitality-review.json', review)]:
        (OUT / name).write_text(json.dumps(data), encoding='utf-8')
        print(OUT / name)


if __name__ == '__main__':
    main()
