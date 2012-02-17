function handleFailure(data, e) {
	Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/icons/warning.png'
			});
	Ext.DomHelper.append("messages", {
				tag : 'span',
				html : "&nbsp;There was an error:<br/>" + data + " " + (e ? e : "")
			});
}

function handleDoneGeneratingFile(url) {
	window.location = url;
}

/*
 * Handler for after the data file creation has been initiated.
 */
function handleStartSuccess(taskId) {
	try {
		Ext.DomHelper.overwrite("messages", "");
		var p = new Gemma.ProgressWindow({
					taskId : taskId,
					callback : handleDoneGeneratingFile
				});
		p.show();
	} catch (e) {
		//console.log(e);
		handleFailure(data, e);
		return;
	}
}

function fetchData( filter, eeId, formatType, qtId, eeDId ) {

	var callParams = [];

	// Get the parameters from the form.
	var commandObj = {
		quantitationTypeId : qtId,
		filter : filter,
		expressionExperimentId : eeId,
		format : formatType,
		experimentalDesignId : eeDId
	};

	callParams.push(commandObj);

	// callback is just for initiating the process.
	var cb = handleStartSuccess;
	var errorHandler = handleFailure;

	callParams.push({
				callback : cb,
				errorHandler : errorHandler
			});

	Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/default/tree/loading.gif'
			});
	Ext.DomHelper.append("messages", "&nbsp;Fetching ...");

	ExpressionExperimentDataFetchController.getDataFile.apply(this, callParams);

}



function fetchCoExpressionData( eeId ) {

	var callParams = [];
	callParams.push(eeId);

	// callback is just for initiating the process.
	var cb = handleStartSuccess;
	var errorHandler = handleFailure;

	callParams.push({
				callback : cb,
				errorHandler : errorHandler
			});

	Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/default/tree/loading.gif'
			});
	Ext.DomHelper.append("messages", "&nbsp;Fetching ...");

	ExpressionExperimentDataFetchController.getCoExpressionDataFile.apply(this, callParams);
}


function fetchDiffExpressionData(analysisId) {

	var callParams = [];
	callParams.push(analysisId);

	// callback is just for initiating the process.
	var cb = handleStartSuccess;
	var errorHandler = handleFailure;

	callParams.push({
				callback : cb,
				errorHandler : errorHandler
			});

	Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/default/tree/loading.gif'
			});
	Ext.DomHelper.append("messages", "&nbsp;Fetching ...");

	ExpressionExperimentDataFetchController.getDiffExpressionDataFile.apply(this, callParams);

}
