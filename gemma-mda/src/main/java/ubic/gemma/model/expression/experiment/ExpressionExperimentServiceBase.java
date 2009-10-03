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

import ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.expression.experiment.ExpressionExperimentService</code>,
 * provides access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService
 */
public abstract class ExpressionExperimentServiceBase extends ubic.gemma.model.common.AuditableServiceImpl implements
        ubic.gemma.model.expression.experiment.ExpressionExperimentService {

    private ubic.gemma.model.expression.experiment.ExpressionExperimentDao expressionExperimentDao;

    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    private GeneCoexpressionAnalysisDao geneCoexpressionAnalysisDao;

    private ExpressionExperimentSetDao expressionExperimentSetDao;

    private ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao bioAssayDimensionDao;

    private ubic.gemma.model.expression.arrayDesign.ArrayDesignDao arrayDesignDao;

    private ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao probe2ProbeCoexpressionDao;

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#countAll()
     */
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.countAll()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#create(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment create(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleCreate( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.create(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#delete(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public void delete( final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        try {
            this.handleDelete( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.delete(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#find(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment find(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleFind( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.find(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#findByAccession(ubic.gemma.model.common.description.DatabaseEntry)
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment findByAccession(
            final ubic.gemma.model.common.description.DatabaseEntry accession ) {
        try {
            return this.handleFindByAccession( accession );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.findByAccession(ubic.gemma.model.common.description.DatabaseEntry accession)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#findByBibliographicReference(ubic.gemma.model.common.description.BibliographicReference)
     */
    public java.util.Collection<ExpressionExperiment> findByBibliographicReference(
            final ubic.gemma.model.common.description.BibliographicReference bibRef ) {
        try {
            return this.handleFindByBibliographicReference( bibRef );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.findByBibliographicReference(ubic.gemma.model.common.description.BibliographicReference bibRef)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#findByBioMaterial(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment findByBioMaterial(
            final ubic.gemma.model.expression.biomaterial.BioMaterial bm ) {
        try {
            return this.handleFindByBioMaterial( bm );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.findByBioMaterial(ubic.gemma.model.expression.biomaterial.BioMaterial bm)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#findByBioMaterials(java.util.Collection)
     */
    public java.util.Collection<ExpressionExperiment> findByBioMaterials( final java.util.Collection bioMaterials ) {
        try {
            return this.handleFindByBioMaterials( bioMaterials );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.findByBioMaterials(java.util.Collection bioMaterials)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#findByExpressedGene(ubic.gemma.model.genome.Gene,
     *      double)
     */
    public java.util.Collection<ExpressionExperiment> findByExpressedGene( final ubic.gemma.model.genome.Gene gene,
            final double rank ) {
        try {
            return this.handleFindByExpressedGene( gene, rank );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.findByExpressedGene(ubic.gemma.model.genome.Gene gene, double rank)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#findByFactorValue(ubic.gemma.model.expression.experiment.FactorValue)
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment findByFactorValue(
            final ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        try {
            return this.handleFindByFactorValue( factorValue );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.findByFactorValue(ubic.gemma.model.expression.experiment.FactorValue factorValue)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#findByFactorValues(java.util.Collection)
     */
    public java.util.Collection<ExpressionExperiment> findByFactorValues( final java.util.Collection factorValues ) {
        try {
            return this.handleFindByFactorValues( factorValues );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.findByFactorValues(java.util.Collection factorValues)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#findByGene(ubic.gemma.model.genome.Gene)
     */
    public java.util.Collection<ExpressionExperiment> findByGene( final ubic.gemma.model.genome.Gene gene ) {
        try {
            return this.handleFindByGene( gene );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.findByGene(ubic.gemma.model.genome.Gene gene)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#findByInvestigator(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    public java.util.Collection<ExpressionExperiment> findByInvestigator(
            final ubic.gemma.model.common.auditAndSecurity.Contact investigator ) {
        try {
            return this.handleFindByInvestigator( investigator );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.findByInvestigator(ubic.gemma.model.common.auditAndSecurity.Contact investigator)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#findByName(java.lang.String)
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment findByName( final java.lang.String name ) {
        try {
            return this.handleFindByName( name );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.findByName(java.lang.String name)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#findByShortName(java.lang.String)
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment findByShortName( final java.lang.String shortName ) {
        try {
            return this.handleFindByShortName( shortName );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.findByShortName(java.lang.String shortName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#findByTaxon(ubic.gemma.model.genome.Taxon)
     */
    public java.util.Collection<ExpressionExperiment> findByTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleFindByTaxon( taxon );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.findByTaxon(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#findOrCreate(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment findOrCreate(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleFindOrCreate( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.findOrCreate(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getAnnotationCounts(java.util.Collection)
     */
    public java.util.Map getAnnotationCounts( final java.util.Collection ids ) {
        try {
            return this.handleGetAnnotationCounts( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getAnnotationCounts(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getArrayDesignsUsed(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public java.util.Collection<ArrayDesign> getArrayDesignsUsed(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleGetArrayDesignsUsed( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getArrayDesignsUsed(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getBioMaterialCount(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public long getBioMaterialCount(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleGetBioMaterialCount( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getBioMaterialCount(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getDesignElementDataVectorCountById(long)
     */
    public long getDesignElementDataVectorCountById( final long id ) {
        try {
            return this.handleGetDesignElementDataVectorCountById( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getDesignElementDataVectorCountById(long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getDesignElementDataVectors(java.util.Collection)
     */
    public java.util.Collection getDesignElementDataVectors( final java.util.Collection quantitationTypes ) {
        try {
            return this.handleGetDesignElementDataVectors( quantitationTypes );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getDesignElementDataVectors(java.util.Collection quantitationTypes)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getDesignElementDataVectors(java.util.Collection,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public java.util.Collection getDesignElementDataVectors( final java.util.Collection designElements,
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        try {
            return this.handleGetDesignElementDataVectors( designElements, quantitationType );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getDesignElementDataVectors(java.util.Collection designElements, ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
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
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getLastArrayDesignUpdate(java.util.Collection,
     *      java.lang.Class)
     */
    public java.util.Map getLastArrayDesignUpdate( final java.util.Collection expressionExperiments,
            final java.lang.Class type ) {
        try {
            return this.handleGetLastArrayDesignUpdate( expressionExperiments, type );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getLastArrayDesignUpdate(java.util.Collection expressionExperiments, java.lang.Class type)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getLastArrayDesignUpdate(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      java.lang.Class)
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditEvent getLastArrayDesignUpdate(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment,
            final java.lang.Class eventType ) {
        try {
            return this.handleGetLastArrayDesignUpdate( expressionExperiment, eventType );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getLastArrayDesignUpdate(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment, java.lang.Class eventType)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getLastLinkAnalysis(java.util.Collection)
     */
    public java.util.Map getLastLinkAnalysis( final java.util.Collection ids ) {
        try {
            return this.handleGetLastLinkAnalysis( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getLastLinkAnalysis(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getLastMissingValueAnalysis(java.util.Collection)
     */
    public java.util.Map getLastMissingValueAnalysis( final java.util.Collection ids ) {
        try {
            return this.handleGetLastMissingValueAnalysis( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getLastMissingValueAnalysis(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getLastProcessedDataUpdate(java.util.Collection)
     */
    public java.util.Map getLastProcessedDataUpdate( final java.util.Collection ids ) {
        try {
            return this.handleGetLastProcessedDataUpdate( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getLastRankComputation(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getLastTroubleEvent(java.util.Collection)
     */
    public java.util.Map getLastTroubleEvent( final java.util.Collection ids ) {
        try {
            return this.handleGetLastTroubleEvent( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getLastTroubleEvent(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getLastValidationEvent(java.util.Collection)
     */
    public java.util.Map getLastValidationEvent( final java.util.Collection ids ) {
        try {
            return this.handleGetLastValidationEvent( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getLastValidationEvent(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getPerTaxonCount()
     */
    public java.util.Map getPerTaxonCount() {
        try {
            return this.handleGetPerTaxonCount();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getPerTaxonCount()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getPopulatedFactorCounts(java.util.Collection)
     */
    public java.util.Map getPopulatedFactorCounts( final java.util.Collection ids ) {
        try {
            return this.handleGetPopulatedFactorCounts( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getPopulatedFactorCounts(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getPreferredQuantitationType(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public java.util.Collection getPreferredQuantitationType(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment EE ) {
        try {
            return this.handleGetPreferredQuantitationType( EE );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getPreferredQuantitationType(ubic.gemma.model.expression.experiment.ExpressionExperiment EE)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getPreferredDesignElementDataVectorCount(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public long getProcessedExpressionVectorCount(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleGetProcessedExpressionVectorCount( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getPreferredDesignElementDataVectorCount(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getQuantitationTypeCountById(java.lang.Long)
     */
    public java.util.Map getQuantitationTypeCountById( final java.lang.Long Id ) {
        try {
            return this.handleGetQuantitationTypeCountById( Id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getQuantitationTypeCountById(java.lang.Long Id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getQuantitationTypes(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public java.util.Collection getQuantitationTypes(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleGetQuantitationTypes( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getQuantitationTypes(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getQuantitationTypes(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.util.Collection<QuantitationType> getQuantitationTypes(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleGetQuantitationTypes( expressionExperiment, arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getQuantitationTypes(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment, ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getSampleRemovalEvents(java.util.Collection)
     */
    public java.util.Map getSampleRemovalEvents( final java.util.Collection expressionExperiments ) {
        try {
            return this.handleGetSampleRemovalEvents( expressionExperiments );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getSampleRemovalEvents(java.util.Collection expressionExperiments)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getSamplingOfVectors(ubic.gemma.model.common.quantitationtype.QuantitationType,
     *      java.lang.Integer)
     */
    public java.util.Collection getSamplingOfVectors(
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType,
            final java.lang.Integer limit ) {
        try {
            return this.handleGetSamplingOfVectors( quantitationType, limit );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getSamplingOfVectors(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType, java.lang.Integer limit)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getSubSets(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public java.util.Collection getSubSets(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleGetSubSets( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getSubSets(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#getTaxon(java.lang.Long)
     */
    public ubic.gemma.model.genome.Taxon getTaxon( final java.lang.Long ExpressionExperimentID ) {
        try {
            return this.handleGetTaxon( ExpressionExperimentID );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.getTaxon(java.lang.Long ExpressionExperimentID)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#load(java.lang.Long)
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.load(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#loadAll()
     */
    public java.util.Collection<ExpressionExperiment> loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.loadAll()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#loadAllValueObjects()
     */
    public java.util.Collection<ExpressionExperimentValueObject> loadAllValueObjects() {
        try {
            return this.handleLoadAllValueObjects();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.loadAllValueObjects()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#loadMultiple(java.util.Collection)
     */
    public java.util.Collection<ExpressionExperiment> loadMultiple( final java.util.Collection ids ) {
        try {
            return this.handleLoadMultiple( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.loadMultiple(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#loadValueObjects(java.util.Collection)
     */
    public java.util.Collection<ExpressionExperimentValueObject> loadValueObjects( final java.util.Collection ids ) {
        try {
            return this.handleLoadValueObjects( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.loadValueObjects(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>arrayDesign</code>'s DAO.
     */
    public void setArrayDesignDao( ubic.gemma.model.expression.arrayDesign.ArrayDesignDao arrayDesignDao ) {
        this.arrayDesignDao = arrayDesignDao;
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
    public void setExpressionExperimentDao(
            ubic.gemma.model.expression.experiment.ExpressionExperimentDao expressionExperimentDao ) {
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
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#thaw(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public void thaw( final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        try {
            this.handleThaw( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.thaw(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#thawLite(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public void thawLite( final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        try {
            this.handleThawLite( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.thawLite(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#update(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public void update( final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        try {
            this.handleUpdate( expressionExperiment );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.experiment.ExpressionExperimentServiceException(
                    "Error performing 'ubic.gemma.model.expression.experiment.ExpressionExperimentService.update(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)' --> "
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
    protected ubic.gemma.model.expression.experiment.ExpressionExperimentDao getExpressionExperimentDao() {
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
     * Performs the core logic for {@link #create(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExpressionExperiment handleCreate(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract void handleDelete(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExpressionExperiment handleFind(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByAccession(ubic.gemma.model.common.description.DatabaseEntry)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExpressionExperiment handleFindByAccession(
            ubic.gemma.model.common.description.DatabaseEntry accession ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #findByBibliographicReference(ubic.gemma.model.common.description.BibliographicReference)}
     */
    protected abstract java.util.Collection<ExpressionExperiment> handleFindByBibliographicReference(
            ubic.gemma.model.common.description.BibliographicReference bibRef ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByBioMaterial(ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExpressionExperiment handleFindByBioMaterial(
            ubic.gemma.model.expression.biomaterial.BioMaterial bm ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByBioMaterials(java.util.Collection)}
     */
    protected abstract java.util.Collection<ExpressionExperiment> handleFindByBioMaterials(
            java.util.Collection bioMaterials ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByExpressedGene(ubic.gemma.model.genome.Gene, double)}
     */
    protected abstract java.util.Collection<ExpressionExperiment> handleFindByExpressedGene(
            ubic.gemma.model.genome.Gene gene, double rank ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByFactorValue(ubic.gemma.model.expression.experiment.FactorValue)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExpressionExperiment handleFindByFactorValue(
            ubic.gemma.model.expression.experiment.FactorValue factorValue ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByFactorValues(java.util.Collection)}
     */
    protected abstract java.util.Collection<ExpressionExperiment> handleFindByFactorValues(
            java.util.Collection factorValues ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract java.util.Collection<ExpressionExperiment> handleFindByGene( ubic.gemma.model.genome.Gene gene )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByInvestigator(ubic.gemma.model.common.auditAndSecurity.Contact)}
     */
    protected abstract java.util.Collection<ExpressionExperiment> handleFindByInvestigator(
            ubic.gemma.model.common.auditAndSecurity.Contact investigator ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExpressionExperiment handleFindByName(
            java.lang.String name ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByShortName(java.lang.String)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExpressionExperiment handleFindByShortName(
            java.lang.String shortName ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection<ExpressionExperiment> handleFindByTaxon( ubic.gemma.model.genome.Taxon taxon )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExpressionExperiment handleFindOrCreate(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getAnnotationCounts(java.util.Collection)}
     */
    protected abstract java.util.Map handleGetAnnotationCounts( java.util.Collection ids ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getArrayDesignsUsed(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract java.util.Collection<ArrayDesign> handleGetArrayDesignsUsed(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getBioMaterialCount(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract long handleGetBioMaterialCount(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getDesignElementDataVectorCountById(long)}
     */
    protected abstract long handleGetDesignElementDataVectorCountById( long id ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getDesignElementDataVectors(java.util.Collection)}
     */
    protected abstract java.util.Collection handleGetDesignElementDataVectors( java.util.Collection quantitationTypes )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getDesignElementDataVectors(java.util.Collection, ubic.gemma.model.common.quantitationtype.QuantitationType)}
     */
    protected abstract java.util.Collection handleGetDesignElementDataVectors( java.util.Collection designElements,
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastArrayDesignUpdate(java.util.Collection, java.lang.Class)}
     */
    protected abstract java.util.Map handleGetLastArrayDesignUpdate( java.util.Collection expressionExperiments,
            java.lang.Class type ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getLastArrayDesignUpdate(ubic.gemma.model.expression.experiment.ExpressionExperiment, java.lang.Class)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.AuditEvent handleGetLastArrayDesignUpdate(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment, java.lang.Class eventType )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastLinkAnalysis(java.util.Collection)}
     */
    protected abstract java.util.Map handleGetLastLinkAnalysis( java.util.Collection ids ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastMissingValueAnalysis(java.util.Collection)}
     */
    protected abstract java.util.Map handleGetLastMissingValueAnalysis( java.util.Collection ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastProcessedDataUpdate(java.util.Collection)}
     */
    protected abstract java.util.Map handleGetLastProcessedDataUpdate( java.util.Collection ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastTroubleEvent(java.util.Collection)}
     */
    protected abstract java.util.Map handleGetLastTroubleEvent( java.util.Collection ids ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastValidationEvent(java.util.Collection)}
     */
    protected abstract java.util.Map handleGetLastValidationEvent( java.util.Collection ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getPerTaxonCount()}
     */
    protected abstract java.util.Map handleGetPerTaxonCount() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getPopulatedFactorCounts(java.util.Collection)}
     */
    protected abstract java.util.Map handleGetPopulatedFactorCounts( java.util.Collection ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getPreferredQuantitationType(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract java.util.Collection handleGetPreferredQuantitationType(
            ubic.gemma.model.expression.experiment.ExpressionExperiment EE ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getPreferredDesignElementDataVectorCount(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract long handleGetProcessedExpressionVectorCount(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getQuantitationTypeCountById(java.lang.Long)}
     */
    protected abstract java.util.Map handleGetQuantitationTypeCountById( java.lang.Long Id ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getQuantitationTypes(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract java.util.Collection handleGetQuantitationTypes(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getQuantitationTypes(ubic.gemma.model.expression.experiment.ExpressionExperiment, ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract Collection<ubic.gemma.model.common.quantitationtype.QuantitationType> handleGetQuantitationTypes(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getSampleRemovalEvents(java.util.Collection)}
     */
    protected abstract java.util.Map handleGetSampleRemovalEvents( java.util.Collection expressionExperiments )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getSamplingOfVectors(ubic.gemma.model.common.quantitationtype.QuantitationType, java.lang.Integer)}
     */
    protected abstract java.util.Collection<DesignElementDataVector> handleGetSamplingOfVectors(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType, java.lang.Integer limit )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getSubSets(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract java.util.Collection handleGetSubSets(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getTaxon(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.genome.Taxon handleGetTaxon( java.lang.Long ExpressionExperimentID )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.expression.experiment.ExpressionExperiment handleLoad( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<ExpressionExperiment> handleLoadAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAllValueObjects()}
     */
    protected abstract java.util.Collection<ExpressionExperimentValueObject> handleLoadAllValueObjects()
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadMultiple(java.util.Collection)}
     */
    protected abstract java.util.Collection<ExpressionExperiment> handleLoadMultiple( java.util.Collection ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadValueObjects(java.util.Collection)}
     */
    protected abstract java.util.Collection<ExpressionExperimentValueObject> handleLoadValueObjects(
            java.util.Collection ids ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract void handleThaw( ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thawLite(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract void handleThawLite(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract void handleUpdate(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

}