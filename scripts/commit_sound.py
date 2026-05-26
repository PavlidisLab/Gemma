#!/usr/bin/env python3
"""
Sonify Gemma's commit history. Each commit becomes a short click; the click's
loudness scales with how much that commit touched (lines or files). Commits
play in chronological order across the output's duration. Recent flurries of
activity stack into a machine-gun roll.

Usage:
    python scripts/commit_sound.py                              # last 20y, 20s wav
    python scripts/commit_sound.py --years 5 --duration 30      # 5y, 30s
    python scripts/commit_sound.py --metric files               # scale by files-changed
    python scripts/commit_sound.py --branch development         # different branch
    python scripts/commit_sound.py --pitch-by-size              # bigger commits sound higher

Outputs (under --out, default /tmp/commit_sound):
    commits.wav     16-bit mono 44.1 kHz
    commits.json    one entry per commit (date, lines, files, amp, pitch)
    CAPTIONS.md     append-only summary

Stdlib-only except numpy.
"""

from __future__ import annotations
import argparse
import datetime as dt
import json
import math
import struct
import subprocess
import sys
import wave
from pathlib import Path
from typing import List, Optional, Tuple

import numpy as np

SAMPLE_RATE = 44100


# --- git ---

def commits_in_window(repo: Path, branch: str, years: float) -> List[Tuple[dt.datetime, str, int, int]]:
    """Return [(committer_date, sha, lines_touched, files_touched), …] oldest→newest."""
    now = dt.datetime.now(dt.timezone.utc)
    since = (now - dt.timedelta(days=int(years * 365.25))).strftime("%Y-%m-%d")
    # git log with --shortstat outputs per-commit:
    #   <unix_ts> <sha>
    #    <N> files changed, <M> insertions(+), <K> deletions(-)
    # with a blank line between commits. Some commits omit the shortstat line
    # if they touch nothing — skip those.
    out = subprocess.check_output(
        [
            "git", "-C", str(repo), "log", branch,
            "--first-parent",
            f"--since={since}",
            "--pretty=format:%x00%ct %H",
            "--shortstat",
        ],
        text=True,
    )
    rows: List[Tuple[dt.datetime, str, int, int]] = []
    # records are separated by NUL because shortstat is multi-line; harder to
    # split on \n alone.
    chunks = out.split("\x00")
    for chunk in chunks:
        chunk = chunk.strip()
        if not chunk:
            continue
        lines = chunk.splitlines()
        head = lines[0]
        try:
            ts_str, sha = head.split(None, 1)
            ts = int(ts_str)
        except ValueError:
            continue
        when = dt.datetime.fromtimestamp(ts, tz=dt.timezone.utc)
        n_lines, n_files = 0, 0
        for ln in lines[1:]:
            ln = ln.strip()
            # e.g. "3 files changed, 47 insertions(+), 12 deletions(-)"
            if "file" in ln and "changed" in ln:
                parts = ln.split(",")
                for p in parts:
                    p = p.strip()
                    tok = p.split()[0]
                    if "file" in p:
                        n_files = int(tok)
                    elif "insertion" in p:
                        n_lines += int(tok)
                    elif "deletion" in p:
                        n_lines += int(tok)
        rows.append((when, sha, n_lines, n_files))
    rows.sort(key=lambda r: r[0])
    return rows


# --- sound generation ---

