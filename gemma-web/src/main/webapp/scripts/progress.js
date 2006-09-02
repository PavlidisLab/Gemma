
function refreshProgress() {
    HttpProgressMonitor.getProgressStatus(updateProgress);
}
function updateProgress( data ) {

    document.getElementById("progressBarText").innerHTML = data.description + "  :" + data.percent + "%"; 
    document.getElementById("progressBarBoxContent").style.width = parseInt(data.percent * 3.5) + "px";	

    if (data.done) {
      document.getElementById("uploadbutton").disabled = false;
     }
     else{
        document.getElementById("uploadbutton").disabled = true;
        window.setTimeout("refreshProgress()", 600);
    }
    return true;
}

function startProgress() {
    document.getElementById("progressBar").style.display = "block";
    document.getElementById("progressBarText").innerHTML = "In progress...";
    document.getElementById("uploadbutton").disabled = true;

    // wait a little while to make sure the upload has started ..
    window.setTimeout("refreshProgress()", 1000);
    return true;
}

function createProgressBar() {

 document.write( '<div id="progressBar" style="display: none;"> <div id="theMeter">  <div id="progressBarText"></div>   <div id="progressBarBox">  <div id="progressBarBoxContent"></div>  </div>  </div>  </div>');
        
}