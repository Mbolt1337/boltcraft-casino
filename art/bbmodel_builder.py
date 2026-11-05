"""Writes GeckoLib .bbmodel files from a readable list of parts, so a model's geometry can be reviewed as code.

Textures stay hand-drawn in art/textures/*.txt: this module only reads where each tile sits in the atlas and points
cube faces at it. A face never stretches a tile: asking for a face bigger than its tile is an error, so every model
keeps vanilla density (one texture pixel per 1/16 block).

Coordinates are Blockbench's: x and z from -8 to 8 for one block, y up, the front of the model facing -Z. Seen
from the front, +X is on the left.
"""
from pathlib import Path
import base64
import json
import uuid as uuidlib

FACES = ('north', 'east', 'south', 'west', 'up', 'down')
SIDES = ('north', 'east', 'south', 'west')


def read_tiles(txt):
    """Tile name -> (u, v, width, height) as placed by art/build.py."""
    tiles = {}
    current = None
    for raw in Path(txt).read_text(encoding='utf-8').splitlines():
        line = raw.strip()
        if not line or line.startswith('#'):
            continue
        if line.startswith('@'):
            parts = line.split()
            current = None
            if parts[0] == '@tile':
                current = [int(parts[2]), int(parts[3]), 0, 0]
                tiles[parts[1]] = current
            continue
        if current is not None:
            current[2] = len(line)
            current[3] += 1
    return {name: tuple(values) for name, values in tiles.items()}


def face_size(face, size):
    dx, dy, dz = size
    return {'north': (dx, dy), 'south': (dx, dy), 'east': (dz, dy), 'west': (dz, dy),
            'up': (dx, dz), 'down': (dx, dz)}[face]


class Face:
    """A face drawn from a tile, starting {offset} pixels into it, optionally turned."""

    def __init__(self, tile, offset=(0, 0), rotation=0):
        self.tile = tile
        self.offset = offset
        self.rotation = rotation


class Model:
    def __init__(self, name, txt, png, width, height):
        self.name = name
        self.tiles = read_tiles(txt)
        self.png = Path(png)
        self.width = width
        self.height = height
        self.elements = []
        self.groups = {}
        self.children = {None: []}
        self.animations = []

    def group(self, name, origin, parent=None, rotation=(0, 0, 0)):
        group_uuid = str(uuidlib.uuid4())
        self.groups[name] = {'uuid': group_uuid, 'name': name, 'origin': list(origin), 'rotation': list(rotation),
                             'export': True, 'visibility': True, 'mirror_uv': False, 'reset': False, 'color': 0,
                             'autouv': 0, 'shade': True, 'locked': False}
        self.children[name] = []
        self.children[parent].append(('group', name))
        return name

    def cube(self, name, start, end, parent, faces, origin=None, rotation=None):
        """faces: face name -> Face or tile name; faces left out are not drawn."""
        size = [b - a for a, b in zip(start, end)]
        drawn = {}
        for face in FACES:
            spec = faces.get(face)
            if spec is None:
                drawn[face] = {'uv': [0, 0, 0, 0], 'texture': None}
                continue
            if isinstance(spec, str):
                spec = Face(spec)
            u, v, width, height = self.tiles[spec.tile]
            fw, fh = face_size(face, size)
            if spec.rotation in (90, 270):
                fw, fh = fh, fw
            ou, ov = spec.offset
            if ou + fw > width + 1e-6 or ov + fh > height + 1e-6 or ou < -1e-6 or ov < -1e-6:
                raise ValueError(f'{self.name}/{name}/{face}: {fw}x{fh} at {spec.offset} does not fit tile '
                                 f'{spec.tile} ({width}x{height})')
            entry = {'uv': [u + ou, v + ov, u + ou + fw, v + ov + fh], 'texture': 0}
            if spec.rotation:
                entry['rotation'] = spec.rotation
            drawn[face] = entry
        element = {'uuid': str(uuidlib.uuid4()), 'name': name, 'type': 'cube', 'from': list(start), 'to': list(end),
                   'origin': list(origin) if origin else [(a + b) / 2 for a, b in zip(start, end)],
                   'color': 0, 'box_uv': False, 'faces': drawn, 'rescale': False, 'locked': False,
                   'light_emission': 0, 'render_order': 'default', 'allow_mirror_modeling': True, 'autouv': 0}
        if rotation:
            element['rotation'] = list(rotation)
        self.elements.append(element)
        self.children[parent].append(('cube', element['uuid']))
        return element

    def animation(self, name, length, keys, loop='once'):
        """keys: bone -> channel -> [(time, [x, y, z]), ...] in Blockbench 5 conventions."""
        animators = {}
        for bone, channels in keys.items():
            frames = []
            for channel, values in channels.items():
                for time, value in values:
                    frames.append({'channel': channel,
                                   'data_points': [{'x': str(value[0]), 'y': str(value[1]), 'z': str(value[2])}],
                                   'uuid': str(uuidlib.uuid4()), 'time': round(time, 4), 'color': -1,
                                   'interpolation': 'linear'})
            animators[self.groups[bone]['uuid']] = {'name': bone, 'type': 'bone', 'keyframes': frames}
        self.animations.append({'uuid': str(uuidlib.uuid4()), 'name': name, 'loop': loop, 'length': length,
                                'snapping': 20, 'override': False, 'selected': False, 'anim_time_update': '',
                                'blend_weight': '', 'start_delay': '', 'loop_delay': '', 'animators': animators})

    def _outline(self, parent):
        nodes = []
        for kind, key in self.children[parent]:
            if kind == 'cube':
                nodes.append(key)
            else:
                nodes.append({'uuid': self.groups[key]['uuid'], 'isOpen': True, 'children': self._outline(key)})
        return nodes

    def save(self, path):
        source = 'data:image/png;base64,' + base64.b64encode(self.png.read_bytes()).decode()
        texture = {'uuid': str(uuidlib.uuid4()), 'name': self.png.name, 'source': source, 'path': '',
                   'relative_path': '', 'width': self.width, 'height': self.height, 'uv_width': self.width,
                   'uv_height': self.height, 'folder': '', 'namespace': '', 'id': '0', 'particle': False,
                   'use_as_default': False, 'layers_enabled': False, 'sync_to_project': '', 'render_mode': 'default',
                   'render_sides': 'auto', 'frame_time': 1, 'frame_order_type': 'loop', 'frame_order': '',
                   'frame_interpolate': False, 'visible': True, 'internal': True, 'saved': False}
        model = {'meta': {'format_version': '5.0', 'model_format': 'geckolib_model', 'box_uv': False},
                 'name': self.name, 'model_identifier': self.name, 'visible_box': [1, 1, 0],
                 'resolution': {'width': self.width, 'height': self.height},
                 'elements': self.elements, 'groups': list(self.groups.values()), 'outliner': self._outline(None),
                 'textures': [texture], 'variable_placeholders': '', 'variable_placeholder_buttons': [],
                 'timeline_setups': [], 'unhandled_root_fields': {}, 'animations': self.animations}
        Path(path).write_text(json.dumps(model, indent='\t'), encoding='utf-8')
        print(f'-> {path}')
