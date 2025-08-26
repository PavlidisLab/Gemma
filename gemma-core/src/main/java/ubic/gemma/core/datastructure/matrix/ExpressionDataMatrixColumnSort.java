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
package ubic.gemma.core.datastructure.matrix;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.expression.diff.BaselineSelection;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.MeasurementUtils;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentFactorUtils;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Methods to organize ExpressionDataMatrices by column (or at least provide the ordering). This works by greedily
 * finding the factor with the fewest categories, sorting the samples by the factor values (measurements are treated as
 * numbers if possible), and recursively repeating this for each block, until there are no more factors with more than
 * one value.
 *
 * @author paul
 */
public class ExpressionDataMatrixColumnSort {

    private static final Log log = LogFactory.getLog( ExpressionDataMatrixColumnSort.class.getName() );

    public static <R> DoubleMatrix<R, BioAssay> orderByExperimentalDesign( DoubleMatrix<R, BioAssay> mat, @Nullable Collection<ExperimentalFactor> factors, @Nullable ExperimentalFactor primaryFactor ) {
        List<BioAssay> bioAssays = mat.getColNames();
        List<BioMaterial> start = new ArrayList<>();
        Map<BioMaterial, BioAssay> bm2ba = new HashMap<>();
        for ( BioAssay bioAssay : bioAssays ) {
            start.add( bioAssay.getSampleUsed() );
            bm2ba.put( bioAssay.getSampleUsed(), bioAssay );
        }
        List<BioMaterial> ordered = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( start, factors, primaryFactor );
        List<BioAssay> newBioAssayOrder = new ArrayList<>();
        for ( BioMaterial bioMaterial : ordered ) {
            assert bm2ba.containsKey( bioMaterial );
            newBioAssayOrder.add( bm2ba.get( bioMaterial ) );
        }
        return mat.subsetColumns( newBioAssayOrder );
    }

    public static List<BioMaterial> orderByExperimentalDesign( BulkExpressionDataMatrix<?> dmatrix, @Nullable Collection<ExperimentalFactor> factors, @Nullable ExperimentalFactor primaryFactor ) {
        List<BioMaterial> bms = new ArrayList<>();
        for ( int i = 0; i < dmatrix.columns(); i++ ) {
            bms.add( dmatrix.getBioMaterialForColumn( i ) );
        }
        return orderByExperimentalDesign( bms, factors, primaryFactor );
    }

    /**
     * @param start         biomaterials to sort
     * @param factors       factors to consider for ordering biomaterials or all of them if {@code null}
     * @param primaryFactor first factor to consider when ordering, auto-detected if {@code null}
     * @return sorted biomaterials
     */
    public static List<BioMaterial> orderByExperimentalDesign( List<BioMaterial> start, @Nullable Collection<ExperimentalFactor> factors, @Nullable ExperimentalFactor primaryFactor ) {
        if ( start.size() <= 1 ) {
            return start;
        }

        Collection<ExperimentalFactor> unsortedFactors;
        if ( factors != null && !factors.isEmpty() ) {
            unsortedFactors = factors;
        } else {
            unsortedFactors = ExpressionDataMatrixColumnSort.getFactors( start );
        }
        if ( unsortedFactors.isEmpty() ) {
            ExpressionDataMatrixColumnSort.log.debug( "No experimental design, sorting by sample name" );
            List<BioMaterial> ordered = new ArrayList<>( start );
            sortBioMaterials( ordered );
            return ordered;
        }

        // sort factors: which one do we want to sort by first
        List<ExperimentalFactor> sortedFactors = ExpressionDataMatrixColumnSort
                .orderFactorsByExperimentalDesign( start, unsortedFactors, primaryFactor );
        // sort biomaterials using sorted factors
        return orderBiomaterialsBySortedFactors( start, sortedFactors );
    }

