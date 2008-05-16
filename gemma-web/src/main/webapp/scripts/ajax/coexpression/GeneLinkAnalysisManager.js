/*
 * Interface for modifying and creating GeneCoexpressionAnalyses. Author: Paul
 * $Id$
 */
Ext.namespace('Ext.Gemma', 'Ext.Gemma.GeneLinkAnalysisGrid');

Ext.onReady(function() {
	Ext.QuickTips.init();
	// Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );

	var admin = dwr.util.getValue("hasAdmin");

	this.analysisGrid = new Ext.Gemma.GeneLinkAnalysisGrid({
		renderTo : "genelinkanalysis-analysisgrid",
		readMethod : ExtCoexpressionSearchController.getCannedAnalyses
				.bind(this),
		editable : admin,
		height : 200,
		title : "Available analyses"
	});

	this.analysisGrid.render();

	var virtualAnalysisGrid = new Ext.Gemma.ExpressionExperimentGrid({
		renderTo : "genelinkanalysis-newanalysis",
		admin : admin,
		title : "Virtual analysis",
		pageSize : 20,
		height : 400,
		ddGroup : "analysisedit",
		rowExpander : true,
		tbar : new Ext.Gemma.EditVirtualAnalysisToolBar()
	});

	this.sourceAnalysisGrid = new Ext.Gemma.ExpressionExperimentGrid({
		renderTo : "genelinkanalysis-datasetgrid",

		editable : false,
		admin : admin,
		height : 400,
		title : "Datasets in source analysis",
		pageSize : 20,
		ddGroup : "analysisedit",
		rowExpander : true,
		tbar : new Ext.Gemma.SourceAnalysisToolBar({
			taxonSearch : false,
			targetGrid : virtualAnalysisGrid
		})
	});

	sourceAnalysisGrid.getTopToolbar().on('after.tbsearch', function(results) {
		this.getStore().removeAll();
		if (results.length > 0) {
			this.getStore().load({
				params : [results]
			});
		}
	}, sourceAnalysisGrid);

	this.sourceAnalysisGrid.getStore().on("load", function() {
		toolbar.updateDatasets();
	}, this);

	this.sourceAnalysisGrid.doLayout();

	var refresh = function(e) {
		this.store.reload({
			callback : function(r, options, ok) {
				// focus on the newly loaded one.
				var recind = this.store.find("id", e);
				var rec = this.store.getAt(recind);
				this.getSelectionModel().selectRecords([rec], false);
			}
		});
	};

	virtualAnalysisGrid.getTopToolbar().on("newAnalysisCreated", refresh,
			this.analysisGrid);
	virtualAnalysisGrid.getTopToolbar().on("analysisUpdated", refresh,
			this.analysisGrid);

	this.sourceAnalysisGrid.render();
	virtualAnalysisGrid.render();

	var showSourceAnalysis = function(target, rowIndex, ev) {
		// Load the source analysis, or the selected one, if it is real.
		Ext.DomHelper.overwrite("messages", "");
		// toolbar.reset();

		var row;
		if (target.grid) { // selectionmodel
			row = target.grid.getStore().getAt(rowIndex);
		} else {
			row = target.getStore().getAt(rowIndex);
		}
		var id = row.get("id");

		var virtual = row.get("virtual");
		var ids = row.get("datasets");
		this.taxon = row.get("taxon");
		this.stringency = row.get("stringency");
		this.setTitle(row.get("name"));
		if (virtual) {
			id = row.get("viewedAnalysisId");
			var callback = function(d) {
				// load the data sets.
				this.getStore().load({
					params : [d]
				});
			}
			// Go back to the server to get the ids of the experiments the
			// selected analysis' parent has.
			GeneLinkAnalysisManagerController.getExperimentIdsInAnalysis(id, {
				callback : callback.createDelegate(this, [], true)
			});
			this.analysisId = id;
		} else {
			this.analysisId = id;
			this.getStore().load({
				params : [ids]
			});
		}
	};

	var showVirtualAnalysis = function(target, rowIndex, ev) {
		// Show the selected virtual analysis members in the right-hand grid, or
		// clear if it is not virtual.
		Ext.DomHelper.overwrite("messages", "");
		var row;
		if (target.grid) { // selectionmodel
			row = target.grid.getStore().getAt(rowIndex);
		} else {
			row = target.getStore().getAt(rowIndex);
		}
		var id = row.get("id");

		var virtual = row.get("virtual")
		if (virtual) {
			this.sourceAnalysisID = row.get("viewedAnalysisId");
			this.analysisId = id;
			this.analysisName = row.get("name");
			this.analysisDescription = row.get("description");
			this.virtual = true;
			this.taxon = row.get("taxon");
			this.stringency = row.get("stringency");
			this.getStore().load({
				params : [row.get("datasets")]
			});
			this.setTitle("Virtual analysis : " + row.get("name"));
		} else {
			this.analysisId = null;
			this.analysisName = null;
			this.analysisDescription = null;
			this.setTitle("Virtual analysis (new)");
			this.getStore().removeAll();
		}

	};

	analysisGrid.getSelectionModel().on("rowselect", showSourceAnalysis,
			this.sourceAnalysisGrid);

	analysisGrid.getSelectionModel().on("rowselect", showVirtualAnalysis,
			virtualAnalysisGrid);

});

