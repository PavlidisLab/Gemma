function submitForm() {
	var dh = Ext.DomHelper;
	var accession = Ext.get("accession").dom.value;
	var suppressMatching = Ext.get("suppressMatching").dom.checked;
	var loadPlatformOnly = Ext.get("loadPlatformOnly").dom.checked;
	
	var callParams = [];
	
	var commandObj = {accession: accession, suppressMatching: suppressMatching, loadPlatformOnly : loadPlatformOnly};
	
	callParams.push(commandObj);
	
	var delegate = handleSuccess.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true)
	
	callParams.push({callback : delegate, errorHandler : errorHandler  });
	
	// this should return quickly, with the task id.
	ExpressionExperimentFormController.run.apply(this, callParams);
	
};

function handleFailure(data, e) {
	console.log(e);
	Ext.DomHelper.overwrite(Ext.get("messages"), "there was an error");
};

function handleSuccess(data) {
	try {
		console.log(dwr.util.toDescriptiveString(data, 5));
		taskId = data;
		Ext.DomHelper.overwrite("taskId", "<input type = 'hidden' name='taskId' id='taskId' value= '" + taskId + "'/> ");
	 	createIndeterminateProgressBar();
	 	startProgress();
	}
	catch (e) {
		console.log(e);
		handleFailure(data, e);
		return;
	}
};