
/*
* AJAX support for file uploads. Uploading a file is done with a multipartform. 
* $Id$
*/
FileUpload = function(form){
	Ext.QuickTips.init();
	this.formId = form;
	Ext.form.Field.prototype.msgTarget = 'side';
	Ext.BLANK_IMAGE_URL="/Gemma/images/s.gif";
    this.addEvents({
        finish : true,
        fail : true,
        start : true,
        cancel : true
    });
    var aform;
    FileUpload.superclass.constructor.call(this);
};

Ext.extend(FileUpload, Ext.util.Observable, {
	/*makeUploadForm : function (divid) {

		this.aform =  new Ext.form.Form({
   			fileUpload: true,
	        method: 'POST',
	        name: 'upload-form',
	        id: 'upload-form' ,
	        url: '/Gemma/uploadFile.html'
		});
	
		this.aform.add(new Ext.form.TextField({msgTarget: 'side',
	    	allowBlank:false,
	    	id: 'file',
	    	inputType: 'file',
	   	 	name: 'file',
	    	fieldLabel: 'File',
	    	blankText: 'You must choose a file' 
		}));
		this.aform.render(divid); 
		this.aform.el.dom.enctype="multipart/form-data";
		this.aform.el.dom.action='/Gemma/uploadFile.html';
	},*/

	handleFailure : function(data) {
		this.fireEvent('fail', data);
		Ext.DomHelper.overwrite("messages", {tag:"img", src:"/Gemma/images/icons/warning.png"});
		Ext.DomHelper.append("messages", {tag:"span", html:"&nbsp;" + data});
	},
	
	/*
	* After the upload is completed.
	*/
	handleResponse : function(form, success, response) {
		try {
			var o = Ext.decode(response.responseText);
		} catch (e) {
			handleFailure(e); // FIXME get exception from the response.
		}
		if (o === null ) {
			handleFailure("Unknown error");
		} else if (  !o.success ) {
			handleFailure(o.error);
		} else {
			this.fireEvent('finish', o);
		}
	},
	
	handleCancel : function(data) {
		this.fireEvent('cancel', data);
	},
	
	/*
	* Call after the upload has started successfully.
	*/
	handleSuccess : function(data) {
		try {
			Ext.DomHelper.overwrite("messages", "Upload started ...");
			var p = new progressbar();
			p.createDeterminateProgressBar();
			p.on("fail", this.handleFailure); 
			p.on('cancel', this.handleCancel);
			p.startProgress();
		} catch (e) {
			handleFailure(data, e);
			return;
		}
	},
	
	isValid : function() {
		return this.aform.isValid();
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
		Ext.Ajax.request({isUpload:true, url: '/Gemma/uploadFile.html', form:this.formId, callback : cb, clientValidation : 1});
		this.handleSuccess();
	
		//FileUploadController.upload.apply( this, callParams);
	
	
	}
	
});


