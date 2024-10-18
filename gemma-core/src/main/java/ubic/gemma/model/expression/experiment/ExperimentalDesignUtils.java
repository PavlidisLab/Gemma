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

import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrixImpl;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisUtil;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;

import java.util.*;

/**
 * @author paul
 */
public class ExperimentalDesignUtils {

    public static final String BATCH_FACTOR_CATEGORY_NAME = ExperimentalFactorService.BATCH_FACTOR_CATEGORY_NAME;
    public static final String BATCH_FACTOR_CATEGORY_URI = ExperimentalFactorService.BATCH_FACTOR_CATEGORY_URI;
    public static final String BATCH_FACTOR_NAME = ExperimentalFactorService.BATCH_FACTOR_NAME;
    public static final String BATCH_FACTOR_NAME_PREFIX = ExperimentalFactorService.BATCH_FACTOR_NAME_PREFIX;
    public static final String FACTOR_VALUE_RNAME_PREFIX = ExperimentalFactorService.FACTOR_VALUE_RNAME_PREFIX;

    /**
     * Check if a factor has missing values (samples that lack an assigned value)
     *
     * @param baselines   not really important for this
     * @param samplesUsed the samples used
     * @param factor      the factor
     * @return false if there are any missing values.
     */
    public static boolean isComplete( ExperimentalFactor factor, List<BioMaterial> samplesUsed,
            Map<ExperimentalFactor, FactorValue> baselines ) {
        for ( BioMaterial samp : samplesUsed ) {
            Object value = ExperimentalDesignUtils.extractFactorValueForSample( baselines, samp, factor );
            if ( value == null )
                return false;
        }

        return true;
    }

    /**
     * Convert factors to a matrix usable in R. The rows are in the same order as the columns of our data matrix
     * (defined by samplesUsed).
     *
     * @param factors     in the order they will be used
     * @param samplesUsed the samples used
     * @param baselines   the baselines
     * @return a design matrix
     */
    public static ObjectMatrix<String, String, Object> buildDesignMatrix( List<ExperimentalFactor> factors,
            List<BioMaterial> samplesUsed, Map<ExperimentalFactor, FactorValue> baselines ) {

        ObjectMatrix<String, String, Object> designMatrix = new ObjectMatrixImpl<>( samplesUsed.size(),
                factors.size() );

        Map<ExperimentalFactor, String> factorNamesInR = new LinkedHashMap<>();
        for ( ExperimentalFactor factor : factors ) {
            factorNamesInR.put( factor, ExperimentalDesignUtils.nameForR( factor ) );
        }
        designMatrix.setColumnNames( new ArrayList<>( factorNamesInR.values() ) );

        List<String> rowNames = new ArrayList<>();

        int row = 0;
        for ( BioMaterial samp : samplesUsed ) {

            rowNames.add( "biomat_" + samp.getId() );

            int col = 0;
            for ( ExperimentalFactor factor : factors ) {
                Object value = ExperimentalDesignUtils.extractFactorValueForSample( baselines, samp, factor );

                designMatrix.set( row, col, value );

                // if the value is null, we have to skip this factor, actually, but we do it later.
                if ( value == null ) {
                    /*
                     * This error could be worked around when we are doing SampleCoexpression.
                     * A legitimate reason is when we have a DEExclude factor and some samples lack any value for one of the other Factors.
                     * We could detect this but it's kind of complicated, rare, and would only apply for that case.
                     */
                    throw new IllegalStateException( "Missing values not tolerated in design matrix" );
                }

                col++;

            }
            row++;

        }

        designMatrix.setRowNames( rowNames );
        return designMatrix;
    }

    /**
     * @param factors factors
     * @return a new collection (same order as the input)
     */
    public static Collection<ExperimentalFactor> factorsWithoutBatch( Collection<ExperimentalFactor> factors ) {
        Collection<ExperimentalFactor> result = new ArrayList<>();
        for ( ExperimentalFactor f : factors ) {
            if ( !ExperimentalDesignUtils.isBatch( f ) ) {
                result.add( f );
            }
        }
        return result;
    }

