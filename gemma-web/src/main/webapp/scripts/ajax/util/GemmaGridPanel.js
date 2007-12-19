Ext.namespace('Ext.Gemma');

/* Ext.Gemma.GemmaGridPanel constructor...
 * 	config is a hash with the following options:
 */
Ext.Gemma.GemmaGridPanel = function ( config ) {

	/* establish default config options...
	 */
	var superConfig = {
		loadMask : true,
		selModel : new Ext.grid.RowSelectionModel(),
		autoHeight : true,
		bbar : [],
		tbar : [],
	};

	/* apply user-defined config options and call the superclass constructor...
	 */
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.GemmaGridPanel.superclass.constructor.call( this, superConfig );

	/* if the toolbars weren't passed in, destroy the default elements that were created...
	 * (defaults were created so that we can have the option of adding toolbars later)
	 */
	if ( ! config.tbar ) { this.getTopToolbar().destroy(); }
	if ( ! config.bbar ) { this.getBottomToolbar().destroy(); }
};

/* static methods
 */
Ext.Gemma.GemmaGridPanel.formatTermWithStyle = function( value, uri ) {
	var class = uri ? "unusedWithUri" : "unusedNoUri";
	var description = uri || "free text";
	return String.format( "<span class='{0}' title='{2}'>{1}</span>", class, value, description );
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.GemmaGridPanel, Ext.grid.EditorGridPanel, {

	autoSizeColumns: function() {
	    for (var i = 0; i < this.colModel.getColumnCount(); i++) {
    		this.autoSizeColumn(i);
	    }
	},

	autoSizeColumn: function(c) {
		var w = this.view.getHeaderCell(c).firstChild.scrollWidth;
		for (var i = 0, l = this.store.getCount(); i < l; i++) {
			w = Math.max(w, this.view.getCell(i, c).firstChild.scrollWidth);
		}
		this.colModel.setColumnWidth(c, w);
		return w;
	},
	
	refresh : function( params ) {
		var reloadOpts = { callback: this.getView().refresh };
		if ( params ) {
			reloadOpts.params = params
		}
		this.getStore().reload( reloadOpts );
	}
	
} );