/*
 * Panel for choosing datasets based on search criteria, or on established
 * "ExpressionExperimentSets".
 * 
 * At the top, the available ExpressionExperimentSets are shown. At the bottom
 * left, searching for experiments; at the bottom right, the experiments that
 * are in the set. If the user is an admin, they can save new
 * ExpressionExperimentSets to the database; otherwise they are saved for the
 * user in a cookie.
 * 
 * @author Paul
 * 
 * @version $Id$
 */
Ext.namespace('Ext.Gemma');

/**
 * 
 * @class Ext.Gemma.DatasetChooserPanel
 * @extends Ext.Window
 */
Ext.Gemma.DatasetChooserPanel = Ext.extend(Ext.Window, {
	id : 'dataset-chooser',
	layout : 'border',
	width : 800,
	height : 500,
	constrainHeader : true,

	onCommit : function() {
		this.hide();
		this.fireEvent("datasets-selected", {
			eeIds : this.currentEeSetGrid.getEEIds()
		});
	},

	initComponent : function() {

		Ext.apply(this, {

			buttons : [{
				id : 'done-selecting-button',
				text : "Done",
				handler : this.onCommit.createDelegate(this, [], true),
				scope : this
			}]
		})

		this.addEvents({
			"datasets-selected" : true
		});

		Ext.Gemma.DatasetChooserPanel.superclass.initComponent.call(this);
	},

	onRender : function(ct, position) {
		Ext.Gemma.DatasetChooserPanel.superclass.onRender.call(this, ct,
				position);

		var admin = dwr.util.getValue("hasAdmin");

		this.eeSetGrid = new Ext.Gemma.ExpressionExperimentSetGrid({
			readMethod : ExpressionExperimentSetController.getAvailableExpressionExperimentSets
					.createDelegate(this),
			editable : admin,
			region : 'north',
			layout : 'fit',
			split : true,
			collapsible : true,
			collapseMode : 'mini',
			loadMask : {
				msg : 'loading'
			},
			height : 200,
			// pageSize : 6,
			title : "Available expression experiment sets"
		});

		this.currentEeSetGrid = new Ext.Gemma.ExpressionExperimentGrid({
			editable : admin,
			region : 'center',
			title : "Select a expression experiment set or create a new one",
			pageSize : 20,
			loadMask : {
				msg : 'Loading datasets ...'
			},
			split : true,
			height : 200,
			width : 400,
			rowExpander : true,
			tbar : new Ext.Gemma.EditExpressionExperimentSetToolbar({
				admin : admin
			})

		});

		this.sourceDatasetsGrid = new Ext.Gemma.ExpressionExperimentGrid({
			editable : false,
			admin : admin,
			title : "Dataset locator",
			region : 'west',
			split : true,
			pageSize : 20,
			height : 200,
			loadMask : {
				msg : 'Searching ...'
			},
			width : 400,
			rowExpander : true,
			tbar : new Ext.Gemma.DataSetSearchAndGrabToolbar({
				taxonSearch : true,
				targetGrid : this.currentEeSetGrid
			})
		});

		var eeSetManager = new Ext.Gemma.EESetManager({
			admin : admin,
			grid : this.currentEeSetGrid,
			store : this.currentEeSetGrid.getStore()
		});

		this.currentEeSetGrid.getTopToolbar().setEESetManager(eeSetManager);
		this.sourceDatasetsGrid.getTopToolbar().grid = this.sourceDatasetsGrid;
		this.currentEeSetGrid.getTopToolbar().grid = this.currentEeSetGrid;

		this.currentEeSetGrid.getTopToolbar().on("clear", function() {
			this.setTitle("Select experiments to add");
		}, this.currentEeSetGrid);

		this.sourceDatasetsGrid.getStore().on("load", function() {
			this.sourceDatasetsGrid.getTopToolbar().updateDatasets();
		}, this);

		this.sourceDatasetsGrid.getTopToolbar().on("after.tbsearch",
				function(results) {
					this.sourceDatasetsGrid.getStore().removeAll();
					this.sourceDatasetsGrid.getStore().load({
						params : [results]
					});
					this.sourceDatasetsGrid.setTitle("Found " + results.length);
				}, this);

		var showCurrentExpressionExperimentSet = function(target, rowIndex, ev) {
			// Show the selected eeset members in the right-hand grid (or empty)
			var row;
			if (target.grid) { // selectionmodel
				row = target.grid.getStore().getAt(rowIndex);
			} else {
				row = target.getStore().getAt(rowIndex);
			}
			this.id = row.get("id");
			this.expressionExperimentSetId = this.id;
			this.name = row.get("name");
			this.description = row.get("description");
			this.editable = row.get("editable");
			this.getStore().removeAll();
			this.getStore().load({
				params : [row.get("expressionExperimentIds")]
			});
			this.setTitle("Datasets in set: " + row.get("name"));
			eeSetManager.currentId = this.id;
			eeSetManager.currentName = this.name;
			eeSetManager.currentDescription = this.description;
			eeSetManager.currentIsEditable = this.editable;

			if (target.setEditable) {
				target.setEditable(this.editable);
			}

			eeSetManager.fireEvent("setChanged");
		};

		this.add(this.eeSetGrid);
		this.add(this.sourceDatasetsGrid);
		this.add(this.currentEeSetGrid);

		this.eeSetGrid.doLayout();
		this.sourceDatasetsGrid.doLayout();
		this.currentEeSetGrid.doLayout();

		eeSetManager.on("create", function() {
			this.eeSetGrid.getStore().reload();
		}); // update the top panel.

		eeSetManager.on("update", function() {
			this.eeSetGrid.getStore().reload();
		}); // update the top panel

		this.eeSetGrid.getSelectionModel().on("rowselect",
				showCurrentExpressionExperimentSet, this.currentEeSetGrid);
	}

});

