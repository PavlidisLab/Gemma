# Phenocarta

## Table of contents

- [About](#about)
- [Data sources](#data-sources)
- [Extraction](#extraction)
- [Disease mapping from external sources to Disease Ontology (DO) terminology](#disease-mapping-from-external-sources-to-disease-ontology--do--terminology)
- [Manual curation of the literature](#manual-curation-of-the-literature)
- [References](#references)
- [Abbreviations](#abbreviations)

## About

Previously known as _Neurocarta_, Phenocarta is a knowledgebase that consolidates information on genes and phenotypes across multiple resources and allows tracking and exploring of the associations. The system enables automatic and manual curation of evidence supporting each association, as well as user-enabled entry of their own annotations. Phenotypes are recorded using controlled vocabularies such as the Disease Ontology to facilitate computational inference and linking to external data sources. The gene-to-phenotype associations are filtered by stringent criteria to focus on the annotations most likely to be relevant. Phenocarta is constantly growing and currently holds more than 40,000 lines of evidence linking over 9,000 genes to 2,000 different phenotypes. In addition to the data coming from external resources, Phenocarta includes a set of in-house manually curated annotations of neurodevelopmental disorders.

Phenocarta is a one-stop shop for researchers looking for candidate genes for any disorder of interest. In Phenocarta, they can review the evidence linking genes to phenotypes and filter out the evidence they’re not interested in. In addition, researchers can enter their own annotations from their experiments and analyze them in the context of existing public annotations.

**The phenocarta tool has its own [UI within the Gemma Website](https://gemma.msl.ubc.ca/phenotypes.html).**

**Publication:**
>[Portales-Casamar, E., et al., Phenocarta: aggregating and sharing disease-gene relations for the neurosciences. BMC Genomics. 2013 Feb 26;14(1):129.](http://www.biomedcentral.com/1471-2164/14/129/abstract)

## Data download

Phenocarta data are regularly exported as tab delimited files, which can be downloaded from [here](https://gemma.msl.ubc.ca/phenocarta/LatestEvidenceExport/).

## Data sources

Detailed statistics about used data can be found can be found at the [Phenocarta statistics page](https://gemma.msl.ubc.ca/neurocartaStatistics.html).

Additional resources have been investigated and discarted for various reasons, but mostly because they are only aggregating data from other sources. These rejected sources include GeneCards: http://www.genecards.org/
[phenomicDB](http://www.phenomicdb.de/), [Genotator](http://genotator.hms.harvard.edu/geno/), [EntrezGene](http://www.ncbi.nlm.nih.gov/gene) and [WikiGenes](http://www.wikigenes.org).

## Extraction

We have defined stringent criteria for automatic inclusion of data from external sources, with the goal of limiting the inclusion of unreliable data or information that we deem of limited utility to our target audience. In this section we provide details of procedures for each resource.

**OMIM<sup>[1]</sup>:** The OMIM data files (morbidmap.txt and mim2gene.txt) are downloaded from the OMIM FTP site. We extract unique mappings between Phenotype MIM numbers and Gene MIM numbers from morbidmap.txt and map the genes to their NCBI identifiers in mim2gene.txt.

**RGD<sup>[2]</sup>:** The RGD Gene-Disease association files (homo_genes_rdo, mus_genes_rdo, rattus_genes_rdo) are downloaded from the RGD FTP site. Annotations with the following evidence codes are ignored: ISS (redundant across species), NAS (non-traceable author’s statements are debatable), IEA (electronic annotations come from other sources (e.g. GAD) and we prefer to get these annotations directly from the source). Annotations without a PubMed reference are ignored as well.

**CTD<sup>[3]</sup>:** The CTD Gene-Disease association file (CTD_genes_diseases.tsv) is downloaded from the CTD website. We only consider curated annotations with Direct Evidence set to “marker/mechanism” or “therapeutic”, and at least one PubMed reference.

**NIH GWAS Catalog <sup>[16]</sup>:** The GWAS catalog is downloaded from NHGRI website (http://www.genome.gov/admin/gwascatalog.txt). We only consider variants that fall within a single and non-ambiguous gene.

### Disease-specific databases:

The SFARI<sup>[4]</sup> annotation files (autism-gene-dataset.csv, gene-score.csv) are downloaded form the SFARI Gene website. Each PubMed reference is imported as separate literature evidence in Phenocarta, with the option of it being defined as “negative” whenever specified in the annotation file. 

The PDGene<sup>[5]</sup>, AlzGene<sup>[6]</sup>, and MSGene<sup>[7]</sup> “Top Results” are extracted from their respective websites. All three databases assess their results for their epidemiological credibility using two methods: 
1. The HuGENet interim criteria for the cumulative assessment of genetic associations<sup>[12, 13]</sup>
2. Bayesian analyses<sup>[14, 15]</sup>. 

Only meta-analysis results with P-values <0.00001 are considered.The “Hot gene list” from ADHDgene<sup>[8]</sup> is extracted from their website. This list includes all genes that have been identified in at least five independent studies. The ALSoD<sup>[9]</sup> top 20 genes are identified through the credibility score analysis provided on their website. The genes are ranked by number of affected patients and by number of mutations per gene, and the ranks are summed to determine the final rank for each gene. 

For the IDGene<sup>[10]</sup> and EpiGAD<sup>[11]</sup> databases, we wanted to extract more information than what was readily accessible through the websites. We manually reviewed the genes listed in each database and used that information as a seed for targeted PubMed searches and manual curation of relevant publications.

## Disease mapping from external sources to Disease Ontology (DO) terminology
For the disorder-specific databases we use the corresponding appropriate terms in DO (e.g. “autism spectrum disorder” for SFARI and “amyotrophic lateral sclerosis” for ALSoD). As described next, for other databases we used a combination of automatic and semi-automatic methods for mapping.

OMIM, RGD, and CTD: These three resources provide OMIM or MeSH terms that we mapped to DO terms as follows. First, we use the Xref mappings provided in the Human_DO.obo ontology file, which covers about 50% of the phenotype-gene mappings in these resources. For the remaining that use terms lacking a DO Xref, we use the NCBO Annotator Web service<sup>[5]</sup> followed by manual quality control to resolve partial matches, increasing coverage substantially. In total about 2/3 of the phenotype-gene associations present in OMIM, RGD, or CTD could be mapped to a DO term. This is due to non-disease terms that are listed in OMIM but not in DO (e.g. “Blood type”, “Ig levels”), and some disease terms missing from DO (mostly syndromic, e.g. TARP syndrome, Jawad syndrome), or missed mappings. We have notified the DO maintainers of these gaps and expect to eventually be able to import a greater fraction of these annotations into Phenocarta.

GWAS Catalog: This resource doesn’t use a controlled vocabulary for their disease/trait terminology. We rely entirely on the NCBO Annotator Web service [5] followed by manual quality control for the mappings to the Disease or Phenotype Ontologies.

## Manual curation of the literature

While the Phenocarta framework is generic, our curation team is focusing on annotations relevant to our primary research interest, neurodevelopmental disorders. In-depth annotations have been produced on the following Disease Ontology terms (including respective children terms): 
1. “Autism Spectrum Disorder” (ASD; DOID_0060041); 
2. “Cerebral Palsy” (CP; DOID_1969); 
3. “Fetal Alcohol Spectrum Disorder” (FASD; DOID_0050696); 
4. “Epilepsy” (DOID_1826); and 
5. “Intellectual disability” (DOID_1059). 

When necessary, phenotype descriptions were complemented with more descriptive Human or Mammalian Phenotype Ontology terms such as “Memory impairment” (HP_0002354), “EEG abnormality” (HP_0002353), or “decreased brain size” (MP_0000774). Curators review the literature using PubMed searches across all fields (that is, the default PubMed setting) using queries such as “epilepsy” AND “genetics”. We avoid making searches that are gene-centric, except as a secondary mechanism to find additional citations on a gene-phenotype relationship identified through initial screening. When possible, review papers are used to identify primary research papers, which are then curated as “Experimental Type Evidence”. 

The curators record details about the experiment using controlled vocabularies, categorized as (for example) “Bio Source”, “Experiment Design”, or “Developmental Stage”. The criterion for inclusion is an experimentally-supported statement linking the gene to the phenotype. The exception is genome-wide studies where the results were not yet confirmed by follow-up experiments. The curated papers involve a wide variety of experiments including both animal models and human studies. For the former, if the authors describe the animal model as a specific model for the disorder of interest, the curators associate the gene studied in the paper directly to the human disease. 

If the authors describe an endophenotype that is related to the disease, the gene is associated to the endophenotype only. In some cases, review papers are used as the source of the annotations instead of drilling down to the original research papers. In that case, it is curated as “Literature Type Evidence” with no details about the experiments. To help users navigate through the evidence, we are, when possible, associating phenotypes to genes in a species-specific way. So, for instance, if the evidence comes from an experiment done in rats, it will be linked in Phenocarta to the rat gene.

## References

1. Amberger J, Bocchini CA, Scott AF, Hamosh A. **McKusick’s Online Mendelian Inheritance in Man (OMIM)**. _Nucleic Acids Res_. 2009, **37**: D793–796.
2. Laulederkind SJF, Tutaj M, Shimoyama M, Hayman GT, Lowry TF, Nigam R, Petri V, Smith JR, Wang S-J, de Pons J, Dwinell MR, Jacob HJ. **Ontology searching and browsing at the Rat Genome Database**. _Database_ 2012, **2012**: bas016–bas016.
3. Davis AP, King BL, Mockus S, Murphy CG, Saraceni-Richards C, Rosenstein M, Wiegers T, Mattingly CJ. **The Comparative Toxicogenomics Database: update 2011**. __Nucleic Acids Res__. 2011, **39**: D1067–1072.
4. Banerjee-Basu S, Packer A. **SFARI Gene: an evolving database for the autism research community**. _Dis Model Mech_ 2010, **3**: 133–135.
5. Lill CM, Roehr JT, McQueen MB, Kavvoura FK, Bagade S, Schjeide B-MM, Schjeide LM, Meissner E, Zauft U, Allen NC, Liu T, Schilling M, Anderson KJ, Beecham G, Berg D, Biernacka JM, Brice A, DeStefano AL, Do CB, Eriksson N, Factor SA, Farrer MJ, Foroud T, Gasser T, Hamza T, Hardy JA, Heutink P, Hill-Burns EM, Klein C, Latourelle JC, Maraganore DM, Martin ER, Martinez M, Myers RH, Nalls MA, Pankratz N, Payami H, Satake W, Scott WK, Sharma M, Singleton AB, Stefansson K, Toda T, Tung JY, Vance J, Wood NW, Zabetian CP, Young P, Tanzi RE, Khoury MJ, Zipp F, Lehrach H, Ioannidis JPA, Bertram L. **Comprehensive research synopsis and systematic meta-analyses in Parkinson’s disease genetics: The PDGene database**. _PLoS Genet_. 2012, **8**: e1002548.
6. Bertram L, McQueen MB, Mullin K, Blacker D, Tanzi RE. **Systematic meta-analyses of Alzheimer disease genetic association studies: the AlzGene database**. _Nat. Genet._ 2007, **39**: 17–23.
7. Lill CM, Roehr JT, McQueen MB, Bagade S, Schjeide BM, Zipp F, Bertram L. **The MSGene Database**. Alzheimer Research Forum. Available at [http://www.msgene.org/].
8. Zhang L, Chang S, Li Z, Zhang K, Du Y, Ott J, Wang J. **ADHDgene: a genetic database for attention deficit hyperactivity disorder**. _Nucleic Acids Research_ 2011, **40**: D1003–D1009.
9. Abel O, Powell JF, Andersen PM, Al-Chalabi A: **ALSoD: A user-friendly online bioinformatics tool for amyotrophic lateral sclerosis genetics**. _Hum. Mutat_. 2012, **33**:1345–1351.
10. (http://gfuncpathdb.ucdenver.edu/iddrc/iddrc/home.php)
11. Tan NCK, Berkovic SF: **The Epilepsy Genetic Association Database (epiGAD): analysis of 165 genetic association studies, 1996-2008**. _Epilepsia_ 2010, **51**:686–689.
12. Ioannidis JPA, Boffetta P, Little J, O’Brien TR, Uitterlinden AG, Vineis P, Balding DJ, Chokkalingam A, Dolan SM, Flanders WD, Higgins JPT, McCarthy MI, McDermott DH, Page GP, Rebbeck TR, Seminara D, Khoury MJ: **Assessment of cumulative evidence on genetic associations: interim guidelines**. _Int J Epidemiol_ 2008, **37**:120–132.
13. Khoury MJ, Bertram L, Boffetta P, Butterworth AS, Chanock SJ, Dolan SM, Fortier I, Garcia-Closas M, Gwinn M, Higgins JPT, Janssens ACJW, Ostell J, Owen RP, Pagon RA, Rebbeck TR, Rothman N, Bernstein JL, Burton PR, Campbell H, Chockalingam A, Furberg H, Little J, O’Brien TR, Seminara D, Vineis P, Winn DM, Yu W, Ioannidis JPA: **Genome-wide association studies, field synopses, and the development of the knowledge base on genetic variation and human diseases**. _Am. J. Epidemiol_. 2009, **170**:269–279.
14. Ioannidis JPA: **Effect of formal statistical significance on the credibility of observational associations**. _Am. J. Epidemiol_. 2008, **168**:374–383; discussion 384–390.
15. Stephens M, Balding DJ: **Bayesian statistical methods for genetic association studies**. _Nat. Rev. Genet_. 2009, **10**:681–690.
16. Hindorff LA, Sethupathy P, Junkins HA, Ramos EM, Mehta JP, Collins FS, Manolio TA: **Potential etiologic and functional implications of genome-wide association loci for human diseases and traits**. _Proc Natl Acad Sci U S A_. 2009, **106**:9362-7.

## Abbreviations
- **OMIM** - Online Mendelian Inheritance in Man
- **RGD** - Rat Genome Database
- **CTD** - Comparative Toxicogenomics Database
- **SFARI Gene** - Simons Foundation Autism Research Initiative Gene Database
- **PDGene** - Parkinson’s Disease Gene Database
- **AlzGene** - Alzheimer’s Disease Gene Database
- **MSGene** - Multiple Sclerosis Gene Database
- **ADHDgene** - Attention Deficit Hyperactivity Disorder Gene Database
- **ISS** - Inferred from Sequence or Structural Similarity
- **NAS** - Non-traceable Author Statement
- **IEA** - Inferred from Electronic Annotation
- **PMID** - PubMed ID
- **ID** - Intellectual Disability
- **epiGAD** - Epilepsy Genetic Association Database
- **ALSoD** - Amyotrophic Lateral Sclerosis Online Genetics Database
