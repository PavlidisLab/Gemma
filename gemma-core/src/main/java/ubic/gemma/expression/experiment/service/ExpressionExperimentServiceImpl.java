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
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ValidatedFlagEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
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
    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private ExpressionExperimentSetDao expressionExperimentSetDao;

    @Autowired
    private GeneCoexpressionAnalysisDao geneCoexpressionAnalysisDao;

    @Autowired
    private SampleCoexpressionAnalysisDao sampleCoexpressionAnalysisDao;

    @Autowired
    private PrincipalComponentAnalysisDao principalComponentAnalysisDao;

    @Autowired
    private ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao probe2ProbeCoexpressionDao;

    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;

    /**
     * @see ExpressionExperimentService#countAll()
     */
    public java.lang.Integer countAll() {
        try {
            return this.expressionExperimentDao.countAll();
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.countAll()' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#create(ExpressionExperiment)
     */
    public ExpressionExperiment create( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.expressionExperimentDao.create( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.create(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#delete(ExpressionExperiment)
     */
    public void delete( final ExpressionExperiment expressionExperiment ) {
        try {
            this.thawLite( expressionExperiment ); // TODO: this is a hack to get it into session, needs to be
                                                   // refactored (high level services should take value objects)
            this.handleDelete( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.delete(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#delete(ExpressionExperiment)
     */
    public void delete( final Long id ) {
        try {
            final ExpressionExperiment ee = this.load( id );
            if(securityService.isEditable( ee )){
                this.handleDelete( ee );
            }else{
                throw new SecurityException("Error performing 'ExpressionExperimentService.delete(ExpressionExperiment expressionExperiment)' --> "+
                        " You do not have permission to edit this experiment.");
            }
            
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.delete(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#find(ExpressionExperiment)
     */
    public ExpressionExperiment find( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.expressionExperimentDao.find( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.find(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByAccession(ubic.gemma.model.common.description.DatabaseEntry)
     */
    public Collection<ExpressionExperiment> findByAccession(
            final ubic.gemma.model.common.description.DatabaseEntry accession ) {
        try {
            return this.expressionExperimentDao.findByAccession( accession );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findByAccession(ubic.gemma.model.common.description.DatabaseEntry accession)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByBibliographicReference(ubic.gemma.model.common.description.BibliographicReference)
     */
    public Collection<ExpressionExperiment> findByBibliographicReference( final BibliographicReference bibRef ) {
        try {
            return this.expressionExperimentDao.findByBibliographicReference( bibRef.getId() );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findByBibliographicReference(ubic.gemma.model.common.description.BibliographicReference bibRef)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByBioMaterial(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    public ExpressionExperiment findByBioMaterial( final ubic.gemma.model.expression.biomaterial.BioMaterial bm ) {
        try {
            return this.expressionExperimentDao.findByBioMaterial( bm );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findByBioMaterial(ubic.gemma.model.expression.biomaterial.BioMaterial bm)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByBioMaterials(Collection)
     */
    public Collection<ExpressionExperiment> findByBioMaterials( final Collection<BioMaterial> bioMaterials ) {
        try {
            return this.expressionExperimentDao.findByBioMaterials( bioMaterials );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findByBioMaterials(Collection bioMaterials)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByExpressedGene(ubic.gemma.model.genome.Gene, double)
     */
    public Collection<ExpressionExperiment> findByExpressedGene( final ubic.gemma.model.genome.Gene gene,
            final double rank ) {
        try {
            return this.expressionExperimentDao.findByExpressedGene( gene, rank );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findByExpressedGene(ubic.gemma.model.genome.Gene gene, double rank)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByFactorValue(FactorValue)
     */
    public ExpressionExperiment findByFactorValue( final FactorValue factorValue ) {
        try {
            return this.expressionExperimentDao.findByFactorValue( factorValue );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findByFactorValue(FactorValue factorValue)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByFactorValues(Collection)
     */
    public Collection<ExpressionExperiment> findByFactorValues( final Collection<FactorValue> factorValues ) {
        try {
            return this.expressionExperimentDao.findByFactorValues( factorValues );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findByFactorValues(Collection factorValues)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByGene(ubic.gemma.model.genome.Gene)
     */
    public Collection<ExpressionExperiment> findByGene( final ubic.gemma.model.genome.Gene gene ) {
        try {
            return this.expressionExperimentDao.findByGene( gene );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findByGene(ubic.gemma.model.genome.Gene gene)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByInvestigator(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    public Collection<ExpressionExperiment> findByInvestigator( final Contact investigator ) {
        try {
            return this.expressionExperimentDao.findByInvestigator( investigator );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findByInvestigator(ubic.gemma.model.common.auditAndSecurity.Contact investigator)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByName(java.lang.String)
     */
    public ExpressionExperiment findByName( final java.lang.String name ) {
        try {
            return this.expressionExperimentDao.findByName( name );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findByName(java.lang.String name)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByParentTaxon(ubic.gemma.model.genome.Taxon)
     */
    public Collection<ExpressionExperiment> findByParentTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.expressionExperimentDao.findByParentTaxon( taxon );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findByParentTaxon(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByShortName(java.lang.String)
     */
    public ExpressionExperiment findByShortName( final java.lang.String shortName ) {
        try {
            return this.expressionExperimentDao.findByShortName( shortName );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findByShortName(java.lang.String shortName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByTaxon(ubic.gemma.model.genome.Taxon)
     */
    public Collection<ExpressionExperiment> findByTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.expressionExperimentDao.findByTaxon( taxon );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findByTaxon(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findOrCreate(ExpressionExperiment)
     */
    public ExpressionExperiment findOrCreate( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.expressionExperimentDao.findOrCreate( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findOrCreate(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getAnnotationCounts(Collection)
     */
    public Map<Long, Integer> getAnnotationCounts( final Collection<Long> ids ) {
        try {
            return this.expressionExperimentDao.getAnnotationCounts( ids );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getAnnotationCounts(Collection ids)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getArrayDesignsUsed(ExpressionExperiment)
     */
    public Collection<ArrayDesign> getArrayDesignsUsed( final BioAssaySet expressionExperiment ) {
        try {
            return this.expressionExperimentDao.getArrayDesignsUsed( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getArrayDesignsUsed(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @return the auditEventDao
     */
    public AuditEventDao getAuditEventDao() {
        return auditEventDao;
    }

    /**
     * @see ExpressionExperimentService#getBioMaterialCount(ExpressionExperiment)
     */
    public Integer getBioMaterialCount( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.expressionExperimentDao.getBioMaterialCount( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getBioMaterialCount(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getDesignElementDataVectorCountById(long)
     */
    public Integer getDesignElementDataVectorCountById( final long id ) {
        try {
            return this.expressionExperimentDao.getDesignElementDataVectorCountById( id );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getDesignElementDataVectorCountById(long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getDesignElementDataVectors(Collection,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public Collection<DesignElementDataVector> getDesignElementDataVectors(
            final Collection<CompositeSequence> designElements,
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        try {
            return this.expressionExperimentDao.getDesignElementDataVectors( designElements, quantitationType );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getDesignElementDataVectors(Collection designElements, ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getDesignElementDataVectors(Collection)
     */
    public Collection<DesignElementDataVector> getDesignElementDataVectors(
            final Collection<QuantitationType> quantitationTypes ) {
        try {
            return this.expressionExperimentDao.getDesignElementDataVectors( quantitationTypes );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getDesignElementDataVectors(Collection quantitationTypes)' --> "
                            + th, th );
        }
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
    public Map<Long, Date> getLastArrayDesignUpdate( final Collection<ExpressionExperiment> expressionExperiments ) {
        try {
            return this.expressionExperimentDao.getLastArrayDesignUpdate( expressionExperiments );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getLastArrayDesignUpdate(Collection expressionExperiments, java.lang.Class type)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getLastArrayDesignUpdate(ExpressionExperiment, java.lang.Class)
     */
    public Date getLastArrayDesignUpdate( final ExpressionExperiment ee ) {
        try {
            return this.expressionExperimentDao.getLastArrayDesignUpdate( ee );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getLastArrayDesignUpdate(ExpressionExperiment expressionExperiment, java.lang.Class eventType)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getLastLinkAnalysis(Collection)
     */
    public Map<Long, AuditEvent> getLastLinkAnalysis( final Collection<Long> ids ) {
        try {
            return getLastEvent( ids, LinkAnalysisEvent.Factory.newInstance() );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getLastLinkAnalysis(Collection ids)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getLastMissingValueAnalysis(Collection)
     */
    public Map<Long, AuditEvent> getLastMissingValueAnalysis( final Collection<Long> ids ) {
        try {
            return getLastEvent( ids, MissingValueAnalysisEvent.Factory.newInstance() );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getLastMissingValueAnalysis(Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getLastProcessedDataUpdate(Collection)
     */
    public Map<Long, AuditEvent> getLastProcessedDataUpdate( final Collection<Long> ids ) {
        try {
            return getLastEvent( ids, ProcessedVectorComputationEvent.Factory.newInstance() );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getLastRankComputation(Collection ids)' --> " + th,
                    th );
        }
    }

    /**
     * @see ExpressionExperimentService#getLastTroubleEvent(Collection)
     */
    public Map<Long, AuditEvent> getLastTroubleEvent( final Collection<Long> ids ) {
        try {
            Collection<ExpressionExperiment> ees = this.loadMultiple( ids );

            // this checks the array designs, too.
            Map<Auditable, AuditEvent> directEvents = this.getAuditEventDao().getLastOutstandingTroubleEvents( ees );

            Map<Long, AuditEvent> troubleMap = new HashMap<Long, AuditEvent>();
            for ( Auditable a : directEvents.keySet() ) {
                troubleMap.put( a.getId(), directEvents.get( a ) );
            }

            return troubleMap;
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getLastTroubleEvent(Collection ids)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getLastValidationEvent(Collection)
     */
    public Map<Long, AuditEvent> getLastValidationEvent( final Collection<Long> ids ) {
        try {
            return getLastEvent( ids, ValidatedFlagEvent.Factory.newInstance() );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getLastValidationEvent(Collection ids)' --> " + th,
                    th );
        }
    }

    /**
     * @see ExpressionExperimentService#getPerTaxonCount()
     */
    public Map<Taxon, Long> getPerTaxonCount() {
        try {
            return this.expressionExperimentDao.getPerTaxonCount();
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getPerTaxonCount()' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getPopulatedFactorCounts(Collection)
     */
    public Map getPopulatedFactorCounts( final Collection<Long> ids ) {
        try {
            return this.expressionExperimentDao.getPopulatedFactorCounts( ids );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getPopulatedFactorCounts(Collection ids)' --> " + th,
                    th );
        }
    }

    /**
     * @see ExpressionExperimentService#getPopulatedFactorCountsExcludeBatch(Collection)
     */
    public Map getPopulatedFactorCountsExcludeBatch( final Collection<Long> ids ) {
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
    public Collection getPreferredQuantitationType( final ExpressionExperiment ee ) {
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

    /**
     * @see ExpressionExperimentService#getPreferredDesignElementDataVectorCount(ExpressionExperiment)
     */
    public Integer getProcessedExpressionVectorCount( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.expressionExperimentDao.getProcessedExpressionVectorCount( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getPreferredDesignElementDataVectorCount(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    public SampleCoexpressionAnalysisDao getSampleCoexpressionAnalysisDao() {
        return sampleCoexpressionAnalysisDao;
    }

    public PrincipalComponentAnalysisDao getPrincipalComponentAnalysisDao() {
        return principalComponentAnalysisDao;
    }

    /**
     * @see ExpressionExperimentService#getQuantitationTypeCountById(java.lang.Long)
     */
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

    /**
     * @see ExpressionExperimentService#getSampleRemovalEvents(Collection)
     */
    public Map getSampleRemovalEvents( final Collection expressionExperiments ) {
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
    public Collection getSamplingOfVectors( final QuantitationType quantitationType, final Integer limit ) {
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
    public Taxon getTaxon( final Long ExpressionExperimentID ) {
        try {
            return this.expressionExperimentDao.getTaxon( ExpressionExperimentID );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getTaxon(java.lang.Long ExpressionExperimentID)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#load(java.lang.Long)
     */
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
    public Collection<ExpressionExperiment> loadAll() {
        try {
            return ( Collection<ExpressionExperiment> ) this.expressionExperimentDao.loadAll();
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.loadAll()' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#loadAllValueObjects()
     */
    public Collection<ExpressionExperimentValueObject> loadAllValueObjects() {
        try {
            return this.handleLoadAllValueObjects();
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.loadAllValueObjects()' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#loadMultiple(Collection)
     */
    public Collection<ExpressionExperiment> loadMultiple( final Collection<Long> ids ) {
        try {
            return ( Collection<ExpressionExperiment> ) this.expressionExperimentDao.load( ids );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.loadMultiple(Collection ids)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#loadValueObjects(Collection)
     */
    @Override
    public Collection<ExpressionExperimentValueObject> loadValueObjects( final Collection<Long> ids ) {
        try {
            /*
             * NOTE: Don't try and call this.loadMultiple(ids) to have security filter out experiments. The security
             * filtering just doesn't work. You need to call loadMultiple before calling loadValueObjects.
             */
            return this.expressionExperimentDao.loadValueObjects( ids );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.loadValueObjects(Collection ids)' --> " + th, th );
        }
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
    public void update( final ExpressionExperiment expressionExperiment ) {
        try {
            this.expressionExperimentDao.update( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.update(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    @Override
    public List<ExpressionExperiment> browse( Integer start, Integer limit ) {
        return this.expressionExperimentDao.browse( start, limit );
    }

    @Override
    public List<ExpressionExperiment> browse( Integer start, Integer limit, String orderField, boolean descending ) {
        return this.expressionExperimentDao.browse( start, limit, orderField, descending );
    }

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
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#browse(java.lang.Intege,
     * java.lang.Integer, java.lang.String, boolean)
     */

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#count()
     */
    @Override
    public Integer count() {
        return this.expressionExperimentDao.count();
    }

    @Override
    public Collection<ExpressionExperiment> findByAccession( String accession ) {
        return this.expressionExperimentDao.findByAccession( accession );
    }

    public ExpressionExperiment findByQuantitationType( QuantitationType type ) {
        return this.expressionExperimentDao.findByQuantitationType( type );
    }

    @Override
    public List<ExpressionExperiment> findByTaxon( Taxon taxon, int limit ) {
        return this.expressionExperimentDao.findByTaxon( taxon, limit );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#findByUpdatedLimit(java.util.Collection,
     * java.lang.Integer)
     */
    public List<ExpressionExperiment> findByUpdatedLimit( Collection<Long> ids, Integer limit ) {
        return this.expressionExperimentDao.findByUpdatedLimit( ids, limit );
    }

    @Override
    public List<ExpressionExperiment> findByUpdatedLimit( int limit ) {
        return this.expressionExperimentDao.findByUpdatedLimit( limit );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentService#getBioAssayDimensions(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getBioAssayDimensions( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.experiment.ExpressionExperimentService#getProcessedDataVectors(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment)
     */
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment ee ) {
        return this.expressionExperimentDao.getProcessedDataVectors( ee );
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

    @Override
    public List<ExpressionExperiment> loadAllOrdered( String orderField, boolean descending ) {
        return this.expressionExperimentDao.loadAllOrdered( orderField, descending );
    }

    @Override
    public List<ExpressionExperiment> loadAllTaxonOrdered( String orderField, boolean descending, Taxon taxon ) {
        return this.expressionExperimentDao.loadAllTaxonOrdered( orderField, descending, taxon );
    }

    @Override
    public List<ExpressionExperiment> loadAllTaxon( Taxon taxon ) {
        return this.expressionExperimentDao.loadAllTaxon( taxon );
    }

    @Override
    public Collection<ExpressionExperiment> loadLackingFactors() {
        return this.expressionExperimentDao.loadLackingFactors();
    }

    @Override
    public Collection<ExpressionExperiment> loadLackingTags() {
        return this.expressionExperimentDao.loadLackingTags();
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
    public Collection<ExpressionExperiment> loadMyExpressionExperiments() {
        return loadAll();
    }

    /*
     * Note: implemented via SpringSecurity.
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#loadUserOwnedExpressionExperiments()
     */
    public Collection<ExpressionExperiment> loadUserOwnedExpressionExperiments() {
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
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleDelete(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    protected void handleDelete( ExpressionExperiment ee ) throws Exception {

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
    protected Collection<ExpressionExperimentValueObject> handleLoadAllValueObjects() throws Exception {

        /* security will filter for us */
        // FIXME I'm not sure if this filtering actually works. See note for handleLoadValueObjects.
        Collection<ExpressionExperiment> experiments = this.loadAll();

        List<Long> filteredIds = new LinkedList<Long>();
        for ( ExpressionExperiment ee : experiments ) {
            filteredIds.add( ee.getId() );
        }

        /* now load the value objects for the filterd ids */
        return this.loadValueObjects( filteredIds );

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
     * Get the terms associated this expression experiment.
     */
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
     * @param uri
     * @return
     */
    private String getLabelFromUri( String uri ) {
        OntologyResource resource = ontologyService.getResource( uri );
        if ( resource != null ) return resource.getLabel();

        return null;
    }

}