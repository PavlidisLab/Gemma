/*
 * The baseCode project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.util.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.ObjectArrayList;

/**
 * Calculate rank statistics for arrays.
 * 
 * @author Paul Pavlidis
 * 
 */
public class Rank {

    /**
     * Return a permutation which puts the array in sorted order. In other words, the values returned indicate the
     * positions of the sorted values in the <em>original</em> array (the lowest value has the lowest rank, but it could
     * be located anywhere in the array). Indexes start from 0. Tied values are put in an arbitrary ordering.
     * 
     * @param array
     * @return
     */
    public static IntArrayList order( DoubleArrayList array ) {
        int size = array.size();
        IntArrayList result = new IntArrayList( new int[size] );

        ObjectArrayList ranks = new ObjectArrayList( size );
        for ( int i = 0; i < size; i++ ) {
            RankData<Double> rd = new RankData<>( i, array.get( i ) );
            ranks.add( rd );
        }
        ranks.sort();

        for ( int i = 0; i < size; i++ ) {
            @SuppressWarnings("unchecked")
            RankData<Double> rd = ( RankData<Double> ) ranks.getQuick( i );
            result.setQuick( i, rd.getIndex() );
        }
        return result;
    }

    /**
     * @param ranks
     * @return
     */
    public static long rankSum( List<Double> ranks ) {
        double sum = 0.0;
        for ( Double rank : ranks ) {
            sum += rank;
        }
        return ( long ) sum;
    }

    /**
     * Rank transform an array. The ranks are constructed based on the sort order of the elements. That is, low values
     * get low numbered ranks starting from 1.
     * <p>
     * Ties are resolved by assigning the average rank for tied values. For example, instead of arbitrarily assigning
     * ties ranks 3,4,5, all three values would get a rank of 4 and no value would get a rank of 3 or 5.
     * <p>
     * Missing values are sorted in their natural order, which means they end up all at one end (at the high ('bad')
     * end)
     * 
     * @param array DoubleArrayList
     * @return cern.colt.list.DoubleArrayList
     * @param array, or null if the ranks could not be computed.
     * @return
     */
    public static DoubleArrayList rankTransform( DoubleArrayList array ) {
        return rankTransform( array, false );
    }

    /**
     * Rank transform an array. The ranks are constructed based on the sort order of the elements. That is, low values
     * get low numbered ranks starting from 1, unless you set descending = true.
     * <p>
     * Ties are resolved by assigning the average rank for tied values. For example, instead of arbitrarily assigning
     * ties ranks 3,4,5, all three values would get a rank of 4 and no value would get a rank of 3 or 5.
     * <p>
     * Missing values are sorted in their natural order, which means they end up all at one end (at the high ('bad')
     * end)
     * 
     * @param array DoubleArrayList
     * @param descending - reverse the usual ordering so larger values are the the front.
     * @return cern.colt.list.DoubleArrayList, or null if the input is empty or null.
     */
    public static DoubleArrayList rankTransform( DoubleArrayList array, boolean descending ) {
        if ( array == null ) {
            return null;
        }

        int size = array.size();
        if ( size == 0 ) {
            return null;
        }

        List<RankData<Double>> ranks = new ArrayList<RankData<Double>>( size );
        DoubleArrayList result = new DoubleArrayList( new double[size] );

        // store the values with their indices - not sorted yet.
        for ( int i = 0; i < size; i++ ) {
            double v = array.get( i );

            if ( descending ) {
                v = -v;
            }
            RankData<Double> rd = new RankData<Double>( i, v );
            // if ( Double.isNaN( v ) ) throw new IllegalArgumentException( "Missing values are not tolerated." );
            ranks.add( rd );
        }

        Collections.sort( ranks );

        // fill in the results. We iterate over the ranks by order.
        Double prevVal = null;
        int rank = 1;
        int nominalRank = 1; // rank we'd have if no ties.
        for ( int i = 0; i < size; i++ ) {
            RankData<Double> rankData = ranks.get( i );
            int index = rankData.getIndex();
            Double val = rankData.getValue();

            result.set( index, nominalRank ); // might not keep.

            // only bump up ranks if we're not tied with the last one.
            if ( prevVal != null && !val.equals( prevVal ) ) {
                rank = nominalRank;
            } else {
                // tied. Do not advance the rank.
                result.set( index, rank );
            }

            prevVal = val;
            nominalRank++;
        }

        // At this point we may have repeated ranks.

        fixTies( result, ranks );

        return result;
    }

