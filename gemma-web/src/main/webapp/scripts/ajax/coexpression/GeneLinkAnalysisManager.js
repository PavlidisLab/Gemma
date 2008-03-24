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
 
	analysisGrid.render();  

	/*
 	* any data sets, which we can add to existing analysis (if they are 'virtual'). TODO: add grabber button.
 	*/
	var datasetsGrid = new Ext.Gemma.ExpressionExperimentGrid( "genelinkanalysis-alldatasets", {
		readMethod : GeneLinkAnalysisManagerController.loadExpressionExperiments.bind( this ),
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
		
	var regularSearchToolbar = new Ext.Gemma.DatasetSourceToolBar(datasetsGrid, { targetGrid : datasetsInAnalysisGrid } );
	
	var toolbar = new Ext.Gemma.AnalysisDatasetSearchToolBar(datasetsInAnalysisGrid, { taxonSearch : false, targetGrid : newAnalysisGrid } );	
	
	var newtoolbar = new Ext.Gemma.NewAnalysisToolBar(newAnalysisGrid, {  } );	
	
	datasetsGrid.render();
	datasetsInAnalysisGrid.render();
	newAnalysisGrid.render();
	
	analysisGrid.on("rowclick", 
		function(grid, rowIndex, ev ) {
			Ext.DomHelper.overwrite("messages", "");
			var row = grid.getStore().getAt(rowIndex);
			var id = row.get("id") ;
			this.analysisId = id;
			this.virtual = row.get("virtual");
			this.taxon = row.get("taxon");
			this.stringency = row.get("stringency");
			this.getStore().load( { params : [ id ] }); 
		}, datasetsInAnalysisGrid 
	);

	datasetsInAnalysisGrid.on( "keypress", 
		function( e ) {
			if (this.virtual && e.getCharCode() == Ext.EventObject.DELETE) {  
				var recs = this.getSelectionModel().getSelections();
				for( var x = 0; x < recs.length; x ++ ) { // for r in recs does not work!
					this.getStore().remove(recs[x]);
					this.getView().refresh();
				}
			}
			
		}, datasetsInAnalysisGrid 
	);
	
	newtoolbar.on("newAnalysisCreated", function(e){ this.store.reload() },newAnalysisGrid);
	
	
 
});
 
/*
 * Toolbar for creating/updating the analysis. Attach to the NewAnalysisGrid.
 */
