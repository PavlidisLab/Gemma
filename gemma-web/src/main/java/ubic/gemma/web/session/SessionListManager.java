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
package ubic.gemma.web.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.model.Reference;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetService;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentSetValueObject;

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

    public Collection<GeneSetValueObject> getAllGeneSets() {
        return getAllGeneSets( null );
    }

    public Collection<GeneSetValueObject> getAllGeneSets( Long taxonId ) {

        // We know that geneSetList will only contain GeneSetValueObjects (via
        // SessionListManager.addGeneSet(GeneSetValueObject) so this cast is okay
        @SuppressWarnings("unchecked")
        List<GeneSetValueObject> castedCollection = ( List ) geneSetList.getAllSessionBoundGroups();

        // filter collection if taxonId is specified
        if ( taxonId != null ) {
            List<GeneSetValueObject> taxonFilteredCollection = new ArrayList<GeneSetValueObject>();
            for ( GeneSetValueObject gsvo : castedCollection ) {
                if ( gsvo.getTaxonId() == taxonId ) {
                    taxonFilteredCollection.add( gsvo );
                }
            }

            castedCollection = taxonFilteredCollection;

        }

        return castedCollection;
    }

    public Collection<GeneSetValueObject> getModifiedGeneSets() {
        return getAllGeneSets( null );
    }
    
    public Collection<GeneSetValueObject> getModifiedGeneSets( Long taxonId ) {

        // We know that geneSetList will only contain GeneSetValueObjects (via
        // SessionListManager.addGeneSet(GeneSetValueObject) so this cast is okay
        @SuppressWarnings("unchecked")
        List<GeneSetValueObject> castedCollection = ( List ) geneSetList.getSessionBoundModifiedGroups();

        // filter collection if taxonId is specified
        if ( taxonId != null ) {
            List<GeneSetValueObject> taxonFilteredCollection = new ArrayList<GeneSetValueObject>();
            for ( GeneSetValueObject gsvo : castedCollection ) {
                if ( gsvo.getTaxonId() == taxonId ) {
                    taxonFilteredCollection.add( gsvo );
                }
            }

            castedCollection = taxonFilteredCollection;

        }

        return castedCollection;
    }

    
    /**
     * Get the members of session-bound group using the group's reference object
     * if a reference is passed in for a group that isn't session-bound, null is returned
     * members are returned as expression experiment value objects
     * @param reference
     * @return
     */
    public Collection<ExpressionExperimentValueObject> getExperimentsInSetByReference( Reference reference ) {

        Collection<ExpressionExperimentValueObject> results = null;

        // if group is db backed
        if ( reference.isDatabaseBacked() ) {

            return results;

        }
        // if group is session bound
        else if (reference.isSessionBound()){

            Collection<ExpressionExperimentSetValueObject> sessionExperimentSets = getAllExperimentSets();
            
            Collection<ExpressionExperiment> expressionExperiments = null;

            for ( ExpressionExperimentSetValueObject eesvo : sessionExperimentSets ) {
                if ( reference.equals( eesvo.getReference() ) ) {
                    expressionExperiments = expressionExperimentService.loadMultiple( eesvo
                            .getMemberIds() );
                    break;
                }
            }

            if ( expressionExperiments != null ) {

                results = new HashSet<ExpressionExperimentValueObject>();

                for ( ExpressionExperiment ee : expressionExperiments ) {

                    results.add( new ExpressionExperimentValueObject( ee ) );

                }

            }

        }

        return results;
    }

    /**
     * Get the members of session-bound group using the group's reference object
     * if a reference is passed in for a group that isn't session-bound, null is returned
     * members are returned as expression experiment value objects
     * @param reference
     * @return
     */
    public Collection<GeneValueObject> getGenesInSetByReference( Reference reference ) {

        Collection<GeneValueObject> results = null;

        if(reference.isDatabaseBacked()){
            return null;
        }
        else if(reference.isSessionBound()) {

            Collection<GeneSetValueObject> sessionGeneSets = getAllGeneSets();

            Collection<Gene> genes = null;

            for ( GeneSetValueObject gsvo : sessionGeneSets ) {
                if ( reference.equals( gsvo.getReference() ) ) {
                    genes = geneService.loadMultiple( gsvo.getMemberIds() );
                    break;
                }
            }

            if ( genes != null ) {

                results = new HashSet<GeneValueObject>();

                for ( Gene g : genes ) {

                    results.add( new GeneValueObject( g ) );

                }

            }

        }

        return results;

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
    
    
    public GeneSetValueObject addGeneSet( GeneSetValueObject gsvo ) {

        return ( GeneSetValueObject ) geneSetList.addSet( gsvo );

    }
    
    public GeneSetValueObject addGeneSet( GeneSetValueObject gsvo, String referenceType ) {

        return ( GeneSetValueObject ) geneSetList.addSet( gsvo , referenceType );

    }

    public void removeGeneSet( GeneSetValueObject gsvo ) {

        geneSetList.removeSet( gsvo );

    }

    public void updateGeneSet( GeneSetValueObject gsvo ) {

        geneSetList.updateSet( gsvo );

    }

    
    public Long incrementAndGetLargestGeneSetSessionId() {
        return geneSetList.incrementAndGetLargestSessionId();
    }

    public Collection<ExpressionExperimentSetValueObject> getAllExperimentSets() {

        @SuppressWarnings("unchecked")
        List<ExpressionExperimentSetValueObject> castedCollection = ( List ) experimentSetList.getAllSessionBoundGroups();

        return castedCollection;

    }

    public Collection<ExpressionExperimentSetValueObject> getModifiedExperimentSets() {

        @SuppressWarnings("unchecked")
        List<ExpressionExperimentSetValueObject> castedCollection = ( List ) experimentSetList.getSessionBoundModifiedGroups();

        return castedCollection;

    }

    public ExpressionExperimentSetValueObject addExperimentSet( ExpressionExperimentSetValueObject eesvo ) {

        return ( ExpressionExperimentSetValueObject ) experimentSetList.addSet( eesvo );

    }

    public ExpressionExperimentSetValueObject addExperimentSet( ExpressionExperimentSetValueObject eesvo , String referenceType) {

        return ( ExpressionExperimentSetValueObject ) experimentSetList.addSet( eesvo, referenceType );

    }

    public void removeExperimentSet( ExpressionExperimentSetValueObject eesvo ) {

        experimentSetList.removeSet( eesvo );

    }

    public void updateExperimentSet( ExpressionExperimentSetValueObject eesvo ) {

        experimentSetList.updateSet( eesvo );

    }

    public Long incrementAndGetLargestExperimentSetSessionId() {
        return experimentSetList.incrementAndGetLargestSessionId();
    }

}
