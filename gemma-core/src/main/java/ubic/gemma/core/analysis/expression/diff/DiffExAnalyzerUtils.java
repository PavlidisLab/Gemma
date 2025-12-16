package ubic.gemma.core.analysis.expression.diff;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrixImpl;
import ubic.gemma.model.common.measurement.MeasurementUtils;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@CommonsLog
public class DiffExAnalyzerUtils {

    public static final String BIO_MATERIAL_RNAME_PREFIX = "biomat_";
    public static final String FACTOR_RNAME_PREFIX = "fact.";
    public static final String FACTOR_VALUE_RNAME_PREFIX = "fv_";
    public static final String FACTOR_VALUE_BASELINE_SUFFIX = "_base";

    /**
     * This bioAssayDimension shouldn't get persisted; it is only for dealing with subset diff ex. analyses.
     *
     * @param columnsToUse columns to use
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
        return BioAssayDimension.Factory.newInstance( bioAssays );
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
                    if ( ExperimentFactorUtils.isBatchFactor( f ) ) {
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

    public static Protocol createProtocolForConfig( DifferentialExpressionAnalysisConfig config, Map<ExperimentalFactor, FactorValue> baselineFactorValues ) {
        Protocol protocol = Protocol.Factory.newInstance();
        protocol.setName( "Differential expression analysis settings" );
        StringBuilder writer = new StringBuilder();
        writer.append( "# Type of analysis: " )
                .append( config.getAnalysisType() == null ? "Unknown" : config.getAnalysisType().toString() )
                .append( "\n" );

        if ( config.getSubsetFactor() != null ) {
            writer.append( "# Subset factor: " ).append( formatFactor( config.getSubsetFactor(), true ) ).append( "\n" );
        }
        if ( config.getSubsetFactorValue() != null ) {
            writer.append( "# Subset analysis for " ).append( formatFactorValue( config.getSubsetFactorValue(), true ) ).append( "\n" );
        }

        writer.append( "# Factors: " )
                .append( config.getFactorsToInclude().stream()
                        .sorted( ExperimentalFactor.COMPARATOR )
                        .map( ( ExperimentalFactor f ) -> formatFactor( f, true ) )
                        .collect( Collectors.joining( ", " ) ) )
                .append( "\n" );

        if ( !config.getInteractionsToInclude().isEmpty() ) {
            writer.append( "# Interactions: " );
            writer.append( config.getInteractionsToInclude().stream()
                    .map( DiffExAnalyzerUtils::formatInteraction )
                    .collect( Collectors.joining( ", " ) ) );
            writer.append( "\n" );
        } else {
            writer.append( "# No interactions defined.\n" );
        }

        if ( !baselineFactorValues.isEmpty() ) {
            writer.append( "# Baselines:\n" );
            List<ExperimentalFactor> factors = baselineFactorValues.keySet().stream()
                    .sorted( ExperimentalFactor.COMPARATOR )
                    .collect( Collectors.toList() );
            for ( ExperimentalFactor ef : factors ) {
                writer.append( "# " ).append( formatFactor( ef, false ) ).append( ": Baseline = " )
                        .append( formatFactorValue( baselineFactorValues.get( ef ), true ) )
                        .append( "\n" );
            }
        } else {
            writer.append( "# No baseline conditions defined, those will be detected automatically.\n" );
        }

        if ( config.isUseWeights() ) {
            writer.append( "# Using weights (only for RNA-Seq datasets)\n" );
        }

        if ( config.isModerateStatistics() ) {
            writer.append( "# Empirical Bayes moderated statistics used\n" );
        }

        writer.append( "\n" );

        int minimumNumberOfCellsPerSample = config.getMinimumNumberOfCellsPerSample() != null ? config.getMinimumNumberOfCellsPerSample() :
                DifferentialExpressionAnalysisFilter.DEFAULT_MINIMUM_NUMBER_OF_CELLS_PER_SAMPLE;
        if ( minimumNumberOfCellsPerSample > 0 ) {
            writer.append( "# Minimum number of cells per sample: " ).append( minimumNumberOfCellsPerSample ).append( "\n" );
            writer.append( "# Samples with less cells than this value will be masked with missing values.\n" );
            writer.append( "# This filter is only applied if the number of cells is filled for the samples (i.e. for single-cell data).\n" );
            writer.append( "\n" );
        }

        int minimumNumberOfCellsPerGene = config.getMinimumNumberOfCellsPerGene() != null ? config.getMinimumNumberOfCellsPerGene() :
                DifferentialExpressionAnalysisFilter.DEFAULT_MINIMUM_NUMBER_OF_CELLS_PER_GENE;
        if ( minimumNumberOfCellsPerGene > 0 ) {
            writer.append( "# Minimum number of cells per gene: " ).append( minimumNumberOfCellsPerGene ).append( "\n" );
            writer.append( "# Design elements with less cells than this value will be tested.\n" );
            writer.append( "# This filter is only applied if the number of cells is filled for the data vector (i.e. for single-cell data).\n" );
            writer.append( "\n" );
        }

        int minimumNumberOfSamplesToApplyRepetitiveValuesFilter = config.getMinimumNumberOfSamplesToApplyRepetitiveValuesFilter() != null ?
                config.getMinimumNumberOfSamplesToApplyRepetitiveValuesFilter() :
                DifferentialExpressionAnalysisFilter.DEFAULT_MINIMUM_NUMBER_OF_SAMPLES_TO_APPLY_REPETITIVE_VALUES_FILTER;
        if ( minimumNumberOfSamplesToApplyRepetitiveValuesFilter > 0 ) {
            double minimumFractionOfUniqueValues = config.getMinimumFractionOfUniqueValues() != null ?
                    config.getMinimumFractionOfUniqueValues() :
                    DifferentialExpressionAnalysisFilter.DEFAULT_MINIMUM_FRACTION_OF_UNIQUE_VALUES;
            writer.append( "# Minimum fraction of unique values: " ).append( minimumFractionOfUniqueValues ).append( "\n" );
            writer.append( "# Design elements with less unique values as a fraction of the number of assays not be tested.\n" );
            writer.append( "# This is only applied if there are at least " ).append( minimumNumberOfSamplesToApplyRepetitiveValuesFilter ).append( " samples being tested.\n" );
            DifferentialExpressionAnalysisFilter.RepetitiveValuesFilterMode filterMode = config.getRepetitiveValuesFilterMode() != null ? config.getRepetitiveValuesFilterMode() : DifferentialExpressionAnalysisFilter.DEFAULT_REPETITIVE_VALUES_FILTER_MODE;
            switch ( filterMode ) {
                case RANK:
                    writer.append( "# This filter is applied on ranks.\n" );
                    break;
                case NOMINAL:
                    writer.append( "# This filter is applied on nominal values.\n" );
                    break;
                case AUTODETECT:
                    writer.append( "# This filter is applied either on rank or nominal values depending on the data.\n" );
                    break;
            }
        }

        double minimumVariance = config.getMinimumVariance() != null ?
                config.getMinimumVariance() :
                DifferentialExpressionAnalysisFilter.DEFAULT_MINIMUM_VARIANCE;
        if ( minimumVariance > 0 ) {
            writer.append( "# Minimum variance: " ).append( minimumVariance ).append( "\n" );
            writer.append( "# Design elements with variance across assays below this threshold will not be tested.\n" );
            writer.append( "\n" );
        }

        protocol.setDescription( writer.toString() );
        return protocol;
    }

    /**
     * Format an interaction of factors.
     */
    public static String formatInteraction( Set<ExperimentalFactor> i ) {
        return i.stream()
                .sorted( ExperimentalFactor.COMPARATOR )
                .map( f -> formatFactor( f, false ) )
                .collect( Collectors.joining( ":" ) );
    }

