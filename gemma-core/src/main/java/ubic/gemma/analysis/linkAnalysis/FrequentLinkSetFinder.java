package ubic.gemma.analysis.linkAnalysis;



import java.util.Collection;
import java.util.HashSet;

import ubic.basecode.dataStructure.matrix.CompressedNamedBitMatrix;
import ubic.basecode.dataStructure.tree.TreeNode;

public class FrequentLinkSetFinder {
    private CompressedNamedBitMatrix linkCount = null;
    private int Threshold = 3;
    private TreeNode root = null;
    int shift = 50000;
    public FrequentLinkSetFinder( CompressedNamedBitMatrix linkCount, int threshold ) {
        super();
        this.linkCount = linkCount;
        Threshold = threshold;
        root = new TreeNode(new Long(0xFFFFFFFFFFFFFFFFL));
        shift = this.linkCount.rows() > this.linkCount.columns()?this.linkCount.rows():this.linkCount.columns();
    }
    private void expand(TreeNode rootNode, HashSet<Long> candidates){
        TreeNode cur = rootNode.getFirstChild();
        while( cur != null){
            long id = ((Long)cur.getElement()).longValue();
            int curRow = (int)(id/shift);
            int curCol = (int)(id%shift);
            candidates.remove( cur.getElement() );
            HashSet curCandidates = new HashSet<Long>();
            Object[] idArray = candidates.toArray();
            TreeNode curNode = null;
            for(int i = 0; i < idArray.length; i++){
                long oneId = ((Long)idArray[i]).longValue();
                int row = (int)(oneId/shift);
                int col = (int)(oneId%shift);
                if(this.linkCount.overlap( curRow, curCol, row, col) >= this.Threshold){
                    curCandidates.add( (Long)idArray[i] );
                    TreeNode oneNode = new TreeNode(idArray[i]);
                    if(curNode == null){
                        curNode = oneNode;
                        cur.setFirstChild( curNode );
                    }else{
                        curNode.setNextSibling( oneNode );
                        curNode = oneNode;
                    }
                }
            }
            this.expand( cur, curCandidates );
            cur = cur.getNextSibling();
        }
    }
    public void find(){
        HashSet <Long> startSet = new HashSet<Long>();
        
        TreeNode cur = null;
        for(int i = 0; i < this.linkCount.rows(); i++)
            for(int j = 0; j < this.linkCount.columns(); j++)
                if(this.linkCount.bitCount( i, j ) >= this.Threshold){
                    startSet.add( new Long((long)i*(long)shift+ j) );
                    TreeNode oneNode = new TreeNode(new Long((long)i*(long)shift+ j) );
                    if(cur == null){
                        root.setFirstChild( oneNode );
                        cur = oneNode;
                    }else{
                        cur.setNextSibling( oneNode );
                        cur = oneNode;
                    }
                }
        this.expand( root, startSet );
    }
}
