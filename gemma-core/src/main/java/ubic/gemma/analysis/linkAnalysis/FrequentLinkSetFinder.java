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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cern.colt.list.ObjectArrayList;
import ubic.basecode.dataStructure.matrix.CompressedNamedBitMatrix;

/**
 * Find frequently-occuring links in a link matrix
 * 
 * @author xwan
 * @version $Id$
 */
public class FrequentLinkSetFinder {

    public static int nodeNum = 0;
    private static Log log = LogFactory.getLog( FrequentLinkSetFinder.class.getName() );
    private int threshold = 3;
    private TreeNode root = null;
    private int merged = 0;

    /**
     * Holds the results (shouldn't be a field?)
     */
    private ObjectArrayList candidatesNodes = null; // a linked list

    private LinkMatrix linkMatrix = null;

    /*
     * FIXME these values should not be hard-coded.
     */
    int minSupport = 7; // minimum support. Should this be the same as threshold?
    int minLinks = 7; // number of links for a "set" before we report it.

    /**
     * @param threshold for support of links to be searched for.
     * @param linkMatrix an already-initialized link matrix.
     */
    public FrequentLinkSetFinder( int threshold, LinkMatrix linkMatrix ) {
        super();
        this.threshold = threshold;
        int num = linkMatrix.getRawMatrix().getBitNum() / CompressedNamedBitMatrix.BITS_PER_ELEMENT + 1;
        root = new TreeNode( 0, new long[num], null );
        root.setLevel( 0 );
        candidatesNodes = new ObjectArrayList();
        this.linkMatrix = linkMatrix;
    }

    /**
     * 
     */
    public void find() {
        find( this.getValidNodes() );
    }

    /**
     * @param validNodes
     */
    public void find( ObjectArrayList validNodes ) {
        validNodes.sort();
        root.setChildren( validNodes );
        nodeNum = nodeNum + validNodes.size();
        log.info( "Initalized " + nodeNum + " nodes" );

        for ( int i = 0, j = root.getChildren().size() - 1; i < j; i++ ) {
            TreeNode iter = ( TreeNode ) root.getChildren().getQuick( i );
            iter.setParent( root );
            this.expand( iter, root.getChildren(), i );
        }

        this.travel( root, minSupport, minLinks );
        this.candidatesNodes.sort();
        for ( int i = 0; i < candidatesNodes.size(); i++ ) {
            TreeNode node = ( TreeNode ) candidatesNodes.getQuick( i );
            this.outputPath( node );
        }
    }

    /**
     * Prints information about the tree for a node.
     * 
     * @param leafNode
     */
    public void outputPath( TreeNode leafNode ) {
        TreeNode iter = leafNode;
        System.err.print( leafNode.getMaskBits() + "\t" );
        for ( int i = 0; i < leafNode.getMask().length; i++ ) {
            for ( int j = 0; j < CompressedNamedBitMatrix.BITS_PER_ELEMENT; j++ )
                if ( ( leafNode.getMask()[i] & ( CompressedNamedBitMatrix.BIT1 << j ) ) != 0 ) {
                    System.err.print( "1" );
                } else {
                    System.err.print( "0" );
                }
        }
        System.err.print( "\t" );

        while ( iter.getParent() != null ) {
            System.err.print( " (" + linkMatrix.getLinkName( iter.getId() ) + ") " );
            iter = iter.getParent();
        }

        for ( int i = 0; i < leafNode.getMask().length; i++ ) {
            for ( int j = 0; j < CompressedNamedBitMatrix.BITS_PER_ELEMENT; j++ )
                if ( ( leafNode.getMask()[i] & ( CompressedNamedBitMatrix.BIT1 << j ) ) != 0 ) {
                    System.err.print( linkMatrix.getEEName( j + i * CompressedNamedBitMatrix.BITS_PER_ELEMENT ) + " " );
                }
        }
        System.err.println( "" );
    }

