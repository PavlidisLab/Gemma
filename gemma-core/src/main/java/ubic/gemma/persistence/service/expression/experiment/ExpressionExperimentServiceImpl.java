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
import ubic.basecode.ontology.simple.OntologyTermSimple;
import ubic.gemma.core.analysis.expression.diff.BaselineSelection;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.common.description.*;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
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
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.persistence.util.Thaws;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.model.common.description.CharacteristicUtils.*;
import static ubic.gemma.model.expression.experiment.StatementUtils.formatStatement;

/**
 * @author pavlidis
 * @author keshav
 * @see ExpressionExperimentService
 */
@Service("expressionExperimentService")
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
    private CoexpressionAnalysisService coexpressionAnalysisService;
    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;
    @Autowired
    private BlacklistedEntityService blacklistedEntityService;
    @Autowired
    private CoexpressionService coexpressionService;
    @Autowired
    private ExpressionExperimentFilterRewriteHelperService filterRewriteService;

    @Autowired
    public ExpressionExperimentServiceImpl( ExpressionExperimentDao expressionExperimentDao ) {
        super( expressionExperimentDao );
        this.expressionExperimentDao = expressionExperimentDao;
    }

    @Override
    @Nonnull
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
    @Transactional(readOnly = true)
    public ExpressionExperiment loadWithAuditTrail( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            Hibernate.initialize( ee.getAuditTrail() );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> loadTroubledIds() {
        return expressionExperimentDao.loadTroubledIds();
    }

    @Override
    @Transactional(readOnly = true)
    public SortedMap<String, String> loadAllIdentifiersAndName( boolean includeNames ) {
        List<ExpressionExperimentDao.Identifiers> allIds = expressionExperimentDao.loadAllIdentifiers();
        TreeMap<String, String> finalIds = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
        populateIdentifierMap( allIds, identifiers -> String.valueOf( identifiers.getId() ), finalIds );
        populateIdentifierMap( allIds, ExpressionExperimentDao.Identifiers::getShortName, finalIds );
        populateIdentifierMap( allIds, ExpressionExperimentDao.Identifiers::getAccession, finalIds );
        if ( includeNames ) {
            populateIdentifierMap( allIds, ExpressionExperimentDao.Identifiers::getName, finalIds );
        }
        return finalIds;
    }

    private void populateIdentifierMap( Collection<ExpressionExperimentDao.Identifiers> identifiers,
            Function<ExpressionExperimentDao.Identifiers, String> extractor, Map<String, String> identifierMap ) {
        Map<String, String> eeIds = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
        Set<String> ambiguousIdentifiers = new HashSet<>();
        for ( ExpressionExperimentDao.Identifiers ids : identifiers ) {
            String id = extractor.apply( ids );
            if ( id == null ) {
                continue;
            }
            if ( identifierMap.containsKey( id ) ) {
                // this indicates that there is already a higher-priority identifier for this EE
                continue;
            }
            if ( eeIds.put( id, ids.getName() ) != null ) {
                // another EE has the same ID
                ambiguousIdentifiers.add( id );
            }
        }
        ambiguousIdentifiers.forEach( eeIds::remove );
        log.info( "Removed " + ambiguousIdentifiers.size() + " ambiguous identifiers." );
        identifierMap.putAll( eeIds );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment reload( ExpressionExperiment ee ) {
        return expressionExperimentDao.reload( ee );
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
        if ( experiment.getExperimentalDesign() == null ) {
            log.info( "Creating missing experimental design for " + experiment );
            experiment.setExperimentalDesign( new ExperimentalDesign() );
        }
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
        if ( experiment.getExperimentalDesign() == null ) {
            log.info( "Creating missing experimental design for " + experiment );
            experiment.setExperimentalDesign( new ExperimentalDesign() );
        }
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
        if ( experiment.getExperimentalDesign() == null ) {
            log.info( "Creating missing experimental design for " + experiment );
            experiment.setExperimentalDesign( new ExperimentalDesign() );
        }
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
    @Transactional(readOnly = true)
    public Collection<RawExpressionDataVector> getRawDataVectors( ExpressionExperiment ee, QuantitationType qt ) {
        return expressionExperimentDao.getRawDataVectors( ee, qt );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<RawExpressionDataVector> getRawDataVectors( ExpressionExperiment ee, List<BioAssay> samples, QuantitationType qt ) {
        return expressionExperimentDao.getRawDataVectors( ee, samples, qt );
    }

    @Override
    @Transactional
    public int addRawDataVectors( ExpressionExperiment ee,
            QuantitationType quantitationType,
            Collection<RawExpressionDataVector> newVectors ) {
        createDimensionIfNecessary( newVectors );
        createQuantitationTypeIfNecessary( newVectors, RawExpressionDataVector.class );
        return expressionExperimentDao.addRawDataVectors( ee, quantitationType, newVectors );
    }

    @Override
    @Transactional
    public int replaceRawDataVectors( ExpressionExperiment ee, QuantitationType qt, Collection<RawExpressionDataVector> vectors ) {
        createDimensionIfNecessary( vectors );
        return expressionExperimentDao.replaceRawDataVectors( ee, qt, vectors );
    }

    @Override
    @Transactional
    public int replaceAllRawDataVectors( ExpressionExperiment ee,
            Collection<RawExpressionDataVector> newVectors ) {
        if ( newVectors.isEmpty() ) {
            throw new UnsupportedOperationException( "Only use this method for replacing vectors, not erasing them" );
        }

        Set<QuantitationType> existingQts = ee.getRawExpressionDataVectors().stream()
                .map( DataVector::getQuantitationType )
                .collect( Collectors.toSet() );

        Set<QuantitationType> newQts = newVectors.stream()
                .map( RawExpressionDataVector::getQuantitationType )
                .collect( Collectors.toSet() );

        Set<QuantitationType> preferredQts = newQts.stream()
                .filter( QuantitationType::getIsPreferred )
                .collect( Collectors.toSet() );
        if ( preferredQts.size() > 1 ) {
            throw new IllegalArgumentException( "There must be exactly one preferred quantitation type." );
        }

        // group the vectors up by QT
        Map<QuantitationType, Set<RawExpressionDataVector>> vectorsByQt = newVectors.stream()
                .collect( Collectors.groupingBy( RawExpressionDataVector::getQuantitationType, Collectors.toSet() ) );

        int replaced = 0;
        for ( Map.Entry<QuantitationType, Set<RawExpressionDataVector>> e : vectorsByQt.entrySet() ) {
            if ( existingQts.contains( e.getKey() ) ) {
                replaced += replaceRawDataVectors( ee, e.getKey(), e.getValue() );
            } else {
                replaced += addRawDataVectors( ee, e.getKey(), e.getValue() );
            }
        }

        for ( QuantitationType qt : existingQts ) {
            if ( !newQts.contains( qt ) ) {
                removeRawDataVectors( ee, qt );
            }
        }

        return replaced;
    }

    @Override
    @Transactional
    public int removeAllRawDataVectors( ExpressionExperiment ee ) {
        return expressionExperimentDao.removeAllRawDataVectors( ee );
    }

    @Override
    @Transactional
    public int removeRawDataVectors( ExpressionExperiment ee, QuantitationType qt ) {
        return removeRawDataVectors( ee, qt, false );
    }

    @Override
    @Transactional
    public int removeRawDataVectors( ExpressionExperiment ee, QuantitationType qt, boolean keepDimension ) {
        return expressionExperimentDao.removeRawDataVectors( ee, qt, keepDimension );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Collection<ProcessedExpressionDataVector>> getProcessedDataVectors( ExpressionExperiment ee ) {
        return Optional.ofNullable( expressionExperimentDao.getProcessedDataVectors( ee ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Collection<ProcessedExpressionDataVector>> getProcessedDataVectors( ExpressionExperiment ee, List<BioAssay> assays ) {
        return Optional.ofNullable( expressionExperimentDao.getProcessedDataVectors( ee, assays ) );
    }

    @Override
    @Transactional
    public int createProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors ) {
        createDimensionIfNecessary( vectors );
        createQuantitationTypeIfNecessary( vectors, ProcessedExpressionDataVector.class );
        return expressionExperimentDao.createProcessedDataVectors( ee, vectors );
    }

    @Override
    @Transactional
    public int removeProcessedDataVectors( ExpressionExperiment ee ) {
        return expressionExperimentDao.removeProcessedDataVectors( ee );
    }

    @Override
    @Transactional
    public int replaceProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors ) {
        createDimensionIfNecessary( vectors );
        // unlike raw vectors, the "new" processed vectors might use a different QT
        createQuantitationTypeIfNecessary( vectors, ProcessedExpressionDataVector.class );
        return expressionExperimentDao.replaceProcessedDataVectors( ee, vectors );
    }

    private void createDimensionIfNecessary( Collection<? extends BulkExpressionDataVector> vectors ) {
        Collection<BioAssayDimension> dimension = vectors.stream()
                .map( BulkExpressionDataVector::getBioAssayDimension )
                .collect( Collectors.toSet() );
        if ( dimension.size() != 1 ) {
            throw new IllegalArgumentException( "Vectors must share a common bioassay dimension" );
        }
        BioAssayDimension bad = dimension.iterator().next();
        if ( bad.getId() == null ) {
            log.info( "Creating " + bad + "..." );
            bad = this.bioAssayDimensionService.findOrCreate( bad );
            for ( BulkExpressionDataVector vector : vectors ) {
                vector.setBioAssayDimension( bad );
            }
        }
    }

    private <T extends DataVector> void createQuantitationTypeIfNecessary( Collection<T> vectors, Class<? extends DataVector> vectorType ) {
        Set<QuantitationType> quantitationType = vectors.stream()
                .map( DataVector::getQuantitationType )
                .collect( Collectors.toSet() );
        if ( quantitationType.size() != 1 ) {
            throw new IllegalArgumentException( "Vectors must share a common quantitation type." );
        }
        QuantitationType qt = quantitationType.iterator().next();
        if ( qt.getId() == null ) {
            log.info( "Creating " + qt + "..." );
            qt = quantitationTypeService.create( qt, vectorType );
            for ( DataVector vector : vectors ) {
                vector.setQuantitationType( qt );
            }
        }
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
    public ExpressionExperiment loadWithPrimaryPublicationAndOtherRelevantPublications( Long id ) {
        ExpressionExperiment ee = load( id );
        if ( ee != null ) {
            if ( ee.getPrimaryPublication() != null ) {
                Hibernate.initialize( ee.getPrimaryPublication() );
                Hibernate.initialize( ee.getPrimaryPublication().getMeshTerms() );
                Hibernate.initialize( ee.getPrimaryPublication().getChemicals() );
                Hibernate.initialize( ee.getPrimaryPublication().getKeywords() );
            }
            Set<BibliographicReference> pubs = ee.getOtherRelevantPublications();

            for ( BibliographicReference pub : pubs ) {
                Hibernate.initialize( pub );
                Hibernate.initialize( pub.getMeshTerms() );
                Hibernate.initialize( pub.getChemicals() );
                Hibernate.initialize( pub.getKeywords() );
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

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findOneByAccession( String accession ) {
        return this.expressionExperimentDao.findOneByAccession( accession );
    }

    /**
     * @see ExpressionExperimentService#findByBibliographicReference(BibliographicReference)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByBibliographicReference( final BibliographicReference bibRef ) {
        return this.expressionExperimentDao.findByBibliographicReference( bibRef );
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
    public Collection<ExpressionExperiment> findByBioMaterial( final BioMaterial bm ) {
        return this.expressionExperimentDao.findByBioMaterial( bm );
    }

    @Override
    public Map<ExpressionExperiment, Collection<BioMaterial>> findByBioMaterials( Collection<BioMaterial> biomaterials ) {
        return this.expressionExperimentDao.findByBioMaterials( biomaterials );
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

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByDesignId( Long designId ) {
        return this.expressionExperimentDao.findByDesignId( designId );
    }

    /**
     * @see ExpressionExperimentService#findByFactor(ExperimentalFactor)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByFactor( final ExperimentalFactor factor ) {
        return this.expressionExperimentDao.findByFactor( factor );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByFactors( Collection<ExperimentalFactor> factors ) {
        return this.expressionExperimentDao.findByFactors( factors );
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
    public Collection<ExpressionExperiment> findByFactorValues( final Collection<FactorValue> factorValues ) {
        return this.expressionExperimentDao.findByFactorValues( factorValues );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByFactorValueIds( Collection<Long> factorValueIds ) {
        return this.expressionExperimentDao.findByFactorValueIds( factorValueIds );
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
    public ExpressionExperiment findOneByName( String name ) {
        return expressionExperimentDao.findOneByName( name );
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

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByShortNameAndThawLite( String shortName ) {
        ExpressionExperiment ee = this.expressionExperimentDao.findByShortName( shortName );
        if ( ee != null ) {
            expressionExperimentDao.thawLite( ee );
        }
        return ee;
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
    public Set<AnnotationValueObject> getAnnotations( ExpressionExperiment expressionExperiment ) {
        Set<AnnotationValueObject> annotations = new LinkedHashSet<>();
        Set<String> seenTerms = new HashSet<>();

        expressionExperimentDao.getExperimentAnnotations( expressionExperiment, false ).stream()
                .filter( this::filterExperimentAnnotations )
                .map( c -> new AnnotationValueObject( c, ExpressionExperiment.class ) )
                .forEach( c -> addIfNovel( annotations, c, seenTerms ) );

        expressionExperimentDao.getExperimentSubSetAnnotations( expressionExperiment ).stream()
                .filter( this::filterSubSetAnnotations )
                .map( c -> new AnnotationValueObject( c, ExpressionExperimentSubSet.class ) )
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

    @Override
    @Transactional(readOnly = true)
    public Set<AnnotationValueObject> getAnnotations( ExpressionExperimentSubSet ee ) {
        Set<AnnotationValueObject> annotations = new HashSet<>();
        Set<String> seenTerms = new HashSet<>();

        // inherited from the EE
        expressionExperimentDao.getExperimentAnnotations( ee.getSourceExperiment(), false ).stream()
                .filter( this::filterExperimentAnnotations )
                .map( c -> new AnnotationValueObject( c, ExpressionExperiment.class ) )
                .forEach( c -> addIfNovel( annotations, c, seenTerms ) );

        // specifically for the subset
        ee.getCharacteristics().stream()
                .filter( this::filterSubSetAnnotations )
                .map( c -> new AnnotationValueObject( c, ExpressionExperimentSubSet.class ) )
                .forEach( c -> addIfNovel( annotations, c, seenTerms ) );

        String[] ignoredPredicates = new String[] {
                "http://gemma.msl.ubc.ca/ont/TGEMO_00166", // duration
                "http://gemma.msl.ubc.ca/ont/TGEMO_00167", // dose
                "http://gemma.msl.ubc.ca/ont/TGEMO_00168"  // development stage
        };
        expressionExperimentDao.getFactorValueAnnotations( ee ).stream()
                .filter( this::filterFactorValueAnnotation )
                .map( c -> new AnnotationValueObject( c.getCategoryUri(), c.getCategory(), c.getSubjectUri(), formatStatement( c, ignoredPredicates ), FactorValue.class ) )
                .forEach( c -> addIfNovel( annotations, c, seenTerms ) );

        expressionExperimentDao.getBioMaterialAnnotations( ee ).stream()
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

    private boolean filterExperimentAnnotations( Characteristic c ) {
        return filterAnnotation( c );
    }

    private boolean filterSubSetAnnotations( Characteristic c ) {
        return filterAnnotation( c );
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
                && !BaselineSelection.isBaselineCondition( c ) && !hasCategory( c, Categories.BLOCK )
                // ignore timepoints
                && !"http://www.ebi.ac.uk/efo/EFO_0000724".equals( c.getCategoryUri() )
                // DE_include/exclude
                && !"http://gemma.msl.ubc.ca/ont/TGEMO_00013".equals( c.getSubjectUri() )
                && !"http://gemma.msl.ubc.ca/ont/TGEMO_00014".equals( c.getSubjectUri() );// ignore baseline conditions
// ignore batch factors
// ignore timepoints
// DE_include/exclude
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

    @Override
    public Filters getEnhancedFilters( Filters f, @Nullable Collection<OntologyTerm> mentionedTerms, @Nullable Collection<OntologyTerm> inferredTerms, long timeout, TimeUnit timeUnit ) throws TimeoutException {
        // do the inference first, some of the terms that we *duplicate* for a second property are subject to inference
        f = filterRewriteService.getFiltersWithInferredAnnotations( f, "ee", mentionedTerms, inferredTerms, timeout, timeUnit );
        f = filterRewriteService.getFiltersWithAdditionalProperties( f );
        return f;
    }

    @Override
    public String getFilterablePropertyDescription( String property ) {
        String desc = super.getFilterablePropertyDescription( property );
        if ( filterRewriteService.supportsInferredAnnotations( property ) ) {
            return "will be expanded with ontology inference" + ( desc != null ? "; " + desc : "" );
        }
        return desc;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BioAssay, Long> getNumberOfDesignElementsPerSample( ExpressionExperiment expressionExperiment ) {
        return expressionExperimentDao.getNumberOfDesignElementsPerSample( expressionExperiment );
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
    public <T extends Exception> ExpressionExperiment loadAndThawLiteOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T {
        ExpressionExperiment ee = loadOrFail( id, exceptionSupplier, message );
        this.expressionExperimentDao.thawLite( ee );
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> ExpressionExperiment loadAndThawLiteOrFail( Long id, Function<String, T> exceptionSupplier ) throws T {
        ExpressionExperiment ee = loadOrFail( id, exceptionSupplier );
        this.expressionExperimentDao.thawLite( ee );
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> ExpressionExperiment loadAndThawLiterOrFail( Long id, Function<String, T> exceptionSupplier ) throws T {
        ExpressionExperiment ee = loadOrFail( id, exceptionSupplier );
        this.expressionExperimentDao.thawLiter( ee );
        return ee;
    }

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
    public ExpressionExperiment loadAndThawLite( Long id ) {
        ExpressionExperiment ee = load( id );
        if ( ee != null ) {
            this.expressionExperimentDao.thawLite( ee );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadAndThawLiteWithRefreshCacheMode( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id, CacheMode.REFRESH );
        if ( ee != null ) {
            this.expressionExperimentDao.thawLite( ee );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> ExpressionExperiment loadAndThawOrFail( Long id, Function<String, T> exceptionSupplier ) throws T {
        ExpressionExperiment ee = loadOrFail( id, exceptionSupplier );
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
    public List<CharacteristicWithUsageStatisticsAndOntologyTerm> getAnnotationsUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds, @Nullable String category, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, int minFrequency, @Nullable Collection<String> retainedTermUris, int maxResults, boolean includePredicates, boolean includeObjects, long timeout, TimeUnit timeUnit ) throws TimeoutException {
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

        Map<Characteristic, Long> result = expressionExperimentDao.getAnnotationsUsageFrequency( eeIds, null, maxResults, minFrequency, category, excludedCategoryUris, excludedTermUris, retainedTermUris, includePredicates, includeObjects );

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
            //noinspection DataFlowIssue
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
    public Collection<ArrayDesign> getArrayDesignsUsed( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getArrayDesignsUsed( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> getArrayDesignsUsed( ExpressionExperiment ee, QuantitationType qt ) {
        Class<? extends DataVector> dvt = quantitationTypeService.getDataVectorType( qt );
        if ( dvt == null ) {
            log.warn( "There are no vectors associated to " + qt + " in " + ee + ", will return no platforms." );
            return Collections.emptySet();
        }
        return this.expressionExperimentDao.getArrayDesignsUsed( ee, qt, dvt );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> getArrayDesignsUsed( ExpressionExperiment ee, QuantitationType qt, Class<? extends DataVector> vectorType ) {
        return this.expressionExperimentDao.getArrayDesignsUsed( ee, qt, vectorType );
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
        bioAssayDimensions.forEach( Thaws::thawBioAssayDimension );
        return bioAssayDimensions;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssayDimension> getBioAssayDimensionsFromSubSets( ExpressionExperiment expressionExperiment ) {
        Collection<BioAssayDimension> bioAssayDimensions = this.expressionExperimentDao
                .getBioAssayDimensionsFromSubSets( expressionExperiment );
        bioAssayDimensions.forEach( Thaws::thawBioAssayDimension );
        return bioAssayDimensions;
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssayDimension getBioAssayDimension( ExpressionExperiment ee, QuantitationType qt, Class<? extends BulkExpressionDataVector> dataVectorType ) {
        return expressionExperimentDao.getBioAssayDimension( ee, qt, dataVectorType );
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssayDimension getBioAssayDimension( ExpressionExperiment ee, QuantitationType qt ) {
        return expressionExperimentDao.getBioAssayDimension( ee, qt );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssayDimension> getBioAssayDimensionsWithAssays( ExpressionExperiment ee, QuantitationType qt ) {
        Collection<BioAssayDimension> bad = expressionExperimentDao.getBioAssayDimensions( ee, qt );
        bad.forEach( Thaws::thawBioAssayDimension );
        return bad;
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssayDimension getBioAssayDimensionById( ExpressionExperiment ee, Long dimensionId, Class<? extends BulkExpressionDataVector> dataVectorType ) {
        return expressionExperimentDao.getBioAssayDimensionById( ee, dimensionId, dataVectorType );
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssayDimension getBioAssayDimensionById( ExpressionExperiment ee, Long dimensionId ) {
        for ( Class<? extends BulkExpressionDataVector> vectorType : quantitationTypeService.getMappedDataVectorType( BulkExpressionDataVector.class ) ) {
            BioAssayDimension bad = expressionExperimentDao.getBioAssayDimensionById( ee, dimensionId, vectorType );
            if ( bad != null ) {
                return bad;
            }
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public long getBioMaterialCount( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getBioMaterialCount( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public long getRawDataVectorCount( final ExpressionExperiment ee ) {
        return this.expressionExperimentDao.getRawDataVectorCount( ee );
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
    public Optional<QuantitationType> getPreferredQuantitationType( final ExpressionExperiment ee ) {
        return Optional.ofNullable( this.expressionExperimentDao.getPreferredQuantitationType( ee ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuantitationType> getProcessedQuantitationType( final ExpressionExperiment ee ) {
        return Optional.ofNullable( this.expressionExperimentDao.getProcessedQuantitationType( ee ) );
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
        return this.quantitationTypeService.findByExpressionExperiment( expressionExperiment ).values().stream()
                .flatMap( Collection::stream )
                .collect( Collectors.toSet() );
    }


    @Override
    @Transactional(readOnly = true)
    public Map<Class<? extends DataVector>, Set<QuantitationType>> getQuantitationTypesByVectorType( ExpressionExperiment ee ) {
        return this.quantitationTypeService.findByExpressionExperiment( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        return this.quantitationTypeService.findByExpressionExperimentAndDimension( expressionExperiment, dimension );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment, BioAssayDimension dimension, Class<? extends BulkExpressionDataVector> dataVectorType ) {
        return quantitationTypeService.findByExpressionExperimentAndDimension( expressionExperiment, dimension, Collections.singleton( dataVectorType ) );
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
    public Collection<ExpressionExperimentSubSet> getSubSetsWithBioAssays( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getSubSets( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> getSubSetsWithCharacteristics( ExpressionExperiment ee ) {
        Collection<ExpressionExperimentSubSet> result = this.expressionExperimentDao.getSubSets( ee );
        for ( ExpressionExperimentSubSet subSet : result ) {
            Hibernate.initialize( subSet.getCharacteristics() );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> getSubSetsByDimension( ExpressionExperiment expressionExperiment ) {
        return expressionExperimentDao.getSubSetsByDimension( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> getSubSetsByDimensionWithBioAssays( ExpressionExperiment expressionExperiment ) {
        Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> result = expressionExperimentDao.getSubSetsByDimension( expressionExperiment );
        for ( Set<ExpressionExperimentSubSet> subSets : result.values() ) {
            for ( ExpressionExperimentSubSet s : subSets ) {
                for ( BioAssay ba : s.getBioAssays() ) {
                    Hibernate.initialize( ba.getSampleUsed() );
                    Hibernate.initialize( ba.getSampleUsed().getSourceBioMaterial() );
                }
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        return expressionExperimentDao.getSubSets( expressionExperiment, dimension );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> getSubSetsWithBioAssays( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        Collection<ExpressionExperimentSubSet> subSets = expressionExperimentDao.getSubSets( expressionExperiment, dimension );
        for ( ExpressionExperimentSubSet s : subSets ) {
            for ( BioAssay ba : s.getSourceExperiment().getBioAssays() ) {
                Hibernate.initialize( ba.getSampleUsed() );
                Hibernate.initialize( ba.getSampleUsed().getSourceBioMaterial() );
            }
            for ( BioAssay ba : s.getBioAssays() ) {
                Hibernate.initialize( ba.getSampleUsed() );
                Hibernate.initialize( ba.getSampleUsed().getSourceBioMaterial() );
            }
        }
        return subSets;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExperimentalFactor, Map<FactorValue, ExpressionExperimentSubSet>> getSubSetsByFactorValue( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        return getSubSetsByFactorValueInternal( getSubSetsWithBioAssays( expressionExperiment, dimension ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<FactorValue, ExpressionExperimentSubSet> getSubSetsByFactorValue( ExpressionExperiment expressionExperiment, ExperimentalFactor experimentalFactor, BioAssayDimension dimension ) {
        // TODO: could this be made more efficient for a single factor?
        return getSubSetsByFactorValueInternal( getSubSetsWithBioAssays( expressionExperiment, dimension ) )
                .get( experimentalFactor );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<FactorValue, ExpressionExperimentSubSet> getSubSetsByFactorValueWithCharacteristicsAndBioAssays( ExpressionExperiment expressionExperiment, ExperimentalFactor experimentalFactor, BioAssayDimension dimension ) {
        Map<FactorValue, ExpressionExperimentSubSet> result;
        result = getSubSetsByFactorValue( expressionExperiment, experimentalFactor, dimension );
        if ( result != null ) {
            for ( ExpressionExperimentSubSet subSet : result.values() ) {
                Hibernate.initialize( subSet.getCharacteristics() );
                for ( BioAssay ba : subSet.getBioAssays() ) {
                    Thaws.thawBioAssay( ba );
                }
            }
        }
        return result;
    }

    private Map<ExperimentalFactor, Map<FactorValue, ExpressionExperimentSubSet>> getSubSetsByFactorValueInternal( Collection<ExpressionExperimentSubSet> subSets ) {
        Map<ExperimentalFactor, Map<FactorValue, Set<ExpressionExperimentSubSet>>> result = new HashMap<>();
        for ( ExpressionExperimentSubSet subSet : subSets ) {
            for ( BioAssay ba : subSet.getBioAssays() ) {
                for ( FactorValue fv : ba.getSampleUsed().getAllFactorValues() ) {
                    result.computeIfAbsent( fv.getExperimentalFactor(), k -> new HashMap<>() )
                            .computeIfAbsent( fv, k -> new HashSet<>() )
                            .add( subSet );
                }
            }
        }
        return result.entrySet().stream()
                // only retain FVs that fully separates subsets
                // if there are as many FVs than subsets, we know there is exactly one subset per FV
                .filter( e -> e.getValue().size() == subSets.size() )
                .collect( Collectors.toMap( Map.Entry::getKey, e -> e.getValue().entrySet().stream().collect( Collectors.toMap( Map.Entry::getKey, e2 -> e2.getValue().iterator().next() ) ) ) );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentSubSet getSubSetByIdWithCharacteristics( ExpressionExperiment ee, Long subSetId ) {
        ExpressionExperimentSubSet result = expressionExperimentDao.getSubSetById( ee, subSetId );
        if ( result != null ) {
            Hibernate.initialize( result.getCharacteristics() );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentSubSet getSubSetByIdWithCharacteristicsAndBioAssays( ExpressionExperiment ee, Long subSetId ) {
        ExpressionExperimentSubSet result = expressionExperimentDao.getSubSetById( ee, subSetId );
        if ( result != null ) {
            result.getSourceExperiment().getBioAssays().forEach( Thaws::thawBioAssay );
            result.getBioAssays().forEach( Thaws::thawBioAssay );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, Taxon> getTaxa( Collection<ExpressionExperiment> ees ) {
        return this.expressionExperimentDao.getTaxa( ees );
    }

    @Override
    @Transactional(readOnly = true)
    public Taxon getTaxon( final ExpressionExperiment ee ) {
        return this.expressionExperimentDao.getTaxon( ee );
    }

    @Override
    public boolean isSingleCell( ExpressionExperiment ee ) {
        return ( ee.getCharacteristics().stream()
                .anyMatch( c -> hasCategory( c, Categories.ASSAY ) && hasAnyValue( c,
                        Values.SINGLE_NUCLEUS_RNA_SEQUENCING_ASSAY,
                        Values.SINGLE_CELL_RNA_SEQUENCING_ASSAY,
                        Values.RNASEQ_OF_CODING_RNA_FROM_SINGLE_CELLS,
                        Values.SINGLE_NUCLEUS_RNA_SEQUENCING,
                        Values.SINGLE_CELL_RNA_SEQUENCING
                ) )
                // exclude FAC-sorted single-cell datasets
                && ee.getCharacteristics().stream()
                .noneMatch( c -> hasCategory( c, Categories.ASSAY )
                        && hasValue( c, Values.FLUORESCENCE_ACTIVATED_CELL_SORTING ) ) )
                // more expensive, check the presence of SC vectors
                || hasSingleCellData( ee );
    }

    @Override
    public boolean isBulkRNASeq( ExpressionExperiment ee ) {
        return ee.getCharacteristics().stream()
                .anyMatch( c -> hasCategory( c, Categories.ASSAY )
                        && hasAnyValue( c ) )
                ||
                // include FAC-sorted single-cell datasets
                ( ee.getCharacteristics().stream()
                        .anyMatch( c -> hasCategory( c, Categories.ASSAY ) && hasAnyValue( c,
                                Values.SINGLE_NUCLEUS_RNA_SEQUENCING_ASSAY,
                                Values.SINGLE_CELL_RNA_SEQUENCING_ASSAY,
                                Values.RNASEQ_OF_CODING_RNA_FROM_SINGLE_CELLS,
                                Values.SINGLE_NUCLEUS_RNA_SEQUENCING,
                                Values.SINGLE_CELL_RNA_SEQUENCING
                        ) )
                        && ee.getCharacteristics().stream()
                        .anyMatch( c -> hasCategory( c, Categories.ASSAY )
                                && hasValue( c, Values.FLUORESCENCE_ACTIVATED_CELL_SORTING ) ) )
                || hasBulkRnaSeqData( ee );
        // TODO: check the presence of vectors from the RNA-Seq pipeline
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRNASeq( ExpressionExperiment ee ) {
        return ee.getCharacteristics().stream()
                .anyMatch( c -> hasCategory( c, Categories.ASSAY ) && hasAnyValue( c,
                        Values.SINGLE_NUCLEUS_RNA_SEQUENCING_ASSAY,
                        Values.SINGLE_CELL_RNA_SEQUENCING_ASSAY,
                        Values.RNASEQ_OF_CODING_RNA_FROM_SINGLE_CELLS,
                        Values.SINGLE_NUCLEUS_RNA_SEQUENCING,
                        Values.SINGLE_CELL_RNA_SEQUENCING,
                        Values.BULK_RNA_SEQ
                ) )
                || ( !isMicroarray( ee ) && hasSequencingPlatform( ee ) )
                || expressionExperimentDao.hasSingleCellQuantitationTypes( ee )
                || hasBulkRnaSeqData( ee )
                ;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMicroarray( ExpressionExperiment ee ) {
        return ee.getCharacteristics().stream()
                .anyMatch( c -> hasCategory( c, Categories.ASSAY ) && hasValue( c, Values.MICROARRAY ) );
    }

    private boolean hasSequencingPlatform( ExpressionExperiment ee ) {
        Collection<ArrayDesign> ads = this.expressionExperimentDao.getArrayDesignsUsed( ee );
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

    private boolean hasBulkRnaSeqData( ExpressionExperiment ee ) {
        // TODO: check for the presence of vectors from the RNA-Seq pipeline
        return false;
    }

    private boolean hasSingleCellData(ExpressionExperiment ee) {
        return expressionExperimentDao.hasSingleCellQuantitationTypes( ee );
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
    public ExpressionExperiment thawLite( final ExpressionExperiment expressionExperiment ) {
        ExpressionExperiment result = ensureInSession( expressionExperiment );
        this.expressionExperimentDao.thawLite( result );
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thawLiter( final ExpressionExperiment expressionExperiment ) {
        ExpressionExperiment result = ensureInSession( expressionExperiment );
        this.expressionExperimentDao.thawLiter( result );
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

        if ( !securityService.isEditableByCurrentUser( ee ) ) {
            throw new SecurityException(
                    "Error performing 'ExpressionExperimentService.remove(ExpressionExperiment expressionExperiment)' --> "
                            + " You do not have permission to edit this experiment." );
        }

        // check if a dataset has coexpression links
        if ( this.coexpressionService.hasLinks( ee ) ) {
            throw new IllegalStateException( ee + " has coexpression links, those must be removed first with 'gemma-cli coexpAnalyze -delete'." );
        }

        // Remove subsets
        Collection<ExpressionExperimentSubSet> subsets = this.getSubSetsWithBioAssays( ee );
        for ( ExpressionExperimentSubSet subset : subsets ) {
            expressionExperimentSubSetService.remove( subset );
        }

        // Remove differential expression analyses
        this.differentialExpressionAnalysisService.removeForExperiment( ee, true );

        // Remove any sample coexpression matrices
        this.sampleCoexpressionAnalysisService.removeForExperiment( ee );

        // Remove PCA
        this.principalComponentAnalysisService.removeForExperiment( ee );

        // Remove coexpression analyses
        this.coexpressionAnalysisService.removeForExperimentAnalyzed( ee );

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
    public void updateQuantitationType( ExpressionExperiment ee, QuantitationType qt ) {
        Assert.notNull( ee.getId(), "The experiment must be persistent." );
        Assert.notNull( qt.getId(), "The quantitation type must be persistent." );
        // FIXME: hashing depends on properties that might have been altered that would in turn affect hashCode(), so we
        //        cannot use contains
        Assert.isTrue( ee.getQuantitationTypes().stream().anyMatch( qt::equals ),
                "The quantitation type does not belong to " + ee + "." );
        if ( qt.getIsSingleCellPreferred() ) {
            // set all other QTs to non-preferred
            for ( QuantitationType otherQt : ee.getQuantitationTypes() ) {
                if ( otherQt.getIsSingleCellPreferred() && !otherQt.equals( qt ) ) {
                    log.info( "Marking " + otherQt + " as non-preferred for single-cell data." );
                    otherQt.setIsSingleCellPreferred( false );
                    quantitationTypeService.update( otherQt );
                }
            }
        }
        if ( qt.getIsPreferred() ) {
            // set all other QTs to non-preferred
            for ( QuantitationType otherQt : ee.getQuantitationTypes() ) {
                if ( otherQt.getIsPreferred() && !otherQt.equals( qt ) ) {
                    log.info( "Marking " + otherQt + " as non-preferred for raw data." );
                    otherQt.setIsPreferred( false );
                    quantitationTypeService.update( otherQt );
                }
            }
        }
        if ( qt.getIsMaskedPreferred() ) {
            // set all other QTs to non-preferred
            for ( QuantitationType otherQt : ee.getQuantitationTypes() ) {
                if ( otherQt.getIsMaskedPreferred() && !otherQt.equals( qt ) ) {
                    log.info( "Marking " + otherQt + " as non-preferred for processed data." );
                    otherQt.setIsMaskedPreferred( false );
                    quantitationTypeService.update( otherQt );
                }
            }
        }
        quantitationTypeService.update( qt );
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