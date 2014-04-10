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
package ubic.gemma.analysis.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrixImpl;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalysisUtil;
import ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.util.FactorValueVector;

/**
 * @author paul
 * @version $Id$
 */
public class ExperimentalDesignUtils {

    public static final String BATCH_FACTOR_CATEGORY_NAME = ExperimentalFactorService.BATCH_FACTOR_CATEGORY_NAME;

    public static final String BATCH_FACTOR_CATEGORY_URI = ExperimentalFactorService.BATCH_FACTOR_CATEGORY_URI;

    public static final String BATCH_FACTOR_NAME = ExperimentalFactorService.BATCH_FACTOR_NAME;

    public static final String BATCH_FACTOR_NAME_PREFIX = ExperimentalFactorService.BATCH_FACTOR_NAME_PREFIX;
    public static final String FACTOR_VALUE_RNAME_PREFIX = ExperimentalFactorService.FACTOR_VALUE_RNAME_PREFIX;

    /**
     * @param factors
     * @return batch factor, if present, or else null
     */
    public static ExperimentalFactor batchFactor( Collection<ExperimentalFactor> factors ) {
        for ( ExperimentalFactor f : factors ) {
            if ( isBatch( f ) ) {
                return f;
            }
        }
        return null;
    }

    /**
     * Check if a factor has missing values (samples that lack an assigned value)
     * 
     * @param factor
     * @param samplesUsed
     * @param baselines not really important for this
     * @return false if there are any missing values.
     */
    public static boolean isComplete( ExperimentalFactor factor, List<BioMaterial> samplesUsed,
            Map<ExperimentalFactor, FactorValue> baselines ) {
        for ( BioMaterial samp : samplesUsed ) {
            Object value = extractFactorValueForSample( baselines, samp, factor );
            if ( value == null ) return false;
        }

        return true;
    }

    /**
     * Convert factors to a matrix usable in R. The rows are in the same order as the columns of our data matrix
     * (defined by samplesUsed).
     * 
     * @param factors in the order they will be used
     * @param samplesUsed
     * @param baselines
     * @return a design matrix
     */
    public static ObjectMatrix<String, String, Object> buildDesignMatrix( List<ExperimentalFactor> factors,
            List<BioMaterial> samplesUsed, Map<ExperimentalFactor, FactorValue> baselines ) {

        ObjectMatrix<String, String, Object> designMatrix = new ObjectMatrixImpl<String, String, Object>(
                samplesUsed.size(), factors.size() );

        Map<ExperimentalFactor, String> factorNamesInR = new LinkedHashMap<ExperimentalFactor, String>();
        for ( ExperimentalFactor factor : factors ) {
            factorNamesInR.put( factor, nameForR( factor ) );
        }
        designMatrix.setColumnNames( new ArrayList<String>( factorNamesInR.values() ) );

        List<String> rowNames = new ArrayList<String>();

        int row = 0;
        for ( BioMaterial samp : samplesUsed ) {

            rowNames.add( "biomat_" + samp.getId() );

            int col = 0;
            for ( ExperimentalFactor factor : factors ) {
                Object value = extractFactorValueForSample( baselines, samp, factor );

                designMatrix.set( row, col, value );

                // if the value is null, we have to skip this factor, actually, but we do it later.
                if ( value == null ) {
                    throw new IllegalStateException( "Missing values not tolerated in design matrix" );
                }

                col++;

            }
            row++;

        }

        //
        // /*
        // * Drop columns that have missing values.
        // */
        // List<String> toKeep = new ArrayList<String>();
        // for ( int i = 0; i < designMatrix.columns(); i++ ) {
        // boolean skip = false;
        // Object[] column = designMatrix.getColumn( i );
        // for ( Object o : column ) {
        // if ( o == null ) {
        // skip = true;
        // }
        // }
        //
        // if ( !skip ) {
        // toKeep.add( designMatrix.getColName( i ) );
        // }
        // }
        //
        // if ( toKeep.isEmpty() ) {
        // throw new IllegalStateException( "Design matrix had no columns without missing values" );
        // }
        //
        // if ( toKeep.size() < designMatrix.columns() ) {
        // designMatrix = designMatrix.subsetColumns( toKeep );
        // }

        designMatrix.setRowNames( rowNames );
        return designMatrix;
    }