Ext.Gemma.NewAnalysisToolBar = function ( grid, config ) {
	var bar = this;
	this.thisGrid = grid; 
	
	this.addEvents('newAnalysisCreated', 'createAnalysisError');
	 
	Ext.Gemma.NewAnalysisToolBar.superclass.constructor.call( this, {
		autoHeight : true,
		renderTo : this.thisGrid.tbar
	} );
	
	
	this.createNewAnalysis = function(analysisName, analysisDescription) {
		
		// when done, disable saving.
		Ext.getCmp('newsave').disable();
		
		var callback = function() { 
			this.fireEvent("newAnalysisCreated", this );
		};
		
		var errorHandler = function( e ) { 
			this.fireEvent("createAnalysisError", this, e);
			Ext.getCmp('newsave').enable();
			this.thisGrid.loadMask.hide();
			Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/iconWarning.gif' });  
	 		Ext.DomHelper.append("messages", {tag : 'span', html : e });  
			
		};
		
		GeneLinkAnalysisManagerController.create(
			{ 
				taxonId : this.thisGrid.taxon.id, 
				stringency : this.thisGrid.stringency, 
				name: analysisName, 
				description: analysisDescription,
				viewedAnalysisId : this.thisGrid.sourceAnalysisID ,  
				datasets : this.thisGrid.getEEIds() 
			}, 
		{ callback : callback.createDelegate(this, [], true), errorHandler : errorHandler.createDelegate(this, [], true)  }  )
	};
	
	
	this.create = function () {
		// dialog to get new name and description
		Ext.DomHelper.overwrite("messages", "");	
		Ext.getCmp('newsave').disable();
		var createDialog = new Ext.Window( {
			renderTo: 'createAnalysisDialog',
			width: 440,
			height: 400,
			shadow: true,
			minWidth: 200,
			minHeight: 150, 
			modal: true,
			layout : 'fit' 
		} );
			
		var nameField = new Ext.form.TextField({
                fieldLabel : 'Name', id: 'analysis-name' , minLength : 3
        });
		                
	    var descriptionField = new Ext.form.TextArea({fieldLabel : 'Description', id:'analysis-description', minLength : 3});
			
		var analysisForm = new Ext.FormPanel({
			labelAlign: 'top'
		});
						
		analysisForm.add(nameField);
		analysisForm.add(descriptionField);
			 
		analysisForm.addButton('Create analysis', function() {
			createDialog.hide();
			this.createNewAnalysis(nameField.getValue(), descriptionField.getValue());
		}, this );
		analysisForm.addButton('Cancel', function() { createDialog.hide(); }, createDialog);
		analysisForm.render(createDialog.body);

		createDialog.show();
		
		
	};
	
	this.clear = function() {
		Ext.DomHelper.overwrite("messages", "");
		this.thisGrid.store.removeAll();
		Ext.getCmp('newclear').disable();
	};
	
	var createBut = new Ext.Button({ id : 'newsave', text : "Save", handler : this.create, scope : this, disabled : true, tooltip : "Save the analysis" });
	
	var clearBut = new Ext.Button({ id : 'newclear', text : "Clear", handler : this.clear, scope : this, disabled : true, tooltip : "Clear the table" });
	
	
	grid.store.on("add", function() {
		Ext.DomHelper.overwrite("messages", "");
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

/*
 * Has grabber
 */
Ext.Gemma.DatasetSourceToolBar = function(grid, config ) {
	var superconfig = config || {};
	this.targetGrid = config.targetGrid; 
	
	Ext.Gemma.DatasetSourceToolBar.superclass.constructor.call( this, grid, superconfig );
	
	if (this.targetGrid) {
		var grabber = new Ext.Button({ id : 'grab', disabled: true, text : "Grab >>", handler : function( button, ev ) {
			if (this.targetGrid.virtual) {
				this.targetGrid.getStore().add( grid.getSelectionModel().getSelections());
				this.targetGrid.getView().refresh();
			}
		}, scope : this });
		this.addFill();
		this.add( grabber );
		grid.store.on("load", function() {
			if (this.targetGrid.virtual) {
				Ext.getCmp('grab').enable();
			} 
		}, this );
		
		this.targetGrid.store.on("load", function() {
			if (this.targetGrid.virtual) {
				Ext.getCmp('grab').enable();
			} 
		}, this);
		
	}
	
	
	
};

Ext.Gemma.AnalysisDatasetSearchToolBar = function( grid, config ) {
	var superconfig = config || {};
	this.owningGrid = grid;
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
		if (this.virtual) {
			saveButton.enable();
		}
		resetButton.enable();
	} );
	
	if (this.targetGrid) {
		var grabber = new Ext.Button({ id : 'grab', disabled: true, text : "Grab >>", handler : function( button, ev ) {
			
			var id = this.owningGrid.analysisId;
			
			// Can't mix two analyses.
			if (id != this.targetGrid.sourceAnalysisID ) {
				this.targetGrid.getStore().removeAll();
			}
		
			this.targetGrid.getStore().add( grid.getSelectionModel().getSelections());
			this.targetGrid.getView().refresh();
			this.targetGrid.sourceAnalysisID = this.owningGrid.analysisId;
			this.targetGrid.stringency = this.owningGrid.stringency;
			this.targetGrid.taxon = this.owningGrid.taxon;
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
		{ id: 'taxon', header: "Taxon", dataIndex: "taxon", renderer : function( r ) { return r.commonName; } },
		{ id: 'stringency', header : "Stringency", dataIndex : "stringency"},
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
			{ name:"taxon" },
			{ name:"virtual", type:"string"},
			{ name:"stringency", type:"int"}
		] );
	}
	return Ext.Gemma.GeneLinkAnalysisGrid.record;
};
 

Ext.extend(Ext.Gemma.NewAnalysisToolBar, Ext.Toolbar, {
});

Ext.extend(Ext.Gemma.AnalysisDatasetGrid, Ext.Gemma.ExpressionExperimentGrid, {
});

Ext.extend(Ext.Gemma.AnalysisDatasetSearchToolBar, Ext.Gemma.DatasetSearchToolBar, {});

Ext.extend(Ext.Gemma.DatasetSourceToolBar, Ext.Gemma.DatasetSearchToolBar, {});

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