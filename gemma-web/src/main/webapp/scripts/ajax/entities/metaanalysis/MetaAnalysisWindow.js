/**
 * This window lets users add new meta-analysis and view analyzed result. 
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.MetaAnalysisWindow = Ext.extend(Ext.Window, {
	metaAnalysis: null, // It can be set to use this window to display included result sets.
	layout: 'fit',
	constrain: true,  // Should not be modal so that other window can be opened.
	width: 740,
	height: 600,
	shadow: true,
	closeAction: 'close',
	initComponent: function() {
		var selectExperimentPanel;
		
		if (!this.metaAnalysis) {
			selectExperimentPanel = new Gemma.MetaAnalysisSelectExperimentPanel({
				listeners: {
					nextButtonClicked: function(panel) {
						var experimentOrExperimentSet = this.getSelectedExperimentOrExperimentSetValueObject();
	
						selectFactorPanel.setSelectedExperimentIds(
							experimentOrExperimentSet.expressionExperimentIds ?
								experimentOrExperimentSet.expressionExperimentIds :
								[ experimentOrExperimentSet.id ]);
					}
				}
			});
		}

		var selectFactorPanel = new Gemma.MetaAnalysisSelectFactorPanel({
			metaAnalysis: this.metaAnalysis,			
			listeners: {
				nextButtonClicked: function(panel) {
					showResultPanel.setResultSetIds(this.getSelectedResultSetIds());

					var maskMessage = 'Read-only. To modify selection, click the "Modify selection" button in the Results section.';					
					selectExperimentPanel.body.mask(maskMessage, 'read-only-warning');
					selectFactorPanel.setPanelReadOnly(maskMessage, 'read-only-warning');
				}
			}
		});		
		
		
		var showResultPanel = new Gemma.MetaAnalysisShowResultPanel({
			metaAnalysis: this.metaAnalysis,			
			listeners: {
				modifySelectionButtonClicked: function() {
					selectExperimentPanel.body.unmask();
					selectFactorPanel.unsetPanelReadOnly();
					
					var indexOfTabToBeActivated = 1;
					tabPanel.setActiveTab(indexOfTabToBeActivated);
					
					for (var i = indexOfTabToBeActivated + 1; i < tabPanel.items.length; i++) {
						tabPanel.getComponent(i).disable();
					}
				}
			}
		});
		this.relayEvents(showResultPanel, ['resultSaved']);
		
		
		var tabPanelItems = this.metaAnalysis ?
			[
				selectFactorPanel,
				showResultPanel
			] :
			[
				selectExperimentPanel,
				selectFactorPanel,
				showResultPanel
			];
	
		
		var tabPanel = new Gemma.WizardTabPanel({
			useCustomInsteadOfTabIcons: (!this.metaAnalysis),
			enableOnlyFirstTab: (!this.metaAnalysis),
			items: tabPanelItems
		});

		Ext.apply(this, {
			items: [
				tabPanel			
			]
		});

		Gemma.MetaAnalysisWindow.superclass.initComponent.call(this);
	}
});
