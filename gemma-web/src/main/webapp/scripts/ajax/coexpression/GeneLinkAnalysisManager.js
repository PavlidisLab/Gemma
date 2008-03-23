/*
* Interface for modifying and creating GeneCoexpressionAnalyses.
* Author: Paul
* $Id$
*/
Ext.namespace('Ext.Gemma', 'Ext.Gemma.GeneLinkAnalysisGrid');

Ext.onReady( function() {
	Ext.QuickTips.init();
	//Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );
	
	var admin = dwr.util.getValue("hasAdmin");
	
	var analysisGrid = new Ext.Gemma.GeneLinkAnalysisGrid( "genelinkanalysis-analysisgrid", {
		readMethod : GeneLinkAnalysisManagerController.getCannedAnalyses.bind( this ),
		editable : admin,
		title : "Available analyses"
	} );
	
	var analysisEditToolbar = new Ext.Gemma.AnalysisEditToolBar(analysisGrid, { targetGrid : datasetsInAnalysisGrid });
	
	analysisGrid.render();  

	/*
 	* any data sets, which we can add to existing analysis (if they are 'virtual'). TODO: add grabber button.
 	*/
	var datasetsGrid = new Ext.Gemma.ExpressionExperimentGrid( "genelinkanalysis-alldatasets", {
		readMethod : ExtCoexpressionSearchController.findExpressionExperiments.bind( this ),
		editable : admin,
		title : "Any dataset",
		pageSize : 10, 
		ddGroup : "analysisedit"} );

	var datasetsInAnalysisGrid = new Ext.Gemma.AnalysisDatasetGrid( "genelinkanalysis-datasetgrid",  {
		readMethod : GeneLinkAnalysisManagerController.getExperimentsInAnalysis.bind( this ),
		editable : admin,
		title : "Datasets in selected analysis",
		pageSize : 10, 
		ddGroup : "analysisedit"
	} );
	
	var newAnalysisGrid = new Ext.Gemma.ExpressionExperimentGrid( "genelinkanalysis-newanalysis", {
		readMethod : GeneLinkAnalysisManagerController.loadExpressionExperiments.bind( this ),
		editable : false,
		title : "New analysis",
		pageSize : 10,
		ddGroup : "analysisedit"
	} );
		
	var regularSearchToolbar = new Ext.Gemma.DatasetSearchToolBar(datasetsGrid, { targetGrid : datasetsInAnalysisGrid } );
	
	var toolbar = new Ext.Gemma.AnalysisDatasetSearchToolBar(datasetsInAnalysisGrid, { taxonSearch : false, targetGrid : newAnalysisGrid } );	
	
	var newtoolbar = new Ext.Gemma.NewAnalysisToolBar(newAnalysisGrid, {  } );	
	
	datasetsGrid.render();
	datasetsInAnalysisGrid.render();
	newAnalysisGrid.render();
	
	analysisGrid.on("rowclick", 
		function(grid, rowIndex, ev ) {
			var row = grid.getStore().getAt(rowIndex);
			var id = row.id ;
			this.analysisId = id;
			this.getStore().load( { params : [ id ] }); 
		}, datasetsInAnalysisGrid 
	);
	
	datasetsInAnalysisGrid.on("load", function() {
		// Need to update 
	}, toolbar );

	datasetsInAnalysisGrid.on( "keypress", 
		function( e ) {
			if ( e.getCharCode() == Ext.EventObject.DELETE ) {  
				var recs = this.getSelectionModel().getSelections();
				for( var x = 0; x < recs.length; x ++ ) { // for r in recs does not work!
					this.getStore().remove(recs[x]);
					this.getView().refresh();
				}
			}
			
		}, datasetsInAnalysisGrid 
	);
 
});

Ext.Gemma.AnalysisEditToolBar = function ( grid, config ) {
	var bar = this;
	var thisGrid = grid;
	this.targetGrid = config.targetGrid;
	 
	// no buttons yet
	 
	Ext.Gemma.AnalysisEditToolBar.superclass.constructor.call( this, {
		autoHeight : true,
		renderTo : thisGrid.tbar
	} );	 
};

