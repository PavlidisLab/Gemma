Ext.onReady(function(){

    var tree = new Ext.tree.TreePanel('tree-div', {
        animate:true, 
        loader: new Ext.tree.DwrTreeLoader({dataUrl:MgedOntologyService.getBioMaterialTerms})
    });

    // set the root node
    var root = new Ext.tree.AsyncTreeNode({
        text: 'Top of the tree',
        draggable:false,
        allowChildre:true,
        id:'root'
    });
    tree.setRootNode(root);

    // render the tree
    tree.render();
    root.expand();
});