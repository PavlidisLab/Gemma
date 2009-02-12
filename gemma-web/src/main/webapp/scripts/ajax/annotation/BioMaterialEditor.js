Ext.namespace('Gemma');

/**
 * Grid with list of biomaterials for editing experimental design parameters.
 */
Gemma.BioMaterialEditor = function(config) {
	return {
		originalConfig : config,
		expressionExperiment : {
			id : config.eeId,
			classDelegatingFor : "ExpressionExperiment"
		},

		/**
		 * We make two ajax calls; the first gets the biomaterials, the second gets the experimentalfactors. These are
		 * run in succession so both values can be given to the BioMaterialGrid constructor.
		 */
		firstCallback : function(data) {

			// second ajax call.
			ExperimentalDesignController.getExperimentalFactors(this.expressionExperiment, function(factorData) {
						config = {
							factors : factorData,
							bioMaterials : data
						};
						Ext.apply(config, this.originalConfig);

						// construct the grid.
						this.grid = new Gemma.BioMaterialGrid(config);
						this.grid.init = this.init.createDelegate(this);
					}.createDelegate(this));
		},

		/**
		 * Gets called on startup but also when a refresh is needed.
		 */
		init : function() {
			if (this.grid) {
				try {
					this.grid.destroy();
				} catch (e) {
				}

			}

			// first ajax call.
			ExperimentalDesignController.getBioMaterials(this.expressionExperiment, this.firstCallback.createDelegate(
							this, [], true));
		}
	};
};

