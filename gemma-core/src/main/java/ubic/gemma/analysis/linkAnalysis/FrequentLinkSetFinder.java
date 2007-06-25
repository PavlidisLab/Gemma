package ubic.gemma.analysis.linkAnalysis;

import cern.colt.list.ObjectArrayList;
import ubic.basecode.dataStructure.matrix.CompressedNamedBitMatrix;

public class FrequentLinkSetFinder {
    /*
     * private class TreeNode { public Object element = null; public TreeNode firstChild = null; public TreeNode
     * nextSibling = null; public TreeNode parent = null; public TreeNode prevSibling = null; public int level = 0;
     * public long mask[]; public TreeNode(Object element){ this.element = element; } }
     */
    private int Threshold = 3;
    private TreeNode root = null;
    public static int nodeNum = 0;
    private int merged = 0;
    private ObjectArrayList candidatesNodes = null; // a linked list
    private LinkBitMatrixUtil linkMatrix = null;

    public FrequentLinkSetFinder( int threshold, LinkBitMatrixUtil linkMatrix) {
        super();
        Threshold = threshold;
        int num = ( int ) ( linkMatrix.getMatrix().getBitNum() / CompressedNamedBitMatrix.DOUBLE_LENGTH ) + 1;
        root = new TreeNode( 0, new long[num], null );
        root.level = 0;
        candidatesNodes = new ObjectArrayList();
        this.linkMatrix = linkMatrix;
    }

    private void travel( TreeNode rootNode, int minExps, int minLinks ) {
        if ( rootNode.child == null
                || ( rootNode.child != null && LinkBitMatrixUtil.countBits( ( ( TreeNode ) rootNode.child.getQuick( 0 ) ).mask ) < minExps ) ) {
            if ( rootNode.level >= minLinks ) this.insertCandidatesNode( rootNode );
            return;
        }
        for ( int i = 0; i < rootNode.child.size(); i++ ) {
            TreeNode childNode = ( TreeNode ) rootNode.child.getQuick( i );
            if ( LinkBitMatrixUtil.countBits( childNode.mask ) >= minExps ) {
                travel( childNode, minExps, minLinks );
            } else {
                if ( rootNode.level >= minLinks ) this.insertCandidatesNode( rootNode );
            }
        }
    }

    private void expand( TreeNode rootNode, ObjectArrayList siblings, int rootNodeIndex ) {
        if ( rootNode.id == 0 ) {
            System.err.println( "Logic Error" );
            System.exit( 0 );
        }
        long[] childMask = new long[rootNode.mask.length];
        ObjectArrayList child = new ObjectArrayList();
        int index = rootNodeIndex + 1;
        while ( index < siblings.size() ) {
            TreeNode iter = ( TreeNode ) siblings.getQuick( index );
            for ( int i = 0; i < childMask.length; i++ )
                childMask[i] = iter.mask[i];
            boolean mergedCondition = true;
            for ( int i = 0; i < childMask.length; i++ ) {
                if ( childMask[i] != ( childMask[i] & rootNode.mask[i] ) ) {
                    childMask[i] = childMask[i] & rootNode.mask[i];
                    mergedCondition = false;
                }
            }
            if ( LinkBitMatrixUtil.countBits( childMask ) >= this.Threshold ) {
                if ( mergedCondition ) {
                    siblings.remove( index );
                    child.add( iter );
                    iter.setLevel( rootNode.level + 1 );
                    iter.setParent( rootNode );
                    merged = merged + 1;
                } else {
                    long mask[] = new long[childMask.length];
                    for ( int i = 0; i < childMask.length; i++ )
                        mask[i] = childMask[i];
                    TreeNode newCreatedNode = new TreeNode( iter.id, mask, null );
                    newCreatedNode.setParent( rootNode );
                    newCreatedNode.setLevel( rootNode.level + 1 );
                    child.add( newCreatedNode );
                    nodeNum++;
                    if ( nodeNum % 10000 == 0 ) System.err.println( nodeNum + " " + merged );
                    index++;
                }
            }
        }
        if ( child.size() > 0 ) {
            child.sort();
            rootNode.setChild( child );
            for ( int i = 0; i < rootNode.child.size() - 1; i++ ) {
                TreeNode iter = ( TreeNode ) rootNode.child.getQuick( i );
                this.expand( iter, rootNode.child, i );
            }
        }
    }

