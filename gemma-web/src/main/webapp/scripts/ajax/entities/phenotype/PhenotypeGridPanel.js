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

		var phenotypeAssociationFormWindow = null;

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
								urlId: selections[i].get('urlId'),
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

		var rightPad = function (val, size, ch) {
	        var result = String(val);
	        if(!ch) {
	            ch = " ";
	        }
	        while (result.length < size) {
	            result += ch;
	        }
	        return result;
	    }
		
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
//						{
//							name: 'count',
//							type: "long",
//							convert: function(value, record) {
//								return record.publicGeneCount + record.privateGeneCount; 
//							}
//						},
						'publicGeneCount',
						'privateGeneCount',
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
//					dataIndex: 'count',
dataIndex: 'publicGeneCount',
					align: "right",
					width: 135,
//					renderer: Ext.util.Format.numberRenderer('0,0'),
		            renderer: function(value, metadata, record, rowIndex, colIndex, store) {
var countTextWidth = 5; // Assume private count has a maximum of 2 digits.		            	
var countText;		            	
						if (record.data.privateGeneCount > 0) {
							countText = record.data.publicGeneCount + rightPad(' (' + record.data.privateGeneCount + ')', countTextWidth, ' ');
						} else {
							countText = record.data.publicGeneCount + rightPad('', countTextWidth, ' ');
						}
return '<pre style="font-size: 0.952em">' + countText + '</pre>';						
		            },
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
				phenotypeSearchField,		
				{
					handler: function() {
						if (phenotypeAssociationFormWindow == null) {
							phenotypeAssociationFormWindow = new Gemma.PhenotypeAssociationForm.Window();
							this.relayEvents(phenotypeAssociationFormWindow, ['phenotypeAssociationChanged']);	
						}
						phenotypeAssociationFormWindow.showWindow(Gemma.PhenotypeAssociationForm.ACTION_CREATE,
							{
								gene: this.currentGene,
								phenotypes: this.currentPhenotypes
							});
					},
					scope: this,
					icon: "/Gemma/images/icons/add.png",
					tooltip: "Add new phenotype association"
				}
			]
		});

		this.superclass().initComponent.call(this);
		
		this.getStore().on('load', 
			function(store, records, options) {
				if (phenotypeSearchField.getValue() !== '') {
					phenotypeSearchField.applyCurrentFilter();
				}
			},
			this // scope
		);
    }
});
