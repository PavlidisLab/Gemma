Ext.namespace('Ext.Gemma');

/* Ext.Gemma.ExperimentalFactorGrid constructor...
 * 	config is a hash with the following options:
 * 		edId	the id of the ExperimentalDesign whose ExperimentalFactors are displayed in the grid
 */
Ext.Gemma.ExperimentalFactorGrid = function ( config ) {

	this.experimentalDesign = {
		id : config.edId,
		classDelegatingFor : "ExperimentalDesign"
	};
	delete config.edId;
	
	this.onRefresh = config.onRefresh; delete config.onRefresh;
	
	this.nameField = new Ext.form.TextField( { } );
	
	this.categoryCombo = new Ext.Gemma.MGEDCombo( { lazyRender : true, termKey : "factor" } );
	var categoryEditor = new Ext.grid.GridEditor( this.categoryCombo );
	this.categoryCombo.on( "select", function ( combo, record, index ) { categoryEditor.completeEdit(); } );
	
	this.descriptionField = new Ext.form.TextField( { } );

	/* establish default config options...
	 */
	var superConfig = {};
	
	superConfig.ds = new Ext.data.Store( {
		proxy : new Ext.data.DWRProxy( ExperimentalDesignController.getExperimentalFactors ),
		reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.ExperimentalFactorGrid.getRecord() )
	} );
	superConfig.ds.load( { params: [ this.experimentalDesign ] } );
	
	var NAME_COLUMN = 0;
	var CATEGORY_COLUMN = 1;
	var DESCRIPTION_COLUMN = 2;
	superConfig.cm = new Ext.grid.ColumnModel( [
		{ header: "Name", dataIndex: "name", editor: this.nameField },
		{ header: "Category", dataIndex: "category", renderer: Ext.Gemma.ExperimentalFactorGrid.getCategoryStyler(), editor: categoryEditor },
		{ header: "Description", dataIndex: "description", editor: this.descriptionField }
	] );
	superConfig.cm.defaultSortable = true;
	superConfig.autoExpandColumn = DESCRIPTION_COLUMN;
	
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.ExperimentalFactorGrid.superclass.constructor.call( this, superConfig );
	
	/* these functions have to happen after we've called the super-constructor so that we know
	 * we're a Grid...
	 */
	this.getStore().on( "load", function () {
		this.autoSizeColumns();
		this.doLayout();
	}, this );
	
	this.on( "afteredit", function( e ) {
		var col = this.getColumnModel().getColumnId( e.column );
		if ( col == CATEGORY_COLUMN ) {
			var f = this.categoryCombo.getTerm.bind( this.categoryCombo );
			var term = f();
			e.record.set( "category", term.term );
			e.record.set( "categoryUri", term.uri );
		}
	} );
	
	var tbar = new Ext.Gemma.ExperimentalFactorToolbar( { grid : this, renderTo : this.tbar } );
};

/* static methods
 */
Ext.Gemma.ExperimentalFactorGrid.getRecord = function() {
	if ( Ext.Gemma.ExperimentalFactorGrid.record === undefined ) {
		Ext.Gemma.ExperimentalFactorGrid.record = Ext.data.Record.create( [
			{ name:"id", type:"int" },
			{ name:"name", type:"string" },
			{ name:"description", type:"string" },
			{ name:"category", type:"string" },
			{ name:"categoryUri", type:"string" }
		] );
	}
	return Ext.Gemma.ExperimentalFactorGrid.record;
};

Ext.Gemma.ExperimentalFactorGrid.getCategoryStyler = function() {
	if ( Ext.Gemma.ExperimentalFactorGrid.categoryStyler === undefined ) {
		/* apply a CSS class depending on whether or not the characteristic has a URI.
		 */
		Ext.Gemma.ExperimentalFactorGrid.categoryStyler = function ( value, metadata, record, row, col, ds ) {
			return Ext.Gemma.GemmaGridPanel.formatTermWithStyle( value, record.data.categoryUri );
		};
	}
	return Ext.Gemma.ExperimentalFactorGrid.categoryStyler;
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.ExperimentalFactorGrid, Ext.Gemma.GemmaGridPanel, {
	
	refresh : function( ct, p ) {
		Ext.Gemma.ExperimentalFactorGrid.superclass.refresh.call( this, ct, p );
		if ( this.onRefresh ) {
			this.onRefresh();
		}
	}
	
} );

