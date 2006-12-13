package ubic.gemma.analysis.linkAnalysis;



import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import ubic.basecode.dataStructure.Visitable;
import ubic.basecode.dataStructure.matrix.CompressedNamedBitMatrix;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;

public class FrequentLinkSetFinder {
	private class TreeNode {
	    public Object element = null;
	    public TreeNode firstChild = null;
	    public TreeNode nextSibling = null;
	    public TreeNode parent = null;
	    public TreeNode prevSibling = null;
	    public int level = 0;
	    public long mask[];
	    public TreeNode(Object element){
	    	this.element = element;
	    }
	}
	private HashMap<TreeNode,Set> sameSiblings = null;
    private GeneService geneService = null;
    private ExpressionExperimentService eeService = null;
    private CompressedNamedBitMatrix linkCount = null;
    private Vector allEE = null;
    private int Threshold = 3;
    private TreeNode root = null;
    private int shift = 100000;
    private int minNumInSet = 8;
    public static int nodeNum = 0; 
    private int pruned = 0;
    private int merged = 0;
    private Vector candidatesNodes = null; //a linked list
    public FrequentLinkSetFinder( CompressedNamedBitMatrix linkCount, int threshold, int minNumInSet) {
        super();
        this.linkCount = linkCount;
        Threshold = threshold;
        root = new TreeNode(new Long(0xFFFFFFFFFFFFFFFFL));
        root.level = 0;
        //shift = this.linkCount.rows() > this.linkCount.columns()?this.linkCount.rows():this.linkCount.columns();
        this.minNumInSet = minNumInSet;
        sameSiblings = new HashMap<TreeNode, Set>();
        candidatesNodes = new Vector();
    }

    private void travel(TreeNode rootNode, int expNum, int linkNum){
    	TreeNode subNode = rootNode.firstChild;
    	while(subNode != null){
    		if(countBits(subNode.mask) >= expNum){
    			if(subNode.level >= linkNum){
    				if(subNode.firstChild == null || (subNode.firstChild != null && countBits(subNode.firstChild.mask) < expNum)){
    					this.insertCandidatesNode(subNode);
    					return;
    				}
    			}
    			travel(subNode, expNum, linkNum);
    		}
    		subNode = subNode.nextSibling;
    	}
    }
    private void expand(TreeNode rootNode){
        TreeNode subBranchRootNode = rootNode.firstChild;
        
        while( subBranchRootNode != null){
        	//if(((Long)subBranchRootNode.element).longValue() == 462718468 && ((Long)rootNode.element).longValue() == 904818468 && rootNode.level == 2)
            	//this.pruned = this.pruned;
            TreeNode iter = subBranchRootNode.nextSibling;
            while(iter != null){
                long[] childMask = new long[iter.mask.length]; 
                for(int i = 0; i < childMask.length; i++) childMask[i] = iter.mask[i];
                boolean notCreatedNewOne = true;
                for(int i = 0; i < childMask.length; i++){
                	if(childMask[i] != (childMask[i] & subBranchRootNode.mask[i])){
                		childMask[i] = childMask[i] & subBranchRootNode.mask[i];
                		notCreatedNewOne = false;
                	}
                }
                TreeNode nextOne = iter.nextSibling;
                if(countBits(childMask) >= this.Threshold){
                	TreeNode newFacedNode = null;
                	if(notCreatedNewOne){
                		iter.prevSibling.nextSibling = iter.nextSibling;
                		if(iter.nextSibling != null)
                			iter.nextSibling.prevSibling = iter.prevSibling;
                		newFacedNode = iter;
                		newFacedNode.prevSibling = newFacedNode.nextSibling = null;
                		merged = merged + 1;
                	}else{
                        newFacedNode = new TreeNode(iter.element);
                        newFacedNode.mask = childMask;
                        nodeNum++;
                        if(nodeNum%10000 == 0)
                        	System.err.println(nodeNum + " " +  merged);
                	}
                	this.addTreeNodeInOrder(subBranchRootNode, newFacedNode,false);
                }
                iter = nextOne;//Use nextOne to save in case the merging broke the links
            }
            TreeNode tmpRecord = subBranchRootNode;
            if(subBranchRootNode.firstChild != null){
            	this.expand( subBranchRootNode);
            }
            subBranchRootNode = subBranchRootNode.nextSibling;
            
            if(tmpRecord.firstChild == null && tmpRecord.level <= this.minNumInSet){ //check the level and prune the nodes
            	if(subBranchRootNode != null) subBranchRootNode.prevSibling = tmpRecord.prevSibling;
            	if( tmpRecord.parent.firstChild == tmpRecord){
        				tmpRecord.parent.firstChild = subBranchRootNode;
            	}else{
            		tmpRecord.prevSibling.nextSibling = subBranchRootNode;
            	}

            	tmpRecord = tmpRecord.prevSibling = tmpRecord.nextSibling = null;
            	nodeNum = nodeNum - 1;
            	pruned = pruned + 1;
            	//if(pruned %10000 == 0) System.err.println("Pruned " + pruned);
            }
        }
        if(root.firstChild == null) System.err.println("Logic Error");
    }
    private void addTreeNodeInOrder(TreeNode parent, TreeNode childNode, boolean merged){
    	TreeNode iter = parent.firstChild;
    	childNode.parent = parent;
    	childNode.level = parent.level + 1;
    	if(iter == null){
    		parent.firstChild = childNode;
    		return;
    	}
    	int childNodeBits = countBits(childNode.mask);
    	while(iter != null){
    		int iterNodeBits = countBits(iter.mask);
    		if(iterNodeBits == childNodeBits){
    			if(merged && compare(childNode.mask, iter.mask)){
    				Set siblingSet = this.sameSiblings.get(iter);
    				if(siblingSet == null){
    					siblingSet = new HashSet<TreeNode>();
    					this.sameSiblings.put(iter,siblingSet);
    				}
    				siblingSet.add(childNode);
    				return;
    			}
    		}
    		if(iterNodeBits < childNodeBits){
    			childNode.nextSibling = iter;
    			if(iter.prevSibling != null){
    				iter.prevSibling.nextSibling = childNode;
    				childNode.prevSibling = iter.prevSibling;
    			}else{
    				parent.firstChild = childNode;
    			}
    			iter.prevSibling = childNode;
    			return;
    		}		
    		if(iter.nextSibling == null) break;
    		else iter = iter.nextSibling;
    	}
    	iter.nextSibling = childNode;
    	childNode.prevSibling = iter;
    	return;
    }
    private void insertCandidatesNode(TreeNode oneNode){
    	int bits = countBits(oneNode.mask);
    	int i;
    	for(i = 0; i < candidatesNodes.size(); i++){
    		TreeNode ele = (TreeNode)candidatesNodes.elementAt(i);
    		if(ele.level < oneNode.level || (ele.level == oneNode.level && countBits(ele.mask) < bits)){
    			candidatesNodes.insertElementAt(oneNode, i);
    			break;
    		}
    	}
    	if(i == candidatesNodes.size()) candidatesNodes.add(oneNode);
    }
    public void outputPath(TreeNode leafNode){
    	TreeNode iter = leafNode;
    	while(iter.parent != null){
    		long oneId = ((Long)iter.element).longValue();
            int row = (int)(oneId/shift);
            int col = (int)(oneId%shift);
        	Object geneId = this.linkCount.getRowName(row);
            String geneName1 = this.geneService.load(((Long)geneId).longValue()).getName();
        	geneId = this.linkCount.getColName(col);
            String geneName2 = this.geneService.load(((Long)geneId).longValue()).getName();
            System.err.print(" ("+geneName1 + " " + geneName2+") ");
    		//System.err.print(oneId + " ");
            iter = iter.parent;
    	}
    	for(int i = 0; i < leafNode.mask.length; i++){
    		for(int j = 0; j < CompressedNamedBitMatrix.DOUBLE_LENGTH; j++)
    			if((leafNode.mask[i]&(CompressedNamedBitMatrix.BIT1<<j)) != 0){
    		    	Object eeId = this.allEE.elementAt(j+i*CompressedNamedBitMatrix.DOUBLE_LENGTH);
    		    	ExpressionExperiment ee = this.eeService.findById((Long)eeId);
    		    	System.err.print(ee.getShortName() + " ");
    			}
    	}
    	System.err.println("");
    }
    public static int countBits(long[] mask){
    	int bits = 0;
    	for(int i = 0; i < mask.length; i++)
    		bits = bits + CompressedNamedBitMatrix.countBits(mask[i]);
    	return bits;
    }
    private boolean compare(long[] mask1, long mask2[]){
    	for(int i = 0; i < mask1.length; i++)
    		if(mask1[i] != mask2[i]) return false;
    	return true;
    }

