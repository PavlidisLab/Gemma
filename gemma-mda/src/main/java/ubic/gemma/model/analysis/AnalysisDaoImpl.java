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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.hibernate.ScrollableResults;

/**
 * @see ubic.gemma.model.analysis.Analysis
 */
public class AnalysisDaoImpl extends ubic.gemma.model.analysis.AnalysisDaoBase {

    @Override
    protected Map handleFindByInvestigations( Collection investigators ) throws Exception {

        //TODO: this query doesn't work. fix.  Causses an npe when hibernate tries to generated the sql
        final String queryString = "select distinct a,a.analyzedInvestigation from AnalysisImpl as a where a = any (select distinct analysis from AnalysisImpl where (:investigations) = any elements(analysis.analyzedInvestigation))";
        //"select distinct a,a.analyzedInvestigation from AnalysisImpl as a where a = any (select distinct analysis from AnalysisImpl where i.analyzedInvestigation.id in (:investigations))
        //"from AnalysisImpl i where :investigation = any elements(i.analyzedInvestigation)", investigation)      
        
        
        Collection<Long> investigatorsById = new HashSet<Long>();
        for ( Object obj : investigators )
            investigatorsById.add( ( ( Investigation ) obj ).getId() );

        org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
        queryObject.setParameterList( "investigations", investigatorsById );

        ScrollableResults list = queryObject.scroll();
        Map<Analysis, Collection<Investigation>> results = new HashMap<Analysis, Collection<Investigation>>();

        while ( list.next() ) {
            Investigation inv = ( Investigation ) list.get( 1 );
            Analysis ana = ( Analysis ) list.get( 0 );

            if ( results.containsKey( ana ) )
                results.get( ana ).add( inv );
            else {
                Collection<Investigation> invs = new HashSet<Investigation>();
                invs.add( inv );
                results.put( ana, invs );
            }
        }

        return results;
    }

}