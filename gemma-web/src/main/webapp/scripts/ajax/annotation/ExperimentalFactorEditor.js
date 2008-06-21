Ext.namespace('Gemma');

Gemma.ExperimentalFactorGrid = Ext.extend(Gemma.GemmaGridPanel, {

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
		name : "category",
		type : "string"
	}, {
		name : "categoryUri",
		type : "string"
	}]),

	categoryStyler : function(value, metadata, record, row, col, ds) {
		return Gemma.GemmaGridPanel.formatTermWithStyle(value,
				record.data.categoryUri);
	},

	columns : [{
		header : "Name",
		dataIndex : "name",
		sortable : true
	}, {
		header : "Category",
		dataIndex : "category",
		renderer : this.categoryStyler,
		sortable : true

	}, {
		header : "Description",
		dataIndex : "description",
		sortable : true
	}],

	initComponent : function() {

		this.experimentalDesign = {
			id : config.edId,
			classDelegatingFor : "ExperimentalDesign"
		};

		this.nameField = new Ext.form.TextField({});
		var nameEditor = new Ext.grid.GridEditor(this.nameField);

		this.categoryCombo = new Gemma.MGEDCombo({
			lazyRender : true,
			termKey : "factor"
		});
		var categoryEditor = new Ext.grid.GridEditor(this.categoryCombo);
		this.categoryCombo.on("select", function(combo, record, index) {
			categoryEditor.completeEdit();
		});

		this.descriptionField = new Ext.form.TextField({});
		var descriptionEditor = new Ext.grid.GridEditor(this.descriptionField);

		this.store = new Ext.data.Store({
			proxy : new Ext.data.DWRProxy(ExperimentalDesignController.getExperimentalFactors),
			reader : new Ext.data.ListRangeReader({
				id : "id"
			}, Gemma.ExperimentalFactorGrid.getRecord())
		});
		this.store.load({
			params : [this.experimentalDesign]
		});

		Gemma.ExperimentalFactorGrid.superclass.initComponent.call(this);

		this.addEvents('experimentalfactorchange');
	},

	onRender : function(c, l) {

		Gemma.ExperimentalFactorGrid.superclass.onRender.call(this, c, l);

		var NAME_COLUMN = 0;
		var CATEGORY_COLUMN = 1;
		var DESCRIPTION_COLUMN = 2;

		this.autoExpandColumn = DESCRIPTION_COLUMN;

		if (this.editable) {
			this.getColumnModel().setEditor(NAME_COLUMN, nameEditor);
			this.getColumnModel().setEditor(CATEGORY_COLUMN, categoryEditor);
			this.getColumnModel().setEditor(DESCRIPTION_COLUMN,
					descriptionEditor);
		}

		if (this.editable) {
			this.on("afteredit", function(e) {
				var col = this.getColumnModel().getColumnId(e.column);
				if (col == CATEGORY_COLUMN) {
					var f = this.categoryCombo.getTerm.bind(this.categoryCombo);
					var term = f();
					e.record.set("category", term.term);
					e.record.set("categoryUri", term.uri);
				}
			});

			var tbar = new Gemma.ExperimentalFactorToolbar({
				grid : this,
				renderTo : this.tbar
			});
		}

	},

	factorCreated : function(factor) {
		this.refresh();
		var efs = [factor];
		this.fireEvent('experimentalfactorchange', this, efs);
	},

	recordsChanged : function(records) {
		this.refresh();
		var efs = [];
		for (var i = 0; i < records.length; ++i) {
			efs.push(records[i].data);
		}
		this.fireEvent('experimentalfactorchange', this, efs);
	},

	idsDeleted : function(ids) {
		this.refresh();
		var efs = [];
		for (var i = 0; i < ids.length; ++i) {
			efs.push(this.store.getById(ids[i]).data);
		}
		this.fireEvent('experimentalfactorchange', this, efs);
	}

});

/*
 * Gemma.ExperimentalFactorToolbar constructor... config is a hash with the
 * following options: grid is the grid that contains the factors.
 */
