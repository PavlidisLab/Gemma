var eeDS;
var eeGrid;

function refreshEEAnnotations() {
	eeDs.reload();
	eeGrid.render();
}

Ext.onReady(function() {
	
	var admin = dwr.util.getValue("auditableId");  // will only be present for admin user	
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
	eeDs.setDefaultSort('classLabel');
	
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
	eeGrid.render();
	
	eeDs.load({params:[g]});
		
});