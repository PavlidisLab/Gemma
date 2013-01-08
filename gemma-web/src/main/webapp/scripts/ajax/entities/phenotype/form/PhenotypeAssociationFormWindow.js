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
	closeAction: 'close',
	initComponent: function() {
		var formPanel = new Gemma.PhenotypeAssociationForm.Panel();
		formPanel.on({
			'cancelButtonClicked' : function() {
				this.close();
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
				phenotypeAssociationChanged: function(phenotypes, gene) {
					this.close();
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
		Gemma.PhenotypeAssociationForm.Window.superclass.initComponent.call(this);
	}
});

Gemma.PhenotypeAssociationForm.Panel = Ext.extend(Ext.FormPanel, {
	labelWidth: 130,	
    initComponent: function() {
    	var ANCHOR_VALUE = '96%';
    	
		var hasLocalErrorMessages = false;
		var hasServerErrorMessages = false;

		var phenotypeErrorMessages = [];
		var literatureErrorMessages = [];
		var experimentalErrorMessages = [];
    	
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

		var errorPanel = new Gemma.PhenotypeAssociationForm.ErrorPanel({
		    region: 'north'	
		});   	

		var updateErrorMessages = function() {
			// Reset it to false so that the OK button is not disabled any more.	
			hasServerErrorMessages = false;
		
			var allErrorMessages = phenotypeErrorMessages;
			
			switch (evidenceTypeComboBox.getValue()) {
				case 'ExperimentalEvidenceValueObject':
					if (experimentalErrorMessages.length > 0) {
						allErrorMessages = allErrorMessages.concat(experimentalErrorMessages);
					}
					break;
				case 'LiteratureEvidenceValueObject':
					if (literatureErrorMessages.length > 0) {
						allErrorMessages = allErrorMessages.concat(literatureErrorMessages);
					}
					break;
			}
			
			hasLocalErrorMessages = allErrorMessages.length > 0;						
		
			if (hasLocalErrorMessages) {					
				var formattedErrorMessages = '';
				for (var i = 0; i < allErrorMessages.length; i++) {
					formattedErrorMessages += allErrorMessages[i];
					if (i < allErrorMessages.length - 1) {
						formattedErrorMessages += '<br />';
					}
				}
				errorPanel.showError(formattedErrorMessages);
			} else {
				errorPanel.hide();
			}
		}

		// Note: it should be called when all form values are valid.
		var generateEvidenceValueObject = function(validatedPhenotypes) {
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

				var phenotypes = [];
				Ext.each(validatedPhenotypes, function(validatedPhenotype, index) {
					var characteristicValueObject = new CharacteristicValueObject();
					characteristicValueObject.id = validatedPhenotype.id;
					characteristicValueObject.urlId = validatedPhenotype.urlId;
					characteristicValueObject.value = validatedPhenotype.value;
					characteristicValueObject.valueUri = validatedPhenotype.valueUri;

					phenotypes.push(characteristicValueObject);
				});
				evidenceValueObject.phenotypes = phenotypes;
				
				evidenceValueObject.className = evidenceType;
				evidenceValueObject.isNegativeEvidence = isNegativeEvidenceCheckbox.getValue();
				evidenceValueObject.description = descriptionTextArea.getValue();
				evidenceValueObject.evidenceCode = evidenceCodeComboBox.getValue();
				evidenceValueObject.id = evidenceId;
				evidenceValueObject.lastUpdated = lastUpdated;
			}
			
			return evidenceValueObject;
		};
		
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
				validtyStatusChanged: function(isModifying, errorMessages) {
					phenotypeErrorMessages = errorMessages;
					updateErrorMessages();
					if (!isModifying && errorPanel.getErrorMessage() === '') {
						this.validateForm(false);
					} 
				},
				scope: this
			}
		});
    	
		var experimentalPanel = new Gemma.PhenotypeAssociationForm.ExperimentalPanel({
			anchor: ANCHOR_VALUE, // Use anchor instead of width so that this panel can be resized as the main window is resized.
			listeners: {
				validtyStatusChanged: function(isModifying, errorMessages) {
					experimentalErrorMessages = errorMessages;
					updateErrorMessages();
					
					if (!isModifying && errorPanel.getErrorMessage() === '') {
							this.validateForm(false);
					}	
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
						literatureErrorMessages = [];
					} else if (pubMedId <= 0) {
						literatureErrorMessages = [ String.format(Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.pubmedIdInvalid, thisLiteraturePanel.pubMedIdFieldLabel) ];
					}

					updateErrorMessages();
				},
				pubMedIdFieldKeyUp: function(thisLiteraturePanel, event) {
					literatureErrorMessages = [];
					updateErrorMessages();
				},
				pubMedIdStoreLoad: function(thisLiteraturePanel, store, records, options) {
		        	// Because it takes time to reload the store, show errors
		        	// only when PudMed Id has not been changed.
					if (options.params.pubMedId === thisLiteraturePanel.getPubMedId()) {
				    	if (store.getTotalCount() > 0) {
							literatureErrorMessages = [];
							updateErrorMessages();

							this.validateForm(false);
				    	} else {
							literatureErrorMessages = [ String.format(Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.pubmedIdInvalid, thisLiteraturePanel.pubMedIdFieldLabel) ];
							updateErrorMessages();
				    	}
					}			    	
				},
				scope: this
			}			
    	});

		var evidenceTypeComboBox = new Gemma.PhenotypeAssociationForm.EvidenceTypeComboBox({
			listeners: {
				select: function(combo, record, index) {
					switch (record.data.evidenceClassName) {
						case 'ExperimentalEvidenceValueObject':
							literaturePanel.hide();
							experimentalPanel.show();

							experimentalPanel.setEvidenceId(evidenceId);
							break;
						case 'LiteratureEvidenceValueObject':
							experimentalPanel.hide();						
							literaturePanel.show();

							literaturePanel.setEvidenceId(evidenceId);
							break;
					}

					updateErrorMessages();

					this.validateForm(false);
				},
				scope: this
			}
		});

		var isNegativeEvidenceCheckbox = new Ext.form.Checkbox({
			fieldLabel: 'Negative Evidence'
		});

		var descriptionTextArea = new Ext.form.TextArea({
			maxLength: 65535, // Data type is TEXT in the database.
			fieldLabel: 'Note',
			anchor: ANCHOR_VALUE,
		    initComponent: function() {
				Ext.apply(this, {
					autoCreate: { tag: 'textarea', rows: '4', maxlength: this.maxLength },
					setDescription: function(description) {
						this.setValue(description);
						this.originalValue = description;
					}
				});
				this.superclass().initComponent.call(this);
		    }
		});
		
		var evidenceCodesForComboBox = [ 'EXP', 'IAGP', 'IC', 'IED', 'IEP', 'IGI', 'IMP', 'IPM', 'QTM', 'TAS' ];		
		var evidenceCodeComboBoxStoreData = [];
		var qtipWidth;
		Ext.each(evidenceCodesForComboBox, function(evidenceCode, index) {
			var evidenceCodeInfo = Gemma.EvidenceCodeInfo[evidenceCode];
			var qtipInfo = Gemma.EvidenceCodeInfo.getQtipInfo(evidenceCode, evidenceCodeInfo);
			
			if (!qtipWidth) {
				qtipWidth = qtipInfo.width;
			}
			
			evidenceCodeComboBoxStoreData.push([
				evidenceCode, evidenceCode + ' (' + evidenceCodeInfo.name + ')', qtipInfo.text
			]);
		});

		var evidenceCodeComboBox = new Ext.form.ComboBox({
			valueField: 'evidenceCode',
			allowBlank: false,
			// If editable is not set to false, then users are able to type an
			// invalid value which makes the store has no records.
			editable: false,  			
			mode: 'local',
			store: new Ext.data.ArrayStore({
				fields:  ['evidenceCode', 'displayText', 'qtip' ],
				data: evidenceCodeComboBoxStoreData
			}),
			tpl: new Ext.XTemplate('<tpl for="."><div ext:qtip="{qtip}" ext:qwidth="' + qtipWidth + '" class="x-combo-list-item">{displayText}</div></tpl>'),
			value: 'IC',
			forceSelection: true,				
			displayField:'displayText',
			width: qtipWidth,
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
								return !hasLocalErrorMessages && !hasServerErrorMessages;
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
						    	evidenceCodeComboBox
						    ]
						}					
					]
				}],
			setData: function(action, data) {
				phenotypesSearchPanel.selectPhenotypes(data.phenotypes, data.gene);
				geneSearchComboBox.selectGene(data.gene);
				
				evidenceId = data.evidenceId;
				
				// if we need to copy data
				if (data.evidenceClassName != null) {
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
							this.fireEvent('phenotypeAssociationChanged', evidenceValueObject.phenotypes,
								geneSearchComboBox.getStore().getById(evidenceValueObject.geneNCBI).data); 
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
			validateForm: function(shouldSubmitAfterValidating) {
				hasServerErrorMessages = false;
				
				if (!hasLocalErrorMessages) {
					errorPanel.hide(); // Hide validation error messages if any.
				
					var evidenceType = evidenceTypeComboBox.getValue();

					// Clear error.
					if (evidenceType === 'ExperimentalEvidenceValueObject') {
						experimentalPanel.showAnnotationError([], null);
					} else if (evidenceType === 'LiteratureEvidenceValueObject') {
						literaturePanel.showAnnotationError([], null);
					}
					
					if (evidenceType !== '' && geneSearchComboBox.getValue() !== '') {
						var selectedPhenotypes = phenotypesSearchPanel.getSelectedPhenotypes();					
										
						if (selectedPhenotypes != null && selectedPhenotypes.length > 0) {
							var isValid = false;
	
							if (evidenceType === 'ExperimentalEvidenceValueObject') {
								if (experimentalPanel.isValid()) {
									isValid = true;
								} 
							} else if (evidenceType === 'LiteratureEvidenceValueObject') {
								if (literaturePanel.isValid()) {
									isValid = true;
								}
							}
							
							if (isValid && evidenceCodeComboBox.getValue() !== '') {
								var evidenceValueObject = generateEvidenceValueObject(selectedPhenotypes);

								this.loadMask.show();

								// Ask the controller to validate only after all fields are filled.
								PhenotypeController.validatePhenotypeAssociationForm(evidenceValueObject, function(validateEvidenceValueObject) {
									this.loadMask.hide();

									var hasError = false;								
									var hasWarning = false;
			
									if (validateEvidenceValueObject == null) {
										hasError = false;
									} else {
										if (validateEvidenceValueObject.problematicEvidenceIds.length > 0) {
											var errorColor = validateEvidenceValueObject.sameEvidenceFound ?
												'red' :
												'orange';
										
											if (evidenceType === 'ExperimentalEvidenceValueObject') {
												experimentalPanel.showAnnotationError(
													validateEvidenceValueObject.problematicEvidenceIds, errorColor);
											} else if (evidenceType === 'LiteratureEvidenceValueObject') {
												literaturePanel.showAnnotationError(
													validateEvidenceValueObject.problematicEvidenceIds, errorColor);
											}
										}
										
										var errorCode = Gemma.convertToEvidenceError(validateEvidenceValueObject); 
										hasWarning = errorCode.isWarning;
										hasError = !hasWarning; 
										if (hasWarning) {
											errorPanel.showWarning(errorCode.errorMessage);
										} else {
											hasServerErrorMessages = true;										
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
						}
					} 
				} // if (!hasLocalErrorMessages)
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
				{
				    text: 'Cancel',
				    handler: function() {
				    	this.fireEvent('cancelButtonClicked');
				    },
					scope: this
				}
			]		
		});
		
		Gemma.PhenotypeAssociationForm.Panel.superclass.initComponent.call(this);
    }
});
