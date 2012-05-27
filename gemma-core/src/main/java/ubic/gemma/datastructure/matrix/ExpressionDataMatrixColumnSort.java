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
package ubic.gemma.datastructure.matrix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Methods to organize ExpressionDataMatrices by column (or at least provide the ordering). This works by greedily
 * finding the factor with the fewest categories, sorting the samples by the factor values (measurements are treated as
 * numbers if possible), and recursively repeating this for each block, until there are no more factors with more than
 * one value.
 * 
 * @author paul
 * @version $Id$
 */
public class ExpressionDataMatrixColumnSort {
    private static Collection<String> controlGroupTerms = new HashSet<String>();

    private static Log log = LogFactory.getLog( ExpressionDataMatrixColumnSort.class.getName() );

    static {
        /*
         * Values or ontology terms we treat as 'baseline'.
         */
        controlGroupTerms.add( "control group" );
        controlGroupTerms.add( "control" );
        controlGroupTerms.add( "untreated" );
        controlGroupTerms.add( "baseline" );
        controlGroupTerms.add( "control_group" );
        controlGroupTerms.add( "wild_type" );
        controlGroupTerms.add( "wild type" );
        controlGroupTerms.add( "http://purl.obolibrary.org/obo/OBI_0100046".toLowerCase() ); // phosphate buffered
                                                                                             // saline.
        controlGroupTerms.add( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#wild_type".toLowerCase() );
        controlGroupTerms.add( "http://purl.org/nbirn/birnlex/ontology/BIRNLex-Investigation.owl#birnlex_2201"
                .toLowerCase() ); // control_group, old.
        controlGroupTerms.add( "http://ontology.neuinfo.org/NIF/DigitalEntities/NIF-Investigation.owl#birnlex_2201"
                .toLowerCase() ); // control_group,
        // new
        // version.

    }

    /**
     * For each factor, try to figure out which one should be treated as baseline using some heuristics. If more than
     * one is viable, only the first one encountered will be marked as baseline.
     * 
     * @param factors
     * @return
     */
    public static Map<ExperimentalFactor, FactorValue> getBaselineLevels( Collection<ExperimentalFactor> factors ) {

        Map<ExperimentalFactor, FactorValue> result = new HashMap<ExperimentalFactor, FactorValue>();

        for ( ExperimentalFactor factor : factors ) {
            for ( FactorValue fv : factor.getFactorValues() ) {
                if ( isBaselineCondition( fv ) ) {
                    if ( result.containsKey( factor ) ) {
                        log.warn( "A second potential baseline was found for " + factor + ": " + fv );
                        continue;
                    }
                    result.put( factor, fv );
                    continue;
                }
            }
        }

        return result;

    }

    /**
     * @param mat
     * @return
     */
    public static <R> DoubleMatrix<R, BioAssay> orderByExperimentalDesign( DoubleMatrix<R, BioAssay> mat ) {

        List<BioAssay> bioAssays = mat.getColNames();

        List<BioMaterial> start = new ArrayList<BioMaterial>();
        Map<BioMaterial, BioAssay> bm2ba = new HashMap<BioMaterial, BioAssay>();
        for ( BioAssay bioAssay : bioAssays ) {
            start.add( bioAssay.getSamplesUsed().iterator().next() );
            bm2ba.put( bioAssay.getSamplesUsed().iterator().next(), bioAssay );
        }
        List<BioMaterial> bm = orderByExperimentalDesign( start, null );
        List<BioAssay> newBioAssayOrder = new ArrayList<BioAssay>();
        for ( BioMaterial bioMaterial : bm ) {
            assert bm2ba.containsKey( bioMaterial );
            newBioAssayOrder.add( bm2ba.get( bioMaterial ) );
        }
        return mat.subsetColumns( newBioAssayOrder );
    }

    /**
     * @param mat
     * @return sorted by experimental design, if possible, or by name if no design exists.
     */
    public static List<BioMaterial> orderByExperimentalDesign( ExpressionDataMatrix<?> mat ) {
        List<BioMaterial> start = getBms( mat );

        List<BioMaterial> ordered = orderByExperimentalDesign( start, null );

        // log.info( "AFTER" );
        assert ordered.size() == start.size() : "Expected " + start.size() + ", got " + ordered.size();
        // StringBuilder buf2 = new StringBuilder();
        //
        // Collection<ExperimentalFactor> efs = getFactors( ordered );
        //
        // for ( BioMaterial bioMaterial : ordered ) {
        // buf2.append( StringUtils.leftPad( bioMaterial.getId().toString(), 3 ) + "  " );
        //
        // for ( ExperimentalFactor ef : efs ) {
        // for ( FactorValue fv : bioMaterial.getFactorValues() ) {
        // if ( fv.getExperimentalFactor().equals( ef ) ) buf2.append( fv + "\t" );
        // }
        // }
        //
        // buf2.append( "\n" );
        // }
        // log.info( buf2.toString() );

        return ordered;

    }

    /**
     * @param start
     * @param factors
     * @return
     */
    public static List<BioMaterial> orderByExperimentalDesign( List<BioMaterial> start,
            Collection<ExperimentalFactor> factors ) {

        if ( start.size() == 1 ) {
            return start;
        }

        if ( start.size() == 0 ) {
            throw new IllegalArgumentException( "Must provide some biomaterials" );
        }

        Collection<ExperimentalFactor> unsortedFactors = null;
        if ( factors != null ) {
            unsortedFactors = factors;
        } else {
            unsortedFactors = getFactors( start );
        }
        if ( unsortedFactors.size() == 0 ) {
            log.warn( "No experimental design, sorting by sample name" );
            orderByName( start );
            return start;
        }

        // breaking this up because some methods need to know the order of factors that was used to sort the
        // biomaterials
        // difficult to know this if >1 factor has same number of values

        // sort factors
        List<ExperimentalFactor> sortedFactors = orderFactorsByExperimentalDesign( start, unsortedFactors );
        // sort biomaterials using sorted factors
        return orderBiomaterialsBySortedFactors( start, sortedFactors );
    }

    /**
     * @param start
     * @param factorsToUse
     * @return list of factors, sorted from simplest (fewest number of values from the biomaterials passed in) to least
     *         simple
     */
    public static List<ExperimentalFactor> orderFactorsByExperimentalDesign( List<BioMaterial> start,
            Collection<ExperimentalFactor> factors ) {

        if ( factors == null || factors.isEmpty() ) {
            log.warn( "No factors supplied for sorting" );
            return new LinkedList<ExperimentalFactor>();
        }

        LinkedList<ExperimentalFactor> sortedFactors = new LinkedList<ExperimentalFactor>();
        Collection<ExperimentalFactor> factorsToTake = new HashSet<ExperimentalFactor>( factors );
        while ( !factorsToTake.isEmpty() ) {
            ExperimentalFactor simplest = chooseSimplestFactor( start, factorsToTake );
            if ( simplest == null ) {
                // none of the factors have more than one factor value. One-sided t-tests ...

                /*
                 * This assertion isn't right -- we now allow this, though we can only have ONE such constant factor.
                 * See bug 2390.
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

    /**
     * Sort biomaterials according to a list of ordered factors
     * 
     * @param start biomaterials to sort
     * @param factorsToUse sorted list of factors to define sort order for biomaterials, cannot be null
     * @return
     */
    public static List<BioMaterial> orderBiomaterialsBySortedFactors( List<BioMaterial> start,
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

        Map<FactorValue, List<BioMaterial>> fv2bms = buildFv2BmMap( start );
        List<BioMaterial> ordered = orderByFactor( simplest, fv2bms, start,
                new HashMap<ExperimentalFactor, Collection<BioMaterial>>() );

        LinkedList<ExperimentalFactor> factorsStillToDo = new LinkedList<ExperimentalFactor>();
        factorsStillToDo.addAll( factors );
        factorsStillToDo.remove( simplest );

        if ( factorsStillToDo.size() == 0 ) {
            /*
             * No more ordering is necessary.
             */
            return ordered;
        }

        log.debug( "Factors: " + factors.size() );

        /*
         * Recurse in and order each chunk. First split it up, but retaining the order we just made.
         */
        LinkedHashMap<FactorValue, List<BioMaterial>> chunks = chunkOnFactor( simplest, ordered );

        if ( chunks == null ) {
            // this means we should bail, gracefully.
            return start;
        }

        /*
         * Process each chunk.
         */
        List<BioMaterial> result = new ArrayList<BioMaterial>();
        for ( FactorValue fv : chunks.keySet() ) {
            List<BioMaterial> chunk = chunks.get( fv );

            if ( chunk.size() < 2 ) {
                result.addAll( chunk );
            } else {
                List<BioMaterial> orderedChunk = orderBiomaterialsBySortedFactors( chunk, factorsStillToDo );
                result.addAll( orderedChunk );
            }
        }

        return result;

    }

    /**
     * @param mat
     * @return
     */
    public static List<BioMaterial> orderByName( ExpressionDataMatrix<?> mat ) {
        List<BioMaterial> start = getBms( mat );
        orderByName( start );
        return start;
    }

    /**
     * @param start
     */
    public static void orderByName( List<BioMaterial> start ) {
        Collections.sort( start, new Comparator<BioMaterial>() {
            @Override
            public int compare( BioMaterial o1, BioMaterial o2 ) {

                if ( o1.getBioAssaysUsedIn().isEmpty() || o2.getBioAssaysUsedIn().isEmpty() )
                    return o1.getName().compareTo( o1.getName() );

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
     * @param bms
     * @return
     */
    private static Map<FactorValue, List<BioMaterial>> buildFv2BmMap( Collection<BioMaterial> bms ) {
        Map<FactorValue, List<BioMaterial>> fv2bms = new HashMap<FactorValue, List<BioMaterial>>();

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
                // used = true;
            }
            //
            // /*
            // * Handle case where biomaterial does not have any factor values.
            // */
            // if ( !used ) {
            // log.info( "No factorValues for " + bm );
            // if ( !fv2bms.containsKey( dummy ) ) {
            // fv2bms.put( dummy, new ArrayList<BioMaterial>() );
            // }
            // fv2bms.get( dummy ).add( bm );
            // }
        }

        for ( Entry<FactorValue, List<BioMaterial>> e : fv2bms.entrySet() ) {
            List<BioMaterial> biomaterials = e.getValue();
            sortBioMaterials( biomaterials );
        }

        return fv2bms;
    }

    /**
     * Choose the factor with the smallest number of categories
     * 
     * @param bms
     * @param factors
     * @return null if no factor has at least 2 values represented, or the factor with the fewest number of values (at
     *         least 2 values that is)
     */
    private static ExperimentalFactor chooseSimplestFactor( List<BioMaterial> bms,
            Collection<ExperimentalFactor> factors ) {

        ExperimentalFactor simplest = null;
        int smallestSize = Integer.MAX_VALUE;

        Collection<FactorValue> usedValues = new HashSet<FactorValue>();
        for ( BioMaterial bm : bms ) {
            usedValues.addAll( bm.getFactorValues() );
        }

        for ( ExperimentalFactor ef : factors ) {

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
     * Divide the biomaterials up into chunks based on the experimental factor given, keeping everybody in order.
     * 
     * @param ef
     * @param bms
     * @return ordered map of fv->bm where fv is of ef, or null if it couldn't be done properly.
     */
    private static LinkedHashMap<FactorValue, List<BioMaterial>> chunkOnFactor( ExperimentalFactor ef,
            List<BioMaterial> bms ) {

        if ( bms == null ) {
            return null;
        }

        LinkedHashMap<FactorValue, List<BioMaterial>> chunks = new LinkedHashMap<FactorValue, List<BioMaterial>>();

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
                if ( log.isDebugEnabled() ) log.debug( bm + " has no value for factor=" + ef + "; using dummy value" );
                chunks.get( dummy ).add( bm );
            }

        }

        if ( chunks.get( dummy ).size() == 0 ) {
            if ( log.isDebugEnabled() ) log.debug( "removing dummy" );
            chunks.remove( dummy );
        }

        log.debug( chunks.size() + " chunks for " + ef + ", from current chunk of size " + bms.size() );

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
     * 
     * @param mat
     * @return
     */
    private static List<BioMaterial> getBms( ExpressionDataMatrix<?> mat ) {
        List<BioMaterial> result = new ArrayList<BioMaterial>();
        for ( int i = 0; i < mat.columns(); i++ ) {
            result.add( mat.getBioMaterialForColumn( i ) );
        }
        return result;
    }

    /**
     * @param bms
     * @return
     */
    private static Collection<ExperimentalFactor> getFactors( Collection<BioMaterial> bms ) {
        Collection<ExperimentalFactor> efs = new HashSet<ExperimentalFactor>();
        for ( BioMaterial bm : bms ) {
            Collection<FactorValue> factorValues = bm.getFactorValues();
            for ( FactorValue fv : factorValues ) {
                efs.add( fv.getExperimentalFactor() );
            }
        }

        for ( java.util.Iterator<ExperimentalFactor> ei = efs.iterator(); ei.hasNext(); ) {
            ExperimentalFactor ef = ei.next();
            if ( ef.getFactorValues().size() < 2 ) {
                ei.remove();
            }
        }

        return efs;
    }

    /**
     * @param ef
     * @param fv2bms map of factorValues to lists of biomaterials that have that factorValue.
     * @param bms Chunk of biomaterials to organize.
     * @param doneFactors
     * @return ordered list, or null if there was a problem.
     */
    private static List<BioMaterial> orderByFactor( ExperimentalFactor ef, Map<FactorValue, List<BioMaterial>> fv2bms,
            List<BioMaterial> bms, Map<ExperimentalFactor, Collection<BioMaterial>> doneFactors ) {

        if ( bms.size() == 1 ) return bms;

        log.debug( "Ordering " + bms.size() + " biomaterials by " + ef );

        sortBioMaterials( bms ); // probably redundant.

        List<FactorValue> factorValues = new ArrayList<FactorValue>( ef.getFactorValues() );

        if ( factorValues.size() < 2 ) {
            /*
             * Not strictly disallowed, but useless.
             */
            return bms;
        }

        sortIfMeasurement( factorValues );
        sortByControl( factorValues );

        LinkedHashMap<FactorValue, List<BioMaterial>> chunks = new LinkedHashMap<FactorValue, List<BioMaterial>>();
        List<BioMaterial> organized = new ArrayList<BioMaterial>();

        organizeByFactorValues( fv2bms, bms, factorValues, chunks, organized );

        if ( log.isDebugEnabled() ) {
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
            log.error( "Could not order by factor: " + ef + " Biomaterial count (" + bms.size()
                    + ") does not equal the size of the reorganized biomaterial list (" + organized.size()
                    + "). Check the experimental design for completeness/correctness" );
            // return bms;
            return null;
        }

        return organized;
    }

    /**
     * Organized the results by the factor values (for one factor)
     * 
     * @param fv2bms master map
     * @param bioMaterialChunk biomaterials to organize
     * @param factorValues factor value to consider - biomaterials will be organized in the order given
     * @param chunks map of factor values to chunks goes here
     * @param organized the results go here
     */
    private static void organizeByFactorValues( Map<FactorValue, List<BioMaterial>> fv2bms,
            List<BioMaterial> bioMaterialChunk, List<FactorValue> factorValues,
            LinkedHashMap<FactorValue, List<BioMaterial>> chunks, List<BioMaterial> organized ) {
        Collection<BioMaterial> seenBioMaterials = new HashSet<BioMaterial>();
        for ( FactorValue fv : factorValues ) {

            if ( !fv2bms.containsKey( fv ) ) {
                /*
                 * This can happen if a factorvalue has been created but not yet associated with any biomaterials. This
                 * can also be cruft.
                 */
                continue;
            }

            // all in entire experiment, so we might not want them all as we may just be processing a small chunk.
            List<BioMaterial> biomsforfv = fv2bms.get( fv );

            for ( BioMaterial bioMaterial : biomsforfv ) {
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
        Collection<BioMaterial> leftovers = new HashSet<BioMaterial>();
        for ( BioMaterial bm : bioMaterialChunk ) {
            if ( !seenBioMaterials.contains( bm ) ) {
                leftovers.add( bm );
            }
        }

        if ( leftovers.size() > 0 ) {
            organized.addAll( leftovers );
            chunks.put( ( FactorValue ) null, new ArrayList<BioMaterial>( leftovers ) );
        }

    }

    /**
     * Organize by id, because the order we got the samples in the first place is a reasonable fallback.
     * 
     * @param biomaterials
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
     * Put control factorvalues first.
     * 
     * @param factorValues
     */
    private static void sortByControl( List<FactorValue> factorValues ) {
        Collections.sort( factorValues, new Comparator<FactorValue>() {
            @Override
            public int compare( FactorValue o1, FactorValue o2 ) {
                if ( isBaselineCondition( o1 ) ) {
                    if ( o2.getIsBaseline() == null ) {
                        return -1;
                    } else if ( isBaselineCondition( o2 ) ) {
                        return 0;
                    }
                    return -1;
                } else if ( isBaselineCondition( o2 ) ) {
                    return 1;
                }
                return 0;
            }
        } );

    }

    /**
     * Sort the factor values by measurement values.
     * 
     * @param factorValues
     */
    private static void sortIfMeasurement( List<FactorValue> factorValues ) {
        if ( factorValues.iterator().next().getMeasurement() == null ) {
            // could check EF instead.
            return;
        }
        log.debug( "Sorting measurements" );
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
                    else if ( d1 > d2 ) return 1;
                    return 0;

                } catch ( NumberFormatException e ) {
                    return o1.getMeasurement().getValue().compareTo( o2.getMeasurement().getValue() );
                }
            }
        } );
    }

    /**
     * @param factorValue
     * @return
     */
    protected static boolean isBaselineCondition( FactorValue factorValue ) {

        if ( factorValue.getIsBaseline() != null ) return factorValue.getIsBaseline();

        // for backwards compatibility we check anyway

        if ( factorValue.getMeasurement() != null ) {
            return false;
        } else if ( factorValue.getCharacteristics().isEmpty() ) {
            /*
             * Just use the value.
             */
            if ( StringUtils.isNotBlank( factorValue.getValue() )
                    && controlGroupTerms.contains( factorValue.getValue().toLowerCase() ) ) {
                return true;
            }
        } else {
            for ( Characteristic c : factorValue.getCharacteristics() ) {
                if ( c instanceof VocabCharacteristic ) {
                    String valueUri = ( ( VocabCharacteristic ) c ).getValueUri();
                    if ( StringUtils.isNotBlank( valueUri ) && controlGroupTerms.contains( valueUri.toLowerCase() ) ) {
                        return true;
                    }
                    if ( StringUtils.isNotBlank( c.getValue() )
                            && controlGroupTerms.contains( c.getValue().toLowerCase() ) ) {
                        return true;
                    }
                } else if ( StringUtils.isNotBlank( c.getValue() )
                        && controlGroupTerms.contains( c.getValue().toLowerCase() ) ) {
                    return true;
                }
            }
        }
        return false;
    }

}
