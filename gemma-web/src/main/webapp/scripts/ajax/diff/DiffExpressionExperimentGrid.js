/*
 */
Ext.namespace('Ext.Gemma');

/**
 * 
 * Grid to display expression experiments with differential evidence for given probe. 
 *  
 * $Id$
 */
Ext.Gemma.DiffExpressionExperimentGrid = Ext.extend(Ext.Gemma.GemmaGridPanel, {

	/*
	 * Do not set header : true here - it breaks it.
	 */
	collapsible : false,
//	readMethod : ExpressionExperimentController.loadExpressionExperiments
//			.createDelegate(this, [], true),

	autoExpandColumn : 'probe',

	editable : true,

	record : Ext.data.Record.create([
			{ name:"id", type:"int"},
			{ name:"expressionExperiment"},
			{ name:"probe", type:"string" },
			{ name:"experimentalFactors" },
			{ name:"p", type:"float" }]),

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
					store : new Ext.Gemma.PagingDataStore({
						proxy : new Ext.data.DWRProxy(this.readMethod),
						reader : new Ext.data.ListRangeReader({
							id : "id"
						}, this.record),
						pageSize : this.pageSize
					})
				});
			}
			Ext.apply(this, {
				bbar : new Ext.Gemma.PagingToolbar({
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
				renderer :Ext.Gemma.DiffExpressionExperimentGrid.getEEStyler()
			}, {
				id : 'probe',
				header : "Probe",
				dataIndex : "probe",
				width : 80,
				sortable : false
			}, { id: 'efs', 
				 header: "Factor(s)", 
				 dataIndex: "experimentalFactors", 
				 renderer: Ext.Gemma.DiffExpressionExperimentGrid.getEFStyler(), 
				 sortable: false 
			}, {
				 id : 'p',
				 header : "P Value",
				 dataIndex : "p",
				 width : 120,
				 sortable : true
			}]
		});
		
		/*
		Ext.apply(this, {
			rowExpander : new Ext.Gemma.DiffProbeGridRowExpander({
				tpl : ""
			})
		});
		this.columns.unshift(this.rowExpander);
		Ext.apply(this, {
			plugins : this.rowExpander
		});
		*/
	
		Ext.Gemma.DiffExpressionExperimentGrid.superclass.initComponent.call(this);

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
Ext.Gemma.ExpressionExperimentGrid.updateDatasetInfo = function(datasets, eeMap) {
	for (var i = 0; i < datasets.length; ++i) {
		var ee = eeMap[datasets[i].id];
		if (ee) {
			datasets[i].shortName = ee.shortName;
			datasets[i].name = ee.name;
		}
	}
};
*/

/* stylers */
Ext.Gemma.DiffExpressionExperimentGrid.getEEStyler = function() {
	if ( Ext.Gemma.DiffExpressionExperimentGrid.eeNameStyler === undefined ) {
		Ext.Gemma.DiffExpressionExperimentGrid.eeNameTemplate = new Ext.Template(
			"<a target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={id}' ext:qtip='{name}'>{shortName}</a>"
		);
		Ext.Gemma.DiffExpressionExperimentGrid.eeNameStyler = function ( value, metadata, record, row, col, ds ) {
			var ee = record.data.expressionExperiment;
			return Ext.Gemma.DiffExpressionExperimentGrid.eeNameTemplate.apply( ee );
		};
	}
	return Ext.Gemma.DiffExpressionExperimentGrid.eeNameStyler;
};

Ext.Gemma.DiffExpressionExperimentGrid.getEENameStyler = function() {
	if ( Ext.Gemma.DiffExpressionExperimentGrid.eeStyler === undefined ) {
		Ext.Gemma.DiffExpressionExperimentGrid.eeTemplate = new Ext.Template(
			"{name}"
		);
		Ext.Gemma.DiffExpressionExperimentGrid.eeStyler = function ( value, metadata, record, row, col, ds ) {
			var ee = record.data.expressionExperiment;
			return Ext.Gemma.DiffExpressionExperimentGrid.eeTemplate.apply( ee );
		};
	}
	return Ext.Gemma.DiffExpressionExperimentGrid.eeStyler;
};

Ext.Gemma.DiffExpressionExperimentGrid.getEFStyler = function() {
	if ( Ext.Gemma.DiffExpressionExperimentGrid.efStyler === undefined ) {
		Ext.Gemma.DiffExpressionExperimentGrid.efTemplate = new Ext.XTemplate(
			
			'<tpl for=".">',
				"<a target='_blank' ext:qtip='{factorValues}'>{name}</a>\n",
				//'<p><tooltip caption={factorValues} descr="factor values">{name}</tooltip></p>',
			'</tpl>'
				
		);
		Ext.Gemma.DiffExpressionExperimentGrid.efStyler = function ( value, metadata, record, row, col, ds ) {
			var efs = record.data.experimentalFactors;
			return Ext.Gemma.DiffExpressionExperimentGrid.efTemplate.apply( efs);
		};
	}
	return Ext.Gemma.DiffExpressionExperimentGrid.efStyler;
};

/*
Ext.Gemma.DiffProbeGridRowExpander = Ext.extend(Ext.grid.RowExpander, {

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
*/