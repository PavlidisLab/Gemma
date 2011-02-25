/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.expression.experiment;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventDao;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;

/**
 * <p>
 * Spring Service base class for <code>ExpressionExperimentService</code>, provides access to all services and entities
 * referenced by this service.
 * </p>
 * 
 * @see ExpressionExperimentService
 */
public abstract class ExpressionExperimentServiceBase implements ExpressionExperimentService {

    @Autowired
    private ubic.gemma.model.expression.arrayDesign.ArrayDesignDao arrayDesignDao;

    @Autowired
    private AuditEventDao auditEventDao;

    @Autowired
    private ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao bioAssayDimensionDao;

    @Autowired
    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private ExpressionExperimentSetDao expressionExperimentSetDao;

    @Autowired
    private GeneCoexpressionAnalysisDao geneCoexpressionAnalysisDao;

    @Autowired
    private ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao probe2ProbeCoexpressionDao;

    /**
     * @see ExpressionExperimentService#countAll()
     */
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
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
            return this.handleCreate( expressionExperiment );
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
            this.handleDelete( expressionExperiment );
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
            return this.handleFind( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.find(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByAccession(ubic.gemma.model.common.description.DatabaseEntry)
     */
    public ExpressionExperiment findByAccession( final ubic.gemma.model.common.description.DatabaseEntry accession ) {
        try {
            return this.handleFindByAccession( accession );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findByAccession(ubic.gemma.model.common.description.DatabaseEntry accession)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByBibliographicReference(ubic.gemma.model.common.description.BibliographicReference)
     */
    public Collection<ExpressionExperiment> findByBibliographicReference(
            final ubic.gemma.model.common.description.BibliographicReference bibRef ) {
        try {
            return this.handleFindByBibliographicReference( bibRef );
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
            return this.handleFindByBioMaterial( bm );
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
            return this.handleFindByBioMaterials( bioMaterials );
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
            return this.handleFindByExpressedGene( gene, rank );
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
            return this.handleFindByFactorValue( factorValue );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findByFactorValue(FactorValue factorValue)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByFactorValues(Collection)
     */
    public Collection<ExpressionExperiment> findByFactorValues( final Collection factorValues ) {
        try {
            return this.handleFindByFactorValues( factorValues );
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
            return this.handleFindByGene( gene );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findByGene(ubic.gemma.model.genome.Gene gene)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#findByInvestigator(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    public Collection<ExpressionExperiment> findByInvestigator(
            final ubic.gemma.model.common.auditAndSecurity.Contact investigator ) {
        try {
            return this.handleFindByInvestigator( investigator );
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
            return this.handleFindByName( name );
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
            return this.handleFindByParentTaxon( taxon );
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
            return this.handleFindByShortName( shortName );
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
            return this.handleFindByTaxon( taxon );
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
            return this.handleFindOrCreate( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.findOrCreate(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getAnnotationCounts(Collection)
     */
    public Map getAnnotationCounts( final Collection<Long> ids ) {
        try {
            return this.handleGetAnnotationCounts( ids );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getAnnotationCounts(Collection ids)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getArrayDesignsUsed(ExpressionExperiment)
     */
    public Collection<ArrayDesign> getArrayDesignsUsed( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleGetArrayDesignsUsed( expressionExperiment );
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
            return this.handleGetBioMaterialCount( expressionExperiment );
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
            return this.handleGetDesignElementDataVectorCountById( id );
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
            return this.handleGetDesignElementDataVectors( designElements, quantitationType );
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
            return this.handleGetDesignElementDataVectors( quantitationTypes );
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
    public Map<ExpressionExperiment, AuditEvent> getLastArrayDesignUpdate( final Collection expressionExperiments,
            final java.lang.Class type ) {
        try {
            return this.handleGetLastArrayDesignUpdate( expressionExperiments, type );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getLastArrayDesignUpdate(Collection expressionExperiments, java.lang.Class type)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getLastArrayDesignUpdate(ExpressionExperiment, java.lang.Class)
     */
    public AuditEvent getLastArrayDesignUpdate( final ExpressionExperiment expressionExperiment,
            final java.lang.Class<? extends AuditEventType> eventType ) {
        try {
            return this.handleGetLastArrayDesignUpdate( expressionExperiment, eventType );
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
            return this.handleGetLastLinkAnalysis( ids );
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
            return this.handleGetLastMissingValueAnalysis( ids );
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
            return this.handleGetLastProcessedDataUpdate( ids );
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
            return this.handleGetLastTroubleEvent( ids );
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
            return this.handleGetLastValidationEvent( ids );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getLastValidationEvent(Collection ids)' --> " + th,
                    th );
        }
    }

    /**
     * @see ExpressionExperimentService#getPerTaxonCount()
     */
    public Map getPerTaxonCount() {
        try {
            return this.handleGetPerTaxonCount();
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
            return this.handleGetPopulatedFactorCounts( ids );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getPopulatedFactorCounts(Collection ids)' --> " + th,
                    th );
        }
    }

    /**
     * @see ExpressionExperimentService#getPreferredQuantitationType(ExpressionExperiment)
     */
    public Collection getPreferredQuantitationType( final ExpressionExperiment EE ) {
        try {
            return this.handleGetPreferredQuantitationType( EE );
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
            return this.handleGetProcessedExpressionVectorCount( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getPreferredDesignElementDataVectorCount(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getQuantitationTypeCountById(java.lang.Long)
     */
    public Map getQuantitationTypeCountById( final java.lang.Long Id ) {
        try {
            return this.handleGetQuantitationTypeCountById( Id );
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
            return this.handleGetQuantitationTypes( expressionExperiment );
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
            return this.handleGetQuantitationTypes( expressionExperiment, arrayDesign );
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
            return this.handleGetSampleRemovalEvents( expressionExperiments );
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
    public Collection getSamplingOfVectors(
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType,
            final java.lang.Integer limit ) {
        try {
            return this.handleGetSamplingOfVectors( quantitationType, limit );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getSamplingOfVectors(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType, java.lang.Integer limit)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getSubSets(ExpressionExperiment)
     */
    public Collection getSubSets( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleGetSubSets( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.getSubSets(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#getTaxon(java.lang.Long)
     */
    public ubic.gemma.model.genome.Taxon getTaxon( final java.lang.Long ExpressionExperimentID ) {
        try {
            return this.handleGetTaxon( ExpressionExperimentID );
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
            return this.handleLoad( id );
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
            return this.handleLoadAll();
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
            return this.handleLoadMultiple( ids );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.loadMultiple(Collection ids)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentService#loadValueObjects(Collection)
     */
    public Collection<ExpressionExperimentValueObject> loadValueObjects( final Collection<Long> ids ) {
        try {
            return this.handleLoadValueObjects( ids );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.loadValueObjects(Collection ids)' --> " + th, th );
        }
    }

    /**
     * Sets the reference to <code>arrayDesign</code>'s DAO.
     */
    public void setArrayDesignDao( ubic.gemma.model.expression.arrayDesign.ArrayDesignDao arrayDesignDao ) {
        this.arrayDesignDao = arrayDesignDao;
    }

    /**
     * @param auditEventDao the auditEventDao to set
     */
    public void setAuditEventDao( AuditEventDao auditEventDao ) {
        this.auditEventDao = auditEventDao;
    }

    /**
     * Sets the reference to <code>bioAssayDimension</code>'s DAO.
     */
    public void setBioAssayDimensionDao(
            ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao bioAssayDimensionDao ) {
        this.bioAssayDimensionDao = bioAssayDimensionDao;
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
            return this.handleThaw( expressionExperiment );
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
            return this.handleThawLite( expressionExperiment );
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
            this.handleUpdate( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ExpressionExperimentServiceException(
                    "Error performing 'ExpressionExperimentService.update(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>arrayDesign</code>'s DAO.
     */
    protected ubic.gemma.model.expression.arrayDesign.ArrayDesignDao getArrayDesignDao() {
        return this.arrayDesignDao;
    }

    /**
     * Gets the reference to <code>bioAssayDimension</code>'s DAO.
     */
    protected ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao getBioAssayDimensionDao() {
        return this.bioAssayDimensionDao;
    }

    /**
     * Gets the reference to <code>expressionExperiment</code>'s DAO.
     */
    protected ExpressionExperimentDao getExpressionExperimentDao() {
        return this.expressionExperimentDao;
    }

    /**
     * Gets the reference to <code>probe2ProbeCoexpression</code>'s DAO.
     */
    protected ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao getProbe2ProbeCoexpressionDao() {
        return this.probe2ProbeCoexpressionDao;
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #create(ExpressionExperiment)}
     */
    protected abstract ExpressionExperiment handleCreate( ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #delete(ExpressionExperiment)}
     */
    protected abstract void handleDelete( ExpressionExperiment expressionExperiment ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ExpressionExperiment)}
     */
    protected abstract ExpressionExperiment handleFind( ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByAccession(ubic.gemma.model.common.description.DatabaseEntry)}
     */
    protected abstract ExpressionExperiment handleFindByAccession(
            ubic.gemma.model.common.description.DatabaseEntry accession ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #findByBibliographicReference(ubic.gemma.model.common.description.BibliographicReference)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByBibliographicReference(
            ubic.gemma.model.common.description.BibliographicReference bibRef ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByBioMaterial(ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract ExpressionExperiment handleFindByBioMaterial(
            ubic.gemma.model.expression.biomaterial.BioMaterial bm ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByBioMaterials(Collection)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByBioMaterials( Collection<BioMaterial> bioMaterials )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByExpressedGene(ubic.gemma.model.genome.Gene, double)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByExpressedGene( ubic.gemma.model.genome.Gene gene,
            double rank ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByFactorValue(FactorValue)}
     */
    protected abstract ExpressionExperiment handleFindByFactorValue( FactorValue factorValue )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByFactorValues(Collection)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByFactorValues( Collection factorValues )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByGene( ubic.gemma.model.genome.Gene gene )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByInvestigator(ubic.gemma.model.common.auditAndSecurity.Contact)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByInvestigator(
            ubic.gemma.model.common.auditAndSecurity.Contact investigator ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract ExpressionExperiment handleFindByName( java.lang.String name ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByParentTaxon( ubic.gemma.model.genome.Taxon taxon )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByShortName(java.lang.String)}
     */
    protected abstract ExpressionExperiment handleFindByShortName( java.lang.String shortName )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByTaxon( ubic.gemma.model.genome.Taxon taxon )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ExpressionExperiment)}
     */
    protected abstract ExpressionExperiment handleFindOrCreate( ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getAnnotationCounts(Collection)}
     */
    protected abstract Map handleGetAnnotationCounts( Collection<Long> ids ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getArrayDesignsUsed(ExpressionExperiment)}
     */
    protected abstract Collection<ArrayDesign> handleGetArrayDesignsUsed( ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getBioMaterialCount(ExpressionExperiment)}
     */
    protected abstract Integer handleGetBioMaterialCount( ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getDesignElementDataVectorCountById(long)}
     */
    protected abstract Integer handleGetDesignElementDataVectorCountById( long id ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getDesignElementDataVectors(Collection, ubic.gemma.model.common.quantitationtype.QuantitationType)}
     */
    protected abstract Collection<DesignElementDataVector> handleGetDesignElementDataVectors(
            Collection<CompositeSequence> designElements,
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getDesignElementDataVectors(Collection)}
     */
    protected abstract Collection<DesignElementDataVector> handleGetDesignElementDataVectors(
            Collection<QuantitationType> quantitationTypes ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastArrayDesignUpdate(Collection, java.lang.Class)}
     */
    protected abstract Map<ExpressionExperiment, AuditEvent> handleGetLastArrayDesignUpdate(
            Collection<ExpressionExperiment> expressionExperiments, java.lang.Class<? extends AuditEventType> type )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastArrayDesignUpdate(ExpressionExperiment, java.lang.Class)}
     */
    protected abstract AuditEvent handleGetLastArrayDesignUpdate( ExpressionExperiment expressionExperiment,
            java.lang.Class eventType ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastLinkAnalysis(Collection)}
     */
    protected abstract Map<Long, AuditEvent> handleGetLastLinkAnalysis( Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastMissingValueAnalysis(Collection)}
     */
    protected abstract Map<Long, AuditEvent> handleGetLastMissingValueAnalysis( Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastProcessedDataUpdate(Collection)}
     */
    protected abstract Map<Long, AuditEvent> handleGetLastProcessedDataUpdate( Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastTroubleEvent(Collection)}
     */
    protected abstract Map<Long, AuditEvent> handleGetLastTroubleEvent( Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastValidationEvent(Collection)}
     */
    protected abstract Map<Long, AuditEvent> handleGetLastValidationEvent( Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getPerTaxonCount()}
     */
    protected abstract Map<Taxon, Long> handleGetPerTaxonCount() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getPopulatedFactorCounts(Collection)}
     */
    protected abstract Map handleGetPopulatedFactorCounts( Collection<Long> ids ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getPreferredQuantitationType(ExpressionExperiment)}
     */
    protected abstract Collection handleGetPreferredQuantitationType( ExpressionExperiment EE )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getPreferredDesignElementDataVectorCount(ExpressionExperiment)}
     */
    protected abstract Integer handleGetProcessedExpressionVectorCount( ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getQuantitationTypeCountById(java.lang.Long)}
     */
    protected abstract Map handleGetQuantitationTypeCountById( java.lang.Long Id ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getQuantitationTypes(ExpressionExperiment)}
     */
    protected abstract Collection<QuantitationType> handleGetQuantitationTypes(
            ExpressionExperiment expressionExperiment ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getQuantitationTypes(ExpressionExperiment, ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract Collection<ubic.gemma.model.common.quantitationtype.QuantitationType> handleGetQuantitationTypes(
            ExpressionExperiment expressionExperiment, ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getSampleRemovalEvents(Collection)}
     */
    protected abstract Map handleGetSampleRemovalEvents( Collection expressionExperiments ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getSamplingOfVectors(ubic.gemma.model.common.quantitationtype.QuantitationType, java.lang.Integer)}
     */
    protected abstract Collection<DesignElementDataVector> handleGetSamplingOfVectors(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType, java.lang.Integer limit )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getSubSets(ExpressionExperiment)}
     */
    protected abstract Collection handleGetSubSets( ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getTaxon(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.genome.Taxon handleGetTaxon( java.lang.Long ExpressionExperimentID )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ExpressionExperiment handleLoad( java.lang.Long id ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract Collection<ExpressionExperiment> handleLoadAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAllValueObjects()}
     */
    protected abstract Collection<ExpressionExperimentValueObject> handleLoadAllValueObjects()
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadMultiple(Collection)}
     */
    protected abstract Collection<ExpressionExperiment> handleLoadMultiple( Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadValueObjects(Collection)}
     */
    protected abstract Collection<ExpressionExperimentValueObject> handleLoadValueObjects( Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(ExpressionExperiment)}
     */
    protected abstract ExpressionExperiment handleThaw( ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thawLite(ExpressionExperiment)}
     */
    protected abstract ExpressionExperiment handleThawLite( ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ExpressionExperiment)}
     */
    protected abstract void handleUpdate( ExpressionExperiment expressionExperiment ) throws java.lang.Exception;

}