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
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService.AnalysisType;
import ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
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
 * @author keshav
 * @version $Id$
 */
@Service
public class AnalysisSelectionAndExecutionService implements ApplicationContextAware {

    private static Log log = LogFactory.getLog( AnalysisSelectionAndExecutionService.class );

    private DifferentialExpressionAnalysisHelperService differentialExpressionAnalysisHelperService = new DifferentialExpressionAnalysisHelperService();

    /*
     * Note - we are context-aware so we can get prototype beans.
     */
    private ApplicationContext applicationContext;

    /**
     * Initiates the differential expression analysi
     * 
     * @param expressionExperiment
     * @return
     */
    public Collection<DifferentialExpressionAnalysis> analyze( ExpressionExperiment expressionExperiment ) {

        AbstractDifferentialExpressionAnalyzer analyzer = determineAnalysis( expressionExperiment, expressionExperiment
                .getExperimentalDesign().getExperimentalFactors(), null );
        if ( analyzer == null ) {
            throw new RuntimeException( "Could not locate an appropriate analyzer" );
        }
        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( expressionExperiment );

        return analyses;

    }

    /**
     * @param expressionExperiment
     * @param config
     * @return
     */
    public Collection<DifferentialExpressionAnalysis> analyze( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config ) {
        AbstractDifferentialExpressionAnalyzer analyzer = determineAnalysis( expressionExperiment,
                config.getFactorsToInclude(), config.getAnalysisType(), config.getSubsetFactor() );

        if ( analyzer == null ) {
            throw new RuntimeException( "Could not locate an appropriate analyzer" );
        }

        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( expressionExperiment, config );

        return analyses;
    }

    /**
     * Initiates the differential expression analysis
     * 
     * @param expressionExperiment
     * @return
     */
    public Collection<DifferentialExpressionAnalysis> analyze( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors ) {

        AbstractDifferentialExpressionAnalyzer analyzer = determineAnalysis( expressionExperiment, factors, null );

        if ( analyzer == null ) {
            throw new RuntimeException( "Could not locate an appropriate analyzer" );
        }

        log.info( "Analysis will be done using " + analyzer.getClass().getSimpleName() );

        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( expressionExperiment, factors );

        return analyses;

    }

    /**
     * @param expressionExperiment
     * @param factors
     * @param type
     * @return
     */
    public Collection<DifferentialExpressionAnalysis> analyze( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors, AnalysisType type ) {

        AbstractDifferentialExpressionAnalyzer analyzer = determineAnalysis( expressionExperiment, factors, type, null );

        if ( analyzer == null ) {
            throw new RuntimeException( "Could not locate an appropriate analyzer" );
        }

        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( expressionExperiment, factors );

        return analyses;

    }

    /**
     * @param expressionExperiment
     * @param factors
     * @param type - preselected value rather than inferring it
     * @param subsetFactor - can be null
     * @return
     */
    public AbstractDifferentialExpressionAnalyzer determineAnalysis( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors, AnalysisType type, ExperimentalFactor subsetFactor ) {

        if ( factors.size() == 0 ) {
            throw new IllegalArgumentException( "Must provide at least one factor" );
        }

        if ( type == null ) {
            return determineAnalysis( expressionExperiment, factors, subsetFactor );
        }

        if ( subsetFactor != null ) {
            /*
             * Basically have to make the subsets, and then validate the choice of model on each of those. The following
             * just assumes that we're going to do something very simple.
             */
            return this.applicationContext.getBean( GenericAncovaAnalyzer.class );
        }

        switch ( type ) {
            case OWA:
                if ( factors.size() != 1 ) {
                    throw new IllegalArgumentException( "Cannot run One-way ANOVA on more than one factor" );
                }
                return this.applicationContext.getBean( OneWayAnovaAnalyzer.class );
            case TWA:
                validateFactorsForTwoWayANOVA( factors );
                return this.applicationContext.getBean( TwoWayAnovaWithoutInteractionsAnalyzer.class );
            case TWIA:
                validateFactorsForTwoWayANOVA( factors );
                if ( !differentialExpressionAnalysisHelperService.blockComplete( expressionExperiment, factors ) ) {
                    throw new IllegalArgumentException(
                            "Experimental design must be block complete to run Two-way ANOVA with interactions" );
                }
                return this.applicationContext.getBean( TwoWayAnovaWithInteractionsAnalyzer.class );
            case TTEST:
                if ( factors.size() != 1 ) {
                    throw new IllegalArgumentException( "Cannot run t-test on more than one factor " );
                }
                for ( ExperimentalFactor experimentalFactor : factors ) {
                    if ( experimentalFactor.getFactorValues().size() < 2 ) {
                        throw new IllegalArgumentException(
                                "Need at least two levels per factor to run two-sample t-test" );
                    }
                }
                return this.applicationContext.getBean( TTestAnalyzer.class );
            case OSTTEST:
                // one sample t-test.
                if ( factors.size() != 1 ) {
                    throw new IllegalArgumentException( "Cannot run t-test on more than one factor " );
                }
                for ( ExperimentalFactor experimentalFactor : factors ) {
                    if ( experimentalFactor.getFactorValues().size() != 1 ) {
                        throw new IllegalArgumentException( "Need only one level for the factor for one-sample t-test" );
                    }
                }
                return this.applicationContext.getBean( TTestAnalyzer.class );
            case GENERICLM:

                return this.applicationContext.getBean( GenericAncovaAnalyzer.class );
            default:
                throw new IllegalArgumentException( "Analyses of that type (" + type + ")are not yet supported" );
        }

    }