    public static Map<ExperimentalFactor, FactorValue> getBaselineConditions( List<BioMaterial> samplesUsed,
            List<ExperimentalFactor> factors ) {
        Map<ExperimentalFactor, FactorValue> baselineConditions = ExpressionDataMatrixColumnSort
                .getBaselineLevels( samplesUsed, factors );

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
     * This puts the control samples up front if possible.
     *
     * @param factors factors
     * @param dmatrix data matrix
     * @return ordered samples
     */
    public static List<BioMaterial> getOrderedSamples( ExpressionDataDoubleMatrix dmatrix,
            List<ExperimentalFactor> factors ) {
        List<BioMaterial> samplesUsed = DifferentialExpressionAnalysisUtil.getBioMaterialsForBioAssays( dmatrix );
        samplesUsed = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( samplesUsed, factors );
        return samplesUsed;
    }

    /**
     * @param ef experimental factor
     * @return true if this factor appears to be a "batch" factor.
     */
    public static boolean isBatch( ExperimentalFactor ef ) {
        if ( ef.getType() != null && ef.getType().equals( FactorType.CONTINUOUS ) ) {
            return false;
        }
        Characteristic category = ef.getCategory();
        return ef.getName().equals( ExperimentalDesignUtils.BATCH_FACTOR_NAME ) &&
                ( category != null && ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME.equals( category.getCategory() ) );
    }

    /**
     * @param ef experimental factor
     * @return true if this factor appears to be a "batch" factor.
     */
    public static boolean isBatch( ExperimentalFactorValueObject ef ) {
        if ( ef.getType() != null && ef.getType().equals( FactorType.CONTINUOUS.name() ) )
            return false;

        String category = ef.getCategory();
        return category != null && ef.getName() != null && category
                .equals( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME ) && ef.getName()
                .contains( ExperimentalDesignUtils.BATCH_FACTOR_NAME ) && ef.getName()
                .contains( ExperimentalDesignUtils.BATCH_FACTOR_NAME_PREFIX );

    }

    /**
     * @param ef experimental factor
     * @return true if the factor is continuous; false if it looks to be categorical.
     */
    public static boolean isContinuous( ExperimentalFactor ef ) {
        if ( ef.getType() != null ) {
            return ef.getType().equals( FactorType.CONTINUOUS );
        }
        for ( FactorValue fv : ef.getFactorValues() ) {
            if ( fv.getMeasurement() != null ) {
                try {
                    //noinspection ResultOfMethodCallIgnored // Checking if parseable
                    Double.parseDouble( fv.getMeasurement().getValue() );
                    return true;
                } catch ( NumberFormatException e ) {
                    return false;
                }
            }
        }
        return false;
    }

    public static String nameForR( ExperimentalFactor experimentalFactor ) {
        return "fact." + experimentalFactor.getId();
    }

    public static String nameForR( FactorValue fv, boolean isBaseline ) {
        return ExperimentalDesignUtils.FACTOR_VALUE_RNAME_PREFIX + fv.getId() + ( isBaseline ? "_base" : "" );
    }

    /**
     * @param factors     factors
     * @param baselines   baselines
     * @param samplesUsed the samples used
     * @return Experimental design matrix
     */
    public static ObjectMatrix<BioMaterial, ExperimentalFactor, Object> sampleInfoMatrix(
            List<ExperimentalFactor> factors, List<BioMaterial> samplesUsed,
            Map<ExperimentalFactor, FactorValue> baselines ) {

        ObjectMatrix<BioMaterial, ExperimentalFactor, Object> designMatrix = new ObjectMatrixImpl<>( samplesUsed.size(),
                factors.size() );

        designMatrix.setColumnNames( factors );

        int row = 0;
        for ( BioMaterial samp : samplesUsed ) {

            int col = 0;
            for ( ExperimentalFactor factor : factors ) {

                Object value = ExperimentalDesignUtils.extractFactorValueForSample( baselines, samp, factor );

                designMatrix.set( row, col, value );

                col++;

            }
            row++;

        }

        designMatrix.setRowNames( samplesUsed );
        return designMatrix;

    }

    /**
     * Sort factors in a consistent way.
     *
     * @param factors factors
     * @return sorted factors
     */
    public static List<ExperimentalFactor> sortFactors( Collection<ExperimentalFactor> factors ) {
        List<ExperimentalFactor> facs = new ArrayList<>( factors );
        Collections.sort( facs, new Comparator<ExperimentalFactor>() {
            @Override
            public int compare( ExperimentalFactor o1, ExperimentalFactor o2 ) {
                return o1.getId().compareTo( o2.getId() );
            }
        } );
        return facs;
    }

    private static Object extractFactorValueForSample( Map<ExperimentalFactor, FactorValue> baselines, BioMaterial samp,
            ExperimentalFactor factor ) {
        FactorValue baseLineFV = baselines.get( factor );

        /*
         * Find this biomaterial's value for the current factor.
         */
        Object value = null;
        boolean found = false;
        for ( FactorValue fv : samp.getAllFactorValues() ) {

            if ( fv.getExperimentalFactor().equals( factor ) ) {

                if ( found ) {
                    // not unique
                    throw new IllegalStateException( "Biomaterial had more than one value for factor: " + factor );
                }

                boolean isBaseline = fv.equals( baseLineFV );

                if ( ExperimentalDesignUtils.isContinuous( factor ) ) {
                    Measurement measurement = fv.getMeasurement();

                    if ( measurement == null ) {
                        value = Double.NaN;
                        continue;
                    } else {
                        try {
                            value = Double.parseDouble( measurement.getValue() );
                        } catch ( NumberFormatException e ) {
                            value = Double.NaN;
                        }
                    }
                } else {
                    /*
                     * We always use a dummy value. It's not as human-readable but at least we're sure it is unique and
                     * R-compliant. (assuming the fv is persistent!)
                     */
                    value = ExperimentalDesignUtils.nameForR( fv, isBaseline );
                }
                found = true;
                // could break here but nice to check for uniqueness.
            }
        }
        if ( !found ) {
            return null;

        }
        return value;
    }
}
