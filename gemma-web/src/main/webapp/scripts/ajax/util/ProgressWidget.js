Ext.namespace('Gemma');

/**
 * Config options;
 * 
 * @param taskId
 *            (required)
 * @param callback
 *            fired when finished, with the task payload as argument
 * @param showAllMessages
 *            when finished, show all the messages. Useful for complex jobs.
 * @class Gemma.ProgressWindow
 * @extends Ext.Window
 */
Gemma.ProgressWindow = Ext.extend(Ext.Window, {

			modal : true,
			closable : false,
			resizable : false,
			stateful : false,
			showAllMessages : false,

			resizable : false,
			collapsible : false,
			// width : 600,
			autoHeight : true,
			autoWidth : true,
			id : "progressWidget-window",

			initComponent : function() {

				this.pBar = new Gemma.ProgressWidget({
							taskId : this.taskId
						});

				Ext.apply(this, {
							items : [this.pBar]
						});

				Gemma.ProgressWindow.superclass.initComponent.call(this);

				this.addEvents('success', 'failed');

				this.on('show', this.pBar.startProgress.createDelegate(this.pBar));
			},

			afterRender : function(a, b) {
				Gemma.ProgressWindow.superclass.afterRender.call(this, a, b);

				this.pBar.on('done', function(payload) {
							this.getEl().switchOff({
										callback : function() {
											if (this.showAllMessages && this.pBar.allMessages.length > 0) {

												var msgs = new Ext.Window({
															title : "Job finished. The following messages were generated",
															layout : 'fit',
															closeAction : 'close',
															items : [{
																		xtype : 'panel',
																		id : 'progress-messages-panel',
																		stateful : false,
																		height : 400,
																		autoScroll : true,
																		width : 400,
																		html : this.pBar.allMessages
																	}]
														});
												msgs.show();

											}
											this.destroy();
										},
										scope : this
									});
							if (this.callback) {
								this.callback(payload);
							}
						}.createDelegate(this));

				this.pBar.on('fail', function(message) {
							var msgs = new Ext.Window({
										title : "Error",
										layout : 'fit',
										closeAction : 'close',
										items : [{
											xtype : 'panel',
											height : 400,
											autoScroll : true,
											width : 400,
											html : message + "<br/>Additional log messages during run:<br/> "
													+ this.pBar.allMessages
										}]
									});
							msgs.show();

							this.destroy();
						}.createDelegate(this));
			}

		});

/**
 * Config options:
 * 
 * @param taskId
 *            (required)
 * @class Gemma.ProgressWidget
 * @extends Ext.Panel
 */
