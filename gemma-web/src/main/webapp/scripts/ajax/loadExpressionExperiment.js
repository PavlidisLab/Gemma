function submitForm() {
	
	// fixme: disable the submit button
	// fixme: show brief startup animation.

	var dh = Ext.DomHelper;
	var accession = Ext.get("accession").dom.value;
	var suppressMatching = Ext.get("suppressMatching").dom.checked;
	var loadPlatformOnly = Ext.get("loadPlatformOnly").dom.checked;
	
	var callParams = [];
	
	var commandObj = {accession: accession, suppressMatching: suppressMatching, loadPlatformOnly : loadPlatformOnly};
	
	callParams.push(commandObj);
	
	var delegate = handleSuccess.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);
	
	callParams.push({callback : delegate, errorHandler : errorHandler  });
	
	// this should return quickly, with the task id.
	ExpressionExperimentFormController.run.apply(this, callParams);
	
};

function handleFailure(data, e) {
	 Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/iconWarning.gif' });  
	 Ext.DomHelper.append("messages", {tag : 'span', html : "There was an error while loading data. Try again or contact the webmaster." });  
};

function handleSuccess(data) {
	try {
		taskId = data;
		Ext.DomHelper.overwrite("taskId", "<input type = 'hidden' name='taskId' id='taskId' value= '" + taskId + "'/> ");
	 	createIndeterminateProgressBar();
	 	startProgress();
	}
	catch (e) {
		handleFailure(data, e);
		return;
	}
};