/**
 * Panel for selecting factors of experiments  
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.MetaAnalysisSelectFactorPanel = Ext.extend(Gemma.WizardTabPanelItemPanel, {
	nextButtonText: 'Run meta-analysis',
	initComponent: function() {
		var experimentSelectedCount = 0;
		
		var nextButton = this.createNextButton();
		nextButton.disable();

		// Assume that if selectedResultSetAnalyses is not null, result sets are shown
		// for viewing only. So, editing is not allowed. 
		var generateExperimentComponents = function(experimentDetails, selectedResultSetAnalyses) {
			var radioGroup = new Ext.form.RadioGroup({
			    items: []
			});
			
			var experimentTitle = '<b>' + experimentDetails.accession + ' ' + experimentDetails.name + '</b>';

			var experimentTitleComponent;
			if (selectedResultSetAnalyses) {
				experimentTitleComponent = new Ext.form.DisplayField({
					style: 'margin: 10px 0 0 20px;', // DisplayField instead of Label is used. Otherwise, top margin is not honored. 
					html: experimentTitle
				});
			} else {
				experimentTitleComponent = new Ext.form.Checkbox({
	        		style: 'margin: 10px 0 0 10px;',
					boxLabel: experimentTitle,
					listeners: {
						check: function(checkbox, checked) {
							if (checked) {
								experimentSelectedCount++;
	
								if (radioGroup.getValue() == null) {
									for (var i = 0; i < radioGroup.items.length; i++) {
										if (!radioGroup.items[i].disabled) {
											radioGroup.items[i].setValue(true);
											break;
										}
									}
								}
							} else {
								experimentSelectedCount--;
								
								radioGroup.reset();
							}
							
							nextButton.setDisabled(experimentSelectedCount < 2);
						}
					}
        		});
			}
        	
        	var experimentResultSetsPanel = new Ext.Panel({
				bodyStyle: 'background-color: transparent; padding: 0 0 20px 40px;',				
        		border: false
        	});
        	
			var totalRadioCount = 0;
        	
			if (experimentDetails.differentialExpressionAnalyses.length == 0) {
				experimentResultSetsPanel.add(new Ext.form.Label({
					style: 'font-style: italic; ',
					disabled: true,
					html: 'No differential expression analysis available' + '<br />'
				}));
			} else {
				var analysesSummaryTree = new Gemma.DifferentialExpressionAnalysesSummaryTree({
			    	experimentDetails: experimentDetails,
			    	editable: false,
			    	style: 'padding-bottom: 20px;'
			    });
			    
			    var generateRadio = function(text, marginLeft, notSuitableForAnalysisMessage, inputValue) {
					var shouldRadioChecked = false;
					if (selectedResultSetAnalyses) {
						for (var i = 0; i < selectedResultSetAnalyses.length; i++) {
							if (selectedResultSetAnalyses[i].experimentId == experimentDetails.id) {
								if (selectedResultSetAnalyses[i].resultSetId == inputValue) {
									shouldRadioChecked = true;
								}
								break;
							}
						}
					}			    	
					return new Ext.form.Radio({
						checked: shouldRadioChecked,
						boxLabel: text + (notSuitableForAnalysisMessage == null ?  
							'' :
							' <i>' + notSuitableForAnalysisMessage + '</i>'),
						name: experimentDetails.id,
						style: 'margin-left: ' + marginLeft + 'px;',
						disabled: notSuitableForAnalysisMessage != null,
						inputValue: inputValue,
						listeners: {
							check: function(radio, checked) {
								if (checked) {
									if (experimentTitleComponent.isXType(Ext.form.Checkbox)) {									
										experimentTitleComponent.setValue(true);
									}
								}
							}
						}
					});	
			    };
			    
			    var checkSuitableForAnalysis = function(attributes) {
			    	var notSuitableForAnalysisMessage = null;
					if (attributes.numberOfFactors > 1) {
						notSuitableForAnalysisMessage = '(Not suitable - Analysis uses more than 1 factor)';
					} else if (attributes.numberOfFactorValues == null || attributes.numberOfFactorValues != 2) {
						notSuitableForAnalysisMessage = '(Not suitable - Analysis based on more than 2 groups)';
					}
					
					return notSuitableForAnalysisMessage;
			    };
			    
				// Sort the tree's child nodes.
				analysesSummaryTree.root.childNodes.sort(function(group1, group2) {
					var strippedText1 = Ext.util.Format.stripTags(group1.text);
					var strippedText2 = Ext.util.Format.stripTags(group2.text);
					
					return (strippedText1 < strippedText2 ?
								-1 :
								strippedText1 > strippedText2 ?
									1 :
									0);
				}); 

				Ext.each(analysesSummaryTree.root.childNodes, function(factorGroup, factorGroupIndex) {
					if (factorGroup.childNodes.length > 0) {
						var label = new Ext.form.Label({
							html: factorGroup.text + '<br />'
						});
						experimentResultSetsPanel.add(label);
						
						var factorRadioCount = 0;
						Ext.each(factorGroup.childNodes, function(factor, factorIndex) {
							var notSuitableForAnalysisMessage = checkSuitableForAnalysis(factor.attributes); 
								
							if (notSuitableForAnalysisMessage == null) {
								factorRadioCount++;
								totalRadioCount++;
							}
							var radio = generateRadio(factor.text, 15, notSuitableForAnalysisMessage, factor.attributes.resultSetId);							
							radioGroup.items.push(radio);
							experimentResultSetsPanel.add(radio);
						});
						label.setDisabled(factorRadioCount === 0);
					} else {
						var notSuitableForAnalysisMessage = checkSuitableForAnalysis(factorGroup.attributes); 
							
						if (notSuitableForAnalysisMessage == null) {
							totalRadioCount++;
						}
						var radio = generateRadio(factorGroup.text, 0, notSuitableForAnalysisMessage, factorGroup.attributes.resultSetId);
						radioGroup.items.push(radio);						
						experimentResultSetsPanel.add(radio);
					}
				});

				experimentResultSetsPanel.on('afterlayout', function() {
						analysesSummaryTree.drawPieCharts();    
					}, analysesSummaryTree, {
						single: true,
						delay: 100
					});
			} 
			
			if (totalRadioCount === 0) {
				experimentTitleComponent.setDisabled(true);
			}
			
			return { 
				hasEnabledRadioButtons: (totalRadioCount > 0),
				experimentTitleComponent: experimentTitleComponent,
				experimentResultSetsPanel: experimentResultSetsPanel
			}
		};
		
		var showExperiments = function(expressionExperimentIds, resultSetAnalyses) {
			this.maskWindow();

			analyzableExperimentsPanel.removeAll();
			nonAnalyzableExperimentsPanel.removeAll();
			
			nextButton.setDisabled(true);

			ExpressionExperimentController.loadExpressionExperiments(expressionExperimentIds, function(experiments) {
				var nonAnalyzableExperimentComponents = [];
				
				var addExperimentComponentsToPanel = function(experimentComponents, containerPanel, componentIndex) {
					var panel = new Ext.Panel({
						border: false,
						bodyStyle: (componentIndex % 2 === 0 ?
							'background-color: #FAFAFA;' :
							'background-color: #FFFFFF;')
					});
					panel.add(experimentComponents.experimentTitleComponent);
					panel.add(experimentComponents.experimentResultSetsPanel);
					containerPanel.add(panel);
				};
				
				var i;
				var analyzableExperimentsPanelIndex = 0;

				for (i = 0; i < experiments.length; i++) {
					var experimentComponents = generateExperimentComponents(experiments[i], resultSetAnalyses);
					
					if (experimentComponents.hasEnabledRadioButtons) {
						addExperimentComponentsToPanel(experimentComponents, analyzableExperimentsPanel, analyzableExperimentsPanelIndex);
						analyzableExperimentsPanelIndex++;
					} else {
						nonAnalyzableExperimentComponents.push(experimentComponents);
					}
				}
				
				
				for (var j = 0; j < nonAnalyzableExperimentComponents.length; j++) {
					addExperimentComponentsToPanel(nonAnalyzableExperimentComponents[j], nonAnalyzableExperimentsPanel, j);
				}
				
				
				this.doLayout();
				
				this.unmaskWindow();					
				
			}.createDelegate(this));
		}.createDelegate(this);
		
		var analyzableExperimentsPanel = new Ext.Panel({
			title: 'Analyzable experiments',
            region: 'center',
			autoScroll: true,
       	 	border: false
		});
		var nonAnalyzableExperimentsPanel = new Ext.Panel({
            title: 'Non-analyzable experiments',
            region: 'south',
			autoScroll: true,
			border: false,

            split: true,
            height: 200
		});

		var setDisabledChildComponentsVisible = function(container, visible) {
			if (container.items && container.items.length > 0) {
				Ext.each(container.items.items, function(item, index) {
					if (item) {
						if (item.items && item.items.length > 0) {
							setDisabledChildComponentsVisible(item, visible);
						} else if (item.disabled &&
								   (item instanceof Ext.form.Checkbox || item instanceof Ext.form.Label)) {
							item.setVisible(visible);
						}
					}
				});
			}
		};
		
		var findSelectedResultSetIds = function(resultSetIds, container) {
			if (container.items && container.items.length > 0) {
				Ext.each(container.items.items, function(item, index) {
					if (item) {
						if (item.items && item.items.length > 0) {
							findSelectedResultSetIds(resultSetIds, item);
						} else if (item instanceof Ext.form.Radio && item.getValue()) {
							resultSetIds.push(item.inputValue);
						}
					}
				});
			}
		};

		var	setPanelReadOnly = function(panel, isReadOnly) {
			panel.items.each(function(item)  {
				if (isReadOnly) {
					item.body.mask();
				} else {
					item.body.unmask();
				}
			});
		}
		
		var buttonPanel = new Ext.Panel({
		 	region: 'south',
		 	border: false,
		 	height: 40,
		 	padding: '10px 0 0 10px',
		 	items: [
		 		nextButton
			]
		 }); 

		var thisPanelItems;		 
		if (this.includedResultSetDetails) {
			var expressionExperimentIds = [];
			
			Ext.each(this.includedResultSetDetails, function(includedResultSetDetail, index) {
				expressionExperimentIds.push(includedResultSetDetail.experimentId);
			});
			
			showExperiments(expressionExperimentIds, this.includedResultSetDetails);
			
			thisPanelItems = [
				analyzableExperimentsPanel
			];
		} else {
			thisPanelItems = [{
					region: 'center',
					layout: 'border',
			 	 	items: [
					 	analyzableExperimentsPanel,
					 	nonAnalyzableExperimentsPanel
					]
				},
				buttonPanel 
			];
		}
		
		this.on({
			afterrender: function() {
				if (this.includedResultSetDetails) {
					// Defer the call. Otherwise, this panel cannot be set read-only.
					Ext.defer(
						function() {
							this.setPanelReadOnly();
						},
						1000,
						this);
				}
			}
		});		


		Ext.apply(this, {
			height: 600,
			layout: 'border',
			title: (this.includedResultSetDetails ? 'Selected' : 'Select') + ' factors',			
			getSelectedResultSetIds: function() {
				var selectedResultSetIds = [];

				findSelectedResultSetIds(selectedResultSetIds, this);

				return selectedResultSetIds;
			},
			items: thisPanelItems,
			tbar: [{
				xtype: 'checkbox',
				boxLabel: 'Hide non-analyzable experiments and factors',
				listeners: {
					check: function(checkbox, checked) {
						nonAnalyzableExperimentsPanel.setVisible(!checked);
						setDisabledChildComponentsVisible(this, !checked);
						this.doLayout();
					},
					scope: this
				}
			}],
			setSelectedExperimentIds: function(expressionExperimentIds) {
				showExperiments(expressionExperimentIds, null);				
			},
			setPanelReadOnly: function(msg, msgCls) {
				analyzableExperimentsPanel.header.mask(msg, msgCls);
				setPanelReadOnly(analyzableExperimentsPanel, true);
				
				if (!this.includedResultSetDetails) {				
					buttonPanel.body.mask();
					setPanelReadOnly(nonAnalyzableExperimentsPanel, true);
				}
			},
			unsetPanelReadOnly: function() {
				analyzableExperimentsPanel.header.unmask();
				buttonPanel.body.unmask();
				
				setPanelReadOnly(analyzableExperimentsPanel, false);
				setPanelReadOnly(nonAnalyzableExperimentsPanel, false);
			}			
		});
		
		Gemma.MetaAnalysisSelectFactorPanel.superclass.initComponent.call(this);
	}
});
