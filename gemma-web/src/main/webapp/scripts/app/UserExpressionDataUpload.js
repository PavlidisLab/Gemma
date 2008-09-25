Ext.namespace('Gemma');
Ext.form.Field.prototype.msgTarget = 'side';
Ext.BLANK_IMAGE_URL = "/Gemma/images/s.gif";

Gemma.DatasetUploadTool = Ext.extend(Ext.util.Observable, {

			commandObject : {},

			agree : function() {
				if (Ext.getCmp('agree').getValue()) {
					// FIXME don't do this until the rest of the form is valid.
					Ext.getCmp('submit-data-button').enable();
				}
			},

			validate : function() {

				/*
				 * Lock the form.
				 */

				/*
				 * Assemble the command object.
				 */
				this.commandObject.shortName = Ext.getCmp('shortName').getValue();

				console.log(this.commandObject);

				/*
				 * Send the data to the server, but don't load it. If everything looks okay, show it to the user for
				 * confirmation. If not, tell them what to fix.
				 */

				ExpressionDataFileUploadController.validate(this.commandObject, {
							callback : this.onStartValidation.createDelegate(this)
						});
				// fireEvent('dataValid');
			},

			onStartValidation : function(taskId) {
				Ext.DomHelper.overwrite("messages", "");
				var p = new Gemma.ProgressWidget({
							taskId : taskId
						});

				var window = new Ext.Window({
							modal : true,
							width : 400,
							items : [p]
						});

				p.on('done', function(payload) {
							this.onValidated(payload);
							window.hide('validate-data-button');
							window.destroy();
							p.destroy();
						}.createDelegate(this));

				p.on('fail', function(message) {
							console.log("failed: " + message);
							// window.getEl().fadeOut({
							// duration : 2000
							// });
							window.hide('validate-data-button');
							window.destroy();
							p.destroy();
						}.createDelegate(this));

				window.show();

				p.startProgress();
			},

			onValidated : function(result) {
				console.log(result);
				if (result.valid) {
					Ext.getCmp('agree').enable();
				} else {

					/*
					 * Display a summary of the problems
					 */

					/*
					 * re-enable the form.
					 */

					/*
					 * Invalidate the fields that need work
					 */

				}
			},

			submitDataset : function() {

				/*
				 * Submit to the server. Start a progress bar. On the server, we have to do the following steps: 1)
				 * validate everything 2) do the conversion 3) forward the user to the new data set page. Make sure they
				 * get instructions on how to
				 */

			}
		});

