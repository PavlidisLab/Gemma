Ext.namespace('Gemma');
Ext.form.Field.prototype.msgTarget = 'side';
Ext.BLANK_IMAGE_URL = "/Gemma/images/s.gif";

Ext.onReady(function() {
	Ext.QuickTips.init();

	var q = Ext.QuickTips;

	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

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

	var panel = new Ext.Panel({
		renderTo : 'form',
		width : 600,
		autoHeight : true,
		frame : true,
		title : "Enter expression experiment details",
		items : [{
					xtype : 'form',
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
								height : 100,
								width : 400,
								fieldLabel : 'Description',
								allowBlank : false,
								emptyText : 'Please enter a brief description of your experiment'
							}, taxonCombo]
				},

				{
					xtype : 'panel',
					style : 'margin : 5px',
					height : 'auto',
					title : "Select the array design you used",
					frame : true,
					layout : 'table',
					layoutConfig : {
						columns : 1
					},

					items : [arrayDesignCombo, {
								id : 'array-design-info-area',
								xtype : 'textarea',
								width : 500,
								style : 'overflow :scroll;margin: 4px 6px 4px 6px;',
								height : 150,
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
						}), uploadForm, {
					xtype : 'label',
					html : 'For help with the file data file format, see '
							+ '<a target="_blank" href="/Gemma/static/expressionExperiment/upload_help.html">this page</a>. '
							+ 'The probe identifiers must match those in the array design on record.'
				}],
		buttons : [{
					id : 'submit-data-button',
					value : 'Submit dataset',
					handler : submitDataset,
					text : "Submit dataset",
					enabled : false
				}]

	});

	arrayDesignCombo.on('select', function(combo, arrayDesign) {
				console.log(arrayDesign);
				Ext.getCmp('array-design-info-area').setValue(arrayDesign.data.description);

			});

	taxonCombo.on('select', function(combo, taxon) {
				arrayDesignCombo.taxonChanged(taxon);
			});

	uploadForm.on('start', function(result) {
				Ext.getCmp('submit-data-button').disable();
			});

	uploadForm.on('finish', function(result) {
				/*
				 * GEt the file information and put it in the value object.
				 */
				Ext.getCmp('submit-data-button').enable();
			});

});


function validate() {
	
}

function submitDataset() {

	/*
	 * Submit to the server. Start a progress bar. On the server, we have to do the following steps: 1) validate
	 * everything 2) do the conversion 3) forward the user to the new data set page. Make sure they get instructions on
	 * how to
	 */

}