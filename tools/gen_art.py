#!/usr/bin/env python3
"""
Single source of truth for all Sip & Smile character art.

Run once (already done for you, the output is committed in the project):
    python3 tools/gen_art.py

It writes:
  * app/src/main/java/com/sipsmile/app/ui/chars/ArtData.kt   (used by Compose scenes)
  * app/src/main/res/drawable/char_<id>.xml / char_<id>_happy.xml (widget images)
  * app/src/main/res/drawable/ic_notification.xml, ic_launcher_foreground.xml, ...
  * tools/preview/*.svg  (for eyeballing)
All characters live in a 200 x 200 box.
"""
import math, os, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_KT = os.path.join(ROOT, "app/src/main/java/com/sipsmile/app/ui/chars/ArtData.kt")
OUT_RES = os.path.join(ROOT, "app/src/main/res/drawable")
OUT_PREVIEW = os.path.join(ROOT, "tools/preview")

DARK = "#2B2A4C"
PINK = "#FF9EB5"
WHITE = "#FFFFFF"

# mood: 0 always, 1 normal only, 2 happy only, 3 extra only (e.g. yawn)
MODE = {"none": 0, "rot": 1, "scalex": 2, "blink": 3}


def f(v):
    s = ("%.2f" % v).rstrip("0").rstrip(".")
    return s if s not in ("-0", "") else "0"


def argb(hexcol, alpha=1.0):
    h = hexcol.lstrip("#")
    r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
    a = int(round(alpha * 255))
    return (a << 24) | (r << 16) | (g << 8) | b


def argb_hex(v):
    return "#%08X" % v


def ellipse_d(cx, cy, rx, ry, rot=0.0):
    c, s = math.cos(math.radians(rot)), math.sin(math.radians(rot))
    x0, y0 = cx - rx * c, cy - rx * s
    x1, y1 = cx + rx * c, cy + rx * s
    return "M%s %sA%s %s %s 1 0 %s %sA%s %s %s 1 0 %s %sZ" % (
        f(x0), f(y0), f(rx), f(ry), f(rot), f(x1), f(y1), f(rx), f(ry), f(rot), f(x0), f(y0))


def poly_d(pts, close=True):
    d = "M" + "L".join("%s %s" % (f(x), f(y)) for x, y in pts)
    return d + ("Z" if close else "")


def arc_pts(cx, cy, rx, ry, a0, a1, n=28):
    out = []
    for i in range(n + 1):
        a = math.radians(a0 + (a1 - a0) * i / n)
        out.append((cx + rx * math.cos(a), cy + ry * math.sin(a)))
    return out


def rotate_pts(pts, cx, cy, deg):
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    return [(cx + (x - cx) * c - (y - cy) * s, cy + (x - cx) * s + (y - cy) * c) for x, y in pts]


def mirror_d(d):
    """Mirror simple absolute-coordinate path data around x=100 (only M L C Q Z with absolute pairs)."""
    import re
    tokens = re.findall(r"[MLCQZAmlcqza]|-?\d*\.?\d+", d)
    out, i = [], 0
    cmd = None
    while i < len(tokens):
        t = tokens[i]
        if t.isalpha():
            cmd = t
            out.append(t)
            i += 1
            if cmd == "Z":
                continue
            continue
        # number pair
        x, y = float(tokens[i]), float(tokens[i + 1])
        out.append("%s %s" % (f(200 - x), f(y)))
        i += 2
    # re-join: letters without spaces, pairs separated by space
    s = ""
    for tok in out:
        if tok.isalpha():
            s += tok
        else:
            s += (" " if s and not s[-1].isalpha() else "") + tok
    return s


