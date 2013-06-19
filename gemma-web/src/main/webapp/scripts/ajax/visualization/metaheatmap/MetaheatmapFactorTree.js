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
         Ext.apply(this, {

               lines : true,
               // tree will not show up in IE9 if root is not visible
               rootVisible : true,
               sortedTree : this.sortedTree,
               cls : 'x-tree-noicon',
               // collapsed : true,

               initializeFromSortedTree : function() {
                  var root = new Ext.tree.TreeNode({
                        expanded : true,
                        text : 'Condition category filters',
                        cls : '',
                        checked : true
                     });

                  this.setRootNode(root);

                  var categoryNodes = this.sortedTree.root.children;
                  for (var i = 0; i < categoryNodes.length; i++) {
                     var sftCategoryNode = categoryNodes[i];

                     var ftCategoryNode = new Ext.tree.TreeNode({
                           expanded : false,
                           singleClickExpand : false,
                           text : (sftCategoryNode.groupName && sftCategoryNode.groupName !== null && sftCategoryNode.groupName !== "null")
                              ? sftCategoryNode.groupName
                              : "No category",
                           checked : true,
                           iconCls : '',
                           cls : ''
                        });

                     var factorNodes = sftCategoryNode.children;
                     for (var j = 0; j < factorNodes.length; j++) {
                        var sftFactorNode = factorNodes[j];
                        var baselines = [];
                        for (k = 0; k < sftFactorNode.items.length; k++) {
                           if (baselines.indexOf(sftFactorNode.items[k].baselineFactorValue) < 0) {
                              baselines.push(sftFactorNode.items[k].baselineFactorValue);
                           }
                        }

                        var ftFactorNode = new Ext.tree.TreeNode({
                              expanded : false,
                              singleClickExpand : false,
                              text : (sftFactorNode.groupName && sftFactorNode.groupName !== null && sftFactorNode.groupName !== "null") ? sftFactorNode.groupName
                                 + "<span style=\"color:grey\"> vs " + baselines.join(',') + '</span>' : "No value",
                              checked : true,
                              cls : '',
                              iconCls : ''
                           });
                        Ext.apply(ftFactorNode, {
                              // used for matching in filtering function
                              contrastFactorValue : sftFactorNode.groupName
                           });
                        ftCategoryNode.appendChild(ftFactorNode);
                     }

                     root.appendChild(ftCategoryNode);
                  }
               }
            });

         Gemma.Metaheatmap.FactorTree.superclass.initComponent.call(this);

      },
      onRender : function() {
         Gemma.Metaheatmap.FactorTree.superclass.onRender.apply(this, arguments);
         this.initializeFromSortedTree();
      }

   });

Ext.reg('Metaheatmap.FactorTree', Gemma.Metaheatmap.FactorTree);