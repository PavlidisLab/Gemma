Ext.namespace('Ext.Gemma');

Ext.Gemma.BioMaterialEditor = function ( config ) {
	return {
		expressionExperiment : {
			id : config.eeId,
			classDelegatingFor : "ExpressionExperiment"
		},
		dwrCallback : function( data ) {
			config.data = data;
			this.grid = new Ext.Gemma.BioMaterialGrid( config );
			this.grid.refresh = this.init.bind( this );
			this.grid.render();
		},
		init : function () {
			if ( this.grid ) {
				// grid.destroy() seems to be broken...
				try {
					this.grid.destroy();
				} catch (e) {}
			}
			ExperimentalDesignController.getBioMaterials( this.expressionExperiment, this.dwrCallback.bind( this ) );
		}
	};
};

/* Ext.Gemma.BioMaterialGrid constructor...
 * 	config is a hash with the following options:
 * 		data	an array containing the data to display in the grid
 */
Ext.Gemma.BioMaterialGrid = function ( config ) {

	this.backingArray = config.data; delete config.data;
	
	/* establish default config options...
	 */
	var superConfig = {};
	
	var data = Ext.Gemma.BioMaterialGrid.transformData( this.backingArray );
	superConfig.ds = new Ext.data.Store( {
		proxy : new Ext.data.MemoryProxy( data ),
		reader : new Ext.data.ArrayReader( { }, Ext.Gemma.BioMaterialGrid.createRecord( this.backingArray[0] ) )
	} );
	superConfig.ds.load();
	
	superConfig.cm = Ext.Gemma.BioMaterialGrid.createColumnModel.call( this, this.backingArray[0] );
	superConfig.cm.defaultSortable = true;
	superConfig.plugins = Ext.Gemma.BioMaterialGrid.getRowExpander();
	superConfig.viewConfig = { forceFit: true };
	
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.BioMaterialGrid.superclass.constructor.call( this, superConfig );
	
	/* these functions have to happen after we've called the super-constructor so that we know
	 * we're a Grid...
	 */
	this.getStore().on( "load", function () {
		this.autoSizeColumns();
		this.doLayout();
	}, this );
	
	this.on( "afteredit", function( e ) {
		var factorId = this.getColumnModel().getColumnId( e.column );
		var combo = this.factorValueCombo[factorId];
		var fvvo = combo.getFactorValue.call( combo );
		e.record.set( factorId, String.format( "fv{0}", fvvo.factorValueId ) );
		this.getView().refresh();
	} );
	
	var tbar = new Ext.Gemma.BioMaterialToolbar( { grid : this, renderTo : this.tbar } );
};

/* static methods
 */
Ext.Gemma.BioMaterialGrid.transformData = function( incoming ) {
	var data = [];
	for (var i=0; i<incoming.length; ++i) {
		var bmvo = incoming[i];
		data[i] = [
			bmvo.id,
			bmvo.name,
			bmvo.description,
			bmvo.characteristics,
			bmvo.assayName,
			bmvo.assayDescription
		];
		for ( factorId in incoming[i].factors ) {
			data[i].push( incoming[i].factorIdToFactorValueId[factorId] );
			//incoming[i][factorId] = incoming[i].factorIdToFactorValueId[factorId];
		}
	}
	return data;
};

Ext.Gemma.BioMaterialGrid.createRecord = function( row ) {
	var fields = [
		{ name:"id", type:"int" },
		{ name:"bmName", type:"string" },
		{ name:"bmDesc", type:"string" },
		{ name:"bmChars", type:"string" },
		{ name:"baName", type:"string" },
		{ name:"baDesc", type:"string" }
	];
	for ( factorId in row.factors ) {
		fields.push( { name:factorId, type:"string" } );
	}
	var record = Ext.data.Record.create( fields );
	return record;
};

Ext.Gemma.BioMaterialGrid.createColumnModel = function( row ) {
	var columns = [
		Ext.Gemma.BioMaterialGrid.getRowExpander(),
		{ id: "bm", header:"BioMaterial", dataIndex:"bmName" },
		{ id: "ba", header:"BioAssay", dataIndex:"baName" }
	];
	this.fvIdToDescription = row.factorValues;
	this.columnRenderer = Ext.Gemma.BioMaterialGrid.createValueRenderer( row.factorValues );
	this.factorValueCombo = [];
	for ( factorId in row.factors ) {
		var efId = factorId.substring(6); // strip "factor" from the id...
		this.factorValueCombo[factorId] = new Ext.Gemma.FactorValueCombo( { efId: efId } );
		columns.push( { id: factorId, header:row.factors[factorId], dataIndex:factorId, renderer:this.columnRenderer, editor:this.factorValueCombo[factorId] } );
	}
	return new Ext.grid.ColumnModel( columns );
};