class Art:
    def __init__(self, cid, name):
        self.id = cid
        self.name = name
        self.layers = []   # dicts
        self.groups = {}   # name -> (px, py, mode, amp)
        self._xf = None

    def group(self, name, px, py, mode, amp):
        self.groups[name] = (px, py, MODE[mode], amp)

    # face-frame transform: points rotated about a pivot
    def set_xf(self, cx=0, cy=0, deg=0):
        self._xf = (cx, cy, deg) if deg else None

    def T(self, x, y):
        if not self._xf:
            return x, y
        cx, cy, deg = self._xf
        return rotate_pts([(x, y)], cx, cy, deg)[0]

    def add(self, d, fill=None, stroke=None, sw=0.0, group="body", mood=0, alpha=1.0, salpha=1.0):
        self.layers.append(dict(
            d=d, fill=argb(fill, alpha) if fill else 0,
            stroke=argb(stroke, salpha) if stroke else 0, sw=sw, group=group, mood=mood))

    def circle(self, cx, cy, r, **kw):
        cx, cy = self.T(cx, cy)
        self.add(ellipse_d(cx, cy, r, r), **kw)

    def ellipse(self, cx, cy, rx, ry, rot=0.0, **kw):
        cx, cy = self.T(cx, cy)
        extra = self._xf[2] if self._xf else 0
        self.add(ellipse_d(cx, cy, rx, ry, rot + extra), **kw)

    def poly(self, pts, **kw):
        self.add(poly_d([self.T(x, y) for x, y in pts]), **kw)

    def quad(self, x0, y0, cx, cy, x1, y1, **kw):
        p0, pc, p1 = self.T(x0, y0), self.T(cx, cy), self.T(x1, y1)
        self.add("M%s %sQ%s %s %s %s" % (f(p0[0]), f(p0[1]), f(pc[0]), f(pc[1]), f(p1[0]), f(p1[1])), **kw)

    # ---- shared kawaii face ----
    def face(self, lx, rx, ey, er=8.0, mouth_y=None, mouth_w=7.0, blush=True, blush_dx=None,
             mouth=True, blink_group="eyes", yawn=None):
        for ex in (lx, rx):
            self.circle(ex, ey, er, fill=DARK, group=blink_group, mood=1)
            self.circle(ex - er * 0.28, ey - er * 0.34, er * 0.40, fill=WHITE, group=blink_group, mood=1)
            self.circle(ex + er * 0.34, ey + er * 0.32, er * 0.18, fill=WHITE, group=blink_group, mood=1)
            # happy eyes ^ ^
            x0, y0 = ex - er * 0.95, ey + er * 0.45
            x1, y1 = ex + er * 0.95, ey + er * 0.45
            self.quad(x0, y0, ex, ey - er * 1.55, x1, y1, stroke=DARK, sw=er * 0.52, mood=2, group="eyesH")
        if blush:
            bd = blush_dx if blush_dx is not None else er * 1.9
            by = ey + er * 1.55
            self.ellipse(lx - bd * 0.55, by, er * 0.95, er * 0.62, fill=PINK, alpha=0.75, group="face")
            self.ellipse(rx + bd * 0.55, by, er * 0.95, er * 0.62, fill=PINK, alpha=0.75, group="face")
        if mouth:
            mx = (lx + rx) / 2
            my = mouth_y if mouth_y is not None else ey + er * 1.9
            self.quad(mx - mouth_w, my, mx, my + mouth_w * 0.95, mx + mouth_w, my,
                      stroke=DARK, sw=2.8, mood=1, group="face")
            # happy open smile
            w = mouth_w * 1.35
            self.add("M%s %sQ%s %s %s %sZ" % (
                f(self.T(mx - w, my)[0]), f(self.T(mx - w, my)[1]),
                f(self.T(mx, my + w * 1.9)[0]), f(self.T(mx, my + w * 1.9)[1]),
                f(self.T(mx + w, my)[0]), f(self.T(mx + w, my)[1])), fill="#7A2E4D", mood=2, group="face")
            self.ellipse(mx, my + w * 0.95, w * 0.5, w * 0.28, fill="#FF8FA8", mood=2, group="face")
            if yawn:
                yx, yy = mx, my + 6
                self.ellipse(yx, yy + 2, 9, 12, fill="#7A2E4D", mood=3, group="face")
                self.ellipse(yx, yy + 8, 5.5, 4, fill="#FF8FA8", mood=3, group="face")


