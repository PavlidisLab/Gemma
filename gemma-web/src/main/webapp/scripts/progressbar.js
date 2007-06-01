/**
 * Progressbar.js. To use this, create a div with id "progress-area" on your page, and a task id in a div with id "taskId". 
 * An optional update area "messages" may be used.
 * Arrange for createIndeterminateProgressBar or createDeterminateProgressBar to be called, followed by startProgress(). 
 * @author Kelsey
 * @author Paul
 * @version $Id$
 */

progressbar = function(){
    this.addEvents({
        finish : true,
        fail : true,
        cancel : true
    });
    progressbar.superclass.constructor.call(this);
	
	
};

Ext.extend(progressbar, Ext.util.Observable, {

	bar : null,
	determinate : false,
	previousMessage :  "",
	timeoutid : null,
	
		// time in ms between updates
	BAR_UPDATE_INTERVAL : 1000,
	
	handleFailure : function (data, e) {
		this.stopProgress();
		var messageArea = Ext.get("messages");
		if (messageArea) {
			var messageText;
			if (data.description) {
				messageText = data.description;
			} else if (!e) {
				messageText = data;
			}
	       this.fireEvent("fail", messageText);
		} else {
			this.fireEvent('fail');
		}
	},
	

	/**
	 * Start the progressbar in motion.
	 */
	startProgress : function () {
	   if (this.determinate == 0){
			document.getElementById("progressTextArea").innerHTML = "Starting job...";
		} else {
			document.getElementById("progressBarText").value = "Please wait...";
		}
		
		var callParams = [dwr.util.getValue("taskId")];
		var callback = this.updateProgress.createDelegate(this, [], true) ;
		var errorHandler = this.handleFailure.createDelegate(this, [], true) ;
		//callParams.push({callback : callback, errorHandler : errorHandler  });
		callParams.push(callback);
		callParams.push(errorHandler);
		var f = this.refreshProgress.createDelegate(this, callParams, false);
		
		this.timeoutid = setInterval(f, this.BAR_UPDATE_INTERVAL);
	},
	
	
	stopProgress : function () {
		window.clearTimeout(this.timeoutid);
		Ext.DomHelper.overwrite("progress-area", "");
	//	this.bar.stop();
		this.previousMessage = null;
	},
	
	
	/* Private
	 * Callback for DWR
	 * @param {Object} data
	 */
	updateProgress : function (data) {	 
	 	if (this.determinate == 1) {
	 		this.updateDeterminateProgress(data);
	 	} else { 
	 		this.updateIndeterminateProgress(data);
		}
	},
	
	updateDeterminateProgress : function (data){
		var messages = "";
		var percent = 0;
		if (data.push) {
			for(var i = 0, len = data.length; i < len; i++) {
				var d = data[i];
				messages = messages + "<br/>" + d.description;
				percent = d.percent; // use last value.
				if (d.failed) {
					return  this.handleFailure(d);
				} else if (d.done) {
					this.fireEvent('done');
					if (d.forwardingURL) {
					  	window.location = d.forwardingURL + "?taskId=" + dwr.util.getValue("taskId");
					  	return;
					}
				}
			}	
		} else {
			if (data.done) {
				return this.handleFailure(data);
			} else if (d.done) {
				this.fireEvent('done');
			}
			percent = data.percent;
		}
	
		document.getElementById("progressBarText").innerHTML = messages + " " + percent + "%";
		document.getElementById("progressBarBoxContent").style.width = parseInt(percent * 3.5) + "px";
	},

			
	updateIndeterminateProgress : function (data){

		var messages = "";
		var percent = 0;
		if (data.push) {
			for(var i = 0, len = data.length; i < len; i++) {
				var d = data[i];
				messages = messages + "<br/>" + d.description;
				percent = d.percent; // use last value.
				if (d.failed) {
					return this.handleFailure(d);
				} else if (d.done) {
					this.fireEvent('done');
					if (d.forwardingURL) {
				  		window.location = d.forwardingURL + "?taskId=" + dwr.util.getValue("taskId");
						return;
					}
				}
			}	
		} else {
			if (data.done) {
				return this.handleFailure(data);
			} else if (d.done) {
				this.fireEvent('done');
			}
			percent = data.percent;
		}
		
		if (!document.getElementById("progressTextArea")) return;
	
	   if (this.previousMessage != messages) {
			this.previousMessage = messages;
			document.getElementById("progressTextArea").innerHTML +=   messages;	
	   		document.getElementById("progressTextArea").scrollTop = document.getElementById("progressTextArea").scrollHeight;
		} else{
			document.getElementById("progressTextArea").innerHTML += ".";	
	   		document.getElementById("progressTextArea").scrollTop = document.getElementById("progressTextArea").scrollHeight;	
		}
	},

	
	/* Private
	 * Send a cancel notification to the server.
	 */
	cancelJob : function () {
		var taskId = dwr.util.getValue("taskId");
		if (this.determinate == 0){
			document.getElementById("progressTextArea").innerHTML = "Cancelling...";
		} else {
			document.getElementById("progressBarText").value = "Cancelling...";
		}
		var f =  this.cancelCallback.createDelegate(this, [], true);
		ProgressStatusService.cancelJob(taskId, f);
	},
	
	/* Private
	 * Check for status from server.
	 */
	refreshProgress : function (taskId, callback, errorHandler) {
		ProgressStatusService.getProgressStatus(taskId, {callback:callback, errorHandler:errorHandler});
	},
	
	/* Private
	 * Callback to handle cancellation
	 * @param {Object} data
	 */
	cancelCallback : function (data)  {
		this.stopProgress();
		var messageArea = Ext.get("messages");
		if (messageArea) {
			Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/icons/ok.png' });  
			Ext.DomHelper.append("messages", "&nbsp;Job was cancelled.");
		}
		this.fireEvent('cancel');	
		
	},
			
			
	/**
	 * Create a progress bar that has no fixed endpoint
	 */
	createIndeterminateProgressBar : function () {
		this.determinate = 0;
		this.bar= this.createIndeterminateBarDetails(300,15,'white',1,'black','#FF9933',85,4,3,"");
		f = this.cancelJob.createDelegate(this, [], true);
		Ext.get("cancel-button").on('click', f);
	},
	


	/**
	 * Create a progress bar that has a known endpoint
	 */
	createDeterminateProgressBar : function (){
		this.determinate = 1;
		var barHtml = '<div id="progressBar" style="display: none;"> <div id="theMeter">  <div id="progressBarText"></div>   <div id="progressBarBox">  <div id="progressBarBoxContent"></div>  </div>  </div>  </div>';
	 	Ext.DomHelper.overwrite("progress-area", barHtml);
	},
	
	 
	createIndeterminateBarDetails : function (w,h,bgc,brdW,brdC,blkC,speed,blocks,count,action){
		var t = '<div ><form style="float:left" ><input type="button" id="cancel-button" name="Cancel" value="cancel" /></form> ';
		t += '<img style="float:right" src="/Gemma/images/default/basic-dialog/progress2.gif" /></div>';
		var ipbHeader = '<div id="progressBar"><div id="progressBarText"> <div class="clob" style="width:350px;margin:10px;padding:4px;" id="progressTextArea"> </div>	</div> <div id="theMeter">	';
		var ipbFooter = '</div>	</div>	 ';
		Ext.DomHelper.overwrite("progress-area", ipbHeader + t + ipbFooter);
	} 
	
 
});

 

function startBar(bn){
	var ie=(document.all)?true:false;
	var t=(ie)?document.all['blocks'+bn]:document.getElementById('blocks'+bn);
	if(parseInt(t.style.left)+t.h+1-(t.blocks*t.h+t.blocks)>t.w){
		t.style.left=-(t.h*2+1)+'px';
		t.ctr++;
		if(t.ctr>=t.count){
			eval(t.action);
			t.ctr=0;
		}
	} else 
		t.style.left=(parseInt(t.style.left)+t.h+1)+'px';
} 
	
