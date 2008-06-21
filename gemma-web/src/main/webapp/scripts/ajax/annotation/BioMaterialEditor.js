Ext.namespace('Gemma');

Gemma.BioMaterialEditor = function(config) {
	return {
		originalConfig : config,
		expressionExperiment : {
			id : config.eeId,
			classDelegatingFor : "ExpressionExperiment"
		},

		/**
		 * We make two ajax calls; the first gets the biomaterials, the second
		 * gets the experimentalfactors. These are run in succession so both
		 * values can be given to the BioMaterialGrid constructor.
		 */
		firstCallback : function(data) {

			// second ajax call.
			ExperimentalDesignController.getExperimentalFactors(
					this.expressionExperiment, function(factorData) {
						config = {
							factors : factorData,
							bioMaterials : data
						};
						Ext.apply(config, this.originalConfig);

						// construct the grid.
						this.grid = new Gemma.BioMaterialGrid(config);
						this.grid.init = this.init.createDelegate(this);
						this.grid.render(); // not needed?
						this.grid.getTopToolbar().grid = this.grid;
					}.createDelegate(this));
		},

		/**
		 * Gets called on startup but also when a refresh is needed.
		 */
		init : function() {
			if (this.grid) {
				// grid.destroy() seems to be broken...
				try {
					this.grid.destroy();
				} catch (e) {
				}
			}

			// first ajax call.
			ExperimentalDesignController.getBioMaterials(
					this.expressionExperiment, this.firstCallback
							.createDelegate(this, [], true));
		}
	};
};

