"""Plans of the two abandoned casinos (PROMPT.md 5.4, 8.6), written as code you can read like a floor plan.

    python art/structures/casinos.py

Writes art/structures/<name>.json: the size and every block. The art preview builds them in a test world
(gradlew runClient -PartPreview -PpreviewScene=structures), marks the casino blocks as fixtures of the building,
fills the chests' loot tables and saves data/boltcraft_casino/structure/<name>.nbt, the file worldgen places.
Builders can later rebuild a casino in creative and save it over the .nbt with a structure block.

Coordinates: x runs west to east, z north to south, y up from the floor (y = 0 is the floor itself). The
facade faces south. A sign on a wall facing F is read by someone looking against F, whose left hand is
F turned clockwise; text and boards run from there to their right.
"""
from pathlib import Path
import json

OUT = Path(__file__).resolve().parent
STEP = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}
CLOCKWISE = {'north': 'east', 'east': 'south', 'south': 'west', 'west': 'north'}
COUNTER = {v: k for k, v in CLOCKWISE.items()}
GLYPHS = {}

# Materials
BRICK = 'minecraft:polished_blackstone_bricks'
STONE = 'minecraft:polished_blackstone'
CHISELED = 'minecraft:chiseled_polished_blackstone'
PLANKS = 'minecraft:dark_oak_planks'
GLASS = 'minecraft:black_stained_glass_pane'
WOOD_PANEL = 'boltcraft_casino:wood_wall_panel'
VELVET_PANEL = 'boltcraft_casino:velvet_wall_panel'
CARPET_RED = 'boltcraft_casino:casino_carpet_burgundy_block'
MARBLE = 'boltcraft_casino:marble_tiles'
CHECKER = 'boltcraft_casino:checkered_tiles'


def load_glyphs():
    for line in (OUT.parent / 'neon_font.txt').read_text(encoding='utf-8').splitlines():
        if line.startswith('= '):
            glyph_id, char = line[2:].split(' ', 1)
            GLYPHS[char] = len(GLYPHS)


