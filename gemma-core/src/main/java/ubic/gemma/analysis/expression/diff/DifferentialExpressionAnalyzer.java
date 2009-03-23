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
package ubic.gemma.analysis.expression.diff;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService.AnalysisType;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
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
 * @spring.bean id="differentialExpressionAnalyzer"
 * @spring.property name="studenttTestAnalyzer" ref="tTestAnalyzer"
 * @spring.property name="oneWayAnovaAnalyzer" ref="oneWayAnovaAnalyzer"
 * @spring.property name="twoWayAnovaWithInteractionsAnalyzer" ref="twoWayAnovaWithInteractionsAnalyzer"
 * @spring.property name="twoWayAnovaWithoutInteractionsAnalyzer" ref="twoWayAnovaWithoutInteractionsAnalyzer"
 * @spring.property name="differentialExpressionAnalysisHelperService" ref="differentialExpressionAnalysisHelperService"
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalyzer {

    private DifferentialExpressionAnalysisHelperService differentialExpressionAnalysisHelperService = null;
    private int EXPERIMENTAL_FACTOR_ONE = 1;
    private int EXPERIMENTAL_FACTOR_TWO = 2;

    private int FACTOR_VALUE_ONE = 1;
    private int FACTOR_VALUE_TWO = 2;
    private Log log = LogFactory.getLog( this.getClass() );
    private OneWayAnovaAnalyzer oneWayAnovaAnalyzer = null;
    private TTestAnalyzer studenttTestAnalyzer = null;
    private TwoWayAnovaWithInteractionsAnalyzer twoWayAnovaWithInteractionsAnalyzer = null;

    private TwoWayAnovaWithoutInteractionsAnalyzer twoWayAnovaWithoutInteractionsAnalyzer = null;

    /**
     * Initiates the differential expression analysis (this is the entry point).
     * 
     * @param expressionExperiment
     * @return
     */
    public DifferentialExpressionAnalysis analyze( ExpressionExperiment expressionExperiment ) {

        AbstractDifferentialExpressionAnalyzer analyzer = determineAnalysis( expressionExperiment );
        if ( analyzer == null ) {
            throw new RuntimeException( "Could not locate an appropriate analyzer" );
        }
        DifferentialExpressionAnalysis analysis = analyzer.run( expressionExperiment );

        return analysis;

    }

    /**
     * Initiates the differential expression analysis (this is the entry point).
     * 
     * @param expressionExperiment
     * @return
     */
    public DifferentialExpressionAnalysis analyze( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors ) {

        AbstractDifferentialExpressionAnalyzer analyzer = determineAnalysis( expressionExperiment );

        if ( analyzer == null ) {
            throw new RuntimeException( "Could not locate an appropriate analyzer" );
        }

        DifferentialExpressionAnalysis analysis = analyzer.run( expressionExperiment, factors );

        return analysis;

    }

    /**
     * Initiates the differential expression analysis (this is the entry point).
     * 
     * @param expressionExperiment
     * @return
     */
    public DifferentialExpressionAnalysis analyze( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors, AnalysisType type ) {

        AbstractDifferentialExpressionAnalyzer analyzer = determineAnalysis( expressionExperiment, factors, type );

        /*
         * FIXME make sure the selected type is compatible with the factors.
         */
        DifferentialExpressionAnalysis analysis = analyzer.run( expressionExperiment, factors );

        return analysis;

    }

    public void setDifferentialExpressionAnalysisHelperService(
            DifferentialExpressionAnalysisHelperService differentialExpressionAnalysisHelperService ) {
        this.differentialExpressionAnalysisHelperService = differentialExpressionAnalysisHelperService;
    }

    public void setOneWayAnovaAnalyzer( OneWayAnovaAnalyzer oneWayAnovaAnalyzer ) {
        this.oneWayAnovaAnalyzer = oneWayAnovaAnalyzer;
    }

    public void setStudenttTestAnalyzer( TTestAnalyzer studenttTestAnalyzer ) {
        this.studenttTestAnalyzer = studenttTestAnalyzer;
    }

    public void setTwoWayAnovaWithInteractionsAnalyzer(
            TwoWayAnovaWithInteractionsAnalyzer twoWayAnovaWithInteractionsAnalyzer ) {
        this.twoWayAnovaWithInteractionsAnalyzer = twoWayAnovaWithInteractionsAnalyzer;
    }

    public void setTwoWayAnovaWithoutInteractionsAnalyzer(
            TwoWayAnovaWithoutInteractionsAnalyzer twoWayAnovaWithoutInteractionsAnalyzer ) {
        this.twoWayAnovaWithoutInteractionsAnalyzer = twoWayAnovaWithoutInteractionsAnalyzer;
    }

    public AbstractDifferentialExpressionAnalyzer determineAnalysis( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors, AnalysisType type ) {

        if ( factors.size() == 0 ) {
            throw new IllegalArgumentException( "Must provide at least one factor" );
        }

        switch ( type ) {
            case OWA:
                if ( factors.size() != 1 ) {
                    throw new IllegalArgumentException( "Cannot run One-way ANOVA on more than one factor" );
                }
                return this.oneWayAnovaAnalyzer;
            case TWA:
                if ( factors.size() != 2 ) {
                    throw new IllegalArgumentException( "Need exactly two factors to run two-way ANOVA" );
                }
                return this.twoWayAnovaWithoutInteractionsAnalyzer;
            case TWIA:
                if ( factors.size() != 2 ) {
                    throw new IllegalArgumentException( "Need exactly two factors to run two-way ANOVA" );
                }
                if ( !differentialExpressionAnalysisHelperService.blockComplete( expressionExperiment, factors ) ) {
                    throw new IllegalArgumentException(
                            "Experimental design must be block complete to run Two-way ANOVA with interactions" );
                }
                return this.twoWayAnovaWithInteractionsAnalyzer;
            case TTEST:
                if ( factors.size() != 1 ) {
                    throw new IllegalArgumentException( "Cannot run t-test on more than one factor " );
                }
                return this.studenttTestAnalyzer;
            default:
                return null;
        }
    }

    /**
     * Determines the analysis to execute based on the experimental factors, factor values, and block design.
     * 
     * @param expressionExperiment
     * @return
     */
    public AbstractDifferentialExpressionAnalyzer determineAnalysis( ExpressionExperiment expressionExperiment ) {

        Collection<ExperimentalFactor> experimentalFactors = expressionExperiment.getExperimentalDesign()
                .getExperimentalFactors();

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

            else if ( factorValues.size() == FACTOR_VALUE_TWO ) {
                /*
                 * Return t-test analyzer. This can be taken care of by the one way anova, but keeping it separate for
                 * clarity.
                 */
                return studenttTestAnalyzer;
            }

            else {
                /*
                 * Return one way anova analyzer. NOTE: This can take care of the t-test as well, since a one-way anova
                 * with two groups is just a t-test
                 */
                return oneWayAnovaAnalyzer;
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
            }
            /* Check for block design and execute two way anova (with or without interactions). */
            if ( !differentialExpressionAnalysisHelperService.blockComplete( expressionExperiment ) ) {
                return twoWayAnovaWithoutInteractionsAnalyzer;
            }
            return twoWayAnovaWithInteractionsAnalyzer;

        }

        log.warn( "Differential expression analysis supports a maximum of 2 experimental factors at this time." );
        return null;

    }

    /**
     * Disconnect from R.
     */
    public void disconnectR() {

        if ( studenttTestAnalyzer == null && oneWayAnovaAnalyzer == null
                && twoWayAnovaWithoutInteractionsAnalyzer == null && twoWayAnovaWithInteractionsAnalyzer == null ) {
            throw new RuntimeException( "No analyzer was specified.  Could not try to terminate R connection." );
        }

        /* disconnect all analyzers */
        if ( studenttTestAnalyzer != null ) {
            studenttTestAnalyzer.disconnectR();
        }
        if ( oneWayAnovaAnalyzer != null ) {
            oneWayAnovaAnalyzer.disconnectR();
        }
        if ( twoWayAnovaWithoutInteractionsAnalyzer != null ) {
            twoWayAnovaWithoutInteractionsAnalyzer.disconnectR();
        }
        if ( twoWayAnovaWithInteractionsAnalyzer != null ) {
            twoWayAnovaWithInteractionsAnalyzer.disconnectR();
        }
    }

    /**
     * @param col
     * @return
     */
    private boolean colIsEmpty( Collection<? extends Object> col ) {
        if ( col == null || col.isEmpty() ) return true;
        return false;
    }

}
