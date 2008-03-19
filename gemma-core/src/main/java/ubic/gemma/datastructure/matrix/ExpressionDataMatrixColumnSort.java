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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Methods to organize ExpressionDataMatrices by column (or at least provide the ordering)
 * 
 * @author paul
 * @version $Id$
 */
public class ExpressionDataMatrixColumnSort {
    private static Log log = LogFactory.getLog( ExpressionDataMatrixColumnSort.class.getName() );

    /**
     * @param mat
     * @return
     */
    public static List<BioMaterial> orderByName( ExpressionDataMatrix mat ) {
        List<BioMaterial> start = getBms( mat );
        Collections.sort( start, new Comparator<BioMaterial>() {
            public int compare( BioMaterial o1, BioMaterial o2 ) {

                if ( o1.getBioAssaysUsedIn().isEmpty() || o2.getBioAssaysUsedIn().isEmpty() )
                    return o1.getName().compareTo( o1.getName() );

                BioAssay ba1 = o1.getBioAssaysUsedIn().iterator().next();
                BioAssay ba2 = o2.getBioAssaysUsedIn().iterator().next();
                if ( ba1.getName() != null && ba2.getName() != null ) {
                    return ba1.getName().compareTo( ba2.getName() );
                } else {
                    if ( o1.getName() != null && o2.getName() != null ) {
                        return o1.getName().compareTo( o2.getName() );
                    } else {
                        return 0;
                    }
                }
            }
        } );
        return start;
    }

    /**
     * @param mat
     * @return
     */
    public static List<BioMaterial> orderByExperimentalDesign( ExpressionDataMatrix mat ) {
        Map<FactorValue, Collection<BioMaterial>> fv2bms = buildFv2BmMap( mat );

        List<BioMaterial> start = getBms( mat );

        Collection<ExperimentalFactor> factors = getFactors( start );

        if ( factors.size() == 0 ) {
            log.warn( "No experimental design, sorting by sample name" );
            return orderByName( mat );
        }

        // TODO Start with the factor with the fewest categories.
        ExperimentalFactor ef = factors.iterator().next();

        List<BioMaterial> ordered = orderByFactor( ef, fv2bms, start,
                new HashMap<ExperimentalFactor, Collection<BioMaterial>>() );

        // log.info( "AFTER" );
        // assert ordered.size() == start.size();
        // StringBuilder buf2 = new StringBuilder();
        // for ( BioMaterial bioMaterial : ordered ) {
        // buf2.append( bioMaterial.getId() + " " );
        // for ( FactorValue fv : bioMaterial.getFactorValues() ) {
        // buf2.append( fv + " " );
        // }
        // buf2.append( "\n" );
        // }
        // log.info( buf2.toString() );

        return ordered;

    }

    /**
     * @param ef
     * @param fv2bms
     * @param bms
     * @param doneFactors
     * @return
     */
    private static List<BioMaterial> orderByFactor( ExperimentalFactor ef,
            Map<FactorValue, Collection<BioMaterial>> fv2bms, List<BioMaterial> bms,
            HashMap<ExperimentalFactor, Collection<BioMaterial>> doneFactors ) {

        if ( bms.size() == 1 ) return bms;

        log.debug( "Ordering " + bms.size() + " biomaterials by " + ef );
        List<FactorValue> factorValues = new ArrayList<FactorValue>( ef.getFactorValues() );

        sortIfMeasurement( factorValues );

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
            return bms;
        }

        if ( !doneFactors.containsKey( ef ) ) {
            doneFactors.put( ef, new HashSet<BioMaterial>() );
        }

        log.debug( "Marking " + bms.size() + " bms done for " + ef.getName() );
        doneFactors.get( ef ).addAll( bms );

