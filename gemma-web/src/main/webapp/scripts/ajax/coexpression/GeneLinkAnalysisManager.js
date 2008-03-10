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

	var datasetsInAnalysisGrid = new Ext.Gemma.ExpressionExperimentGrid( "genelinkanalysis-datasetgrid",  {
		readMethod : GeneLinkAnalysisManagerController.getExperimentsInAnalysis.bind( this ),
		editable : admin,
		title : "Datasets in selected analysis",
		pageSize : 10,
		ddGroup : "analysisedit"
	} );
	
	var datasetGrid = new Ext.Gemma.ExpressionExperimentGrid( "genelinkanalysis-datasetchoosegrid", {
		readMethod : GeneLinkAnalysisManagerController.loadExpressionExperiments.bind( this ),
		editable : false,
		title : "Available datasets",
		pageSize : 10,
		ddGroup : "analysisedit"
	} );
		
	var toolbar = new Ext.Gemma.DatasetSearchToolBar(datasetGrid, { targetGrid : datasetsInAnalysisGrid } );	
	
	datasetGrid.render();
	datasetsInAnalysisGrid.render();
	
	analysisGrid.on("rowclick", 
		function(grid, rowIndex, ev ) {
			var row = grid.getStore().getAt(rowIndex);
			var id = row.id ;
			this.getStore().load( { params : [ id ] }); 
		}, datasetsInAnalysisGrid 
	);

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
	
	var createBut = new Ext.Button({text : "Create", handler : create, scope : this });
	
	var updateBut = new Ext.Button({text : "Update", handler : update, scope : this });
	
	// disable the update button if selected is virtual
	
	// enable the create button if it is virtual
	
	// on create, ask for name and description. After adding data sets, make dirty - user has to click update.
	
	var update = function() {
		// only if the selected item is dirty.
	};
	
	var create = function () {
		// dialog to get new name and description
	};
	
	Ext.Gemma.AnalysisEditToolBar.superclass.constructor.call( this, {
		autoHeight : true,
		renderTo : thisGrid.tbar
	} );	
	
	this.addButton( createBut );
	this.addSpacer();
	this.addButton( updateBut );
}

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