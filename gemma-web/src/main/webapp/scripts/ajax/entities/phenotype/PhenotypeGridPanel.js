/**
 * It displays all the available phenotypes in the system.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.PhenotypeGridPanel = Ext.extend(Ext.grid.GridPanel, {
	title: "Phenotypes",
    autoScroll: true,
    stripeRows: true,
	width: 350,
	height: 300,			
	split: true,
	loadMask: true,
    viewConfig: {
        forceFit: true
    },
    initComponent: function() {
		this.addEvents('phenotypeSelectionChange');

		var checkboxSelectionModel = new Ext.grid.CheckboxSelectionModel({
			dataIndex: 'isChecked',
			singleSelect: false,
			header: '', // remove the "select all" checkbox on the header 
		    listeners: {
				rowdeselect: function(selectionModel, rowIndex, record) {
					record.set('isChecked', false);
				},
				rowselect: function(selectionModel, rowIndex, record) {
					record.set('isChecked', true);
				},
		        selectionchange: function(selectionModel) {
					var selectedPhenotypes;
		
					if (selectionModel.hasSelection()) {
						selectedPhenotypes = [];
						
						var selections = selectionModel.getSelections();
					    for (var i = 0; i < selections.length; i++) {
					        selectedPhenotypes.push({
					        	value: selections[i].get('value'),
					        	valueUri: selections[i].get('valueUri')
					        });
						}
					} else {
						selectedPhenotypes = null;
					}
					this.fireEvent('phenotypeSelectionChange', selectedPhenotypes);
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

		Ext.apply(this, {
			store: new Ext.data.Store({
				proxy: this.phenotypeStoreProxy,
				reader: new Ext.data.JsonReader({
					root: 'records', // required.
					successProperty: 'success', // same as default.
					messageProperty: 'message', // optional
					totalProperty: 'totalRecords', // default is 'total'; optional unless paging.
					idProperty: "urlId",
					fields: [
						'urlId',
						'value',
						'valueUri',
						{ name: 'occurence', type: "long" },
						{ name: 'isChecked', sortDir: 'DESC' }
					]
				}),
				sortInfo: {	field: 'value', direction: 'ASC' },
				autoLoad: true
			}),
			columns:[
				checkboxSelectionModel,
				{
					header: "Phenotype",
					dataIndex: 'value',
					width: 285,
					renderToolTip: true,
					sortable: true
				},{
					header: "Gene Count",
					dataIndex: 'occurence',
					align: "right",
					width: 135,
					renderer: Ext.util.Format.numberRenderer('0,0'),
					sortable: true
			    }
			],
		    sm: checkboxSelectionModel,
		    listeners: {
		    	headerclick: function(gridPanel, columnIndex, event) {
		    		if (columnIndex == 0) {
		    			this.getStore().sort('isChecked');
		    		}
		    	}
		    },
			tbar: [
				phenotypeSearchField			
// TODO: The following codes have been commented out because the new feature "create phenotype association" is still being implemented.
//				{
//					handler: function() {
//						var phenotypeAssociationFormWindow = new Gemma.PhenotypeAssociationForm.Window();
//// TODO: I think I should not reload, but add the new phenotype association AND keep the original selection instead of selecting the newly created phenotype association. 						
//						phenotypeAssociationFormWindow.on('phenotypeAssociationFormWindowHidden',
//							function(isPhenotypeAssociationCreated) {
//								if (isPhenotypeAssociationCreated) {
//									var gridStore = this.getStore();
//									gridStore.reload(gridStore.lastOptions);
//								}
//							},
//							this);
//						phenotypeAssociationFormWindow.showWindow(this.currentPhenotypes, this.currentGene);
//					},
//					scope: this,
//					icon: "/Gemma/images/icons/add.png",
//					tooltip: "Add new phenotype association"
//				}
			]
		});

		this.superclass().initComponent.call(this);
    }
});