        int chunkStartIndex = 0;
        for ( FactorValue factorValue : chunks.keySet() ) {

            List<BioMaterial> chunk = chunks.get( factorValue );

            log.debug( "Processing chunk for " + factorValue + " with " + chunk.size() + " members" );

            if ( chunk.size() == 1 ) {
                chunkStartIndex += chunk.size();
                continue; // can't reorder it!
            }

            Collection<ExperimentalFactor> factors = getFactors( chunk );
            factor: for ( ExperimentalFactor nextFactor : factors ) {

                if ( nextFactor.equals( ef ) ) continue;
                if ( doneFactors.containsKey( nextFactor ) ) {
                    for ( BioMaterial seen : doneFactors.get( nextFactor ) ) {
                        if ( chunk.contains( seen ) ) {
                            continue factor;
                        }
                    }
                }

                log.debug( "Reordering " + chunk.size() + "bms by " + nextFactor );

                List<BioMaterial> orderedChunk = orderByFactor( nextFactor, fv2bms, chunk, doneFactors );

                /*
                 * Reorganize the current chunk
                 */
                log.debug( "Implementing ordering by " + nextFactor );
                int j = 0;
                for ( int i = chunkStartIndex; i < chunkStartIndex + chunk.size(); i++ ) {
                    organized.set( i, orderedChunk.get( j ) );
                    j++;
                }
                break; // only one factor at a time, please
            }
            chunkStartIndex += chunk.size();
        }
        return organized;
    }

    /**
     * @param fv2bms master map
     * @param bms biomaterials to use
     * @param factorValues factor value to consider
     * @param chunks map of factor values to chunks goes here
     * @param organized the results go here
     */
    private static void organizeByFactorValues( Map<FactorValue, Collection<BioMaterial>> fv2bms,
            List<BioMaterial> bms, List<FactorValue> factorValues,
            LinkedHashMap<FactorValue, List<BioMaterial>> chunks, List<BioMaterial> organized ) {
        for ( FactorValue fv : factorValues ) {

            // all in entire experiment, so we might not want them all.
            Collection<BioMaterial> biomsforfv = fv2bms.get( fv );

            for ( BioMaterial bioMaterial : biomsforfv ) {
                if ( bms.contains( bioMaterial ) ) {
                    if ( !chunks.containsKey( fv ) ) {
                        chunks.put( fv, new ArrayList<BioMaterial>() );
                    }
                    chunks.get( fv ).add( bioMaterial );
                }
            }

            // If we used that fv ...
            if ( chunks.containsKey( fv ) ) {
                organized.addAll( chunks.get( fv ) ); // now at least this is in order of this factor
            }
        }
    }

    /**
     * @param factorValues
     */
    private static void sortIfMeasurement( List<FactorValue> factorValues ) {
        if ( factorValues.iterator().next().getMeasurement() == null ) {
            return;
        }
        log.debug( "Sorting" );
        Collections.sort( factorValues, new Comparator<FactorValue>() {
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
     * Get all biomaterials for a matrix.
     * 
     * @param mat
     * @return
     */
    private static List<BioMaterial> getBms( ExpressionDataMatrix mat ) {
        List<BioMaterial> result = new ArrayList<BioMaterial>();
        for ( int i = 0; i < mat.columns(); i++ ) {
            result.add( mat.getBioMaterialForColumn( i ) );
        }
        return result;
    }

    private static Map<FactorValue, Collection<BioMaterial>> buildFv2BmMap( ExpressionDataMatrix mat ) {
        Map<FactorValue, Collection<BioMaterial>> fv2bms = new HashMap<FactorValue, Collection<BioMaterial>>();
        for ( int i = 0; i < mat.columns(); i++ ) {
            BioMaterial bm = mat.getBioMaterialForColumn( i );
            Collection<FactorValue> factorValues = bm.getFactorValues();
            for ( FactorValue fv : factorValues ) {
                if ( !fv2bms.containsKey( fv ) ) {
                    fv2bms.put( fv, new HashSet<BioMaterial>() );
                }
                fv2bms.get( fv ).add( bm );
            }
        }
        return fv2bms;
    }

    private static Collection<ExperimentalFactor> getFactors( Collection<BioMaterial> bms ) {
        Collection<ExperimentalFactor> efs = new HashSet<ExperimentalFactor>();
        for ( BioMaterial bm : bms ) {
            Collection<FactorValue> factorValues = bm.getFactorValues();
            for ( FactorValue fv : factorValues ) {
                efs.add( fv.getExperimentalFactor() );
            }
        }
        return efs;
    }

}
