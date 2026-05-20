package ubic.gemma.rest.util.args;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;
import ubic.gemma.core.analysis.preprocess.OutlierDetails;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.search.*;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayUtils;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.DesignPreflightReport;
import ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.IdentifiableUtils;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.MalformedArgException;

import org.springframework.lang.Nullable;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServiceUnavailableException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DatasetArgService extends AbstractEntityArgService<ExpressionExperiment, ExpressionExperimentService> {

    private final SearchService searchService;
    private final ArrayDesignService adService;
    private final BioAssayService baService;
    private final OutlierDetectionService outlierDetectionService;

    @Autowired
    public DatasetArgService( ExpressionExperimentService service, SearchService searchService, ArrayDesignService adService, BioAssayService baService, OutlierDetectionService outlierDetectionService ) {
        super( service );
        this.searchService = searchService;
        this.adService = adService;
        this.baService = baService;
        this.outlierDetectionService = outlierDetectionService;
    }

    /**
     * Retrieve an ID for a given dataset argument.
     */
    @Nullable
    public Long getEntityId( DatasetArg<?> datasetArg ) {
        return datasetArg.getEntityId( service );
    }

    /**
     * Obtain a list of exclude URIs from an argument containing excluded URIs.
     *
     * @param excludedUrisArg argument containing excluded URIs or null if unspecified
     * @param excludeFreeText if true, null will be included in the returned list which will result in the exclusion of
     *                        free-text categories or terms
     * @return null if excludedUrisArg is null and excludeFreeText is false, otherwise a list of excluded URIs
     */
    @Nullable
    public List<String> getExcludedUris( @Nullable StringArrayArg excludedUrisArg, boolean excludeFreeText, boolean excludeUncategorizedTerms ) {
        List<String> result = null;
        if ( excludedUrisArg != null ) {
            result = excludedUrisArg.getValue();
        }
        if ( excludeFreeText || excludeUncategorizedTerms ) {
            if ( result == null ) {
                result = new ArrayList<>();
            } else {
                result = new ArrayList<>( result );
            }
        }
        if ( excludeFreeText ) {
            result.add( ExpressionExperimentService.FREE_TEXT );
        }
        if ( excludeUncategorizedTerms ) {
            result.add( ExpressionExperimentService.UNCATEGORIZED );
        }
        return result;
    }

    @Override
    public Filters getFilters( FilterArg<ExpressionExperiment> filterArg ) throws BadRequestException, ServiceUnavailableException {
        return getFilters( filterArg, null, null );
    }

    public Filters getFilters( FilterArg<ExpressionExperiment> filterArg, @Nullable Collection<OntologyTerm> mentionedTerms, @Nullable Collection<OntologyTerm> inferredTerms ) throws ServiceUnavailableException {
        try {
            return service.getEnhancedFilters( super.getFilters( filterArg ), mentionedTerms, inferredTerms, 30, TimeUnit.SECONDS );
        } catch ( TimeoutException e ) {
            throw new ServiceUnavailableException( "Inferring terms for the filter timed out.", DateUtils.addSeconds( new Date(), 30 ), e );
        }
    }

    public Filters getFilters( FilterArg<ExpressionExperiment> filterArg, @Nullable Collection<OntologyTerm> mentionedTerms, @Nullable Collection<OntologyTerm> inferredTerms, long timeout, TimeUnit timeUnit ) throws TimeoutException {
        return service.getEnhancedFilters( super.getFilters( filterArg ), mentionedTerms, inferredTerms, timeout, timeUnit );
    }

    /**
     * Cursor-mode counterpart to {@link ExpressionExperimentService#loadValueObjects(Filters, Sort, int, int)}.
     * Always sorts by ascending {@code id} (the primary key, indexed and unique) — see
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1d. The caller's {@code Filters} still
     * applies (so endpoints like {@code GET /taxa/{taxon}/datasets} can pre-compose the
     * {@code taxon.id = ?} constraint into the filter and pass it through). The user's
     * {@code ?sort=} arg is intentionally not honoured in cursor mode because the DAO
     * currently restricts cursors to single-component id sorts (recce sec. 3.4 — to be
     * lifted in phase B once the index audit is complete).
     */
    public CursorPage<ExpressionExperimentValueObject> getDatasetsByCursor( @Nullable Filters filters, @Nullable Cursor cursor, int limit ) {
        return service.loadValueObjectsByCursor( filters, service.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ), cursor, limit );
    }

    /**
     * Cursor-mode counterpart to {@link ExpressionExperimentService#loadBlacklistedValueObjects(Filters, Sort, int, int)}.
     * Always sorts by ascending {@code id} (the primary key, indexed and unique) — see
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1t (the EE-targeted twin of step 1h).
     * The caller's {@code Filters} still applies on top of the blacklist short-name/accession
     * predicate composed inside the DAO. The user's {@code ?sort=} arg is intentionally not
     * honoured in cursor mode because the DAO currently restricts cursors to single-component
     * id sorts (recce §3.4 — to be lifted in phase B once the index audit is complete).
     */
    public CursorPage<ExpressionExperimentValueObject> getBlacklistedDatasetsByCursor( @Nullable Filters filters, @Nullable Cursor cursor, int limit ) {
        return service.loadBlacklistedValueObjectsByCursor( filters, service.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ), cursor, limit );
    }

    /**
     * Obtain the search results for a given query and highlighter.
     *
     * @param highlighter   a highlighter to use for the query or null to ignore
     * @param queryWarnings a collection that will receive warnings regarding the full-text query
     * @throws BadRequestException          if the query is empty
     * @throws ServiceUnavailableException  if the search times out
     * @throws InternalServerErrorException for any other search-related exceptions
     */
    public List<SearchResult<ExpressionExperiment>> getResultsForSearchQuery( QueryArg query, @Nullable Highlighter highlighter, @Nullable Collection<Throwable> queryWarnings ) throws BadRequestException, ServiceUnavailableException, InternalServerErrorException {
        try {
            SearchSettings settings = SearchSettings.builder()
                    .query( query.getValue() )
                    .resultType( ExpressionExperiment.class )
                    .fillResults( false )
                    .build();
            return searchService.search( settings, new SearchContext( highlighter, queryWarnings != null ? queryWarnings::add : null ) )
                    .getByResultObjectType( ExpressionExperiment.class );
        } catch ( ParseSearchException e ) {
            throw new MalformedArgException( "Invalid search query: " + e.getQuery(), e );
        } catch ( SearchTimeoutException e ) {
            throw new ServiceUnavailableException( e.getMessage(), DateUtils.addSeconds( new Date(), 30 ), e.getCause() );
        } catch ( SearchException e ) {
            throw new InternalServerErrorException( e );
        }
    }

    /**
     * Shortcut for extracting the result IDs and scores from {@link #getResultsForSearchQuery(QueryArg, Highlighter, Collection)}.
     *
     * @see #getResultsForSearchQuery(QueryArg, Highlighter, Collection)
     */
    public Set<Long> getIdsForSearchQuery( QueryArg query, Map<Long, Double> scoreById, @Nullable Collection<Throwable> queryWarnings ) {
        List<SearchResult<ExpressionExperiment>> _results = getResultsForSearchQuery( query, null, queryWarnings );
        for ( SearchResult<ExpressionExperiment> result : _results ) {
            scoreById.put( result.getResultId(), result.getScore() );
        }
        return _results.stream().map( SearchResult::getResultId ).collect( Collectors.toSet() );
    }

    /**
     * Shortcut for extracting the result IDs from {@link #getResultsForSearchQuery(QueryArg, Highlighter, Collection)}.
     *
     * @see #getResultsForSearchQuery(QueryArg, Highlighter, Collection)
     */
    public Set<Long> getIdsForSearchQuery( QueryArg query, @Nullable Collection<Throwable> queryWarnings ) {
        return getResultsForSearchQuery( query, null, queryWarnings ).stream()
                .map( SearchResult::getResultId )
                .collect( Collectors.toSet() );
    }

    /**
     * Retrieve a dataset with quantitation type initialized.
     */
    public Set<QuantitationTypeValueObject> getQuantitationTypes( DatasetArg<?> arg ) {
        return new HashSet<>( service.getQuantitationTypeValueObjects( getEntity( arg ) ) );
    }

    /**
     * Retrieves the Platforms of the Dataset that this argument represents.
     *
     * @return a collection of Platforms that the dataset represented by this argument is in.
     */
    public List<ArrayDesignValueObject> getPlatforms( DatasetArg<?> arg ) {
        ExpressionExperiment ee = this.getEntity( arg );
        return adService.loadValueObjectsForEE( ee.getId() );
    }

    /**
     * @return a collection of BioAssays that represent the experiments samples.
     * <p>
     * Uses the narrow {@link ExpressionExperimentService#thawBioAssays(ExpressionExperiment)}
     * thaw rather than the broader {@code thawLite}: the {@code BioAssayValueObject}
     * ctor only reads the per-assay shape (array design, original platform,
     * biomaterial-with-factor-values), so warming the nine EE-level lazy
     * collections that {@code thawLite} touches (publications, otherParts,
     * factors, factor values, quantitation types, characteristics, accession,
     * mean-variance, geeq, curationDetails) is dead pre-fetch on this code
     * path. See {@code SAMPLES_DESIGN_PERF_RECCE.md} for the measurement.
     */
    public List<BioAssayValueObject> getSamples( DatasetArg<?> arg ) {
        ExpressionExperiment ee = service.thawBioAssays( this.getEntity( arg ) );
        List<BioAssayValueObject> bioAssayValueObjects = baService.loadValueObjects( ee.getBioAssays(), null, true, true );
        populateOutliers( ee, bioAssayValueObjects );
        return bioAssayValueObjects;
    }

    /**
     * Cursor-mode counterpart to {@link #getSamples(DatasetArg)} for the {@code GET
     * /datasets/{dataset}/samples} endpoint — see {@code CURSOR_PAGINATION_STEP1_PLAN.md}
     * step 1k. Walks the EE→bioAssays association directly via
     * {@link BioAssayService#loadValueObjectsByCursorForExpressionExperiment(ExpressionExperiment, Cursor, int)};
     * always sorts by ascending {@code id} (the primary key, indexed and unique). The
     * {@code thawLite} step is intentionally omitted in cursor mode because the keyset
     * HQL fetches the assays directly (it doesn't iterate {@code ee.getBioAssays()} as
     * a lazy collection). Outliers are populated post-hoc on the returned page's data,
     * matching the offset-mode VO shape.
     * <p>
     * Note: this branch is taken only when no {@code quantitationType} / {@code
     * useProcessedQuantitationType} parameter is supplied — see
     * {@link ubic.gemma.rest.DatasetsWebService#getDatasetSamples}. The QT-narrowed
     * variants intentionally remain offset-mode (they sort by
     * {@code BioAssay::getName} and apply a {@link BioAssayDimension} restriction that
     * is not expressible as an {@code id}-only cursor).
     */
    public CursorPage<BioAssayValueObject> getSamplesByCursor( DatasetArg<?> arg, @Nullable Cursor cursor, int limit ) {
        ExpressionExperiment ee = this.getEntity( arg );
        CursorPage<BioAssayValueObject> page = baService.loadValueObjectsByCursorForExpressionExperiment( ee, cursor, limit );
        // populateOutliers takes the underlying VOs in place; the CursorPage's data
        // list is what it iterates (CursorPage IS-A List<VO>).
        populateOutliers( ee, page );
        return page;
    }

    /**
     * Obtain a collection of BioAssays that represent the experiments samples for a particular quantitation type.
     */
    public List<BioAssayValueObject> getSamples( DatasetArg<?> datasetArg, QuantitationType qt ) {
        ExpressionExperiment ee = service.thawLite( getEntity( datasetArg ) );
        List<BioAssay> bad = service.getBioAssayDimensionsWithAssays( ee, qt ).stream()
                .map( BioAssayDimension::getBioAssays )
                .flatMap( Collection::stream )
                .distinct()
                .sorted( Comparator.comparing( BioAssay::getName ) )
                .collect( Collectors.toList() );
        if ( bad.isEmpty() ) {
            throw new NotFoundException( "There are no assays associated to " + qt + "." );
        }
        Map<BioAssay, BioAssay> assay2sourceAssayMap = BioAssayUtils.createBioAssayToSourceBioAssayMap( ee, bad );
        List<BioAssayValueObject> bioAssayValueObjects = baService.loadValueObjects( bad, assay2sourceAssayMap, true, true );
        populateOutliers( ee, bioAssayValueObjects );
        return bioAssayValueObjects;
    }

    /**
     * @return a collection of Annotations value objects that represent the experiments annotations.
     */
    public Set<AnnotationValueObject> getAnnotations( DatasetArg<?> arg ) {
        ExpressionExperiment ee = this.getEntity( arg );
        return service.getAnnotations( ee );
    }

    /**
     * @return the full structured experimental design (factors, factor values with statements, and biomaterial
     *         to factor-value assignments).
     */
    public ExperimentalDesignValueObject getExperimentalDesign( DatasetArg<?> arg ) {
        ExpressionExperiment ee = this.getEntity( arg );
        ExperimentalDesignValueObject vo = service.getExperimentalDesignValueObject( ee );
        if ( vo == null ) {
            throw new NotFoundException( ee.getShortName() + " does not have an experimental design." );
        }
        return vo;
    }

    /**
     * Run a dry-run preflight for the proposed design replacement.
     */
    public DesignPreflightReport previewDesignChange( DatasetArg<?> arg, ExperimentalDesignValueObject proposed ) {
        if ( proposed == null ) {
            throw new BadRequestException( "A proposed design must be supplied in the request body." );
        }
        ExpressionExperiment ee = this.getEntity( arg );
        return service.previewDesignChange( ee, proposed );
    }

    /**
     * Result of an applyDesignChange call. Exactly one of {@link #updated} or {@link #blockingReport} is non-null.
     * {@code blockingReport} is set when the proposed payload is rejected (blockers or unauthorised cascade); the
     * caller is expected to translate it to 400 / 409 as appropriate.
     */
    public static final class DesignChangeResult {
        @Nullable
        public final ExperimentalDesignValueObject updated;
        @Nullable
        public final DesignPreflightReport blockingReport;
        public final boolean forceRequired;

        private DesignChangeResult( @Nullable ExperimentalDesignValueObject updated,
                @Nullable DesignPreflightReport blockingReport, boolean forceRequired ) {
            this.updated = updated;
            this.blockingReport = blockingReport;
            this.forceRequired = forceRequired;
        }

        public static DesignChangeResult ok( ExperimentalDesignValueObject updated ) {
            return new DesignChangeResult( updated, null, false );
        }

        public static DesignChangeResult blocked( DesignPreflightReport report ) {
            return new DesignChangeResult( null, report, false );
        }

        public static DesignChangeResult forceRequired( DesignPreflightReport report ) {
            return new DesignChangeResult( null, report, true );
        }
    }

    /**
     * Validate and apply a proposed design replacement.
     * <p>
     * When the preflight report carries blockers, returns {@link DesignChangeResult#blocked} without
     * mutating state. When the preflight report has no blockers but predicts differential-expression analyses
     * to be deleted and {@code force} is false, returns {@link DesignChangeResult#forceRequired} (the cascade
     * needs explicit consent). Otherwise applies the change and returns {@link DesignChangeResult#ok} with the
     * fresh design VO.
     */
    public DesignChangeResult applyDesignChange( DatasetArg<?> arg, ExperimentalDesignValueObject proposed, boolean force ) {
        if ( proposed == null ) {
            throw new BadRequestException( "A proposed design must be supplied in the request body." );
        }
        ExpressionExperiment ee = this.getEntity( arg );
        DesignPreflightReport report = service.previewDesignChange( ee, proposed );
        if ( !report.getBlockers().isEmpty() ) {
            return DesignChangeResult.blocked( report );
        }
        if ( !report.getDifferentialExpressionAnalysesToDelete().isEmpty() && !force ) {
            return DesignChangeResult.forceRequired( report );
        }
        ExperimentalDesignValueObject updated = service.applyDesignChange( ee, proposed );
        return DesignChangeResult.ok( updated );
    }

    public List<ExpressionExperimentSubSet> getSubSets( DatasetArg<?> datasetArg ) {
        return service.getSubSetsWithCharacteristics( getEntity( datasetArg ) ).stream()
                .sorted( Comparator.comparing( ExpressionExperimentSubSet::getName ) )
                .collect( Collectors.toList() );
    }

    public ExpressionExperimentSubSet getSubSet( DatasetArg<?> datasetArg, Long subSetId ) {
        ExpressionExperiment ee = getEntity( datasetArg );
        ExpressionExperimentSubSet subset = service.getSubSetByIdWithCharacteristics( ee, subSetId );
        if ( subset == null ) {
            throw new NotFoundException( "No subset found with ID " + subSetId );
        }
        return subset;
    }

    public List<Long> getSubSetGroupIds( DatasetArg<?> datasetArg, ExpressionExperimentSubSet subset ) {
        // TODO: only retrieve the subset groups for the given subset
        return getSubSetsGroupIds( datasetArg ).getOrDefault( subset, Collections.emptyList() );
    }

    public Map<ExpressionExperimentSubSet, List<Long>> getSubSetsGroupIds( DatasetArg<?> datasetArg ) {
        Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> ss2bad = service.getSubSetsByDimension( getEntity( datasetArg ) );
        Map<ExpressionExperimentSubSet, List<Long>> subSetGroups = new HashMap<>();
        for ( Map.Entry<BioAssayDimension, Set<ExpressionExperimentSubSet>> entry : ss2bad.entrySet() ) {
            for ( ExpressionExperimentSubSet s : entry.getValue() ) {
                subSetGroups.computeIfAbsent( s, k -> new ArrayList<>() )
                        .add( entry.getKey().getId() );
            }
        }
        subSetGroups.values().forEach( list -> list.sort( Comparator.naturalOrder() ) );
        return subSetGroups;
    }

    public List<BioAssayValueObject> getSubSetSamples( DatasetArg<?> datasetArg, Long subSetId ) {
        ExpressionExperiment ee = getEntity( datasetArg );
        ExpressionExperimentSubSet subset = service.getSubSetByIdWithCharacteristicsAndBioAssays( ee, subSetId );
        if ( subset == null ) {
            throw new NotFoundException( "No subset found with ID " + subSetId );
        }
        Map<BioAssay, BioAssay> assay2sourceAssayMap = BioAssayUtils.createBioAssayToSourceBioAssayMap( subset.getSourceExperiment(), subset.getBioAssays() );
        List<BioAssayValueObject> bioAssayValueObjects = baService.loadValueObjects( subset.getBioAssays(), assay2sourceAssayMap, true, true );
        populateOutliers( subset.getSourceExperiment(), bioAssayValueObjects );
        return bioAssayValueObjects;
    }

    /**
     * Cursor-mode counterpart to {@link #getSubSetSamples(DatasetArg, Long)} for the
     * {@code GET /datasets/{dataset}/subSets/{subSet}/samples} endpoint — see
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1u (the subset-scoped twin of step 1k
     * for {@code GET /datasets/{dataset}/samples}). Walks the
     * {@code ExpressionExperimentSubSet→bioAssays} association directly via
     * {@link BioAssayService#loadValueObjectsByCursorForSubSet(ExpressionExperimentSubSet, Cursor, int)};
     * always sorts by ascending {@code id} (primary key, indexed and unique).
     * <p>
     * The assay→source-assay mapping (used to populate the VO's {@code sourceBioAssayId})
     * is built post-hoc against the subset's source experiment so the VO shape matches
     * offset mode exactly. Outliers are also populated post-hoc on the returned page.
     * <p>
     * Subset existence is validated up-front (mirroring the offset variant's
     * {@link NotFoundException} on unknown {@code subSetId}); the
     * {@code getSubSetByIdWithCharacteristicsAndBioAssays} loader is reused intentionally
     * — it returns the subset entity with its source experiment populated, which the
     * source-assay map and outlier helpers both need, without forcing the full
     * {@code subset.bioAssays} collection to materialise (Hibernate lazy-loads on access).
     */
    public CursorPage<BioAssayValueObject> getSubSetSamplesByCursor( DatasetArg<?> datasetArg, Long subSetId, @Nullable Cursor cursor, int limit ) {
        ExpressionExperiment ee = getEntity( datasetArg );
        ExpressionExperimentSubSet subset = service.getSubSetByIdWithCharacteristicsAndBioAssays( ee, subSetId );
        if ( subset == null ) {
            throw new NotFoundException( "No subset found with ID " + subSetId );
        }
        CursorPage<BioAssayValueObject> rawPage = baService.loadValueObjectsByCursorForSubSet( subset, cursor, limit );
        // The DAO returns VOs without sourceBioAssayId populated (the source-assay map needs
        // the source experiment's bioAssays as a filter set, which only the service layer has).
        // Build the source-assay map against subset.getSourceExperiment() and rewrite the
        // VOs so the shape matches the offset mode caller (sourceBioAssayId /
        // sourceBioAssayShortName populated). createBioAssayToSourceBioAssayMap uses
        // subset.getBioAssays() as the *filter* (not iteration), so it is bounded to the
        // subset's assay set (not the page's, intentionally — a subset's assay set is
        // already finite by construction).
        CursorPage<BioAssayValueObject> page;
        if ( !rawPage.isEmpty() ) {
            Map<Long, BioAssay> byId = new HashMap<>();
            for ( BioAssay ba : subset.getBioAssays() ) {
                byId.put( ba.getId(), ba );
            }
            List<BioAssay> pageAssays = new ArrayList<>( rawPage.size() );
            for ( BioAssayValueObject vo : rawPage ) {
                BioAssay ba = byId.get( vo.getId() );
                if ( ba != null ) {
                    pageAssays.add( ba );
                }
            }
            Map<BioAssay, BioAssay> assay2sourceAssayMap = BioAssayUtils.createBioAssayToSourceBioAssayMap( subset.getSourceExperiment(), pageAssays );
            // Use CursorPage#map to project the VOs while preserving cursor tokens/limit/sort.
            page = rawPage.map( vo -> {
                BioAssay ba = byId.get( vo.getId() );
                if ( ba == null ) {
                    return vo;
                }
                BioAssay src = assay2sourceAssayMap.get( ba );
                return new BioAssayValueObject( ba, null, src, true, true );
            } );
        } else {
            page = rawPage;
        }
        populateOutliers( subset.getSourceExperiment(), page );
        return page;
    }

    public QuantitationType getPreferredQuantitationType( DatasetArg<?> datasetArg ) {
        return service.getPreferredQuantitationType( getEntity( datasetArg ) )
                .orElseThrow( () -> new NotFoundException( "No preferred quantitation type found for dataset with ID " + datasetArg + "." ) );
    }

    public List<BibliographicReferenceValueObject> getPublications( DatasetArg<?> datasetArg ) {
        Long eeId = getEntityId( datasetArg );
        if ( eeId == null ) {
            throw new NotFoundException( "Dataset " + datasetArg + " does not exist." );
        }
        ExpressionExperiment ee = service.loadWithPrimaryPublicationAndOtherRelevantPublications( eeId );
        if ( ee == null ) {
            throw new NotFoundException( "Dataset " + datasetArg + " does not exist." );
        }
        BibliographicReference prim_ref = ee.getPrimaryPublication();
        Set<BibliographicReference> other_refs = ee.getOtherRelevantPublications();
        List<BibliographicReferenceValueObject> out = new ArrayList<>();
        if ( prim_ref != null ) {
            out.add( new BibliographicReferenceValueObject( prim_ref ) );
        }
        for ( BibliographicReference ref : other_refs ) {
            if ( prim_ref != null && Objects.equals( ref.getId(), prim_ref.getId() ) ) {
                continue;
            }
            out.add( new BibliographicReferenceValueObject( ref ) );
        }

        out.sort( Comparator.comparing( IdentifiableUtils::getRequiredId ) );

        return out;
    }

    public void populateOutliers( ExpressionExperiment ee, Collection<BioAssayValueObject> bioAssayValueObjects ) {
        outlierDetectionService.getOutlierDetails( ee ).ifPresent( outliers -> {
            Set<Long> predictedOutlierBioAssayIds = outliers.stream()
                    .map( OutlierDetails::getBioAssayId )
                    .collect( Collectors.toSet() );
            for ( BioAssayValueObject vo : bioAssayValueObjects ) {
                vo.setPredictedOutlier( predictedOutlierBioAssayIds.contains( vo.getId() ) );
            }
        } );
    }
}
