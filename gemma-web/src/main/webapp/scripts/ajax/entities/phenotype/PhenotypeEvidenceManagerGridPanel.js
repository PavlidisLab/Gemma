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
						request.params.taxonId,
						request.params.limit == null ?
							50 :
							request.params.limit
					];
				}
			}
		}
	}),
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
		Gemma.PhenotypeEvidenceManagerGridPanel.superclass.initComponent.call(this);

		var taxonCombo = new Gemma.TaxonCombo({
			isDisplayTaxonWithEvidence : true,
			stateId : null, // don't remember taxon value if
			// user navigates away then comes
			// back
			emptyText : "Filter by taxon",
			allTaxa : true, // want an 'All taxa' option
			value: '-1'			
		});
		taxonCombo.getStore().on('doneLoading', function() {
			if (this.taxonid) {
				taxonCombo.setValue(this.taxonid);
			} else {
				this.taxonid = (taxonCombo.getStore().getAt(0)) ?
					taxonCombo.getStore().getAt(0).get('id') :
					"-1";
				taxonCombo.setValue(this.taxonid);
			}
		}, this);				
		taxonCombo.on({
			select: function(combo, record, index) {
				reloadStore();
			},
			scope: this
		});
		
		var dataFilterCombo = new Gemma.DataFilterCombo();
		dataFilterCombo.on({
			select: function(combo, record, index) {
				reloadStore();
			}
		});
		dataFilterCombo.setValue(50);
		
		var reloadStore = function() {
			this.getStore().reload({
	    		params: {
					taxonId: taxonCombo.getValue() === '-1' ?
								null :
								taxonCombo.getValue(),
					limit: dataFilterCombo.getValue()
	    		}
	    	});
		}.createDelegate(this);

		this.getTopToolbar().addButton(taxonCombo);
		this.getTopToolbar().addButton(dataFilterCombo);
		
		var columnModel = this.getColumnModel();
	}
});
