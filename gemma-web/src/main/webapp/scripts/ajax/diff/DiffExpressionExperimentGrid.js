/*
 */
Ext.namespace('Gemma');

/**
 * 
 * Grid to display expression experiments with differential evidence for given
 * probe.
 * 
 * $Id$
 */
Gemma.DiffExpressionExperimentGrid = Ext.extend(Gemma.GemmaGridPanel, {

	/*
	 * Do not set header : true here - it breaks it.
	 */
	collapsible : false,
	// readMethod : ExpressionExperimentController.loadExpressionExperiments
	// .createDelegate(this, [], true),

	autoExpandColumn : 'probe',

	editable : true,

	record : Ext.data.Record.create([{
		name : "id",
		type : "int"
	}, {
		name : "expressionExperiment"
	}, {
		name : "probe",
		type : "string"
	}, {
		name : "experimentalFactors"
	}, {
		name : "metThreshold",
		type : "boolean"
	}, {
		name : "p",
		type : "float"
	}]),

	initComponent : function() {
		if (this.pageSize) {
			if (this.records) {
				Ext.apply(this, {
					store : new Ext.data.Store({
						proxy : new Ext.ux.data.PagingMemoryProxy(this.records),
						reader : new Ext.data.ListRangeReader({}, this.record),
						pageSize : this.pageSize
					})
				});
			} else {
				Ext.apply(this, {
					store : new Gemma.PagingDataStore({
						proxy : new Ext.data.DWRProxy(this.readMethod),
						reader : new Ext.data.ListRangeReader({
							id : "id"
						}, this.record),
						pageSize : this.pageSize
					})
				});
			}
			Ext.apply(this, {
				bbar : new Gemma.PagingToolbar({
					pageSize : this.pageSize,
					store : this.store
				})
			});
		} else {
			if (!this.records) {
				Ext.apply(this, {
					store : new Ext.data.Store({
						proxy : new Ext.data.DWRProxy(this.readMethod),
						reader : new Ext.data.ListRangeReader({
							id : "id"
						}, this.record)
					})
				});
			} else {
				Ext.apply(this, {
					store : new Ext.data.Store({
						proxy : new Ext.data.MemoryProxy(this.records),
						reader : new Ext.data.ListRangeReader({}, this.record)
					})
				});
			}
		}

		Ext.apply(this, {
			columns : [{
				id : 'expressionExperiment',
				header : "Dataset",
				dataIndex : "expressionExperiment",
				width : 80,
				sortable : false,
				renderer : Gemma.DiffExpressionExperimentGrid.getEEStyler()
			}, {
				id : 'probe',
				header : "Probe",
				dataIndex : "probe",
				width : 80,
				sortable : false
			}, {
				id : 'efs',
				header : "Factor(s)",
				dataIndex : "experimentalFactors",
				renderer : Gemma.DiffExpressionExperimentGrid.getEFStyler(),
				sortable : false
			}, {
				id : 'metThreshold',
				header : "Met Threshold",
				dataIndex : "metThreshold",
				width : 80,
				sortable : true
			}, {
				id : 'p',
				header : "P Value",
				dataIndex : "p",
				width : 120,
				sortable : true
			}]
		});

		/*
		 * Ext.apply(this, { rowExpander : new Gemma.DiffProbeGridRowExpander({
		 * tpl : "" }) }); this.columns.unshift(this.rowExpander);
		 * Ext.apply(this, { plugins : this.rowExpander });
		 */

		Gemma.DiffExpressionExperimentGrid.superclass.initComponent.call(this);

		this.on("keypress", function(e) {
			if (e.getCharCode() == Ext.EventObject.DELETE) {
				this.removeSelected();
			}
		}, this);

		this.getStore().on("load", function() {
			this.doLayout();
		}, this);

	},

	removeSelected : function() {
		var recs = this.getSelectionModel().getSelections();
		for (var x = 0; x < recs.length; x++) { // for r in recs
			// does
			// not
			// work!
			this.getStore().remove(recs[x]);
			this.getView().refresh();
		}
	},

	formatAssayCount : function(value, metadata, record, row, col, ds) {
		return String
				.format(
						"{0}&nbsp;<a href='/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id={1}'><img src='/Gemma/images/magnifier.png' height='10' width='10'/></a>",
						record.data.bioAssayCount, record.data.id);
	},

	formatEE : function(value, metadata, record, row, col, ds) {
		var eeTemplate = new Ext.Template("<a target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={id}' ext:qtip='{name}'>{shortName}</a>");
		return eeTemplate.apply(record.data);
	},

	getEEIds : function() {
		var result = [];
		this.store.each(function(rec) {
			result.push(rec.get("id"));
		});
		return result;
	},

	isEditable : function() {
		return this.editable;
	},

	setEditable : function(b) {
		this.editable = b;
	}

});

