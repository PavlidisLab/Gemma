function refreshProgress() {
	HttpProgressMonitor.getProgressStatus(updateProgress);
}
var previousMessage;

function updateProgress(data) {
     //document.getElementById("progressBarText").innerHTML = data.description;
   if (previousMessage != data.description) {
		previouseMessage = data.description;

		document.getElementById("progressTextArea").value += data.description + "\n";	
   	document.getElementById("progressTextArea").scrollTop = document.getElementById("progressTextArea").scrollHeight;
	}
	
	if (data.done && data.forwardingURL != null) {
			redirect( data.forwardingURL );
	} else {
		window.setTimeout("refreshProgress()", 1000);
	}
	
	return true;
}

function redirect(url) {
   window.location = url;
}

function startProgress() {
	document.getElementById("progressBar").style.display = "block";
	document.getElementById("progressTextArea").value = "Monitoring Progress...";
   

    // wait a little while to make sure the progress job has started ..
	progressMotion();
	window.setTimeout("refreshProgress()", 800);
	return true;
}
function createProgressBar() {
	document.write(" <div id=\"progressBar\" style=\"display:none;\"> <div id=\"theMeter\">	<div id=\"progressBarText\"><textarea id=\"progressTextArea\" name=\"\" rows=5 cols=60 readonly=true> </textarea>	</div><div id=\"progressBarBox\"><div id=\"progressBarBoxContent\"></div>	</div>	</div>	</div>	<form> <input type=\"hidden\" name=\"taskId\" />		</form> ");
}
function moveProgress(count) {
	document.getElementById("progressBarBoxContent").style.width = parseInt(count * 35) + "px";
}
function progressMotion() {

   //move forward
	for (i = 0; i < 10; i++) {
		window.setTimeout("moveProgress(" + i + ")", 50 * i);
	}
	
	//move backwards
	for (j = 10; j > -1; j--) {
		window.setTimeout("moveProgress(" + j + ")", (50 * (10 - j)) + 500);
	}
	window.setTimeout("progressMotion()", 1000);
}

