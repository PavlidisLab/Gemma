function updateReport(id) {

	var callParams = [];
	
	var commandObj = {id : id};

	callParams.push(commandObj);
	
	var delegate = handleReportUpdateSuccess.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);
	
	callParams.push({callback : delegate, errorHandler : errorHandler  });
	
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
	Ext.DomHelper.append("messages", {tag : 'span', html : "&nbsp;There was an error:<br/>" + data + e });  
};

function reset(data) {
	uploadButton.enable();
}

function handleReportUpdateSuccess(data) {
	try {
		taskId = data;
		Ext.DomHelper.overwrite("messages", "");  
		Ext.DomHelper.overwrite("taskId", "<input type = 'hidden' name='taskId' id='taskId' value= '" + taskId + "'/> ");
		var p = new progressbar();
	 	p.createIndeterminateProgressBar();
		p.on('fail', handleFailure);
		p.on('cancel', reset);
		p.on('done', handleDoneUpdateReport);
	 	p.startProgress();
	}
	catch (e) {
		handleFailure(data, e);
		return;
	}
	
};

function handleReportLoadSuccess(data) {
	try {
	    Ext.DomHelper.overwrite("messages", "");
	    var arrayDesignSummaryDiv = "arraySummary_" + data.id;
	    if (Ext.get(arrayDesignSummaryDiv) != null) {
			Ext.DomHelper.overwrite(arrayDesignSummaryDiv, data.html);
		}  
	} catch (e) {
		handleFailure(data, e);
		return;
	}
};

function handleDoneUpdateReport(data){
	Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/default/tree/loading.gif' }); 
	var id = data.id;
	var callParams = [];
	var commandObj = {id : id};
	callParams.push(commandObj);
	var callback = handleReportLoadSuccess.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);
	callParams.push(callback);
	callParams.push(errorHandler);
	ArrayDesignController.getReportHtml.apply( this, callParams);
	
	 
};