def band(cx, cy, rx, ry, y1, y2):
    """horizontal band of an ellipse between y1 and y2 as path data (arcs along the ellipse)."""
    def xoff(y):
        t = 1 - ((y - cy) / ry) ** 2
        return rx * math.sqrt(max(t, 0))
    a, b = xoff(y1), xoff(y2)
    return ("M%s %sL%s %sA%s %s 0 0 1 %s %sL%s %sA%s %s 0 0 1 %s %sZ" % (
        f(cx - a), f(y1), f(cx + a), f(y1), f(rx), f(ry), f(cx + b), f(y2),
        f(cx - b), f(y2), f(rx), f(ry), f(cx - a), f(y1)))


# ---------------------------------------------------------------- characters
def make_bird():
    a = Art("bird", "Pip the Bluebird")
    a.group("wingL", 68, 106, "rot", 34)
    a.group("wingR", 132, 106, "rot", -34)
    a.group("eyes", 100, 98, "blink", 0.92)
    a.group("tuft", 100, 54, "rot", 8)
    # soft ground shadow
    a.ellipse(100, 186, 46, 7, fill="#2B2A4C", alpha=0.10)
    # wings (behind body)
    wl = "M70 100C44 92 22 108 26 138C44 142 64 130 74 114Z"
    a.add(wl, fill="#5FB3D3", group="wingL")
    a.add("M44 112C40 122 40 130 44 136", stroke="#8AD0EA", sw=3, group="wingL")
    wr = mirror_d(wl)
    a.add(wr, fill="#5FB3D3", group="wingR")
    a.add(mirror_d("M44 112C40 122 40 130 44 136"), stroke="#8AD0EA", sw=3, group="wingR")
    # feet
    a.ellipse(84, 174, 11, 5.5, fill="#FFB84D")
    a.ellipse(116, 174, 11, 5.5, fill="#FFB84D")
    # body + belly
    a.ellipse(100, 110, 64, 60, fill="#7EC8E3")
    a.ellipse(100, 130, 42, 33, fill=WHITE)
    # head tuft
    a.add("M92 54C86 40 92 30 102 28C100 38 104 46 100 54Z", fill="#5FB3D3", group="tuft")
    a.add("M104 54C106 44 114 38 122 40C118 46 116 52 110 56Z", fill="#5FB3D3", group="tuft")
    # scarf
    a.add("M54 138C76 160 124 160 146 138L150 152C124 176 76 176 50 152Z", fill="#FF7A8A")
    a.add("M118 160L142 168L136 190L114 182Z", fill="#FF7A8A")
    a.add("M120 168L137 174", stroke="#FF9FAB", sw=3, group="body")
    # beak
    a.add("M91 106L109 106L100 122Z", fill="#FFB84D", stroke="#FFB84D", sw=5)
    a.face(76, 124, 98, er=8.5, blush_dx=14, mouth=False)
    return a


def make_butterfly():
    a = Art("butterfly", "Flutter the Butterfly")
    a.group("wingL", 100, 104, "scalex", 0.62)
    a.group("wingR", 100, 104, "scalex", 0.62)
    a.group("eyes", 100, 86, "blink", 0.92)
    a.group("antenna", 100, 66, "rot", 6)
    up = "M97 98C64 12 6 36 20 90C26 112 68 114 97 102Z"
    lo = "M97 104C60 106 24 122 36 158C50 186 92 152 97 112Z"
    for g, mir in (("wingL", False), ("wingR", True)):
        m = (lambda d: mirror_d(d)) if mir else (lambda d: d)
        a.add(m(up), fill="#FFB6D9", group=g)
        a.add(m(lo), fill="#C8A2FF", group=g)
        a.add(m("M96 100C70 60 38 52 28 80"), stroke="#FFD3EA", sw=4, group=g)
        cx = lambda x: 200 - x if mir else x
        a.circle(cx(44), 64, 9, fill=WHITE, alpha=0.7, group=g)
        a.circle(cx(30), 92, 5, fill=WHITE, alpha=0.7, group=g)
        a.circle(cx(56), 140, 7, fill=WHITE, alpha=0.7, group=g)
    # antennae
    a.add("M92 66C88 50 82 40 74 34", stroke="#5B4B8A", sw=3.2, group="antenna")
    a.add("M108 66C112 50 118 40 126 34", stroke="#5B4B8A", sw=3.2, group="antenna")
    a.circle(73, 33, 4.5, fill="#FF8FB8", group="antenna")
    a.circle(127, 33, 4.5, fill="#FF8FB8", group="antenna")
    # body & head
    a.ellipse(100, 132, 14, 40, fill="#5B4B8A")
    a.add("M92 120L108 120M92 132L108 132M93 144L107 144", stroke="#7D6BB4", sw=3)
    a.circle(100, 86, 26, fill="#FFF1E4")
    # flower crown
    for fx, fy in ((82, 66), (100, 60), (118, 66)):
        for k in range(5):
            ang = math.radians(k * 72 - 90)
            a.circle(fx + 5.5 * math.cos(ang), fy + 5.5 * math.sin(ang), 4, fill="#FFD84D")
        a.circle(fx, fy, 3.4, fill="#FF8F6B")
    a.face(90, 110, 88, er=5.8, blush_dx=10, mouth_y=100, mouth_w=5.0)
    return a


