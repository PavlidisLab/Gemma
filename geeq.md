# Gene Expression Experiment Quality (Geeq)

Geeq is a method of measuring the quality and suitability of a dataset.

**Quality** refers to data quality, wherein the same study could have been done twice with the same technical parameters and in one case yield bad quality data, and in another high quality data.

**Suitability** mostly refers to technical aspects which, if we were doing the study ourselves, we would have altered to make it optimal for analyses of the sort used in Gemma.

## Mechanism

The suitability and quality scores are calculated based on several factors. Different factors contribute to suitability and to quality. Each factor is evaluated separately, but some are dependent on each other. (e.g. batch effect can not be evaluated if there is not batch information, and will have a default value). The final score is an arithmetic average of the all the factors.

The scores of **datasets in curation** can change significantly, as the curators fill in some missing pieces or improve
some of the measured factors. We make the socre public for these datasets, but it should be taken into accoutn that the score is not final and not fully representative of the dataset.

### Visual representation

On the Gemma website, we use colored emoticons to give intuitive visual representation of both the quality and suitability scores. In most places, hovering over the emoticon will reveal the numerical value of the score. 

