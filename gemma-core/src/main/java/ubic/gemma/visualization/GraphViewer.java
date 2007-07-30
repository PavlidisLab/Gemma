package ubic.gemma.visualization;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import com.j_spaces.obf.cp;
import com.sun.org.apache.bcel.internal.generic.MULTIANEWARRAY;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.layout.CircleLayout;
import prefuse.action.layout.Layout;
import prefuse.action.layout.RandomLayout;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.action.layout.graph.RadialTreeLayout;
import prefuse.activity.Activity;
import prefuse.controls.DragControl;
import prefuse.controls.FocusControl;
import prefuse.controls.NeighborHighlightControl;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.ColorMap;
import prefuse.util.FontLib;
import prefuse.util.PrefuseLib;
import prefuse.util.force.ForceSimulator;
import prefuse.util.io.IOLib;
import prefuse.util.io.SimpleFileFilter;
import prefuse.util.ui.JForcePanel;
import prefuse.visual.DecoratorItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;

import cern.colt.list.ObjectArrayList;

import ubic.gemma.analysis.linkAnalysis.LinkBitMatrixUtil;
import ubic.gemma.analysis.linkAnalysis.LinkGraphClustering;
import ubic.gemma.analysis.linkAnalysis.TreeNode;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.ontology.OntologyTerm;

public class GraphViewer implements PropertyChangeListener,ActionListener, WindowListener  {
	private JFileChooser ed = null;
	private Map<OntologyTerm, Integer> goTermsCounter = null;
	private static String SelectedGoTerm = "";
	
	private Visualization vis = new Visualization();;
	private Display display = null;
	private JFrame frame = null;
	private int currentGraphIndex = 0;
	private JPanel dynamicPanel = null;
	private JFormattedTextField clusterIndex;
	private static boolean WINDOW_CLOSED = false;
	
	private JButton forceLayout, restart, circleLayout, radiaTreeLayout, exported, previous, next;

	
	private ObjectArrayList clusterRootNodes = null;
	private ObjectArrayList treeNodes = null;
	
	/** Node table schema used for generated Graphs */
    public static final String NODENAME = "name";
    public static final String NODETYPE = "nodetype";
    public static final String NODEATTR = "nodeattr";
    public static final Schema NODE_SCHEMA = new Schema();
    static {
        NODE_SCHEMA.addColumn(NODENAME, String.class, "");
        NODE_SCHEMA.addColumn(NODETYPE, String.class, "");
        NODE_SCHEMA.addColumn(NODEATTR, String.class, "");
    }
    
    public static final Schema EDGE_SCHEMA = new Schema();
    public static final String EDGENAME = "name";
    static {
        EDGE_SCHEMA.addColumn(EDGENAME, String.class);
    }
    private static final Schema DECORATOR_SCHEMA = PrefuseLib.getVisualItemSchema(); 
    static 
    { 
    	DECORATOR_SCHEMA.setDefault(VisualItem.INTERACTIVE, false); 
    	DECORATOR_SCHEMA.setDefault(VisualItem.TEXTCOLOR, ColorLib.gray(0)); 
    	DECORATOR_SCHEMA.setDefault(VisualItem.FONT, FontLib.getFont("Tahoma", 10)); 
    }
    
