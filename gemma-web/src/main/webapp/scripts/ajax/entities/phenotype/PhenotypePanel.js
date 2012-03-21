/**
 * It displays all the available phenotyope associations in the system.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.PhenotypePanel = Ext.extend(Ext.Panel, {
	// Configs that should be set if used outside of Gemma (BEGIN)
	phenotypeStoreProxy: null,
	geneStoreProxy: null,
	evidenceStoreProxy: null,
	geneColumnRenderer: null, 
	createPhenotypeAssociationHandler: null,
	// Configs that should be set if used outside of Gemma (END)
	height: 600,
	width: 760,
	layout: 'border',        
    initComponent: function() {
    	if (!((this.phenotypeStoreProxy && this.geneStoreProxy && this.evidenceStoreProxy && this.geneColumnRenderer && this.createPhenotypeAssociationHandler) ||
    	      (!this.phenotypeStoreProxy && !this.geneStoreProxy && !this.evidenceStoreProxy && !this.geneColumnRenderer && !this.createPhenotypeAssociationHandler))) {
    		Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.PhenotypePanel.setupErrorTitle, Gemma.HelpText.WidgetDefaults.PhenotypePanel.setupErrorText);
    	} else {
			var currentPhenotypes = null;
			var currentGene = null;

			var phenotypeGrid = new Gemma.PhenotypeGridPanel({
				region: "west",
				phenotypeStoreProxy: this.phenotypeStoreProxy,
				createPhenotypeAssociationHandler: this.createPhenotypeAssociationHandler,					
				listeners: {
					phenotypeSelectionChange: function(selectedPhenotypes) {
			            geneGrid.setCurrentPhenotypes(selectedPhenotypes);
						currentPhenotypes = selectedPhenotypes;
        			}
				}
			});
			this.relayEvents(phenotypeGrid, ['phenotypeAssociationChanged']);			

	    	var geneGrid = new Gemma.PhenotypeGeneGridPanel({
				region: "center",
				geneStoreProxy: this.geneStoreProxy,
				createPhenotypeAssociationHandler: this.createPhenotypeAssociationHandler,
				listeners: {
					geneSelectionChange: function(selectedPhenotypes, selectedGene) {
						evidenceGrid.setCurrentData(selectedPhenotypes, selectedGene);
						currentGene = selectedGene;
        			}
				}
			});
			geneGrid.getColumnModel().setRenderer(0,
				this.geneColumnRenderer ?
					this.geneColumnRenderer :
					function(value, metadata, record, row, col, ds) {
						return String.format("{1} <a target='_blank' href='/Gemma/gene/showGene.html?id={0}' ext:qtip='Go to {1} Details (in new window)'><img src='/Gemma/images/icons/magnifier.png' height='10' width='10'/></a> ",
							record.data.id, record.data.officialSymbol);
					}
			);
			this.relayEvents(geneGrid, ['phenotypeAssociationChanged']);
			
	    	var evidenceGrid = new Gemma.PhenotypeEvidenceGridPanel({
	    		region: 'center',
				evidenceStoreProxy: this.evidenceStoreProxy,	    		
				createPhenotypeAssociationHandler: this.createPhenotypeAssociationHandler
	    	});
			this.relayEvents(evidenceGrid, ['phenotypeAssociationChanged']);

			var selectRecordsOnLoad = function(gridPanel, recordIds) {
				gridPanel.getStore().on('load', 
					function(store, records, options) {
						if (recordIds.length > 0) {				
							var selModel = gridPanel.getSelectionModel();				
			            	selModel.clearSelections();
		
							var firstRowIndex = store.indexOfId(recordIds[0]);
			            	selModel.selectRow(firstRowIndex, false); // false to not keep existing selections
					        for (var i = 1; i < recordIds.length; i++) {
					            selModel.selectRow(store.indexOfId(recordIds[i]), true); // true to keep existing selections
			        		}
			        		gridPanel.getView().focusRow(firstRowIndex); // Make sure the first selected record is viewable.
						}				
					},
					this, // scope
					{
						single: true,
						delay: 500  // Delay the handler. Otherwise, the current record is selected but not viewable in FireFox as of 2012-02-01 if it is not in the first page of the grid. There is no such issue in Chrome.
					})};		
			
			var reloadWholePanel = function() {
				if (currentPhenotypes != null && currentPhenotypes.length > 0) {
					var currentPhenotypeUrlIds = [];
					for (var i = 0; i < currentPhenotypes.length; i++) {
						currentPhenotypeUrlIds.push(currentPhenotypes[i].urlId);				
					}
					selectRecordsOnLoad(phenotypeGrid, currentPhenotypeUrlIds);
		
					if (currentGene != null) {
						// geneGrid's store will be loaded after phenotypeGrid's original rows are selected later on.
						selectRecordsOnLoad(geneGrid, [ currentGene.id ]);
					}
				}
		
				var phenotypeGridStore = phenotypeGrid.getStore();
				phenotypeGridStore.reload(phenotypeGridStore.lastOptions);
			};

			if (!this.createPhenotypeAssociationHandler) {
				Gemma.Application.currentUser.on("logIn", reloadWholePanel,	this);
				Gemma.Application.currentUser.on("logOut", reloadWholePanel, this);
			}

			Ext.apply(this, {
	        	items: [
		        	{
						xtype: 'panel',
					    height: 200,
					    layout: 'border',
					    viewConfig: {
					        forceFit: true
					    },
					    items: [
					    	phenotypeGrid,
					        geneGrid
					    ],
						region: 'north',
						split: true
		        	},
		            evidenceGrid
		        ],
				listeners: {
					'phenotypeAssociationChanged': reloadWholePanel,
					scope: this
				}
			});
			
			if (Ext.get("phenotypeUrlId") != null && Ext.get("phenotypeUrlId").getValue() != "") {
				selectRecordsOnLoad(phenotypeGrid, [ Ext.get("phenotypeUrlId").getValue() ]);
				
				if (Ext.get("geneId") != null && Ext.get("geneId").getValue() != "") {
					selectRecordsOnLoad(geneGrid, [ parseInt(Ext.get("geneId").getValue()) ]);
				}
			}
    	}

		this.superclass().initComponent.call(this);
    }
});

Gemma.PhenotypePanelSearchField = Ext.extend(Ext.form.TwinTriggerField, {
	enableKeyEvents: true,
    validationEvent: false,
    validateOnBlur: false,
    trigger1Class: 'x-form-clear-trigger',
    trigger2Class: 'x-form-search-trigger',
    hideTrigger1: true,
    width: 220,
    hasSearch: false,
	listeners: {
		keyup: function(field, e) {
            this.onTrigger2Click();
		}
	},
    onTrigger1Click: function() {
        if (this.hasSearch) {
            this.el.dom.value = '';
            this.triggers[0].hide();
            this.hasSearch = false;
			this.fireEvent('filterRemoved');
        }
        this.superclass().onTrigger1Click.call(this);
    },
    onTrigger2Click: function() {
        var typedString = this.getRawValue().toLowerCase();
        if (typedString.length < 1) {
            this.onTrigger1Click();
            return;
        }

        this.hasSearch = true;
        this.triggers[0].show();
        
		var recordFilter = function(record, filterFields) {
			for (var i = 0; i < filterFields.length; i++) {
				if (record.get(filterFields[i]).toLowerCase().indexOf(typedString) >= 0) {
					return true;
				}
			}
		    return false;
		}
		this.fireEvent('filterApplied', recordFilter);

        this.superclass().onTrigger2Click.call(this);
    },
    applyCurrentFilter: function() {
    	this.onTrigger2Click();
    },
    initComponent: function() {
    	this.addEvents('filterApplied', 'filterRemoved');

        this.superclass().initComponent.call(this);
    }
});
