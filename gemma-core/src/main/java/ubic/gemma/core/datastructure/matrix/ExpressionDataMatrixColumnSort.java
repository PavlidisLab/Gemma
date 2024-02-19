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
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.expression.diff.BaselineSelection;
import ubic.gemma.core.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.util.*;
import java.util.Map.Entry;

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

    /**
     * Identify the FactorValue that should be treated as 'Baseline' for each of the given factors. This is done
     * heuristically, and if all else fails we choose arbitrarily.
     *
     * @param factors factors
     * @return map
     */
    public static Map<ExperimentalFactor, FactorValue> getBaselineLevels( Collection<ExperimentalFactor> factors ) {
        return ExpressionDataMatrixColumnSort.getBaselineLevels( null, factors );
    }

    /**
     * Identify the FactorValue that should be treated as 'Baseline' for each of the given factors. This is done
     * heuristically, and if all else fails we choose arbitrarily. For continuous factors, the minimum value is treated
     * as baseline.
     *
     * @param samplesUsed These are used to make sure we don't bother using factor values as baselines if they are not
     *                    used by any of the samples. This is important for subsets. If null, this is ignored.
     * @param factors     factors
     * @return map of factors to the baseline factorvalue for that factor.
     */
    public static Map<ExperimentalFactor, FactorValue> getBaselineLevels( List<BioMaterial> samplesUsed,
            Collection<ExperimentalFactor> factors ) {

        Map<ExperimentalFactor, FactorValue> result = new HashMap<>();

        for ( ExperimentalFactor factor : factors ) {

            if ( factor.getFactorValues().isEmpty() ) {
                throw new IllegalStateException( "Factor has no factor values: " + factor );
            }

            if ( ExperimentalDesignUtils.isContinuous( factor ) ) {
                // then there is no baseline, but we'll take the minimum value.
                TreeMap<Double, FactorValue> sortedVals = new TreeMap<>();
                for ( FactorValue fv : factor.getFactorValues() ) {

                    /*
                     * Check that this factor value is used by at least one of the given samples. Only matters if this
                     * is a subset of the full data set.
                     */
                    if ( samplesUsed != null && !ExpressionDataMatrixColumnSort.used( fv, samplesUsed ) ) {
                        // this factorValue cannot be a candidate baseline for this subset.
                        continue;
                    }

                    if ( fv.getMeasurement() == null ) {
                        throw new IllegalStateException( "Continuous factors should have Measurements as values" );
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
                    if ( samplesUsed != null && !ExpressionDataMatrixColumnSort.used( fv, samplesUsed ) ) {
                        // this factorValue cannot be a candidate baseline for this subset.
                        continue;
                    }

                    if ( BaselineSelection.isForcedBaseline( fv ) ) {
                        ExpressionDataMatrixColumnSort.log.debug( "Baseline chosen: " + fv );
                        result.put( factor, fv );
                        break;
                    }

                    if ( BaselineSelection.isBaselineCondition( fv ) ) {
                        if ( result.containsKey( factor ) ) {
                            ExpressionDataMatrixColumnSort.log
                                    .warn( "A second potential baseline was found for " + factor + ": " + fv );
                            continue;
                        }
                        ExpressionDataMatrixColumnSort.log.debug( "Baseline chosen: " + fv );
                        result.put( factor, fv );
                    }
                }

                if ( !result.containsKey( factor ) ) { // fallback
                    FactorValue arbitraryBaselineFV = null;

                    if ( samplesUsed != null ) {
                        // make sure we choose a fv that is actually used (see above for non-arbitrary case)
                        for ( FactorValue fv : factor.getFactorValues() ) {
                            for ( BioMaterial bm : samplesUsed ) {
                                for ( FactorValue bfv : bm.getFactorValues() ) {
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
                    if ( !ExperimentalDesignUtils.isBatch( factor ) ) {
                        ExpressionDataMatrixColumnSort.log
                                .info( "Falling back on choosing baseline arbitrarily: " + arbitraryBaselineFV );
                    }
                    result.put( factor, arbitraryBaselineFV );
                }
            }
        }

        return result;

    }

    public static <R> DoubleMatrix<R, BioAssay> orderByExperimentalDesign( DoubleMatrix<R, BioAssay> mat ) {

        List<BioAssay> bioAssays = mat.getColNames();

        List<BioMaterial> start = new ArrayList<>();
        Map<BioMaterial, BioAssay> bm2ba = new HashMap<>();
        for ( BioAssay bioAssay : bioAssays ) {
            start.add( bioAssay.getSampleUsed() );
            bm2ba.put( bioAssay.getSampleUsed(), bioAssay );
        }
        List<BioMaterial> bm = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( start, null );
        List<BioAssay> newBioAssayOrder = new ArrayList<>();
        for ( BioMaterial bioMaterial : bm ) {
            assert bm2ba.containsKey( bioMaterial );
            newBioAssayOrder.add( bm2ba.get( bioMaterial ) );
        }
        return mat.subsetColumns( newBioAssayOrder );
    }

    /**
     * @param mat matrix
     * @return bio materials
     */
    public static List<BioMaterial> orderByExperimentalDesign( BulkExpressionDataMatrix<?> mat ) {
        List<BioMaterial> start = ExpressionDataMatrixColumnSort.getBms( mat );

        List<BioMaterial> ordered = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( start, null );

        assert ordered.size() == start.size() : "Expected " + start.size() + ", got " + ordered.size();

        return ordered;

    }

    /**
     * @param factors, can be null
     * @param start    start
     * @return bio materials
     */
    public static List<BioMaterial> orderByExperimentalDesign( List<BioMaterial> start,
            Collection<ExperimentalFactor> factors ) {

        if ( start.size() == 1 ) {
            return start;
        }

        if ( start.size() == 0 ) {
            throw new IllegalArgumentException( "Must provide some biomaterials" );
        }

        Collection<ExperimentalFactor> unsortedFactors;
        if ( factors != null ) {
            unsortedFactors = factors;
        } else {
            unsortedFactors = ExpressionDataMatrixColumnSort.getFactors( start );
        }
        if ( unsortedFactors.size() == 0 ) {
            ExpressionDataMatrixColumnSort.log.debug( "No experimental design, sorting by sample name" );
            ExpressionDataMatrixColumnSort.orderByName( start );
            return start;
        }

        // sort factors: which one do we want to sort by first
        List<ExperimentalFactor> sortedFactors = ExpressionDataMatrixColumnSort
                .orderFactorsByExperimentalDesign( start, unsortedFactors );
        // sort biomaterials using sorted factors
        return ExpressionDataMatrixColumnSort.orderBiomaterialsBySortedFactors( start, sortedFactors );
    }

    private static void orderByName( List<BioMaterial> start ) {
        Collections.sort( start, new Comparator<BioMaterial>() {
            @Override
            public int compare( BioMaterial o1, BioMaterial o2 ) {

                if ( o1.getBioAssaysUsedIn().isEmpty() || o2.getBioAssaysUsedIn().isEmpty() )
                    return o1.getName().compareTo( o2.getName() );

                BioAssay ba1 = o1.getBioAssaysUsedIn().iterator().next();
                BioAssay ba2 = o2.getBioAssaysUsedIn().iterator().next();
                if ( ba1.getName() != null && ba2.getName() != null ) {
                    return ba1.getName().compareTo( ba2.getName() );
                }
                if ( o1.getName() != null && o2.getName() != null ) {
                    return o1.getName().compareTo( o2.getName() );
                }
                return 0;

            }
        } );
    }

    /**
     * @return list of factors, sorted from simplest (fewest number of values from the biomaterials passed in) to least
     * simple. Continuous factors will always be first, and batch factors last.
     */
    private static List<ExperimentalFactor> orderFactorsByExperimentalDesign( List<BioMaterial> start,
            Collection<ExperimentalFactor> factors ) {

        if ( factors == null || factors.isEmpty() ) {
            ExpressionDataMatrixColumnSort.log.warn( "No factors supplied for sorting" );
            return new LinkedList<>();
        }

        LinkedList<ExperimentalFactor> sortedFactors = new LinkedList<>();
        Collection<ExperimentalFactor> factorsToTake = new HashSet<>( factors );
        while ( !factorsToTake.isEmpty() ) {
            ExperimentalFactor simplest = ExpressionDataMatrixColumnSort.chooseSimplestFactor( start, factorsToTake );
            if ( simplest == null ) {
                // none of the factors have more than one factor value. One-sided t-tests ...

                /*
                 * This assertion isn't right -- we now allow this, though we can only have ONE such constant factor.
                 * See bug 2390. Unless we are dealing with a subset, in which case there can be any number of constant
                 * factors within the subset.
                 */
                // assert factors.size() == 1 :
                // "It's possible to have just one factor value, but only if there is only one factor.";

                sortedFactors.addAll( factors );
                return sortedFactors;
            }
            sortedFactors.addLast( simplest );

            factorsToTake.remove( simplest );
        }

        return sortedFactors;
    }

    private static Map<FactorValue, List<BioMaterial>> buildFv2BmMap( Collection<BioMaterial> bms ) {
        Map<FactorValue, List<BioMaterial>> fv2bms = new HashMap<>();

        FactorValue dummy = FactorValue.Factory.newInstance();
        dummy.setId( -1L );

        for ( BioMaterial bm : bms ) {
            // boolean used = false;
            Collection<FactorValue> factorValues = bm.getFactorValues();
            for ( FactorValue fv : factorValues ) {
                if ( !fv2bms.containsKey( fv ) ) {
                    fv2bms.put( fv, new ArrayList<BioMaterial>() );
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
     * Choose the factor with the smallest number of categories. 'Batch' is a special case and is always considered
     * 'last'. Another special case is if a factor is continuous: it is returned first and aborts reordering by other
     * factors.
     *
     * @return null if no factor has at least 2 values represented, or the factor with the fewest number of values (at
     * least 2 values that is)
     */
    private static ExperimentalFactor chooseSimplestFactor( List<BioMaterial> bms,
            Collection<ExperimentalFactor> factors ) {

        ExperimentalFactor simplest = null;
        int smallestSize = Integer.MAX_VALUE;

        Collection<FactorValue> usedValues = new HashSet<>();
        for ( BioMaterial bm : bms ) {
            usedValues.addAll( bm.getFactorValues() );
        }

        for ( ExperimentalFactor ef : factors ) {

            if ( ExperimentalDesignUtils.isContinuous( ef ) ) {
                return ef;
            }

            /*
             * Always push 'batch' down the list
             */
            if ( factors.size() > 1 && ExperimentalDesignUtils.isBatch( ef ) ) {
                continue;
            }

            int numvals = 0;
            for ( FactorValue fv : ef.getFactorValues() ) {
                if ( usedValues.contains( fv ) ) {
                    numvals++;
                }
            }

            if ( numvals > 1 && numvals < smallestSize ) {
                smallestSize = numvals;
                simplest = ef;
            }
        }
        return simplest;
    }

    /**
     * Divide the biomaterials up into chunks based on the experimental factor given, keeping everybody in order. If the
     * factor is continuous, there is just one chunk.
     *
     * @return ordered map of fv->bm where fv is of ef, or null if it couldn't be done properly.
     */
    private static LinkedHashMap<FactorValue, List<BioMaterial>> chunkOnFactor( ExperimentalFactor ef,
            List<BioMaterial> bms ) {

        if ( bms == null ) {
            return null;
        }

        LinkedHashMap<FactorValue, List<BioMaterial>> chunks = new LinkedHashMap<>();

        /*
         * Get the factor values in the order we have things right now
         */
        for ( BioMaterial bm : bms ) {
            for ( FactorValue fv : bm.getFactorValues() ) {
                if ( !ef.getFactorValues().contains( fv ) ) {
                    continue;
                }
                if ( chunks.keySet().contains( fv ) ) {
                    continue;
                }
                chunks.put( fv, new ArrayList<BioMaterial>() );
            }
        }

        /*
         * What if bm doesn't have a value for the factorvalue. Need a dummy value.
         */
        FactorValue dummy = FactorValue.Factory.newInstance( ef );
        dummy.setValue( "" );
        dummy.setId( -1L );
        chunks.put( dummy, new ArrayList<BioMaterial>() );

        for ( BioMaterial bm : bms ) {
            boolean found = false;
            for ( FactorValue fv : bm.getFactorValues() ) {
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

        if ( chunks.get( dummy ).size() == 0 ) {
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
     * Get all biomaterials for a matrix.
     */
    private static List<BioMaterial> getBms( BulkExpressionDataMatrix<?> mat ) {
        List<BioMaterial> result = new ArrayList<>();
        for ( int i = 0; i < mat.columns(); i++ ) {
            result.add( mat.getBioMaterialForColumn( i ) );
        }
        return result;
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
            Collection<FactorValue> factorValues = bm.getFactorValues();
            for ( FactorValue fv : factorValues ) {
                if ( !usedFactorValues.containsKey( fv.getExperimentalFactor() ) ) {
                    usedFactorValues.put( fv.getExperimentalFactor(), new HashSet<>() );
                }
                usedFactorValues.get( fv.getExperimentalFactor() ).add( fv );
            }
        }

        for ( java.util.Iterator<ExperimentalFactor> ei = usedFactorValues.keySet().iterator(); ei.hasNext(); ) {
            ExperimentalFactor ef = ei.next();
            if ( usedFactorValues.get( ef ).size() < 2 ) {
                ei.remove();
            }
        }
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

        if ( bms.size() == 1 )
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

        if ( !ExperimentalDesignUtils.isContinuous( ef ) ) {
            ExpressionDataMatrixColumnSort.sortByControl( factorValues );
        } else {
            ExpressionDataMatrixColumnSort.sortIfMeasurement( factorValues );
        }

        LinkedHashMap<FactorValue, List<BioMaterial>> chunks = new LinkedHashMap<>();
        List<BioMaterial> organized = new ArrayList<>();

        ExpressionDataMatrixColumnSort.organizeByFactorValues( fv2bms, bms, factorValues, chunks, organized );

        if ( ExpressionDataMatrixColumnSort.log.isDebugEnabled() ) {
            for ( BioMaterial b : organized ) {
                for ( FactorValue f : b.getFactorValues() ) {
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
            ExpressionDataMatrixColumnSort.log
                    .error( "Could not order by factor: " + ef + " Biomaterial count (" + bms.size()
                            + ") does not equal the size of the reorganized biomaterial list (" + organized.size()
                            + "). Check the experimental design for completeness/correctness" );
            return bms;
        }

        return organized;
    }

    /**
     * <p>
     * Sort biomaterials according to a list of ordered factors.
     * </p>
     * <p>
     * If any factor is continuous, we sort by it and don't do any further sorting.
     * </p><p>
     * Otherwise, for categorical factors, we sort recursively (by levels of the first factor, then
     * within that by levels of the second factor etc.)
     * </p><p>
     * Any batch factor is used last (we sort by batch only within the most granular factor's levels)
     * </p>
     *
     * @param start   biomaterials to sort
     * @param factors sorted list of factors to define sort order for biomaterials, cannot be null
     */
    private static List<BioMaterial> orderBiomaterialsBySortedFactors( List<BioMaterial> start,
            List<ExperimentalFactor> factors ) {

        if ( start.size() == 1 ) {
            return start;
        }

        if ( start.size() == 0 ) {
            throw new IllegalArgumentException( "Must provide some biomaterials" );
        }
        if ( factors == null ) {
            throw new IllegalArgumentException( "Must provide sorted factors, or at least an empty list" );
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

        // Abort ordering, so we are ordered only by the first continuous factor.
        if ( ExperimentalDesignUtils.isContinuous( simplest ) ) {
            assert ordered != null;
            return ordered;
        }

        LinkedList<ExperimentalFactor> factorsStillToDo = new LinkedList<>();
        factorsStillToDo.addAll( factors );
        factorsStillToDo.remove( simplest );

        if ( factorsStillToDo.size() == 0 ) {
            /*
             * No more ordering is necessary.
             */
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

            if ( chunk.size() < 2 ) {
                result.addAll( chunk );
            } else {
                List<BioMaterial> orderedChunk = ExpressionDataMatrixColumnSort
                        .orderBiomaterialsBySortedFactors( chunk, factorsStillToDo );
                if ( orderedChunk != null ) {
                    result.addAll( orderedChunk );
                }
            }
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
                        chunks.put( fv, new ArrayList<BioMaterial>() );
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

        if ( leftovers.size() > 0 ) {
            organized.addAll( leftovers );
            chunks.put( null, new ArrayList<>( leftovers ) );
        }

    }

    /**
     * Organize by id, because the order we got the samples in the first place is a reasonable fallback.
     */
    private static void sortBioMaterials( List<BioMaterial> biomaterials ) {
        Collections.sort( biomaterials, new Comparator<BioMaterial>() {
            @Override
            public int compare( BioMaterial o1, BioMaterial o2 ) {
                return o1.getId().compareTo( o2.getId() );
            }
        } );
    }

    /**
     * Put control factor values first.
     */
    private static void sortByControl( List<FactorValue> factorValues ) {
        Collections.sort( factorValues, new Comparator<FactorValue>() {
            @Override
            public int compare( FactorValue o1, FactorValue o2 ) {
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
            }
        } );

    }

    /**
     * Sort the factor values by measurement values.
     */
    private static void sortIfMeasurement( List<FactorValue> factorValues ) {
        ExpressionDataMatrixColumnSort.log.debug( "Sorting measurements" );
        Collections.sort( factorValues, new Comparator<FactorValue>() {
            @Override
            public int compare( FactorValue o1, FactorValue o2 ) {
                try {
                    assert o1.getMeasurement() != null;
                    assert o2.getMeasurement() != null;

                    double d1 = Double.parseDouble( o1.getMeasurement().getValue() );
                    double d2 = Double.parseDouble( o2.getMeasurement().getValue() );
                    if ( d1 < d2 )
                        return -1;
                    else if ( d1 > d2 )
                        return 1;
                    return 0;

                } catch ( NumberFormatException e ) {
                    return o1.getMeasurement().getValue().compareTo( o2.getMeasurement().getValue() );
                }
            }
        } );
    }

    /**
     * @return true if the factorvalue is used by at least one of the samples.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // Better semantics
    private static boolean used( FactorValue fv, List<BioMaterial> samplesUsed ) {
        for ( BioMaterial bm : samplesUsed ) {
            for ( FactorValue bfv : bm.getFactorValues() ) {
                if ( fv.equals( bfv ) ) {
                    return true;
                }
            }
        }
        return false;
    }

}
