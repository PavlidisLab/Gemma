

function doUpdate( id ) {
	var callParams = [];
	callParams.push(id);
	
	var delegate = updateDone.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);
	
	callParams.push({callback : delegate, errorHandler : errorHandler  });
	
	BibliographicReferenceController.update.apply(this, callParams);
	Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/default/tree/loading.gif' });  
	Ext.DomHelper.append("messages", {tag : 'span', html : "&nbsp;Please wait..." });  
	
};

function updateDone(data) {
	Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/icons/ok.png' });  
	Ext.DomHelper.append("messages", {tag : 'span', html : "&nbsp;Updated" });
};

function handleFailure(data, e) {
	Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/icons/warning.png' });  
	Ext.DomHelper.append("messages", {tag : 'span', html : "&nbsp;There was an error: " + data });  
};