
/**
 * Shows submitted jobs. Work in progress.
 * 
 * @author paul
 * @version $Id$
 */
Ext.onReady(function() {

	var v = new Ext.grid.GridPanel({
		renderTo : 'submittedTasks',
		id : 'submittedTaskGrid',
		height : 300,
		autoScroll : true,
		width : 800,
		loadMask : true,
		viewConfig : {
			forceFit : true
		},
		columns : [{
					header : "TaskId",
					dataIndex : "taskId",
					sortable : true,
					width : 100
				}, {
					header : "Submitted",
					dataIndex : "submissionTime",
					renderer : Ext.util.Format.dateRenderer('g:i:s l'),
					width : 120
				}, {
					header : "Started",
					dataIndex : "startTime",
					renderer : Ext.util.Format.dateRenderer('g:i:s l'),
					width : 120
				}, {
					header : "Runtime (s)",
					tooltip : "How long the job has been running",
					dataIndex : "startTime",
					renderer : function(value, metaData, record, rowIndex, colIndex, store) {
						if (record.get("startTime")) {
							return (new Date() - record.get("startTime")) / 1000;
						} else {
							return "Queued for " + (new Date() - record.get("submissionTime")) / 1000;
						}
					},
					width : 120
				}, {
                    header : "Status",
                    dataIndex : "taskStatus",
                    width : 60
                }, {
					header : "Submitter",
					dataIndex : "submitter"
				}, {
					header : "Type",
					dataIndex : "taskType",
					renderer : function(value, metaData, record, rowIndex, colIndex, store) {
						return value.replace(/.*\./, '').replace(/Impl$/, '');
					}
				}, {
					header : "Running remotely",
					dataIndex : "runningRemotely"
				}, {
					header : "Cancel",
					dataIndex : "taskId",
					renderer : function(value, metaData, record, rowIndex, colIndex, store) {
						return '<span class="link" onClick="Ext.getCmp(\'submittedTaskGrid\').cancelTask(\'' + value
								+ '\')"><img src="/Gemma/images/icons/stop.png" /></span>';
					}
				}],

		cancelTask : function(taskId) {
			Ext.Msg.show({
				title : 'Are you sure?',
				msg : 'Are you sure you want to cancel this task?',
				buttons : Ext.Msg.YESNO,
				fn : function(btn, text) {
					if (btn === 'yes') {
						ProgressStatusService.cancelJob(taskId, function(successfullyCancelled) {
							if (!successfullyCancelled) {
								Ext.Msg
										.alert("Couldn't cancel",
												"Sorry, the job couldn't be cancelled; perhaps it finished or was cancelled already?");
							} else {
								this.store.load();
							}
						});
					}
				},
				scope : this
			});
		},

		store : new Ext.data.Store({
					proxy : new Ext.data.DWRProxy({
								apiActionToHandlerMap : {
									read : {
										dwrFunction : ProgressStatusService.getSubmittedTasks
									}
								}
							}),
					autoLoad : true,
					reader : new Ext.data.ListRangeReader({
								id : 'taskId',
								record : Ext.data.Record.create([{
											name : "taskId",
											type : "string"
										}, {
											name : "submissionTime",
											type : "date"
										}, {
											name : "startTime",
											type : "date"
										}, {
											name : "taskStatus",
											type : "string"
										}, {
                                            name : "submitter",
                                            type : "string"
                                        }, {
											name : "taskType",
											type : "string"
										}, {
											name : "runningRemotely",
											type : "boolean"
										}])
							})
				}),
		buttons : [{
					text : "Refresh",
					handler : function() {
						v.store.load();
					}
				}]

	});

});