Ext.namespace('Ext.Gemma');

/* Ext.Gemma.FactorValueGrid constructor...
 * 	config is a hash with the following options:
 * 		edId	the id of the ExperimentalDesign whose ExperimentalFactors can be displayed in the grid
 * 		efId	the id of the ExperimentalFactor whose FactorValues are displayed in the grid
 * 		form	the name of the HTML form the grid is in
 */
Ext.Gemma.FactorValueGrid = function ( config ) {

	this.experimentalDesign = {
		id : config.edId,
		classDelegatingFor : "ExperimentalDesign"
	};
	delete config.edId;
	this.experimentalFactor = {
		id : config.efId,
		classDelegatingFor : "ExperimentalFactor"
	};
	delete config.efId;
	
	this.form = config.form; delete config.form;
	this.onRefresh = config.onRefresh; delete config.onRefresh;
	
	this.categoryCombo = new Ext.Gemma.MGEDCombo( { lazyRender : true, termKey : "factor" } );
	var categoryEditor = new Ext.grid.GridEditor( this.categoryCombo );
	this.categoryCombo.on( "select", function ( combo, record, index ) { categoryEditor.completeEdit(); } );
	
	this.valueCombo = new Ext.Gemma.CharacteristicCombo( { lazyRender : true } );
	var valueEditor = new Ext.grid.GridEditor( this.valueCombo );
	this.valueCombo.on( "select", function ( combo, record, index ) { valueEditor.completeEdit(); } );

	/* establish default config options...
	 */
	var superConfig = {};
	
	superConfig.ds = new Ext.data.GroupingStore( {
		proxy : new Ext.data.DWRProxy( ExperimentalDesignController.getFactorValuesWithCharacteristics ),
		reader : new Ext.data.ListRangeReader( {id:"charId"}, Ext.Gemma.FactorValueGrid.getRecord() ),
		groupField : "factorValueId",
		sortInfo : { field: "category", direction: "ASC" }
	} );
	if ( this.experimentalFactor.id ) {
		superConfig.ds.load( { params: [ this.experimentalFactor ] } );
	}
	superConfig.view = new Ext.grid.GroupingView( {
		enableGroupingMenu : false,
		enableNoGroups : false,
		groupTextTpl : '<input type="checkbox" name="selectedFactorValues" value="{[ values.rs[0].data.factorValueId ]}" /> {[ values.rs[0].data.factorValueString ]}',
		hideGroupedColumn : true,
		showGroupName : true
	} );
	
	var FACTOR_VALUE_COLUMN = 0;
	var CATEGORY_COLUMN = 1;
	var VALUE_COLUMN = 2;
	superConfig.cm = new Ext.grid.ColumnModel( [
		{ header: "FactorValue", dataIndex: "factorValueId" },
		{ header: "Category", dataIndex: "category", renderer: Ext.Gemma.FactorValueGrid.getCategoryStyler(), editor: categoryEditor },
		{ header: "Value", dataIndex: "value", renderer: Ext.Gemma.FactorValueGrid.getValueStyler(), editor: valueEditor }
	] );
	superConfig.cm.defaultSortable = true;
	superConfig.autoExpandColumn = VALUE_COLUMN;
	
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.FactorValueGrid.superclass.constructor.call( this, superConfig );
	
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
			 this.categoryCombo.getTerm.all(this, this.categoryCombo );
			e.record.set( "category", term.term );
			e.record.set( "categoryUri", term.uri );
		} else if ( col == VALUE_COLUMN ) {
			this.valueCombo.getCharacteristic.call(this, this.valueCombo );
			e.record.set( "value", c.value );
			e.record.set( "valueUri", c.valueUri );
		}
		this.getView().refresh();
	} );
	
	this.factorValueToolbar = new Ext.Gemma.FactorValueToolbar( { grid : this, renderTo : this.tbar } );
};

/* static methods
 */
Ext.Gemma.FactorValueGrid.getRecord = function() {
	if ( Ext.Gemma.FactorValueGrid.record === undefined ) {
		Ext.Gemma.FactorValueGrid.record = Ext.data.Record.create( [
			{ name:"charId", type:"int" },
			{ name:"factorValueId", type:"int" },
			{ name:"category", type:"string" },
			{ name:"categoryUri", type:"string" },
			{ name:"value", type:"string" },
			{ name:"valueUri", type:"string" },
			{ name:"factorValueString", type:"string" }
		] );
	}
	return Ext.Gemma.FactorValueGrid.record;
};

