Ext.ns('Ext.ux');
/**
 * adds an "initialText" config option that will show the value just like empty text, but only 
 * before the store loads.
 * 
 * deferEmptyText must be set to true
 */
Ext.ux.InitialTextGridView = Ext.extend(Ext.grid.GridView, {
	initialText : false,
    /**
     * @private
     */
    afterRender : function() {
        if (!this.ds || !this.cm) {
            return;
        }
        
        this.mainBody.dom.innerHTML = this.renderBody() || '&#160;';
        this.processRows(0, true);

        if (this.deferEmptyText !== true) {
            this.applyEmptyText();
        }
        
        if (this.initialText){
        	this.applyInitialText();
        }
        
        this.grid.fireEvent('viewready', this.grid);
    },

    /**
     * @private
     * Displays the configured emptyText if there are currently no rows to display
     */
    applyInitialText : function() {
        if (this.initialText && !this.hasRows()) {
            this.mainBody.update('<div class="x-grid-empty">' + this.initialText + '</div>');
        }
    },
});