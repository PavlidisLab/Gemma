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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.common.Auditable;
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
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
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
@Service
public class ExpressionExperimentServiceImpl extends
        ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase {

    private Log log = LogFactory.getLog( this.getClass() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#browse(java.lang.Integer,
     * java.lang.Integer)
     */
    @Override
    public List<ExpressionExperiment> browse( Integer start, Integer limit ) {
        return this.getExpressionExperimentDao().browse( start, limit );
    }

    @Override
    public List<ExpressionExperiment> browse( Integer start, Integer limit, String orderField, boolean descending ) {
        return this.getExpressionExperimentDao().browse( start, limit, orderField, descending );
    }

    public List<ExpressionExperiment> browseSpecificIds( Integer start, Integer limit, Collection<Long> ids ) {
        return this.getExpressionExperimentDao().browseSpecificIds( start, limit, ids );
    }

    @Override
    public List<ExpressionExperiment> browseSpecificIds( Integer start, Integer limit, String orderField,
            boolean descending, Collection<Long> ids ) {
        return this.getExpressionExperimentDao().browseSpecificIds( start, limit, orderField, descending, ids );
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
        return this.getExpressionExperimentDao().count();
    }

    @Override
    public Collection<ExpressionExperiment> findByAccession( String accession ) {
        return this.getExpressionExperimentDao().findByAccession( accession );
    }

    public ExpressionExperiment findByQuantitationType( QuantitationType type ) {
        return this.getExpressionExperimentDao().findByQuantitationType( type );
    }

    @Override
    public List<ExpressionExperiment> findByTaxon( Taxon taxon, int limit ) {
        return this.getExpressionExperimentDao().findByTaxon( taxon, limit );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService#findByUpdatedLimit(java.util.Collection,
     * java.lang.Integer)
     */
    public List<ExpressionExperiment> findByUpdatedLimit( Collection<Long> ids, Integer limit ) {
        return this.getExpressionExperimentDao().findByUpdatedLimit( ids, limit );
    }

    @Override
    public List<ExpressionExperiment> findByUpdatedLimit( int limit ) {
        return this.getExpressionExperimentDao().findByUpdatedLimit( limit );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentService#getBioAssayDimensions(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment ) {
        return this.getExpressionExperimentDao().getBioAssayDimensions( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.experiment.ExpressionExperimentService#getProcessedDataVectors(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment)
     */
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment ee ) {
        return this.getExpressionExperimentDao().getProcessedDataVectors( ee );
    }

    @Override
    public Collection<Long> getUntroubled( Collection<Long> ids ) {
        Collection<Long> firstPass = this.getExpressionExperimentDao().getUntroubled( ids );

        /*
         * Now check the array designs.
         */
        Map<ArrayDesign, Collection<Long>> ads = this.getExpressionExperimentDao().getArrayDesignsUsed( firstPass );
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
        return this.getExpressionExperimentDao().loadAllOrdered( orderField, descending );
    }

    @Override
    public List<ExpressionExperiment> loadAllTaxonOrdered( String orderField, boolean descending, Taxon taxon ) {
        return this.getExpressionExperimentDao().loadAllTaxonOrdered( orderField, descending, taxon );
    }

    @Override
    public List<ExpressionExperiment> loadAllTaxon( Taxon taxon ) {
        return this.getExpressionExperimentDao().loadAllTaxon( taxon );
    }

    @Override
    public Collection<ExpressionExperiment> loadLackingFactors() {
        return this.getExpressionExperimentDao().loadLackingFactors();
    }

    @Override
    public Collection<ExpressionExperiment> loadLackingTags() {
        return this.getExpressionExperimentDao().loadLackingTags();
    }

    @Override
    public List<ExpressionExperiment> loadMultipleOrdered( String orderField, boolean descending, Collection<Long> ids ) {
        return this.getExpressionExperimentDao().loadMultipleOrdered( orderField, descending, ids );
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

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getExpressionExperimentDao().countAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleCreate(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    protected ExpressionExperiment handleCreate( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().create( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleDelete(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
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

        // Remove differential expression analyses
        Collection<DifferentialExpressionAnalysis> diffAnalyses = this.getDifferentialExpressionAnalysisDao()
                .findByInvestigation( ee );
        for ( DifferentialExpressionAnalysis de : diffAnalyses ) {
            Long toDelete = de.getId();
            this.getDifferentialExpressionAnalysisDao().remove( toDelete );
        }

        // remove any sample coexpression matrices
        this.getSampleCoexpressionAnalysisDao().remove( ee );

        // Remove PCA
        PrincipalComponentAnalysis pca = this.getPrincipalComponentAnalysisDao().findByExperiment( ee );
        if ( pca != null ) {
            this.getPrincipalComponentAnalysisDao().remove( pca );
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
                log.info( "Removing " + ee + " from " + eeset );
                eeset.getExperiments().remove( ee );
                this.getExpressionExperimentSetDao().update( eeset );

            }
        }

        this.getExpressionExperimentDao().remove( ee );
    }

    /*
     * (non-Javadoc)
     * 
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
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByAccession(ubic.gemma.model
     * .common.description.DatabaseEntry)
     */
    @Override
    protected Collection<ExpressionExperiment> handleFindByAccession( DatabaseEntry accession ) throws Exception {
        return this.getExpressionExperimentDao().findByAccession( accession );
    }

    /*
     * (non-Javadoc)
     * 
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

    @Override
    protected Collection<ExpressionExperiment> handleFindByBioMaterials( Collection<BioMaterial> bioMaterials )
            throws Exception {
        return this.getExpressionExperimentDao().findByBioMaterials( bioMaterials );
    }

    /*
     * (non-Javadoc)
     * 
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

    @Override
    protected Collection<ExpressionExperiment> handleFindByFactorValues( Collection factorValues ) throws Exception {
        return this.getExpressionExperimentDao().findByFactorValues( factorValues );
    }

    /*
     * (non-Javadoc)
     * 
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
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByInvestigator(ubic.gemma.model
     * .common.auditAndSecurity.Contact)
     */
    @Override
    protected Collection<ExpressionExperiment> handleFindByInvestigator( Contact investigator ) throws Exception {
        return this.getExpressionExperimentDao().findByInvestigator( investigator );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByName(java.lang.String)
     */
    @Override
    protected ExpressionExperiment handleFindByName( String name ) throws Exception {
        return this.getExpressionExperimentDao().findByName( name );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByParentTaxon(ubic.gemma.model
     * .genome .Taxon)
     */
    @Override
    protected Collection<ExpressionExperiment> handleFindByParentTaxon( Taxon taxon ) throws Exception {
        return this.getExpressionExperimentDao().findByParentTaxon( taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByShortName(java.lang.String)
     */
    @Override
    protected ExpressionExperiment handleFindByShortName( String shortName ) throws Exception {
        return this.getExpressionExperimentDao().findByShortName( shortName );
    }

    /*
     * (non-Javadoc)
     * 
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
     * 
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
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetAnnotationCounts(java.util.Collection
     * )
     */
    @Override
    protected Map<Long, Integer> handleGetAnnotationCounts( Collection<Long> ids ) throws Exception {
        return this.getExpressionExperimentDao().getAnnotationCounts( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetArrayDesignsUsed(ubic.gemma.model
     * .expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Collection<ArrayDesign> handleGetArrayDesignsUsed( ExpressionExperiment expressionExperiment ) {
        return this.getExpressionExperimentDao().getArrayDesignsUsed( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetBioMaterialCount(ubic.gemma.model
     * .expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Integer handleGetBioMaterialCount( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().getBioMaterialCount( expressionExperiment );
    }

    @Override
    protected Integer handleGetDesignElementDataVectorCountById( long id ) throws Exception {
        return this.getExpressionExperimentDao().getDesignElementDataVectorCountById( id );
    }

    @Override
    protected Collection<DesignElementDataVector> handleGetDesignElementDataVectors(
            Collection<CompositeSequence> designElements, QuantitationType quantitationType ) throws Exception {
        return this.getExpressionExperimentDao().getDesignElementDataVectors( designElements, quantitationType );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetDesignElementDataVectors(ubic
     * .gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection)
     */
    @Override
    protected Collection<DesignElementDataVector> handleGetDesignElementDataVectors(
            Collection<QuantitationType> quantitationTypes ) throws Exception {
        return this.getExpressionExperimentDao().getDesignElementDataVectors( quantitationTypes );
    }

    @Override
    protected Map<Long, Date> handleGetLastArrayDesignUpdate( Collection<ExpressionExperiment> expressionExperiments )
            throws Exception {
        return this.getExpressionExperimentDao().getLastArrayDesignUpdate( expressionExperiments );
    }

    @Override
    protected Date handleGetLastArrayDesignUpdate( ExpressionExperiment ee ) throws Exception {
        return this.getExpressionExperimentDao().getLastArrayDesignUpdate( ee );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastLinkAnalysis(java.util.Collection
     * )
     */
    @Override
    protected Map<Long, AuditEvent> handleGetLastLinkAnalysis( Collection<Long> ids ) throws Exception {

        return getLastEvent( ids, LinkAnalysisEvent.Factory.newInstance() );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastMissingValueAnalysis(java
     * .util.Collection)
     */
    @Override
    protected Map<Long, AuditEvent> handleGetLastMissingValueAnalysis( Collection<Long> ids ) throws Exception {
        return getLastEvent( ids, MissingValueAnalysisEvent.Factory.newInstance() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastRankComputation(java.util
     * .Collection)
     */
    @Override
    protected Map<Long, AuditEvent> handleGetLastProcessedDataUpdate( Collection<Long> ids ) throws Exception {
        return getLastEvent( ids, ProcessedVectorComputationEvent.Factory.newInstance() );
    }

    /*
     * Note this is a little tricky since we have to reach through to check the ArrayDesigns. (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastTroubleEvent(java.util.Collection
     * )
     */
    @Override
    protected Map<Long, AuditEvent> handleGetLastTroubleEvent( Collection<Long> ids ) throws Exception {
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<ExpressionExperiment> ees = this.loadMultiple( ids );

        // this checks the array designs, too.
        Map<Auditable, AuditEvent> directEvents = this.getAuditEventDao().getLastOutstandingTroubleEvents( ees );

        Map<Long, AuditEvent> troubleMap = new HashMap<Long, AuditEvent>();
        for ( Auditable a : directEvents.keySet() ) {
            troubleMap.put( a.getId(), directEvents.get( a ) );
        }

        return troubleMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastValidationEvent(java.util
     * .Collection)
     */
    @Override
    protected Map<Long, AuditEvent> handleGetLastValidationEvent( Collection<Long> ids ) throws Exception {
        return getLastEvent( ids, ValidatedFlagEvent.Factory.newInstance() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetPerTaxonCount()
     */
    @Override
    protected Map<Taxon, Long> handleGetPerTaxonCount() throws Exception {
        return this.getExpressionExperimentDao().getPerTaxonCount();
    }

    @Override
    protected Map handleGetPopulatedFactorCounts( Collection<Long> ids ) throws Exception {
        return this.getExpressionExperimentDao().getPopulatedFactorCounts( ids );
    }

    @Override
    protected Map handleGetPopulatedFactorCountsExcludeBatch( Collection<Long> ids ) throws Exception {
        return this.getExpressionExperimentDao().getPopulatedFactorCountsExcludeBatch( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByBibliographicReference(ubic
     * .gemma.model.common.description.BibliographicReference)
     */
    @Override
    protected Collection<QuantitationType> handleGetPreferredQuantitationType( ExpressionExperiment ee )
            throws Exception {
        Collection<QuantitationType> preferredQuantitationTypes = new HashSet<QuantitationType>();

        Collection<QuantitationType> quantitationTypes = this.getQuantitationTypes( ee );

        ee = handleThawLite( ee ); // why?
        for ( QuantitationType qt : quantitationTypes ) {
            if ( qt.getIsPreferred() ) {
                preferredQuantitationTypes.add( qt );
            }
        }
        return preferredQuantitationTypes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetPreferredDesignElementDataVectorCount
     * (ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Integer handleGetProcessedExpressionVectorCount( ExpressionExperiment expressionExperiment )
            throws Exception {
        return this.getExpressionExperimentDao().getProcessedExpressionVectorCount( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetQuantitationTypeCountById(ubic
     * .gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Map<QuantitationType, Integer> handleGetQuantitationTypeCountById( Long Id ) throws Exception {
        return this.getExpressionExperimentDao().getQuantitationTypeCountById( Id );
    }

    @Override
    protected Collection<QuantitationType> handleGetQuantitationTypes( ExpressionExperiment expressionExperiment )
            throws Exception {
        return this.getExpressionExperimentDao().getQuantitationTypes( expressionExperiment );
    }

    @Override
    protected Collection<QuantitationType> handleGetQuantitationTypes( ExpressionExperiment expressionExperiment,
            ArrayDesign arrayDesign ) throws Exception {
        return this.getExpressionExperimentDao().getQuantitationTypes( expressionExperiment, arrayDesign );
    }

    @Override
    protected Map<ExpressionExperiment, Collection<AuditEvent>> handleGetSampleRemovalEvents(
            Collection expressionExperiments ) throws Exception {
        return this.getExpressionExperimentDao().getSampleRemovalEvents( expressionExperiments );
    }

    @Override
    protected Collection<DesignElementDataVector> handleGetSamplingOfVectors( QuantitationType quantitationType,
            Integer limit ) throws Exception {
        return this.getExpressionExperimentDao().getSamplingOfVectors( quantitationType, limit );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetSubSets(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    protected Collection<ExpressionExperimentSubSet> handleGetSubSets( ExpressionExperiment expressionExperiment )
            throws Exception {
        return this.getExpressionExperimentDao().getSubSets( expressionExperiment );
    }

    @Override
    protected Taxon handleGetTaxon( Long id ) {
        return this.getExpressionExperimentDao().getTaxon( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoad(java.lang.Long)
     */
    @Override
    protected ExpressionExperiment handleLoad( Long id ) throws Exception {
        return this.getExpressionExperimentDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoadAll()
     */
    @Override
    @Monitored
    protected Collection<ExpressionExperiment> handleLoadAll() throws Exception {
        return ( Collection<ExpressionExperiment> ) this.getExpressionExperimentDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoadAllValueObjects()
     */
    @Override
    protected Collection<ExpressionExperimentValueObject> handleLoadAllValueObjects() throws Exception {

        /* security will filter for us */
        // FIXME I'm not sure if this filtering actually works. See note for handleLoadValueObjects.
        Collection<ExpressionExperiment> experiments = this.loadAll();

        Collection<Long> filteredIds = new HashSet<Long>();
        for ( ExpressionExperiment ee : experiments ) {
            filteredIds.add( ee.getId() );
        }

        /* now load the value objects for the filterd ids */
        return this.loadValueObjects( filteredIds );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoadMultiple(java.util.Collection)
     */
    @Override
    protected Collection<ExpressionExperiment> handleLoadMultiple( Collection<Long> ids ) throws Exception {
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) this.getExpressionExperimentDao()
                .load( ids );
        return ees;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoadValueObjects(java.util.Collection
     * )
     */
    @Override
    protected Collection<ExpressionExperimentValueObject> handleLoadValueObjects( Collection<Long> ids )
            throws Exception {
        /*
         * NOTE: Don't try and call this.loadMultiple(ids) to have security filter out experiments. The security
         * filtering just doesn't work. You need to call loadMultiple before calling loadValueObjects.
         */
        return this.getExpressionExperimentDao().loadValueObjects( ids );
    }

    @Override
    protected ExpressionExperiment handleThaw( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().thaw( expressionExperiment );
    }

    @Override
    protected ExpressionExperiment handleThawLite( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().thawBioAssays( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleUpdate(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    protected void handleUpdate( ExpressionExperiment expressionExperiment ) throws Exception {
        this.getExpressionExperimentDao().update( expressionExperiment );
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

}