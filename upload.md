# Uploading data

Gemma users are allowed to upload their own experiments (as long as they comply with our [terms and conditions](terms.md)) so they can use the analytical tools of Gemma with their data.
To access the data upload form, you have to be registered and logged in. The go to My Gemma -> Load Data.

## File format
The data file should be a simple tab-delimited text file, with each row representing the dependent variable measurements (e.g., expression levels or ratios) for one set of observations. Each column represents an observation or sample (e.g., a single microarray).

- **Your file can be compressed.** We suggest this as a way to speed up uploads. GZIP and ZIP formats are supported.
- **The input files are tab delimited.** Comma or space-delimited files will not work. Exporting files as text from Excel is a good way to produce appropriate files.
- **Missing values are ok.** They should be indicated by blanks or “NA”.
- **Notice the ‘corner’ string in the example below.** All columns including the example names have a heading. It does not matter what you put in the corner, but it must not be blank. The parser uses the header to figure out how many features you have, so if you skip the corner string it will appear that you have extra data, resulting in an error message.
- **There can only be one column of descriptors.** All other data must be your numeric feature data. In other words, don’t include extra columns in your file that are not part of the data or the example labels. Extra columns will either result in an error (most likely) or invalid results (if your extra columns look like data).
- **The row names must match the probe ids in the platform.** If they do not match up, the data will be ignored. Gemma will issue warnings about mismatches between probe names in your data and in the platform.
- **Use sensible names for the samples.** Later, if you are [editing the experimental design](designs.md), you will want to re-use those names.

### Example data
In the table below, each row represents the expression measurements for one probe (or ‘probe set’). The columns represent different arrays which were run. The top of your data file might look like this.

```
GENE	MUTANT1	MUTANT2	MUTANT3	WILDTYPE1	WILDTYPE2	WILDTYPE3
100001_at	-36.3	77.8	64.4	89.4	126.6	86.2
100002_at	1504.2	1512	944.5	1157.9	1652	1358.9
100003_at	845.9	966.5	1057.4	987.4	764.1	878.5
100004_at	2304.4	1991.1	2783.7	1929.8	2236.8	2664.1
100005_at	3826.5	2876.9	4514.1	3187.8	2454.3	3730.6
100006_at	3635	2584.6	3554.9	2810.9	1629	2248.6
100007_at	6328.4	6197.8	7236.4	6224.9	6950	6206.8
100009_r_at	6580.6	8715.9	5280.3	6569.4	8513.4	7236
100010_at	368.2	344.5	-62.4	200	282.7	583.4
100011_at	1949.7	2511.3	1937.8	2684.1	1722.5	2101.3
100012_at	3145.6	2936.7	3358.4	4250.8	2706.4	2776
100013_at	-1098.4	-720.8	-1418.8	-886.9	-764.4	-1247.6
100014_at	1108	1197	985.4	1216.7	1328.1	1161.5
100015_at	6005	1040.6	4434.1	1069.4	864.8	2617.4
100016_at	4485.3	3236.2	4910.2	3474.6	3447.1	3493
100017_at	497.5	399.3	964.2	347.7	524.5	561.3
100018_at	540	1209.7	811.1	1880.8	317.9	587.8
100019_at	-303.5	46.4	0.9	53.4	-252.6	-346.9
100020_at	1606.3	1570.4	1996.6	3319.7	1803.4	1811.7
100021_at	1349.8	1193.5	764.7	331.5	1175	783.9
```

## Supported platforms
A list of supported platforms can be found on the [Gemma platforms page](https://gemma.msl.ubc.ca/arrays/showAllArrayDesigns.html). If your platform is not on the list, plese contact us.

## Data privacy
Be default, the uploaded dataset will belong to you and others will not be able to access it. It is possible to make it public or to share the dataset only with a group of collaborators.

Please note that even for data that are kept private, it is not allowed to upload any data containing personal information, as per our [terms and conditions](terms.md).
