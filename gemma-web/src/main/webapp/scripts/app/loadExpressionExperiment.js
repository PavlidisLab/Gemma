/**
 * Loading of data from GEO or ArrayExpress
 * @author paul
 * @version $Id$ 
 */
 
 var uploadButton;
var arrayDesignCombo;
Ext.onReady(function() {
			uploadButton = new Ext.Button({
						renderTo : "upload-button",
						text : "Start loading",
						width : 100

					});

			arrayDesignCombo = new Gemma.ArrayDesignCombo({
						renderTo : 'arrayDesignCombo',
						id : 'arrayDesign',
						width : 200
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
	var allowArrayExpressDesign = Ext.get("allowArrayExpressDesign").dom.checked;
	var arrayDesign = arrayDesignCombo.getArrayDesign();
	var arrayDesignName = null;
	if (arrayDesign) {
		arrayDesignName = arrayDesign.get("shortName");
	}

	var callParams = [];

	var commandObj = {
		accession : accession,
		suppressMatching : suppressMatching,
		loadPlatformOnly : loadPlatformOnly,
		splitByPlatform : splitByPlatform,
		arrayExpress : arrayExpress,
		allowSuperSeriesLoad : allowSuperSeriesLoad,
		arrayDesignName : arrayDesignName,
		allowArrayExpressDesign : allowArrayExpressDesign
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
				src : '/Gemma/images/loading.gif'
			});
	Ext.DomHelper.append("messages", "&nbsp;Submitting job...");

	uploadButton.disable();

	ExpressionExperimentLoadController.load.apply(this, callParams);

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