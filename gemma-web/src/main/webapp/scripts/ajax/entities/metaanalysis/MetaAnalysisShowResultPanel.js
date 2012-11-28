/**
 * Panel for showing analyzed result  
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.MetaAnalysisShowResultPanel = Ext.extend(Gemma.WizardTabPanelItemPanel, {
	title: 'Results',
	nextButtonText: 'Save results',
	layout: 'border',
	metaAnalysis: null,
	numResultsShown: 500,
	initComponent: function() {
		var currentMetaAnalysis = this.metaAnalysis;

		var nextButton = this.createNextButton();
		
		var summaryLabel = new Ext.form.Label();
		
		var limitDisplayCombo = new Ext.form.ComboBox({
			width: 180,
			editable: false,  			
		    triggerAction: 'all',
		    mode: 'local',
		    store: new Ext.data.ArrayStore({
		        fields: [ 'shouldLimit', 'displayText' ],
		        data: [
		        	[ true, 'Display top ' + this.numResultsShown + ' results'],
		        	[ false, 'Display all results']
		        ]
		    }),
		    value: true, // By default, we should limit number of results shown.
		    valueField: 'shouldLimit',
		    displayField: 'displayText',
		    listeners: {
		    	select: function(combo, record, index) {
					if (!resultLabel.loadMask) {
						resultLabel.loadMask = new Ext.LoadMask(resultLabel.getEl(), {
							msg: "Loading ..."
						});
					}
					resultLabel.loadMask.show();
					
					// Defer the call. Otherwise, the loading mask does not show.
					Ext.defer(showResults, 1, this);
		    	}
		    }
		});
		
		var headerPanel = new Ext.Panel({
			region: 'north',
			layout: 'vbox',
			align: 'stretch',
		 	border: false,
		 	height: 90, // It must be set because it is in the north region.
			defaults: {
				margins: '10 0 0 10',
				style: 'white-space: nowrap;'					
			},
		 	items: [
		 		summaryLabel,
		 		limitDisplayCombo
		 	]
		});
		
		var resultLabel = new Ext.form.Label({
			region: 'center',
			autoScroll: true,
			style: 'background-color: #FFFFFF;' // By default, the background color is blue.
		});

		var thisPanelItems = [ 
			headerPanel,
			resultLabel
		];

		var showResults = function() {
			var resultText = '';

			if (resultLabel.loadMask) {
				resultLabel.loadMask.hide();
			}

			if (currentMetaAnalysis) {
				// Sort results by p-value.
				currentMetaAnalysis.results.sort(function(result1, result2) {  
		            return result1.metaPvalue < result2.metaPvalue ?
		            	-1 :
		            	result1.metaPvalue == result2.metaPvalue ?
		            		0 :
		            		1;  
				});
					
				var shouldLimitDisplayComboBeShown = currentMetaAnalysis.results.length > this.numResultsShown; 

				// Show limitDisplayCombo only when we have results more than this.numResultsShown.
				if (shouldLimitDisplayComboBeShown) {
					headerPanel.setHeight(80);
					limitDisplayCombo.show();
				} else {
					headerPanel.setHeight(40);
					limitDisplayCombo.hide();
				}

				summaryLabel.setText('<b>Number of genes analyzed</b>: ' + currentMetaAnalysis.numGenesAnalyzed + '<br />' +				
									 '<b>Number of genes with q-value < 0.1</b>: ' + currentMetaAnalysis.results.length, false);

				var stringStyle = 'style="padding: 0 10px 0 10px;"'; 
				var numberStyle = 'style="padding: 0 10px 0 10px; text-align: right;"';
				var directionStyle = 'style="padding: 0 10px 0 10px; text-align: center; font-size: 12px"';
				
				resultText += 
					'<table>' +
						'<tr>' +
							'<th ' + stringStyle + '>Symbol</th>' +
							'<th ' + stringStyle + '>Name</th>' +
							'<th ' + numberStyle + '>p-value</th>' +
							'<th ' + numberStyle + '>q-value</th>' +
							'<th ' + stringStyle + '>Direction</th>' +
						'</tr>';

				var metaAnalysisMaxIndex = shouldLimitDisplayComboBeShown && limitDisplayCombo.getValue() ?
					this.numResultsShown :
					currentMetaAnalysis.results.length;
					
				var numCharactersForDisplay = 80;
				
				for (var i = 0; i < metaAnalysisMaxIndex; i++) {
					result = currentMetaAnalysis.results[i];
				
					resultText += 
						'<tr>' +
							'<td ' + stringStyle + '>' + result.geneSymbol + '</td>' +
							'<td ' + stringStyle + '>' + Ext.util.Format.ellipsis(result.geneName, numCharactersForDisplay, true) + '</td>' +
							'<td ' + numberStyle + '>' + result.metaPvalue.toExponential(2) + '</td>' +
							'<td ' + numberStyle + '>' + result.metaQvalue.toExponential(2) + '</td>' +
							'<td ' + directionStyle + '>' + (result.upperTail ?
								'&uarr;' :
								'&darr;') + '</td>' +
						'</tr>';	
				}

				resultText += '</table>';
				
				resultLabel.setText(resultText, false);
				
				nextButton.setDisabled(false);
			} else {
				summaryLabel.setText('<b>No results were significant.</b>', false);
				nextButton.setDisabled(true);
			}
			
			this.doLayout();
			
		}.createDelegate(this);		
		
		// If this panel is for showing meta-analysis, show its results.
		// Otherwise, add buttons to the bottom panel for working on meta-analysis.
		if (this.metaAnalysis) {
			showResults(this.metaAnalysis);
		} else {
			thisPanelItems.push({
			 	region: 'south',
				layout: 'hbox',
			 	border: false,
			 	height: 40,
			 	padding: '10px 0 0 10px',
			 	items: [
			 		nextButton, {
			 			xtype: 'button',
			 			margins: '0 0 0 10',
			 			text: 'Modify selection',
			 			handler: function() {
							Ext.MessageBox.show({
								title: 'Modify selection',
								msg: 'If you proceed, you will lose your current results.',
								buttons: Ext.MessageBox.OKCANCEL,
								fn: function(button) {
									if (button === 'ok') {
										this.fireEvent('modifySelectionButtonClicked');
									}
								},
								scope: this
							});				 				
			 			},
			 			scope: this
			 		}
				]
			 });
		}
		
		var resultSetIdsToBeSaved = [];

		Ext.apply(this, {
			nextButtonHandler: function() {
				var saveResultWindow = new Gemma.MetaAnalysisSaveResultWindow({
					listeners: {
						okButtonClicked: function(name, description) {
                            var callParams = [];
                            callParams.push(resultSetIdsToBeSaved);
                            callParams.push(name);
                            callParams.push(description);
                            callParams.push({
                                callback : function(data) {
                                    var k = new Gemma.WaitHandler();
                                    k.handleWait(data, true);
                                    k.on('done', function(geneDifferentialExpressionMetaAnalysis) {
										// Assume if it is not null, saving result is successful.
										if (geneDifferentialExpressionMetaAnalysis != null) {
											this.fireEvent('resultSaved');
										}
                                    }.createDelegate(this));
                                }.createDelegate(this),
                                errorHandler : function(error) {
									Ext.Msg.alert("Result sets cannot be saved", error);
                                }.createDelegate(this)
                            });
                            DiffExMetaAnalyzerController.saveResultSets.apply(this, callParams);
						},
						scope: this
					}
				});
				saveResultWindow.show();
			},
			items: thisPanelItems,
			setResultSetIds: function(resultSetIds) {
				resultSetIdsToBeSaved = resultSetIds;

				// Reset components responsible for displaying results.
				summaryLabel.setText('', false);
				limitDisplayCombo.hide();
				resultLabel.setText('', false);

				var numResultsRequired = -1;

                var callParams = [];
                callParams.push(resultSetIds);
                callParams.push(numResultsRequired); // TODO: This should be removed after server code is updated not to require it.
                callParams.push({
                    callback : function(data) {
                        var k = new Gemma.WaitHandler();
                        k.handleWait(data, true);
                        k.on('done', function(geneDifferentialExpressionMetaAnalysis) {
							currentMetaAnalysis = geneDifferentialExpressionMetaAnalysis;
                        	showResults();
                        }.createDelegate(this));
                    }.createDelegate(this),
                    errorHandler : function(error) {
						Ext.Msg.alert("Result sets cannot be analyzed", error);
                    }.createDelegate(this)
                });
                DiffExMetaAnalyzerController.analyzeResultSets.apply(this, callParams);
			}
		});

		Gemma.MetaAnalysisShowResultPanel.superclass.initComponent.call(this);
	}
});
