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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Stack;

import cern.colt.list.ObjectArrayList;

/**
 * TODO Document Me
 * 
 * @author xwan
 * @version $Id$
 */
public class LinkGraphClustering {

    /**
     * @param leafNodes
     * @param internalNodes
     * @param root
     */
    public static void collectTreeNodes( ObjectArrayList leafNodes, ObjectArrayList internalNodes, TreeNode root ) {
        assert ( leafNodes != null && internalNodes != null );
        // dept first search for leaf node order
        Stack<Object> stack = new Stack<Object>();
        stack.push( root );
        while ( !stack.empty() ) {
            TreeNode iter = ( TreeNode ) stack.pop();
            if ( iter.getChildren() == null ) // leaf node
                leafNodes.add( iter );
            else {
                internalNodes.add( iter );
                stack.push( iter.getChildren().getQuick( 1 ) );
                stack.push( iter.getChildren().getQuick( 0 ) );
            }
        }

        // breadth first search
        /*
         * int treeOrder = 0; Queue<TreeNode> queue = new LinkedList<TreeNode>(); queue.add(root); while(queue.size() !=
         * 0){ TreeNode iter = queue.remove(); if(iter.child == null) //leaf node leafNodes.add(iter); else{
         * iter.setOrder(treeOrder); treeOrder = treeOrder + 1; internalNodes.add(iter); queue.add(iter.child[0]);
         * queue.add(iter.child[1]); } }
         */
        return;
    }

    private ObjectArrayList eligibleNodes = new ObjectArrayList();
    private ObjectArrayList closedNodes = new ObjectArrayList();
    private int Threshold;
    private TreeNode fake = null;
    private int order = 0;
    private int nodeUpdates = 0;

    private LinkMatrix linkMatrix = null;

    /**
     * @param threshold
     * @param linkMatrixUtil
     */
    public LinkGraphClustering( int threshold, LinkMatrix linkMatrixUtil ) {
        Threshold = threshold;
        TreeNode.reSetSorting();
        this.linkMatrix = linkMatrixUtil;
    }