Gemma.ProgressWidget = Ext.extend(Ext.Panel, {
			BAR_UPDATE_INTERVAL : 2000,
			allMessages : "",
			previousMessage : '',
			timeoutid : null,
			resizable : false,
			waiting : false,
			layout : 'fit',
			bodyBorder : false,
			stateful : false,
			id : "progressWidget-panel",
			// width : 600,

			initComponent : function() {

				this.progressBar = new Ext.ProgressBar({
							style : 'font-weight:normal;font-size:smaller;',
							width : 400,
							text : "Initializing ..."
						});

				Ext.apply(this, {
							items : [this.progressBar]
						});

				Gemma.ProgressWidget.superclass.initComponent.call(this);

				Ext.apply(this);

				this.addEvents('done', 'fail', 'cancel');
			},

			/**
			 * Handle failure of the server to respond to a check.
			 * 
			 * @param {}
			 *            data
			 * @param {}
			 *            e
			 */
			handleResponseFailure : function(data, e) {
				/*
				 * keep going. Hopefully just a temporary network lapse.
				 */
			},

			/**
			 * Handle failure of a job.
			 * 
			 * @param {}
			 *            data
			 * @param {}
			 *            e
			 */
			handleFailure : function(data, e) {
				this.stopProgress();
				var messageText = "";
				if (data.description) {
					messageText = data.description;
				} else if (!e) {
					messageText = data;
				} else if (e.message) {
					messageText = e.message;
				} else {
					messageText = e;
				}
				this.fireEvent("fail", messageText);
			},

			/**
			 * Start the progressbar in motion.
			 */
			startProgress : function() {
				/*
				 * Don't start twice.
				 */
				if (this.waiting) {
					return;
				}
				this.progressBar.wait({
							text : "Starting ..."
						});

				if (!this.taskId) {

					var taskId = this.findTaskId();

					if (!taskId) {
						alert("no task id");
						return;
					}

					this.taskId = taskId;

				}

				var callParams = [];
				var callback = this.updateProgress.createDelegate(this);
				var errorHandler = this.handleResponseFailure.createDelegate(this);
				callParams.push(callback);
				callParams.push(errorHandler);

				/*
				 * Start it running on a delay.
				 */

				var f = this.refreshProgress.createDelegate(this, callParams, false);
				this.timeoutid = window.setInterval(f, this.BAR_UPDATE_INTERVAL);
			},

			stopProgress : function() {
				window.clearInterval(this.timeoutid);
				this.previousMessage = null;
				this.waiting = false;
				this.progressBar.updateText("Finished ...");
				this.progressBar.reset();
			},

			handleFinalResult : function(result) {
				this.stopProgress();
				this.fireEvent('done', result);
			},

			done : false,

			updateProgress : function(data) {
				this.waiting = false;
				var messages = "";
				var messagesToSave = '';

				for (var i = 0, len = data.length; i < len; i++) {
					var d = data[i];
					if (messages.length > 0) {
						messages = messages + "; " + d.description;
					} else {
						messages = d.description;
					}
					messagesToSave = messagesToSave + "<br/>" + d.description;

					if (d.failed) {
						return this.handleFailure(d);
					} else if (d.done && !this.done) {
						// try to ensure we only call this once.
						TaskCompletionController.checkResult(this.taskId, {
									callback : this.handleFinalResult.createDelegate(this),
									errorHandler : this.handleFailure.createDelegate(this)
								});
						this.done = true;

					}
				}

				if (this.previousMessage != messages && messages.length > 0) {
					this.allMessages = this.allMessages + messagesToSave;

					// chop them if they are too long
					messages = Ext.util.Format.ellipses(messages, 200);

					this.previousMessage = messages;

					this.progressBar.updateText(messages);

				} else {
					this.progressBar.text = this.progressBar.text.replace('.......', '');

					this.progressBar.updateText(this.progressBar.text + '.');
				}
			},

			/*
			 * Private Send a cancel notification to the server.
			 */
			cancelJob : function() {
				this.stopProgress();
				// document.getElementById("progressTextArea").innerHTML = "Cancelling...";
				var f = this.cancelCallback.createDelegate(this, [], true);
				ProgressStatusService.cancelJob(this.taskId, f);
			},

			/*
			 * Private Check for status from server.
			 */
			refreshProgress : function(callback, errorHandler) {
				// only check for status if we aren't already waiting for a reply.
				if (!this.waiting) {
					ProgressStatusService.getProgressStatus(this.taskId, {
								callback : callback,
								errorHandler : errorHandler
							});
					this.waiting = true;
				}
			},

			/*
			 * Private callback to handle cancellation @param {Object} data
			 */
			cancelCallback : function(data) {
				this.stopProgress(); // should already be stopped.
				var messageArea = Ext.get("messages");
				if (messageArea) {
					Ext.DomHelper.overwrite("messages", {
								tag : 'img',
								src : '/Gemma/images/icons/ok.png'
							});
					Ext.DomHelper.append("messages", "&nbsp;Job was cancelled.");
				}
				this.fireEvent('cancel');

			},

			findTaskId : function() {

				// try to get from query string

				var queryStart = document.URL.indexOf("?");
				if (queryStart > -1) {
					var param = Ext.urlDecode(document.URL.substr(queryStart + 1));
					if (param.taskId)
						return param.taskId;
				}

				// try to get from hidden input field.
				return dwr.util.getValue("taskId");
			}

		});