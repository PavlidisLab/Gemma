## Updates

### Update 2.9.2

Subject, predicates and objects of statements are now exposed in the `FilterArgExpressionExperiment` model. Their URIs
are expanded with ontology inference.

Consequently, the following properties have been added:

- `experimentalDesign.experimentalFactors.factorValues.characteristics.subject`,
- `experimentalDesign.experimentalFactors.factorValues.characteristics.subjectUri`,
- `experimentalDesign.experimentalFactors.factorValues.characteristics.predicate`,
- `experimentalDesign.experimentalFactors.factorValues.characteristics.predicateUri`,
- `experimentalDesign.experimentalFactors.factorValues.characteristics.object`,
- `experimentalDesign.experimentalFactors.factorValues.characteristics.objectUri`,

For the `allCharacteristics` special collection, the following properties are now available:

- `allCharacteristics.subject`,
- `allCharacteristics.subjectUri`,
- `allCharacteristics.predicate`,
- `allCharacteristics.predicateUri`,
- `allCharacteristics.object`,
- `allCharacteristics.objectUri`,

Filterable properties that are subject to ontology inference are now clearly indicated on the OpenAPI specification.

Filterable properties can now be deprecated in the specification. The `x-gemma-filterable-property` section of the
specification will contain a `deprecated` field for these.

```yaml
x-gemma-filterable-properties:
  - name: "experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri"
    type: "string[]"
    description: "will be expanded with ontology inference; use experimentalDesign.experimentalFactors.factorValues.characteristics.subjectUri instead; alias for experimentalDesign.experimentalFactors.factorValues.characteristics.subjectUri"
    deprecated: true
```

The following properties are now deprecated:

- `experimentalDesign.experimentalFactors.factorValues.characteristics.value`,
  `experimentalDesign.experimentalFactors.factorValues.characteristics.subject` should be used instead
- `experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri`,
  `experimentalDesign.experimentalFactors.factorValues.characteristics.subjectUri` should be used instead

Note that the `allCharacteristics` will retain its `value` and `valueUri` properties as this collection still holds
regular characteristics.

### Update 2.9.1

Add a `protocol` parameter to `getDatasetsCellTypeAssignment` endpoint to locate a cell type assignment by the name of
its protocol. The list of available protocols is not publicly available, but some values can be discovered by looking up
the response of the API endpoint. The most common protocol is `author-submitted` which allow one to retrieve
author-submitted cell types.

The response of `getAnnotationsParents` and `getAnnotationsChildren` are now nested in a `data` object. This occurred
in the 1.32.1 release of Gemma, but the REST API did not have an update reflecting this breaking change.

### Update 2.9.0

#### Single-cell data in Gemma

The following endpoints were added to access single-cell data:

- `/datasets/{datasetId}/singleCellDimension`
- `/datasets/{datasetId}/cellTypeAssignment`
- `/datasets/{datasetId}/cellLevelCharacteristics`
- `/datasets/{datasetId}/data/singleCell`

The `singleCellDimension` endpoint returns useful high-level metadata about the single-cell data such as cell IDs, cell
type assignments, etc.

Two formats are exposed: tabular or MEX. If MEX is picked, the `Accept` header should be set to
`application/vnd.10xgenomics.mex`. The REST API will produce a TAR archive with MEX files organized by samples.

#### Subset and subset groups

The REST API now expose subset structure. This used to be hidden as it was only used for subset DE analysis. However,
with the arrival of single-cell data in Gemma that uses this feature to organize assays, it makes sense to expose it.

Subsets can be grouped together in a subset group which is attached to a set of vectors.

- `/datasets/{datasetId}/subSets`
- `/datasets/{datasetId}/subSets/{subSetId}`
- `/datasets/{datasetId}/subSets/{subSetId}/samples`
- `/datasets/{datasetId}/subSetGroups`
- `/datasets/{datasetId}/subSetGroups/{subSetGroupId}`

### Update 2.8.6

Add `quantitationType` and `useProcessedQuantitationType` to `getDatasetSamples` endpoint. This allows one to request
samples matching the expression data. For now, this is not really useful, but in the 2.9.0 release, some datasets will
have expression data at the sub-assay level.

### Update 2.8.5

Use 200 as a default response code.

### Update 2.8.4

- when ordering by a column, null values are always displayed last

### Update 2.8.3

#### Ordering full-text results

Datasets can now be filtered by a full-text query and sorted by another field (i.e. `lastUpdated`). If a `query` is
passed to `getDatasets`, a field named `searchResult.score` becomes available for sorting.

#### Warnings in payloads

Add an optional `warnings` collection in a response. This is meant to provide feedback that would not usually result in
a 400 error. One example is for invalid query syntax.

The structure of `error` has been adjusted to comply with the Google JSON style-guide. In particular, `error.errors` is
now a list or structured error objects instead of a map.

