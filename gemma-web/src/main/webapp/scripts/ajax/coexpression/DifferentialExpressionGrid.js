Ext.namespace('Gemma');

/*
 * Gemma.DifferentialExpressionGrid constructor... config is a hash with the
 * following options:
 */
Gemma.DifferentialExpressionGrid = function(config) {

	this.pageSize = config.pageSize;
	delete config.pageSize;
	this.geneId = config.geneId;
	delete config.geneId;
	this.threshold = config.threshold;
	delete config.threshold;

	/*
	 * keep a reference to ourselves to avoid convoluted scope issues below...
	 */
	var thisGrid = this;

	/*
	 * establish default config options...
	 */
	var superConfig = {
		collapsible : true,
		editable : false,
		viewConfig : {
			emptyText : "no differential expression results available"
		}
	};

	if (this.pageSize) {
		superConfig.ds = new Gemma.PagingDataStore({
			proxy : new Ext.data.DWRProxy(DifferentialExpressionSearchController.getDifferentialExpression),
			reader : new Ext.data.ListRangeReader({
				id : "id"
			}, Gemma.DifferentialExpressionGrid.getRecord()),
			pageSize : this.pageSize
		});
		superConfig.bbar = new Gemma.PagingToolbar({
			pageSize : this.pageSize,
			store : superConfig.ds
		});
	} else {
		superConfig.ds = new Ext.data.Store({
			proxy : new Ext.data.DWRProxy(DifferentialExpressionSearchController.getDifferentialExpression),
			reader : new Ext.data.ListRangeReader({
				id : "id"
			}, Gemma.DifferentialExpressionGrid.getRecord())
		});
	}
	superConfig.ds.setDefaultSort('p');
	superConfig.ds.load({
		params : [this.geneId, this.threshold]
	});

	superConfig.cm = new Ext.grid.ColumnModel([{
		id : 'ee',
		header : "Dataset",
		dataIndex : "expressionExperiment",
		renderer : Gemma.DifferentialExpressionGrid.getEEStyler(),
		width : 80
	}, {
		id : 'name',
		header : "Name",
		dataIndex : "expressionExperiment",
		renderer : Gemma.DifferentialExpressionGrid.getEENameStyler(),
		width : 120
	}, {
		id : 'probe',
		header : "Probe",
		dataIndex : "probe"
	}, {
		id : 'efs',
		header : "Factor(s)",
		dataIndex : "experimentalFactors",
		renderer : Gemma.DifferentialExpressionGrid.getEFStyler(),
		sortable : false
	}, {
		id : 'p',
		header : "Sig. (FDR)",
		dataIndex : "p",
		renderer : function(p) {
			return p.toFixed(6);
		}
	}]);
	superConfig.cm.defaultSortable = true;

	superConfig.autoExpandColumn = 'name';

	for (property in config) {
		superConfig[property] = config[property];
	}
	Gemma.DifferentialExpressionGrid.superclass.constructor.call(this,
			superConfig);

	this.getStore().on("load", function() {
		this.doLayout();
	}, this);

};

/*
 * static methods
 */
Gemma.DifferentialExpressionGrid.getRecord = function() {
	if (Gemma.DifferentialExpressionGrid.record === undefined) {
		Gemma.DifferentialExpressionGrid.record = Ext.data.Record.create([{
			name : "expressionExperiment",
			sortType : function(ee) {
				return ee.shortName;
			}
		}, {
			name : "probe",
			type : "string"
		}, {
			name : "experimentalFactors"
		}, {
			name : "p",
			type : "float"
		}]);
	}
	return Gemma.DifferentialExpressionGrid.record;
};

Gemma.DifferentialExpressionGrid.getEEStyler = function() {
	if (Gemma.DifferentialExpressionGrid.eeNameStyler === undefined) {
		Gemma.DifferentialExpressionGrid.eeNameTemplate = new Ext.Template("<a target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={id}' ext:qtip='{name}'>{shortName}</a>");
		Gemma.DifferentialExpressionGrid.eeNameStyler = function(value,
				metadata, record, row, col, ds) {
			var ee = record.data.expressionExperiment;
			return Gemma.DifferentialExpressionGrid.eeNameTemplate.apply(ee);
		};
	}
	return Gemma.DifferentialExpressionGrid.eeNameStyler;
};

Gemma.DifferentialExpressionGrid.getEENameStyler = function() {
	if (Gemma.DifferentialExpressionGrid.eeStyler === undefined) {
		Gemma.DifferentialExpressionGrid.eeTemplate = new Ext.Template("{name}");
		Gemma.DifferentialExpressionGrid.eeStyler = function(value, metadata,
				record, row, col, ds) {
			var ee = record.data.expressionExperiment;
			return Gemma.DifferentialExpressionGrid.eeTemplate.apply(ee);
		};
	}
	return Gemma.DifferentialExpressionGrid.eeStyler;
};

Gemma.DifferentialExpressionGrid.getEFStyler = function() {
	if (Gemma.DifferentialExpressionGrid.efStyler === undefined) {
		// Gemma.DifferentialExpressionGrid.efTemplate = new Ext.XTemplate(
		// "<tpl for='.'>",
		// "{name} ({category})",
		// "</tpl>"
		// );
		Gemma.DifferentialExpressionGrid.efStyler = function(value, metadata,
				record, row, col, ds) {
			var efs = record.data.experimentalFactors;
			var names = [];
			for (var i = 0; i < efs.length; ++i) {
				names.push(efs[i].name || "unnamed factor");
			}
			return names.join(",");
				// return Gemma.DifferentialExpressionGrid.efTemplate.apply( ef
				// );
		};
	}
	return Gemma.DifferentialExpressionGrid.efStyler;
};

/*
 * instance methods...
 */
Ext.extend(Gemma.DifferentialExpressionGrid, Gemma.GemmaGridPanel, {

});