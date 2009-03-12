Ext.namespace('Gemma');

/**
 * Forms the bottom panel of the experimental design editor.
 * 
 * @author Luke, Paul
 * @version $Id$
 */
Gemma.FactorValueGrid = Ext.extend(Gemma.GemmaGridPanel, {

	loadMask : true,

	record : Ext.data.Record.create([{
				name : "charId",
				type : "int"
			}, {
				name : "id",
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
				name : "factorValue",
				type : "string"
			}]),

	categoryStyler : function(value, metadata, record, row, col, ds) {
		return Gemma.GemmaGridPanel.formatTermWithStyle(value, record.data.categoryUri);
	},

	valueStyler : function(value, metadata, record, row, col, ds) {
		if (value) {
			return Gemma.GemmaGridPanel.formatTermWithStyle(value, record.data.valueUri);
		} else {
			return '<span style="color:grey">Double-click to define the value</span>';
		}
	},

	initComponent : function() {

		this.columns = [{
					header : "FactorValue",
					dataIndex : "id"
				}, {
					header : "Category",
					dataIndex : "category",
					renderer : this.categoryStyler
				}, {
					header : "Value",
					dataIndex : "factorValue",
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
					groupField : "id",
					sortInfo : {
						field : "category",
						direction : "ASC"
					}
				});

		/*
		 * The checkboxes defined here require that this.form be set: a <form> element wrapping the div that this goes
		 * in. Clumsy but it works.
		 */
		var groupTextTpl = this.editable
				? '<input id="{[ values.rs[0].data.id ]}" type="checkbox" name="selectedFactorValues" value="{[ values.rs[0].data.id ]}" />&nbsp;&nbsp; '
				: '';
		groupTextTpl = groupTextTpl + '{[ values.rs[0].data.factorValue ]}';

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

		this.addEvents('factorvaluecreate', 'factorvaluechange', 'factorvaluedelete');

		if (this.editable) {

			this.getColumnModel().setEditor(CATEGORY_COLUMN, categoryEditor);
			this.getColumnModel().setEditor(VALUE_COLUMN, valueEditor);

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

			this.getSelectionModel().on("selectionchange", function(model) {
						// console.log("selection");
						var selected = model.getSelections();
						this.revertButton.disable();
						for (var i = 0; i < selected.length; ++i) {
							if (selected[i].dirty) {
								this.revertButton.enable();
								break;
							}
						}
						if (selected.length > 0) {
							this.deleteFactorValueButton.enable();
							this.characteristicToolbar.deleteButton.enable();
						} else {
							this.deleteFactorValueButton.disable();
							this.characteristicToolbar.deleteButton.disable();
						}
					}, this.getTopToolbar());

			this.on("afteredit", function(model) {
						this.saveButton.enable();
						this.revertButton.enable();
					}, this.getTopToolbar());

			/*
			 * Create a new factorvalue
			 */
			this.getTopToolbar().on("create", function() {
						var ef = this.experimentalFactor;
						var oldmsg = this.loadMask.msg;
						this.loadMask.msg = "Creating new factor value";
						this.loadMask.show();

						var callback = function() {
							this.loadMask.hide();
							this.loadMask.msg = oldmsg;
							this.factorValueCreated(ef);
							this.getTopToolbar().characteristicToolbar.setExperimentalFactor(ef.id);
						}.createDelegate(this);
						var errorHandler = function(er) {
							this.loadMask.hide();
							this.loadMask.msg = oldmsg;
							Ext.Msg.alert("Error", er);
						}.createDelegate(this);

						ExperimentalDesignController.createFactorValue(this.experimentalFactor, {
									callback : callback,
									errorHandler : errorHandler
								});
					}.createDelegate(this));

			/*
			 * delete a factor value
			 */
			this.getTopToolbar().on("delete", function() {
						var selected = this.getSelectedFactorValues();
						if (selected) {
							Ext.getCmp('factor-value-delete-button').disable();
							var ef = this.experimentalFactor;
							var callback = function() {
								this.factorValuesDeleted(selected);
							}.createDelegate(this);
							ExperimentalDesignController.deleteFactorValues(ef, selected, callback);
						}
					}.createDelegate(this), this);

			/*
			 * Commit changes to factor values (added characteristics)
			 */
			this.getTopToolbar().on("save", function() {
						var edited = this.getEditedRecords();
						var seen = {}, fvids = [];
						for (var i = 0; i < edited.length; ++i) {
							// ??
						}
						var callback = function() {
							this.factorValuesChanged(edited);
						}.createDelegate(this);
						ExperimentalDesignController.updateFactorValueCharacteristics(edited, callback);
					}, this);

			this.getTopToolbar().on("undo", this.revertSelected.createDelegate(this), this);

			this.getTopToolbar().on("toggleExpand", function() {
						this.getView().toggleAllGroups()
					}.createDelegate(this), this);

		}

		if (this.experimentalFactor.id) {
			this.store.load({
						params : [this.experimentalFactor]
					});
		}
	}, // init component

	onRender : function(c, p) {
		Gemma.FactorValueGrid.superclass.onRender.call(this, c, p);
		// have to do this here, because the toolbar isn't created until
		// rendering.
		var ct = this.getTopToolbar().characteristicToolbar;
		if (ct) {
			ct.on("create", function(f, c) {
						var callback = function() {
							console.log("reload");
							ct.factorValueCombo.store.reload();
							this.factorValuesChanged.call(this, []);
							// TODO do something to reset the text of the
							// selected
							// item,
							// in case it changed...
						}.createDelegate(this);
						ExperimentalDesignController.createFactorValueCharacteristic(f, c, callback);
					}.createDelegate(this), this);

			ct.on("delete", function() {
						// console.log("deleting factorvalue(s)");
						var selected = this.getSelectedRecords();
						var callback = function() {
							this.factorValuesChanged.call(this, selected);
						}.createDelegate(this);
						ExperimentalDesignController.deleteFactorValueCharacteristics(selected, callback);
					}.createDelegate(this), this);
		}
	},

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
		this.getTopToolbar().setExperimentalFactor(efId);
	},

	/**
	 * Processing of the checkboxes defined in the tmpl for the groups. Requires that this.form be set: a <form> element
	 * wrapping the div that this goes in. Clumsy but it works.
	 */
	getSelectedFactorValues : function() {
		if (this.form) {
			var f = document.forms[this.form];
			var checkboxes = f.selectedFactorValues;
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
		}
	}

});