    /**
     * @return list of factors, sorted from simplest (fewest number of values from the biomaterials passed in) to least
     * simple.
     */
    private static List<ExperimentalFactor> orderFactorsByExperimentalDesign( List<BioMaterial> start,
            Collection<ExperimentalFactor> factors, @Nullable ExperimentalFactor primaryFactor ) {
        Assert.isTrue( primaryFactor == null || factors.contains( primaryFactor ),
                "The primary factor must be in the provided list of factors." );

        if ( factors.isEmpty() ) {
            ExpressionDataMatrixColumnSort.log.warn( "No factors supplied for sorting." );
            return Collections.emptyList();
        }

        return factors.stream()
                .sorted( getSimplestFactorComparator( start, primaryFactor ) )
                .collect( Collectors.toList() );
    }

    /**
     * Choose the factor with the smallest number of categories. 'Batch' is a special case and is always considered
     * 'last'. Another special case is if a factor is continuous: it is returned first and aborts reordering by other
     * factors.
     *
     * @return null if no factor has at least 2 values represented, or the factor with the fewest number of values (at
     * least 2 values that is)
     */
    private static Comparator<ExperimentalFactor> getSimplestFactorComparator( Collection<BioMaterial> samples, @Nullable ExperimentalFactor primaryFactor ) {
        Set<FactorValue> usedFactorValues = samples.stream()
                .flatMap( bm -> bm.getAllFactorValues().stream() )
                .collect( Collectors.toSet() );

        Map<ExperimentalFactor, Integer> usedValues = new HashMap<>();
        for ( FactorValue fv : usedFactorValues ) {
            if ( usedValues.containsKey( fv.getExperimentalFactor() ) ) {
                usedValues.put( fv.getExperimentalFactor(), usedValues.get( fv.getExperimentalFactor() ) + 1 );
            } else {
                usedValues.put( fv.getExperimentalFactor(), 1 );
            }
        }

        return Comparator
                // primary factor should be first, regardless of any other consideration
                .comparing( ( ExperimentalFactor factor ) -> primaryFactor != null ? ( primaryFactor.equals( factor ) ? 0 : 1 ) : 0 )
                // factor needs to have at least one used value
                .thenComparing( factor -> usedValues.getOrDefault( factor, 0 ) > 1 ? 0 : 1 )
                // always push 'batch' down the list
                .thenComparing( f -> ExperimentFactorUtils.isBatchFactor( f ) ? 1 : 0 )
                // pick the factor with the least number of used values
                .thenComparingInt( f -> usedValues.getOrDefault( f, 0 ) )
                // prefer categorical factor over continuous ones
                .thenComparing( f -> f.getType() == FactorType.CATEGORICAL ? 0 : 1 );
    }

    private static Map<FactorValue, List<BioMaterial>> buildFv2BmMap( Collection<BioMaterial> bms ) {
        Map<FactorValue, List<BioMaterial>> fv2bms = new HashMap<>();
        for ( BioMaterial bm : bms ) {
            // boolean used = false;
            Collection<FactorValue> factorValues = bm.getAllFactorValues();
            for ( FactorValue fv : factorValues ) {
                if ( !fv2bms.containsKey( fv ) ) {
                    fv2bms.put( fv, new ArrayList<>() );
                }
                fv2bms.get( fv ).add( bm );
            }
        }
        for ( Entry<FactorValue, List<BioMaterial>> e : fv2bms.entrySet() ) {
            List<BioMaterial> biomaterials = e.getValue();
            ExpressionDataMatrixColumnSort.sortBioMaterials( biomaterials );
        }
        return fv2bms;
    }


