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

import java.io.Serializable;
import java.util.List;
import java.util.TreeMap;

import static ubic.gemma.persistence.util.ByteArrayUtils.*;

/**
 * Represents a GeneCoexpressionNodeDegree
 *
 * @author Paul
 */
public class GeneCoexpressionNodeDegreeValueObject implements Serializable {

    private Long geneId;
    private TreeMap<Integer, Integer> nodeDegreesNeg = new TreeMap<>();
    private TreeMap<Integer, Integer> nodeDegreesPos = new TreeMap<>();
    private TreeMap<Integer, Double> relDegreesNeg = new TreeMap<>();
    private TreeMap<Integer, Double> relDegreesPos = new TreeMap<>();

    public GeneCoexpressionNodeDegreeValueObject() {
        super();
    }

    public GeneCoexpressionNodeDegreeValueObject( GeneCoexpressionNodeDegree entity ) {
        this.geneId = entity.getGeneId();
        this.initLinkCounts( entity.getLinkCountsPositive(), true );
        this.initLinkCounts( entity.getLinkCountsNegative(), false );
        this.initRelRanks( entity.getRelativeLinkRanksPositive(), true );
        this.initRelRanks( entity.getRelativeLinkRanksNegative(), false );
    }

    public double[] asDoubleArrayNegRanks() {
        return this.asDoubleArray( relDegreesNeg );
    }

    public double[] asDoubleArrayPosRanks() {
        return this.asDoubleArray( relDegreesPos );
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

    public Long getGeneId() {
        return geneId;
    }

    /**
     * @param  support  value
     * @param  positive positive
     * @return how many links have this much support (specifically).
     */
    public Integer getLinksWithExactSupport( Integer support, boolean positive ) {
        if ( positive ) {
            return nodeDegreesPos.getOrDefault( support, 0 );
        }
        return nodeDegreesNeg.getOrDefault( support, 0 );

    }

    /**
     * @param  i support
     * @return total number of links (this is just the total of positive and negative; if some of those are with the
     *           same genes it's a double count, sorry)
     */
    public Integer getLinksWithMinimumSupport( int i ) {
        return this.getLinksWithMinimumSupport( i, true ) + this.getLinksWithMinimumSupport( i, false );
    }

    /**
     * @param  support  threshold
     * @param  positive positive
     * @return how many links have at least this much support (cumulative)
     */
    public Integer getLinksWithMinimumSupport( Integer support, boolean positive ) {
        assert support >= 0;
        int sum = 0;

        if ( positive ) {
            int maxSupportPos = this.getMaxSupportPos();
            for ( Integer i = support; i <= maxSupportPos; i++ ) {
                sum += nodeDegreesPos.getOrDefault( i, 0 );
            }
        } else {
            int maxSupportNeg = this.getMaxSupportNeg();
            for ( Integer i = support; i <= maxSupportNeg; i++ ) {
                sum += nodeDegreesNeg.getOrDefault( i, 0 );
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
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
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

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        GeneCoexpressionNodeDegreeValueObject other = ( GeneCoexpressionNodeDegreeValueObject ) obj;
        if ( geneId == null ) {
            return other.geneId == null;
        }
        return geneId.equals( other.geneId );
    }

    @Override
    public String toString() {
        return "NodeDegree [geneId=" + geneId + ", nodeDegreesPos=" + StringUtils.join( nodeDegreesPos.values(), " " )
                + ", nodeDegreesNeg=" + StringUtils.join( nodeDegreesNeg.values(), " " ) + "]";
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

    /**
     * Equivalent to getLinksWithMinimumSupport( 0 )
     *
     * @return how many links this gene has in total, across all levels of support (positive and negative correlations
     *         combined)
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

        r.setRelativeLinkRanksPositive(
                doubleArrayToBytes( this.asDoubleArrayPosRanks() ) );
        r.setRelativeLinkRanksNegative(
                doubleArrayToBytes( this.asDoubleArrayNegRanks() ) );

        r.setLinkCountsPositive( intArrayToBytes( this.asIntArrayPos() ) );
        r.setLinkCountsNegative( intArrayToBytes( this.asIntArrayNeg() ) );

        return r;
    }

    private void initLinkCounts( byte[] linkCountBytes, boolean isPositive ) {
        int[] linkCounts = byteArrayToInts( linkCountBytes );

        if ( linkCounts.length < 2 ) {
            return;
        }

        //noinspection Duplicates // Can not extract method, arrays have diferent primitive types
        if ( isPositive ) {
            nodeDegreesPos = new TreeMap<>();
            for ( int i = 1; i < linkCounts.length; i++ ) {
                nodeDegreesPos.put( i, linkCounts[i] );
            }
        } else {
            nodeDegreesNeg = new TreeMap<>();
            for ( int i = 1; i < linkCounts.length; i++ ) {
                nodeDegreesNeg.put( i, linkCounts[i] );
            }
        }

    }

    private void initRelRanks( byte[] relativeLinkRanks, boolean isPositive ) {
        double[] ranks = byteArrayToDoubles( relativeLinkRanks );

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
            return this.toPrimitive( list );
        list.setSize( Math.max( list.size(), map.lastKey() + 1 ) );
        for ( Integer s : map.keySet() ) {
            list.set( s, map.get( s ) );
        }

        return this.toPrimitive( list );
    }

    private int[] asIntArray( TreeMap<Integer, Integer> nodedeg ) {
        IntArrayList list = new IntArrayList();
        if ( nodedeg.isEmpty() )
            return this.toPrimitive( list );
        Integer maxSupport = nodedeg.lastKey();
        list.setSize( maxSupport + 1 );
        for ( int s = 0; s <= maxSupport; s++ ) {
            list.set( s, nodedeg.getOrDefault( s, 0 ) );
        }
        return this.toPrimitive( list );
    }

}