    private LinkBitMatrixUtil linkMatrix = null;
	public GraphViewer(ObjectArrayList nodes, boolean MULTIPLE_TREES, LinkBitMatrixUtil linkMatrix){
		WINDOW_CLOSED = false;
		if(MULTIPLE_TREES)
			this.clusterRootNodes = nodes;
		else
			this.treeNodes = nodes;
		this.linkMatrix = linkMatrix;
		init_view();
	}
	private void init_buttons(JPanel buttonPanel){
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        forceLayout = new JButton("Force Layout");
        forceLayout.addActionListener(this);
        buttonPanel.add(forceLayout, c);
        c.gridx = 1;
        restart = new JButton("Restart");
        restart.addActionListener(this);
        buttonPanel.add(restart,c);
        c.gridx = 2;
        buttonPanel.add(getSaveButton(), c);
        c.gridx = 0;
        c.gridy = 1;

        circleLayout = new JButton("Circle Layout");
        circleLayout.addActionListener(this);
        buttonPanel.add(circleLayout, c);

        c.gridx = 1;
        radiaTreeLayout = new JButton("RadiaTree Layout");
        radiaTreeLayout.addActionListener(this);
        buttonPanel.add(radiaTreeLayout, c);
    	c.gridx = -1;
        if(this.clusterRootNodes != null){
        	c.gridy = 2;
        	c.gridx = c.gridx + 1;
        	previous = new JButton("Previous");
        	previous.addActionListener(this);
        	buttonPanel.add(previous, c);
        	
        	c.gridx = c.gridx + 1;
        	next = new JButton("Next");
        	next.addActionListener(this);
        	buttonPanel.add(next, c);

        	c.gridy = c.gridy + 1;
            c.gridx = 0;
            NumberFormat indexFormat = NumberFormat.getIntegerInstance();
            clusterIndex = new JFormattedTextField(indexFormat);
            clusterIndex.setValue(new Integer(1));
            clusterIndex.addPropertyChangeListener("value", this);
            JLabel clusterLabel = new JLabel("Cluster:");
            clusterLabel.setLabelFor(clusterIndex);
            buttonPanel.add(clusterLabel, c);
            c.gridx = c.gridx + 1;
            buttonPanel.add(clusterIndex,c);
        }
	}
	private void init_view(){
		frame = new JFrame("Graph Viewer");
		frame.setDefaultLookAndFeelDecorated(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setSize(1200, 800);
		
        
		JSplitPane mainSplitPanel = new JSplitPane();
        
		display = new Display(vis);
        display.setSize(700,700);
        display.pan(350, 350);
        // main display controls
        display.addControlListener(new FocusControl(1));
        display.addControlListener(new DragControl());
        display.addControlListener(new PanControl());
        display.addControlListener(new ZoomControl());
        display.addControlListener(new WheelZoomControl());
        display.addControlListener(new ZoomToFitControl());
        display.addControlListener(new NeighborHighlightControl());
        mainSplitPanel.setLeftComponent(display);
        mainSplitPanel.setOneTouchExpandable(true);
        mainSplitPanel.setContinuousLayout(false);
        mainSplitPanel.setDividerLocation(700);
        
		JSplitPane rightSplitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);		
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		mainSplitPanel.setRightComponent(rightSplitPanel);
        rightSplitPanel.setTopComponent(buttonPanel);
        rightSplitPanel.setDividerLocation(100);
        init_buttons(buttonPanel);
        
        dynamicPanel = new JPanel(new GridBagLayout());
        rightSplitPanel.setBottomComponent(new JScrollPane(dynamicPanel));

		frame.add(mainSplitPanel);
		frame.addWindowListener(this);
	}
	public void run(){
		if(this.clusterRootNodes == null && this.treeNodes == null){
			System.err.println("Display Demo Graph");
		}
		Graph g = get_graph(this.currentGraphIndex);
		initVisualization(g);
		vis.run("init");
		vis.run("color");  // assign the colors
		vis.run("layout"); // start up the animated layout
		//vis.run("layoutForce");
		frame.pack();           // layout components in window
		frame.setVisible(true); // show the window
		this.frame.validate();
		this.frame.repaint();
		while(!WINDOW_CLOSED){
			try{
				Thread.currentThread().sleep(1000);
			}catch(InterruptedException e){
				e.printStackTrace();
				return;
			}
		}
		System.err.println("Finished");
    }
	private Graph get_graph(int currentIndex){
		Graph g = null;
		if(this.clusterRootNodes == null && this.treeNodes == null){
			if(currentIndex%2 == 0)
				g = getGrid(10,10);
			else
				g = getGrid(5,5);
			return g;
		}
		ObjectArrayList leafNodes = null;
		if(this.treeNodes != null) 
			leafNodes = treeNodes;
		else{
			leafNodes = new ObjectArrayList();
			TreeNode root = (TreeNode)clusterRootNodes.get(currentIndex);
			LinkGraphClustering.collectTreeNodes(leafNodes, new ObjectArrayList(), root);
		}
		Collection<Long> treeIds = new HashSet<Long>();
		for(int i = 0; i < leafNodes.size(); i++){
			TreeNode treeNode = (TreeNode)leafNodes.get(i);
			treeIds.add(((TreeNode)treeNode).id);
		}
		goTermsCounter = linkMatrix.computeGOOverlap(treeIds, 20);
		for(OntologyTerm ontologyTerm:goTermsCounter.keySet()){
			int num = goTermsCounter.get(ontologyTerm);
			System.err.println("("+num+") " + ontologyTerm.getTerm()+":"+ ontologyTerm.getComment());
			if(SelectedGoTerm.length() == 0) SelectedGoTerm = ontologyTerm.getTerm();
		}
		Map<Long, Node> gene2Node = new HashMap<Long, Node>();
		g = new Graph();
		g.getNodeTable().addColumns(NODE_SCHEMA);
		g.getEdgeTable().addColumns(EDGE_SCHEMA);
		for(int i = 0; i < leafNodes.size(); i++){
			Object obj = leafNodes.get(i);
			Gene[] pairedGene = linkMatrix.getPairedGenes(((TreeNode) obj).id);
			for(Gene gene:pairedGene){
				if(!gene2Node.containsKey(gene.getId())){
					Node node = g.addNode();
					node.setString(NODENAME, gene.getName());
					if(gene.getName().matches("(RPL|RPS)(.*)"))node.setString(NODETYPE, "R");
					else if(gene.getNcbiId() == null || gene.getNcbiId().length() == 0) node.setString(NODETYPE, "G");
					else node.setString(NODETYPE, "N");

					String goTerms = "";
					Collection<OntologyTerm> goEntries = linkMatrix.getUtilService().getGOTerms(gene);
					for(OntologyTerm ontologyTerm:goEntries){
						goTerms = goTerms + ontologyTerm.getTerm()+";";
					}
					node.setString(NODEATTR, new String(goTerms));
					gene2Node.put(gene.getId(), node);
				}
			}
			Node node1 = gene2Node.get(pairedGene[0].getId());
			Node node2 = gene2Node.get(pairedGene[1].getId());
			Integer  goOverlaped = linkMatrix.computeGOOverlap(((TreeNode)obj).id);
			Edge edge = g.addEdge(node1,node2);
			//edge.setDouble(WEIGHT,goOverlaped);
			edge.setString(EDGENAME,goOverlaped.toString());
		}
		return g;
	}
	private void initVisualization(Graph g){
		vis = new Visualization();
		//draw the "name" for NodeItems
		LabelRenderer r = new LabelRenderer(NODENAME);
		r.setRoundedCorner(8, 8); //round the corners
		//create a new default renderer factory
		//return our name label renderer as the default for all non-EdgeItems
		//includes straight line edges for EdgeItems by default
		vis.setRendererFactory(new DefaultRendererFactory(r));
		vis.add("graph", g);

		vis.addDecorators(EDGENAME, "graph.edges",DECORATOR_SCHEMA);
		FillColorAction fill = new FillColorAction("graph.nodes");
		ColorAction text = new ColorAction("graph.nodes",VisualItem.TEXTCOLOR, ColorLib.gray(0));
		ColorAction edges = new ColorAction("graph.edges",VisualItem.STROKECOLOR, ColorLib.gray(200));
		
		ActionList initAction = new ActionList();
		initAction.add(text);
		initAction.add(edges);
		initAction.add(new RandomLayout("graph"));

		ActionList color = new ActionList();
		color.add(fill);
		
//		 create an action list with an animated layout
//		 the INFINITY parameter tells the action list to run indefinitely
		ActionList layout = new ActionList(Activity.INFINITY);
		layout.add(new LabelLayout(EDGENAME));
		layout.add(new RepaintAction());
		//color.add(new RandomLayout("graph"));
		vis.putAction("init", initAction);
		vis.putAction("color", color);
		vis.putAction("layout", layout);
		
//		 create an action list containing all color assignments
		ForceDirectedLayout fdl = new ForceDirectedLayout("graph"); 
		ForceSimulator fsim = fdl.getForceSimulator(); 
		fsim.getForces()[2].setParameter(1, 120); //DefaultSpringLength
		
		ActionList layoutForce = new ActionList(Activity.INFINITY);
		layoutForce.add(fdl);
		vis.putAction("layoutForce", layoutForce);
		
		// create a new Display that pull from our Visualization
		display.setVisualization(vis);

		
        int hops = 30;
        dynamicPanel.removeAll();
    	JForcePanel fpanel = new JForcePanel(fsim);;
    	

		/*
		final GraphDistanceFilter filter = new GraphDistanceFilter("graph", hops);
		final JValueSlider slider = new JValueSlider("Distance", 0, hops, hops);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				filter.setDistance(slider.getValue().intValue());
				vis.run("draw");
			}
		});
		slider.setBackground(Color.WHITE);
		slider.setPreferredSize(new Dimension(300,30));
		slider.setMaximumSize(new Dimension(300,30));

		Box cf = new Box(BoxLayout.Y_AXIS);
		cf.add(slider);
		cf.setBorder(BorderFactory.createTitledBorder("Connectivity Filter"));
		fpanel.add(cf);
		fpanel.add(Box.createVerticalGlue());
		*/
        Box info = new Box(BoxLayout.Y_AXIS);
		info.add(new JLabel("Go Term Summary for Cluster" + (currentGraphIndex+1) +":"));

		ButtonGroup group = new ButtonGroup();
		ActionListener action = new ActionListener(){
			public void actionPerformed(ActionEvent x){
				SelectedGoTerm = x.getActionCommand();
				System.err.println(SelectedGoTerm+ " is selected");
				vis.run("color");
			} 
		};
		//Add go terms information
		if(this.clusterRootNodes == null && this.treeNodes == null){
			//For demo graph
			JRadioButton button1 = new JRadioButton("First");
			button1.setActionCommand("First");
			JRadioButton button2 = new JRadioButton("Second");
			button2.setActionCommand("Second");
			button1.setSelected(true);
			button1.addActionListener(action);
			button2.addActionListener(action);
			info.add(button1);
			info.add(button2);
			group.add(button1);
			group.add(button2);
		}else{
			for(OntologyTerm ontologyTerm:goTermsCounter.keySet()){
				int num = goTermsCounter.get(ontologyTerm);
				//info.add(new JLabel("("+num+") " + ontologyEntry.getValue()+":"+ ontologyEntry.getDescription()));
				JRadioButton button = new JRadioButton("("+num+") " + ontologyTerm.getTerm());
				button.setActionCommand(ontologyTerm.getTerm());
				button.addActionListener(action);
				info.add(button);
				group.add(button);
			}
		}
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 3;
		dynamicPanel.add(info);
		c.ipady = 0;
		c.gridx = 0;
		c.gridy = 1;
		dynamicPanel.add(fpanel,c);
		dynamicPanel.validate();
		dynamicPanel.repaint();
	}
	private void restart(int index){
//		boolean pause =vis.getAction("layoutForce").isRunning(); 
//		if(pause) 
//			vis.cancel("layoutForce");
		vis.reset();
		display.clearDamage();
		
		Graph g = get_graph(index);
		//vis.add("graph", g);
		initVisualization(g);
		this.frame.validate();
		this.frame.repaint();
		vis.run("init");
		vis.run("color");  // assign the colors
		vis.run("layout"); // start up the animated layout
		vis.run("layoutForce");
	}
	
