
/**
 * Simple support for uploading a file.
 * 
 * @author Paul
 * @version $Id$
 */
Ext.namespace("Gemma");
Gemma.FileUploadForm = Ext.extend(Ext.Panel, {

			width : 500,
			autoHeight : true,

			reset : function() {
				Ext.getCmp('form-file').reset();
				Ext.getCmp('messages').setStatus('');
			},

			initComponent : function() {
				Ext.apply(this, {
							width : 500,
							frame : false,
							frame : true,

							items : [new Ext.form.FormPanel({
										id : 'uploadform',
										labelWidth : 50,
										fileUpload : true,
										defaults : {
											anchor : '95%',
											allowBlank : false,
											msgTarget : 'side'
										},
										items : [{
													xtype : 'hidden',
													value : this.taskId,
													id : 'taskId',
													name : 'taskId'
												}, {
													xtype : 'fileuploadfield',
													id : 'form-file',
													emptyText : 'Select a file',
													fieldLabel : 'File',
													name : 'file-path',
													listeners : {
														'fileselected' : function(field, value) {
															Ext.getCmp('file-upload-button').enable();
														}.createDelegate(this)

													},
													buttonCfg : {
														text : '',
														iconCls : 'upload-icon'

													}
												}],
										buttons : [{
											text : 'Upload',
											id : 'file-upload-button',
											disabled : true,
											handler : function() {
												var taskId = parseInt(Math.random() * 1e12, 12);
												this.taskId = taskId;
												var form = Ext.getCmp('uploadform').getForm();
												if (form.isValid()) {
													form.submit({
																url : '/Gemma/uploadFile.html',
																method : 'post',
																waitMsg : 'Uploading your file ...',
																success : function(form, a) {
																	var m = a.result;
																	Ext.getCmp('messages')
																			.setText("File uploaded: " + m.originalFile
																					+ "; " + m.size + " bytes");
																	this.fireEvent('finish', m);

																}.createDelegate(this),
																failure : function(form, a) {
																	Ext.Msg.alert('Failure',
																			'Problem with processing of file "'
																					+ a.result.originalFile
																					+ '" on the server');
																	this.fireEvent('fail', a.result);
																}.createDelegate(this)
															});
													this.fireEvent('start');
												}
											}.createDelegate(this)
										}]
									})],
							bbar : new Ext.StatusBar({
										id : 'messages'
									})
						});

				Gemma.FileUploadForm.superclass.initComponent.call(this);

				this.addEvents({
							finish : true,
							fail : true,
							start : true,
							cancel : true
						});

			}

		});