/* Ext.Gemma.ExperimentalFactorToolbar constructor...
 * 	config is a hash with the following options:
 * 		grid is the grid that contains the factors.
 */
Ext.Gemma.ExperimentalFactorToolbar = function ( config ) {

	this.grid = config.grid; delete config.grid;
	this.experimentalDesign = this.grid.experimentalDesign;
	
	/* keep a reference to ourselves so we don't have to worry about scope in the
	 * button handlers below...
	 */
	var thisToolbar = this;
	
	/* establish default config options...
	 */
	var superConfig = {};
	
	/* add our items in front of anything specified in the config above...
	 */
	this.categoryCombo = new Ext.Gemma.MGEDCombo( { emptyText : "Select a category", termKey : "factor" } );
	this.categoryCombo.on( "select", function() {
		createButton.enable();
	} );
	this.descriptionField = new Ext.form.TextField( { emptyText : "Type a description" } );
	var createButton = new Ext.Toolbar.Button( {
		text : "create",
		tooltip : "Create the new experimental factor",
		disabled : true,
		handler : function() {
			ExperimentalDesignController.createExperimentalFactor(
				thisToolbar.experimentalDesign,
				thisToolbar.getExperimentalFactorValueObject(),
				thisToolbar.grid.refresh.bind( thisToolbar.grid )
			);
			thisToolbar.categoryCombo.reset();
			thisToolbar.descriptionField.reset();			
			createButton.disable();
		}
	} );
	var deleteButton = new Ext.Toolbar.Button( {
		text : "delete",
		tooltip : "Delete selected experimental factors",
		disabled : true,
		handler : function() {
			ExperimentalDesignController.deleteExperimentalFactors(
				thisToolbar.experimentalDesign,
				thisToolbar.grid.getSelectedIds(),
				thisToolbar.grid.refresh.bind( thisToolbar.grid )
			);
			deleteButton.disable();
		}
	} );
	this.grid.getSelectionModel().on( "selectionchange", function( model ) {
		var selected = model.getSelections();
		if ( selected.length > 0 ) {
			deleteButton.enable();
		}
		else {
			deleteButton.disable();
		}
	} );
	var saveButton = new Ext.Toolbar.Button( {
		text : "save",
		tooltip : "Save changed experimental factors",
		disabled : true,
		handler : function() {
			saveButton.disable();
			var edited = thisToolbar.grid.getEditedRecords();
			var callback = thisToolbar.grid.refresh.bind( thisToolbar.grid );
			ExperimentalDesignController.updateExperimentalFactors( edited, callback );
		}
	} );
	this.grid.on( "afteredit", function( model ) {
		saveButton.enable();
	} );
	var revertButton = new Ext.Toolbar.Button( {
		text : "revert",
		tooltip : "Undo changes to selected experimental factors",
		disabled : true,
		handler : function() {
			thisToolbar.grid.revertSelected();
		}
	} );
	this.grid.getSelectionModel().on( "selectionchange", function( model ) {
		var selected = model.getSelections();
		revertButton.disable();
		for ( var i=0; i<selected.length; ++i ) {
			if ( selected[i].dirty ) {
				revertButton.enable();
				break;
			}
		}
	} );
	
	var items = [
		new Ext.Toolbar.TextItem( "Add an Experimental Factor:" ),
		new Ext.Toolbar.Spacer(),
		this.categoryCombo,
		new Ext.Toolbar.Spacer(),
		this.descriptionField,
		new Ext.Toolbar.Spacer(),
		createButton,
		new Ext.Toolbar.Separator(),
		deleteButton,
		new Ext.Toolbar.Separator(),
		saveButton,
		new Ext.Toolbar.Separator(),
		revertButton
	];
	config.items = config.items ? items.concat( config.items ) : items;
	
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.ExperimentalFactorToolbar.superclass.constructor.call( this, superConfig );
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.ExperimentalFactorToolbar, Ext.Toolbar, {

	getExperimentalFactorValueObject : function() {
		var category = this.categoryCombo.getTerm();
		var description = this.descriptionField.getValue();
		return {
			name : category.term,
			description : description,
			category : category.term,
			categoryUri : category.uri
		};
	}

} );