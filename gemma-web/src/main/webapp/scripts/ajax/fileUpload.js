
/*
* AJAX support for file uploads. Uploading a file is done with a multipartform. 
* $Id$
*/
FileUpload = function(){
	Ext.QuickTips.init();
    this.addEvents({
        finish : true,
        fail : true,
        cancel : true
    });
    dwr.engine.setActiveReverseAjax(true);
    FileUpload.superclass.constructor.call(this);
};

Ext.extend(FileUpload, Ext.util.Observable, {
	makeUploadForm : function (divid) {

		var aform =  new Ext.form.Form({
   			fileUpload: true,
	        method: 'POST',
	        name: 'upload-form',
	        id: 'upload-form',
	        url: '/Gemma/uploadFile.html'
		});
	
		aform.add(new Ext.form.TextField({msgTarget: 'side',
	    	allowBlank: false,
	    	id: 'file',
	    	inputType: 'file',
	   	 	name: 'file',
	    	fieldLabel: 'File',
	    	blankText: 'You must choose a file' 
		}));
		 
		aform.render(divid); 
		aform.el.dom.enctype="multipart/form-data";
		aform.el.dom.action='/Gemma/uploadFile.html';
	},


	handleFailure : function(data) {
		Ext.DomHelper.overwrite("messages", {tag:"img", src:"/Gemma/images/icons/warning.png"});
		Ext.DomHelper.append("messages", {tag:"span", html:"&nbsp;There was an error while uploading the file:<br/>" + data});
	},
	
	/*
	* After the upload is completed.
	*/
	//handleResponse : function(form, success, response) {
	//	Ext.DomHelper.append("messages", {tag:"span", html:"&nbsp;It worked"});
	//	var o = Ext.decode(response.responseText);
	//	if (o.success == "false") {
	//		handleFailure("(?)");
	//	}
		
	//},
	
	handleResponse : function(file) {
		Ext.DomHelper.append("messages", {tag:"span", html:"Uploaded to: " + file});
	},
	
	/*
	* Call after the upload has started successfully.
	*/
	handleSuccess : function(data) {
		try {
			Ext.DomHelper.overwrite("messages", "Upload started ...");
			var p = new progressbar();
			p.createDeterminateProgressBar();
			p.on("fail", handleFailure); 
			p.on('cancel', reset);
			p.on('done', handleResponse);
	//		p.startProgress();
		} catch (e) {
			handleFailure(data, e);
			return;
		}
	},
	
	startUpload :  function(divid) {
	 	//var successHandler = this.handleSuccess.createDelegate(this, [], true);
		//var errorHandler = this.handleFailure.createDelegate(this, [], true);
		
		var file = dwr.util.getValue("file");
		var callParams = [];
		var commandObj = {file : file};
		callParams.push(commandObj);
		var callback = handleSuccess.createDelegate(this, [], true);
		var errorHandler = handleFailure.createDelegate(this, [], true);
		callParams.push(callback);
		callParams.push(errorHandler);
		
		var cb = this.handleResponse.createDelegate(this,[], true);
		//	Ext.Ajax.request({form:'upload-form', callback : cb});
	
		FileUploadController.upload.apply( this, callParams);
	
		this.handleSuccess();
	},
	
});