Ext.Gemma.BioMaterialGrid.getRowExpander = function() {
	if ( Ext.Gemma.BioMaterialGrid.rowExpander === undefined ) {
		Ext.Gemma.BioMaterialGrid.rowExpander = new Ext.grid.RowExpander( {
			tpl : new Ext.Template(
				"<dl style='margin-left: 1em; margin-bottom: 2px;'><dt>BioMaterial {bmName}</dt><dd>{bmDesc}<br>{bmChars}</dd>",
				"<dt>BioAssay {baName}</dt><dd>{baDesc}</dd></dl>"
			)
		} );
	}
	return Ext.Gemma.BioMaterialGrid.rowExpander;
};

Ext.Gemma.BioMaterialGrid.createValueRenderer = function( factorValues ) {
	return function ( value, metadata, record, row, col, ds ) {
		return factorValues[value] ? factorValues[value] : value;
	};
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.BioMaterialGrid, Ext.Gemma.GemmaGridPanel, {

	reloadFactorValues : function() {
		for ( var factorId in this.factorValueCombo ) {
			if ( factorId.substring(0, 6) == "factor" ) {
				var combo = this.factorValueCombo[factorId];
				var column = this.getColumnModel().getColumnById( factorId );
				combo.setExperimentalFactor( combo.experimentalFactor.id, function( r, options, success ) {
					var fvs = {};
					for ( var i=0; i<r.length; ++i ) {
						fvs[ "fv" + r[i].data.factorValueId ] = r[i].data.factorValueString;
					}
					var renderer = Ext.Gemma.BioMaterialGrid.createValueRenderer( fvs );
					column.renderer = renderer;
					this.getView().refresh();
				} );
			}
		}
	}
	
} );

/* Ext.Gemma.BioMaterialToolbar constructor...
 * 	config is a hash with the following options:
 * 		grid is the grid that contains the factor values.
 */
Ext.Gemma.BioMaterialToolbar = function ( config ) {

	this.grid = config.grid; delete config.grid;
	
	/* keep a reference to ourselves so we don't have to worry about scope in the
	 * button handlers below...
	 */
	var thisToolbar = this;
	
	/* establish default config options...
	 */
	var superConfig = {};
	
	/* add our items in front of anything specified in the config above...
	 */
	var saveButton = new Ext.Toolbar.Button( {
		text : "save",
		tooltip : "Save changed biomaterials",
		disabled : true,
		handler : function() {
			var edited = thisToolbar.grid.getEditedRecords();
			var bmvos = [];
			for ( var i=0; i<edited.length; ++i ) {	
				var row = edited[i];
				var bmvo = {
					id : row.id,
					factorIdToFactorValueId : {}
				};
				for ( property in row ) {
					if ( property.substring(0, 6) == "factor" ) {
						bmvo.factorIdToFactorValueId[property] = row[property];
					}
				}
				bmvos.push( bmvo );
			}
			var callback = thisToolbar.grid.refresh.bind( thisToolbar.grid );
			ExperimentalDesignController.updateBioMaterials( bmvos, callback );
			saveButton.disable();
		}
	} );
	this.grid.on( "afteredit", function( model ) {
		saveButton.enable();
	} );
	
	var revertButton = new Ext.Toolbar.Button( {
		text : "revert",
		tooltip : "Undo changes to selected biomaterials",
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
	
	var refreshButton = new Ext.Toolbar.Button( {
		text : "expand all",
		tooltip : "Show all biomaterial details",
		handler : function() {
			var expander = Ext.Gemma.BioMaterialGrid.getRowExpander()
			for ( var i=0; thisToolbar.grid.getView().getRow( i ); ++i ) {
				expander.expandRow( i );
			}
		}
	} );
	
	var items = [
		new Ext.Toolbar.TextItem( "Make changes in the grid below" ),
		new Ext.Toolbar.Spacer(),
		saveButton,
		new Ext.Toolbar.Separator(),
		revertButton,
		new Ext.Toolbar.Separator(),
		refreshButton
	];
	config.items = config.items ? items.concat( config.items ) : items;
	
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.BioMaterialToolbar.superclass.constructor.call( this, superConfig );
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.BioMaterialToolbar, Ext.Toolbar, {

} );