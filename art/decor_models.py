"""Writes the Java block models of the decor blocks (everything but neon, see art/neon.py).

    python art/decor_models.py

Models are built from boxes in model pixels, facing north: wall pieces hang on the wall at z = 16, furniture faces
the player who places it. Faces take the part of their texture under their own pixels (vanilla automatic UV), so
the textures in art/textures/decor.txt are drawn as the material looks across a whole block. Pieces that reach
outside the block, like the grand chandelier, name their UV themselves.
"""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
MODELS = ROOT / 'src/main/resources/assets/boltcraft_casino/models'
DIRECTIONS = ('north', 'south', 'east', 'west', 'up', 'down')
FULL_BRIGHT = {'block_light': 15, 'sky_light': 15}
ROPE_COLORS = ('red', 'blue', 'green', 'black')


def decor(name):
    return f'boltcraft_casino:block/decor/{name}'


def box(frm, to, faces, uv=None, glow=False, rotation=None, skip=()):
    """faces: a texture variable for every face, or {direction: variable or (variable, uv, rotation)}."""
    if isinstance(faces, str):
        faces = {d: faces for d in DIRECTIONS}
    element = {'from': list(frm), 'to': list(to), 'faces': {}}
    for direction, face in faces.items():
        if direction in skip:
            continue
        rot = None
        face_uv = uv
        if isinstance(face, tuple):
            face, face_uv, *rest = face
            rot = rest[0] if rest else None
        entry = {'texture': face}
        if face_uv is not None:
            entry['uv'] = list(face_uv)
        if rot:
            entry['rotation'] = rot
        element['faces'][direction] = entry
    if glow:
        element['shade'] = False
        element['neoforge_data'] = FULL_BRIGHT
    if rotation:
        element['rotation'] = rotation
    return element


def write(path, textures, elements, parent='block/block', render_type=None, ao=True, display=None):
    data = {'parent': parent}
    if render_type:
        data['render_type'] = render_type
    if not ao:
        data['ambientocclusion'] = False
    data['textures'] = textures
    data['elements'] = elements
    if display:
        data['display'] = display
    out = MODELS / f'{path}.json'
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')


