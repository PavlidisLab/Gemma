Ext.namespace('Gemma');

/*
 * Gemma.FactorValueGrid constructor... config is a hash with the following
 * options: edId the id of the ExperimentalDesign whose ExperimentalFactors can
 * be displayed in the grid efId the id of the ExperimentalFactor whose
 * FactorValues are displayed in the grid form the name of the HTML form the
 * grid is in
 */
Gemma.FactorValueGrid = function(config) {

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

	this.form = config.form;
	delete config.form;

	this.editable = config.editable;

	this.categoryCombo = new Gemma.MGEDCombo({
		lazyRender : true,
		termKey : "factorvalue"
	});
	var categoryEditor = new Ext.grid.GridEditor(this.categoryCombo);
	this.categoryCombo.on("select", function(combo, record, index) {
		categoryEditor.completeEdit();
	});

	this.valueCombo = new Gemma.CharacteristicCombo({
		lazyRender : true
	});
	var valueEditor = new Ext.grid.GridEditor(this.valueCombo);
	this.valueCombo.on("select", function(combo, record, index) {
		valueEditor.completeEdit();
	});

	/*
	 * establish default config options...
	 */
	var superConfig = {};

	superConfig.ds = new Ext.data.GroupingStore({
		proxy : new Ext.data.DWRProxy(ExperimentalDesignController.getFactorValuesWithCharacteristics),
		reader : new Ext.data.ListRangeReader({
			id : "charId"
		}, Gemma.FactorValueGrid.getRecord()),
		groupField : "factorValueId",
		sortInfo : {
			field : "category",
			direction : "ASC"
		}
	});
	if (this.experimentalFactor.id) {
		superConfig.ds.load({
			params : [this.experimentalFactor]
		});
	}
	var groupTextTpl = this.editable
			? '<input type="checkbox" name="selectedFactorValues" value="{[ values.rs[0].data.factorValueId ]}" /> '
			: '';
	groupTextTpl = groupTextTpl + '{[ values.rs[0].data.factorValueString ]}';
	superConfig.view = new Ext.grid.GroupingView({
		enableGroupingMenu : false,
		enableNoGroups : false,
		groupTextTpl : groupTextTpl,
		hideGroupedColumn : true,
		showGroupName : true,
		startCollapsed : true
	});

	var FACTOR_VALUE_COLUMN = 0;
	var CATEGORY_COLUMN = 1;
	var VALUE_COLUMN = 2;
	superConfig.cm = new Ext.grid.ColumnModel([{
		header : "FactorValue",
		dataIndex : "factorValueId"
	}, {
		header : "Category",
		dataIndex : "category",
		renderer : Gemma.FactorValueGrid.getCategoryStyler()
	}, {
		header : "Value",
		dataIndex : "value",
		renderer : Gemma.FactorValueGrid.getValueStyler()
	}]);
	superConfig.cm.defaultSortable = true;
	superConfig.autoExpandColumn = VALUE_COLUMN;
	if (this.editable) {
		superConfig.cm.setEditor(CATEGORY_COLUMN, categoryEditor);
		superConfig.cm.setEditor(VALUE_COLUMN, valueEditor);
	}

	for (property in config) {
		superConfig[property] = config[property];
	}
	Gemma.FactorValueGrid.superclass.constructor.call(this, superConfig);

	/*
	 * these functions have to happen after we've called the super-constructor
	 * so that we know we're a Grid...
	 */
	this.getStore().on("load", function() {
		this.autoSizeColumns();
		this.doLayout();
	}, this);

	if (this.editable) {
		this.on("afteredit", function(e) {
			var col = this.getColumnModel().getColumnId(e.column);
			if (col == CATEGORY_COLUMN) {
				var term = this.categoryCombo.getTerm.call(this.categoryCombo);
				e.record.set("category", term.term);
				e.record.set("categoryUri", term.uri);
			} else if (col == VALUE_COLUMN) {
				var c = this.valueCombo.getCharacteristic.call(this.valueCombo);
				e.record.set("value", c.value);
				e.record.set("valueUri", c.valueUri);
			}
			this.getView().refresh();
		});
	}

	this.factorValueToolbar = new Gemma.FactorValueToolbar({
		grid : this,
		renderTo : this.tbar
	});
};

