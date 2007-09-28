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

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.RankComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ValidatedFlagEvent;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentService
 */
public class ExpressionExperimentServiceImpl extends
        ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByExpressedGene(ubic.gemma.model.genome.Gene,
     *      double)
     */
    @Override
    protected Collection handleFindByExpressedGene( Gene gene, double rank ) throws Exception {
        return this.getExpressionExperimentDao().findByExpressedGene( gene, rank );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleFindByGene( Gene gene ) throws Exception {
        return this.getExpressionExperimentDao().findByGene( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected Collection handleFindByTaxon( Taxon taxon ) throws Exception {
        return this.getExpressionExperimentDao().findByTaxon( taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoadAllValueObjects()
     */
    @Override
    protected Collection handleLoadAllValueObjects() throws Exception {
        return this.getExpressionExperimentDao().loadAllValueObjects();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByInvestigator(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    @Override
    protected Collection handleFindByInvestigator( Contact investigator ) throws Exception {
        return this.getExpressionExperimentDao().findByInvestigator( investigator );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getExpressionExperimentDao().countAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByAccession(ubic.gemma.model.common.description.DatabaseEntry)
     */
    @Override
    protected ExpressionExperiment handleFindByAccession( DatabaseEntry accession ) throws Exception {
        return this.getExpressionExperimentDao().findByAccession( accession );
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
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindOrCreate(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected ExpressionExperiment handleFindOrCreate( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().findOrCreate( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleUpdate(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected void handleUpdate( ExpressionExperiment expressionExperiment ) throws Exception {
        this.getExpressionExperimentDao().update( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoadAll()
     */
    @Override
    protected Collection handleLoadAll() throws Exception {
        return this.getExpressionExperimentDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleDelete(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected void handleDelete( ExpressionExperiment ee ) throws Exception {
        this.getExpressionExperimentDao().remove( ee );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoad(java.lang.Long)
     */
    @Override
    protected ExpressionExperiment handleLoad( Long id ) throws Exception {
        return ( ExpressionExperiment ) this.getExpressionExperimentDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleCreate(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected ExpressionExperiment handleCreate( ExpressionExperiment expressionExperiment ) throws Exception {
        return ( ExpressionExperiment ) this.getExpressionExperimentDao().create( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFind(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected ExpressionExperiment handleFind( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().find( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetQuantitationTypeCountById(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Map handleGetQuantitationTypeCountById( Long Id ) throws Exception {
        return this.getExpressionExperimentDao().getQuantitationTypeCountById( Id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetArrayDesignsUsed(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Collection<ArrayDesign> handleGetArrayDesignsUsed( ExpressionExperiment expressionExperiment ) {
        Collection<ArrayDesign> result = new HashSet<ArrayDesign>();
        this.getExpressionExperimentDao().thawBioAssays( expressionExperiment );
        for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
            result.add( ba.getArrayDesignUsed() );
        }
        return result;
    }

    @Override
    protected long handleGetDesignElementDataVectorCountById( long id ) throws Exception {
        return this.getExpressionExperimentDao().getDesignElementDataVectorCountById( id );
    }

    @Override
    protected ExpressionExperiment handleFindByShortName( String shortName ) throws Exception {
        return this.getExpressionExperimentDao().findByShortName( shortName );
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
     * (non-Javadoc) This only returns 1 taxon, the 1st taxon as decided by the join which ever that is. The good news
     * is as a buisness rule we only allow 1 taxon per EE.
     */

    @Override
    protected Taxon handleGetTaxon( Long id ) {
        return this.getExpressionExperimentDao().getTaxon( id );
    }

    @Override
    protected Collection handleGetQuantitationTypes( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().getQuantitationTypes( expressionExperiment );
    }

    @Override
    protected Collection handleGetSamplingOfVectors( QuantitationType quantitationType, Integer limit )
            throws Exception {
        return this.getExpressionExperimentDao().getSamplingOfVectors( quantitationType, limit );
    }

    @Override
    protected Collection handleGetDesignElementDataVectors( Collection designElements, QuantitationType quantitationType )
            throws Exception {
        return this.getExpressionExperimentDao().getDesignElementDataVectors( designElements, quantitationType );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetPerTaxonCount()
     */
    @Override
    protected Map handleGetPerTaxonCount() throws Exception {
        return this.getExpressionExperimentDao().getPerTaxonCount();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoadValueObjects(java.util.Collection)
     */
    @Override
    protected Collection handleLoadValueObjects( Collection ids ) throws Exception {
        return this.getExpressionExperimentDao().loadValueObjects( ids );
    }

    @Override
    protected Collection handleGetQuantitationTypes( ExpressionExperiment expressionExperiment, ArrayDesign arrayDesign )
            throws Exception {
        return this.getExpressionExperimentDao().getQuantitationTypes( expressionExperiment, arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetBioMaterialCount(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected long handleGetBioMaterialCount( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getExpressionExperimentDao().getBioMaterialCount( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetPreferredDesignElementDataVectorCount(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected long handleGetPreferredDesignElementDataVectorCount( ExpressionExperiment expressionExperiment )
            throws Exception {
        return this.getExpressionExperimentDao().getPreferredDesignElementDataVectorCount( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleLoadMultiple(java.util.Collection)
     */
    @Override
    protected Collection handleLoadMultiple( Collection ids ) throws Exception {
        return this.getExpressionExperimentDao().load( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetDesignElementDataVectors(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      java.util.Collection)
     */
    @Override
    protected Collection handleGetDesignElementDataVectors( ExpressionExperiment expressionExperiment,
            Collection quantitationTypes ) throws Exception {
        return this.getExpressionExperimentDao().getDesignElementDataVectors( expressionExperiment, quantitationTypes );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetDesignElementDataVectors(Map,
     *      QuantitationType)
     */
    @Override
    protected Map handleGetDesignElementDataVectors( Map cs2gene, QuantitationType qt ) throws Exception {
        return this.getExpressionExperimentDao().getDesignElementDataVectors( cs2gene, qt );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastLinkAnalysis(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetLastLinkAnalysis( Collection ids ) throws Exception {

        return getLastEvent( ids, LinkAnalysisEvent.Factory.newInstance() );

    }

    /**
     * @param ids
     * @param type
     * @returns a map of the expression experiment ids to the last audit event for the given audit event type the map
     *          can contain nulls if the specified auditEventType isn't found for a given expression experiment id
     */
    @SuppressWarnings("unchecked")
    private Map<Long, AuditEvent> getLastEvent( Collection<Long> ids, AuditEventType type ) {

        Map<Long, AuditEvent> lastEventMap = new HashMap<Long, AuditEvent>();

        // This could be optimized by builing the map in the dao and having getLastAuditEvent take a collection of ids.
        Collection<ExpressionExperiment> ees = this.loadMultiple( ids );
        AuditEvent last;
        for ( ExpressionExperiment experiment : ees ) {

            last = getLastAuditEvent( experiment, type );
            lastEventMap.put( experiment.getId(), last );

        }

        return lastEventMap;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastMissingValueAnalysis(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetLastMissingValueAnalysis( Collection ids ) throws Exception {
        return getLastEvent( ids, MissingValueAnalysisEvent.Factory.newInstance() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastRankComputation(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetLastRankComputation( Collection ids ) throws Exception {
        return getLastEvent( ids, RankComputationEvent.Factory.newInstance() );
    }

    @Override
    @SuppressWarnings("unchecked")
    protected AuditEvent handleGetLastArrayDesignUpdate( ExpressionExperiment ee ) throws Exception {
        return this.getExpressionExperimentDao().getLastArrayDesignUpdate( ee );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByBibliographicReference(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    protected Collection handleFindByBibliographicReference( BibliographicReference bibRef ) throws Exception {
        return this.getExpressionExperimentDao().findByBibliographicReference( bibRef.getId() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleFindByBibliographicReference(ubic.gemma.model.common.description.BibliographicReference)
     */
    protected Collection handleGetPreferredQuantitationType( ExpressionExperiment EE ) throws Exception {
        Collection preferredQuantitationTypes = new HashSet();
        handleThawLite( EE );
        for ( QuantitationType qt : EE.getQuantitationTypes() ) {
            if ( qt.getIsPreferred() ) {
                preferredQuantitationTypes.add( qt );
            }
        }
        return preferredQuantitationTypes;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastTroubleEvent(java.util.Collection)
     */
    @Override
    protected Map handleGetLastTroubleEvent( Collection ids ) throws Exception {
        Map <Long, Collection<AuditEvent>> eeEvents = this.getExpressionExperimentDao().getAuditEvents( ids );
        Map <Long, Map<Long, Collection<AuditEvent>>> adEvents = this.getExpressionExperimentDao().getArrayDesignAuditEvents( ids );
        Map <Long, AuditEvent> troubleMap = new HashMap<Long, AuditEvent>();
        for ( Long eeId : eeEvents.keySet() ) {

            /* first check for trouble events on the expression experiment itself...
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
            
            /* if there was no trouble on the expression experiment, check the component
             * array designs...
             */
            Map <Long, Collection<AuditEvent>> myAdEvents = adEvents.get( eeId );
            if ( myAdEvents != null ) {
                for ( Long adId : myAdEvents.keySet() ) {

                    events = myAdEvents.get( adId );
                    if ( events == null )
                        continue;

                    AuditEvent adTroubleEvent = getLastOutstandingTroubleEvent( events );
                    if ( adTroubleEvent != null)
                        if ( troubleEvent == null || troubleEvent.getDate().before( adTroubleEvent.getDate() ) )
                            troubleEvent = adTroubleEvent;

                }
            
                if ( troubleEvent != null )
                    troubleMap.put( eeId, troubleEvent );
            }
        }
        return troubleMap;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentServiceBase#handleGetLastValidationEvent(java.util.Collection)
     */
    @Override
    protected Map handleGetLastValidationEvent( Collection ids ) throws Exception {
        return getLastEvent( ids, ValidatedFlagEvent.Factory.newInstance() );
    }
    
//    /**
//     * @see ubic.gemma.model.common.AuditableService#getLastAuditEvent(ubic.gemma.model.common.Auditable, ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType)
//     */
//    @Override
//    protected ubic.gemma.model.common.auditAndSecurity.AuditEvent handleGetLastAuditEvent( final Auditable auditable, AuditEventType type ) throws java.lang.Exception {
//        return this.getExpressionExperimentDao().getLastAuditEvent( auditable, type );
//    }
    
}