Ext.Gemma.FactorValueGrid.getCategoryStyler = function() {
	if ( Ext.Gemma.FactorValueGrid.categoryStyler === undefined ) {
		/* apply a CSS class depending on whether or not the characteristic has a URI.
		 */
		Ext.Gemma.FactorValueGrid.categoryStyler = function ( value, metadata, record, row, col, ds ) {
			return Ext.Gemma.GemmaGridPanel.formatTermWithStyle( value, record.data.categoryUri );
		};
	}
	return Ext.Gemma.FactorValueGrid.categoryStyler;
};

Ext.Gemma.FactorValueGrid.getValueStyler = function() {
	if ( Ext.Gemma.FactorValueGrid.valueStyler === undefined ) {
		/* apply a CSS class depending on whether or not the characteristic has a URI.
		 */
		Ext.Gemma.FactorValueGrid.valueStyler = function ( value, metadata, record, row, col, ds ) {
			return Ext.Gemma.GemmaGridPanel.formatTermWithStyle( value, record.data.valueUri );
		};
	}
	return Ext.Gemma.FactorValueGrid.valueStyler;
};

Ext.Gemma.FactorValueGrid.flattenCharacteristics = function ( chars ) {
	var s = "";
	for ( var i=0; i<chars.length; ++i ) {
		var c = chars[i].data;
		var category = c.category.length > 0 ? c.category : "&lt;no category&gt;";
		var value = c.value.length > 0 ? c.value : "&lt;no value&gt;";
		s = s + String.format( "{0}: {1}", category, value );
		if ( i+1<chars.length ) {
			s = s + ", ";
		}
	}
	return s;
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.FactorValueGrid, Ext.Gemma.GemmaGridPanel, {

	setExperimentalFactor : function ( efId ) {
		this.experimentalFactor.id = efId;
		this.refresh( [ this.experimentalFactor ] );
	},
	
	getSelectedFactorValues : function () {
		var form = document.forms[ this.form ];
		var checkboxes = form.selectedFactorValues;
		var values = [];
		for ( var i=0; i<checkboxes.length; ++i) {
			if ( checkboxes[i].checked ) {
				values.push( checkboxes[i].value );
			}
		}
		return values;
	},
	
	reloadExperimentalFactors : function() {
		this.factorValueToolbar.reloadExperimentalFactors();
	},
	
	refresh : function( ct, p ) {
		Ext.Gemma.FactorValueGrid.superclass.refresh.call( this, ct, p );
		if ( this.onRefresh ) {
			this.onRefresh();
		}
	}
	
} );

/* Ext.Gemma.FactorValueToolbar constructor...
 * 	config is a hash with the following options:
 * 		grid is the grid that contains the factor values.
 */
Ext.Gemma.FactorValueToolbar = function ( config ) {

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
	this.factorCombo = new Ext.Gemma.ExperimentalFactorCombo( { edId : this.experimentalDesign.id } );
	var factorCombo = this.factorCombo;
	factorCombo.on( "select", function ( combo, record, index ) {
		thisToolbar.grid.setExperimentalFactor( record.id );
		createFactorValueButton.enable();
		characteristicToolbar.setExperimentalFactor( record.id );
	} );
	
	var createFactorValueButton = new Ext.Toolbar.Button( {
		text : "create",
		tooltip : "Create a new factor value",
		disabled : true,
		handler : function() {
			ExperimentalDesignController.createFactorValue(
				thisToolbar.grid.experimentalFactor,
				function() {
					thisToolbar.grid.refresh.call( thisToolbar.grid ); 
					characteristicToolbar.setExperimentalFactor( thisToolbar.grid.experimentalFactor.id );
				}
			);
		}
	} );
	
	var deleteFactorValueButton = new Ext.Toolbar.Button( {
		text : "delete",
		tooltip : "Delete selected factor values",
		disabled : false,
		handler : function() {
			ExperimentalDesignController.deleteFactorValues(
				thisToolbar.grid.experimentalFactor,
				thisToolbar.grid.getSelectedFactorValues(),
				thisToolbar.grid.refresh.bind( thisToolbar.grid )
			);
		}
	} );
	
	var items = [
		new Ext.Toolbar.TextItem( "Show Factor Values for:" ),
		new Ext.Toolbar.Spacer(),
		factorCombo,
		new Ext.Toolbar.Spacer(),
		createFactorValueButton,
		new Ext.Toolbar.Separator(),
		deleteFactorValueButton
	];
	config.items = config.items ? items.concat( config.items ) : items;
	
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.FactorValueToolbar.superclass.constructor.call( this, superConfig );
	
	var characteristicToolbar = new Ext.Gemma.FactorValueCharacteristicToolbar( { grid : thisToolbar.grid, renderTo : thisToolbar.getEl().createChild() } );
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.FactorValueToolbar, Ext.Toolbar, {

	reloadExperimentalFactors : function() {
		this.factorCombo.store.reload();
	}

} );

