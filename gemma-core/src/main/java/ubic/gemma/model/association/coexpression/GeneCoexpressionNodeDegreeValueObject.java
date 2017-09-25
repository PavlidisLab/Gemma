/*
 * The gemma project
 * 
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.model.association.coexpression;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import ubic.basecode.io.ByteArrayConverter;

import java.util.List;
import java.util.TreeMap;

/**
 * Represents a GeneCoexpressionNodeDegree
 *
 * @author Paul
 */
public class GeneCoexpressionNodeDegreeValueObject {

    private static ByteArrayConverter bac = new ByteArrayConverter();
    private Long geneId;
    private TreeMap<Integer, Integer> nodeDegreesNeg = new TreeMap<>();
    private TreeMap<Integer, Integer> nodeDegreesPos = new TreeMap<>();
    private TreeMap<Integer, Double> relDegreesNeg = new TreeMap<>();
    private TreeMap<Integer, Double> relDegreesPos = new TreeMap<>();

    public GeneCoexpressionNodeDegreeValueObject( GeneCoexpressionNodeDegree entity ) {
        this.geneId = entity.getGeneId();
        initLinkCounts( entity.getLinkCountsPositive(), true );
        initLinkCounts( entity.getLinkCountsNegative(), false );
        initRelRanks( entity.getRelativeLinkRanksPositive(), true );
        initRelRanks( entity.getRelativeLinkRanksNegative(), false );
    }

    public double[] asDoubleArrayNegRanks() {
        return asDoubleArray( relDegreesNeg );
    }

    public double[] asDoubleArrayPosRanks() {
        return asDoubleArray( relDegreesPos );
    }

    /**
     * @return counts at each level of support, starting from 0 (which will be 0), up to the maximum support.
     */
    public int[] asIntArrayNeg() {
        return this.asIntArray( nodeDegreesNeg );
    }

    /**
     * @return counts at each level of support, starting from 0 (which will be 0), up to the maximum support.
     */
    public int[] asIntArrayPos() {
        return this.asIntArray( nodeDegreesPos );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        GeneCoexpressionNodeDegreeValueObject other = ( GeneCoexpressionNodeDegreeValueObject ) obj;
        if ( geneId == null ) {
            if ( other.geneId != null )
                return false;
        } else if ( !geneId.equals( other.geneId ) )
            return false;
        return true;
    }

    public Long getGeneId() {
        return geneId;
    }

    /**
     * @param support value
     * @return how many links have this much support (specifically).
     */
    public Integer getLinksWithExactSupport( Integer support, boolean positive ) {
        if ( positive ) {
            return nodeDegreesPos.containsKey( support ) ? nodeDegreesPos.get( support ) : 0;
        }
        return nodeDegreesNeg.containsKey( support ) ? nodeDegreesNeg.get( support ) : 0;

    }

    /**
     * @return total number of links (this is just the total of positive and negative; if some of those are with the
     * same genes it's a double count, sorry)
     */
    public Integer getLinksWithMinimumSupport( int i ) {
        return this.getLinksWithMinimumSupport( i, true ) + this.getLinksWithMinimumSupport( i, false );
    }

    /**
     * @param support threshold
     * @return how many links have at least this much support (cumulative)
     */
    public Integer getLinksWithMinimumSupport( Integer support, boolean positive ) {
        assert support >= 0;
        int sum = 0;

        if ( positive ) {
            int maxSupportPos = getMaxSupportPos();
            for ( Integer i = support; i <= maxSupportPos; i++ ) {
                sum += nodeDegreesPos.containsKey( i ) ? nodeDegreesPos.get( i ) : 0;
            }
        } else {
            int maxSupportNeg = getMaxSupportNeg();
            for ( Integer i = support; i <= maxSupportNeg; i++ ) {
                sum += nodeDegreesNeg.containsKey( i ) ? nodeDegreesNeg.get( i ) : 0;
            }
        }

        return sum;
    }

    public int getMaxSupportNeg() {
        return this.nodeDegreesNeg.isEmpty() ? 0 : this.nodeDegreesNeg.lastKey();
    }

    /**
     * @return the largest value for support for this gene
     */
    public int getMaxSupportPos() {
        return this.nodeDegreesPos.isEmpty() ? 0 : this.nodeDegreesPos.lastKey();
    }

