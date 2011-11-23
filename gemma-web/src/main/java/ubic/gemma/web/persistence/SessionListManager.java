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
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetService;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.expression.experiment.SessionBoundExpressionExperimentSetValueObject;
import ubic.gemma.genome.gene.SessionBoundGeneSetValueObject;

@Service
public class SessionListManager {

    @Autowired
    GeneSetListContainer geneSetList;

    @Autowired
    ExperimentSetListContainer experimentSetList;

    @Autowired
    ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    @Autowired
    ExpressionExperimentReportService expressionExperimentReportService;

    @Autowired
    GeneService geneService;

    @Autowired
    GeneSetService geneSetService;

    public Collection<SessionBoundGeneSetValueObject> getAllGeneSets() {
        return getAllGeneSets( null );
    }

    public Collection<SessionBoundGeneSetValueObject> getAllGeneSets( Long taxonId ) {

        // We know that geneSetList will only contain SessionBoundGeneSetValueObjects (via
        // SessionListManager.addGeneSet(SessionBoundGeneSetValueObject) so this cast is okay
        @SuppressWarnings("unchecked")
        List<SessionBoundGeneSetValueObject> castedCollection = ( List ) geneSetList.getAllSessionBoundGroups();

        // filter collection if taxonId is specified
        if ( taxonId != null ) {
            List<SessionBoundGeneSetValueObject> taxonFilteredCollection = new ArrayList<SessionBoundGeneSetValueObject>();
            for ( SessionBoundGeneSetValueObject gsvo : castedCollection ) {
                if ( gsvo.getTaxonId() == taxonId ) {
                    taxonFilteredCollection.add( gsvo );
                }
            }

            castedCollection = taxonFilteredCollection;

        }

        return castedCollection;
    }

    public Collection<SessionBoundGeneSetValueObject> getModifiedGeneSets() {

        // We know that geneSetList will only contain SessionBoundGeneSetValueObjects (via
        // SessionListManager.addGeneSet(SessionBoundGeneSetValueObject) so this cast is okay
        @SuppressWarnings("unchecked")
        List<SessionBoundGeneSetValueObject> castedCollection = ( List ) geneSetList.getSessionBoundModifiedGroups();

        return castedCollection;
    }

    public Collection<SessionBoundGeneSetValueObject> getModifiedGeneSets( Long taxonId ) {

        // We know that geneSetList will only contain SessionBoundGeneSetValueObjects (via
        // SessionListManager.addGeneSet(SessionBoundGeneSetValueObject) so this cast is okay
        @SuppressWarnings("unchecked")
        List<SessionBoundGeneSetValueObject> castedCollection = ( List ) geneSetList.getSessionBoundModifiedGroups();

        // filter collection if taxonId is specified
        if ( taxonId != null ) {
            List<SessionBoundGeneSetValueObject> taxonFilteredCollection = new ArrayList<SessionBoundGeneSetValueObject>();
            for ( SessionBoundGeneSetValueObject gsvo : castedCollection ) {
                if ( gsvo.getTaxonId() == taxonId ) {
                    taxonFilteredCollection.add( gsvo );
                }
            }

            castedCollection = taxonFilteredCollection;

        }

        return castedCollection;
    }

    /**
     * Get the session-bound group using the group's id 
     * 
     * @param reference
     * @return
     */
    public SessionBoundExpressionExperimentSetValueObject getExperimentSetById( Long id ) {

        Collection<SessionBoundExpressionExperimentSetValueObject> sessionExperimentSets = getAllExperimentSets();

        for ( SessionBoundExpressionExperimentSetValueObject esvo : sessionExperimentSets ) {
            if ( id.equals( esvo.getId() ) ) {
                return esvo;
            }
        }
        return null;

    }


    /**
     * Get the session-bound group using the group's id
     * 
     * @param reference
     * @return
     */
    public SessionBoundGeneSetValueObject getGeneSetById( Long id ) {

        Collection<SessionBoundGeneSetValueObject> sessionGeneSets = getAllGeneSets();

        for ( SessionBoundGeneSetValueObject gsvo : sessionGeneSets ) {
            if ( id.equals( gsvo.getId() ) ) {
                return gsvo;
            }
        }
        return null;

    }

    /**
     * AJAX If the current user has access to given gene group will return the gene ids in the gene group
     * 
     * @param groupId
     * @return
     */
    public Collection<GeneValueObject> getGenesInGroup( Long groupId ) {

        Collection<GeneValueObject> results = null;

        GeneSet gs = geneSetService.load( groupId );
        if ( gs == null ) return null; // FIXME: Send and error code/feedback?

        results = GeneValueObject.convertMembers2GeneValueObjects( gs.getMembers() );

        return results;

    }

