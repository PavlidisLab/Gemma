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

import ubic.gemma.model.expression.experiment.FactorValue;

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
     * @see ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResultDaoBase#handleGetFactorValues(ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResult)
     */
    @Override
    protected Collection handleGetFactorValues(
            DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult ) throws Exception {

        final String queryString = "select f from ExpressionAnalysisResultSetImpl rs"
                + " inner join rs.results r inner join rs.experimentalFactor ef inner join ef.factorValues f where r=:differentialExpressionAnalysisResult";

        String[] paramNames = { "differentialExpressionAnalysisResult" };
        Object[] objectValues = { differentialExpressionAnalysisResult };

        return this.getHibernateTemplate().findByNamedParam( queryString, paramNames, objectValues );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResultDaoBase#handleGetFactorValues(java.util.Collection)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Map handleGetFactorValues( Collection differentialExpressionAnalysisResults ) throws Exception {

        Map<DifferentialExpressionAnalysisResult, Collection<FactorValue>> factorValuesByResult = new HashMap<DifferentialExpressionAnalysisResult, Collection<FactorValue>>();

        final String queryString = "select f, r from ExpressionAnalysisResultSetImpl rs"
                + " inner join rs.results r inner join rs.experimentalFactor ef inner join ef.factorValues f where r in (:differentialExpressionAnalysisResults)";

        String[] paramNames = { "differentialExpressionAnalysisResults" };
        Object[] objectValues = { differentialExpressionAnalysisResults };

        List qr = this.getHibernateTemplate().findByNamedParam( queryString, paramNames, objectValues );

        if ( qr == null || qr.isEmpty() ) return factorValuesByResult;

        for ( Object o : qr ) {
            Object[] ar = ( Object[] ) o;
            FactorValue f = ( FactorValue ) ar[0];
            DifferentialExpressionAnalysisResult e = ( DifferentialExpressionAnalysisResult ) ar[1];

            Collection<FactorValue> fvs = null;
            Collection<DifferentialExpressionAnalysisResult> keys = factorValuesByResult.keySet();
            if ( keys.contains( e ) ) {
                fvs = factorValuesByResult.get( e );
                fvs.add( f );
                factorValuesByResult.put( e, fvs );
            } else {
                fvs = new HashSet<FactorValue>();
                fvs.add( f );
                factorValuesByResult.put( e, fvs );
            }

            log.info( e );
        }

        return factorValuesByResult;

    }

}