FRONT_VIEW = {'gui': {'rotation': [0, 180, 0]}, 'fixed': {'rotation': [0, 180, 0]},
              'ground': {'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]}}


# ---------------------------------------------------------------- marquee lights

def marquee():
    """A gold-edged channel along the wall with a bulb every four pixels; the lit pair runs along the strip.

    Each arm is half a block long, so a straight run is two arms and a corner two arms at right angles. Vertical
    arms show the horizontal channel turned a quarter, and each of their bulbs borrows a horizontal bulb's place
    in the animation so the light runs upwards.
    """
    textures = {'channel': decor('marquee_channel'), 'gold': decor('gold'), 'bulbs': decor('marquee_bulbs'),
                'particle': decor('gold')}
    bulb_uv = [(1 + 4 * i, 7, 3 + 4 * i, 9) for i in range(4)]

    def bulb(x0, y0, uv):
        # x0, y0: the bulb's top left as the viewer sees the front.
        return box((14 - x0, 14 - y0, 13), (16 - x0, 16 - y0, 14),
                   {d: ('#bulbs', uv) for d in ('north', 'up', 'down', 'east', 'west')}, glow=True)

    def channel(x0, y0, x1, y1, front_uv, rot=None):
        faces = {'north': ('#channel', front_uv, rot) if rot else ('#channel', front_uv)}
        for d in ('up', 'down', 'east', 'west'):
            faces[d] = '#gold'
        faces['south'] = '#gold'
        return box((16 - x1, 16 - y1, 14), (16 - x0, 16 - y0, 16), faces)

    arms = {
        'left': [channel(0, 6, 8, 10, (0, 6, 8, 10)), bulb(1, 7, bulb_uv[0]), bulb(5, 7, bulb_uv[1])],
        'right': [channel(8, 6, 16, 10, (8, 6, 16, 10)), bulb(9, 7, bulb_uv[2]), bulb(13, 7, bulb_uv[3])],
        'up': [channel(6, 0, 10, 8, (0, 6, 8, 10), 270), bulb(7, 1, bulb_uv[3]), bulb(7, 5, bulb_uv[2])],
        'down': [channel(6, 8, 10, 16, (8, 6, 16, 10), 270), bulb(7, 9, bulb_uv[1]), bulb(7, 13, bulb_uv[0])],
    }
    for arm, elements in arms.items():
        write(f'block/decor/marquee_{arm}', textures, elements, render_type='minecraft:cutout')
    write('item/marquee_lights', textures, arms['left'] + arms['right'], render_type='minecraft:cutout',
          display=FRONT_VIEW)


# ---------------------------------------------------------------- velvet rope

def velvet_rope():
    """A gold stanchion; the rope sags in steps from the post to the middle between two posts."""
    gold = {'gold': decor('gold'), 'particle': decor('gold')}
    post = [
        box((4, 0, 4), (12, 1, 12), '#gold'),
        box((5, 1, 5), (11, 2, 11), '#gold'),
        box((7, 2, 7), (9, 13, 9), '#gold'),
        box((6, 13, 6), (10, 15, 10), '#gold'),
        box((7, 15, 7), (9, 16, 9), '#gold'),
    ]
    write('block/decor/velvet_rope_post', gold, post)

    def side(z_flip=False):
        segments = [((7, 11, 5), (9, 13, 7)), ((7, 10, 2), (9, 12, 5)), ((7, 9, 0), (9, 11, 2))]
        out = []
        for (x0, y0, z0), (x1, y1, z1) in segments:
            if z_flip:
                z0, z1 = 16 - z1, 16 - z0
            out.append(box((x0, y0, z0), (x1, y1, z1), '#rope'))
        return out

    for color in ROPE_COLORS:
        textures = {'rope': decor(f'rope_{color}'), 'particle': decor(f'rope_{color}')}
        write(f'block/decor/velvet_rope_side_{color}', textures, side())
        write(f'item/velvet_rope_{color}', {**gold, **textures}, post + side() + side(True),
              display={'gui': {'rotation': [30, 45, 0], 'scale': [0.625, 0.625, 0.625]},
                       'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
                       'fixed': {'rotation': [0, 90, 0], 'scale': [0.5, 0.5, 0.5]}})


# ---------------------------------------------------------------- bar

def bar_counter():
    """Customer side to the north: a padded velvet front set back two pixels under the overhanging top, and a
    brass foot rail. The outer corner has customers on the north and west, the inner one a notch at north-west."""
    textures = {'top': decor('dark_wood'), 'edge': decor('gold'), 'front': decor('counter_front'),
                'back': decor('counter_back'), 'wood': decor('dark_wood'), 'gold': decor('gold'),
                'particle': decor('dark_wood')}
    top = box((0, 14, 0), (16, 16, 16), {'up': '#top', 'down': '#top', 'north': ('#edge', (0, 0, 16, 2)),
                                         'south': ('#edge', (0, 0, 16, 2)), 'east': ('#edge', (0, 0, 16, 2)),
                                         'west': ('#edge', (0, 0, 16, 2))})

    def rail(x0, z0, x1, z1):
        return box((x0, 2, z0), (x1, 3, z1), '#gold')

    straight = [
        top,
        box((0, 0, 2), (16, 14, 16), {'north': '#front', 'south': '#back', 'east': '#wood', 'west': '#wood'}),
        rail(0, 0, 16, 1), rail(3, 1, 4, 2), rail(12, 1, 13, 2),
    ]
    outer = [
        top,
        box((2, 0, 2), (16, 14, 16), {'north': '#front', 'west': '#front', 'south': '#back', 'east': '#wood'}),
        rail(0, 0, 16, 1), rail(0, 1, 1, 16), rail(12, 1, 13, 2), rail(1, 12, 2, 13),
    ]
    inner = [
        top,
        box((0, 0, 2), (16, 14, 16), {'north': '#front', 'south': '#back', 'east': '#wood', 'west': '#wood'}),
        box((2, 0, 0), (16, 14, 2), {'north': '#back', 'west': '#front', 'east': '#wood'}),
        rail(0, 0, 1, 1),
    ]
    write('block/decor/bar_counter', textures, straight)
    write('block/decor/bar_counter_outer', textures, outer)
    write('block/decor/bar_counter_inner', textures, inner)


def bar_stool():
    textures = {'gold': decor('gold'), 'velvet': decor('velvet'), 'tufted': decor('velvet_tufted'),
                'particle': decor('velvet')}
    elements = [
        box((4, 0, 4), (12, 1, 12), '#gold'),
        box((7, 1, 7), (9, 10, 9), '#gold'),
        box((5, 4, 5), (11, 5, 6), '#gold'),
        box((5, 4, 10), (11, 5, 11), '#gold'),
        box((5, 4, 6), (6, 5, 10), '#gold'),
        box((10, 4, 6), (11, 5, 10), '#gold'),
        box((3, 10, 3), (13, 11, 13), '#gold'),
        box((3, 11, 3), (13, 14, 13), {'up': '#tufted', 'north': '#velvet', 'south': '#velvet', 'east': '#velvet',
                                        'west': '#velvet', 'down': '#velvet'}),
    ]
    write('block/decor/bar_stool', textures, elements)


# ---------------------------------------------------------------- walls and columns

def gold_skirting():
    textures = {'wood': decor('dark_wood'), 'gold': decor('gold'), 'particle': decor('dark_wood')}
    write('block/decor/gold_skirting', textures, [
        box((0, 0, 14), (16, 4, 16), '#wood'),
        box((0, 4, 13), (16, 5, 16), '#gold'),
        box((0, 0, 13), (16, 1, 14), '#gold'),
    ])


def column(stone='', name='column'):
    """Marble columns, or with stone='_onyx' black onyx with a gilded capital and plinth."""
    textures = {'shaft': decor('column_shaft' + stone), 'trim': decor('column_trim' + stone),
                'marble': decor('marble' + stone), 'gold': decor('gold'), 'particle': decor('marble' + stone)}
    side = lambda tex: {d: tex for d in ('north', 'south', 'east', 'west')}
    plinth = box((1, 0, 1), (15, 3, 15), {**side('#trim'), 'up': '#marble', 'down': '#marble'})
    low_ring = box((2, 3, 2), (14, 4, 14), '#gold')
    high_ring = box((2, 12, 2), (14, 13, 14), '#gold')
    capital = box((1, 13, 1), (15, 16, 15), {**side('#trim'), 'up': '#marble', 'down': '#marble'})
    shaft = lambda y0, y1: box((3, y0, 3), (13, y1, 13), {**side('#shaft'), 'up': '#marble', 'down': '#marble'})
    write(f'block/decor/{name}_single', textures, [plinth, low_ring, shaft(4, 12), high_ring, capital])
    write(f'block/decor/{name}_bottom', textures, [plinth, low_ring, shaft(4, 16)])
    write(f'block/decor/{name}_middle', textures, [shaft(0, 16)])
    write(f'block/decor/{name}_top', textures, [shaft(0, 12), high_ring, capital])


# ---------------------------------------------------------------- light

def crystal_chandelier():
    """A small chandelier on a short chain: a gold ring with four candles, crystal drops and a crystal ball."""
    textures = {'gold': decor('gold'), 'crystal': decor('crystal'), 'candle': decor('marble'),
                'flame': decor('flame'), 'particle': decor('crystal')}
    elements = [
        box((6, 15, 6), (10, 16, 10), '#gold'),
        box((7, 12, 7), (9, 15, 9), '#gold'),
        box((7, 5, 7), (9, 12, 9), '#gold'),
        box((3, 7, 3), (13, 8, 4), '#gold'),
        box((3, 7, 12), (13, 8, 13), '#gold'),
        box((3, 7, 4), (4, 8, 12), '#gold'),
        box((12, 7, 4), (13, 8, 12), '#gold'),
        box((4, 7, 7), (12, 8, 9), '#gold'),
        box((7, 7, 4), (9, 8, 12), '#gold'),
        box((6, 2, 6), (10, 5, 10), '#crystal'),
        box((7, 1, 7), (9, 2, 9), '#crystal'),
    ]
    for x, z in [(3, 3), (12, 3), (3, 12), (12, 12)]:
        elements.append(box((x, 8, z), (x + 1, 10, z + 1), '#candle'))
        elements.append(box((x, 10, z), (x + 1, 11, z + 1), '#flame', glow=True))
    for x, z in [(5, 3), (10, 3), (5, 12), (10, 12), (3, 5), (3, 10), (12, 5), (12, 10)]:
        elements.append(box((x, 5, z), (x + 1, 7, z + 1), '#crystal'))
    write('block/decor/crystal_chandelier', textures, elements, render_type='minecraft:cutout', ao=False)


def grand_chandelier():
    """A three by three chandelier hanging from the middle block: two tiers of candles over a cascade of crystal.

    It reaches a block out on every side and a block down, so every face names a small UV of its own material.
    """
    textures = {'gold': decor('gold'), 'crystal': decor('crystal'), 'candle': decor('marble'),
                'flame': decor('flame'), 'particle': decor('crystal')}
    uv = {'#gold': (6, 6, 9, 9), '#crystal': (4, 4, 8, 8), '#candle': (4, 4, 6, 6), '#flame': (5, 5, 7, 7)}

    def part(frm, to, tex, glow=False):
        return box(frm, to, tex, uv=uv[tex], glow=glow)

    elements = [
        part((6, 15, 6), (10, 16, 10), '#gold'),
        part((7, 8, 7), (9, 15, 9), '#gold'),
        part((6, -6, 6), (10, 8, 10), '#gold'),
        part((5, 0, 5), (11, 2, 11), '#crystal'),
        part((5, -10, 5), (11, -6, 11), '#crystal'),
        part((6, -14, 6), (10, -10, 10), '#crystal'),
        part((7, -16, 7), (9, -14, 9), '#gold'),
    ]
    rings = [(12, 2, -4, 20), (20, -6, -12, 28)]
    for radius, y, lo, hi in rings:
        elements += [
            part((lo, y, lo), (hi, y + 1, lo + 1), '#gold'),
            part((lo, y, hi - 1), (hi, y + 1, hi), '#gold'),
            part((lo, y, lo + 1), (lo + 1, y + 1, hi - 1), '#gold'),
            part((hi - 1, y, lo + 1), (hi, y + 1, hi - 1), '#gold'),
            part((lo + 1, y, 7), (6, y + 1, 9), '#gold'),
            part((10, y, 7), (hi - 1, y + 1, 9), '#gold'),
            part((7, y, lo + 1), (9, y + 1, 6), '#gold'),
            part((7, y, 10), (9, y + 1, hi - 1), '#gold'),
        ]
        step = (hi - lo - 1) / 4
        spots = sorted({round(lo + step * i) for i in range(5)} | {hi - 1})
        candles = {(x, z) for x in spots for z in (lo, hi - 1)} | {(x, z) for z in spots for x in (lo, hi - 1)}
        for x, z in sorted(candles):
            elements.append(part((x, y + 1, z), (x + 1, y + 3, z + 1), '#candle'))
            elements.append(part((x, y + 3, z), (x + 1, y + 4, z + 1), '#flame', glow=True))
        drops = [(x, z) for x in spots[1:-1] for z in (lo, hi - 1)] + [(x, z) for z in spots[1:-1] for x in (lo, hi - 1)]
        for x, z in drops:
            elements.append(part((x, y - 3, z), (x + 1, y, z + 1), '#crystal'))
    write('block/decor/grand_chandelier', textures, elements, render_type='minecraft:cutout', ao=False)
    write('item/grand_chandelier', textures, elements, render_type='minecraft:cutout', ao=False, display={
        'gui': {'rotation': [30, 225, 0], 'translation': [0, 1, 0], 'scale': [0.24, 0.24, 0.24]},
        'ground': {'translation': [0, 3, 0], 'scale': [0.12, 0.12, 0.12]},
        'fixed': {'scale': [0.3, 0.3, 0.3]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.15, 0.15, 0.15]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.18, 0.18, 0.18]},
        'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.18, 0.18, 0.18]},
    })


