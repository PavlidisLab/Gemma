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
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
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
@Component
public class AnalysisSelectionAndExecutionServiceImpl implements AnalysisSelectionAndExecutionService {

    private static Log log = LogFactory.getLog( AnalysisSelectionAndExecutionServiceImpl.class );

    /*
     * We are context-aware so we can get prototype beans.
     */
    private ApplicationContext applicationContext;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.AnalysisSelectionAndExecutionService#analyze(ubic.gemma.model.expression.
     * experiment.ExpressionExperiment)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> analyze( ExpressionExperiment expressionExperiment ) {

        AnalysisType analyzer = determineAnalysis( expressionExperiment, expressionExperiment.getExperimentalDesign()
                .getExperimentalFactors(), null, true );
        if ( analyzer == null ) {
            throw new RuntimeException( "Could not locate an appropriate analyzer" );
        }
        Collection<DifferentialExpressionAnalysis> analyses = this.applicationContext.getBean( DiffExAnalyzer.class )
                .run( expressionExperiment );

        return analyses;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.AnalysisSelectionAndExecutionService#analyze(ubic.gemma.model.expression.
     * experiment.ExpressionExperiment, ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalysisConfig)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> analyze( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config ) {
        AnalysisType analyzer = determineAnalysis( expressionExperiment, config );

        if ( analyzer == null ) {
            throw new RuntimeException( "Could not locate an appropriate analyzer" );
        }

        Collection<DifferentialExpressionAnalysis> analyses = this.applicationContext.getBean( DiffExAnalyzer.class )
                .run( expressionExperiment, config );

        return analyses;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.AnalysisSelectionAndExecutionService#analyze(ubic.gemma.model.expression.
     * experiment.ExpressionExperiment, java.util.Collection)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> analyze( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors ) {

        AnalysisType analyzer = determineAnalysis( expressionExperiment, factors, null, true );

        if ( analyzer == null ) {
            throw new RuntimeException( "Could not locate an appropriate analyzer" );
        }

        log.info( "Analysis will be done using " + analyzer.getClass().getSimpleName() );

        Collection<DifferentialExpressionAnalysis> analyses = this.applicationContext.getBean( DiffExAnalyzer.class )
                .run( expressionExperiment, factors );

        return analyses;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.AnalysisSelectionAndExecutionService#analyze(ubic.gemma.model.expression.
     * experiment.ExpressionExperiment, java.util.Collection,
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService.AnalysisType)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> analyze( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors, AnalysisType type ) {

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setAnalysisType( type );
        config.setFactorsToInclude( factors );
        AnalysisType analyzer = determineAnalysis( expressionExperiment, config );

        if ( analyzer == null ) {
            throw new RuntimeException( "Could not locate an appropriate analyzer" );
        }

        Collection<DifferentialExpressionAnalysis> analyses = this.applicationContext.getBean( DiffExAnalyzer.class )
                .run( expressionExperiment, factors );

        return analyses;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.expression.diff.AnalysisSelectionAndExecutionService#determineAnalysis(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment, java.util.Collection,
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService.AnalysisType,
     * ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    public AnalysisType determineAnalysis( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config ) {

        if ( config.getFactorsToInclude().isEmpty() ) {
            throw new IllegalArgumentException( "Must provide at least one factor" );
        }

        if ( config.getAnalysisType() == null ) {
            return determineAnalysis( expressionExperiment, config.getFactorsToInclude(), null, true );
        }

        if ( config.getSubsetFactor() != null ) {
            /*
             * Basically have to make the subsets, and then validate the choice of model on each of those. The following
             * just assumes that we're going to do something very simple.
             */
            return AnalysisType.GENERICLM;
        }

        switch ( config.getAnalysisType() ) {
            case OWA:
                if ( config.getFactorsToInclude().size() != 1 ) {
                    throw new IllegalArgumentException( "Cannot run One-way ANOVA on more than one factor" );
                }
                return AnalysisType.OWA;
            case TWA:
                validateFactorsForTwoWayANOVA( config.getFactorsToInclude() );
                if ( !DifferentialExpressionAnalysisUtil.blockComplete( expressionExperiment,
                        config.getFactorsToInclude() ) ) {
                    throw new IllegalArgumentException(
                            "Experimental design must be block complete to run Two-way ANOVA with interactions" );
                }
                return AnalysisType.TWA;
            case TWANI:
                // NO interactions.
                validateFactorsForTwoWayANOVA( config.getFactorsToInclude() );
                return AnalysisType.TWANI;
            case TTEST:
                if ( config.getFactorsToInclude().size() != 1 ) {
                    throw new IllegalArgumentException( "Cannot run t-test on more than one factor " );
                }
                for ( ExperimentalFactor experimentalFactor : config.getFactorsToInclude() ) {
                    if ( experimentalFactor.getFactorValues().size() < 2 ) {
                        throw new IllegalArgumentException(
                                "Need at least two levels per factor to run two-sample t-test" );
                    }
                }
                return AnalysisType.TTEST;
            case OSTTEST:
                // one sample t-test.
                if ( config.getFactorsToInclude().size() != 1 ) {
                    throw new IllegalArgumentException( "Cannot run t-test on more than one factor " );
                }
                for ( ExperimentalFactor experimentalFactor : config.getFactorsToInclude() ) {
                    if ( experimentalFactor.getFactorValues().size() != 1 ) {
                        throw new IllegalArgumentException( "Need only one level for the factor for one-sample t-test" );
                    }
                }
                return AnalysisType.OSTTEST;
            case GENERICLM:

                return AnalysisType.GENERICLM;
            default:
                throw new IllegalArgumentException( "Analyses of that type (" + config.getAnalysisType()
                        + ")are not yet supported" );
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.expression.diff.AnalysisSelectionAndExecutionService#determineAnalysis(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment, java.util.Collection,
     * ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    public AnalysisType determineAnalysis( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> experimentalFactors, ExperimentalFactor subsetFactor,
            boolean includeInteractionsIfPossible ) {

        Collection<ExperimentalFactor> efsToUse = getFactorsToUse( expressionExperiment, experimentalFactors );

        if ( subsetFactor != null ) {
            /*
             * Note that the interaction term might still get used (if selected), we just don't decide here.
             */
            return AnalysisType.GENERICLM;
        }

        if ( efsToUse.size() == 1 ) {

            ExperimentalFactor experimentalFactor = efsToUse.iterator().next();
            Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();

            /*
             * Check that there is more than one value in at least one group
             */
            boolean ok = DifferentialExpressionAnalysisUtil.checkValidForLm( expressionExperiment, experimentalFactor );

            if ( !ok ) {
                return null;
            }

            if ( factorValues.isEmpty() )
                throw new IllegalArgumentException(
                        "Collection of factor values is either null or 0. Cannot execute differential expression analysis." );
            if ( factorValues.size() == 1 ) {
                // one sample t-test.
                return AnalysisType.OSTTEST;
            }

            else if ( factorValues.size() == 2 ) {
                /*
                 * Return t-test analyzer. This can be taken care of by the one way anova, but keeping it separate for
                 * clarity.
                 */
                return AnalysisType.TTEST;
            }

            else {

                /*
                 * Return one way anova analyzer. NOTE: This can take care of the t-test as well, since a one-way anova
                 * with two groups is just a t-test
                 */
                return AnalysisType.OWA;
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
                        return AnalysisType.GENERICLM;
                    }

                    return null;
                }
                if ( BatchInfoPopulationServiceImpl.isBatchFactor( f ) ) {
                    log.info( "One of the two factors is 'batch', not using it for an interaction" );
                    okForInteraction = false;
                }
            }
            /* Check for block design and execute two way anova (with or without interactions). */
            if ( !includeInteractionsIfPossible
                    || !DifferentialExpressionAnalysisUtil.blockComplete( expressionExperiment, experimentalFactors )
                    || !okForInteraction ) {
                return AnalysisType.TWANI; // NO interactions
            }
            return AnalysisType.TWA;
        } else {
            /*
             * Upstream we bail if there are too many factors.
             */
            return AnalysisType.GENERICLM;
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
     * @see
     * ubic.gemma.analysis.expression.diff.AnalysisSelectionAndExecutionService#setApplicationContext(org.springframework
     * .context.ApplicationContext)
     */
    @Override
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
