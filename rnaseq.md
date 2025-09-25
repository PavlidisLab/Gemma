# RNA-seq processing pipeline

Our [RNA-seq processing pipeline](https://github.com/PavlidisLab/rnaseq-pipeline) downloads FASTQ files from the NCBI's [Sequence Read Archive](https://www.ncbi.nlm.nih.gov/sra), 
trims read adapters with [Cutadapt](https://cutadapt.readthedocs.io/en/stable), aligns reads using [STAR](https://pubmed.ncbi.nlm.nih.gov/23104886) and quantifies gene expression levels 
 using [RSEM](https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-12-323). The pipeline also generates alignment statistics using [FastQC](https://www.bioinformatics.babraham.ac.uk/projects/fastqc/) and [MultiQC](https://seqera.io/multiqc/). 
 
Gene-level quantification data are then loaded into Gemma’s database.