    private void validateFactorsForTwoWayANOVA( Collection<ExperimentalFactor> factors ) {
        if ( factors.size() != 2 ) {
            throw new IllegalArgumentException( "Need exactly two factors to run two-way ANOVA" );
        }
        for ( ExperimentalFactor experimentalFactor : factors ) {
            if ( experimentalFactor.getFactorValues().size() < 2 ) {
                throw new IllegalArgumentException( "Need at least two levels per factor to run ANOVA" );
            }
        }
    }

    /**
     * Determines the analysis to execute based on the experimental factors, factor values, and block design.
     * 
     * @param expressionExperiment
     * @param factors which factors to use, or null if to use all from the experiment
     * @param subsetFactor can ben null
     * @return an appropriate analyzer
     * @throws an exception if the experiment doesn't have a valid experimental design.
     */
    public AbstractDifferentialExpressionAnalyzer determineAnalysis( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> experimentalFactors, ExperimentalFactor subsetFactor ) {

        Collection<ExperimentalFactor> efsToUse = getFactorsToUse( expressionExperiment, experimentalFactors );

        if ( subsetFactor != null ) {
            return this.applicationContext.getBean( GenericAncovaAnalyzer.class );
        }

        if ( efsToUse.size() == 1 ) {

            ExperimentalFactor experimentalFactor = efsToUse.iterator().next();
            Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();

            /*
             * Check that there is more than one value in at least one group
             */
            boolean ok = differentialExpressionAnalysisHelperService.checkValidForLm( expressionExperiment,
                    experimentalFactor );

            if ( !ok ) {
                return null;
            }

            if ( factorValues.isEmpty() )
                throw new IllegalArgumentException(
                        "Collection of factor values is either null or 0. Cannot execute differential expression analysis." );
            if ( factorValues.size() == 1 ) {
                // one sample t-test.
                return this.applicationContext.getBean( TTestAnalyzer.class );
            }

            else if ( factorValues.size() == 2 ) {
                /*
                 * Return t-test analyzer. This can be taken care of by the one way anova, but keeping it separate for
                 * clarity.
                 */
                return this.applicationContext.getBean( TTestAnalyzer.class );
            }

            else {

                /*
                 * Return one way anova analyzer. NOTE: This can take care of the t-test as well, since a one-way anova
                 * with two groups is just a t-test
                 */
                return this.applicationContext.getBean( OneWayAnovaAnalyzer.class );
            }

        }

        else if ( efsToUse.size() == 2 ) {
            /*
             * Candidate for ANOVA
             */
            boolean okForInteraction = true;
            for ( ExperimentalFactor f : efsToUse ) {
                Collection<FactorValue> factorValues = f.getFactorValues();
                if ( factorValues.size() == 1 ) {

                    boolean useIntercept = false;
                    // check for a ratiometric quantitation type.
                    for ( QuantitationType qt : expressionExperiment.getQuantitationTypes() ) {
                        if ( qt.getIsPreferred() && qt.getIsRatio() ) {
                            // use ANOVA but treat the intercept as a factor.
                            useIntercept = true;
                            break;
                        }
                    }

                    if ( useIntercept ) {
                        return this.applicationContext.getBean( GenericAncovaAnalyzer.class );
                    }

                    return null;
                }
                if ( BatchInfoPopulationService.isBatchFactor( f ) ) {
                    log.info( "One of the two factors is 'batch', not using it for an interaction" );
                    okForInteraction = false;
                }
            }
            /* Check for block design and execute two way anova (with or without interactions). */
            if ( !differentialExpressionAnalysisHelperService.blockComplete( expressionExperiment, experimentalFactors )
                    || !okForInteraction ) {
                return this.applicationContext.getBean( TwoWayAnovaWithoutInteractionsAnalyzer.class );
            }
            return this.applicationContext.getBean( TwoWayAnovaWithInteractionsAnalyzer.class );

        } else {
            /*
             * Upstream we bail if there are too many factors.
             */
            return this.applicationContext.getBean( GenericAncovaAnalyzer.class );
        }
    }

    /**
     * @param expressionExperiment
     * @param experimentalFactors
     * @return
     */
    private Collection<ExperimentalFactor> getFactorsToUse( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> experimentalFactors ) {
        Collection<ExperimentalFactor> efsToUse = null;
        if ( experimentalFactors == null || experimentalFactors.isEmpty() ) {
            efsToUse = expressionExperiment.getExperimentalDesign().getExperimentalFactors();
        } else {
            efsToUse = experimentalFactors;
            if ( efsToUse.isEmpty() ) {
                throw new IllegalArgumentException(
                        "No experimental factors.  Cannot execute differential expression analysis." );
            }

            // sanity check...
            for ( ExperimentalFactor experimentalFactor : efsToUse ) {
                if ( !experimentalFactor.getExperimentalDesign().equals( expressionExperiment.getExperimentalDesign() ) ) {
                    throw new IllegalArgumentException( "Factors must come from the experiment provided" );
                }
            }
        }
        return efsToUse;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.
     * ApplicationContext)
     */
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