/*
 * Toolbar for creating/updating the analysis. Attach to the
 * virtualAnalysisGrid.
 */
Ext.Gemma.EditVirtualAnalysisToolBar = function(config) {
	var bar = this;
	this.addEvents('newAnalysisCreated', 'createAnalysisError');

	Ext.Gemma.EditVirtualAnalysisToolBar.superclass.constructor.call(this, {
		autoHeight : true
	});

	var createDialog = new Ext.Window({
		el : 'createAnalysisDialog',
		title : "Save new analysis",
		width : 440,
		height : 400,
		shadow : true,
		minWidth : 200,
		minHeight : 150,
		modal : true,
		layout : 'fit'
	});

	this.createNewAnalysis = function(analysisName, analysisDescription) {

		if (this.ownerCt.analysisId) {
			this.updateAnalysis(analysisName, analysisDescription);
			return;
		}

		var callback = function(newid) {
			Ext.getCmp('newsave').enable();
			this.fireEvent("newAnalysisCreated", this, newid);
		};

		var errorHandler = function(e) {
			this.fireEvent("createAnalysisError", this, e);
			Ext.getCmp('newsave').enable();
			this.ownerCt.loadMask.hide();
			Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/iconWarning.gif'
			});
			Ext.DomHelper.append("messages", {
				tag : 'span',
				html : e
			});

		};

		GeneLinkAnalysisManagerController.create({
			taxonId : this.ownerCt.taxon.id,
			stringency : this.ownerCt.stringency,
			name : analysisName,
			description : analysisDescription,
			viewedAnalysisId : this.ownerCt.sourceAnalysisID,
			datasets : this.ownerCt.getEEIds()
		}, {
			callback : callback.createDelegate(this, [], true),
			errorHandler : errorHandler.createDelegate(this, [], true)
		})
	};

	this.updateAnalysis = function(analysisName, analysisDescription) {

		var callback = function() {
			Ext.getCmp('newsave').enable();
			this.fireEvent("analysisUpdated", this, this.ownerCt.analysisId);
		};

		var errorHandler = function(e) {
			this.fireEvent("updateAnalysisError", this, e);
			Ext.getCmp('newsave').enable();
			this.ownerCt.loadMask.hide();
			Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/iconWarning.gif'
			});
			Ext.DomHelper.append("messages", {
				tag : 'span',
				html : e
			});
		};

		GeneLinkAnalysisManagerController.update({
			name : analysisName,
			id : this.ownerCt.analysisId,
			description : analysisDescription,
			datasets : this.ownerCt.getEEIds()
		}, {
			callback : callback.createDelegate(this, [], true),
			errorHandler : errorHandler.createDelegate(this, [], true)
		})
	};

	this.reset = function() {
		if (this.ownerCt.analysisId) {
			var callback = function(d) {
				// load the data sets.
				this.getStore().load({
					params : [d]
				});
			}
			// Go back to the server to get the ids of the experiments the
			// selected analysis has.
			var id = this.ownerCt.analysisId;
			GeneLinkAnalysisManagerController.getExperimentIdsInAnalysis(id, {
				callback : callback.createDelegate(this.ownerCt, [], true)
			});

		}
	};

	this.create = function() {
		// dialog to get new name and description

		Ext.DomHelper.overwrite("messages", "");
		Ext.getCmp('newsave').disable();

		if (!createDialog.rendered) {
			createDialog.render();

			var nameField = new Ext.form.TextField({
				fieldLabel : 'Name',
				id : 'analysis-name',
				minLength : 3,
				width : 100
			});

			var descriptionField = new Ext.form.TextArea({
				fieldLabel : 'Description',
				id : 'analysis-description',
				minLength : 3,
				width : 300
			});

			var analysisForm = new Ext.FormPanel({
				labelAlign : 'top'
			});

			if (this.ownerCt.analysisName) {
				nameField.setValue(this.ownerCt.analysisName);
			}

			if (this.ownerCt.analysisDescription) {
				descriptionField.setValue(this.ownerCt.analysisDescription);
			}

			analysisForm.add(nameField);
			analysisForm.add(descriptionField);

			analysisForm.addButton('Save/Update', function() {
				createDialog.hide();
				this.createNewAnalysis(nameField.getValue(), descriptionField
						.getValue());
			}, this);
			analysisForm.addButton('Cancel', function() {
				createDialog.hide();
			}, createDialog);
			analysisForm.render(createDialog.body);
		}
		createDialog.show();

	};

	this.clear = function() {
		Ext.DomHelper.overwrite("messages", "");
		this.ownerCt.store.removeAll();
		Ext.getCmp('newclear').disable();
	};

		// cannot do here.
		// ownerCt.store.on("add", function() {
		// Ext.DomHelper.overwrite("messages", "");
		//
		// if (grid.analysisId) {
		// Ext.getCmp('newreset').enable();
		// } else {
		// Ext.getCmp('newclear').enable();
		// }
		// Ext.getCmp('newsave').enable();
		//
		// });
		//
		// ownerCt.store.on("remove", function() {
		// Ext.getCmp('newsave').enable();
		// if (grid.analysisId) {
		// Ext.getCmp('newreset').enable();
		// }
		// });

};