    public void propertyChange(PropertyChangeEvent e) {
        Object source = e.getSource();
        if (source == clusterIndex) {
        	int index = ((Number)clusterIndex.getValue()).intValue();
        	if(index <= 0 || index > clusterRootNodes.size()){
        		String message = "The Index must be between 0 and " + this.clusterRootNodes.size();
        		JOptionPane.showMessageDialog( null, "Error: " + message + "\n", "Error", JOptionPane.ERROR_MESSAGE );
        	}else{
        		currentGraphIndex = index - 1;
        		restart(currentGraphIndex);
        	}
        }
    }
    public void actionPerformed(ActionEvent e) {
    	Object source = e.getSource();
    	if(source == forceLayout){
			boolean pause =vis.getAction("layoutForce").isRunning(); 
			if(pause){
				forceLayout.setText("Force Layout");
				vis.cancel("layoutForce");
			}
			else{
				forceLayout.setText("Pause");
				vis.run("layoutForce");
			}
			forceLayout.invalidate();
			forceLayout.repaint();
			//this.frame.invalidate();
			//this.frame.repaint();
    	}
    	if(source == restart){
			forceLayout.setText("Pause");
			restart(currentGraphIndex);    		
    	}
    	if(source == circleLayout){
			ActionList circleLayout = new ActionList();
			circleLayout.add(new CircleLayout("graph.nodes"));
			boolean pause = vis.getAction("layoutForce").isRunning(); 
			if(pause) 
				vis.cancel("layoutForce");
			forceLayout.setText("Force Layout");
			vis.putAction("circleLayout", circleLayout);
			vis.run("circleLayout");
			vis.invalidateAll();
			vis.repaint();
    	}
    	if(source == radiaTreeLayout){
			ActionList layout = new ActionList();
			layout.add(new RadialTreeLayout("graph"));
			boolean pause =vis.getAction("layoutForce").isRunning(); 
			if(pause) 
				vis.cancel("layoutForce");
			forceLayout.setText("Force Layout");
			vis.putAction("radialLayout", layout);
			vis.run("radialLayout");
			vis.invalidateAll();
			vis.repaint();
    	}
    	if(source == next){
			if(currentGraphIndex < clusterRootNodes.size() - 1){
				currentGraphIndex++;
				forceLayout.setText("Pause");
				clusterIndex.setValue(new Integer(currentGraphIndex+1));
			}
    	}
    	if(source == previous){
			if(currentGraphIndex > 0){
				forceLayout.setText("Pause");
				currentGraphIndex--;
				clusterIndex.setValue(new Integer(currentGraphIndex+1));
			}
    	}
    }

