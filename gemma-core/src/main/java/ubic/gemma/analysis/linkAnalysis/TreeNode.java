package ubic.gemma.analysis.linkAnalysis;

import java.io.Serializable;

import cern.colt.list.ObjectArrayList;


public class TreeNode implements Comparable<TreeNode>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7454412470040843963L;
	public static final int MASKBITS = 1;
	public static final int COMMONBITS = 2;
	public static final int LEVEL = 3;
	public static final int ORDER = 4;
	private static int SORTING = MASKBITS; 
	public long id;
	public Integer maskBits = 0;
	public long [] mask;
	public TreeNode closestNode = null;
	public Integer commonBits = 0;
	public ObjectArrayList child;
	public TreeNode parent = null;
	public Integer level = 0;
	public Integer order = 0; //for tree generation

    public TreeNode(long id, long[] mask, ObjectArrayList child) {
    	this.id = id;
    	this.child = child;
    	this.mask = mask;
    	this.maskBits = LinkBitMatrixUtil.countBits(this.mask);
    }
    public int compareTo( TreeNode o ) {
    	int res = 0;    	
    	switch(TreeNode.SORTING){
    	case TreeNode.MASKBITS:
    		res = maskBits.compareTo(o.maskBits)*(-1);
    		break;
    	case TreeNode.COMMONBITS:
    		res = commonBits.compareTo(o.commonBits)*(-1);
    		break;
    	case TreeNode.LEVEL:
    		res = level.compareTo(o.level)*(-1);
    		break;
		case TreeNode.ORDER:
			res = order.compareTo(o.order)*(-1);
			break;
		}
        return res;
    	//return maskBits.compareTo(o.maskBits)*(-1);
    }
    public static void setSorting(int sorting){
    	TreeNode.SORTING = sorting;
    }
    public static void reSetSorting(){
    	TreeNode.SORTING = TreeNode.MASKBITS;
    }

    public void setClosestNode(TreeNode closestNode){
    	this.closestNode = closestNode;
    	commonBits = LinkBitMatrixUtil.overlapBits(mask, closestNode.mask);
    }
    public void setParent(TreeNode parent){
    	this.parent = parent;
    }
    public void setChild(ObjectArrayList child){
    	this.child = child;
    }
    public void setLevel(int level){
     	this.level = level;
    }
    public void setOrder(int order){
     	this.order = order;
    }

}
