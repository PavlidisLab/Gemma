/*
 */
Ext.namespace('Gemma');

/**
 * 
 * Grid to display ExpressionExperiments. Author: Paul (based on Luke's CoexpressionDatasetGrid) $Id:
 * ExpressionExperimentGrid.js,v 1.13 2008/04/23 19:54:46 kelsey Exp $
 */
Gemma.ExpressionExperimentGrid = Ext.extend(Gemma.GemmaGridPanel, {

	/*
	 * Do not set header : true here - it breaks it.
	 */
	collapsible : false,
	readMethod : ExpressionExperimentController.loadExpressionExperiments.createDelegate(this, [], true),

	autoExpandColumn : 'name',

	editable : true,
	stateful : false,

	record : Ext.data.Record.create([{
				name : "id",
				type : "int"
			}, {
				name : "shortName",
				type : "string"
			}, {
				name : "name",
				type : "string"
			}, {
				name : "arrayDesignCount",
				type : "int"
			}, {
				name : "bioAssayCount",
				type : "int"
			}, {
				name : "externalUri",
				type : "string"
			}, {
				name : "description",
				type : "string"
			}, {
				name : "differentialExpressionAnalysisId",
				type : "string"
			}, {
				name : 'taxonId',
				type : 'int'
			}]),

	searchForText : function(button, keyev) {
		var text = this.searchInGridField.getValue();
		if (text.length < 2) {
			this.clearFilter();
			return;
		}
		this.getStore().filterBy(this.getSearchFun(text), this, 0);
	},

	clearFilter : function() {
		this.getStore().clearFilter();
	},

	getSearchFun : function(text) {
		var value = new RegExp(Ext.escapeRe(text), 'i');
		return function(r, id) {
			var obj = r.data;
			return value.match(obj.name) || value.match(obj.shortName);
		}
	},

	initComponent : function() {
		this.searchInGridField = new Ext.form.TextField({
					enableKeyEvents : true,
					emptyText : 'Filter',
					tooltip : "Text typed here will ",
					listeners : {
						"keyup" : {
							fn : this.searchForText.createDelegate(this),
							scope : this,
							options : {
								delay : 100
							}
						}
					}
				});

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
		Ext.apply(this, {
					bbar : new Ext.Toolbar({
								items : ['->', {
											xtype : 'button',
											handler : this.clearFilter.createDelegate(this),
											scope : this,
											cls : 'x-btn-text',
											text : 'Reset filter'
										}, ' ', this.searchInGridField]
							})
				});

		Ext.apply(this, {
			columns : [{
				id : 'shortName',
				header : "Dataset",
				dataIndex : "shortName",
				tooltip : "The unique short name for the dataset, often the accession number from the originating source database. Click on the name to view the details page.",
				renderer : this.formatEE,
				// width : 80,
				sortable : true
			}, {
				id : 'name',
				header : "Name",
				dataIndex : "name",
				tooltip : "The descriptive name of the dataset, usually supplied by the submitter",
				// width : 120,
				sortable : true
			}, {
				id : 'arrays',
				header : "Arrays",
				dataIndex : "arrayDesignCount",
				hidden : true,
				tooltip : "The number of different types of array platforms used",
				// width : 50,
				sortable : true
			}, {
				id : 'assays',
				header : "Assays",
				dataIndex : "bioAssayCount",
				renderer : this.formatAssayCount,
				tooltip : "The number of arrays (~samples) present in the study",
				// width : 50,
				sortable : true
			}]
		});

		if (this.showAnalysisInfo) {
			this.columns.push({
						id : 'analyses',
						header : "Diff.An.",
						dataIndex : "differentialExpressionAnalysisId",
						tooltip : "Indicates whether differential expression data is available for the study",
						renderer : this.formatAnalysisInfo,
						sortable : true
					});
		}

		if (this.rowExpander) {
			Ext.apply(this, {
						rowExpander : new Gemma.EEGridRowExpander({
									tpl : ""
								})
					});
			this.columns.unshift(this.rowExpander);
			Ext.apply(this, {
						plugins : this.rowExpander
					});
		}

		Gemma.ExpressionExperimentGrid.superclass.initComponent.call(this);

		this.on("keypress", function(e) {
					if (e.getCharCode() == Ext.EventObject.DELETE) {
						this.removeSelected();
					}
				}, this);

		this.getStore().on("load", function(store, records, options) {
					this.doLayout.createDelegate(this);
				}, this);

		if (this.eeids) {
			this.getStore().load({
						params : [this.eeids]
					});
		}

	},

	afterRender : function() {
		Gemma.ExpressionExperimentGrid.superclass.afterRender.call(this);
		if (this.getTopToolbar()) {
			this.getTopToolbar().grid = this;
		}
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

	formatAnalysisInfo : function(value, metadata, record, row, col, ds) {
		var id = record.get("differentialExpressionAnalysisId");
		if (id) {
			return "<img src='/Gemma/images/icons/ok.png' height='16' width='16' ext:qtip='Has differential expression analysis' />";
		} else {
			return "";
		}
	},

	formatAssayCount : function(value, metadata, record, row, col, ds) {
		return record.get("bioAssayCount");
		// return String
		// .format(
		// "{0}&nbsp;<a target='_blank'
		// href='/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id={1}'><img
		// src='/Gemma/images/magnifier.png' height='10' width='10'/></a>",
		// record.data.bioAssayCount, record.data.id);
	},

	formatEE : function(value, metadata, record, row, col, ds) {
		// fixme: this is duplicated code.
		var eeTemplate = new Ext.XTemplate(
				'<tpl for="."><a target="_blank" title="{name}" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=',
				'{[values.sourceExperiment ? values.sourceExperiment : values.id]}"',
				' ext:qtip="{name}">{shortName}</a></tpl>');
		return eeTemplate.apply(record.data);
	},

	/**
	 * Return all the ids of the experiments shown in this grid.
	 */
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

Gemma.ExpressionExperimentListView = Ext.extend(Ext.list.ListView, {
	columns : [{
		id : 'shortName',
		header : "Dataset",
		dataIndex : "shortName",
		tooltip : "The unique short name for the dataset, often the accession number from the originating source database. Click on the name to view the details page.",
		renderer : this.formatEE,
		width : 0.2,
		sortable : true
	}, {
		id : 'name',
		header : "Name",
		dataIndex : "name",
		tooltip : "The descriptive name of the dataset, usually supplied by the submitter",
		width : 0.2,
		sortable : true
	}, {
		id : 'arrays',
		header : "Arrays",
		dataIndex : "arrayDesignCount",
		hidden : true,
		tooltip : "The number of different types of array platforms used",
		width : 0.2,
		sortable : true
	}, {
		id : 'assays',
		header : "Assays",
		dataIndex : "bioAssayCount",
		tooltip : "The number of arrays (~samples) present in the study",
		width : 0.2,
		sortable : true
	}],
	store : new Ext.data.Store({
				proxy : new Ext.data.DWRProxy({
							apiActionToHandlerMap : {
								read : {
									dwrFunction : ExpressionExperimentController.loadExpressionExperiments
								}
							}
						}),
				reader : new Ext.data.ListRangeReader({
							id : "id",
							fields : [{
										name : "id",
										type : "int"
									}, {
										name : "shortName",
										type : "string"
									}, {
										name : "name",
										type : "string"
									}, {
										name : "arrayDesignCount",
										type : "int"
									}, {
										name : "bioAssayCount",
										type : "int"
									}, {
										name : "externalUri",
										type : "string"
									}, {
										name : "description",
										type : "string"
									}, {
										name : "differentialExpressionAnalysisId",
										type : "string"
									}, {
										name : 'taxonId',
										type : 'int'
									}]
						})
			})
});

/**
 * 
 * @param {}
 *            datasets
 * @param {}
 *            eeMap
 */
Gemma.ExpressionExperimentGrid.updateDatasetInfo = function(datasets, eeMap) {
	for (var i = 0; i < datasets.length; ++i) {
		var ee = eeMap[datasets[i].id];
		if (ee) {
			datasets[i].shortName = ee.shortName;
			datasets[i].name = ee.name;
		}
	}
};

/**
 * 
 * @class Gemma.EEGridRowExpander
 * @extends Ext.grid.RowExpander
 */
Gemma.EEGridRowExpander = Ext.extend(Ext.grid.RowExpander, {

			fillExpander : function(data, body, rowIndex) {
				Ext.DomHelper.overwrite(body, {
							tag : 'p',
							html : data
						});
			},

			beforeExpand : function(record, body, rowIndex) {
				ExpressionExperimentController.getDescription(record.id, {
							callback : this.fillExpander.createDelegate(this, [body, rowIndex], true)
						});
				return true;
			}

		});
