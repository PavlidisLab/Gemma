        
function refreshProgress() {
var i=0;
var j=10;
	for(i=0; i<10; i++) 
		window.setTimeout("moveProgress(i)", 50);
	
	for(j=10; j>0; j--)	
		window.setTimeout("moveProgress(j)", 50);
		 
    HttpProgressMonitor.getProgressStatus(updateProgress);
}
function updateProgress( data ) {

    document.getElementById("progressBarText").innerHTML = data.description + "  :" + data.percent + "%"; 
    
    if (data.done) {
      document.getElementById("uploadbutton").disabled = false;
     }
     else{
        document.getElementById("uploadbutton").disabled = true;
        window.setTimeout("refreshProgress()", 200);
    }
    return true;
}

function startProgress() {
    document.getElementById("progressBar").style.display = "block";
    document.getElementById("progressBarText").innerHTML = "In progress...";
    document.getElementById("uploadbutton").disabled = true;

    // wait a little while to make sure the upload has started ..
    window.setTimeout("refreshProgress()", 500);
    return true;
}


function createProgressBar() {

 document.write('<div id="progressBar" style="display: none;"> <div id="theMeter">  <div id="progressBarText"></div>   <div id="progressBarBox">  <div id="progressBarBoxContent"></div>  </div>  </div>  </div>');       
}

function moveProgress(count) {
document.getElementById("progressBarBoxContent").style.width = parseInt(count * 35) + "px"
}

