Ext.namespace('Gemma');

/*
 * instance methods...
 */
Gemma.CoexpressionDatasetGrid = Ext.extend(Gemma.GemmaGridPanel, {

	collapsible : true,
	collapsed : true,
	hidden : true,
	title : 'wow',
	style : "margin-top: 1em; margin-bottom: .5em;",
	autoHeight : true,
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
				name : "rawCoexpressionLinkCount",
				type : "int"
			}, {
				name : "coexpressionLinkCount",
				type : "int"
			}, {
				name : "hasProbeSpecificForQueryGene"
			}, {
				name : "arrayDesignCount",
				type : "int"
			}, {
				name : "externalUri",
				type : "string"
			}, {
				name : "bioAssayCount",
				type : "int"
			}, {
				name : "queryGene",
				type : "string"
			}]),

	loadData : function(d) {
		var datasets = {}, numDatasets = 0;
		for (var i = 0; i < d.length; ++i) {
			if (!datasets[d[i].id]) {
				datasets[d[i].id] = 1;
				++numDatasets;
			}
		}
		var title = String.format("{0} dataset{1} relevant{2} coexpression data", numDatasets, numDatasets == 1
						? " has"
						: "s have", this.adjective ? " " + this.adjective : "");

		this.setTitle(title);
		this.show();
		this.getStore().proxy.data = d;
		this.getStore().reload();
	},

	initComponent : function() {

		Ext.apply(this, {
					store : new Ext.data.GroupingStore({
								proxy : new Ext.data.MemoryProxy([]),
								reader : new Ext.data.ListRangeReader({}, this.record),
								groupField : 'queryGene',
								sortInfo : {
									field : 'coexpressionLinkCount',
									dir : 'DESC'
								}
							}),

					view : new Ext.grid.GroupingView({
								hideGroupedColumn : true
							}),

					columns : [{
						id : 'shortName',
						header : "Dataset",
						dataIndex : "shortName",
						tooltip : "Dataset common name"
							// renderer : this.eeStyler.createDelegate(this)
						}, {
						id : 'name',
						header : "Name",
						dataIndex : "name",
						tooltip : "Dataset Long Name"
					}, {
						id : 'queryGene',
						header : "Query Gene",
						dataIndex : "queryGene",
						hidden : true
					}, {
						header : "Raw Links",
						dataIndex : "rawCoexpressionLinkCount",
						tooltip : "# of possible links for the query gene in this data set",
						hidden : true
					}, {
						header : "Contributing Links",
						dataIndex : "coexpressionLinkCount",
						tooltip : "# contributions to confirmed links"
					}, {
						header : "Specific Probe",
						dataIndex : "hasProbeSpecificForQueryGene",
						type : "boolean",
						tooltip : "Does the dataset have a probe that is specific for the query gene?",
						renderer : this.booleanStyler.createDelegate(this)
					},
							// , {
							// id : 'arrays',
							// header : "Arrays",
							// dataIndex : "arrayDesignCount",
							// tooltip : "# of Array Designs"
							// },
							{
								id : 'assays',
								header : "Assays",
								dataIndex : "bioAssayCount",
								tooltip : "# of Assays"
								// renderer : this.assayCountStyler.createDelegate(this)
							}]
				});

		Gemma.CoexpressionDatasetGrid.superclass.initComponent.call(this);

	},

	assayCountStyler : function(value, metadata, record, row, col, ds) {
		return String.format(
				"{0}&nbsp;<a href='/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id={1}'>"
						+ "<img src='/Gemma/images/icons/magnifier.png' height='10' width='10'/></a>",
				record.data.bioAssayCount, record.data.id);
	},

	booleanStyler : function(value, metadata, record, row, col, ds) {
		if (value) {
			return "<img src='/Gemma/images/icons/ok.png' height='10' width='10' />";
		}
		return "";
	},

	eeTemplate : new Ext.Template("<a target='_blank' "
			+ "href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={id}' ext:qtip='{name}'>{shortName}</a>"),

	eeStyler : function(value, metadata, record, row, col, ds) {
		this.eeTemplate.apply(record.data);
	}

});

Gemma.CoexpressionDatasetGrid.updateDatasetInfo = function(datasets, eeMap) {
	for (var i = 0; i < datasets.length; ++i) {
		var ee = eeMap[datasets[i].id];
		if (ee) {
			datasets[i].shortName = ee.shortName;
			datasets[i].name = ee.name;
		}
	}
};
