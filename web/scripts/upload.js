
function refreshProgress() {
    UploadMonitor.getUploadInfo(updateProgress);
}
function updateProgress(uploadInfo) {
    if (uploadInfo.inProgress) {
        document.getElementById("uploadbutton").disabled = true;
        document.getElementById("file").disabled = true;
        var fileIndex = uploadInfo.fileIndex;
        var progressPercent = Math.ceil((uploadInfo.bytesRead / uploadInfo.totalSize) * 100);
        document.getElementById("progressBarText").innerHTML = "Upload in progress: " + progressPercent + "%";
        document.getElementById("progressBarBoxContent").style.width = parseInt(progressPercent * 3.5) + "px";
        window.setTimeout("refreshProgress()", 1000);
    } else {
        document.getElementById("uploadbutton").disabled = false;
        document.getElementById("file").disabled = false;
    }
    return true;
}
function startProgress() {
    document.getElementById("progressBar").style.display = "block";
    document.getElementById("progressBarText").innerHTML = "Upload in progress: 0%";
    document.getElementById("uploadbutton").disabled = true;

    // wait a little while to make sure the upload has started ..
    window.setTimeout("refreshProgress()", 1500);
    return true;
}