### Update 2.8.2

- add a `characteristics` collection to the `ExpressionExperimentValueObject` model
- add support for `any`, `all` and `none` quantifiers in filters
- add `experimentalFactorType` to `FactorValueValueObject` and `FactorValueBasicValueObject`

#### Quantifiers in filters

It is now possible to apply a quantifier when filtering by a collection of entities. For example, one can filter
datasets with "disease" characteristics by using the following filter: `none(characteristics.category = disease)`. The
default quantifier is `any`.

### Update 2.8.1

- add `factorValueId` and `secondFactorValueId` in `ContrastResultValueObject`. Those are populated in `getResultSet`
  when the query parameter `includeFactorValuesInContrasts` is set to `true` to make the payload slimmer since the
  factors can be looked up by ID in `experimentalFactors`. It will default to `false` in the 2.9.0.
- add `taxonId` in `GeneValueObject` and a `includeTaxonInGenes` query parameter to `getResultSet`. When set to `true`,
  taxa information will be omitted from individual genes; a `taxonId` field will be populated and a `taxa` collection
  in `DifferentialExpressionAnalysisResultSetValueObject` will be available. It will default to `false` in the 2.9.0.
- don't render the details of the experimental factor in `FactorValueValueObject` when it is rendered in the context of
  an `ExperimentalFactorValueObject`
- rename `bioAssaySetId` to `experimentAnalyzedId` and `sourceExperiment` to `sourceExperimentId` in
  `DifferentialExpressionAnalysisValueObject`, previous names are deprecated
- add `subsetFactorValueId` in `DifferentialExpressionAnalysisValueObject` and populate it when`subsetFactorValue`
  is not initialized
- omit `factorValuesUsedByExperimentalFactorId` in `DifferentialExpressionAnalysisValueObject` when not set
- omit `charId` in `FactorValueValueObject`
- omit `accessions`, `aliases` and `multifunctionalityRank` in `GeneValueObject` when not set
- add `taxonId` in `GeneValueObject` and use it in `getResultSet` to avoid producing a full taxon for every single
  result, taxa are made available in a `taxa` field in `DifferentialExpressionAnalysisResultSetValueObject`
- populate `baselineGroup` and `secondBaselineGroup` in `getResultSet` for interaction and continuous factors

#### Future breaking changes for 2.9.0

- `subsetFactorId` and `subsetFactor` in `DifferentialExpressionAnalysisValueObject` will become mutually exclusives.
- `includeFactorValuesInContrasts` will default to `false` in `getResultSet` and as a result, `factorValue`
  and `secondFactorValue` will be omitted from `ContrastResultValueObject` in `getResultSet`.
- `includeTaxonInGenes` will default to `false` in `getResultSet` and as a result, `taxon` will no longer be populated
  in genes

Discussion related to these changes are available in https://github.com/PavlidisLab/Gemma/issues/1198.

### Update 2.8.0

- add `getAnnotationsParents` and `getAnnotationsChildren` endpoint to perform the exact same ontology inference we use
  for searching datasets
- add `getDatasetsDifferentialExpressionAnalysisResultsForGene`
  and `getDatasetsDifferentialExpressionAnalysisResultsForGeneInTaxon` endpoints for retrieving differential expression
  analysis results for a given gene across all datasets matching typical filters and full-text query
- improve descriptions of various parameters that accept identifiers
- allow a quantitation type name to be used when retrieving raw vectors

### Update 2.7.6

- add build information to the root and to individual error responses

### Update 2.7.5

- fix a bug in `getTaxonDatasets` sorting parameter, it was indicating `Taxon` instead of `ExpressionExperiment`
- disambiguate all endpoints that expect a gene identifier with a taxon argument, the previous endpoints still exist but
  will now raise `400 Bad Request` when an ambiguous identifier is supplied instead of returning an arbitrary result
- add endpoints to retrieve all genes with pagination
- merge `getResultSets` and `getResultSetsAsTsv` endpoints in the OpenAPI specification
- add support for offset/limit and threshold arguments for retrieving DE results and retaining most significant probes

### Update 2.7.4

- indicate 503 status codes for endpoints that could timeout due to a long-running search

### Update 2.7.3

- fix double-gzipping for the `getPlatformAnnotations` endpoint
- add a limit argument `getDatasetCategoriesUsageStatistics` with a default value of 200
- more parent terms now include in `getDatasetAnnotationsUsageFrequency`
- search is much more efficient and now capable of handling more advanced syntax

#### More free-text categories

We've backfilled thousands of free-text categories from GEO sample metadata which resulted in
the `getDatasetCategoriesUsageFrequency` endpoint producing far more results than usual. This is now being alleviated
by a new `limit` parameter with a default value of 200.

#### Complete inference for parent terms in `getDatasetAnnotationsUsageFrequency`

