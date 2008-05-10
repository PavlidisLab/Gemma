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

function reset(data) {
	uploadButton.enable();
}

function handleReportLoadSuccess(data) {
	try {
		Ext.DomHelper.overwrite("messages", "");
		var arrayDesignSummaryDiv = "arraySummary_" + data.id;
		if (Ext.get(arrayDesignSummaryDiv) !== null) {
			Ext.DomHelper.overwrite(arrayDesignSummaryDiv, data.html);
		}
	} catch (e) {
		handleFailure(data, e);
		return;
	}
}

function handleDoneUpdateReport(data) {
	Ext.DomHelper.overwrite("messages", {
		tag : 'img',
		src : '/Gemma/images/icons/ok.png'
	});
	var id = data.id;
	var callParams = [];
	var commandObj = {
		id : id
	};
	callParams.push(commandObj);
	var callback = handleReportLoadSuccess.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);
	callParams.push(callback);
	callParams.push(errorHandler);
	ArrayDesignController.getReportHtml.apply(this, callParams);

}

function handleNewAlternateName(data) {
	Ext.DomHelper.overwrite("messages", "");
	Ext.DomHelper.overwrite("alternate-names", data);
}

function handleReportUpdateSuccess(data) {
	try {
		taskId = data;
		Ext.DomHelper.overwrite("messages", "");
		Ext.DomHelper.overwrite("taskId",
				"<input type = 'hidden' name='taskId' id='taskId' value= '"
						+ taskId + "'/> ");
		var p = new progressbar();
		p.createIndeterminateProgressBar();
		p.on('fail', handleFailure);
		p.on('cancel', reset);
		p.on('done', handleDoneUpdateReport);
		p.startProgress();
	} catch (e) {
		handleFailure(data, e);
		return;
	}

}

function updateReport(id) {

	var callParams = [];

	var commandObj = {
		id : id
	};

	callParams.push(commandObj);

	var delegate = handleReportUpdateSuccess.createDelegate(this, [], true);
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

	ArrayDesignController.updateReport.apply(this, callParams);

}

function getAlternateName(id) {
	var dialog = new Ext.Window({
		title : "Enter a new alternate name",
		modal : true,
		layout : 'fit',
		autoHeight : true,
		width : 300,
		closeAction : 'hide',
		easing : 3,
		defaultType : 'textfield',
		items : [{
			id : "alternate-name-textfield",
			fieldLabel : 'Name',
			name : 'name'
		}],

		buttons : [{
			text : 'Cancel',
			handler : function() {
				dialog.hide();
			}
		}, {
			text : 'Save',
			handler : function() {
				var name = Ext.get("alternate-name-textfield").getValue();
				addAlternateName(id, name);
				dialog.hide();
			},
			scope : dialog
		},]

	});

	dialog.show();

}

function addAlternateName(id, newName) {

	var callParams = [];

	callParams.push(id, newName);

	var delegate = handleNewAlternateName.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);

	callParams.push({
		callback : delegate,
		errorHandler : errorHandler
	});

	Ext.DomHelper.overwrite("messages", {
		tag : 'img',
		src : '/Gemma/images/default/tree/loading.gif'
	});

	ArrayDesignController.addAlternateName.apply(this, callParams);

}

function remove(id) {
	alert("Are you sure?");
}