    public Double getRankAtMinimumSupport( Integer support, boolean positive ) {
        // this can be invalid if node degree isn't updated for this gene.F
        if ( positive ) {
            return relDegreesPos.get( support );

        }
        return relDegreesNeg.get( support );

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( geneId == null ) ? 0 : geneId.hashCode() );
        return result;
    }

    public synchronized void increment( Integer support, boolean positive ) {
        if ( positive ) {
            if ( !nodeDegreesPos.containsKey( support ) ) {
                nodeDegreesPos.put( support, 1 );
            } else {
                nodeDegreesPos.put( support, nodeDegreesPos.get( support ) + 1 );
            }
        } else {
            if ( !nodeDegreesNeg.containsKey( support ) ) {
                nodeDegreesNeg.put( support, 1 );
            } else {
                nodeDegreesNeg.put( support, nodeDegreesNeg.get( support ) + 1 );
            }
        }

    }

    @Override
    public String toString() {
        return "NodeDegree [geneId=" + geneId + ", nodeDegreesPos=" + StringUtils.join( nodeDegreesPos.values(), " " )
                + ", nodeDegreesNeg=" + StringUtils.join( nodeDegreesNeg.values(), " " ) + "]";
    }

    /**
     * Equivalent to getLinksWithMinimumSupport( 0 )
     *
     * @return how many links this gene has in total, across all levels of support (positive and negative correlations
     * combined)
     */
    public int total() {
        return this.getLinksWithMinimumSupport( 0, true ) + this.getLinksWithMinimumSupport( 0, false );
    }

    /**
     * Used during recomputation only.
     */
    public void clear() {
        this.nodeDegreesNeg.clear();
        this.nodeDegreesPos.clear();
    }

    public GeneCoexpressionNodeDegree toEntity() {
        GeneCoexpressionNodeDegree r = new GeneCoexpressionNodeDegree();
        r.setGeneId( this.geneId );

        r.setRelativeLinkRanksPositive( bac.doubleArrayToBytes( asDoubleArrayPosRanks() ) );
        r.setRelativeLinkRanksNegative( bac.doubleArrayToBytes( asDoubleArrayNegRanks() ) );

        r.setLinkCountsPositive( bac.intArrayToBytes( asIntArrayPos() ) );
        r.setLinkCountsNegative( bac.intArrayToBytes( asIntArrayNeg() ) );

        return r;
    }

    private void initLinkCounts( byte[] linkCountBytes, boolean isPositive ) {
        int[] byteArrayToInts = bac.byteArrayToInts( linkCountBytes );

        if ( byteArrayToInts.length < 2 ) {
            return;
        }

        //noinspection Duplicates // Can not extract method, arrays have diferent primitive types
        if ( isPositive ) {
            nodeDegreesPos = new TreeMap<>();
            for ( int i = 1; i < byteArrayToInts.length; i++ ) {
                nodeDegreesPos.put( i, byteArrayToInts[i] );
            }
        } else {
            nodeDegreesNeg = new TreeMap<>();
            for ( int i = 1; i < byteArrayToInts.length; i++ ) {
                nodeDegreesNeg.put( i, byteArrayToInts[i] );
            }
        }

    }

    private void initRelRanks( byte[] relativeLinkRanks, boolean isPositive ) {
        double[] ranks = bac.byteArrayToDoubles( relativeLinkRanks );

        if ( ranks.length < 2 ) {
            return;
        }

        //noinspection Duplicates // Can not extract method, arrays have diferent primitive types
        if ( isPositive ) {
            relDegreesPos = new TreeMap<>();
            for ( int i = 1; i < ranks.length; i++ ) {
                relDegreesPos.put( i, ranks[i] );
            }
        } else {
            relDegreesNeg = new TreeMap<>();
            for ( int i = 1; i < ranks.length; i++ ) {
                relDegreesNeg.put( i, ranks[i] );
            }
        }

    }

    private double[] toPrimitive( DoubleArrayList list ) {
        //noinspection unchecked
        return ArrayUtils.toPrimitive( ( ( List<Double> ) list.toList() ).toArray( new Double[] {} ) );
    }

    private int[] toPrimitive( IntArrayList list ) {
        //noinspection unchecked
        return ArrayUtils.toPrimitive( ( ( List<Integer> ) list.toList() ).toArray( new Integer[] {} ) );
    }

    private double[] asDoubleArray( TreeMap<Integer, Double> map ) {
        DoubleArrayList list = new DoubleArrayList();
        if ( map.isEmpty() )
            return toPrimitive( list );
        list.setSize( Math.max( list.size(), map.lastKey() + 1 ) );
        for ( Integer s : map.keySet() ) {
            list.set( s, map.get( s ) );
        }

        return toPrimitive( list );
    }

    private int[] asIntArray( TreeMap<Integer, Integer> nodeDegreesNeg ) {
        IntArrayList list = new IntArrayList();
        if ( nodeDegreesNeg.isEmpty() )
            return toPrimitive( list );
        Integer maxSupport = nodeDegreesNeg.lastKey();
        list.setSize( maxSupport + 1 );
        for ( Integer s = 0; s <= maxSupport; s++ ) {
            if ( nodeDegreesNeg.containsKey( s ) ) {
                list.set( s, nodeDegreesNeg.get( s ) );
            } else {
                list.set( s, 0 );
            }
        }
        return toPrimitive( list );
    }

}
