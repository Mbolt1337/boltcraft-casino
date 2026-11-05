"""Copy approved MCP Java exports into game resources; preserve their geometry, UVs and manual atlas."""
from pathlib import Path
import json
import shutil
import copy

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/boltcraft_casino'
MODELS = ASSETS / 'models/block/hospitality'
MODELS.mkdir(parents=True, exist_ok=True)
shutil.copy2(ROOT / 'models/hospitality.png', ASSETS / 'textures/block/hospitality.png')

for name in ['dining_chair', 'serving_tray', 'table_lamp', 'bottle_shelf']:
    model = json.loads((ROOT / 'models' / f'{name}.json').read_text(encoding='utf-8'))
    model['parent'] = 'minecraft:block/block'
    model['render_type'] = 'minecraft:cutout'
    model['textures'] = {key: 'boltcraft_casino:block/hospitality' for key in model['textures']}
    model['textures']['particle'] = 'boltcraft_casino:block/hospitality'
    model.pop('groups', None)
    if name == 'dining_chair':
        model['display'] = {'gui': {'rotation': [30, 225, 0], 'translation': [0, -1, 0], 'scale': [.53] * 3}}
    if name == 'table_lamp':
        for cube in model['elements']:
            if cube['from'] == [3, 7, 3] and cube['to'] == [13, 8, 13]:
                cube['shade'] = False
                cube['neoforge_data'] = {'block_light': 15, 'sky_light': 15}
    (MODELS / f'{name}.json').write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')
    if name in ('table_lamp', 'serving_tray'):
        for lowering in range(1, 9):
            lowered = copy.deepcopy(model)
            for cube in lowered['elements']:
                for corner in ('from', 'to'):
                    cube[corner][1] -= lowering
                if 'rotation' in cube:
                    cube['rotation']['origin'][1] -= lowering
            (MODELS / f'{name}_lowered_{lowering}.json').write_text(
                json.dumps(lowered, indent=2) + '\n', encoding='utf-8')
    print(f'{name}: approved model -> game resources')
