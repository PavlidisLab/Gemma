var eeDS;
var eeGrid;
var characteristicIdList;

function refreshEEAnnotations() {
	eeDs.reload();
	eeGrid.render();
}


 var gridClickHandler = function(grid, rowIndex, event){
       		//Get the ids of the selected characteritics and put them in characteriticIdList
	       	var selected = eeGrid.getSelectionModel().getSelections();	
	   
	    	characteristicIdList = [];
	    	for(var index=0; index<selected.length; index++) {	    		
	    		characteristicIdList.push(selected[index].id);
	    	}  	
       	
       }
       
 
Ext.onReady(function() {
		
	var id = dwr.util.getValue("eeId");
	var clazz = dwr.util.getValue("eeClass");
	
	// classDelegatingFor is the ExpressionExperiment class.
	var g = {id:id, classDelegatingFor:clazz};
	
	var eeRecordType = Ext.data.Record.create([
		{name:"id", type:"int"},
		{name:"className", type:"string"}, 
		{name:"termName", type:"string"}]);
		
	eeDs = new Ext.data.Store(
		{
		proxy:new Ext.data.DWRProxy(ExpressionExperimentController.getAnnotation), 
		reader:new Ext.data.ListRangeReader({id:"id"}, eeRecordType), 
		remoteSort:false
		});
	eeDs.setDefaultSort('className');
	
	var cm = new Ext.grid.ColumnModel([
			{header: "Class",  width: 150, dataIndex:"className"}, 
			{header: "Term", width: 500, dataIndex:"termName" }]);
	cm.defaultSortable = false;
 
	eeGrid = new Ext.grid.Grid("eeAnnotations", {
		ds:eeDs,
		cm:cm,
		loadMask: true,
		autoExpandColumn: 1
	});

    eeGrid.on("rowclick", gridClickHandler);

	eeGrid.render();
	
	eeDs.load({params:[g]});
		
});