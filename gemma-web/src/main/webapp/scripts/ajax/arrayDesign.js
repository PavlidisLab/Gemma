function updateReport(id) {

	var callParams = [];
	
	var commandObj = {id : id};

	callParams.push(commandObj);
	
	var delegate = handleReportUpdateSuccess.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);
	
	callParams.push({callback : delegate, errorHandler : errorHandler  });
	
	// this should return quickly, with the task id.
	Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/default/tree/loading.gif' });  
	Ext.DomHelper.append("messages", "&nbsp;Submitting ...");  
	
	ArrayDesignController.updateReport.apply(this, callParams);

};

function remove(id) {
	alert("Are you sure?");
}


function handleFailure(data, e) {
	Ext.DomHelper.overwrite("taskId", "");
	Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/icons/warning.png' });  
	Ext.DomHelper.append("messages", {tag : 'span', html : "&nbsp;There was an error:<br/>" + data });  
	uploadButton.enable();
};

function reset(data) {
	uploadButton.enable();
}

function handleReportUpdateSuccess(data) {
// todo: refresh the div containing the report information
	try {
		taskId = data;
		Ext.DomHelper.overwrite("messages", "");  
		Ext.DomHelper.overwrite("taskId", "<input type = 'hidden' name='taskId' id='taskId' value= '" + taskId + "'/> ");
		var p = new progressbar();
	 	p.createIndeterminateProgressBar();
		p.on('fail', handleFailure);
		p.on('cancel', reset);
		p.on('done', handleDone);
	 	p.startProgress();
	}
	catch (e) {
		handleFailure(data, e);
		return;
	}
	
};

function handleDone(data){
		Ext.DomHelper.overwrite("messages", "");  
		Ext.DomHelper.overwrite("taskId", "");
	alert("Hello");
};