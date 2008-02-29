/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.expression.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;

/**
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResult
 */
public class DifferentialExpressionAnalysisResultDaoImpl extends
        ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResultDaoBase {

    private Log log = LogFactory.getLog( this.getClass() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResultDaoBase#handleGetExperimentalFactors(ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResult)
     */
    @Override
    protected Collection handleGetExperimentalFactors(
            DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult ) throws Exception {

        final String queryString = "select ef from ExpressionAnalysisResultSetImpl rs"
                + " inner join rs.results r inner join rs.experimentalFactor ef where r=:differentialExpressionAnalysisResult";

        String[] paramNames = { "differentialExpressionAnalysisResult" };
        Object[] objectValues = { differentialExpressionAnalysisResult };

        return this.getHibernateTemplate().findByNamedParam( queryString, paramNames, objectValues );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResultDaoBase#handleGetExperimentalFactors(java.util.Collection)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Map handleGetExperimentalFactors( Collection differentialExpressionAnalysisResults ) throws Exception {

        Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> factorsByResult = new HashMap<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>>();
        if ( differentialExpressionAnalysisResults.size() == 0 ) {
            return factorsByResult;
        }

        final String queryString = "select ef, r from ExpressionAnalysisResultSetImpl rs"
                + " inner join rs.results r inner join rs.experimentalFactor ef where r in (:differentialExpressionAnalysisResults)";

        String[] paramNames = { "differentialExpressionAnalysisResults" };
        Object[] objectValues = { differentialExpressionAnalysisResults };

        List qr = this.getHibernateTemplate().findByNamedParam( queryString, paramNames, objectValues );

        if ( qr == null || qr.isEmpty() ) return factorsByResult;

        for ( Object o : qr ) {
            Object[] ar = ( Object[] ) o;
            ExperimentalFactor f = ( ExperimentalFactor ) ar[0];
            DifferentialExpressionAnalysisResult res = ( DifferentialExpressionAnalysisResult ) ar[1];

            if ( !factorsByResult.containsKey( res ) ) {
                factorsByResult.put( res, new HashSet<ExperimentalFactor>() );
            }

            factorsByResult.get( res ).add( f );

            if ( log.isDebugEnabled() ) log.debug( res );
        }

        return factorsByResult;

    }
}