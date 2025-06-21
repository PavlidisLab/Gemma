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

package ubic.gemma.web.controller.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.model.genome.gene.SessionBoundGeneSetValueObject;
import ubic.gemma.persistence.service.genome.gene.GeneSetService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.SessionBoundExpressionExperimentSetValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author thea
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

    @Override
    public SessionBoundExpressionExperimentSetValueObject addExperimentSet(
            SessionBoundExpressionExperimentSetValueObject eesvo ) {

        return ( SessionBoundExpressionExperimentSetValueObject ) experimentSetList.addSet( eesvo, true );

    }

    @Override
    public SessionBoundExpressionExperimentSetValueObject addExperimentSet(
            SessionBoundExpressionExperimentSetValueObject eesvo, boolean modified ) {

        return ( SessionBoundExpressionExperimentSetValueObject ) experimentSetList.addSet( eesvo, modified );

    }

    @Override
    public SessionBoundGeneSetValueObject addGeneSet( SessionBoundGeneSetValueObject gsvo ) {

        return this.addGeneSet( gsvo, true );

    }

    @Override
    public SessionBoundGeneSetValueObject addGeneSet( SessionBoundGeneSetValueObject gsvo, boolean modified ) {

        return ( SessionBoundGeneSetValueObject ) geneSetList.addSet( gsvo, modified );

    }

    @Override
    public Collection<SessionBoundExpressionExperimentSetValueObject> getAllExperimentSets() {

        @SuppressWarnings("unchecked") List<SessionBoundExpressionExperimentSetValueObject> castedCollection = ( List<SessionBoundExpressionExperimentSetValueObject> ) experimentSetList
                .getAllSessionBoundGroups();

        return castedCollection;

    }

    @Override
    public Collection<SessionBoundGeneSetValueObject> getAllGeneSets() {
        return this.getAllGeneSets( null );
    }

    @Override
    public Collection<SessionBoundGeneSetValueObject> getAllGeneSets( Long taxonId ) {

        // We know that geneSetList will only contain SessionBoundGeneSetValueObjects (via
        // SessionListManager.addGeneSet(SessionBoundGeneSetValueObject) so this cast is okay
        @SuppressWarnings("unchecked") List<SessionBoundGeneSetValueObject> castedCollection = ( List<SessionBoundGeneSetValueObject> ) geneSetList
                .getAllSessionBoundGroups();

        // filter collection if taxonId is specified
        if ( taxonId != null ) {
            castedCollection = this.filterGeneSetsByTaxon( taxonId, castedCollection );
        }

        return castedCollection;
    }

    @Override
    public Collection<Long> getExperimentIdsInSet( Long id ) {
        ExpressionExperimentSet eeSet = expressionExperimentSetService.loadOrFail( id ); // secure
        Collection<BioAssaySet> datasets = eeSet.getExperiments(); // Not secure.
        Collection<Long> eeids = new HashSet<>();
        for ( BioAssaySet ee : datasets ) {
            eeids.add( ee.getId() );
        }
        return eeids;
    }

    @Override
    public SessionBoundExpressionExperimentSetValueObject getExperimentSetById( Long id ) {

        Collection<SessionBoundExpressionExperimentSetValueObject> sessionExperimentSets = this.getAllExperimentSets();

        for ( SessionBoundExpressionExperimentSetValueObject esvo : sessionExperimentSets ) {
            if ( id.equals( esvo.getId() ) ) {
                return esvo;
            }
        }
        return null;

    }

    @Override
    public Collection<ExpressionExperimentDetailsValueObject> getExperimentsInSet( Long id ) {
        Collection<Long> eeids = this.getExperimentIdsInSet( id );
        Collection<ExpressionExperimentDetailsValueObject> result = expressionExperimentService
                .loadDetailsValueObjectsByIdsWithCache( eeids );
        expressionExperimentReportService.populateReportInformation( result );
        return result;
    }

    @Override
    public SessionBoundGeneSetValueObject getGeneSetById( Long id ) {

        Collection<SessionBoundGeneSetValueObject> sessionGeneSets = this.getAllGeneSets();

        for ( SessionBoundGeneSetValueObject gsvo : sessionGeneSets ) {
            if ( id.equals( gsvo.getId() ) ) {
                return gsvo;
            }
        }
        return null;

    }

    @Override
    public Collection<GeneValueObject> getGenesInGroup( Long groupId ) {

        Collection<GeneValueObject> results;

        GeneSet gs = geneSetService.load( groupId );
        if ( gs == null )
            return null; // TODO: Send and error code/feedback?

        results = GeneValueObject.convertMembers2GeneValueObjects( gs.getMembers() );

        return results;

    }

    @Override
    public Collection<SessionBoundExpressionExperimentSetValueObject> getModifiedExperimentSets() {

        @SuppressWarnings("unchecked") List<SessionBoundExpressionExperimentSetValueObject> castedCollection = ( List<SessionBoundExpressionExperimentSetValueObject> ) experimentSetList
                .getSessionBoundModifiedGroups();

        return castedCollection;

    }

    @Override
    public Collection<SessionBoundExpressionExperimentSetValueObject> getModifiedExperimentSets( Long taxonId ) {

        @SuppressWarnings("unchecked") List<SessionBoundExpressionExperimentSetValueObject> castedCollection = ( List<SessionBoundExpressionExperimentSetValueObject> ) experimentSetList
                .getSessionBoundModifiedGroups();

        // filter collection if taxonId is specified
        if ( taxonId != null ) {
            castedCollection = this.filterExperimentSetsByTaxon( taxonId, castedCollection );
        }

        return castedCollection;
    }

    @Override
    public Collection<SessionBoundGeneSetValueObject> getModifiedGeneSets() {

        // We know that geneSetList will only contain SessionBoundGeneSetValueObjects (via
        // SessionListManager.addGeneSet(SessionBoundGeneSetValueObject) so this cast is okay
        @SuppressWarnings("unchecked") List<SessionBoundGeneSetValueObject> castedCollection = ( List<SessionBoundGeneSetValueObject> ) geneSetList
                .getSessionBoundModifiedGroups();

        return castedCollection;
    }

    @Override
    public Collection<SessionBoundGeneSetValueObject> getModifiedGeneSets( Long taxonId ) {

        // We know that geneSetList will only contain SessionBoundGeneSetValueObjects (via
        // SessionListManager.addGeneSet(SessionBoundGeneSetValueObject) so this cast is okay
        @SuppressWarnings("unchecked") List<SessionBoundGeneSetValueObject> castedCollection = ( List<SessionBoundGeneSetValueObject> ) geneSetList
                .getSessionBoundModifiedGroups();

        // filter collection if taxonId is specified
        if ( taxonId != null ) {
            castedCollection = this.filterGeneSetsByTaxon( taxonId, castedCollection );
        }

        return castedCollection;
    }

    @Override
    public Long incrementAndGetLargestExperimentSetSessionId() {
        return experimentSetList.incrementAndGetLargestSessionId();
    }

    @Override
    public Long incrementAndGetLargestGeneSetSessionId() {
        return geneSetList.incrementAndGetLargestSessionId();
    }

    @Override
    public void removeExperimentSet( SessionBoundExpressionExperimentSetValueObject eesvo ) {

        experimentSetList.removeSet( eesvo );

    }

    @Override
    public void removeGeneSet( SessionBoundGeneSetValueObject gsvo ) {

        geneSetList.removeSet( gsvo );

    }

    @Override
    public void updateExperimentSet( SessionBoundExpressionExperimentSetValueObject eesvo ) {

        experimentSetList.updateSet( eesvo );

    }

    @Override
    public void updateGeneSet( SessionBoundGeneSetValueObject gsvo ) {

        geneSetList.updateSet( gsvo );

    }

    /**
     * Return only those sets that have taxonId equal to the taxonId param
     */
    private List<SessionBoundExpressionExperimentSetValueObject> filterExperimentSetsByTaxon( Long taxonId,
            List<SessionBoundExpressionExperimentSetValueObject> castedCollection ) {
        List<SessionBoundExpressionExperimentSetValueObject> taxonFilteredCollection = new ArrayList<>();
        for ( SessionBoundExpressionExperimentSetValueObject eesvo : castedCollection ) {
            if ( eesvo.getTaxonId().equals( taxonId ) ) {
                taxonFilteredCollection.add( eesvo );
            }
        }

        return taxonFilteredCollection;
    }

    /**
     * Return only those sets that have taxonId equal to the taxonId param
     */
    private List<SessionBoundGeneSetValueObject> filterGeneSetsByTaxon( Long taxonId,
            List<SessionBoundGeneSetValueObject> castedCollection ) {
        List<SessionBoundGeneSetValueObject> taxonFilteredCollection = new ArrayList<>();
        for ( SessionBoundGeneSetValueObject gsvo : castedCollection ) {
            if ( gsvo.getTaxonId().equals( taxonId ) ) {
                taxonFilteredCollection.add( gsvo );
            }
        }

        return taxonFilteredCollection;
    }

}
