package ubic.gemma.core.analysis.expression.diff;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@CommonsLog
public class DiffExAnalyzerUtils {

    /**
     * This bioAssayDimension shouldn't get persisted; it is only for dealing with subset diff ex. analyses.
     *
     * @param  columnsToUse columns to use
     * @return bio assay dimension
     */
    public static BioAssayDimension createBADMap( List<BioMaterial> columnsToUse ) {
        /*
         * Indices of the biomaterials in the original matrix.
         */
        List<BioAssay> bioAssays = new ArrayList<>();
        for ( BioMaterial bm : columnsToUse ) {
            bioAssays.add( bm.getBioAssaysUsedIn().iterator().next() );
        }

        /*
         * fix the upper level column name maps.
         */
        BioAssayDimension reorderedDim = BioAssayDimension.Factory.newInstance();
        reorderedDim.setBioAssays( bioAssays );
        reorderedDim.setName( "For analysis" );
        reorderedDim.setDescription( bioAssays.size() + " bioAssays" );

        return reorderedDim;
    }

    public static void populateFactorValuesFromBASet( BioAssaySet ee, ExperimentalFactor f,
            Collection<FactorValue> fvs ) {
        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            for ( FactorValue fv : bm.getAllFactorValues() ) {
                if ( fv.getExperimentalFactor().equals( f ) ) {
                    fvs.add( fv );
                }
            }
        }
    }

    /**
     * Convert the data into a string-keyed matrix. Assumes that the row names of the designMatrix
     * are concordant with the column names of the namedMatrix
     */
    public static DoubleMatrix<String, String> makeDataMatrix( ObjectMatrix<String, String, Object> designMatrix,
            DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix ) {

        DoubleMatrix<String, String> sNamedMatrix = new DenseDoubleMatrix<>( namedMatrix.asArray() );
        for ( int i = 0; i < namedMatrix.rows(); i++ ) {
            Assert.notNull( namedMatrix.getRowName( i ).getId(), "Design element must be persistent." );
            sNamedMatrix.addRowName( nameForR( namedMatrix.getRowName( i ) ) );
        }
        sNamedMatrix.setColumnNames( designMatrix.getRowNames() );
        return sNamedMatrix;
    }

    public static String nameForR( CompositeSequence cs ) {
        Assert.notNull( cs.getId(), "Design element must be persistent to have a R-suitable name." );
        return String.valueOf( cs.getId() );
    }

    /**
     * FIXME this should probably deal with the case of outliers and also the {@link LinearModelAnalyzer}'s
     * EXCLUDE_CHARACTERISTICS_VALUES
     *
     * @return selected type of analysis such as t-test, two-way ANOVA, etc.
     */
    public static AnalysisType determineAnalysisType( BioAssaySet bioAssaySet, DifferentialExpressionAnalysisConfig config ) {

        if ( config.getFactorsToInclude().isEmpty() ) {
            throw new IllegalArgumentException( "Must provide at least one factor" );
        }

        if ( config.getAnalysisType() == null ) {
            AnalysisType type = determineAnalysisType( bioAssaySet, config.getFactorsToInclude(), config.getSubsetFactor(), true );

            if ( type == null ) {
                throw new IllegalArgumentException( "The analysis type could not be determined" );
            }

            if ( type.equals( AnalysisType.TWO_WAY_ANOVA_NO_INTERACTION ) ) {
                /*
                 * Ensure the config does not have interactions.
                 */
                log.info( "Any interaction term will be dropped from the configuration as it "
                        + "cannot be analyzed (no replicates? block incomplete?)" );
                config.getInteractionsToInclude().clear();
                // config.setAnalysisType( type ); // don't need this side-effect.
            }
            config.setAnalysisType( type );
            return type;
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
            case TWO_WAY_ANOVA_WITH_INTERACTION:
                validateFactorsForTwoWayANOVA( config.getFactorsToInclude() );
                if ( !DifferentialExpressionAnalysisUtil.blockComplete( bioAssaySet, config.getFactorsToInclude() ) ) {
                    throw new IllegalArgumentException(
                            "Experimental design must be block complete to run Two-way ANOVA with interactions" );
                }
                return AnalysisType.TWO_WAY_ANOVA_WITH_INTERACTION;
            case TWO_WAY_ANOVA_NO_INTERACTION:
                // NO interactions.
                validateFactorsForTwoWayANOVA( config.getFactorsToInclude() );
                return AnalysisType.TWO_WAY_ANOVA_NO_INTERACTION;
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
                        throw new IllegalArgumentException(
                                "Need only one level for the factor for one-sample t-test" );
                    }
                }
                return AnalysisType.OSTTEST;
            case GENERICLM:
                return AnalysisType.GENERICLM;
            default:
                throw new IllegalArgumentException(
                        "Analyses of that type (" + config.getAnalysisType() + ")are not yet supported" );
        }

    }

    /**
     * Determines the analysis to execute based on the experimental factors, factor values, and block design.
     * <p>
     * FIXME: this should probably deal with the case of outliers and also the {@link LinearModelAnalyzer}'s
     *        EXCLUDE_CHARACTERISTICS_VALUES
     *
     * @param bioAssaySet                   experiment or subset to determine the analysis type for
     * @param experimentalFactors           which factors to use, or null if to use all from the experiment
     * @param subsetFactor                  can be null
     * @param includeInteractionsIfPossible include interactions among the provided experimental factors if possible
     * @return an appropriate analysis type
     */
    public static AnalysisType determineAnalysisType( BioAssaySet bioAssaySet, Collection<ExperimentalFactor> experimentalFactors,
            @Nullable ExperimentalFactor subsetFactor, boolean includeInteractionsIfPossible ) {

        Collection<ExperimentalFactor> efsToUse = getFactorsToUse( bioAssaySet, experimentalFactors );

        if ( subsetFactor != null ) {
            /*
             * Note that the interaction term might still get used (if selected), we just don't decide here.
             */
            return AnalysisType.GENERICLM;
        }

        assert !efsToUse.isEmpty();

        /*
         * If any of the factors are continuous, just use a generic glm.
         */
        for ( ExperimentalFactor ef : efsToUse ) {
            if ( ef.getType().equals( FactorType.CONTINUOUS ) ) {
                return AnalysisType.GENERICLM;
            }
        }

        Collection<FactorValue> factorValues;

        switch ( efsToUse.size() ) {
            case 1:

                ExperimentalFactor experimentalFactor = efsToUse.iterator().next();
                factorValues = experimentalFactor.getFactorValues();

                /*
                 * Check that there is more than one value in at least one group
                 */
                boolean ok = DifferentialExpressionAnalysisUtil.checkValidForLm( bioAssaySet, experimentalFactor );

                if ( !ok ) {
                    return null;
                }

                if ( factorValues.isEmpty() )
                    throw new IllegalArgumentException(
                            "Collection of factor values is either null or 0. Cannot execute differential expression analysis." );
                switch ( factorValues.size() ) {
                    case 1:
                        // one sample t-test.
                        return AnalysisType.OSTTEST;
                    case 2:
                        /*
                         * Return t-test analyzer. This can be taken care of by the one way anova, but keeping it
                         * separate for
                         * clarity.
                         */
                        return AnalysisType.TTEST;
                    default:

                        /*
                         * Return one way anova analyzer. NOTE: This can take care of the t-test as well, since a
                         * one-way anova
                         * with two groups is just a t-test
                         */
                        return AnalysisType.OWA;
                }

            case 2:
                /*
                 * Candidate for ANOVA
                 */
                boolean okForInteraction = true;
                Collection<QuantitationType> qts = getQts( bioAssaySet );
                for ( ExperimentalFactor f : efsToUse ) {
                    factorValues = f.getFactorValues();
                    if ( factorValues.size() == 1 ) {

                        boolean useIntercept = false;
                        // check for a ratiometric quantitation type.
                        for ( QuantitationType qt : qts ) {
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
                    if ( ExperimentalDesignUtils.isBatchFactor( f ) ) {
                        log.info( "One of the two factors is 'batch', not using it for an interaction" );
                        okForInteraction = false;
                    }

                }
                /* Check for block design and execute two way ANOVA (with or without interactions). */
                if ( !includeInteractionsIfPossible || !DifferentialExpressionAnalysisUtil
                        .blockComplete( bioAssaySet, experimentalFactors ) || !okForInteraction ) {
                    return AnalysisType.TWO_WAY_ANOVA_NO_INTERACTION; // NO interactions
                }
                return AnalysisType.TWO_WAY_ANOVA_WITH_INTERACTION;
            default:
                /*
                 * Upstream we bail if there are too many factors.
                 */
                return AnalysisType.GENERICLM;
        }
    }

    private static Collection<ExperimentalFactor> getFactorsToUse( BioAssaySet bioAssaySet,
            Collection<ExperimentalFactor> experimentalFactors ) {
        Collection<ExperimentalFactor> efsToUse;

        ExperimentalDesign design;

        if ( bioAssaySet instanceof ExpressionExperiment ) {
            design = ( ( ExpressionExperiment ) bioAssaySet ).getExperimentalDesign();
        } else if ( bioAssaySet instanceof ExpressionExperimentSubSet ) {
            design = ( ( ExpressionExperimentSubSet ) bioAssaySet ).getSourceExperiment().getExperimentalDesign();
        } else {
            throw new UnsupportedOperationException( "Cannot deal with a " + bioAssaySet.getClass() );
        }

        if ( design == null ) {
            throw new IllegalStateException( bioAssaySet + " does not have an experimental design." );
        }

        if ( experimentalFactors == null || experimentalFactors.isEmpty() ) {
            efsToUse = new HashSet<>( design.getExperimentalFactors() );
            if ( efsToUse.isEmpty() ) {
                throw new IllegalStateException(
                        "No factors given, nor in the experiment's design. Cannot execute differential expression analysis." );
            }
        } else {
            efsToUse = experimentalFactors;
            if ( efsToUse.isEmpty() ) {
                throw new IllegalArgumentException(
                        "No experimental factors.  Cannot execute differential expression analysis." );
            }

            // sanity check...
            for ( ExperimentalFactor experimentalFactor : efsToUse ) {
                if ( !experimentalFactor.getExperimentalDesign().getId().equals( design.getId() ) ) {
                    throw new IllegalArgumentException( "Factors must come from the experiment provided" );
                }
            }
        }
        return efsToUse;
    }

    private static Collection<QuantitationType> getQts( BioAssaySet bioAssaySet ) {
        if ( bioAssaySet instanceof ExpressionExperiment ) {
            return ( ( ExpressionExperiment ) bioAssaySet ).getQuantitationTypes();
        } else if ( bioAssaySet instanceof ExpressionExperimentSubSet ) {
            return ( ( ExpressionExperimentSubSet ) bioAssaySet ).getSourceExperiment().getQuantitationTypes();
        } else {
            throw new UnsupportedOperationException( "Cannot deal with a " + bioAssaySet.getClass() );
        }
    }

    private static void validateFactorsForTwoWayANOVA( Collection<ExperimentalFactor> factors ) {
        if ( factors.size() != 2 ) {
            throw new IllegalArgumentException( "Need exactly two factors to run two-way ANOVA" );
        }
        for ( ExperimentalFactor experimentalFactor : factors ) {
            if ( experimentalFactor.getFactorValues().size() < 2 ) {
                throw new IllegalArgumentException( "Need at least two levels per factor to run ANOVA" );
            }
        }
    }

    public static void writeConfig( DifferentialExpressionAnalysisConfig config, Writer writer ) throws IOException {
        writer.append( "# Type of analysis: " )
                .append( config.getAnalysisType() == null ? "Unknown" : config.getAnalysisType().toString() )
                .append( "\n" );

        writer.append( "# Factors: " )
                .append( StringUtils.join( config.getFactorsToInclude(), " " ) )
                .append( "\n" );

        if ( config.getSubsetFactor() != null ) {
            writer.append( "# Subset factor: " ).append( formatFactor( config.getSubsetFactor() ) ).append( "\n" );
        }
        if ( config.getSubsetFactorValue() != null ) {
            writer.append( "# Subset analysis for " ).append( formatFactorValue( config.getSubsetFactorValue() ) ).append( "\n" );
        }

        if ( !config.getInteractionsToInclude().isEmpty() ) {
            writer.append( "# Interactions: " );
            writer.append( config.getInteractionsToInclude().stream()
                    .map( i -> i.stream().map( DiffExAnalyzerUtils::formatFactor ).collect( Collectors.joining( ":" ) ) )
                    .collect( Collectors.joining( ", " ) ) );
            writer.append( "\n" );
        }

        if ( !config.getBaselineFactorValues().isEmpty() ) {
            writer.append( "# Baselines:\n" );
            for ( ExperimentalFactor ef : config.getBaselineFactorValues().keySet() ) {
                writer.append( "# " ).append( ef.getName() ).append( ": Baseline = " )
                        .append( formatFactorValue( config.getBaselineFactorValues().get( ef ) ) )
                        .append( "\n" );
            }
        }

        if ( config.isUseWeights() ) {
            writer.append( "# Using weights (only for RNA-Seq datasets)\n" );
        }

        if ( config.isModerateStatistics() ) {
            writer.append( "# Empirical Bayes moderated statistics used\n" );
        }
    }

    private static String formatFactor( ExperimentalFactor ef ) {
        return ef.getName();
    }

    private static String formatFactorValue( FactorValue fv ) {
        return fv.getValue();
    }
}