def make_cloud():
    a = Art("cloud", "Nimbus the Cozy Cloud")
    a.group("eyes", 100, 120, "blink", 0.92)
    a.group("brolly", 100, 66, "rot", 7)
    shapes = [("e", 100, 134, 74, 36), ("c", 58, 116, 32), ("c", 98, 96, 42), ("c", 142, 114, 34)]

    def put(dy, fill):
        for s in shapes:
            if s[0] == "e":
                a.ellipse(s[1], s[2] + dy, s[3], s[4], fill=fill)
            else:
                a.circle(s[1], s[2] + dy, s[3], fill=fill)
    a.ellipse(100, 186, 56, 6, fill="#2B2A4C", alpha=0.08)
    put(8, "#CFE2FA")
    put(0, WHITE)
    a.add("M46 150C70 160 130 160 154 150", stroke="#E6F1FF", sw=6)
    # umbrella hat (tilted)
    pts = arc_pts(100, 66, 34, 30, 180, 360, 24)
    pts = rotate_pts(pts, 100, 66, -12)
    a.add(poly_d(pts), fill="#FF9F7A", group="brolly")
    a.add(poly_d(rotate_pts([(100, 36), (84, 40), (80, 66), (100, 66)], 100, 66, -12)), fill="#FFC2A6", group="brolly")
    a.add(poly_d(rotate_pts([(100, 36), (116, 40), (120, 66), (100, 66)], 100, 66, -12)), fill="#FFC2A6", group="brolly")
    hx, hy = rotate_pts([(100, 36)], 100, 66, -12)[0]
    a.add("M%s %sL%s %s" % (f(hx), f(hy), f(hx - 2), f(hy - 9)), stroke="#8B5E3C", sw=3.2, group="brolly")
    a.circle(hx - 2, hy - 11, 3.6, fill="#FFE066", group="brolly")
    a.face(80, 120, 118, er=8.5, blush_dx=14, mouth_y=134, mouth_w=7.5)
    return a


def make_plane():
    a = Art("paper_plane", "Zoom the Paper Plane")
    a.ellipse(100, 190, 52, 6, fill="#2B2A4C", alpha=0.08)
    N, T1, F, T2 = (180, 100), (22, 36), (78, 114), (62, 172)
    # shaded underside
    a.poly([N, F, T2], fill="#BFE3FF", stroke="#BFE3FF", sw=10)
    a.poly([F, T2, (104, 152)], fill="#9CCBF0", stroke="#9CCBF0", sw=6)
    # main face wing
    a.poly([T1, N, F], fill=WHITE, stroke=WHITE, sw=10)
    a.add("M%s %sL%s %sL%s %s" % (f(T1[0]), f(T1[1]), f(N[0]), f(N[1]), f(F[0]), f(F[1])), stroke="#E3F1FF", sw=3)
    # face (tilted a little to follow the wing)
    a.set_xf(86, 84, 12)
    a.circle(66, 84, 14, fill="#D8EEFF", stroke="#7C83FD", sw=4, alpha=0.55)
    a.circle(104, 84, 14, fill="#D8EEFF", stroke="#7C83FD", sw=4, alpha=0.55)
    a.add("M%s %sL%s %s" % (f(a.T(80, 84)[0]), f(a.T(80, 84)[1]), f(a.T(90, 84)[0]), f(a.T(90, 84)[1])),
          stroke="#7C83FD", sw=4)
    a.face(66, 104, 84, er=7.5, blush_dx=11, mouth_y=102, mouth_w=6.5)
    a.set_xf()
    # goggle strap towards the tail
    a.add("M32 62L52 76", stroke="#7C83FD", sw=5)
    return a


