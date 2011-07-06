/**
 * *
 * 
 * @author AZ
 * @version $Id$
 */
Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

/**
 * Constructor for a tree that builds its nodes from a parameter object the
 * parameter object must be an array of objects with two fields: text(mandatory)
 * and children (optional) For example: [{text:'parent1', children:
 * [{text:'child1', children: []}] },{ text: 'parent2' }, ...]
 * 
 * @param nodes
 *            objects to build the treeNodes from
 * @class Gemma.DifferentialExpressionAnalysesSummaryTree
 * @extends Ext.tree.TreePanel
 * 
 */
Gemma.SelectTree = Ext.extend(Ext.tree.TreePanel, {

			// renderTo:'tree-div',
			animate : true,
			id : 'selectTree',
			rootVisible : false,
			enableDD : false,
			cls : 'x-tree-noicon',
			lines : false,
			// containerScroll:true,
			// panel
			autoScroll : false,
			// ddScroll:true,
			border : false,
			layout : 'fit',
			root : {
				text : 'root'
			},

			constructor : function(nodes) {
				Ext.apply(this, {
							nodeParams : nodes
						});
				Gemma.DifferentialExpressionAnalysesSummaryTree.superclass.constructor.call(this);
			},

			initComponent : function() {
				Gemma.DifferentialExpressionAnalysesSummaryTree.superclass.initComponent.call(this);
				this.build();
				var sorter = new Ext.tree.TreeSorter(this, {});
			},
			build : function() {
				var root = new Ext.tree.TreeNode({
							expanded : true,
							id : 'selectTreeRoot',
							text : 'root'
						});
				this.setRootNode(root);
				this.buildNodes(root, this.nodeParams);
			},
			buildNodes : function(parent, nodeParams) {
				var node;
				var nodeParam;
				var hasChildren;
				var i;
				for (i = 0; i < nodeParams.length; i++) {

					nodeParam = nodeParams[i];

					if (nodeParam.children && nodeParam.children !== null && nodeParam.children.length !== 0) {
						hasChildren = true;
						node = new Ext.tree.TreeNode({
									expanded : false,
									singleClickExpand : true,
									text : nodeParam.text,
									checked : true,
									leaf : false
								});
					} else {
						hasChildren = false;
						node = new Ext.tree.TreeNode({
									expanded : false,
									singleClickExpand : true,
									text : nodeParam.text,
									checked : true,
									leaf : true
								});
					}

					if (hasChildren) {
						this.buildNodes(node, nodeParam.children);
					}
					parent.appendChild(node);
				}
			}
		});

/**
 * Constructor for a tree that builds its nodes from a parameter object the
 * parameter object must be an array of objects with two fields: text and
 * children For example: [{ text:'parent1', children: [{ text:'child1',
 * children: [] }] },{ text: 'parent2', children:[] }, ...]
 * 
 * This is like Gemma.SelectTree, but no two nodes will have the same name and
 * if two node definitions passed in have the same text value, one node will be
 * created with the aggregate of children
 * 
 * @param nodes
 *            objects to build the treeNodes from
 * @class Gemma.DifferentialExpressionAnalysesSummaryTree
 * @extends Ext.tree.TreePanel
 */
Gemma.FactorSelectTree = Ext.extend(Gemma.SelectTree, {

			lines : true,
			rootVisible : true,

			initComponent : function() {
				Gemma.DifferentialExpressionAnalysesSummaryTree.superclass.initComponent.call(this);
				this.build();
				var sorter = new Ext.tree.TreeSorter(this, {
							sortType : function(node) {
								return node.text.toLowerCase().replace(" ", "");// so
								// that
								// "deseaseState"
								// and
								// "Disease
								// State"
								// are
								// always
								// adjacent
							}
						});
			},

			build : function() {
				var root = new Ext.tree.TreeNode({
							expanded : true,
							id : 'selectTreeRoot',
							text : ''
						});
				this.setRootNode(root);
				this.buildNodes(root, this.nodeParams, true);
				root.setText('Filter Experiments by Factor'); // title it
				// after so
				// title of tree
				// isn't in node
				// ids
			},
			/**
			 * return nodes
			 */
			buildNodes : function(parent, nodeParams, haveCheck) {
				var node;
				var nodeParam;
				var hasChildren = false;
				var i;
				for (i = 0; i < nodeParams.length; i++) {
					nodeParam = nodeParams[i];
					if (nodeParam.children && nodeParam.children !== null && nodeParam.children.length !== 0) {
						hasChildren = true;
					}
					if( nodeParam.text === null ){
						if(hasChildren){
							nodeParam.text = "unavailable";
						}else{
							return;
						}
					}
					// if node already exists in tree with same text, get that
					// node and have the children assigned to that node
					// will this confuse recursion? if multi-leveled?
					node = this.getNodeById(parent.text + nodeParam.text.toLowerCase());

					if (!typeof node !== 'undefined' && node && node !== null) { // if
						// node
						// exists
						// in
						// tree
						// already

					} else { // if node doen't exist in tree already
						if (haveCheck) {
							node = new Ext.tree.TreeNode({
										expanded : false,
										singleClickExpand : false,
										id : parent.text + nodeParam.text.toLowerCase(), // so
										// that
										// multiple
										// nodes
										// with
										// same
										// text
										// aren't
										// created
										text : nodeParam.text,
										checked : true
									});
						} else {
							node = new Ext.tree.TreeNode({
										expanded : false,
										singleClickExpand : false,
										id : parent.text + nodeParam.text.toLowerCase(), // so
										// that
										// multiple
										// nodes
										// with
										// same
										// text
										// aren't
										// created
										text : nodeParam.text
									});
						}
					}

					if (hasChildren) {
						this.buildNodes(node, nodeParam.children, false);
					}
					var testNode = this.getNodeById(parent.text + nodeParam.text.toLowerCase());
					if (typeof testNode === 'undefined') {// don't need to
						// re-add it if it's
						// already there
						parent.appendChild(node);
					}
				}
			}
		});

Ext.reg('factorSelectTree', Gemma.FactorSelectTree);
