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
package ubic.gemma.model.expression.experiment;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.AuditableDao;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ValidatedFlagEvent;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.monitor.Monitored;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService
 */
public class ExpressionExperimentServiceImpl extends
        ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase {

    private Log log = LogFactory.getLog( this.getClass() );

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
            last = getLastAuditEvent( experiment, type );
            lastEventMap.put( experiment.getId(), last );
        }
        return lastEventMap;
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getExpressionExperimentDao().countAll();
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleCreate(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    protected ExpressionExperiment handleCreate( ExpressionExperiment expressionExperiment ) throws Exception {
        return ( ExpressionExperiment ) this.getExpressionExperimentDao().create( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleDelete(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void handleDelete( ExpressionExperiment ee ) throws Exception {

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

        // Remove differential expression analyses
        Collection<DifferentialExpressionAnalysis> diffAnalyses = this.getDifferentialExpressionAnalysisDao()
                .findByInvestigation( ee );
        for ( DifferentialExpressionAnalysis de : diffAnalyses ) {
            Long toDelete = de.getId();
            this.getDifferentialExpressionAnalysisDao().remove( toDelete );
        }

        // Remove probe2probe links
        this.getProbe2ProbeCoexpressionDao().deleteLinks( ee );

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

                eeset.getExperiments().remove( ee );
                this.getExpressionExperimentDao().update( eeset );

            }
        }

        this.getExpressionExperimentDao().remove( ee );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFind(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    protected ExpressionExperiment handleFind( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().find( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByAccession(ubic.gemma.model
     * .common.description.DatabaseEntry)
     */
    @Override
    protected ExpressionExperiment handleFindByAccession( DatabaseEntry accession ) throws Exception {
        return this.getExpressionExperimentDao().findByAccession( accession );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByBibliographicReference(ubic
     * .gemma.model.common.description.BibliographicReference)
     */
    @Override
    protected Collection<ExpressionExperiment> handleFindByBibliographicReference( BibliographicReference bibRef )
            throws Exception {
        return this.getExpressionExperimentDao().findByBibliographicReference( bibRef.getId() );
    }

    @Override
    protected ExpressionExperiment handleFindByBioMaterial( BioMaterial bm ) throws Exception {
        return this.getExpressionExperimentDao().findByBioMaterial( bm );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByExpressedGene(ubic.gemma.model
     * .genome.Gene, double)
     */
    @Override
    protected Collection<ExpressionExperiment> handleFindByExpressedGene( Gene gene, double rank ) throws Exception {
        return this.getExpressionExperimentDao().findByExpressedGene( gene, rank );
    }

    @Override
    protected ExpressionExperiment handleFindByFactorValue( FactorValue factorValue ) throws Exception {
        return this.getExpressionExperimentDao().findByFactorValue( factorValue );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByGene(ubic.gemma.model.genome
     * .Gene)
     */
    @Override
    protected Collection<ExpressionExperiment> handleFindByGene( Gene gene ) throws Exception {
        return this.getExpressionExperimentDao().findByGene( gene );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByInvestigator(ubic.gemma.model
     * .common.auditAndSecurity.Contact)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<ExpressionExperiment> handleFindByInvestigator( Contact investigator ) throws Exception {
        return this.getExpressionExperimentDao().findByInvestigator( investigator );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByName(java.lang.String)
     */
    @Override
    protected ExpressionExperiment handleFindByName( String name ) throws Exception {
        return this.getExpressionExperimentDao().findByName( name );
    }

    @Override
    protected ExpressionExperiment handleFindByShortName( String shortName ) throws Exception {
        return this.getExpressionExperimentDao().findByShortName( shortName );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByTaxon(ubic.gemma.model.genome
     * .Taxon)
     */
    @Override
    protected Collection<ExpressionExperiment> handleFindByTaxon( Taxon taxon ) throws Exception {
        return this.getExpressionExperimentDao().findByTaxon( taxon );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindOrCreate(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    protected ExpressionExperiment handleFindOrCreate( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().findOrCreate( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetArrayDesignsUsed(ubic.gemma.model
     * .expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Collection<ArrayDesign> handleGetArrayDesignsUsed( ExpressionExperiment expressionExperiment ) {
        return this.getExpressionExperimentDao().getArrayDesignsUsed( expressionExperiment );
    }

    @Override
    protected Collection<Gene> handleGetAssayedGenes( ExpressionExperiment ee, Double rankThreshold ) throws Exception {
        return this.getExpressionExperimentDao().getAssayedGenes( ee, rankThreshold );
    }

    @Override
    protected Collection<CompositeSequence> handleGetAssayedProbes( ExpressionExperiment expressionExperiment,
            Double rankThreshold ) throws Exception {
        return this.getExpressionExperimentDao().getAssayedProbes( expressionExperiment, rankThreshold );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetBioMaterialCount(ubic.gemma.model
     * .expression.experiment.ExpressionExperiment)
     */
    @Override
    protected long handleGetBioMaterialCount( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().getBioMaterialCount( expressionExperiment );
    }

    @Override
    protected long handleGetDesignElementDataVectorCountById( long id ) throws Exception {
        return this.getExpressionExperimentDao().getDesignElementDataVectorCountById( id );
    }

    /*
     * (non-Javadoc) This only returns 1 taxon, the 1st taxon as decided by the join which ever that is. The good news
     * is as a buisness rule we only allow 1 taxon per EE.
     */

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetDesignElementDataVectors(ubic
     * .gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection)
     */
    @Override
    protected Collection handleGetDesignElementDataVectors( Collection quantitationTypes ) throws Exception {
        return this.getExpressionExperimentDao().getDesignElementDataVectors( quantitationTypes );
    }

    @Override
    protected Collection handleGetDesignElementDataVectors( Collection designElements, QuantitationType quantitationType )
            throws Exception {
        return this.getExpressionExperimentDao().getDesignElementDataVectors( designElements, quantitationType );
    }

    @Override
    protected Map handleGetLastArrayDesignUpdate( Collection expressionExperiments, Class type ) throws Exception {
        return this.getExpressionExperimentDao().getLastArrayDesignUpdate( expressionExperiments, type );
    }

    @Override
    @SuppressWarnings("unchecked")
    protected AuditEvent handleGetLastArrayDesignUpdate( ExpressionExperiment ee, Class eventType ) throws Exception {
        return this.getExpressionExperimentDao().getLastArrayDesignUpdate( ee, eventType );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastLinkAnalysis(java.util.Collection
     * )
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetLastLinkAnalysis( Collection ids ) throws Exception {

        return getLastEvent( ids, LinkAnalysisEvent.Factory.newInstance() );

    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastMissingValueAnalysis(java
     * .util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetLastMissingValueAnalysis( Collection ids ) throws Exception {
        return getLastEvent( ids, MissingValueAnalysisEvent.Factory.newInstance() );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastRankComputation(java.util
     * .Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetLastProcessedDataUpdate( Collection ids ) throws Exception {
        return getLastEvent( ids, ProcessedVectorComputationEvent.Factory.newInstance() );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastTroubleEvent(java.util.Collection
     * )
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetLastTroubleEvent( Collection /* <Long> */ids ) throws Exception {
        Map<Long, Collection<AuditEvent>> eeEvents = this.getExpressionExperimentDao().getAuditEvents( ids );
        Map<Long, Map<Long, Collection<AuditEvent>>> adEvents = this.getExpressionExperimentDao()
                .getArrayDesignAuditEvents( ids );
        Map<Long, AuditEvent> troubleMap = new HashMap<Long, AuditEvent>();
        for ( Long eeId : eeEvents.keySet() ) {

            /*
             * first check for trouble events on the expression experiment itself...
             */
            Collection<AuditEvent> events = eeEvents.get( eeId );
            AuditEvent troubleEvent = null;
            if ( events != null ) {
                troubleEvent = getLastOutstandingTroubleEvent( events );
                if ( troubleEvent != null ) {
                    troubleMap.put( eeId, troubleEvent );
                    continue;
                }
            }

            /*
             * if there was no trouble on the expression experiment, check the component array designs...
             */
            Map<Long, Collection<AuditEvent>> myAdEvents = adEvents.get( eeId );
            if ( myAdEvents != null ) {
                for ( Long adId : myAdEvents.keySet() ) {

                    events = myAdEvents.get( adId );
                    if ( events == null ) continue;

                    AuditEvent adTroubleEvent = getLastOutstandingTroubleEvent( events );
                    if ( adTroubleEvent != null )
                        if ( troubleEvent == null || troubleEvent.getDate().before( adTroubleEvent.getDate() ) )
                            troubleEvent = adTroubleEvent;

                }

                if ( troubleEvent != null ) troubleMap.put( eeId, troubleEvent );
            }
        }
        return troubleMap;
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastValidationEvent(java.util
     * .Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetLastValidationEvent( Collection ids ) throws Exception {
        return getLastEvent( ids, ValidatedFlagEvent.Factory.newInstance() );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetPerTaxonCount()
     */
    @Override
    protected Map handleGetPerTaxonCount() throws Exception {
        return this.getExpressionExperimentDao().getPerTaxonCount();
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetPreferredDesignElementDataVectorCount
     * (ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected long handleGetProcessedExpressionVectorCount( ExpressionExperiment expressionExperiment )
            throws Exception {
        return this.getExpressionExperimentDao().getProcessedExpressionVectorCount( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByBibliographicReference(ubic
     * .gemma.model.common.description.BibliographicReference)
     */
    @Override
    protected Collection<QuantitationType> handleGetPreferredQuantitationType( ExpressionExperiment ee )
            throws Exception {
        Collection<QuantitationType> preferredQuantitationTypes = new HashSet<QuantitationType>();
        handleThawLite( ee );
        for ( QuantitationType qt : ee.getQuantitationTypes() ) {
            if ( qt.getIsPreferred() ) {
                preferredQuantitationTypes.add( qt );
            }
        }
        return preferredQuantitationTypes;
    }

    //
    // @Override
    // protected QuantitationType handleGetMaskedPreferredQuantitationType( ExpressionExperiment ee ) throws Exception {
    // return this.getExpressionExperimentDao().getMaskedPreferredQuantitationType( ee );
    // }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetQuantitationTypeCountById(ubic
     * .gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Map handleGetQuantitationTypeCountById( Long Id ) throws Exception {
        return this.getExpressionExperimentDao().getQuantitationTypeCountById( Id );
    }

    @Override
    protected Collection handleGetQuantitationTypes( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().getQuantitationTypes( expressionExperiment );
    }

    @Override
    protected Collection<QuantitationType> handleGetQuantitationTypes( ExpressionExperiment expressionExperiment,
            ArrayDesign arrayDesign ) throws Exception {
        return this.getExpressionExperimentDao().getQuantitationTypes( expressionExperiment, arrayDesign );
    }

    @Override
    protected Collection<DesignElementDataVector> handleGetSamplingOfVectors( QuantitationType quantitationType,
            Integer limit ) throws Exception {
        return this.getExpressionExperimentDao().getSamplingOfVectors( quantitationType, limit );
    }

    @Override
    protected Taxon handleGetTaxon( Long id ) {
        return this.getExpressionExperimentDao().getTaxon( id );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoad(java.lang.Long)
     */
    @Override
    protected ExpressionExperiment handleLoad( Long id ) throws Exception {
        return ( ExpressionExperiment ) this.getExpressionExperimentDao().load( id );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoadAll()
     */
    @Override
    @Monitored
    protected Collection<ExpressionExperiment> handleLoadAll() throws Exception {
        return this.getExpressionExperimentDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoadAllValueObjects()
     */
    @Override
    protected Collection<ExpressionExperimentValueObject> handleLoadAllValueObjects() throws Exception {
        return this.getExpressionExperimentDao().loadAllValueObjects();
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoadMultiple(java.util.Collection)
     */
    @Override
    protected Collection<ExpressionExperiment> handleLoadMultiple( Collection ids ) throws Exception {
        Collection<ExpressionExperiment> ees = this.getExpressionExperimentDao().load( ids );
        return ees;
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoadValueObjects(java.util.Collection
     * )
     */
    @Override
    protected Collection<ExpressionExperimentValueObject> handleLoadValueObjects( Collection ids ) throws Exception {
        return this.getExpressionExperimentDao().loadValueObjects( ids );
    }

    @Override
    protected void handleThaw( ExpressionExperiment expressionExperiment ) throws Exception {
        this.getExpressionExperimentDao().thaw( expressionExperiment );
    }

    @Override
    protected void handleThawLite( ExpressionExperiment expressionExperiment ) throws Exception {
        this.getExpressionExperimentDao().thawBioAssays( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleUpdate(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    protected void handleUpdate( ExpressionExperiment expressionExperiment ) throws Exception {
        this.getExpressionExperimentDao().update( expressionExperiment );
    }

    @Override
    protected Map handleGetAnnotationCounts( Collection ids ) throws Exception {
        return this.getExpressionExperimentDao().getAnnotationCounts( ids );
    }

    @Override
    protected Map handleGetPopulatedFactorCounts( Collection ids ) throws Exception {
        return this.getExpressionExperimentDao().getPopulatedFactorCounts( ids );
    }

    @Override
    protected Map /* <ExpressionExperiment, Collection<AuditEvent>> */handleGetSampleRemovalEvents(
            Collection expressionExperiments ) throws Exception {
        return this.getExpressionExperimentDao().getSampleRemovalEvents( expressionExperiments );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetSubSets(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    protected Collection handleGetSubSets( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().getSubSets( expressionExperiment );
    }

    @Override
    protected Collection<ExpressionExperiment> handleFindByBioMaterials( Collection bioMaterials ) throws Exception {
        return this.getExpressionExperimentDao().findByBioMaterials( bioMaterials );
    }

    @Override
    protected Collection<ExpressionExperiment> handleFindByFactorValues( Collection factorValues ) throws Exception {
        return this.getExpressionExperimentDao().findByFactorValues( factorValues );
    }

    /*
     * (non-Javadoc)
     * @seeubic.gemma.model.expression.experiment.ExpressionExperimentService#getProcessedDataVectors(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment)
     */
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment ee ) {
        return this.getExpressionExperimentDao().getProcessedDataVectors( ee );
    }

    public ExpressionExperiment findByQuantitationType( QuantitationType type ) {
        return this.getExpressionExperimentDao().findByQuantitationType( type );
    }
}