function refreshProgress() {
	HttpProgressMonitor.getProgressStatus(updateProgress);
}
var determinate;

function updateProgress(data) {
 
 	if (determinate == 1)
 		updateDeterminateProgress(data);
 	else 
 		updateIndeterminateProgress(data);
 		
 	if (data.done && data.forwardingURL != null) {
			redirect( data.forwardingURL );
	} else {
		window.setTimeout("refreshProgress()", 1000);
	}
	
	return true;
}

function updateDeterminateProgress(data){
	document.getElementById("progressBarText").innerHTML = data.description + " " + data.percent + "%";
	document.getElementById("progressBarBoxContent").style.width = parseInt(data.percent * 3.5) + "px";
}

var previousMessage = "";
function updateIndeterminateProgress(data){
 		
   if (previousMessage != data.description) {
		previousMessage = data.description
		
		document.getElementById("progressTextArea").value += data.description + "\n";	
   	document.getElementById("progressTextArea").scrollTop = document.getElementById("progressTextArea").scrollHeight;
	}
	

}

function redirect(url) {
   window.location = url;
}

function startProgress() {
	document.getElementById("progressBar").style.display = "block";
	
   

   if (determinate == 0){
		progressMotion();
		document.getElementById("progressTextArea").value = "Monitoring Progress...";
	}	else
		document.getElementById("progressBarText").innerHTML = "Monitoring Progress...";
	
	
	window.setTimeout("refreshProgress()", 800);
	return true;
}
function createIndeterminateProgressBar() {
	determinate = 0;
	document.write(" <div id=\"progressBar\" style=\"display:none;\"> <div id=\"theMeter\">	<div id=\"progressBarText\"><textarea id=\"progressTextArea\" name=\"\" rows=5 cols=60 readonly=true> </textarea>	</div><div id=\"progressBarBox\"><div id=\"progressBarBoxContent\"></div>	</div>	</div>	</div>	<form> <input type=\"hidden\" name=\"taskId\" />		</form> ");
}

function crateDeterminateProgressBar(){
	determinate = 1;
	document.write("<div id=\"progressBar\" style=\"display: none;\"> <div id=\"theMeter\">  <div id=\"progressBarText\"></div>   <div id=\"progressBarBox\">  <div id=\"progressBarBoxContent\"></div>  </div>  </div>  </div>");
	
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