/**
 * 
 * @param {}
 *            config
 */
Ext.Gemma.ExpressionExperimentSetGrid = function(config) {

	Ext.apply(this, config);

	this.store = new Ext.data.Store({
		proxy : new Ext.data.DWRProxy(this.readMethod),
		reader : new Ext.data.ListRangeReader({
			id : "id"
		}, this.record)
	});

	Ext.Gemma.ExpressionExperimentSetGrid.superclass.constructor.call(this,
			config);

	this.addEvents({
		'loadExpressionExperimentSet' : true
	});

	this.getStore().on("load", function() {
		this.doLayout();
	}, this);

	if (!this.noInitialLoad) {
		this.getStore().load({
				// params : this.getReadParams()
				});
	}

};

/**
 * 
 * @class Ext.Gemma.ExpressionExperimentSetGrid
 * @extends Ext.grid.GridPanel
 */
Ext.extend(Ext.Gemma.ExpressionExperimentSetGrid, Ext.grid.GridPanel, {

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
		name : "name"
	}, {
		name : "description"
	}, {
		name : "numExperiments",
		type : "int"
	}, {
		name : "modifiable",
		type : "bool"
	}, {
		name : "expressionExperimentIds"
	}]),

	columns : [{
		id : 'name',
		header : "Name",
		dataIndex : "name",
		sortable : true
	}, {
		id : 'description',
		header : "Description",
		dataIndex : "description",
		sortable : true
	}, {
		id : 'datasets',
		header : "Num datasets",
		dataIndex : "numExperiments",
		sortable : true
	}, {
		id : 'modifiable',
		header : "Editable",
		dataIndex : "modifiable",
		sortable : true
	}]

});

/**
 * Toolbar for creating/updating expressionExperimentSet. Attach to the
 * virtualAnalysisGrid. Either save to the database or to a cookie.
 */
Ext.Gemma.EditExpressionExperimentSetToolbar = Ext.extend(Ext.Toolbar, {

	setEESetManager : function(eesetManager) {
		this.eesetManager = eesetManager;
	},

	getEESetManager : function() {
		return this.eesetManager;
	},

	onRemove : function() {
	},

	onAdd : function() {
		this.newBut.enable();
		this.resetBut.enable();
		this.saveBut.enable();
	},

	onReset : function() {

	},

	afterRender : function() {
		Ext.Gemma.EditExpressionExperimentSetToolbar.superclass.afterRender
				.call(this);

		this.newBut = new Ext.Button({
			id : 'newnew',
			text : "New",
			handler : function() {
				this.newBut.enable();
				this.saveBut.enable();
				this.resetBut.disable();
				this.getEESetManager().clear();
				this.fireEvent("clear");
			},
			scope : this,
			disabled : false,
			tooltip : "Start a new set"
		});

		this.saveBut = new Ext.Button({
			id : 'newsave',
			text : "Save",
			handler : function() {
				this.newBut.enable();
				this.saveBut.disable();
				this.resetBut.disable();
				this.getEESetManager().saveOrUpdate();
			},
			scope : this,
			disabled : false,
			tooltip : "Save the set"
		});

		this.resetBut = new Ext.Button({
			id : 'newreset',
			text : "Reset",
			handler : function() {
				this.newBut.enable();
				this.saveBut.disable();
				this.resetBut.disable();
				this.getEESetManager().reset();
			},
			scope : this,
			disabled : true,
			tooltip : "Reset to stored version"
		});

//		this.deleteBut = new Ext.Button({
//			id : 'newreset',
//			text : "Delete selected",
//			handler : this.removeSelected.createDelegate(this),
//			scope : this,
//			disabled : false,
//			tooltip : "Reset to stored version"
//		});

		this.addButton(this.newBut);
		this.addButton(this.saveBut);
		this.addButton(this.resetBut);
	//	this.addButton(this.deleteBut);

		if (!this.admin) {
			this.saveBut.disable();
		}

	},
	constructor : function(config) {

		Ext.Gemma.EditExpressionExperimentSetToolbar.superclass.constructor
				.call(this, config);

		this.addEvents('newExpressionExperimentSetCreated',
				'createExpressionExperimentError', 'clear');

	}
});

/**
 * 
 * @class Ext.Gemma.EESetManager
 * @extends Ext.util.Observable
 */
