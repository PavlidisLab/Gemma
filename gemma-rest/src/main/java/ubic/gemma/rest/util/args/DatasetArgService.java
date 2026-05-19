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
     */
    public List<BioAssayValueObject> getSamples( DatasetArg<?> arg ) {
        ExpressionExperiment ee = service.thawLite( this.getEntity( arg ) );
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
