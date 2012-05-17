/**
 * Common config for components in phenotype tab panel
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');


Gemma.PhenotypeGridPanelCommonConfig = Ext.extend(Object, {
	constructor: function() {
		var clickedSelections = [];

		var phenotypeStoreProxy = null;
		var phenotypeStoreReader = null;
    	
		var generateGeneCountHTML = function(width, geneCountText) {
			return '<span style="float: right; text-align: right; width: ' + width + 'px;">' + geneCountText + '</span>';
		}

		Ext.apply(this, {
			resetSelectionConfig: function() {
				clickedSelections = [];
			},
			getStoreProxy: function(defaultProxy) {
				var proxyToBeReturned;

				if (defaultProxy == null) {
					if (phenotypeStoreProxy == null) {
						phenotypeStoreProxy = new Ext.data.DWRProxy(PhenotypeController.loadAllPhenotypesByTree);
					}
					proxyToBeReturned = phenotypeStoreProxy; 
				} else {
					proxyToBeReturned = defaultProxy;
				}
				
				return proxyToBeReturned;
			},
			getStoreReader: function() {
				if (phenotypeStoreReader == null) {
					phenotypeStoreReader = new Ext.data.JsonReader({
						idProperty: "urlId",
						fields: [
							'urlId',
							{ name: 'value', sortType: Ext.data.SortTypes.asUCString },
							'valueUri',
							'publicGeneCount',
							'privateGeneCount',
					     	'_id',       // for phenotype tree only
					     	'_parent',   // for phenotype tree only
					     	'_is_leaf',  // for phenotype tree only
							'dbPhenotype',                                              // for phenotype list only
					     	{ name: 'isChecked', sortDir: 'DESC', defaultValue: false } // for phenotype list only
				     	]
					});
				}
				return phenotypeStoreReader;
			},
			getHideHandler: function(gridPanel) {
				// false NOT to bypass the conditional checks and events described in deselectRow
				gridPanel.getSelectionModel().clearSelections(false);
				clickedSelections = [];
				gridPanel.fireEvent('phenotypeSelectionChange', clickedSelections);
			},
	    	// cellclick instead of selection model's selectionchange event handler is implemented 
	    	// for letting listeners know that phenotype selections have been changed
	    	// because selectionchange events are fired even when rows are deselected in code. 
			getCellClickHandler: function(gridPanel, rowIndex, columnIndex, event) {
				var newSelections = gridPanel.getSelectionModel().getSelections();
					
				var hasSameSelections = (clickedSelections.length === newSelections.length);
					
				if (hasSameSelections) {
					for (var i = 0; hasSameSelections && i < clickedSelections.length; i++) {
						hasSameSelections = (clickedSelections[i].get('urlId') === newSelections[i].get('urlId'));
					}
				}
					
				if (!hasSameSelections) {
					var selectedPhenotypes = [];
					
					clickedSelections = newSelections;
				    for (var i = 0; i < clickedSelections.length; i++) {
				        selectedPhenotypes.push({
							urlId: clickedSelections[i].get('urlId'),
				        	value: clickedSelections[i].get('value'),
				        	valueUri: clickedSelections[i].get('valueUri')
				        });
					}
					gridPanel.fireEvent('phenotypeSelectionChange', selectedPhenotypes);						
				}
			},
			getPhenotypeValueColumn: function(defaults) {
				return Ext.apply({
					header: "Phenotype",
					dataIndex: 'value',
					width: 215,
				    renderer : function(value, metaData, record, rowIndex, colIndex, store){
					    metaData.attr = 'ext:qtip="' + value + '<br />' + record.data.valueUri + '"';
				        return value;
				    }
			    }, defaults);
			},
			getGeneCountColumn: function(defaults) {
				return Ext.apply({
					header: "Gene Count",
					dataIndex: 'publicGeneCount',
					align: "right",
					width: 115,
		            renderer: function(value, metaData, record, rowIndex, colIndex, store) {
			    		// Use min-width so that the cell will not be wrapped into 2 lines.
		            	// min-width is equal to the sum of the widths of private and public count.
		            	metaData.attr = 'style="padding-right: 15px; min-width: 66px;"';
							   
						return generateGeneCountHTML(26, (record.data.privateGeneCount > 0 ?
								   '(' + record.data.privateGeneCount + ')' :
								   '&nbsp;')) +
							   ' ' +
							   generateGeneCountHTML(40, record.data.publicGeneCount);
		            }
			    }, defaults);
			},
			getAddNewPhenotypeAssociationButton: function(gridPanel, defaultButtonHandler) {
				return {
					handler: gridPanel.createPhenotypeAssociationHandler ?
						gridPanel.createPhenotypeAssociationHandler :
						function() {
							var phenotypeAssociationFormWindow = new Gemma.PhenotypeAssociationForm.Window();

							gridPanel.relayEvents(phenotypeAssociationFormWindow, ['phenotypeAssociationChanged']);	
							phenotypeAssociationFormWindow.showWindow(Gemma.PhenotypeAssociationForm.ACTION_CREATE,
								{
									gene: gridPanel.currentGene,
									phenotypes: gridPanel.currentPhenotypes
								});
						},
					icon: "/Gemma/images/icons/add.png",
					tooltip: "Add new phenotype association"
				};
			}
		});
	}
});