    private static String formatFactor( ExperimentalFactor ef, boolean verbose ) {
        return verbose ? ef.toString() : ef.getName();
    }

    private static String formatFactorValue( FactorValue fv, boolean verbose ) {
        return verbose ? fv.toString() : FactorValueUtils.getSummaryString( fv );
    }

    /**
     * Build a design matrix for the given factors and samples.
     *
     * @param factors            factors
     * @param samplesUsed        the samples used
     * @param allowMissingValues whether to allow missing values, if set to true, the returned matrix may contain nulls
     * @return the experimental design matrix
     * @throws IllegalStateException if missing values are found and allowMissingValues is false
     */
    public static ObjectMatrix<BioMaterial, ExperimentalFactor, Object> buildDesignMatrix( List<ExperimentalFactor> factors,
            List<BioMaterial> samplesUsed, boolean allowMissingValues ) {
        ObjectMatrix<BioMaterial, ExperimentalFactor, Object> designMatrix = new ObjectMatrixImpl<>( samplesUsed.size(), factors.size() );
        designMatrix.setColumnNames( factors );
        designMatrix.setRowNames( samplesUsed );
        populateDesignMatrix( designMatrix, factors, samplesUsed, BaselineSelection.getBaselineConditions( samplesUsed, factors ), allowMissingValues );
        return designMatrix;
    }

    /**
     * Build an R-friendly design matrix.
     * <p>
     * Rows and columns use names derived from {@link #nameForR(BioMaterial)}, {@link #nameForR(ExperimentalFactor)} and
     * {@link #nameForR(FactorValue, boolean)} such that the resulting matrix can be passed to R for analysis. It is
     * otherwise identical to {@link #buildDesignMatrix(List, List, boolean)}.
     */
    public static ObjectMatrix<String, String, Object> buildRDesignMatrix( List<ExperimentalFactor> factors,
            List<BioMaterial> samplesUsed, boolean allowMissingValues ) {
        return buildRDesignMatrix( factors, samplesUsed, BaselineSelection.getBaselineConditions( samplesUsed, factors ), allowMissingValues );
    }

