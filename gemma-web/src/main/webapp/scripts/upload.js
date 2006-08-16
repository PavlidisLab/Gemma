/*
*
*   Original code from  Pierre-Alexandre Losson -- http://www.telio.be/blog
*   email : plosson@users.sourceforge.net
* 
* $Id$
*/

function refreshProgress() {
    updateProgress( HttpProgressMonitor.getProgressStatus() );
}

function updateProgress(progressData) {

 
    if (!progressData.isDone) {
        document.getElementById("uploadbutton").disabled = true;
        document.getElementById("file").disabled = true;
        document.getElementById("progressBarText").innerHTML =  progressData.getDescription() + " : " + progressData.getPercent() + "% ";
        document.getElementById("progressBarBoxContent").style.width = parseInt(progressData.getPercent() * 3.5) + "px";
        window.setTimeout("refreshProgress()", 1000);
    } else {
        document.getElementById("uploadbutton").disabled = false;
        document.getElementById("file").disabled = false;
        document.getElementById("progressBarText").innerHTML =  progressData.getDescription() + " : " + progressData.getPercent() + "% ";
        document.getElementById("progressBarBoxContent").style.width = parseInt(progressData.getPercent() * 3.5) + "px";
       
    }
    return true;
}
function startProgress() {
    document.getElementById("progressBar").style.display = "block";
    document.getElementById("progressBarText").innerHTML = "Progress: 0%";
    document.getElementById("uploadbutton").disabled = true;

    // wait a little while to make sure the upload has started ..
    window.setTimeout("refreshProgress()", 1200);
    return true;
}

