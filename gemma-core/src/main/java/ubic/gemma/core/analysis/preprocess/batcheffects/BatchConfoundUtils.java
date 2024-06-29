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
package ubic.gemma.core.analysis.preprocess.batcheffects;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.springframework.stereotype.Service;
import ubic.basecode.math.KruskalWallis;
import ubic.gemma.core.analysis.preprocess.svd.SVDServiceHelperImpl;
import ubic.gemma.model.expression.experiment.ExperimentalDesignUtils;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.util.*;

/**
 * Test if an experimental design is confounded with batches.
 *
 * @author paul
 * @see BatchConfound
 */
@Service
public class BatchConfoundUtils {

    private static final Log log = LogFactory.getLog( BatchConfoundUtils.class.getName() );

    /**
     *
     * @param ee experiment or experiment subset
     * @return collection of confounds (one for each confounded factor)
     */
    public static Collection<BatchConfound> test( BioAssaySet ee ) {
        Map<ExperimentalFactor, Map<Long, Double>> bioMaterialFactorMap = getBioMaterialFactorMap( ee );
        return factorBatchConfoundTest( ee, bioMaterialFactorMap );
    }

    /**
     *
     * @param ee experiment or experiment subset
     * @return map of factors to map of factor -> bioassay -> factorvalue indicator
     */
    private static Map<ExperimentalFactor, Map<Long, Double>> getBioMaterialFactorMap( BioAssaySet ee ) {
        Map<ExperimentalFactor, Map<Long, Double>> bioMaterialFactorMap = new HashMap<>();

        for ( BioAssay bioAssay : ee.getBioAssays() ) {
            BioMaterial bm = bioAssay.getSampleUsed();
            SVDServiceHelperImpl.populateBMFMap( bioMaterialFactorMap, bm );
        }
        return bioMaterialFactorMap;
    }