    /*
     * private void expand(TreeNode rootNode){ TreeNode subBranchRootNode = rootNode.firstChild; long[] childMask = new
     * long[subBranchRootNode.mask.length]; while( subBranchRootNode != null){
     * //if(((Long)subBranchRootNode.element).longValue() == 462718468 && ((Long)rootNode.element).longValue() ==
     * 904818468 && rootNode.level == 2) //this.pruned = this.pruned; TreeNode iter = subBranchRootNode.nextSibling;
     * while(iter != null){ for(int i = 0; i < childMask.length; i++) childMask[i] = iter.mask[i]; boolean
     * mergedCondition = true; for(int i = 0; i < childMask.length; i++){ if(childMask[i] != (childMask[i] &
     * subBranchRootNode.mask[i])){ childMask[i] = childMask[i] & subBranchRootNode.mask[i]; mergedCondition = false; } }
     * TreeNode nextOne = iter.nextSibling; if(MetaLinkFinder.countBits(childMask) >= this.Threshold){ TreeNode
     * newCreatedNode = null; if(mergedCondition){ iter.prevSibling.nextSibling = iter.nextSibling; if(iter.nextSibling !=
     * null) iter.nextSibling.prevSibling = iter.prevSibling; newCreatedNode = iter; newCreatedNode.prevSibling =
     * newCreatedNode.nextSibling = null; merged = merged + 1; }else{ newCreatedNode = new TreeNode(iter.element);
     * newCreatedNode.mask = new long[childMask.length]; for(int i = 0; i < childMask.length; i++)
     * newCreatedNode.mask[i] = childMask[i]; nodeNum++; if(nodeNum%10000 == 0) System.err.println(nodeNum + " " +
     * merged); } this.addTreeNodeInOrder(subBranchRootNode, newCreatedNode, mergedCondition); } iter = nextOne;//Use
     * nextOne to save in case the merging broke the links } TreeNode tmpRecord = subBranchRootNode;
     * if(subBranchRootNode.firstChild != null){ this.expand( subBranchRootNode); } subBranchRootNode =
     * subBranchRootNode.nextSibling; if(tmpRecord.firstChild == null && tmpRecord.level <= this.minLinksInSet){ //check
     * the level and prune the nodes if(subBranchRootNode != null) subBranchRootNode.prevSibling =
     * tmpRecord.prevSibling; if( tmpRecord.parent.firstChild == tmpRecord){ tmpRecord.parent.firstChild =
     * subBranchRootNode; }else{ tmpRecord.prevSibling.nextSibling = subBranchRootNode; } tmpRecord =
     * tmpRecord.prevSibling = tmpRecord.nextSibling = null; nodeNum = nodeNum - 1; pruned = pruned + 1; //if(pruned
     * %10000 == 0) System.err.println("Pruned " + pruned); } } if(root.firstChild == null) System.err.println("Logic
     * Error"); } private void addTreeNodeInOrder(TreeNode parent, TreeNode childNode, boolean merged){ TreeNode iter =
     * parent.firstChild; childNode.parent = parent; childNode.level = parent.level + 1; if(iter == null){
     * parent.firstChild = childNode; return; } int childNodeBits = MetaLinkFinder.countBits(childNode.mask);
     * while(true){ int iterNodeBits = MetaLinkFinder.countBits(iter.mask); if(iterNodeBits <= childNodeBits){
     * childNode.nextSibling = iter; if(iter.prevSibling != null){ iter.prevSibling.nextSibling = childNode;
     * childNode.prevSibling = iter.prevSibling; }else{ parent.firstChild = childNode; } iter.prevSibling = childNode;
     * break; } if(iter.nextSibling == null) { iter.nextSibling = childNode; childNode.prevSibling = iter; break; } iter =
     * iter.nextSibling; } return; }
     */
    private void insertCandidatesNode( TreeNode oneNode ) {
        candidatesNodes.add( oneNode );
        /*
         * int bits = MetaLinkFinder.countBits(oneNode.mask); int i; for(i = 0; i < candidatesNodes.size(); i++){
         * TreeNode ele = (TreeNode)candidatesNodes.elementAt(i); if(ele.level < oneNode.level || (ele.level ==
         * oneNode.level && MetaLinkFinder.countBits(ele.mask) < bits)){ candidatesNodes.insertElementAt(oneNode, i);
         * break; } } if(i == candidatesNodes.size()) candidatesNodes.add(oneNode);
         */
    }

