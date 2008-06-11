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

function handleDoneGeneratingFile(data) {
	// Ext.DomHelper.overwrite("messages", "");
	// var url = data.url;
	// Now fetch the url that the server has given us.
}

/*
 * Handler for after the data file creation has been initiated.
 */
function handleStartSuccess(taskId) {
	try {
		Ext.DomHelper.overwrite("messages", "");
		var p = new progressbar({
			taskId : taskId,
			doForward : true
		});
		p.createIndeterminateProgressBar();
		p.on('fail', handleFailure);
		p.on('done', handleDoneGeneratingFile);
		p.startProgress();
	} catch (e) {
		alert("error: " + e);
		handleFailure(data, e);
		return;
	}
}

function fetchData(filter, eeId, formatType, qtId, eeDId) {

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