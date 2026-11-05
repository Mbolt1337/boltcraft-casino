"""Builds models/roulette_table.bbmodel, the 2x2 roulette table, from art/textures/roulette_table.txt.

    python art/build.py art/textures/roulette_table.txt
    python art/roulette_table_model.py

The table is centred on the middle of its four blocks: x and z from -16 to 16, players at the front (-Z), the
croupier behind the wheel at the back (+Z). Bones the renderer drives: wheel (turns), ball_orbit (turns the
ball around the wheel), ball (rolls inwards and drops into a pocket) and marker (set on the winning number).
The round parts of the wheel follow the pixels drawn in the bowl_top tile, so geometry and texture always match.
"""
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from bbmodel_builder import Face, Model, SIDES  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
TXT = ROOT / 'art/textures/roulette_table.txt'
PNG = ROOT / 'src/main/resources/assets/boltcraft_casino/textures/block/roulette_table.png'

FELT_Y = 13
APRON_BOTTOM = 6
RAIL_TOP = 15
BOWL_CENTRE_Z = 8
BOWL_TOP = 15
TRACK_Y = 14
HEAD_Y = 13.25


def sides(tile, **overrides):
    faces = {side: tile for side in SIDES}
    faces.update(overrides)
    return faces


def tile_rows(name):
    """The rows of a tile as drawn in the txt file (for rot180 tiles: as the player at the front sees them)."""
    rows, active = [], False
    for raw in TXT.read_text(encoding='utf-8').splitlines():
        line = raw.strip()
        if line.startswith('@'):
            active = line.split()[:2] == ['@tile', name]
            continue
        if active and line and not line.startswith('#'):
            rows.append(line)
    return rows


def wall_blocks():
    """Rectangles covering the drawn bowl ring: (x0, x1, z0, z1) over the bowl's 16x16 area at x -8..8, z 0..16."""
    rows = tile_rows('bowl_top')
    size = len(rows)
    runs = []
    for r, row in enumerate(rows):
        c = 0
        while c < size:
            if row[c] == '.':
                c += 1
                continue
            start = c
            while c < size and row[c] != '.':
                c += 1
            # Drawing column c is x from 8 - c - 1 to 8 - c; drawing row r is z from 16 - r - 1 to 16 - r.
            runs.append([8 - c, 8 - start, size - r - 1, size - r])
    merged = []
    for run in runs:
        below = next((m for m in merged if m[0] == run[0] and m[1] == run[1] and m[2] == run[3]), None)
        if below:
            below[2] = run[2]
        else:
            merged.append(run)
    return merged


