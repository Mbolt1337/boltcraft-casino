"""The two abandoned casinos, after Maxim's four concepts of 25 October 2026 (docs/concepts-2026-10-25/references).

This is a building plan written as code, room by room and bay by bay; nothing in it is random. The concepts'
architecture, and what carries it here:

- dark brick walls on a stone plinth, articulated by projecting pylons (polished blackstone);
- red fields framed in thin gold (gilded panels, which join into one framed field however large);
- chunky gold where the concepts show gold blocks: window sills and lintels, pylon caps, the parapet coping, the
  stepped crest, the bases and capitals of the piers (gilded trim);
- thin gold lines between floors and at the roof (gilded cornices) and red neon below them;
- a central tower with a lit CASINO marquee and a stepped crest of giant chips; BoltCraft banners on the pylons;
- a canopy over the entrance on thick piers, its red fascia edged with chasing lights, a lit coffered underside;
- tall glazed bays showing the lit halls; red carpet up the entrance steps between gilded railings; lanterns on
  stone posts, hedges in stone planters, a fountain;
- inside: coffered ceilings, onyx pilasters, red panelled walls with gold cornices, chandeliers, rows of slot
  machines, roulette and card tables, a bar with bottle shelves, a lounge, and in the grand hall an atrium with a
  grand staircase up to a gallery, a VIP floor, a vault room and a terrace over the canopy.

Coordinates as in casinos.py: x west to east, z north (back) to south (front), y up; the facade faces south.
"""

BRICK = 'minecraft:polished_blackstone_bricks'
STONE = 'minecraft:polished_blackstone'
CAP = 'minecraft:polished_blackstone_brick_slab[type=bottom]'
STAIR = 'minecraft:polished_blackstone_brick_stairs'
PANEL = 'boltcraft_casino:gilded_panel'
TRIM = 'boltcraft_casino:gilded_trim'
CORNICE = 'boltcraft_casino:gilded_cornice'
COFFER = 'boltcraft_casino:coffered_ceiling'
CARPET_STAIR = 'boltcraft_casino:carpet_stairs'
ONYX = 'boltcraft_casino:onyx_column'
LANTERN = 'boltcraft_casino:gilded_lantern[hanging=false]'
HANGING_LANTERN = 'boltcraft_casino:gilded_lantern[hanging=true]'
RAILING = 'boltcraft_casino:gilded_railing'
WOOD_PANEL = 'boltcraft_casino:wood_wall_panel'
CARPET = 'boltcraft_casino:casino_carpet_burgundy_block'
NAVY = 'boltcraft_casino:casino_carpet_navy_block'
NAVY_RUG = 'boltcraft_casino:casino_carpet_navy'
MARBLE = 'boltcraft_casino:marble_tiles'
CHECKER = 'boltcraft_casino:checkered_tiles'
PLANKS = 'minecraft:dark_oak_planks'
GLASS = 'minecraft:red_stained_glass_pane'
PAVING = 'minecraft:polished_andesite'
KERB = 'minecraft:stone_bricks'
HEDGE = 'minecraft:azalea_leaves[persistent=true]'
FLOWERING = 'minecraft:flowering_azalea_leaves[persistent=true]'
WATER = 'minecraft:water'


def board(b, name, width, height, x, y, z, facing):
    """A wall board of width x height blocks, its bottom left (as the viewer sees it) at x, y, z."""
    for row in range(height):
        for column, (px, pz) in enumerate(b.run(x, z, facing, width)):
            b.set(px, y + row, pz, f'boltcraft_casino:{name}[facing={facing},part={row * width + column}]')


def neon_line(b, x1, x2, y, z, facing, color='red'):
    for x in range(x1, x2 + 1):
        b.set(x, y, z, f'boltcraft_casino:neon_tube[facing={facing},color={color}]')


def lamp_post(b, x, y, z):
    """A stone post with a brass lantern, as the concepts line their forecourts."""
    b.set(x, y, z, KERB)
    b.set(x, y + 1, z, 'minecraft:stone_brick_wall')
    b.set(x, y + 2, z, LANTERN)


def planter(b, x1, z1, x2, z2, y, flowers=False):
    """A stone planter one block high with clipped hedge on top."""
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            b.set(x, y, z, KERB)
            b.set(x, y + 1, z, FLOWERING if flowers and (x + z) % 3 == 0 else HEDGE)


