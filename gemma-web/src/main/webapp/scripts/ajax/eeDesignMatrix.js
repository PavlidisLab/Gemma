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
		
		this.grid = new Ext.grid.GridPanel( {
			ds : this.ds,
			cm : cm,
			renderTo : "eeDesignMatrix",
			autoHeight : true,
			autoWidth : true,
			viewConfig : { autoFill : true }
		} );
		this.grid.render();
		
		this.grid.autoSizeColumns = function() {
		    for (var i = 0; i < this.colModel.getColumnCount(); i++) {
    			this.autoSizeColumn(i);
		    }
		};
		this.grid.autoSizeColumn = function(c) {
			var w = this.view.getHeaderCell(c).firstChild.scrollWidth;
			for (var i = 0, l = this.store.getCount(); i < l; i++) {
				w = Math.max(w, this.view.getCell(i, c).firstChild.scrollWidth);
			}
			this.colModel.setColumnWidth(c, w);
			return w;
		};
		this.grid.autoSizeColumns();
		this.grid.doLayout();
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