Ext.Gemma.SourceAnalysisToolBar = function(config) {

	this.filtering = true;

	Ext.Gemma.SourceAnalysisToolBar.superclass.constructor.call(this, config);

};

/*
 * Constructor
 */
Ext.Gemma.GeneLinkAnalysisGrid = function(config) {
	Ext.apply(this, config);

	if (this.pageSize) {
		this.ds = new Ext.Gemma.PagingDataStore({
			proxy : new Ext.data.DWRProxy(this.readMethod),
			reader : new Ext.data.ListRangeReader({
				id : "id"
			}, this.record),
			pageSize : this.pageSize
		});
		this.bbar = new Ext.Gemma.PagingToolbar({
			pageSize : this.pageSize,
			store : this.ds
		});
	} else {
		this.ds = new Ext.data.Store({
			proxy : new Ext.data.DWRProxy(this.readMethod),
			reader : new Ext.data.ListRangeReader({
				id : "id"
			}, this.record)
		});
	}

	Ext.Gemma.GeneLinkAnalysisGrid.superclass.constructor.call(this, config);

	// this.autoExpandColumn = 'name';

	this.addEvents({
		'loadAnalysis' : true
	});

	this.getStore().on("load", function() {
		this.autoSizeColumns();
		this.doLayout();
	}, this);

	if (!this.noInitialLoad) {
		this.getStore().load({});
	}
};

