## Overview
Gemma is a set of tools for genomics data meta-analysis, currently primarily targeted at the analysis of gene expression profiles. Gemma contains data from thousands of public studies, referencing thousands of published papers. Users can search, access and visualize coexpression and differential expression results.

### Key features:
- Re-annotated expression platforms at the sequence level, which allows for more consistent cross-platform comparisons.
- Support for a variety of expression technologies, including Affymetrix, Illumina and other oligonucleotide arrays, one channel and ratiometric cDNA arrays and RNA-seq data.
- Manual and automated annotation of datasets, that enhance usability of data.
- Access through RESTful web services allows incorporation of data and capabilities of Gemma in any other software.
- Users have the option to securely upload their own data, with an option to be privately shared with other users of their choice. 
- Possibility to create and save gene sets or dataset groups for ease of use in queries.

### Important Q&A
##### How do you pronounce ‘Gemma’?
The ‘G’ is soft, as in ‘general’.

##### How do you map probes to genes?
Essentially as described by Barnes et al, 2005. Gene annotations are obtained from NCBI and UCSC.

##### Isn’t expression data very noisy?
Yes, sometimes. This is a motivation for performing meta-analyses: to look for results that are in some sense consistent across laboratories.

##### Isn’t data quality a problem?
Yes; see the question about noisy data. We have been working hard to ensure that data sets we use for analysis are of high quality, or to “clean up” those that have problems. One problem we have observed is the presence of outlier samples, which are flagged and removed. Batch correction is implemented where possible and we analyze data from raw sources (CEL files or FASTQ files) where possible.

### Glossary
- **Array Design, Platform**: A microarray design. For example, the HG-U133A is a specific Array Design.
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

## Terms and Conditions 
This documentation is licensed under the [Creative Commons Attribution-ShareAlike 3.0 Unported License](https://creativecommons.org/licenses/by-sa/3.0/)

The rest of this section concerns the use of the [Gemma website](https://gemma.msl.ubc.ca/) and the Gemma Web Services, including the Gemma RESTful API, as [documented here](https://gemma.msl.ubc.ca/resources/restapidocs/).

Please **[read the full agreement after clicking here](terms.md)**!

### Non-binding summary of terms:
- We will not use or redistribute non-public data without permission, nor share your registration details with anyone.
- Uploaded data from published work will be considered public. Please consider putting your data in GEO or another public database instead of loading it directly into Gemma. We will load public datasets from GEO on request.
- Use of the site is at your own risk. Users should not upload highly sensitive data.
- Users **must not** upload data that contains identifiable patient information.
- There is no warranty associated with the use of the site or the data provided.

## Credits
If you use our tool for your research, please cite:
*Zoubarev, A., et al., Gemma: A resource for the re-use, sharing and meta-analysis of expression profiling data. Bioinformatics, 2012.* [(Link to paper)](http://dx.doi.org/doi:10.1093/bioinformatics/bts430)

Copyright © University of British Columbia