def wall_sconce():
    textures = {'gold': decor('gold'), 'crystal': decor('crystal'), 'candle': decor('marble'),
                'flame': decor('flame'), 'particle': decor('gold')}
    write('block/decor/wall_sconce', textures, [
        box((6, 3, 15), (10, 13, 16), '#gold'),
        box((7, 6, 11), (9, 8, 15), '#gold'),
        box((6, 8, 9), (10, 9, 13), '#gold'),
        box((7, 9, 10), (9, 13, 12), '#candle'),
        box((7, 13, 10), (9, 14, 12), '#flame', glow=True),
        box((7, 14, 10), (8, 15, 11), '#flame', glow=True),
        box((7, 5, 10), (9, 8, 12), '#crystal'),
    ], render_type='minecraft:cutout')


# ---------------------------------------------------------------- furniture

def velvet_sofa():
    """Seat facing north, back to the south; an arm on each end that has no sofa next to it."""
    textures = {'velvet': decor('velvet'), 'tufted': decor('velvet_tufted'), 'wood': decor('dark_wood'),
                'gold': decor('gold'), 'particle': decor('velvet')}
    base = [
        box((0, 1, 1), (16, 3, 16), '#wood'),
        box((0, 3, 1), (16, 8, 13), '#velvet'),
        box((0, 3, 13), (16, 15, 16), {'north': '#tufted', 'up': '#velvet', 'south': '#velvet', 'east': '#velvet',
                                        'west': '#velvet', 'down': '#velvet'}),
        box((1, 0, 2), (3, 1, 4), '#gold'),
        box((13, 0, 2), (15, 1, 4), '#gold'),
        box((1, 0, 13), (3, 1, 15), '#gold'),
        box((13, 0, 13), (15, 1, 15), '#gold'),
    ]
    arm_left = [box((0, 3, 1), (3, 11, 16), '#velvet'), box((0, 11, 1), (3, 12, 13), '#gold')]
    arm_right = [box((13, 3, 1), (16, 11, 16), '#velvet'), box((13, 11, 1), (16, 12, 13), '#gold')]
    write('block/decor/velvet_sofa', textures, base)
    # The sitter faces north, so their left hand is to the west (x = 0).
    write('block/decor/velvet_sofa_arm_left', textures, arm_left)
    write('block/decor/velvet_sofa_arm_right', textures, arm_right)
    write('item/velvet_sofa', textures, base + arm_left + arm_right)


