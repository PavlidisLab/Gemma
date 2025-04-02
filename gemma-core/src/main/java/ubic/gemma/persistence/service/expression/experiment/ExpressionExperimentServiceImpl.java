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
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.model.OntologyTermSimple;
import ubic.gemma.core.analysis.expression.diff.BaselineSelection;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.ListUtils;
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
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionService;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.model.expression.experiment.StatementUtils.formatStatement;
import static ubic.gemma.persistence.util.SubqueryUtils.guessAliases;

/**
 * @author pavlidis
 * @author keshav
 * @see ExpressionExperimentService
 */
@Service
public class ExpressionExperimentServiceImpl
        extends AbstractFilteringVoEnabledService<ExpressionExperiment, ExpressionExperimentValueObject>
        implements ExpressionExperimentService {

    private final ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private AuditEventService auditEventService;
    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;
    @Autowired
    private BioMaterialService bioMaterialService;
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
    private CoexpressionService coexpressionService;

    @Autowired
    public ExpressionExperimentServiceImpl( ExpressionExperimentDao expressionExperimentDao ) {
        super( expressionExperimentDao );
        this.expressionExperimentDao = expressionExperimentDao;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadReference( Long id ) {
        return expressionExperimentDao.loadReference( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadReferences( Collection<Long> ids ) {
        return expressionExperimentDao.loadReference( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadAllReferences() {
        return expressionExperimentDao.loadReference( expressionExperimentDao.loadIds( null, null ) );
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
    public FactorValue addFactorValue( ExpressionExperiment ee, FactorValue fv ) {
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
        return fv;
    }


    @Override
    @Transactional
    public void addFactorValues( ExpressionExperiment ee, Map<BioMaterial, FactorValue> fvs ) {
        ExpressionExperiment experiment = requireNonNull( expressionExperimentDao.load( ee.getId() ) );
        Collection<ExperimentalFactor> efs = experiment.getExperimentalDesign().getExperimentalFactors();
        int count = 0;
        for ( BioMaterial bm : fvs.keySet() ) {
            FactorValue fv = fvs.get( bm );
            fv.setSecurityOwner( experiment );
            fv = this.factorValueService.create( fv );

            for ( ExperimentalFactor ef : efs ) {
                if ( fv.getExperimentalFactor().equals( ef ) ) {
                    ef.getFactorValues().add( fv );
                    break;
                }
            }
            bm.getFactorValues().add( fv );
            ++count;
            if ( count % 50 == 0 ) {
                log.info( "Processed: " + count + " biomaterials for new factor values" );
            }
        }
        log.info( "Processed: " + count + " biomaterials for new factor values, updating ..." );
        //  expressionExperimentDao.update( experiment );
        bioMaterialService.update( fvs.keySet() );
    }

    @Override
    @Transactional
    public int addRawDataVectors( ExpressionExperiment ee,
            QuantitationType quantitationType,
            Collection<RawExpressionDataVector> newVectors ) {
        Collection<BioAssayDimension> BADs = new HashSet<>();
        for ( RawExpressionDataVector vec : newVectors ) {
            BADs.add( vec.getBioAssayDimension() );
        }

        if ( BADs.size() > 1 ) {
            throw new IllegalArgumentException( "Vectors must share a common bioassay dimension" );
        }

        BioAssayDimension bad = BADs.iterator().next();
        if ( bad.getId() == null ) {
            log.info( "Creating " + bad + "..." );
            bad = this.bioAssayDimensionService.findOrCreate( bad );
            for ( RawExpressionDataVector vector : newVectors ) {
                vector.setBioAssayDimension( bad );
            }
        }

        return expressionExperimentDao.addRawDataVectors( ee, quantitationType, newVectors );
    }

    @Override
    @Transactional
    public int replaceRawDataVectors( ExpressionExperiment ee, QuantitationType qt, Collection<RawExpressionDataVector> vectors ) {
        return expressionExperimentDao.replaceRawDataVectors( ee, qt, vectors );
    }

    @Override
    @Transactional
    public int replaceAllRawDataVectors( ExpressionExperiment ee,
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

        // remove the vectors
        removeAllRawDataVectors( ee );

        // group the vectors up by QT
        Map<QuantitationType, Set<RawExpressionDataVector>> BADs = newVectors.stream()
                .collect( Collectors.groupingBy( RawExpressionDataVector::getQuantitationType, Collectors.toSet() ) );

        int added = 0;
        for ( Map.Entry<QuantitationType, Set<RawExpressionDataVector>> e : BADs.entrySet() ) {
            added += this.addRawDataVectors( ee, e.getKey(), e.getValue() );
        }

        return added;
    }

    @Override
    @Transactional
    public int removeAllRawDataVectors( ExpressionExperiment ee ) {
        return expressionExperimentDao.removeAllRawDataVectors( ee );
    }

    @Override
    @Transactional
    public int removeRawDataVectors( ExpressionExperiment ee, QuantitationType qt ) {
        return expressionExperimentDao.removeRawDataVectors( ee, qt );
    }

    @Override
    @Transactional
    public void createProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors ) {
        expressionExperimentDao.createProcessedDataVectors( ee, vectors );
    }

    @Override
    @Transactional
    public int removeProcessedDataVectors( ExpressionExperiment ee ) {
        return expressionExperimentDao.removeProcessedDataVectors( ee );
    }

    @Override
    @Transactional
    public int replaceProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors ) {
        return expressionExperimentDao.replaceProcessedDataVectors( ee, vectors );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperiment> browse( int start, int limit ) {
        return this.expressionExperimentDao.browse( start, limit );
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
        Set<AnnotationValueObject> annotations = new LinkedHashSet<>();
        Set<String> seenTerms = new HashSet<>();

        expressionExperimentDao.getExperimentAnnotations( expressionExperiment, false ).stream()
                .filter( this::filterAnnotation )
                .map( c -> new AnnotationValueObject( c, ExpressionExperiment.class ) )
                .forEach( c -> addIfNovel( annotations, c, seenTerms ) );

        String[] ignoredPredicates = new String[] {
                "http://gemma.msl.ubc.ca/ont/TGEMO_00166", // duration
                "http://gemma.msl.ubc.ca/ont/TGEMO_00167", // dose
                "http://gemma.msl.ubc.ca/ont/TGEMO_00168"  // development stage
        };
        expressionExperimentDao.getFactorValueAnnotations( expressionExperiment ).stream()
                .filter( this::filterFactorValueAnnotation )
                .map( c -> new AnnotationValueObject( c.getCategoryUri(), c.getCategory(), c.getSubjectUri(), formatStatement( c, ignoredPredicates ), FactorValue.class ) )
                .forEach( c -> addIfNovel( annotations, c, seenTerms ) );

        expressionExperimentDao.getBioMaterialAnnotations( expressionExperiment, false ).stream()
                .filter( this::filterBioMaterialAnnotation )
                .map( c -> new AnnotationValueObject( c, BioMaterial.class ) )
                .forEach( c -> addIfNovel( annotations, c, seenTerms ) );

        return annotations;
    }

    /**
     * Check if a term is novel and add it to the set of seen terms.
     */
    private void addIfNovel( Collection<AnnotationValueObject> annotations, AnnotationValueObject term, Set<String> seenTerms ) {
        if ( seenTerms.add( StringUtils.lowerCase( StringUtils.normalizeSpace( term.getTermName() ) ) ) ) {
            annotations.add( term );
        }
    }

    /**
     * Filter factor value annotations to be included as experiment tags.
     * <p>
     * FIXME filtering here is going to have to be more elaborate for this to be useful.
     * URIs checked for validity Aug 2024
     */
    private boolean filterFactorValueAnnotation( Statement c ) {
        return filterAnnotation( c )
                // ignore baseline conditions
                && !BaselineSelection.isBaselineCondition( c )
                // ignore batch factors
                && !ExperimentalDesignUtils.isBatch( c )
                // ignore timepoints
                && !"http://www.ebi.ac.uk/efo/EFO_0000724".equals( c.getCategoryUri() )
                // DE_include/exclude
                && !"http://gemma.msl.ubc.ca/ont/TGEMO_00013".equals( c.getSubjectUri() )
                && !"http://gemma.msl.ubc.ca/ont/TGEMO_00014".equals( c.getSubjectUri() );
    }

    /**
     * Filter sample annotations to be included as experiment tags.
     * <p>
     * TODO If can be done without much slowdown, add: certain selected (constant?) characteristics from
     * biomaterials? (non-redundant with tags)
     */
    private boolean filterBioMaterialAnnotation( Characteristic c ) {
        return filterAnnotation( c )
                && !"MaterialType".equalsIgnoreCase( c.getCategory() )
                && !"molecular entity".equalsIgnoreCase( c.getCategory() )
                && !"LabelCompound".equalsIgnoreCase( c.getCategory() )
                && !BaselineSelection.isBaselineCondition( c );
    }

    private boolean filterAnnotation( Characteristic characteristic ) {
        return filterAnnotation( characteristic.getCategoryUri(), characteristic.getCategory(), characteristic.getValueUri(), characteristic.getValue() );
    }

    /**
     * Filter the object of a statement.
     */
    private boolean filterStatementObject( Statement statement, boolean first ) {
        if ( first ) {
            Assert.notNull( statement.getPredicate() );
            Assert.notNull( statement.getObject() );
            return filterStatementObject( statement.getCategoryUri(), statement.getCategory(), statement.getSubjectUri(), statement.getSubject(), statement.getPredicateUri(), statement.getPredicate(), statement.getObjectUri(), statement.getObject() );
        } else {
            Assert.notNull( statement.getSecondPredicate() );
            Assert.notNull( statement.getSecondObject() );
            return filterStatementObject( statement.getCategoryUri(), statement.getCategory(), statement.getSubjectUri(), statement.getSubject(), statement.getSecondPredicateUri(), statement.getSecondPredicate(), statement.getSecondObjectUri(), statement.getSecondObject() );
        }
    }

    private boolean filterStatementObject( @Nullable String categoryUri, @Nullable String category, @Nullable String subjectUri, String subject, @Nullable String predicateUri, String predicate, @Nullable String objectUri, String object ) {
        return filterAnnotation( categoryUri, category, objectUri, object );
    }

    /**
     * Minimal requirements for an annotation to be included as an experiment tag.
     */
    private boolean filterAnnotation( @Nullable String categoryUri, @Nullable String category, @Nullable String valueUri, String value ) {
        // ignore uncategorized terms
        return category != null
                // ignore free-text categories
                && categoryUri != null // free-text categories
                // ignore free-text terms
                && valueUri != null;
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
    public Filters getFiltersWithInferredAnnotations( Filters f, @Nullable Collection<OntologyTerm> mentionedTerms, @Nullable Collection<OntologyTerm> inferredTerms, long timeout, TimeUnit timeUnit ) throws TimeoutException {
        StopWatch timer = StopWatch.createStarted();
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
                Set<OntologyTerm> terms = ontologyService.getTerms( e.getValue(), Math.max( timeUnit.toMillis( timeout ) - timer.getTime(), 0 ), TimeUnit.MILLISECONDS );
                if ( mentionedTerms != null ) {
                    mentionedTerms.addAll( terms );
                }
                Set<OntologyTerm> c = ontologyService.getChildren( terms, false, true, Math.max( timeUnit.toMillis( timeout ) - timer.getTime(), 0 ), TimeUnit.MILLISECONDS );
                if ( inferredTerms != null ) {
                    inferredTerms.addAll( terms );
                    inferredTerms.addAll( c );
                }
                Set<String> termAndChildrenUris = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );
                termAndChildrenUris.addAll( e.getValue() );
                termAndChildrenUris.addAll( c.stream()
                        .map( OntologyTerm::getUri )
                        .collect( Collectors.toList() ) );
                for ( List<String> termAndChildrenUrisBatch : org.apache.commons.collections4.ListUtils.partition( new ArrayList<>( termAndChildrenUris ), QueryUtils.MAX_PARAMETER_LIST_SIZE ) ) {
                    Filter g;
                    if ( termAndChildrenUrisBatch.size() == 1 ) {
                        g = Filter.by( e.getKey().getObjectAlias(), e.getKey().getPropertyName(), String.class, Filter.Operator.eq, termAndChildrenUrisBatch.iterator().next(), e.getKey().getOriginalProperty() );
                    } else if ( termAndChildrenUris.size() > 1 ) {
                        g = Filter.by( e.getKey().getObjectAlias(), e.getKey().getPropertyName(), String.class, Filter.Operator.in, termAndChildrenUrisBatch, e.getKey().getOriginalProperty() );
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

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadAndThaw( Long id ) {
        ExpressionExperiment ee = load( id );
        if ( ee != null ) {
            this.expressionExperimentDao.thaw( ee );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadAndThawWithRefreshCacheMode( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id, CacheMode.REFRESH );
        if ( ee != null ) {
            this.expressionExperimentDao.thaw( ee );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadAndThawLiteWithRefreshCacheMode( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id, CacheMode.REFRESH );
        if ( ee != null ) {
            this.expressionExperimentDao.thawWithoutVectors( ee );
        }
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
    public long countWithCache( @Nullable Filters filters, @Nullable Set<Long> extraIds ) {
        if ( extraIds != null ) {
            List<Long> eeIds = loadIdsWithCache( filters, null );
            eeIds.retainAll( extraIds );
            return eeIds.size();
        }
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
    public Map<Characteristic, Long> getCategoriesUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, @Nullable Collection<String> retainedTermUris, int maxResults ) {
        Collection<Long> eeIds;
        if ( filters == null || filters.isEmpty() ) {
            eeIds = extraIds;
        } else {
            eeIds = expressionExperimentDao.loadIdsWithCache( filters, null );
            if ( extraIds != null ) {
                eeIds.retainAll( extraIds );
            }
        }
        if ( excludedTermUris != null ) {
            try {
                excludedTermUris = inferTermsUris( excludedTermUris, 30000 );
            } catch ( TimeoutException e ) {
                log.warn( "Inference for excluded terms too too much time to compute, will only use the original set of terms." );
            }
        }
        return expressionExperimentDao.getCategoriesUsageFrequency( eeIds, excludedCategoryUris, excludedTermUris, retainedTermUris, maxResults );
    }

    /**
     * If the term cannot be resolved via {@link OntologyService#getTerm(String, long, TimeUnit)}, an attempt is done to
     * resolve its category and assign it as its parent. This handles free-text terms that lack a value URI.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CharacteristicWithUsageStatisticsAndOntologyTerm> getAnnotationsUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds, @Nullable String category, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, int minFrequency, @Nullable Collection<String> retainedTermUris, int maxResults, long timeout, TimeUnit timeUnit ) throws TimeoutException {
        StopWatch timer = StopWatch.createStarted();
        if ( excludedTermUris != null ) {
            try {
                excludedTermUris = inferTermsUris( excludedTermUris, Math.max( timeUnit.toMillis( timeout ) - timer.getTime(), 0 ) );
            } catch ( TimeoutException e ) {
                log.warn( "Inference for excluded terms too too much time to compute, will only use the original set of terms." );
            }
        }

        Collection<Long> eeIds;
        if ( filters == null || filters.isEmpty() ) {
            eeIds = extraIds;
        } else {
            eeIds = expressionExperimentDao.loadIdsWithCache( filters, null );
            if ( extraIds != null ) {
                eeIds.retainAll( extraIds );
            }
        }

        Map<Characteristic, Long> result = expressionExperimentDao.getAnnotationsUsageFrequency( eeIds, null, maxResults, minFrequency, category, excludedCategoryUris, excludedTermUris, retainedTermUris );

        List<CharacteristicWithUsageStatisticsAndOntologyTerm> resultWithParents = new ArrayList<>( result.size() );

        // gather all the values and categories
        Set<String> uris = result.keySet().stream()
                .flatMap( c -> Stream.of( c.getValueUri(), c.getCategoryUri() ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
        Map<String, Set<OntologyTerm>> termByUri = ontologyService.getTerms( uris, Math.max( timeUnit.toMillis( timeout ) - timer.getTime(), 0 ), TimeUnit.MILLISECONDS ).stream()
                .filter( t -> t.getUri() != null ) // should never occur, but better be safe than sorry
                .collect( Collectors.groupingBy( OntologyTerm::getUri, Collectors.toSet() ) );

        for ( Map.Entry<Characteristic, Long> entry : result.entrySet() ) {
            Characteristic c = entry.getKey();
            OntologyTerm term;
            if ( c.getValueUri() != null && termByUri.containsKey( c.getValueUri() ) ) {
                // TODO: handle more than one term per URI
                term = termByUri.get( c.getValueUri() ).iterator().next();
            } else if ( c.getCategoryUri() != null && termByUri.containsKey( c.getCategoryUri() ) ) {
                term = new OntologyTermSimpleWithCategory( c.getValueUri(), c.getValue(), termByUri.get( c.getCategoryUri() ).iterator().next() );
            } else {
                // create an uncategorized term
                term = new OntologyTermSimpleWithCategory( c.getValueUri(), c.getValue(), null );
            }
            resultWithParents.add( new CharacteristicWithUsageStatisticsAndOntologyTerm( entry.getKey(), entry.getValue(), term ) );
        }

        // sort in descending order
        resultWithParents.sort( Comparator.comparing( CharacteristicWithUsageStatisticsAndOntologyTerm::getNumberOfExpressionExperiments, Comparator.reverseOrder() ) );

        return resultWithParents;
    }

    /**
     * Infer all the implied terms from the given collection of term URIs.
     */
    private Set<String> inferTermsUris( Collection<String> termUris, long timeoutMs ) throws TimeoutException {
        StopWatch timer = StopWatch.createStarted();
        Set<String> excludedTermUris = new HashSet<>( termUris );
        // null is a special indicator for free-text terms or categories
        boolean removedFreeText = excludedTermUris.remove( FREE_TEXT );
        boolean removedUncategorized = excludedTermUris.remove( UNCATEGORIZED );
        // expand exclusions with implied terms via subclass relation
        Set<OntologyTerm> excludedTerms = ontologyService.getTerms( excludedTermUris, Math.max( timeoutMs - timer.getTime(), 0 ), TimeUnit.MILLISECONDS );
        // exclude terms using the subClass relation
        Set<OntologyTerm> impliedTerms = ontologyService.getChildren( excludedTerms, false, false, Math.max( timeoutMs - timer.getTime(), 0 ), TimeUnit.MILLISECONDS );
        for ( OntologyTerm t : impliedTerms ) {
            excludedTermUris.add( t.getUri() );
        }
        if ( removedFreeText ) {
            excludedTermUris.add( FREE_TEXT );
        }
        if ( removedUncategorized ) {
            excludedTermUris.add( UNCATEGORIZED );
        }
        return excludedTermUris;
    }

    /**
     * Extension of {@link OntologyTermSimple} that adds a category term as unique parent.
     */
    private static class OntologyTermSimpleWithCategory extends OntologyTermSimple {

        @Nullable
        private final OntologyTerm categoryTerm;

        public OntologyTermSimpleWithCategory( @Nullable String uri, String term, @Nullable OntologyTerm categoryTerm ) {
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
                return Stream.concat( Stream.of( categoryTerm ), Stream.of( categoryTerm ).flatMap( t -> t.getParents( false, includeAdditionalProperties, keepObsoletes ).stream() ) )
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
    public Collection<Gene> getGenesUsedByPreferredVectors( ExpressionExperiment experimentConstraint ) {
        return this.expressionExperimentDao.getGenesUsedByPreferredVectors( experimentConstraint );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<TechnologyType, Long> getTechnologyTypeUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds ) {
        if ( filters == null || filters.isEmpty() ) {
            if ( extraIds != null ) {
                return expressionExperimentDao.getTechnologyTypeUsageFrequency( extraIds );
            } else {
                return expressionExperimentDao.getTechnologyTypeUsageFrequency();
            }
        } else {
            List<Long> ids = this.expressionExperimentDao.loadIdsWithCache( filters, null );
            if ( extraIds != null ) {
                ids.retainAll( extraIds );
            }
            return expressionExperimentDao.getTechnologyTypeUsageFrequency( ids );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ArrayDesign, Long> getArrayDesignUsedOrOriginalPlatformUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds, int maxResults ) {
        Map<ArrayDesign, Long> result;
        if ( filters == null || filters.isEmpty() ) {
            if ( extraIds != null ) {
                result = new HashMap<>( expressionExperimentDao.getArrayDesignsUsageFrequency( extraIds, maxResults ) );
                for ( Map.Entry<ArrayDesign, Long> e : expressionExperimentDao.getOriginalPlatformsUsageFrequency( extraIds, maxResults ).entrySet() ) {
                    result.compute( e.getKey(), ( k, v ) -> ( v != null ? v : 0L ) + e.getValue() );
                }
            } else {
                result = new HashMap<>( expressionExperimentDao.getArrayDesignsUsageFrequency( maxResults ) );
                for ( Map.Entry<ArrayDesign, Long> e : expressionExperimentDao.getOriginalPlatformsUsageFrequency( maxResults ).entrySet() ) {
                    result.compute( e.getKey(), ( k, v ) -> ( v != null ? v : 0L ) + e.getValue() );
                }
            }
        } else {
            List<Long> ids = this.expressionExperimentDao.loadIdsWithCache( filters, null );
            if ( extraIds != null ) {
                ids.retainAll( extraIds );
            }
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
    public Map<Taxon, Long> getTaxaUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds ) {
        if ( filters == null || filters.isEmpty() ) {
            if ( extraIds != null ) {
                return expressionExperimentDao.getPerTaxonCount( extraIds );
            } else {
                return expressionExperimentDao.getPerTaxonCount();
            }
        } else {
            List<Long> ids = this.expressionExperimentDao.loadIdsWithCache( filters, null );
            if ( extraIds != null ) {
                ids.retainAll( extraIds );
            }
            return expressionExperimentDao.getPerTaxonCount( ids );
        }
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
    public boolean hasProcessedExpressionData( ExpressionExperiment ee ) {
        return expressionExperimentDao.hasProcessedExpressionData( ee );
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
        ee = ensureInSession( ee );

        if ( !securityService.isEditable( ee ) ) {
            throw new SecurityException(
                    "Error performing 'ExpressionExperimentService.remove(ExpressionExperiment expressionExperiment)' --> "
                            + " You do not have permission to edit this experiment." );
        }

        // check if a dataset has coexpression links
        if ( this.coexpressionService.hasLinks( ee ) ) {
            throw new IllegalStateException( ee + " has coexpression links, those must be removed first with 'gemma-cli coexpAnalyze -delete'." );
        }

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
        this.expressionExperimentSetService.removeFromSets( ee );

        super.remove( ee );
    }

    @Override
    @Transactional
    public void remove( Collection<ExpressionExperiment> entities ) {
        entities.forEach( this::remove );
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
        return ev == null || !( ev.getEventType() instanceof UnsuitableForDifferentialExpressionAnalysisEvent );
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