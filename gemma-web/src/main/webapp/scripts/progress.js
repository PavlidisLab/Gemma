
function refreshProgress() {
    HttpProgressMonitor.getProgressStatus(updateProgress);
}
function updateProgress( data ) {

    document.getElementById("progressBarText").innerHTML = data.description + " " + data.percent + "%"; 
    document.getElementById("progressBarBoxContent").style.width = parseInt(data.percent * 3.5) + "px";	

    if (data.done) {
      if (data.forwardingURL != null)
      		window.location = data.forwardingURL
     }
     else{
        
        window.setTimeout("refreshProgress()", 600);
    }
    return true;
}

function startProgress() {
    document.getElementById("progressBar").style.display = "block";
    document.getElementById("progressBarText").innerHTML = "In progress...";
    

    // wait a little while to make sure the job has started ..
    window.setTimeout("refreshProgress()", 1000);
    return true;
}

function createProgressBar() {

 document.write('<div id="progressBar" style="display: none;"> <div id="theMeter">  <div id="progressBarText"></div>   <div id="progressBarBox">  <div id="progressBarBoxContent"></div>  </div>  </div>  </div>');       
}
        