def tables():
    wood = {'wood': decor('dark_wood'), 'gold': decor('gold'), 'particle': decor('dark_wood')}
    legs = lambda height, tex: [box((x, 0, z), (x + 2, height, z + 2), tex) for x in (2, 12) for z in (2, 12)]
    write('block/decor/coffee_table', wood, [
        box((1, 6, 1), (15, 8, 15), {'up': '#wood', 'down': '#wood', 'north': '#gold', 'south': '#gold',
                                     'east': '#gold', 'west': '#gold'}),
        *legs(6, '#gold'),
    ])
    textures = {**wood, 'felt': decor('card_table_top'), 'velvet': decor('velvet'),
                'particle': decor('card_table_top')}
    write('block/decor/card_table', textures, [
        box((1, 11, 1), (15, 12, 15), {'up': '#felt'}),
        box((0, 11, 0), (16, 13, 1), '#velvet'),
        box((0, 11, 15), (16, 13, 16), '#velvet'),
        box((0, 11, 1), (1, 13, 15), '#velvet'),
        box((15, 11, 1), (16, 13, 15), '#velvet'),
        box((1, 9, 1), (15, 11, 15), '#wood'),
        *legs(9, '#wood'),
    ])


def velvet_curtain():
    """Pleats of velvet: four strips a block, the lit ones standing out from the wall."""
    textures = {'curtain': decor('curtain'), 'gold': decor('gold'), 'fringe': decor('fringe'),
                'particle': decor('curtain')}

    def pleats(y0, y1):
        # Strips at x 4..8 and 12..16 take the lit ridges of the texture (it is mapped right to left on the front).
        return [box((x, y0, 12 if out else 13), (x + 4, y1, 15 if out else 16), '#curtain')
                for x, out in ((0, False), (4, True), (8, False), (12, True))]

    valance = [box((0, 12, 11), (16, 16, 14), '#curtain'), box((0, 11, 11), (16, 12, 14), '#gold')]
    fringe = [box((0, 0, 11.9), (16, 5, 11.9), {'north': '#fringe', 'south': '#fringe'})]
    write('block/decor/velvet_curtain_single', textures, valance + pleats(0, 11) + fringe,
          render_type='minecraft:cutout')
    write('block/decor/velvet_curtain_top', textures, valance + pleats(0, 11), render_type='minecraft:cutout')
    write('block/decor/velvet_curtain_middle', textures, pleats(0, 16), render_type='minecraft:cutout')
    write('block/decor/velvet_curtain_bottom', textures, pleats(0, 16) + fringe, render_type='minecraft:cutout')