/*
 * Toolbar for creating/updating the analysis. Attach to the NewAnalysisGrid.
 */
Ext.Gemma.NewAnalysisToolBar = function ( grid, config ) {
	var bar = this;
	this.thisGrid = grid;
	this.targetGrid = config.targetGrid;
		
	Ext.Gemma.NewAnalysisToolBar.superclass.constructor.call( this, {
		autoHeight : true,
		renderTo : this.thisGrid.tbar
	} );
	
	this.create = function () {
		// dialog to get new name and description
		alert("You clicked 'save'");
		
		// save to server. This should be a one-time event. 
		// The analysis should then get added to the 'available analyses' table. The user will be able to edit it later from the middle table.
		
		// when done, disable it.
		Ext.getCmp('newsave').disable();
	};
	
	this.clear = function() {
		this.thisGrid.store.removeAll();
		Ext.getCmp('newclear').disable();
	};
	
	var createBut = new Ext.Button({ id : 'newsave', text : "Save", handler : this.create, scope : this, disabled : true, tooltip : "Save the analysis" });
	
	var clearBut = new Ext.Button({ id : 'newclear', text : "Clear", handler : this.clear, scope : this, disabled : true, tooltip : "Clear the table" });
	
	
	grid.store.on("add", function() {
		Ext.getCmp('newclear').enable();
		Ext.getCmp('newsave').enable();
	});
	
	grid.store.on("remove", function() {
		Ext.getCmp('newsave').enable();
	});
	
	this.addButton( createBut );
	this.addButton( clearBut ); 
};

/*
 * Grid to display the datasets in an analysis; allows filtering (for creating new ones) and editing (if it is virtual).
 */
Ext.Gemma.AnalysisDatasetGrid = function( div, config ) {
	Ext.Gemma.AnalysisDatasetGrid.superclass.constructor.call( this, div, config );
};

Ext.Gemma.AnalysisDatasetSearchToolBar = function( grid, config ) {
	var superconfig = config || {};
	this.targetGrid = config.targetGrid; 
	
	Ext.Gemma.AnalysisDatasetSearchToolBar.superclass.constructor.call( this, grid, superconfig );
	
	this.eeSearchField = new Ext.Gemma.AnalysisDatasetSearchField( {
		fieldLabel : "Experiment keywords"
	} );
	
	this.updateAnalysis = function() {
		alert("You clicked 'save'");
		// get the ids from the records in the store
		
		// send back to the server
		// GeneLinkAnalysisManagerController.update()
	};
	
	this.reset = function() {
		if ( grid.analysisId ) {
			grid.getStore().load( { params : [  grid.analysisId ] });
		} 
	};
	
	var saveButton = new Ext.Button( {
		id : 'save',
		text : "Save",
		handler: this.updateAnalysis, 
		scope : this,
		disabled : true,
		tooltip : "Save changes to this analysis"
		}
	);
	
	var resetButton = new Ext.Button( {
		id : 'reset',
		text : "Reset",
		handler: this.reset, 
		scope : this,
		disabled : true,
		tooltip : "Restore the list of datasets for the analysis"
		}
	);
	 
	this.addFill();
	this.add( saveButton );
	this.add( resetButton );
	
	grid.store.on("remove", function() { 
		saveButton.enable();
		resetButton.enable();
	} );
	
	if (this.targetGrid) {
		var grabber = new Ext.Button({ id : 'grab', disabled: true, text : "Grab >>", handler : function( button, ev ) {
			this.targetGrid.getStore().add( grid.getSelectionModel().getSelections());
			this.targetGrid.getView().refresh();
		}, scope : this });
		this.add( grabber );
		grid.store.on("load", function() {
			Ext.getCmp('grab').enable(); 
		}, this );
	}
	
};

Ext.Gemma.AnalysisDatasetSearchField = function( config ) {
	Ext.Gemma.AnalysisDatasetSearchField.superclass.constructor.call( this, config );
};

