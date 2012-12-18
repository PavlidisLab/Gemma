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
			autoHeight : true,

			// autoWidth : true,
			width : 400,
			id : "progressWidget-window",

			initComponent : function() {

				this.pBar = new Gemma.ProgressWidget({
							taskId : this.taskId
						});

				Ext.apply(this, {
							items : [this.pBar]
						});

				Gemma.ProgressWindow.superclass.initComponent.call(this);

				this.addEvents('done', 'fail');

				this.on('show', this.start, this);

				this.relayEvents(this.pBar, ['done', 'fail']);
			},

			start : function() {
				this.pBar.on('done', function(payload) {
							if (this.callback) {
								this.callback(payload);
							}
						}, this);

				this.pBar.on('fail', function(message) {
							if (this.errorHandler) {
								this.errorHandler(message);
							}
						}, this);

				this.pBar.on('cancel', function(successfullyCancelled) {
							if (this.errorHandler) {
								if (successfullyCancelled) {
									this.errorHandler("Job was cancelled");
								} else {
									this.errorHandler("Could not be cancelled?");
								}
							}
						}, this);

				this.pBar.startProgress();
			},

			afterRender : function(a, b) {
				Gemma.ProgressWindow.superclass.afterRender.call(this, a, b);

				this.pBar.on('done', function(payload) {
							this.getEl().switchOff({
										callback : function() {
											if (this.showAllMessages && this.pBar.allMessages.length > 0) {
												this.pBar.showAllMessages("Job completed normally");
											}
											this.destroy();
										},
										scope : this
									});
						}.createDelegate(this));

				this.pBar.on('fail', function(message) {
							this.pBar.allMessages = message + "<br/><br/>Other messages:<br/>" + this.pBar.allMessages;
							this.pBar.showAllMessages("Job failed!");
							this.destroy();
						}.createDelegate(this));

				this.pBar.on('cancel', function(successfullyCancelled) {
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
	noEmailOption : false,
	// layout : 'fit',
	bodyBorder : false,
	stateful : false,
	
	id : "progressWidget-panel",

	showAllMessages : function(t) {
		if (!this.allMessages) {
			return;
		}
		
		
		if (!this.logsActive){
			this.insert(1, new Ext.form.Label({
				id: "logsProgressWidget",
				html : this.allMessages			
			}) );
			this.logsActive=true;
		}else{
			this.remove(1,true);
			this.logsActive=false;
		}
		
		this.doLayout();
		
		this.on('message-received', function() {
			
			Ext.DomHelper.overwrite('logsProgressWidget', {
				id : 'logsProgressWidget',				
				html : this.allMessages
			});
		});
		

	},

	initComponent : function() {

		this.progressBar = new Ext.ProgressBar({
					width : 400,
					text : "Initializing ..."
				});

		Ext.apply(this, {
					items : [this.progressBar],
					buttons : [{
								text : "Logs",
								id: "progresslogsbutton",
								tooltip : "Show log messages",
								handler : function() {
									this.showAllMessages("Messages so far");
								},								
								scope : this
							}, {
								text : "Cancel Job",
								id: "progresscancelbutton",
								tooltip : "Attempt to stop the job.",
								handler : function() {

									Ext.Msg.show({
												title : 'Cancel?',
												msg : 'Are you sure?',
												buttons : Ext.Msg.YESNO,
												fn : function(btn) {
													if (btn == 'yes') {
														this.cancelJob();
													}
												}.createDelegate(this),
												icon : Ext.MessageBox.QUESTION
											});

								},								
								scope : this
							}, {
								text : "Hide",
								id: "progresshidebutton",
								tooltip : "Remove the progress bar and return to the page",
								handler : function() {
									/*
									 * FIXME add link to job listing
									 */
									var k = Ext.Msg.show({
												title : "Discontinuing monitoring",
												msg : "The job will continue to run. You can get an email on completion. ",
												buttons : {
													ok : 'OK',
													cancel : 'Email me'
												},
												fn : function(btn) {
													if (btn == 'cancel') {
														ProgressStatusService.addEmailAlert(this.taskId);
													}
												},
												scope : this
											});

									this.stopProgress();
									if (this.ownerCt) { // ugly.
										this.ownerCt.destroy();
									} else {
										this.destroy();
									}

								},
								scope : this
							}]
				});

		Gemma.ProgressWidget.superclass.initComponent.call(this);

		Ext.apply(this);

		this.addEvents('done', 'fail', 'cancel', 'message-received');
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
		this.waiting = false;
		this.progressBar.updateText("Waiting...");
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
		this.progressBar.reset();
	},

	handleFinalResult : function(result) {
		this.stopProgress();
		this.fireEvent('done', result);
	},

	done : false,

	le : 0.3,

	updateProgress : function(data) {

		// for testing: simulate a network failure.
		// if (Math.random() < this.le ) {
		// this.handleResponseFailure();
		// this.le = 0.0;
		// return;
		// }

		this.waiting = false;
		var messages = "";
		var messagesToSave = '';

		for (var i = 0, len = data.length; i < len; i++) {
			var d = data[i];

			/*
			 * Only show the most recent message in the progress bar.
			 */
			messages = d.description;

			/*
			 * But put all messages in the logs.
			 */
			messagesToSave = messagesToSave  + d.description + "<br/>";

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
			messages = Ext.util.Format.ellipsis(messages, 70);

			this.previousMessage = messages;

			this.progressBar.updateText(messages);

			this.fireEvent('message-received', messages);
		} else {
			/*
			 * Just show dots to make it clear stuff is still happening.
			 */
			this.progressBar.updateText(this.progressBar.text.replace('.......', ''));
			this.progressBar.updateText(this.progressBar.text + '.');
		}
	},

	/*
	 * Private Send a cancel notification to the server.
	 */
	cancelJob : function() {
		var f = this.cancelCallback.createDelegate(this);
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
	cancelCallback : function(successfullyCancelled) {

		this.stopProgress();
		this.fireEvent('cancel', successfullyCancelled);

		if (!successfullyCancelled) {
			Ext.Msg.alert("Couldn't cancel",
					"Sorry, the job couldn't be cancelled; perhaps it finished or was cancelled already?");
		} else {
			this.showAllMessages("Job was cancelled");
		}
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
	},
	
	hideLogsAndCancelButtons : function(){
		
		Ext.getCmp("progresslogsbutton").setVisible(false);
		Ext.getCmp("progresscancelbutton").setVisible(false);
	}

});