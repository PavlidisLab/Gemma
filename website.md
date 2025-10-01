# Gemma website

The Gemma website provides access to curated transcriptomic datasets and differential expression analysis results. From the main page, 
you can search for a specific dataset using its GSE accession number or explore a dataset using the Gemma browser, which includes advanced search options.

## Dataset pages

Each dataset page in Gemma includes four main tabs: Overview, Experimental Design, Visualize Expression and Diagnostics. Below we describe the content 
and functionality of each.

### Overview

The Overview tab summarizes key information about the dataset:

- Species (taxon)
- Number of samples
- Number of expression vectors (genes)
- Platform(s) used
- Associated publication

It also includes dataset tags and status indicators.

**Dataset tags**

There are three types of tags shown:
- yellow tags: relate to the experimental design (i.e. factor value), automatically extracted from metadata
- blue tags: relate to sample information, automatically extracted from metadata.
- green tags: dataset level tags added by curators. These tags describe dataset features that are constant across all of the samples
- (e.g. tissue type, disease state) or relevant topics/concepts that may not appear explicitly in metadata; they ensure that searches
- for a relevant concept would retrieve the dataset.

**Dataset status**

Colour-coded badges and emoticons indicate dataset quality and processing state, such as batching information and whether data have been 
reanalyzed. Emoticons correspond to the dataset’s GEEQ score (see [Data curation](curation.md) for details) and they give an intuitive visual 
representation of both the quality and suitability scores. Hovering over the emoticon will reveal the numerical value of the score.

**Differential expression analysis**

Differential expression results are summarized using several visualization options:
- pie chart icon - hovering shows the total number of differentially expressed genes
- heatmap - shows the top differentially expressed platform elements for each factor
- P-value distribution  

A complete table of differential expression results (i.e. log2 fold change, t-statistics and P-value) can be downloaded for further inspection.

### Experimental design

The ‘Experimental Design’ tab displays the layout of the dataset’s experimental design in a tabular format:
- columns represent experimental factors 
- rows represent each combination of factor levels, with the corresponding number of samples.

This view allows you to quickly understand how the study was structured.

### Visualize expression

The ‘Visualize expression’ tab shows a heatmap of 20 randomly selected expressed genes (platform elements). Heatmap columns correspond to samples, 
while the color bars above the heatmap show the distribution of factor values for each experimental factor (including the batch, if present) across 
the samples. The heatmap can be redrawn for a different selection of genes by clicking on the ‘Visualize’ button. 

The user has options to toggle sample name labels, switch to a line plot or download the displayed data in a tab-delimited format, by clicking on 
the icons at the bottom of the page.

### Diagnostics

The ‘Diagnostics’ tab provides plots for assessing dataset quality:
- sample–sample gene expression correlation heatmap
- PCA scree plot
- PCA–Factor association plot 
- mean–variance scatterplot

Clicking on a plot opens a larger version with additional details, such as sample names and color legend.

This tab also includes information about removed outlier samples, which can be observed as ‘grayed’ out rows/columns in the sample correlation 
 heatmap.

## Gemma browser


The Gemma browser uses a familiar “shopping-style” interface, offering multiple ways to search for datasets. The search parameters are described 
in the left panel:
- you can perform free-text searches by typing keywords into the search box at the top of the page
- you can select dataset features from three categories:
  - taxa – human, mouse, rat
  - platform – RNA-seq, microarray
  - annotations - ontology-based concepts describing experimental features

Behind the scenes, your selections are translated into API queries that retrieve the most relevant matches. The results are displayed in a table 
on the right, which lists the identified experiments along with key information for each. 