/*
* Constructor
*/
Ext.Gemma.GeneLinkAnalysisGrid =  function ( div, config ) { 
	this.readMethod = config.readMethod; delete config.readMethod;
	this.readParams = config.readParams; delete config.readParams;
	this.editable = config.editable; delete config.editable;
	this.pageSize = config.pageSize; delete config.pageSize;
	
	/* keep a reference to ourselves to avoid convoluted scope issues below...
	 */
	var thisGrid = this;
	
	/* establish default config options...
	 */
	var superConfig = {
	 	renderTo : div
	};
	
	if ( this.pageSize ) {
		superConfig.ds = new Ext.Gemma.PagingDataStore( {
			proxy : new Ext.data.DWRProxy( this.readMethod ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.GeneLinkAnalysisGrid.getRecord() ),
			pageSize : this.pageSize
		} );
		superConfig.bbar = new Ext.Gemma.PagingToolbar( {
			pageSize : this.pageSize,
			store : superConfig.ds
		} );
	} else {
		superConfig.ds = new Ext.data.Store( {
			proxy : new Ext.data.DWRProxy( this.readMethod ),
			reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.GeneLinkAnalysisGrid.getRecord() )
		} );
	}
	
	superConfig.cm = new Ext.grid.ColumnModel( [
		{ id: 'name', header: "Name", dataIndex: "name" },
		{ id: 'description', header: "Description", dataIndex: "description" },
		{ id: 'datasets', header: "Num datasets", dataIndex: "numDatasets" },
		{ id: 'taxon', header: "Taxon", dataIndex: "taxon" },
		{ id: 'virtual', header: "Virtual", dataIndex: "virtual" }
	] );
	
	superConfig.cm.defaultSortable = true; 
	superConfig.autoExpandColumn = 'description';

	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.GeneLinkAnalysisGrid.superclass.constructor.call( this, superConfig );
	
	this.getStore().on( "load", function () {
		this.autoSizeColumns();
		this.doLayout();
	}, this );
	
	if ( ! this.noInitialLoad ) {
		this.getStore().load( { params : this.getReadParams() } );
	}
};


/* 
 * Fields match the Analysis object returned.
 */
Ext.Gemma.GeneLinkAnalysisGrid.getRecord = function() {
	if ( Ext.Gemma.GeneLinkAnalysisGrid.record === undefined ) {
		Ext.Gemma.GeneLinkAnalysisGrid.record = Ext.data.Record.create( [
			{ name:"id", type:"int" },
			{ name:"name", type:"string" },
			{ name:"description", type:"string" },
			{ name:"numDatasets", type:"int" },
			{ name:"taxon", type:"object" },
			{ name:"virtual", type:"string"}
		] );
	}
	return Ext.Gemma.GeneLinkAnalysisGrid.record;
};

Ext.extend(Ext.Gemma.AnalysisEditToolBar, Ext.Toolbar, {
});

Ext.extend(Ext.Gemma.NewAnalysisToolBar, Ext.Toolbar, {
});

Ext.extend(Ext.Gemma.AnalysisDatasetGrid, Ext.Gemma.ExpressionExperimentGrid, {
});

Ext.extend(Ext.Gemma.AnalysisDatasetSearchToolBar, Ext.Gemma.DatasetSearchToolBar, {});

Ext.extend(Ext.Gemma.AnalysisDatasetSearchField, Ext.Gemma.DatasetSearchField, {
	
	findDatasets : function ( filterFrom ) {
		var params = [ this.getValue(), this.taxon ? this.taxon.id : -1 ];
		params.push( filterFrom );
		if ( params == this.lastParams ) {
			return;
		}
		if ( this.fireEvent('beforesearch', this, params ) !== false ) {
			this.lastParams = params;
			ExtCoexpressionSearchController.filterExpressionExperiments( params[0], params[1], params[2], this.foundDatasets.bind( this ) );
        }
	}
	
});

/*
* Displays the available analyses.
*/
Ext.extend( Ext.Gemma.GeneLinkAnalysisGrid, Ext.Gemma.GemmaGridPanel, {

	initComponent : function() {
        Ext.Gemma.GeneLinkAnalysisGrid.superclass.initComponent.call(this);
        this.addEvents(
            {'loadAnalysis': true}
        );
    },

	getReadParams : function() {
		return ( typeof this.readParams == "function" ) ? this.readParams() : this.readParams;
	}
});