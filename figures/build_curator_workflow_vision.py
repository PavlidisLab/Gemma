"""Curator workflow system — architecture vision figure.

Documents the moving parts of the new curator workflow system:
  - Curator-facing dashboard (gemma-ui Curation app) showing live
    shared tickets
  - Two action paths from a ticket: curate (edit design / tags /
    factor values), or dispatch a Nextflow pipeline run on Slurm
  - Experiment lifecycle: Loading -> Review -> Curating -> Public
    with rework loop back to Review
  - All the backing services, storage, and compute pieces that
    make it work — dashed borders mark planned components

Sources:
  - PIPELINES_AND_SCHEDULER_RECCE.md
  - GEMMA_CURATION_CALL_SURFACE.md
  - GEMMA_CURATION_FEATURE_WISHLIST.md
  - HEATMAP_REWRITE_RECCE.md
"""
from __future__ import annotations

import os
import sys
from datetime import date

SKILL_PATH = os.path.expanduser(
    "~/.claude/skills/architecture-figures/python"
)
if SKILL_PATH not in sys.path:
    sys.path.insert(0, SKILL_PATH)

from pavlab_arch.style import apply_rcparams
from pavlab_arch.layout import figure, svg_safe
from pavlab_arch.palette import (
    ACCENT, ACCENT_2, ACCENT_3, ACCENT_4, ACCENT_5,
    DET, SUBTLE, GRID, TEXT, SOFT_BG, tint,
)
from pavlab_arch.primitives import (
    box, oval, cylinder, circle,
    arrow, labeled_arrow, container, legend_block,
)

apply_rcparams()
fig, ax = figure(shape="slide")

# Title
ax.text(1.0, 97.5, "Curator workflow system — architecture vision",
        ha="left", va="top", fontsize=14, fontweight="bold", color=TEXT)
ax.text(1.0, 93.8,
        "Moving parts as of 2026-05-21 · dashed border = planned, not yet shipped",
        ha="left", va="top", fontsize=8.5, color=SUBTLE, style="italic")

# Experiment lifecycle state machine (top right)
LS_Y_CTR, LS_H, LS_W, LS_X0, LS_GAP = 89.0, 5.5, 8.5, 52.0, 1.3
states = [("Loading", DET), ("Review", ACCENT_3),
          ("Curating", ACCENT), ("Public", ACCENT_2)]
state_centres: dict[str, tuple[float, float]] = {}
for i, (name, c) in enumerate(states):
    x = LS_X0 + i * (LS_W + LS_GAP)
    box(ax, x, LS_Y_CTR - LS_H / 2, LS_W, LS_H,
        fc=tint(c), ec=c, text=name, text_color=c,
        fontsize=9.0, fontweight="bold", radius=0.7, lw=1.4)
    state_centres[name] = (x + LS_W / 2, LS_Y_CTR)

for a, b in [("Loading", "Review"), ("Review", "Curating"),
             ("Curating", "Public")]:
    xa, _ = state_centres[a]
    xb, _ = state_centres[b]
    arrow(ax, xa + LS_W / 2 - 0.2, LS_Y_CTR, xb - LS_W / 2 + 0.2, LS_Y_CTR,
          color=SUBTLE, lw=1.4, mut=10, shrinkA=2, shrinkB=2)

xp, _ = state_centres["Public"]
xr, _ = state_centres["Review"]
labeled_arrow(ax, xp, LS_Y_CTR - LS_H / 2 - 0.3,
              xr, LS_Y_CTR - LS_H / 2 - 0.3,
              "return to review",
              connectionstyle="arc3,rad=0.45",
              color=ACCENT_4, lw=1.4, mut=10, style="-|>",
              linestyle=(0, (4, 2)), shrinkA=2, shrinkB=2,
              label_color=ACCENT_4, label_side=-2.5,
              label_along=0.5, label_fontsize=7.0)