    /**
     * Divide the biomaterials up into chunks based on the experimental factor given, keeping everybody in order. If the
     * factor is continuous, there is just one chunk.
     *
     * @return ordered map of fv->bm where fv is of ef, or null if it couldn't be done properly.
     */
    private static LinkedHashMap<FactorValue, List<BioMaterial>> chunkOnFactor( ExperimentalFactor ef,
            @Nullable List<BioMaterial> bms ) {

        if ( bms == null ) {
            return null;
        }

        LinkedHashMap<FactorValue, List<BioMaterial>> chunks = new LinkedHashMap<>();

        /*
         * Get the factor values in the order we have things right now
         */
        for ( BioMaterial bm : bms ) {
            for ( FactorValue fv : bm.getAllFactorValues() ) {
                if ( !ef.getFactorValues().contains( fv ) ) {
                    continue;
                }
                if ( chunks.containsKey( fv ) ) {
                    continue;
                }
                chunks.put( fv, new ArrayList<>() );
            }
        }

        /*
         * What if bm doesn't have a value for the factorvalue. Need a dummy value.
         */
        FactorValue dummy = FactorValue.Factory.newInstance( ef );
        dummy.setValue( "" );
        dummy.setId( -1L );
        chunks.put( dummy, new ArrayList<>() );

        for ( BioMaterial bm : bms ) {
            boolean found = false;
            for ( FactorValue fv : bm.getAllFactorValues() ) {
                if ( ef.getFactorValues().contains( fv ) ) {
                    found = true;
                    assert chunks.containsKey( fv );
                    chunks.get( fv ).add( bm );
                }
            }

            if ( !found ) {
                if ( ExpressionDataMatrixColumnSort.log.isDebugEnabled() )
                    ExpressionDataMatrixColumnSort.log
                            .debug( bm + " has no value for factor=" + ef + "; using dummy value" );
                chunks.get( dummy ).add( bm );
            }

        }

        if ( chunks.get( dummy ).isEmpty() ) {
            if ( ExpressionDataMatrixColumnSort.log.isDebugEnabled() )
                ExpressionDataMatrixColumnSort.log.debug( "removing dummy" );
            chunks.remove( dummy );
        }

        ExpressionDataMatrixColumnSort.log
                .debug( chunks.size() + " chunks for " + ef + ", from current chunk of size " + bms.size() );

        /*
         * Sanity check
         */
        int total = 0;
        for ( FactorValue fv : chunks.keySet() ) {
            List<BioMaterial> chunk = chunks.get( fv );
            total += chunk.size();
        }

        assert total == bms.size() : "expected " + bms.size() + ", got " + total;

        return chunks;
    }

    /**
     * Get all (non-constant) factors used by the passed biomaterials
     *
     * @param bms biomaterials
     * @return factors relevant to these biomaterials, ignoring those which have constant values.
     */
    private static Collection<ExperimentalFactor> getFactors( Collection<BioMaterial> bms ) {
        Map<ExperimentalFactor, Collection<FactorValue>> usedFactorValues = new HashMap<>();
        for ( BioMaterial bm : bms ) {
            Collection<FactorValue> factorValues = bm.getAllFactorValues();
            for ( FactorValue fv : factorValues ) {

                if ( fv.getCharacteristics().stream().map( Characteristic::getValue ).anyMatch( "DE_Exclude"::equalsIgnoreCase ) ) {
                    continue;
                }

                if ( !usedFactorValues.containsKey( fv.getExperimentalFactor() ) ) {
                    usedFactorValues.put( fv.getExperimentalFactor(), new HashSet<>() );
                }
                usedFactorValues.get( fv.getExperimentalFactor() ).add( fv );
            }
        }

        usedFactorValues.keySet().removeIf( ef -> usedFactorValues.get( ef ).size() < 2 );
        log.debug( usedFactorValues.size() + " factors retained " );
        return usedFactorValues.keySet();
    }

