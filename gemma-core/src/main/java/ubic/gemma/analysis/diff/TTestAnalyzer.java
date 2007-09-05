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
import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * @author keshav
 * @version $Id$
 */
public class TTestAnalyzer extends AbstractAnalyzer {

    private Log log = LogFactory.getLog( this.getClass() );

    public TTestAnalyzer() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.AbstractAnalyzer#getSignificantGenes(java.util.Collection)
     */
    @Override
    public Collection<Gene> getSignificantGenes( Collection<ExperimentalFactor> experimentalFactors ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.AbstractAnalyzer#getPValues(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      java.util.Collection)
     */
    @Override
    public Hashtable<Gene, Double> getPValues( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> experimentalFactors ) {

        tTest( expressionExperiment, experimentalFactors );

        return null;
    }

    /**
     * @param factorValues
     * @return
     */
    protected double tTest( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> experimentalFactors ) {

        double pVal = 0;

        Collection<ExperimentalFactor> efs = expressionExperiment.getExperimentalDesign().getExperimentalFactors();

        for ( ExperimentalFactor experimentalFactor : experimentalFactors ) {

            if ( !efs.contains( experimentalFactor ) )
                throw new RuntimeException( "Supplied experimental factor " + experimentalFactor
                        + "does not match the experimental factors of the design." );

            // 1. Get the expression values for each factor value. There could be multiple bioassays (levels) for each
            // factor value (group).
            // 2. Store each group of expression values as a double[][] (or another structure in baseCode).
            // 3. Each row of the double[][] holds all the values for one group of one factor value. Input this one dim
            // array into the R method (see SimpleTTestAnalyzer).

            // Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();
            //
            // for ( FactorValue factorValue : factorValues ) {
            //                
            // }
        }
        return pVal;
    }
}
