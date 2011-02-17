/*
 */
Ext.namespace('Gemma');

/**
 * 
 * Simple text data view to display a defined number of ExpressionExperiments. Must be added to a container. 
 * Author: Thea (based on Pauls's ExpressionExperimentGrid.js)
 */

Gemma.ExpressionExperimentDataView = Ext.extend(Ext.DataView, {
	emptyText:'Preview of selected experiments',
	readMethod : ExpressionExperimentController.loadStatusSummaries.createDelegate(this, [], true),

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

	initComponent : function() {
		
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
		
		Ext.apply(this, {tpl: new Ext.Template('<div><b>{shortName}</b><br>{name}</div>')})


		Gemma.ExpressionExperimentDataView.superclass.initComponent.call(this);

		this.on("keypress", function(e) {
					if (e.getCharCode() == Ext.EventObject.DELETE) {
						this.removeSelected();
					}
				}, this);

		this.getStore().on("load", function(store, records, options) {
					this.ownerCt.doLayout.createDelegate(this);
				}, this);

		if (this.eeids) {
			this.getStore().load({
						params : [this.eeids]
					});
		}

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
		width : 0.4,
		sortable : true
	}, {
		id : 'arrays',
		header : "Arrays",
		dataIndex : "arrayDesignCount",
		hidden : true,
		tooltip : "The number of different types of array platforms used",
		width : 0.1,
		sortable : true
	}, {
		id : 'assays',
		header : "Assays",
		dataIndex : "bioAssayCount",
		tooltip : "The number of arrays (~samples) present in the study",
		width : 0.1,
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
Gemma.ExpressionExperimentDataView.updateDatasetInfo = function(datasets, eeMap) {
	for (var i = 0; i < datasets.length; ++i) {
		var ee = eeMap[datasets[i].id];
		if (ee) {
			datasets[i].shortName = ee.shortName;
			datasets[i].name = ee.name;
		}
	}
};


Gemma.ExpressionExperimentDataViewPanel = Ext.extend(Ext.Panel,{
	height:200,
	width:140,
	id:'experimentPreview',
	html:'experiment panel',
	initComponent: function(){
		Gemma.ExpressionExperimentDataViewPanel.superclass.initComponent();
		this.dataView = new Gemma.ExpressionExperimentDataView();
		this.dataView.getStore.load();
		this.add(this.dataView);
	}
});
