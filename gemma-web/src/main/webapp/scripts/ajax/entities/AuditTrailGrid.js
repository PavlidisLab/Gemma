/**
 * 
 */
Ext.namespace('Gemma');
Gemma.AuditTrailGrid = Ext.extend(Ext.grid.GridPanel, {

			title : "History",
			collapsible : true,
			height : 200,
			width : 720,
			loadMask : true,
			stateful : false,

			record : Ext.data.Record.create([{
						name : "id",
						type : "int"
					}, {
						name : "date",
						type : "date"
					}, {
						name : "actionName",
						type : "string"
					}, {
						name : "note",
						type : "string"
					}, {
						name : "detail",
						type : "string"
					}, {
						name : "performer"
					}, {
						name : "eventTypeName",
						type : "string"
					}]),

			createEvent : function(obj) {
				var cb = function() {
					this.getStore().reload();
				}.createDelegate(this);
				AuditController.addAuditEvent(this.auditable, obj.type, obj.comment, obj.details, {
							callback : cb
						});
			},

			showAddEventDialog : function() {
				if (!this.addEventDialog) {
					this.addEventDialog = new Gemma.AddAuditEventDialog();
					this.addEventDialog.on("commit", function(resultObj) {
								this.createEvent(resultObj)
							}.createDelegate(this));
				}
				this.addEventDialog.show();
			},

			initComponent : function() {

				Ext.apply(this, {
							columns : [{
										header : "Date",
										width : 105,
										dataIndex : "date",
										sortable : true
									}, {
										header : "Action",
										width : 50,
										dataIndex : "actionName"
									}, {
										header : "Performer",
										width : 100,
										dataIndex : "performer"
									}, {
										header : "Event",
										width : 120,
										dataIndex : "eventTypeName"
									}, {
										header : "Comment",
										width : 275,
										dataIndex : "note"
									}],
							store : new Ext.data.Store({
										proxy : new Ext.data.DWRProxy(AuditController.getEvents),
										reader : new Ext.data.ListRangeReader({
													id : "id"
												}, this.record),
										remoteSort : false
									}),
							tbar : [{
										xtype : 'button',
										text : 'Add audit event',
										tooltip : 'Add an audit event',
										handler : this.showAddEventDialog,
										scope : this
									}]
						});

				Gemma.AuditTrailGrid.superclass.initComponent.call(this);

				this.getColumnModel().defaultSortable = false;
				this.getStore().setDefaultSort('date');

				this.getStore().load({
							params : [this.auditable]
						});

				this.on('rowdblclick', function(grid, row, event) {
							var record = this.getStore().getAt(row).data;
							var note = record.note;
							var detail = record.detail;
							var content = "Date: " + record.date + "<br />Performer: " + record.performer
									+ "<br />Note: " + note + "<br />Details: " + detail;

							Ext.MessageBox.alert("Event details", content);
						});
			}
		});

/**
 * 
 */
Gemma.AddAuditEventDialog = Ext.extend(Ext.Window, {

			height : 350,
			width : 550,
			shadow : true,
			minWidth : 200,
			minHeight : 150,
			closeAction : "hide",
			modal : true,
			layout : 'fit',
			layoutConfig : {
				forceFit : true
			},

			title : "Add an audit event",

			validate : function() {
				return this.auditEventTypeCombo.isValid() && this.auditEventCommentField.isValid()
						&& this.auditEventDetailField.isValid();
			},

			initComponent : function() {

				this.auditEventTypeStore = new Ext.data.SimpleStore({
							fields : ['type', 'description'],
							data : [['CommentedEvent', 'Comment'], ['TroubleStatusFlagEvent', 'Trouble flag'],
									['OKStatusFlagEvent', 'OK flag (clear Trouble flag)'],
									['ValidatedFlagEvent', 'Validated flag']]
						});

				this.auditEventTypeCombo = new Ext.form.ComboBox({
							fieldLabel : 'Event type',
							store : this.auditEventTypeStore,
							displayField : 'description',
							valueField : 'type',
							typeAhead : true,
							mode : 'local',
							allowBlank : false,
							triggerAction : 'all',
							emptyText : 'Select an event type',
							editable : false,
							width : 180
						});
				this.auditEventCommentField = new Ext.form.TextField({
							fieldLabel : 'Comment',
							width : 400,
							allowBlank : true
						});
				this.auditEventDetailField = new Ext.form.TextArea({
							fieldLabel : 'Details',
							height : 200,
							width : 400,
							allowBlank : true
						});

				this.fs = new Ext.form.FieldSet({
							items : [this.auditEventTypeCombo, this.auditEventCommentField, this.auditEventDetailField]
						});

				Ext.apply(this, {
							items : [this.fs],
							buttons : [{
										text : 'Add Event',
										handler : function() {
											if (this.validate()) {
												this.hide();
												this.fireEvent('commit', {
															comment : this.auditEventCommentField.getValue(),
															type : this.auditEventTypeCombo.getValue(),
															details : this.auditEventDetailField.getValue()
														});
											} else {
												Ext.Msg.alert("Error", "You must fill in the required fields");
											}
										}.createDelegate(this),
										scope : this
									}, {
										text : 'Cancel',
										handler : this.hide.createDelegate(this)
									}]
						});

				Gemma.AddAuditEventDialog.superclass.initComponent.call(this);

				this.addEvents('commit');

			}

		});
