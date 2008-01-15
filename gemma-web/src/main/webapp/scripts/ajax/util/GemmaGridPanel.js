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
		autoScroll : true,
		bbar : [],
		tbar : []
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
	if ( ! config.tbar ) { this.destroyTopToolbar = true; }
	if ( ! config.bbar ) { this.destroyBottomToolbar = true; }
};

/* static methods
 */
Ext.Gemma.GemmaGridPanel.formatTermWithStyle = function( value, uri ) {
	var style = uri ? "unusedWithUri" : "unusedNoUri";
	var description = uri || "free text";
	return String.format( "<span class='{0}' ext:qtip='{2}'>{1}</span>", style, value, description );
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
	
	render : function( ct, p ) {
		Ext.Gemma.GemmaGridPanel.superclass.render.call( this, ct, p );
		if ( this.destroyTopToolbar ) {
			this.getTopToolbar().destroy();
			delete this.destroyTopToolbar;
		}
		if ( this.destroyBottomToolbar ) {
			this.getBottomToolbar().destroy();
			delete this.destroyBottomToolbar;
		}
	},
	
	getEditedRecords : function() {
		var edited = [];
		var all = this.getStore().getRange();
		for ( var i=0; i<all.length; ++i ) {
			if ( all[i].dirty ) {
				edited.push( all[i].data );
			}
		}
		return edited;
	},
	
	getSelectedRecords : function() {
		var records = [];
		var selected = this.getSelectionModel().getSelections();
		for ( var i=0; i<selected.length; ++i ) {
			records.push( selected[i].data );
		}
		return records;	
	},
	
	getSelectedIds : function() {
		var ids = [];
		var selected = this.getSelectionModel().getSelections();
		for ( var i=0; i<selected.length; ++i ) {
			ids.push( selected[i].id );
		}
		return ids;	
	},
	
	refresh : function( params ) {
		var reloadOpts = { callback: this.getView().refresh.bind( this.getView() ) };
		if ( params ) {
			reloadOpts.params = params;
		}
		this.getStore().reload( reloadOpts );
	},
	
	revertSelected : function( ) {
		var selected = this.getSelectionModel().getSelections();
		for ( var i=0; i<selected.length; ++i ) {
			selected[i].reject();
		}
		this.getView().refresh();
	}
	
} );