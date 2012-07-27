/**
 * It displays all evidence owned by the current user. 
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.PhenotypeEvidenceManagerGridPanel = Ext.extend(Gemma.PhenotypeEvidenceGridPanel, {
	storeAutoLoad: true,
	storeSortInfo: { field: 'lastUpdated', direction: 'DESC' },
	evidenceStoreProxy: new Ext.data.DWRProxy({
        apiActionToHandlerMap: {
	        read: {
    	        dwrFunction: PhenotypeController.findEvidenceByFilters,
        	    getDwrArgsFunction: function(request) {
					return [
						request.params.taxonId === '-1' ?
							null :
							request.params.taxonId,
						request.params.limit == null ? // It is undefined when the widget is first loaded.
							50 :
							request.params.limit
					];
				}
			}
		}
	}),
	displayPhenotypeAsLink: true,
	// Set it to false so that we can create evidence without specifying currentGene in the parent class.
	allowCreateOnlyWhenGeneSpecified: false,
	title: 'Phenotype Association Manager',
	hasOwnerColumns: true,
	hasRelevanceColumn: false,
	extraColumns: [{
		startIndex: 2,
		columns: [{ 
					header: 'Gene',
					dataIndex: 'geneOfficialSymbol',
					width: 0.15,
					renderer: function(value, metadata, record, row, col, ds) {
		            	var geneLink = '/Gemma/gene/showGene.html?id=' + record.data.geneId;
						
						return String.format("{0} <a target='_blank' href='" + geneLink + "' ext:qtip='Go to {0} Details (in new window)'><img src='/Gemma/images/icons/magnifier.png' height='10' width='10'/></a> ",
							value);
					},					
					sortable: true
				},
				{ 
					header: 'Taxon',
					dataIndex: 'taxonCommonName',
					width: 0.15,
					sortable: true
				}]
	}],
	initComponent: function() {
		var taxonCombo = new Gemma.TaxonCombo({
			isDisplayTaxonWithEvidence : true,
			stateId : null, // don't remember taxon value if user navigates away then comes back
			emptyText : "Filter by taxon",
			allTaxa : true, // want an 'All taxa' option
			value: '-1',
			listeners : {
				select: function(combo, record, index) {
					reloadStore();
				}
			}
		});
		taxonCombo.getStore().on('doneLoading', function() {
			// I have to do this. Otherwise, the combo box will display -1.
			taxonCombo.setValue(taxonCombo.getValue());
		});
		
		var dataFilterCombo = new Gemma.DataFilterCombo({
			value: 50,
			listeners : {
				select: function(combo, record, index) {
					reloadStore();
				}
			}
		});
		
		var reloadStore = function() {
			this.getStore().reload({
	    		params: {
	    			taxonId: taxonCombo.getValue(),
					limit: dataFilterCombo.getValue()
	    		}
	    	});
		}.createDelegate(this);

		Ext.apply(this, {
			listeners: {
				phenotypeAssociationChanged: function(phenotypes, gene) {
					reloadStore();
				}
			}
		});
		
		Gemma.PhenotypeEvidenceManagerGridPanel.superclass.initComponent.call(this);

		this.getTopToolbar().addButton(taxonCombo);
		this.getTopToolbar().addButton(dataFilterCombo);
	}
});