Gemma.ExperimentalFactorToolbar = function(config) {

	this.grid = config.grid;
	delete config.grid;
	this.experimentalDesign = this.grid.experimentalDesign;

	/*
	 * keep a reference to ourselves so we don't have to worry about scope in
	 * the button handlers below...
	 */
	var thisToolbar = this;

	/*
	 * establish default config options...
	 */
	var superConfig = {};

	/*
	 * add our items in front of anything specified in the config above...
	 */
	this.categoryCombo = new Gemma.MGEDCombo({
		emptyText : "Select a category",
		termKey : "factor"
	});
	this.categoryCombo.on("select", function() {
		createButton.enable();
	});
	this.descriptionField = new Ext.form.TextField({
		emptyText : "Type a description"
	});
	var createButton = new Ext.Toolbar.Button({
		text : "create",
		tooltip : "Create the new experimental factor",
		disabled : true,
		handler : function() {
			var created = thisToolbar.getExperimentalFactorValueObject();
			createButton.disable();
			thisToolbar.categoryCombo.reset();
			thisToolbar.descriptionField.reset();
			var callback = function() {
				thisToolbar.grid.factorCreated.call(thisToolbar.grid, created);
			};
			ExperimentalDesignController.createExperimentalFactor(
					thisToolbar.experimentalDesign, created, callback);
		}
	});
	var deleteButton = new Ext.Toolbar.Button({
		text : "delete",
		tooltip : "Delete selected experimental factors",
		disabled : true,
		handler : function() {
			var oldmsg = this.loadMask.msg;
			this.loadMask.msg = "Deleting experimental factor(s)";
			this.loadMask.show();
			deleteButton.disable();
			var selected = thisToolbar.grid.getSelectedIds();
			var callback = function() {
				thisToolbar.grid.idsDeleted.call(thisToolbar.grid, selected);
				thisToolbar.grid.loadMask.hide();
				thisToolbar.grid.loadMask.msg = oldmsg;
			};
			var errorHandler = function() {
				thisToolbar.grid.loadMask.hide();
				thisToolbar.grid.loadMask.msg = oldmsg;
			};
			ExperimentalDesignController.deleteExperimentalFactors(
					thisToolbar.experimentalDesign, selected, {
						callback : callback,
						errorHandler : errorHandler
					});
		},
		scope : this.grid
	});
	this.grid.getSelectionModel().on("selectionchange", function(model) {
		var selected = model.getSelections();
		if (selected.length > 0) {
			deleteButton.enable();
		} else {
			deleteButton.disable();
		}
	});
	var saveButton = new Ext.Toolbar.Button({
		text : "save",
		tooltip : "Save changed experimental factors",
		disabled : true,
		handler : function() {
			saveButton.disable();
			var edited = thisToolbar.grid.getEditedRecords();
			var callback = function() {
				thisToolbar.grid.recordsChanged.call(thisToolbar.grid, edited);
			};
			ExperimentalDesignController.updateExperimentalFactors(edited,
					callback);
		}
	});
	this.grid.on("afteredit", function(model) {
		saveButton.enable();
		revertButton.enable();
	});
	var revertButton = new Ext.Toolbar.Button({
		text : "revert",
		tooltip : "Undo changes to selected experimental factors",
		disabled : true,
		handler : function() {
			thisToolbar.grid.revertSelected();
		}
	});
	this.grid.getSelectionModel().on("selectionchange", function(model) {
		var selected = model.getSelections();
		revertButton.disable();
		for (var i = 0; i < selected.length; ++i) {
			if (selected[i].dirty) {
				revertButton.enable();
				break;
			}
		}
	});

	var items = [new Ext.Toolbar.TextItem("Add an Experimental Factor:"),
			new Ext.Toolbar.Spacer(), this.categoryCombo,
			new Ext.Toolbar.Spacer(), this.descriptionField,
			new Ext.Toolbar.Spacer(), createButton,
			new Ext.Toolbar.Separator(), deleteButton,
			new Ext.Toolbar.Separator(), saveButton,
			new Ext.Toolbar.Separator(), revertButton];
	config.items = config.items ? items.concat(config.items) : items;

	for (property in config) {
		superConfig[property] = config[property];
	}
	Gemma.ExperimentalFactorToolbar.superclass.constructor.call(this,
			superConfig);
};

/*
 * instance methods...
 */
Ext.extend(Gemma.ExperimentalFactorToolbar, Ext.Toolbar, {

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

});