    /**
     * @param fv2bms map of factorValues to lists of biomaterials that have that factorValue.
     * @param bms    Chunk of biomaterials to organize.
     * @return ordered list, or null if there was a problem.
     */
    private static List<BioMaterial> orderByFactor( ExperimentalFactor ef, Map<FactorValue, List<BioMaterial>> fv2bms,
            List<BioMaterial> bms ) {

        if ( bms.size() <= 1 )
            return bms;

        ExpressionDataMatrixColumnSort.log.debug( "Ordering " + bms.size() + " biomaterials by " + ef );

        ExpressionDataMatrixColumnSort.sortBioMaterials( bms ); // probably redundant.

        List<FactorValue> factorValues = new ArrayList<>( ef.getFactorValues() );

        if ( factorValues.size() < 2 ) {
            /*
             * Not strictly disallowed, but useless.
             */
            return bms;
        }

        if ( ef.getType().equals( FactorType.CONTINUOUS ) ) {
            ExpressionDataMatrixColumnSort.sortByMeasurement( factorValues );
        } else {
            ExpressionDataMatrixColumnSort.sortByControl( factorValues );
        }

        LinkedHashMap<FactorValue, List<BioMaterial>> chunks = new LinkedHashMap<>();
        List<BioMaterial> organized = new ArrayList<>();

        ExpressionDataMatrixColumnSort.organizeByFactorValues( fv2bms, bms, factorValues, chunks, organized );

        if ( ExpressionDataMatrixColumnSort.log.isDebugEnabled() ) {
            for ( BioMaterial b : organized ) {
                for ( FactorValue f : b.getAllFactorValues() ) {
                    if ( f.getExperimentalFactor().equals( ef ) ) {
                        System.err.println( b.getId() + " " + f );
                    }
                }
            }
        }

        /*
         * At this point, the relevant part data set has been organized into chunks based on the current EF. Now we need
         * to sort each of those by any EFs they _refer to_.
         */
        if ( organized.size() != bms.size() ) {
            // fail gracefully.
            ExpressionDataMatrixColumnSort.log.warn( "Could not order by factor: " + ef + " Biomaterial count (" + bms.size()
                    + ") does not equal the size of the reorganized biomaterial list (" + organized.size()
                    + "). Check the experimental design for completeness/correctness" );
            return bms;
        }

        return organized;
    }

    /**
     * Sort biomaterials according to a list of ordered factors.
     * <p>
     * For categorical factors, we sort recursively (by levels of the first factor, then
     * within that by levels of the second factor etc.)
     * <p>
     * Any batch factor is used last (we sort by batch only within the most granular factor's levels)
     *
     * @param start   biomaterials to sort
     * @param factors sorted list of factors to define sort order for biomaterials, cannot be null
     */
    private static List<BioMaterial> orderBiomaterialsBySortedFactors( List<BioMaterial> start,
            List<ExperimentalFactor> factors ) {
        Assert.notNull( factors, "Must provide sorted factors, or at least an empty list." );

        if ( start.size() <= 1 ) {
            return start;
        }

        if ( factors.isEmpty() ) {
            // we're done.
            return start;
        }

        ExperimentalFactor simplest = factors.get( 0 );

        if ( simplest == null ) {
            // we're done.
            return start;
        }

        /*
         * Order this chunk by the selected factor
         */

        Map<FactorValue, List<BioMaterial>> fv2bms = ExpressionDataMatrixColumnSort.buildFv2BmMap( start );

        List<BioMaterial> ordered = ExpressionDataMatrixColumnSort.orderByFactor( simplest, fv2bms, start );

        LinkedList<ExperimentalFactor> factorsStillToDo = new LinkedList<>( factors );
        factorsStillToDo.remove( simplest );

        if ( factorsStillToDo.isEmpty() ) {
            // no more ordering is necessary.
            return ordered;
        }

        ExpressionDataMatrixColumnSort.log.debug( "Factors: " + factors.size() );

        /*
         * Recurse in and order each chunk. First split it up, but retaining the order we just made.
         */
        LinkedHashMap<FactorValue, List<BioMaterial>> chunks = ExpressionDataMatrixColumnSort
                .chunkOnFactor( simplest, ordered );

        if ( chunks == null ) {
            // this means we should bail, gracefully.
            return start;
        }

        /*
         * Process each chunk.
         */
        List<BioMaterial> result = new ArrayList<>();
        for ( FactorValue fv : chunks.keySet() ) {
            List<BioMaterial> chunk = chunks.get( fv );
            result.addAll( orderBiomaterialsBySortedFactors( chunk, factorsStillToDo ) );
        }

        return result;

    }

