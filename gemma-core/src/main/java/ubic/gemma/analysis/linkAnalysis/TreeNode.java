/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.analysis.linkAnalysis;

import java.io.Serializable;

import cern.colt.list.ObjectArrayList;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
public class TreeNode implements Comparable<TreeNode>, Serializable {

    private static final long serialVersionUID = 7454412470040843963L;
    public static final int MASKBITS = 1;
    public static final int COMMONBITS = 2;
    public static final int LEVEL = 3;
    public static final int ORDER = 4;
    private static int SORTING = MASKBITS;
    private long id;
    private Integer maskBits = 0;
    private long[] mask;
    private TreeNode closestNode = null;
    private Integer commonBits = 0;
    private ObjectArrayList children;
    private TreeNode parent = null;
    private Integer level = 0;
    private Integer order = 0; // for tree generation

    /**
     * @param id
     * @param mask
     * @param child
     */
    public TreeNode( long id, long[] mask, ObjectArrayList child ) {
        this.id = id;
        this.children = child;
        this.mask = mask;
        this.maskBits = LinkMatrix.countBits( this.mask );
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( TreeNode o ) {
        int res = 0;
        switch ( TreeNode.SORTING ) {
            case TreeNode.MASKBITS:
                res = maskBits.compareTo( o.maskBits ) * ( -1 );
                break;
            case TreeNode.COMMONBITS:
                res = commonBits.compareTo( o.commonBits ) * ( -1 );
                break;
            case TreeNode.LEVEL:
                res = level.compareTo( o.level ) * ( -1 );
                break;
            case TreeNode.ORDER:
                res = order.compareTo( o.order ) * ( -1 );
                break;
        }
        return res;
        // return maskBits.compareTo(o.maskBits)*(-1);
    }

    /**
     * @param sorting
     */
    public static void setSorting( int sorting ) {
        TreeNode.SORTING = sorting;
    }

    /**
     * 
     */
    public static void reSetSorting() {
        TreeNode.SORTING = TreeNode.MASKBITS;
    }

    /**
     * @param closestNode
     */
    public void setClosestNode( TreeNode closestNode ) {
        this.closestNode = closestNode;
        commonBits = LinkMatrix.overlapBits( mask, closestNode.mask );
    }

    /**
     * @param parent
     */
    public void setParent( TreeNode parent ) {
        this.parent = parent;
    }

    /**
     * @param child
     */
    public void setChildren( ObjectArrayList child ) {
        this.children = child;
    }

    /**
     * @param level
     */
    public void setLevel( int level ) {
        this.level = level;
    }

    /**
     * @param order
     */
    public void setOrder( int order ) {
        this.order = order;
    }

    public long getId() {
        return id;
    }

    public Integer getMaskBits() {
        return maskBits;
    }

    public long[] getMask() {
        return mask;
    }

    public TreeNode getClosestNode() {
        return closestNode;
    }

    public Integer getCommonBits() {
        return commonBits;
    }

    public TreeNode getParent() {
        return parent;
    }

    public Integer getLevel() {
        return level;
    }

    public Integer getOrder() {
        return order;
    }

    public ObjectArrayList getChildren() {
        return children;
    }

    /**
     * @param allBits
     */
    public void setMask( long[] allBits ) {
        this.mask = allBits;
    }

}
