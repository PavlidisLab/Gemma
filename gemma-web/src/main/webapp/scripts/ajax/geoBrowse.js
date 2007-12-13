function showDetails( accession ) {
	var callParams = [];
	callParams.push(accession);
	
	var delegate = handleSuccess.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);
	
	callParams.push({callback : delegate, errorHandler : errorHandler  });
	
	GeoBrowserService.getDetails.apply(this, callParams);
	Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/default/tree/loading.gif' });  
	Ext.DomHelper.append("messages", {tag : 'span', html : "&nbsp;Please wait..." });  
	
};

function handleSuccess(data) {
	Ext.DomHelper.overwrite("messages", {tag : 'div', html:data });   
};

function handleFailure(data, e) {
	Ext.DomHelper.overwrite("taskId", "");
	Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/icons/warning.png' });  
	Ext.DomHelper.append("messages", {tag : 'span', html : "&nbsp;There was an error: " + data });  
};

function load(accession) {

	var dh = Ext.DomHelper;
	var suppressMatching = "false";
	var loadPlatformOnly = "false";
	var arrayExpress = "false";
	var arrayDesign = "";
	
	var callParams = [];
	
	var commandObj = {accession: accession, suppressMatching: suppressMatching, loadPlatformOnly : loadPlatformOnly, arrayExpress: arrayExpress, arrayDesignName: arrayDesign};

	callParams.push(commandObj);
	
	var delegate = handleLoadSuccess.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);
	
	callParams.push({callback : delegate, errorHandler : errorHandler  });
	
	// this should return quickly, with the task id.
	Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/default/tree/loading.gif' });  
	Ext.DomHelper.append("messages", "&nbsp;Submitting job...");  
	ExpressionExperimentLoadController.run.apply(this, callParams);
	
};


function handleLoadSuccess(data) {
	try {
		taskId = data;
		Ext.DomHelper.overwrite("messages", "");  
		Ext.DomHelper.overwrite("taskId", "<input type = 'hidden' name='taskId' id='taskId' value= '" + taskId + "'/> ");
		var p = new progressbar();
	 	p.createIndeterminateProgressBar();
		p.on('fail', handleFailure);
		p.on('cancel', reset);
	 	p.startProgress();
	}
	catch (e) {
		handleFailure(data, e);
		return;
	}
};