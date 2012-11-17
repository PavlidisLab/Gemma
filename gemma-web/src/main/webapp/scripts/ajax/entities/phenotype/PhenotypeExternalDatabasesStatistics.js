Ext.namespace('Gemma');

Gemma.ExternalDatabasesStatistics = Ext.extend(Gemma.GemmaGridPanel, {
	loadMask : true,
	record : Ext.data.Record.create([ {
		name : "name",
		type : "string"
	}, {
		name : "numEvidence",
		type : "int"
	}, {
		name : "numGenes",
		type : "int"
	}, {
		name : "numPhenotypes",
		type : "int"
	}, {
		name : "lastUpdateDate",
		type : "string"
	} ]),
	initComponent : function() {

		Gemma.ExternalDatabasesStatistics.superclass.initComponent.call(this);

		var store = new Ext.data.Store({
			autoLoad : true,

			proxy : new Ext.data.DWRProxy(
					PhenotypeController.calculateExternalDatabasesStatistics),

			reader : new Ext.data.JsonReader({
				fields : [ 'name', 'description', 'webUri', 'numEvidence',
						'numGenes', 'numPhenotypes', 'lastUpdateDate' ]
			}),

			sortInfo : {
				field : 'name',
				direction : 'ASC'
			}

		});

		function renderDatabase(val, metaData, record, row, col, store,
				gridView) {
			return '<A HREF=\'' + record.data.webUri + '\'>' + val + '</A>';
		}

		Ext.apply(this, {
			store : store
		});

		Ext.apply(this, {
			colModel : new Ext.grid.ColumnModel({
				defaults : {
					sortable : true
				},

				columns : [ {

					header : "External database",
					dataIndex : "name",
					renderer : renderDatabase,
					width : 0.55
				}, {

					header : "Description",
					dataIndex : "description",
					width : 0.55
				}, {

					header : "Number of evidence",
					dataIndex : "numEvidence",
					width : 0.55
				}, {

					header : "Number of genes",
					dataIndex : "numGenes",
					width : 0.55
				}, {

					header : "Number of phenotypes",
					dataIndex : "numPhenotypes",
					width : 0.55
				}, {

					header : "Last updated date",
					dataIndex : "lastUpdateDate",
					width : 0.55
				}

				]
			})
		});
	}
});
