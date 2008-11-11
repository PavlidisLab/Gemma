Ext.namespace('Gemma');

/**
 * 
 * Grid to display expression experiments with differential evidence for each probe (e.g., for a single gene).
 * 
 * $Id$
 */
Gemma.ProbeLevelDiffExGrid = Ext.extend(Ext.grid.GridPanel, {

	autoExpandColumn : 'efs',
	height : 300,
	loadMask : true,
	stateful : false,
	viewConfig : {
// forceFit : true
	},

	readMethod : DifferentialExpressionSearchController.getDifferentialExpression,

	convertEE : function(s) {
		return s.shortName;
	},

	convertEF : function(s) {
		return s[0].name;
	},

	initComponent : function() {

		Ext.apply(this, {
					record : Ext.data.Record.create([{
								name : "expressionExperiment",
								sortType : this.convertEE
							}, {
								name : "probe"
							}, {
								name : "experimentalFactors",
								sortType : this.convertEF
							}, {
								name : "metThreshold",
								type : "boolean"
							}, {
								name : "fisherContribution",
								type : "boolean"
							}, {
								name : "p",
								type : "float"
							}])
				});

		if (this.pageSize) {
			Ext.apply(this, {
						store : new Gemma.PagingDataStore({
									proxy : new Ext.data.DWRProxy(this.readMethod),
									reader : new Ext.data.ListRangeReader({}, this.record),
									pageSize : this.pageSize,
									sortInfo : {
										field : "p",
										direction : "ASC"
									}
								})
					});
			Ext.apply(this, {
						bbar : new Gemma.PagingToolbar({
									pageSize : this.pageSize,
									store : this.store
								})
					});
		} else {
			// nonpaging
			Ext.apply(this, {
						store : new Ext.data.Store({
									proxy : new Ext.data.DWRProxy(this.readMethod),
									reader : new Ext.data.ListRangeReader({}, this.record),
									sortInfo : {
										field : "p",
										direction : "ASC"
									}
								})
					});
		}

		Ext.apply(this, {
			columns : [{
						id : 'expressionExperiment',
						header : "Dataset",
						dataIndex : "expressionExperiment",
						toolTip : "The study/expression experiment the result came from",
						sortable : true,
						renderer : Gemma.ProbeLevelDiffExGrid.getEEStyler()
					}, {
						id : 'expressionExperimentName',
						header : "Name",
						width : 100,
						dataIndex : "expressionExperimentName",
						toolTip : "The experiment name (abbreviated)",
						sortable : true,
						renderer : function(value, metadata, record, row, col, ds) {
							return Ext.util.Format.ellipsis(record.get('expressionExperiment').name, 20);
						}.createDelegate(this)
					}, {
						id : 'probe',
						header : "Probe",
						dataIndex : "probe",
						width : 80,
						toolTip : "The specific probe; shown in color if it was used for the Meta-P-value computation",
						renderer : Gemma.ProbeLevelDiffExGrid.getProbeStyler(),
						sortable : true
					}, {
						id : 'efs',
						header : "Factor",
						toolTip : "The factor that was examined",
						dataIndex : "experimentalFactors",
						renderer : Gemma.ProbeLevelDiffExGrid.getEFStyler(),
						sortable : true
					}, {
						id : 'p',
						header : "Sig. (q-value)",
						toolTip : "The significance measure of the result for the probe, shown in color if it met your threshold",
						dataIndex : "p",
						width : 80,
						renderer : function(p, metadata, record) {
							if (record.get("metThreshold")) {
								metadata.css = "metThreshold"; // typo.css
							}
							if (p < 0.001) {
								return sprintf("%.3e", p);
							} else {
								return sprintf("%.3f", p);
							}
						},
						sortable : true
					}]
		});

		Gemma.ProbeLevelDiffExGrid.superclass.initComponent.call(this);

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
	},

	metThresholdStyler : function(value, metadata, record, row, col, ds) {
		if (value) {
			return "&bull;";
		} else {
			return "";
		}
	}

});

/*
 * Gemma.ExpressionExperimentGrid.updateDatasetInfo = function(datasets, eeMap) { for (var i = 0; i < datasets.length;
 * ++i) { var ee = eeMap[datasets[i].id]; if (ee) { datasets[i].shortName = ee.shortName; datasets[i].name = ee.name; } } };
 */

/* stylers */
Gemma.ProbeLevelDiffExGrid.getEEStyler = function() {
	if (Gemma.ProbeLevelDiffExGrid.eeNameStyler === undefined) {
		Gemma.ProbeLevelDiffExGrid.eeNameTemplate = new Ext.Template("<tpl for='.'><a target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={id}' ext:qtip='{name}'>{shortName}</a></tpl>");
		Gemma.ProbeLevelDiffExGrid.eeNameStyler = function(value, metadata, record, row, col, ds) {
			var ee = record.data.expressionExperiment;
			return Gemma.ProbeLevelDiffExGrid.eeNameTemplate.apply(ee);
		};
	}
	return Gemma.ProbeLevelDiffExGrid.eeNameStyler;
};

Gemma.ProbeLevelDiffExGrid.getEENameStyler = function() {
	if (Gemma.ProbeLevelDiffExGrid.eeStyler === undefined) {
		Gemma.ProbeLevelDiffExGrid.eeTemplate = new Ext.Template("{name}");
		Gemma.ProbeLevelDiffExGrid.eeStyler = function(value, metadata, record, row, col, ds) {
			var ee = record.data.expressionExperiment;
			return Gemma.ProbeLevelDiffExGrid.eeTemplate.apply(ee);
		};
	}
	return Gemma.ProbeLevelDiffExGrid.eeStyler;
};

Gemma.ProbeLevelDiffExGrid.getProbeStyler = function() {
	if (Gemma.ProbeLevelDiffExGrid.probeStyler === undefined) {
		Gemma.ProbeLevelDiffExGrid.probeStyler = function(value, metadata, record, row, col, ds) {

			var probe = record.data.probe;

			if (record.data.fisherContribution) {
				return "<span style='color:#3A3'>" + probe + "</span>";
			} else {
				return "<span style='color:#808080'>" + probe + "</span>";
			}
		};
	}
	return Gemma.ProbeLevelDiffExGrid.probeStyler;
};

Gemma.ProbeLevelDiffExGrid.getEFStyler = function() {
	if (Gemma.ProbeLevelDiffExGrid.efStyler === undefined) {
		Gemma.ProbeLevelDiffExGrid.efTemplate = new Ext.XTemplate(

				'<tpl for=".">',
				// "<a target='_blank' ext:qtip='{factorValues}'>{name}</a>\n",
				"<div ext:qtip='{factorValues}'>{name}</div>", '</tpl>'

		);
		Gemma.ProbeLevelDiffExGrid.efStyler = function(value, metadata, record, row, col, ds) {
			var efs = record.data.experimentalFactors;
			return Gemma.ProbeLevelDiffExGrid.efTemplate.apply(efs);
		};
	}
	return Gemma.ProbeLevelDiffExGrid.efStyler;
};