def lining(b, x1, z1, x2, z2, y1, y2, pilasters):
    """The inside of the walls: wood wainscot, red gilded panels above, a gold cornice at the ceiling, and onyx
    pilasters dividing the panels into bays."""
    b.lining_rows = {**getattr(b, 'lining_rows', {}),
                     **{y: WOOD_PANEL if y < y1 + 2 else PANEL for y in range(y1, y2)}}
    b.ring(x1, z1, x2, z2, y1, y1 + 1, WOOD_PANEL)
    b.ring(x1, z1, x2, z2, y1 + 2, y2 - 1, PANEL)
    b.ring(x1, z1, x2, z2, y2, y2, CORNICE)
    for x in range(x1, x2 + 1, pilasters):
        for z in (z1, z2):
            b.fill(x, y1, z, x, y2 - 1, z, ONYX)
    for z in range(z1, z2 + 1, pilasters):
        for x in (x1, x2):
            b.fill(x, y1, z, x, y2 - 1, z, ONYX)


def opening(b, cells, y1, y2):
    """Cuts a hole y1..y2 through the walls at cells, (x, z) pairs of the outer wall and of its lining. A pilaster
    the hole cuts becomes plain lining above and below it, so no stub of onyx is left hanging."""
    for x, z in cells:
        for y, state in getattr(b, 'lining_rows', {}).items():
            if y1 - 3 <= y <= y2 + 3 and b.blocks.get((x, y, z)) == ONYX:
                b.set(x, y, z, state)
        b.clear(x, y1, z, x, y2, z)


def glazing(b, x1, x2, y1, y2, z, lining_z, mullions=(), sill=True, lintel=True):
    """A tall glazed bay in a wall running along x: tinted glass with stone mullions, open through the lining, a
    gold sill under it and a gold lintel over it."""
    opening(b, [(x, zz) for x in range(x1, x2 + 1) for zz in (z, lining_z)], y1, y2)
    for x in range(x1, x2 + 1):
        b.fill(x, y1, z, x, y2, z, STONE if x in mullions else GLASS)
        for y, wanted in ((y1 - 1, sill), (y2 + 1, lintel)):
            if wanted:
                b.set(x, y, z, TRIM)


def side_glazing(b, x, z1, z2, y1, y2, lining_x, mullions=(), sill=True, lintel=True):
    """The same in a wall running along z."""
    opening(b, [(xx, z) for z in range(z1, z2 + 1) for xx in (x, lining_x)], y1, y2)
    for z in range(z1, z2 + 1):
        b.fill(x, y1, z, x, y2, z, STONE if z in mullions else GLASS)
        for y, wanted in ((y1 - 1, sill), (y2 + 1, lintel)):
            if wanted:
                b.set(x, y, z, TRIM)


def chandelier(b, x, y, z, chain=0, grand=False):
    """A chandelier hanging from the ceiling above y + chain."""
    for dy in range(1, chain + 1):
        b.set(x, y + dy, z, 'minecraft:chain')
    b.set(x, y, z, 'boltcraft_casino:grand_chandelier' if grand else 'boltcraft_casino:crystal_chandelier')


def slot_row(b, xs, y, z, facing, stool_z):
    """Machines side by side, each with a stool a step back from it: a seat needs room for the sitter's legs."""
    for x in xs:
        b.slot_machine(x, y, z, facing)
        b.set(x, y, stool_z, 'boltcraft_casino:bar_stool')


def bar(b, x1, x2, y, z, wall_z, stool_z):
    """Counters facing the room, the bartender's walk behind, shelves of bottles on the wall."""
    for x in range(x1, x2 + 1):
        b.set(x, y, z, 'boltcraft_casino:bar_counter[facing=south]')
    for x in range(x1 + 1, x2, 2):
        b.set(x, y, stool_z, 'boltcraft_casino:bar_stool')
    for x in range(x1 + 1, x2, 3):
        b.set(x, y + 2, wall_z, 'boltcraft_casino:bottle_shelf[facing=south]')
    b.set(x1, y + 1, z, 'boltcraft_casino:table_lamp')
    b.set(x2, y + 1, z, 'boltcraft_casino:serving_tray')


def coffee(b, x, y, z, prop):
    b.set(x, y, z, 'boltcraft_casino:coffee_table')
    b.set(x, y + 1, z, 'boltcraft_casino:' + prop)