	private JButton getSaveButton(){
		initFilter(".", "img");
		JButton eb = new JButton("Export to file"); 
		eb.setEnabled(true); 
		eb.addActionListener(new ActionListener(){ 
			public void actionPerformed(ActionEvent x){ 
				boolean pause =vis.getAction("layoutForce").isRunning(); 
				if(pause) 
					vis.cancel("layoutForce");  
				int returnVal = ed.showSaveDialog(display); 
				if(returnVal == JFileChooser.APPROVE_OPTION) { 
					System.out.println("You chose to save this file: " + 
							ed.getSelectedFile().getName()); 
					File selectedFile = ed.getSelectedFile(); 
					String fType = IOLib.getExtension(selectedFile); 

					if (fType == null){ 
						fType = ((SimpleFileFilter) ed.getFileFilter()).getExtension(); 
						selectedFile = new File(selectedFile.toString()+"."+ fType); 
					} 
					if (selectedFile.exists()) { 
						int response = JOptionPane.showConfirmDialog(display,"The file \"" + selectedFile.getName() 
								+ "\" already exists.\nDo you want to replace it?", "Confirm Save", 
								JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE); 
//						if (response == JOptionPane.NO_OPTION){ 
//							if(pause) 
//								vis.run("layoutforce"); 
//							return;} 
					} 

					try { 
						OutputStream out = new BufferedOutputStream(new FileOutputStream(selectedFile)); 
						display.saveImage(out, fType, 1.0); 
						out.close(); 
					}catch(IOException e){ 
						e.printStackTrace(); 
					}  
//					if(pause) 
//						vis.run("layoutForce"); 
//				} else{ 
//					if(pause) 
//						vis.run("layoutForce"); 
//					return; //user canceled 
				} 
			} 
		}); 
		return eb;
	}
	void initFilter(String dataDir, String title){
		File defaultFile = new File(dataDir + title +".jpg"); 
		System.out.println("Default: " + defaultFile.toString()); 
		ed = new JFileChooser(defaultFile); 
		ed.setDialogTitle("Save graph to file"); 
		ed.setAcceptAllFileFilterUsed(false); 
		SimpleFileFilter filter = new SimpleFileFilter("bmp", "Bitmap file (*.bmp)"); 
		ed.addChoosableFileFilter(filter); 
		filter = new SimpleFileFilter("png", "Portable Network Graphics (*.png)"); 
		ed.addChoosableFileFilter(filter); 
		filter = new SimpleFileFilter("jpg", "JPEG file (*.jpg)"); 
		ed.addChoosableFileFilter(filter); 

//		add a button to export the display 
	}

