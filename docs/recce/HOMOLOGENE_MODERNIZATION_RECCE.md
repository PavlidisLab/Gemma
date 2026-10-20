# Homologene → NCBI Datasets orthologs — modernization recce

Recce date: 2026-08-27. Baseline SHA: `4bb5365c45`.
Subject: `gemma-core/src/main/java/ubic/gemma/core/loader/genome/gene/ncbi/homology/`.

**Priority: LOW.** Nothing is broken and nothing is blocked on this. The current
implementation works, is off by default in production config, and the data it serves is
stable (frozen, in fact — see §1). File this as opportunistic cleanup for whenever
someone is already in the gene-loading code, not as scheduled work.

## 1. What we do today

Gemma's only definition of homology is "same NCBI HomoloGene group":

- **Source** — `ftp.ncbi.nih.gov` `pub/HomoloGene/last-archive/homologene.data`
  (`gemma-core/src/main/resources/project.properties:13-15`), pulled by
  `HomologeneFetcher` / `HomologeneNcbiFtpResource` into
  `${gemma.download.path}/homologene/`.
- **Parse** — `HomologeneServiceImpl.parseHomologeneFile()` reads the tab-delimited
  dump (`HID | taxon | NCBI gene id | symbol | protein gi | protein accession`) and
  keeps only fields 0-2. It builds two in-memory `ConcurrentHashMap`s:
  `gene2Group (ncbiGeneId → HID)` and `group2Gene (HID → [ncbiGeneId])`.
- **Storage** — none. There is no persisted homology in Gemma 2.0. The legacy
  `GENE_HOMOLOGY` table was dropped in `sql/migrations/db.0.0.5.sql:259`; it survives on
  prod only as cruft and is already on the drop list in
  `docs/design/FLYWAY_PROD_FOLLOWUP.md:20`.
- **Lookup** — `getHomologues(Gene)` maps the gene's `ncbiGeneId` to its HID, then
  resolves every other NCBI id in the group through `geneService.findByNCBIId`.
- **Exposure** — `GeneValueObject.homologues`, populated in
  `GeneServiceImpl.loadGeneDetails` (~line 260) and served by
  `GET /genes/{gene}/homologues` plus the gene overview (`GeneWebService.java:612`).
- **Activation** — opt-in and off by default: `load.homologene=false`
  (`default.properties:256`), lazily initialised through the `AsyncFactoryBean` in
  `HomologeneConfig`. `ConfigurationLinter:35` actively warns when it is enabled under
  the CLI.

## 2. Why it is worth replacing eventually

1. **The source is frozen.** The config points at `last-archive/`, not `current/` —
   NCBI stopped updating HomoloGene. Every homologue Gemma reports is from a build that
   no longer tracks current gene annotation. New NCBI gene ids (and any gene whose id
   has been retired/merged since the freeze) simply have no homology in Gemma, silently.
2. **FTP transport.** `HomologeneFetcher` extends `FtpFetcher` and speaks plain FTP to
   NCBI. That is a transport NCBI has been steadily deprecating in favour of HTTPS
   endpoints, and it is one of the few remaining FTP dependencies in the loader tree.
3. **Coverage is coarse.** HomoloGene groups conflate orthologs and close paralogs, and
   the parser enforces one group per gene (duplicates are dropped with a WARN in
   `parseHomologeneFile`), so the model cannot express one-to-many orthology at all.
4. **No provenance.** Nothing records which HomoloGene build a homologue came from, so
   we cannot report or version the answer.

## 3. Suggested replacement

**NCBI Datasets v2** — CLI docs at
<https://www.ncbi.nlm.nih.gov/datasets/docs/v2/command-line-tools/>. The `datasets` tool
has a dedicated ortholog path under its Genes section ("Download orthologs" /
"Download an ortholog data package"), with `dataformat` converting the resulting package
metadata into TSV. There is a matching v2 REST API if we would rather not ship a binary.

Rough shape of the port, in increasing order of ambition:

- **Minimum** — keep the current in-memory two-map design and the `HomologeneService`
  interface exactly as-is; swap only the `Resource` behind it for one that reads an
  NCBI-Datasets ortholog TSV (fetched over HTTPS, or staged on disk by ops). The service
  is already constructor-injected with a `Resource` (`HomologeneConfig`), so this is a
  single new `Resource` implementation plus a parser change. Retire `HomologeneFetcher`
  and the three `ncbi.*.homologene.*` properties with it.
- **Better** — rename the abstraction away from "homologene" (it is orthology, not
  HomoloGene) and record the source build/date so the REST layer can report provenance.
- **Most** — allow one-to-many orthology and drop the one-group-per-gene invariant. This
  changes `GeneValueObject.homologues` semantics and touches the UI, which is why it is
  a separate decision from the source swap.

Alternative sources worth a glance if we are re-sourcing anyway: NCBI's
`gene_orthologs` flat file (same data as the Datasets ortholog reports, plain
tab-delimited, HTTPS), Ensembl Compara, or OrthoDB. `gene_orthologs` is the smallest
change from where we are since it is already keyed on NCBI gene id, which is what
`gene2Group` and `findByNCBIId` both want.

## 4. Constraints for whoever picks this up

- Respect NCBI rate limits — see `docs/notes/NCBI_RATE_LIMIT_NOTE.md`.
- `HomologeneServiceTest` constructs the factory directly with a `ClassPathResource`
  fixture (`data/loader/genome/homologene/homologene.testdata.txt`) and deliberately
  avoids a Spring context — see the `AbstractAsyncFactoryBean` pitfall in `CLAUDE.md`.
  Any new source needs an equivalent cached fixture in that shape, not a live fetch.
- Keep `load.homologene` defaulting to `false`; the CLI lint warning depends on it.