def make_bee():
    a = Art("bee", "Barnaby the Honey Bee")
    a.group("wingL", 86, 76, "rot", 24)
    a.group("wingR", 114, 76, "rot", -24)
    a.group("eyes", 100, 112, "blink", 0.92)
    a.group("antenna", 100, 72, "rot", 6)
    a.ellipse(100, 188, 46, 6, fill="#2B2A4C", alpha=0.08)
    a.ellipse(66, 54, 30, 17, rot=-32, fill="#E8F7FF", alpha=0.82, stroke="#BFE3FF", sw=3, group="wingL")
    a.ellipse(134, 54, 30, 17, rot=32, fill="#E8F7FF", alpha=0.82, stroke="#BFE3FF", sw=3, group="wingR")
    a.add("M90 70C84 50 74 42 64 38", stroke="#5D4037", sw=3.2, group="antenna")
    a.add("M110 70C116 50 126 42 136 38", stroke="#5D4037", sw=3.2, group="antenna")
    a.circle(63, 37, 5, fill="#5D4037", group="antenna")
    a.circle(137, 37, 5, fill="#5D4037", group="antenna")
    a.ellipse(100, 118, 62, 54, fill="#FFD54F")
    a.ellipse(100, 112, 50, 38, fill="#FFE585", alpha=0.35)
    a.add(band(100, 118, 62, 54, 142, 154), fill="#5D4037")
    a.add(band(100, 118, 62, 54, 161, 169), fill="#5D4037")
    # sunglasses resting on top of head
    a.add("M66 84C66 76 74 74 82 74L94 74C98 74 98 82 94 88C90 94 74 94 70 90Z", fill="#2B2A4C")
    a.add("M134 84C134 76 126 74 118 74L106 74C102 74 102 82 106 88C110 94 126 94 130 90Z", fill="#2B2A4C")
    a.add("M92 80L108 80", stroke="#2B2A4C", sw=4)
    a.ellipse(78, 79, 6, 2.6, rot=-20, fill=WHITE, alpha=0.5)
    a.ellipse(122, 79, 6, 2.6, rot=20, fill=WHITE, alpha=0.5)
    a.face(78, 122, 112, er=8.5, blush_dx=14, mouth_y=128, mouth_w=7.5)
    return a


def make_moon():
    a = Art("moon", "Lumi the Crescent Moon")
    a.group("eyes", 100, 112, "blink", 0.92)
    a.group("pom", 150, 44, "rot", 16)
    a.circle(100, 106, 94, fill="#FFF9D6", alpha=0.45)
    a.circle(100, 106, 80, fill="#FFF9D6", alpha=0.55)
    a.circle(100, 108, 66, fill="#FFF3B0")
    a.circle(150, 82, 10, fill="#F2E08A")
    a.circle(58, 146, 12, fill="#F2E08A")
    a.circle(146, 148, 7, fill="#F2E08A")
    a.circle(52, 96, 5, fill="#F2E08A")
    # nightcap
    a.add("M50 72C58 28 112 14 156 40C170 50 172 66 164 74C142 56 100 54 50 72Z", fill="#8E9AF7")
    a.add("M50 72C100 54 142 56 164 74L160 84C138 68 100 68 52 84Z", fill=WHITE)
    a.add("M92 34C110 30 130 34 146 42", stroke="#AAB4FF", sw=4)
    # star pom
    star = []
    for k in range(10):
        r = 11 if k % 2 == 0 else 4.8
        ang = math.radians(-90 + k * 36)
        star.append((160 + r * math.cos(ang), 46 + r * math.sin(ang)))
    a.add(poly_d(star), fill="#FFE066", stroke="#FFE066", sw=2, group="pom")
    a.face(78, 122, 114, er=8.5, blush_dx=14, mouth_y=132, mouth_w=7, yawn=True)
    return a


