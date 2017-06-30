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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchConfound;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchConfoundValueObject;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.common.description.*;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.VoEnabledService;
import ubic.gemma.persistence.service.analysis.expression.coexpression.SampleCoexpressionAnalysisDao;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisDao;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventDao;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionDao;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorDao;

import java.util.*;

/**
 * @author pavlidis
 * @author keshav
 * @see ExpressionExperimentService
 */
@Service
@Transactional
public class ExpressionExperimentServiceImpl
        extends VoEnabledService<ExpressionExperiment, ExpressionExperimentValueObject>
        implements ExpressionExperimentService {

    private static final double BATCH_CONFOUND_THRESHOLD = 0.01;

    private final ExpressionExperimentDao expressionExperimentDao;
    private AuditEventDao auditEventDao;
    private BioAssayDimensionDao bioAssayDimensionDao;
    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;
    private DatabaseEntryService databaseEntryService;
    private ExpressionExperimentSetService expressionExperimentSetService;
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;
    private ExperimentalFactorDao experimentalFactorDao;
    private FactorValueDao factorValueDao;
    private RawExpressionDataVectorDao rawExpressionDataVectorDao;
    private OntologyService ontologyService;
    private PrincipalComponentAnalysisService principalComponentAnalysisService;
    private ProcessedExpressionDataVectorService processedVectorService;
    private QuantitationTypeService quantitationTypeDao;
    private SampleCoexpressionAnalysisDao sampleCoexpressionAnalysisDao;
    private SearchService searchService;
    private SecurityService securityService;
    private SVDService svdService;

    @Autowired
    public ExpressionExperimentServiceImpl( ExpressionExperimentDao expressionExperimentDao ) {
        super( expressionExperimentDao );
        this.expressionExperimentDao = expressionExperimentDao;
    }

    @Autowired
    public void setAuditEventDao( AuditEventDao auditEventDao ) {
        this.auditEventDao = auditEventDao;
    }

    @Autowired
    public void setBioAssayDimensionDao( BioAssayDimensionDao bioAssayDimensionDao ) {
        this.bioAssayDimensionDao = bioAssayDimensionDao;
    }

    @Autowired
    public void setDifferentialExpressionAnalysisDao(
            DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao ) {
        this.differentialExpressionAnalysisDao = differentialExpressionAnalysisDao;
    }

    @Autowired
    public void setDatabaseEntryService( DatabaseEntryService databaseEntryService ) {
        this.databaseEntryService = databaseEntryService;
    }

    @Autowired
    public void setExpressionExperimentSetService( ExpressionExperimentSetService expressionExperimentSetService ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
    }

    @Autowired
    public void setExpressionExperimentSubSetService(
            ExpressionExperimentSubSetService expressionExperimentSubSetService ) {
        this.expressionExperimentSubSetService = expressionExperimentSubSetService;
    }

    @Autowired
    public void setExperimentalFactorDao( ExperimentalFactorDao experimentalFactorDao ) {
        this.experimentalFactorDao = experimentalFactorDao;
    }

    @Autowired
    public void setFactorValueDao( FactorValueDao factorValueDao ) {
        this.factorValueDao = factorValueDao;
    }

    @Autowired
    public void setRawExpressionDataVectorDao( RawExpressionDataVectorDao rawExpressionDataVectorDao ) {
        this.rawExpressionDataVectorDao = rawExpressionDataVectorDao;
    }

    @Autowired
    public void setOntologyService( OntologyService ontologyService ) {
        this.ontologyService = ontologyService;
    }

    @Autowired
    public void setPrincipalComponentAnalysisService(
            PrincipalComponentAnalysisService principalComponentAnalysisService ) {
        this.principalComponentAnalysisService = principalComponentAnalysisService;
    }

    @Autowired
    public void setProcessedVectorService( ProcessedExpressionDataVectorService processedVectorService ) {
        this.processedVectorService = processedVectorService;
    }

    @Autowired
    public void setQuantitationTypeDao( QuantitationTypeService quantitationTypeDao ) {
        this.quantitationTypeDao = quantitationTypeDao;
    }

    @Autowired
    public void setSampleCoexpressionAnalysisDao( SampleCoexpressionAnalysisDao sampleCoexpressionAnalysisDao ) {
        this.sampleCoexpressionAnalysisDao = sampleCoexpressionAnalysisDao;
    }

    @Autowired
    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    @Autowired
    public void setSecurityService( SecurityService securityService ) {
        this.securityService = securityService;
    }

    @Autowired
    public void setSvdService( SVDService svdService ) {
        this.svdService = svdService;
    }

    @Override
    public Integer countNotTroubled(){
        return this.expressionExperimentDao.countNotTroubled();
    }

    @Override
    @Transactional
    public ExpressionExperiment addVectors( ExpressionExperiment ee, ArrayDesign ad,
            Collection<RawExpressionDataVector> newVectors ) {

        // ee = this.load( ee.getId() );
        Collection<BioAssayDimension> bads = new HashSet<BioAssayDimension>();
        Collection<QuantitationType> qts = new HashSet<QuantitationType>();
        for ( RawExpressionDataVector vec : newVectors ) {
            bads.add( vec.getBioAssayDimension() );
            qts.add( vec.getQuantitationType() );
        }

        if ( bads.size() > 1 ) {
            throw new IllegalArgumentException( "Vectors must share a common bioassaydimension" );
        }

        if ( qts.size() > 1 ) {
            throw new UnsupportedOperationException(
                    "Can only replace with one type of vector (only one quantitation type)" );
        }

        BioAssayDimension bad = bads.iterator().next();

        bad = this.bioAssayDimensionDao.findOrCreate( bad );
        assert bad.getBioAssays().size() > 0;

        QuantitationType newQt = qts.iterator().next();

        if ( newQt.getId() == null ) {
            newQt = this.quantitationTypeDao.create( newQt );
        } else {
            log.warn( "Quantitation type already had an ID...:" + newQt );
        }

        for ( RawExpressionDataVector vec : newVectors ) {
            vec.setBioAssayDimension( bad );
            vec.setQuantitationType( newQt );
        }

        ee = rawExpressionDataVectorDao.addVectors( ee.getId(), newVectors );

        ArrayDesign vectorAd = newVectors.iterator().next().getDesignElement().getArrayDesign();

        if ( ad == null ) {
            for ( BioAssay ba : ee.getBioAssays() ) {
                if ( !vectorAd.equals( ba.getArrayDesignUsed() ) ) {
                    throw new IllegalArgumentException( "Vectors must use the array design as the bioassays" );
                }
            }
        } else if ( !vectorAd.equals( ad ) ) {
            throw new IllegalArgumentException( "Vectors must use the array design indicated" );
        }

        for ( BioAssay ba : ee.getBioAssays() ) {
            ba.setArrayDesignUsed( ad );
        }

        // this is a denormalization; easy to forget to update this.
        ee.getQuantitationTypes().add( newQt );

        // this.update( ee ); // is this even necessary? should flush.

        log.info( ee.getRawExpressionDataVectors().size() + " vectors for experiment" );

        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperiment> browse( Integer start, Integer limit ) {
        return this.expressionExperimentDao.browse( start, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperiment> browse( Integer start, Integer limit, String orderField, boolean descending ) {
        return this.expressionExperimentDao.browse( start, limit, orderField, descending );
    }

    @Override
    @Transactional
    public void remove( Long id ) {
        final ExpressionExperiment ee = this.load( id );

        if ( !securityService.isEditable( ee ) ) {
            throw new SecurityException(
                    "Error performing 'ExpressionExperimentService.remove(ExpressionExperiment expressionExperiment)' --> "
                            + " You do not have permission to edit this experiment." );
        }

        // Remove subsets
        Collection<ExpressionExperimentSubSet> subsets = getSubSets( ee );
        for ( ExpressionExperimentSubSet subset : subsets ) {
            expressionExperimentSubSetService.delete( subset );
        }

        // Remove differential expression analyses
        Collection<DifferentialExpressionAnalysis> diffAnalyses = this.differentialExpressionAnalysisDao
                .findByInvestigation( ee );
        for ( DifferentialExpressionAnalysis de : diffAnalyses ) {
            Long toDelete = de.getId();
            this.differentialExpressionAnalysisDao.remove( toDelete );
        }

        // remove any sample coexpression matrices
        this.sampleCoexpressionAnalysisDao.removeForExperiment( ee );

        // Remove PCA
        Collection<PrincipalComponentAnalysis> pcas = this.principalComponentAnalysisService.findByExperiment( ee );
        for ( PrincipalComponentAnalysis pca : pcas ) {
            this.principalComponentAnalysisService.remove( pca );
        }

        /*
         * FIXME: delete probecoexpression analysis; gene coexexpression will linger.
         */

        /*
         * Delete any expression experiment sets that only have this one ee in it. If possible remove this experiment
         * from other sets, and update them. IMPORTANT, this section assumes that we already checked for gene2gene
         * analyses!
         */
        Collection<ExpressionExperimentSet> sets = this.expressionExperimentSetService.find( ee );
        for ( ExpressionExperimentSet eeset : sets ) {
            if ( eeset.getExperiments().size() == 1 && eeset.getExperiments().iterator().next().equals( ee ) ) {
                log.info( "Removing from set " + eeset );
                this.expressionExperimentSetService
                        .remove( eeset ); // remove the set because in only contains this experiment
            } else {
                log.info( "Removing " + ee + " from " + eeset );
                eeset.getExperiments().remove( ee );
                this.expressionExperimentSetService.update( eeset ); // update set to not reference this experiment.
            }
        }
        this.expressionExperimentDao.remove( ee );
    }

    /**
     * @see ExpressionExperimentService#remove(ExpressionExperiment)
     */
    @Override
    @Transactional
    public void remove( ExpressionExperiment expressionExperiment ) {
        // Can not call DAO directly since we have to do some service-layer level house keeping
        this.remove( expressionExperiment.getId() );
    }

    /**
     * returns ids of search results
     *
     * @return collection of ids or an empty collection
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Long> filter( String searchString ) {

        Map<Class<?>, List<SearchResult>> searchResultsMap = searchService
                .search( SearchSettingsImpl.expressionExperimentSearch( searchString ) );

        assert searchResultsMap != null;

        Collection<SearchResult> searchResults = searchResultsMap.get( ExpressionExperiment.class );

        Collection<Long> ids = new ArrayList<Long>( searchResults.size() );

        for ( SearchResult s : searchResults ) {
            ids.add( s.getId() );
        }

        return ids;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByAccession( String accession ) {
        return this.expressionExperimentDao.findByAccession( accession );
    }

    /**
     * @see ExpressionExperimentService#findByAccession(DatabaseEntry)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByAccession( final DatabaseEntry accession ) {
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
    public Collection<ExpressionExperiment> findByBioMaterials( final Collection<BioMaterial> bioMaterials ) {
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
    public Collection<ExpressionExperiment> findByFactorValues( final Collection<FactorValue> factorValues ) {
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
     * @see ExpressionExperimentService#findByInvestigator(Contact)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByInvestigator( final Contact investigator ) {
        return this.expressionExperimentDao.findByInvestigator( investigator );
    }

    /**
     * @see ExpressionExperimentService#findByName(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByName( final String name ) {
        return this.expressionExperimentDao.findByName( name );
    }

    /**
     * @see ExpressionExperimentService#findByParentTaxon(Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByParentTaxon( final Taxon taxon ) {
        return this.expressionExperimentDao.findByParentTaxon( taxon );
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
    public List<ExpressionExperiment> findByUpdatedLimit( Integer limit ) {
        return this.expressionExperimentDao.findByUpdatedLimit( limit );
    }

    @Override
    @Transactional
    public ExpressionExperiment findOrCreate( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.findOrCreate( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getAnnotationCounts( final Collection<Long> ids ) {
        return this.expressionExperimentDao.getAnnotationCounts( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<AnnotationValueObject> getAnnotations( Long eeId ) {
        ExpressionExperiment expressionExperiment = load( eeId );
        Collection<AnnotationValueObject> annotations = new ArrayList<AnnotationValueObject>();
        for ( Characteristic c : expressionExperiment.getCharacteristics() ) {
            AnnotationValueObject annotationValue = new AnnotationValueObject();
            annotationValue.setId( c.getId() );
            annotationValue.setClassName( c.getCategory() );
            annotationValue.setTermName( c.getValue() );
            annotationValue.setEvidenceCode( c.getEvidenceCode() != null ? c.getEvidenceCode().toString() : "" );
            if ( c instanceof VocabCharacteristic ) {
                VocabCharacteristic vc = ( VocabCharacteristic ) c;
                annotationValue.setClassUri( vc.getCategoryUri() );
                String className = getLabelFromUri( vc.getCategoryUri() );
                if ( className != null )
                    annotationValue.setClassName( className );
                annotationValue.setTermUri( vc.getValueUri() );
                String termName = getLabelFromUri( vc.getValueUri() );
                if ( termName != null )
                    annotationValue.setTermName( termName );
                annotationValue.setObjectClass( VocabCharacteristic.class.getSimpleName() );
            } else {
                annotationValue.setObjectClass( Characteristic.class.getSimpleName() );
            }
            annotations.add( annotationValue );
        }
        return annotations;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> getArrayDesignsUsed( final BioAssaySet expressionExperiment ) {
        return this.expressionExperimentDao.getArrayDesignsUsed( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public String getBatchConfound( ExpressionExperiment ee ) {
        Collection<BatchConfoundValueObject> confounds;
        try {
            confounds = BatchConfound.test( ee );
        } catch ( Exception e ) {
            return null;
        }
        String result = null;

        for ( BatchConfoundValueObject c : confounds ) {
            if ( c.getP() < BATCH_CONFOUND_THRESHOLD ) {
                String factorName = c.getEf().getName();
                result = "Factor: " + factorName + " may be confounded with batches; p=" + String
                        .format( "%.2g", c.getP() ) + "<br />";
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public BatchEffectDetails getBatchEffect( ExpressionExperiment ee ) {

        BatchEffectDetails details = new BatchEffectDetails();

        details.setDataWasBatchCorrected( false );
        for ( QuantitationType qt : this.expressionExperimentDao.getQuantitationTypes( ee ) ) {
            if ( qt.getIsMaskedPreferred() && qt.getIsBatchCorrected() ) {
                details.setDataWasBatchCorrected( true );
                details.setHasBatchInformation( true );
            }

        }

        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( BatchInfoPopulationServiceImpl.isBatchFactor( ef ) ) {
                details.setHasBatchInformation( true );
                SVDValueObject svd = svdService.getSvdFactorAnalysis( ee.getId() );
                if ( svd == null )
                    break;
                double minp = 1.0;

                for ( Integer component : svd.getFactorPvals().keySet() ) {
                    Map<Long, Double> cmpEffects = svd.getFactorPvals().get( component );

                    Double pval = cmpEffects.get( ef.getId() );
                    if ( pval != null && pval < minp ) {
                        details.setPvalue( pval );
                        details.setComponent( component + 1 );
                        details.setComponentVarianceProportion( svd.getVariances()[component] );
                        minp = pval;
                    }

                }
                return details;
            }
        }
        return details;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment ) {
        Collection<BioAssayDimension> bioAssayDimensions = this.expressionExperimentDao
                .getBioAssayDimensions( expressionExperiment );
        Collection<BioAssayDimension> thawedBioAssayDimensions = new HashSet<BioAssayDimension>();
        for ( BioAssayDimension bioAssayDimension : bioAssayDimensions ) {
            thawedBioAssayDimensions.add( this.bioAssayDimensionDao.thaw( bioAssayDimension ) );
        }
        return thawedBioAssayDimensions;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getBioMaterialCount( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getBioMaterialCount( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getDesignElementDataVectorCountById( final Long id ) {
        return this.expressionExperimentDao.getDesignElementDataVectorCountById( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DesignElementDataVector> getDesignElementDataVectors(
            final Collection<CompositeSequence> designElements, final QuantitationType quantitationType ) {
        return this.expressionExperimentDao.getDesignElementDataVectors( designElements, quantitationType );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DesignElementDataVector> getDesignElementDataVectors(
            final Collection<QuantitationType> quantitationTypes ) {
        return this.expressionExperimentDao.getDesignElementDataVectors( quantitationTypes );
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
        return getLastEvent( this.load( ids ), LinkAnalysisEvent.Factory.newInstance() );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastMissingValueAnalysis( final Collection<Long> ids ) {
        return getLastEvent( this.load( ids ), MissingValueAnalysisEvent.Factory.newInstance() );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastProcessedDataUpdate( final Collection<Long> ids ) {
        return getLastEvent( this.load( ids ), ProcessedVectorComputationEvent.Factory.newInstance() );
    }

    /**
     * @return a map of the expression experiment ids to the last audit event for the given audit event type the map
     * can contain nulls if the specified auditEventType isn't found for a given expression experiment id
     */
    private Map<Long, AuditEvent> getLastEvent( Collection<ExpressionExperiment> ees, AuditEventType type ) {

        Map<Long, AuditEvent> lastEventMap = new HashMap<Long, AuditEvent>();
        AuditEvent last;
        for ( ExpressionExperiment experiment : ees ) {
            last = this.auditEventDao.getLastEvent( experiment, type.getClass() );
            lastEventMap.put( experiment.getId(), last );
        }
        return lastEventMap;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Taxon, Long> getPerTaxonCount() {
        return this.expressionExperimentDao.getPerTaxonCount();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getPopulatedFactorCounts( final Collection<Long> ids ) {
        return this.expressionExperimentDao.getPopulatedFactorCounts( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getPopulatedFactorCountsExcludeBatch( final Collection<Long> ids ) {
        return this.expressionExperimentDao.getPopulatedFactorCountsExcludeBatch( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationType> getPreferredQuantitationType( final ExpressionExperiment ee ) {
        Collection<QuantitationType> preferredQuantitationTypes = new HashSet<QuantitationType>();

        Collection<QuantitationType> quantitationTypes = this.getQuantitationTypes( ee );

        for ( QuantitationType qt : quantitationTypes ) {
            if ( qt.getIsPreferred() ) {
                preferredQuantitationTypes.add( qt );
            }
        }
        return preferredQuantitationTypes;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment ee ) {
        return this.expressionExperimentDao.getProcessedDataVectors( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<QuantitationType, Integer> getQuantitationTypeCountById( final Long Id ) {
        return this.expressionExperimentDao.getQuantitationTypeCountById( Id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationType> getQuantitationTypes( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getQuantitationTypes( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationType> getQuantitationTypes( final ExpressionExperiment expressionExperiment,
            final ArrayDesign arrayDesign ) {
        return this.expressionExperimentDao.getQuantitationTypes( expressionExperiment, arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            final Collection<ExpressionExperiment> expressionExperiments ) {
        return this.expressionExperimentDao.getSampleRemovalEvents( expressionExperiments );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DesignElementDataVector> getSamplingOfVectors( final QuantitationType quantitationType,
            final Integer limit ) {
        return this.expressionExperimentDao.getSamplingOfVectors( quantitationType, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> getSubSets( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getSubSets( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Taxon getTaxon( final BioAssaySet bioAssaySet ) {
        return this.expressionExperimentDao.getTaxon( bioAssaySet );
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends BioAssaySet> Map<T, Taxon> getTaxa( Collection<T> bioAssaySets ) {
        return this.expressionExperimentDao.getTaxa( bioAssaySets );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentValueObject> loadValueObjectsFilter( int offset, int limit, String orderBy,
            boolean asc, String accession ) {
        return this.expressionExperimentDao
                .listFilter( offset, limit, orderBy, asc, this.databaseEntryService.load( accession ) );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperimentValueObject> loadAllValueObjectsOrdered( String orderField, boolean descending ) {
        return this.expressionExperimentDao.loadAllValueObjectsOrdered( orderField, descending );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperimentValueObject> loadAllValueObjectsTaxon( Taxon taxon ) {
        return this.expressionExperimentDao.loadAllValueObjectsTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperimentValueObject> loadAllValueObjectsTaxonOrdered( String orderField, boolean descending,
            Taxon taxon ) {
        return this.expressionExperimentDao.loadAllValueObjectsTaxonOrdered( orderField, descending, taxon );
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
    public Collection<ExpressionExperiment> loadMyExpressionExperiments() {
        return loadAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadMySharedExpressionExperiments() {
        return loadAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadUserOwnedExpressionExperiments() {
        return loadAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentValueObject> loadValueObjects( final Collection<Long> ids,
            boolean maintainOrder ) {
        return this.expressionExperimentDao.loadValueObjects( ids, maintainOrder );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperimentValueObject> loadValueObjectsOrdered( String orderField, boolean descending,
            Collection<Long> ids ) {
        return new ArrayList<ExpressionExperimentValueObject>(
                this.expressionExperimentDao.loadValueObjectsOrdered( orderField, descending, ids ) );
    }

    @Override
    public List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjects( String orderField, boolean descending,
            List<Long> ids, Taxon taxon, boolean admin, int limit, int start ) {
        return this.expressionExperimentDao
                .loadDetailsValueObjects( orderField, descending, ids, taxon, admin, limit, start );
    }

    @Override
    @Transactional
    public int removeData( ExpressionExperiment ee, QuantitationType qt ) {
        ExpressionExperiment eeToUpdate = this.load( ee.getId() );
        Collection<RawExpressionDataVector> vecsToRemove = new ArrayList<RawExpressionDataVector>();
        for ( RawExpressionDataVector oldvec : eeToUpdate.getRawExpressionDataVectors() ) {
            if ( oldvec.getQuantitationType().equals( qt ) ) {
                vecsToRemove.add( oldvec );
            }
        }

        if ( vecsToRemove.isEmpty() ) {
            throw new IllegalArgumentException( "No vectors to remove for quantitation type=" + qt );
        }

        eeToUpdate.getRawExpressionDataVectors().removeAll( vecsToRemove );
        log.info( "Removing unused quantitation type: " + qt );
        eeToUpdate.getQuantitationTypes().remove( qt );
        return vecsToRemove.size();
    }

    @Override
    @Transactional
    public ExpressionExperiment replaceVectors( ExpressionExperiment ee, ArrayDesign ad,
            Collection<RawExpressionDataVector> newVectors ) {

        if ( newVectors == null || newVectors.isEmpty() ) {
            throw new UnsupportedOperationException( "Only use this method for replacing vectors, not erasing them" );
        }

        // to attach to session correctly.
        ExpressionExperiment eeToUpdate = this.load( ee.getId() );

        // remove old vectors. FIXME are we sure we want to do this?
        Collection<QuantitationType> qtsToRemove = new HashSet<QuantitationType>();
        for ( RawExpressionDataVector oldvec : eeToUpdate.getRawExpressionDataVectors() ) {
            qtsToRemove.add( oldvec.getQuantitationType() );
        }
        rawExpressionDataVectorDao.remove( eeToUpdate.getRawExpressionDataVectors() );
        processedVectorService.remove( eeToUpdate.getProcessedExpressionDataVectors() );
        eeToUpdate.getProcessedExpressionDataVectors().clear();
        eeToUpdate.getRawExpressionDataVectors().clear();

        for ( QuantitationType oldqt : qtsToRemove ) {
            log.info( "Removing unused quantitation type: " + oldqt );
            quantitationTypeDao.remove( oldqt );
        }

        return addVectors( eeToUpdate, ad, newVectors );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thaw( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.thaw( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thawLite( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.thawWithoutVectors( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thawBioAssays( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.thawBioAssays( expressionExperiment );
    }

    @Override
    public ExpressionExperiment thawLiter( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.thawForFrontEnd( expressionExperiment );
    }

    @Override
    @Transactional
    public ExperimentalFactor addFactor( ExpressionExperiment ee, ExperimentalFactor factor ) {
        ExpressionExperiment experiment = expressionExperimentDao.load( ee.getId() );
        factor.setExperimentalDesign( experiment.getExperimentalDesign() );
        factor.setSecurityOwner( experiment );
        factor = experimentalFactorDao.create( factor ); // to make sure we get acls.
        experiment.getExperimentalDesign().getExperimentalFactors().add( factor );
        expressionExperimentDao.update( experiment );
        return factor;
    }

    @Override
    @Transactional
    public FactorValue addFactorValue( ExpressionExperiment ee, FactorValue fv ) {
        assert fv.getExperimentalFactor() != null;
        ExpressionExperiment experiment = expressionExperimentDao.load( ee.getId() );
        fv.setSecurityOwner( experiment );
        Collection<ExperimentalFactor> efs = experiment.getExperimentalDesign().getExperimentalFactors();
        fv = this.factorValueDao.create( fv );
        for ( ExperimentalFactor ef : efs ) {
            if ( fv.getExperimentalFactor().equals( ef ) ) {
                ef.getFactorValues().add( fv );
                break;
            }
        }
        expressionExperimentDao.update( experiment );
        return fv;

    }

    /**
     * @param ee the expression experiment to be checked for trouble. This method will usually be preferred over checking
     *           the curation details of the object directly, as this method also checks all the array designs the given
     *           experiment belongs to.
     * @return true, if the given experiment, or any of its parenting array designs is troubled. False otherwise
     */
    @Override
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

    /**
     * Will add the vocab characteristic to the expression experiment and persist the changes.
     *
     * @param vc If the evidence code is null, it will be filled in with IC. A category and value must be provided.
     * @param ee the experiment to add the characteristics to.
     */
    @Override
    public void saveExpressionExperimentStatement( Characteristic vc, ExpressionExperiment ee ) {
        ee = thawLite( load( ee.getId() ) ); // Necessary to make sure we have the persistent version of the given ee.
        ontologyService.addExpressionExperimentStatement( vc, ee );
        update( ee );
    }

    /**
     * Will add all the vocab characteristics to the expression experiment and persist the changes.
     *
     * @param vc Collection of the characteristics to be added to the experiment. If the evidence code is null, it will
     *           be filled in with IC. A category and value must be provided.
     * @param ee the experiment to add the characteristics to.
     */
    @Override
    public void saveExpressionExperimentStatements( Collection<Characteristic> vc, ExpressionExperiment ee ) {
        for ( Characteristic characteristic : vc ) {
            saveExpressionExperimentStatement( characteristic, ee );
        }
    }

    private String getLabelFromUri( String uri ) {
        OntologyResource resource = ontologyService.getResource( uri );
        if ( resource != null )
            return resource.getLabel();

        return null;
    }

}