    /**
     * Organized the results by the factor values (for one factor)
     *
     * @param fv2bms           master map
     * @param bioMaterialChunk biomaterials to organize
     * @param factorValues     factor value to consider - biomaterials will be organized in the order given
     * @param chunks           map of factor values to chunks goes here
     * @param organized        the results go here
     */
    private static void organizeByFactorValues( Map<FactorValue, List<BioMaterial>> fv2bms,
            List<BioMaterial> bioMaterialChunk, List<FactorValue> factorValues,
            LinkedHashMap<FactorValue, List<BioMaterial>> chunks, List<BioMaterial> organized ) {
        Collection<BioMaterial> seenBioMaterials = new HashSet<>();
        for ( FactorValue fv : factorValues ) {

            if ( !fv2bms.containsKey( fv ) ) {
                /*
                 * This can happen if a factorvalue has been created but not yet associated with any biomaterials. This
                 * can also be cruft.
                 */
                continue;
            }

            // all in entire experiment, so we might not want them all as we may just be processing a small chunk.
            List<BioMaterial> bioMsForFv = fv2bms.get( fv );

            for ( BioMaterial bioMaterial : bioMsForFv ) {
                if ( bioMaterialChunk.contains( bioMaterial ) ) {
                    if ( !chunks.containsKey( fv ) ) {
                        chunks.put( fv, new ArrayList<>() );
                    }
                    if ( !chunks.get( fv ).contains( bioMaterial ) ) {
                        /*
                         * shouldn't be twice, but ya never know.
                         */
                        chunks.get( fv ).add( bioMaterial );
                    }
                }
                seenBioMaterials.add( bioMaterial );
            }

            // If we used that fv ...
            if ( chunks.containsKey( fv ) ) {
                organized.addAll( chunks.get( fv ) ); // now at least this is in order of this factor
            }
        }

        // Leftovers contains biomaterials which have no factorvalue assigned for this factor.
        Collection<BioMaterial> leftovers = new HashSet<>();
        for ( BioMaterial bm : bioMaterialChunk ) {
            if ( !seenBioMaterials.contains( bm ) ) {
                leftovers.add( bm );
            }
        }

        if ( !leftovers.isEmpty() ) {
            organized.addAll( leftovers );
            chunks.put( null, new ArrayList<>( leftovers ) );
        }

    }

    /**
     * Organize by id, because the order we got the samples in the first place is a reasonable fallback.
     */
    private static void sortBioMaterials( List<BioMaterial> biomaterials ) {
        biomaterials.sort( Comparator.comparing( BioMaterial::getName ) );
    }

    /**
     * Put control factor values first.
     */
    private static void sortByControl( List<FactorValue> factorValues ) {
        factorValues.sort( ( o1, o2 ) -> {
            if ( BaselineSelection.isBaselineCondition( o1 ) ) {
                if ( o2.getIsBaseline() == null ) {
                    return -1;
                } else if ( BaselineSelection.isBaselineCondition( o2 ) ) {
                    return 0;
                }
                return -1;
            } else if ( BaselineSelection.isBaselineCondition( o2 ) ) {
                return 1;
            }
            return 0;
        } );

    }

    /**
     * Sort the factor values by measurement values.
     * <p>
     * NAs are sorted to the end.
     */
    private static void sortByMeasurement( List<FactorValue> factorValues ) {
        ExpressionDataMatrixColumnSort.log.debug( "Sorting measurements" );
        factorValues.sort( Comparator.comparing( FactorValue::getMeasurement, Comparator.nullsLast( Comparator.comparingDouble( MeasurementUtils::measurement2double ) ) ) );
    }
}
