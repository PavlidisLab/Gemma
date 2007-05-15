

var GridUI = function() {
  var ds;
	var grid; //component
	var columnModel; // definition of the columns
	
	function initDataSource() {
		var recordType = Ext.data.Record.create([
		  {name: "id", type: "int"},
		  {name: "term", type: "string"},
			{name: "comment", type: "string"}			
		  ]);

		  ds = new Ext.data.Store({
		    proxy: new Ext.data.DWRProxy(MgedOntologyService.getTerm, {pagingAndSort : true }),
		    reader: new Ext.data.ListRangeReader( 
					{id:'id', totalProperty:'totalSize'}, recordType),
		    remoteSort: true
		  });
		
			ds.on("load", function () {
			});		
	}
	
	function getColumnModel() {
		if(!columnModel) {
			columnModel = new Ext.grid.ColumnModel(
				[
					{
						header: 'Description',
						width: 250,
						sortable: true,
						dataIndex: 'comment'
					},
					{
						header: 'id',
						width: 250,
						sortable: true,
						dataIndex: 'id'
					},
					{
						header: 'Term',
						width: 250,
						sortable: true,
						dataIndex: 'term'				
					}																
				]);
		}
		return columnModel;
	}	
	
	function buildGrid() {				
		grid = new Ext.grid.Grid(
			'mygrid',
			{
				ds: ds,
				cm: getColumnModel(),
				autoSizeColumns: true,
				selModel: new Ext.grid.RowSelectionModel({singleSelect:true})
			}
		);
		
		
		grid.render();
	}
			

	return {
		init : function() {
			initDataSource();
			ds.load(null);			
			buildGrid();
		},
		
		getStore: function() {
			return ds;
		}
	}
}();

	Ext.onReady(GridUI.init, GridUI, true);	