def make_drop():
    a = Art("water_drop", "Dewy the Water-Drop Buddy")
    a.group("eyes", 100, 128, "blink", 0.92)
    a.group("hat", 100, 58, "rot", 6)
    a.ellipse(100, 190, 44, 6, fill="#2B2A4C", alpha=0.10)
    d = "M100 30C100 30 40 96 40 132C40 166 66 190 100 190C134 190 160 166 160 132C160 96 100 30 100 30Z"
    a.add(d, fill="#5EBBFA")
    a.add("M46 152C60 186 140 186 154 152C140 174 60 174 46 152Z", fill="#3FA0EE")
    a.ellipse(70, 124, 11, 23, rot=18, fill=WHITE, alpha=0.5)
    a.circle(62, 150, 4, fill=WHITE, alpha=0.5)
    # sailor hat
    a.add("M72 60C72 30 128 30 128 60Z", fill=WHITE, group="hat")
    a.add("M73 51C90 56 110 56 127 51L128 58C110 63 90 63 72 58Z", fill="#3FA0EE", group="hat")
    a.ellipse(100, 61, 33, 9, fill=WHITE, stroke="#DDEBFF", sw=2, group="hat")
    a.circle(100, 34, 4.6, fill="#FF7A8A", group="hat")
    a.face(80, 120, 130, er=9, blush_dx=15, mouth_y=148, mouth_w=7.5)
    return a


ALL = [make_drop(), make_bird(), make_butterfly(), make_cloud(), make_plane(), make_bee(), make_moon()]


# ---------------------------------------------------------------- writers
def layers_for(art, mood):
    # mood: 'normal' or 'happy'
    keep = (0, 1) if mood == "normal" else (0, 2)
    return [l for l in art.layers if l["mood"] in keep]


def vector_xml(art, mood, size=200, viewport=200, tx=0, ty=0, scale=1.0):
    lines = ['<?xml version="1.0" encoding="utf-8"?>',
             '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
             '    android:width="%sdp"' % size, '    android:height="%sdp"' % size,
             '    android:viewportWidth="%s"' % viewport, '    android:viewportHeight="%s">' % viewport]
    body = []
    for l in layers_for(art, mood):
        attrs = ['android:pathData="%s"' % l["d"]]
        if l["fill"]:
            attrs.append('android:fillColor="%s"' % argb_hex(l["fill"]))
        if l["stroke"]:
            attrs += ['android:strokeColor="%s"' % argb_hex(l["stroke"]),
                      'android:strokeWidth="%s"' % f(l["sw"]),
                      'android:strokeLineCap="round"', 'android:strokeLineJoin="round"']
        body.append("<path " + " ".join(attrs) + "/>")
    if scale != 1.0 or tx or ty:
        lines.append('    <group android:translateX="%s" android:translateY="%s" android:scaleX="%s" android:scaleY="%s">' % (
            f(tx), f(ty), f(scale), f(scale)))
        lines += ["        " + b for b in body]
        lines.append("    </group>")
    else:
        lines += ["    " + b for b in body]
    lines.append("</vector>")
    return "\n".join(lines) + "\n"


def svg_text(art, mood):
    parts = ['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" width="200" height="200">']
    for l in layers_for(art, mood):
        fill = ('fill="#%06X" fill-opacity="%s"' % (l["fill"] & 0xFFFFFF, f(((l["fill"] >> 24) & 255) / 255))) if l["fill"] else 'fill="none"'
        stroke = ('stroke="#%06X" stroke-opacity="%s" stroke-width="%s" stroke-linecap="round" stroke-linejoin="round"' % (
            l["stroke"] & 0xFFFFFF, f(((l["stroke"] >> 24) & 255) / 255), f(l["sw"]))) if l["stroke"] else ""
        parts.append('<path d="%s" %s %s/>' % (l["d"], fill, stroke))
    parts.append("</svg>")
    return "\n".join(parts)