def tier_band(y: float, h: float, label: str) -> None:
    box(ax, 0.5, y, 99.0, h, fc=SOFT_BG, ec=GRID, lw=0.6, radius=1.5)
    ax.text(2.5, y + h / 2, label, ha="left", va="center",
            fontsize=8.0, color=SUBTLE, style="italic")


# Tier 1 — Frontend
T1_Y, T1_H = 64.0, 19.0
tier_band(T1_Y, T1_H, "Front-end\n(gemma-ui)")

CURATOR_CX, CURATOR_CY = 13.0, 73.5
circle(ax, CURATOR_CX, CURATOR_CY, r=4.2,
       fc=tint(ACCENT), ec=ACCENT,
       text="curator", text_color=ACCENT,
       fontsize=8.5, fontweight="bold", lw=1.4)
ax.text(CURATOR_CX, CURATOR_CY - 6.8, "browser",
        ha="center", va="center", fontsize=7.5,
        color=SUBTLE, style="italic")

CURATION_X, CURATION_Y, CURATION_W, CURATION_H = 22.0, 67.0, 32.0, 13.5
box(ax, CURATION_X, CURATION_Y, CURATION_W, CURATION_H,
    fc=tint(ACCENT), ec=ACCENT, lw=1.4, radius=0.9)
ax.text(CURATION_X + CURATION_W / 2, CURATION_Y + CURATION_H - 2.8,
        "Curation app", ha="center", va="center",
        fontsize=10.0, fontweight="bold", color=TEXT)
ax.text(CURATION_X + CURATION_W / 2, CURATION_Y + CURATION_H - 6.0,
        "dashboard · live shared tickets",
        ha="center", va="center", fontsize=7.5,
        color=SUBTLE, style="italic")
sub_w, sub_h = 9.0, 4.2
sub_y = CURATION_Y + 1.8
for i, (lbl, col) in enumerate([("Tickets", ACCENT_3),
                                  ("Edit ED", ACCENT),
                                  ("Run jobs", ACCENT_5)]):
    sx = CURATION_X + 1.7 + i * (sub_w + 0.7)
    box(ax, sx, sub_y, sub_w, sub_h,
        fc=tint(col), ec=col, text=lbl, text_color=col,
        fontsize=8.0, fontweight="bold", lw=1.0, radius=0.5)

BROWSER_X, BROWSER_Y, BROWSER_W, BROWSER_H = 60.0, 67.0, 28.0, 13.5
box(ax, BROWSER_X, BROWSER_Y, BROWSER_W, BROWSER_H,
    fc=tint(ACCENT), ec=ACCENT, lw=1.4, radius=0.9)
ax.text(BROWSER_X + BROWSER_W / 2, BROWSER_Y + BROWSER_H - 2.8,
        "Browser app", ha="center", va="center",
        fontsize=10.0, fontweight="bold", color=TEXT)
ax.text(BROWSER_X + BROWSER_W / 2, BROWSER_Y + BROWSER_H - 6.0,
        "public browse · gene + dataset pages",
        ha="center", va="center", fontsize=7.5,
        color=SUBTLE, style="italic")
bsub_w = 8.0
bsub_y = BROWSER_Y + 1.8
for i, (lbl, col) in enumerate([("Datasets", ACCENT_2),
                                  ("Genes", ACCENT_5),
                                  ("Search", ACCENT_3)]):
    sx = BROWSER_X + 1.5 + i * (bsub_w + 0.7)
    box(ax, sx, bsub_y, bsub_w, sub_h,
        fc=tint(col), ec=col, text=lbl, text_color=col,
        fontsize=8.0, fontweight="bold", lw=1.0, radius=0.5)

labeled_arrow(ax, CURATOR_CX + 4.5, CURATOR_CY,
              CURATION_X, CURATION_Y + CURATION_H / 2,
              "HTTPS", color=SUBTLE, lw=1.4, mut=12,
              style="<|-|>", shrinkA=2, shrinkB=2,
              label_along=0.55, label_side=2.0)
