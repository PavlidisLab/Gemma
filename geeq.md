# Gene Expression Experiment Quality (Geeq)

Geeq is a method of measuring the quality and suitability of a dataset.

Quality refers to data quality, wherein the same study could have been done twice with the same technical parameters and in one case yield bad quality data, and in another high quality data.

Suitability mostly refers to technical aspects which, if we were doing the study ourselves, we would have altered to make it optimal for analyses of the sort used in Gemma.

Suitability Score factors
Publication
Checks whether the experiment has a publication.

-1.0 if experiment has no publication
+1.0 otherwise
Amount of platforms
Checks the amount of platforms the experiment uses.

-1.0 if experiment uses more than 2 platforms
-0.5 if experiment uses more than 1 platform
+1.0 otherwise
Platform technology
Extra punishment for platform technology inconsistency

-1.0 if experiment uses more than 1 platform and all platforms do not have the same technology type
+1.0 otherwise
Platform popularity
Scores each platforms according to its popularity. If the experiment has multiple platforms, then the final score is the arithmetic value.

-1.0 if platform is used in less than 10 EEs
-0.5 if platform is used in less than 20 EEs
+0.0 if platform is used in less than 50 EEs
+0.5 if platform is used in less than 100 EEs
+1.0 otherwise
Platform size
Scores each platform according to its size. If the experiment has multiple platforms, then the final score is the arithmetic mean.

-1.0 if gene count of the platform is less than 5k
-0.5 if gene count of the platform is less than 10k
+0.0 if gene count of the platform is less than 15k
+0.5 if gene count of the platform is less than 18k
+1.0 otherwise
Experiment size
Scores the experiment based on the amount of its samples.

-1.0 if experiment has less than 20 samples
-0.5 if experiment has less than 50 samples
+0.0 if experiment has less than 100 samples
+0.5 if experiment has less than 200 samples
+1.0 otherwise
Raw data availability
Check whether the raw data is available for the experiment. This fact also shows up as the 'external' badge on the dataset details page.

-1.0 if no raw data available
+1.0 otherwise
Missing values
Checks whether the experiment has any missing values. It is assumed that it does not have any missing values, if the raw data is available.

-1.0 if there are no raw data available and the experiment has any missing values, or there are no computed vectors
+1.0 otherwise
Outliers
Checks the amount of detected outliers. The removed outliers are not counted. The score is calculated based on the ratio of the amount of outliers to the total sample size.

-1.0 if ratio > 5% or if the scoring fails
-0.5 if ratio > 2%
+0.0 if ratio > 0.1%
+0.5 if ratio > 0%
+1.0 if ratio = 0%
When this factor is being scored, there can be several problems that cause the scoring to fail. This information is stored in the corrMatIssues field, and can have the following values:

1 - if the correlation matrix is empty
2 - if the correlation matrix has NaN values
 

Quality Score factors
Platform technlogy
Checks the technology of all used platforms

-1.0 if any platform is two-color
+1.0 otherwise
Minimum amount of replicates
The experiment has to have adesign and at least two conditions. Factor values from first two factors are loaded, and all factor value combinations are considered - the score is then calculated based on the factor value combination that has the lowest amount of replicates.

-1.0 if lowest replicate amount is < 4 but is not 1, or if there are problems
+0.0 if lowest replicate amount is < 10 but is not 1.
+1.0 otherwise

When this factor is being scored, there can be several problems that cause the scoring to fail. This information is stored in the replicatesIssues field, and can have the following values:

1 - if the experiment has no design
2 - if there were no factor values found
3 - if all replicate amounts were 1
4 - if lowest replicate was 0 (that really should not happen though)
Batch information availability
Checks whether the experiment does have batch information available.

-1.0 if no batch info available
+1.0 otherwise
Batch effect
Checks for batch effect. This can be improved by doing batch correction. The value considered for the final quality score can be overridden using the manual* fields.

-1.0 if batch pVal &lt; 0.0001 or (manualHasStrongBatchEffect & manualBatchEffectActive)
+1.0 if batch pVal &gt; 0.1 or (!manualHasNoBatchEffect & manualBatchEffectActive)
+0.0 otherwise
While this factor is being scored, the information about whether the experiment was batch corrected is also detected and stored in the batchCorrected field.

Batch confound
Checks whether the data is confounded with the batches. The value considered for the final quality score can be overridden using the manual* fields.

-1.0 if data confound detected or (manualHasBatchConfound &amp; manualBatchConfoundActive)
+1.0 otherwise
Mean and median sample correlation, and sample correlation variance

Calcualtes the mean and median sample correlation, and the sample correlation variance. The values are then presented as separate factors.

+r use the computed value
+0.0 if correlation matrix is empty
(warning) Only the median correlation is included in the total quality score.