/*
 * static methods
 */
Gemma.FactorValueGrid.getRecord = function() {
	if (Gemma.FactorValueGrid.record === undefined) {
		Gemma.FactorValueGrid.record = Ext.data.Record.create([{
			name : "charId",
			type : "int"
		}, {
			name : "factorValueId",
			type : "int"
		}, {
			name : "category",
			type : "string"
		}, {
			name : "categoryUri",
			type : "string"
		}, {
			name : "value",
			type : "string"
		}, {
			name : "measurement",
			type : "bool"
		}, {
			name : "valueUri",
			type : "string"
		}, {
			name : "factorValueString",
			type : "string"
		}]);
	}
	return Gemma.FactorValueGrid.record;
};

Gemma.FactorValueGrid.getCategoryStyler = function() {
	if (Gemma.FactorValueGrid.categoryStyler === undefined) {
		/*
		 * apply a CSS class depending on whether or not the characteristic has
		 * a URI.
		 */
		Gemma.FactorValueGrid.categoryStyler = function(value, metadata,
				record, row, col, ds) {
			return Gemma.GemmaGridPanel.formatTermWithStyle(value,
					record.data.categoryUri);
		};
	}
	return Gemma.FactorValueGrid.categoryStyler;
};

Gemma.FactorValueGrid.getValueStyler = function() {
	if (Gemma.FactorValueGrid.valueStyler === undefined) {
		/*
		 * apply a CSS class depending on whether or not the characteristic has
		 * a URI.
		 */
		Gemma.FactorValueGrid.valueStyler = function(value, metadata, record,
				row, col, ds) {
			return Gemma.GemmaGridPanel.formatTermWithStyle(value,
					record.data.valueUri);
		};
	}
	return Gemma.FactorValueGrid.valueStyler;
};

Gemma.FactorValueGrid.flattenCharacteristics = function(chars) {
	var s = "";
	for (var i = 0; i < chars.length; ++i) {
		var c = chars[i].data;
		var category = c.category.length > 0
				? c.category
				: "&lt;no category&gt;";
		var value = c.value.length > 0 ? c.value : "&lt;no value&gt;";
		s = s + String.format("{0}: {1}", category, value);
		if (i + 1 < chars.length) {
			s = s + ", ";
		}
	}
	return s;
};

/*
 * instance methods...
 */
Ext.extend(Gemma.FactorValueGrid, Gemma.GemmaGridPanel, {

	initComponent : function() {
		Gemma.FactorValueGrid.superclass.initComponent.call(this);

		this.addEvents('factorvaluecreate', 'factorvaluechange',
				'factorvaluedelete');
	},

	// refresh : function( ct, p ) {
	// Gemma.FactorValueGrid.superclass.refresh.call( this, ct, p );
	// if ( this.onRefresh ) {
	// this.onRefresh();
	// }
	// },

	factorValueCreated : function(ef) {
		this.refresh();
		var fvs = [];
		this.fireEvent('factorvaluecreate', this, fvs);
	},

	factorValuesChanged : function(fvs) {
		this.refresh();
		this.fireEvent('factorvaluechange', this, fvs);
	},

	factorValuesDeleted : function(fvs) {
		this.refresh();
		this.fireEvent('factorvaluedelete', this, fvs);
	},

	setExperimentalFactor : function(efId) {
		this.experimentalFactor.id = efId;
		this.refresh([this.experimentalFactor]);
	},

	getSelectedFactorValues : function() {
		var form = document.forms[this.form];
		var checkboxes = form.selectedFactorValues;
		if (!checkboxes.length) {
			checkboxes = [checkboxes];
		}
		var values = [];
		for (var i = 0; i < checkboxes.length; ++i) {
			if (checkboxes[i].checked) {
				values.push(checkboxes[i].value);
			}
		}
		return values;
	},

	reloadExperimentalFactors : function() {
		this.factorValueToolbar.reloadExperimentalFactors();
	}

});

