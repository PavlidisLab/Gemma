var DesignMatrix = {
	ds : null,
	grid : null,
	dwrCallback : function( rows ) {
		var factors = rows[0].factors;
		var record = [];
		var columns = [];
		for (var i=0; i<factors.length; ++i) {
			record.push( { name : factors[i], type : "string" } );
			columns.push( { header : factors[i], dataIndex : factors[i], sortable : "true" } );
		}
		record.push( { name : "count", type : "int" } );
		columns.push( { header : "Assays", dataIndex : "count", sortable : "true" } );
		var DesignMatrixRow = Ext.data.Record.create( record );
		var cm = new Ext.grid.ColumnModel( columns );
		
		var data = [];
		for (var i=0; i<rows.length; ++i) {
			data[i] = [];
			for (var j=0; j<factors.length; ++j) {
				data[i][j] = rows[i].factorValueMap[factors[j]];
			}
			data[i][factors.length] = rows[i].count;
		}
		this.ds = new Ext.data.Store( {
			proxy : new Ext.data.MemoryProxy( data ), 
			reader : new Ext.data.ArrayReader( { }, DesignMatrixRow ),
			remoteSort : false
		} );
		this.ds.load();
		
		this.grid = new Ext.grid.Grid( "eeDesignMatrix", { ds : this.ds, cm : cm } );
		this.grid.render();
		this.grid.getView().autoSizeColumns();
	},
	init : function() {
		var entityDelegator = {
			id : dwr.util.getValue("eeId"),
			classDelegatingFor : dwr.util.getValue("eeClass")
		};
		ExpressionExperimentController.getDesignMatrixRows( entityDelegator, this.dwrCallback );
	}
}

Ext.onReady( DesignMatrix.init, DesignMatrix );