/* Ext.Gemma.FactorValueCharacteristicToolbar constructor...
 * 	config is a hash with the following options:
 * 		grid is the grid that contains the factor values.
 */
Ext.Gemma.FactorValueCharacteristicToolbar = function ( config ) {

	this.grid = config.grid; delete config.grid;
	this.experimentalDesign = this.grid.experimentalDesign;
	this.experimentalFactor = this.grid.experimentalFactor;
	this.factorValue = {
		id : 0,
		classDelegatingFor : "FactorValue"
	};
	
	/* keep a reference to ourselves so we don't have to worry about scope in the
	 * button handlers below...
	 */
	var thisToolbar = this;
	
	/* establish default config options...
	 */
	var superConfig = {};
	
	/* add our items in front of anything specified in the config above...
	 */
	this.factorValueCombo = new Ext.Gemma.FactorValueCombo( {
		disabled: this.experimentalFactor.id ? false : true
	} );
	var factorValueCombo = this.factorValueCombo;
	factorValueCombo.on( "select", function( combo, record, index ) {
		thisToolbar.factorValue.id = record.data.factorValueId;
		mgedCombo.enable();
	} );
		
	var mgedCombo = new Ext.Gemma.MGEDCombo( {
		disabled: true,
		emptyText: "Select a class",
		termKey: "factor"
	} );
	mgedCombo.on( "select", function ( combo, record, index ) {
		charCombo.setCategory( record.data.term, record.data.uri );
		charCombo.enable();
		createButton.enable();
	} );
	
	var charCombo = new Ext.Gemma.CharacteristicCombo( {
		disabled: true
	} );
	
	var createButton = new Ext.Toolbar.Button( {
		text : "create",
		tooltip : "Create the new characteristic",
		disabled : true,
		handler : function() {
			ExperimentalDesignController.createFactorValueCharacteristic(
				thisToolbar.factorValue,
				charCombo.getCharacteristic(),
				function() {
					thisToolbar.grid.refresh.call( thisToolbar.grid );
					thisToolbar.factorValueCombo.store.reload();
				}
			);
			// removed in response to bug 1016 mgedCombo.reset();
			charCombo.reset();			
			createButton.disable();
		}
	} );
	
	var deleteButton = new Ext.Toolbar.Button( {
		text : "delete",
		tooltip : "Delete selected characteristics",
		disabled : true,
		handler : function() {
			ExperimentalDesignController.deleteFactorValueCharacteristics(
				thisToolbar.grid.getSelectedRecords(),
				thisToolbar.grid.refresh.bind( thisToolbar.grid )
			);
		}
	} );
	this.grid.getSelectionModel().on( "selectionchange", function( model ) {
		var selected = model.getSelections();
		if ( selected.length > 0 )
			deleteButton.enable();
		else
			deleteButton.disable();
	} );
	
	var saveButton = new Ext.Toolbar.Button( {
		text : "save",
		tooltip : "Save changed characteristics",
		disabled : true,
		handler : function() {
			var edited = thisToolbar.grid.getEditedRecords();
			var callback = thisToolbar.grid.refresh.bind( thisToolbar.grid );
			ExperimentalDesignController.updateFactorValueCharacteristics( edited, callback );
			saveButton.disable();
		}
	} );
	this.grid.on( "afteredit", function( model ) {
		saveButton.enable();
	} );
	var revertButton = new Ext.Toolbar.Button( {
		text : "revert",
		tooltip : "Undo changes to selected characteristics",
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
		new Ext.Toolbar.TextItem( "Add a Characteristic to:" ),
		factorValueCombo,
		new Ext.Toolbar.Spacer(),
		mgedCombo,
		new Ext.Toolbar.Spacer(),
		charCombo,
		new Ext.Toolbar.Spacer(),
		createButton,
		new Ext.Toolbar.Spacer(),
		deleteButton,
		new Ext.Toolbar.Spacer(),
		saveButton,
		new Ext.Toolbar.Spacer(),
		revertButton
	];
	config.items = config.items ? items.concat( config.items ) : items;
	
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.FactorValueCharacteristicToolbar.superclass.constructor.call( this, superConfig );
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.FactorValueCharacteristicToolbar, Ext.Toolbar, {

	setExperimentalFactor : function ( efId ) {
		this.experimentalFactor.id = efId;
		this.factorValueCombo.setExperimentalFactor( efId );
		this.factorValueCombo.enable(); // TODO do this in the callback
	}
} );