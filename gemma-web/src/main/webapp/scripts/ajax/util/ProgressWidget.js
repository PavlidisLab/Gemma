Ext.namespace('Gemma');

/**
 * Config options:
 * 
 * @param taskId
 * @class Gemma.ProgressWidget
 * @extends Ext.Panel
 */
Gemma.ProgressWidget = Ext.extend(Ext.Panel, {

			messages : "",
			previousMessage : '',

			initComponent : function(config) {

				this.progressBar = new Ext.ProgressBar({
							text : "Initializing ...",
							width : 400,
							height : 60
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

				this.progressBar.updateText("Starting job ...");
				this.progressBar.show();

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
				 * Call it immediately
				 */
				this.refreshProgress(callback, errorHandler);

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
			},

			/*
			 * Private Callback for DWR @param {Object} data
			 */
			updateProgress : function(data) {
				this.waiting = false;
				this.updateIndeterminateProgress(data);
			},

			maybeDoForward : function(url) {
				if (this.doForward && url) {
					window.location = url;
				}
			},

			updateIndeterminateProgress : function(data) {

				var messages = "";
				if (data.push) {
					for (var i = 0, len = data.length; i < len; i++) {
						var d = data[i];
						messages = messages + "<br/>" + d.description;
						if (d.failed) {
							this.stopProgress();
							return this.handleFailure(d);
						} else if (d.done) {
							this.fireEvent('done', d.payload);
							this.stopProgress();
							if (this.doFoward && d.forwardingURL !== undefined && d.forwardingURL !== null) {
								window.location = d.forwardingURL + "?taskId=" + this.taskId;
							} else {
								var callback = this.maybeDoForward.createDelegate(this, [], true);
								var errorHandler = this.handleFailure.createDelegate(this, [], true);
								TaskCompletionController.checkResult(this.taskId, {
											callback : callback,
											errorHandler : errorHandler
										});
							}
							return;
						}
					}
				} else {
					if (data.done) {
						this.stopProgress();
						return this.handleFailure(data);
					} else if (d.done) {
						this.fireEvent('done', data.payload);
						this.stopProgress();
					}
				}

				if (!document.getElementById("progressTextArea"))
					return;

				if (this.previousMessage != messages && messages.length > 0) {
					this.previousMessage = messages;

					this.progressBar.updateText(messages);
					this.messages = this.messages + messages;
					document.getElementById("progressTextArea").innerHTML += messages;
				} else {
					this.progressBar.updateText(this.progressBar + '.');
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