def build():
    model = Model('roulette_table', TXT, PNG, 128, 128)
    table = model.group('table', (0, 0, 0))

    # Apron: four boards under the top, inset one pixel, with a velvet band between gold lines.
    edge = Face('apron_inner', (0, 4))
    edge_across = Face('apron_underside_v')
    model.cube('apron_front', (-15, APRON_BOTTOM, -15), (15, 11, -14), table,
               dict(north='apron_front', south='apron_inner', down=edge))
    model.cube('apron_back', (-15, APRON_BOTTOM, 14), (15, 11, 15), table,
               dict(south='apron_side', north='apron_inner', down=edge))
    model.cube('apron_left', (14, APRON_BOTTOM, -14), (15, 11, 14), table,
               dict(east='apron_side', west='apron_inner', down=edge_across))
    model.cube('apron_right', (-15, APRON_BOTTOM, -14), (-14, 11, 14), table,
               dict(west='apron_side', east='apron_inner', down=edge_across))
    # Four stout legs inside the apron corners.
    for x in (10, -14):
        for z in (-14, 10):
            model.cube(f'leg_{x}_{z}', (x, 0, z), (x + 4, 11, z + 4), table,
                       dict(sides('leg_side'), down='leg_foot'))
    model.cube('top', (-16, 11, -16), (16, FELT_Y, 16), table,
               dict(sides('slab_side'), up='felt_top', down='underside'))

    rail = dict(up='rail_top', north='rail_outer', south='rail_inner', east='rail_end', west='rail_end')
    model.cube('rail_front', (-16, FELT_Y, -16), (16, RAIL_TOP, -14), table, rail)
    model.cube('rail_left', (14, FELT_Y, -14), (16, RAIL_TOP, 16), table,
               dict(up='rail_top_v', east='rail_outer', west='rail_inner', north='rail_end', south='rail_end'))
    model.cube('rail_right', (-16, FELT_Y, -14), (-14, RAIL_TOP, 16), table,
               dict(up='rail_top_v', west='rail_outer', east='rail_inner', north='rail_end', south='rail_end'))
    back = dict(up='rail_top', south='rail_outer', north='rail_inner', east='rail_end', west='rail_end')
    model.cube('rail_back_left', (6, FELT_Y, 14), (14, RAIL_TOP, 16), table, back)
    model.cube('rail_back_right', (-14, FELT_Y, 14), (-6, RAIL_TOP, 16), table, back)

    bowl = model.group('bowl', (0, FELT_Y, BOWL_CENTRE_Z), table)
    for index, (x0, x1, z0, z1) in enumerate(wall_blocks()):
        model.cube(f'bowl_wall_{index}', (x0, FELT_Y, z0), (x1, BOWL_TOP, z1), bowl,
                   dict(sides('bowl_side'), up=Face('bowl_top', (x0 + 8, z0))))
    model.cube('track', (-8, TRACK_Y, 0), (8, TRACK_Y, 16), bowl, {'up': 'track'})

    wheel = model.group('wheel', (0, HEAD_Y, BOWL_CENTRE_Z), bowl)
    model.cube('head', (-6, HEAD_Y, 2), (6, HEAD_Y, 14), wheel, {'up': 'head'})
    model.cube('cone', (-3, HEAD_Y, 5), (3, HEAD_Y + 1, 11), wheel, dict(sides('cone_side'), up='cone_top'))
    model.cube('collar', (-2, HEAD_Y + 1, 6), (2, HEAD_Y + 2, 10), wheel,
               dict(sides('collar_side'), up='collar_top'))
    model.cube('turret_post', (-1, HEAD_Y + 2, 7), (1, HEAD_Y + 4, 9), wheel,
               dict(sides('post_side'), up='post_top'))
    arm = 'turret_arm'
    model.cube('turret_arm_x', (-3, HEAD_Y + 4, 7.5), (3, HEAD_Y + 5, 8.5), wheel,
               dict(north=arm, south=arm, up=arm, down=arm, east=arm, west=arm))
    across = Face('turret_arm_v')
    model.cube('turret_arm_z', (-0.5, HEAD_Y + 4, 5), (0.5, HEAD_Y + 5, 11), wheel,
               dict(east=arm, west=arm, up=across, down=across, north=arm, south=arm))

    orbit = model.group('ball_orbit', (0, TRACK_Y, BOWL_CENTRE_Z), bowl)
    ball = model.group('ball', (0, TRACK_Y + 0.5, 1.5), orbit)
    model.cube('ball', (-0.5, TRACK_Y, 1), (0.5, TRACK_Y + 1, 2), ball, {face: 'ball' for face in
                                                                          ('north', 'east', 'south', 'west', 'up')})

    rack = model.group('chip_rack', (-11, FELT_Y, 7), table)
    model.cube('rack_tray', (-14, FELT_Y, 3), (-8, FELT_Y + 1, 11), rack, dict(sides('rack_side'), up='rack_top'))
    # Tubes from the centre outwards: white, red, blue, black.
    for column, x in enumerate((-10, -11, -12, -13)):
        model.cube(f'rack_tube_{column}', (x, FELT_Y + 1, 3.5), (x + 1, FELT_Y + 2, 10.5), rack,
                   dict(up=Face('rack_chips_top', (column, 0)),
                        east=Face('rack_chips_side', (0, column)), west=Face('rack_chips_side', (0, column)),
                        north=Face('rack_chips_end', (column, 0)), south=Face('rack_chips_end', (column, 0))))

    marker = model.group('marker', (11, FELT_Y, 10), table)
    model.cube('marker_foot', (10.5, FELT_Y, 9.5), (11.5, FELT_Y + 1, 10.5), marker,
               dict(sides('marker_foot'), up='marker_foot_top'))
    model.cube('marker_crystal', (10.5, FELT_Y + 1, 9.5), (11.5, FELT_Y + 2, 10.5), marker,
               dict(sides('marker_crystal'), up='marker_crystal_top'))

    model.save(ROOT / 'models/roulette_table.bbmodel')


def pocket_table():
    """Angle and radius of each pocket pixel in wheel order, for RouletteWheel. Angles in degrees from the front of
    the wheel, in the sense of the bones' Y rotation, which turns the drawing anticlockwise; radii in pixels from
    the wheel centre."""
    import math
    wheel = [0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10,
             5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26]
    red = {1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36}
    rows = tile_rows('head')
    centre = len(rows) / 2
    pockets = [(c, r) for r, row in enumerate(rows) for c, ch in enumerate(row) if ch in 'R1H']

    def on_screen(pixel):
        return math.atan2(pixel[1] + .5 - centre, pixel[0] + .5 - centre)

    zero = next(pixel for pixel in pockets if rows[pixel[1]][pixel[0]] == 'H')
    order = sorted(pockets, key=lambda pixel: (on_screen(pixel) - on_screen(zero)) % (2 * math.pi))
    angles, radii = [], []
    for number, (c, r) in zip(wheel, order):
        expected = 'H' if number == 0 else 'R' if number in red else '1'
        if rows[r][c] != expected:
            raise ValueError(f'Pocket {number} is drawn as {rows[r][c]!r}, expected {expected!r}')
        x, y = c + .5 - centre, r + .5 - centre
        angles.append(round(90 - math.degrees(math.atan2(y, x)), 2) % 360)
        radii.append(round(math.hypot(x, y), 3))
    return angles, radii


if __name__ == '__main__':
    build()
    angles, radii = pocket_table()
    print('POCKET_ANGLES =', ', '.join(f'{a:g}' for a in angles))
    print('POCKET_RADII =', ', '.join(f'{r:g}' for r in radii))