def gold_planter():
    textures = {'pot': decor('planter'), 'soil': decor('soil'), 'palm': decor('palm'), 'particle': decor('planter')}
    elements = [
        box((3, 0, 3), (13, 7, 13), '#pot'),
        box((2, 7, 2), (14, 9, 3), '#pot'),
        box((2, 7, 13), (14, 9, 14), '#pot'),
        box((2, 7, 3), (3, 9, 13), '#pot'),
        box((13, 7, 3), (14, 9, 13), '#pot'),
        box((3, 7, 3), (13, 8, 13), {'up': '#soil'}),
    ]
    for angle in (45, -45):
        elements.append(box((0.8, 8, 8), (15.2, 24, 8), {'north': ('#palm', (0, 0, 16, 16)),
                                                        'south': ('#palm', (0, 0, 16, 16))},
                            rotation={'origin': [8, 8, 8], 'axis': 'y', 'angle': angle, 'rescale': True}))
    write('block/decor/gold_planter', textures, elements, render_type='minecraft:cutout')


def architecture(name):
    return f'boltcraft_casino:block/architecture/{name}'


def gilded_lantern():
    """A square brass lantern: a base, glass lit from inside between four corner posts, a stepped roof."""
    textures = {'gold': decor('gold'), 'glass': architecture('lantern_glass'), 'particle': decor('gold')}
    body = [
        box((4, 0, 4), (12, 1, 12), '#gold'),
        box((5, 1, 5), (11, 10, 11), {d: ('#glass', (2, 1, 14, 15)) for d in ('north', 'south', 'east', 'west')},
            glow=True),
        box((4, 10, 4), (12, 11, 12), '#gold'),
        box((5, 11, 5), (11, 12, 11), '#gold'),
        box((6, 12, 6), (10, 13, 10), '#gold'),
        box((7, 13, 7), (9, 14, 9), '#gold'),
    ]
    for x, z in ((4, 4), (11, 4), (4, 11), (11, 11)):
        body.append(box((x, 1, z), (x + 1, 10, z + 1), '#gold'))
    write('block/architecture/gilded_lantern', textures, body, render_type='minecraft:cutout')
    write('block/architecture/gilded_lantern_hanging', textures,
          body + [box((7, 14, 7), (9, 16, 9), '#gold')], render_type='minecraft:cutout')


