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
package ubic.gemma.expression.experiment.service;

import gemma.gsec.SecurityService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.basecode.ontology.model.OntologyResource;
import ubic.gemma.analysis.preprocess.batcheffects.BatchConfound;
import ubic.gemma.analysis.preprocess.batcheffects.BatchConfoundValueObject;
import ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.analysis.preprocess.svd.SVDService;
import ubic.gemma.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisDao;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisDao;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventDao;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ValidatedFlagEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorDao;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorDao;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueDao;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.expression.experiment.service.ExpressionExperimentService
 */
@Service
@Transactional
public class ExpressionExperimentServiceImpl implements ExpressionExperimentService {

    private static final double BATCH_CONFOUND_THRESHOLD = 0.01;

    private static final Double BATCH_EFFECT_PVALTHRESHOLD = 0.01;

    @Autowired
    private AuditEventDao auditEventDao;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private BioAssayDimensionDao bioAssayDimensionDao;

    @Autowired
    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private ExpressionExperimentSetDao expressionExperimentSetDao;

    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;

    @Autowired
    private ExperimentalFactorDao experimentalFactorDao;

    @Autowired
    private FactorValueDao factorValueDao;

    @Autowired
    private RawExpressionDataVectorDao rawExpressionDataVectorDao;

    @Autowired
    private GeneCoexpressionAnalysisDao geneCoexpressionAnalysisDao;

    private Log log = LogFactory.getLog( this.getClass() );

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private PrincipalComponentAnalysisDao principalComponentAnalysisDao;

    @Autowired
    private ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao probe2ProbeCoexpressionDao;

    @Autowired
    private ProcessedExpressionDataVectorDao processedVectorDao;

    @Autowired
    private QuantitationTypeService quantitationTypeDao;

    @Autowired
    private SampleCoexpressionAnalysisDao sampleCoexpressionAnalysisDao;

    @Autowired
    private SearchService searchService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private SVDService svdService;

    @Autowired
    private RawExpressionDataVectorDao vectorDao;