Gemma.BioMaterialGrid = Ext.extend(Gemma.GemmaGridPanel, {
	viewConfig : {
		forceFit : true
	},

	/**
	 * See ExperimentalDesignController.getExperimentalFactors and
	 * ExperimentalFactorValueObject AND FactorValueValueObject to see layout of
	 * the object that is passed.
	 * 
	 * @param factors,
	 *            fetched with getExperimentalFactors.
	 */
	createColumns : function(factors) {
		var columns = [this.rowExpander, {
			id : "bm",
			header : "BioMaterial",
			dataIndex : "bmName",
			sortable : true
		}, {
			id : "ba",
			header : "BioAssay",
			dataIndex : "baName",
			sortable : true
		}];

		this.factorValueCombos = [];
		for (f in factors) {
			var factor = factors[f];
			// f is the id.
			if (factor.id) {
				var factorId = "factor" + factor.id;
				// Create one factorValueCombo per factor.
				this.factorValueCombos[factorId] = new Gemma.FactorValueCombo({
					efId : factor.id,
					lazyInit : false,
					lazyRender : true,
					record : this.fvRecord
				});
				var editor;
				if (this.editable) {
					editor = this.factorValueCombos[factorId];
				}

				var factorValues = [];
				// factorValueValueObjects
				for (fv in factor.values) {
					if (!factor.values[fv].factorValueId) {
						continue;
					}
					var fvs = factor.values[fv].factorValueString;
					var id = "fv" + factor.values[fv].factorValueId;
					if (factorValues.indexOf(fvs < 0)) {
						factorValues[id] = fvs;
					}
				}

				/*
				 * Generate a function to render the factor values as displayed
				 * in the cells. At this point factorValue contains all the
				 * possible values for this factor.
				 */
				var rend = this.createValueRenderer(factorValues);

				/*
				 * Define the column for this particular factor.
				 */
				columns.push({
					id : factorId,
					header : factor.name,
					dataIndex : factorId,
					renderer : rend,
					editor : editor,
					sortable : true
				});

			}
		}
		return columns;
	},

	/**
	 * See ExperimentalDesignController.getBioMaterials BioMaterialValueObject
	 * to see layout of the object that is passed. *
	 * 
	 * @param biomaterial
	 *            A template so we know how the records will be laid out.
	 */
	createRecord : function(biomaterial) {

		var fields = [{
			name : "id",
			type : "int"
		}, {
			name : "bmName",
			type : "string"
		}, {
			name : "bmDesc",
			type : "string"
		}, {
			name : "bmChars",
			type : "string"
		}, {
			name : "baName",
			type : "string"
		}, {
			name : "baDesc",
			type : "string"
		}];

		// Add one slot per factor. The name of the fields will be like
		// 'factor428'. This must be used as the dataIndex for the columnModel.
		if (biomaterial.factors) {
			for (factorId in biomaterial.factors) {
				if (factorId.indexOf("factor") >= 0) {
					var o = {
						name : factorId, // the dataIndex, used in the
						// columnModel to access this.
						type : "string"
					};
					fields.push(o);
				}
			}
		}
		var record = Ext.data.Record.create(fields);
		return record;
	},

	initComponent : function() {

		var data = this.transformData(this.bioMaterials);

		this.record = this.createRecord(this.bioMaterials[0]);

		Ext.apply(this, {
			factorValueCombo : {},
			plugins : this.rowExpander,
			store : new Ext.data.Store({
				proxy : new Ext.data.MemoryProxy(data),
				reader : new Ext.data.ArrayReader({}, this.record)
			})
		});

		// must be done separately.
		Ext.apply(this, {
			columns : this.createColumns(this.factors)
		});

		this.tbar = new Gemma.BioMaterialToolbar({});

		Gemma.BioMaterialGrid.superclass.initComponent.call(this);

		if (this.editable) {
			this.on("afteredit", function(e) {
				var factorId = this.getColumnModel().getColumnId(e.column);
				var combo = this.factorValueCombo[factorId];
				var fvvo = combo.getFactorValue.call(combo);
				e.record.set(factorId, fvvo.factorValueId);
				this.getView().refresh();
			});
		}

		/*
		 * Event handlers for toolbar buttons.
		 * 
		 */
		this.getTopToolbar().on("toggleExpand", function() {
			this.rowExpander.toggleAll();
		}, this);

		this.getTopToolbar().on("apply", function(factor, factorValue) {
			var selected = this.getSelectionModel().getSelections();
			for (var i = 0; i < selected.length; ++i) {
				selected[i].set(factor, factorValue);
			}
			this.getView().refresh();
		}, this);

		this.getTopToolbar().on("save", function() {
			var edited = this.getEditedRecords();
			var bmvos = [];
			for (var i = 0; i < edited.length; ++i) {
				var row = edited[i];
				var bmvo = {
					id : row.id,
					factorIdToFactorValueId : {}
				};
				for (var j in row) {
					if (row[j].substring(0, 6) == "factor") {
						bmvo.factorIdToFactorValueId[j] = row[j];
					}
				}
				bmvos.push(bmvo);
			}
			var callback = this.refresh.createDelegate(this); // check
			ExperimentalDesignController.updateBioMaterials(bmvos, callback);
		}, this);

		this.getStore().load();
	},

	onRender : function(c, l) {
		this.getTopToolbar().grid = this;
		this.getTopToolbar().editable = this.editable;
		Gemma.BioMaterialGrid.superclass.onRender.call(this, c, l);
	},

	/**
	 * Turn the incoming biomaterial valueobjects into an array structure that
	 * can be loaded into an ArrayReader.
	 */
	transformData : function(incoming) {
		var data = [];
		for (var i = 0; i < incoming.length; ++i) {
			var bmvo = incoming[i];
			var factors = incoming[i].factors;
			data[i] = [bmvo.id, bmvo.name, bmvo.description,
					bmvo.characteristics, bmvo.assayName, bmvo.assayDescription];

			for (factorId in factors) {
				if (factorId.indexOf("factor") >= 0) {
					data[i].push(incoming[i].factorIdToFactorValueId[factorId]);
				}
			}
		}
		return data;
	},

	fvRecord : Ext.data.Record.create([{
		name : "charId",
		type : "int"
	}, {
		name : "factorValueId",
		type : "string",
		convert : function(v) {
			return "fv" + v;
		}
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
		name : "valueUri",
		type : "string"
	}, {
		name : "factorValueString",
		type : "string"
	}]),

	reloadFactorValues : function() {
		for (var i in this.factorValueCombo) {
			var factorId = this.factorValueCombo[i];
			if (factorId.substring(0, 6) == "factor") {
				var combo = this.factorValueCombo[factorId];
				var column = this.getColumnModel().getColumnById(factorId);
				// FIXME this wll no longer work.
				combo.setExperimentalFactor(combo.experimentalFactor.id,
						function(r, options, success) {
							var fvs = {};
							for (var i = 0; i < r.length; ++i) {
								fvs["fv" + r[i].data.factorValueId] = r[i].data.factorValueString;
							}
							var renderer = this.createValueRenderer(fvs);
							column.renderer = renderer;
							this.getView().refresh();
						});
			}
		}
	},

	createValueRenderer : function(factorValues) {
		return function(value, metadata, record, row, col, ds) {
			return factorValues[value] ? factorValues[value] : value;
		};
	},

	rowExpander : new Ext.grid.RowExpander({
		tpl : new Ext.Template(
				"<dl style='margin-left: 1em; margin-bottom: 2px;'><dt>BioMaterial {bmName}</dt><dd>{bmDesc}<br>{bmChars}</dd>",
				"<dt>BioAssay {baName}</dt><dd>{baDesc}</dd></dl>")
	})

});

