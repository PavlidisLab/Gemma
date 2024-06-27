# Overview
Gemma (pronounced: _Jemma_) is database of curated and re-analyzed gene expression studies. 

## Key features:
- All data sets are manually curated and QC'd in a standardized pipeline.
- Support for a variety of expression technologies, including RNA-seq, Affymetrix, Illumina and other oligonucleotide arrays, one channel and ratiometric cDNA arrays.
- Re-annotated microarray platforms at the sequence level, which allows for more consistent cross-platform comparisons.
- Re-analysis of data sets for differential expression
- GUI and programmatic access

## Terms and Conditions 

Please refer to the **[Terms and conditions](terms.md)** page!

## Glossary

You may encounter some of these terms when using Gemma.

- **Array Design, Platform**: A microarray design or other expression platform. For example, the HG-U133A is a specific Array Design.
- **BioMaterial**: An RNA sample, along with the description of where and how it was obtained and prepared. For data collected on ratiometric arrays, this may reflect a combination of two RNA samples, one of which was used as a reference. (Near-synonym: **sample**)
- **BioAssay, Assay**: The combination of a BioMaterial with an ArrayDesign Represents a RNA sample run on a single microarray. (Near-synonym: **sample**)
- **Characteristic, Annotation, Tag**: A descriptive annotation applied to a dataset or sample. For example, “Organism Part: Liver” is a characteristic which pairs the MGED Ontology term “Organism part” with the value “Liver”. Characteristics are also used to describe Experimental Factors and Factor Values.
- **Data vector**: Typically refers to an expression profile for one probe in one dataset, though the precise definition depends on context.
- **Experimental design**: Describes the organization of samples in an Expression Experiment.
- **Experimental factor**: A value which was varied (or variable) in an experiment and which is considered part of the experimental design. Each sample in the study will have a factor value for each factor. Factors can be categorical (e.g., “tissue”) or continuous (e.g., “Weight in grams”).
- **Expression Experiment, Experiment, Dataset, Study**: A group of BioAssays, for example as grouped together in a single GEO accession.
- **Expression Experiment Set, Dataset set**: A grouping of Expression Experiments.
- **Factor value**: See experimental factor for an explanation.
- **Preferred quantitation type**: The quantitation type that is used when we want to speak of “expression levels” or “expression measurements”. The values for the preferred quantitation type are the ones that are actually used in most analyses or visualizations. Each study should have only one preferred quantitation type.
- **Processed expression data**: The data for the preferred quantitation type.
- **Quantitation type**: Describes values measured on in bioassays, either coming from the original scan or derived values. For example, “Signal”, “Background” or “Absent/Present call” might be descriptions of quantitation types.
- **Sample**: In typical usage, synonymous with BioAssay. May refer to a BioMaterial. Hopefully the intended meaning should be clear from the context (or not important to distinguish).


## Using the Gemma website

This section is currently undergoing revision.

## Programmatic use of Gemma

Gemma has a RESTful API, documented  **[here](https://gemma.msl.ubc.ca/resources/restapidocs/)**. There you will find all necessary information on how to interact with gemma
programatically. 

However, most users will find more convenient programmatic access to Gemma's data and analyses via <a href="https://doi.org/doi:10.18129/B9.bioc.gemma.R">gemma.R</a> (R/Bioconductor)
 and <a href="https://github.com/PavlidisLab/gemmapy">gemmapy</a> (Python).

The R and Python packages offer similar functionality and have their own documentation and examples/vignettes. For convenience we have 
gathered some examples in both languages here **[here](R-python-examples.html)**.

## Data sources
We are indebted to the many researchers who have made data publicly available. Lists of published papers that relate to the data included in Gemma are available [here (full list)](https://gemma.msl.ubc.ca/bibRef/showAllEeBibRefs.html) and [here (search)](https://gemma.msl.ubc.ca/bibRef/searchBibRefs.html).

If your data is in Gemma, and your paper is not listed, please let us know.

## Contact

If you find a problem or need help, you can file a new github issue, or contact us at [pavlab-support@msl.ubc.ca](mailto:pavlab-support@msl.ubc.ca).

## Credits

### Financial support

As of 2023, Gemma is primarily supported by a grant from NIMH, and additional support from NSERC and CFI, for which we are grateful!

### Citing

If you use any of Gemma tools or data for your research, please cite one of the following papers:

[Lim et al. Curation of over 10 000 transcriptomic studies to enable data reuse. Database, 2021](https://doi.org/10.1093/database/baab006)

[Zoubarev, A., et al., Gemma: A resource for the re-use, sharing and meta-analysis of expression profiling data. Bioinformatics, 2012.](http://dx.doi.org/doi:10.1093/bioinformatics/bts430)

**Project lead:**
[Paul Pavlidis, Ph.D.](http://pavlab.msl.ubc.ca/paul-pavlidis/)


Copyright © University of British Columbia
