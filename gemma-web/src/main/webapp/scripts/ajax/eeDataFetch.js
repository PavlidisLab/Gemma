function handleFailure(data, e) {
	Ext.DomHelper.overwrite("taskId", "");
	Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/icons/warning.png' });  
	Ext.DomHelper.append("messages", {tag : 'span', html : "&nbsp;There was an error:<br/>" + data + e });  
}

function handleDoneGeneratingFile( data ){
//	Ext.DomHelper.overwrite("messages", ""); 
//	var url = data.url;
	// Now fetch the url that the server has given us.
}

/*
 * Handler for after the data file creation has been initiated.
 */
function handleFetchSuccess(data) {
	try {
		taskId = data;
		Ext.DomHelper.overwrite("messages", "");  
		Ext.DomHelper.overwrite("taskId", "<input type = 'hidden' name='taskId' id='taskId' value= '" + taskId + "'/> ");
		var p = new progressbar({doForward : true });
	 	p.createIndeterminateProgressBar();
		p.on('fail', handleFailure);
		p.on('done', handleDoneGeneratingFile);
	 	p.startProgress();
	}
	catch (e) {
		handleFailure(data, e);
		return;
	}
}

function fetchData( filter, eeId, formatType, qtId, eeDId  ) {

	var callParams = [];
	
	// Get the parameters from the form.
	var commandObj = {quantitationTypeId : qtId, filter : filter, expressionExperimentId : eeId, format : formatType , experimentalDesignId : eeDId };

	callParams.push(commandObj);
	
	var delegate = handleFetchSuccess.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);
	
	callParams.push({callback : delegate, errorHandler : errorHandler  });
	
	Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/default/tree/loading.gif' });  
	Ext.DomHelper.append("messages", "&nbsp;Fetching ...");  
	
	ExpressionExperimentDataFetchController.getDataFile.apply(this, callParams);

}