    /**
     * @param fileName
     */
    public void readTreeFromFile( String fileName ) {
        try {
            /* Create a file to write the serialized tree to. */
            /* Open the file and set to read objects from it. */
            FileInputStream istream = new FileInputStream( fileName );
            ObjectInputStream q = new ObjectInputStream( istream );
            /* Read a tree object, and all the subtrees */
            this.eligibleNodes = ( ObjectArrayList ) q.readObject();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * 
     */
    public void run() {
        this.init();
        run( ( ( 2 ) * this.Threshold ) );
    }

    /**
     * @param pre
     * @param treeNodes
     */
    public void saveClusters( String pre, ObjectArrayList treeNodes ) {
        String prefix = pre;
        if ( prefix == null || prefix.length() == 0 ) prefix = "cluster";
        for ( int i = 0; i < treeNodes.size(); i++ ) {
            TreeNode treeNode = ( TreeNode ) treeNodes.getQuick( i );
            saveToTreeViewFile( prefix + ( i + 1 ), treeNode );
        }
    }

    /**
     * @param fileName
     */
    public void saveToFile( String fileName ) {
        try {
            /* Create a file to write the serialized tree to. */
            FileOutputStream ostream = new FileOutputStream( fileName );
            /* Create the output stream */
            ObjectOutputStream p = new ObjectOutputStream( ostream );

            p.writeObject( this.eligibleNodes ); // Write the tree to the stream.
            p.flush();
            ostream.close(); // close the file.
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
    }

    /**
     * @param filePrefix
     * @param root
     */
    public void saveToTreeViewFile( String filePrefix, TreeNode root ) {
        try {
            FileWriter cdtOut = new FileWriter( new File( filePrefix + ".cdt" ) );
            FileWriter gtrOut = new FileWriter( new File( filePrefix + ".gtr" ) );
            ObjectArrayList leafNodes = new ObjectArrayList();
            ObjectArrayList internalNodes = new ObjectArrayList();
            HashMap<TreeNode, String> nodeNames = new HashMap<TreeNode, String>();

            collectTreeNodes( leafNodes, internalNodes, root );
            TreeNode.setSorting( TreeNode.ORDER );
            internalNodes.sort();
            TreeNode.reSetSorting();

            long[] missingMask = getMissingMask( leafNodes );

            String leafNodeNamePrefix = "Link";
            String internalNodeNamePrefix = "Node";
            String suffix = "X";
            for ( int i = 0; i < leafNodes.size(); i++ ) {
                TreeNode oneNode = ( TreeNode ) leafNodes.get( i );
                nodeNames.put( oneNode, leafNodeNamePrefix + i + suffix );
            }
            int maximalCommonBits = 0;
            for ( int i = 0; i < internalNodes.size(); i++ ) {
                TreeNode oneNode = ( TreeNode ) internalNodes.get( i );
                nodeNames.put( oneNode, internalNodeNamePrefix + i + suffix );
                if ( oneNode.getMaskBits() > maximalCommonBits ) maximalCommonBits = oneNode.getMaskBits();
            }
            // Generate cdt and gtr file
            // write the head
            cdtOut.write( "GID" + "\t" + "YORF" + "\t" + "NAME" + "\t" + "GWEIGHT" );
            for ( int i = 0; i < linkMatrix.getRawMatrix().getBitNum(); i++ ) {
                if ( LinkMatrix.checkBits( missingMask, i ) ) cdtOut.write( "\t" + linkMatrix.getEEName( i ) );
            }
            cdtOut.write( "\n" );
            for ( int i = 0; i < leafNodes.size(); i++ ) {
                TreeNode child = ( TreeNode ) leafNodes.get( i );
                cdtOut.write( nodeNames.get( child ) + "\t" + linkMatrix.getLinkName( child.getId() ) + "\t"
                        + linkMatrix.getLinkName( child.getId() ) + "\t" + 1 );
                for ( int j = 0; j < linkMatrix.getRawMatrix().getBitNum(); j++ ) {
                    if ( LinkMatrix.checkBits( missingMask, j ) ) {
                        if ( linkMatrix.checkEEConfirmation( child.getId(), j ) )
                            cdtOut.write( "\t" + 1 );
                        else
                            cdtOut.write( "\t" + 0 );
                    }
                }
                cdtOut.write( "\n" );

            }

            for ( int i = internalNodes.size() - 1; i >= 0; i-- ) {
                TreeNode oneNode = ( TreeNode ) internalNodes.get( i );
                String parent = nodeNames.get( oneNode );
                String leftChild = nodeNames.get( oneNode.getChildren().getQuick( 0 ) );
                String rightChild = nodeNames.get( oneNode.getChildren().getQuick( 1 ) );
                int bits = LinkMatrix.countBits( oneNode.getMask() );
                gtrOut.write( parent + "\t" + leftChild + "\t" + rightChild + "\t" + bits
                        / ( ( maximalCommonBits ) + 0.0001 ) + "\n" );

            }
            cdtOut.close();
            gtrOut.close();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * @param clusterNum
     * @return
     */
    public ObjectArrayList selectClusterBasedOnSize( int clusterNum ) {
        ObjectArrayList res = new ObjectArrayList();
        /*
         * TreeNode res = (TreeNode)this.eligibleNodes.get(0); int level = res.level; for(int i = 1; i <
         * this.eligibleNodes.size(); i++){ TreeNode iter = (TreeNode)this.eligibleNodes.get(i); if(iter.level > level){
         * level = iter.level; res = iter; } }
         */
        TreeNode.setSorting( TreeNode.LEVEL );
        this.eligibleNodes.sort();
        res.addAllOfFromTo( this.eligibleNodes, 0, clusterNum - 1 );
        TreeNode.reSetSorting();
        this.eligibleNodes.sort();
        return res;
    }

    /**
     * @param num
     * @return
     */
    public ObjectArrayList selectClustersToSave( int num ) {
        ObjectArrayList treeNodes = selectClusterBasedOnSize( num );
        TreeNode root = ( TreeNode ) treeNodes.getQuick( 0 );
        saveToTreeViewFile( "clusterSize", root );
        root = selectClusterWithMaximalBits( 10 );
        saveToTreeViewFile( "clusterBits", root );
        saveClusters( "cluster", treeNodes );
        return treeNodes;
    }

    /**
     * Find the root node of the cluster that contains the link with maximum occurrences in the database
     * 
     * @param level
     * @return
     */
    public TreeNode selectClusterWithMaximalBits( int level ) {
        /** *Get all leaf nodes and add them into the closed table******** */
        /** if maintaining the closed table to save all merged nodes, this search wouldn't be needed** */
        for ( int i = 0; i < this.eligibleNodes.size(); i++ ) {
            TreeNode oneNode = ( TreeNode ) this.eligibleNodes.get( i );
            if ( oneNode.getChildren() != null ) {
                ObjectArrayList a1 = new ObjectArrayList();
                ObjectArrayList a2 = new ObjectArrayList();
                collectTreeNodes( a1, a2, oneNode );
                this.closedNodes.addAllOfFromTo( a1, 0, a1.size() - 1 );
            }
        }
        TreeNode.setSorting( TreeNode.COMMONBITS );
        this.closedNodes.sort();
        TreeNode.reSetSorting();
        TreeNode res = ( TreeNode ) this.closedNodes.get( 0 );
        // get the root node
        while ( res.getParent() != null ) {
            res = res.getParent();
        }
        return res;
    }

    /**
     * @return
     */
    public TreeNode selectMaximalCluster() {
        TreeNode.setSorting( TreeNode.LEVEL );
        this.eligibleNodes.sort();
        TreeNode.reSetSorting();
        TreeNode res = ( TreeNode ) this.eligibleNodes.getQuick( 0 );
        this.eligibleNodes.sort();
        return res;
    }

    /**
     * 
     */
    public void testSerilizable() {
        // this.Threshold = 2;
        this.init( 1000, 1000 );
        this.run( this.Threshold );
        this.saveToFile( "tree.tmp" );
        // this.readTreeFromFile("tree.tmp");

    }

    /**
     * @return
     */
    private int cluster() {
        int indexOfNodeForMerging = this.findMergeNode();
        TreeNode nodeForMerging = ( TreeNode ) this.eligibleNodes.get( indexOfNodeForMerging );
        TreeNode parent = mergeNodes( nodeForMerging );
        if ( nodeForMerging.getCommonBits() < this.Threshold ) return nodeForMerging.getCommonBits();
        TreeNode pairedNode = nodeForMerging.getClosestNode();
        Integer indexOfPairedNode = -1;

        ObjectArrayList allAffectedNodes = new ObjectArrayList();

        for ( int i = 0; i < this.eligibleNodes.size(); i++ ) {
            TreeNode curNode = ( TreeNode ) this.eligibleNodes.get( i );
            if ( curNode.equals( nodeForMerging ) ) continue;
            if ( curNode.equals( pairedNode ) ) {
                indexOfPairedNode = i;
                continue;
            }
            if ( curNode.getClosestNode().equals( nodeForMerging ) || curNode.getClosestNode().equals( pairedNode ) )
                allAffectedNodes.add( curNode );
        }
        // remove child nodes
        closedNodes.add( this.eligibleNodes.get( indexOfNodeForMerging ) );
        closedNodes.add( this.eligibleNodes.get( indexOfPairedNode ) );
        if ( indexOfNodeForMerging > indexOfPairedNode ) {
            this.eligibleNodes.remove( indexOfNodeForMerging );
            this.eligibleNodes.remove( indexOfPairedNode );
        } else {
            this.eligibleNodes.remove( indexOfPairedNode );
            this.eligibleNodes.remove( indexOfNodeForMerging );
        }
        this.eligibleNodes.add( parent );
        this.eligibleNodes.sort();
        allAffectedNodes.add( parent );
        allAffectedNodes.sort();
        this.update( allAffectedNodes );
        return parent.getMaskBits();
    }

    /**
     * @param oneNode
     */
    private void findClosestOnes( TreeNode oneNode ) {
        if ( oneNode.getClosestNode() != fake ) return;
        TreeNode curNode = oneNode;
        TreeNode closestNode = fake;
        int bits = 0;
        for ( int i = 0; i < eligibleNodes.size(); i++ ) {
            TreeNode pairedNode = ( TreeNode ) eligibleNodes.get( i );
            if ( pairedNode.getMaskBits() < bits ) break; // No need to iterate further
            if ( pairedNode.equals( curNode ) ) continue;

            int pairedBits = LinkMatrix.overlapBits( curNode.getMask(), pairedNode.getMask() );

            if ( pairedBits > bits || ( pairedBits == bits && pairedNode.getLevel() > closestNode.getLevel() ) ) {
                if ( pairedBits > pairedNode.getCommonBits()
                        || ( pairedBits == pairedNode.getCommonBits() && pairedNode.getClosestNode().equals( curNode ) )
                        || ( pairedBits == pairedNode.getCommonBits() && curNode.getLevel() > pairedNode
                                .getClosestNode().getLevel() ) ) {
                    closestNode = pairedNode;
                    bits = pairedBits;
                }
            }
        }
        curNode.setClosestNode( closestNode );
        this.nodeUpdates = this.nodeUpdates + 1;
    }

    /**
     * @return
     */
    private int findMergeNode() {
        int index = 0;
        TreeNode mergedNode = ( TreeNode ) this.eligibleNodes.get( index );
        for ( int i = 1; i < this.eligibleNodes.size(); i++ ) {
            TreeNode curNode = ( TreeNode ) this.eligibleNodes.get( i );
            if ( curNode.getMaskBits() < mergedNode.getCommonBits() ) break; // No need to iterate further
            if ( curNode.getCommonBits() > mergedNode.getCommonBits()
                    || ( curNode.getCommonBits() == mergedNode.getCommonBits() && curNode.getLevel() > mergedNode
                            .getLevel() ) ) {
                index = i;
                mergedNode = curNode;
            }
        }
        return index;
    }

    /**
     * @param leafNodes
     * @return
     */
    private long[] getMissingMask( ObjectArrayList leafNodes ) {
        TreeNode oneNode = ( TreeNode ) leafNodes.get( 0 );
        long[] missingMask = new long[oneNode.getMask().length];
        for ( int i = 0; i < leafNodes.size(); i++ ) {
            oneNode = ( TreeNode ) leafNodes.get( i );
            missingMask = LinkMatrix.OR( missingMask, oneNode.getMask() );
        }
        return missingMask;
    }

    /**
     * 
     */
    private void init() {
        init( linkMatrix.getRawMatrix().rows(), linkMatrix.getRawMatrix().columns() );
    }

    /**
     * @param rows
     * @param cols
     */
    private void init( int rows, int cols ) {
        for ( int i = 0; i < rows; i++ )
            for ( int j = i + 1; j < cols; j++ ) {
                if ( linkMatrix.getRawMatrix().bitCount( i, j ) >= this.Threshold && !linkMatrix.filter( i, j ) ) {
                    long[] mask = linkMatrix.getRawMatrix().getAllBits( i, j );
                    TreeNode oneNode = new TreeNode( linkMatrix.generateId( i, j ), mask, null );
                    if ( this.fake == null ) {
                        long[] fakeMask = new long[mask.length];
                        for ( int ii = 0; ii < fakeMask.length; ii++ )
                            fakeMask[ii] = 0;
                        this.fake = new TreeNode( 0, fakeMask, null );
                        this.fake.setClosestNode( fake );
                    }
                    oneNode.setClosestNode( fake );
                    eligibleNodes.add( oneNode );
                }
            }
        eligibleNodes.sort();
        for ( int i = 0; i < eligibleNodes.size(); i++ ) {
            TreeNode curNode = ( TreeNode ) eligibleNodes.get( i );
            this.findClosestOnes( curNode );
        }
    }

    /**
     * @param nodeForMerging
     * @return
     */
    private TreeNode mergeNodes( TreeNode nodeForMerging ) {
        TreeNode closestNode = nodeForMerging.getClosestNode();
        ObjectArrayList childNodes = new ObjectArrayList();
        childNodes.add( nodeForMerging );
        childNodes.add( closestNode );
        long mask[] = LinkMatrix.AND( nodeForMerging.getMask(), closestNode.getMask() );
        TreeNode parent = new TreeNode( 0, mask, childNodes );
        parent.setClosestNode( fake );
        nodeForMerging.setParent( parent );
        closestNode.setParent( parent );
        int level = closestNode.getLevel() > nodeForMerging.getLevel() ? closestNode.getLevel() : nodeForMerging
                .getLevel();
        level = level + 1;
        parent.setLevel( level );
        order = order + 1;
        parent.setOrder( order );
        /*
         * if(nodeForMerging.id != 0) System.err.print(nodeForMerging.id + "\t"); else
         * System.err.print(nodeForMerging.maskBits + "\t"); if(closestNode.id != 0) System.err.print(closestNode.id);
         * else System.err.print(closestNode.maskBits); System.err.println("(" + nodeForMerging.commonBits+","+
         * closestNode.commonBits + ")"); if(nodeForMerging.id == 36719047 && closestNode.id == 36719054){ parent =
         * parent; }
         */
        return parent;
    }

    /**
     * @param stopStringency
     */
    private void run( int stopStringency ) {
        int i = 0;
        while ( true ) {
            if ( i % 60 == 0 ) {
                System.err.println();
                System.err.print( i + "\t" + this.eligibleNodes.size() + "\t" );
            }
            int bits = this.cluster();
            if ( bits < stopStringency ) break;
            System.err.print( bits + " " );
            i++;
        }
        System.err.println();
        System.err.println( "Total Updates = " + this.nodeUpdates );
    }

    /**
     * @param affectedNodes
     */
    private void update( ObjectArrayList affectedNodes ) {
        for ( int i = 0; i < affectedNodes.size(); i++ ) {
            TreeNode curNode = ( TreeNode ) affectedNodes.get( i );
            curNode.setClosestNode( this.fake );
        }
        for ( int i = 0; i < affectedNodes.size(); i++ ) {
            TreeNode curNode = ( TreeNode ) affectedNodes.get( i );
            this.findClosestOnes( curNode );
        }
    }

}
