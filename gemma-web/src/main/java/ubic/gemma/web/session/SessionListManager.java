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
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.persistence.GemmaSessionBackedValueObject;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentSetValueObject;

@Service
public class SessionListManager {

    @Autowired
    GeneSetListContainer geneSetList;

    @Autowired
    ExperimentSetListContainer experimentSetList;

    public Collection<GeneSetValueObject> getRecentGeneSets() {
        return getRecentGeneSets( null );
    }

    public Collection<GeneSetValueObject> getRecentGeneSets( Long taxonId ) {

        // We know that geneSetList will only contain GeneSetValueObjects (via
        // SessionListManager.addGeneSet(GeneSetValueObject) so this cast is okay
        @SuppressWarnings("unchecked")
        List<GeneSetValueObject> castedCollection = ( List ) geneSetList.getRecentSets();

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

    public GeneSetValueObject addGeneSet( GeneSetValueObject gsvo ) {

        return ( GeneSetValueObject ) geneSetList.addSet( gsvo );

    }

    public void removeGeneSet( GeneSetValueObject gsvo ) {

        geneSetList.removeSet( gsvo );

    }

    public void updateGeneSet( GeneSetValueObject gsvo ) {

        geneSetList.updateSet( gsvo );

    }

    // this gives result(from the DB) unique Session Ids(used by the front end store) if it doesn't already have one
    public void setUniqueGeneSetStoreIds( Collection<GeneSetValueObject> result ) {

        // this cast is safe because we know that we are getting a Collection of GeneSetValueObjects(which implements
        // GemmaSessionBackedValueObject
        @SuppressWarnings("unchecked")
        Collection<GemmaSessionBackedValueObject> castedCollection = ( Collection ) result;

        geneSetList.setUniqueSetStoreIds( castedCollection );

    }

    public boolean isDbBackedGeneSetSessionId( Long sessionId ) {

        return geneSetList.isDbBackedSessionId( sessionId );

    }

    public Long getDbGeneSetIdBySessionId( Long sessionId ) {
        return geneSetList.getDbIdFromSessionId( sessionId );
    }

    public Long incrementAndGetLargestGeneSetSessionId() {
        return geneSetList.incrementAndGetLargestSessionId();
    }

    public Collection<ExpressionExperimentSetValueObject> getRecentExperimentSets() {

        @SuppressWarnings("unchecked")
        List<ExpressionExperimentSetValueObject> castedCollection = ( List ) experimentSetList.getRecentSets();

        return castedCollection;

    }

    public ExpressionExperimentSetValueObject addExperimentSet( ExpressionExperimentSetValueObject eesvo ) {

        return ( ExpressionExperimentSetValueObject ) experimentSetList.addSet( eesvo );

    }

    public void removeExperimentSet( ExpressionExperimentSetValueObject eesvo ) {

        experimentSetList.removeSet( eesvo );

    }

    public void updateExperimentSet( ExpressionExperimentSetValueObject eesvo ) {

        experimentSetList.updateSet( eesvo );

    }

    public void setUniqueExperimentSetStoreIds( Collection<ExpressionExperimentSetValueObject> result ) {

        // this cast is safe because we know that we are getting a Collection of
        // ExpressionExperimentSetValueObjects(which implements GemmaSessionBackedValueObject
        @SuppressWarnings("unchecked")
        Collection<GemmaSessionBackedValueObject> castedCollection = ( Collection ) result;

        experimentSetList.setUniqueSetStoreIds( castedCollection );

    }

    public Long incrementAndGetLargestExperimentSetSessionId() {
        return experimentSetList.incrementAndGetLargestSessionId();
    }

    public boolean isDbBackedExperimentSetSessionId( Long sessionId ) {

        return experimentSetList.isDbBackedSessionId( sessionId );

    }

    public Long getDbExperimentSetIdBySessionId( Long sessionId ) {
        return experimentSetList.getDbIdFromSessionId( sessionId );
    }

}
