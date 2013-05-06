function handleFailure(data, e) {
	
	Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/icons/warning.png'
			});
	Ext.DomHelper.append("messages", {
				tag : 'span',
				html : "&nbsp;There was an error:<br/>" + data + " " + (e ? e : "")
			});
	
	Ext.MessageBox.alert("Error", data + " ");
	
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

	// Get the parameters from the form.
	var commandObj = {
		quantitationTypeId : qtId,
		filter : filter,
		expressionExperimentId : eeId,
		format : formatType,
		experimentalDesignId : eeDId
	};

	Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/default/tree/loading.gif'
			});
	Ext.DomHelper.append("messages", "&nbsp;Fetching ...");
	
	ExpressionExperimentDataFetchController.getDataFile(commandObj, {
        callback : function(taskId) {
           var task = new Gemma.ObservableSubmittedTask({
                 'taskId' : taskId
              });
           
           task.on('task-completed', function(url) {
        	   handleDoneGeneratingFile(url);
            });
           
           task.showTaskProgressWindow({});
           
        },
        errorHandler : handleFailure
     });

}



function fetchCoExpressionData( eeId ) {

	Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/default/tree/loading.gif'
			});
	Ext.DomHelper.append("messages", "&nbsp;Fetching ...");
	
	ExpressionExperimentDataFetchController.getCoExpressionDataFile(eeId, {
        callback : function(taskId) {
           var task = new Gemma.ObservableSubmittedTask({
                 'taskId' : taskId
              });
           
           task.on('task-completed', function(url) {
        	   handleDoneGeneratingFile(url);
            });
           
           task.showTaskProgressWindow({});
           
        },
        errorHandler : handleFailure
     });
}


function fetchDiffExpressionData(analysisId) {

	Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/default/tree/loading.gif'
			});
	Ext.DomHelper.append("messages", "&nbsp;Fetching ...");
	
	ExpressionExperimentDataFetchController.getDiffExpressionDataFile(analysisId, {
        callback : function(taskId) {
           var task = new Gemma.ObservableSubmittedTask({
                 'taskId' : taskId
              });
           
           task.on('task-completed', function(url) {
        	   handleDoneGeneratingFile(url);
            });
           
           task.showTaskProgressWindow({});
           
        },
        errorHandler : handleFailure
     });

}
