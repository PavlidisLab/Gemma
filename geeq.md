# Gene Expression Experiment Quality (Geeq)

Geeq is a method of measuring the quality and suitability of a dataset.

**Quality** refers to data quality, wherein the same study could have been done twice with the same technical parameters and in one case yield bad quality data, and in another high quality data.

**Suitability** mostly refers to technical aspects which, if we were doing the study ourselves, we would have altered to make it optimal for analyses of the sort used in Gemma.

## Mechanism

The suitability and quality scores are calculated based on several factors. Different factors contribute to suitability and to quality. Each factor is evaluated separately, but some are dependent on each other. (e.g. batch effect can not be evaluated if there is not batch information, and will have a default value). The final score is an arithmetic average of the all the factors.

The factor scores can have any value from the [-1, 1] interval, where -1 is the worst possible score, and +1 is
the best possible score. Because of the various dependencies between the factors, it
is very unlikely that the final (sutability or quality) score will ever be equal to -1.

The scores of **datasets in curation** can change significantly, as the curators fill in some missing pieces or improve
some of the measured factors. We make the socre public for these datasets, but it should be taken into accoutn that the score is not final and not fully representative of the dataset.

### Visual representation

On the Gemma website, we use colored emoticons to give intuitive visual representation of both the quality and suitability scores. In mostr places, hovering over the emoticon will reveal the numerical value of the score. 

The color of the emoticon will change from red, to yellow, to green based on where on the [-1, 1] interval the score is. 
If the score is higher than 0.3, the icon will be smiling a face 🙂, if lower than -0.3, the icon will be a frowing face 🙁 and scores in between will show a neutral face 😐.

## Suitability Score factors
### Publication
Checks whether the experiment has a publication.
- -1.0 if experiment has no publication
- +1.0 otherwise

### Amount of platforms
Checks the amount of platforms the experiment uses.
- -1.0 if experiment uses more than 2 platforms
- -0.5 if experiment uses more than 1 platform
- +1.0 otherwise

### Platform technology
Extra punishment for platform technology inconsistency
- -1.0 if experiment uses more than 1 platform and all platforms do not have the same technology type
- +1.0 otherwise

### Platform popularity
Scores each platforms according to its popularity. If the experiment has multiple platforms, then the final score is the arithmetic value.
- -1.0 if platform is used in less than 10 EEs
- -0.5 if platform is used in less than 20 EEs
- +0.0 if platform is used in less than 50 EEs
- +0.5 if platform is used in less than 100 EEs
- +1.0 otherwise

### Platform size
Scores each platform according to its size. If the experiment has multiple platforms, then the final score is the arithmetic mean.
- -1.0 if gene count of the platform is less than 5k
- -0.5 if gene count of the platform is less than 10k
- +0.0 if gene count of the platform is less than 15k
- +0.5 if gene count of the platform is less than 18k
- +1.0 otherwise

### Experiment size
Scores the experiment based on the amount of its samples.
- -1.0 if experiment has less than 20 samples
- -0.5 if experiment has less than 50 samples
- +0.0 if experiment has less than 100 samples
- +0.5 if experiment has less than 200 samples
- +1.0 otherwise

### Raw data availability
Check whether the raw data is available for the experiment. This fact also shows up as the 'external' badge on the dataset details page.
- -1.0 if no raw data available
- +1.0 otherwise

### Missing values
Checks whether the experiment has any missing values. It is assumed that it does not have any missing values, if the raw data is available.
- -1.0 if there are no raw data available and the experiment has any missing values, or there are no computed vectors
- +1.0 otherwise

### Outliers
Checks the amount of detected outliers. The removed outliers are not counted. The score is calculated based on the ratio of the amount of outliers to the total sample size.
- -1.0 if there are any outliers
- +1.0 if there are no outliers

## Quality Score factors
### Platform technlogy
Checks the technology of all used platforms
- -1.0 if any platform is two-color
- +1.0 otherwise

### Minimum amount of replicates
The experiment has to have a design and at least two conditions. Factor values from first two factors are loaded, and all factor value combinations are considered - the score is then calculated based on the factor value combination that has the lowest amount of replicates. Batch factor is disregarded during this calculation, as are any factor values marked as "DE_Exclude".
- -1.0 if lowest replicate amount is < 4 but is not 1, or if there are problems
- +0.0 if lowest replicate amount is < 10 but is not 1.
- +1.0 otherwise

### Batch information availability
Checks whether the experiment does have batch information available.
- -1.0 if no batch info available
- +1.0 otherwise

### Batch effect
Checks for batch effect. This can be improved by doing batch correction. The value considered for the final quality score can be overridden using the manual* fields.
- -1.0 if batch pVal &lt; 0.0001 or (manualHasStrongBatchEffect & manualBatchEffectActive)
- +1.0 if batch pVal &gt; 0.1 or (!manualHasNoBatchEffect & manualBatchEffectActive)
- +0.0 otherwise

### Batch confound
Checks whether the data is confounded with the batches. The value considered for the final quality score can be overridden using the manual* fields.
- -1.0 if data confound detected or (manualHasBatchConfound &amp; manualBatchConfoundActive)
- +1.0 otherwise

### Median sample correlation
Calcualtes the median sample correlation r.
- +r use the computed value
- +0.0 if correlation can no be calculated.
