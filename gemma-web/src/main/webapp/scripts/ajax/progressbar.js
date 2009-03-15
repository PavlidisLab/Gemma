/**
 * Progressbar.js.
 * 
 * 
 * To use:
 * 
 * Pass the taskId into the progrsesbar constructor OR (currently doesn't work) create a div with id "progress-area" on
 * your page, and a task id in a div with id "taskId".
 * 
 * An optional update area "messages" may be used. Arrange for createIndeterminateProgressBar or
 * createDeterminateProgressBar to be called, followed by startProgress().
 * 
 * @author Kelsey
 * @author Paul
 * @version $Id$
 */

progressbar = function(config) {

	Ext.apply(this, config);

	this.addEvents({
				finish : true,
				fail : true,
				cancel : true
			});

	progressbar.superclass.constructor.call(this);
};

Ext.extend(progressbar, Ext.util.Observable, {

	waiting : false,
	bar : null,
	determinate : false,
	previousMessage : "",
	timeoutid : null,
	detbarwidth : 200,

	// time in ms between updates
	BAR_UPDATE_INTERVAL : 4000,

	/**
	 * Used to handle failures of responses...
	 * 
	 * @param {}
	 *            data
	 * @param {}
	 *            e
	 */
	handleResponseFailure : function(data, e) {
		/*
		 * keep trying...
		 */
	},

	handleFailure : function(data, e) {
		this.stopProgress();
		var messageArea = Ext.get("messages");
		if (messageArea) {
			var messageText;
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
		} else {
			this.fireEvent('fail');
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

		// FIME: this doesn't work nor does Ext.get("taskId")
		// the returned value is always a blank string (from both)
		// The task id is being returned from the server (checked)
		// Perhaps is some dom refresh problem

	},

	/**
	 * Start the progressbar in motion.
	 */
	startProgress : function() {
		if (this.determinate == 0) {
			document.getElementById("progressTextArea").innerHTML = "Starting job...";
		} else {
			// document.getElementById("progressTextArea").value = "Please
			// wait...";
		}

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
		var f = this.refreshProgress.createDelegate(this, callParams, false);
		this.timeoutid = window.setInterval(f, this.BAR_UPDATE_INTERVAL);
	},

	stopProgress : function() {
		window.clearInterval(this.timeoutid);
		// Ext.DomHelper.overwrite("progress-area", "");
		this.previousMessage = null;
		this.waiting = false;
	},

	/*
	 * Private Callback for DWR @param {Object} data
	 */
	updateProgress : function(data) {
		this.waiting = false;
		if (this.determinate == 1) {
			this.updateDeterminateProgress(data);
		} else {
			this.updateIndeterminateProgress(data);
		}
	},

	updateDeterminateProgress : function(data) {
		var messages = "";
		var percent = 0;
		if (data.push) {
			for (var i = 0, len = data.length; i < len; i++) {
				var d = data[i];
				messages = messages + "<br/>" + d.description;
				delta = d.percent - percent;
				percent = d.percent; // use last value.

				if (delta > 5) {
					// document.getElementById("progressTextArea").innerHTML =
					// messages + " " + percent + "%";
					document.getElementById("progress-bar-box-content").style.width = parseInt(percent
							* (detbarwidth / 100))
							+ "px";
				}

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
						TaskCompletionController.checkResult(this.taskId, callback);
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
			percent = data.percent;
		}

		// document.getElementById("progressTextArea").innerHTML = messages + "
		// " + percent + "%";
		document.getElementById("progress-bar-box-content").style.width = parseInt(percent * this.detbarwidth / 100)
				+ "px";
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
			document.getElementById("progressTextArea").innerHTML += messages;
			document.getElementById("progressTextArea").scrollTop = document.getElementById("progressTextArea").scrollHeight;
		} else {
			document.getElementById("progressTextArea").innerHTML += ".";
			document.getElementById("progressTextArea").scrollTop = document.getElementById("progressTextArea").scrollHeight;
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
	 * Private Callback to handle cancellation @param {Object} data
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

	/**
	 * Create a progress bar that has no fixed endpoint
	 */
	createIndeterminateProgressBar : function() {
		this.determinate = 0;
		this.bar = this.createIndeterminateBarDetails();
		f = this.cancelJob.createDelegate(this, [], true);
		Ext.get("cancel-button").on('click', f);
	},

	/**
	 * Create a progress bar that has a known endpoint
	 */
	createDeterminateProgressBar : function() {
		this.determinate = 1;
		var div = '<div style="width:350px;margin:10px;">	<input style="float:left;margin:0px;" type="button" id="cancel-button" name="Cancel" value="Cancel job" />	<div id="progbar" style="margin:5px;width:'
				+ detbarwidth
				+ 'x;float:right;border:1px solid;">		<div id="progress-bar-box-content" style="float:left;height:10px;width:40px;">		</div>	</div></div>'
		Ext.DomHelper.overwrite("progress-area", div);
		f = this.cancelJob.createDelegate(this, [], true);
		Ext.get("cancel-button").on('click', f);
	},

	createIndeterminateBarDetails : function() {
		var div = '<div id="progressTextArea" class="clob" style="font-size:smaller;width:650px;margin:10px;padding:4px;" ><input type="textarea" /></div><div style="width:650px;"><input style="float:left" type="button" id="cancel-button" name="Cancel" value="Cancel job" /><img style="float:right" src="/Gemma/images/loading.gif" /></div>';
		Ext.DomHelper.overwrite("progress-area", div);
	}

});