labeled_arrow(ax, CURATOR_CX + 4.5, CURATOR_CY - 1.5,
              BROWSER_X, BROWSER_Y + 4.0,
              "HTTPS", color=SUBTLE, lw=1.2, mut=10,
              style="<|-|>", shrinkA=2, shrinkB=2,
              connectionstyle="arc3,rad=-0.18",
              label_along=0.55, label_side=-2.5)

# Tier 2 — Services
T2_Y, T2_H = 42.0, 20.0
tier_band(T2_Y, T2_H, "Back-end\nservices")

AGENT_X, AGENT_Y, AGENT_W, AGENT_H = 7.0, 45.0, 24.0, 14.0
box(ax, AGENT_X, AGENT_Y, AGENT_W, AGENT_H,
    fc=tint(ACCENT_5), ec=ACCENT_5, lw=1.4, radius=0.9)
ax.text(AGENT_X + AGENT_W / 2, AGENT_Y + AGENT_H - 2.8,
        "gemma-curation-agents", ha="center", va="center",
        fontsize=9.3, fontweight="bold", color=TEXT)
ax.text(AGENT_X + AGENT_W / 2, AGENT_Y + AGENT_H - 6.6,
        "Python · FastAPI\nproposers + auditor + pub finder",
        ha="center", va="center", fontsize=7.3,
        color=SUBTLE, style="italic")

REST_X, REST_Y, REST_W, REST_H = 35.0, 45.0, 26.0, 14.0
box(ax, REST_X, REST_Y, REST_W, REST_H,
    fc=tint(ACCENT_3), ec=ACCENT_3, lw=1.4, radius=0.9)
ax.text(REST_X + REST_W / 2, REST_Y + REST_H - 2.8,
        "gemma-rest", ha="center", va="center",
        fontsize=10.0, fontweight="bold", color=TEXT)
ax.text(REST_X + REST_W / 2, REST_Y + REST_H - 6.6,
        "Java 17 · Spring · Jersey\nREST API + ACL + service layer",
        ha="center", va="center", fontsize=7.3,
        color=SUBTLE, style="italic")

SCHED_X, SCHED_Y, SCHED_W, SCHED_H = 65.0, 45.0, 22.0, 14.0
box(ax, SCHED_X, SCHED_Y, SCHED_W, SCHED_H,
    fc=tint(ACCENT_3), ec=ACCENT_3, lw=1.4, radius=0.9,
    linestyle=(0, (4, 2.5)))
ax.text(SCHED_X + SCHED_W / 2, SCHED_Y + SCHED_H - 2.8,
        "Pipeline scheduler", ha="center", va="center",
        fontsize=9.3, fontweight="bold", color=TEXT)
ax.text(SCHED_X + SCHED_W / 2, SCHED_Y + SCHED_H - 6.6,
        "PLANNED · extends TaskRunningService\n+ PipelineExecutor SPI",
        ha="center", va="center", fontsize=7.3,
        color=SUBTLE, style="italic")

LLM_X, LLM_Y, LLM_W, LLM_H = 90.0, 47.0, 9.0, 10.0
box(ax, LLM_X, LLM_Y, LLM_W, LLM_H,
    fc=tint(DET), ec=DET, lw=1.2, radius=0.7,
    linestyle=(0, (3, 2)))
ax.text(LLM_X + LLM_W / 2, LLM_Y + LLM_H - 2.5,
        "LLM API", ha="center", va="center",
        fontsize=8.5, fontweight="bold", color=TEXT)
ax.text(LLM_X + LLM_W / 2, LLM_Y + LLM_H - 5.5,
        "external\n3rd-party", ha="center", va="center",
        fontsize=7.0, color=SUBTLE, style="italic")

labeled_arrow(ax, CURATION_X + 4.0, CURATION_Y,
              AGENT_X + AGENT_W * 0.45, AGENT_Y + AGENT_H,
              "REST/JSON", color=SUBTLE, lw=1.4, mut=12,
              style="<|-|>", shrinkA=2, shrinkB=2,
              connectionstyle="arc3,rad=0.12",
              label_along=0.55, label_side=2.2)
