function reset(data) {

}

function handleSuccess(data) {
	Ext.DomHelper.overwrite("messages", {
		tag : 'div',
		html : data
	});
}

function handleUsabilitySuccess(data, accession) {

	if (data) {
		Ext.DomHelper.overwrite( accession + "-rating", {
		tag : 'img',
		src : '/Gemma/images/icons/thumbsup.png'
		});
	} else {
	Ext.DomHelper.overwrite( accession + "-rating", {
		tag : 'img',
		src : '/Gemma/images/icons/thumbsdown-red.png'
		});
	}

	
}

function handleFailure(data, e) {
	Ext.DomHelper.overwrite("taskId", "");
	Ext.DomHelper.overwrite("messages", {
		tag : 'img',
		src : '/Gemma/images/icons/warning.png'
	});
	Ext.DomHelper.append("messages", {
		tag : 'span',
		html : "&nbsp;There was an error: " + data
	});
}

function toggleUsability(accession) {
	var callParams = [];
	callParams.push(accession);

	var delegate = handleUsabilitySuccess.createDelegate(this, [accession], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);

	callParams.push({
		callback : delegate,
		errorHandler : errorHandler
	});

	GeoBrowserService.toggleUsability.apply(this, callParams);
	Ext.DomHelper.overwrite(accession + "-rating", {
		tag : 'img',
		src : '/Gemma/images/default/tree/loading.gif'
	});
}

function showDetails(accession) {
	var callParams = [];
	callParams.push(accession);

	var delegate = handleSuccess.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);

	callParams.push({
		callback : delegate,
		errorHandler : errorHandler
	});

	GeoBrowserService.getDetails.apply(this, callParams);
	Ext.DomHelper.overwrite("messages", {
		tag : 'img',
		src : '/Gemma/images/default/tree/loading.gif'
	});
	Ext.DomHelper.append("messages", {
		tag : 'span',
		html : "&nbsp;Please wait..."
	});

}

function handleLoadSuccess(taskId) {
	try {
		Ext.DomHelper.overwrite("messages", "");

		var p = new Gemma.ProgressWidget({
					taskId : taskId
				});

		var w = new Ext.Window({
					modal : true,
					closable : false,
					width : 500,
					items : [p],
					buttons : [{
								id : 'cancel-button',
								handler : function() {
									p.cancelJob();
								},
								text : 'Cancel'
							}]
				});

		p.on('done', function(payload) {
					// this.onDoneLoading(payload);
					w.hide('upload-button');
					w.destroy();
					p.destroy();
					document.location.reload(true); // user will get a warning, but that's okay.
					Ext.DomHelper.overwrite("messages", "Successfully loaded.");
				}.createDelegate(this));

		p.on('fail', function(payload) {
					w.hide('upload-button');
					w.destroy();
					p.destroy();
					handleFailure(payload);
				});

		p.on('cancel', function() {
					w.hide();
					w.destroy();
					p.destroy();
					reset();
				});

		w.show('upload-button');

		p.startProgress();
	} catch (e) {
		handleFailure(data, e);
		return;
	}
};
 

function load(accession) {

	var dh = Ext.DomHelper;
	var suppressMatching = "false";
	var loadPlatformOnly = "false";
	var arrayExpress = "false";
	var arrayDesign = "";

	var callParams = [];

	var commandObj = {
		accession : accession,
		suppressMatching : suppressMatching,
		loadPlatformOnly : loadPlatformOnly,
		arrayExpress : arrayExpress,
		arrayDesignName : arrayDesign
	};

	callParams.push(commandObj);

	var delegate = handleLoadSuccess.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);

	callParams.push({
		callback : delegate,
		errorHandler : errorHandler
	});

	// this should return quickly, with the task id.
	Ext.DomHelper.overwrite("messages", {
		tag : 'img',
		src : '/Gemma/images/default/tree/loading.gif'
	});
	Ext.DomHelper.append("messages", "&nbsp;Submitting job...");
	ExpressionExperimentLoadController.run.apply(this, callParams);

}