/*
 * Gemma.BioMaterialToolbar constructor... config is a hash with the following
 * options: grid is the grid that contains the factor values.
 */
Gemma.BioMaterialToolbar = Ext.extend(Ext.Toolbar, {

	initComponent : function() {
		Gemma.BioMaterialToolbar.superclass.initComponent.call(this);

		this.addEvents("revertSelected", "toggleExpand", "apply", "save");
	},

	onRender : function(c, l) {
		Gemma.BioMaterialToolbar.superclass.onRender.call(this, c, l);

		var saveButton = new Ext.Toolbar.Button({
			text : "save",
			tooltip : "Save changed biomaterials",
			disabled : true,
			handler : function() {
				this.fireEvent("save");
				saveButton.disable();
			}
		});

		this.grid.on("afteredit", function(model) {
			saveButton.enable();
		});

		var revertButton = new Ext.Toolbar.Button({
			text : "revert",
			tooltip : "Undo changes to selected biomaterials",
			disabled : true,
			handler : function() {
				this.grid.revertSelected();
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

		var refreshButton = new Ext.Toolbar.Button({
			text : "Expand/collapse all",
			tooltip : "Show/hide all biomaterial details",
			handler : function() {
				this.fireEvent("toggleExpand");
			}.createDelegate(this)
		});

		if (this.editable) {
			this.addText("Make changes to the grid below");
			this.addSpacer();
			this.addButton(saveButton);
			this.addSeparator();
			this.addButton(revertButton);
		}
		this.addFill();
		this.addButton(refreshButton);

		if (this.editable) {
			this.factorCombo = new Gemma.ExperimentalFactorCombo({
				emptyText : "select a factor",
				edId : this.grid.edId
			});
			var factorCombo = this.factorCombo;
			factorCombo.on("select", function(combo, record, index) {
				factorValueCombo.setExperimentalFactor(record.id);
				factorValueCombo.enable(); // TODO do this in the callback
			});

			this.factorValueCombo = new Gemma.FactorValueCombo({
				emptyText : "select a factor value",
				disabled : true
			});
			var factorValueCombo = this.factorValueCombo;
			factorValueCombo.on("select", function(combo, record, index) {
				this.grid.getSelectionModel().on("selectionchange",
						enableApplyOnSelect);
				enableApplyOnSelect(this.grid.getSelectionModel());
			});

			var applyButton = new Ext.Toolbar.Button({
				text : "apply",
				tooltip : "Apply this value to selected biomaterials",
				disabled : true,
				handler : function() {
					var factor = "factor" + factorCombo.getValue();
					var factorValue = "fv" + factorValueCombo.getValue();
					this.fireEvent("apply", factor, factorValue)
					saveButton.enable();
				}
			});

			var enableApplyOnSelect = function(model) {
				var selected = model.getSelections();
				if (selected.length > 0) {
					applyButton.enable();
				} else {
					applyButton.disable();
				}
			};

			var secondToolbar = new Ext.Toolbar(this.getEl().createChild());
			secondToolbar.addText("Bulk changes:");
			secondToolbar.addSpacer();
			secondToolbar.addField(factorCombo);
			secondToolbar.addSpacer();
			secondToolbar.addField(factorValueCombo);
			secondToolbar.addSpacer();
			secondToolbar.addField(applyButton);
		}
	}
});