    /**
     * A variant of {@link #buildRDesignMatrix(List, List, boolean)} that allows for reusing baselines for repeated
     * calls. This is used for subset analysis.
     */
    public static ObjectMatrix<String, String, Object> buildRDesignMatrix( List<ExperimentalFactor> factors,
            List<BioMaterial> samplesUsed, Map<ExperimentalFactor, FactorValue> baselines, boolean allowMissingValues ) {
        ObjectMatrix<String, String, Object> designMatrix = new ObjectMatrixImpl<>( samplesUsed.size(), factors.size() );
        designMatrix.setColumnNames( factors.stream().map( DiffExAnalyzerUtils::nameForR ).collect( Collectors.toList() ) );
        designMatrix.setRowNames( samplesUsed.stream().map( DiffExAnalyzerUtils::nameForR ).collect( Collectors.toList() ) );
        populateDesignMatrix( designMatrix, factors, samplesUsed, baselines, allowMissingValues );
        return designMatrix;
    }

    private static void populateDesignMatrix( ObjectMatrix<?, ?, Object> designMatrix, List<ExperimentalFactor> factors, List<BioMaterial> samplesUsed, Map<ExperimentalFactor, FactorValue> baselines, boolean allowMissingValues ) {
        Map<ExperimentalFactor, Map<BioMaterial, FactorValue>> factorValueMap = ExperimentalDesignUtils.getFactorValueMap( factors, samplesUsed );
        for ( int i = 0; i < samplesUsed.size(); i++ ) {
            BioMaterial samp = samplesUsed.get( i );
            for ( int j = 0; j < factors.size(); j++ ) {
                ExperimentalFactor factor = factors.get( j );
                Object value = DiffExAnalyzerUtils.extractFactorValueForSample( samp, factor, factorValueMap, baselines.get( factor ) );
                // if the value is null, we have to skip this factor, actually, but we do it later.
                if ( !allowMissingValues && value == null ) {
                    // FIXME: This error could be worked around when we are doing SampleCoexpression. A legitimate
                    //        reason is when we have a DEExclude factor and some samples lack any value for one of the
                    //        other factors. We could detect this but it's kind of complicated, rare, and would only
                    //        apply for that case.
                    throw new IllegalStateException( samp + " does not have a value for " + factor + "." );
                }
                designMatrix.set( i, j, value );
            }
        }
    }

    /**
     * Extract the "value" of a factor for a sample.
     *
     * @param sample   sample
     * @param factor   factor to extract a value for
     * @param baseline the baseline to use (iff the factor is categorical)
     * @return a double for a continuous factor (or null if the measurement is not set), a string for continuous factor
     * or null if the factor is not set for the given sample
     * @throws IllegalStateException if there is more than one factor value assigned to a given sample
     */
    @Nullable
    private static Object extractFactorValueForSample( BioMaterial sample, ExperimentalFactor factor, Map<ExperimentalFactor, Map<BioMaterial, FactorValue>> factorValueMap, @Nullable FactorValue baseline ) {
        Assert.isTrue( factor.getType().equals( FactorType.CONTINUOUS ) || baseline != null,
                "There is no baseline defined for " + factor + "." );
        FactorValue factorValue = factorValueMap.get( factor ).get( sample );
        if ( factorValue == null ) {
            return null;
        }
        return extractFactorValue( factorValue, factor.getType().equals( FactorType.CONTINUOUS ), baseline != null && baseline.equals( factorValue ) );
    }

    private static Object extractFactorValue( FactorValue factorValue, boolean isContinuous, boolean isBaseline ) {
        if ( isContinuous ) {
            if ( factorValue.getMeasurement() == null ) {
                throw new IllegalStateException( "Measurement is null for continuous factor value " + factorValue + "." );
            }
            return MeasurementUtils.measurement2double( factorValue.getMeasurement() );
        } else {
            /*
             * We always use a dummy value. It's not as human-readable but at least we're sure it is unique and
             * R-compliant. (assuming the fv is persistent!)
             */
            return nameForR( factorValue, isBaseline );
        }
    }

    /**
     * Create a name for a sample suitable for R.
     */
    public static String nameForR( BioMaterial sample ) {
        Assert.notNull( sample.getId(), "Sample must have an ID to have a R-suitable name." );
        return BIO_MATERIAL_RNAME_PREFIX + sample.getId();
    }

    /**
     * Create a name for the factor that is suitable for R.
     */
    public static String nameForR( ExperimentalFactor experimentalFactor ) {
        Assert.notNull( experimentalFactor.getId(), "Factor must have an ID to have a R-suitable name." );
        return FACTOR_RNAME_PREFIX + experimentalFactor.getId();
    }

    /**
     * Create a name for the factor value that is suitable for R.
     */
    public static String nameForR( FactorValue fv, boolean isBaseline ) {
        Assert.isTrue( fv.getExperimentalFactor().getType() == FactorType.CATEGORICAL || !isBaseline,
                "Continuous factors cannot have a baseline." );
        Assert.notNull( fv.getId(), "Factor value must have an ID to have a R-suitable name." );
        return FACTOR_VALUE_RNAME_PREFIX + fv.getId() + ( isBaseline ? FACTOR_VALUE_BASELINE_SUFFIX : "" );
    }
}
