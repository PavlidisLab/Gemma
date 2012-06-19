/**
 * It displays all the available phenotypes in the system.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.PhenotypeGridPanel = Ext.extend(Ext.grid.GridPanel, {
	storeAutoLoad: false,
	title: "Phenotypes List",
    autoScroll: true,
    stripeRows: true,
	loadMask: true,
    viewConfig: {
        forceFit: true
    },
    initComponent: function() {
		var checkboxSelectionModel = new Ext.grid.CheckboxSelectionModel({
			dataIndex: 'isChecked',
			singleSelect: false,
			header: '', // remove the "select all" checkbox on the header 
		    listeners: {
				rowdeselect: function(selectionModel, rowIndex, record) {
					// I must defer changing the field isChecked. Otherwise, 
					// other events such as cellclick will not be fired.
					Ext.defer(
						function() {
							record.set('isChecked', false);
						},
						500);
				},
				rowselect: function(selectionModel, rowIndex, record) {
					// I must defer changing the field isChecked. Otherwise, 
					// other events such as cellclick will not be fired.
					Ext.defer(
						function() {
							record.set('isChecked', true);
						},
						500);
				},
				scope: this
		    }
		});
	    
		var phenotypeSearchField = new Gemma.PhenotypePanelSearchField({
			emptyText: 'Search Phenotypes',
			listeners: {
				filterApplied: function(recordFilter) {
					var filterFields = ['value'];
					this.getStore().filterBy(
						function(record) {
							if (this.getSelectionModel().isSelected(record) || recordFilter(record, filterFields)) {
								return true;
							}
						    return false;
					    },
					    this	
					);
				},
				filterRemoved: function() {
					this.getStore().clearFilter(false);
				}, 
				scope: this
			}
		});

		var commonConfig = new Gemma.PhenotypeGridPanelCommonConfig();

		Ext.apply(this, {
			store: new Ext.data.Store({
				proxy: commonConfig.getStoreProxy(this.phenotypeStoreProxy),
				baseParams: commonConfig.getBaseParams(),				
				reader: new Ext.data.JsonReader({
					idProperty: 'urlId',
					fields: commonConfig.getStoreReaderFields()
				}),
				autoLoad: this.storeAutoLoad,
				sortInfo: {	field: 'value', direction: 'ASC' }
			}),
			columns:[
				checkboxSelectionModel,
				commonConfig.getPhenotypeValueColumn({ sortable: true }),
				commonConfig.getGeneCountColumn({ sortable: true })
			],
		    sm: checkboxSelectionModel,
		    listeners: {
				hide: commonConfig.getHideHandler,
				cellclick: commonConfig.getCellClickHandler,
		    	headerclick: function(gridPanel, columnIndex, event) {
		    		if (columnIndex == 0) {
		    			this.getStore().sort('isChecked');
		    		}
		    	}
		    },
			tbar: [
				phenotypeSearchField,
				commonConfig.getAddNewPhenotypeAssociationButton(this)
			]
		});

		this.superclass().initComponent.call(this);
		
		this.getStore().on('load',
			function(store, records, options) {
				commonConfig.resetSelectionConfig();	

				if (phenotypeSearchField.getValue() !== '') {
					phenotypeSearchField.applyCurrentFilter();
				}
				
				var recordsToBeRemoved = [];
				for (var i = 0; i < records.length; i++) {
					if (!records[i].data.dbPhenotype) {					
						// Don't clone records to be removed. Otherwise, they will not be removed.	
						recordsToBeRemoved.push(records[i]);
					}
				}
				store.suspendEvents();
				store.remove(recordsToBeRemoved);
				store.resumeEvents();
				store.fireEvent('datachanged', store);
			},
			this // scope
		);
    }
});
