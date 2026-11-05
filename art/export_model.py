"""Turns Blockbench Java exports into the mod's block and item model files.

    python art/export_model.py block models/cashier.json cashier
        A Java block model: points its texture at textures/block/<name>.png, its particle at
        <name>_particle.png and inherits vanilla's block display transforms for the inventory icon.

    python art/export_model.py geckolib-item models/vault_item.json vault
        The inventory model of a GeckoLib block. GeckoLib models are centred on the block (x and z from -8
        to 8) while Java models use 0 to 16, so every element is shifted by 8 on x and z first.

    python art/export_model.py geckolib-item-tall models/slot_machine_item.json slot_machine
        The same for a block two blocks tall, scaled down so it fits an inventory slot and a hand.

    python art/export_model.py geckolib-item-wide models/roulette_table_item.json roulette_table
        The same for a 2x2 block centred on the middle of its four blocks.

Java models in 1.21.1 rotate elements by 45 degrees at most, so elements turned further are left out:
those are the reel faces hidden inside the slot machine, which only the GeckoLib model needs.
"""
from pathlib import Path
import json
import sys

ROOT = Path(__file__).resolve().parents[1]
MODID = 'boltcraft_casino'
MODELS = ROOT / f'src/main/resources/assets/{MODID}/models/block'

# Display transforms for models two blocks tall: vanilla block/block scaled down, and turned so the front, not
# the side, faces the player in the inventory and in first person. Left-hand rotations are mirrored by the game.
TALL_DISPLAY = {
    'gui': {'rotation': [30, 155, 0], 'translation': [0, -3, 0], 'scale': [0.36, 0.36, 0.36]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 1, 0], 'scale': [0.2, 0.2, 0.2]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, -2.5, 0], 'scale': [0.3, 0.3, 0.3]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 1, 0], 'scale': [0.25, 0.25, 0.25]},
    'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 1, 0], 'scale': [0.25, 0.25, 0.25]},
    'firstperson_righthand': {'rotation': [0, 160, 0], 'translation': [0, -0.5, 0], 'scale': [0.2, 0.2, 0.2]},
    'firstperson_lefthand': {'rotation': [0, 160, 0], 'translation': [0, -0.5, 0], 'scale': [0.2, 0.2, 0.2]},
}


# Display transforms for 2x2 models: the whole table shown from above at a quarter of its size.
WIDE_DISPLAY = {
    'gui': {'rotation': [30, 205, 0], 'translation': [0, 1, 0], 'scale': [0.3, 0.3, 0.3]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.15, 0.15, 0.15]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.3, 0.3, 0.3]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.2, 0.2, 0.2]},
    'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.2, 0.2, 0.2]},
    'firstperson_righthand': {'rotation': [0, 160, 0], 'translation': [0, 1, 0], 'scale': [0.18, 0.18, 0.18]},
    'firstperson_lefthand': {'rotation': [0, 160, 0], 'translation': [0, 1, 0], 'scale': [0.18, 0.18, 0.18]},
}


def shift_to_block_space(elements):
    for element in elements:
        for corner in ('from', 'to'):
            element[corner][0] += 8
            element[corner][2] += 8
        rotation = element.get('rotation')
        if rotation and 'origin' in rotation:
            rotation['origin'][0] += 8
            rotation['origin'][2] += 8


def fits_java_rotation(element):
    rotation = element.get('rotation')
    return rotation is None or abs(rotation['angle']) <= 45


def export(kind, source, name):
    model = json.loads(Path(source).read_text(encoding='utf-8'))
    elements = [element for element in model['elements'] if fits_java_rotation(element)]
    target = MODELS / f'{name}.json'
    if kind in ('geckolib-item', 'geckolib-item-tall', 'geckolib-item-wide'):
        shift_to_block_space(elements)
        target = MODELS / f'{name}_item.json'
    elif kind != 'block':
        raise SystemExit(f'Unknown model kind {kind}')
    result = {
        'parent': 'minecraft:block/block',
        'texture_size': model['texture_size'],
        'textures': {'0': f'{MODID}:block/{name}', 'particle': f'{MODID}:block/{name}_particle'},
        'elements': elements,
    }
    if kind == 'geckolib-item-tall':
        result['display'] = TALL_DISPLAY
    elif kind == 'geckolib-item-wide':
        result['display'] = WIDE_DISPLAY
    target.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(f'{source} -> {target.relative_to(ROOT)}')


if __name__ == '__main__':
    export(sys.argv[1], sys.argv[2], sys.argv[3])
