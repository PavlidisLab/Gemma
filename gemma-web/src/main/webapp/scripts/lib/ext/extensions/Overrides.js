
/**
 * Add method to get all checked nodes from a tree
 * 
 * Usage:
 * // to get array of nodes
 * var checked_nodes = treepanel.getChecked();
 * // to get array of ids of checked nodes
 * var checked_ids = treepanel.getChecked('id');
 * 
 * From: http://www.sencha.com/forum/showthread.php?129729-How-to-get-checked-nodes-from-TreePanel
 * 
 * @param {Object} prop
 */
Ext.override(Ext.tree.TreePanel,{
    getChecked: function( propP ){
        var prop = (propP || null);
        var checked = [];

        this.getView().getTreeStore().getRootNode().cascadeBy(function(node){
           if( node.data.checked ){
                if( prop && node.data[prop] ) checked.push(node.data[prop]);
                else checked.push(node);
           }
        });

        return checked;
    }
});