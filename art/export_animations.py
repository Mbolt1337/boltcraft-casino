"""Writes a model's Blockbench animations as a GeckoLib (Bedrock) animation file.

    python art/export_animations.py <model.bbmodel> <animation.json>
    e.g. models/croupier.bbmodel and src/main/resources/assets/boltcraft_casino/animations/entity/croupier.animation.json

Blockbench 5 stores rotations with X and Y negated relative to Bedrock, and positions with X negated, because the
Bedrock geometry export mirrors X. Scales are the same in both.
"""
from pathlib import Path
import json
import sys


def bedrock(channel, value):
    x, y, z = value
    if channel == 'rotation':
        return [-x, -y, z]
    if channel == 'position':
        return [-x, y, z]
    return [x, y, z]


def export(source, target):
    model = json.loads(Path(source).read_text(encoding='utf-8'))
    animations = {}
    for clip in model.get('animations', []):
        bones = {}
        for animator in clip.get('animators', {}).values():
            channels = {}
            for key in sorted(animator.get('keyframes', []), key=lambda frame: frame['time']):
                point = key['data_points'][0]
                value = [float(point[axis]) for axis in 'xyz']
                channels.setdefault(key['channel'], {})[f"{key['time']:g}"] = bedrock(key['channel'], value)
            bones[animator['name']] = channels
        entry = {'animation_length': clip['length'], 'bones': bones}
        if clip.get('loop') == 'loop':
            entry['loop'] = True
        animations[clip['name']] = entry
    Path(target).write_text(json.dumps({'format_version': '1.8.0', 'animations': animations}, indent=2) + '\n',
                            encoding='utf-8')
    print(f'{source} -> {target}')


if __name__ == '__main__':
    export(sys.argv[1], sys.argv[2])
