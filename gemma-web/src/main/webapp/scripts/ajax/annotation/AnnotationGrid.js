Ext.namespace('Ext.Gemma');

/* Ext.Gemma.AnnotationGrid constructor...
 */
Ext.Gemma.AnnotationGrid = function ( div, config ) {
	
	/* we're expecting the following config options:
	 * 	readMethod : the DWR method that returns the list of AnnotationValueObjects
	 * 		( e.g.: ExpressionExperimentController.getAnnotation )
	 * 	readParams : an array of parameters that will be passed to the readMethod
	 * 		( e.e.: [ { id:x, classDelegatingFor:"ExpressionExperimentImpl" } ] )
	 */
	this.readMethod = config.readMethod;
	this.readParams = config.readParams;
	
	if ( Ext.Gemma.AnnotationGrid.record == undefined ) {
		Ext.Gemma.AnnotationGrid.record = Ext.data.Record.create( [
			{ name:"id", type:"int" },
			{ name:"classUri", type:"string" },
			{ name:"className", type:"string" },
			{ name:"termUri",type:"string" },
			{ name:"termName", type:"string" }
		] );
	}
	if ( !config.ds ) {
		config.ds = new Ext.data.Store( {
			proxy : new Ext.data.DWRProxy( this.readMethod ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.AnnotationGrid.record )
		} );
		config.ds.setDefaultSort('className');
	}
	config.ds.load( { params : this.readParams } );
	
	if ( Ext.Gemma.AnnotationGrid.styler == undefined ) {
		/* apply a CSS class depending on whether or not the characteristic has a URI.
		 */
		Ext.Gemma.AnnotationGrid.styler = function ( value, metadata, record, row, col, ds ) {
			var class = record.data.termUri ? "unusedWithUri" : "unusedNoUri";
			var description = record.data.termUri || "free text";
			return String.format( "<span class='{0}' title='{2}'>{1}</span>", class, value, description );
		}
	}
	if ( !config.cm ) {
		config.cm = new Ext.grid.ColumnModel( [
			{ header: "Class", width: 150, dataIndex: "className" },
			{ header: "Term", width: 500, dataIndex: "termName", renderer: Ext.Gemma.AnnotationGrid.styler }
		] );
		config.cm.defaultSortable = true;
	}
	
	config.loadMask = config.loadMask || true;
	config.autoExpandColumn = config.autoExpandColumn || 1;

	Ext.Gemma.AnnotationGrid.superclass.constructor.call( this, div, config );
}

/* other public methods...
 */
Ext.extend( Ext.Gemma.AnnotationGrid, Ext.grid.Grid, {

	getSelectedIds : function() {
		var selected = this.getSelectionModel().getSelections();
		var ids = [];
		for ( var i=0; i<selected.length; ++i ) {
			ids.push( selected[i].id );
		}
		return ids;	
	},
	
	refresh : function() {
		this.getDataSource().reload();
		this.getView().refresh();
	}
	
} );