package ubic.gemma.analysis.linkAnalysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import cern.colt.list.ObjectArrayList;

public class LinkGraphClustering{
    private ObjectArrayList eligibleNodes = new ObjectArrayList();
    private ObjectArrayList closedNodes = new ObjectArrayList();
    private int Threshold;
    private TreeNode fake = null;
    private int order = 0;
    private int nodeUpdates = 0;
    private LinkBitMatrixUtil linkMatrix = null;
    
    public LinkGraphClustering(int threshold, LinkBitMatrixUtil linkMatrix) {
        Threshold = threshold;
        TreeNode.reSetSorting();
        this.linkMatrix = linkMatrix;
    }
    private void findClosestOnes(TreeNode oneNode){
    	if(oneNode.closestNode != fake) return;
    	TreeNode curNode = oneNode;
    	TreeNode closestNode = fake;
    	int bits = 0;
    	for(int i = 0; i < eligibleNodes.size(); i++){
    		TreeNode pairedNode = (TreeNode)eligibleNodes.get(i);
    		if(pairedNode.maskBits < bits) break; //No need to iterate further
    		if(pairedNode.equals(curNode)) continue;

    		int pairedBits = LinkBitMatrixUtil.overlapBits(curNode.mask, pairedNode.mask);

    		if(pairedBits > bits|| (pairedBits == bits && pairedNode.level > closestNode.level)){
    			if( pairedBits > pairedNode.commonBits || (pairedBits == pairedNode.commonBits && pairedNode.closestNode.equals(curNode)) || (pairedBits == pairedNode.commonBits &&  curNode.level > pairedNode.closestNode.level))
    			{
    				closestNode = pairedNode;
    				bits = pairedBits;
    			}
    		}
    	}
    	curNode.setClosestNode(closestNode);
    	this.nodeUpdates = this.nodeUpdates + 1;
    }

    
    private TreeNode mergeNodes(TreeNode nodeForMerging){
    	TreeNode closestNode = nodeForMerging.closestNode;
    	ObjectArrayList childNodes = new ObjectArrayList();
    	childNodes.add(nodeForMerging);
    	childNodes.add(closestNode);
    	long mask[] = LinkBitMatrixUtil.AND(nodeForMerging.mask, closestNode.mask);
    	TreeNode parent = new TreeNode(0, mask, childNodes);
    	parent.setClosestNode(fake);
    	nodeForMerging.setParent(parent);
    	closestNode.setParent(parent);
    	int level = closestNode.level > nodeForMerging.level? closestNode.level:nodeForMerging.level;
    	level = level + 1;
    	parent.setLevel(level);
    	order = order + 1;
    	parent.setOrder(order);
    	/*
    	if(nodeForMerging.id != 0) System.err.print(nodeForMerging.id + "\t");
    	else System.err.print(nodeForMerging.maskBits + "\t");
    	if(closestNode.id != 0) System.err.print(closestNode.id);
    	else System.err.print(closestNode.maskBits);
    	System.err.println("(" + nodeForMerging.commonBits+","+ closestNode.commonBits + ")");
    	if(nodeForMerging.id == 36719047 && closestNode.id == 36719054){
    		parent = parent;
    	}
    	*/
    	return parent;
    }
    private void update(ObjectArrayList affectedNodes){
    	for(int i = 0; i < affectedNodes.size(); i++){
    		TreeNode curNode = (TreeNode)affectedNodes.get(i);
    		curNode.setClosestNode(this.fake);
    	}
    	for(int i = 0; i < affectedNodes.size(); i++){
    		TreeNode curNode = (TreeNode)affectedNodes.get(i);
    		this.findClosestOnes(curNode);
    	}
    }