Ext.Gemma.EESetManager = Ext.extend(Ext.util.Observable, {

	currentId : null,
	currentName : "",
	currentDescription : "",
	currentIsEditable : true,

	clear : function() {
		this.store.removeAll();
		this.currentIsEditable = true;
		this.currentName = "";
		this.currentDescription = "";
		this.currentId = null;
	},

	reset : function() {
		var callback = function(d) {
			// load the data sets.
			this.getStore().load({
				params : [d]
			});
		}
		// Go back to the server to get the ids of the experiments the
		// selected analysis has.

		if (this.currentSetId == null) {
			return;
		}

		ExpressionExperimentSetController.getExperimentIdsInAnalysis(
				this.currentSetId, {
					callback : callback.createDelegate(this.grid, [], true)
				});

	},

	getDetails : function() {
		if (!this.currentIsEditable) {
			Ext.Msg.alert("Sorry", "This set is read-only");
			return;
		}

		if (!this.detailsWin) {
			this.detailsWin = new Ext.Gemma.DetailsWindow();
			this.detailsWin.on("commit", function(args) {
				this.currentName = args.name;
				this.currentDescription = args.description;
				if (this.admin) {
					if (this.currentId == null) {
						this.create();
					} else {
						this.update();
					}
				}
			}, this);
		}

		this.detailsWin.name = this.currentName;
		this.detailsWin.description = this.currentDescription;
		this.detailsWin.show();
	},

	create : function() {
		if (!this.currentIsEditable || this.currentId != null) {
			return;
		}

		if (this.grid.getEEIds().length == 0) {
			Ext.Msg.alert("Sorry", "You must add experiments first");
		}

		if (!this.currentName) {
			Ext.Msg.alert("Sorry", "You must provide a name for the set");
			return;
		}

		var callback = function(newid) {
			this.fireEvent("save", this, newid);
			Ext.Msg.alert("Set saved", "Saved!");
		};

		var errorHandler = function(e) {
			this.fireEvent("eeSetCreateError", this, e);
			this.grid.loadMask.hide();
		};

		ExpressionExperimentSetController.create({
			name : this.currentName,
			description : this.currentDescription,
			expressionExperimentIds : this.grid.getEEIds()
		}, {
			callback : callback.createDelegate(this, [], true),
			errorHandler : errorHandler.createDelegate(this, [], true)
		})
	},

	saveOrUpdate : function() {
		this.getDetails();
	},

	update : function() {
		if (!this.currentIsEditable) {
			Ext.Msg.alert("Sorry",
					"Cannot edit the current set -- it is read-only");
			return;
		}

		var callback = function() {
			this.createbut.enable();
			this.fireEvent("save", this);
			Ext.Msg.alert("Success", "Saved!");
		}

		var errorHandler = function(e) {
			this.fireEvent("eeSetUpdateError", this, e);
		};

		ExpressionExperimentSetController.update({
			name : this.currentName,
			id : this.currentId,
			description : this.currentDescription,
			expressionExperimentIds : this.grid.getEEIds()
		}, {
			callback : callback.createDelegate(this, [], true),
			errorHandler : errorHandler.createDelegate(this, [], true)
		});
	},

	constructor : function(config) {
		Ext.apply(this, config);
		this.addEvents({
			"clear" : true,
			"reset" : true,
			"newset" : true,
			"save" : true,
			"setchanged" : true,
			"eeSetUpdateError" : true,
			"eeSetSaveerror" : true
		})
	}

});

Ext.Gemma.DetailsWindow = Ext.extend(Ext.Window, {
	owner : this,
	width : 500,
	height : 300,
	closeAction : 'hide',
	id : 'eeset-dialog',
	title : "Provide or edit expression experiment set details",
	shadow : true,
	modal : true,

	onCommit : function() {

		if (!this.nameField.validate() || this.nameField.getValue() == null) {
			Ext.Msg.alert("Sorry", "You must provide a name for the set");
			return;
		}

		this.hide();
		var values = Ext.getCmp('eeset-form').getForm().getValues();
		return this.fireEvent("commit", {
			name : values.eesetname,
			description : values.eesetdescription
		});
	},

	initComponent : function() {

		this.commitBut = new Ext.Button({
			text : "Save",
			handler : this.onCommit,
			scope : this,
			tooltip : "Save"
		});

		this.nameField = new Ext.form.TextField({
			fieldLabel : 'Name',
			value : this.name,
			id : 'eesetname',
			minLength : 3,
			invalidText : "You must provide a name",
			width : 300
		});

		Ext.apply(this, {

			items : new Ext.FormPanel({
				frame : true,
				labelAlign : 'left',
				id : 'eeset-form',
				height : 250,
				items : new Ext.form.FieldSet({
					height : 200,
					items : [this.nameField, new Ext.form.TextArea({
						fieldLabel : 'Description',
						value : this.description,
						id : 'eesetdescription',
						width : 300
					})]
				}),
				buttons : [{
					text : "Cancel",
					handler : this.hide.createDelegate(this, [])
				}, this.commitBut]

			})
		});

		Ext.Gemma.DetailsWindow.superclass.initComponent.call(this);
	}
});