The `getDatasetAnnotationsUsageFrequency` endpoint now include parent terms that satisfy the `hasPart` relation. We've
rewritten the logic under the hood to be much more efficient and cache frequently requested terms.

#### Advanced search syntax

The search endpoint and individual query parameters now support an advanced search syntax provided by Lucene.

### Update 2.7.2

Expose statements in `FactorValueValueObject` and `FactorValueBasicValueObject`.

A statement is constituted of three part: a subject, a predicate and an object. All those entities are identified by
using IDs from the Gemma Factor Value Ontology (TGFVO). Those IDs are not stable as they depend on the numerical ID
assigned to the factor value and the order in which an entity appears in the characteristics and statements.

Entities that are not involved in any statement are retained in the `characteristics` collection. Those entities are
also being assigned an ID from TGFVO.

```yaml
id: 1 # ID of the factor value
ontologyId: http://gemma.msl.ubc.ca/ont/TGFVO/1 # an TGFVO ID for the factor value
experimentalFactorId: 1
experimentalFactorCategory:
  - id: 1
    category: genotype
    categoryUri: null
    valueId: http://gemma.msl.ubc.ca/ont/TGFVO/1/3 # an TGFVO ID for the value
    value:
    valueUri:
value:   # deprecated
summary: # a summary of the factor value (those have been significantly improved!)
characteristics:
  - id: 1
    category: genotype
    categoryUri: null
    valueId: http://gemma.msl.ubc.ca/ont/TGFVO/1/3 # an TGFVO ID for the value
    value:
    valueUri:
statements:
  - category: genotype
    categoryUri: null
    subjectId: http://gemma.msl.ubc.ca/ont/TGFVO/1/1 # an TFGVO ID for the subject
    subject: PAX6
    subjectUri: null
    predicate: has modifier
    predicateUri: null #
    objectId: http://gemma.msl.ubc.ca/ont/TGFVO/1/2 # an TGFVO ID for the object
    object: over-expression # a modifier term
    objectUri: null # 
```

`FactorValueBasicValueObject.summary` is no longer deprecated and have been significantly improved by incorporating the
statements.

`CharacteristicValueObject` and `CharacteristicBasicValueObject` have been unified in a single class.

### Update 2.7.1

- improved highlights of search results
- fix the limit for getDatasetsAnnotations() endpoint to 5000 in the specification
- fix missing initialization of datasets retrieved from the cache

Highlights now use Markdown syntax for formatting. Fields for highlighted ontology terms now use complete object
path instead of just `term`. Last but not least, highlights from multiple results are merged.

### Update 2.7.0

New endpoints for counting the number of results: `getNumberOfDatasets`, `getNumberOfPlatforms`,
`getNumberOfResultSets`. These endpoints are faster than looking up `totalElements` as no data is retrieved or
converted.

Datasets can now be filtered by annotations at the sample, factor value and all levels using the three newly
exposed `experimentalDesign.experimentalFactors.factorValues.characteristics`, `bioAssays.sampleUsed.characteristics`
and `allCharacteristics` collections. The two useful available properties for filtering are `value` and `valueUri`.

New `getDatasetsAnnotationsUsageStatistics`, `getDatasetsPlatformsUsageStatistics` and `getDatasetsTaxaUsageStatistics`
endpoints for retrieving annotations, platforms and taxa used by the matched datasets. The endpoint accepts the
same `filter` argument of `getDatasets`, allowing one to easily navigate terms, platforms and taxa available for
filtering furthermore.

Properties available for filtering and sorting are enumerated in the description of the corresponding parameter.
There's been a number of fixes and additional tests performed to ensure that all advertised properties are working
as expected.

The `FilterArg` and `SortArg`-based parameters now have OpenAPI extensions to enumerate available properties in a
structured format under the `x-gemma-filterable-properties`key. Possible values are exposed for enumerated types.

```yaml
x-gemma-filterable-properties:
  - name: technologyType
type: string
allowedValues:
  - value: ONECOLOR
    label: One Color
  - value: SEQUENCING
    label: Sequencing
security:
  basicAuth: [GROUP_ADMIN]
  cookieAuth: [GROUP_ADMIN]
```

