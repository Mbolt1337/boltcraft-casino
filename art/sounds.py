"""Synthesises every sound of the mod into src/main/resources/assets/boltcraft_casino/sounds/.

    python art/sounds.py

All sounds are original: built from struck-object models (sums of decaying partials), filtered noise and
envelopes, with a fixed random seed so a rebuild gives the same files. OGG Vorbis is encoded with the FFmpeg
binary that the imageio-ffmpeg package carries.

Loops (reel_spin, roulette_spin) are built from events whose timing repeats exactly within the loop's length,
so they join without a click.
"""
from pathlib import Path
import subprocess

import imageio_ffmpeg
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/assets/boltcraft_casino/sounds/block'
RATE = 44100
rng = np.random.default_rng(1337)


# ---------------------------------------------------------------------------------------------------------------
# Building blocks


def silence(seconds):
    return np.zeros(int(seconds * RATE))


def times(seconds):
    return np.arange(int(seconds * RATE)) / RATE


def struck(partials, seconds, attack=0.002):
    """A struck object: partials are (frequency, amplitude, decay time in seconds)."""
    t = times(seconds)
    out = np.zeros_like(t)
    for frequency, amplitude, decay in partials:
        phase = rng.uniform(0, 2 * np.pi)
        out += amplitude * np.sin(2 * np.pi * frequency * t + phase) * np.exp(-t / decay)
    ramp = np.clip(t / attack, 0, 1)
    return out * ramp


def noise(seconds):
    return rng.uniform(-1, 1, int(seconds * RATE))


def one_pole_lowpass(signal, cutoff):
    a = np.exp(-2 * np.pi * cutoff / RATE)
    out = np.empty_like(signal)
    state = 0.0
    for i, x in enumerate(signal):
        state = (1 - a) * x + a * state
        out[i] = state
    return out


def bandpass(signal, centre, q):
    """RBJ band-pass biquad, constant skirt gain."""
    w = 2 * np.pi * centre / RATE
    alpha = np.sin(w) / (2 * q)
    b0, b1, b2 = alpha, 0.0, -alpha
    a0, a1, a2 = 1 + alpha, -2 * np.cos(w), 1 - alpha
    b0, b1, b2, a1, a2 = b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0
    out = np.empty_like(signal)
    x1 = x2 = y1 = y2 = 0.0
    for i, x in enumerate(signal):
        y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2, x1, y2, y1 = x1, x, y1, y
        out[i] = y
    return out


def envelope(seconds, attack, decay):
    t = times(seconds)
    return np.clip(t / attack, 0, 1) * np.exp(-np.maximum(t - attack, 0) / decay)


def place(track, sound, at):
    start = int(at * RATE)
    end = min(len(track), start + len(sound))
    if start < len(track):
        track[start:end] += sound[:end - start]


def place_looped(track, sound, at):
    """Places a sound into a loop, wrapping its tail round to the start."""
    start = int(at * RATE) % len(track)
    for offset in range(0, len(sound), len(track)):
        piece = sound[offset:offset + len(track)]
        first = min(len(piece), len(track) - start)
        track[start:start + first] += piece[:first]
        track[:len(piece) - first] += piece[first:]


def finish(track, peak=0.89, fade=0.01):
    track = track - np.mean(track)
    fade_samples = int(fade * RATE)
    if fade_samples and len(track) > 2 * fade_samples:
        track[-fade_samples:] *= np.linspace(1, 0, fade_samples)
    return track / np.max(np.abs(track)) * peak


def finish_loop(track, peak=0.8):
    track = track - np.mean(track)
    return track / np.max(np.abs(track)) * peak


def save(name, track):
    OUT.mkdir(parents=True, exist_ok=True)
    target = OUT / f'{name}.ogg'
    subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(), '-y', '-loglevel', 'error', '-f', 'f32le', '-ar', str(RATE),
                    '-ac', '1', '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', str(target)],
                   input=track.astype(np.float32).tobytes(), check=True)
    print(f'-> {target.relative_to(ROOT)} ({len(track) / RATE:.2f} s)')


# ---------------------------------------------------------------------------------------------------------------
# Materials: each returns one hit


def clay_chip(pitch=1.0):
    """A clay casino chip knocking another: hard, bright and very short."""
    hit = struck([(2350 * pitch, 1.0, 0.018), (3680 * pitch, 0.7, 0.012), (5210 * pitch, 0.45, 0.008),
                  (7400 * pitch, 0.25, 0.005)], 0.08)
    click = bandpass(noise(0.01), 4000 * pitch, 1.2) * envelope(0.01, 0.0005, 0.002)
    out = hit
    out[:len(click)] += click * 1.5
    return out


