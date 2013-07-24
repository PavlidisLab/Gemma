/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

package ubic.gemma.web.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.expression.experiment.SessionBoundExpressionExperimentSetValueObject;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.genome.gene.SessionBoundGeneSetValueObject;
import ubic.gemma.genome.gene.service.GeneSetService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * TODO Document Me
 * 
 * @author thea
 * @version $Id$
 */
@Component
public class SessionListManagerImpl implements SessionListManager {

    @Autowired
    private GeneSetListContainer geneSetList;

    @Autowired
    private ExperimentSetListContainer experimentSetList;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

    @Autowired
    private GeneSetService geneSetService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#addExperimentSet(ubic.gemma.expression.experiment.
     * SessionBoundExpressionExperimentSetValueObject)
     */
    @Override
    public SessionBoundExpressionExperimentSetValueObject addExperimentSet(
            SessionBoundExpressionExperimentSetValueObject eesvo ) {

        return ( SessionBoundExpressionExperimentSetValueObject ) experimentSetList.addSet( eesvo, true );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#addExperimentSet(ubic.gemma.expression.experiment.
     * SessionBoundExpressionExperimentSetValueObject, boolean)
     */
    @Override
    public SessionBoundExpressionExperimentSetValueObject addExperimentSet(
            SessionBoundExpressionExperimentSetValueObject eesvo, boolean modified ) {

        return ( SessionBoundExpressionExperimentSetValueObject ) experimentSetList.addSet( eesvo, modified );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.persistence.SessionListManager#addGeneSet(ubic.gemma.genome.gene.SessionBoundGeneSetValueObject)
     */
    @Override
    public SessionBoundGeneSetValueObject addGeneSet( SessionBoundGeneSetValueObject gsvo ) {

        return addGeneSet( gsvo, true );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.persistence.SessionListManager#addGeneSet(ubic.gemma.genome.gene.SessionBoundGeneSetValueObject,
     * boolean)
     */
    @Override
    public SessionBoundGeneSetValueObject addGeneSet( SessionBoundGeneSetValueObject gsvo, boolean modified ) {

        return ( SessionBoundGeneSetValueObject ) geneSetList.addSet( gsvo, modified );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#getAllExperimentSets()
     */
    @Override
    public Collection<SessionBoundExpressionExperimentSetValueObject> getAllExperimentSets() {

        List<SessionBoundExpressionExperimentSetValueObject> castedCollection = ( List<SessionBoundExpressionExperimentSetValueObject> ) experimentSetList
                .getAllSessionBoundGroups();

        return castedCollection;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#getAllGeneSets()
     */
    @Override
    public Collection<SessionBoundGeneSetValueObject> getAllGeneSets() {
        return getAllGeneSets( null );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#getAllGeneSets(java.lang.Long)
     */
    @Override
    public Collection<SessionBoundGeneSetValueObject> getAllGeneSets( Long taxonId ) {

        // We know that geneSetList will only contain SessionBoundGeneSetValueObjects (via
        // SessionListManager.addGeneSet(SessionBoundGeneSetValueObject) so this cast is okay
        List<SessionBoundGeneSetValueObject> castedCollection = ( List<SessionBoundGeneSetValueObject> ) geneSetList
                .getAllSessionBoundGroups();

        // filter collection if taxonId is specified
        if ( taxonId != null ) {
            castedCollection = filterGeneSetsByTaxon( taxonId, castedCollection );
        }

        return castedCollection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#getExperimentIdsInSet(java.lang.Long)
     */
    @Override
    public Collection<Long> getExperimentIdsInSet( Long id ) {
        ExpressionExperimentSet eeSet = expressionExperimentSetService.load( id ); // secure
        Collection<BioAssaySet> datasets = eeSet.getExperiments(); // Not secure.
        Collection<Long> eeids = new HashSet<Long>();
        for ( BioAssaySet ee : datasets ) {
            eeids.add( ee.getId() );
        }
        return eeids;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#getExperimentSetById(java.lang.Long)
     */
    @Override
    public SessionBoundExpressionExperimentSetValueObject getExperimentSetById( Long id ) {

        Collection<SessionBoundExpressionExperimentSetValueObject> sessionExperimentSets = getAllExperimentSets();

        for ( SessionBoundExpressionExperimentSetValueObject esvo : sessionExperimentSets ) {
            if ( id.equals( esvo.getId() ) ) {
                return esvo;
            }
        }
        return null;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#getExperimentsInSet(java.lang.Long)
     */
    @Override
    public Collection<ExpressionExperimentValueObject> getExperimentsInSet( Long id ) {
        Collection<Long> eeids = getExperimentIdsInSet( id );
        Collection<ExpressionExperimentValueObject> result = expressionExperimentService
                .loadValueObjects( eeids, false );
        expressionExperimentReportService.getReportInformation( result );
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#getGeneSetById(java.lang.Long)
     */
    @Override
    public SessionBoundGeneSetValueObject getGeneSetById( Long id ) {

        Collection<SessionBoundGeneSetValueObject> sessionGeneSets = getAllGeneSets();

        for ( SessionBoundGeneSetValueObject gsvo : sessionGeneSets ) {
            if ( id.equals( gsvo.getId() ) ) {
                return gsvo;
            }
        }
        return null;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#getGenesInGroup(java.lang.Long)
     */
    @Override
    public Collection<GeneValueObject> getGenesInGroup( Long groupId ) {

        Collection<GeneValueObject> results = null;

        GeneSet gs = geneSetService.load( groupId );
        if ( gs == null ) return null; // FIXME: Send and error code/feedback?

        results = GeneValueObject.convertMembers2GeneValueObjects( gs.getMembers() );

        return results;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#getModifiedExperimentSets()
     */
    @Override
    public Collection<SessionBoundExpressionExperimentSetValueObject> getModifiedExperimentSets() {

        List<SessionBoundExpressionExperimentSetValueObject> castedCollection = ( List<SessionBoundExpressionExperimentSetValueObject> ) experimentSetList
                .getSessionBoundModifiedGroups();

        return castedCollection;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#getModifiedExperimentSets(java.lang.Long)
     */
    @Override
    public Collection<SessionBoundExpressionExperimentSetValueObject> getModifiedExperimentSets( Long taxonId ) {

        List<SessionBoundExpressionExperimentSetValueObject> castedCollection = ( List<SessionBoundExpressionExperimentSetValueObject> ) experimentSetList
                .getSessionBoundModifiedGroups();

        // filter collection if taxonId is specified
        if ( taxonId != null ) {
            castedCollection = this.filterExperimentSetsByTaxon( taxonId, castedCollection );
        }

        return castedCollection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#getModifiedGeneSets()
     */
    @Override
    public Collection<SessionBoundGeneSetValueObject> getModifiedGeneSets() {

        // We know that geneSetList will only contain SessionBoundGeneSetValueObjects (via
        // SessionListManager.addGeneSet(SessionBoundGeneSetValueObject) so this cast is okay
        List<SessionBoundGeneSetValueObject> castedCollection = ( List<SessionBoundGeneSetValueObject> ) geneSetList
                .getSessionBoundModifiedGroups();

        return castedCollection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#getModifiedGeneSets(java.lang.Long)
     */
    @Override
    public Collection<SessionBoundGeneSetValueObject> getModifiedGeneSets( Long taxonId ) {

        // We know that geneSetList will only contain SessionBoundGeneSetValueObjects (via
        // SessionListManager.addGeneSet(SessionBoundGeneSetValueObject) so this cast is okay
        List<SessionBoundGeneSetValueObject> castedCollection = ( List<SessionBoundGeneSetValueObject> ) geneSetList
                .getSessionBoundModifiedGroups();

        // filter collection if taxonId is specified
        if ( taxonId != null ) {
            castedCollection = this.filterGeneSetsByTaxon( taxonId, castedCollection );
        }

        return castedCollection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#incrementAndGetLargestExperimentSetSessionId()
     */
    @Override
    public Long incrementAndGetLargestExperimentSetSessionId() {
        return experimentSetList.incrementAndGetLargestSessionId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#incrementAndGetLargestGeneSetSessionId()
     */
    @Override
    public Long incrementAndGetLargestGeneSetSessionId() {
        return geneSetList.incrementAndGetLargestSessionId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#removeExperimentSet(ubic.gemma.expression.experiment.
     * SessionBoundExpressionExperimentSetValueObject)
     */
    @Override
    public void removeExperimentSet( SessionBoundExpressionExperimentSetValueObject eesvo ) {

        experimentSetList.removeSet( eesvo );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.persistence.SessionListManager#removeGeneSet(ubic.gemma.genome.gene.SessionBoundGeneSetValueObject
     * )
     */
    @Override
    public void removeGeneSet( SessionBoundGeneSetValueObject gsvo ) {

        geneSetList.removeSet( gsvo );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.persistence.SessionListManager#updateExperimentSet(ubic.gemma.expression.experiment.
     * SessionBoundExpressionExperimentSetValueObject)
     */
    @Override
    public void updateExperimentSet( SessionBoundExpressionExperimentSetValueObject eesvo ) {

        experimentSetList.updateSet( eesvo );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.persistence.SessionListManager#updateGeneSet(ubic.gemma.genome.gene.SessionBoundGeneSetValueObject
     * )
     */
    @Override
    public void updateGeneSet( SessionBoundGeneSetValueObject gsvo ) {

        geneSetList.updateSet( gsvo );

    }

    /**
     * Return only those sets that have taxonId equal to the taxonId param
     * 
     * @param taxonId
     * @param castedCollection
     * @return
     */
    private List<SessionBoundExpressionExperimentSetValueObject> filterExperimentSetsByTaxon( Long taxonId,
            List<SessionBoundExpressionExperimentSetValueObject> castedCollection ) {
        List<SessionBoundExpressionExperimentSetValueObject> taxonFilteredCollection = new ArrayList<SessionBoundExpressionExperimentSetValueObject>();
        for ( SessionBoundExpressionExperimentSetValueObject eesvo : castedCollection ) {
            if ( eesvo.getTaxonId().equals( taxonId ) ) {
                taxonFilteredCollection.add( eesvo );
            }
        }

        return taxonFilteredCollection;
    }

    /**
     * Return only those sets that have taxonId equal to the taxonId param
     * 
     * @param taxonId
     * @param castedCollection
     * @return
     */
    private List<SessionBoundGeneSetValueObject> filterGeneSetsByTaxon( Long taxonId,
            List<SessionBoundGeneSetValueObject> castedCollection ) {
        List<SessionBoundGeneSetValueObject> taxonFilteredCollection = new ArrayList<SessionBoundGeneSetValueObject>();
        for ( SessionBoundGeneSetValueObject gsvo : castedCollection ) {
            if ( gsvo.getTaxonId().equals( taxonId ) ) {
                taxonFilteredCollection.add( gsvo );
            }
        }

        return taxonFilteredCollection;
    }

}