    public void outputPath( TreeNode leafNode ) {
        TreeNode iter = leafNode;
        System.err.print( leafNode.maskBits + "\t" );
        for ( int i = 0; i < leafNode.mask.length; i++ ) {
            for ( int j = 0; j < CompressedNamedBitMatrix.DOUBLE_LENGTH; j++ )
                if ( ( leafNode.mask[i] & ( CompressedNamedBitMatrix.BIT1 << j ) ) != 0 ) {
                    System.err.print( "1" );
                } else {
                    System.err.print( "0" );
                }
        }
        System.err.print( "\t" );

        while ( iter.parent != null ) {
            System.err.print( " (" + linkMatrix.getLinkName( iter.id ) + ") " );
            // System.err.print(oneId + " ");
            iter = iter.parent;
        }
        for ( int i = 0; i < leafNode.mask.length; i++ ) {
            for ( int j = 0; j < CompressedNamedBitMatrix.DOUBLE_LENGTH; j++ )
                if ( ( leafNode.mask[i] & ( CompressedNamedBitMatrix.BIT1 << j ) ) != 0 ) {
                    System.err.print( linkMatrix.getEEName( j + i * CompressedNamedBitMatrix.DOUBLE_LENGTH ) + " " );
                }
        }
        System.err.println( "" );
    }

    private ObjectArrayList getValidNodes() {
        ObjectArrayList validNodes = new ObjectArrayList();
        for ( int i = 0; i < linkMatrix.getMatrix().rows(); i++ )
            for ( int j = i + 1; j < linkMatrix.getMatrix().columns(); j++ ) {
                if ( linkMatrix.getMatrix().bitCount( i, j ) >= this.Threshold ) {
                    TreeNode oneNode = new TreeNode( LinkBitMatrixUtil.generateId( i, j ), linkMatrix.getMatrix()
                            .getAllBits( i, j ), null );
                    oneNode.mask = linkMatrix.getMatrix().getAllBits( i, j );
                    validNodes.add( oneNode );
                }
            }
        return validNodes;
    }

    public void find() {
        find( this.getValidNodes() );
    }

    public void find( ObjectArrayList validNodes ) {
        validNodes.sort();
        root.setChild( validNodes );
        nodeNum = nodeNum + validNodes.size();
        System.err.println( "Initalized " + nodeNum + " nodes" );
        // for(int i = 0; i < validNodes.size(); i++){
        // TreeNode iter = (TreeNode)validNodes.getQuick(i);
        // System.err.println(iter.id + " ------>" + MetaLinkFinder.getLinkName(iter.id));
        // }

        for ( int i = 0; i < root.child.size() - 1; i++ ) {
            TreeNode iter = ( TreeNode ) root.child.getQuick( i );
            iter.setParent( root );
            this.expand( iter, root.child, i );
        }
        this.travel( root, 7, 7 );
        this.candidatesNodes.sort();
        for ( int i = 0; i < candidatesNodes.size(); i++ ) {
            TreeNode node = ( TreeNode ) candidatesNodes.getQuick( i );
            this.outputPath( node );
        }
    }

    public void output( int num ) {

    }
}