def card_table(b, x, y, z):
    """A card table with a chair at each end, a step away from it."""
    b.set(x, y, z, 'boltcraft_casino:card_table[facing=north]')
    b.set(x - 2, y, z, 'boltcraft_casino:dining_chair[facing=east]')
    b.set(x + 2, y, z, 'boltcraft_casino:dining_chair[facing=west]')


def fountain(b, x, z, y):
    """A stone basin of still water with cut corners, round a marble pedestal carrying a lantern."""
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if abs(dx) == 3 and abs(dz) == 3:
                continue
            rim = abs(dx) == 3 or abs(dz) == 3 or abs(dx) + abs(dz) == 5
            b.set(x + dx, y, z + dz, KERB if rim else WATER)
            b.set(x + dx, y - 1, z + dz, KERB)
    b.set(x, y, z, MARBLE)
    b.set(x, y + 1, z, MARBLE)
    b.set(x, y + 2, z, LANTERN)


def flight(b, x1, x2, runner, top_y, z, cheeks):
    """Steps down from a porch at top_y + 1 to the ground: each step a block further out and lower, a carpet
    runner up the middle, stone cheeks with gilded railings at both ends."""
    for step, y in enumerate(range(top_y, 0, -1)):
        for x in range(x1, x2 + 1):
            b.set(x, y, z + step, (CARPET_STAIR if x in runner else STAIR) + '[facing=north]')
            if y > 1:
                b.fill(x, 1, z + step, x, y - 1, z + step, STONE)
        for x in cheeks:
            b.fill(x, 1, z + step, x, y, z + step, STONE)
            b.set(x, y + 1, z + step, RAILING)


# ---------------------------------------------------------------------------------------------- neon lounge

