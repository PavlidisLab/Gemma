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
				record.get("categoryUri"));
	},

	initComponent : function() {

		this.experimentalDesign = {
			id : this.edId,
			classDelegatingFor : "ExperimentalDesign"
		};

		Ext.apply(this, {	columns : [{
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
	}]});
		
		this.store = new Ext.data.Store({
			proxy : new Ext.data.DWRProxy(ExperimentalDesignController.getExperimentalFactors),
			reader : new Ext.data.ListRangeReader({
				id : "id"
			}, this.record)
		});

		if (this.editable) {
			this.tbar = new Gemma.ExperimentalFactorToolbar({});
		}

		Gemma.ExperimentalFactorGrid.superclass.initComponent.call(this);

		this.addEvents('experimentalfactorchange');

		this.store.load({
			params : [this.experimentalDesign]
		});

	},

	onRender : function(c, l) {

		Gemma.ExperimentalFactorGrid.superclass.onRender.call(this, c, l);

		var NAME_COLUMN = 0;
		var CATEGORY_COLUMN = 1;
		var DESCRIPTION_COLUMN = 2;

		this.autoExpandColumn = DESCRIPTION_COLUMN;

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

		if (this.editable) {
			this.getColumnModel().setEditor(NAME_COLUMN, nameEditor);
			this.getColumnModel().setEditor(CATEGORY_COLUMN, categoryEditor);
			this.getColumnModel().setEditor(DESCRIPTION_COLUMN,
					descriptionEditor);

			this.getTopToolbar().on("create", function(newFactorValue) {
				var callback = function() {
					this.factorCreated(newFactorValue);
				}.createDelegate(this);
				ExperimentalDesignController.createExperimentalFactor(
						this.experimentalDesign, newFactorValue, callback);
			}.createDelegate(this));

			this.getTopToolbar().on("delete", function() {
				var selected = this.getSelectedIds();
				var oldmsg = this.loadMask.msg;
				this.loadMask.msg = "Deleting experimental factor(s)";
				this.loadMask.show();
				var callback = function() {
					this.idsDeleted(selected);
					this.loadMask.hide();
					this.loadMask.msg = oldmsg;
				}.createDelegate(this);
				var errorHandler = function() {
					this.loadMask.hide();
					this.loadMask.msg = oldmsg;
				};
				ExperimentalDesignController.deleteExperimentalFactors(
						this.experimentalDesign, selected, {
							callback : callback,
							errorHandler : errorHandler
						});
			});
			
			this.getTopToolbar().on("save", function() {
				var edited = this.getEditedRecords();
				var callback = function() {
					this.recordsChanged.(edited);
				}.createDelegate(this);
				ExperimentalDesignController.updateExperimentalFactors(edited,
						callback);
			});
			
			this.getTopToolbar().on("undo", function() {this.revertSelected();});

			this.on("afteredit", function(e) {
				var col = this.getColumnModel().getColumnId(e.column);
				if (col == CATEGORY_COLUMN) {
					var f = this.categoryCombo.getTerm.bind(this.categoryCombo);
					var term = f();
					e.record.set("category", term.term);
					e.record.set("categoryUri", term.uri);
				}
			});
			
			
		this.on("afteredit", function(model) {
			this.saveButton.enable();
			this.revertButton.enable();
		},   this.getTopToolbar());
		
		this.getSelectionModel().on("selectionchange", function(model) {
			var selected = model.getSelections();
			if (selected.length > 0) {
				this.deleteButton.enable();
			} else {
				this.deleteButton.disable();
			}
			this.revertButton.disable();
			for (var i = 0; i < selected.length; ++i) {
				if (selected[i].dirty) {
					this.revertButton.enable();
					break;
				}
			}
		}, this.getTopToolbar());
		} // if editable.
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
 * instance methods...
 */
Gemma.ExperimentalFactorToolbar =Ext.extend( Ext.Toolbar, {

	onRender : function(c, l) {
		Gemma.ExperimentalFactorToolbar.superclass.onRender.call(this, c, l);

		/*
		 * add our items in front of anything specified in the config above...
		 */
		this.categoryCombo = new Gemma.MGEDCombo({
			emptyText : "Select a category",
			termKey : "factor"
		});
		this.categoryCombo.on("select", function() {
			this.createButton.enable();
		}, this);
		this.descriptionField = new Ext.form.TextField({
			emptyText : "Type a description"
		});
		this.createButton = new Ext.Toolbar.Button({
			text : "create",
			tooltip : "Create the new experimental factor",
			disabled : true,
			handler : function() {
				this.fireEvent("create", this
						.getExperimentalFactorValueObject());

				this.createButton.disable();
				this.categoryCombo.reset();
				this.descriptionField.reset();
			},
			scope : this
		});
		this. deleteButton = new Ext.Toolbar.Button({
			text : "delete",
			tooltip : "Delete selected experimental factors",
			disabled : true,
			handler : function() {
				this.deleteButton.disable();
				this.fireEvent("delete");
			},
			scope : this
		});
		
		this. revertButton = new Ext.Toolbar.Button({
			text : "revert",
			tooltip : "Undo changes to selected experimental factors",
			disabled : true,
			handler : function() {
				this.fireEvent("undo");
			},
			scope : this
		});
		
		this.saveButton = new Ext.Toolbar.Button({
			text : "save",
			tooltip : "Save changed experimental factors",
			disabled : true,
			handler : function() {
				this.saveButton.disable();
				this.fireEvent("save");
			}
		});
	

		this.addText("Add an Experimental Factor:");
		this.addSpacer();
		this.add(this.categoryCombo);
		this.addSpacer();
		this.add(this.descriptionField);
		this.addSpacer();
		this.addButton(this.createButton);
		this.addSeparator();
		this.addButton(this.deleteButton);
		this.addSeparator();
		this.addButton(this.saveButton);
		this.addSeparator();
		this.addButton(this.revertButton);

	},
	
	initComponent : function() {
		Gemma.ExperimentalFactorToolbar.superclass.initComponent.call(this);
		this.addEvents("save", "undo", "create", "delete");
	},

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