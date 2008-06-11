var DesignMatrix = {
	ds : null,
	grid : null,
	dwrCallback : function(rows) {
		var factors = rows[0].factors;
		var record = [];
		var columns = [];
		columns.push({
			header : "Assays",
			dataIndex : "count",
			sortable : "true",
			tooltip : "How many assays are in this group"
		});
		for (var i = 0; i < factors.length; ++i) {
			record.push({
				name : factors[i],
				type : "string"
			});
			columns.push({
				header : factors[i],
				dataIndex : factors[i],
				sortable : "true"
			});
		}
		record.push({
			name : "count",
			type : "int"
		});

		var DesignMatrixRow = Ext.data.Record.create(record);
		var cm = new Ext.grid.ColumnModel(columns);

		var data = [];
		for (var k = 0; k < rows.length; ++k) {
			data[k] = [];
			for (var j = 0; j < factors.length; ++j) {
				data[k][j] = rows[k].factorValueMap[factors[j]];
			}
			data[k][factors.length] = rows[k].count;
		}
		this.ds = new Ext.data.Store({
			proxy : new Ext.data.MemoryProxy(data),
			reader : new Ext.data.ArrayReader({}, DesignMatrixRow),
			remoteSort : false
		});
		this.ds.load();

		this.grid = new Ext.grid.GridPanel({
			ds : this.ds,
			cm : cm,
			renderTo : "eeDesignMatrix",
			autoHeight : true,
			maxHeight : 300,
			autoWidth : true,
			viewConfig : {
				autoFill : true
			}
		});
		this.grid.render();

		this.grid.doLayout();
	},
	init : function() {
		var entityDelegator = {
			id : dwr.util.getValue("eeId"),
			classDelegatingFor : dwr.util.getValue("eeClass")
		};
		ExpressionExperimentController.getDesignMatrixRows(entityDelegator,
				this.dwrCallback);
	}
};
