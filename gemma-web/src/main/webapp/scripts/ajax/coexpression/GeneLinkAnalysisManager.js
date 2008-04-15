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
		readMethod : ExtCoexpressionSearchController.getCannedAnalyses.bind( this ),
		editable : admin,
		title : "Available analyses"
	} );
 
	analysisGrid.render();  

 
	var sourceAnalysisGrid = new Ext.Gemma.ExpressionExperimentGrid( "genelinkanalysis-datasetgrid",  {
		readMethod : ExpressionExperimentController.loadExpressionExperiments.bind( this ),
		editable : false,
		admin : admin,
		title : "Datasets in source analysis",
		pageSize : 20, 
		ddGroup : "analysisedit",
		rowExpander : true
	} );
	
	sourceAnalysisGrid.getStore().on( "load", function () {
		toolbar.updateDatasets();
	}, this );
	
	var virtualAnalysisGrid = new Ext.Gemma.ExpressionExperimentGrid( "genelinkanalysis-newanalysis", {
		readMethod : ExpressionExperimentController.loadExpressionExperiments.bind( this ),
		editable : admin,
		title : "Virtual analysis",
		pageSize : 20,
		ddGroup : "analysisedit",
		rowExpander : true
	} ); 
	
	
	
	var toolbar = new Ext.Gemma.SourceAnalysisToolBar(sourceAnalysisGrid, { taxonSearch : false, targetGrid : virtualAnalysisGrid } );	
	
	
	var refresh = function( e ){ 
			this.store.reload( { callback : function(r,options,ok) {
				// focus on the newly loaded one.
				var recind = this.store.find("id", e);
				var rec = this.store.getAt(recind);
				this.getSelectionModel().selectRecords([rec], false); 
			}});
	}
	
	if ( admin ) {
		var newtoolbar = new Ext.Gemma.EditVirtualAnalysisToolBar(virtualAnalysisGrid, {  } );
		newtoolbar.on("newAnalysisCreated", refresh,analysisGrid);
		newtoolbar.on("analysisUpdated",refresh,analysisGrid);
	}	
	 
	sourceAnalysisGrid.render();
	virtualAnalysisGrid.render();
	
	var showSourceAnalysis = function(target, rowIndex, ev ) {
		// Load the source analysis, or the selected one, if it is real.
		Ext.DomHelper.overwrite("messages", "");
		toolbar.reset();
		
		var row;
		if (target.grid) { // selectionmodel
			row = target.grid.getStore().getAt(rowIndex);
		} else {
			row = target.getStore().getAt(rowIndex);
		}
		var id = row.get("id") ;
		
		var virtual = row.get("virtual");
		var ids = row.get("datasets");
		this.taxon = row.get("taxon");
		this.stringency = row.get("stringency");
		this.setTitle(row.get("name"));
		if ( virtual ) {
			id = row.get("viewedAnalysisId");
			var callback = function( d ) {
				// load the data sets.
				this.getStore().load( { params : [ d ] });  
			}
			// Go back to the server to get the ids of the experiments the selected analysis' parent has.
			GeneLinkAnalysisManagerController.getExperimentIdsInAnalysis( id, {callback : callback.createDelegate(this, [], true) });
			this.analysisId = id;
		} else {		
			this.analysisId = id;
			this.getStore().load( { params : [ ids ] }); 
		} 
	};
	
	var showVirtualAnalysis = function(target, rowIndex, ev ) {
		// Show the selected virtual analysis members in the right-hand grid, or clear if it is not virtual.
		Ext.DomHelper.overwrite("messages", "");
		var row;
		if (target.grid) { // selectionmodel
			row = target.grid.getStore().getAt(rowIndex);
		} else {
			row = target.getStore().getAt(rowIndex);
		}
		var id = row.get("id") ;
					
		var virtual = row.get("virtual")
		if ( virtual ) {
			this.sourceAnalysisID = row.get("viewedAnalysisId");
			this.analysisId = id;
			this.analysisName = row.get("name");
			this.analysisDescription = row.get("description");
			this.virtual = true;
			this.taxon = row.get("taxon");
			this.stringency = row.get("stringency");
			this.getStore().load( { params : [ row.get("datasets") ] }); 
			this.setTitle("Virtual analysis : " + row.get("name"));
		} else {
			this.analysisId = null; 
			this.analysisName = null;
			this.analysisDescription = null;
			this.setTitle("Virtual analysis (new)");
			this.getStore().removeAll();
		}
			
	};
	
	analysisGrid.getSelectionModel().on("rowselect", 
		showSourceAnalysis, sourceAnalysisGrid 
	);
	
	analysisGrid.getSelectionModel().on("rowselect", 
		showVirtualAnalysis, virtualAnalysisGrid
	);
  
	
});
 
/*
 * Toolbar for creating/updating the analysis. Attach to the virtualAnalysisGrid.
 */
