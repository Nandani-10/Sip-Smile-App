#!/usr/bin/env python3
"""Generates the soft sounds into app/src/main/res/raw (already done for you)."""
import os, wave
import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app/src/main/res/raw")
os.makedirs(OUT, exist_ok=True)


def t_(dur):
    return np.arange(int(SR * dur)) / SR


def bell(freq, dur, vol=1.0, decay=5.5):
    t = t_(dur)
    # soft bell: fundamental + gentle harmonics, quick-but-smooth attack
    s = (np.sin(2 * np.pi * freq * t) + 0.35 * np.sin(2 * np.pi * freq * 2.0 * t) * np.exp(-3 * t)
         + 0.12 * np.sin(2 * np.pi * freq * 3.01 * t) * np.exp(-6 * t))
    env = np.exp(-decay * t) * np.minimum(1, t / 0.012)
    return vol * s * env


def place(buf, sig, start):
    i = int(start * SR)
    buf[i:i + len(sig)] += sig[: max(0, len(buf) - i)]


def reverb(x, delays=((0.083, .30), (0.131, .22), (0.197, .16), (0.271, .10))):
    out = x.copy()
    for d, g in delays:
        k = int(d * SR)
        out[k:] += g * x[:-k]
    return out


def finish(x, peak=0.30, fade=0.03):
    x = x - np.mean(x)
    x = x / (np.max(np.abs(x)) + 1e-9) * peak
    n = int(fade * SR)
    x[:n] *= np.linspace(0, 1, n)
    x[-n:] *= np.linspace(1, 0, n)
    return x


def write(name, x):
    pcm = (np.clip(x, -1, 1) * 32767).astype("<i2")
    with wave.open(os.path.join(OUT, name), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(pcm.tobytes())


# 1) soft chime: three rising notes (G5, B5, D6) with a dreamy tail
buf = np.zeros(int(SR * 2.2))
for k, fr in enumerate((784.0, 987.8, 1174.7)):
    place(buf, bell(fr, 1.6, 1.0 - 0.1 * k, decay=3.6), 0.0 + k * 0.22)
write("chime_soft.wav", finish(reverb(buf)))


# 2) bird tweet: two gentle chirps + a small trill, frequency-modulated
def chirp(f0, f1, dur, wob=0.0, wobf=0.0):
    t = t_(dur)
    f = f0 + (f1 - f0) * (t / dur) ** 0.8 + wob * np.sin(2 * np.pi * wobf * t)
    ph = 2 * np.pi * np.cumsum(f) / SR
    env = np.sin(np.pi * t / dur) ** 1.5
    return env * (np.sin(ph) + 0.25 * np.sin(2 * ph))


buf = np.zeros(int(SR * 1.5))
place(buf, chirp(2300, 3500, 0.14), 0.00)
place(buf, chirp(2700, 3900, 0.17, wob=120, wobf=40), 0.22)
for k in range(4):
    place(buf, chirp(3300, 2900, 0.05), 0.55 + k * 0.075)
write("bird_tweet.wav", finish(reverb(buf, ((0.05, .18), (0.11, .10))), peak=0.28))

# 3) sparkle: tiny rising twinkle
buf = np.zeros(int(SR * 1.6))
for k, fr in enumerate((1568, 2093, 2637, 3136, 3951)):
    place(buf, bell(fr, 0.8, 0.8, decay=7.0), k * 0.09)
write("sparkle.wav", finish(reverb(buf), peak=0.26))

# 4) plop: water drop for "I drank"
t = t_(0.5)
f = 900 * np.exp(-9 * t) + 220
ph = 2 * np.pi * np.cumsum(f) / SR
x = np.sin(ph) * np.exp(-9 * t) * np.minimum(1, t / 0.004)
t2 = t_(0.35)
f2 = 1500 * np.exp(-14 * t2) + 400
x2 = np.sin(2 * np.pi * np.cumsum(f2) / SR) * np.exp(-14 * t2) * 0.5
buf = np.zeros(int(SR * 0.7))
place(buf, x, 0.0)
place(buf, x2, 0.11)
write("water_plop.wav", finish(reverb(buf, ((0.06, .2),)), peak=0.35))
print("sounds written to", OUT)
