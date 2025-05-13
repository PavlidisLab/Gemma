# Single Cell RNA-seq Experiments

Gemma now supports single-cell data. This is still a work in progress, but the main features for compatibility are now in place and we have started to make data sets available through the Gemma web site and web services.

Here are a few details about our approach:

- We are re-annotating cell types in many data sets. This is necessitated because most authors do not provide cell type annotations. At this time, our pipeline supports neocortex and hippocampus of human and rat. More brain regions and tissues will be added based on availability of reference data (and our own resources). For details on the methods we are using, please see [our annotation pipeline repository](https://github.com/PavlidisLab/sc-annotation-pipeline).
- We are currently focusing on data sets collected with 10X Genomics platforms. We are not yet re-analyzing the data from FASTQ files. For now, this limits us to data sets where count matrices are provided by the authors. Large numbers of additional data sets will be eligible for inclusion once we have our re-analysis pipeline up and running.
- While we store cell-level data, the presentation in Gemma is primarily based on pseudo-bulk (aggregated) data for each cell type, per sample. We are conducting differential expression analysis in the same manner as for other data sets. We are still evaluating our pipeline for any adjustments that may be needed.
- Support for single-cell experiment access is implemented in the R package and coming to the Python package soon.
- We are especially keen to get feedback on the features, data and analysis results. Please contact us through pavlab-support@msl.ubc.ca with any questions or comments.