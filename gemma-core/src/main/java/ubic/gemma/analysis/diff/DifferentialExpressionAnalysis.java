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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.analysis.ExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * A differential expression analysis tool that executes the appropriate analysis based on the number of experimental
 * factors and factor values, as well as the block design.
 * <p>
 * Implementations of the selected analyses; t-test, one way anova, and two way anova with and without interactions are
 * based on the details of the paper written by P. Pavlidis, Methods 31 (2003) 282-289.
 * <p>
 * See http://www.bioinformatics.ubc.ca/pavlidis/lab/docs/reprints/anova-methods.pdf.
 * 
 * @spring.bean id="differentialExpressionAnalysis"
 * @spring.property name="studenttTestAnalyzer" ref="tTestAnalyzer"
 * @spring.property name="oneWayAnovaAnalyzer" ref="oneWayAnovaAnalyzer"
 * @spring.property name="twoWayAnovaWithInteractionsAnalyzer" ref="twoWayAnovaWithInteractionsAnalyzer"
 * @spring.property name="twoWayAnovaWithoutInteractionsAnalyzer" ref="twoWayAnovaWithoutInteractionsAnalyzer"
 * @spring.property name="analyzerHelper" ref="analyzerHelper"
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysis {
    private Log log = LogFactory.getLog( this.getClass() );

    private int EXPERIMENTAL_FACTOR_ONE = 1;
    private int EXPERIMENTAL_FACTOR_TWO = 2;
    private int FACTOR_VALUE_ONE = 1;
    private int FACTOR_VALUE_TWO = 2;

    private TTestAnalyzer studenttTestAnalyzer = null;
    private OneWayAnovaAnalyzer oneWayAnovaAnalyzer = null;
    private TwoWayAnovaWithInteractionsAnalyzer twoWayAnovaWithInteractionsAnalyzer = null;
    private TwoWayAnovaWithoutInteractionsAnalyzer twoWayAnovaWithoutInteractionsAnalyzer = null;
    private AnalyzerHelper analyzerHelper = null;

    ExpressionAnalysis expressionAnalysis = null;

    /**
     * Initiates the differential expression analysis (this is the entry point).
     * 
     * @param expressionExperiment
     */
    public void analyze( ExpressionExperiment expressionExperiment ) {

        AbstractDifferentialExpressionAnalyzer analyzer = determineAnalysis( expressionExperiment );

        expressionAnalysis = analyzer.getExpressionAnalysis( expressionExperiment );

    }

    /**
     * Returns the expression analysis and results from the executed analysis.
     * 
     * @return
     */
    public ExpressionAnalysis getExpressionAnalysis() {
        if ( expressionAnalysis == null )
            throw new RuntimeException( "Analysis was never executed.  Run the analysis first." );
        return expressionAnalysis;
    }

    /**
     * Determines the analysis to execute based on the experimental factors, factor values, and block design.
     * 
     * @param expressionExperiment
     * @return
     */
    protected AbstractDifferentialExpressionAnalyzer determineAnalysis( ExpressionExperiment expressionExperiment ) {

        Collection<ExperimentalFactor> experimentalFactors = expressionExperiment.getExperimentalDesign()
                .getExperimentalFactors();

        if ( colIsEmpty( experimentalFactors ) ) {
            throw new RuntimeException(
                    "Collection of experimental factors is either null or 0.  Cannot execute differential expression analysis." );
        }

        if ( experimentalFactors.size() == EXPERIMENTAL_FACTOR_ONE ) {

            log.info( "1 experimental factor found." );

            ExperimentalFactor experimentalFactor = experimentalFactors.iterator().next();
            Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();

            if ( colIsEmpty( factorValues ) )
                throw new RuntimeException(
                        "Collection of factor values is either null or 0. Cannot execute differential expression analysis." );
            if ( factorValues.size() == FACTOR_VALUE_ONE ) {
                throw new RuntimeException( experimentalFactors.size() + " experimental factor(s) with "
                        + factorValues.size() + " factor value(s).  Cannot execute differential expression analysis." );
            }

            else if ( factorValues.size() == FACTOR_VALUE_TWO ) {
                /*
                 * Return t-test analyzer. This can be taken care of by the one way anova, but keeping it separate for
                 * clarity.
                 */
                log.info( "Running t test." );
                return studenttTestAnalyzer;
            }

            else {
                log.info( factorValues.size() + " factor values.  Running one way anova." );
                /*
                 * Return one way anova analyzer. This can take care of the t-test as well, since a one-way anova with
                 * two groups is just a t-test
                 */
                return oneWayAnovaAnalyzer;
            }

        }

        else if ( experimentalFactors.size() == EXPERIMENTAL_FACTOR_TWO ) {

            log.info( "2 experimental factors found." );

            for ( ExperimentalFactor f : experimentalFactors ) {
                Collection<FactorValue> factorValues = f.getFactorValues();
                if ( colIsEmpty( factorValues ) || factorValues.size() < FACTOR_VALUE_TWO ) {
                    throw new RuntimeException( experimentalFactors.size() + " experimental factor(s) with "
                            + factorValues.size()
                            + " factor value(s).  Cannot execute differential expression analysis." );
                }
            }
            /* Check for block design and execute two way anova (with or without interactions). */
            if ( !analyzerHelper.blockComplete( expressionExperiment ) ) {
                log.info( "Running two way anova without interactions." );
                return twoWayAnovaWithoutInteractionsAnalyzer;
            } else {
                log.info( "Running two way anova with interactions." );
                return twoWayAnovaWithInteractionsAnalyzer;
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
        if ( col == null || col.size() == 0 ) return true;

        return false;
    }

    public void setOneWayAnovaAnalyzer( OneWayAnovaAnalyzer oneWayAnovaAnalyzer ) {
        this.oneWayAnovaAnalyzer = oneWayAnovaAnalyzer;
    }

    public void setTwoWayAnovaWithInteractionsAnalyzer(
            TwoWayAnovaWithInteractionsAnalyzer twoWayAnovaWithInteractionsAnalyzer ) {
        this.twoWayAnovaWithInteractionsAnalyzer = twoWayAnovaWithInteractionsAnalyzer;
    }

    public void setTwoWayAnovaWithoutInteractionsAnalyzer(
            TwoWayAnovaWithoutInteractionsAnalyzer twoWayAnovaWithoutInteractionsAnalyzer ) {
        this.twoWayAnovaWithoutInteractionsAnalyzer = twoWayAnovaWithoutInteractionsAnalyzer;
    }

    public void setStudenttTestAnalyzer( TTestAnalyzer studenttTestAnalyzer ) {
        this.studenttTestAnalyzer = studenttTestAnalyzer;
    }

    public void setAnalyzerHelper( AnalyzerHelper analyzerHelper ) {
        this.analyzerHelper = analyzerHelper;
    }
}