    /**
     * Rank transform a map, where the values are Comparable values we wish to rank. Ties are broken as for the other
     * methods. Ranks are zero-based
     * <p>
     * Missing values are sorted in their natural order, which means they end up all at one end (at the high ('bad')
     * end)
     * 
     * @param m java.util.Map with keys Objects, values Doubles.
     * @return A java.util.Map keys=old keys, values=java.lang.Double rank of the key. Non-integer values mean tie
     *         splits.
     */
    public static <K> Map<K, Double> rankTransform( Map<K, ? extends Comparable<?>> m ) {
        return rankTransform( m, false );
    }

    /**
     * Ties are broken as for the other methods. CAUTION - ranks start at 0.
     * <p>
     * Missing values are sorted in their natural order, which means they end up all at one end (at the high ('bad')
     * end)
     * 
     * @param <K>
     * @param m
     * @param desc if true, the lowest (first) rank will be for the highest value.
     * @return A java.util.Map keys=old keys, values=java.lang.Double rank of the key. Non-integer values mean tie
     *         splits.
     */
    public static <K> Map<K, Double> rankTransform( Map<K, ? extends Comparable<?>> m, boolean desc ) {

        List<KeyAndValueData<K>> values = new ArrayList<KeyAndValueData<K>>();

        for ( Iterator<K> itr = m.keySet().iterator(); itr.hasNext(); ) {

            K key = itr.next();
            Comparable<?> val = m.get( key );

            values.add( new KeyAndValueData<K>( 0, key, val ) );
        }

        return rankTransform( m, desc, values );
    }

    /**
     * @param ranksWithTies
     */
    private static void fixTies( DoubleArrayList ranksWithTies, List<? extends RankData<?>> ranks ) {

        int numties = 1;
        Double prev = null;
        int i = 0;
        // iterate over in order of the values.
        for ( int j = ranksWithTies.size(); i < j; i++ ) {
            RankData<?> rankData = ranks.get( i );
            int index = rankData.getIndex();
            double rank = ranksWithTies.getQuick( index );

            if ( prev != null ) {
                if ( rank == prev.doubleValue() ) {
                    // record how many ties we have seen
                    numties++;
                    // System.err.println( "Tied rank: " + rank );
                } else if ( numties > 1 ) {
                    // we're past the batch of ties and need to adjust them
                    for ( int k = 1; k <= numties; k++ ) {
                        int indexOfTied = ranks.get( i - k ).getIndex(); // original rank?
                        double rawRankInTie = ranksWithTies.getQuick( indexOfTied );
                        double meanRank = meanRank( rawRankInTie, numties );
                        ranksWithTies.setQuick( indexOfTied, meanRank );
                    }
                    numties = 1; // reset
                }
                // no tie, nothing needs to be changed.
            }
            prev = rank;
        }

        // cleanup the end of the array if there were ties there.
        if ( numties > 1 ) {
            for ( int k = 1; k <= numties; k++ ) {
                int indexOfTied = ( ( RankData<?> ) ranks.get( i - k ) ).getIndex();
                double rawRankInTie = ranksWithTies.getQuick( indexOfTied );
                double meanRank = meanRank( rawRankInTie, numties );
                ranksWithTies.setQuick( indexOfTied, meanRank );
            }
        }

    }