    /**
     * @param ee
     * @param ad
     * @param newVectors
     * @return
     */
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
    @Transactional(readOnly = true)
    public List<ExpressionExperiment> browseSpecificIds( Integer start, Integer limit, Collection<Long> ids ) {
        return this.expressionExperimentDao.browseSpecificIds( start, limit, ids );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperiment> browseSpecificIds( Integer start, Integer limit, String orderField,
            boolean descending, Collection<Long> ids ) {
        return this.expressionExperimentDao.browseSpecificIds( start, limit, orderField, descending, ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentService#count()
     */
    @Override
    @Transactional(readOnly = true)
    public Integer count() {
        return this.expressionExperimentDao.count();
    }

    /**
     * @see ExpressionExperimentService#countAll()
     */
    @Override
    @Transactional(readOnly = true)
    public java.lang.Integer countAll() {
        return this.expressionExperimentDao.countAll();
    }

    /**
     * @see ExpressionExperimentService#create(ExpressionExperiment)
     */
    @Override
    @Transactional
    public ExpressionExperiment create( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.create( expressionExperiment );
    }

    /**
     * @see ExpressionExperimentService#delete(ExpressionExperiment)
     */
    @Override
    @Transactional
    public void delete( final ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment == null || expressionExperiment.getId() == null ) {
            throw new IllegalArgumentException( "Experiment is null or had null id" );
        }
        delete( expressionExperiment.getId() );
    }

    /**
     * @see ExpressionExperimentService#delete(ExpressionExperiment)
     */
    @Override
    @Transactional
    public void delete( final Long id ) {

        final ExpressionExperiment ee = this.load( id );
        if ( securityService.isEditable( ee ) ) {
            this.handleDelete( ee );
        } else {
            throw new SecurityException(
                    "Error performing 'ExpressionExperimentService.delete(ExpressionExperiment expressionExperiment)' --> "
                            + " You do not have permission to edit this experiment." );
        }

    }

    /**
     * returns ids of search results
     * 
     * @param searchString
     * @return collection of ids or an empty collection
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Long> filter( String searchString ) {

        Map<Class<?>, List<SearchResult>> searchResultsMap = searchService.search( SearchSettingsImpl
                .expressionExperimentSearch( searchString ) );

        assert searchResultsMap != null;

        Collection<SearchResult> searchResults = searchResultsMap.get( ExpressionExperiment.class );

        Collection<Long> ids = new ArrayList<Long>( searchResults.size() );

        for ( SearchResult s : searchResults ) {
            ids.add( s.getId() );
        }

        return ids;
    }

    /**
     * @see ExpressionExperimentService#find(ExpressionExperiment)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment find( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.find( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByAccession( String accession ) {
        return this.expressionExperimentDao.findByAccession( accession );
    }

    /**
     * @see ExpressionExperimentService#findByAccession(ubic.gemma.model.common.description.DatabaseEntry)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByAccession(
            final ubic.gemma.model.common.description.DatabaseEntry accession ) {
        return this.expressionExperimentDao.findByAccession( accession );
    }

    /**
     * @see ExpressionExperimentService#findByBibliographicReference(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByBibliographicReference( final BibliographicReference bibRef ) {
        return this.expressionExperimentDao.findByBibliographicReference( bibRef.getId() );
    }

    /**
     * @see ExpressionExperimentService#findByBioAssay(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByBioAssay( final ubic.gemma.model.expression.bioAssay.BioAssay ba ) {
        return this.expressionExperimentDao.findByBioAssay( ba );
    }

    /**
     * @see ExpressionExperimentService#findByBioMaterial(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByBioMaterial( final ubic.gemma.model.expression.biomaterial.BioMaterial bm ) {
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
     * @see ExpressionExperimentService#findByExpressedGene(ubic.gemma.model.genome.Gene, double)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByExpressedGene( final ubic.gemma.model.genome.Gene gene,
            final double rank ) {
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
     * @see ExpressionExperimentService#findByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByGene( final ubic.gemma.model.genome.Gene gene ) {
        return this.expressionExperimentDao.findByGene( gene );
    }

    /**
     * @see ExpressionExperimentService#findByInvestigator(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByInvestigator( final Contact investigator ) {
        return this.expressionExperimentDao.findByInvestigator( investigator );
    }

    /**
     * @see ExpressionExperimentService#findByName(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByName( final java.lang.String name ) {
        return this.expressionExperimentDao.findByName( name );
    }

    /**
     * @see ExpressionExperimentService#findByParentTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByParentTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        return this.expressionExperimentDao.findByParentTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByQuantitationType( QuantitationType type ) {
        return this.expressionExperimentDao.findByQuantitationType( type );
    }

    /**
     * @see ExpressionExperimentService#findByShortName(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByShortName( final java.lang.String shortName ) {
        return this.expressionExperimentDao.findByShortName( shortName );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperiment> findByTaxon( Taxon taxon, Integer limit ) {
        return this.expressionExperimentDao.findByTaxon( taxon, limit );
    }

    /**
     * @see ExpressionExperimentService#findByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        return this.expressionExperimentDao.findByTaxon( taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#findByUpdatedLimit(java.util.Collection,
     * java.lang.Integer)
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperiment> findByUpdatedLimit( Collection<Long> ids, Integer limit ) {
        return this.expressionExperimentDao.findByUpdatedLimit( ids, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperiment> findByUpdatedLimit( Integer limit ) {
        return this.expressionExperimentDao.findByUpdatedLimit( limit );
    }

    /**
     * @see ExpressionExperimentService#findOrCreate(ExpressionExperiment)
     */
    @Override
    @Transactional
    public ExpressionExperiment findOrCreate( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.findOrCreate( expressionExperiment );
    }

    /**
     * @see ExpressionExperimentService#getAnnotationCounts(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getAnnotationCounts( final Collection<Long> ids ) {
        return this.expressionExperimentDao.getAnnotationCounts( ids );
    }

    /**
     * Get the terms associated this expression experiment.
     */
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
            annotationValue.setEvidenceCode( c.getEvidenceCode().toString() );
            if ( c instanceof VocabCharacteristic ) {
                VocabCharacteristic vc = ( VocabCharacteristic ) c;
                annotationValue.setClassUri( vc.getCategoryUri() );
                String className = getLabelFromUri( vc.getCategoryUri() );
                if ( className != null ) annotationValue.setClassName( className );
                annotationValue.setTermUri( vc.getValueUri() );
                String termName = getLabelFromUri( vc.getValueUri() );
                if ( termName != null ) annotationValue.setTermName( termName );
                annotationValue.setObjectClass( VocabCharacteristic.class.getSimpleName() );
            } else {
                annotationValue.setObjectClass( Characteristic.class.getSimpleName() );
            }
            annotations.add( annotationValue );
        }
        return annotations;
    }

