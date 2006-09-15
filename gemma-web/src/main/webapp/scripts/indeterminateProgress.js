        
function refreshProgress() {  
    HttpProgressMonitor.getProgressStatus(updateProgress);
}

function updateProgress( data ) {

    document.getElementById("progressBarText").innerHTML = data.description + "  :" + data.percent + "%"; 
    
    if (data.done) {
      //document.getElementById("uploadbutton").disabled = false;
     }
     else{
        //document.getElementById("uploadbutton").disabled = true;
        window.setTimeout("refreshProgress()", 1000);
    }
    return true;
}

function startProgress() {
    document.getElementById("progressBar").style.display = "block";
    document.getElementById("progressBarText").innerHTML = "In progress...";
    //document.getElementById("uploadbutton").disabled = true;

    // wait a little while to make sure the upload has started ..
    progressMotion();
    window.setTimeout("refreshProgress()", 500);
    return true;
}


function createProgressBar() {

 document.write('<div id="progressBar" style="display: none;"> <div id="theMeter">  <div id="progressBarText"></div>   <div id="progressBarBox">  <div id="progressBarBoxContent"></div>  </div>  </div>  </div>');       
}

function moveProgress(count) {
document.getElementById("progressBarBoxContent").style.width = parseInt(count * 35) + "px"
}

function progressMotion(){
  	for(i=0; i<10; i++) {
		window.setTimeout("moveProgress(" + i + ")", 50*i);
		}
		
	for(j=10; j>-1; j--){	
		window.setTimeout("moveProgress("+ j +")", (50* (10-j)) + 500);
		 }

	window.setTimeout("progressMotion()", 1000);
}