/**
 * 
 */
Gemma.FactorValueToolbar = Ext.extend(Ext.Toolbar, {
			initComponent : function() {
				Gemma.FactorValueToolbar.superclass.initComponent.call(this);
				this.addEvents("create", "save", "delete", "undo", "toggleExpand");
			},

			onRender : function(c, p) {

				Gemma.FactorValueToolbar.superclass.onRender.call(this, c, p);

				this.createFactorValueButton = new Ext.Toolbar.Button({
							text : "Create",
							id : 'factor-value-create-button',
							tooltip : "Create a new factor value for the current factor",
							disabled : true,
							handler : function() {
								this.fireEvent("create");
							},
							scope : this
						});

				this.deleteFactorValueButton = new Ext.Toolbar.Button({
							text : "Delete",
							id : 'factor-value-delete-button',
							tooltip : "Delete checked factor values",
							disabled : false,
							handler : function() {
								Ext.Msg.confirm('Deleting factor value(s)', 'Are you sure? This cannot be undone',
										function(but) {
											if (but == 'yes') {
												// this.deleteFactorValueButton.disable();
												this.fireEvent("delete");
											}
										}.createDelegate(this));
							}.createDelegate(this)
						});

				this.saveButton = new Ext.Toolbar.Button({
							text : "Save",
							id : 'factor-value-save-button',
							tooltip : "Commit changes to  factor values",
							disabled : true,
							handler : function() {
								this.saveButton.disable();
								this.fireEvent("save");
							}.createDelegate(this)
						});

				this.revertButton = new Ext.Toolbar.Button({
							text : "undo",
							id : 'factor-value-undo-button',
							tooltip : "Undo changes to selected factor values",
							disabled : true,
							handler : function() {
								this.fireEvent("undo");
							},
							scope : this
						});

				if (this.editable) {
					this.addButton(this.createFactorValueButton);
					this.addSeparator();
					this.addButton(this.deleteFactorValueButton);
					this.addSpacer();
					this.addButton(this.saveButton);
					this.addSpacer();
					this.addButton(this.revertButton);
				}

				this.expandButton = new Ext.Toolbar.Button({
							text : "Expand/collapse all",
							tooltip : "Show/hide all factor value details",
							handler : function() {
								this.fireEvent("toggleExpand");
							},
							scope : this
						});

				this.addFill();
				this.addButton(this.expandButton);

				if (this.editable) {
					this.characteristicToolbar = new Gemma.FactorValueCharacteristicToolbar({
								renderTo : this.getEl().createChild()
							});
					this.add(this.characteristicToolbar);
				}
			},

			setExperimentalFactor : function(efId) {
				this.efId = efId;
				this.createFactorValueButton.enable();
				if (this.characteristicToolbar) {
					this.characteristicToolbar.setExperimentalFactor(efId);
				}
			}

		});

