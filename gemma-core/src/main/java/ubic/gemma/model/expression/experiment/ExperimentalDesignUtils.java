/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.expression.experiment;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrixImpl;
import ubic.gemma.core.analysis.expression.diff.BaselineSelection;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author paul
 */
@CommonsLog
@ParametersAreNonnullByDefault
public class ExperimentalDesignUtils {

    /**
     * A list of all categories considered to be batch.
     */
    public static final List<Category> BATCH_FACTOR_CATEGORIES = Collections.singletonList( Categories.BLOCK );
    /**
     * Name used by a batch factor.
     * <p>
     * This is used only if the factor lacks a category.
     */
    public static final String BATCH_FACTOR_NAME = "batch";

    public static final String BIO_MATERIAL_RNAME_PREFIX = "biomat_";
    public static final String FACTOR_RNAME_PREFIX = "fact.";
    public static final String FACTOR_VALUE_RNAME_PREFIX = "fv_";
    public static final String FACTOR_VALUE_BASELINE_SUFFIX = "_base";

    /**
     * Check if a factor has missing values (samples that lack an assigned value)
     * @param samplesUsed the samples used
     * @param factor      the factor
     * @return false if there are any missing values.
     */
    public static boolean isComplete( ExperimentalFactor factor, List<BioMaterial> samplesUsed ) {
        Assert.isTrue( samplesUsed.size() > 1, "At least one sample must be supplied." );
        for ( BioMaterial samp : samplesUsed ) {
            if ( samp.getAllFactorValues().stream()
                    .noneMatch( fv -> fv.getExperimentalFactor().equals( factor ) ) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Create a sample to factor value mapping.
     * <p>
     * Under normal circumstances, there should be only one factor value per sample.
     */
    public static Map<BioMaterial, Set<FactorValue>> getSampleToFactorValuesMap( ExperimentalFactor factor, Collection<BioMaterial> samplesUsed ) {
        return samplesUsed.stream()
                .collect( Collectors.toMap( bm -> bm, bm -> bm.getAllFactorValues().stream()
                        .filter( fv -> fv.getExperimentalFactor().equals( factor ) )
                        .collect( Collectors.toSet() ) ) );
    }

    /**
     * Build a design matrix for the given factors and samples.
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
        populateDesignMatrix( designMatrix, factors, samplesUsed, getBaselineConditions( samplesUsed, factors ), allowMissingValues );
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
        return buildRDesignMatrix( factors, samplesUsed, getBaselineConditions( samplesUsed, factors ), allowMissingValues );
    }

    /**
     * A variant of {@link #buildRDesignMatrix(List, List, boolean)} that allows for reusing baselines for repeated
     * calls. This is used for subset analysis.
     */
    public static ObjectMatrix<String, String, Object> buildRDesignMatrix( List<ExperimentalFactor> factors,
            List<BioMaterial> samplesUsed, Map<ExperimentalFactor, FactorValue> baselines, boolean allowMissingValues ) {
        ObjectMatrix<String, String, Object> designMatrix = new ObjectMatrixImpl<>( samplesUsed.size(), factors.size() );
        designMatrix.setColumnNames( factors.stream().map( ExperimentalDesignUtils::nameForR ).collect( Collectors.toList() ) );
        designMatrix.setRowNames( samplesUsed.stream().map( ExperimentalDesignUtils::nameForR ).collect( Collectors.toList() ) );
        populateDesignMatrix( designMatrix, factors, samplesUsed, baselines, allowMissingValues );
        return designMatrix;
    }

    private static void populateDesignMatrix( ObjectMatrix<?, ?, Object> designMatrix, List<ExperimentalFactor> factors, List<BioMaterial> samplesUsed, Map<ExperimentalFactor, FactorValue> baselines, boolean allowMissingValues ) {
        for ( int i = 0; i < samplesUsed.size(); i++ ) {
            BioMaterial samp = samplesUsed.get( i );
            for ( int j = 0; j < factors.size(); j++ ) {
                ExperimentalFactor factor = factors.get( j );
                Object value = extractFactorValueForSample( samp, factor, baselines.get( factor ) );
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

    public static Map<ExperimentalFactor, FactorValue> getBaselineConditions( List<BioMaterial> samplesUsed,
            List<ExperimentalFactor> factors ) {
        Map<ExperimentalFactor, FactorValue> baselineConditions = getBaselineLevels( factors, samplesUsed );

        /*
         * For factors that don't have an obvious baseline, use the first factorvalue.
         */
        Collection<FactorValue> factorValuesOfFirstSample = samplesUsed.iterator().next().getAllFactorValues();
        for ( ExperimentalFactor factor : factors ) {
            if ( !baselineConditions.containsKey( factor ) ) {

                for ( FactorValue biomf : factorValuesOfFirstSample ) {
                    /*
                     * the first biomaterial has the values used as baseline
                     */
                    if ( biomf.getExperimentalFactor().equals( factor ) ) {
                        baselineConditions.put( factor, biomf );
                    }
                }
            }
        }

        /*
         * TODO: for OrganismPart (etc) we should allow there to be no baseline but use the global mean as the reference
         * point.
         */

        return baselineConditions;
    }

    /**
     * Identify the FactorValue that should be treated as 'Baseline' for each of the given factors. This is done
     * heuristically, and if all else fails we choose arbitrarily.
     *
     * @param factors factors
     * @return map
     */
    public static Map<ExperimentalFactor, FactorValue> getBaselineLevels( Collection<ExperimentalFactor> factors ) {
        return getBaselineLevels( factors, null );
    }

    /**
     * Identify the FactorValue that should be treated as 'Baseline' for each of the given factors. This is done
     * heuristically, and if all else fails we choose arbitrarily. For continuous factors, the minimum value is treated
     * as baseline.
     *
     * @param factors     factors
     * @param samplesUsed These are used to make sure we don't bother using factor values as baselines if they are not
     *                    used by any of the samples. This is important for subsets. If null, this is ignored.
     * @return map of factors to the baseline factorvalue for that factor.
     */
    public static Map<ExperimentalFactor, FactorValue> getBaselineLevels( Collection<ExperimentalFactor> factors, @Nullable List<BioMaterial> samplesUsed ) {

        Map<ExperimentalFactor, FactorValue> result = new HashMap<>();

        for ( ExperimentalFactor factor : factors ) {

            if ( factor.getFactorValues().isEmpty() ) {
                throw new IllegalStateException( "Factor has no factor values: " + factor );
            }

            if ( factor.getType().equals( FactorType.CONTINUOUS ) ) {
                // then there is no baseline, but we'll take the minimum value.
                TreeMap<Double, FactorValue> sortedVals = new TreeMap<>();
                for ( FactorValue fv : factor.getFactorValues() ) {

                    /*
                     * Check that this factor value is used by at least one of the given samples. Only matters if this
                     * is a subset of the full data set.
                     */
                    if ( samplesUsed != null && !used( fv, samplesUsed ) ) {
                        // this factorValue cannot be a candidate baseline for this subset.
                        continue;
                    }

                    if ( fv.getMeasurement() == null || fv.getMeasurement().getValue() == null ) {
                        // throw new IllegalStateException( "Continuous factors should have Measurements as values" );
                        // This can happen if a value is missing, as nothing would be added to the BioMaterial.
                        log.warn( "No value for continuous factor " + factor + " for a sample, will treat as NaN" );
                        sortedVals.put( Double.NaN, fv );
                        continue;
                    }

                    if ( fv.getMeasurement().getValue().isEmpty() ) {
                        log.warn( "No value for continuous factor " + factor + " for a sample, will treat as NaN" );
                        sortedVals.put( Double.NaN, fv );
                        continue;
                    }

                    Double v = Double.parseDouble( fv.getMeasurement().getValue() );
                    sortedVals.put( v, fv );
                }

                if ( sortedVals.isEmpty() ) {
                    log.warn( "No values for continuous factor " + factor );
                    continue;
                }
                result.put( factor, sortedVals.firstEntry().getValue() );

            } else {

                for ( FactorValue fv : factor.getFactorValues() ) {

                    /*
                     * Check that this factor value is used by at least one of the given samples. Only matters if this
                     * is a subset of the full data set.
                     */
                    if ( samplesUsed != null && !used( fv, samplesUsed ) ) {
                        // this factorValue cannot be a candidate baseline for this subset.
                        continue;
                    }

                    if ( BaselineSelection.isForcedBaseline( fv ) ) {
                        log.debug( "Baseline chosen: " + fv );
                        result.put( factor, fv );
                        break;
                    }

                    if ( BaselineSelection.isBaselineCondition( fv ) ) {
                        if ( result.containsKey( factor ) ) {
                            log.warn( "A second potential baseline was found for " + factor + ": " + fv );
                            continue;
                        }
                        log.debug( "Baseline chosen: " + fv );
                        result.put( factor, fv );
                    }
                }

                if ( !result.containsKey( factor ) ) { // fallback
                    FactorValue arbitraryBaselineFV = null;

                    if ( samplesUsed != null ) {
                        // make sure we choose a fv that is actually used (see above for non-arbitrary case)
                        for ( FactorValue fv : factor.getFactorValues() ) {
                            for ( BioMaterial bm : samplesUsed ) {
                                for ( FactorValue bfv : bm.getAllFactorValues() ) {
                                    if ( fv.equals( bfv ) ) {
                                        arbitraryBaselineFV = fv;
                                        break;
                                    }
                                }
                                if ( arbitraryBaselineFV != null )
                                    break;
                            }
                            if ( arbitraryBaselineFV != null )
                                break;
                        }

                        if ( arbitraryBaselineFV == null ) {
                            // If we get here, we had passed in the samples in consideration but none had a value assigned.
                            throw new IllegalStateException(
                                    "None of the samplesUsed have a value for factor:  " + factor + " (" + factor
                                            .getFactorValues().size() + " factor values) - ensure samples are assigned this factor" );
                        }

                    } else {
                        // I'm not sure the use case of this line but it would only be used if we didn't pass in any samples to consider.
                        arbitraryBaselineFV = factor.getFactorValues().iterator().next();
                    }

                    // There's no need to log this for batch factors, they are inherently arbitrary and only used
                    // during batch correction.
                    if ( !ExperimentalDesignUtils.isBatchFactor( factor ) ) {
                        log.info( "Falling back on choosing baseline arbitrarily: " + arbitraryBaselineFV );
                    }
                    result.put( factor, arbitraryBaselineFV );
                }
            }
        }

        return result;
    }

    /**
     * @return true if the factorvalue is used by at least one of the samples.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // Better semantics
    private static boolean used( FactorValue fv, List<BioMaterial> samplesUsed ) {
        for ( BioMaterial bm : samplesUsed ) {
            for ( FactorValue bfv : bm.getAllFactorValues() ) {
                if ( fv.equals( bfv ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if a factor is a batch factor.
     */
    public static boolean isBatchFactor( ExperimentalFactor ef ) {
        if ( ef.getType().equals( FactorType.CONTINUOUS ) ) {
            return false;
        }
        Characteristic category = ef.getCategory();
        if ( category != null ) {
            return BATCH_FACTOR_CATEGORIES.stream()
                    .anyMatch( c -> CharacteristicUtils.hasCategory( category, c ) );
        }
        return ExperimentalDesignUtils.BATCH_FACTOR_NAME.equalsIgnoreCase( ef.getName() );
    }

    /**
     * Check if a given factor VO is a batch factor.
     */
    public static boolean isBatchFactor( ExperimentalFactorValueObject ef ) {
        if ( ef.getType().equals( FactorType.CONTINUOUS.name() ) ) {
            return false;
        }
        String category = ef.getCategory();
        String categoryUri = ef.getCategoryUri();
        if ( category != null ) {
            return BATCH_FACTOR_CATEGORIES.stream()
                    .anyMatch( c -> CharacteristicUtils.equals( category, categoryUri, c.getCategory(), c.getCategoryUri() ) );
        }
        return ExperimentalDesignUtils.BATCH_FACTOR_NAME.equalsIgnoreCase( ef.getName() );
    }

    /**
     * Create a name for a sample suitable for R.
     */
    public static String nameForR( BioMaterial sample ) {
        Assert.notNull( sample.getId(), "Sample must have an ID to have a R-suitable name." );
        return ExperimentalDesignUtils.BIO_MATERIAL_RNAME_PREFIX + sample.getId();
    }

    /**
     * Create a name for the factor that is suitable for R.
     */
    public static String nameForR( ExperimentalFactor experimentalFactor ) {
        Assert.notNull( experimentalFactor.getId(), "Factor must have an ID to have a R-suitable name." );
        return ExperimentalDesignUtils.FACTOR_RNAME_PREFIX + experimentalFactor.getId();
    }

    /**
     * Create a name for the factor value that is suitable for R.
     */
    public static String nameForR( FactorValue fv, boolean isBaseline ) {
        Assert.isTrue( fv.getExperimentalFactor().getType() == FactorType.CATEGORICAL || !isBaseline,
                "Continuous factors cannot have a baseline." );
        Assert.notNull( fv.getId(), "Factor value must have an ID to have a R-suitable name." );
        return ExperimentalDesignUtils.FACTOR_VALUE_RNAME_PREFIX + fv.getId() + ( isBaseline ? FACTOR_VALUE_BASELINE_SUFFIX : "" );
    }

    /**
     * Sort factors in a consistent way.
     * <p>
     * For this to work, the factors must be persistent as the order will be based on the numerical ID.
     */
    public static List<ExperimentalFactor> getOrderedFactors( Collection<ExperimentalFactor> factors ) {
        return factors.stream()
                .sorted( Comparator.comparing( ExperimentalFactor::getId ) )
                .collect( Collectors.toList() );
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
    private static Object extractFactorValueForSample( BioMaterial sample, ExperimentalFactor factor, @Nullable FactorValue baseline ) {
        Assert.isTrue( factor.getType().equals( FactorType.CONTINUOUS ) || baseline != null,
                "There is no baseline defined for " + factor + "." );
        Set<FactorValue> factorValues = sample.getAllFactorValues().stream()
                .filter( fv -> fv.getExperimentalFactor().equals( factor ) )
                .collect( Collectors.toSet() );
        if ( factorValues.isEmpty() ) {
            return null;
        } else if ( factorValues.size() > 1 ) {
            // not unique
            throw new IllegalStateException( sample + " had more than one value for " + factor + ": " + factorValues + "." );
        } else {
            FactorValue factorValue = factorValues.iterator().next();
            return extractFactorValue( factorValue, factor.getType().equals( FactorType.CONTINUOUS ), baseline != null && baseline.equals( factorValue ) );
        }
    }

    private static Object extractFactorValue( FactorValue factorValue, boolean isContinuous, boolean isBaseline ) {
        if ( isContinuous ) {
            if ( factorValue.getMeasurement() == null ) {
                throw new IllegalStateException( "Measurement is null for continuous factor value " + factorValue + "." );
            }
            return extractMeasurement( factorValue.getMeasurement() );
        } else {
            /*
             * We always use a dummy value. It's not as human-readable but at least we're sure it is unique and
             * R-compliant. (assuming the fv is persistent!)
             */
            return nameForR( factorValue, isBaseline );
        }
    }

    /**
     * Extract the value of a measurement.
     */
    private static double extractMeasurement( Measurement measurement ) {
        Assert.isTrue( measurement.getRepresentation() == PrimitiveType.DOUBLE,
                "Only double is supported as representation for measurements." );
        if ( measurement.getValue() == null ) {
            // NaN cannot be stored in the database, so we use null to indicate it
            return Double.NaN;
        } else {
            try {
                return Double.parseDouble( measurement.getValue() );
            } catch ( NumberFormatException e ) {
                log.error( "Could not parse value from " + measurement + ", it will be treated as NaN.", e );
                return Double.NaN;
            }
        }
    }
}
