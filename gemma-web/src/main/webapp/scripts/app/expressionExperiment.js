function handleFailure(data, e) {
	Ext.DomHelper.overwrite("taskId", "");
	Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/icons/warning.png'
			});
	Ext.DomHelper.append("messages", {
				tag : 'span',
				html : "&nbsp;There was an error:<br/>" + data + e
			});
}

function handleSuccess(data, e) {
	Ext.DomHelper.overwrite("taskId", "");
	Ext.DomHelper.append("messages", {
				tag : 'span',
				html : "Done"
			});
}

function handleWait(data, forward) {
	try {
		taskId = data;
		Ext.DomHelper.overwrite("messages", "");

		var p = new progressbar({
					taskId : taskId,
					doForward : forward
				});
		p.createIndeterminateProgressBar();
		p.on('fail', handleFailure);
		p.on('done', handleSuccess);
		p.startProgress();
	} catch (e) {
		handleFailure(data, e);
		return;
	}
}

function updateEEReport(id) {
	var callParams = [];
	callParams.push(id);
	var delegate = handleWait.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);
	callParams.push({
				callback : delegate,
				errorHandler : errorHandler
			});
	Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/default/tree/loading.gif'
			});
	Ext.DomHelper.append("messages", "&nbsp;Submitting ...");
	ExpressionExperimentController.updateReport.apply(this, callParams);
}

function deleteExperiment(id) {
	// show confirmation dialog
	var dialog = new Ext.Window({
				title : "Confirm deletion",
				modal : true,
				layout : 'fit',
				autoHeight : true,
				width : 300,
				closeAction : 'hide',
				easing : 3,
				defaultType : 'textfield',
				items : [{
							xtype : 'label',
							text : "This cannot be undone"
						}],
				buttons : [{
							text : 'Cancel',
							handler : function() {
								dialog.hide();
							}
						}, {
							text : 'Confirm',
							handler : function() {
								dialog.hide();
								var callParams = []
								callParams.push(id);
								var delegate = handleWait.createDelegate(this, [], true);
								var errorHandler = handleFailure.createDelegate(this, [], true);
								callParams.push({
											callback : delegate,
											errorHandler : errorHandler
										});
								Ext.DomHelper.overwrite("messages", {
											tag : 'img',
											src : '/Gemma/images/default/tree/loading.gif'
										});
								Ext.DomHelper.append("messages", "&nbsp;Submitting ...");
								ExpressionExperimentController.deleteById.apply(this, callParams);
							},
							scope : dialog
						}]
			});

	dialog.show();
};