Ext.onReady(function() {
	Ext.QuickTips.init();

	var q = Ext.QuickTips;

	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	tool = new Gemma.DatasetUploadTool();

	var uploadForm = new Gemma.FileUploadForm({
				title : 'Upload your data file',
				style : 'margin : 5px',
				allowBlank : false
			});

	var taxonCombo = new Gemma.TaxonCombo({
				fieldLabel : "Taxon",
				allowBlank : false
			});

	var arrayDesignCombo = new Gemma.ArrayDesignCombo({
				minHeight : 80,
				bodyStyle : 'padding: 10px',
				allowBlank : false,
				width : 500
			});

	var panel = new Ext.form.FormPanel({
		renderTo : 'form',
		width : 600,
		autoHeight : true,
		frame : true,
		title : "Enter expression experiment details",
		items : [{
					xtype : 'fieldset',
					title : 'The basics',
					autoHeight : true,
					style : 'margin : 5px',
					bodyStyle : "padding : 10px",
					items : [{
								xtype : 'textfield',
								id : 'shortName',
								fieldLabel : 'Short name',
								emptyText : 'Must be unique',
								width : 200,
								allowBlank : false
							}, {
								xtype : 'textfield',
								fieldLabel : 'Name',
								id : 'name',
								emptyText : 'Enter a longer name here',
								width : 400,
								allowBlank : false
							}, {
								xtype : 'textarea',
								id : 'description',
								height : 70,
								width : 400,
								fieldLabel : 'Description',
								allowBlank : true,
								emptyText : 'Please enter a brief abstract describing your experiment'
							}, taxonCombo]
				},

				{
					xtype : 'fieldset',
					style : 'margin : 5px',
					height : 'auto',
					title : "Select the array design you used",

					layout : 'table',
					layoutConfig : {
						columns : 1
					},

					items : [arrayDesignCombo, {
								id : 'array-design-info-area',
								xtype : 'textarea',
								width : 500,
								style : 'overflow :scroll;margin: 4px 0px 4px 0px;',
								height : 100,
								readOnly : true
							}, {
								xtype : 'label',
								html : "Don't see your array design listed? Please see "
										+ "<a target='_blank' href='/Gemma/arrayDesgins.html'>the array design details page</a>"
										+ " for more information, "
										+ "or <a href='mailto:gemma@ubic.ca'>let us know</a> about your array design."
							}]
					// fixme add link to report.
				}, new Gemma.QuantitationTypePanel({
							id : 'quantitation-type-panel',
							style : 'margin : 5px'
						}),  /* uploadForm,*/ {
					xtype : 'label',
					html : 'For help with the file data file format, see '
							+ '<a target="_blank" href="/Gemma/static/expressionExperiment/upload_help.html">this page</a>. '
							+ 'The probe identifiers must match those in the array design on record.'
				}, {
					xtype : 'fieldset',
					id : 'availability-form',
					title : 'Security/availability information',
					labelWidth : 200,
					autoHeight : true,
					items : [{
								xtype : 'numberfield',
								enableKeyEvents : true,
								minLength : 7,
								maxLength : 9,
								allowNegative : false,
								id : 'pubmedid',
								allowDecimals : false,
								fieldLabel : 'Pubmed ID',
								boxLabel : "If provided, your data will be made publicly viewable"
							}, {
								xtype : 'checkbox',
								id : 'public',
								boxLabel : "If checked, your data will immediately be viewable by anybody",
								fieldLabel : "Make my data publicly available"
							}, {
								xtype : 'checkbox',
								id : 'agree',
								enabled : false,
								handler : tool.agree,
								fieldLabel : "I have read the '<a target=\'_blank\' href='/Gemma/static/dataUploadTermsAndConditions.html'>terms and conditions</a>'"
							}]
				}],
		buttons : [{
					id : 'validate-data-button',
					value : 'Validate entries',
					handler : tool.validate,
					scope : tool,
					text : "Validate entries",
					enabled : false
				}, {
					id : 'submit-data-button',
					value : 'Submit dataset',
					handler : tool.submitDataset,
					scope : tool,
					text : "Submit dataset",
					enabled : false
				}]

	});

	Ext.getCmp('pubmedid').on('keyup', function(e, a) {
				if (!Ext.getCmp('pubmedid').getValue()) {
					Ext.getCmp('public').setValue(false);
					Ext.getCmp('public').enable();
				} else if (Ext.getCmp('pubmedid').isValid()) {
					Ext.getCmp('public').setValue(true);
					Ext.getCmp('public').disable();
					tool.commandObject.pubMedId = Ext.getCmp('pubmedid').getValue()
				} else {
					Ext.getCmp('public').setValue(false);
					Ext.getCmp('public').enable();
				}
			});

	arrayDesignCombo.on('select', function(combo, arrayDesign) {
				console.log(arrayDesign);
				Ext.getCmp('array-design-info-area').setValue(arrayDesign.data.description);
				tool.commandObject.arrayDesign = arrayDesign.data;
			});

	taxonCombo.on('select', function(combo, taxon) {
				arrayDesignCombo.taxonChanged(taxon.data);
				tool.commandObject.taxon = taxon.data;
			}.createDelegate(this));

	taxonCombo.on('ready', function(taxon) {
				var task = new Ext.util.DelayedTask(arrayDesignCombo.taxonChanged, arrayDesignCombo, [taxon]);
				task.delay(500);
			}.createDelegate(this));

	uploadForm.on('start', function(result) {
				Ext.getCmp('submit-data-button').disable();
			}.createDelegate(this));

	uploadForm.on('finish', function(result) {
				/*
				 * Get the file information and put it in the value object.
				 */
				if (result.success) {
					Ext.getCmp('validate-data-button').enable();
					tool.commandObject.serverFilePath = result.localFile;
					tool.commandObject.originalFileName = result.originalFile;
				}
			});

});
