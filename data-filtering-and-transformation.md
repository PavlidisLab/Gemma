# Data filtering and transformation performed by Gemma

## Bulk import (RNA-Seq or Microarray)

Very little filtering is done for bulk RNA-Seq data.

RNA-Seq data is normalized in $$\log_2\mathrm{cpm}$$. We also import FPKM and raw counts.

## Single-cell import (single-cell only)

For storage efficiency, we drop barcodes that lack data points. This includes
barcodes that might have genes expressed, but for which there is no design
element in the target platform (e.g. lncRNA or predicted genes).

For Cell Ranger, we import the filtered data which results from applying the
OrdMag and EmptyDrops[^ordmag-emptydrops] algorithms to identify and exclude
doublets. When data is reprocessed by us, this is done systematically. When
data is imported from GEO, we apply some heuristics to detect if the data is
unfiltered and reapply the exact same algorithm Cell Ranger uses.

For other platforms, we assume that the authors have adequately filtered cells.

[^ordmag-emptydrops]: [Calling cell barcodes](https://www.10xgenomics.com/support/software/cell-ranger/latest/algorithms-overview/cr-gex-algorithm#cell_calling)

## Pseudo-bulk aggregation (single-cell only)

When aggregating pseudo-bulks, cells that were identified as outliers by the
cell type annotation pipeline are excluded. They are not counted toward cell
statistics. Gemma stores these outliers as masks in the cell-level
characteristics.

Gemma also keep tracks of how many cells were involved in the calculation of
every single aggregated value. This is used later on to filter samples or genes
with too few cells.

Aggregated data are normalized in $$\log_2\mathrm{cpm}$$ by normalizing each
pseudo-bulk by its library size.

## Processed data creation

Pseudo-bulk aggregation and bulk data import results in "raw" data vectors that
needs to be "processed". This mainly consists of ensuring that the data is on
a $$\log_2$$ scale and performing quantile normalization.

 1. outliers are masked (deprecated)
 2. quantile normalization

Outliers are currently masked in processed data vectors, but this is going to
be removed at some point in favour of masking data prior to an analysis.

Quantile normalization has an impact on later stage filtering because it
assigns values from a common mean distribution. This results in identical
values for genes with the same ranks across samples.

## Filtering prior to analysis

Processed data in Gemma is subject to filters prior to perform any analysis.

For differential expression analysis:

1. outliers are masked (e.g. replaced with missing value indicators)
2. samples with too few cells are masked ($$<100$$; single-cell only)
3. genes with too few cells are dropped ($$<3$$; single-cell only)
4. genes with too many repeated values are dropped (at least 30% of the number
   of samples; there must be at least 4 samples to apply this filter)
5. genes low variance are dropped ($$<0.01$$[^note-on-variance])

For other analyses (PCA, sample correlation, etc), the filtering is a bit more
stringent:

1. probes with no biological sequences are dropped (Microarray only, RNA-Seq
   platforms always have a biological sequence)
2. Affymetrix control probes are dropped (Microarray only)
3. outliers are masked (e.g. replaced with missing value indicators)
4. genes with too many missing values are dropped (at least 7 values or 30% of the number of samples)
6. genes with low expression are dropped ($$<0.2$$)
5. genes low variance are dropped ($$<0.01$$[^note-on-variance])

[^note-on-variance]: Data is always represented on a $$\log_2$$ scale when
                     filtering is performed, so a variance filter of $$0.01$$
                     results in a standard deviation filter of $$0.1$$.

### Probes with biological sequence filter

TODO

### Affymetrix control probe filter

TODO

### Outliers filter

Samples that appear to be outliers in correlation analysis are marked as
outliers by our curators.

### Too few cells filter

Samples with less than 100 cells are masked.

Genes with fewer than 3 cells across samples are removed.

The samples are masked *before* considering the number of cells for a given
gene. This ensures that a sample that is not going to be considered for the
analysis does not contribute to the cell count for the genes it expresses.

### Missing values filter

TODO

### Low variance filter

Genes with less than $$0.01$$ of variance are removed.

### Repetitive values filter

Genes with more than 30% of repeated values are removed.

Due to quantile normalization, values with the same rank are deemed equal.

