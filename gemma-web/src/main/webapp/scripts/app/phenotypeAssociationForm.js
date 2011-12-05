Ext.namespace('Gemma');

Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

var geneSelectedLabel = new Ext.form.Label({
	fieldLabel: "&nbsp;",
	labelSeparator : ''
});

Gemma.PhenotypeSearchComboBox = Ext.extend(Ext.form.ComboBox, {
	forceSelection: true,				
    store: new Ext.data.JsonStore({
		proxy: new Ext.data.DWRProxy(PhenotypeController.searchOntologyForPhenotypes),
	    fields: [ 'valueUri', 'value', 'alreadyPresentInDatabase', {
	    	name: 'comboText',
	    	convert: function(value, record) {
	    		return '<div style="font-size:12px;" class="x-combo-list-item" >' +
	    			record.value + '</div>';
	    	}
	    }]
	}),
    valueField: 'valueUri', 
    displayField: 'value',
    typeAhead: false,
    loadingText: 'Searching...',
    minChars: 2,
    width: 400,
    listWidth: 400,
    pageSize: 0,
    hideTrigger: true,
    triggerAction: 'all',
	getParams : function(query) {
		return [
			query,
			(geneSearchComboBox.getValue() == '' ? null : geneSearchComboBox.getValue()) ];
	},
	enableKeyEvents : true,
	listEmptyText : 'No results found',
	autoSelect: false,

	tpl: new Ext.XTemplate('<tpl for=".">' +
				'{[ this.renderItem(values) ]}' +
				'</tpl>', {
					renderItem: function(values){
						var freeTxtTpl = new Ext.XTemplate('<div style="font-size:11px;background-color:#FFFFE3" class="x-combo-list-item" ' +
											'ext:qtip="{value}">{value}</div>');
						var defaultTpl = new Ext.XTemplate('<div style="font-size:11px;background-color:#ECF4FF" class="x-combo-list-item" ' +
											'ext:qtip="{value}">{value}</div>');
						
						if (values.alreadyPresentInDatabase) {
							return freeTxtTpl.apply(values);
						} else { 
							return defaultTpl.apply(values);
						}
					}
				}),
	listeners: {
        specialkey: function(formField, e){
            // e.HOME, e.END, e.PAGE_UP, e.PAGE_DOWN,
            // e.TAB, e.ESC, arrow keys: e.LEFT, e.RIGHT, e.UP, e.DOWN
            if ( e.getKey() === e.TAB || e.getKey() === e.RIGHT  || e.getKey() === e.DOWN ) {
                this.expand();
            }else if (e.getKey() === e.ENTER ) {
                this.doQuery(this.lastQuery);
                this.collapse();
            }else if (e.getKey() === e.ESC ) {
                this.collapse();
            }
        }
//		focus: function(combo) {
//			geneSelectedLabel.setText('');
//		},
//		blur: function(combo) {
//			// I have to do the following because users may have selected a value from the combobox and then delete all text right after the selection.
//			if (this.value === '') {				
//				geneSelectedLabel.setText("", false);
//			} else {
//				var record = this.findRecord(this.valueField, this.value);
//				if (record) {
//					geneSelectedLabel.setText(record.data.comboText, false);
//				} else {
//					geneSelectedLabel.setText("", false);
//				}
//			}
//		},
//	    select: function(combo, record, index) {
//			geneSelectedLabel.setText(record.data.comboText, false);
//	        this.collapse();
//	        displayGene(this.value);
//	    }
	}
});