/*
 * Gemma.ExpressionExperimentGrid.updateDatasetInfo = function(datasets, eeMap) {
 * for (var i = 0; i < datasets.length; ++i) { var ee = eeMap[datasets[i].id];
 * if (ee) { datasets[i].shortName = ee.shortName; datasets[i].name = ee.name; } } };
 */

/* stylers */
Gemma.DiffExpressionExperimentGrid.getEEStyler = function() {
	if (Gemma.DiffExpressionExperimentGrid.eeNameStyler === undefined) {
		Gemma.DiffExpressionExperimentGrid.eeNameTemplate = new Ext.Template("<a target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={id}' ext:qtip='{name}'>{shortName}</a>");
		Gemma.DiffExpressionExperimentGrid.eeNameStyler = function(value,
				metadata, record, row, col, ds) {
			var ee = record.data.expressionExperiment;
			return Gemma.DiffExpressionExperimentGrid.eeNameTemplate.apply(ee);
		};
	}
	return Gemma.DiffExpressionExperimentGrid.eeNameStyler;
};

Gemma.DiffExpressionExperimentGrid.getEENameStyler = function() {
	if (Gemma.DiffExpressionExperimentGrid.eeStyler === undefined) {
		Gemma.DiffExpressionExperimentGrid.eeTemplate = new Ext.Template("{name}");
		Gemma.DiffExpressionExperimentGrid.eeStyler = function(value, metadata,
				record, row, col, ds) {
			var ee = record.data.expressionExperiment;
			return Gemma.DiffExpressionExperimentGrid.eeTemplate.apply(ee);
		};
	}
	return Gemma.DiffExpressionExperimentGrid.eeStyler;
};

Gemma.DiffExpressionExperimentGrid.getEFStyler = function() {
	if (Gemma.DiffExpressionExperimentGrid.efStyler === undefined) {
		Gemma.DiffExpressionExperimentGrid.efTemplate = new Ext.XTemplate(

		'<tpl for=".">',
				"<a target='_blank' ext:qtip='{factorValues}'>{name}</a>\n",
				// '<p><tooltip caption={factorValues} descr="factor
				// values">{name}</tooltip></p>',
				'</tpl>'

		);
		Gemma.DiffExpressionExperimentGrid.efStyler = function(value, metadata,
				record, row, col, ds) {
			var efs = record.data.experimentalFactors;
			return Gemma.DiffExpressionExperimentGrid.efTemplate.apply(efs);
		};
	}
	return Gemma.DiffExpressionExperimentGrid.efStyler;
};

/*
 * Gemma.DiffProbeGridRowExpander = Ext.extend(Ext.grid.RowExpander, {
 * 
 * fillExpander : function(data, body, rowIndex) { Ext.DomHelper.overwrite(body, {
 * tag : 'p', html : data }); },
 * 
 * beforeExpand : function(record, body, rowIndex) {
 * ExpressionExperimentController.getDescription(record.id, { callback :
 * this.fillExpander.createDelegate(this, [body, rowIndex], true) }); return
 * true; }
 * 
 * });
 */