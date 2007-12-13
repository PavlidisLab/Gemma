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
	Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/icons/warning.png' });  
	Ext.DomHelper.append("messages", {tag : 'span', html : "&nbsp;There was an error: " + data });  
};