def kt_str(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def write_kotlin():
    out = ["// GENERATED by tools/gen_art.py - do not edit by hand.",
           "package com.sipsmile.app.ui.chars", "",
           "object ArtData {",
           "    val all: List<CharArt> = listOf("]
    for art in ALL:
        out.append("        CharArt(")
        out.append("            id = %s," % kt_str(art.id))
        out.append("            name = %s," % kt_str(art.name))
        out.append("            groups = mapOf(")
        for gname, (px, py, mode, amp) in art.groups.items():
            out.append("                %s to ArtGroup(%sf, %sf, %d, %sf)," % (kt_str(gname), f(px), f(py), mode, f(amp)))
        out.append("            ),")
        out.append("            layers = listOf(")
        for l in art.layers:
            out.append("                ArtLayer(%s, %d, 0x%08XL, 0x%08XL, %sf, %s)," % (
                kt_str(l["group"]), l["mood"], l["fill"], l["stroke"], f(l["sw"]), kt_str(l["d"])))
        out.append("            ),")
        out.append("        ),")
    out.append("    )")
    out.append("}")
    os.makedirs(os.path.dirname(OUT_KT), exist_ok=True)
    open(OUT_KT, "w").write("\n".join(out) + "\n")


def write_resources():
    os.makedirs(OUT_RES, exist_ok=True)
    os.makedirs(OUT_PREVIEW, exist_ok=True)
    for art in ALL:
        for mood in ("normal", "happy"):
            name = "char_%s%s" % (art.id, "" if mood == "normal" else "_happy")
            open(os.path.join(OUT_RES, name + ".xml"), "w").write(vector_xml(art, mood))
            open(os.path.join(OUT_PREVIEW, name + ".svg"), "w").write(svg_text(art, mood))

    drop = [a for a in ALL if a.id == "water_drop"][0]

    # ---- launcher foreground: drop w/o hat, centred in the 66dp safe zone of a 108 canvas
    fg = ['<?xml version="1.0" encoding="utf-8"?>',
          '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
          '    android:width="108dp" android:height="108dp"',
          '    android:viewportWidth="108" android:viewportHeight="108">',
          '    <group android:translateX="26.4" android:translateY="23" android:scaleX="0.28" android:scaleY="0.28">']
    for l in drop.layers:
        if l["mood"] not in (0, 1) or l["group"] == "hat":
            continue
        attrs = ['android:pathData="%s"' % l["d"]]
        if l["fill"]:
            attrs.append('android:fillColor="%s"' % argb_hex(l["fill"]))
        if l["stroke"]:
            attrs += ['android:strokeColor="%s"' % argb_hex(l["stroke"]), 'android:strokeWidth="%s"' % f(l["sw"]),
                      'android:strokeLineCap="round"', 'android:strokeLineJoin="round"']
        fg.append("        <path " + " ".join(attrs) + "/>")
    fg += ["    </group>", "</vector>"]
    open(os.path.join(OUT_RES, "ic_launcher_foreground.xml"), "w").write("\n".join(fg) + "\n")

    # ---- monochrome (themed icon) + notification icon: drop silhouette with eye cut-outs (evenOdd)
    def silhouette(size, vp, scale, tx, ty, name):
        body = "M100 30C100 30 40 96 40 132C40 166 66 190 100 190C134 190 160 166 160 132C160 96 100 30 100 30Z"
        eyes = ellipse_d(80, 130, 9, 9) + ellipse_d(120, 130, 9, 9)
        smile = "M86 146Q100 162 114 146L110 144Q100 154 90 144Z"
        xml = ['<?xml version="1.0" encoding="utf-8"?>',
               '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
               '    android:width="%sdp" android:height="%sdp"' % (size, size),
               '    android:viewportWidth="%s" android:viewportHeight="%s">' % (vp, vp),
               '    <group android:translateX="%s" android:translateY="%s" android:scaleX="%s" android:scaleY="%s">' % (tx, ty, scale, scale),
               '        <path android:fillColor="#FFFFFFFF" android:fillType="evenOdd" android:pathData="%s"/>' % (body + eyes + smile),
               '    </group>', '</vector>']
        open(os.path.join(OUT_RES, name), "w").write("\n".join(xml) + "\n")
    silhouette(24, 24, 0.1, 2.0, 1.5, "ic_notification.xml")
    silhouette(108, 108, 0.28, 26.4, 23, "ic_launcher_monochrome.xml")


if __name__ == "__main__":
    write_kotlin()
    write_resources()
    print("ok", len(ALL), "characters")
