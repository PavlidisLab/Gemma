
/**
 * Shows submitted jobs. Work in progress.
 * 
 * @author paul
 * @version $Id$
 */
Ext.onReady(function() {

			var v = new Ext.grid.GridPanel({
						renderTo : 'submittedTasks',
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
									header : "Submitter",
									dataIndex : "submitter"
								}, {
									header : "Type",
									dataIndex : "taskInterface",
									renderer : function(value, metaData, record, rowIndex, colIndex, store) {
										return value.replace(/.*\./, '').replace(/Impl$/, '');
									}
								}, {
									header : "Method",
									dataIndex : "taskMethod"
								}, {
									header : "On grid",
									dataIndex : "willRunOnGrid"
								}],

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
															name : "submitter",
															type : "string"
														}, {
															name : "taskInterface",
															type : "string"
														}, {
															name : "taskMethod",
															type : "string"
														}, {
															name : "willRunOnGrid",
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