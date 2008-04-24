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
package ubic.gemma.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Contains methods to perform 'common' queries that are needed across DAOs.
 * 
 * @author paul
 * @version $Id$
 */
public class CommonQueries {

    /**
     * @param ees collection of expression experiments.
     * @return map of array designs to the experiments they were used in.
     */
    public static Map<ArrayDesign, Collection<ExpressionExperiment>> getArrayDesignsUsed(
            Collection<ExpressionExperiment> ees, Session session ) {
        Map<ArrayDesign, Collection<ExpressionExperiment>> eeAdMap = new HashMap<ArrayDesign, Collection<ExpressionExperiment>>();
        
        //Safety 1st....
        if (ees == null || ees.isEmpty())   return eeAdMap;
        
        final String eeAdQuery = "select distinct ee,b.arrayDesignUsed from ExpressionExperimentImpl as ee inner join "
                + "ee.bioAssays b where ee in (:ees)";

        org.hibernate.Query queryObject = session.createQuery( eeAdQuery );
        queryObject.setCacheable( true );
        queryObject.setParameterList( "ees", ees );

        List qr = queryObject.list();
        for ( Object o : qr ) {
            Object[] ar = ( Object[] ) o;
            ExpressionExperiment ee = ( ExpressionExperiment ) ar[0];
            ArrayDesign ad = ( ArrayDesign ) ar[1];
            if ( !eeAdMap.containsKey( ad ) ) {
                eeAdMap.put( ad, new HashSet<ExpressionExperiment>() );
            }
            eeAdMap.get( ad ).add( ee );
        }

        return eeAdMap;
    }

}