labeled_arrow(ax, CURATION_X + CURATION_W * 0.7, CURATION_Y,
              REST_X + REST_W * 0.35, REST_Y + REST_H,
              "REST/JSON", color=SUBTLE, lw=1.4, mut=12,
              style="<|-|>", shrinkA=2, shrinkB=2,
              label_along=0.55, label_side=2.2)
labeled_arrow(ax, BROWSER_X + BROWSER_W * 0.3, BROWSER_Y,
              REST_X + REST_W * 0.7, REST_Y + REST_H,
              "REST/JSON", color=SUBTLE, lw=1.4, mut=12,
              style="<|-|>", shrinkA=2, shrinkB=2,
              label_along=0.5, label_side=2.0)
labeled_arrow(ax, AGENT_X + AGENT_W, AGENT_Y + AGENT_H * 0.55,
              REST_X, REST_Y + REST_H * 0.55,
              "REST/JSON", color=SUBTLE, lw=1.4, mut=12,
              style="<|-|>", shrinkA=2, shrinkB=2,
              label_along=0.5, label_side=2.0)
labeled_arrow(ax, REST_X + REST_W, REST_Y + REST_H * 0.55,
              SCHED_X, SCHED_Y + SCHED_H * 0.55,
              "in-process", color=SUBTLE, lw=1.4, mut=12,
              style="<|-|>", shrinkA=2, shrinkB=2,
              label_along=0.5, label_side=2.0)
labeled_arrow(ax, AGENT_X + AGENT_W * 0.7, AGENT_Y + AGENT_H,
              LLM_X + LLM_W * 0.4, LLM_Y + LLM_H,
              "HTTPS", color=SUBTLE, lw=1.3, mut=10,
              style="-|>", linestyle=(0, (4, 2.5)),
              shrinkA=2, shrinkB=2,
              connectionstyle="arc3,rad=-0.38",
              label_along=0.45, label_side=2.0)

# Tier 3 — Storage
T3_Y, T3_H = 22.0, 18.0
tier_band(T3_Y, T3_H, "Storage")

PROP_CX, PROP_CY, PROP_W, PROP_H = 12.0, 31.0, 11.5, 8.0
oval(ax, PROP_CX, PROP_CY, PROP_W, PROP_H,
     fc=tint(ACCENT_5), ec=ACCENT_5,
     text="proposals\nSQLite", text_color=ACCENT_5,
     fontsize=8.0, fontweight="bold", lw=1.3)

FAISS_CX, FAISS_CY, FAISS_W, FAISS_H = 25.0, 31.0, 11.5, 8.0
oval(ax, FAISS_CX, FAISS_CY, FAISS_W, FAISS_H,
     fc=tint(ACCENT_5), ec=ACCENT_5,
     text="FAISS\nembedding index", text_color=ACCENT_5,
     fontsize=7.8, fontweight="bold", lw=1.3)

GEMD_CX, GEMD_CY, GEMD_W, GEMD_H = 47.0, 31.0, 16.0, 12.0
cylinder(ax, GEMD_CX, GEMD_CY, GEMD_W, GEMD_H,
         fc=tint(ACCENT_2), ec=ACCENT_2,
         text="gemd MySQL", text_color=ACCENT_2,
         fontsize=10.0, fontweight="bold", lw=1.4)
ax.text(GEMD_CX, GEMD_CY - GEMD_H / 2 - 1.4,
        "system of record · ~1B vector rows",
        ha="center", va="top", fontsize=7.2,
        color=SUBTLE, style="italic")

PRUN_CX, PRUN_CY, PRUN_W, PRUN_H = 66.0, 31.0, 14.0, 10.0
cylinder(ax, PRUN_CX, PRUN_CY, PRUN_W, PRUN_H,
         fc=tint(ACCENT_2), ec=ACCENT_2,
         text="PIPELINE_RUN", text_color=ACCENT_2,
         fontsize=9.0, fontweight="bold", lw=1.4,
         linestyle=(0, (4, 2.5)))