/**
 * 
 * @class Gemma.FactorValueCharacteristicToolbar
 * @extends Ext.Toolbar
 */
Gemma.FactorValueCharacteristicToolbar = Ext.extend(Ext.Toolbar, {

			initComponent : function() {
				Gemma.FactorValueCharacteristicToolbar.superclass.initComponent.call(this);

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

				this.addEvents("create", "delete");
			},

			onRender : function(c, l) {
				Gemma.FactorValueCharacteristicToolbar.superclass.onRender.call(this, c, l);

				this.factorValueCombo = new Gemma.FactorValueCombo({
							disabled : this.experimentalFactor.id >= 0 ? false : true,
							efId : this.experimentalFactor.id >= 0 ? this.experimentalFactor.id : null
						});

				this.factorValueCombo.on("select", function(combo, record, index) {
							this.factorValue.id = record.get("id");
							this.mgedCombo.enable();
						}.createDelegate(this));

				this.mgedCombo = new Gemma.MGEDCombo({
							disabled : true,
							emptyText : "Select a category",
							termKey : "factorvalue"
						});

				this.mgedCombo.on("select", function(combo, record, index) {
							this.charCombo.setCategory(record.get("term"), record.get("uri"));
							this.charCombo.enable();
							this.createButton.enable();
						}.createDelegate(this));

				this.charCombo = new Gemma.CharacteristicCombo({
							disabled : true
						});

				this.createButton = new Ext.Toolbar.Button({
							text : "Add",
							tooltip : "Add the new characteristic to the selected factor value",
							disabled : true,
							handler : function() {
								var c = this.charCombo.getCharacteristic();

								if (!this.factorValue || !c) {
									Ext.Msg.alert("You must select a factor value and set a characteristic.");
								} else {

									this.createButton.disable();
									// removed in response to bug 1016 mgedCombo.reset();
									this.charCombo.reset();
									this.fireEvent("create", this.factorValue, c);
								}
							},
							scope : this
						});

				this.charCombo.on("select", function() {
							this.createButton.enable();
						}.createDelegate(this));

				this.charCombo.on("change", function() {
							this.createButton.enable();
						}.createDelegate(this));

				this.deleteButton = new Ext.Toolbar.Button({
							text : "Remove Characteristic",
							tooltip : "Delete the selected characteristic(s) from factor values",
							disabled : true,
							handler : function() {
								this.deleteButton.disable();
								this.fireEvent("delete");
							},
							scope : this
						});

				this.addText("Append a characteristic to:");
				this.add(this.factorValueCombo);
				this.addSpacer();
				this.addText(" with category:");
				this.add(this.mgedCombo);
				this.addSpacer();
				this.addText(" and value:");
				this.add(this.charCombo);
				this.addSpacer();
				this.addButton(this.createButton);
				this.addSpacer();
				this.addButton(this.deleteButton);

			},

			setExperimentalFactor : function(efId) {
				this.experimentalFactor.id = efId;
				this.factorValueCombo.setExperimentalFactor(efId, function() {
							this.factorValueCombo.enable();
							this.mgedCombo.enable();
							this.charCombo.enable();
						}.createDelegate(this));
			}
		});