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
		var currentSelections = [];		
    
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

		var generateGeneCountHTML = function(width, geneCountText) {
			return '<span style="float: left; text-align: right; width: ' + width + 'px;">' + geneCountText + '</span>'; 
		}
		
		Ext.apply(this, {
			store: new Ext.data.Store({
				proxy: this.phenotypeStoreProxy == null ?
					new Ext.data.DWRProxy(PhenotypeController.loadAllPhenotypes) :
					this.phenotypeStoreProxy,
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
					dataIndex: 'publicGeneCount',
					align: "right",
					width: 135,
		            renderer: function(value, metadata, record, rowIndex, colIndex, store) {
						return generateGeneCountHTML(50, record.data.publicGeneCount) + ' ' +
							   generateGeneCountHTML(26, (record.data.privateGeneCount > 0 ? '(' + record.data.privateGeneCount + ')' : '&nbsp;'));
		            },
					sortable: true
			    }
			],
		    sm: checkboxSelectionModel,
		    listeners: {
		    	// cellclick instead of selection model's selectionchange event handler is implemented 
		    	// for letting listeners know that phenotype selections have been changed
		    	// because selectionchange events are fired even when rows are deselected in code. 
				cellclick: function(thisGrid, rowIndex, columnIndex, event) {
					var newSelections = this.getSelectionModel().getSelections();
						
					var hasSameSelections = (currentSelections.length === newSelections.length);
						
					if (hasSameSelections) {
						for (var i = 0; hasSameSelections && i < currentSelections.length; i++) {
							hasSameSelections = (currentSelections[i].get('urlId') === newSelections[i].get('urlId'));
						}
					}
						
					if (!hasSameSelections) {
						var selectedPhenotypes = [];
						
						currentSelections = newSelections;
					    for (var i = 0; i < currentSelections.length; i++) {
					        selectedPhenotypes.push({
								urlId: currentSelections[i].get('urlId'),
					        	value: currentSelections[i].get('value'),
					        	valueUri: currentSelections[i].get('valueUri')
					        });
						}
						this.fireEvent('phenotypeSelectionChange', selectedPhenotypes);						
					}
				},
		    	headerclick: function(gridPanel, columnIndex, event) {
		    		if (columnIndex == 0) {
		    			this.getStore().sort('isChecked');
		    		}
		    	}
		    },
			tbar: [
				phenotypeSearchField,		
				{
					handler: this.createPhenotypeAssociationHandler ?
						this.createPhenotypeAssociationHandler :
						function() {
							var phenotypeAssociationFormWindow = new Gemma.PhenotypeAssociationForm.Window();

							this.relayEvents(phenotypeAssociationFormWindow, ['phenotypeAssociationChanged']);	
							phenotypeAssociationFormWindow.showWindow(Gemma.PhenotypeAssociationForm.ACTION_CREATE,
								{
									gene: this.currentGene,
									phenotypes: this.currentPhenotypes
								});
						}
					,
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
