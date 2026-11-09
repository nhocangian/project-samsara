"""
Âm thanh tổng hợp cho đèn ở Tầng Dưới (cần ffmpeg có libvorbis, numpy). Chạy từ thư mục mod/:
    python tools/generate_tang_duoi_sounds.py

  sounds/tang_duoi/den_hong_zap_0..3.ogg   chập điện đèn hỏng: vài tia nổ giòn ngắn, không có đuôi ù trầm
  sounds/tang_duoi/den_chop_tick_0..2.ogg  đèn chập chờn bắt lại: tiếng tách nhỏ, kéo theo vệt ù rất ngắn

Mono (Minecraft chỉ định vị 3D được âm mono).
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "samsara", "sounds", "tang_duoi")
RATE = 44100


def buzz(n):
    """Tiếng ù điện: sóng vuông 100 Hz làm mềm, giàu hài."""
    t = np.arange(n) / RATE
    return np.tanh(6.0 * np.sin(2 * np.pi * 100.0 * t))


def highpass(x):
    """Lọc thông cao rất thô (sai phân) để tiếng nổ giòn, không ồm."""
    return np.diff(x, prepend=x[0])


def arc(rng, ms, level):
    n = int(RATE * ms / 1000)
    noise = highpass(rng.standard_normal(n))
    noise /= max(1e-6, np.abs(noise).max())
    env = np.exp(-np.linspace(0, rng.uniform(3.0, 7.0), n))
    return level * env * (0.8 * noise + 0.25 * buzz(n))


def assemble(events, total_ms):
    out = np.zeros(int(RATE * total_ms / 1000))
    for start_ms, sig in events:
        i = int(RATE * start_ms / 1000)
        j = min(len(out), i + len(sig))
        out[i:j] += sig[: j - i]
    # Tắt dần ở mép để không kêu tạch ở cuối file
    fade = min(len(out), int(RATE * 0.01))
    out[-fade:] *= np.linspace(1, 0, fade)
    return out / max(1e-6, np.abs(out).max()) * 0.95


def zap(seed):
    rng = np.random.default_rng(seed)
    t = 0.0
    events = []
    for _ in range(rng.integers(4, 9)):
        ms = rng.uniform(6, 34)
        events.append((t, arc(rng, ms, rng.uniform(0.45, 1.0))))
        t += ms + rng.uniform(4, 70)
    return assemble(events, t + 20)


def tick(seed):
    rng = np.random.default_rng(seed)
    n = int(RATE * 0.09)
    hum = 0.2 * buzz(n) * np.linspace(1, 0, n) ** 3
    return assemble([(0, arc(rng, rng.uniform(5, 12), 0.8)), (6, hum)], 110)


def save(sig, name):
    with tempfile.TemporaryDirectory() as d:
        path = os.path.join(d, "a.wav")
        with wave.open(path, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes((sig * 32767).astype(np.int16).tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", path, "-c:a", "libvorbis", "-q:a", "5",
                        os.path.join(OUT, name + ".ogg")], check=True)


def main():
    os.makedirs(OUT, exist_ok=True)
    for i in range(4):
        save(zap(9100 + i), f"den_hong_zap_{i}")
    for i in range(3):
        save(tick(9200 + i), f"den_chop_tick_{i}")


if __name__ == "__main__":
    main()
