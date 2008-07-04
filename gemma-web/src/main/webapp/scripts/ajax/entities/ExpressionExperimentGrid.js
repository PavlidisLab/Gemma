/*
 */
Ext.namespace('Gemma');

/**
 * 
 * Grid to display ExpressionExperiments. Author: Paul (based on Luke's
 * CoexpressionDatasetGrid) $Id: ExpressionExperimentGrid.js,v 1.13 2008/04/23
 * 19:54:46 kelsey Exp $
 */
Gemma.ExpressionExperimentGrid = Ext.extend(Gemma.GemmaGridPanel, {

	/*
	 * Do not set header : true here - it breaks it.
	 */
	collapsible : false,
	readMethod : ExpressionExperimentController.loadExpressionExperiments
			.createDelegate(this, [], true),

	autoExpandColumn : 'name',

	editable : true,

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
				id : 'shortName',
				header : "Dataset",
				dataIndex : "shortName",
				renderer : this.formatEE,
		//		width : 80,
				sortable : true
			}, {
				id : 'name',
				header : "Name",
				dataIndex : "name",
			//	width : 120,
				sortable : true
			}, {
				id : 'arrays',
				header : "Arrays",
				dataIndex : "arrayDesignCount",
			//	width : 50,
				sortable : true
			}, {
				id : 'assays',
				header : "Assays",
				dataIndex : "bioAssayCount",
				renderer : this.formatAssayCount,
			//	width : 50,
				sortable : true
			}]
		});

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

		this.getStore().on("load", function() {
			this.doLayout();
		}, this);

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

	formatAssayCount : function(value, metadata, record, row, col, ds) {
		return record.get("bioAssayCount");
//		return String
//				.format(
//						"{0}&nbsp;<a target='_blank' href='/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id={1}'><img src='/Gemma/images/magnifier.png' height='10' width='10'/></a>",
//						record.data.bioAssayCount, record.data.id);
	},

	formatEE : function(value, metadata, record, row, col, ds) {
		var eeTemplate = new Ext.Template("<a target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={id}' ext:qtip='{name}'>{shortName}</a>");
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

Gemma.ExpressionExperimentGrid.updateDatasetInfo = function(datasets, eeMap) {
	for (var i = 0; i < datasets.length; ++i) {
		var ee = eeMap[datasets[i].id];
		if (ee) {
			datasets[i].shortName = ee.shortName;
			datasets[i].name = ee.name;
		}
	}
};

Gemma.EEGridRowExpander = Ext.extend(Ext.grid.RowExpander, {

	fillExpander : function(data, body, rowIndex) {
		Ext.DomHelper.overwrite(body, {
			tag : 'p',
			html : data
		});
	},

	beforeExpand : function(record, body, rowIndex) {
		ExpressionExperimentController.getDescription(record.id, {
			callback : this.fillExpander.createDelegate(this, [body, rowIndex],
					true)
		});
		return true;
	}

});
