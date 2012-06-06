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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.security.SecurityService;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.expression.experiment.service.ExpressionExperimentService
 */
@Service
public class ExpressionExperimentServiceImpl implements ExpressionExperimentService {

    private Log log = LogFactory.getLog( this.getClass() );

    @Autowired
    private SecurityService securityService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private OntologyService ontologyService;
    
    @Autowired
    private AuditEventDao auditEventDao;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private ExpressionExperimentSetDao expressionExperimentSetDao;

    @Autowired
    private GeneCoexpressionAnalysisDao geneCoexpressionAnalysisDao;

    @Autowired
    private BioAssayDimensionDao bioAssayDimensionDao;

    @Autowired
    private SampleCoexpressionAnalysisDao sampleCoexpressionAnalysisDao;

    @Autowired
    private PrincipalComponentAnalysisDao principalComponentAnalysisDao;

    @Autowired
    private ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao probe2ProbeCoexpressionDao;

    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;

    @Autowired
    private QuantitationTypeService quantitationTypeDao;

    @Autowired
    private SVDService svdService;

    private static final double BATCH_CONFOUND_THRESHOLD = 0.01;

    private static final Double BATCH_EFFECT_PVALTHRESHOLD = 0.01;
    
    @Override
    public List<ExpressionExperiment> browse( Integer start, Integer limit ) {
        return this.expressionExperimentDao.browse( start, limit );
    }

    @Override
    public List<ExpressionExperiment> browse( Integer start, Integer limit, String orderField, boolean descending ) {
        return this.expressionExperimentDao.browse( start, limit, orderField, descending );
    }

    @Override
    public List<ExpressionExperiment> browseSpecificIds( Integer start, Integer limit, Collection<Long> ids ) {
        return this.expressionExperimentDao.browseSpecificIds( start, limit, ids );
    }

    @Override
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
    public Integer count() {
        return this.expressionExperimentDao.count();
    }

    /**
     * @see ExpressionExperimentService#countAll()
     */
    @Override
    public java.lang.Integer countAll() {
        return this.expressionExperimentDao.countAll();
    }

    /**
     * @see ExpressionExperimentService#create(ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment create( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.create( expressionExperiment );
    }

    /**
     * @see ExpressionExperimentService#delete(ExpressionExperiment)
     */
    @Override
    public void delete( final ExpressionExperiment expressionExperiment ) {
        delete( expressionExperiment.getId() );
    }

