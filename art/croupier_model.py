"""Builds models/croupier.bbmodel, the villager croupier standing behind the roulette table.

    python art/build.py art/textures/croupier.txt
    python art/croupier_model.py
    python art/export_animations.py models/croupier.bbmodel \
        src/main/resources/assets/boltcraft_casino/animations/entity/croupier.animation.json

Villager proportions: head 8x10x8 with the long nose, body 8x12x6, legs 4x12x4. Unlike a villager his arms are
free, 4x12x4 like an illager's, hinged at the shoulders, so he can turn the wheel and wave over the layout. The
body bone holds the head and arms, so leaning over the table carries them along.

Rotations are in Blockbench 5 degrees: +X raises an arm forward or tips the head back; -X leans the body forward.
"""
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from bbmodel_builder import Model  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
TXT = ROOT / 'art/textures/croupier.txt'
PNG = ROOT / 'src/main/resources/assets/boltcraft_casino/textures/entity/croupier.png'
REST = 6


def limb(front, back, outer, inner, top, bottom, right):
    """Faces of an arm or leg on the croupier's right (+X) or left side."""
    faces = dict(north=front, south=back, up=top, down=bottom)
    faces.update(east=outer, west=inner) if right else faces.update(west=outer, east=inner)
    return faces


def build():
    model = Model('croupier', TXT, PNG, 64, 64)
    root = model.group('croupier', (0, 0, 0))

    body = model.group('body', (0, 12, 0), root)
    model.cube('torso', (-4, 12, -3), (4, 24, 3), body,
               dict(north='body_front', south='body_back', east='body_side', west='body_side', up='body_top',
                    down='body_bottom'))
    model.cube('bow_tie', (-2, 22, -3.5), (2, 23, -3), body,
               dict(north='bow_front', east='bow_side', west='bow_side', up='bow_front', down='bow_front'))

    head = model.group('head', (0, 24, 0), body)
    model.cube('skull', (-4, 24, -4), (4, 34, 4), head,
               dict(north='head_front', south='head_back', east='head_right', west='head_left', up='head_top',
                    down='head_bottom'))
    model.cube('nose', (-1, 23, -6), (1, 27, -4), head,
               dict(north='nose_front', east='nose_side', west='nose_side', up='nose_top', down='nose_bottom'))

    arm = ('arm_front', 'arm_back', 'arm_outer', 'arm_inner', 'arm_top', 'arm_bottom')
    right_arm = model.group('right_arm', (5, 22, 0), body)
    model.cube('right_sleeve', (4, 12, -2), (8, 24, 2), right_arm, limb(*arm, right=True))
    left_arm = model.group('left_arm', (-5, 22, 0), body)
    model.cube('left_sleeve', (-8, 12, -2), (-4, 24, 2), left_arm, limb(*arm, right=False))

    leg = ('leg_front', 'leg_back', 'leg_outer', 'leg_inner', 'leg_top', 'leg_bottom')
    right_leg = model.group('right_leg', (2, 12, 0), root)
    model.cube('right_trouser', (0, 0, -2), (4, 12, 2), right_leg, limb(*leg, right=True))
    left_leg = model.group('left_leg', (-2, 12, 0), root)
    model.cube('left_trouser', (-4, 0, -2), (0, 12, 2), left_leg, limb(*leg, right=False))

    animations(model)
    model.save(ROOT / 'models/croupier.bbmodel')


def rot(*keys):
    return [(time, list(value)) for time, value in keys]


def animations(model):
    # Waiting: looks from one side of the table to the other, arms hanging just in front of him.
    model.animation('animation.croupier.idle', 6, {
        'head': {'rotation': rot((0, (0, 0, 0)), (1.5, (0, 12, 0)), (3, (0, 0, 0)), (4.5, (0, -12, 0)),
                                 (6, (0, 0, 0)))},
        'right_arm': {'rotation': rot((0, (REST, 0, 0)), (3, (REST + 3, 0, 0)), (6, (REST, 0, 0)))},
        'left_arm': {'rotation': rot((0, (REST, 0, 0)), (3, (REST + 3, 0, 0)), (6, (REST, 0, 0)))},
    }, loop='loop')

    # A spin starts: he leans over the wheel, turns it by the turret with his right hand and flicks the ball
    # away with his left.
    model.animation('animation.croupier.launch_ball', 2, {
        'body': {'rotation': rot((0, (0, 0, 0)), (0.4, (-14, 0, 0)), (1.3, (-14, 0, 0)), (1.8, (0, 0, 0)))},
        'head': {'rotation': rot((0, (0, 0, 0)), (0.4, (-10, 0, 0)), (1.3, (-10, 0, 0)), (1.8, (0, 0, 0)))},
        'right_arm': {'rotation': rot((0, (REST, 0, 0)), (0.4, (70, 0, -12)), (0.55, (70, 0, -12)),
                                      (0.85, (62, -35, -12)), (1.2, (40, 0, -6)), (1.6, (REST, 0, 0)))},
        'left_arm': {'rotation': rot((0, (REST, 0, 0)), (0.8, (40, 0, 12)), (1.0, (78, 0, 12)),
                                     (1.15, (52, 0, 8)), (1.7, (REST, 0, 0)))},
    })

    # "No more bets": the right hand sweeps flat over the layout and comes back.
    model.animation('animation.croupier.close_bets', 1.6, {
        'body': {'rotation': rot((0, (0, 0, 0)), (0.35, (-6, 0, 0)), (1.15, (-6, 0, 0)), (1.6, (0, 0, 0)))},
        'right_arm': {'rotation': rot((0, (REST, 0, 0)), (0.35, (80, 0, 0)), (0.6, (80, 0, -25)),
                                      (0.85, (80, 0, 15)), (1.05, (80, 0, -8)), (1.15, (80, 0, 0)),
                                      (1.6, (REST, 0, 0)))},
    })

    # The result: a look down at the wheel, then he points at the number and opens his left hand to the players.
    model.animation('animation.croupier.announce_result', 2, {
        'head': {'rotation': rot((0, (0, 0, 0)), (0.35, (-18, 0, 0)), (0.6, (-18, 0, 0)), (0.9, (0, 0, 0)),
                                 (1.3, (0, 10, 0)), (1.9, (0, 0, 0)))},
        'right_arm': {'rotation': rot((0, (REST, 0, 0)), (0.9, (55, 0, -8)), (1.5, (55, 0, -8)),
                                      (2, (REST, 0, 0)))},
        'left_arm': {'rotation': rot((0, (REST, 0, 0)), (0.9, (REST, 0, 0)), (1.2, (35, 0, -25)),
                                     (1.6, (35, 0, -25)), (2, (REST, 0, 0)))},
    })


if __name__ == '__main__':
    build()