Gemma.GeneSearchComboBox = Ext.extend(Ext.form.ComboBox, {
						allowBlank: false,
						forceSelection: true,				
					    store: new Ext.data.JsonStore({
							proxy: new Ext.data.DWRProxy(GenePickerController.searchGenes),
						    fields: [ 'id', 'name', 'officialName', 'taxonCommonName', {
						    	name: 'comboText',
						    	convert: function(value, record) {
						    		return '<div style="font-size:12px;" class="x-combo-list-item" >' +
						    			record.officialName + ' <span style="color:grey">(' + record.taxonCommonName + ')</span></div>';
						    	}
						    }]
						}),
					    valueField: 'id',
					    displayField: 'name',
					    typeAhead: false,
					    loadingText: 'Searching...',
					    emptyText: 'Search genes by keyword',
					    minChars: 2,
					    width: 150,
					    listWidth: 400,
					    pageSize: 0,
					    hideTrigger: true,
					    triggerAction: 'all',
					    tpl: new Ext.XTemplate('<tpl for="."><div style="font-size:11px;background-color:#ECF4FF" class="x-combo-list-item" ' +
												'ext:qtip="{name}: {officialName} ({taxonCommonName})"><b>{name}</b>: {officialName} <span style="color:grey">({taxonCommonName})</span></div></tpl>'),
						listeners: {
							focus: function(combo) {
								geneSelectedLabel.setText('');
							},
							blur: function(combo) {
								// I have to do the following because users may have selected a value from the combobox and then delete all text right after the selection.
								if (this.value === '') {				
									geneSelectedLabel.setText("", false);
								} else {
									var record = this.findRecord(this.valueField, this.value);
									if (record) {
										geneSelectedLabel.setText(record.data.comboText, false);
									} else {
										geneSelectedLabel.setText("", false);
									}
								}
							},
						    select: function(combo, record, index) {
								geneSelectedLabel.setText(record.data.comboText, false);
						        this.collapse();
						    }
						},
					   	getParams : function(query) {
							return [ query, null ];
						}
					});

var geneSearchComboBox = new Gemma.GeneSearchComboBox();

Gemma.PubMedIdField = Ext.extend(Ext.form.NumberField, {
	allowDecimals: false,
	minLength: 7,
	maxLength: 9,
	allowNegative: false,
	width: 100,
	autoCreate: {tag: 'input', type: 'text', size: '20', autocomplete: 'off', maxlength: '9'}
});

Gemma.LiteratureFieldSet = Ext.extend(Ext.form.FieldSet, {
    initComponent: function() {
		var pubMedIdField = new Gemma.PubMedIdField();
		var bibliographicReferenceDetailsPanel = new Gemma.BibliographicReference.DetailsPanel({
       		fieldLabel: '&nbsp;',
       		labelSeparator: '',
       		myParent: this
		});
        
		var pubMedStore = new Ext.data.Store({
			proxy: new Ext.data.DWRProxy({
				        apiActionToHandlerMap: {
			    	        read: {
			        	        dwrFunction: PhenotypeController.findBibliographicReference,
			            	    getDwrArgsFunction: function(request){
			            	    	return [request.params["pubMedId"]];
				                }
			    	        }
				        }
			    	}),
			reader: new Ext.data.JsonReader({
		        fields: Gemma.BibliographicReference.Record
			})
		});
		pubMedStore.on({
		    'load': {
		        fn: function(store, records, options){
		        	if (pubMedStore.getTotalCount() > 0) {
						bibliographicReferenceDetailsPanel.updateFields(pubMedStore.getAt(0));
						bibliographicReferenceDetailsPanel.show();
						bibliographicReferenceDetailsPanel.myParent.doLayout();
		        	} else {
						bibliographicReferenceDetailsPanel.hide();
						bibliographicReferenceDetailsPanel.myParent.doLayout();
		        	}
		        },
		        scope:this
		    }
		});    
		
		
		var pubMedIdFieldPanel = new Ext.Panel({
       		fieldLabel: 'PudMed Id',
			border: false,
			bodyStyle: 'padding-bottom: 3px;',
			layout: 'hbox',
			align: 'stretch',
	    	items: [ 
				pubMedIdField,
				{
					xtype: 'button',
					icon:'/Gemma/images/icons/ok16.png',
					tooltip: 'Check PubMed Id',
					handler: function() {
						var pubMedId = pubMedIdField.getValue();
						pubMedStore.reload({
							params: {
								'pubMedId': pubMedId
							}
						});
		    		}
		    	}					
	    	] 
		}); 
        
		Ext.apply(this, {
	        title: 'Literature Evidence',
	        collapsible: true,
	        hidden: true,
	        autoHeight:true,
			width: 700,        
			labelWidth: 120,
	        defaultType: 'textfield',
	        items :[
	        	pubMedIdFieldPanel,
       			bibliographicReferenceDetailsPanel
	        ]
		});

		Gemma.LiteratureFieldSet.superclass.initComponent.call(this);
		
		bibliographicReferenceDetailsPanel.hide();
    }
});

