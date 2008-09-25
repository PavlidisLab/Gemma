Ext.namespace('Gemma');

/**
 * Config options:
 * 
 * @param taskId
 * @class Gemma.ProgressWidget
 * @extends Ext.Panel
 */
Gemma.ProgressWidget = Ext.extend(Ext.Panel, {
			BAR_UPDATE_INTERVAL : 2000,
			allMessages : "",
			previousMessage : '',
			timeoutid : null,
			waiting : false,
			initComponent : function(config) {

				this.progressBar = new Ext.ProgressBar({
							text : "Initializing ..."
						});

				Ext.apply(this, {
							items : [this.progressBar]
						});

				Gemma.ProgressWidget.superclass.initComponent.call(this);

				Ext.apply(this, config);

				this.addEvents('done', 'fail', 'cancel');
			},

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
				var errorHandler = this.handleFailure.createDelegate(this);
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
					this.previousMessage = messages;
					this.progressBar.updateText(messages);
					this.allMessages = this.allMessages + messages;
					// document.getElementById("progressTextArea").innerHTML += messages;
				} else {
					this.progressBar.updateText(this.progressBar.text + '.');
				}
			},

			/*
			 * Private Send a cancel notification to the server.
			 */
			cancelJob : function() {
				this.waiting = false;
				document.getElementById("progressTextArea").innerHTML = "Cancelling...";
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
				this.stopProgress();
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