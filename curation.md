# Data Curation

## Table of Contents
- [Data Curation](#data-curation)
  * [Workflow](#workflow)
  * [Data Sources](#data-sources)
  * [Sequence analysis and annotation](#sequence-analysis-and-annotation)
  * [Data Annotation](#data-annotation)
  * [Quality control](#quality-control)

## Workflow

<img src="/assets/img/gemma-workflow.jpg" height="400" width="auto" alt="Gemma workflow"/>

The figure above outlines the key steps taken to gather data and prepare it for use in Gemma. These steps include dataset selection, platform processing, expression data processing, metadata curation and downstream analyses. Many of these steps are automated, with some human intervention and manual curation at key stages. 

When selecting which datasets to import into Gemma, we mainly focus on studies relating to the nervous system and conducted in human, mouse or rat. We de-prioritize datasets that have small sample sizes as these are less suitable for the downstream analyses implemented in Gemma. We also prioritize studies that have biological replication and a clear experimental design affording specific comparisons between contrasting conditions (e.g. disease vs control). 

##Platform Processing

Gemma supports a wide variety of microarray platforms, including Affymetrix GeneChips, Agilent spotted arrays, Illumina BeadArrays and many two-color platforms, as well as short-read RNA-seq and single-cell RNA-seq data. A key step in platform processing is linking expression data to genes, especially for microarray platforms, where the probe sequences on the arrays were often designed prior to the availability of high-quality reference genome sequences and annotations. 







The platform (describing the transcript entities that were assayed), the experimental data and the experimental design (describing the conditions the samples represent, such as “tumor” vs. “control”) are then processed. Microarray platforms are reannotated. The experimental data is checked for quality and the experimental design provided with a structured description. The data are then analyzed and the results are stored in the system for later retrieval. Some of these steps are discussed in more detail in the following sections, and additional information is available in [Lim et al. 2021](https://pubmed.ncbi.nlm.nih.gov/33599246/).

## Data Sources

Gemma imports GEO series (GSE*), and obtains annotations from the associated dataset(s) (GDS*) if available. Along with these, the ‘platform’ (array design or microarray type, GPL*) is also imported. It is common for a GEO experiment to use more than one platform. The data for the different platforms are combined in Gemma. The way this is done depends on how the experiment was designed. In some cases, the same samples were each run on more than one platform. In this case, the platforms are “stacked” so each sample’s expression vector includes the elements (probes) from all the platforms. In other cases, the samples were each run on just one platform, but the platform varied for sample to sample. For example, in some studies the Affymetrix HGU133A platform was used for some samples, and for other the Plus 2 array was used. The data for the elements in common are combined in Gemma, so the “stacking” is at the probe/sequence level.

Note that unlike the situation in GEO, a sample can only appear in one Gemma experiment (based on the “GSM” identifier). This means that some experiments imported from GEO do not have the same samples as the corresponding GEO entry, because samples that appear in experiments already in Gemma are removed at load time. For example, in GEO, ‘series’ are sometimes assembled into ‘super-series’; similarly, in GEO some samples appear in multiple overlapping studies. We also tend to use the super-series if the super-series is sufficiently cohesive (in our opinion), but in other cases we import only the constituent series independently.

## Sequence analysis and annotation

**Microarray platforms**: Each expression platform (i.e. microarray type or “array design”) was re-annotated at the sequence level using methods essentially as described in [Barnes et al. 2005](https://pubmed.ncbi.nlm.nih.gov/16237126/). Briefly, the sequences were aligned to the appropriate genome assembly using BLAT. Repeatmasker was used to mask aligned regions containing repeats. The [UCSC GoldenPath](https://genome.ucsc.edu/) genome annotation database was then used to map high-quality alignments to genes based on the “known gene” and “Refseq” tracks, with the use of additional information from other tracks. Gene information including Gene Ontology annotation was imported from NCBI’s gene database with additional data from GoldenPath. Because our methods require sequence-level analysis, platforms which lack probe level sequence information are currently not usable for analysis in Gemma. All the tools for interfacing with sequence analysis resources are provided as part of the Gemma source code distribution.

**RNA-seq data**: We reanalyze raw read data using standard alignment and quantification approaches such as STAR and RSEM. 

Information on the current versions of reference databases used in Gemma are available via the "About" menu.

## Data Annotation

In addition to the free text imported along with the data, we are manually curating the data in Gemma to provide high-quality annotations. By ‘annotation’ we refer primarily to descriptive terms applied to each data set, and to the detailed description of the experimental design. Experimental designs are available on import of data from GEO in some cases (as “GEO subsets”). In all other cases, a curator uses the Gemma administrative tools to label each sample according to factors such as tissue and treatment. Experiments themselves are ‘tagged’ with terms describing the study at a higher level. For both types of annotations, as much as is practical we use terms from Open Biomedical Ontologies. Among the benefits of using ontologies are more uniformity within Gemma and better interoperation with other resources.  These annotations are used to help locate data sets in searches, as well as in performing statistical analyses. 

Most data sets have been reviewed by at least two curators to ensure that experimental designs and other aspects of the experiment are described accurately and to help ensure uniformity and coherence of annotations across studies.

## Quality control

Although the data are presumably quality-controlled by the original publishers of the data sets prior to publication or submission to public repositories, we have undertaken our own quality control checks of the data. Therefore Gemma includes the ability to flag samples as outliers. The data from outliers are treated as missing by Gemma. Data sets with identified quality problems or other issues preventing preprocessing are retained in the system but ignored during analytical procedures.

Outlier samples are inditially identified by an automated procedure, which is followed by manual review.  

Another quality control step is looking at batch effects and conducting batch correction (using the ComBat algorithm) if appropriate. The process is largely automated.

## Experimental designs

An experimental design describes the characteristics of each sample as relevant to the analysis of the data. Our curation team is responsible for annotating each sample in Gemma, recording our interpretation of the essential features of the design for each data set. Where possible our curation reflects the intent of the experimenters as indicated in their GEO submission and/or publication.

An experimental design is organized around **Experimental Factors** (EF). A factor is a known variable in an experiment, such as "age", "genotype" or "treatment". An experiment can have any number of factors, but most have only 1-3.

Each sample has a specific **Factor Value** for each Factor. For example, for "genotype", the available values might be "wild-type" and "mutant". For a parameter like "age", the values might be continuous values (numbers) like "10.1 years".

Because experimental designs are curated manually by our team, errors can occur. If you spot a problem, please let us know.