    /**
     *
     * @param ee experiment or subset
     * @param bioMaterialFactorMap as per getBioMaterialFactorMap()
     * @return collection of BatchConfoundValueObjects
     */
    private static Collection<BatchConfound> factorBatchConfoundTest( BioAssaySet ee,
            Map<ExperimentalFactor, Map<Long, Double>> bioMaterialFactorMap ) throws IllegalArgumentException {

        Map<Long, Long> batchMembership = new HashMap<>();
        ExperimentalFactor batchFactor = null;
        Map<Long, Integer> batchIndexes = new HashMap<>();
        Collection<Long> usedBatches = new HashSet<>(); // track batches these samples actually occupy
        for ( ExperimentalFactor ef : bioMaterialFactorMap.keySet() ) {
            if ( ExperimentalDesignUtils.isBatch( ef ) ) {
                batchFactor = ef;

                Map<Long, Double> bmToFv = bioMaterialFactorMap.get( batchFactor );

                if ( bmToFv == null ) {
                    log.warn( "No biomaterial --> factor value map for batch factor: " + batchFactor );
                    continue;
                }

                int index = 0;
                for ( FactorValue fv : batchFactor.getFactorValues() ) {
                    batchIndexes.put( fv.getId(), index++ );
                }

                for ( Long bmId : bmToFv.keySet() ) {
                    batchMembership.put( bmId, bmToFv.get( bmId ).longValue() ); // not perfectly safe cast
                    usedBatches.add( bmToFv.get( bmId ).longValue() );
                }
                break;
            }
        }

        Set<BatchConfound> result = new HashSet<>();

        // note that a batch can be "used" but irrelevant in a subset for some factors if they are only applied to some samples
        // so we have to do more checking later.
        if ( batchFactor == null || usedBatches.size() < 2 ) {
            return result; // there can be no confound if there is no batch info or only one batch
        }

        /*
         * Compare other factors to batches to look for confounds.
         */

        for ( ExperimentalFactor ef : bioMaterialFactorMap.keySet() ) {

            if ( ef.equals( batchFactor ) )
                continue;

            // ignore factors that we add with the aim of resolving confounds.
            if ( ef.getCategory() != null && ef.getCategory().getValue().equalsIgnoreCase( "collection of material" ) )
                continue;

            Map<Long, Double> bmToFv = bioMaterialFactorMap.get( ef );
            Collection<Double> usedFactorValues = new HashSet<>( bmToFv.values() );
            int numBioMaterials = bmToFv.keySet().size();

            assert numBioMaterials > 0 : "No biomaterials for " + ef;

            double p = Double.NaN;
            double chiSquare;
            int df;

            int numBatches = batchFactor.getFactorValues().size();
            if ( ExperimentalDesignUtils.isContinuous( ef ) ) {

                DoubleArrayList factorValues = new DoubleArrayList( numBioMaterials );
                factorValues.setSize( numBioMaterials );

                IntArrayList batches = new IntArrayList( numBioMaterials );
                batches.setSize( numBioMaterials );

                int j = 0;
                for ( Long bmId : bmToFv.keySet() ) {

                    assert factorValues.size() > 0 : "Biomaterial to factorValue is empty for " + ef;

                    factorValues.set( j, bmToFv.get( bmId ) ); // ensures we only look at actually used factorvalues.
                    long batch = batchMembership.get( bmId );
                    batches.set( j, batchIndexes.get( batch ) );
                    j++;
                }

                p = KruskalWallis.test( factorValues, batches );
                df = KruskalWallis.dof( factorValues, batches );
                chiSquare = KruskalWallis.kwStatistic( factorValues, batches );

//                log.debug( "KWallis\t" + ee.getId() + "\t" + ee.getShortName() + "\t" + ef.getId() + "\t" + ef.getName()
//                        + "\t" + String.format( "%.2f", chiSquare ) + "\t" + df + "\t" + String.format( "%.2g", p )
//                        + "\t" + numBatches );
            } else {

                Map<Long, Integer> factorValueIndexes = new HashMap<>();
                int index = 0;
                for ( FactorValue fv : ef.getFactorValues() ) {
                    // only use the used factorvalues
                    if ( !usedFactorValues.contains( fv.getId().doubleValue() ) ) {
                        continue;
                    }

                    factorValueIndexes.put( fv.getId(), index++ );
                }

                if ( factorValueIndexes.size() < 2 ) {
                    // This can happen if we're looking at a subset by this factor
                    continue; // to the next factor
                }

                Map<Long, Long> factorValueMembership = new HashMap<>();

                for ( Long bmId : bmToFv.keySet() ) {
                    factorValueMembership.put( bmId, bmToFv.get( bmId ).longValue() );
                }

                // numbatches could still be incorrect, so we have to clean this up later.
                long[][] counts = new long[numBatches][usedFactorValues.size()];

                for ( int i = 0; i < batchIndexes.size(); i++ ) {
                    for ( int j = 0; j < factorValueIndexes.size(); j++ ) {
                        counts[i][j] = 0;
                    }
                }

                for ( Long bm : bmToFv.keySet() ) {
                    Long batch = batchMembership.get( bm );
                    if ( batch == null || !batchIndexes.containsKey( batch ) ) {
                        log.warn( "No batch membership for : " + bm );
                        continue;
                    }
                    int batchIndex = batchIndexes.get( batch );
                    Long fv = factorValueMembership.get( bm );
                    if ( fv == null || !factorValueIndexes.containsKey( fv ) ) {
                        log.warn( "No factor value for : " + bm );
                        continue;
                    }
                    int factorIndex = factorValueIndexes.get( fv );
                    counts[batchIndex][factorIndex]++;
                }

                // check for unused batches
                List<Integer> usedBatchesForFactor = new ArrayList<>();
                int i = 0;
                for ( long[] c : counts ) {
                    long total = 0;
                    for ( long f : c ) {
                        total += f;
                    }
                    if ( total == 0 ) {
                        log.debug( "Batch " + i + " not used by " + ef + " in " + ee );
                    } else {
                        usedBatchesForFactor.add( i );
                    }
                    i++;
                }

                // trim down again
                long[][] finalCounts = new long[usedBatchesForFactor.size()][];
                int j = 0;
                for ( int b : usedBatchesForFactor ) {
                    finalCounts[j++] = counts[b];
                }
                if ( finalCounts.length < 2 ) {
                    continue; // to the next factor
                }

                /*
                 * The problem with chi-square test is it is underpowered and we don't detect perfect confounds
                 * when the sample size is small e.g. 3 + 3.
                 * So for small sample sizes we apply some special cases 1) when we have a 2x2 table and 3) when we have a small number of batches and observations.
                 * Otherwise we use the chisquare test.
                 */
                ChiSquareTest cst = new ChiSquareTest();
                // initialize this value; we'll use it when my special test doesn't turn up anything.
                try {
                    chiSquare = cst.chiSquare( finalCounts );
                } catch ( IllegalArgumentException e ) {
                    log.warn( "IllegalArgumentException exception computing ChiSq for : " + ef + "; Error was: " + e
                            .getMessage() );
                    chiSquare = Double.NaN;
                }

                if ( finalCounts.length == 2 && finalCounts[0].length == 2 ) { // treat as odds ratio computation
                    double numerator = ( double ) finalCounts[0][0] * finalCounts[1][1];
                    double denominator = ( double ) finalCounts[0][1] * finalCounts[1][0];

                    // if either value is zero, we have a perfect confound
                    if ( numerator == 0 || denominator == 0 ) {
                        chiSquare = Double.POSITIVE_INFINITY; // effectively we shift to fisher's exact test here.
                    }

                } else if ( numBioMaterials <= 10 && finalCounts.length <= 4 ) { // number of batches and number of samples is small

                    // look for pairs of rows and columns where there is only one non-zero value in each, which would be a confound.
                    for ( int r = 0; r < finalCounts.length; r++ ) {
                        int numNonzero = 0;
                        int nonZeroIndex = -1;
                        for ( int c = 0; c < finalCounts[0].length; c++ ) {
                            if ( finalCounts[r][c] != 0 ) {
                                numNonzero++;
                                nonZeroIndex = c;
                            }
                        }
                        // inspect the column
                        if ( numNonzero == 1 ) {
                            int numNonzeroColumnVals = 0;
                            for ( int r2 = 0; r2 < finalCounts.length; r2++ ) {
                                if ( finalCounts[r2][nonZeroIndex] != 0 ) {
                                    numNonzeroColumnVals++;
                                }
                            }
                            if ( numNonzeroColumnVals == 1 ) {
                                chiSquare = Double.POSITIVE_INFINITY;
                                break;
                            }
                        }
                    }
                }

                df = ( finalCounts.length - 1 ) * ( finalCounts[0].length - 1 );
                ChiSquaredDistribution distribution = new ChiSquaredDistribution( df );

                if ( chiSquare == Double.POSITIVE_INFINITY ) {
                    p = 0.0;
                } else if ( !Double.isNaN( chiSquare ) ) {
                    p = 1.0 - distribution.cumulativeProbability( chiSquare );
                }

//                log.debug( "ChiSq\t" + ee.getId() + "\t" + ee.getShortName() + "\t" + ef.getId() + "\t" + ef.getName()
//                        + "\t" + String.format( "%.2f", chiSquare ) + "\t" + df + "\t" + String.format( "%.2g", p )
//                        + "\t" + numBatches );
            }

            BatchConfound summary = new BatchConfound( ee, ef, chiSquare, df, p, numBatches );

            result.add( summary );
        }
        return result;
    }
}
