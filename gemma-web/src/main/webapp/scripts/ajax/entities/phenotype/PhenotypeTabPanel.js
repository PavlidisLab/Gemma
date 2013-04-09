/**
 * This tab panel contains phenotype tree grid panel and phenotype grid panel.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.PhenotypeTabPanel = Ext.extend(Ext.TabPanel, {
	phenotypeStoreProxy: null,
	activeTab: 0,
	width: 330,
	split: true,
    initComponent: function() {
		var getGridPanelConfig = function(isFirstTab) {
			return {
		    	// Only the first tab's grid panel should have storeAutoLoad set to true.
				storeAutoLoad: isFirstTab,
				phenotypeStoreProxy: this.phenotypeStoreProxy
			}
		}.createDelegate(this);
    	
		var gridPanels = [
			new Gemma.PhenotypeTreeGridPanel(getGridPanelConfig(true)),
			new Gemma.PhenotypeGridPanel(getGridPanelConfig(false))
		];

    	var syncGridPanelsOnSourceStoreLoad = function(sourceStore) {
			sourceStore.on('load', 
				function(store, records, options) {
					if (records.length > 0) {
						var data = [];
						for (var i = 0; i < records.length; i++) {
							data.push(Ext.apply({}, records[i].data));
						}
						for (var i = 0; i < gridPanels.length; i++) {
							var currStore = gridPanels[i].getStore();
							if (currStore !== sourceStore) {
								currStore.loadData(data);
							}
						}
					}
				},
				this, // scope
				{
					single: true
				});
    	}
    	
		for (var i = 0; i < gridPanels.length; i++) {
			gridPanels[i].on('viewready',
				function(gridPanel) {
					gridPanel.getView().emptyText = Gemma.HelpText.WidgetDefaults.PhenotypePanel.noRecordEmptyText;
					
					// Defer empty text only for the first tab.
					if (gridPanel === gridPanels[0]) {
						gridPanel.getView().deferEmptyText = true;
					} else {
						gridPanel.getView().applyEmptyText();
					}
				});
		}
		
		for (var i = 0; i < gridPanels.length; i++) {
		    this.relayEvents(gridPanels[i], ['phenotypeAssociationChanged', 'phenotypeSelectionChange']);
		}

		Ext.apply(this, {
			reloadActiveTab: function(filters) {
				// Note: I must have it. Otherwise, the tree gridpanel's store still keeps the node even  
				// if the node should not be in the store after reload. 
				this.getActiveTab().getSelectionModel().clearSelections();
	
				var activeTabStore = this.getActiveTab().getStore();
			
				syncGridPanelsOnSourceStoreLoad(activeTabStore);	
				
				
				var taxonId;
				var showOnlyEditable; 
				
				if (filters == null) {
					taxonId = '-1';
					showOnlyEditable = false; 
				} else {
					taxonId = filters.taxonId;
					showOnlyEditable = filters.showOnlyEditable;
				}
				activeTabStore.reload({
					params: {
						taxonId: taxonId,
						showOnlyEditable: showOnlyEditable
					}
				});
			},
			items: gridPanels
		});
		Gemma.PhenotypeTabPanel.superclass.initComponent.call(this);

		syncGridPanelsOnSourceStoreLoad(gridPanels[0].getStore());

		// I need to manually call setActiveTab() to set active tab. Otherwise, getActiveTab() returns null.
		this.setActiveTab(this.activeTab);
    }	
});