    /**
     * @param id
     * @return
     */
    public Collection<ExpressionExperimentValueObject> getExperimentsInSet( Long id ) {
        Collection<Long> eeids = getExperimentIdsInSet( id );
        Collection<ExpressionExperimentValueObject> result = expressionExperimentService.loadValueObjects( eeids );
        expressionExperimentReportService.fillReportInformation( result );
        return result;
    }

    /**
     * @param id
     * @return
     */
    public Collection<Long> getExperimentIdsInSet( Long id ) {
        ExpressionExperimentSet eeSet = expressionExperimentSetService.load( id ); // secure
        Collection<BioAssaySet> datasets = eeSet.getExperiments(); // Not secure.
        Collection<Long> eeids = new HashSet<Long>();
        for ( BioAssaySet ee : datasets ) {
            eeids.add( ee.getId() );
        }
        return eeids;
    }

    public SessionBoundGeneSetValueObject addGeneSet( SessionBoundGeneSetValueObject gsvo ) {

        return addGeneSet( gsvo, true );

    }

    public SessionBoundGeneSetValueObject addGeneSet( SessionBoundGeneSetValueObject gsvo, boolean modified ) {

        return ( SessionBoundGeneSetValueObject ) geneSetList.addSet( gsvo, modified );

    }

    public void removeGeneSet( SessionBoundGeneSetValueObject gsvo ) {

        geneSetList.removeSet( gsvo );

    }

    public void updateGeneSet( SessionBoundGeneSetValueObject gsvo ) {

        geneSetList.updateSet( gsvo );

    }

    public Long incrementAndGetLargestGeneSetSessionId() {
        return geneSetList.incrementAndGetLargestSessionId();
    }

    public Collection<SessionBoundExpressionExperimentSetValueObject> getAllExperimentSets() {

        @SuppressWarnings("unchecked")
        List<SessionBoundExpressionExperimentSetValueObject> castedCollection = ( List ) experimentSetList
                .getAllSessionBoundGroups();

        return castedCollection;

    }

    public Collection<SessionBoundExpressionExperimentSetValueObject> getModifiedExperimentSets() {

        @SuppressWarnings("unchecked")
        List<SessionBoundExpressionExperimentSetValueObject> castedCollection = ( List ) experimentSetList
                .getSessionBoundModifiedGroups();

        return castedCollection;

    }
    

    public Collection<SessionBoundExpressionExperimentSetValueObject> getModifiedExperimentSets( Long taxonId ) {

        // We know that geneSetList will only contain SessionBoundGeneSetValueObjects (via
        // SessionListManager.addGeneSet(SessionBoundGeneSetValueObject) so this cast is okay
        @SuppressWarnings("unchecked")
        List<SessionBoundExpressionExperimentSetValueObject> castedCollection = ( List ) experimentSetList.getSessionBoundModifiedGroups();

        // filter collection if taxonId is specified
        if ( taxonId != null ) {
            List<SessionBoundExpressionExperimentSetValueObject> taxonFilteredCollection = new ArrayList<SessionBoundExpressionExperimentSetValueObject>();
            for ( SessionBoundExpressionExperimentSetValueObject gsvo : castedCollection ) {
                if ( gsvo.getTaxonId() == taxonId ) {
                    taxonFilteredCollection.add( gsvo );
                }
            }

            castedCollection = taxonFilteredCollection;

        }

        return castedCollection;
    }

    public SessionBoundExpressionExperimentSetValueObject addExperimentSet( SessionBoundExpressionExperimentSetValueObject eesvo ) {

        return ( SessionBoundExpressionExperimentSetValueObject ) experimentSetList.addSet( eesvo, true );

    }

    public SessionBoundExpressionExperimentSetValueObject addExperimentSet( SessionBoundExpressionExperimentSetValueObject eesvo,
            boolean modified ) {

        return ( SessionBoundExpressionExperimentSetValueObject ) experimentSetList.addSet( eesvo, modified );

    }

    public void removeExperimentSet( SessionBoundExpressionExperimentSetValueObject eesvo ) {

        experimentSetList.removeSet( eesvo );

    }

    public void updateExperimentSet( SessionBoundExpressionExperimentSetValueObject eesvo ) {

        experimentSetList.updateSet( eesvo );

    }

    public Long incrementAndGetLargestExperimentSetSessionId() {
        return experimentSetList.incrementAndGetLargestSessionId();
    }

}
