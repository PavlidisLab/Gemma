
var characteristicIdList;
var bmId; //the id of the biomaterial
var annotationsDs;
var annotationsGrid;

function refreshAnnotations() {
	annotationsDs.reload();
	annotationsGrid.render();
}


 var gridClickHandler = function(grid, rowIndex, event){
       		//Get the ids of the selected characteritics and put them in characteriticIdList
	       	var selected = annotationsGrid.getSelectionModel().getSelections();	
	   
	    	characteristicIdList = [];
	    	for(var index=0; index<selected.length; index++) {	    		
	    		characteristicIdList.push(selected[index].id);
	    	}  	
	    	
	    	if (deleteButton !== undefined) {
	    		if (characteristicIdList.length == 0)
	    			deleteButton.disable();
	    		else
	    			deleteButton.enable();
	    	}
       	
       }
       
 
Ext.onReady(function() {
		
	bmId = dwr.util.getValue("bmId");
	var clazz = dwr.util.getValue("bmClass");
	
	// classDelegatingFor is the bioMaterial class.
	var g = {id:bmId, classDelegatingFor:clazz};
	
	var eeRecordType = Ext.data.Record.create([
		{name:"id", type:"int"},
		{name:"className", type:"string"}, 
		{name:"termName", type:"string"}]);
		
	annotationsDs = new Ext.data.Store(
		{
		proxy:new Ext.data.DWRProxy(BioMaterialController.getAnnotation), 
		reader:new Ext.data.ListRangeReader({id:"id"}, eeRecordType), 
		remoteSort:false
		});
	annotationsDs.setDefaultSort('className');
	
	var cm = new Ext.grid.ColumnModel([
			{header: "Class",  width: 150, dataIndex:"className"}, 
			{header: "Term", width: 500, dataIndex:"termName" }]);
	cm.defaultSortable = false;
 
	annotationsGrid = new Ext.grid.Grid("bmAnnotations", {
		ds:annotationsDs,
		cm:cm,
		loadMask: true,
		autoExpandColumn: 1
	});

    annotationsGrid.on("rowclick", gridClickHandler);

	annotationsGrid.render();
	
	annotationsDs.load({params:[g]});
		
});