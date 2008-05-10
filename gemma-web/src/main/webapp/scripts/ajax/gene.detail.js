var goTermGrid = function() {
	var ds;
	var grid; // component
	var columnModel; // definition of the columns

	var golink = function(d) {
		var g = d.replace("_", ":");
		return "<a target='_blank' href='http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?view=details&query="
				+ g + "'>" + g + "</a>";
	};

	function initDataSource() {
		var recordType = Ext.data.Record.create([{
			name : "id",
			type : "int"
		}, {
			name : "termUri",
			type : "string"
		}, {
			name : "termName",
			type : "string"
		}, {
			name : "evidenceCode",
			type : "string"
		}]);
		ds = new Ext.data.Store({
			proxy : new Ext.data.DWRProxy(GeneController.findGOTerms),
			reader : new Ext.data.ListRangeReader({
				id : "id"
			}, recordType),
			remoteSort : false
		});
		ds.on("load", function() {
		});
	}

	function getColumnModel() {
		if (!columnModel) {
			columnModel = new Ext.grid.ColumnModel([{
				header : "ID",
				dataIndex : "termUri",
				renderer : golink,
				width : 75
			}, {
				header : "Term",
				dataIndex : "termName",
				width : 375
			}, {
				header : "Evidence Code",
				dataIndex : "evidenceCode",
				width : 100
			}

			]);
			columnModel.defaultSortable = true;
		}
		return columnModel;
	}
	function buildGrid() {
		grid = new Ext.grid.GridPanel({
			renderTo : "go-grid",
			height : Ext.get("go-grid").getHeight(),
			ds : ds,
			cm : getColumnModel(),
			loadMask : true
		});
		grid.render();
	}
	return {
		init : function() {
			var geneid = dwr.util.getValue("gene");
			var g = {
				id : geneid
			};
			initDataSource();
			buildGrid();
			ds.load({
				params : [g]
			});
		},
		getStore : function() {
			return ds;
		}
	};
}();

Ext.onReady(goTermGrid.init);

Ext.onReady(function() {
	var geneid = dwr.util.getValue("gene");
	var g = {
		id : geneid
	};
	var converttype = function(d) {
		return d.value;
	};
	var recordType = Ext.data.Record.create([{
		name : "id",
		type : "int"
	}, {
		name : "name",
		type : "string"
	}, {
		name : "description",
		type : "string"
	}, {
		name : "type",
		convert : converttype
	}]);

	var ds = new Ext.data.Store({
				proxy : new Ext.data.DWRProxy(GeneController.getProducts),
				reader : new Ext.data.ListRangeReader({
					id : "id"
				}, recordType),
				remoteSort : false
			});
	ds.setDefaultSort('type', 'name');

	var cm = new Ext.grid.ColumnModel([{
		header : "Name",
		width : 80,
		dataIndex : "name"
	}, {
		header : "Type",
		width : 80,
		dataIndex : "type"
	}, {
		header : "Description",
		width : 270,
		dataIndex : "description"
	}]);
	cm.defaultSortable = true;

	var grid = new Ext.grid.GridPanel({
		renderTo : "geneproduct-grid",
		height : Ext.get("geneproduct-grid").getHeight(),
		ds : ds,
		cm : cm,
		loadMask : true
	});
	grid.render();
	ds.load({
		params : [g]
	});

});