Ext.onReady(function() {

	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	var phenotypeComboCount = 0;
	
	function createPhenotypeCombo() {
		var name = 'phenotype' + phenotypeComboCount;

		var comboPanel = new Ext.Panel({
		border: false,
		bodyStyle: 'padding-bottom: 3px;',
		layout: 'hbox',
		align: 'stretch',
	    	items: [ 
//	    		phenotypeCombo
new Gemma.PhenotypeSearchComboBox()
	    	] 
		}); 
		
		if (phenotypeComboCount > 0) {
			comboPanel.add({
					border: false,
					padding: 2
				},
				{
					xtype: 'button', // Remove phenotype button
					text: '<b> &nbsp;- &nbsp;</b>',
					handler: function() {
						phenotypePanel.remove(comboPanel);
						phenotypePanel.doLayout();
						phenotypeComboCount--;
		    		}
		    	});
		}
		
		phenotypeComboCount++;
		
		return comboPanel;
	}	
	
	var phenotypePanel = new Ext.Panel({
		fieldLabel: 'Phenotype',
		border: false,
		layout: 'form',
    	items: [ 
    		createPhenotypeCombo()
    	] 
	});

	var addMorePhenotypeButton = new Ext.Button({
		text: '<b> &nbsp;+ &nbsp;</b>', // 'Add more Phenotype',
		fieldLabel: "&nbsp;",
		labelSeparator : '',
		handler: function() {
			phenotypePanel.add(createPhenotypeCombo());
			phenotypePanel.doLayout();
		}
	});
	
	
	
// relevant pudmed
	var relevantPudMedCount = 0;
	
	function createRelevantPudMedTextfield() {
		var name = 'relevantPudMed' + relevantPudMedCount;

		var relevantPudMedTextfield = new Ext.form.TextField({
			id: name,
			name: name,
			width: 100
		});
		
		var textfieldPanel = new Ext.Panel({
			border: false,
			bodyStyle: 'padding-bottom: 3px;',
			layout: 'hbox',
			align: 'stretch',
	    	items: [ 
	    		relevantPudMedTextfield
	    	] 
		}); 
		
		if (relevantPudMedCount > 0) {
			textfieldPanel.add({
					border: false,
					padding: 2
				},
				{
					xtype: 'button',	    			
					text: '<b> &nbsp;- &nbsp;</b>', //'Remove' ,
					handler: function() {
						relevantPudMedPanel.remove(textfieldPanel);
						relevantPudMedPanel.doLayout();
						relevantPudMedCount--;
		    		}
		    	});
		}
		
		relevantPudMedCount++;
		
		return textfieldPanel;
	}	
	
	var relevantPudMedPanel = new Ext.Panel({
		fieldLabel: 'Relevant PudMed Id',
		border: false,
		layout: 'form',
    	items: [ 
    		createRelevantPudMedTextfield()
    	] 
	});

	var addMoreRelevantPudMedButton = new Ext.Button({
		text: '<b> &nbsp;+ &nbsp;</b>', //'Add more Relevant PudMed Id',
		fieldLabel: "&nbsp;",
		labelSeparator : '',
		handler: function() {
			relevantPudMedPanel.add(createRelevantPudMedTextfield());
			relevantPudMedPanel.doLayout();
		}
	});
	
	

	
	
// tags
	var experimentalTagCount = 0;
	
	function createExperimentalTag() {
		var categoryName = 'experimentalTagCategory' + experimentalTagCount;
		var valueName = 'experimentalTagValue' + experimentalTagCount;

		var tagPanel = new Ext.Panel({
			border: false,
			bodyStyle: 'padding-bottom: 3px;',
			layout: 'hbox',
			align: 'stretch',
	    	items: [ 
		    	{
					xtype: 'combo',
					id: categoryName,
					name: categoryName,
//					fieldLabel: '',
					mode: 'local',
					store: new Ext.data.ArrayStore({
						fields:  ['id', 'tagCategory'],
						data: [['1', 'BioSource'], ['2', 'DevelopmentalStage'], ['3', 'Experiment'], ['4', 'ExperimentDesign'], ['5', 'OrganismPart'], ['6', 'Treatment']]
					}),
					forceSelection: true,				
					displayField:'tagCategory',
		//			valueField: 'id',
					width: 150,
					typeAhead: true,
					triggerAction: 'all',
					selectOnFocus:true
		    	},
		    	{
		    		xtype: 'textfield',
					id: valueName,
					name: valueName,
					width: 375
		    	}
	    	] 
		}); 
		
		if (experimentalTagCount > 0) {
			tagPanel.add({
					border: false,
					padding: 2
				},
				{
					xtype: 'button',	    			
					text: '<b> &nbsp;- &nbsp;</b>', //'Remove' ,
					handler: function() {
						experimentTagPanel.remove(tagPanel);
						experimentTagPanel.doLayout();
						experimentalTagCount--;
		    		}
		    	});
		}
		
		experimentalTagCount++;
		
		return tagPanel;
	}	
	
	var experimentTagPanel = new Ext.Panel({
		fieldLabel: 'Tags',
		border: false,
		layout: 'form',
    	items: [ 
    		createExperimentalTag()
    	] 
	});

	var addMoreExperimentalTagButton = new Ext.Button({
		text: '<b> &nbsp;+ &nbsp;</b>', //'Add more Experimental Tag',
		fieldLabel: "&nbsp;",
		labelSeparator : '',
		handler: function() {
			experimentTagPanel.add(createExperimentalTag());
			experimentTagPanel.doLayout();
		}
	});
	
	
	
	
	
	
	
	var experimentalFieldSet = new Ext.form.FieldSet({
        title: 'Experimental Evidence',
        collapsible: true,
        hidden: true,
        autoHeight:true,
		width: 700,        
		labelWidth: 120,
//        defaults: {width: 210},
        defaultType: 'textfield',
        items :[{
                fieldLabel: 'Primary PudMed Id',
                name: 'primaryPudMedId',
                width: 100
            },
            relevantPudMedPanel,
            addMoreRelevantPudMedButton,
            experimentTagPanel,
            addMoreExperimentalTagButton
        ]
	});
	
	var externalDatabaseFieldSet = new Ext.form.FieldSet({
        title: 'External Database Evidence',
        collapsible: true,
        hidden: true,
        autoHeight:true,
		width: 700,        
		labelWidth: 120,
        defaults: {width: 550},
        defaultType: 'textfield',
        items :[			{
				xtype: 'combo',
				name: 'externalDatabaseName',
				fieldLabel: 'Database',
				mode: 'local',
				store: new Ext.data.ArrayStore({
					fields:  ['id', 'databaseName'],
					data: [['1', 'SFARI']]
				}),
				forceSelection: true,				
				displayField:'databaseName',
	//			valueField: 'id',
				width: 200,
				typeAhead: true,
				triggerAction: 'all',
				selectOnFocus:true

            },{
                fieldLabel: 'Link',
                name: 'databaseLink'
            }
        ]
	});
	
//	var literatureFieldSet = new Ext.form.FieldSet({
//        title: 'Literature Evidence',
//        collapsible: true,
//        hidden: true,
//        autoHeight:true,
//		width: 700,        
//		labelWidth: 120,
//        defaults: {width: 210},
//        defaultType: 'textfield',
//        pubMedIdField: new Gemma.PubMedIdField({
//        		fieldLabel: 'PudMed Id'
//        }), 
//        items :[
//        	this.pubMedIdField
//        ]
//	});

	var literatureFieldSet = new Gemma.LiteratureFieldSet();

	
	var createPhenotypeFormPanel = new Ext.FormPanel({ 
		url: '/Gemma/processPhenotypeAssociationCreateForm.html',  // TODO for Frances
		title: 'Create Phenotype Association',
		width: 250,
//autoHeight: true,
height: 80,
monitorValid : true,
		labelWidth: 130,
		autoScroll: true,
		defaults: {
			blankText: 'This field is required'
		},
		bodyStyle: 'padding: 5px;',
		items: [
			{
				xtype: 'compositefield',
			    fieldLabel: 'Gene',
			    items: [
					geneSearchComboBox,	    
					geneSelectedLabel
			    ]
			},
			phenotypePanel,
			addMorePhenotypeButton,
			{
				border: false,
				padding: 5
			},
			{
				xtype: 'combo',
				name: 'typeOfEvidence',
				fieldLabel: 'Type of Evidence',
				allowBlank: false,
				mode: 'local',
				store: new Ext.data.ArrayStore({
					fields:  ['id', 'evidenceType'],
//					data: [['1', 'Experimental'], ['2', 'External Database'], ['3', 'Literature']]
data: [['3', 'Literature']]
				}),
				forceSelection: true,				
				displayField:'evidenceType',
	//			valueField: 'id',
				width: 200,
				typeAhead: true,
				triggerAction: 'all',
				selectOnFocus:true,
				listeners: {
					'select': function(combo, record, index) {
						switch (record.data.id) {
							case '1':
								experimentalFieldSet.show();
								externalDatabaseFieldSet.hide();
								literatureFieldSet.hide();
								break;
							case '2':
								experimentalFieldSet.hide();
								externalDatabaseFieldSet.show();
								literatureFieldSet.hide();
								break;
							case '3':
								experimentalFieldSet.hide();
								externalDatabaseFieldSet.hide();
								literatureFieldSet.show();
								break;
						}
						createPhenotypeFormPanel.doLayout();
					}
				}
			},
			experimentalFieldSet,
			externalDatabaseFieldSet,
			literatureFieldSet,
			{
				xtype: 'textarea',
				name: 'descriptionOfEvidence',
				fieldLabel: 'Note',
				width: 565
			}
		],
		buttonAlign: 'left',
		buttons: [
			{
			    text: 'Create',
			    formBind: true,
			    style: 'margin: 0 0 0 130px;',
			    handler: function() {
			        if (createPhenotypeFormPanel.getForm().isValid()) {
			            createPhenotypeFormPanel.getForm().submit({
			                url: 'form-submit.php',
			                waitMsg: 'Create Phenotype Association ...',
			                success: function(form, action) {
			                    // server responded with success = true
			//                    var result = action.result;
			                },
			                failure: function(form, action) {
			                    if (action.failureType === Ext.form.Action.CONNECT_FAILURE) {
			                        Ext.Msg.alert('Error', 'Status:' + action.response.status + ': ' + action.response.statusText);
			                    }
			                    if (action.failureType === Ext.form.Action.SERVER_INVALID){
			                        // server responded with success = false
			                        Ext.Msg.alert('Invalid', action.result.errormsg);
			                    }
			                }
			            });
			        }
			    }
			},
			{
			    text: 'Reset',
			    handler: function(){
			        createPhenotypeFormPanel.getForm().reset();
			        geneSelectedLabel.setText('');
			    }
			}]		
	}); // createPhenotypeFormPanel
	

	new Gemma.GemmaViewPort({
		defaults: {	border: false },
		style: 'background-color: white;',
	 	centerPanelConfig: createPhenotypeFormPanel
	});
});
