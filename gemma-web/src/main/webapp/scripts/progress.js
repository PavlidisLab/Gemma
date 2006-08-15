/*
*
*   Original code from  Pierre-Alexandre Losson -- http://www.telio.be/blog
*   email : plosson@users.sourceforge.net
* 
* $Id$
*/

function refreshProgress() {
    progressMonitor.getUploadInfo(updateProgress);
}
function updateProgress(ProgressData data) {
    if (data.isDone) {
      document.getElementById("uploadbutton").disabled = false;
      document.getElementById("progressBarText").innerHTML = data.getDescription(); 	
     }
     else
        document.getElementById("uploadbutton").disabled = true;
        document.getElementById("progressBarText").innerHTML = "Upload in progress: " + data.getPercent() + "%";
        document.getElementById("progressBarBoxContent").style.width = parseInt(percent * 3.5) + "px";
        window.setTimeout("refreshProgress()", 1000);
    
    return true;
}
function startProgress() {
    document.getElementById("progressBar").style.display = "block";
    document.getElementById("progressBarText").innerHTML = "Job in progress: 0%";
    document.getElementById("uploadbutton").disabled = true;

    // wait a little while to make sure the upload has started ..
    window.setTimeout("refreshProgress()", 1500);
    return true;
}
