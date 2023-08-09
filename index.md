# Overview
Gemma is database of curated and re-analyzed gene expression studies. 

This webpage serves as an introduction and end-user documentation.

## Table of contents
- [Key features:](#key-features-)
- [Important Q&A](#important-q-a)
- [Glossary](#glossary)
- [Terms and Conditions](#terms-and-conditions)
- [Using the Gemma website](#using-the-gemma-website)
- [RESTful API](#restful-api)
- [Contact](#contact)
- [Data Sources](#data-sources)
- [Credits](#credits)
  * [Financial support](#financial-support)
  * [Publications](#publications)
  * [Contributors](#contributors)
  * [Data sources](#data-sources)

## Key features:
- Re-annotated expression platforms at the sequence level, which allows for more consistent cross-platform comparisons.
  + You can read about the **[data curation process here](curation.md)**.
- Support for a variety of expression technologies, including Affymetrix, Illumina and other oligonucleotide arrays, one channel and ratiometric cDNA arrays and RNA-seq data.
- Manual and automated annotation of datasets, that enhance usability of data.
- Access through the [Gemma website](https://gemma.msl.ubc.ca) provides a graphical interface for easy access.
- Access through [RESTful web services](https://gemma.msl.ubc.ca/resources/restapidocs/) allows incorporation of data and computational capabilities of Gemma with any other software.

## Important Q&A
##### How do you pronounce ‘Gemma’?
The ‘G’ is soft, as in ‘general’.

##### How do you map microarray probes to genes?
Essentially as described by Barnes et al, 2005. Gene annotations are obtained from NCBI and UCSC.

##### Isn’t data quality a problem?
We have been working hard to ensure that data sets we use for analysis are of high quality, or to “clean up” those that have problems. One problem we have observed is the presence of outlier samples, which are flagged and removed. Batch correction is implemented where possible and we analyze data from raw sources (CEL files or FASTQ files) where possible. We also have a **[dataset scoring system](geeq.md)** in place.

## Glossary
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

The rest of this section concerns the use of the [Gemma website](https://gemma.msl.ubc.ca/) and the Gemma Web Services, including the Gemma RESTful API (which is [documented here](https://gemma.msl.ubc.ca/resources/restapidocs/)).

Please **[read the full agreement after clicking here](terms.md)**!

### Non-binding summary of terms:
- Use of the site is at your own risk. Users should not upload highly sensitive data.
- There is no warranty associated with the use of the site or the data provided.

## Using the Gemma website
These guides will help you navigate and use the tools provided through the [gemma website](https://gemma.msl.ubc.ca/).
- **[Searching for Coexpression and differential expression](search.md)**
- **[Uploading your own experimental data](upload.md)**
- **[Manging gene and experiment sets](gene_experiment_sets.md)**
- **[Browsing and managing experimental designs](designs.md)**
- **[About Phenocarta](phenocarta.md)**
- **[About the dataset scores](geeq.md)**

## RESTful API
The API has its own interactive documentation, where you will find all necessary information on how to interact with gemma
programatically. 
Please follow **[this link to the RESTful API documentation](https://gemma.msl.ubc.ca/resources/restapidocs/)**

## Data sources
We are indebted to the many researchers who have made data publicly available. Lists of published papers that relate to the data included in Gemma are available [here (full list)](https://gemma.msl.ubc.ca/bibRef/showAllEeBibRefs.html) and [here (search)](https://gemma.msl.ubc.ca/bibRef/searchBibRefs.html).

If your data is in Gemma, and your paper is not listed, please let us know.

## Contact

If you find a problem or need help, you can file a new github issue, or contact us at [pavlab-support@msl.ubc.ca](mailto:pavlab-support@msl.ubc.ca).

## Credits

### Financial support

| [<img src="{{site.imgurl}}/logo_NIH.png" alt="National Insitute of Health"/>](https://www.nih.gov/) | [<img src="{{site.imgurl}}/logo_CFI.png" alt="Canada foundation for innovation"/>](https://www.innovation.ca/) | [<img src="{{site.imgurl}}/logo_MSFHR.jpg" alt="Michael Smith Foundation for Health Research"/>](http://www.msfhr.org/) | [<img src="{{site.imgurl}}/logo_NDN.png" alt="Neuro dev net"/>](http://www.neurodevnet.ca/) | 
| :---: | :---: | :---: | :---: |
| (NIGMS/NIMH) Grant: GM0769990 | [<img src="{{site.imgurl}}/logo_CIHR.png" alt="Canadian Institute of Health Research"/>](http://www.cihr-irsc.gc.ca/) |  [<img src="{{site.imgurl}}/logo_GBC.gif" alt="Genome British Columbia"/>](https://www.genomebc.ca/) | [<img src="{{site.imgurl}}/logo_NSERC.png" alt="Natural Sciences and Engineering Research Council of Canada"/>](http://www.nserc-crsng.gc.ca/) |

### Publications

**If you use any of Gemma tools for your research, please cite:**
> *[Zoubarev, A., et al., Gemma: A resource for the re-use, sharing and meta-analysis of expression profiling data. Bioinformatics, 2012.](http://dx.doi.org/doi:10.1093/bioinformatics/bts430)*

**Other publications**
>[Lee, H.K., et al., Coexpression analysis of human genes across many microarray data sets. Genome Research, 2004. 14: p. 1085-1094.](https://www.ncbi.nlm.nih.gov/pubmed/15173114?dopt=Abstract)

### Contributors

**Project lead:**
[Paul Pavlidis, Ph.D.](http://pavlab.msl.ubc.ca/paul-pavlidis/)

**Developers:**
Matthew Jacobson, Justin Leong, Manuel Belmadani, Stepan Tesar.

**Data curation:**
Brenna Li, James Liu, Patrick Savage, Nathan Holmes, Jenni Hantula, Nathan Eveleigh, John Choi, Artemis Lai, Cathy Kwok, Celia Siu, Luchia Tseng, Lydia Xu, Mark Lee, Olivia Marais, Roland Au, Suzanne Lane, Tianna Koreman, Willie Kwok, Yiqi Chen,
Brandown Huntington, John Phan, Jimmy Liu, Cindy-Lee Crichlow, Sophia Ly, Ellie Hogan

**System administration:**
Kevin Griffin, Stephen Macdonald, Dima Vavilov

#### Research
The following people have contributed code, algorithms, implementations of algorithms, or other computational work relating to Gemma.

- Elodie Portales-Casamar – _Phenocarta_
- Jesse Gillis – _Coexpression analysis_
- Leon French – _Ontologies and annotations_
- Meeta Mistry – _Gene Ontology metrics, differential expression_
- Raymond Lim – _Differential expression meta-analysis_
- Vaneet Lotay – _Coexpression algorithm testing_
- Xiang Wan – _Coexpression analysis_

#### Alumni
- Anton Zoubarev – Lead programmer (2010 – 2014)
- Cameron McDonald – Programmer (2011 – 2014)
- Frances Lui – Programmer (2011 – 2013)
- Nicolas St-Georges – Programmer (2011 – 2014)
- Gavin Ha – Undergraduate research assistant – web services (2008)
- Joseph “JR” Santos – Programmer (2006-2007)
- Kelsey Hamer – Lead programmer (2006-2010)
- Kiran Keshav – Developer (2005) and consultant
- Louise Donnison – Developer (2009-2010)
- Luke Mccarthy – Programmer (2007- Feb 2008)
- Thea Van Rossum – Programmer (2011 – 2012)
- Celia Siu – Systems
- Hugh Brown – Systems

Other contributers to early stages of Gemma include David Quigley, Anshu Sinha and Gozde Cozen. Gemma’s precursor was [TMM](https://home.pavlab.msl.ubc.ca/tmm/), which was developed by Homin Lee, Jon Sajdak, Jie Qin and Amy Hsu. Martin Krzywinski has provided helpful advice on visualization.
___

Copyright © University of British Columbia