def gilded_railing():
    """A gold balustrade: a capped square post, and towards each neighbour a top rail, a foot rail and a baluster."""
    textures = {'gold': decor('gold'), 'particle': decor('gold')}
    post = [box((6, 0, 6), (10, 15, 10), '#gold'), box((5, 15, 5), (11, 16, 11), '#gold')]

    def side(flip=False):
        parts = [((7, 13, 0), (9, 15, 6)), ((7, 1, 0), (9, 2, 6)), ((7, 2, 2), (9, 13, 4))]
        out = []
        for (x0, y0, z0), (x1, y1, z1) in parts:
            if flip:
                z0, z1 = 16 - z1, 16 - z0
            out.append(box((x0, y0, z0), (x1, y1, z1), '#gold'))
        return out

    write('block/architecture/gilded_railing_post', textures, post)
    write('block/architecture/gilded_railing_side', textures, side())
    write('item/gilded_railing', textures, post + side() + side(True),
          display={'gui': {'rotation': [30, 45, 0], 'scale': [0.625, 0.625, 0.625]},
                   'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
                   'fixed': {'rotation': [0, 90, 0], 'scale': [0.5, 0.5, 0.5]}})


def main():
    marquee()
    velvet_rope()
    bar_counter()
    bar_stool()
    gold_skirting()
    column()
    column('_onyx', 'onyx_column')
    gilded_lantern()
    gilded_railing()
    crystal_chandelier()
    grand_chandelier()
    wall_sconce()
    velvet_sofa()
    tables()
    velvet_curtain()
    gold_planter()
    print('-> decor models written')


if __name__ == '__main__':
    main()
