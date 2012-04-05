/**
 * This form lets users create or edit a phenotype association. 
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma.PhenotypeAssociationForm');

Gemma.PhenotypeAssociationForm.ACTION_CREATE = 'create';
Gemma.PhenotypeAssociationForm.ACTION_EDIT = 'edit';

Gemma.PhenotypeAssociationForm.Window = Ext.extend(Ext.Window, {
	layout: 'fit',
	modal: true,
	constrain: true,
	width: 700,
	height: 500,
	shadow: true,
	closeAction: 'hide',
	initComponent: function() {
		var formPanel = new Gemma.PhenotypeAssociationForm.Panel();
		formPanel.on({
			'hide' : function(thisFormPanel) {
				this.hide();
			},
 			scope: this
		});
		this.relayEvents(formPanel, ['phenotypeAssociationChanged']);			

		var showLogInWindow = function(action, data) {
			Gemma.AjaxLogin.showLoginWindowFn();
	
			Gemma.Application.currentUser.on("logIn",
				function(userName, isAdmin) {	
					Ext.getBody().unmask();

					formPanel.setData(action, data);
					this.show();
				},
				this,
				{
					single: true
				});
	    };
		
		Ext.apply(this, {
			listeners: {
				phenotypeAssociationChanged: function() {
					this.hide();
				},
				hide: function(thisWindow) {
					formPanel.resetForm(false);
				},
				scope: this
			},
			showWindow: function(action, data) {
				if (action === Gemma.PhenotypeAssociationForm.ACTION_CREATE) {
					this.setTitle('Add New Phenotype Association');
				} else if (action === Gemma.PhenotypeAssociationForm.ACTION_EDIT) {
					this.setTitle('Edit Phenotype Association');
				}

				SignupController.loginCheck({
	                callback: function(result){
	                	if (result.loggedIn){
							formPanel.setData(action, data);
							this.show();
	                	}
	                	else{
	                		showLogInWindow.call(this, action, data); 
	                	}
	                }.createDelegate(this)
	            });
			},
			items: [
				formPanel			
			]});
		this.superclass().initComponent.call(this);
	}
});

Gemma.PhenotypeAssociationForm.Panel = Ext.extend(Ext.FormPanel, {
	labelWidth: 130,	
    initComponent: function() {
    	var ANCHOR_VALUE = '96%';
    	
		var hasError = true;

		// They are null when this form is used for creating evidence.
		var evidenceId = null;		
		var lastUpdated = null;

		// loadMask needs to be set up on render because this.getEl() has not been defined yet before render.     	
		this.on('render', function(thisPanel) {
			if (!thisPanel.loadMask) {
				thisPanel.loadMask = new Ext.LoadMask(thisPanel.getEl(), {
					msg: evidenceId == null ?
							"Adding new phenotype association ..." :
							"Updating phenotype association ..."
				});
			}
		});    	

		var geneSearchComboBox = new Gemma.PhenotypeAssociationForm.GeneSearchComboBox({
			listeners: {
				blur: function(combo) {
					var currentGeneNcbiId = combo.getValue();
					
					if (currentGeneNcbiId == '') {
						phenotypesSearchPanel.setCurrentGeneNcbiId(null);
						experimentalPanel.setCurrentGeneTaxonId(null);
					} else {
						phenotypesSearchPanel.setCurrentGeneNcbiId(currentGeneNcbiId);
						
						var geneRecord = combo.getStore().getById(currentGeneNcbiId);
						
						// This should not happen. Check for null just in case something bad happens.
						if (geneRecord == null) {
							experimentalPanel.setCurrentGeneTaxonId(null);
						} else {
							experimentalPanel.setCurrentGeneTaxonId(geneRecord.data.taxonId);
						}
					}
					
					
					this.validateForm(false);
				},
				select: function(combo, record, index) {
					this.validateForm(false);
				},
				scope: this
			}
    	});

		var phenotypesSearchPanel = new Gemma.PhenotypeAssociationForm.PhenotypesSearchPanel({
			anchor: ANCHOR_VALUE,
			listeners: {
				blur: function() {
					this.validateForm(false);
				},
				select: function(combo, record, index) {
					this.validateForm(false);
				},
				phenotypeFieldAdded: function() {
					this.validateForm(false);
				},
				phenotypeFieldCleared: function() {
					this.validateForm(false);
				},
				phenotypeFieldRemoved: function() {
					this.validateForm(false);
				},
				scope: this
			}
		});
    	
    	var literaturePanel = new Gemma.PhenotypeAssociationForm.LiteraturePanel({
			anchor: ANCHOR_VALUE, // Use anchor instead of width so that this panel can be resized as the main window is resized.
			listeners: {
				pubMedIdFieldBlur: function(thisLiteraturePanel) {
					var pubMedId = thisLiteraturePanel.getPubMedId();					
			
					if (pubMedId === "") {
						this.hideErrorPanel();					
					} else if (pubMedId <= 0) {
						hasError = true;
						this.showPubMedIdError();
					}
				},
				pubMedIdFieldKeyPress: function(thisLiteraturePanel, event) {
					hasError = false;					
					this.hideErrorPanel();
				},
				pubMedIdStoreLoad: function(thisLiteraturePanel, store, records, options) {
		        	// Because it takes time to reload the store, show errors
		        	// only when PudMed Id has not been changed (e.g. by clicking the Reset button).
					if (options.params.pubMedId === thisLiteraturePanel.getPubMedId()) {
				    	if (store.getTotalCount() > 0) {
							this.hideErrorPanel();
							this.validateForm(false);
				    	} else {
							hasError = true;
							this.showPubMedIdError();
				    	}
					}			    	
				},
				scope: this
			}			
    	});

		var experimentalPanel = new Gemma.PhenotypeAssociationForm.ExperimentalPanel({
			anchor: ANCHOR_VALUE, // Use anchor instead of width so that this panel can be resized as the main window is resized.
			listeners: {
				pubMedIdsDuplicate: function(primaryPubMedIdLabel, secondaryPubMedIdLabel) {
					errorPanel.showError(String.format(
   						Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.pubMedIdsDuplicate,
   						primaryPubMedIdLabel, secondaryPubMedIdLabel));
					hasError = true;						
				},
				pubMedIdOnlyPrimaryEmpty: function(primaryPubMedIdLabel, secondaryPubMedIdLabel) {
					errorPanel.showError(String.format(
   						Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.pubMedIdOnlyPrimaryEmpty,
   						primaryPubMedIdLabel, secondaryPubMedIdLabel));
					hasError = true;						
				},
				pubMedIdsInvalid: function(pubMedIdLabels) {
					// if we don't have any invalid PubMed Id fields
					if (pubMedIdLabels.length === 0) {
						this.hideErrorPanel();
						hasError = false;						
					} else {
		    			var errorMessages = '';
		    			for (var i = 0; i < pubMedIdLabels.length; i++) {
		   					errorMessages += String.format(
		   						Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.pubmedIdInvalid,
		   						pubMedIdLabels[i]);
			   				if (i < pubMedIdLabels.length - 1) {
			   					errorMessages += '<br />';
			   				}
		    			}
						errorPanel.showError(errorMessages);
						hasError = true;						
					}
				},
				pubMedIdStoreLoad: function() {
					if (experimentalPanel.isValid()) {
						this.validateForm(false);
					} else {
						hasError = true;
					}
				},
				keyup: function(component) {
					if (experimentalPanel.isValid()) {
						this.validateForm(false);
					} else {
						hasError = true;
					}	
				}, 
				select: function(component) {
					if (experimentalPanel.isValid()) {
						this.validateForm(false);
					} else {
						hasError = true;
					}
				},
				experimentTagFieldAdded: function() {
					if (experimentalPanel.isValid()) {
						this.validateForm(false);
					} else {
						hasError = true;
					}
				},
				experimentTagFieldCleared: function() {
					if (experimentalPanel.isValid()) {
						this.validateForm(false);
					} else {
						hasError = true;
					}
				},
				experimentTagFieldRemoved: function() {
					if (experimentalPanel.isValid()) {
						this.validateForm(false);
					} else {
						hasError = true;
					}
				},
				scope: this
			}
		});
    	
		var errorPanel = new Gemma.PhenotypeAssociationForm.ErrorPanel({
		    region: 'north'	
		});   	

		var evidenceCodeComboBox = new Ext.form.ComboBox({
			valueField: 'evidenceCode',
			allowBlank: false,
			// If editable is not set to false, then users are able to type an
			// invalid value which makes the store has no records. When users press
			// the Reset button, the combo box will display the default value 'IC'.
			editable: false,  			
			mode: 'local',
			store: new Ext.data.ArrayStore({
				fields:  ['evidenceCode', 'evidenceCodeDisplayText'],
				data: [
						['EXP', Gemma.EvidenceCodes.expText],
						['IC', Gemma.EvidenceCodes.icText],
						['TAS', Gemma.EvidenceCodes.tasText],
						['IEP', Gemma.EvidenceCodes.iepText],
						['IMP', Gemma.EvidenceCodes.impText],
						['IGI', Gemma.EvidenceCodes.igiText]
					  ]
			}),
			value: 'IC',
			forceSelection: true,				
			displayField:'evidenceCodeDisplayText',
			width: 200,
			typeAhead: true,
			triggerAction: 'all',
			selectOnFocus:true,
			listeners: {
				'blur': function(comboBox) { 
					this.validateForm(false);
				},
				scope: this
			},
		    initComponent: function() {
				Ext.apply(this, {
					selectEvidenceCode: function(evidenceCode) {
						this.setValue(evidenceCode);
						this.originalValue = evidenceCode;
					}
				});
				this.superclass().initComponent.call(this);
		    }
		});
    	
		var evidenceTypeComboBox = new Gemma.PhenotypeAssociationForm.EvidenceTypeComboBox({
			listeners: {
				select: function(combo, record, index) {
					switch (record.data.evidenceClassName) {
						case 'ExperimentalEvidenceValueObject':
							experimentalPanel.show();
							literaturePanel.hide();
							break;
						case 'LiteratureEvidenceValueObject':
							experimentalPanel.hide();						
							literaturePanel.show();
							break;
					}
					this.validateForm(false);
				},
				scope: this
			}
		});

		var isNegativeEvidenceCheckbox = new Ext.form.Checkbox({
			fieldLabel: 'Negative Evidence'
		});

		var descriptionTextArea = new Ext.form.TextArea({
			fieldLabel: 'Note',
			anchor: ANCHOR_VALUE,
		    initComponent: function() {
				Ext.apply(this, {
					setDescription: function(description) {
						this.setValue(description);
						this.originalValue = description;
					}
				});
				this.superclass().initComponent.call(this);
		    }
		});

		// Assume all form values are valid.
		var generateEvidenceValueObject = function(validatedPhenotypeValueUris) {
			var evidenceType = evidenceTypeComboBox.getValue();

			var evidenceValueObject;

			if (evidenceType === 'ExperimentalEvidenceValueObject') {
				if (experimentalPanel.isValid()) {
					var experimentalValues = experimentalPanel.getValues();
					
					evidenceValueObject = new ExperimentalEvidenceValueObject();
					evidenceValueObject.primaryPublicationCitationValueObject = experimentalValues.primaryPublicationCitationValueObject;
					evidenceValueObject.relevantPublicationsCitationValueObjects = experimentalValues.relevantPublicationsCitationValueObjects;
					evidenceValueObject.experimentCharacteristics = experimentalValues.experimentCharacteristics;
				}
			} else if (evidenceType === 'LiteratureEvidenceValueObject') {
				if (literaturePanel.isValid()) {
					evidenceValueObject = new LiteratureEvidenceValueObject();
					evidenceValueObject.citationValueObject = literaturePanel.getCitationValueObject();
				}
			}
		
			if (evidenceValueObject != null) {
				evidenceValueObject.geneNCBI = geneSearchComboBox.getValue();
				evidenceValueObject.phenotypes = validatedPhenotypeValueUris;
				evidenceValueObject.className = evidenceType;
				evidenceValueObject.isNegativeEvidence = isNegativeEvidenceCheckbox.getValue();
				evidenceValueObject.description = descriptionTextArea.getValue();
				evidenceValueObject.evidenceCode = evidenceCodeComboBox.getValue();
				evidenceValueObject.id = evidenceId;
				evidenceValueObject.lastUpdated = lastUpdated;
			}
			
			return evidenceValueObject;
		};
		
    	Ext.apply(this, {
			layout: 'border',    	
			monitorValid : true,
			items: [
				errorPanel,
				{
					xtype: 'panel',
				    region: 'center',
					layout: 'form',
				    border: false,
					autoScroll: true,
					defaults: {
						blankText: 'This field is required'
					},
					padding: '15px 0px 8px 15px',						
					items: [
						{
							// This component is always hidden and its main
							// purpose is to make the OK button 
							// enabled/disabled correctly.   
							xtype: 'textfield', 
							hidden: true,
							validator: function() {
								return !hasError;
							}
						},						
						{
							xtype: 'compositefield',
						    fieldLabel: 'Gene',
						    autoWidth: true,
						    items: [
								geneSearchComboBox,	    
								geneSearchComboBox.getGeneSelectedLabel()
						    ]
						},
						phenotypesSearchPanel,							
						evidenceTypeComboBox,
						isNegativeEvidenceCheckbox,
						experimentalPanel,							
						literaturePanel,
						descriptionTextArea,
						{
							xtype: 'compositefield',
							fieldLabel: "Evidence Code",
						    autoWidth: true,
						    items: [
						    	evidenceCodeComboBox,
								{
									xtype: 'displayfield',
									value: '<img src="/Gemma/images/help.png" ext:qtip="' +
											'<b>' + Gemma.EvidenceCodes.expText + '</b><br />' + Gemma.EvidenceCodes.expTT + '<br /><br />' + 
											'<b>' + Gemma.EvidenceCodes.icText + '</b><br />' + Gemma.EvidenceCodes.icTT + '<br /><br />' + 
											'<b>' + Gemma.EvidenceCodes.tasText + '</b><br />' + Gemma.EvidenceCodes.tasTT + '<br /><br />' + 
											'<b>' + Gemma.EvidenceCodes.iepText + '</b><br />' + Gemma.EvidenceCodes.iepTT + '<br /><br />' + 
											'<b>' + Gemma.EvidenceCodes.impText + '</b><br />' + Gemma.EvidenceCodes.impTT + '<br /><br />' + 
											'<b>' + Gemma.EvidenceCodes.igiText + '</b><br />' + Gemma.EvidenceCodes.igiTT + '<br /><br />' + 
											'" />',
									margins: '4 0 0 0'			
								}
						    ]
						}					
					]
				}],
			setData: function(action, data) {
				phenotypesSearchPanel.selectPhenotypes(data.phenotypes, data.gene);
				geneSearchComboBox.selectGene(data.gene);
				
				evidenceId = data.evidenceId;
				
				// if we are editing evidence
				if (evidenceId != null) {
					hasError = false;
					
					lastUpdated = data.lastUpdated;
			
					evidenceTypeComboBox.selectEvidenceType(data.evidenceClassName);
					
					switch (data.evidenceClassName) {
						case 'LiteratureEvidenceValueObject':
							// Should call setEvidenceId() before setPubMedId()
							// because finding bibliographic reference requires
							// evidence id.
							literaturePanel.setEvidenceId(evidenceId);
							
							// Don't need to show literaturePanel explicitly 
							// because setting PubMed Id shows it automatically.
							literaturePanel.setPubMedId(data.pubMedId);

							experimentalPanel.hide();							
							experimentalPanel.selectExperimentalData('', '', null, null);
							break;
						case 'ExperimentalEvidenceValueObject':
							experimentalPanel.show();						
							experimentalPanel.setEvidenceId(evidenceId);	
							experimentalPanel.selectExperimentalData(data.primaryPubMedId, data.secondaryPubMedId,
								data.experimentCharacteristics, data.gene);

							literaturePanel.hide();	
							literaturePanel.setPubMedId('');	
							break;
					}
					isNegativeEvidenceCheckbox.setValue(data.isNegativeEvidence);
					descriptionTextArea.setDescription(data.description);
					evidenceCodeComboBox.selectEvidenceCode(data.evidenceCode);
				}
				
				if (this.hidden) {
					this.show();
				}
			},
			submitForm: function(evidenceValueObject) {
			    if (this.getForm().isValid()) {
			    	var isCreating = (evidenceId == null);

					this.loadMask.show();

					PhenotypeController.processPhenotypeAssociationForm(evidenceValueObject, function(validateEvidenceValueObject) {
						this.loadMask.hide();

						if (validateEvidenceValueObject == null) {	
			            	Ext.Msg.alert('Phenotype association ' + (isCreating ? 'added' : 'updated'), 'Phenotype association has been ' + (isCreating ? 'added' : 'updated') + '.');
							this.fireEvent('phenotypeAssociationChanged');			
			            } else {
			            	var title = 'Cannot ' + (isCreating ? 'add' : 'edit') + ' phenotype association';

							Ext.Msg.alert(title, Gemma.convertToEvidenceError(validateEvidenceValueObject).errorMessage,
								function() {
									if (validateEvidenceValueObject.userNotLoggedIn) {
										Gemma.AjaxLogin.showLoginWindowFn();
									}
								}
							);
				        }
					}.createDelegate(this));
			    }
			},
// TODO: Remove it later			
			showPubMedIdError: function() {
//				errorPanel.showError(Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.pubmedIdInvalid);
				errorPanel.showError(String.format(Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.pubmedIdInvalid, 'PubMed Id'));
			},
			hideErrorPanel: function() {
				errorPanel.hide();
			},
			validateForm: function(shouldSubmitAfterValidating) {
				hasError = false;
//				errorPanel.hide();
				
				var evidenceType = evidenceTypeComboBox.getValue();
				
				if (evidenceType === '' || geneSearchComboBox.getValue() === '') {
					hasError = true;
					errorPanel.hide();
				} else {
					var phenotypeValueUris = phenotypesSearchPanel.getSelectedPhenotypes();					
									
					if (phenotypeValueUris == null || phenotypeValueUris.length <= 0) {
						hasError = true;
//						errorPanel.hide();
					} else {
						if (evidenceType === 'ExperimentalEvidenceValueObject') {
							if (!experimentalPanel.isValid()) {
								hasError = true;
							} 
						} else if (evidenceType === 'LiteratureEvidenceValueObject') {
							if (!literaturePanel.isValid()) {
								hasError = true;
								if (literaturePanel.getPubMedId() !== '') {
									this.showPubMedIdError(literaturePanel.pubMedIdFieldLabel);
								}
							}
						}
						
						if (!hasError) {
							if (evidenceCodeComboBox.getValue() === '') {
								hasError = true;
								errorPanel.hide();
							} else {
								var evidenceValueObject = generateEvidenceValueObject(phenotypeValueUris);
								
								// Ask the controller to validate only after all fields are filled.
								PhenotypeController.validatePhenotypeAssociationForm(evidenceValueObject, function(validateEvidenceValueObject) {
						
// TODO: Because using the controller to validate takes time, fields such as gene value MAY have been changed. 
// Thus, I may need to show error ONLY when no fields have been changed after the controller call.  
									var hasWarning = false;
		
									if (validateEvidenceValueObject == null) {
										hasError = false;
										errorPanel.hide();
									} else {
										var errorCode = Gemma.convertToEvidenceError(validateEvidenceValueObject); 
										hasWarning = errorCode.isWarning;
										hasError = !hasWarning; 
										if (hasWarning) {
											errorPanel.showWarning(errorCode.errorMessage);
										} else {
											errorPanel.showError(errorCode.errorMessage);
										}
		
									}						
		
									if (shouldSubmitAfterValidating) {
										if (!hasError) {
											if (hasWarning) {
												Ext.MessageBox.confirm('Confirm',
													'<b>' + errorPanel.getErrorMessage() + '</b><br />' +
														'Are you sure you want to ' + (evidenceId == null ? 'add' : 'edit') + ' phenotype association?',
													function(button) {
														if (button === 'yes') {
															this.submitForm(evidenceValueObject);
														}
													},
													this);
											} else {
												this.submitForm(evidenceValueObject);
											}
										}
									}
								}.createDelegate(this));
							}				
						} // if (!hasError)
					}
				}
			},
			resetForm: function(shouldValidate) {
			    this.hideErrorPanel();
			    
				geneSearchComboBox.reset();
				phenotypesSearchPanel.reset();
				evidenceTypeComboBox.reset();	
				isNegativeEvidenceCheckbox.setValue(false);
			    literaturePanel.reset();
				experimentalPanel.reset();
				descriptionTextArea.reset();
				evidenceCodeComboBox.reset();
				if (shouldValidate) {				
					this.validateForm(false);
				}
			},	
			buttonAlign: 'right',
			buttons: [
				{
				    text: 'OK',
				    formBind: true,
				    handler: function() {
						this.validateForm(true);
				    },
					scope: this
				},
// TODO: Should remove all the code related to this Reset button				
//				{
//					text: 'Reset',
//					handler: function() {
//						this.resetForm(true);
//					},
//					scope: this
//				},
				{
				    text: 'Cancel',
				    handler: function() {
				    	this.hide();
				    },
					scope: this
				}
			]		
		});
		
		this.superclass().initComponent.call(this);
    }
});