def make_click(samples: int, amp: float, pitch_hz: float, sr: int = SAMPLE_RATE) -> np.ndarray:
    """
    Attack-release tonal blip, ~40 ms total. The rise-fall shape (vs a sharp
    decay-only click) gives concurrent commits a "tide" feeling: when many
    clicks overlap, the ramps stack into a single audible swell rather than
    a single louder pop.

    Body is sine + an octave harmonic for a slightly brighter timbre that
    survives piling up. Per-click amplitude is intentionally low so dense
    regions can sum to genuinely louder peaks without each click clipping
    on its own.
    """
    t = np.arange(samples) / sr
    attack_samples = int(0.004 * sr)   # 4 ms attack — soft enough to ramp, fast enough to pop in
    env = np.empty(samples)
    env[:attack_samples] = np.linspace(0.0, 1.0, attack_samples)
    rel_t = t[attack_samples:] - t[attack_samples]
    env[attack_samples:] = np.exp(-rel_t * 45.0)        # ~22 ms 1/e — body to pile up
    body = np.sin(2 * np.pi * pitch_hz * t) + 0.30 * np.sin(2 * np.pi * 2 * pitch_hz * t)
    return amp * env * body


def render(commits: List[Tuple[dt.datetime, str, int, int]],
           duration_s: float,
           metric: str,
           pitch_by_size: bool,
           sr: int = SAMPLE_RATE) -> Tuple[np.ndarray, List[dict]]:
    """Mix all commits into a single mono buffer."""
    n_total = int(duration_s * sr)
    buf = np.zeros(n_total, dtype=np.float64)
    if not commits:
        return buf, []
    first_ts = commits[0][0].timestamp()
    last_ts  = commits[-1][0].timestamp()
    span = max(last_ts - first_ts, 1.0)

    # Loudness map: log scale, since the distribution is heavy-tailed.
    # MAX caps a "huge" commit at full loudness; quieter commits scale down.
    if metric == "files":
        sizes = [c[3] for c in commits]
    else:
        sizes = [c[2] for c in commits]
    log_sizes = [math.log10(s + 1) for s in sizes]
    max_log = max(log_sizes) if log_sizes else 1.0
    if max_log <= 0:
        max_log = 1.0

    # click length: 40 ms total so attack + 22 ms decay both fit cleanly.
    click_len = int(0.040 * sr)
    # Per-click amplitude cap. Intentionally low (was 1.0): the dense region of
    # the timeline has ~5-10 overlapping clicks, and we want THAT to be the
    # loudest moment, not a single big commit early on. With cap = 0.15, four
    # stacked clicks sum to ~0.6 (the linear regime); ten clicks sum to ~1.0
    # and just kiss the hard clip.
    per_click_cap = 0.15

    log_records: List[dict] = []
    for (when, sha, n_lines, n_files), lsz in zip(commits, log_sizes):
        rel = (when.timestamp() - first_ts) / span
        pos = int(rel * (n_total - click_len))
        # Scale by size, but stay under per_click_cap so density (not single-
        # commit magnitude) is what makes a moment loud.
        amp = per_click_cap * max(0.05, min(1.0, lsz / max_log))
        if pitch_by_size:
            # 800 Hz (small) → 2400 Hz (huge). Logarithmic in size.
            pitch = 800 + 1600 * (lsz / max_log)
        else:
            pitch = 1400.0
        click = make_click(click_len, amp, pitch, sr=sr)
        end = pos + click_len
        if end <= n_total:
            buf[pos:end] += click
        else:
            buf[pos:n_total] += click[: n_total - pos]
        log_records.append({
            "sha": sha,
            "when": when.isoformat(),
            "lines": n_lines,
            "files": n_files,
            "amp": round(amp, 3),
            "pitch_hz": round(pitch, 1),
            "t_s": round(rel * duration_s, 3),
        })

    # Linear sum is the contrast-preserving choice: we DO want the dense moment
    # to be louder than a single click. Apply only a knee-soft limiter above
    # 0.85 so the linear regime (sparse + a few overlapping clicks) keeps its
    # dynamic range. tanh-from-zero (the prior approach) compressed quiet
    # moments alongside loud ones and killed the swell entirely.
    knee = 0.85
    over = np.abs(buf) > knee
    if over.any():
        sign = np.sign(buf[over])
        x = np.abs(buf[over]) - knee
        # Limit asymptote at 1.0: keeps the integer-int16 conversion clean.
        buf[over] = sign * (knee + (1.0 - knee) * np.tanh(x / (1.0 - knee)))
    peak = float(np.max(np.abs(buf))) or 1.0
    if peak > 0.95:
        buf = buf * (0.95 / peak)
    return buf, log_records


