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
					formPanel.resetForm();
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
    initComponent: function() {
    	var ANCHOR_VALUE = '96%';
    	
		var hasError = true;

		// They are null when this form is used for creating evidence.
		var evidenceId = null;		
		var lastUpdated = null;

		var geneSearchComboBox = new Gemma.PhenotypeAssociationForm.GeneSearchComboBox({
			listeners: {
				blur: function(combo) {
					phenotypesSearchPanel.setCurrentGeneNcbiId(combo.getValue() == '' ? null : combo.getValue());
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
//					this.hideErrorPanel();
					this.validateForm(false);
				},
				phenotypeFieldCleared: function() {
//					this.hideErrorPanel();
					this.validateForm(false);
				},
				phenotypeFieldRemoved: function() {
					this.validateForm(false);
				},
				scope: this
			}
		});
    	
    	var literaturePanel = new Gemma.PhenotypeAssociationForm.LiteraturePanel({
			pudMedIdValidator: function() {
				return !hasError;
			},
			listeners: {
				blur: function(numberField) {
					var pubMedId = numberField.getValue();
			
					if (pubMedId === "") {
						this.hideErrorPanel();					
					} else if (pubMedId <= 0) {
						this.showPubMedIdError();
					}
				},
				keypress: function(numberField, event) {
					this.hideErrorPanel();
				},
				load: function(store, records, options) {
		        	// Because it takes time to reload the store, show errors
		        	// only when PudMed Id has not been changed (e.g. by clicking the Reset button).
					if (options.params.pubMedId === literaturePanel.getPubMedId()) {
				    	if (store.getTotalCount() > 0) {
							this.hideErrorPanel();
							this.validateForm(false);
				    	} else {
							this.showPubMedIdError();
				    	}
					}			    	
				},
				scope: this
			}
    	});
    	
		var errorPanel = new Gemma.PhenotypeAssociationForm.ErrorPanel({
		    region: 'north'	
		});   	

		var evidenceCodeComboBox = new Ext.form.ComboBox({
			hiddenName: 'evidenceCode',
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
				blur: function(comboBox) { 
					this.validateForm(false);
				},
				select: function(combo, record, index) {
					switch (record.data.evidenceClassName) {
						case 'LiteratureEvidenceValueObject':
		//								experimentalFieldSet.hide();
		//								externalDatabaseFieldSet.hide();
							literaturePanel.show();
							break;
					}
				},
				scope: this
			}
		});

		var descriptionTextArea = new Ext.form.TextArea({
			name: 'description',
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
				//			experimentalFieldSet,
				//			externalDatabaseFieldSet,
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
					lastUpdated = data.lastUpdated;
			
					evidenceTypeComboBox.selectEvidenceType(data.evidenceClassName);
					
					switch (data.evidenceClassName) {
						case 'LiteratureEvidenceValueObject':
//							literaturePanel.show();
							literaturePanel.setPubMedId(data.pubMedId);
						break;
					}
					
					descriptionTextArea.setDescription(data.description);
					evidenceCodeComboBox.selectEvidenceCode(data.evidenceCode);
				}
				
				if (this.hidden) {
					this.show();
				}
			},
			submitForm: function() {
			    if (this.getForm().isValid()) {
			    	var isCreating = (evidenceId == null);
			    	
			        this.getForm().submit({
			            url: '/Gemma/processPhenotypeAssociationForm.html',
			            params: {
			            	evidenceId: evidenceId,
			            	lastUpdated: lastUpdated
			            },
			            waitMsg: (isCreating ? 'Adding' : 'Editing') + ' Phenotype Association ...',
			            success: function(form, action) {
			            	Ext.Msg.alert('Phenotype association ' + (isCreating ? 'added' : 'updated'), 'Phenotype association has been ' + (isCreating ? 'added' : 'updated') + '.');
							this.fireEvent('phenotypeAssociationChanged');			
			            },
			            failure: function(form, action) {
			            	var title = 'Cannot ' + (evidenceId == null ? 'add' : 'edit') + ' phenotype association';

					        switch (action.failureType) {
					            case Ext.form.Action.CLIENT_INVALID:
					                Ext.Msg.alert(title, 'Some fields are still invalid.');
					                break;
					            case Ext.form.Action.CONNECT_FAILURE:
					            	// This failure can happen when there is some unexpected exception thrown in the server side.
					                Ext.Msg.alert(title, 'Server communication failure: ' + action.response.status + ' - ' + action.response.statusText);
					                break;
					            case Ext.form.Action.SERVER_INVALID:
									var validateEvidenceValueObject = action.result;
									Ext.Msg.alert(title, Gemma.convertToEvidenceError(validateEvidenceValueObject).errorMessage,
										function() {
											if (validateEvidenceValueObject.userNotLoggedIn) {
												Gemma.AjaxLogin.showLoginWindowFn();
											}
										}
									);
									
									break;
					    	}
			            },
			            scope: this
			        });
			    }
			},
			showPubMedIdError: function() {
				errorPanel.showError(Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.pubmedIdInvalid);
			},
			hideErrorPanel: function() {
				errorPanel.hide();
			},
			validateForm: function(shouldSubmitAfterValidating) {
				var evidenceType = evidenceTypeComboBox.getValue();
				if (evidenceType === '') {
					hasError = true;
					errorPanel.hide();
				} else if (evidenceType === 'LiteratureEvidenceValueObject') {
					var phenotypeValueUris = phenotypesSearchPanel.validatePhenotypes();
					
					if (geneSearchComboBox.getValue() === '' ||
						phenotypeValueUris == null || phenotypeValueUris.length <= 0 ||
						literaturePanel.getPubMedId() === '' || evidenceCodeComboBox.getValue() === '') {
						hasError = true;
						errorPanel.hide();
					} else {
						var prevGeneValue = geneSearchComboBox.getValue();

						// Ask the controller to validate only after all fields are filled.
						PhenotypeController.validatePhenotypeAssociation(
							geneSearchComboBox.getValue(), phenotypeValueUris, evidenceType,  
							literaturePanel.getPubMedId(), descriptionTextArea.getValue(), evidenceCodeComboBox.getValue(), 
							evidenceId, lastUpdated, function(validateEvidenceValueObject) {

// TODO: I don't think this test is neccessary.								
							// Because using the controller to validate takes time, fields such as gene value could be changed (e.g. by clicking the Reset button). 
							// Thus, we should show error ONLY when a sample test field has not been changed after the controller call. I picked Gene as a sample.  
							if (prevGeneValue === geneSearchComboBox.getValue()) {
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
														this.submitForm();
													}
												},
												this);
										} else {
											this.submitForm();
										}
									}
								}
							}							
						}.createDelegate(this));
					}
				}
			},
			resetForm: function() {
// TODO: I don't think I need it.				
//				this.getForm().reset();
			    this.hideErrorPanel();
			    
				geneSearchComboBox.reset();
				phenotypesSearchPanel.reset();
				evidenceTypeComboBox.reset();	
			    literaturePanel.reset();
				descriptionTextArea.reset();
				evidenceCodeComboBox.reset();
				
				this.validateForm(false);
			},	
			buttonAlign: 'right',
			buttons: [
				{
				    text: 'Cancel',
				    handler: function() {
				    	this.hide();
				    },
					scope: this
				},
				{
					text: 'Reset',
					handler: function() {
						this.resetForm();
					},
					scope: this
				},
				{
				    text: 'OK',
				    formBind: true,
				    handler: function() {
						if (evidenceTypeComboBox.getValue() === 'LiteratureEvidenceValueObject') {
							this.validateForm(true);
						}
				    },
					scope: this
				}
			]		
		});
		
		this.superclass().initComponent.call(this);
    }
});
