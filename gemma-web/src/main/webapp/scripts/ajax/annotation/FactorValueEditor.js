Ext.namespace('Gemma');

/*
 * Gemma.FactorValueGrid constructor... config is a hash with the following
 * options: edId the id of the ExperimentalDesign whose ExperimentalFactors can
 * be displayed in the grid efId the id of the ExperimentalFactor whose
 * FactorValues are displayed in the grid form the name of the HTML form the
 * grid is in
 */
Gemma.FactorValueGrid = function(config) {

	this.factorValueToolbar = new Gemma.FactorValueToolbar({
		grid : this,
		renderTo : this.tbar
	});
};

/*
 * instance methods...
 */
Gemma.FactorValueGrid = Ext.extend(Gemma.GemmaGridPanel, {

	record : Ext.data.Record.create([{
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
	}]),
	categoryStyler : function(value, metadata, record, row, col, ds) {
		return Gemma.GemmaGridPanel.formatTermWithStyle(value,
				record.data.categoryUri);
	},
	valueStyler : function(value, metadata, record, row, col, ds) {
		return Gemma.GemmaGridPanel.formatTermWithStyle(value,
				record.data.valueUri);
	},
	flattenCharacteristics : function(chars) {
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
	},
	initComponent : function() {

		this.columns = [{
			header : "FactorValue",
			dataIndex : "factorValueId"
		}, {
			header : "Category",
			dataIndex : "category",
			renderer : this.categoryStyler
		}, {
			header : "Value",
			dataIndex : "value",
			renderer : this.valueStyler
		}];

		this.experimentalDesign = {
			id : this.edId,
			classDelegatingFor : "ExperimentalDesign"
		};
		this.experimentalFactor = {
			id : this.efId,
			classDelegatingFor : "ExperimentalFactor"
		};

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

		this.store = new Ext.data.GroupingStore({
			proxy : new Ext.data.DWRProxy(ExperimentalDesignController.getFactorValuesWithCharacteristics),
			reader : new Ext.data.ListRangeReader({
				id : "charId"
			}, this.record),
			groupField : "factorValueId",
			sortInfo : {
				field : "category",
				direction : "ASC"
			}
		});

		var groupTextTpl = this.editable
				? '<input type="checkbox" name="selectedFactorValues" value="{[ values.rs[0].data.factorValueId ]}" /> '
				: '';
		groupTextTpl = groupTextTpl
				+ '{[ values.rs[0].data.factorValueString ]}';

		this.view = new Ext.grid.GroupingView({
			enableGroupingMenu : false,
			enableNoGroups : false,
			groupTextTpl : groupTextTpl,
			hideGroupedColumn : true,
			showGroupName : true,
			startCollapsed : true
		});

		this.tbar = new Gemma.FactorValueToolbar({
			editable : this.editable,
			experimentalDesign : this.experimentalDesign
		});

		var FACTOR_VALUE_COLUMN = 0;
		var CATEGORY_COLUMN = 1;
		var VALUE_COLUMN = 2;
		this.autoExpandColumn = VALUE_COLUMN;

		// ///////////////////////////////////////
		Gemma.FactorValueGrid.superclass.initComponent.call(this);

		this.addEvents('factorvaluecreate', 'factorvaluechange',
				'factorvaluedelete');

		this.getTopToolbar().on("select", function(id) {
			this.setExperimentalFactor(id);
		}.createDelegate(this), this);

		if (this.editable) {

			this.getColumnModel().setEditor(CATEGORY_COLUMN, categoryEditor);
			this.getColumnModel().setEditor(VALUE_COLUMN, valueEditor);

			this.on("afteredit", function(e) {
				var col = this.getColumnModel().getColumnId(e.column);
				if (col == CATEGORY_COLUMN) {
					var term = this.categoryCombo.getTerm
							.call(this.categoryCombo);
					e.record.set("category", term.term);
					e.record.set("categoryUri", term.uri);
				} else if (col == VALUE_COLUMN) {
					var c = this.valueCombo.getCharacteristic
							.call(this.valueCombo);
					e.record.set("value", c.value);
					e.record.set("valueUri", c.valueUri);
				}
				this.getView().refresh();
			});

			this.getSelectionModel().on("selectionchange", function(model) {
				var selected = model.getSelections();
				this.revertButton.disable();
				for (var i = 0; i < selected.length; ++i) {
					if (selected[i].dirty) {
						this.revertButton.enable();
						break;
					}
				}

				if (selected.length > 0) {
					this.deleteButton.enable();
				} else {
					this.deleteButton.disable();
				}
			});

			this.on("afteredit", function(model) {
				this.saveButton.enable();
			}, this.getTopToolbar());

			this.getTopToolbar().on("save", function() {
			});

			/*
			 * Create a new factorvalue
			 */
			this.getTopToolbar().on("create", function() {
				var ef = this.experimentalFactor;
				var callback = function() {
					this.factorValueCreated.call(this, ef);
					this.getTopToolbar().characteristicToolbar
							.setExperimentalFactor(ef.id);
				};
				ExperimentalDesignController.createFactorValue(
						this.experimentalFactor, callback);
			}.createDelegate(this));

			/*
			 * delete a factor value
			 */
			this.getTopToolbar().on("delete", function() {
				var selected = this.getSelectedFactorValues();
				var ef = this.experimentalFactor;
				var callback = function() {
					this.factorValuesDeleted.call(this, selected);
				};
				ExperimentalDesignController.deleteFactorValues(ef, selected,
						callback);
			}.createDelegate(this));

			this.getTopToolbar().on("toggleExpand",
					this.getView().toggleAllGroups.createDelegate(this));

			var ct = this.getTopToolbar().characteristicToolbar;
			if (ct) {
				ct.on("save", function() {
					var edited = this.getEditedRecords();
					var seen = {}, fvids = [];
					for (var i = 0; i < edited.length; ++i) {
						// ??
					}
					var callback = function() {
						this.factorValuesChanged.call(this, edited);
					};
					ExperimentalDesignController
							.updateFactorValueCharacteristics(edited, callback);
				});
				ct.on("create", function(f, c) {
					var callback = function() {
						this.factorValueCombo.store.reload();

						this.factorValuesChanged.call(this, []);
							// TODO do something to reset the text of the
							// selected
							// item,
							// in case it changed...
					};
					ExperimentalDesignController
							.createFactorValueCharacteristic(f, c, callback);
				});

				ct.on("undo", this.revertSelected);

				ct.on("delete", function() {
					var selected = this.getSelectedRecords();
					var callback = function() {
						this.factorValuesChanged.call(this, selected);
					};
					ExperimentalDesignController
							.deleteFactorValueCharacteristics(selected,
									callback);
				});
			}
		}

		if (this.experimentalFactor.id) {
			this.store.load({
				params : [this.experimentalFactor]
			});
		}
	},

	// refresh : function(ct, p) {
	// Gemma.FactorValueGrid.superclass.refresh.call(this, ct, p);
	// if (this.onRefresh) {
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
Gemma.FactorValueToolbar = Ext.extend(Ext.Toolbar, {
	initComponent : function() {
		Gemma.FactorValueToolbar.superclass.initComponent.call(this);
		this.addEvents("select", "create", "delete", "toggleAll");
	},

	onRender : function(c, p) {

		Gemma.FactorValueToolbar.superclass.onRender.call(this, c, p);

		this.factorCombo = new Gemma.ExperimentalFactorCombo({
			edId : this.experimentalDesign.id
		});

		this.factorCombo.on("select", function(combo, record, index) {
			this.fireEvent("select", record.id);
			this.createFactorValueButton.enable();
			if (this.characteristicToolbar) {
				this.characteristicToolbar.setExperimentalFactor(record.id);
			}
		}, this);

		this.createFactorValueButton = new Ext.Toolbar.Button({
			text : "create",
			tooltip : "Create a new factor value",
			disabled : true,
			handler : function() {
				this.fireEvent("create");
			},
			scope : this
		});

		this.deleteFactorValueButton = new Ext.Toolbar.Button({
			text : "delete",
			tooltip : "Delete selected factor values",
			disabled : false,
			handler : function() {
				this.fireEvent("delete");
			}
		});

		this.addText("Show Factor Values for:");
		this.addSpacer();
		this.add(this.factorCombo);

		if (this.editable) {
			this.addSpacer();
			this.addButton(this.createFactorValueButton);
			this.addSeparator();
			this.addButton(this.deleteFactorValueButton);
		}

		this.refreshButton = new Ext.Toolbar.Button({
			text : "Expand/collapse all",
			tooltip : "Show/hide all factor value details",
			handler : function() {
				this.fireEvent("toggleExpand");
			},
			scope : this
		});

		this.addFill();
		this.addButton(this.refreshButton);

		if (this.editable) {
			this.characteristicToolbar = new Gemma.FactorValueCharacteristicToolbar({
				renderTo : this.getEl().createChild()
			});
			this.add(this.characteristicToolbar);
		}
	},

	reloadExperimentalFactors : function() {
		this.factorCombo.store.reload();
	}

});

/**
 * 
 * @class Gemma.FactorValueCharacteristicToolbar
 * @extends Ext.Toolbar
 */
Gemma.FactorValueCharacteristicToolbar = Ext.extend(Ext.Toolbar, {

	initComponent : function() {
		Gemma.FactorValueCharacteristicToolbar.superclass.initComponent
				.call(this);

		Ext.apply(this, {
			factorValue : {
				id : -1,
				classDelegatingFor : "FactorValue"
			},

			experimentalFactor : {
				id : -1,
				classDelegatingFor : "ExperimentalFactor"
			}
		});

		this.addEvents("save", "create", "delete", "undo");
	},

	onRender : function(c, l) {
		Gemma.FactorValueCharacteristicToolbar.superclass.onRender.call(this,
				c, l);

		this.factorValueCombo = new Gemma.FactorValueCombo({
			disabled : this.experimentalFactor.id ? false : true
		});

		this.factorValueCombo.on("select", function(combo, record, index) {
			this.factorValue.id = record.get("factorValueId");
			mgedCombo.enable();
		}.createDelegate(this));

		var mgedCombo = new Gemma.MGEDCombo({
			disabled : true,
			emptyText : "Select a class",
			termKey : "factorvalue"
		});

		mgedCombo.on("select", function(combo, record, index) {
			charCombo.setCategory(record.get("term"), record.get("uri"));
			charCombo.enable();
			this.createButton.enable();
		}.createDelegate(this));

		var charCombo = new Gemma.CharacteristicCombo({
			disabled : true
		});

		this.createButton = new Ext.Toolbar.Button({
			text : "create",
			tooltip : "Create the new characteristic",
			disabled : true,
			handler : function() {
				var c = charCombo.getCharacteristic();
				this.createButton.disable();
				// removed in response to bug 1016 mgedCombo.reset();
				charCombo.reset();
				this.fireEvent("create", this.factorValue, c);
			},
			scope : this
		});

		this.deleteButton = new Ext.Toolbar.Button({
			text : "delete",
			tooltip : "Delete selected characteristics",
			disabled : true,
			handler : function() {
				deleteButton.disable();
				this.fireEvent("delete");
			}
		});

		this.saveButton = new Ext.Toolbar.Button({
			text : "save",
			tooltip : "Save changed characteristics",
			disabled : true,
			handler : function() {
				saveButton.disable();
				this.fireEvent("save");

			}
		});

		this.revertButton = new Ext.Toolbar.Button({
			text : "revert",
			tooltip : "Undo changes to selected characteristics",
			disabled : true,
			handler : function() {
				this.fireEvent("undo");
			}
		});

		this.addText("Add a characteristic to:");
		this.add(this.factorValueCombo);
		this.addSpacer();
		this.add(mgedCombo);
		this.addSpacer();
		this.add(charCombo);
		this.addSpacer();
		this.addButton(this.createButton);
		this.addSpacer();
		this.addButton(this.saveButton)
		this.addSpacer();
		this.addButton(this.revertButton);

	},

	setExperimentalFactor : function(efId) {
		this.experimentalFactor.id = efId;
		this.factorValueCombo.setExperimentalFactor(efId);
		this.factorValueCombo.enable(); // TODO do this in the callback
	}
});