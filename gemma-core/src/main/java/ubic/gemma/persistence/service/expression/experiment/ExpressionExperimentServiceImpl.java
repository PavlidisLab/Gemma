/*
 * The Gemma project.
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service.expression.experiment;

import gemma.gsec.SecurityService;
import lombok.Value;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.model.OntologyTermSimple;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchConfound;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchConfoundUtils;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.core.util.ListUtils.validateSparseRangeArray;
import static ubic.gemma.persistence.service.SubqueryUtils.guessAliases;

/**
 * @author pavlidis
 * @author keshav
 * @see ExpressionExperimentService
 */
@Service
public class ExpressionExperimentServiceImpl
        extends AbstractFilteringVoEnabledService<ExpressionExperiment, ExpressionExperimentValueObject>
        implements ExpressionExperimentService {

    private static final double BATCH_CONFOUND_THRESHOLD = 0.01;
    private static final double BATCH_EFFECT_THRESHOLD = 0.01;

    private final ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private AuditEventService auditEventService;
    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;
    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;
    @Autowired
    private ExperimentalFactorService experimentalFactorService;
    @Autowired
    private FactorValueService factorValueService;
    @Autowired
    private OntologyService ontologyService;
    @Autowired
    private PrincipalComponentAnalysisService principalComponentAnalysisService;
    @Autowired
    private QuantitationTypeService quantitationTypeService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private SVDService svdService;
    @Autowired
    private CoexpressionAnalysisService coexpressionAnalysisService;
    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;
    @Autowired
    private BlacklistedEntityService blacklistedEntityService;

    @Autowired
    public ExpressionExperimentServiceImpl( ExpressionExperimentDao expressionExperimentDao ) {
        super( expressionExperimentDao );
        this.expressionExperimentDao = expressionExperimentDao;
    }

    @Override
    @Transactional
    public ExperimentalFactor addFactor( ExpressionExperiment ee, ExperimentalFactor factor ) {
        ExpressionExperiment experiment = expressionExperimentDao.load( ee.getId() );
        if ( experiment == null ) {
            throw new IllegalArgumentException( "The passed EE does not exist anymore." );
        }
        factor.setExperimentalDesign( experiment.getExperimentalDesign() );
        factor.setSecurityOwner( experiment );
        factor = experimentalFactorService.create( factor ); // to make sure we get acls.
        experiment.getExperimentalDesign().getExperimentalFactors().add( factor );
        expressionExperimentDao.update( experiment );
        return factor;
    }

    @Override
    @Transactional
    public void addFactorValue( ExpressionExperiment ee, FactorValue fv ) {
        assert fv.getExperimentalFactor() != null;
        ExpressionExperiment experiment = requireNonNull( expressionExperimentDao.load( ee.getId() ) );
        fv.setSecurityOwner( experiment );
        Collection<ExperimentalFactor> efs = experiment.getExperimentalDesign().getExperimentalFactors();
        fv = this.factorValueService.create( fv );
        for ( ExperimentalFactor ef : efs ) {
            if ( fv.getExperimentalFactor().equals( ef ) ) {
                ef.getFactorValues().add( fv );
                break;
            }
        }
        expressionExperimentDao.update( experiment );

    }

    @Override
    @Transactional
    public ExpressionExperiment addRawVectors( ExpressionExperiment ee,
            Collection<RawExpressionDataVector> newVectors ) {

        // reload to ensure it is in the session
        ee = loadOrFail( ee.getId() );

        Collection<BioAssayDimension> BADs = new HashSet<>();
        Collection<QuantitationType> qts = new HashSet<>();
        for ( RawExpressionDataVector vec : newVectors ) {
            BADs.add( vec.getBioAssayDimension() );
            qts.add( vec.getQuantitationType() );
        }

        if ( BADs.size() > 1 ) {
            throw new IllegalArgumentException( "Vectors must share a common bioassay dimension" );
        }

        if ( qts.size() > 1 ) {
            throw new UnsupportedOperationException(
                    "Can only replace with one type of vector (only one quantitation type)" );
        }

        BioAssayDimension bad = BADs.iterator().next();

        if ( bad.getId() == null ) {
            bad = this.bioAssayDimensionService.findOrCreate( bad );
        }
        assert bad.getBioAssays().size() > 0;

        QuantitationType newQt = qts.iterator().next();
        if ( newQt.getId() == null ) { // we try to re-use QTs, but if not:
            newQt = this.quantitationTypeService.create( newQt );
        }

        /*
         * This is probably a more or less redundant setting, but doesn't hurt to make sure.
         */
        ArrayDesign vectorAd = newVectors.iterator().next().getDesignElement().getArrayDesign();
        for ( BioAssay ba : bad.getBioAssays() ) {
            ba.setArrayDesignUsed( vectorAd );
        }

        for ( RawExpressionDataVector vec : newVectors ) {
            vec.setBioAssayDimension( bad );
            vec.setQuantitationType( newQt );
        }

        ee.getRawExpressionDataVectors().addAll( newVectors );

        // this is a denormalization; easy to forget to update this.
        ee.getQuantitationTypes().add( newQt );

        update( ee );

        AbstractService.log.info( ee.getRawExpressionDataVectors().size() + " vectors for experiment" );

        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperiment> browse( int start, int limit ) {
        return this.expressionExperimentDao.browse( start, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkHasBatchInfo( ExpressionExperiment ee ) {
        if ( ee.getExperimentalDesign() == null ) {
            return false;
        }

        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( BatchInfoPopulationServiceImpl.isBatchFactor( ef ) ) {
                return true;
            }
        }

        AuditEvent ev = this.auditEventService.getLastEvent( ee, BatchInformationFetchingEvent.class );
        if ( ev == null ) return false;
        return ev.getEventType().getClass().isAssignableFrom( BatchInformationFetchingEvent.class )
                || ev.getEventType().getClass().isAssignableFrom( SingleBatchDeterminationEvent.class ); // 
    }

    @Override
    @Transactional(readOnly = true)
    public BatchInformationFetchingEvent checkBatchFetchStatus( ExpressionExperiment ee ) {
        if ( ee.getExperimentalDesign() == null )
            return null;

        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( BatchInfoPopulationServiceImpl.isBatchFactor( ef ) ) {
                return new BatchInformationFetchingEvent(); // signal success
            }
        }

        AuditEvent ev = this.auditEventService.getLastEvent( ee, BatchInformationFetchingEvent.class );
        if ( ev == null ) return null;
        return ( BatchInformationFetchingEvent ) ev.getEventType();

    }

    /**
     * returns ids of search results
     *
     * @return collection of ids or an empty collection
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Long> filter( String searchString ) throws SearchException {

        SearchService.SearchResultMap searchResultsMap = searchService
                .search( SearchSettings.expressionExperimentSearch( searchString ) );

        assert searchResultsMap != null;

        List<SearchResult<ExpressionExperiment>> searchResults = searchResultsMap.getByResultObjectType( ExpressionExperiment.class );

        Collection<Long> ids = new ArrayList<>( searchResults.size() );

        for ( SearchResult<ExpressionExperiment> s : searchResults ) {
            ids.add( s.getResultId() );
        }

        return ids;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> filterByTaxon( Collection<Long> ids, Taxon taxon ) {
        return this.expressionExperimentDao.filterByTaxon( ids, taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadWithPrimaryPublication( Long id ) {
        ExpressionExperiment ee = load( id );
        if ( ee != null ) {
            if ( ee.getPrimaryPublication() != null ) {
                Hibernate.initialize( ee.getPrimaryPublication() );
                Hibernate.initialize( ee.getPrimaryPublication().getMeshTerms() );
                Hibernate.initialize( ee.getPrimaryPublication().getChemicals() );
                Hibernate.initialize( ee.getPrimaryPublication().getKeywords() );
            }
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadWithMeanVarianceRelation( Long id ) {
        ExpressionExperiment ee = load( id );
        if ( ee != null ) {
            Hibernate.initialize( ee.getMeanVarianceRelation() );
        }
        return ee;
    }

    /**
     * @see ExpressionExperimentService#findByAccession(DatabaseEntry)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByAccession( final DatabaseEntry accession ) {
        return this.expressionExperimentDao.findByAccession( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByAccession( String accession ) {
        return this.expressionExperimentDao.findByAccession( accession );
    }

    /**
     * @see ExpressionExperimentService#findByBibliographicReference(BibliographicReference)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByBibliographicReference( final BibliographicReference bibRef ) {
        return this.expressionExperimentDao.findByBibliographicReference( bibRef.getId() );
    }

    /**
     * @see ExpressionExperimentService#findByBioAssay(BioAssay)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByBioAssay( final BioAssay ba ) {
        return this.expressionExperimentDao.findByBioAssay( ba );
    }

    /**
     * @see ExpressionExperimentService#findByBioMaterial(BioMaterial)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByBioMaterial( final BioMaterial bm ) {
        return this.expressionExperimentDao.findByBioMaterial( bm );
    }

    /**
     * @see ExpressionExperimentService#findByBioMaterials(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, BioMaterial> findByBioMaterials( final Collection<BioMaterial> bioMaterials ) {
        return this.expressionExperimentDao.findByBioMaterials( bioMaterials );
    }

    /**
     * @see ExpressionExperimentService#findByExpressedGene(Gene, double)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByExpressedGene( final Gene gene, final double rank ) {
        return this.expressionExperimentDao.findByExpressedGene( gene, rank );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByDesign( ExperimentalDesign ed ) {
        return this.expressionExperimentDao.findByDesign( ed );
    }

    /**
     * @see ExpressionExperimentService#findByFactor(ExperimentalFactor)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByFactor( final ExperimentalFactor factor ) {
        return this.expressionExperimentDao.findByFactor( factor );
    }

    /**
     * @see ExpressionExperimentService#findByFactorValue(FactorValue)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByFactorValue( final FactorValue factorValue ) {
        return this.expressionExperimentDao.findByFactorValue( factorValue );
    }

    /**
     * @see ExpressionExperimentService#findByFactorValue(FactorValue)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByFactorValue( final Long factorValueId ) {
        return this.expressionExperimentDao.findByFactorValue( factorValueId );
    }

    /**
     * @see ExpressionExperimentService#findByFactorValues(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, FactorValue> findByFactorValues( final Collection<FactorValue> factorValues ) {
        return this.expressionExperimentDao.findByFactorValues( factorValues );
    }

    /**
     * @see ExpressionExperimentService#findByGene(Gene)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByGene( final Gene gene ) {
        return this.expressionExperimentDao.findByGene( gene );
    }

    /**
     * @see ExpressionExperimentService#findByName(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByName( final String name ) {
        return this.expressionExperimentDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByQuantitationType( QuantitationType type ) {
        return this.expressionExperimentDao.findByQuantitationType( type );
    }

    /**
     * @see ExpressionExperimentService#findByShortName(String)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByShortName( final String shortName ) {
        return this.expressionExperimentDao.findByShortName( shortName );
    }

    /**
     * @see ExpressionExperimentService#findByTaxon(Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByTaxon( final Taxon taxon ) {
        return this.expressionExperimentDao.findByTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperiment> findByUpdatedLimit( int limit ) {
        return this.expressionExperimentDao.findByUpdatedLimit( limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findUpdatedAfter( Date date ) {
        return this.expressionExperimentDao.findUpdatedAfter( date );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getAnnotationCountsByIds( final Collection<Long> ids ) {
        return this.expressionExperimentDao.getAnnotationCounts( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Set<AnnotationValueObject> getAnnotationsById( Long eeId ) {
        ExpressionExperiment expressionExperiment = requireNonNull( this.load( eeId ) );
        Set<AnnotationValueObject> annotations = new HashSet<>();
        Collection<String> seenTerms = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );

        for ( Characteristic c : expressionExperiment.getCharacteristics() ) {
            AnnotationValueObject annotationValue = new AnnotationValueObject( c, ExpressionExperiment.class );
            if ( seenTerms.add( annotationValue.getTermName() ) ) {
                annotations.add( annotationValue );
            }
        }

        /*
         * TODO If can be done without much slowdown, add: certain characteristics from factor values? (non-baseline,
         * non-batch, non-redundant with tags). This is tricky because they are so specific...
         */
        for ( AnnotationValueObject v : this.getAnnotationsByFactorValues( eeId ) ) {
            if ( seenTerms.add( v.getTermName() ) ) {
                annotations.add( v );
            }
        }

        /*
         * TODO If can be done without much slowdown, add: certain selected (constant?) characteristics from
         * biomaterials? (non-redundant with tags)
         */
        for ( AnnotationValueObject v : this.getAnnotationsByBioMaterials( eeId ) ) {
            if ( seenTerms.add( v.getTermName() ) ) {
                annotations.add( v );
            }
        }

        return annotations;
    }

    /**
     * Only the mention of these properties will result in inferred term expansion.
     * <p>
     * Note: we do not apply inference to category URIs as they are (a) too broad and (b) their sub-terms are never used.
     */
    private static final String[] PROPERTIES_USED_FOR_ANNOTATIONS = {
            "allCharacteristics.valueUri",
            "characteristics.valueUri",
            "bioAssays.sampleUsed.characteristics.valueUri",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri"
    };

    /**
     * The approach here is to construct a collection for each sub-clause in the expression that regroups all the
     * predicates that apply to characteristics as well as their inferred terms.
     * <p>
     * The transformation only applies to properties that represent {@link Characteristic} objects such as {@code characteristics},
     * {@code allCharacteristics}, {@code bioAssays.sample.characteristics} and {@code experimentalDesign.experimentalFactors.factorValues.characteristics}
     * <p>
     * Given {@code characteristics.valueUri = a}, we construct a collection clause such as
     * {@code characteristics.valueUri in (a, children of a...)}.
     * <p>
     * For efficiency, all the terms mentioned in a sub-clause are grouped by {@link SubClauseKey} and aggregated in a
     * single collection. If a term is mentioned multiple times, it is simplified as a single appearance in the
     * collection.
     * <p>
     * For example, {@code characteristics.termUri = a or characteristics.termUri = b} will be transformed into {@code characteristics.termUri in (a, b, children of a and b...)}.
     */
    @Override
    public Filters getFiltersWithInferredAnnotations( Filters f, @Nullable Collection<OntologyTerm> mentionedTerms ) {
        Filters f2 = Filters.empty();
        // apply inference to terms
        // collect clauses mentioning terms
        final Map<SubClauseKey, Set<String>> termUrisBySubClause = new HashMap<>();
        for ( List<Filter> clause : f ) {
            Filters.FiltersClauseBuilder clauseBuilder = f2.and();
            for ( Filter subClause : clause ) {
                if ( ArrayUtils.contains( PROPERTIES_USED_FOR_ANNOTATIONS, subClause.getOriginalProperty() ) ) {
                    // handle nested subqueries
                    subClause = FiltersUtils.unnestSubquery( subClause );
                    Set<String> it = termUrisBySubClause.computeIfAbsent( SubClauseKey.from( subClause.getObjectAlias(), subClause.getPropertyName(), subClause.getOriginalProperty() ), k -> new HashSet<>() );
                    // rewrite the clause to contain all the inferred terms
                    if ( subClause.getRequiredValue() instanceof Collection ) {
                        //noinspection unchecked
                        it.addAll( ( Collection<String> ) subClause.getRequiredValue() );
                    } else if ( subClause.getRequiredValue() instanceof String ) {
                        it.add( ( String ) subClause.getRequiredValue() );
                    } else {
                        clauseBuilder = clauseBuilder.or( subClause );
                    }
                } else {
                    // clause is irrelevant, so we add it as it is
                    clauseBuilder = clauseBuilder.or( subClause );
                }
            }
            // recreate a clause with inferred terms
            for ( Map.Entry<SubClauseKey, Set<String>> e : termUrisBySubClause.entrySet() ) {
                Collection<String> termAndChildrenUris = new HashSet<>( e.getValue() );
                Set<OntologyTerm> terms = ontologyService.getTerms( e.getValue() );
                termAndChildrenUris.addAll( ontologyService.getChildren( terms, false, true ).stream()
                        .map( OntologyTerm::getUri )
                        .collect( Collectors.toList() ) );
                if ( mentionedTerms != null ) {
                    mentionedTerms.addAll( terms );
                }
                Filter g;
                if ( termAndChildrenUris.size() == 1 ) {
                    g = Filter.by( e.getKey().getObjectAlias(), e.getKey().getPropertyName(), String.class, Filter.Operator.eq, termAndChildrenUris.iterator().next(), e.getKey().getOriginalProperty() );
                } else if ( termAndChildrenUris.size() > 1 ) {
                    g = Filter.by( e.getKey().getObjectAlias(), e.getKey().getPropertyName(), String.class, Filter.Operator.in, termAndChildrenUris, e.getKey().getOriginalProperty() );
                } else {
                    continue; // empty clause, is that even possible?
                }
                // this is the case for all the properties declared in PROPERTY_USED_FOR_ANNOTATIONS
                assert g.getOriginalProperty() != null;
                assert g.getObjectAlias() != null;
                // nest the filter in a subquery, all the applicable properties are one-to-many
                String prefix = g.getOriginalProperty().substring( 0, g.getOriginalProperty().lastIndexOf( '.' ) + 1 );
                String objectAlias = g.getObjectAlias();
                clauseBuilder = clauseBuilder.or( Filter.by( "ee", "id", Long.class,
                        Filter.Operator.inSubquery, new Subquery( "ExpressionExperiment", "id", guessAliases( prefix, objectAlias ), g ) ) );
            }
            f2 = clauseBuilder.build();
            termUrisBySubClause.clear();
        }
        return f2;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadWithCharacteristics( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            Hibernate.initialize( ee.getCharacteristics() );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssaySet loadBioAssaySet( Long id ) {
        return expressionExperimentDao.loadBioAssaySet( id );
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> ExpressionExperiment loadAndThawLiteOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T {
        ExpressionExperiment ee = loadOrFail( id, exceptionSupplier, message );
        this.expressionExperimentDao.thawWithoutVectors( ee );
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> ExpressionExperiment loadAndThawOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T {
        ExpressionExperiment ee = loadOrFail( id, exceptionSupplier, message );
        this.expressionExperimentDao.thaw( ee );
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> loadIdsWithCache( @Nullable Filters filters, @Nullable Sort sort ) {
        return expressionExperimentDao.loadIdsWithCache( filters, sort );
    }

    @Override
    @Transactional(readOnly = true)
    public long countWithCache( @Nullable Filters filters ) {
        return expressionExperimentDao.countWithCache( filters );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ExpressionExperimentValueObject> loadValueObjectsWithCache( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return expressionExperimentDao.loadValueObjectsWithCache( filters, sort, offset, limit );
    }

    /**
     * Identifies a sub-clause in a filter.
     */
    @Value(staticConstructor = "from")
    private static class SubClauseKey {
        String objectAlias;
        String propertyName;
        String originalProperty;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Characteristic, Long> getCategoriesUsageFrequency( @Nullable Filters filters, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, @Nullable Collection<String> retainedTermUris ) {
        List<Long> eeIds;
        if ( filters == null || filters.isEmpty() ) {
            eeIds = null;
        } else {
            eeIds = expressionExperimentDao.loadIdsWithCache( filters, null );
        }
        if ( excludedTermUris != null ) {
            excludedTermUris = inferTermsUris( excludedTermUris );
        }
        return expressionExperimentDao.getCategoriesUsageFrequency( eeIds, excludedCategoryUris, excludedTermUris, retainedTermUris );
    }

    /**
     * If the term cannot be resolved via {@link OntologyService#getTerm(String)}, an attempt is done to resolve its
     * category and assign it as its parent. This handles free-text terms that lack a value URI.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CharacteristicWithUsageStatisticsAndOntologyTerm> getAnnotationsUsageFrequency( @Nullable Filters filters, int maxResults, int minFrequency, @Nullable String category, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, @Nullable Collection<String> retainedTermUris ) {
        if ( excludedTermUris != null ) {
            excludedTermUris = inferTermsUris( excludedTermUris );
        }

        Map<Characteristic, Long> result;
        if ( filters == null || filters.isEmpty() ) {
            result = expressionExperimentDao.getAnnotationsUsageFrequency( null, null, maxResults, minFrequency, category, excludedCategoryUris, excludedTermUris, retainedTermUris );
        } else {
            List<Long> eeIds = expressionExperimentDao.loadIdsWithCache( filters, null );
            result = expressionExperimentDao.getAnnotationsUsageFrequency( eeIds, null, maxResults, minFrequency, category, excludedCategoryUris, excludedTermUris, retainedTermUris );
        }

        List<CharacteristicWithUsageStatisticsAndOntologyTerm> resultWithParents = new ArrayList<>( result.size() );

        // gather all the values and categories
        Set<String> uris = result.keySet().stream()
                .flatMap( c -> Stream.of( c.getValueUri(), c.getCategoryUri() ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
        // TODO: handle more than one term per URI
        Map<String, Set<OntologyTerm>> termByUri = ontologyService.getTerms( uris ).stream()
                .collect( Collectors.groupingBy( OntologyTerm::getUri, Collectors.toSet() ) );

        for ( Map.Entry<Characteristic, Long> entry : result.entrySet() ) {
            Characteristic c = entry.getKey();
            OntologyTerm term;
            if ( c.getValueUri() != null && termByUri.containsKey( c.getValueUri() ) ) {
                term = termByUri.get( c.getValueUri() ).iterator().next();
            } else if ( c.getCategoryUri() != null && termByUri.containsKey( c.getCategoryUri() ) ) {
                term = new OntologyTermSimpleWithCategory( c.getValueUri(), c.getValue(), termByUri.get( c.getCategoryUri() ).iterator().next() );
            } else {
                // create an uncategorized term
                term = new OntologyTermSimpleWithCategory( c.getValueUri(), c.getValue(), null );
            }
            resultWithParents.add( new CharacteristicWithUsageStatisticsAndOntologyTerm( entry.getKey(), entry.getValue(), term ) );
        }

        return resultWithParents;
    }

    /**
     * Infer all the implied terms from the given collection of term URIs.
     */
    private Set<String> inferTermsUris( Collection<String> termUris ) {
        Set<String> excludedTermUris = new HashSet<>( termUris );
        // expand exclusions with implied terms via subclass relation
        Set<OntologyTerm> excludedTerms = ontologyService.getTerms( excludedTermUris );
        // exclude terms using the subClass relation
        Set<OntologyTerm> impliedTerms = ontologyService.getChildren( excludedTerms, false, false );
        for ( OntologyTerm t : impliedTerms ) {
            excludedTermUris.add( t.getUri() );
        }
        return excludedTermUris;
    }

    /**
     * Extension of {@link OntologyTermSimple} that adds a category term as unique parent.
     */
    private static class OntologyTermSimpleWithCategory extends OntologyTermSimple {

        @Nullable
        private final OntologyTerm categoryTerm;

        public OntologyTermSimpleWithCategory( String uri, String term, @Nullable OntologyTerm categoryTerm ) {
            super( uri, term );
            this.categoryTerm = categoryTerm;
        }

        @Override
        public Collection<OntologyTerm> getParents( boolean direct, boolean includeAdditionalProperties, boolean keepObsoletes ) {
            if ( categoryTerm == null ) {
                return Collections.emptySet();
            }
            if ( direct ) {
                return Collections.singleton( categoryTerm );
            } else {
                // combine the direct parents + all the parents from the parents
                return Stream.concat( Stream.of( categoryTerm ), Stream.of( categoryTerm ).flatMap( t -> getParents( false, includeAdditionalProperties, keepObsoletes ).stream() ) )
                        .collect( Collectors.toSet() );
            }
        }

        @Override
        public boolean isRoot() {
            return categoryTerm == null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> getArrayDesignsUsed( final BioAssaySet expressionExperiment ) {
        return this.expressionExperimentDao.getArrayDesignsUsed( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<TechnologyType, Long> getTechnologyTypeUsageFrequency( @Nullable Filters filters ) {
        if ( filters == null || filters.isEmpty() ) {
            return expressionExperimentDao.getTechnologyTypeUsageFrequency();
        } else {
            List<Long> ids = this.expressionExperimentDao.loadIdsWithCache( filters, null );
            return expressionExperimentDao.getTechnologyTypeUsageFrequency( ids );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ArrayDesign, Long> getArrayDesignUsedOrOriginalPlatformUsageFrequency( @Nullable Filters filters, int maxResults ) {
        Map<ArrayDesign, Long> result;
        if ( filters == null || filters.isEmpty() ) {
            result = new HashMap<>( expressionExperimentDao.getArrayDesignsUsageFrequency( maxResults ) );
            for ( Map.Entry<ArrayDesign, Long> e : expressionExperimentDao.getOriginalPlatformsUsageFrequency( maxResults ).entrySet() ) {
                result.compute( e.getKey(), ( k, v ) -> ( v != null ? v : 0L ) + e.getValue() );
            }
        } else {
            List<Long> ids = this.expressionExperimentDao.loadIdsWithCache( filters, null );
            result = new HashMap<>( expressionExperimentDao.getArrayDesignsUsageFrequency( ids, maxResults ) );
            for ( Map.Entry<ArrayDesign, Long> e : expressionExperimentDao.getOriginalPlatformsUsageFrequency( ids, maxResults ).entrySet() ) {
                result.compute( e.getKey(), ( k, v ) -> ( v != null ? v : 0L ) + e.getValue() );
            }
        }
        // retain top results
        // this happens when original platforms are mixed in
        if ( maxResults > 0 && result.size() > maxResults ) {
            return result.entrySet()
                    .stream()
                    .sorted( Map.Entry.comparingByValue( Comparator.reverseOrder() ) )
                    .limit( maxResults )
                    .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Taxon, Long> getTaxaUsageFrequency( @Nullable Filters filters ) {
        if ( filters == null || filters.isEmpty() ) {
            return expressionExperimentDao.getPerTaxonCount();
        } else {
            List<Long> ids = this.expressionExperimentDao.loadIdsWithCache( filters, null );
            return expressionExperimentDao.getPerTaxonCount( ids );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getBatchConfound( ExpressionExperiment ee ) {
        ee = this.thawBioAssays( ee );

        if ( !this.checkHasBatchInfo( ee ) ) {
            return null;
        }

        Collection<BatchConfound> confounds;
        try {
            confounds = BatchConfoundUtils.test( ee );
        } catch ( NotStrictlyPositiveException e ) {
            AbstractService.log.error( String.format( "Batch confound test for %s threw a NonStrictlyPositiveException! Returning null.", ee ), e );
            return null;
        }

        StringBuilder result = new StringBuilder();
        // Confounds have to be sorted in order to always get the same string
        List<BatchConfound> listConfounds = new ArrayList<>( confounds );
        listConfounds.sort( Comparator.comparing( BatchConfound::toString ) );

        for ( BatchConfound c : listConfounds ) {
            if ( c.getP() < ExpressionExperimentServiceImpl.BATCH_CONFOUND_THRESHOLD ) {
                String factorName = c.getEf().getName();
                if ( result.toString().isEmpty() ) {
                    result.append(
                            "One or more factors were confounded with batches in the full design; batch correction was not performed. "
                                    + "Analyses may not be affected if performed on non-confounded subsets. Factor(s) confounded were: " );
                } else {
                    result.append( ", " );
                }
                result.append( factorName );
            }
        }

        // Now check subsets, if relevant.
        if ( !listConfounds.isEmpty() && gemma.gsec.util.SecurityUtil.isUserAdmin() ) {
            Collection<ExpressionExperimentSubSet> subSets = this.getSubSets( ee );
            if ( !subSets.isEmpty() ) {
                for ( ExpressionExperimentSubSet subset : subSets ) {
                    try {
                        confounds = BatchConfoundUtils.test( subset );
                        for ( BatchConfound c : confounds ) {
                            if ( c.getP() < ExpressionExperimentServiceImpl.BATCH_CONFOUND_THRESHOLD ) {
                                result.append( "<br/><br/>Confound still exists for " + c.getEf().getName() + " in " + subset );
                            }
                        }
                    } catch ( NotStrictlyPositiveException e ) {

                    }
                }
            }
        }

        return StringUtils.stripToNull( result.toString() );
    }

    private boolean checkIfSingleBatch( ExpressionExperiment ee ) {
        AuditEvent ev = this.auditEventService.getLastEvent( ee, BatchInformationFetchingEvent.class );
        if ( ev == null ) return false;

        if ( SingleBatchDeterminationEvent.class.isAssignableFrom( ev.getEventType().getClass() ) ) {
            return true;
        }

        // address cases that were run prior to having the SingleBatchDeterminationEvent type.
        if ( ev.getNote() != null && ( ev.getNote().startsWith( "1 batch" ) || ev.getNote().startsWith( "AffyScanDateExtractor; 0 batches" ) ) ) {
            return true;
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public BatchEffectDetails getBatchEffectDetails( ExpressionExperiment ee ) {
        ee = this.thawLiter( ee );

        BatchEffectDetails details = new BatchEffectDetails( this.checkBatchFetchStatus( ee ),
                this.getHasBeenBatchCorrected( ee ), this.checkIfSingleBatch( ee ) );

        // if missing or failed, we can't compute a P-value
        if ( !details.hasBatchInformation() || details.hasProblematicBatchInformation() ) {
            return details;
        }

        // we can't compute a P-value for a single batch
        if ( details.isSingleBatch() ) {
            return details;
        }

        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( BatchInfoPopulationServiceImpl.isBatchFactor( ef ) ) {
                SVDValueObject svd = svdService.getSvdFactorAnalysis( ee.getId() );
                if ( svd == null ) {
                    log.warn( "SVD was null for " + ef + ", can't compute batch effect statistics." );
                    break;
                }
                double minP = 1.0;
                for ( Integer component : svd.getFactorPvals().keySet() ) {
                    Map<Long, Double> cmpEffects = svd.getFactorPvals().get( component );
                    Double pVal = cmpEffects.get( ef.getId() );

                    if ( pVal != null && pVal < minP ) {
                        details.setBatchEffectStatistics( pVal, component + 1, svd.getVariances()[component] );
                        minP = pVal;
                    }

                }
                return details;
            }
        }

        log.warn( String.format( "No suitable batch factor was found for %s to obtain batch effect statistics.", ee ) );

        return details;
    }

    /**
     * WARNING: do not change these strings as they are used directly in ExpressionExperimentPage.js
     */
    @Override
    @Transactional(readOnly = true)
    public BatchEffectType getBatchEffect( ExpressionExperiment ee ) {
        BatchEffectDetails beDetails = this.getBatchEffectDetails( ee );
        if ( !beDetails.hasBatchInformation() ) {
            return BatchEffectType.NO_BATCH_INFO;
        } else if ( beDetails.getHasSingletonBatches() ) {
            return BatchEffectType.SINGLETON_BATCHES_FAILURE;
        } else if ( beDetails.getHasUninformativeBatchInformation() ) {
            return BatchEffectType.UNINFORMATIVE_HEADERS_FAILURE;
        } else if ( beDetails.isSingleBatch() ) {
            return BatchEffectType.SINGLE_BATCH_SUCCESS;
        } else if ( beDetails.getDataWasBatchCorrected() ) {
            // Checked for in ExpressionExperimentDetails.js::renderStatus()
            return BatchEffectType.BATCH_CORRECTED_SUCCESS;
        } else if ( beDetails.hasProblematicBatchInformation() ) {
            // sort of generic
            return BatchEffectType.PROBLEMATIC_BATCH_INFO_FAILURE;
        } else if ( beDetails.getBatchEffectStatistics() == null ) {
            return BatchEffectType.BATCH_EFFECT_UNDETERMINED_FAILURE;
        } else if ( beDetails.getBatchEffectStatistics().getPvalue() < ExpressionExperimentServiceImpl.BATCH_EFFECT_THRESHOLD ) {
            return BatchEffectType.BATCH_EFFECT_FAILURE;
        } else {
            return BatchEffectType.NO_BATCH_EFFECT_SUCCESS;
        }
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public String getBatchEffectStatistics( ExpressionExperiment ee ) {
        BatchEffectDetails beDetails = this.getBatchEffectDetails( ee );
        if ( beDetails.getBatchEffectStatistics() != null ) {
            return String.format( "This data set may have a batch artifact (PC %d), p=%.5g",
                    beDetails.getBatchEffectStatistics().getComponent(),
                    beDetails.getBatchEffectStatistics().getPvalue() );
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment ) {
        Collection<BioAssayDimension> bioAssayDimensions = this.expressionExperimentDao
                .getBioAssayDimensions( expressionExperiment );
        Collection<BioAssayDimension> thawedBioAssayDimensions = new HashSet<>();
        for ( BioAssayDimension bioAssayDimension : bioAssayDimensions ) {
            thawedBioAssayDimensions.add( this.bioAssayDimensionService.thaw( bioAssayDimension ) );
        }
        return thawedBioAssayDimensions;
    }

    @Override
    @Transactional(readOnly = true)
    public long getBioMaterialCount( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getBioMaterialCount( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public long getDesignElementDataVectorCount( final ExpressionExperiment ee ) {
        return this.expressionExperimentDao.getDesignElementDataVectorCount( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getExperimentsWithOutliers() {
        return this.expressionExperimentDao.getExperimentsWithOutliers();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Date> getLastArrayDesignUpdate( final Collection<ExpressionExperiment> expressionExperiments ) {
        return this.expressionExperimentDao.getLastArrayDesignUpdate( expressionExperiments );
    }

    @Override
    @Transactional(readOnly = true)
    public Date getLastArrayDesignUpdate( final ExpressionExperiment ee ) {
        return this.expressionExperimentDao.getLastArrayDesignUpdate( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastLinkAnalysis( final Collection<Long> ids ) {
        return this.getLastEvent( this.load( ids ), new LinkAnalysisEvent() );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastMissingValueAnalysis( final Collection<Long> ids ) {
        return this.getLastEvent( this.load( ids ), new MissingValueAnalysisEvent() );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastProcessedDataUpdate( final Collection<Long> ids ) {
        return this.getLastEvent( this.load( ids ), new ProcessedVectorComputationEvent() );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Taxon, Long> getPerTaxonCount() {
        return this.expressionExperimentDao.getPerTaxonCount();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getPopulatedFactorCounts( final Collection<Long> ids ) {
        return this.expressionExperimentDao.getPopulatedFactorCounts( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getPopulatedFactorCountsExcludeBatch( final Collection<Long> ids ) {
        return this.expressionExperimentDao.getPopulatedFactorCountsExcludeBatch( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public QuantitationType getPreferredQuantitationType( final ExpressionExperiment ee ) {
        return this.expressionExperimentDao.getPreferredQuantitationType( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public QuantitationType getMaskedPreferredQuantitationType( ExpressionExperiment ee ) {
        return expressionExperimentDao.getMaskedPreferredQuantitationType( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<QuantitationType, Long> getQuantitationTypeCount( ExpressionExperiment ee ) {
        return this.expressionExperimentDao.getQuantitationTypeCount( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationType> getQuantitationTypes( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getQuantitationTypes( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment ee, ArrayDesign oldAd ) {
        return this.expressionExperimentDao.getQuantitationTypes( ee, oldAd );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationTypeValueObject> getQuantitationTypeValueObjects( ExpressionExperiment expressionExperiment ) {
        expressionExperiment = ensureInSession( expressionExperiment );
        return quantitationTypeService.loadValueObjectsWithExpressionExperiment( expressionExperiment.getQuantitationTypes(), expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            final Collection<ExpressionExperiment> expressionExperiments ) {
        return this.expressionExperimentDao.getSampleRemovalEvents( expressionExperiments );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> getSubSets( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getSubSets( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends BioAssaySet> Map<T, Taxon> getTaxa( Collection<T> bioAssaySets ) {
        return this.expressionExperimentDao.getTaxa( bioAssaySets );
    }

    @Override
    @Transactional(readOnly = true)
    public Taxon getTaxon( final BioAssaySet bioAssaySet ) {
        return this.expressionExperimentDao.getTaxon( bioAssaySet );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRNASeq( ExpressionExperiment expressionExperiment ) {
        Collection<ArrayDesign> ads = this.expressionExperimentDao.getArrayDesignsUsed( expressionExperiment );
        /*
         * This isn't completely bulletproof. We are simply assuming that if any of the platforms isn't a microarray (or
         * 'OTHER'), it's RNA-seq.
         */
        for ( ArrayDesign ad : ads ) {
            TechnologyType techtype = ad.getTechnologyType();

            if ( techtype.equals( TechnologyType.SEQUENCING )
                    || techtype.equals( TechnologyType.GENELIST ) ) {
                return true;
            }
        }
        return false;

    }

    /**
     * @param ee the expression experiment to be checked for trouble. This method will usually be preferred over
     *           checking
     *           the curation details of the object directly, as this method also checks all the array designs the
     *           given
     *           experiment belongs to.
     * @return true, if the given experiment, or any of its parenting array designs is troubled. False otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isTroubled( ExpressionExperiment ee ) {
        if ( ee.getCurationDetails().getTroubled() )
            return true;
        Collection<ArrayDesign> ads = this.getArrayDesignsUsed( ee );
        for ( ArrayDesign ad : ads ) {
            if ( ad.getCurationDetails().getTroubled() )
                return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjects( @Nullable Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit ) {
        return this.expressionExperimentDao.loadDetailsValueObjects( ids, taxon, sort, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsWithCache( Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit ) {
        return this.expressionExperimentDao.loadDetailsValueObjectsByIdsWithCache( ids, taxon, sort, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIds( Collection<Long> ids ) {
        return this.expressionExperimentDao.loadDetailsValueObjectsByIds( ids );
    }

    @Transactional(readOnly = true)
    public List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIdsWithCache( Collection<Long> ids ) {
        return this.expressionExperimentDao.loadDetailsValueObjectsByIdsWithCache( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ExpressionExperimentValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return expressionExperimentDao.loadBlacklistedValueObjects( filters, sort, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadLackingFactors() {
        return this.expressionExperimentDao.loadLackingFactors();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadLackingTags() {
        return this.expressionExperimentDao.loadLackingTags();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperimentValueObject> loadValueObjectsByIdsWithRelationsAndCache( List<Long> ids ) {
        List<ExpressionExperiment> results = expressionExperimentDao.loadWithRelationsAndCache( ids );
        Map<Long, Integer> id2position = ListUtils.indexOfElements( ids );
        return expressionExperimentDao.loadValueObjects( results ).stream()
                .sorted( Comparator.comparing( vo -> id2position.get( vo.getId() ) ) )
                .collect( Collectors.toList() );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperimentValueObject> loadValueObjectsByIds( final List<Long> ids,
            boolean maintainOrder ) {
        List<ExpressionExperimentValueObject> results = this.expressionExperimentDao.loadValueObjectsByIds( ids );

        // sort results according to ids
        if ( maintainOrder ) {
            Map<Long, Integer> id2position = ListUtils.indexOfElements( ids );
            return results.stream()
                    .sorted( Comparator.comparing( vo -> id2position.get( vo.getId() ) ) )
                    .collect( Collectors.toList() );
        }

        return results;
    }

    @Override
    @Transactional
    public ExpressionExperiment replaceRawVectors( ExpressionExperiment ee,
            Collection<RawExpressionDataVector> newVectors ) {

        if ( newVectors.isEmpty() ) {
            throw new UnsupportedOperationException( "Only use this method for replacing vectors, not erasing them" );
        }

        Set<QuantitationType> newQts = newVectors.stream()
                .map( RawExpressionDataVector::getQuantitationType )
                .collect( Collectors.toSet() );

        Set<QuantitationType> preferredQts = newQts.stream().filter( QuantitationType::getIsPreferred ).collect( Collectors.toSet() );
        if ( preferredQts.size() != 1 ) {
            throw new IllegalArgumentException( String.format( "New vectors for %s must have exactly one preferred quantitation type.",
                    ee ) );
        }

        // to attach to session correctly.
        ExpressionExperiment eeToUpdate = this.loadOrFail( ee.getId() );

        // remove existing QTs attached to raw vectors
        Collection<QuantitationType> qtsToRemove = eeToUpdate.getRawExpressionDataVectors().stream()
                .map( RawExpressionDataVector::getQuantitationType )
                // These QTs might still be getting used by the replaced vectors.
                .filter( q -> !newQts.contains( q ) )
                .collect( Collectors.toSet() );
        ee.getQuantitationTypes().removeAll( qtsToRemove );

        // remove the vectors
        eeToUpdate.getRawExpressionDataVectors().clear();

        // group the vectors up by bioassay dimension, if need be. This could be modified to handle multiple quantitation types if need be.
        Map<BioAssayDimension, Set<RawExpressionDataVector>> BADs = newVectors.stream()
                .collect( Collectors.groupingBy( RawExpressionDataVector::getBioAssayDimension, Collectors.toSet() ) );

        for ( Collection<RawExpressionDataVector> vectors : BADs.values() ) {
            ee = this.addRawVectors( eeToUpdate, vectors );
        }
        return ee;
    }

    @Override
    @Transactional
    public void addSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, Collection<SingleCellExpressionDataVector> vectors ) {
        Assert.notNull( ee.getId(), "The dataset must be persistent." );
        Assert.notNull( quantitationType.getId(), "The quantitation type must be persistent." );
        Assert.isTrue( !ee.getQuantitationTypes().contains( quantitationType ),
                String.format( "%s already have vectors for the quantitation type: %s; use replaceSingleCellDataVectors() to replace existing vectors.",
                        ee, quantitationType ) );
        validateSingleCellDataVectors( ee, quantitationType, vectors );
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        if ( scd.getId() == null ) {
            log.info( "Creating a new single-cell dimension for " + ee + ": " + scd );
            expressionExperimentDao.createSingleCellDimension( ee, scd );
        }
        for ( SingleCellExpressionDataVector v : vectors ) {
            v.setExpressionExperiment( ee );
        }
        int previousSize = ee.getSingleCellExpressionDataVectors().size();
        log.info( String.format( "Adding %d single-cell vectors to %s for %s", vectors.size(), ee, quantitationType ) );
        ee.getSingleCellExpressionDataVectors().addAll( vectors );
        int numVectorsAdded = ee.getSingleCellExpressionDataVectors().size() - previousSize;
        // make all other single-cell QTs non-preferred
        if ( quantitationType.getIsPreferred() ) {
            for ( QuantitationType qt : ee.getQuantitationTypes() ) {
                if ( qt.getIsPreferred() ) {
                    log.info( "Setting " + qt + " to non-preferred since we're adding a new set of preferred vectors to " + ee );
                    qt.setIsPreferred( false );
                    break; // there is at most 1 set of preferred vectors
                }
            }
        }
        ee.getQuantitationTypes().add( quantitationType );
        update( ee ); // will take care of creating vectors
        auditTrailService.addUpdateEvent( ee, DataAddedEvent.class,
                String.format( "Added %d vectors for %s with dimension %s", numVectorsAdded, quantitationType, scd ) );
    }

    @Override
    @Transactional
    public void replaceSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, Collection<SingleCellExpressionDataVector> vectors ) {
        Assert.notNull( ee.getId(), "The dataset must be persistent." );
        Assert.notNull( quantitationType.getId(), "The quantitation type must be persistent." );
        Assert.isTrue( ee.getQuantitationTypes().contains( quantitationType ),
                String.format( "%s does not have the quantitation type: %s; use addSingleCellDataVectors() to add new vectors instead.",
                        ee, quantitationType ) );
        validateSingleCellDataVectors( ee, quantitationType, vectors );
        boolean scdCreated = false;
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        if ( scd.getId() == null ) {
            log.info( "Creating a new single-cell dimension for " + ee + ": " + scd );
            expressionExperimentDao.createSingleCellDimension( ee, scd );
            scdCreated = true;
        }
        Set<SingleCellExpressionDataVector> vectorsToBeReplaced = ee.getSingleCellExpressionDataVectors().stream()
                .filter( v -> v.getQuantitationType().equals( quantitationType ) ).collect( Collectors.toSet() );
        for ( SingleCellExpressionDataVector v : vectors ) {
            v.setExpressionExperiment( ee );
        }
        int previousSize = ee.getSingleCellExpressionDataVectors().size();
        if ( !vectorsToBeReplaced.isEmpty() ) {
            // if the SCD was created, we do not need to check additional vectors for removing the existing one
            removeSingleCellVectorsAndDimensionIfNecessary( ee, vectorsToBeReplaced, scdCreated ? null : vectors );
        } else {
            log.warn( "No vectors with the quantitation type: " + quantitationType );
        }
        int numVectorsRemoved = ee.getSingleCellExpressionDataVectors().size() - previousSize;
        log.info( String.format( "Adding %d single-cell vectors to %s for %s", vectors.size(), ee, quantitationType ) );
        ee.getSingleCellExpressionDataVectors().addAll( vectors );
        int numVectorsAdded = ee.getSingleCellExpressionDataVectors().size() - ( previousSize - numVectorsRemoved );
        update( ee );
        auditTrailService.addUpdateEvent( ee, DataReplacedEvent.class,
                String.format( "Replaced %d vectors with %d vectors for %s with dimension %s.", numVectorsRemoved, numVectorsAdded, quantitationType, scd ) );
    }

    private void validateSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, Collection<SingleCellExpressionDataVector> vectors ) {
        Assert.notNull( quantitationType.getId(), "The quantitation type must be persistent." );
        Assert.isTrue( !vectors.isEmpty(), "At least one single-cell vector has to be supplied; use removeSingleCellDataVectors() to remove vectors instead." );
        Assert.isTrue( vectors.stream().allMatch( v -> v.getExpressionExperiment() == null || v.getExpressionExperiment().equals( ee ) ),
                "Some of the vectors belong to other expression experiments." );
        Assert.isTrue( vectors.stream().allMatch( v -> v.getQuantitationType() == quantitationType ),
                "All vectors must have the same quantitation type: " + quantitationType );
        Assert.isTrue( vectors.stream().allMatch( v -> v.getDesignElement() != null && v.getDesignElement().getId() != null ),
                "All vectors must have a persistent design element." );
        // TODO: allow vectors from multiple platforms
        CompositeSequence element = vectors.iterator().next().getDesignElement();
        ArrayDesign platform = element.getArrayDesign();
        Assert.isTrue( vectors.stream().allMatch( v -> v.getDesignElement().getArrayDesign().equals( platform ) ),
                "All vectors must have a persistent design element from the same platform." );
        SingleCellDimension singleCellDimension = vectors.iterator().next().getSingleCellDimension();
        validateSingleCellDimension( ee, singleCellDimension );
        Assert.isTrue( vectors.stream().allMatch( v -> v.getSingleCellDimension() == singleCellDimension ),
                "All vectors must share the same dimension: " + singleCellDimension );
        Assert.isTrue( singleCellDimension.getId() == null
                        || ee.getSingleCellExpressionDataVectors().stream().map( SingleCellExpressionDataVector::getSingleCellDimension ).anyMatch( singleCellDimension::equals ),
                singleCellDimension + " is persistent, but does not belong any single-cell vector of this dataset: " + ee );
    }

    /**
     * Validate single-cell dimension.
     */
    private void validateSingleCellDimension( ExpressionExperiment ee, SingleCellDimension scbad ) {
        Assert.isTrue( scbad.getCellIds().size() == scbad.getNumberOfCells(),
                "The number of cell IDs must match the number of cells." );
        if ( scbad.getCellTypes() != null ) {
            Assert.notNull( scbad.getNumberOfCellTypeLabels() );
            Assert.notNull( scbad.getCellTypeLabels() );
            Assert.isTrue( scbad.getCellTypes().length == scbad.getCellIds().size(),
                    "The number of cell types must match the number of cell IDs." );
            Assert.isTrue( scbad.getCellTypeLabels().size() == scbad.getNumberOfCellTypeLabels(),
                    "The number of cell types must match the number of values the cellTypeLabels collection." );
        } else {
            Assert.isNull( scbad.getCellTypeLabels() );
            Assert.isNull( scbad.getNumberOfCellTypeLabels(), "There is no cell types assigned, the number of cell types must be null." );
        }
        Assert.isTrue( ee.getBioAssays().containsAll( scbad.getBioAssays() ), "Not all supplied BioAssays belong to " + ee );
        validateSparseRangeArray( scbad.getBioAssays(), scbad.getBioAssaysOffset(), scbad.getNumberOfCells() );
    }

    @Override
    @Transactional
    public void removeSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType ) {
        Assert.notNull( ee.getId(), "The dataset must be persistent." );
        Assert.notNull( quantitationType.getId(), "The quantitation type must be persistent." );
        Assert.isTrue( ee.getQuantitationTypes().contains( quantitationType ),
                String.format( "%s does not have the quantitation type %s.", ee, quantitationType ) );
        Set<SingleCellExpressionDataVector> vectors = ee.getSingleCellExpressionDataVectors().stream()
                .filter( v -> v.getQuantitationType().equals( quantitationType ) ).collect( Collectors.toSet() );
        SingleCellDimension scd;
        if ( !vectors.isEmpty() ) {
            scd = vectors.iterator().next().getSingleCellDimension();
            removeSingleCellVectorsAndDimensionIfNecessary( ee, vectors, null );
        } else {
            scd = null;
            log.warn( "No vectors with the quantitation type: " + quantitationType );
        }
        ee.getQuantitationTypes().remove( quantitationType );
        update( ee );
        if ( !vectors.isEmpty() ) {
            auditTrailService.addUpdateEvent( ee, DataRemovedEvent.class,
                    String.format( "Removed %d vectors for %s with dimension %s.", vectors.size(), quantitationType, scd ) );
        }
    }

    /**
     * @deprecated do not use this, it's only meant as a workaround for deleting single-cell vectors
     */
    @Autowired
    @Deprecated
    private SessionFactory sessionFactory;

    /**
     * Remove the given single-cell vectors and their corresponding single-cell dimension if necessary.
     * @param ee the experiment to remove the vectors from.
     * @param additionalVectors additional vectors to check if the single-cell dimension is still in use (i.e. vectors that are in the process of being added).
     * @return true if the vectors were removed, false otherwise.
     */
    private void removeSingleCellVectorsAndDimensionIfNecessary( ExpressionExperiment ee,
            Collection<SingleCellExpressionDataVector> vectors,
            @Nullable Collection<SingleCellExpressionDataVector> additionalVectors ) {
        log.info( String.format( "Removing %d single-cell vectors for %s...", vectors.size(), ee ) );
        ee.getSingleCellExpressionDataVectors().removeAll( vectors );
        // FIXME: flushing shouldn't be necessary here, but Hibernate does appear to cascade vectors removal prior to removing the SCD or QT...
        sessionFactory.getCurrentSession().flush();
        // check if SCD is still in use else remove it
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        boolean scdStillUsed = false;
        for ( SingleCellExpressionDataVector v : ee.getSingleCellExpressionDataVectors() ) {
            if ( v.getSingleCellDimension().equals( scd ) ) {
                scdStillUsed = true;
                break;
            }
        }
        if ( !scdStillUsed && additionalVectors != null ) {
            for ( SingleCellExpressionDataVector v : additionalVectors ) {
                if ( v.getSingleCellDimension().equals( scd ) ) {
                    scdStillUsed = true;
                    break;
                }
            }
        }
        if ( !scdStillUsed ) {
            log.info( "Removing unused single-cell dimension " + scd + " for " + ee );
            expressionExperimentDao.deleteSingleCellDimension( ee, scd );
        }
    }

    /**
     * Will add the characteristic to the expression experiment and persist the changes.
     *
     * @param ee the experiment to add the characteristics to.
     * @param vc If the evidence code is null, it will be filled in with IC. A category and value must be provided.
     */
    @Override
    @Transactional
    public void addCharacteristic( ExpressionExperiment ee, Characteristic vc ) {
        Assert.isTrue( StringUtils.isNotBlank( vc.getCategory() ), "Must provide a category" );
        Assert.isTrue( StringUtils.isNotBlank( vc.getValue() ), "Must provide a value" );

        ee = ensureInSession( ee );

        if ( vc.getEvidenceCode() == null ) {
            log.debug( String.format( "No evidence code set for %s, defaulting to %s.", vc, GOEvidenceCode.IC ) );
            vc.setEvidenceCode( GOEvidenceCode.IC ); // assume: manually added characteristic
        }

        ExpressionExperimentServiceImpl.log
                .info( "Adding characteristic '" + vc.getValue() + "' to " + ee.getShortName() + " (ID=" + ee.getId()
                        + ") : " + vc );

        ee.getCharacteristics().add( vc );
        this.update( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thaw( final ExpressionExperiment expressionExperiment ) {
        ExpressionExperiment result = ensureInSession( expressionExperiment );
        this.expressionExperimentDao.thaw( result );
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thawBioAssays( final ExpressionExperiment expressionExperiment ) {
        ExpressionExperiment result = ensureInSession( expressionExperiment );
        this.expressionExperimentDao.thawBioAssays( result );
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thawLite( final ExpressionExperiment expressionExperiment ) {
        ExpressionExperiment result = ensureInSession( expressionExperiment );
        this.expressionExperimentDao.thawWithoutVectors( result );
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thawLiter( final ExpressionExperiment expressionExperiment ) {
        ExpressionExperiment result = ensureInSession( expressionExperiment );
        this.expressionExperimentDao.thawForFrontEnd( result );
        return result;
    }

    @Override
    @Transactional
    public void remove( Long id ) {
        ExpressionExperiment ee = this.load( id );
        if ( ee == null ) {
            log.warn( "ExpressionExperiment was null after reloading, skipping removal altogether." );
            return;
        }
        remove( ee );
    }

    /**
     * Deletes an experiment and all of its associated objects, including coexpression links. Some types of associated
     * objects may need to be deleted before this can be run (example: analyses involving multiple experiments; these
     * will not be deleted automatically).
     */
    @Override
    @Transactional
    public void remove( ExpressionExperiment ee ) {
        if ( !securityService.isEditable( ee ) ) {
            throw new SecurityException(
                    "Error performing 'ExpressionExperimentService.remove(ExpressionExperiment expressionExperiment)' --> "
                            + " You do not have permission to edit this experiment." );
        }

        // thaw everything
        ee = thaw( ee );

        // Remove subsets
        Collection<ExpressionExperimentSubSet> subsets = this.getSubSets( ee );
        for ( ExpressionExperimentSubSet subset : subsets ) {
            expressionExperimentSubSetService.remove( subset );
        }

        // Remove differential expression analyses
        this.differentialExpressionAnalysisService.removeForExperiment( ee );

        // Remove any sample coexpression matrices
        this.sampleCoexpressionAnalysisService.removeForExperiment( ee );

        // Remove PCA
        this.principalComponentAnalysisService.removeForExperiment( ee );

        // Remove coexpression analyses
        this.coexpressionAnalysisService.removeForExperiment( ee );

        /*
         * Delete any expression experiment sets that only have this one ee in it. If possible remove this experiment
         * from other sets, and update them. IMPORTANT, this section assumes that we already checked for gene2gene
         * analyses!
         */
        Collection<ExpressionExperimentSet> sets = this.expressionExperimentSetService.find( ee );
        for ( ExpressionExperimentSet eeSet : sets ) {
            if ( eeSet.getExperiments().size() == 1 && eeSet.getExperiments().iterator().next().equals( ee ) ) {
                AbstractService.log.info( "Removing from set " + eeSet );
                this.expressionExperimentSetService
                        .remove( eeSet ); // remove the set because in only contains this experiment
            } else {
                AbstractService.log.info( "Removing " + ee + " from " + eeSet );
                eeSet.getExperiments().remove( ee );
                this.expressionExperimentSetService.update( eeSet ); // update set to not reference this experiment.
            }
        }

        super.remove( ee );
    }

    @Override
    @Transactional
    public void remove( Collection<ExpressionExperiment> entities ) {
        entities.forEach( this::remove );
    }

    private Collection<? extends AnnotationValueObject> getAnnotationsByFactorValues( Long eeId ) {
        return this.expressionExperimentDao.getAnnotationsByFactorValues( eeId );
    }

    private Collection<? extends AnnotationValueObject> getAnnotationsByBioMaterials( Long eeId ) {
        return this.expressionExperimentDao.getAnnotationsByBioMaterials( eeId );

    }

    private boolean getHasBeenBatchCorrected( ExpressionExperiment ee ) {
        for ( QuantitationType qt : ee.getQuantitationTypes() ) {
            if ( qt.getIsBatchCorrected() ) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param ees  experiments
     * @param type event type
     * @return a map of the expression experiment ids to the last audit event for the given audit event type the
     * map
     * can contain nulls if the specified auditEventType isn't found for a given expression experiment id
     */
    private Map<Long, AuditEvent> getLastEvent( Collection<ExpressionExperiment> ees, AuditEventType type ) {

        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        AuditEvent last;
        for ( ExpressionExperiment experiment : ees ) {
            last = this.auditEventService.getLastEvent( experiment, type.getClass() );
            lastEventMap.put( experiment.getId(), last );
        }
        return lastEventMap;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService#isBlackListed(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isBlackListed( String geoAccession ) {
        return this.blacklistedEntityService.isBlacklisted( geoAccession );
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean isSuitableForDEA( ExpressionExperiment ee ) {
        AuditEvent ev = auditEventService.getLastEvent( ee, DifferentialExpressionSuitabilityEvent.class );
        return ev == null || !UnsuitableForDifferentialExpressionAnalysisEvent.class.isAssignableFrom( ev.getEventType().getClass() );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getExperimentsLackingPublications() {
        return this.expressionExperimentDao.getExperimentsLackingPublications();
    }

    @Override
    @Transactional
    public MeanVarianceRelation updateMeanVarianceRelation( ExpressionExperiment ee, MeanVarianceRelation mvr ) {
        return expressionExperimentDao.updateMeanVarianceRelation( ee, mvr );
    }

    @Override
    @Transactional(readOnly = true)
    public long countBioMaterials( @Nullable Filters filters ) {
        return expressionExperimentDao.countBioMaterials( filters );
    }

    /**
     * Checks for special properties that are allowed to be referenced on certain objects. E.g. characteristics on EEs.
     * {@inheritDoc}
     */
    @Override
    public Collection<ConfigAttribute> getFilterablePropertyConfigAttributes( String property ) {
        if ( property.equals( "geeq.publicSuitabilityScore" ) ) {
            return SecurityConfig.createList( "GROUP_ADMIN" );
        } else {
            return null;
        }
    }
}