/*
 * Displays the available analyses.
 */
Ext.extend(Ext.Gemma.GeneLinkAnalysisGrid, Ext.grid.GridPanel, {

	autoExpandColumn : 'description',
	selModel : new Ext.grid.RowSelectionModel(),
	stripeRows : true,
	viewConfig : {
		forceFit : true
	},
	autoExpandMax : 400,

	record : Ext.data.Record.create([{
		name : "id",
		type : "int"
	}, {
		name : "name",
		type : "string"
	}, {
		name : "description",
		type : "string"
	}, {
		name : "numDatasets",
		type : "int"
	}, {
		name : "taxon"
	}, {
		name : "virtual",
		type : "bool"
	}, {
		name : "stringency",
		type : "int"
	}, {
		name : "viewedAnalysisId",
		type : "int"
	}, {
		name : "datasets"
	}]),

	columns : [{
		id : 'name',
		header : "Name",
		dataIndex : "name"
	}, {
		id : 'description',
		header : "Description",
		dataIndex : "description",
		sortable : true,
		width : 100
	}, {
		id : 'datasets',
		header : "Num datasets",
		dataIndex : "numDatasets",
		sortable : true
	}, {
		id : 'taxon',
		header : "Taxon",
		dataIndex : "taxon",
		renderer : function(r) {
			return r.commonName;
		},
		sortable : true,
		width : 100
	}, {
		id : 'stringency',
		header : "Stringency",
		dataIndex : "stringency",
		sortable : true,
		width : 100
	}, {
		id : 'virtual',
		header : "Virtual",
		dataIndex : "virtual",
		sortable : true,
		width : 100
	}]

});

Ext.extend(Ext.Gemma.EditVirtualAnalysisToolBar, Ext.Toolbar, {
	afterRender : function() {
		Ext.Gemma.EditVirtualAnalysisToolBar.superclass.afterRender.call(this);

		// add the buttons.
		var createBut = new Ext.Button({
			id : 'newsave',
			text : "Save",
			handler : this.create,
			scope : this,
			disabled : false,
			tooltip : "Save the analysis"
		});

		var clearBut = new Ext.Button({
			id : 'newclear',
			text : "Clear",
			handler : this.clear,
			scope : this,
			disabled : true,
			tooltip : "Clear the table"
		});

		var resetBut = new Ext.Button({
			id : 'newreset',
			text : "Reset",
			handler : this.reset,
			scope : this,
			disabled : true,
			tooltip : "Reset to stored version"
		});

		if (!this.admin) {
			this.createBut.disable();
		}
		this.addButton(createBut);
		this.addButton(clearBut);
		this.addButton(resetBut);

	}
});

Ext.extend(Ext.Gemma.SourceAnalysisToolBar, Ext.Gemma.DatasetSearchToolBar, {

	afterRender : function() {
		Ext.Gemma.SourceAnalysisToolBar.superclass.afterRender.call(this);
		if (this.targetGrid && this.targetGrid.editable) {
			var grabber = new Ext.Button({
				id : 'grab',
				disabled : true,
				text : "Grab >>",
				handler : function(button, ev) {

					var id = this.owningGrid.analysisId;

					// Can't mix two analyses.
					if (id != this.targetGrid.sourceAnalysisID) {
						this.targetGrid.getStore().removeAll();
					}
					this.targetGrid.getStore().add(this.grid
							.getSelectionModel().getSelections());
					this.targetGrid.getView().refresh();
					this.targetGrid.sourceAnalysisID = this.container.analysisId;
					this.targetGrid.stringency = this.container.stringency;
					this.targetGrid.taxon = this.container.taxon;
				},
				scope : this
			});
			this.add(grabber);
			// grid.store.on("load", function() {
			// Ext.getCmp('grab').enable();
			// }, this);
		}
	}

});
