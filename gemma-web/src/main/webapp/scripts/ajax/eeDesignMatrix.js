/**
 * Displays summary of an experimental design
 * 
 * @version $Id$
 * @author Luke, Paul
 * @type
 */
var DesignMatrix = {
	
	
	
	build : function(rows) {

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
			var fn =  factors[i] ;  
			record.push({
						name : cleanName(fn),
						type : "string"
					});
			columns.push({
						header : fn,
						dataIndex : cleanName(fn),
						sortable : "true",
						tooltip : fn
					});
		}

		record.push({
					name : "count",
					type : "int"
				});

		var designMatrixRow = Ext.data.Record.create(record);
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
					reader : new Ext.data.ArrayReader({}, designMatrixRow),
					remoteSort : false
				});
		this.ds.load();

		this.grid = new Ext.grid.GridPanel({
					ds : this.ds,
					cm : cm,
					collapsible : true,
					title : "Experimental Design overview",
					renderTo : "eeDesignMatrix",
					height : 125,
					width : 600,
					viewConfig : {
						forceFit : true
					}
				});
	},

	init : function(entityDelegator) {
		ExpressionExperimentController.getDesignMatrixRows(entityDelegator, this.build);
	}
};

function cleanName(string) {
		return string.replace(/[\\\/\"\'\s\(\),\.;]/g, "_");
}