Gemma.BioMaterialGrid = Ext.extend(Gemma.GemmaGridPanel, {
	// viewConfig : {
	// forceFit : true
	// },

	loadMask : true,
	autoExpandColumn : 'bm',

	/**
	 * See ExperimentalDesignController.getExperimentalFactors and ExperimentalFactorValueObject AND
	 * FactorValueValueObject to see layout of the object that is passed.
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

				// Create one factorValueCombo per factor. It contains all the
				// factor values.
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
					if (!factor.values[fv].id) {
						continue;
					}
					var fvs = factor.values[fv].factorValue;
					var id = "fv" + factor.values[fv].id;
					if (factorValues.indexOf(fvs < 0)) {
						factorValues[id] = fvs;
					}
				}

				/*
				 * Generate a function to render the factor values as displayed in the cells. At this point factorValue
				 * contains all the possible values for this factor.
				 */
				var rend = this.createValueRenderer(factorValues);

				/*
				 * Define the column for this particular factor.
				 */
				columns.push({
							id : id,
							header : factor.name,
							dataIndex : id,
							renderer : rend,
							editor : editor,
							sortable : true
						});

			}
		}
		return columns;
	},

	/**
	 * See ExperimentalDesignController.getBioMaterials BioMaterialValueObject to see layout of the object that is
	 * passed. *
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

		this.tbar = new Gemma.BioMaterialToolbar({
					edId : this.edId,
					editable : this.editable
				});

		Gemma.BioMaterialGrid.superclass.initComponent.call(this);

		/*
		 * Event handlers for toolbar buttons.
		 * 
		 */
		this.getTopToolbar().on("toggleExpand", function() {
					this.rowExpander.toggleAll();
				}, this);

		this.getTopToolbar().on("refresh", function() {
					this.init();
				}, this);

		if (this.editable) {
			this.on("afteredit", function(e) {
						var factorId = this.getColumnModel().getColumnId(e.column);
						var combo = this.factorValueCombos[factorId];
						var fvvo = combo.getFactorValue();
						e.record.set(factorId, fvvo.id);
						this.getTopToolbar().saveButton.enable();
						this.getView().refresh();
					}, this);

			this.getTopToolbar().on("apply", function(factor, factorValue) {
						var selected = this.getSelectionModel().getSelections();
						for (var i = 0; i < selected.length; ++i) {
							selected[i].set(factor, factorValue);
						}
						this.getView().refresh();
					}, this);

			/*
			 * Save edited records to the db.
			 */
			this.getTopToolbar().on("save", function() {
						// console.log("Saving ...");
						var edited = this.getEditedRecords();
						var bmvos = [];
						for (var i = 0; i < edited.length; ++i) {
							var row = edited[i];
							var bmvo = {
								id : row.id,
								factorIdToFactorValueId : {}
							};
							// looking for 'factor569' -> fv54910
							for (var j in row) {
								if (typeof row[j] == 'string' && row[j].indexOf("fv") >= 0) {
									bmvo.factorIdToFactorValueId[j] = row[j];
								}
							}
							bmvos.push(bmvo);
						}

						/*
						 * When we return from the server, reload the factor values.
						 */
						var callback = this.init; // check

						ExperimentalDesignController.updateBioMaterials(bmvos, callback);
					}.createDelegate(this), this);

			this.on("afteredit", function(model) {
						this.getTopToolbar().saveButton.enable();
					}.createDelegate(this));

			this.getSelectionModel().on("selectionchange", function(model) {
						var selected = model.getSelections();
						this.getTopToolbar().revertButton.disable();
						for (var i = 0; i < selected.length; ++i) {
							if (selected[i].dirty) {
								this.getTopToolbar().revertButton.enable();
								break;
							}
						}
					}.createDelegate(this), this);

			this.getSelectionModel().on("selectionchange", function(model) {
						this.enableApplyOnSelect(model);
					}.createDelegate(this.getTopToolbar()), this.getTopToolbar());

			this.getTopToolbar().on("undo", this.revertSelected, this);
		}

		this.getStore().load(/*
								 * { params : {}, callback : function() { this.sort("bmName"); } }
								 */);
	},

	/**
	 * Turn the incoming biomaterial valueobjects into an array structure that can be loaded into an ArrayReader.
	 */
	transformData : function(incoming) {
		var data = [];
		for (var i = 0; i < incoming.length; ++i) {
			var bmvo = incoming[i];
			var factors = incoming[i].factors;
			data[i] = [bmvo.id, bmvo.name, bmvo.description, bmvo.characteristics, bmvo.assayName,
					bmvo.assayDescription];

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
				name : "id",
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
				name : "factorValue",
				type : "string"
			}]),

	reloadFactorValues : function() {
		for (var i in this.factorValueCombos) {
			var factorId = this.factorValueCombos[i];
			if (typeof factorId == 'string' && factorId.substring(0, 6) == "factor") {
				var combo = this.factorValueCombos[factorId];
				var column = this.getColumnModel().getColumnById(factorId);
				combo.setExperimentalFactor(combo.experimentalFactor.id, function(r, options, success) {
							var fvs = {};
							for (var i = 0; i < r.length; ++i) {
								fvs["fv" + r[i].get("id")] = r[i].get("factorValue");
							}
							var renderer = this.createValueRenderer(fvs);
							column.renderer = renderer;
							this.getView().refresh();
						});
			}
		}
		this.getTopToolbar().factorValueCombo.store.reload();
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

/**
 * 
 */
Gemma.BioMaterialToolbar = Ext.extend(Ext.Toolbar, {

			initComponent : function() {

				this.items = [];
				if (this.editable) {

					this.saveButton = new Ext.Toolbar.Button({
								text : "Save",
								tooltip : "Save changed biomaterials",
								disabled : true,
								handler : function() {
									this.fireEvent("save");
									this.saveButton.disable();
								},
								scope : this
							});

					this.revertButton = new Ext.Toolbar.Button({
								text : "Undo",
								tooltip : "Undo changes to selected biomaterials",
								disabled : true,
								handler : function() {
									this.fireEvent("undo");
								},
								scope : this
							});

					this.factorCombo = new Gemma.ExperimentalFactorCombo({
								width : 200,
								emptyText : "select a factor",
								edId : this.edId
							});

					this.factorCombo.on("select", function(combo, record, index) {
								this.factorValueCombo.setExperimentalFactor(record.id);
								this.factorValueCombo.enable();
							}, this);

					this.factorValueCombo = new Gemma.FactorValueCombo({
								emptyText : "Select a factor value",
								disabled : false,
								width : 200
							});

					this.applyButton = new Ext.Toolbar.Button({
								text : "Apply",
								tooltip : "Apply this value to selected biomaterials",
								disabled : true,
								width : 100,
								handler : function() {
									// console.log("Apply");
									var factor = "factor" + this.factorCombo.getValue();
									var factorValue = "fv" + this.factorValueCombo.getValue();
									this.fireEvent("apply", factor, factorValue);
									this.saveButton.enable();
								},
								scope : this
							});

					this.items = [this.saveButton, ' ', this.revertButton, '-', "Bulk changes:", ' ', this.factorCombo,
							' ', this.factorValueCombo, this.applyButton];
				}

				var refreshButton = new Ext.Toolbar.Button({
							text : "Refresh",
							tooltip : "Reload the data",
							handler : function() {
								this.fireEvent("refresh");
							}.createDelegate(this)

						});

				var expandButton = new Ext.Toolbar.Button({
							text : "Expand/collapse all",
							tooltip : "Show/hide all biomaterial details",
							handler : function() {
								this.fireEvent("toggleExpand");
							}.createDelegate(this)
						});

				this.items.push('->');
				this.items.push(refreshButton);
				this.items.push(expandButton);

				Gemma.BioMaterialToolbar.superclass.initComponent.call(this);

				this.addEvents("revertSelected", "toggleExpand", "apply", "save", "refresh", "undo");
			},

			enableApplyOnSelect : function(model) {
				var selected = model.getSelections();
				if (selected.length > 0) {
					this.applyButton.enable();
				} else {
					this.applyButton.disable();
				}
			}
		});