    private int findMergeNode(){
    	int index = 0;
    	TreeNode mergedNode = (TreeNode)this.eligibleNodes.get(index);
    	for(int i = 1; i < this.eligibleNodes.size(); i++){
    		TreeNode curNode = (TreeNode) this.eligibleNodes.get(i);
    		if(curNode.maskBits < mergedNode.commonBits) break; //No need to iterate further
    		if(curNode.commonBits > mergedNode.commonBits || (curNode.commonBits == mergedNode.commonBits && curNode.level > mergedNode.level)){
    			index = i;
    			mergedNode = curNode;
    		}
    	}
    	return index;
    }
    public void saveToFile(String fileName){
    	try {
    		/* Create a file to write the serialized tree to. */
    		FileOutputStream ostream = new FileOutputStream(fileName);
    		/* Create the output stream */
    		ObjectOutputStream p = new ObjectOutputStream(ostream);

     		p.writeObject(this.eligibleNodes); // Write the tree to the stream.
    		p.flush();
    		ostream.close();    // close the file.
    	} catch (Exception ex) {
    		ex.printStackTrace();
    	}
    }
    public static void collectTreeNodes(ObjectArrayList leafNodes, ObjectArrayList internalNodes, TreeNode root){
    	assert(leafNodes != null && internalNodes != null);
    	//dept first search for leaf node order
    	Stack<Object> stack = new Stack<Object>();
    	stack.push(root);
    	while(!stack.empty()){
    		TreeNode iter = (TreeNode)stack.pop();
    		if(iter.child == null) //leaf node
    			leafNodes.add(iter);
    		else{
    			internalNodes.add(iter);
    			stack.push(iter.child.getQuick(1));
    			stack.push(iter.child.getQuick(0));
    		}
    	}

    	//breadth first search
    	/*
    	 *     	int treeOrder = 0;
    	 *         	Queue<TreeNode> queue = new LinkedList<TreeNode>();
    	queue.add(root);
    	while(queue.size() != 0){
    		TreeNode iter = queue.remove();
    		if(iter.child == null) //leaf node
    			leafNodes.add(iter);
    		else{
    			iter.setOrder(treeOrder);
    			treeOrder = treeOrder + 1;
    			internalNodes.add(iter);
    			queue.add(iter.child[0]);
    			queue.add(iter.child[1]);
    		}
    	}
    	*/
    	return;
    }
    public ObjectArrayList selectClusterBasedOnSize(int clusterNum){
    	ObjectArrayList res = new ObjectArrayList(); 
    	/*TreeNode res = (TreeNode)this.eligibleNodes.get(0);
    	int level = res.level;
    	for(int i = 1; i < this.eligibleNodes.size(); i++){
    		TreeNode iter = (TreeNode)this.eligibleNodes.get(i);
    		if(iter.level > level){
    			level = iter.level;
    			res = iter;
    		}
    	}*/
    	TreeNode.setSorting(TreeNode.LEVEL);
    	this.eligibleNodes.sort();
    	res.addAllOfFromTo(this.eligibleNodes, 0, clusterNum - 1);
    	TreeNode.reSetSorting();
    	this.eligibleNodes.sort();
    	return res;
    }
    public TreeNode selectMaximalCluster(){
    	TreeNode.setSorting(TreeNode.LEVEL);
    	this.eligibleNodes.sort();
    	TreeNode.reSetSorting();
    	TreeNode res = (TreeNode)this.eligibleNodes.getQuick(0);
    	this.eligibleNodes.sort();
    	return res;
    }
    //Find the root node of the cluster that contains the link with maximum occurrences in the database
    public TreeNode selectClusterWithMaximalBits(int level){
    	/***Get all leaf nodes and add them into the closed table*********/
    	/** if maintaining the closed table to save all merged nodes, this search wouldn't be needed***/
    	for(int i = 0; i < this.eligibleNodes.size(); i++){
    		TreeNode oneNode = (TreeNode)this.eligibleNodes.get(i);
    		if(oneNode.child != null){
    			ObjectArrayList a1 = new ObjectArrayList();
    			ObjectArrayList a2 = new ObjectArrayList();
    			collectTreeNodes(a1, a2, oneNode);
    			this.closedNodes.addAllOfFromTo(a1, 0, a1.size() - 1);
    		}
    	}
    	TreeNode.setSorting(TreeNode.COMMONBITS);
    	this.closedNodes.sort();
    	TreeNode.reSetSorting();
    	TreeNode res = (TreeNode)this.closedNodes.get(0);
    	//get the root node
    	while(res.parent != null){
    		res = res.parent;
    	}
    	return res;
    }
    public ObjectArrayList selectClustersToSave(int num){
    	ObjectArrayList treeNodes = selectClusterBasedOnSize(num);
    	TreeNode root = (TreeNode)treeNodes.getQuick(0);
    	saveToTreeViewFile("clusterSize", root);
    	root = selectClusterWithMaximalBits(10);
    	saveToTreeViewFile("clusterBits", root);
    	saveClusters("cluster", treeNodes);
    	return treeNodes;
    }
    public void saveClusters(String pre, ObjectArrayList treeNodes){
    	String prefix = pre;
    	if(prefix == null || prefix.length() == 0) prefix = "cluster";
    	for(int i = 0; i < treeNodes.size(); i++){
    		TreeNode treeNode = (TreeNode)treeNodes.getQuick(i);
    		saveToTreeViewFile(prefix+(i+1), treeNode);
    	}
    }
    private long[] getMissingMask(ObjectArrayList leafNodes){
    	TreeNode oneNode = (TreeNode)leafNodes.get(0);
    	long[] missingMask = new long[oneNode.mask.length];
    	for(int i = 0; i < leafNodes.size(); i++){
    		oneNode = (TreeNode)leafNodes.get(i);
    		missingMask = LinkBitMatrixUtil.OR(missingMask, oneNode.mask);
    	}
    	return missingMask;
    }
    public void saveToTreeViewFile(String filePrefix, TreeNode root){
    	try{
    		FileWriter cdtOut = new FileWriter(new File(filePrefix+".cdt"));
    		FileWriter gtrOut = new FileWriter(new File(filePrefix+".gtr"));
    		ObjectArrayList leafNodes = new ObjectArrayList();
    		ObjectArrayList internalNodes = new ObjectArrayList();
    		HashMap<TreeNode, String> nodeNames = new HashMap<TreeNode, String>();

    		collectTreeNodes(leafNodes, internalNodes, root);
    		TreeNode.setSorting(TreeNode.ORDER);
    		internalNodes.sort();
    		TreeNode.reSetSorting();
    		
    		long[] missingMask = getMissingMask(leafNodes);

    		String leafNodeNamePrefix = "Link";
    		String internalNodeNamePrefix = "Node";
    		String suffix = "X";
    		for(int i = 0; i< leafNodes.size(); i++){
    			TreeNode oneNode = (TreeNode)leafNodes.get(i);
    			nodeNames.put(oneNode, leafNodeNamePrefix + i + suffix);
    		}
    		int maximalCommonBits = 0;
    		for(int i = 0; i< internalNodes.size(); i++){
    			TreeNode oneNode = (TreeNode)internalNodes.get(i);
    			nodeNames.put(oneNode, internalNodeNamePrefix + i + suffix);
    			if(oneNode.maskBits > maximalCommonBits)
    				maximalCommonBits = oneNode.maskBits;
    		}
    		//Generate cdt and gtr file
    		//write the head
    		cdtOut.write("GID"+"\t"+"YORF"+"\t"+"NAME"+"\t"+"GWEIGHT");
    		for(int i = 0; i < linkMatrix.getMatrix().getBitNum(); i++){
    			if(LinkBitMatrixUtil.checkBits(missingMask, i))
    				cdtOut.write("\t"+linkMatrix.getEEName(i));
    		}
    		cdtOut.write("\n");
    		for(int i = 0; i < leafNodes.size(); i++){
    			TreeNode child = (TreeNode)leafNodes.get(i);
				cdtOut.write(nodeNames.get(child) + "\t" + linkMatrix.getLinkName(child.id) + "\t" + linkMatrix.getLinkName(child.id)+"\t"+ 1);    			
				for(int j = 0; j < linkMatrix.getMatrix().getBitNum(); j++){
					if(LinkBitMatrixUtil.checkBits(missingMask, j))
					{
						if(linkMatrix.checkEEConfirmation(child.id,j))
							cdtOut.write("\t" + 1);
						else
							cdtOut.write("\t" + 0);
					}
				}
				cdtOut.write("\n");

    		}
    		
    		for(int i = internalNodes.size() - 1; i >= 0; i--){
    			TreeNode oneNode = (TreeNode)internalNodes.get(i);
    			String parent = nodeNames.get(oneNode);
    			String leftChild = nodeNames.get(oneNode.child.getQuick(0));
    			String rightChild = nodeNames.get(oneNode.child.getQuick(1));
    			int bits = LinkBitMatrixUtil.countBits(oneNode.mask);
    			gtrOut.write(parent+"\t"+leftChild+"\t"+rightChild+"\t"+(double)bits/(((double)maximalCommonBits)+0.0001)+"\n");
    			/*
    			for(int childIndex = 0; childIndex < oneNode.child.length; childIndex++){
    				if(oneNode.child[childIndex].child == null){
    					TreeNode child = oneNode.child[childIndex]; 
    					cdtOut.write(nodeNames.get(child) + "\t" + MetaLinkFinder.getLinkName(child.id) + "\t" + MetaLinkFinder.getLinkName(child.id)+"_"+MetaLinkFinder.countBits(child.mask) + "\t"+ 1);    			
    					for(int j = 0; j < MetaLinkFinder.linkCount.getBitNum(); j++){
    						if(MetaLinkFinder.checkBits(missingMask, j))
    						{
    							if(MetaLinkFinder.checkEEConfirmation(child.id,j))
    								cdtOut.write("\t" + 1);
    							else
    								cdtOut.write("\t" + 0);
    						}
    					}
    					cdtOut.write("\n");
    				}
    			}
    			*/
    		}
    		cdtOut.close();
    		gtrOut.close();
    	}catch(Exception e){
            e.printStackTrace();
        }
    }
    public void readTreeFromFile(String fileName){
    	try {
    		/* Create a file to write the serialized tree to. */
    		/* Open the file and set to read objects from it. */
    		FileInputStream istream = new FileInputStream(fileName);
    		ObjectInputStream q = new ObjectInputStream(istream);
    		/* Read a tree object, and all the subtrees */
    		this.eligibleNodes = (ObjectArrayList)q.readObject();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    public void testSerilizable(){
    	//this.Threshold = 2;
    	this.init(1000,1000);
    	this.run(this.Threshold);
    	this.saveToFile("tree.tmp");
    	//this.readTreeFromFile("tree.tmp");
    	
    }
    private void init(int rows, int cols){
        for(int i = 0; i < rows; i++)
            for(int j = i+1; j < cols; j++){
                if(linkMatrix.getMatrix().bitCount( i, j ) >= this.Threshold && !linkMatrix.filter(i, j)){
                	long[] mask = linkMatrix.getMatrix().getAllBits(i, j);
                    TreeNode oneNode = new TreeNode(LinkBitMatrixUtil.generateId(i,j), mask, null );
                    if(this.fake == null){
                        long[] fakeMask = new long[mask.length];
                        for(int ii = 0; ii < fakeMask.length; ii++) fakeMask[ii] = 0;
                        this.fake = new TreeNode(0, fakeMask, null);
                        this.fake.setClosestNode(fake);
                    }
                    oneNode.setClosestNode(fake);        
                    eligibleNodes.add(oneNode);
                }
            }
       	eligibleNodes.sort();
    	for(int i = 0; i < eligibleNodes.size(); i++){
    		TreeNode curNode = (TreeNode)eligibleNodes.get(i);
    		this.findClosestOnes(curNode);
    	}
    }
    private void init(){
    	init(linkMatrix.getMatrix().rows(), linkMatrix.getMatrix().columns());
    }

    public void run(){
    	this.init();
    	run((int)((2)*this.Threshold));
    }
    private void run(int stopStringency){
    	int i = 0;
    	while(true){
    		if(i%60 == 0){
    			System.err.println();
    			System.err.print(i+"\t" + this.eligibleNodes.size()+ "\t");
    		}
    		int bits = this.clustering();
    		if( bits < stopStringency) break;
    		System.err.print(bits + " ");
    		i++;
    	}
    	System.err.println();
    	System.err.println("Total Updates = " + this.nodeUpdates);
    }
    private int clustering(){
    	int indexOfNodeForMerging = this.findMergeNode();
    	TreeNode nodeForMerging = (TreeNode)this.eligibleNodes.get(indexOfNodeForMerging);
    	TreeNode parent = mergeNodes(nodeForMerging);
    	if(nodeForMerging.commonBits < this.Threshold) return nodeForMerging.commonBits;
    	TreeNode pairedNode  = nodeForMerging.closestNode;
    	Integer indexOfPairedNode = -1;
    	
    	ObjectArrayList allAffectedNodes = new ObjectArrayList();;
    	for(int i = 0; i < this.eligibleNodes.size(); i++){
    		TreeNode curNode = (TreeNode)this.eligibleNodes.get(i);
    		if(curNode.equals(nodeForMerging)) continue;
    		if(curNode.equals(pairedNode)){
    			indexOfPairedNode = i;
    			continue;
    		}
    		if(curNode.closestNode.equals(nodeForMerging) || curNode.closestNode.equals(pairedNode))
    			allAffectedNodes.add(curNode);
    	}
    	//remove child nodes
    	closedNodes.add(this.eligibleNodes.get(indexOfNodeForMerging));
    	closedNodes.add(this.eligibleNodes.get(indexOfPairedNode));
    	if(indexOfNodeForMerging > indexOfPairedNode){
    		this.eligibleNodes.remove(indexOfNodeForMerging);
    		this.eligibleNodes.remove(indexOfPairedNode);
    	}else{
    		this.eligibleNodes.remove(indexOfPairedNode);
    		this.eligibleNodes.remove(indexOfNodeForMerging);
    	}
    	this.eligibleNodes.add(parent);
    	this.eligibleNodes.sort();
    	allAffectedNodes.add(parent);
    	allAffectedNodes.sort();
    	this.update(allAffectedNodes);
    	return parent.maskBits;
    }

}
