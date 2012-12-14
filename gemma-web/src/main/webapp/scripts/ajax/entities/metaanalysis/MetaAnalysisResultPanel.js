/**
 * Panel for showing meta-analysis result  
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.MetaAnalysisResultPanel = Ext.extend(Ext.Panel, {
	metaAnalysis: null,
	defaultQvalueThreshold: null,
	showLimitDisplayCombo: true,
	numResultsLimit: 500,
	border: false,
	layout: 'border',
	initComponent: function() {
		var totalNumberOfResults = 0;
		
		var summaryLabel = new Ext.form.Label();
		
		var limitDisplayCombo = this.showLimitDisplayCombo ? 
			new Ext.form.ComboBox({
				width: 180,
				editable: false,  			
			    triggerAction: 'all',
			    mode: 'local',
			    store: new Ext.data.ArrayStore({
			        fields: [ 'shouldLimit', 'displayText' ],
			        data: [
			        	[ true, 'Display top ' + this.numResultsLimit + ' results'],
			        	[ false, 'Display all results']
			        ]
			    }),
			    value: true, // By default, we should limit number of results shown.
			    valueField: 'shouldLimit',
			    displayField: 'displayText',
			    listeners: {
			    	select: function(combo, record, index) {
						this.showResults();
			    	},
					scope: this
			    }
			}) :
			null;
		
		var headerPanelItems = [ summaryLabel ];
		if (this.showLimitDisplayCombo) {
			headerPanelItems.push(limitDisplayCombo);
		}

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
		 	items: headerPanelItems
		});
		
		var resultLabel = new Ext.form.Label({
			region: 'center',
			autoScroll: true,
			style: 'background-color: #FFFFFF;' // By default, the background color is blue.
		});

		var showResultsWithoutMask = function(threshold) {
			var resultText = '';

			if (resultLabel.loadMask) {
				resultLabel.loadMask.hide();
			}

			if (this.metaAnalysis) {
				if (threshold != null && threshold <= 0) {
					resultLabel.setText('');
					summaryLabel.setText('');
				} else {
					// Sort results by p-value.
					this.metaAnalysis.results.sort(function(result1, result2) {  
			            return result1.metaPvalue < result2.metaPvalue ?
			            	-1 :
			            	result1.metaPvalue == result2.metaPvalue ?
			            		0 :
			            		1;  
					});
						
					// Show limitDisplayCombo only when we have results more than this.numResultsLimit.
					var shouldLimitDisplayComboBeShown = this.showLimitDisplayCombo && this.metaAnalysis.results.length > this.numResultsLimit; 
	
					if (shouldLimitDisplayComboBeShown) {
						headerPanel.setHeight(80);
						limitDisplayCombo.show();
					} else {
						headerPanel.setHeight(40);
						if (limitDisplayCombo) {
							limitDisplayCombo.hide();
						}
					}
	
					var stringStyle = 'style="padding: 0 10px 0 10px; vertical-align: top;"'; 
					var numberStyle = 'style="padding: 0 10px 0 10px; vertical-align: top; text-align: right; white-space: nowrap;"';

					var directionStyleProperties = 'padding: 0 10px 0 10px; text-align: center; font-size: 12px; vertical-align: top; ';
					var upDirectionStyle = 'style="' + directionStyleProperties + ' color: #0B6138;"'; // green
					var downDirectionStyle = 'style="' + directionStyleProperties + ' color: #FF0000;"'; // red
					
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
						this.numResultsLimit :
						this.metaAnalysis.results.length;
						
					var NUM_CHARACTERS_FOR_DISPLAY = 100;
					
					var numResultsDisplayed = 0;

					for (var i = 0; i < metaAnalysisMaxIndex; i++) {
						result = this.metaAnalysis.results[i];

						// If threshold is null, we don't do any "filtering".
						if (threshold == null || result.metaQvalue < threshold) {
							numResultsDisplayed++;
							resultText += 
								'<tr>' +
									'<td ' + stringStyle + '>' + result.geneSymbol + '</td>' +
									'<td ' + stringStyle + '>' + Ext.util.Format.ellipsis(result.geneName, NUM_CHARACTERS_FOR_DISPLAY, true) + '</td>' +
									'<td ' + numberStyle + '>' + result.metaPvalue.toExponential(2) + '</td>' +
									'<td ' + numberStyle + '>' + result.metaQvalue.toExponential(2) + '</td>' +
									(result.upperTail ?
										'<td ' + upDirectionStyle + '>&uarr;</td>' :
										'<td ' + downDirectionStyle + '>&darr;</td>') +
								'</tr>';
						}
					}
	
					resultText += '</table>';
					
					resultLabel.setText(resultText, false);
					
					totalNumberOfResults = (threshold == null ? 
						this.metaAnalysis.results.length :
						numResultsDisplayed);
					
					summaryLabel.setText('<b>Number of genes analyzed</b>: ' + this.metaAnalysis.numGenesAnalyzed + '<br />' +				
										 '<b>Number of genes with q-value < ' + 
										 	(threshold == null ? 
										 		this.defaultQvalueThreshold :
										 		threshold) + '</b>: ' +
										 totalNumberOfResults, false);
				}					
			} else {
				summaryLabel.setText('<b>No results were significant.</b>', false);
			}
			
			this.doLayout();
		}.createDelegate(this);
			
		Ext.apply(this, {
			getTotalNumberOfResults: function() {
				return totalNumberOfResults;
			},
			setMetaAnalysis: function(metaAnalysis) {
				this.metaAnalysis = metaAnalysis;
				showResultsWithoutMask();
			},
			// Reset components responsible for displaying results.
			clear: function(errorMessage) {
				summaryLabel.setText(errorMessage ? '<b>' + errorMessage + '</b>': '', false);
				resultLabel.setText('', false);
				
				if (limitDisplayCombo) {
					limitDisplayCombo.hide();
				}
				
				this.doLayout();
			},
			showResults: function(threshold) {
				if (resultLabel.getEl()) {
					if (!resultLabel.loadMask) {
						resultLabel.loadMask = new Ext.LoadMask(resultLabel.getEl(), {
							msg: "Loading ..."
						});
					}
					resultLabel.loadMask.show();
	
					// Defer the call. Otherwise, the loading mask does not show.
					Ext.defer(showResultsWithoutMask, // function to call
						10,                           // delay in milliseconds
						this,                         // scope
						[ threshold ]);               // arguments to the function
				} else {
					showResultsWithoutMask(threshold);
				}
			},
			items: [
				headerPanel,
				resultLabel
			]
		});
		
		if (this.metaAnalysis) {
			showResultsWithoutMask();
		}

		Gemma.MetaAnalysisResultPanel.superclass.initComponent.call(this);
	}
});