    /**
     * @param <K>
     * @param ranksWithTies
     * @param ranks
     */
    private static <K> void fixTies( Map<K, Double> ranksWithTies, List<KeyAndValueData<K>> ranks ) {
        int numties = 1;
        Double prev = null;
        int i = 0;
        // iterate over in order of the values.
        for ( int j = ranksWithTies.size(); i < j; i++ ) {
            KeyAndValueData<K> rankData = ranks.get( i );
            double rank = ranksWithTies.get( rankData.getKey() );
            if ( prev != null ) {
                if ( rank == prev.doubleValue() ) {
                    // record how many ties we have seen
                    numties++;
                    // System.err.println( "Tied rank: " + rank );
                } else if ( numties > 1 ) {
                    // we're past the batch of ties and need to adjust them
                    for ( int k = 1; k <= numties; k++ ) {
                        K indexOfTied = ranks.get( i - k ).getKey(); // original key
                        double rawRankInTie = ranksWithTies.get( indexOfTied );
                        double meanRank = meanRank( rawRankInTie, numties );
                        ranksWithTies.put( indexOfTied, meanRank );
                    }
                    numties = 1; // reset
                }
                // no tie, nothing needs to be changed.
            }
            prev = rank;
        }

        // cleanup the end of the array if there were ties there.
        if ( numties > 1 ) {
            for ( int k = 1; k <= numties; k++ ) {
                K indexOfTied = ranks.get( i - k ).getKey();
                double rawRankInTie = ranksWithTies.get( indexOfTied );
                double meanRank = meanRank( rawRankInTie, numties );
                ranksWithTies.put( indexOfTied, meanRank );
            }
        }
    }

    /**
     * @param rawRank
     * @param numties
     * @return
     */
    private static double meanRank( double rawRank, int numties ) {
        double total = 0;
        for ( int i = 0; i < numties; i++ ) {
            total = total + rawRank + i;
        }
        return total / numties;
    }

    /**
     * @param m
     * @param desc
     * @param values this gets sorted.
     * @return A java.util.Map keys=old keys, values=java.lang.Double rank of the key. Non-integer values mean tie
     *         splits.
     */
    @SuppressWarnings("unchecked")
    private static <K> Map<K, Double> rankTransform( Map<K, ? extends Comparable<?>> m, boolean desc,
            List<KeyAndValueData<K>> values ) {

        Collections.sort( values );
        Map<K, Double> result = new HashMap<K, Double>();

        Double rank = 0.0;
        Comparable<K> prevVal = null;
        Double nominalRank = 0.0; // rank we'd have if no ties.
        for ( int i = 0; i < m.size(); i++ ) {
            result.put( values.get( i ).getKey(), ( double ) nominalRank ); // might not keep.

            Comparable<K> val = values.get( i ).getValue();
            // only bump up ranks if we're not tied with the last one.
            if ( prevVal != null && !val.equals( prevVal ) ) {
                rank = nominalRank;
            } else {
                // tied. Do not advance the rank.
                result.put( values.get( i ).getKey(), rank );
            }

            prevVal = val;
            nominalRank++;

        }

        fixTies( result, values );

        if ( desc ) {
            /*
             * Reverse all the values.
             */
            Map<K, Double> finalResult = new HashMap<K, Double>();
            double mr = result.size();
            for ( K k : result.keySet() ) {
                double d = result.get( k );
                finalResult.put( k, mr - d - 1.0 );
            }
            return finalResult;
        }

        return result;
    }
}

/*
 * Helper class for rankTransform map.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class KeyAndValueData<K> extends RankData {
    private K key;

    public KeyAndValueData( int index, K id, Comparable<?> v ) {
        super( index, v );
        this.key = id;
    }

    public int compareTo( KeyAndValueData<K> other ) {
        return this.value.compareTo( other.getValue() );
    }

    public K getKey() {
        return key;
    }
}

/*
 * Helper class for rankTransform .
 */
class RankData<C extends Comparable<C>> implements Comparable<RankData<C>> {

    int index = 0;

    C value = null;

    public RankData( int tindex, C tvalue ) {
        index = tindex;
        value = tvalue;
    }

    @Override
    public int compareTo( RankData<C> other ) {
        return this.value.compareTo( other.getValue() );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( !( obj instanceof RankData ) ) return false;
        return this.value.equals( ( ( RankData<C> ) obj ).getValue() );
    }

    public int getIndex() {
        return index;
    }

    public C getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        if ( value == null ) return 1;
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "Index=" + index + " value=" + value;
    }
}