/*
 * Gemma.FactorValueToolbar constructor... config is a hash with the following
 * options: grid is the grid that contains the factor values.
 */
Gemma.FactorValueToolbar = function(config) {

	this.grid = config.grid;
	delete config.grid;
	this.experimentalDesign = this.grid.experimentalDesign;
	this.editable = this.grid.editable;

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
	this.factorCombo = new Gemma.ExperimentalFactorCombo({
		edId : this.experimentalDesign.id
	});
	var factorCombo = this.factorCombo;
	factorCombo.on("select", function(combo, record, index) {
		thisToolbar.grid.setExperimentalFactor(record.id);
		createFactorValueButton.enable();
		if (thisToolbar.characteristicToolbar) {
			thisToolbar.characteristicToolbar.setExperimentalFactor(record.id);
		}
	});

	var createFactorValueButton = new Ext.Toolbar.Button({
		text : "create",
		tooltip : "Create a new factor value",
		disabled : true,
		handler : function() {
			var ef = thisToolbar.grid.experimentalFactor;
			var callback = function() {
				thisToolbar.grid.factorValueCreated.call(thisToolbar.grid, ef);
				thisToolbar.characteristicToolbar.setExperimentalFactor(ef.id);
			};
			ExperimentalDesignController.createFactorValue(
					thisToolbar.grid.experimentalFactor, callback);
		}
	});

	var deleteFactorValueButton = new Ext.Toolbar.Button({
		text : "delete",
		tooltip : "Delete selected factor values",
		disabled : false,
		handler : function() {
			var ef = thisToolbar.grid.experimentalFactor;
			var selected = thisToolbar.grid.getSelectedFactorValues();
			var callback = function() {
				thisToolbar.grid.factorValuesDeleted.call(thisToolbar.grid,
						selected);
			};
			ExperimentalDesignController.deleteFactorValues(ef, selected,
					callback);
		}
	});

	var items = [new Ext.Toolbar.TextItem("Show Factor Values for:"),
			new Ext.Toolbar.Spacer(), factorCombo];
	if (this.editable) {
		items.push(new Ext.Toolbar.Spacer(), createFactorValueButton,
				new Ext.Toolbar.Separator(), deleteFactorValueButton);
	}

	if (this.grid.getView().toggleAllGroups) {
		var refreshButton = new Ext.Toolbar.Button({
			text : "Expand/collapse all",
			tooltip : "Show/hide all factor value details",
			handler : function() {
				this.grid.getView().toggleAllGroups();
			},
			scope : this
		});

		items.push(new Ext.Toolbar.Fill(), refreshButton);
	}
	config.items = config.items ? items.concat(config.items) : items;

	for (property in config) {
		superConfig[property] = config[property];
	}
	Gemma.FactorValueToolbar.superclass.constructor.call(this, superConfig);

	if (this.editable) {
		this.characteristicToolbar = new Gemma.FactorValueCharacteristicToolbar({
			grid : thisToolbar.grid,
			renderTo : thisToolbar.getEl().createChild()
		});
	}

};

/*
 * instance methods...
 */
Ext.extend(Gemma.FactorValueToolbar, Ext.Toolbar, {

	reloadExperimentalFactors : function() {
		this.factorCombo.store.reload();
	}

});

/*
 * Gemma.FactorValueCharacteristicToolbar constructor... config is a hash with
 * the following options: grid is the grid that contains the factor values.
 */
