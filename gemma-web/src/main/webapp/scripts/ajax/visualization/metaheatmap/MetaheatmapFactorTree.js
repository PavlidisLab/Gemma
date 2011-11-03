/**
 * @author AZ
 * @version $Id$
 */
Ext.namespace('Gemma.Metaheatmap');

/**
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */
Gemma.Metaheatmap.FactorTree = Ext.extend(Ext.tree.TreePanel, {
	initComponent : function() {
		Ext.apply (this, {
			
			lines 		: true,
			rootVisible : false,
			sortedTree  : this.sortedTree,
			cls : 'x-tree-noicon',
			//collapsed : true,
			
			initializeFromSortedTree : function () {				
				var root = new Ext.tree.TreeNode({
					expanded : true,
					text : 'Filter Experiments by Factor',
					cls : '',
				});

				this.setRootNode (root);

				var categoryNodes = this.sortedTree.root.children;
				for (var i = 0; i < categoryNodes.length; i++) {
					var sftCategoryNode = categoryNodes[i];

					var ftCategoryNode = new Ext.tree.TreeNode({
						expanded : true,
						singleClickExpand : false,
						//	id : parent.text + nodeParam.text.toLowerCase(),
						text : sftCategoryNode.groupName,
						checked : true,
						iconCls : '',
						cls : ''
					});				

					var factorNodes = sftCategoryNode.children;
					for (var j = 0; j < factorNodes.length; j++ ) {
						var sftFactorNode = factorNodes[j];

						var ftFactorNode = new Ext.tree.TreeNode({
							expanded : false,
							singleClickExpand : false,
							//	id : parent.text + nodeParam.text.toLowerCase(),
							text : sftFactorNode.groupName,
							checked : true,
							cls : '',
							iconCls : ''
						});				
						ftCategoryNode.appendChild (ftFactorNode);						
					}
					
					root.appendChild (ftCategoryNode);
				}
			}
		});
		
		Gemma.Metaheatmap.FactorTree.superclass.initComponent.call (this);
	},
	onRender : function() {
		Gemma.Metaheatmap.FactorTree.superclass.onRender.apply (this, arguments);
		this.initializeFromSortedTree();
	}

});

Ext.reg('Metaheatmap.FactorTree', Gemma.Metaheatmap.FactorTree);