def write_wav(path: Path, buf: np.ndarray, sr: int = SAMPLE_RATE) -> None:
    pcm = np.clip(buf * 32767, -32768, 32767).astype("<i2")
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes(pcm.tobytes())


# --- main ---

def main(argv: List[str]) -> int:
    p = argparse.ArgumentParser(description="Sonify Gemma commit history into a short WAV.")
    p.add_argument("--repo",     default=str(Path(__file__).resolve().parents[1]))
    p.add_argument("--branch",   default="phase2-acl-migrate")
    p.add_argument("--years",    type=float, default=20.0,
                   help="Years of history to cover (default 20).")
    p.add_argument("--duration", type=float, default=20.0,
                   help="Output WAV duration in seconds (default 20).")
    p.add_argument("--metric",   choices=("lines", "files"), default="lines",
                   help="What scales loudness: total insertions+deletions (lines, default) "
                        "or files-changed count.")
    p.add_argument("--pitch-by-size", action="store_true",
                   help="Bigger commits get a higher pitch (default: flat 1.4 kHz).")
    p.add_argument("--out",      default="/tmp/commit_sound")
    a = p.parse_args(argv)

    repo = Path(a.repo).resolve()
    out_dir = Path(a.out).resolve()
    out_dir.mkdir(parents=True, exist_ok=True)

    print(f"[commit_sound] reading commits on {a.branch} (last {a.years:g}y)…", file=sys.stderr)
    rows = commits_in_window(repo, a.branch, a.years)
    print(f"[commit_sound] {len(rows)} commits", file=sys.stderr)
    if not rows:
        print("[commit_sound] no commits in window; nothing to write.", file=sys.stderr)
        return 1

    buf, log = render(
        rows,
        duration_s=a.duration,
        metric=a.metric,
        pitch_by_size=a.pitch_by_size,
    )
    wav_path = out_dir / "commits.wav"
    write_wav(wav_path, buf)
    (out_dir / "commits.json").write_text(json.dumps({
        "branch": a.branch,
        "years": a.years,
        "duration_s": a.duration,
        "metric": a.metric,
        "pitch_by_size": a.pitch_by_size,
        "sample_rate": SAMPLE_RATE,
        "n_commits": len(log),
        "first": log[0]["when"] if log else None,
        "last":  log[-1]["when"] if log else None,
        "commits": log,
    }, indent=2), encoding="utf-8")

    cap = out_dir / "CAPTIONS.md"
    with cap.open("a", encoding="utf-8") as fh:
        fh.write(
            f"\n## commit_sound — generated {dt.datetime.now(dt.timezone.utc).strftime('%Y-%m-%d %H:%M UTC')}\n\n"
            f"What it is: {a.duration:g}-second sonification of {len(rows):,} commits on "
            f"branch `{a.branch}`, last {a.years:g}y of history. Each commit is a 12 ms decayed sine "
            f"click; loudness ∝ log10({'files changed' if a.metric == 'files' else 'lines added+removed'} + 1). "
            f"Output soft-clipped (tanh) so concurrent commits stack without distortion.\n\n"
            f"How to listen: play `commits.wav`. The first beats are commits from "
            f"{log[0]['when'][:10]}; the last are from {log[-1]['when'][:10]}. "
            f"Sparse start → dense end is the expected shape if recent activity dominates.\n\n"
            f"Regenerate: `python scripts/commit_sound.py --years {a.years:g} --duration {a.duration:g}"
            f"{' --pitch-by-size' if a.pitch_by_size else ''}"
            f"{' --metric files' if a.metric == 'files' else ''}`.\n"
        )

    print(f"[commit_sound] wrote {wav_path}", file=sys.stderr)
    print(f"[commit_sound] wrote {out_dir / 'commits.json'}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
