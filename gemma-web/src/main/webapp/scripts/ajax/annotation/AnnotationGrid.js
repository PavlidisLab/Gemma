Ext.namespace('Ext.Gemma');

/* Ext.Gemma.AnnotationGrid constructor...
 * 	div is the name of the div in which to render the grid.
 * 	config is a hash with the following options:
 * 		readMethod : the DWR method that returns the list of AnnotationValueObjects
 * 			( e.g.: ExpressionExperimentController.getAnnotation )
 * 		readParams : an array of parameters that will be passed to the readMethod
 * 			( e.e.: [ { id:x, classDelegatingFor:"ExpressionExperimentImpl" } ] )
 * 		             or a pointer to a function that will return the array of parameters
 * 		editable : if true, the annotations in the grid will be editable
 * 		showParent : if true, a link to the parent object will appear in the grid
 * 		noInitialLoad : if true, the grid will not be loaded immediately upon creation
 * 		pageSize : if defined, the grid will be paged on the client side, with the defined page size
 */
Ext.Gemma.AnnotationGrid = function ( div, config ) {
	
	this.readMethod = config.readMethod; delete config.readMethod;
	this.readParams = config.readParams; delete config.readParams;
	this.editable = config.editable; delete config.editable;
	this.showParent = config.showParent; delete config.showParent;
	this.noInitialLoad = config.noInitialLoad; delete config.noInitialLoad;
	this.pageSize = config.pageSize; delete config.pageSize;
	
	var superConfig = { };
	var thisGrid = this;
	
	if ( this.pageSize ) {
		superConfig.ds = new Ext.Gemma.PagingDataStore( {
			proxy : new Ext.data.DWRProxy( this.readMethod ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.AnnotationGrid.getRecord() ),
			pageSize : this.pageSize
		} );
	} else {
		superConfig.ds = new Ext.data.Store( {
			proxy : new Ext.data.DWRProxy( this.readMethod ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.AnnotationGrid.getRecord() )
		} );
	}
	superConfig.ds.setDefaultSort('className');
	superConfig.ds.on( "load", function() {
		thisGrid.getView().autoSizeColumns();
	} );
	
	superConfig.cm = new Ext.grid.ColumnModel( [
		{ header: "Class", dataIndex: "className" },
		{ header: "Term", dataIndex: "termName", renderer: Ext.Gemma.AnnotationGrid.getStyler() },
		{ header: "Parent", dataIndex: "parentLink", hidden: this.showParent ? false: true }
	] );
	superConfig.cm.defaultSortable = true;
	var CATEGORY_COLUMN = 0;
	var VALUE_COLUMN = 1;
	if ( this.editable ) {
		this.categoryCombo = new Ext.Gemma.MGEDCombo( { lazyRender : true } );
		var categoryEditor = new Ext.grid.GridEditor( this.categoryCombo );
		this.categoryCombo.on( "select", function ( combo, record, index ) { categoryEditor.completeEdit(); } );
		superConfig.cm.setEditor( CATEGORY_COLUMN, categoryEditor );
		
		this.valueCombo = new Ext.Gemma.CharacteristicCombo( { lazyRender : true } );
		var valueEditor = new Ext.grid.GridEditor( this.valueCombo );
		this.valueCombo.on( "select", function ( combo, record, index ) { valueEditor.completeEdit(); } );
		superConfig.cm.setEditor( VALUE_COLUMN, valueEditor );
	}
	
	superConfig.selModel = new Ext.grid.RowSelectionModel();
	
	superConfig.loadMask = true;
	superConfig.autoExpandColumn = this.showParent ? 2 : 1;

	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.AnnotationGrid.superclass.constructor.call( this, div, superConfig );
	
	/* these functions have to happen after we've called the super-constructor so that we know
	 * we're a Grid...
	 */
	if ( this.editable ) {
//		this.on( "afteredit", function( e ) {
//			var row = e.record.data;
//			var c = Ext.Gemma.AnnotationGrid.convertToCharacteristic( row );
//			var callback = this.refresh.bind( this );
//			CharacteristicBrowserController.updateCharacteristics( [ c ], callback );
//		} );
		
		this.on( "beforeedit", function( e ) {
			var row = e.record.data;
			var col = this.getColumnModel().getColumnId( e.column );
			if ( col == VALUE_COLUMN ) {
				var f = this.valueCombo.setCategory.bind( this.valueCombo );
				f( row.className, row.classUri );
			}
		} );
		
		this.on( "afteredit", function( e ) {
			var row = e.record.data;
			var col = this.getColumnModel().getColumnId( e.column );
			if ( col == CATEGORY_COLUMN ) {
				var f = this.categoryCombo.getTerm.bind( this.categoryCombo );
				var term = f();
				row.className = term.term;
				row.classUri = term.uri;
			} else if ( col == VALUE_COLUMN ) {
				var f = this.valueCombo.getCharacteristic.bind( this.valueCombo );
				var c = f();
				row.termName = c.value;
				row.termUri = c.valueUri;
			}
			this.getView().refresh();
		} );
	}
	
	if ( ! this.noInitialLoad )
		this.getDataSource().load( { params : this.getReadParams() } );
};