    public static Graph getGrid(int m, int n) {
        Graph g = new Graph();
        g.getNodeTable().addColumns(NODE_SCHEMA);
        g.getEdgeTable().addColumns(EDGE_SCHEMA);
        
        Node[] nodes = new Node[m*n];
        for ( int i = 0; i < m*n; ++i ) {
            nodes[i] = g.addNode();
            nodes[i].setString(NODENAME, String.valueOf(i));
            if(i > m*n/2)
            	nodes[i].setString(NODEATTR, "First");
            else
            	nodes[i].setString(NODEATTR, "Second");
            Edge edge = null;
            if ( i >= n )
                edge = g.addEdge(nodes[i-n], nodes[i]);
            if ( i % n != 0 )
            	edge = g.addEdge(nodes[i-1], nodes[i]);
            if(edge != null)
            	edge.setString(EDGENAME, "test");
        }
        return g;
    }

	public static class LabelLayout extends Layout { 
		public LabelLayout(String group) { 
			super(group); 
		} 
		public void run(double frac) { 
			Iterator iter = m_vis.items(m_group); 
			while ( iter.hasNext() ) { 
				DecoratorItem item = (DecoratorItem)iter.next(); 
				VisualItem node = item.getDecoratedItem();
				Rectangle2D bounds = node.getBounds(); 
				setX(item, null, bounds.getCenterX()); 
				setY(item, null, bounds.getCenterY()); 
			} 
		} 
	} // end of inner class LabelLayout
	public static class FillColorAction extends ColorAction {
        private ColorMap cmap = new ColorMap(
            ColorLib.getInterpolatedPalette(10,
            		ColorLib.rgb(190,190,255), ColorLib.rgb(0,0,0)), 0, 9);

        public FillColorAction(String group) {
            super(group, VisualItem.FILLCOLOR);
        }
        
        public int getColor(VisualItem item) {
            if ( item instanceof NodeItem ) {
                NodeItem nitem = (NodeItem)item;
                if(nitem.getString(NODEATTR).contains(SelectedGoTerm)){
                	return ColorLib.rgb(191,99,130);
                	
                }else
                	return cmap.getColor(1);
            } else {
                return cmap.getColor(0);
            }
        }
        
    } // end of inner class TreeMapColorAction
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
	}
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		WINDOW_CLOSED = true;
		System.err.println("CLOSING");
	}
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
	}
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
}
