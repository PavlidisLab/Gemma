/*
 * The gemma-model project
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

import java.util.List;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ubic.basecode.io.ByteArrayConverter;
import cern.colt.list.IntArrayList;

/**
 * Represents a GeneCoexpressionNodeDegree
 * 
 * @author Paul
 * @version $Id$
 */
public class GeneCoexpressionNodeDegreeValueObject {

    private static ByteArrayConverter bac = new ByteArrayConverter();

    private Long geneId;

    private TreeMap<Integer, Integer> nodeDegrees = new TreeMap<>();

    private TreeMap<Integer, Double> relDegrees = new TreeMap<>();

    private static Logger log = LoggerFactory.getLogger( GeneCoexpressionNodeDegreeValueObject.class );

    public GeneCoexpressionNodeDegreeValueObject( GeneCoexpressionNodeDegree entity ) {
        this.geneId = entity.getGeneId();
        initLinkCounts( entity.getLinkCounts() );
        initRelRanks( entity.getRelativeLinkRanks() );
    }

    /**
     * @param relativeLinkRanks
     */
    private void initRelRanks( byte[] relativeLinkRanks ) {
        double[] ranks = bac.byteArrayToDoubles( relativeLinkRanks );

        if ( ranks.length < 2 ) {
            return;
        }

        relDegrees = new TreeMap<>();
        for ( int i = 1; i < ranks.length; i++ ) {
            relDegrees.put( i, ranks[i] );
        }

    }

    /**
     * counts at each level of support, starting from 0 (which will be 0)
     * 
     * @return
     */
    public int[] asIntArray() {
        IntArrayList list = new IntArrayList();
        if ( nodeDegrees.isEmpty() ) return toPrimitive( list );
        list.setSize( Math.max( list.size(), nodeDegrees.lastKey() + 1 ) );
        for ( Integer s : nodeDegrees.keySet() ) {
            list.set( s, nodeDegrees.get( s ) );
        }

        return toPrimitive( list );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        GeneCoexpressionNodeDegreeValueObject other = ( GeneCoexpressionNodeDegreeValueObject ) obj;
        if ( geneId == null ) {
            if ( other.geneId != null ) return false;
        } else if ( !geneId.equals( other.geneId ) ) return false;
        return true;
    }

    public Long getGeneId() {
        return geneId;
    }

    /**
     * @param support value
     * @return how many links have this much support (specifically).
     */
    public Integer getLinksWithExactSupport( Integer support ) {
        return nodeDegrees.containsKey( support ) ? nodeDegrees.get( support ) : 0;
    }

    /**
     * @param support threshold
     * @return how many links have at least this much support (cumulative)
     */
    public Integer getLinksWithMinimumSupport( Integer support ) {
        assert support >= 0;
        int sum = 0;
        for ( int i = support; i <= getMaxSupport(); i++ ) {
            sum += nodeDegrees.containsKey( i ) ? nodeDegrees.get( i ) : 0;
        }

        if ( sum == 0 ) {
            // this can happen if the node degree is not yet populated, but yet know it has to be at least one
            log.warn( "Node degree information invalid for gene=" + geneId );
            return 1;
        }

        return sum;
    }

    /**
     * @param support
     * @return
     */
    public Double getRankAtMinimumSupport( Integer support ) {
        // this can be invalid if node degree isn't updated for this gene.F
        return relDegrees.get( support );
    }

    /**
     * @return the largest value for support for this gene
     */
    public int getMaxSupport() {
        return this.nodeDegrees.isEmpty() ? 0 : this.nodeDegrees.lastKey();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( geneId == null ) ? 0 : geneId.hashCode() );
        return result;
    }

    /**
     * @param support
     */
    public synchronized void increment( Integer support ) {
        if ( !nodeDegrees.containsKey( support ) ) {
            nodeDegrees.put( support, 1 );
        } else {
            nodeDegrees.put( support, nodeDegrees.get( support ) + 1 );
        }
    }

    @Override
    public String toString() {
        return "NodeDegree [geneId=" + geneId + ", nodeDegrees=" + StringUtils.join( nodeDegrees.values(), " " )
                + ", nodeDegrees=" + StringUtils.join( nodeDegrees.values(), " " ) + "]";
    }

    /**
     * Equivalent to getLinksWithMinimumSupport( 0 )
     * 
     * @return how many links this gene has in total, across all levels of support
     */
    public int total() {
        return this.getLinksWithMinimumSupport( 0 );
    }

    /**
     * Used during recomputation only.
     */
    protected void clear() {
        this.nodeDegrees.clear();
    }

    protected GeneCoexpressionNodeDegree toEntity() {
        GeneCoexpressionNodeDegree r = new GeneCoexpressionNodeDegreeImpl();
        r.setGeneId( this.geneId );

        r.setLinkCounts( bac.intArrayToBytes( asIntArray() ) );

        return r;
    }

    /**
     * @return
     */
    private void initLinkCounts( byte[] linkCountBytes ) {
        int[] byteArrayToInts = bac.byteArrayToInts( linkCountBytes );

        if ( byteArrayToInts.length < 2 ) {
            return;
        }

        nodeDegrees = new TreeMap<>();
        for ( int i = 1; i < byteArrayToInts.length; i++ ) {
            nodeDegrees.put( i, byteArrayToInts[i] );
        }

    }

    /**
     * @param list
     * @return
     */
    private int[] toPrimitive( IntArrayList list ) {
        return ArrayUtils.toPrimitive( ( ( List<Integer> ) list.toList() ).toArray( new Integer[] {} ) );
    }

}
