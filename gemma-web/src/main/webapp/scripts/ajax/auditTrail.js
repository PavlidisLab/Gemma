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
		
	var ds = new Ext.data.Store(
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
		{header: "Event", width: 120, dataIndex:"eventTypeName"}, 
		{header: "Comment", width: 300, dataIndex:"note"}]);
	
	cm.defaultSortable = false;

	var grid = new Ext.grid.Grid("auditTrail",
		{ds:ds, cm:cm, loadMask: true });
	grid.render();

	var toolbar = new Ext.Toolbar("auditTrailToolbar");
	var auditEventTypeStore = new Ext.data.SimpleStore({
		fields: ['type', 'description'],
		data: [
			['TroubleStatusFlagEvent', 'Trouble flag'],
			['OKStatusFlagEvent', 'OK flag (clear Trouble flag)']
		]
	});
	var auditEventTypeCombo = new Ext.form.ComboBox({
		store: auditEventTypeStore,
		displayField: 'description',
		typeAhead: true,
		mode: 'local',
		triggerAction: 'all',
		emptyText: 'Select an event type...',
		width: 200
	});
	var auditEventCommentField = new Ext.form.TextField({
		fieldLabel: 'Comment',
		name: 'comment',
		width: 400,
		allowBlank: true
	});
	toolbar.addField(auditEventTypeCombo);
	toolbar.addField(auditEventCommentField);
	var saveButton = toolbar.addButton({
		text: 'save',
		tooltip: 'Save the AuditEvent',
		handler: function() {
			alert("Save button clicked with '" +
				auditEventTypeCombo.getValue() + "' and '" +
				auditEventCommentField.getValue() + "'");
			// TODO implement AuditTrailServiceHelper
			// AuditTrailServiceHelper.addEvent(
			// 	auditableId,
			// 	auditEventTypeCombo.getValue(),
			// 	auditEventCommentField.getValue(),
			// 	function() { ds.reload(); grid.render(); }
			// );
		}
	});
	
	ds.load({params:[g]});
	
});
