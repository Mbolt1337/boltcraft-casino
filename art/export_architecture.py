"""Copy the approved Blockbench export and hand-painted texture into game resources."""
from pathlib import Path
import json
import shutil

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/boltcraft_casino'
MODEL_OUT = ASSETS / 'models/block/architecture'
TEXTURE_OUT = ASSETS / 'textures/block/architecture'


def main():
    model = json.loads((ROOT / 'models/gilded_trim.json').read_text(encoding='utf-8'))
    model['parent'] = 'minecraft:block/block'
    texture = 'boltcraft_casino:block/architecture/gilded_trim'
    model['textures'] = {key: texture for key in model['textures']}
    model['textures']['particle'] = texture
    for cube in model['elements']:
        # This approved model fills one voxel. Internal faces can be culled in long architectural runs.
        assert cube['from'] == [0, 0, 0] and cube['to'] == [16, 16, 16]
        for direction, face in cube['faces'].items():
            face['cullface'] = direction
    MODEL_OUT.mkdir(parents=True, exist_ok=True)
    TEXTURE_OUT.mkdir(parents=True, exist_ok=True)
    (MODEL_OUT / 'gilded_trim.json').write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')
    shutil.copy2(ROOT / 'models/gilded_trim.png', TEXTURE_OUT / 'gilded_trim.png')
    print('Approved gilded trim -> game resources')


if __name__ == '__main__':
    main()