def build_lounge(building):
    """A one-storey lounge after concepts 2 and 3: a sign tower with the small marquee and a crest of three chips,
    a canopy between bannered pylons, glazed bays over hedges, eight slot machines along the side walls, roulette
    in the middle under the grand chandelier, the cashier, the bar and the vault room at the back."""
    b = building('neon_lounge', 25, 20, 27)
    b.clear(0, 1, 0, 24, 19, 26)
    # Ground: stone foundation, the plinth, the hall floor at y = 2, a paved forecourt.
    b.fill(0, 0, 0, 24, 0, 26, KERB)
    b.fill(0, 0, 23, 24, 0, 26, PAVING)
    b.fill(1, 1, 1, 23, 1, 18, STONE)
    b.fill(1, 2, 1, 23, 2, 18, PLANKS)
    b.fill(3, 2, 3, 21, 2, 16, CARPET)
    b.fill(9, 2, 7, 15, 2, 13, CHECKER)
    b.fill(13, 2, 3, 21, 2, 5, MARBLE)
    # The porch in front of the door.
    b.fill(9, 1, 19, 15, 1, 22, STONE)
    b.fill(9, 2, 19, 15, 2, 22, MARBLE)
    b.fill(11, 2, 17, 13, 2, 22, CARPET)

    # Walls: brick from the plinth to the cornice, a parapet capped with slabs, coffers under the roof.
    b.ring(1, 1, 23, 18, 2, 9, BRICK)
    b.ring(1, 1, 23, 18, 8, 8, PANEL)
    b.ring(1, 1, 23, 18, 10, 10, CORNICE)
    b.ring(1, 1, 23, 18, 11, 11, BRICK)
    b.ring(1, 1, 23, 18, 12, 12, TRIM)
    b.fill(2, 9, 2, 22, 9, 17, COFFER)
    b.fill(2, 10, 2, 22, 10, 17, STONE)
    lining(b, 2, 2, 22, 17, 3, 8, 5)
    neon_line(b, 2, 22, 9, 19, 'south')

    # Glazed bays over the hedges, and on the sides above the machines.
    for x1, x2 in ((2, 5), (19, 22)):
        glazing(b, x1, x2, 4, 7, 18, 17, lintel=False)
        planter(b, x1, 19, x2, 19, 1)
    # The vault room in the back left corner keeps a blind wall; red strips framed in gold break up the plain
    # stretches of the sides and the back.
    side_glazing(b, 1, 14, 16, 4, 7, 2, lintel=False)
    for z1, z2 in ((4, 6), (14, 16)):
        side_glazing(b, 23, z1, z2, 4, 7, 22, lintel=False)
    b.fill(1, 3, 5, 1, 7, 5, PANEL)
    for x in (1, 23):
        b.fill(x, 3, 10, x, 7, 11, PANEL)
    for x in (4, 12, 20):
        b.fill(x, 3, 1, x, 7, 1, PANEL)

    # Corner pylons: a red strip framed in gold, a gold cap, a lantern.
    for x in (1, 23):
        b.fill(x, 1, 19, x, 11, 19, BRICK)
        b.fill(x, 3, 19, x, 8, 19, PANEL)
        b.set(x, 12, 19, TRIM)
        b.set(x, 13, 19, LANTERN)

    # Canopy over the porch on two onyx columns: a red fascia framed in gold with chasing lights along it, a gold
    # cornice on top, coffers and lanterns underneath.
    b.fill(10, 7, 19, 14, 7, 21, COFFER)
    for x in range(9, 16):
        b.set(x, 7, 22, PANEL)
        b.set(x, 7, 23, 'boltcraft_casino:marquee_lights[facing=south]')
    for x in (9, 15):
        b.fill(x, 7, 19, x, 7, 21, PANEL)
        b.fill(x, 3, 22, x, 6, 22, ONYX)
    b.fill(9, 8, 19, 15, 8, 22, TRIM)
    for x in (10, 14):
        b.set(x, 6, 20, HANGING_LANTERN)

    # Pylons flanking the canopy, a BoltCraft banner on each, a lantern at each corner of the sign.
    for x in (7, 16):
        b.fill(x, 1, 19, x + 1, 9, 19, BRICK)
        board(b, 'boltcraft_banner', 2, 4, x, 3, 20, 'south')
    for x in (7, 17):
        b.set(x, 11, 19, LANTERN)

    # The sign tower over the canopy: the marquee on a gold cornice, then a stepped crest of three chips with the
    # red one raised, gold cornices on each step, lanterns on top.
    b.fill(8, 12, 18, 16, 16, 18, BRICK)
    b.fill(10, 17, 18, 14, 17, 18, BRICK)
    b.fill(8, 9, 19, 16, 16, 19, BRICK)
    b.fill(10, 17, 19, 14, 17, 19, BRICK)
    b.fill(7, 10, 19, 17, 10, 19, TRIM)
    for x in range(8, 17):
        b.set(x, 10, 20, 'boltcraft_casino:marquee_lights[facing=south]')
    board(b, 'casino_marquee_small', 9, 3, 8, 11, 20, 'south')
    for color, x, y in (('blue', 8, 14), ('red', 11, 15), ('black', 14, 14)):
        board(b, f'giant_chip_{color}', 3, 3, x, y, 20, 'south')
    for x1, x2, y in ((8, 9, 17), (15, 16, 17), (10, 14, 18)):
        b.fill(x1, y, 18, x2, y, 19, TRIM)
    for x, y in ((8, 18), (16, 18), (12, 19)):
        b.set(x, y, 19, LANTERN)

    # Entrance: a curtained portal, carpeted steps down to the forecourt, lantern posts.
    opening(b, [(x, z) for x in range(11, 14) for z in (17, 18)], 3, 5)
    b.curtain(11, 13, 3, 5, 17, 'north')
    flight(b, 9, 15, range(11, 14), 2, 23, ())
    b.fill(11, 0, 25, 13, 0, 26, CARPET)
    for x in (2, 22):
        lamp_post(b, x, 1, 25)

    # Vault room in the back left corner behind a panelled partition and a staff door.
    b.fill(8, 3, 3, 8, 8, 5, WOOD_PANEL)
    b.fill(3, 3, 6, 8, 8, 6, WOOD_PANEL)
    b.door(5, 3, 6, 'south')
    b.set(4, 3, 3, 'boltcraft_casino:vault[facing=south]')
    b.chest(7, 3, 3, 'south', 'casino_lounge')
    b.set(3, 5, 4, 'boltcraft_casino:wall_sconce[facing=east]')

    # Cashier between the vault room and the bar, the chip sign above it.
    b.set(9, 3, 4, 'boltcraft_casino:bar_counter[facing=south]')
    b.set(10, 3, 4, 'boltcraft_casino:cashier[facing=south]')
    b.set(11, 3, 4, 'boltcraft_casino:bar_counter[facing=south]')
    b.set(10, 6, 3, 'boltcraft_casino:neon_sign_chip[facing=south]')

    # Bar in the back right corner, the BAR board over its shelves.
    bar(b, 14, 20, 3, 4, 3, 6)
    b.panel('bar', 2, 16, 7, 3, 'south')
    b.chest(21, 3, 3, 'south', 'casino_lounge')

    # Slot machines down both side walls under the JACKPOT board, stools a step back.
    for z in (8, 10, 12, 14):
        b.slot_machine(3, 3, z, 'east')
        b.set(5, 3, z, 'boltcraft_casino:bar_stool')
        b.slot_machine(21, 3, z, 'west')
        b.set(19, 3, z, 'boltcraft_casino:bar_stool')
    b.panel('jackpot', 4, 3, 6, 12, 'east')

    # Roulette in the middle under the grand chandelier, the croupier's place behind it.
    b.roulette(12, 3, 10, 'south')
    for x in (11, 12):
        b.set(x, 3, 12, 'boltcraft_casino:bar_stool')
    chandelier(b, 12, 7, 9, chain=1, grand=True)
    for x, z in ((6, 11), (18, 11), (12, 15), (17, 7)):
        chandelier(b, x, 8, z)

    # A lounge corner by the front windows.
    b.sofa(7, 3, 16, 'north', 3)
    coffee(b, 8, 3, 14, 'table_lamp')
    card_table(b, 16, 3, 15)
    for x, z in ((3, 16), (21, 16), (3, 7), (21, 7)):
        b.set(x, 3, z, 'boltcraft_casino:gold_planter')

    # Where a visitor stands: the block above the floor.
    b.markers = {
        'entrance': [12, 1, 26], 'foyer': [12, 3, 15], 'cashier': [10, 3, 6],
        'roulette': [14, 3, 11], 'bar': [17, 3, 7], 'bartender': [17, 3, 3],
        'vault': [5, 3, 4], 'staff': [5, 3, 7],
    }
    b.save()


