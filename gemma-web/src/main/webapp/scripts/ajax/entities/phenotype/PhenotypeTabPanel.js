/**
 * This tab panel contains phenotype tree grid panel and phenotype grid panel.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.PhenotypeTabPanel = Ext.extend(Ext.TabPanel, {
	phenotypeStoreProxy: null,
	createPhenotypeAssociationHandler: null,
	activeTab: 0,
	width: 300,
	split: true,
    initComponent: function() {
    	var syncDestinationStoresOnSourceStoreLoad = function(sourceStore, destinationStores) {
			sourceStore.on('load', 
				function(store, records, options) {
					var data = [];
					for (var i = 0; i < records.length; i++) {
						data.push(Ext.apply({}, records[i].data));						
					}
					for (var i = 0; i < destinationStores.length; i++) {
						var currStore = destinationStores[i].getStore();
						if (currStore !== sourceStore) {
							currStore.loadData(data);
						}				
					}
				},
				this, // scope
				{
					single: true
				});
    	}
    	
    	// Only the first tab's grid panel should have storeAutoLoad set to true.
    	var phenotypeTreeGridPanel = new Gemma.PhenotypeTreeGridPanel({
			storeAutoLoad: true,
			phenotypeStoreProxy: this.phenotypeStoreProxy,
			createPhenotypeAssociationHandler: this.createPhenotypeAssociationHandler					
		});
		var phenotypeGrid = new Gemma.PhenotypeGridPanel({
			storeAutoLoad: false,
			phenotypeStoreProxy: this.phenotypeStoreProxy,
			createPhenotypeAssociationHandler: this.createPhenotypeAssociationHandler
		});
		
    	this.relayEvents(phenotypeTreeGridPanel,
    		['phenotypeAssociationChanged', 'phenotypeSelectionChange']);
    	this.relayEvents(phenotypeGrid,
    		['phenotypeAssociationChanged', 'phenotypeSelectionChange']);

		Ext.apply(this, {
			reloadActiveTab: function() {
				var activeTabStore = this.getActiveTab().getStore();
			
				// this.items.items are all the components in this tab panel.
				syncDestinationStoresOnSourceStoreLoad(activeTabStore, this.items.items);	
				
				activeTabStore.reload(activeTabStore.lastOptions);
			},
			items: [
				phenotypeTreeGridPanel,
				phenotypeGrid
			]
		});
		this.superclass().initComponent.call(this);

		syncDestinationStoresOnSourceStoreLoad(phenotypeTreeGridPanel.getStore(), [ phenotypeGrid ]);

		// I need to manually call setActiveTab() to set active tab. Otherwise, getActiveTab() returns null.
		this.setActiveTab(this.activeTab);
    }	
});
