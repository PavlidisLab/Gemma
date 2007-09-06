/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysis {
    private Log log = LogFactory.getLog( this.getClass() );

    private int EXPERIMENTAL_FACTOR_ONE = 1;
    private int EXPERIMENTAL_FACTOR_TWO = 2;
    private int FACTOR_VALUE_ONE = 1;
    private int FACTOR_VALUE_TWO = 2;

    HashMap<DesignElement, Double> pvalues = null;

    Collection<DesignElement> significantGenes = null;

    public void analyze( ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> experimentalFactors ) {

        AbstractAnalyzer analyzer = determineAnalysis( experimentalFactors );

        pvalues = analyzer.getPValues( expressionExperiment, experimentalFactors );

        significantGenes = analyzer.getSignificantGenes( experimentalFactors );
    }

    /**
     * Determines the analysis to execute based on the experimental factors and factor values.
     * 
     * @param experimentalFactors
     */
    protected AbstractAnalyzer determineAnalysis( Collection<ExperimentalFactor> experimentalFactors ) {

        if ( colIsEmpty( experimentalFactors ) ) {
            throw new RuntimeException(
                    "Collection of experimental factors is either null or 0.  Cannot execute differential expression analysis." );
        }

        if ( experimentalFactors.size() == EXPERIMENTAL_FACTOR_ONE ) {

            ExperimentalFactor experimentalFactor = experimentalFactors.iterator().next();
            Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();

            if ( colIsEmpty( factorValues ) )
                throw new RuntimeException(
                        "Collection of factor values is either null or 0. Cannot execute differential expression analysis." );
            if ( factorValues.size() == FACTOR_VALUE_ONE ) {
                throw new RuntimeException( experimentalFactors.size() + " experimental factor(s) with "
                        + factorValues.size() + " factor value(s).  Cannot execute differential expression analysis." );
            }

            else if ( experimentalFactors.size() == FACTOR_VALUE_TWO ) {
                return new TTestAnalyzer();
            }

            else {
                log.debug( experimentalFactors.size() + " experimental factor(s) with " + factorValues.size()
                        + " factor value(s).  Running one way anova." );
                // execute one way anova ... this can take care of the t-test as well, since a one-way anova with two
                // groups is just a t-test
                return null;
            }

        }

        else if ( experimentalFactors.size() == EXPERIMENTAL_FACTOR_TWO ) {
            for ( ExperimentalFactor f : experimentalFactors ) {
                Collection<FactorValue> factorValues = f.getFactorValues();
                if ( colIsEmpty( factorValues ) || factorValues.size() < FACTOR_VALUE_TWO ) {
                    throw new RuntimeException( experimentalFactors.size() + " experimental factor(s) with "
                            + factorValues.size()
                            + " factor value(s).  Cannot execute differential expression analysis." );
                }
                // check for block design and execute two way anova (with or without interactions)
                return null;
            }
        }

        throw new RuntimeException(
                "Differential expression analysis supports a maximum of 2 experimental factors at this time." );

    }

    /**
     * @param col
     * @return
     */
    private boolean colIsEmpty( Collection col ) {
        if ( col == null ) return true;

        if ( col.size() == 0 ) return true;

        return false;
    }

}
