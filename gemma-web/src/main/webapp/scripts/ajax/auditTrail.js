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
	
	var addEventDialogDiv = Ext.DomHelper.append(document.body, {
		tag: 'div',
		id: 'addEventDialogDiv'
	});
	addEventDialogDiv.width = 300;
	addEventDialogDiv.height = 200;
	addEventDialogDiv.style.visibility = 'hidden';
	var addEventDialog = new Ext.BasicDialog(addEventDialogDiv, {
		width: 440,
		height: 400,
		shadow: true,
		minWidth: 200,
		minHeight: 150,
		proxyDrag: true
	});
	addEventDialog.setTitle('Add Audit Event');
	var auditEventTypeStore = new Ext.data.SimpleStore({
		fields: ['type', 'description'],
		data: [
			['TroubleStatusFlagEvent', 'Trouble flag'],
			['OKStatusFlagEvent', 'OK flag (clear Trouble flag)'],
			['ValidatedFlagEvent', 'Validated flag']
		]
	});
	var auditEventTypeCombo = new Ext.form.ComboBox({
		fieldLabel: 'Event type',
		store: auditEventTypeStore,
		displayField: 'description',
		valueField: 'type',
		typeAhead: true,
		mode: 'local',
		triggerAction: 'all',
		emptyText: 'Select an event type...',
		editable: false,
		width: 180
	});
	var auditEventCommentField = new Ext.form.TextField({
		fieldLabel: 'Comment',
		width: 400,
		allowBlank: true
	});
	var auditEventDetailField = new Ext.form.TextArea({
		fieldLabel: 'Details',
		height: 200,
		width: 400,
		allowBlank: true
	});
	var addEventForm = new Ext.Form({
		labelAlign: 'top'
	});
	addEventForm.add(auditEventTypeCombo);
	addEventForm.add(auditEventCommentField);
	addEventForm.add(auditEventDetailField);
	addEventForm.addButton('Add Event', function() {
		AuditController.addAuditEvent(
			g,
			auditEventTypeCombo.getValue(),
			auditEventCommentField.getValue(),
			auditEventDetailField.getValue(),
			function() { ds.reload(); grid.getView().refresh(true); }
		);
		addEventDialog.hide;
	}, addEventDialog); //.disable();
	addEventForm.addButton('Cancel', addEventDialog.hide, addEventDialog);
	addEventForm.render(addEventDialog.body);

	var auditTrailToolbar = new Ext.Toolbar(grid.getView().getHeaderPanel(true));
	auditTrailToolbar.addSpacer({ width: 400 });
	auditTrailToolbar.addButton({
		text: 'Add audit event',
		tooltip: 'Add an audit event',
		handler: function() {
			addEventDialog.show(Ext.get('auditTrail').dom);
		}
	});
	
	ds.load({params:[g]});
	
});