def metal_tick(pitch=1.0, decay=0.02):
    """A small steel pawl clicking over a tooth."""
    return struck([(3100 * pitch, 1.0, decay), (4870 * pitch, 0.6, decay * 0.7), (6900 * pitch, 0.3, decay * 0.5)],
                  decay * 5)


def thud(frequency, seconds=0.25, decay=0.06):
    """A heavy body being stopped: a low sine with a fast pitch drop and some knock."""
    t = times(seconds)
    sweep = frequency * (1 + 0.6 * np.exp(-t / 0.015))
    body = np.sin(2 * np.pi * np.cumsum(sweep) / RATE) * np.exp(-t / decay)
    knock = one_pole_lowpass(noise(seconds), 1800) * np.exp(-t / 0.012) * 2.5
    return body + knock


def bell(frequency, seconds=1.2, decay=0.5, brightness=1.0):
    """A small hand bell: the minor-third and fifth-ish partials of a bell, with a strike."""
    partials = [(frequency * 0.5, 0.35, decay * 1.4), (frequency, 1.0, decay), (frequency * 1.19, 0.45, decay * 0.8),
                (frequency * 1.5, 0.4, decay * 0.7), (frequency * 2.0, 0.5 * brightness, decay * 0.5),
                (frequency * 2.51, 0.25 * brightness, decay * 0.35), (frequency * 3.0, 0.15 * brightness, decay * 0.25)]
    return struck(partials, seconds, attack=0.001)


def wood_knock(frequency=1150, decay=0.03):
    """The ball against wooden frets and the bowl."""
    return struck([(frequency, 1.0, decay), (frequency * 2.29, 0.5, decay * 0.6), (frequency * 3.71, 0.25,
                                                                                    decay * 0.4)],
                  decay * 6)


def note(name):
    names = {'C': -9, 'D': -7, 'E': -5, 'F': -4, 'G': -2, 'A': 0, 'B': 2}
    letter, octave = name[0], int(name[-1])
    shift = 1 if '#' in name else 0
    return 440 * 2 ** ((names[letter] + shift + 12 * (octave - 4)) / 12)


# ---------------------------------------------------------------------------------------------------------------
# Sounds


def chip_clink(variant):
    """A small stack of chips set down: two or three clacks a few milliseconds apart."""
    track = silence(0.3)
    at = 0.0
    for _ in range(2 + variant % 2):
        place(track, clay_chip(rng.uniform(0.9, 1.12)) * rng.uniform(0.6, 1.0), at)
        at += rng.uniform(0.025, 0.06)
    return finish(track)


def lever_pull():
    """The handle is pulled down over a ratchet, hits the stop and the spring throws it back."""
    track = silence(0.75)
    at = 0.0
    for click in range(9):
        place(track, metal_tick(1.0 + click * 0.015) * 0.5, at)
        at += 0.032 - click * 0.0015
    place(track, thud(95, 0.3, 0.08) * 0.9, at + 0.01)
    t = times(0.4)
    spring = np.sin(2 * np.pi * (180 + 40 * np.sin(2 * np.pi * 9 * t)) * t) * np.exp(-t / 0.12) * 0.25
    place(track, spring, at + 0.05)
    return finish(track)


def reel_spin():
    """Loop: three reels turning, each detent clicking at its own steady rate, over a soft motor hum."""
    length = 2.0
    track = silence(length)
    for reel, rate in enumerate((18, 21, 24)):
        step = length / (rate * length)
        for index in range(int(rate * length)):
            place_looped(track, metal_tick(0.8 + reel * 0.07, 0.012) * 0.22, index * step + reel * 0.011)
    t = times(length)
    hum = (np.sin(2 * np.pi * 90 * t) + 0.4 * np.sin(2 * np.pi * 180 * t)) * 0.08
    hiss = bandpass(noise(length), 2200, 0.7) * 0.05
    return finish_loop(track + hum + hiss)


def reel_stop():
    """A reel caught by its brake: a dull clunk with a click on top."""
    track = silence(0.25)
    place(track, thud(140, 0.25, 0.05) * 0.8, 0)
    place(track, metal_tick(0.9, 0.015) * 0.6, 0.004)
    return finish(track)


def win_small():
    """Three bells up a major chord and a few chips."""
    track = silence(1.3)
    for index, name in enumerate(('C6', 'E6', 'G6')):
        place(track, bell(note(name), 1.0, 0.35) * 0.6, index * 0.09)
    for index in range(4):
        place(track, clay_chip(rng.uniform(0.9, 1.1)) * 0.25, 0.35 + index * 0.07 + rng.uniform(0, 0.03))
    return finish(track)


