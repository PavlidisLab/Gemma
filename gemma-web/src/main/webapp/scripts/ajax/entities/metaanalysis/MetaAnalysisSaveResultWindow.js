/**
 * This window asks users to provide name and description for the meta-analysis they want to save. 
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.MetaAnalysisSaveResultWindow = Ext.extend(Ext.Window, {
	title: 'Save results',
	modal: true,
	constrain: true,
	width: 500,
	shadow: true,
	closeAction: 'close',
	initComponent: function() {
		var nameTextField = new Ext.form.TextField({
			maxLength: 255,
			allowBlank: false,
			fieldLabel: 'Name',
			anchor: '100%'
		});
		
		var descriptionTextArea = new Ext.form.TextArea({
			maxLength: 65535, // Data type is TEXT in the database.
			fieldLabel: 'Description',
			anchor: '100%',
		    initComponent: function() {
				Ext.apply(this, {
					autoCreate: { tag: 'textarea', rows: '4', maxlength: this.maxLength }
				});
				this.superclass().initComponent.call(this);
		    }
		});

    	Ext.apply(this, {
			items: [{
				xtype: 'form',
				monitorValid : true,
				padding: '15px',						
				items: [
					nameTextField,
					descriptionTextArea
				],		
				buttons: [
					{
					    text: 'OK',
					    formBind: true,
					    handler: function() {
							this.fireEvent('okButtonClicked', nameTextField.getValue(), descriptionTextArea.getValue());
					    	this.close();
					    },
						scope: this
					},
					{
					    text: 'Cancel',
					    handler: function() {
					    	this.close();
					    },
						scope: this
					}
				]
			}]
		});
		
		Gemma.MetaAnalysisSaveResultWindow.superclass.initComponent.call(this);
	}
});