/* static methods
 */
Ext.Gemma.AnnotationGrid.getRecord = function() {
	if ( Ext.Gemma.AnnotationGrid.record == undefined ) {
		Ext.Gemma.AnnotationGrid.record = Ext.data.Record.create( [
			{ name:"id", type:"int" },
			{ name:"classUri", type:"string" },
			{ name:"className", type:"string" },
			{ name:"termUri", type:"string" },
			{ name:"termName", type:"string" },
			{ name:"parentDescription", type:"string" },
			{ name:"parentLink", type:"string" }
		] );
	}
	return Ext.Gemma.AnnotationGrid.record;
};

Ext.Gemma.AnnotationGrid.formatWithStyle = function( value, uri ) {
	var class = uri ? "unusedWithUri" : "unusedNoUri";
	var description = uri || "free text";
	return String.format( "<span class='{0}' title='{2}'>{1}</span>", class, value, description );
};

Ext.Gemma.AnnotationGrid.getStyler = function() {
	if ( Ext.Gemma.AnnotationGrid.styler == undefined ) {
		/* apply a CSS class depending on whether or not the characteristic has a URI.
		 */
		Ext.Gemma.AnnotationGrid.styler = function ( value, metadata, record, row, col, ds ) {
			return Ext.Gemma.AnnotationGrid.formatWithStyle( value, record.data.termUri );
		}
	}
	return Ext.Gemma.AnnotationGrid.styler;
};

Ext.Gemma.AnnotationGrid.convertToCharacteristic = function( record ) {
	var c = {
		id : record.id,
		category : record.className,
		value : record.termName
	};
	/* if we don't have a valueURI set, don't return URI fields or
	 * a VocabCharacteristic will be created when we only want a
	 * Characteristic...
	 */
	if ( record.termUri ) {
		c.categoryUri = record.classUri;
		c.valueUri = record.termUri;
	}
	return c;
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.AnnotationGrid, Ext.grid.EditorGrid, {
	
	refresh : function( params ) {
		var reloadOpts = { callback: this.getView().refresh };
		if ( params ) {
			reloadOpts.params = params
		}
		this.getDataSource().reload( reloadOpts );
	},

	getReadParams : function() {
		return ( typeof this.readParams == "function" ) ? this.readParams() : this.readParams;
	},
	
	getSelectedIds : function() {
		var selected = this.getSelectionModel().getSelections();
		var ids = [];
		for ( var i=0; i<selected.length; ++i ) {
			ids.push( selected[i].id );
		}
		return ids;	
	},
	
	getSelectedCharacteristics : function() {
		var selected = this.getSelectionModel().getSelections();
		var chars = [];
		for ( var i=0; i<selected.length; ++i ) {
			var row = selected[i].data;
			chars.push( Ext.Gemma.AnnotationGrid.convertToCharacteristic( row ) );
		}
		return chars;	
	},
	
	getEditedCharacteristics : function() {
		var chars = [];
		this.getDataSource().each( function( record ) {
			if ( record.dirty ) {
				var row = record.data;
				chars.push( Ext.Gemma.AnnotationGrid.convertToCharacteristic( row ) );
			}
		} );
		return chars;
	}
	
} );