Some of the exposed properties such as `geeq.publicSuitabilityScore` require specific authorities to use. This is
documented in `x-filterable-properties` by specifying
a [Security Requirement Object](https://spec.openapis.org/oas/latest.html#security-requirement-object).

Types that use the `[]` suffix are using sub-queries under the hood. It implies that the entity will be matched if
at least one related entity matches the supplied filter. For example,
the `characteristics.valueUri = http://purl.obolibrary.org/obo/UBERON_0002107`
filter will match datasets with at least one `UBERON:0002107` tag attached.

Filtered endpoints (including paginated and limited ones) now expose a `groupBy` array that enumerates all the
properties used to group results. This helps clear confusion about what constitute a business key in the returned
response.

### Update 2.6.1

Add support for filtering platforms by taxon ID, common name, scientific name, etc. for the `/platforms` endpoint.

### Update 2.6.0

Add a new `externalDatabases` attribute to the main endpoint that displays version of some of the main external
databases that we are using. This exposes versions and last updates for genomes, gene annotations, GO terms, and
much more!

The `ExternalDatabaseValueObject` now exposes a `description` which provides additional details.

### Update 2.5.2

Restore `factors` in `BioMaterialValueObject` as it is being still used by our RNA-Seq pipeline. The attribute is
deprecated and should not be used and will be removed when we find a suitable alternative.

Introduce a new endpoint to retrieve quantitation types for a given dataset and parameters to retrieve expression
data by quantitation type to `getDatasetProcessedExpression` and `getDatasetRawExpression`. As it was too difficult
to extends `getDatasetExpression` to also support quantitation type while retaining the filter feature, we decided
to deprecate it and reintroduce filtering for both raw and processed expression in the future.

Fix return type for `getResultSets` which was incorrectly referring to a renamed VO.

Annotate all possible types for `SearchResult.resultObject`. This incidentally includes the `GeneSetValueObject`
in the specification which is not exposed elsewhere in the API.

### Update 2.5.1

Restore `objectClass` visibility in `AnnotationValueObject`.

Fix incorrect response types for annotations search endpoints returning datasets.

### Update 2.5.0

Major cleanups were performed in this release in order to stabilize the specification. Numerous properties from
Gemma Web that were never intended to be exposed in Gemma REST have been hidden. It's a bit too much to describe
in here, but you can navigate to the schemas section below to get a good glance at the models.

Favour `numberOfSomething` instead of `somethingCount` which is clearer. The older names are kept for
backward-compatibility, but should be considered deprecated.

Gene aliases and multifunctionality rank are now filled in `GeneValueObject`.

Uniformly use `TaxonValueObject` to represent taxon. This is breaking change for the `ExpressionExperimentValueObject`
and `ArrayDesignValueObject` as their `taxon` property will be an `object` instead of a `string`. Properties such
as `taxonId` are now deprecated and `taxon.id` should be used instead.

Entities that have IDs now all inherit from `IdentifiableValueObject`. This implies that you can assume the
presence of an `id` in a search result `resultObject` attribute for example.

New `/search` endpoint! for an unified search experience. Annotation-based search endpoints under `/annotations`
are now deprecated.

New API docs! While not as nice looking, the previous theme will be gradually ported to Swagger UI as we focused
on functionality over prettiness for this release.

### Update 2.4.0 through 2.4.1

Release notes for the 2.4 series were not written down, so I'll try to do my best to recall features that were
introduced at that time.

An [OpenAPI](https://www.openapis.org/) specification was introduced and available under `/rest/v2/openapi.json`,
although not fully stabilized.

Add a `/resultSets` endpoint to navigate result sets directly, by ID or by dataset.

Add a `/resultSets/{resultSetId}` endpoint to retrieve a specific result set by its ID. This endpoint can be
negotiated with an `Accept: text/tab-separated-values` header to obtain a TSV representation.

Add a `/datasets/{dataset}/analyses/differential/resultSets` endpoint that essentially redirect to a specific
`/resultSet` endpoint by dataset ID.

Add an endpoint to retrieve preferred raw expression vectors.

### Update 2.3.4

November 6th, 2018

November 6th [2.3.4] Bug fixes in the dataset search endpoint.

November 5th [2.3.3] Added filtering parameters to dataset search.

October 25th [2.3.2] Changed behavior of the dataset search endpoint to more closely match the Gemma web interface.

October 2nd [2.3.1] Added group information to the User value object.

September 27th [2.3.0] Breaking change in Taxa: Abbreviation property has been removed and is therefore no longer
an accepted identifier.

### Update 2.2.6

June 7th, 2018

Code maintenance, bug fixes. Geeq scores stable and made public.

June 7th [2.2.6] Added: User authentication endpoint.

May 2nd [2.2.5] Fixed: Cleaned up and optimized platforms/elements endpoint, removed redundant information
(recursive properties nesting).

April 12th [2.2.3] Fixed: Array arguments not handling non-string properties properly, e.g. `ncbiIds` of genes.

April 9th [2.2.1] Fixed: Filter argument not working when the filtered field was a primitive type. This most
significantly allows filtering by geeq boolean and double properties.

### Update 2.2.0

February 8th, 2018

Breaking change in the 'Dataset differential analysis' endpoint:

- No longer using `qValueThreshold` parameter.
- Response format changed, now using `DifferentialExpressionAnalysisValueObject` instead
  of `DifferentialExpressionValueObject`
- [Experimental] Added Geeq (Gene Expression Experiment Quality) scores to the dataset value objects