ax.text(PRUN_CX, PRUN_CY - PRUN_H / 2 - 1.4,
        "PLANNED · job state",
        ha="center", va="top", fontsize=7.0,
        color=SUBTLE, style="italic")

LUC_CX, LUC_CY, LUC_W, LUC_H = 85.0, 31.0, 13.0, 8.0
oval(ax, LUC_CX, LUC_CY, LUC_W, LUC_H,
     fc=tint(ACCENT_3), ec=ACCENT_3,
     text="Lucene\nsearch indexes", text_color=ACCENT_3,
     fontsize=7.8, fontweight="bold", lw=1.3)

labeled_arrow(ax, AGENT_X + AGENT_W * 0.3, AGENT_Y,
              PROP_CX, PROP_CY + PROP_H / 2,
              "SQL", color=SUBTLE, lw=1.3, mut=10,
              style="<|-|>", shrinkA=2, shrinkB=2,
              label_along=0.55, label_side=2.0)
labeled_arrow(ax, AGENT_X + AGENT_W * 0.75, AGENT_Y,
              FAISS_CX, FAISS_CY + FAISS_H / 2,
              "mmap", color=SUBTLE, lw=1.3, mut=10,
              style="<|-|>", shrinkA=2, shrinkB=2,
              label_along=0.55, label_side=2.0)
labeled_arrow(ax, REST_X + REST_W * 0.4, REST_Y,
              GEMD_CX, GEMD_CY + GEMD_H / 2,
              "JDBC", color=SUBTLE, lw=1.5, mut=12,
              style="<|-|>", shrinkA=2, shrinkB=2,
              label_along=0.5, label_side=2.0)
labeled_arrow(ax, REST_X + REST_W * 0.85, REST_Y,
              LUC_CX - LUC_W * 0.2, LUC_CY + LUC_H / 2,
              "Hibernate Search", color=SUBTLE, lw=1.2, mut=10,
              style="<|-|>", shrinkA=2, shrinkB=2,
              connectionstyle="arc3,rad=0.22",
              label_along=0.55, label_side=2.0,
              label_fontsize=7.0)
labeled_arrow(ax, SCHED_X + SCHED_W * 0.4, SCHED_Y,
              PRUN_CX, PRUN_CY + PRUN_H / 2,
              "JDBC", color=SUBTLE, lw=1.3, mut=10,
              style="<|-|>", linestyle=(0, (4, 2.5)),
              shrinkA=2, shrinkB=2,
              label_along=0.55, label_side=2.0)

# Tier 4 — Compute
T4_Y, T4_H = 3.0, 17.0
tier_band(T4_Y, T4_H, "Compute\n(cluster)")

container(ax, 30.0, 5.5, 90.0, 17.5, label="Slurm cluster",
          ec=DET, lw=1.4, label_fontsize=8.5)

SCA_X, SCA_Y, SCA_W, SCA_H = 33.5, 7.0, 25.0, 7.5
box(ax, SCA_X, SCA_Y, SCA_W, SCA_H,
    fc=tint(DET), ec=DET, lw=1.3, radius=0.6)
ax.text(SCA_X + SCA_W / 2, SCA_Y + SCA_H - 2.0,
        "sc-annotation-pipeline", ha="center", va="center",
        fontsize=8.5, fontweight="bold", color=TEXT)
ax.text(SCA_X + SCA_W / 2, SCA_Y + SCA_H - 4.8,
        "Nextflow · scVI + RF",
        ha="center", va="center", fontsize=7.0,
        color=SUBTLE, style="italic")

RNA_X, RNA_Y, RNA_W, RNA_H = 62.0, 7.0, 25.0, 7.5
box(ax, RNA_X, RNA_Y, RNA_W, RNA_H,
    fc=tint(DET), ec=DET, lw=1.3, radius=0.6)
ax.text(RNA_X + RNA_W / 2, RNA_Y + RNA_H - 2.0,
        "rnaseq-pipeline", ha="center", va="center",
        fontsize=8.5, fontweight="bold", color=TEXT)
