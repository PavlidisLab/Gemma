Ext.onReady(function() {
		
	var id = dwr.util.getValue("auditableId");
	
	// this will be the case if we're not admins.
	if (!id) {
		return;
	}
	
	var clazz = dwr.util.getValue("auditableClass");
	
	// classDelegatingFor is the specific type of auditable.
	var g = {id:id, classDelegatingFor:clazz};
	var converttype = function(d) {
		return d.value;
	};
	
	var convertUser = function(d) {
		return d.userName;
	};
	
	var recordType = Ext.data.Record.create([
		{name:"id", type:"int"}, 
		{name:"date", type:"date"}, 
		{name:"actionName", type:"string"}, 
		{name:"note", type:"string"}, 
		{name:"performer", convert : convertUser }, 
		{name:"eventTypeName", type:"string"}]);
		
	var	ds = new Ext.data.Store(
		{
		proxy:new Ext.data.DWRProxy(AuditController.getEvents), 
		reader:new Ext.data.ListRangeReader({id:"id"}, recordType), 
		remoteSort:false
		});
		
	ds.setDefaultSort('date');
	
	var cm = new Ext.grid.ColumnModel([
			{header: "Date",  width: 80, dataIndex:"date"}, 
			{header: "Action", width: 50, dataIndex:"actionName" }, 
			{header: "Performer", width: 100, dataIndex:"performer"}, 
			{header: "Event", width: 100, dataIndex:"eventTypeName"}, 
			{header: "Comment", width: 300, dataIndex:"note"}]);
	
	cm.defaultSortable = false;
 
	var grid = new Ext.grid.Grid("auditTrail", {ds:ds, cm:cm, loadMask: true });
	grid.render();
	ds.load({params:[g]});
		
});