    /**
     * @see ExpressionExperimentService#getArrayDesignsUsed(ExpressionExperiment)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> getArrayDesignsUsed( final BioAssaySet expressionExperiment ) {
        return this.expressionExperimentDao.getArrayDesignsUsed( expressionExperiment );
    }

    /**
     * @return the auditEventDao
     */
    @Transactional(readOnly = true)
    public AuditEventDao getAuditEventDao() {
        return auditEventDao;
    }

    /**
     * @param ee
     * @return String msg describing confound if it is present, null otherwise
     */
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
                result = "Factor: " + factorName + " may be confounded with batches; p="
                        + String.format( "%.2g", c.getP() ) + "<br />";
            }
        }
        return result;
    }

    /**
     * @param ee
     * @return String msg describing effect if it is present, null otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public String getBatchEffect( ExpressionExperiment ee ) {
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( BatchInfoPopulationServiceImpl.isBatchFactor( ef ) ) {
                SVDValueObject svd = svdService.getSvdFactorAnalysis( ee.getId() );
                if ( svd == null ) break;
                for ( Integer component : svd.getFactorPvals().keySet() ) {
                    Map<Long, Double> cmpEffects = svd.getFactorPvals().get( component );

                    Double pval = cmpEffects.get( ef.getId() );
                    if ( pval != null && pval < BATCH_EFFECT_PVALTHRESHOLD ) {
                        return "This data set may have a batch artifact (PC" + ( component + 1 ) + "); p="
                                + String.format( "%.2g", pval ) + "<br />";
                    }
                }
            }
        }
        return null;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentService#getBioAssayDimensions(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getBioAssayDimensions( expressionExperiment );
    }

    /**
     * @see ExpressionExperimentService#getBioMaterialCount(ExpressionExperiment)
     */
    @Override
    @Transactional(readOnly = true)
    public Integer getBioMaterialCount( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getBioMaterialCount( expressionExperiment );
    }

    /**
     * @see ExpressionExperimentService#getDesignElementDataVectorCountById(long)
     */
    @Override
    @Transactional(readOnly = true)
    public Integer getDesignElementDataVectorCountById( final Long id ) {
        return this.expressionExperimentDao.getDesignElementDataVectorCountById( id );
    }

    /**
     * @see ExpressionExperimentService#getDesignElementDataVectors(Collection,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<DesignElementDataVector> getDesignElementDataVectors(
            final Collection<CompositeSequence> designElements,
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        return this.expressionExperimentDao.getDesignElementDataVectors( designElements, quantitationType );
    }

    /**
     * @see ExpressionExperimentService#getDesignElementDataVectors(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<DesignElementDataVector> getDesignElementDataVectors(
            final Collection<QuantitationType> quantitationTypes ) {
        return this.expressionExperimentDao.getDesignElementDataVectors( quantitationTypes );
    }

    /**
     * @return the differentialExpressionAnalysisDao
     */
    public DifferentialExpressionAnalysisDao getDifferentialExpressionAnalysisDao() {
        return differentialExpressionAnalysisDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getExperimentsWithBatchEffect() {
        List<ExpressionExperiment> entities = new ArrayList<ExpressionExperiment>();
        entities.addAll( ( Collection<? extends ExpressionExperiment> ) this.auditTrailService.getEntitiesWithEvent(
                ExpressionExperiment.class, BatchInformationFetchingEvent.class ) );
        Collection<ExpressionExperiment> toRemove = new ArrayList<ExpressionExperiment>();
        for ( ExpressionExperiment ee : entities ) {
            if ( this.getBatchEffect( ee ) == null ) {
                toRemove.add( ee );
            }
        }
        entities.removeAll( toRemove );
        return entities;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getExperimentsWithOutliers() {
        return this.expressionExperimentDao.getExperimentsWithOutliers();

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getExperimentsWithEvent( Class<? extends AuditEventType> auditEventClass ) {
        List<ExpressionExperiment> entities = new ArrayList<ExpressionExperiment>();
        entities.addAll( ( Collection<? extends ExpressionExperiment> ) this.auditTrailService.getEntitiesWithEvent(
                ExpressionExperiment.class, auditEventClass ) );
        return entities;
    }

    /**
     * @return the expressionExperimentSetDao
     */
    public ExpressionExperimentSetDao getExpressionExperimentSetDao() {
        return expressionExperimentSetDao;
    }

    public GeneCoexpressionAnalysisDao getGeneCoexpressionAnalysisDao() {
        return geneCoexpressionAnalysisDao;
    }

    /**
     * @see ExpressionExperimentService#getLastArrayDesignUpdate(Collection, java.lang.Class)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Date> getLastArrayDesignUpdate( final Collection<ExpressionExperiment> expressionExperiments ) {
        return this.expressionExperimentDao.getLastArrayDesignUpdate( expressionExperiments );
    }

    /**
     * @see ExpressionExperimentService#getLastArrayDesignUpdate(ExpressionExperiment, java.lang.Class)
     */
    @Override
    @Transactional(readOnly = true)
    public Date getLastArrayDesignUpdate( final ExpressionExperiment ee ) {
        return this.expressionExperimentDao.getLastArrayDesignUpdate( ee );
    }

    /**
     * @see ExpressionExperimentService#getLastLinkAnalysis(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastLinkAnalysis( final Collection<Long> ids ) {
        return getLastEvent( ids, LinkAnalysisEvent.Factory.newInstance() );
    }

    /**
     * @see ExpressionExperimentService#getLastMissingValueAnalysis(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastMissingValueAnalysis( final Collection<Long> ids ) {
        return getLastEvent( ids, MissingValueAnalysisEvent.Factory.newInstance() );
    }

    /**
     * @see ExpressionExperimentService#getLastProcessedDataUpdate(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastProcessedDataUpdate( final Collection<Long> ids ) {
        return getLastEvent( ids, ProcessedVectorComputationEvent.Factory.newInstance() );
    }

    /**
     * @see ExpressionExperimentService#getLastTroubleEvent(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastTroubleEvent( final Collection<Long> ids ) {
        Collection<ExpressionExperiment> ees = this.loadMultiple( ids );

        // this checks the array designs, too.
        Map<Auditable, AuditEvent> directEvents = this.getAuditEventDao().getLastOutstandingTroubleEvents( ees );

        Map<Long, AuditEvent> troubleMap = new HashMap<Long, AuditEvent>();
        for ( Auditable a : directEvents.keySet() ) {
            troubleMap.put( a.getId(), directEvents.get( a ) );
        }

        return troubleMap;
    }

    /**
     * @see ExpressionExperimentService#getLastValidationEvent(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastValidationEvent( final Collection<Long> ids ) {
        return getLastEvent( ids, ValidatedFlagEvent.Factory.newInstance() );
    }

    /**
     * @see ExpressionExperimentService#getPerTaxonCount()
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Taxon, Long> getPerTaxonCount() {
        return this.expressionExperimentDao.getPerTaxonCount();
    }

    /**
     * @see ExpressionExperimentService#getPopulatedFactorCounts(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getPopulatedFactorCounts( final Collection<Long> ids ) {
        return this.expressionExperimentDao.getPopulatedFactorCounts( ids );
    }

    /**
     * @see ExpressionExperimentService#getPopulatedFactorCountsExcludeBatch(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getPopulatedFactorCountsExcludeBatch( final Collection<Long> ids ) {
        return this.expressionExperimentDao.getPopulatedFactorCountsExcludeBatch( ids );
    }

    /**
     * @see ExpressionExperimentService#getPreferredQuantitationType(ExpressionExperiment)
     */
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

    public PrincipalComponentAnalysisDao getPrincipalComponentAnalysisDao() {
        return principalComponentAnalysisDao;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.experiment.ExpressionExperimentService#getProcessedDataVectors(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment ee ) {
        return this.expressionExperimentDao.getProcessedDataVectors( ee );
    }

    /**
     * @see ExpressionExperimentService#getQuantitationTypeCountById(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<QuantitationType, Integer> getQuantitationTypeCountById( final java.lang.Long Id ) {
        return this.expressionExperimentDao.getQuantitationTypeCountById( Id );
    }

    /**
     * @see ExpressionExperimentService#getQuantitationTypes(ExpressionExperiment)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationType> getQuantitationTypes( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getQuantitationTypes( expressionExperiment );
    }

    /**
     * @see ExpressionExperimentService#getQuantitationTypes(ExpressionExperiment,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationType> getQuantitationTypes( final ExpressionExperiment expressionExperiment,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        return this.expressionExperimentDao.getQuantitationTypes( expressionExperiment, arrayDesign );
    }

    public SampleCoexpressionAnalysisDao getSampleCoexpressionAnalysisDao() {
        return sampleCoexpressionAnalysisDao;
    }

    /**
     * @see ExpressionExperimentService#getSampleRemovalEvents(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            final Collection<ExpressionExperiment> expressionExperiments ) {
        return this.expressionExperimentDao.getSampleRemovalEvents( expressionExperiments );
    }

    /**
     * @see ExpressionExperimentService#getSamplingOfVectors(ubic.gemma.model.common.quantitationtype.QuantitationType,
     *      java.lang.Integer)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<DesignElementDataVector> getSamplingOfVectors( final QuantitationType quantitationType,
            final Integer limit ) {
        return this.expressionExperimentDao.getSamplingOfVectors( quantitationType, limit );
    }

    /**
     * @see ExpressionExperimentService#getSubSets(ExpressionExperiment)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> getSubSets( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getSubSets( expressionExperiment );
    }

    /**
     * @see ExpressionExperimentService#getTaxon(java.lang.Long)
     */
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
    public Collection<Long> getUntroubled( Collection<Long> ids ) {
        Collection<Long> firstPass = this.expressionExperimentDao.getUntroubled( ids );

        /*
         * Now check the array designs.
         */
        Map<ArrayDesign, Collection<Long>> ads = this.expressionExperimentDao.getArrayDesignsUsed( firstPass );
        Collection<Long> troubled = new HashSet<Long>();
        for ( ArrayDesign a : ads.keySet() ) {
            if ( a.getStatus().getTroubled() ) {
                troubled.addAll( ads.get( a ) );
            }
        }

        firstPass.removeAll( troubled );

        return firstPass;
    }

    /**
     * @see ExpressionExperimentService#load(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment load( final java.lang.Long id ) {
        return this.expressionExperimentDao.load( id );
    }

    /**
     * @see ExpressionExperimentService#loadAll()
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadAll() {
        return ( Collection<ExpressionExperiment> ) this.expressionExperimentDao.loadAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentValueObject> loadAllValueObjects() {
        return this.expressionExperimentDao.loadAllValueObjects();
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
    public List<ExpressionExperimentValueObject> loadAllValueObjectsTaxonOrdered( String orderField,
            boolean descending, Taxon taxon ) {
        return this.expressionExperimentDao.loadAllValueObjectsTaxonOrdered( orderField, descending, taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadBySubsetId( Long id ) {
        return this.expressionExperimentSubSetService.load( id ).getSourceExperiment();
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

    /**
     * @see ExpressionExperimentService#loadMultiple(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadMultiple( final Collection<Long> ids ) {
        return ( Collection<ExpressionExperiment> ) this.expressionExperimentDao.load( ids );
    }

    /*
     * Note: implemented via SpringSecurity.
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#loadMyExpressionExperiments()
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadMyExpressionExperiments() {
        return loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#loadMySharedExpressionExperiments()
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadMySharedExpressionExperiments() {
        return loadAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadTroubled() {
        Map<Long, AuditEvent> lastTroubleEvents = this.getLastTroubleEvents();
        return this.loadMultiple( lastTroubleEvents.keySet() );
    }

    /*
     * Note: implemented via SpringSecurity.
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#loadUserOwnedExpressionExperiments()
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadUserOwnedExpressionExperiments() {
        return loadAll();
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentValueObject loadValueObject( Long eeId ) {
        return this.expressionExperimentDao.loadValueObject( eeId );
    }

    /**
     * @see ExpressionExperimentService#loadValueObjects(Collection, boolean)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentValueObject> loadValueObjects( final Collection<Long> ids,
            boolean maintainOrder ) {
        return this.expressionExperimentDao.loadValueObjects( ids, maintainOrder );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.expression.experiment.service.ExpressionExperimentService#loadValueObjectsOrdered(java.lang.String,
     * boolean, java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperimentValueObject> loadValueObjectsOrdered( String orderField, boolean descending,
            Collection<Long> ids ) {

        return new ArrayList<ExpressionExperimentValueObject>( this.expressionExperimentDao.loadValueObjectsOrdered(
                orderField, descending, ids ) );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentService#removeData(ubic.gemma.model.expression.
     * experiment.ExpressionExperiment, ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
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
        eeToUpdate.getQuantitationTypes().remove( qt );
        // this.update( eeToUpdate ); // will flush.
        // quantitationTypeDao.remove( qt );
        return vecsToRemove.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.expression.experiment.service.ExpressionExperimentService#replaceVectors(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment, ubic.gemma.model.expression.arrayDesign.ArrayDesign, java.util.Collection)
     */
    @Override
    @Transactional
    public ExpressionExperiment replaceVectors( ExpressionExperiment ee, ArrayDesign ad,
            Collection<RawExpressionDataVector> newVectors ) {

        if ( newVectors == null || newVectors.isEmpty() ) {
            throw new UnsupportedOperationException( "Only use this method for replacing vectors, not erasing them" );
        }

        // to attach to session correctly.
        ExpressionExperiment eeToUpdate = this.load( ee.getId() );

        // remove old vectors.
        Collection<QuantitationType> qtsToRemove = new HashSet<QuantitationType>();
        for ( RawExpressionDataVector oldvec : eeToUpdate.getRawExpressionDataVectors() ) {
            qtsToRemove.add( oldvec.getQuantitationType() );
        }
        vectorDao.remove( eeToUpdate.getRawExpressionDataVectors() );
        processedVectorDao.remove( eeToUpdate.getProcessedExpressionDataVectors() );
        eeToUpdate.getProcessedExpressionDataVectors().clear();
        eeToUpdate.getRawExpressionDataVectors().clear();

        for ( QuantitationType oldqt : qtsToRemove ) {
            quantitationTypeDao.remove( oldqt );
        }

        return addVectors( eeToUpdate, ad, newVectors );
    }

    /**
     * Needed for tests.
     */
    public void setExpressionExperimentDao( ExpressionExperimentDao expressionExperimentDao ) {
        this.expressionExperimentDao = expressionExperimentDao;
    }

    /**
     * @see ExpressionExperimentService#thaw(ExpressionExperiment)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thaw( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.thaw( expressionExperiment );
    }

    /**
     * @see ExpressionExperimentService#thawLite(ExpressionExperiment)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thawLite( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.thawBioAssays( expressionExperiment );
    }

    /**
     * @see ExpressionExperimentService#thawLite(ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment thawLiter( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.thawBioAssaysLiter( expressionExperiment );
    }

    /**
     * @see ExpressionExperimentService#update(ExpressionExperiment)
     */
    @Override
    @Transactional
    public void update( final ExpressionExperiment expressionExperiment ) {
        this.expressionExperimentDao.update( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleDelete(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    protected void handleDelete( ExpressionExperiment ee ) {

        if ( ee == null ) {
            throw new IllegalArgumentException( "Experiment cannot be null" );
        }

        /*
         * If we remove the experiment from the set, analyses that used the set have to cope with this. For G2G,the data
         * sets are stored in order of IDs, but the actual ids are not stored (we refer back to the eeset), so coping
         * will not be possible (at best we can mark it as troubled). If there is no analysis object using the set, it's
         * okay. There are ways around this but it's messy, so for now we just refuse to delete such experiments.
         */
        Collection<GeneCoexpressionAnalysis> g2gAnalyses = this.getGeneCoexpressionAnalysisDao().findByInvestigation(
                ee );

        if ( g2gAnalyses.size() > 0 ) {
            throw new IllegalArgumentException( "Sorry, you can't delete " + ee
                    + "; it is part of at least one coexpression meta analysis: "
                    + g2gAnalyses.iterator().next().getName() );
        }

        // Remove subsets
        Collection<ExpressionExperimentSubSet> subsets = getSubSets( ee );
        for ( ExpressionExperimentSubSet subset : subsets ) {
            expressionExperimentSubSetService.delete( subset );
        }

        // Remove differential expression analyses
        Collection<DifferentialExpressionAnalysis> diffAnalyses = this.getDifferentialExpressionAnalysisDao()
                .findByInvestigation( ee );
        for ( DifferentialExpressionAnalysis de : diffAnalyses ) {
            Long toDelete = de.getId();
            this.getDifferentialExpressionAnalysisDao().remove( toDelete );
        }

        // remove any sample coexpression matrices
        this.getSampleCoexpressionAnalysisDao().removeForExperiment( ee );

        // Remove PCA
        Collection<PrincipalComponentAnalysis> pcas = this.getPrincipalComponentAnalysisDao().findByExperiment( ee );
        for ( PrincipalComponentAnalysis pca : pcas ) {
            this.getPrincipalComponentAnalysisDao().remove( pca );
        }

        // Remove probe2probe links
        this.probe2ProbeCoexpressionDao.deleteLinks( ee );

        /*
         * Delete any expression experiment sets that only have this one ee in it. If possible remove this experiment
         * from other sets, and update them. IMPORTANT, this section assumes that we already checked for gene2gene
         * analyses!
         */
        Collection<ExpressionExperimentSet> sets = this.getExpressionExperimentSetDao().find( ee );
        for ( ExpressionExperimentSet eeset : sets ) {
            if ( eeset.getExperiments().size() == 1 && eeset.getExperiments().iterator().next().equals( ee ) ) {
                this.getExpressionExperimentSetDao().remove( eeset );
            } else {
                log.info( "Removing " + ee + " from " + eeset );
                eeset.getExperiments().remove( ee );
                this.getExpressionExperimentSetDao().update( eeset );

            }
        }

        this.expressionExperimentDao.remove( ee );
    }

    /**
     * @param uri
     * @return
     */
    private String getLabelFromUri( String uri ) {
        OntologyResource resource = ontologyService.getResource( uri );
        if ( resource != null ) return resource.getLabel();

        return null;
    }

    /**
     * @param ids
     * @param type
     * @returns a map of the expression experiment ids to the last audit event for the given audit event type the map
     *          can contain nulls if the specified auditEventType isn't found for a given expression experiment id
     * @see AuditableDao.getLastAuditEvent and getLastTypedAuditEvents for faster methods.
     */
    private Map<Long, AuditEvent> getLastEvent( Collection<Long> ids, AuditEventType type ) {

        Map<Long, AuditEvent> lastEventMap = new HashMap<Long, AuditEvent>();
        Collection<ExpressionExperiment> ees = this.loadMultiple( ids );
        AuditEvent last;
        for ( ExpressionExperiment experiment : ees ) {
            last = this.getAuditEventDao().getLastEvent( experiment, type.getClass() );
            lastEventMap.put( experiment.getId(), last );
        }
        return lastEventMap;
    }

    /**
     * @return
     */
    private Map<Long, AuditEvent> getLastTroubleEvents() {
        Collection<ExpressionExperiment> ees = this.loadAll();

        // this checks the array designs, too.
        Map<Auditable, AuditEvent> directEvents = this.getAuditEventDao().getLastOutstandingTroubleEvents( ees );

        Map<Long, AuditEvent> troubleMap = new HashMap<Long, AuditEvent>();
        for ( Auditable a : directEvents.keySet() ) {
            troubleMap.put( a.getId(), directEvents.get( a ) );
        }

        return troubleMap;
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

    // @Override
    // @Transactional
    // public void addFactors( ExpressionExperiment ee, Collection<ExperimentalFactor> factors ) {
    // ExpressionExperiment experiment = expressionExperimentDao.load( ee.getId() );
    // for ( ExperimentalFactor ef : factors ) {
    // ef.setSecurityOwner( experiment );
    // ef.setExperimentalDesign( experiment.getExperimentalDesign() );
    // experimentalFactorDao.create( ef );
    // }
    // experiment.getExperimentalDesign().getExperimentalFactors().addAll( factors );
    // expressionExperimentDao.update( experiment );
    // // FIXME this should return the factors, but I was having trouble with that.
    // }

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

    // @Override
    // @Transactional
    // public void addFactorValues( ExpressionExperiment ee, Collection<FactorValue> fvs ) {
    // ExpressionExperiment experiment = expressionExperimentDao.load( ee.getId() );
    // Collection<ExperimentalFactor> efs = experiment.getExperimentalDesign().getExperimentalFactors();
    // for ( FactorValue fv : fvs ) {
    // fv.setSecurityOwner( experiment );
    // FactorValue pfv = this.factorValueDao.create( fv );
    // for ( ExperimentalFactor ef : efs ) {
    // if ( pfv.getExperimentalFactor().equals( ef ) ) {
    // ef.getFactorValues().add( pfv );
    // break;
    // }
    // }
    // }
    // expressionExperimentDao.update( experiment );
    // // FIXME this should return the factorvalues, but I was having trouble with that.
    // }

}