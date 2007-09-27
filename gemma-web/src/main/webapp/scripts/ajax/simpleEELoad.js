
var uploadButton;
var uploader = new FileUpload();
Ext.onReady(function () {
	uploader.makeUploadForm("file-upload");
	uploadButton = new Ext.Button("upload-button", {text:"Start loading"});
	uploadButton.on("click", submitForm);
});
function submitForm() {

	// first upload the file.
	uploader.startUpload();
	var name = Ext.get("name").dom.value;
	var shortName = Ext.get("shortName").dom.value;
	var description = Ext.get("description").dom.value;
	var arrayDesigns = Ext.get("arrayDesigns").dom.value;
	var quantitationTypeName = Ext.get("quantitationTypeName").dom.value;
	var quantitationTypeDescription = Ext.get("quantitationTypeDescription").dom.value;
	var scale = Ext.get("scale").dom.value;
	var type = Ext.get("type").dom.value;
	var isRatio = Ext.get("isRatio").dom.checked;
	//var file = Ext.get("dataFile.file").dom.value;
	var callParams = [];
	var commandObj = {name:name, shortName:shortName, description:description, arrayDesigns:arrayDesigns, quantitationTypeName:quantitationTypeName, scale:scale, type:type, isRatio:isRatio};
	callParams.push(commandObj);
	var delegate = handleSuccess.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);
	callParams.push({callback:delegate, errorHandler:errorHandler});
	
	//Ext.DomHelper.overwrite("messages", {tag:"img", src:"/Gemma/images/default/tree/loading.gif"});
//	Ext.DomHelper.append("messages", "&nbsp;Submitting job...");
	//	uploadButton.disable();
//	SimpleExpressionExperimentLoadController.load.apply(this, callParams);
}

function handleFailure(data, e) {
	Ext.DomHelper.overwrite("taskId", "");
	Ext.DomHelper.overwrite("messages", {tag:"img", src:"/Gemma/images/icons/warning.png"});
	Ext.DomHelper.append("messages", {tag:"span", html:"&nbsp;There was an error while loading data:<br/>" + data});
	uploadButton.enable();
}
function reset(data) {
	uploadButton.enable();
}
function handleSuccess(data) {
	try {
		taskId = data;
		Ext.DomHelper.overwrite("messages", "");
		Ext.DomHelper.overwrite("taskId", "<input type = 'hidden' name='taskId' id='taskId' value= '" + taskId + "'/> ");
		var p = new progressbar();
		p.createIndeterminateProgressBar();
		p.on("fail", handleFailure);
		p.on("cancel", reset);
		p.startProgress();
	}
	catch (e) {
		handleFailure(data, e);
		return;
	}
}