class Building:
    def __init__(self, name, width, height, depth):
        self.name = name
        self.size = (width, height, depth)
        self.blocks = {}
        self.chests = []
        self.markers = {}
        self.stair_volume = []

    def set(self, x, y, z, state):
        w, h, d = self.size
        if not (0 <= x < w and 0 <= y < h and 0 <= z < d):
            raise ValueError(f'{self.name}: {state} at {x} {y} {z} is outside the building')
        self.blocks[(x, y, z)] = state

    def fill(self, x1, y1, z1, x2, y2, z2, state):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, state)

    def ring(self, x1, z1, x2, z2, y1, y2, state):
        """Walls round a rectangle, one block thick."""
        self.fill(x1, y1, z1, x2, y2, z1, state)
        self.fill(x1, y1, z2, x2, y2, z2, state)
        self.fill(x1, y1, z1, x1, y2, z2, state)
        self.fill(x2, y1, z1, x2, y2, z2, state)

    def clear(self, x1, y1, z1, x2, y2, z2):
        self.fill(x1, y1, z1, x2, y2, z2, 'minecraft:air')

    def run(self, x, z, facing, count, gap=1):
        """Positions along a wall facing `facing`, from the viewer's left to their right."""
        dx, dz = STEP[COUNTER[facing]]
        return [(x + dx * i * gap, z + dz * i * gap) for i in range(count)]

    def letters(self, text, x, y, z, facing, color):
        for (lx, lz), char in zip(self.run(x, z, facing, len(text)), text):
            if char != ' ':
                self.set(lx, y, lz, f'boltcraft_casino:neon_letter[facing={facing},glyph={GLYPHS[char]},color={color}]')

    def spaced(self, text, x, y, z, facing, color):
        """Letters with a block between them, for a long facade."""
        for (lx, lz), char in zip(self.run(x, z, facing, len(text), 2), text):
            self.set(lx, y, lz, f'boltcraft_casino:neon_letter[facing={facing},glyph={GLYPHS[char]},color={color}]')

    def panel(self, name, width, x, y, z, facing):
        for part, (px, pz) in enumerate(self.run(x, z, facing, width)):
            self.set(px, y, pz, f'boltcraft_casino:neon_panel_{name}[facing={facing},part={part}]')

    def marquee_frame(self, x1, y1, x2, y2, z, facing):
        """Chasing lights round a rectangle on a wall that runs along x."""
        for x in range(x1, x2 + 1):
            self.set(x, y1, z, f'boltcraft_casino:marquee_lights[facing={facing}]')
            self.set(x, y2, z, f'boltcraft_casino:marquee_lights[facing={facing}]')
        for y in range(y1 + 1, y2):
            self.set(x1, y, z, f'boltcraft_casino:marquee_lights[facing={facing}]')
            self.set(x2, y, z, f'boltcraft_casino:marquee_lights[facing={facing}]')

    def column(self, x, z, y1, y2):
        self.fill(x, y1, z, x, y2, z, 'boltcraft_casino:casino_column')

    def slot_machine(self, x, y, z, facing):
        self.set(x, y, z, f'boltcraft_casino:slot_machine[facing={facing},half=lower]')
        self.set(x, y + 1, z, f'boltcraft_casino:slot_machine[facing={facing},half=upper]')

    def roulette(self, x, y, z, facing):
        """x, z: the table's front right block as its players see it; the croupier stands behind."""
        side = STEP[CLOCKWISE[facing]]
        back = STEP[CLOCKWISE[CLOCKWISE[facing]]]
        for part in range(4):
            px = x + side[0] * (part & 1) + back[0] * (part >> 1)
            pz = z + side[1] * (part & 1) + back[1] * (part >> 1)
            self.set(px, y, pz, f'boltcraft_casino:roulette_table[facing={facing},part={part}]')

    def sofa(self, x, y, z, facing, count):
        dx, dz = STEP[CLOCKWISE[facing]]
        for i in range(count):
            self.set(x + dx * i, y, z + dz * i, f'boltcraft_casino:velvet_sofa[facing={facing}]')

    def curtain(self, x1, x2, y1, y2, z, facing):
        self.fill(x1, y1, z, x2, y2, z, f'boltcraft_casino:velvet_curtain[facing={facing}]')

    def door(self, x, y, z, facing, hinge='left'):
        self.set(x, y, z, f'minecraft:dark_oak_door[facing={facing},half=lower,hinge={hinge}]')
        self.set(x, y + 1, z, f'minecraft:dark_oak_door[facing={facing},half=upper,hinge={hinge}]')

    def chest(self, x, y, z, facing, loot):
        self.set(x, y, z, f'minecraft:chest[facing={facing}]')
        self.chests.append([x, y, z, f'boltcraft_casino:chests/{loot}'])

    def save(self):
        w, h, d = self.size
        blocks = [[x, y, z, state] for (x, y, z), state in sorted(self.blocks.items(), key=lambda b: (b[0][1], b[0][2], b[0][0]))]
        data = {'name': self.name, 'size': [w, h, d], 'blocks': blocks, 'chests': self.chests,
                'markers': self.markers, 'stair_volume': self.stair_volume}
        # Keep each block on one line: the larger templates contain tens of thousands of cells.
        lines = ['{']
        for key, value in data.items():
            if key == 'blocks':
                lines.append('  "blocks": [')
                lines.extend('    ' + json.dumps(block) + (',' if i < len(blocks) - 1 else '')
                             for i, block in enumerate(blocks))
                lines.append('  ],')
            else:
                lines.append('  ' + json.dumps(key) + ': ' + json.dumps(value)
                             + (',' if key != 'stair_volume' else ''))
        lines.append('}')
        (OUT / f'{self.name}.json').write_text('\n'.join(lines) + '\n', encoding='utf-8')
        print(f'-> art/structures/{self.name}.json, {w}x{h}x{d}, {len(blocks)} blocks')


def shell(b, x1, z1, x2, z2, top, interior_upper=VELVET_PANEL):
    """Two-layer walls: dark brick outside with a plinth and corner piers, wood and velvet panelling inside."""
    b.ring(x1, z1, x2, z2, 1, top, BRICK)
    b.ring(x1, z1, x2, z2, 1, 1, STONE)
    for x, z in [(x1, z1), (x2, z1), (x1, z2), (x2, z2)]:
        b.fill(x, 1, z, x, top, z, CHISELED)
    b.ring(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 1, 2, WOOD_PANEL)
    b.ring(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 3, top - 1, interior_upper)


def neon_lounge():
    from concept_casinos import build_lounge
    build_lounge(Building)


def grand_hall():
    from concept_casinos import build_hall
    build_hall(Building)


if __name__ == '__main__':
    load_glyphs()
    neon_lounge()
    grand_hall()