def win_big():
    """A rising run of bells, a held chord and a stream of chips."""
    track = silence(1.9)
    for index, name in enumerate(('C6', 'E6', 'G6', 'C7', 'E7')):
        place(track, bell(note(name), 1.4, 0.45) * 0.55, index * 0.08)
    for name in ('C6', 'E6', 'G6'):
        place(track, bell(note(name), 1.4, 0.6, 0.6) * 0.3, 0.45)
    at = 0.3
    while at < 1.5:
        place(track, clay_chip(rng.uniform(0.85, 1.15)) * rng.uniform(0.15, 0.3), at)
        at += rng.uniform(0.02, 0.06)
    return finish(track)


def jackpot():
    """Fanfare: two rising runs, a ringing chord with tremolo and a long cascade of chips."""
    track = silence(3.0)
    run = ('C6', 'E6', 'G6', 'C7', 'E7', 'G7', 'C8')
    for index, name in enumerate(run):
        place(track, bell(note(name), 1.4, 0.4) * 0.5, index * 0.07)
    for index, name in enumerate(run):
        place(track, bell(note(name), 1.4, 0.4) * 0.4, 0.6 + index * 0.06)
    chord = np.zeros(int(2.0 * RATE))
    for name in ('C6', 'E6', 'G6', 'C7'):
        chord += bell(note(name), 2.0, 0.9, 0.7)[:len(chord)]
    t = times(2.0)
    place(track, chord * (1 + 0.35 * np.sin(2 * np.pi * 7 * t)) * 0.3, 1.05)
    at = 0.5
    while at < 2.6:
        density = 0.015 + 0.03 * (at - 0.5) / 2.1
        place(track, clay_chip(rng.uniform(0.8, 1.2)) * rng.uniform(0.12, 0.28), at)
        at += rng.uniform(density * 0.5, density * 1.5)
    return finish(track, fade=0.2)


def vault_open():
    """The dial clicks round, the bolts draw back with a clunk and the heavy door swings on its hinge."""
    track = silence(1.1)
    for index in range(5):
        place(track, metal_tick(1.2, 0.01) * 0.35, index * 0.045)
    place(track, thud(110, 0.3, 0.07) * 0.8, 0.26)
    place(track, metal_tick(0.6, 0.03) * 0.5, 0.27)
    creak_length = 0.7
    t = times(creak_length)
    pitch = 260 + 120 * t / creak_length
    rasp = np.sign(np.sin(2 * np.pi * np.cumsum(pitch) / RATE)) * (0.5 + 0.5 * rng.uniform(0, 1, len(t)))
    creak = bandpass(rasp, 900, 3) * np.sin(np.pi * t / creak_length) * 0.4
    place(track, creak, 0.36)
    return finish(track, fade=0.1)


def vault_close():
    """A short swing, the heavy door landing in its frame and the bolts shooting home."""
    track = silence(0.8)
    t = times(0.25)
    swing = bandpass(noise(0.25), 500, 1.5) * np.sin(np.pi * t / 0.25) * 0.25
    place(track, swing, 0)
    place(track, thud(75, 0.45, 0.12), 0.22)
    place(track, metal_tick(0.55, 0.035) * 0.6, 0.38)
    place(track, metal_tick(0.6, 0.03) * 0.45, 0.43)
    return finish(track, fade=0.1)


def roulette_spin():
    """Loop: the ball rolling round the wooden track, rumbling and catching the deflectors, over the wheel."""
    length = 2.4
    t = times(length)
    laps = 4
    lap = length / laps
    roll = one_pole_lowpass(noise(length), 700)
    roll *= 0.7 + 0.3 * np.sin(2 * np.pi * t / lap * 8)
    track = roll * 1.4
    for index in range(laps * 8):
        place_looped(track, wood_knock(rng.uniform(1300, 1500), 0.012) * rng.uniform(0.1, 0.22), index * lap / 8)
    wheel = bandpass(noise(length), 320, 1.0) * 0.5
    track += wheel
    return finish_loop(track, 0.7)


def ball_drop():
    """The ball falls off the track, bounces from fret to fret with shorter and softer hops and settles."""
    track = silence(1.2)
    at = 0.0
    gap = 0.16
    strength = 1.0
    for _ in range(9):
        place(track, wood_knock(rng.uniform(1000, 1600), 0.025) * strength, at)
        at += gap
        gap *= 0.72
        strength *= 0.75
    t = times(0.4)
    rattle = bandpass(noise(0.4), 1800, 2) * np.exp(-t / 0.1) * 0.15
    place(track, rattle, at)
    return finish(track)


SOUNDS = {
    'lever_pull': lever_pull,
    'reel_spin': reel_spin,
    'reel_stop': reel_stop,
    'win_small': win_small,
    'win_big': win_big,
    'jackpot': jackpot,
    'vault_open': vault_open,
    'vault_close': vault_close,
    'roulette_spin': roulette_spin,
    'ball_drop': ball_drop,
}

if __name__ == '__main__':
    for variant in range(1, 4):
        save(f'chip_clink{variant}', chip_clink(variant))
    for name, make in SOUNDS.items():
        save(name, make())
