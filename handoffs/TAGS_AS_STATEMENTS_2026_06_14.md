# Tags-as-statements — wire shape + agent-side proposer

**Filed:** 2026-06-14 by GUI Claude (apps/curation)
**For:** both brothers — see "Who owns what" below.

## The gap

Experiment-level tags (Gemma's `ExpressionExperiment.characteristic`,
what curation UI calls "tags") are currently shipped on
`/rest/v2/datasets/{id}/design` as a flat shape:

```json
{
  "id": 4,
  "category": { "label": "genotype", "uri": "EFO_0000513" },
  "value":    { "label": "Homozygous negative", "uri": "TGEMO_00001" },
  "inferred": false,
  "inferredSource": "",
  "evidenceCode": "IC"
}
```

This can't express a knockout-applies-to-all-samples shape like
**`genotype · Abca4 · has_genotype · Homozygous negative`**. Paul
flagged this on `experiments/13331?ticket=51` (GSE79061):
the experiment has three separate genotype tags (`Abca4` gene,
`Rdh8` gene, `Homozygous negative`) where the canonical
representation is a single tag with statements binding the two
genes to the genotype outcome.

Paul: "the gemma endpoint already supports the full shape" — i.e.
the Java `Characteristic` entity already carries statements; it's
the wire mirrors + agent proposer + UI that need to catch up.

## Proposed wire shape

Mirror the existing `FactorValue.statements` field one level up on
the tag. Snake-case on the wire, single optional list:

```json
{
  "id": 4,
  "category": { "label": "genotype", "uri": "EFO_0000513" },
  "value":    { "label": "Abca4 Homozygous negative", "uri": null },
  "statements": [
    {
      "subject":   { "label": "Abca4 [mouse] ATP-binding cassette …",
                     "uri": "http://purl.org/commons/record/ncbi_gene/11304" },
      "predicate": { "label": "has_genotype",
                     "uri": "http://gemma.msl.ubc.ca/ont/TGEMO_00166" },
      "object":    { "label": "Homozygous negative",
                     "uri": "http://purl.obolibrary.org/obo/TGEMO_00001" }
    }
  ],
  "inferred": false,
  "inferredSource": "",
  "evidenceCode": "IC"
}
```

- `statements` is optional + back-compat — flat tags leave it absent
  or empty. Pre-existing tags round-trip unchanged.
- `value` stays useful as a human-readable summary even on
  statement-shaped tags (parallel to `FactorValue.free_text_label`).
- Statement shape mirrors `FactorValue.statements[]` exactly:
  optional per-statement `category`, required `subject`, optional
  `predicate` + `object`, optional `subtask_decisions` /
  `original_value` / `supporting_evidence` for the proposer-side
  variant.

## Who owns what

### Bro 2 (Gemma Java REST API — `~/Dev/eclipseworkspace/Gemma/`)

If `Characteristic.statements` already round-trips through the JSON
adapter, **no change needed** — just confirm. Paul said the endpoint
"already supports the full shape" so I think the wire is fine and
this is mostly a "verify + flip on the local_api proxy" task.

If the field is in the entity but not the JSON view, please add it
to the `ExpressionExperiment` characteristic serializer. Snake-
case the field name on the wire (the curation UI does
camelCase → snake_case normalization in `apps/curation/src/api/client.ts`,
and tags' `inferred_source` / `evidence_code` are already snaked
that way).

### Bro 1 (curation agents — `~/Dev/gemma-curation-agents/`)

Two surfaces:

1. **`gemma_curation_agents/agents/curation_proposer/schemas.py`** —
   widen `TagProposal` with an optional
   `statements: list[StatementProposal] = Field(default_factory=list)`
   field. Same shape as `FactorValueProposal.statements`.
   Back-compat: legacy proposals validate unchanged with
   `statements=[]`.

2. **Tag proposer prompt** — teach the proposer to decompose values
   into S-P-O when the source evidence supports it. Canonical
   trigger: tags with `category=genotype` where the paper describes
   a gene knockout / knock-in / mutation — emit a statement with
   `subject=<gene>`, `predicate=<has_genotype>`,
   `object=<allele/state>` instead of three loose tags. Other
   likely cases: `treatment` tags decomposing into
   `subject=<drug>`, `predicate=<delivered_at_dose>`, `object=<dose>`
   (mirrors the FV-side pattern). Predicate vocabulary should mirror
   what the FV proposer uses.

3. **Audit-side**: the calibration pipeline's tag-matching needs to
   compare statement-shaped tags. For now (until the rich shape is
   widespread on golds), treating two tags as a match when their
   `(category, value)` pair agrees IS fine — statement
   enrichment is additive. Later we can sharpen the match using
   statement-level comparison (the FV-level `pairFvs` logic is a
   reasonable template).

4. **Local_api**: `/rest/v2/datasets/{id}/design` needs to forward
   the statements field when the Gemma source carries it. Right
   now I curl'd 13331's design and got no `statements` on tags —
   not sure whether that's a local_api gap or a snapshot that
   pre-dates the field.

## UI side (already shipped 2026-06-14, this turn)

For visibility — what's done on the curation UI:

- **`Tag` type** (`apps/curation/src/features/experiment/types.ts`)
  gained `statements?: Statement[]`. Same `Statement` shape FVs use.
- **`TagProposal` type** (`apps/curation/src/api/types.ts`)
  gained `statements?: StatementProposal[]`. Mirrors bro 1's pending
  schema widening.
- **`TagBar`** (`overview/OverviewPanel.tsx` —
  `EditableDirectGroupChip`) now renders statements when present:
  both the single-tag chip and the multi-tag-expanded inner chip
  switch to `<TagStatementInline>` (a new module-local helper)
  when `tag.statements?.length > 0`. Falls back to the flat
  value chip otherwise. Convention matches `FvDisplayRow`'s
  ontology-vs-free-text styling (emerald-weight = anchored,
  italic slate = free-text, mono caption = predicate).

What's NOT done yet:
- Tag editor (`ChipEditor`) still operates on the flat
  `{category, value}` model. Editing a statement-tag drops the
  statements. Phase 2 of the UI work.
- Tag finding cards (`calibration_match` / `calibration_agent_extra`
  / `calibration_gold_only_miss` for tags) still render via the
  flat tag shape. Phase 3 of the UI work.
- Mutations (`addTag` / `setTagCategory` / `setTagValue`) don't
  send statements. Phase 2.
- Comparison panel (`base=… cmp=…`) tag rendering hasn't been
  touched yet.

Paul confirmed full-chain scope; I'm shipping phased so each step
is reviewable.

## Concrete example to verify against

GSE79061 (Gemma id 13331). Current shipping data: three separate
genotype tags. Desired after this work lands:

```json
"tags": [
  {
    "category": { "label": "genotype", "uri": "EFO_0000513" },
    "value": { "label": "Abca4 / Rdh8 double knockout", "uri": null },
    "statements": [
      { "subject": { "label": "Abca4 [mouse] …", "uri": "ncbi_gene/11304" },
        "predicate": { "label": "has_genotype", "uri": "TGEMO_00166" },
        "object": { "label": "Homozygous negative", "uri": "TGEMO_00001" } },
      { "subject": { "label": "Rdh8 [mouse] …", "uri": "ncbi_gene/235033" },
        "predicate": { "label": "has_genotype", "uri": "TGEMO_00166" },
        "object": { "label": "Homozygous negative", "uri": "TGEMO_00001" } }
    ]
  }
]
```

Replaces tags id=2, 3, 4 from the current shipping payload.
