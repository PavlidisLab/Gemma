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
import ubic.gemma.core.util.math.KruskalWallis;
import ubic.gemma.core.analysis.preprocess.svd.SVDServiceImpl;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.util.IdentifiableUtils;

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
     * Smallest number of usable biomaterials the Kruskal-Wallis leg will accept.
     * <p>
     * {@link KruskalWallis#test(DoubleArrayList, IntArrayList)} asserts {@code scores.size() > 2} and its statistic
     * scales by {@code 12 / (n * (n + 1))}, so a smaller sample is either an assertion error or a meaningless number.
     */
    private static final int MIN_BIOMATERIALS_FOR_KRUSKAL_WALLIS = 3;

    /**
     *
     * @param ee experiment or experiment subset
     * @return collection of confounds (one for each confounded factor)
     */
    public static Collection<BatchConfound> test( BioAssaySet ee ) {
        return factorBatchConfoundTest( ee, getBioMaterialFactorMap( ee ) );
    }

    /**
     *
     * @param ee experiment or experiment subset
     * @return map of factors to map of factor -> bioassay -> factorvalue indicator
     */
    private static Map<ExperimentalFactor, Map<BioMaterial, Number>> getBioMaterialFactorMap( BioAssaySet ee ) {
        Map<ExperimentalFactor, Map<BioMaterial, Number>> bioMaterialFactorMap = new HashMap<>();
        for ( BioAssay bioAssay : ee.getBioAssays() ) {
            BioMaterial bm = bioAssay.getSampleUsed();
            SVDServiceImpl.populateBMFMap( bioMaterialFactorMap, bm );
        }
        // fill missing values
        for ( BioAssay bioAssay : ee.getBioAssays() ) {
            bioMaterialFactorMap.forEach( ( ef, map ) -> map.putIfAbsent( bioAssay.getSampleUsed(), null ) );
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
            Map<ExperimentalFactor, Map<BioMaterial, Number>> bioMaterialFactorMap ) throws IllegalArgumentException {

        // BM -> FV
        Map<BioMaterial, FactorValue> batchMembership = new HashMap<>();
        ExperimentalFactor batchFactor = null;
        // FV -> index
        Map<FactorValue, Integer> batchIndexes = new HashMap<>();
        for ( ExperimentalFactor ef : bioMaterialFactorMap.keySet() ) {
            if ( ExperimentFactorUtils.isBatchFactor( ef ) ) {
                batchFactor = ef;

                Map<Long, FactorValue> factorValueById = IdentifiableUtils.getIdMap( ef.getFactorValues() );

                // batch factors are not continuous, so the ID is always a Long
                Map<BioMaterial, Number> bmToFvId = bioMaterialFactorMap.get( batchFactor );

                if ( bmToFvId == null ) {
                    log.warn( "No biomaterial --> factor value map for batch factor: " + batchFactor );
                    continue;
                }

                int index = 0;
                for ( FactorValue fv : batchFactor.getFactorValues() ) {
                    batchIndexes.put( fv, index++ );
                }

                for ( Map.Entry<BioMaterial, Number> bmEntry : bmToFvId.entrySet() ) {
                    BioMaterial bm = bmEntry.getKey();
                    Number fvId = bmEntry.getValue();
                    FactorValue val;
                    if ( fvId != null ) {
                        val = factorValueById.get( ( Long ) fvId );
                    } else {
                        // If a sample is missing a FV for the batch factor, that will basically break all the following
                        // logic, so by assigning a factor value, we can at least continue the analysis.
                        val = batchIndexes.keySet().iterator().next();
                        log.warn( bm + " does not have a factor value for " + ef + ", it was assigned an arbitrary one: " + val + "." );
                    }
                    batchMembership.put( bm, val );
                }
                break;
            }
        }

        // note that a batch can be "used" but irrelevant in a subset for some factors if they are only applied to some samples
        // so we have to do more checking later.
        if ( batchFactor == null || new HashSet<>( batchMembership.values() ).size() < 2 ) {
            log.warn( "There is no batch factor or only one possible batch, no batch confounds are possible." );
            return Collections.emptySet();
        }

        Set<BatchConfound> result = new HashSet<>();

        /*
         * Compare other factors to batches to look for confounds.
         */

        for ( Map.Entry<ExperimentalFactor, Map<BioMaterial, Number>> bmfmEntry : bioMaterialFactorMap.entrySet() ) {
            ExperimentalFactor ef = bmfmEntry.getKey();
            if ( ef.equals( batchFactor ) )
                continue;

            // ignore factors that we add with the aim of resolving confounds.
            // Characteristic.getValue() is nullable, so compare from the literal side
            if ( ef.getCategory() != null && "collection of material".equalsIgnoreCase( ef.getCategory().getValue() ) )
                continue;

            Map<BioMaterial, Number> bmToFv = bmfmEntry.getValue();
            int numBioMaterials = bmToFv.keySet().size();

            assert numBioMaterials > 0 : "No biomaterials for " + ef;

            double p = Double.NaN;
            double chiSquare;
            int df;

            int numBatches = batchFactor.getFactorValues().size();
            if ( ef.getType().equals( FactorType.CONTINUOUS ) ) {

                // getBioMaterialFactorMap() pads every factor's map with a null for each biomaterial that has no
                // factor value, so bmToFv may hold fewer usable entries than numBioMaterials. Append rather than
                // index into a pre-sized list: setSize() pre-fills with 0.0 / 0, so skipping an entry in a pre-sized
                // list would leave the two index-aligned lists carrying a fabricated observation in batch 0.
                DoubleArrayList factorValues = new DoubleArrayList( numBioMaterials );
                IntArrayList batches = new IntArrayList( numBioMaterials );
                Set<Integer> usedBatchesForFactor = new HashSet<>();

                for ( Map.Entry<BioMaterial, Number> bmEntry : bmToFv.entrySet() ) {
                    BioMaterial bm = bmEntry.getKey();
                    Number value = bmEntry.getValue();
                    if ( value == null ) {
                        // no value for this factor, so this biomaterial cannot be ranked
                        log.debug( "No factor value for " + bm + " and " + ef + "." );
                        continue;
                    }
                    Integer batchIndex = batchIndexes.get( batchMembership.get( bm ) );
                    if ( batchIndex == null ) {
                        // no batch to rank it against
                        log.warn( "No batch membership for : " + bm );
                        continue;
                    }
                    factorValues.add( value.doubleValue() ); // ensures we only look at actually used factorvalues.
                    batches.add( batchIndex );
                    usedBatchesForFactor.add( batchIndex );
                }

                if ( factorValues.size() < numBioMaterials ) {
                    log.warn( ( numBioMaterials - factorValues.size() ) + "/" + numBioMaterials + " biomaterials lack a value for "
                            + ef + " or a batch assignment in " + ee + "; they are excluded from its batch confound test." );
                }

                if ( factorValues.size() < MIN_BIOMATERIALS_FOR_KRUSKAL_WALLIS || usedBatchesForFactor.size() < 2 ) {
                    log.warn( "Only " + factorValues.size() + " biomaterial(s) in " + usedBatchesForFactor.size()
                            + " batch(es) can be tested for confound with " + ef + " in " + ee + ", skipping it." );
                    continue; // to the next factor
                }

                p = KruskalWallis.test( factorValues, batches );
                df = KruskalWallis.dof( factorValues, batches );
                chiSquare = KruskalWallis.kwStatistic( factorValues, batches );

//                log.debug( "KWallis\t" + ee.getId() + "\t" + ee.getShortName() + "\t" + ef.getId() + "\t" + ef.getName()
//                        + "\t" + String.format( "%.2f", chiSquare ) + "\t" + df + "\t" + String.format( "%.2g", p )
//                        + "\t" + numBatches );
            } else {
                Map<Long, FactorValue> factorValueById = IdentifiableUtils.getIdMap( ef.getFactorValues() );

                // getBioMaterialFactorMap() pads every factor's map with a null for each biomaterial that
                // has no factor value (see the CONTINUOUS branch above). Skip those: a null counted as a
                // used factor value widens the contingency table below by one permanently-empty column.
                Set<FactorValue> usedFactorValues = new HashSet<>( bmToFv.size() );
                for ( Number val : bmToFv.values() ) {
                    if ( val == null ) {
                        continue;
                    }
                    usedFactorValues.add( factorValueById.get( ( Long ) val ) );
                }

                Map<FactorValue, Integer> factorValueToIndex = new HashMap<>();
                int index = 0;
                for ( FactorValue fv : ef.getFactorValues() ) {
                    // only use the used factorvalues
                    if ( !usedFactorValues.contains( fv ) ) {
                        continue;
                    }
                    factorValueToIndex.put( fv, index++ );
                }

                if ( factorValueToIndex.size() < 2 ) {
                    // This can happen if we're looking at a subset by this factor
                    continue; // to the next factor
                }

                Map<BioMaterial, FactorValue> factorValueMembership = new HashMap<>();

                for ( Map.Entry<BioMaterial, Number> bmEntry : bmToFv.entrySet() ) {
                    factorValueMembership.put( bmEntry.getKey(), factorValueById.get( ( Long ) bmEntry.getValue() ) );
                }

                // numbatches could still be incorrect, so we have to clean this up later. Width comes from
                // factorValueToIndex, the only map the counting loop below indexes through; sizing it from
                // usedFactorValues instead leaves a trailing all-zero column whenever the two disagree,
                // which defeats the finalCounts[0].length == 2 odds-ratio case and makes chi-square NaN.
                long[][] counts = new long[numBatches][factorValueToIndex.size()];

                for ( BioMaterial bm : bmToFv.keySet() ) {
                    FactorValue batch = batchMembership.get( bm );
                    if ( batch == null || !batchIndexes.containsKey( batch ) ) {
                        log.warn( "No batch membership for : " + bm );
                        continue;
                    }
                    int batchIndex = batchIndexes.get( batch );
                    FactorValue fv = factorValueMembership.get( bm );
                    if ( fv == null || !factorValueToIndex.containsKey( fv ) ) {
                        log.warn( "No factor value for : " + bm );
                        continue;
                    }
                    int factorIndex = factorValueToIndex.get( fv );
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
                    for ( long[] finalCount : finalCounts ) {
                        int numNonzero = 0;
                        int nonZeroIndex = -1;
                        for ( int c = 0; c < finalCounts[0].length; c++ ) {
                            if ( finalCount[c] != 0 ) {
                                numNonzero++;
                                nonZeroIndex = c;
                            }
                        }
                        // inspect the column
                        if ( numNonzero == 1 ) {
                            int numNonzeroColumnVals = 0;
                            for ( long[] count : finalCounts ) {
                                if ( count[nonZeroIndex] != 0 ) {
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