# ---------------------------------------------------------------------------------------------- grand hall

def build_hall(building):
    """A two-storey casino palace after concepts 1 and 4: a tower with the large marquee and a stepped chip crest
    between bannered pylons; a deep canopy on four piers whose roof is a terrace off the upper floor; glazed wings
    with a red band and gold cornices; a forecourt with a fountain, hedges and lantern posts. Inside, an atrium with
    a grand staircase under the grand chandelier, a slot hall and a gaming room, a bar under the landing, a lounge,
    and upstairs galleries round the atrium, a VIP floor with roulette and the vault room, a lounge and the
    terrace room in the tower."""
    b = building('grand_hall', 41, 33, 46)
    b.clear(0, 1, 0, 40, 32, 45)
    # Ground: foundation, the plinth up to the hall floor at y = 3, the porch, a paved forecourt.
    b.fill(0, 0, 0, 40, 0, 45, KERB)
    b.fill(0, 0, 31, 40, 0, 45, PAVING)
    for x in range(0, 41, 8):
        b.fill(x, 0, 39, x, 0, 45, KERB)
    b.fill(1, 1, 1, 39, 2, 30, STONE)
    b.fill(1, 3, 1, 39, 3, 30, PLANKS)
    b.fill(3, 3, 3, 37, 3, 28, CARPET)
    b.fill(14, 3, 21, 26, 3, 28, MARBLE)
    b.fill(19, 3, 21, 21, 3, 29, CARPET)
    b.fill(14, 3, 3, 26, 3, 7, CHECKER)
    b.fill(27, 3, 3, 37, 3, 21, CHECKER)
    b.fill(29, 3, 5, 36, 3, 19, CARPET)
    b.fill(10, 1, 31, 30, 2, 38, STONE)
    b.fill(10, 3, 31, 30, 3, 38, MARBLE)
    b.fill(19, 3, 30, 21, 3, 38, CARPET)

    # Walls to the roof cornice, a parapet, the roof over coffered ceilings; panelled linings on both floors.
    b.ring(1, 1, 39, 30, 3, 21, BRICK)
    b.ring(1, 1, 39, 30, 11, 11, PANEL)
    b.ring(1, 1, 39, 30, 12, 12, CORNICE)
    b.ring(1, 1, 39, 30, 22, 22, CORNICE)
    b.ring(1, 1, 39, 30, 23, 23, BRICK)
    b.ring(1, 1, 39, 30, 24, 24, TRIM)
    b.fill(2, 21, 2, 38, 21, 29, COFFER)
    b.fill(2, 22, 2, 38, 22, 29, STONE)
    lining(b, 2, 2, 38, 29, 4, 11, 6)
    b.ring(2, 2, 38, 29, 12, 12, PLANKS)
    lining(b, 2, 2, 38, 29, 13, 20, 6)
    neon_line(b, 2, 38, 21, 31, 'south')

    # Glazed bays: both floors of both wings at the front, the upper floor at the sides and back, the lounge.
    for x1, x2, mullions in ((3, 11, (5, 9)), (29, 37, (31, 35))):
        glazing(b, x1, x2, 4, 9, 30, 29, mullions)
        glazing(b, x1, x2, 14, 19, 30, 29, mullions)
    for x, lining_x in ((1, 2), (39, 38)):
        for z1, z2, mullion in ((4, 10, 7), (17, 23, 20)):
            side_glazing(b, x, z1, z2, 5, 9, lining_x, (mullion,))
            # The vault room upstairs in the north-west corner keeps a blind wall.
            if (x, z1) != (1, 4):
                side_glazing(b, x, z1, z2, 15, 19, lining_x, (mullion,))
        side_glazing(b, x, 25, 27, 5, 9, lining_x)
        b.fill(1, 14, 6, 1, 19, 7, PANEL)
        # A red strip framed in gold on each floor between the bays, as on the pylons at the front.
        b.fill(x, 4, 13, x, 9, 14, PANEL)
        b.fill(x, 14, 13, x, 19, 14, PANEL)
    glazing(b, 15, 25, 15, 19, 1, 2, (18, 22))
    glazing(b, 29, 35, 15, 19, 1, 2, (32,))
    for x in (5, 12, 27, 34):
        b.fill(x, 4, 1, x + 1, 9, 1, PANEL)
    for x in (12, 27):
        b.fill(x, 14, 1, x + 1, 19, 1, PANEL)

    # Entrance: a tall curtained portal into the lobby.
    opening(b, [(x, z) for x in range(18, 23) for z in (29, 30)], 4, 8)
    b.curtain(18, 22, 4, 8, 29, 'north')

    # Canopy over the porch: a red fascia framed in gold hanging two blocks deep with chasing lights along it,
    # coffers and hanging lanterns underneath. Four thick piers with gold capitals carry it; its roof is a terrace
    # with a gilded railing.
    b.fill(10, 12, 31, 30, 12, 38, COFFER)
    for x in range(10, 31):
        b.fill(x, 10, 39, x, 11, 39, PANEL)
        b.set(x, 10, 40, 'boltcraft_casino:marquee_lights[facing=south]')
        b.set(x, 12, 39, TRIM)
        b.set(x, 13, 39, RAILING)
    for x in (10, 30):
        for z in range(31, 39):
            b.fill(x, 10, z, x, 11, z, PANEL)
            b.set(x, 12, z, TRIM)
            b.set(x, 13, z, RAILING)
    for x in (10, 16, 23, 29):
        b.fill(x, 4, 37, x + 1, 4, 38, TRIM)
        b.fill(x, 5, 37, x + 1, 9, 38, BRICK)
        b.fill(x, 6, 38, x + 1, 8, 38, PANEL)
        b.fill(x, 10, 37, x + 1, 10, 38, TRIM)
        b.fill(x, 11, 37, x + 1, 11, 38, BRICK)
    for z in range(31, 37):
        for x in (10, 30):
            b.set(x, 4, z, RAILING)
    for x in (14, 20, 26):
        b.set(x, 11, 35, HANGING_LANTERN)
    b.fill(11, 13, 31, 29, 13, 38, 'boltcraft_casino:casino_carpet_burgundy')
    for x in (12, 28):
        b.set(x, 13, 37, 'boltcraft_casino:gold_planter')

    # Pylons: the corners and the pair flanking the tower, red fields in gold, lanterns on the caps; banners hang
    # on the tower pylons over the terrace.
    for x in (1, 38):
        b.fill(x, 1, 31, x + 1, 23, 31, BRICK)
        b.fill(x, 4, 31, x + 1, 9, 31, PANEL)
        b.fill(x, 14, 31, x + 1, 19, 31, PANEL)
        b.fill(x, 24, 31, x + 1, 24, 31, TRIM)
        b.set(x, 25, 31, LANTERN)
    for x in (12, 27):
        b.fill(x, 4, 31, x + 1, 25, 32, BRICK)
        b.fill(x, 4, 32, x + 1, 9, 32, PANEL)
        b.fill(x, 20, 32, x + 1, 23, 32, PANEL)
        b.fill(x, 26, 31, x + 1, 26, 32, TRIM)
        b.set(x, 27, 32, LANTERN)
        board(b, 'boltcraft_banner', 2, 4, x, 15, 33, 'south')

    # The tower: the large marquee on a gold cornice, the stepped crest of chips with the red one raised, gold
    # cornices on each step, lanterns on top. Inside it, over the entrance, the terrace room.
    b.fill(14, 13, 31, 26, 28, 32, BRICK)
    b.fill(18, 29, 32, 22, 29, 32, BRICK)
    b.fill(14, 20, 33, 26, 20, 33, TRIM)
    for x in range(14, 27):
        b.set(x, 20, 34, 'boltcraft_casino:marquee_lights[facing=south]')
    board(b, 'casino_marquee', 13, 4, 14, 21, 33, 'south')
    b.fill(14, 25, 33, 26, 25, 33, CORNICE)
    for color, x, y in (('blue', 15, 26), ('red', 19, 27), ('black', 23, 26)):
        board(b, f'giant_chip_{color}', 3, 3, x, y, 33, 'south')
    for x1, x2, y in ((14, 17, 29), (23, 26, 29), (18, 22, 30)):
        b.fill(x1, y, 32, x2, y, 33, TRIM)
    for x, y in ((14, 30), (26, 30), (20, 31)):
        b.set(x, y, 33, LANTERN)
    b.clear(14, 13, 29, 26, 19, 30)
    b.clear(15, 13, 31, 25, 19, 31)
    b.fill(14, 12, 29, 26, 12, 31, NAVY)
    glazing(b, 15, 25, 13, 19, 32, 31, (16, 18, 22, 24))
    b.clear(19, 13, 32, 21, 16, 32)
    b.curtain(19, 21, 13, 16, 31, 'north')

    # Steps down to the forecourt: a carpet runner between stone stairs, gilded railings on the cheeks.
    flight(b, 11, 29, range(18, 23), 3, 39, (10, 30))
    b.fill(18, 0, 42, 22, 0, 45, CARPET)

    # Garden yards either side of the porch: a fountain on the left, hedges on the right, lanterns at the foot of
    # the steps and along the front of the forecourt.
    fountain(b, 5, 35, 1)
    planter(b, 32, 32, 38, 33, 1, flowers=True)
    planter(b, 32, 36, 33, 37, 1)
    planter(b, 37, 36, 38, 37, 1)
    for x, z in ((10, 42), (30, 42), (2, 44), (14, 44), (26, 44), (38, 44)):
        lamp_post(b, x, 1, z)

    # Lobby: the cashier desk on its west side, the office behind it.
    for z in (24, 26, 28):
        b.set(12, 4, z, 'boltcraft_casino:bar_counter[facing=east]')
    for z in (25, 27):
        b.set(12, 4, z, 'boltcraft_casino:cashier[facing=east]')
    b.fill(3, 4, 22, 12, 10, 22, WOOD_PANEL)
    b.door(8, 4, 22, 'north')
    b.chest(3, 4, 28, 'east', 'casino_hall')
    b.set(12, 8, 26, 'boltcraft_casino:neon_sign_chip[facing=east]')

    # Slot hall in the west wing: two islands of machines back to back under the JACKPOT board.
    xs = (4, 6, 8, 10)
    slot_row(b, xs, 4, 9, 'north', 7)
    slot_row(b, xs, 4, 10, 'south', 12)
    slot_row(b, xs, 4, 16, 'north', 14)
    slot_row(b, xs, 4, 17, 'south', 19)
    b.panel('jackpot', 4, 3, 8, 15, 'east')

    # Gaming room in the east wing: two roulette tables and two card tables.
    for z in (9, 17):
        b.roulette(35, 4, z, 'south')
        for x in (34, 35):
            b.set(x, 4, z + 2, 'boltcraft_casino:bar_stool')
    for z in (8, 16):
        card_table(b, 30, 4, z)

    # Bar under the landing, the BAR board above its shelves.
    bar(b, 15, 25, 4, 6, 3, 8)
    b.panel('bar', 2, 19, 9, 3, 'south')
    b.chest(37, 4, 3, 'west', 'casino_hall')

    # Lounge in the east of the lobby.
    b.sofa(29, 4, 28, 'north', 4)
    for x, prop in ((30, 'table_lamp'), (32, 'serving_tray')):
        coffee(b, x, 4, 26, prop)
    for x, z in ((14, 21), (26, 21), (37, 22), (3, 21)):
        b.set(x, 4, z, 'boltcraft_casino:gold_planter')

    # Upper floor on coffered ceilings: both wings, a landing across the back, a gallery across the front.
    for x1, z1, x2, z2 in ((3, 3, 13, 28), (27, 3, 37, 28), (14, 3, 26, 11), (14, 25, 26, 28)):
        b.fill(x1, 12, z1, x2, 12, z2, COFFER)
        b.fill(x1, 13, z1, x2, 13, z2, NAVY_RUG)

    # Grand staircase up the middle of the atrium to the landing, carpeted, between gilded railings.
    for step in range(9):
        y, z = 4 + step, 20 - step
        if y > 4:
            b.fill(18, 4, z, 22, y - 1, z, BRICK)
        b.fill(18, y, z, 22, y, z, CARPET_STAIR + '[facing=north]')
        for x in (17, 23):
            b.fill(x, 4, z, x, y, z, BRICK)
            b.set(x, y + 1, z, RAILING)
    b.stair_volume = [18, 4, 12, 22, 13, 20]

    # Railings along every gallery edge over the atrium; onyx columns carry the galleries.
    for z in range(12, 25):
        b.set(13, 13, z, RAILING)
        b.set(27, 13, z, RAILING)
    for x in list(range(14, 17)) + list(range(24, 27)):
        b.set(x, 13, 11, RAILING)
    for x in range(14, 27):
        b.set(x, 13, 25, RAILING)
    for x, z in ((13, 12), (27, 12), (13, 24), (27, 24), (13, 28), (27, 28)):
        b.fill(x, 4, z, x, 20, z, ONYX)
    chandelier(b, 20, 16, 17, chain=4, grand=True)
    for x, z in ((16, 14), (24, 14), (16, 21), (24, 21)):
        chandelier(b, x, 17, z, chain=3, grand=True)

    # VIP floor in the west wing: the vault room in the corner, roulette, a card table, a sofa.
    b.fill(9, 13, 3, 9, 20, 8, WOOD_PANEL)
    b.fill(3, 13, 9, 9, 20, 9, WOOD_PANEL)
    b.door(6, 13, 9, 'south')
    b.panel('vip', 2, 5, 16, 10, 'south')
    b.set(4, 13, 3, 'boltcraft_casino:vault[facing=south]')
    b.chest(8, 13, 3, 'south', 'casino_hall')
    b.chest(3, 13, 6, 'east', 'casino_hall')
    b.roulette(9, 13, 16, 'south')
    for x in (8, 9):
        b.set(x, 13, 18, 'boltcraft_casino:bar_stool')
    card_table(b, 6, 13, 24)
    b.sofa(3, 13, 12, 'east', 3)
    coffee(b, 5, 13, 13, 'serving_tray')

    # Lounge on the east gallery, looking down into the atrium.
    b.sofa(36, 13, 6, 'west', 4)
    coffee(b, 34, 13, 5, 'table_lamp')
    b.sofa(36, 13, 18, 'west', 4)
    coffee(b, 34, 13, 16, 'serving_tray')
    card_table(b, 31, 13, 11)

    # Card tables on the landing at the top of the stairs; sofas on the front gallery facing the chandeliers,
    # their tables a step ahead so there is room for the legs.
    for x in (17, 23):
        card_table(b, x, 13, 6)
    for x, prop in ((15, 'table_lamp'), (23, 'serving_tray')):
        b.sofa(x, 13, 28, 'north', 3)
        coffee(b, x + 1, 13, 26, prop)
    for x, z in ((26, 4), (14, 4), (37, 27), (3, 27)):
        b.set(x, 13, z, 'boltcraft_casino:gold_planter')

    # Light: chandeliers under the coffers of both floors, lanterns over the bar.
    for x, z in ((7, 5), (7, 13), (7, 20), (33, 5), (33, 13), (33, 20)):
        chandelier(b, x, 11, z)
        chandelier(b, x, 20, z)
    for x in (16, 20, 24):
        b.set(x, 11, 5, HANGING_LANTERN)
    chandelier(b, 20, 11, 26)

    b.markers = {
        'entrance': [20, 1, 45], 'foyer': [20, 4, 26], 'cashier': [14, 4, 26],
        'roulette': [33, 4, 13], 'bar': [20, 4, 9], 'bartender': [20, 4, 4],
        'upper': [20, 13, 8], 'vault': [5, 13, 5], 'balcony': [20, 13, 35],
        'gallery': [20, 13, 27], 'stair_landing': [16, 13, 10],
    }
    b.save()