    /**
     * @see ExpressionExperimentService#delete(ExpressionExperiment)
     */
    @Override
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
    public Collection<Long> filter( String searchString ) {

        Map<Class<?>, List<SearchResult>> searchResultsMap = searchService.search( SearchSettings
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
    public ExpressionExperiment find( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.find( expressionExperiment );
    }

    @Override
    public Collection<ExpressionExperiment> findByAccession( String accession ) {
        return this.expressionExperimentDao.findByAccession( accession );
    }

    /**
     * @see ExpressionExperimentService#findByAccession(ubic.gemma.model.common.description.DatabaseEntry)
     */
    @Override
    public Collection<ExpressionExperiment> findByAccession(
            final ubic.gemma.model.common.description.DatabaseEntry accession ) {
        return this.expressionExperimentDao.findByAccession( accession );
    }

    /**
     * @see ExpressionExperimentService#findByBibliographicReference(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    public Collection<ExpressionExperiment> findByBibliographicReference( final BibliographicReference bibRef ) {
        return this.expressionExperimentDao.findByBibliographicReference( bibRef.getId() );
    }

    /**
     * @see ExpressionExperimentService#findByBioMaterial(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    public ExpressionExperiment findByBioMaterial( final ubic.gemma.model.expression.biomaterial.BioMaterial bm ) {
        return this.expressionExperimentDao.findByBioMaterial( bm );
    }

    /**
     * @see ExpressionExperimentService#findByBioMaterials(Collection)
     */
    @Override
    public Collection<ExpressionExperiment> findByBioMaterials( final Collection<BioMaterial> bioMaterials ) {
        return this.expressionExperimentDao.findByBioMaterials( bioMaterials );
    }

    /**
     * @see ExpressionExperimentService#findByExpressedGene(ubic.gemma.model.genome.Gene, double)
     */
    @Override
    public Collection<ExpressionExperiment> findByExpressedGene( final ubic.gemma.model.genome.Gene gene,
            final double rank ) {
        return this.expressionExperimentDao.findByExpressedGene( gene, rank );
    }

    /**
     * @see ExpressionExperimentService#findByFactorValue(FactorValue)
     */
    @Override
    public ExpressionExperiment findByFactorValue( final FactorValue factorValue ) {
        return this.expressionExperimentDao.findByFactorValue( factorValue );
    }

    /**
     * @see ExpressionExperimentService#findByFactorValues(Collection)
     */
    @Override
    public Collection<ExpressionExperiment> findByFactorValues( final Collection<FactorValue> factorValues ) {
        return this.expressionExperimentDao.findByFactorValues( factorValues );
    }

    /**
     * @see ExpressionExperimentService#findByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Collection<ExpressionExperiment> findByGene( final ubic.gemma.model.genome.Gene gene ) {
        return this.expressionExperimentDao.findByGene( gene );
    }

    /**
     * @see ExpressionExperimentService#findByInvestigator(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    @Override
    public Collection<ExpressionExperiment> findByInvestigator( final Contact investigator ) {
        return this.expressionExperimentDao.findByInvestigator( investigator );
    }

    /**
     * @see ExpressionExperimentService#findByName(java.lang.String)
     */
    @Override
    public ExpressionExperiment findByName( final java.lang.String name ) {
        return this.expressionExperimentDao.findByName( name );
    }

    /**
     * @see ExpressionExperimentService#findByParentTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<ExpressionExperiment> findByParentTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        return this.expressionExperimentDao.findByParentTaxon( taxon );
    }

    @Override
    public ExpressionExperiment findByQuantitationType( QuantitationType type ) {
        return this.expressionExperimentDao.findByQuantitationType( type );
    }

    /**
     * @see ExpressionExperimentService#findByShortName(java.lang.String)
     */
    @Override
    public ExpressionExperiment findByShortName( final java.lang.String shortName ) {
        return this.expressionExperimentDao.findByShortName( shortName );
    }

    @Override
    public List<ExpressionExperiment> findByTaxon( Taxon taxon, int limit ) {
        return this.expressionExperimentDao.findByTaxon( taxon, limit );
    }

    /**
     * @see ExpressionExperimentService#findByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
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
    public List<ExpressionExperiment> findByUpdatedLimit( Collection<Long> ids, Integer limit ) {
        return this.expressionExperimentDao.findByUpdatedLimit( ids, limit );
    }

    @Override
    public List<ExpressionExperiment> findByUpdatedLimit( int limit ) {
        return this.expressionExperimentDao.findByUpdatedLimit( limit );
    }

    /**
     * @see ExpressionExperimentService#findOrCreate(ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment findOrCreate( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.findOrCreate( expressionExperiment );
    }

    /**
     * @see ExpressionExperimentService#getAnnotationCounts(Collection)
     */
    @Override
    public Map<Long, Integer> getAnnotationCounts( final Collection<Long> ids ) {
        return this.expressionExperimentDao.getAnnotationCounts( ids );
    }

    /**
     * Get the terms associated this expression experiment.
     */
    @Override
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
    public Collection<ArrayDesign> getArrayDesignsUsed( final BioAssaySet expressionExperiment ) {
        return this.expressionExperimentDao.getArrayDesignsUsed( expressionExperiment );
    }

    /**
     * @return the auditEventDao
     */
    public AuditEventDao getAuditEventDao() {
        return auditEventDao;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentService#getBioAssayDimensions(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getBioAssayDimensions( expressionExperiment );
    }

    /**
     * @see ExpressionExperimentService#getBioMaterialCount(ExpressionExperiment)
     */
    @Override
    public Integer getBioMaterialCount( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getBioMaterialCount( expressionExperiment );
    }

    /**
     * @see ExpressionExperimentService#getDesignElementDataVectorCountById(long)
     */
    @Override
    public Integer getDesignElementDataVectorCountById( final long id ) {
        return this.expressionExperimentDao.getDesignElementDataVectorCountById( id );
    }

    /**
     * @see ExpressionExperimentService#getDesignElementDataVectors(Collection,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    public Collection<DesignElementDataVector> getDesignElementDataVectors(
            final Collection<CompositeSequence> designElements,
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        return this.expressionExperimentDao.getDesignElementDataVectors( designElements, quantitationType );
    }

    /**
     * @see ExpressionExperimentService#getDesignElementDataVectors(Collection)
     */
    @Override
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
    public Map<Long, Date> getLastArrayDesignUpdate( final Collection<ExpressionExperiment> expressionExperiments ) {
        return this.expressionExperimentDao.getLastArrayDesignUpdate( expressionExperiments );
    }

    /**
     * @see ExpressionExperimentService#getLastArrayDesignUpdate(ExpressionExperiment, java.lang.Class)
     */
    @Override
    public Date getLastArrayDesignUpdate( final ExpressionExperiment ee ) {
        return this.expressionExperimentDao.getLastArrayDesignUpdate( ee );
    }

    /**
     * @see ExpressionExperimentService#getLastLinkAnalysis(Collection)
     */
    @Override
    public Map<Long, AuditEvent> getLastLinkAnalysis( final Collection<Long> ids ) {
        return getLastEvent( ids, LinkAnalysisEvent.Factory.newInstance() );
    }

    /**
     * @see ExpressionExperimentService#getLastMissingValueAnalysis(Collection)
     */
    @Override
    public Map<Long, AuditEvent> getLastMissingValueAnalysis( final Collection<Long> ids ) {
        return getLastEvent( ids, MissingValueAnalysisEvent.Factory.newInstance() );
    }

    /**
     * @see ExpressionExperimentService#getLastProcessedDataUpdate(Collection)
     */
    @Override
    public Map<Long, AuditEvent> getLastProcessedDataUpdate( final Collection<Long> ids ) {
        return getLastEvent( ids, ProcessedVectorComputationEvent.Factory.newInstance() );
    }

    /**
     * @see ExpressionExperimentService#getLastTroubleEvent(Collection)
     */
    @Override
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
    public Map<Long, AuditEvent> getLastValidationEvent( final Collection<Long> ids ) {
        return getLastEvent( ids, ValidatedFlagEvent.Factory.newInstance() );
    }

    /**
     * @see ExpressionExperimentService#getPerTaxonCount()
     */
    @Override
    public Map<Taxon, Long> getPerTaxonCount() {
        return this.expressionExperimentDao.getPerTaxonCount();
    }

    /**
     * @see ExpressionExperimentService#getPopulatedFactorCounts(Collection)
     */
    @Override
    public Map<Long, Integer> getPopulatedFactorCounts( final Collection<Long> ids ) {
        return this.expressionExperimentDao.getPopulatedFactorCounts( ids );
    }

    /**
     * @see ExpressionExperimentService#getPopulatedFactorCountsExcludeBatch(Collection)
     */
    @Override
    public Map<Long, Integer> getPopulatedFactorCountsExcludeBatch( final Collection<Long> ids ) {
        try {
            return this.expressionExperimentDao.getPopulatedFactorCountsExcludeBatch( ids );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getPopulatedFactorCountsExcludeBatch(Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getPreferredQuantitationType(ExpressionExperiment)
     */
    @Override
    public Collection<QuantitationType> getPreferredQuantitationType( final ExpressionExperiment ee ) {
        try {
            Collection<QuantitationType> preferredQuantitationTypes = new HashSet<QuantitationType>();

            Collection<QuantitationType> quantitationTypes = this.getQuantitationTypes( ee );

            for ( QuantitationType qt : quantitationTypes ) {
                if ( qt.getIsPreferred() ) {
                    preferredQuantitationTypes.add( qt );
                }
            }
            return preferredQuantitationTypes;
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getPreferredQuantitationType(ExpressionExperiment EE)' --> "
                            + th, th );
        }
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
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment ee ) {
        return this.expressionExperimentDao.getProcessedDataVectors( ee );
    }

    /**
     * @see ExpressionExperimentService#getPreferredDesignElementDataVectorCount(ExpressionExperiment)
     */
    @Override
    public Integer getProcessedExpressionVectorCount( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.expressionExperimentDao.getProcessedExpressionVectorCount( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getPreferredDesignElementDataVectorCount(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getQuantitationTypeCountById(java.lang.Long)
     */
    @Override
    public Map<QuantitationType, Integer> getQuantitationTypeCountById( final java.lang.Long Id ) {
        try {
            return this.expressionExperimentDao.getQuantitationTypeCountById( Id );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getQuantitationTypeCountById(java.lang.Long Id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getQuantitationTypes(ExpressionExperiment)
     */
    @Override
    public Collection<QuantitationType> getQuantitationTypes( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.expressionExperimentDao.getQuantitationTypes( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getQuantitationTypes(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getQuantitationTypes(ExpressionExperiment,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public Collection<QuantitationType> getQuantitationTypes( final ExpressionExperiment expressionExperiment,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.expressionExperimentDao.getQuantitationTypes( expressionExperiment, arrayDesign );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getQuantitationTypes(ExpressionExperiment expressionExperiment, ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    public SampleCoexpressionAnalysisDao getSampleCoexpressionAnalysisDao() {
        return sampleCoexpressionAnalysisDao;
    }

    /**
     * @see ExpressionExperimentService#getSampleRemovalEvents(Collection)
     */
    @Override
    public Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            final Collection<ExpressionExperiment> expressionExperiments ) {
        try {
            return this.expressionExperimentDao.getSampleRemovalEvents( expressionExperiments );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getSampleRemovalEvents(Collection expressionExperiments)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getSamplingOfVectors(ubic.gemma.model.common.quantitationtype.QuantitationType,
     *      java.lang.Integer)
     */
    @Override
    @Deprecated
    public Collection<DesignElementDataVector> getSamplingOfVectors( final QuantitationType quantitationType,
            final Integer limit ) {
        try {
            return this.expressionExperimentDao.getSamplingOfVectors( quantitationType, limit );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getSamplingOfVectors(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType, java.lang.Integer limit)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getSubSets(ExpressionExperiment)
     */
    @Override
    public Collection<ExpressionExperimentSubSet> getSubSets( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.expressionExperimentDao.getSubSets( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getSubSets(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getTaxon(java.lang.Long)
     */
    @Override
    public Taxon getTaxon( final BioAssaySet bioAssaySet ) {
        try {
            return this.expressionExperimentDao.getTaxon( bioAssaySet );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getTaxon(java.lang.Long ExpressionExperimentID)' --> "
                            + th, th );
        }
    }

    @Override
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
    public ExpressionExperiment load( final java.lang.Long id ) {
        try {
            return this.expressionExperimentDao.load( id );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.load(java.lang.Long id)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#loadAll()
     */
    @Override
    public Collection<ExpressionExperiment> loadAll() {
        try {
            return ( Collection<ExpressionExperiment> ) this.expressionExperimentDao.loadAll();
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.loadAll()' --> " + th, th );
        }
    }

    @Override
    public List<ExpressionExperiment> loadAllOrdered( String orderField, boolean descending ) {
        return this.expressionExperimentDao.loadAllOrdered( orderField, descending );
    }

    @Override
    public List<ExpressionExperiment> loadAllTaxon( Taxon taxon ) {
        return this.expressionExperimentDao.loadAllTaxon( taxon );
    }

    @Override
    public List<ExpressionExperiment> loadAllTaxonOrdered( String orderField, boolean descending, Taxon taxon ) {
        return this.expressionExperimentDao.loadAllTaxonOrdered( orderField, descending, taxon );
    }

    /**
     * @see ExpressionExperimentService#loadAllValueObjects()
     */
    public Collection<ExpressionExperimentValueObject> loadAllValueObjects() {
        return this.handleLoadAllValueObjects();
    }

    @Override
    public Collection<ExpressionExperiment> loadLackingFactors() {
        return this.expressionExperimentDao.loadLackingFactors();
    }

    @Override
    public Collection<ExpressionExperiment> loadLackingTags() {
        return this.expressionExperimentDao.loadLackingTags();
    }

    /**
     * @see ExpressionExperimentService#loadMultiple(Collection)
     */
    @Override
    public Collection<ExpressionExperiment> loadMultiple( final Collection<Long> ids ) {
        try {
            return ( Collection<ExpressionExperiment> ) this.expressionExperimentDao.load( ids );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.loadMultiple(Collection ids)' --> " + th, th );
        }
    }

    @Override
    public List<ExpressionExperiment> loadMultipleOrdered( String orderField, boolean descending, Collection<Long> ids ) {
        return this.expressionExperimentDao.loadMultipleOrdered( orderField, descending, ids );
    }

    /*
     * Note: implemented via SpringSecurity.
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#loadMyExpressionExperiments()
     */
    @Override
    public Collection<ExpressionExperiment> loadMyExpressionExperiments() {
        return loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#loadMySharedExpressionExperiments()
     */
    @Override
    public Collection<ExpressionExperiment> loadMySharedExpressionExperiments() {
        return loadAll();
    }

    @Override
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
    public Collection<ExpressionExperiment> loadUserOwnedExpressionExperiments() {
        return loadAll();
    }

    /**
     * @see ExpressionExperimentService#loadValueObjects(Collection, boolean)
     */
    @Override
    public Collection<ExpressionExperimentValueObject> loadValueObjects( final Collection<Long> ids,
            boolean maintainOrder ) {
        /*
         * NOTE: Don't try and call this.loadMultiple(ids) to have security filter out experiments. The security
         * filtering just doesn't work. You need to call loadMultiple before calling loadValueObjects.
         */
        return this.expressionExperimentDao.loadValueObjects( ids, maintainOrder );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.expression.experiment.service.ExpressionExperimentService#replaceVectors(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment, ubic.gemma.model.expression.arrayDesign.ArrayDesign, java.util.Collection)
     */
    @Override
    public ExpressionExperiment replaceVectors( ExpressionExperiment ee, ArrayDesign ad,
            Collection<RawExpressionDataVector> vectors ) {

        if ( vectors == null || vectors.isEmpty() ) {
            throw new UnsupportedOperationException( "Only use this method for replacing vectors, not erasing them" );
        }

        Collection<BioAssayDimension> bads = new HashSet<BioAssayDimension>();
        Collection<QuantitationType> qts = new HashSet<QuantitationType>();
        for ( RawExpressionDataVector vec : vectors ) {
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

        ExpressionExperiment eeToUpdate = this.load( ee.getId() );
        BioAssayDimension bad = bads.iterator().next();
        bad = this.bioAssayDimensionDao.create( bad );

        QuantitationType qt = qts.iterator().next();
        qt = this.quantitationTypeDao.create( qt );

        for ( RawExpressionDataVector vec : vectors ) {
            vec.setBioAssayDimension( bad );
            vec.setQuantitationType( qt );
        }

        eeToUpdate.getProcessedExpressionDataVectors().clear();
        eeToUpdate.setRawExpressionDataVectors( vectors );
        ArrayDesign vectorAd = vectors.iterator().next().getDesignElement().getArrayDesign();

        if ( ad == null ) {
            for ( BioAssay ba : eeToUpdate.getBioAssays() ) {
                if ( !vectorAd.equals( ba.getArrayDesignUsed() ) ) {
                    throw new IllegalArgumentException( "Vectors must use the array design as the bioassays" );
                }
            }
        } else if ( !vectorAd.equals( ad ) ) {
            throw new IllegalArgumentException( "Vectors must use the array design indicated" );
        }

        for ( BioAssay ba : eeToUpdate.getBioAssays() ) {
            ba.setArrayDesignUsed( ad );
        }
        this.update( eeToUpdate );
        ee = eeToUpdate;
        return eeToUpdate;
    }

    @Override
    public void replaceVectors( ExpressionExperiment ee, Collection<RawExpressionDataVector> vectors ) {
        this.replaceVectors( ee, null, vectors );
    }

    /**
     * @param auditEventDao the auditEventDao to set
     */
    public void setAuditEventDao( AuditEventDao auditEventDao ) {
        this.auditEventDao = auditEventDao;
    }

    /**
     * @param differentialExpressionAnalysisDao the differentialExpressionAnalysisDao to set
     */
    public void setDifferentialExpressionAnalysisDao(
            DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao ) {
        this.differentialExpressionAnalysisDao = differentialExpressionAnalysisDao;
    }

    /**
     * Sets the reference to <code>expressionExperiment</code>'s DAO.
     */
    public void setExpressionExperimentDao( ExpressionExperimentDao expressionExperimentDao ) {
        this.expressionExperimentDao = expressionExperimentDao;
    }

    /**
     * @param expressionExperimentSetDao the expressionExperimentSetDao to set
     */
    public void setExpressionExperimentSetDao( ExpressionExperimentSetDao expressionExperimentSetDao ) {
        this.expressionExperimentSetDao = expressionExperimentSetDao;
    }

    public void setGeneCoexpressionAnalysisDao( GeneCoexpressionAnalysisDao geneCoexpressionAnalysisDao ) {
        this.geneCoexpressionAnalysisDao = geneCoexpressionAnalysisDao;
    }

    /**
     * Sets the reference to <code>probe2ProbeCoexpression</code>'s DAO.
     */
    public void setProbe2ProbeCoexpressionDao(
            ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao probe2ProbeCoexpressionDao ) {
        this.probe2ProbeCoexpressionDao = probe2ProbeCoexpressionDao;
    }

    /**
     * @see ExpressionExperimentService#thaw(ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment thaw( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.expressionExperimentDao.thaw( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.thaw(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#thawLite(ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment thawLite( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.expressionExperimentDao.thawBioAssays( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.thawLite(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#thawLite(ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment thawLiter( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.expressionExperimentDao.thawBioAssaysLiter( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.thawLite(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#update(ExpressionExperiment)
     */
    @Override
    public void update( final ExpressionExperiment expressionExperiment ) {
        try {
            this.expressionExperimentDao.update( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.update(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
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
        PrincipalComponentAnalysis pca = this.getPrincipalComponentAnalysisDao().findByExperiment( ee );
        if ( pca != null ) {
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoadAllValueObjects()
     */
    protected Collection<ExpressionExperimentValueObject> handleLoadAllValueObjects() {

        /* security will filter for us */
        // FIXME I'm not sure if this filtering actually works. See note for handleLoadValueObjects.
        Collection<ExpressionExperiment> experiments = this.loadAll();

        List<Long> filteredIds = new LinkedList<Long>();
        for ( ExpressionExperiment ee : experiments ) {
            filteredIds.add( ee.getId() );
        }

        /* now load the value objects for the filterd ids */
        return this.loadValueObjects( filteredIds, false );

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
    public List<ExpressionExperimentValueObject> getExperimentsWithEvent( Class<? extends AuditEventType> auditEventClass ){
        List<ExpressionExperiment> entities = new ArrayList<ExpressionExperiment>();
        entities.addAll( ( Collection<? extends ExpressionExperiment> ) this.auditTrailService.getEntitiesWithEvent( ExpressionExperiment.class, auditEventClass ) );
        return ExpressionExperimentValueObject.convert2ValueObjectsOrdered( entities );
    }
    

    @Override
    public List<ExpressionExperimentValueObject> getExperimentsWithBatchEffect( ){
        List<ExpressionExperiment> entities = new ArrayList<ExpressionExperiment>();
        entities.addAll( ( Collection<? extends ExpressionExperiment> ) this.auditTrailService.getEntitiesWithEvent( ExpressionExperiment.class, BatchInformationFetchingEvent.class ) );
        Collection<ExpressionExperiment> toRemove = new ArrayList<ExpressionExperiment>();
        for(ExpressionExperiment ee : entities){
            if(this.describeBatchEffect( ee ) == null){
                toRemove.add( ee );
            }
        }
        entities.removeAll( toRemove );
        return ExpressionExperimentValueObject.convert2ValueObjectsOrdered( entities );        
    }
    
    /**
     * @param ee
     * @return String msg describing confound if it is present, null otherwise
     */
    @Override
    public String describeBatchConfound( ExpressionExperiment ee ) {
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
                            + String.format( "%.2g", c.getP() ) + "<br />" ;
                }
            }
            return result;
    }

    /**
     * @param ee
     * @return String msg describing effect if it is present, null otherwise
     */
    @Override
    public String describeBatchEffect( ExpressionExperiment ee ) {
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
}