    /**
     * @param factors
     * @return a new collection (same order as the input)
     */
    public static Collection<ExperimentalFactor> factorsWithoutBatch( Collection<ExperimentalFactor> factors ) {
        Collection<ExperimentalFactor> result = new ArrayList<ExperimentalFactor>();
        for ( ExperimentalFactor f : factors ) {
            if ( !isBatch( f ) ) {
                result.add( f );
            }
        }
        return result;
    }

    /**
     * @param samplesUsed
     * @param factors
     * @return
     */
    public static Map<ExperimentalFactor, FactorValue> getBaselineConditions( List<BioMaterial> samplesUsed,
            List<ExperimentalFactor> factors ) {
        Map<ExperimentalFactor, FactorValue> baselineConditions = ExpressionDataMatrixColumnSort.getBaselineLevels(
                samplesUsed, factors );

        /*
         * For factors that don't have an obvious baseline, use the first factorvalue.
         */
        Collection<FactorValue> factorValuesOfFirstSample = samplesUsed.iterator().next().getFactorValues();
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
     * @param dmatrix
     * @param factors
     * @return
     */
    public static List<BioMaterial> getOrderedSamples( ExpressionDataDoubleMatrix dmatrix,
            List<ExperimentalFactor> factors ) {
        List<BioMaterial> samplesUsed = DifferentialExpressionAnalysisUtil.getBioMaterialsForBioAssays( dmatrix );
        samplesUsed = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( samplesUsed, factors );
        return samplesUsed;
    }

    /**
     * @param ef
     * @return true if this factor appears to be a "batch" factor.
     */
    public static boolean isBatch( ExperimentalFactor ef ) {
        if ( ef.getType() != null && ef.getType().equals( FactorType.CONTINUOUS ) ) return false;

        Characteristic category = ef.getCategory();
        if ( ef.getName().equals( BATCH_FACTOR_NAME ) && category.getCategory().equals( BATCH_FACTOR_CATEGORY_NAME ) )
            return true;

        return false;
    }

    /**
     * @param ef
     * @return true if this factor appears to be a "batch" factor.
     */
    public static boolean isBatch( ExperimentalFactorValueObject ef ) {
        if ( ef.getType() != null && ef.getType().equals( FactorType.CONTINUOUS ) ) return false;

        String category = ef.getCategory();
        if ( category != null && ef.getName() != null ) {
            if ( category.equals( BATCH_FACTOR_CATEGORY_NAME ) && ef.getName().contains( BATCH_FACTOR_NAME )
                    && ef.getName().contains( BATCH_FACTOR_NAME_PREFIX ) ) return true;
        }

        return false;
    }

    /**
     * @param experimentalFactor
     * @return true if the factor is continuous; false if it looks to be categorical.
     */
    public static boolean isContinuous( ExperimentalFactor ef ) {
        if ( ef.getType() != null ) {
            return ef.getType().equals( FactorType.CONTINUOUS );
        }
        for ( FactorValue fv : ef.getFactorValues() ) {
            if ( fv.getMeasurement() != null ) {
                try {
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
        return FACTOR_VALUE_RNAME_PREFIX + fv.getId() + ( isBaseline ? "_base" : "" );
    }

    /**
     * @param fv
     * @return
     */
    public static String prettyString( FactorValue fv ) {

        if ( fv.getMeasurement() != null ) {
            return fv.getMeasurement().getValue();
        } else if ( fv.getCharacteristics().isEmpty() ) {
            return fv.getValue();
        }
        StringBuilder buf = new StringBuilder();
        for ( Characteristic c : fv.getCharacteristics() ) {
            buf.append( c.getValue() );
            if ( fv.getCharacteristics().size() > 1 ) buf.append( " | " );
        }
        return buf.toString();

    }

    /**
     * @param expressionExperiment
     * @param removeBatchFactor if true, any factor(s) that look like "batch information" will be ignored.
     * @return
     */
    public static CountingMap<FactorValueVector> getDesignMatrix(
            ExpressionExperiment expressionExperiment, boolean removeBatchFactor ) {

        Collection<ExperimentalFactor> factors = expressionExperiment.getExperimentalDesign()
                .getExperimentalFactors();

        for ( Iterator<ExperimentalFactor> iterator = factors.iterator(); iterator.hasNext(); ) {
            ExperimentalFactor experimentalFactor = iterator.next();
            if ( BatchInfoPopulationServiceImpl.isBatchFactor( experimentalFactor ) ) {
                iterator.remove();
            }
        }

        CountingMap<FactorValueVector> assayCount = new CountingMap<FactorValueVector>();
        for ( BioAssay assay : expressionExperiment.getBioAssays() ) {
            BioMaterial sample = assay.getSampleUsed();
            assayCount.increment( new FactorValueVector( factors, sample.getFactorValues() ) );

        }

        return assayCount;

    }
    
    /**
     * @param factors
     * @param samplesUsed
     * @param baselines
     * @return Experimental design matrix
     */
    public static ObjectMatrix<BioMaterial, ExperimentalFactor, Object> sampleInfoMatrix(
            List<ExperimentalFactor> factors, List<BioMaterial> samplesUsed,
            Map<ExperimentalFactor, FactorValue> baselines ) {

        ObjectMatrix<BioMaterial, ExperimentalFactor, Object> designMatrix = new ObjectMatrixImpl<BioMaterial, ExperimentalFactor, Object>(
                samplesUsed.size(), factors.size() );

        designMatrix.setColumnNames( factors );

        int row = 0;
        for ( BioMaterial samp : samplesUsed ) {

            int col = 0;
            for ( ExperimentalFactor factor : factors ) {

                Object value = extractFactorValueForSample( baselines, samp, factor );

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
     * @param factors
     * @return
     */
    public static List<ExperimentalFactor> sortFactors( Collection<ExperimentalFactor> factors ) {
        List<ExperimentalFactor> facs = new ArrayList<ExperimentalFactor>();
        facs.addAll( factors );
        Collections.sort( facs, new Comparator<ExperimentalFactor>() {
            @Override
            public int compare( ExperimentalFactor o1, ExperimentalFactor o2 ) {
                return o1.getId().compareTo( o2.getId() );
            }
        } );
        return facs;
    }

    /**
     * @param baselines
     * @param samp
     * @param factor
     * @return
     */
    private static Object extractFactorValueForSample( Map<ExperimentalFactor, FactorValue> baselines,
            BioMaterial samp, ExperimentalFactor factor ) {
        FactorValue baseLineFV = baselines.get( factor );

        /*
         * Find this biomaterial's value for the current factor.
         */
        Object value = null;
        boolean found = false;
        for ( FactorValue fv : samp.getFactorValues() ) {

            if ( fv.getExperimentalFactor().equals( factor ) ) {

                if ( found ) {
                    // not unique
                    throw new IllegalStateException( "Biomaterial had more than one value for factor: " + factor );
                }

                boolean isBaseline = baseLineFV != null && fv.equals( baseLineFV );

                if ( isContinuous( factor ) ) {
                    Measurement measurement = fv.getMeasurement();
                    assert measurement != null;
                    try {
                        value = Double.parseDouble( measurement.getValue() );
                    } catch ( NumberFormatException e ) {
                        value = Double.NaN;
                    }
                } else {
                    /*
                     * We always use a dummy value. It's not as human-readable but at least we're sure it is unique and
                     * R-compliant. (assuming the fv is persistent!)
                     */
                    value = nameForR( fv, isBaseline );
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
