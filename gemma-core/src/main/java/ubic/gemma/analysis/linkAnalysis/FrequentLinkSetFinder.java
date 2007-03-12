package ubic.gemma.analysis.linkAnalysis;



import java.io.File;
import java.io.FileWriter;
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
    private int Threshold = 3;
    private TreeNode root = null;
    private int minLinksInSet = 8;
    public static int nodeNum = 0; 
    private int pruned = 0;
    private int merged = 0;
    private Vector candidatesNodes = null; //a linked list
    public FrequentLinkSetFinder( int threshold, int minLinksInSet) {
        super();
        Threshold = threshold;
        root = new TreeNode(new Long(0xFFFFFFFFFFFFFFFFL));
        root.level = 0;
        this.minLinksInSet = minLinksInSet;
        candidatesNodes = new Vector();
    }

    private void travel(TreeNode rootNode, int minExps, int minLinks){
    	TreeNode subNode = rootNode.firstChild;
    	while(subNode != null){
    		if(MetaLinkFinder.countBits(subNode.mask) >= minExps){
    			if(subNode.level >= minLinks){
    				if(subNode.firstChild == null || (subNode.firstChild != null && MetaLinkFinder.countBits(subNode.firstChild.mask) < minExps)){
    					this.insertCandidatesNode(subNode);
    					return;
    				}
    			}
    			travel(subNode, minExps, minLinks);
    		}
    		subNode = subNode.nextSibling;
    	}
    }
    private void expand(TreeNode rootNode){
        TreeNode subBranchRootNode = rootNode.firstChild;
        long[] childMask = new long[subBranchRootNode.mask.length];
        while( subBranchRootNode != null){
        	//if(((Long)subBranchRootNode.element).longValue() == 462718468 && ((Long)rootNode.element).longValue() == 904818468 && rootNode.level == 2)
            	//this.pruned = this.pruned;
            TreeNode iter = subBranchRootNode.nextSibling;
            while(iter != null){
                for(int i = 0; i < childMask.length; i++) childMask[i] = iter.mask[i];
                boolean mergedCondition = true;
                for(int i = 0; i < childMask.length; i++){
                	if(childMask[i] != (childMask[i] & subBranchRootNode.mask[i])){
                		childMask[i] = childMask[i] & subBranchRootNode.mask[i];
                		mergedCondition = false;
                	}
                }
                TreeNode nextOne = iter.nextSibling;
                if(MetaLinkFinder.countBits(childMask) >= this.Threshold){
                	TreeNode newCreatedNode = null;
                	if(mergedCondition){
                		iter.prevSibling.nextSibling = iter.nextSibling;
                		if(iter.nextSibling != null)
                			iter.nextSibling.prevSibling = iter.prevSibling;
                		newCreatedNode = iter;
                		newCreatedNode.prevSibling = newCreatedNode.nextSibling = null;
                		merged = merged + 1;
                	}else{
                		newCreatedNode = new TreeNode(iter.element);
                		newCreatedNode.mask = new long[childMask.length];
                		for(int i = 0; i < childMask.length; i++) newCreatedNode.mask[i] = childMask[i];
                        nodeNum++;
                        if(nodeNum%10000 == 0)
                        	System.err.println(nodeNum + " " +  merged);
                	}
                	this.addTreeNodeInOrder(subBranchRootNode, newCreatedNode, mergedCondition);
                }
                iter = nextOne;//Use nextOne to save in case the merging broke the links
            }
            TreeNode tmpRecord = subBranchRootNode;
            if(subBranchRootNode.firstChild != null){
            	this.expand( subBranchRootNode);
            }
            subBranchRootNode = subBranchRootNode.nextSibling;
            
            if(tmpRecord.firstChild == null && tmpRecord.level <= this.minLinksInSet){ //check the level and prune the nodes
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
    	
    	int childNodeBits = MetaLinkFinder.countBits(childNode.mask);
    	while(true){
    		int iterNodeBits = MetaLinkFinder.countBits(iter.mask);
    		if(iterNodeBits <= childNodeBits){
    			childNode.nextSibling = iter;
    			if(iter.prevSibling != null){
    				iter.prevSibling.nextSibling = childNode;
    				childNode.prevSibling = iter.prevSibling;
    			}else{
    				parent.firstChild = childNode;
    			}
    			iter.prevSibling = childNode;
    			break;
    		}		
    		
    		if(iter.nextSibling == null) {
    	    	iter.nextSibling = childNode;
    	    	childNode.prevSibling = iter;
    			break;
    		}
    		iter = iter.nextSibling;
    	}
    	return;
    }
    private void insertCandidatesNode(TreeNode oneNode){
    	int bits = MetaLinkFinder.countBits(oneNode.mask);
    	int i;
    	for(i = 0; i < candidatesNodes.size(); i++){
    		TreeNode ele = (TreeNode)candidatesNodes.elementAt(i);
    		if(ele.level < oneNode.level || (ele.level == oneNode.level && MetaLinkFinder.countBits(ele.mask) < bits)){
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
            System.err.print(" ("+MetaLinkFinder.getLinkName(oneId)+") ");
    		//System.err.print(oneId + " ");
            iter = iter.parent;
    	}
    	for(int i = 0; i < leafNode.mask.length; i++){
    		for(int j = 0; j < CompressedNamedBitMatrix.DOUBLE_LENGTH; j++)
    			if((leafNode.mask[i]&(CompressedNamedBitMatrix.BIT1<<j)) != 0){
    		    	System.err.print(MetaLinkFinder.getEEName(j+i*CompressedNamedBitMatrix.DOUBLE_LENGTH) + " ");
    			}
    	}
    	System.err.println("");
    }
    public void saveLinkMatrix(String outFile, int stringency){
        try{
            FileWriter out = new FileWriter(new File(outFile));
            for(int i = 0; i < MetaLinkFinder.linkCount.rows(); i++){
            	if(i%1000 == 0) System.err.println(i + " -> " + MetaLinkFinder.linkCount.rows());
                for(int j = i+1; j < MetaLinkFinder.linkCount.columns(); j++){
                    if(MetaLinkFinder.linkCount.bitCount( i, j ) >= this.Threshold){
                        TreeNode oneNode = new TreeNode(MetaLinkFinder.generateId(i, j));
                        oneNode.mask = MetaLinkFinder.linkCount.getAllBits(i, j);
                        nodeNum++;
                        this.addTreeNodeInOrder(root, oneNode,true);
                    }
                }
        	}
            System.err.println("Initalized " + nodeNum + " nodes");
            HashMap<Object,Integer> indexMap = new HashMap<Object, Integer>();
            int index = 0;
            TreeNode iter = root.firstChild;
            while(iter != null){
            	indexMap.put(iter.element, new Integer(index));
            	index = index + 1;
            	iter = iter.nextSibling;
            }
            TreeNode curNode = root.firstChild;
            while(curNode != null){
            	iter = curNode.nextSibling;
            	int rowIndex = indexMap.get(curNode.element);
            	if(rowIndex%5000 == 0) System.err.println(rowIndex);
            	while(iter != null){
            		int commonBits = MetaLinkFinder.overlapBits(curNode.mask, iter.mask);
            		if(commonBits >= stringency){
            			int colIndex = indexMap.get(iter.element);
            			out.write( rowIndex + "\t"+colIndex+"\t" + commonBits + "\n" );
            			out.write( colIndex + "\t"+rowIndex+"\t" + commonBits + "\n" );
            		}
            		iter = iter.nextSibling;
            	}
            	curNode = curNode.nextSibling;
            }
            out.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void find(){
        for(int i = 0; i < MetaLinkFinder.linkCount.rows(); i++)
            for(int j = i+1; j < MetaLinkFinder.linkCount.columns(); j++){
                if(MetaLinkFinder.linkCount.bitCount( i, j ) >= this.Threshold){
                    TreeNode oneNode = new TreeNode(MetaLinkFinder.generateId(i, j) );
                    oneNode.mask = MetaLinkFinder.linkCount.getAllBits(i, j);
                    nodeNum++;
                    this.addTreeNodeInOrder(root, oneNode,true);
                }
            }
        System.err.println("Initalized " + nodeNum + " nodes");
        this.expand( root);
        this.travel(root, 17, 7);
        System.err.println(this.candidatesNodes.size()+ " " + ((TreeNode)candidatesNodes.elementAt(0)).level);
        this.outputPath((TreeNode)candidatesNodes.elementAt(0));
        int mostBits = 0;
        TreeNode nodeWithMostBits = null;
        for(int i = 0; i < candidatesNodes.size(); i++){
        	TreeNode node = (TreeNode)candidatesNodes.elementAt(i);
        	if(MetaLinkFinder.countBits(node.mask) > mostBits){mostBits = MetaLinkFinder.countBits(node.mask);nodeWithMostBits = node;}
        }
        for(int i = 0; i < candidatesNodes.size(); i++){
        	TreeNode node = (TreeNode)candidatesNodes.elementAt(i);
        	if(mostBits == MetaLinkFinder.countBits(node.mask)){System.err.println(node.level + " ");this.outputPath(node);}
        }
    }
    public void output(int num){
    	
    }
}
