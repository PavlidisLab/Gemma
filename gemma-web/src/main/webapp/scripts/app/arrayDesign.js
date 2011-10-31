Ext.namespace('Gemma');
Ext.onReady(function() {

			var panel = Ext.TabPanel({

						renderTo : 'arrayDesign',
						activeTab : 0,
						items : [{
									title : 'Information'
								}, {
									title : 'Probes'
								}, {
									title : 'Experiments'
								}]

					});

		});

function handleFailure(data, e) {
	reportFeedback("error", data, e);
}

function reset(data) {
	uploadButton.enable();
}

// array designs grid page doesn't have a 'messages' div
function reportFeedback(type, text, e){
	var messagesDiv = (Ext.get("messages") !== null);
	if(type === "error"){
		if(messagesDiv){
			Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/icons/warning.png'
			});
			Ext.DomHelper.append("messages", {
				tag : 'span',
				html : "&nbsp;There was an error:<br/>" + text + e
			});
		}else{
			Ext.Msg.show({
			   title:'Error',
			   msg: "There was an error:<br/>" + text + e,
			   buttons: Ext.Msg.OK,
			   icon: Ext.MessageBox.WARNING
			});
		}
		
	}else if(type === "loading"){
		if (messagesDiv) {
			Ext.DomHelper.overwrite("messages", {
				tag : 'img',
				src : '/Gemma/images/default/tree/loading.gif'
			});
		}
	}else if(type === "success"){
		if (messagesDiv) {
			Ext.DomHelper.overwrite("messages", "");
		}
	}
}

function handleReportLoadSuccess(data) {
	try {
		reportFeedback("success");
		var arrayDesignSummaryDiv = "arraySummary_" + data.id;
		if (Ext.get(arrayDesignSummaryDiv) !== null) {
			Ext.DomHelper.overwrite(arrayDesignSummaryDiv, data.html);
		}
	} catch (e) {
		handleFailure(data, e);
		return;
	}
}

function handleDoneUpdateReport(id) {

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
	reportFeedback("success");
	if(Ext.get("alternate-names") !== null){
		Ext.DomHelper.overwrite("alternate-names", data);
	}
}

function handleReportUpdateSuccess(taskId) {
	try {

		reportFeedback("success");
		var p = new progressbar({
					taskId : taskId
				});
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

function updateArrayDesignReport(id) {

	var callParams = [];
	callParams.push({
				id : id
			});
	callParams.push({
				callback : function(data) {
					var k = new Gemma.WaitHandler();
					k.handleWait(data, false);
					k.on('done', function(payload) {
								// this.fireEvent('reportUpdated', payload)
								handleDoneUpdateReport(id);
							});
				}.createDelegate(this)
			});

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
						}]

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

	reportFeedback("loading");

	ArrayDesignController.addAlternateName.apply(this, callParams);

}

function remove(id) {
	alert("Are you sure?");
}
