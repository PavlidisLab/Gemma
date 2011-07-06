
// Override textArea to allow control of word wrapping
// just adds a wordWrap config field to textArea
// from here: http://www.sencha.com/forum/showthread.php?52122-preventing-word-wrap-in-textarea
// needed for download window of diff ex viz
Ext.override(Ext.form.TextArea, {
    initComponent: Ext.form.TextArea.prototype.initComponent.createSequence(function(){
        Ext.applyIf(this, {
            wordWrap: true
        });
    }),
    
    onRender: Ext.form.TextArea.prototype.onRender.createSequence(function(ct, position){
        this.el.setOverflow('auto');
        if (this.wordWrap === false) {
            if (!Ext.isIE) {
                this.el.set({
                    wrap: 'off'
                });
            }
            else {
                this.el.dom.wrap = 'off';
            }
        }
        if (this.preventScrollbars === true) {
            this.el.setStyle('overflow', 'hidden');
        }
    })
});

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
           	   if( prop && node.data[prop] ) {checked.push(node.data[prop]);}
           	   else {checked.push(node);}
           }
        });

        return checked;
    }
});



