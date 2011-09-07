
/**
 * Dialog to ask user for information about a new set (or potentially modifications to an existing one)
 * 
 * @extends Ext.Window
 */
Gemma.CreateSetDetailsWindow = Ext.extend(Ext.Window, {
			width : 500,
			height : 300,
			closeAction : 'hide',
			title : "Provide or edit group details",
			shadow : true,
			modal : true,
			initComponent : function() {

				this.addEvents("commit");
				this.formId = Ext.id();
				
				Ext.apply(this, {
					items: new Ext.FormPanel({
						id: this.id + 'FormPanel',
						ref: 'formPanel',
						frame: false,
						labelAlign: 'left',
						height: 250,
						items: new Ext.form.FieldSet({
							id: this.id + 'FieldSet',
							ref: 'fieldSet',
							height: 240,
							items: [new Ext.form.TextField({
								ref: 'nameField',
								id: this.id + "Name",
								fieldLabel: 'Name',
								name: 'newSetName',
								minLength: 3,
								allowBlank: false,
								invalidText: "You must provide a name",
								msgTarget: 'side',
								width: 300
							}), new Ext.form.TextArea({
								ref: 'descField',
								id: this.id + 'Desc',
								fieldLabel: 'Description',
								name: 'newSetDescription',
								value: this.description,
								width: 300
							//value: this.suggestedDescription
							})			/*, new Ext.form.Radio({
			 fieldLabel : 'Private',
			 name : 'publicPrivate',
			 checked: true
			 }), new Ext.form.Radio({
			 fieldLabel : 'Public',
			 name : 'publicPrivate',
			 checked: false
			 })*/
							]
						})
					}),
					buttons: [{
						text: "Cancel",
						handler: this.hide.createDelegate(this, [])
					}, {
						text: "OK",
						scope:this,
						handler: function(){
							if (!this.formPanel.fieldSet.nameField.validate()) {
								return;
							}
							var values = this.formPanel.getForm().getValues();
							this.fireEvent("commit", {
								name: values.newSetName,
								description: values.newSetDescription
							});
							this.hide();
							return;
						}
					}]
				
				});

				Gemma.CreateSetDetailsWindow.superclass.initComponent.call(this);
				this.addEvents("commit");
			}
		});