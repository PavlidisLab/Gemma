
/**
 * Dialog to ask user for information about a new set (or potentially modifications to an existing one)
 * <p>
 * May provide a store, used during validation of set name.
 * @extends Ext.Window
 */

Gemma.CreateSetDetailsWindow = Ext.extend(Ext.Window, {
			width : 500,
			height : 300,
			closeAction : 'hide',
			title : "Provide or edit group details",
   			shadow : true,
			modal : true,
			/**
			 * taxon combo option to make it only display taxa with experiments
			 */
			isDisplayTaxonWithDatasets : false,
			/**
			 * optional config, if set it will be used during validation to check for set name duplicates
			 */
			store : false,
			/**
			 * set the taxon combo to the corresponding id param and optionally disable the combo
			 * @param taxonId
			 * @param leaveEnabled whether to leave the combo enabled, false by default
			 */
			lockInTaxonId: function(taxId, leaveEnabled){
				if(taxId && taxId !== null){	
					this.taxCombo.on('ready', function(){
						this.taxCombo.setTaxonById(taxId);
						this.taxCombo.setDisabled(leaveEnabled);
					}, this);
				}
			},
			initComponent : function() {

				this.addEvents("commit");
				this.formId = Ext.id();
				
				// allow taxon to be set in configs on instantiation
				this.taxCombo = new Gemma.TaxonCombo({
								name: 'newEesetTaxon',
								fieldLabel: 'Taxon',
								isDisplayTaxonWithDatasets: this.isDisplayTaxonWithDatasets,
								invalidText: "You must select a taxon",
								msgTarget: 'side',
								validator: function(val){
									return (val === "" || val === undefined || val === null)?'Taxon value required':true; 
								}
							});
				
				
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
							items: [this.taxCombo, new Ext.form.TextField({
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
							}), new Ext.form.Checkbox({
								fieldLabel: 'Public group',
								name: 'publik',
 								checked : this.publik,
								value: this.publik,
								width: 300
							})]
						})
					}),
					buttons: [{
						text: "Cancel",
						handler: this.hide.createDelegate(this, [])
					}, {
						text: "OK",
						scope:this,
						handler : this.onCommit.createDelegate(this)
					}]
				
				});

				Gemma.CreateSetDetailsWindow.superclass.initComponent.call(this);
				this.addEvents("commit");
			},
			/**
			 * private
			 */
			onCommit: function(){
				if (this.taxCombo.validate() &&
				this.formPanel.fieldSet.nameField.validate()) {
				
					var values = this.formPanel.getForm().getValues();
					var taxon = this.formPanel.getForm().findField('newEesetTaxon').getTaxon();
					
					// if store was specified, check for duplicate names
					if (this.store && this.store instanceof Ext.data.Store) {
						var indexOfExisting = this.store.findBy(function(record, id){
							return record.get("name") === values.newSetName;
						}, this);
						
						if (indexOfExisting >= 0) {
							/*
							 * This might not be good enough, since sets they don't own
							 * won't be listed - but we'll figure it out on the server
							 * side.
							 */
							Ext.Msg.alert(Gemma.HelpText.CommonWarnings.DuplicateName.title,
								Gemma.HelpText.CommonWarnings.DuplicateName.text );
							return;
						}
					}
					
					this.fireEvent("commit", {
						name: values.newSetName,
						description: values.newSetDescription,
						publik: (typeof values.publik !== "undefined" && values.publik === "on"),
						taxon: taxon
					});
					this.hide();
				}
				return;
			}
		});