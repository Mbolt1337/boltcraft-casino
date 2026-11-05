"""Writes the slot machine's GeckoLib animations and copies the jackpot keys into models/slot_machine.bbmodel.

    python art/slot_machine_animations.py

The keys are written here rather than in Blockbench because the jackpot's running lights are dozens of keys that
follow one rule. Bedrock rotations negate Blockbench 5's X and Y; scales are the same in both.
"""
from pathlib import Path
import json
import uuid

ROOT = Path(__file__).resolve().parents[1]
ANIMATIONS = ROOT / 'src/main/resources/assets/boltcraft_casino/animations/block/slot_machine.animation.json'
BBMODEL = ROOT / 'models/slot_machine.bbmodel'

REST = [1, 1, 1]
BULBS = 6

# Lever: pulled towards the player, held, let go. Blockbench shows the pull as -55 degrees.
LEVER_PULL = [(0, 0), (0.2, 55), (0.3, 55), (0.7, 0)]

# Win: the whole lamp bar swells three times in one second.
WIN_LAMPS = [(0, REST), (0.15, [1.25, 1.6, 1.6]), (0.3, REST), (0.45, [1.25, 1.6, 1.6]), (0.6, REST),
             (0.75, [1.25, 1.6, 1.6]), (1.0, REST)]

# Jackpot: a light runs along the bulbs from left to right five times while the topper beats every half second.
JACKPOT_LENGTH = 3.0
CHASE_CYCLE = 0.55
CHASE_STEP = 0.1
CHASE_CYCLES = 5
BULB_LIT = [1.9, 1.9, 1.9]
MARQUEE_BEAT = 0.5
MARQUEE_BEATS = 6
MARQUEE_SWOLLEN = [1.06, 1.12, 1.06]


def chase(bulb):
    keys = []
    for cycle in range(CHASE_CYCLES):
        start = cycle * CHASE_CYCLE + bulb * CHASE_STEP
        keys += [(start, REST), (start + 0.05, BULB_LIT), (start + 0.15, REST)]
    return keys


def beats():
    keys = []
    for beat in range(MARQUEE_BEATS):
        start = beat * MARQUEE_BEAT
        keys += [(start, REST), (start + 0.12, MARQUEE_SWOLLEN), (start + 0.3, REST)]
    return keys


def jackpot_bones():
    bones = {f'bulb_{bulb + 1}': chase(bulb) for bulb in range(BULBS)}
    bones['marquee'] = beats()
    return bones


def bedrock(keys):
    return {f'{round(time, 3):g}': value for time, value in keys}


def write_animations():
    jackpot = {bone: {'scale': bedrock(keys)} for bone, keys in jackpot_bones().items()}
    animations = {
        'format_version': '1.8.0',
        'animations': {
            'animation.slot_machine.spin': {'animation_length': 0.7, 'bones': {
                'lever': {'rotation': bedrock([(time, [angle, 0, 0]) for time, angle in LEVER_PULL])}}},
            'animation.slot_machine.win': {'animation_length': 1.0, 'bones': {
                'lamps': {'scale': bedrock(WIN_LAMPS)}}},
            'animation.slot_machine.jackpot': {'animation_length': JACKPOT_LENGTH, 'bones': jackpot},
        },
    }
    ANIMATIONS.write_text(json.dumps(animations, indent=2) + '\n', encoding='utf-8')
    print(f'-> {ANIMATIONS.relative_to(ROOT)}')


def write_bbmodel_jackpot():
    model = json.loads(BBMODEL.read_text(encoding='utf-8'))
    groups = {group['name']: group['uuid'] for group in model['groups']}
    clip = next(a for a in model['animations'] if a['name'] == 'animation.slot_machine.jackpot')
    clip['length'] = JACKPOT_LENGTH
    clip['animators'] = {}
    for bone, keys in jackpot_bones().items():
        clip['animators'][groups[bone]] = {'name': bone, 'type': 'bone', 'keyframes': [{
            'channel': 'scale',
            'data_points': [{'x': str(value[0]), 'y': str(value[1]), 'z': str(value[2])}],
            'uuid': str(uuid.uuid4()),
            'time': round(time, 3),
            'color': -1,
            'interpolation': 'linear',
        } for time, value in keys]}
    BBMODEL.write_text(json.dumps(model, indent='\t'), encoding='utf-8')
    print(f'-> {BBMODEL.relative_to(ROOT)}')


if __name__ == '__main__':
    write_animations()
    write_bbmodel_jackpot()