    /**
     * @param rootNode
     * @param siblings
     * @param rootNodeIndex
     */
    private void expand( TreeNode rootNode, ObjectArrayList siblings, int rootNodeIndex ) {
        if ( rootNode.getId() == 0 ) {
            throw new IllegalStateException( "Logic Error" );
        }
        long[] childMask = new long[rootNode.getMask().length];
        ObjectArrayList child = new ObjectArrayList();
        int index = rootNodeIndex + 1;
        while ( index < siblings.size() ) {
            TreeNode iter = ( TreeNode ) siblings.getQuick( index );
            for ( int i = 0; i < childMask.length; i++ )
                childMask[i] = iter.getMask()[i];
            boolean mergedCondition = true;
            for ( int i = 0; i < childMask.length; i++ ) {
                if ( childMask[i] != ( childMask[i] & rootNode.getMask()[i] ) ) {
                    childMask[i] = childMask[i] & rootNode.getMask()[i];
                    mergedCondition = false;
                }
            }
            if ( LinkMatrix.countBits( childMask ) >= this.threshold ) {
                if ( mergedCondition ) {
                    siblings.remove( index );
                    child.add( iter );
                    iter.setLevel( rootNode.getLevel() + 1 );
                    iter.setParent( rootNode );
                    merged = merged + 1;
                } else {
                    long mask[] = new long[childMask.length];
                    for ( int i = 0; i < childMask.length; i++ )
                        mask[i] = childMask[i];
                    TreeNode newCreatedNode = new TreeNode( iter.getId(), mask, null );
                    newCreatedNode.setParent( rootNode );
                    newCreatedNode.setLevel( rootNode.getLevel() + 1 );
                    child.add( newCreatedNode );
                    nodeNum++;
                    if ( nodeNum % 10000 == 0 ) System.err.println( nodeNum + " " + merged );
                    index++;
                }
            }
        }
        if ( child.size() > 0 ) {
            child.sort();
            rootNode.setChildren( child );
            for ( int i = 0; i < rootNode.getChildren().size() - 1; i++ ) {
                TreeNode iter = ( TreeNode ) rootNode.getChildren().getQuick( i );
                this.expand( iter, rootNode.getChildren(), i );
            }
        }
    }

    /**
     * @return nodes which have support of at least threshold value.
     */
    private ObjectArrayList getValidNodes() {
        ObjectArrayList validNodes = new ObjectArrayList();
        for ( int i = 0; i < linkMatrix.getRawMatrix().rows(); i++ ) {
            for ( int j = i + 1; j < linkMatrix.getRawMatrix().columns(); j++ ) {
                if ( linkMatrix.getRawMatrix().bitCount( i, j ) >= this.threshold ) {
                    TreeNode oneNode = new TreeNode( linkMatrix.generateId( i, j ), linkMatrix.getRawMatrix()
                            .getAllBits( i, j ), null );
                    oneNode.setMask( linkMatrix.getRawMatrix().getAllBits( i, j ) );
                    validNodes.add( oneNode );
                }
            }
        }
        return validNodes;
    }

    /**
     * @param oneNode
     */
    private void insertCandidatesNode( TreeNode oneNode ) {
        candidatesNodes.add( oneNode );
    }

    /**
     * This is the main method responsible for finding frequent itemsets once the tree is populated.
     * 
     * @param rootNode
     * @param minExps
     * @param minLinks
     */
    private void travel( TreeNode rootNode, int minExps, int minLinks ) {

        /*
         * Leaf node.
         */
        if ( rootNode.getChildren() == null
                || ( rootNode.getChildren() != null && LinkMatrix.countBits( ( ( TreeNode ) rootNode.getChildren()
                        .getQuick( 0 ) ).getMask() ) < minExps ) ) {
            if ( rootNode.getLevel() >= minLinks ) this.insertCandidatesNode( rootNode );
            return;
        }

        /*
         * Not a leaf, hit the children.
         */
        for ( int i = 0; i < rootNode.getChildren().size(); i++ ) {
            TreeNode childNode = ( TreeNode ) rootNode.getChildren().getQuick( i );
            if ( LinkMatrix.countBits( childNode.getMask() ) >= minExps ) {
                travel( childNode, minExps, minLinks );
            } else {
                if ( rootNode.getLevel() >= minLinks ) this.insertCandidatesNode( rootNode );
            }
        }
    }

}
