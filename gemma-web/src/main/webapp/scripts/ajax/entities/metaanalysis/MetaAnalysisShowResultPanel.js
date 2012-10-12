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
	numResultsRequired: 100,
	metaAnalysis: null,
	initComponent: function() {
		var nextButton = this.createNextButton();
		
		var resultLabel = new Ext.form.Label({
			region: 'center',
			autoScroll: true
		});
 
		var showResults = function(geneDifferentialExpressionMetaAnalysis) {
			var resultText = '';

			if (geneDifferentialExpressionMetaAnalysis) {
				resultText += 
					'<b>Number of genes analyzed</b>: ' + geneDifferentialExpressionMetaAnalysis.numGenesAnalyzed + '<br />' +
					'<b>Number of genes with q-value < 0.1</b>: ' + geneDifferentialExpressionMetaAnalysis.numResultsInitially + '<br />' 
					
				if (geneDifferentialExpressionMetaAnalysis.numResultsInitially > this.numResultsRequired) {
					resultText += '<b>Only the top 100 results are displayed.</b><br />';
				}
					
				resultText += '<br />';
				
				resultText += 
					'<table>' +
						'<tr>' +
							'<th>Symbol</th>' +
							'<th>Name</th>' +
							'<th style="width: 80px">p-value</th>' +
							'<th style="width: 80px">q-value</th>' +
							'<th>Mean log fold change</th>' +
							'<th>Results used</th>' +
						'</tr>';
				
				Ext.each(geneDifferentialExpressionMetaAnalysis.results, function(result, index) {
					resultText += 
						'<tr>' +
							'<td>' + result.gene.officialSymbol + '</td>' +
							'<td>' + result.gene.officialName + '</td>' +
							'<td>' + result.metaPvalue.toExponential(3) + '</td>' +
							'<td>' + result.metaQvalue.toExponential(3) + '</td>' +
							'<td>' + result.meanLogFoldChange.toExponential(3) + '</td>' +
							'<td>' + result.resultsUsedCount + '</td>' +
						'</tr>';
				});
				
				resultText += '</table>';
			} else {
				resultText += 'No results were significant.';
				nextButton.setDisabled(true);
			}
			resultLabel.setText(resultText, false);
			
			this.unmaskWindow();					
			
		}.createDelegate(this);		
		
		
		var thisPanelItems = [ resultLabel ];

		// If this panel is not for read-only, add buttons at the bottom of the panel.
		if (!this.metaAnalysis) {
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
		
		if (this.metaAnalysis) {
			showResults(this.metaAnalysis);
		}
		
		var resultSetIdsToBeSaved = [];
		
		Ext.apply(this, {
			nextButtonHandler: function() {
				var saveResultWindow = new Gemma.MetaAnalysisSaveResultWindow({
					listeners: {
						okButtonClicked: function(name, description) {
							this.maskWindow('Saving results ...');
							
							ExpressionExperimentController.saveResultSets(resultSetIdsToBeSaved, name, description, function(geneDifferentialExpressionMetaAnalysis) {
								// Assume if it is not null, saving result is successful.
								if (geneDifferentialExpressionMetaAnalysis != null) {
									this.fireEvent('resultSaved');
								}
								
								this.unmaskWindow();
							}.createDelegate(this));
						},
						scope: this
					}
				});
				saveResultWindow.show();
			},
			layout: 'border',
			items: thisPanelItems,
			setResultSetIds: function(resultSetIds) {
				this.maskWindow('Running meta-analysis ...');
				
				resultSetIdsToBeSaved = resultSetIds;

				resultLabel.setText('', false);
				
				ExpressionExperimentController.analyzeResultSets(resultSetIds, this.numResultsRequired, {
					callback: function(geneDifferentialExpressionMetaAnalysis) {
							showResults(geneDifferentialExpressionMetaAnalysis);					
						}.createDelegate(this),
					timeout: 900000, // = 15 * 60 * 1000 (15 minutes)
					errorHandler: function(result) {
							Ext.Msg.show({
								title: Gemma.HelpText.CommonWarnings.Timeout.title,
								msg: Gemma.HelpText.CommonWarnings.Timeout.text,
								buttons: Ext.Msg.OK,
								fn: function(btn, text, opt) {
									if (btn == 'ok'){
								    	this.unmaskWindow();
								        this.fireEvent('modifySelectionButtonClicked');
								    }
							   	},
							   scope: this
							});						
						}.createDelegate(this)
				});
			}
		});

		Gemma.MetaAnalysisShowResultPanel.superclass.initComponent.call(this);
	}
});
