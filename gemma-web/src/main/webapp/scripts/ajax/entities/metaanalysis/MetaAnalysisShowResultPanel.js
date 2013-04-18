/**
 * Panel for showing analyzed result  
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.MetaAnalysisShowResultPanel = Ext.extend(Gemma.WizardTabPanelItemPanel, {
	title: 'Results',
	metaAnalysis: null,
	defaultQvalueThreshold: null,
	numResultsLimit: 500,
	nextButtonText: 'Save results',
	layout: 'border',
	initComponent: function() {
		var nextButton = this.createNextButton();
		
		var resultPanel = new Gemma.MetaAnalysisResultPanel({
			region: 'center',
			metaAnalysis: this.metaAnalysis,
			defaultQvalueThreshold: this.defaultQvalueThreshold,
			showLimitDisplayCombo: true,
			showDownloadButton: true
		});

		var thisPanelItems = [ 
			resultPanel		
		];

		// If this panel is not for showing meta-analysis, add buttons to the bottom panel for working on meta-analysis.
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
                                callback : function(taskId) {
                                    var task = new Gemma.ObservableSubmittedTask({'taskId':taskId});
                                    task.showTaskProgressWindow({'expandLogOnFinish': true});
                                    task.on('task-completed', function(metaAnalysisReturned) {
										// Assume if it is null, saving result is NOT successful.
										if (metaAnalysisReturned === null) {
											Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.MetaAnalysisShowResultPanel.ErrorTitle.resultSetsNotSaved,
														  Gemma.HelpText.CommonErrors.errorUnknown);
										} else {
											this.fireEvent('resultSaved');
										}
                                    }.createDelegate(this));
									task.on('task-failed', function(data) {
										Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.MetaAnalysisShowResultPanel.ErrorTitle.resultSetsNotSaved,
													  data);
									}.createDelegate(this));
                                }.createDelegate(this),
                                errorHandler : function(error) {
									Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.MetaAnalysisShowResultPanel.ErrorTitle.resultSetsNotSaved, error);
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

				resultPanel.clear();			

                var callParams = [];
                callParams.push(resultSetIds);
                callParams.push({
                    callback : function(taskId) {
                        var task = new Gemma.ObservableSubmittedTask({'taskId':taskId});
                        task.showTaskProgressWindow({'expandLogOnFinish': true});
                        task.on('task-completed', function(metaAnalysisReturned) {
							resultPanel.setMetaAnalysis(metaAnalysisReturned);
							nextButton.setDisabled(!metaAnalysisReturned);
                        }.createDelegate(this));
						task.on('task-failed', function() {
							// Argument data is not used because data is Java exception class name which is not useful to the user.
							resultPanel.clear(Gemma.HelpText.WidgetDefaults.MetaAnalysisShowResultPanel.ErrorMessage.resultSetsNotAnalyzed);
							nextButton.setDisabled(true);
						}.createDelegate(this));
                    }.createDelegate(this),
                    errorHandler : function(error) {
						Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.MetaAnalysisShowResultPanel.ErrorTitle.resultSetsNotAnalyzed, error);
                    }.createDelegate(this)
                });
                DiffExMetaAnalyzerController.analyzeResultSets.apply(this, callParams);
			}
		});

		Gemma.MetaAnalysisShowResultPanel.superclass.initComponent.call(this);
	}
});
