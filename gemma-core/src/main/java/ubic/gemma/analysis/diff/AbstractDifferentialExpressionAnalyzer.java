/*
 * The Gemma project
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
package ubic.gemma.analysis.diff;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.analysis.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * An abstract differential expression analyzer to be extended by analyzers which will make use of R. For example, see
 * {@link OneWayAnovaAnalyzer}.
 * 
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractDifferentialExpressionAnalyzer extends AbstractAnalyzer {

    private Log log = LogFactory.getLog( this.getClass() );

    /**
     * @param expressionExperiment
     * @return ExpressionAnalysis
     */
    public abstract DifferentialExpressionAnalysis getDifferentialExpressionAnalysis(
            ExpressionExperiment expressionExperiment );

    /**
     * @param pvalues
     * @return returns the qvalues for the pvalues
     */
    protected double[] getQValues( double[] pvalues ) {

        if ( rc == null ) {
            connectToR();
        }
        rc.loadLibrary( "qvalue" );

        StringBuffer qvalueCommand = new StringBuffer();
        String pvalsName = "pvals";
        rc.assign( pvalsName, pvalues );
        qvalueCommand.append( "qvalue(" + pvalsName + ")$qvalues" );
        double[] qvalues = rc.doubleArrayEval( qvalueCommand.toString() );

        if ( qvalues == null ) {
            log.error( "Null qvalues.  Check the R side." );
            return null;
        }

        if ( qvalues.length != pvalues.length ) {
            log.error( "Number of q values and p values must match.  Qvalues - " + qvalues.length + ": Pvalues - "
                    + pvalues.length );
            return null;
        }

        return qvalues;
    }

    /**
     * Returns the preferred {@link QuantitationType}.
     * 
     * @param vectors
     * @return
     */
    protected QuantitationType getPreferredQuantitationType( Collection<DesignElementDataVector> vectors ) {
        // FIXME could be slow?
        QuantitationType qt = null;
        for ( DesignElementDataVector vector : vectors ) {
            qt = vector.getQuantitationType();
            if ( qt.getIsPreferred() ) {
                break;
            }
        }
        return qt;
    }
}