    public void find(){
        for(int i = 0; i < this.linkCount.rows(); i++)
            for(int j = i+1; j < this.linkCount.columns(); j++){
            	if(!compare(this.linkCount.getAllBits( i, j ), this.linkCount.getAllBits( j,i )))
            		System.err.println(i + "  " + j);
                if(this.linkCount.bitCount( i, j ) >= this.Threshold){
                    TreeNode oneNode = new TreeNode(new Long((long)i*(long)shift+ j) );
                    oneNode.mask = this.linkCount.getAllBits(i, j);
                    nodeNum++;
                    this.addTreeNodeInOrder(root, oneNode,false);
                }
            }
        System.err.println(this.sameSiblings.size());
        this.expand( root);
        this.travel(root, 9, 9);
        System.err.println(this.candidatesNodes.size()+ " " + ((TreeNode)candidatesNodes.elementAt(0)).level);
        this.outputPath((TreeNode)candidatesNodes.elementAt(0));
        int mostBits = 0;
        TreeNode nodeWithMostBits = null;
        for(int i = 0; i < candidatesNodes.size(); i++){
        	TreeNode node = (TreeNode)candidatesNodes.elementAt(i);
        	if(countBits(node.mask) > mostBits){mostBits = countBits(node.mask);nodeWithMostBits = node;}
        }
        for(int i = 0; i < candidatesNodes.size(); i++){
        	TreeNode node = (TreeNode)candidatesNodes.elementAt(i);
        	if(mostBits == countBits(node.mask)){System.err.println(node.level + " ");this.outputPath(node);}
        }
    }
    public void output(int num){
    	
    }
    public void setGeneService(GeneService geneService){
    	this.geneService = geneService;
    }
    public void setEEService(ExpressionExperimentService eeService){
    	this.eeService = eeService;
    }
    public void setEEIndex(java.util.Vector vector){
    	this.allEE = vector;
    }
}
