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
Gemma.DiffExpressionExperimentGrid = Ext.extend(Ext.grid.GridPanel, {

	/*
	 * Do not set header : true here - it breaks it.
	 */
	autoExpandColumn : 'experimentalFactors',
	height : 200,
	layout : 'fit',
	viewConfig : {
		forceFit : true
	},
	autoScroll : true,

	record : Ext.data.Record.create([{
		name : "id",
		type : "int"
	}, {
		name : "expressionExperiment"
	}, {
		name : "probe"
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
			// paging.
			if (this.records) {
				Ext.apply(this, {
					store : new Ext.data.Store({
						proxy : new Ext.ux.data.PagingMemoryProxy(this.records),
						reader : new Ext.data.ListRangeReader({}, this.record),
						pageSize : this.pageSize,
						sortInfo : {
							field : "p",
							direction : "ASC"
						}
					})
				});
			} else {
				Ext.apply(this, {
					store : new Gemma.PagingDataStore({
						proxy : new Ext.data.DWRProxy(this.readMethod),
						reader : new Ext.data.ListRangeReader({
							id : "id"
						}, this.record),
						pageSize : this.pageSize,
						sortInfo : {
							field : "p",
							direction : "ASC"
						}
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
			// nonpaging
			if (!this.records) {
				// data read from server.
				Ext.apply(this, {
					store : new Ext.data.Store({
						proxy : new Ext.data.DWRProxy(this.readMethod),
						reader : new Ext.data.ListRangeReader({
							id : "id"
						}, this.record),
						sortInfo : {
							field : "p",
							direction : "ASC"
						}
					})
				});
			} else {
				// initialize with records.
				Ext.apply(this, {
					store : new Ext.data.Store({
						proxy : new Ext.data.MemoryProxy(this.records),
						reader : new Ext.data.ListRangeReader({}, this.record),
						sortInfo : {
							field : "p",
							direction : "ASC"
						}
					})
				});
			}
		}

		Ext.apply(this, {
			columns : [{
				id : 'expressionExperiment',
				header : "Dataset",
				dataIndex : "expressionExperiment",
				sortable : false,
				renderer : Gemma.DiffExpressionExperimentGrid.getEEStyler()
			}, {
				id : 'probe',
				header : "Probe",
				dataIndex : "probe",
				renderer : Gemma.DiffExpressionExperimentGrid.getProbeStyler(),
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
				sortable : true
			}, {
				id : 'p',
				header : "Sig. (FDR)",
				dataIndex : "p",
				renderer : function(p) {
					if (p < 0.001) {
						return sprintf("%.3e", p);
					} else {
						return sprintf("%.3f", p);
					}
				},
				sortable : true
			}]
		});

		/*
		 * Ext.apply(this, { rowExpander : new Gemma.DiffProbeGridRowExpander({
		 * tpl : "" }) }); this.columns.unshift(this.rowExpander);
		 * Ext.apply(this, { plugins : this.rowExpander });
		 */

		Gemma.DiffExpressionExperimentGrid.superclass.initComponent.call(this);

		// this.getStore().on("load", function() {
		// this.doLayout();
		// this.getView().refresh();
		// }, this);

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

Gemma.DiffExpressionExperimentGrid.getProbeStyler = function() {
	if (Gemma.DiffExpressionExperimentGrid.probeStyler === undefined) {
		Gemma.DiffExpressionExperimentGrid.probeStyler = function(value, metadata,
				record, row, col, ds) {
			
			var probe = record.data.probe;

			if (record.data.fisherContribution){
				return "<span style='color:#3A3'>"+probe+"</span>";
			}
			else{
				return "<span style='color:#808080'>"+probe+"</span>";
			}
		};
	}
	return Gemma.DiffExpressionExperimentGrid.probeStyler;
};			

Gemma.DiffExpressionExperimentGrid.getEFStyler = function() {
	if (Gemma.DiffExpressionExperimentGrid.efStyler === undefined) {
		Gemma.DiffExpressionExperimentGrid.efTemplate = new Ext.XTemplate(

		'<tpl for=".">',
				//"<a target='_blank' ext:qtip='{factorValues}'>{name}</a>\n",
				"<div ext:qtip='{factorValues}'>{name}</div>",	
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
