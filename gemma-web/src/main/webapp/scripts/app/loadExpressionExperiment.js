var uploadButton;

Ext.onReady(function() {
			uploadButton = new Ext.Button("upload-button", {
						text : "Start loading",
						width : 100

					});
			uploadButton.on("click", submitForm);
		});

function submitForm() {

	var dh = Ext.DomHelper;
	var accession = Ext.get("accession").dom.value;
	var suppressMatching = Ext.get("suppressMatching").dom.checked;
	var loadPlatformOnly = Ext.get("loadPlatformOnly").dom.checked;
	var splitByPlatform = Ext.get("splitByPlatform").dom.checked;
	var allowSuperSeriesLoad = Ext.get("allowSuperSeriesLoad").dom.checked;
	var arrayExpress = Ext.get("arrayExpress").dom.checked;
	var arrayDesign = Ext.get("arrayDesign").dom.value;

	var callParams = [];

	var commandObj = {
		accession : accession,
		suppressMatching : suppressMatching,
		loadPlatformOnly : loadPlatformOnly,
		splitByPlatform : splitByPlatform,
		arrayExpress : arrayExpress,
		allowSuperSeriesLoad : allowSuperSeriesLoad,
		arrayDesignName : arrayDesign
	};

	callParams.push(commandObj);

	var delegate = handleSuccess.createDelegate(this, [], true);
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

	uploadButton.disable();

	ExpressionExperimentLoadController.run.apply(this, callParams);

};

function handleFailure(data, e) {
	Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/icons/warning.png'
			});
	Ext.DomHelper.append("messages", {
				tag : 'span',
				html : "&nbsp;There was an error while loading data:<br/>" + data
			});
	uploadButton.enable();
};

function reset(data) {
	uploadButton.enable();
}

function handleSuccess(taskId) {
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
					window.location = payload;
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