Gemma.FactorValueCharacteristicToolbar = function(config) {

	this.grid = config.grid;
	delete config.grid;
	this.experimentalDesign = this.grid.experimentalDesign;
	this.experimentalFactor = this.grid.experimentalFactor;
	this.factorValue = {
		id : 0,
		classDelegatingFor : "FactorValue"
	};

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
	this.factorValueCombo = new Gemma.FactorValueCombo({
		disabled : this.experimentalFactor.id ? false : true
	});
	var factorValueCombo = this.factorValueCombo;
	factorValueCombo.on("select", function(combo, record, index) {
		thisToolbar.factorValue.id = record.data.factorValueId;
		mgedCombo.enable();
	});

	var mgedCombo = new Gemma.MGEDCombo({
		disabled : true,
		emptyText : "Select a class",
		termKey : "factorvalue"
	});
	mgedCombo.on("select", function(combo, record, index) {
		charCombo.setCategory(record.data.term, record.data.uri);
		charCombo.enable();
		createButton.enable();
	});

	var charCombo = new Gemma.CharacteristicCombo({
		disabled : true
	});

	var createButton = new Ext.Toolbar.Button({
		text : "create",
		tooltip : "Create the new characteristic",
		disabled : true,
		handler : function() {
			var c = charCombo.getCharacteristic();
			createButton.disable();
			// removed in response to bug 1016 mgedCombo.reset();
			charCombo.reset();
			var callback = function() {
				thisToolbar.grid.factorValuesChanged.call(thisToolbar.grid, []);
				thisToolbar.factorValueCombo.store.reload();
					// TODO do something to reset the text of the selected item,
					// in case it changed...
			};
			ExperimentalDesignController.createFactorValueCharacteristic(
					thisToolbar.factorValue, c, callback);
		}
	});

	var deleteButton = new Ext.Toolbar.Button({
		text : "delete",
		tooltip : "Delete selected characteristics",
		disabled : true,
		handler : function() {
			deleteButton.disable();
			var selected = thisToolbar.grid.getSelectedRecords();
			var callback = function() {
				thisToolbar.grid.factorValuesChanged.call(thisToolbar.grid,
						selected);
			};
			ExperimentalDesignController.deleteFactorValueCharacteristics(
					selected, callback);
		}
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
		tooltip : "Save changed characteristics",
		disabled : true,
		handler : function() {
			saveButton.disable();
			var edited = thisToolbar.grid.getEditedRecords();
			var seen = {}, fvids = [];
			for (var i = 0; i < edited.length; ++i) {
			}
			var callback = function() {
				thisToolbar.grid.factorValuesChanged.call(thisToolbar.grid,
						edited);
			};
			ExperimentalDesignController.updateFactorValueCharacteristics(
					edited, callback);
		}
	});
	this.grid.on("afteredit", function(model) {
		saveButton.enable();
	});
	var revertButton = new Ext.Toolbar.Button({
		text : "revert",
		tooltip : "Undo changes to selected characteristics",
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

	// TODO when factor values are added or deleted, refresh the factor value
	// combo as appropriate...

	var items = [new Ext.Toolbar.TextItem("Add a Characteristic to:"),
			factorValueCombo, new Ext.Toolbar.Spacer(), mgedCombo,
			new Ext.Toolbar.Spacer(), charCombo, new Ext.Toolbar.Spacer(),
			createButton, new Ext.Toolbar.Spacer(), deleteButton,
			new Ext.Toolbar.Spacer(), saveButton, new Ext.Toolbar.Spacer(),
			revertButton];
	config.items = config.items ? items.concat(config.items) : items;

	for (property in config) {
		superConfig[property] = config[property];
	}
	Gemma.FactorValueCharacteristicToolbar.superclass.constructor.call(this,
			superConfig);
};

/*
 * instance methods...
 */
Ext.extend(Gemma.FactorValueCharacteristicToolbar, Ext.Toolbar, {

	setExperimentalFactor : function(efId) {
		this.experimentalFactor.id = efId;
		this.factorValueCombo.setExperimentalFactor(efId);
		this.factorValueCombo.enable(); // TODO do this in the callback
	}
});