ax.text(RNA_X + RNA_W / 2, RNA_Y + RNA_H - 4.8,
        "Luigi today / Nextflow planned",
        ha="center", va="center", fontsize=7.0,
        color=SUBTLE, style="italic")

labeled_arrow(ax, SCHED_X + SCHED_W * 0.55, SCHED_Y,
              SCA_X + SCA_W * 0.5, SCA_Y + SCA_H,
              "sbatch · SSH", color=ACCENT_3, lw=1.5, mut=12,
              style="-|>", linestyle=(0, (4, 2.5)),
              shrinkA=2, shrinkB=2,
              connectionstyle="arc3,rad=0.25",
              label_along=0.55, label_side=-2.0,
              label_fontsize=7.2)
labeled_arrow(ax, SCHED_X + SCHED_W * 0.85, SCHED_Y,
              RNA_X + RNA_W * 0.5, RNA_Y + RNA_H,
              "sbatch · SSH", color=ACCENT_3, lw=1.5, mut=12,
              style="-|>", linestyle=(0, (4, 2.5)),
              shrinkA=2, shrinkB=2,
              connectionstyle="arc3,rad=0.30",
              label_along=0.55, label_side=-2.0,
              label_fontsize=7.2)
labeled_arrow(ax, SCA_X + SCA_W * 0.4, SCA_Y + SCA_H,
              GEMD_CX - GEMD_W * 0.3, GEMD_CY - GEMD_H / 2,
              "results · CLI / JDBC", color=ACCENT_2, lw=1.4, mut=12,
              style="-|>", shrinkA=2, shrinkB=2,
              connectionstyle="arc3,rad=-0.32",
              label_along=0.45, label_side=2.0,
              label_fontsize=7.2)
labeled_arrow(ax, RNA_X + RNA_W * 0.5, RNA_Y + RNA_H,
              GEMD_CX + GEMD_W * 0.3, GEMD_CY - GEMD_H / 2,
              "results · CLI / JDBC", color=ACCENT_2, lw=1.4, mut=12,
              style="-|>", shrinkA=2, shrinkB=2,
              connectionstyle="arc3,rad=0.32",
              label_along=0.55, label_side=2.0,
              label_fontsize=7.2)

# Legend
legend_block(ax, x=1.5, y_top=2.5, specs=[
    (ACCENT,   False, "Frontend / curator",  "curation + browser apps"),
    (ACCENT_3, False, "Backend service",     "Java REST + scheduler"),
    (ACCENT_5, False, "Agent / ML-adjacent", "Python proposers · FAISS"),
    (ACCENT_2, False, "System of record",    "gemd MySQL · cylinder"),
    (DET,      False, "External / compute",  "Slurm · LLM API · dashed"),
], title=None, chip_h=1.4, row_gap=0.4, label_fontsize=7.0)

ax.text(52.0, 1.4,
        "Dashed border = planned, not yet shipped.\n"
        "Bidirectional arrows where read + write or request + response.",
        ha="left", va="bottom", fontsize=7.2,
        color=SUBTLE, style="italic")
ax.text(99.0, 0.3,
        "build_curator_workflow_vision.py · " + date.today().isoformat(),
        ha="right", va="bottom", fontsize=6.5, color=SUBTLE)

svg_safe(ax)
out_dir = os.path.dirname(os.path.abspath(__file__))
canonical = os.path.join(out_dir, "curator_workflow_vision.svg")
stamped = os.path.join(
    out_dir, f"curator_workflow_vision_{date.today().isoformat()}.svg")
for p in (canonical, stamped):
    fig.savefig(p, format="svg", bbox_inches="tight", facecolor="white")
    print(f"wrote {p}")
png_path = os.path.join(out_dir, "curator_workflow_vision.png")
fig.savefig(png_path, format="png", dpi=170,
            bbox_inches="tight", facecolor="white")
print(f"wrote {png_path}")