Ext.Gemma.EditVirtualAnalysisToolBar = function ( grid, config ) {
	var bar = this;
	this.thisGrid = grid; 
	this.addEvents('newAnalysisCreated', 'createAnalysisError');
	 
	Ext.Gemma.EditVirtualAnalysisToolBar.superclass.constructor.call( this, {
		autoHeight : true,
		renderTo : this.thisGrid.tbar
	} );
	
	var createDialog = new Ext.Window( {
			el: 'createAnalysisDialog',
			title : "Save new analysis",
			width: 440,
			height: 400,
			shadow: true,
			minWidth: 200,
			minHeight: 150, 
			modal: true,
			layout : 'fit' 
	 } );
	
	this.createNewAnalysis = function(analysisName, analysisDescription) {
		
		if (this.thisGrid.analysisId) {
			this.updateAnalysis(analysisName, analysisDescription);
			return;
		}
		
		var callback = function( newid ) { 
			Ext.getCmp('newsave').enable();
			this.fireEvent("newAnalysisCreated", this, newid );
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
	
	
	
	this.updateAnalysis = function(analysisName, analysisDescription) {
	 
		var callback = function() { 
			Ext.getCmp('newsave').enable(); 
			this.fireEvent("analysisUpdated", this, this.thisGrid.analysisId );
		};
		
		var errorHandler = function( e ) { 
			this.fireEvent("updateAnalysisError", this, e);
			Ext.getCmp('newsave').enable();
			this.thisGrid.loadMask.hide();
			Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/iconWarning.gif' });  
	 		Ext.DomHelper.append("messages", {tag : 'span', html : e });  
		};
		
		GeneLinkAnalysisManagerController.update(
			{ 
				name: analysisName, 
				id : this.thisGrid.analysisId,
				description: analysisDescription,
				datasets : this.thisGrid.getEEIds() 
			}, 
		{ callback : callback.createDelegate(this, [], true), errorHandler : errorHandler.createDelegate(this, [], true)  }  )
	};
	
	
	this.reset = function() {
		if ( this.thisGrid.analysisId ) {
			var callback = function( d ) {
				// load the data sets.
				this.getStore().load( { params : [ d ] });  
			}
			// Go back to the server to get the ids of the experiments the selected analysis has.
			var id = this.thisGrid.analysisId;
			GeneLinkAnalysisManagerController.getExperimentIdsInAnalysis( id, {callback : callback.createDelegate(this.thisGrid, [], true) });
			
		} 
	};
	
	
	this.create = function () {
		// dialog to get new name and description
		
		
		Ext.DomHelper.overwrite("messages", "");	
		Ext.getCmp('newsave').disable();
		
		if (!createDialog.rendered) {
			createDialog.render();
		
			var nameField = new Ext.form.TextField({
	                fieldLabel : 'Name', id: 'analysis-name' , minLength : 3, width : 100
	        });
			                
		    var descriptionField = new Ext.form.TextArea({fieldLabel : 'Description', id:'analysis-description', minLength : 3, width : 300});
				
			var analysisForm = new Ext.FormPanel({
				labelAlign: 'top'
			});
			
			if (this.thisGrid.analysisName) {
				nameField.setValue(this.thisGrid.analysisName);
			}
			
			if (this.thisGrid.analysisDescription) {
				descriptionField.setValue(this.thisGrid.analysisDescription);
			}
							
			analysisForm.add(nameField);
			analysisForm.add(descriptionField);
			 
			analysisForm.addButton('Save/Update', function() {
				createDialog.hide();
				this.createNewAnalysis(nameField.getValue(), descriptionField.getValue());
			}, this );
			analysisForm.addButton('Cancel', function() { createDialog.hide(); }, createDialog);
			analysisForm.render(createDialog.body);
		}
		createDialog.show();
		
		
	};
	
	this.clear = function() {
		Ext.DomHelper.overwrite("messages", "");
		this.thisGrid.store.removeAll();
		Ext.getCmp('newclear').disable();
	};
	
	var createBut = new Ext.Button({ id : 'newsave', text : "Save", handler : this.create, scope : this, disabled : false, tooltip : "Save the analysis" });
	
	var clearBut = new Ext.Button({ id : 'newclear', text : "Clear", handler : this.clear, scope : this, disabled : true, tooltip : "Clear the table" });
	
	var resetBut = new Ext.Button({ id : 'newreset', text : "Reset", handler : this.reset, scope : this, disabled : true, tooltip : "Reset to stored version" });
	
	grid.store.on("add", function() {
		Ext.DomHelper.overwrite("messages", "");
		
		if ( grid.analysisId ) {
			Ext.getCmp('newreset').enable();
		} else {
			Ext.getCmp('newclear').enable();
		}	
		Ext.getCmp('newsave').enable();
		
		
	});
	
	grid.store.on("remove", function() {
		Ext.getCmp('newsave').enable();
		if (grid.analysisId ) {
			Ext.getCmp('newreset').enable();
		}
	});
	
	
	this.addButton( createBut );
	this.addButton( clearBut ); 
	this.addButton( resetBut ); 
};


Ext.Gemma.SourceAnalysisToolBar = function( grid, config ) {
	var superconfig = config || {};
	this.owningGrid = grid;
	this.targetGrid = config.targetGrid; 
	superconfig.filtering = true;
	
	Ext.Gemma.SourceAnalysisToolBar.superclass.constructor.call( this, grid, superconfig );
	
	if (this.targetGrid && this.targetGrid.editable ) {
		var grabber = new Ext.Button({ id : 'grab', disabled: true, text : "Grab >>", handler : function( button, ev ) {
			
			var id = this.owningGrid.analysisId;
			
			// Can't mix two analyses.
			if (id != this.targetGrid.sourceAnalysisID ) {
				this.targetGrid.getStore().removeAll();
			} 
			this.targetGrid.getStore().add(grid.getSelectionModel().getSelections());
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
			{ name:"virtual", type:"bool" },
			{ name:"stringency", type:"int"},
			{ name:"viewedAnalysisId", type:"int"},
			{ name:"datasets"}
		] );
	}
	return Ext.Gemma.GeneLinkAnalysisGrid.record;
};
 

Ext.extend(Ext.Gemma.EditVirtualAnalysisToolBar, Ext.Toolbar, {
});

Ext.extend(Ext.Gemma.SourceAnalysisToolBar, Ext.Gemma.DatasetSearchToolBar, {
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