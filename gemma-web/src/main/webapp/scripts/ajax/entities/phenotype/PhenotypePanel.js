/**
 * It displays all the available phenotyope associations in the system.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.PhenotypePanel = Ext.extend(Ext.Panel, {
    initComponent: function() {
    	if ((this.phenotypeStoreProxy && this.geneStoreProxy && this.geneColumnRenderer) ||
    	    (!this.phenotypeStoreProxy && !this.geneStoreProxy && !this.geneColumnRenderer)) {
    	    	
			var phenotypeGrid = new Gemma.PhenotypeGridPanel({
				region: "west",
				phenotypeStoreProxy: this.phenotypeStoreProxy ?
					this.phenotypeStoreProxy :
					new Ext.data.DWRProxy(PhenotypeController.loadAllPhenotypes),
				listeners: {
					phenotypeSelectionChange: function(selectedPhenotypes) {
			            geneGrid.setCurrentPhenotypes(selectedPhenotypes);
        			}
				}
			});

	    	var geneGrid = new Gemma.PhenotypeGeneGridPanel({
				region: "center",
				geneStoreProxy: this.geneStoreProxy ?
							   		this.geneStoreProxy :
							   		new Ext.data.DWRProxy({
								        apiActionToHandlerMap: {
							    	        read: {
							        	        dwrFunction: PhenotypeController.findCandidateGenes,
							            	    getDwrArgsFunction: function(request){
							            	    	return [request.params["phenotypeValueUri"]];
								                }
							    	        }
								        }
							    	}),
				listeners: {
					geneSelectionChange: function(selectedPhenotypes, selectedGene, selectedGeneEvidence) {
						evidenceGrid.setCurrentData(selectedPhenotypes, selectedGene, selectedGeneEvidence);
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
			
	    	var evidenceGrid = new Gemma.PhenotypeEvidenceGridPanel({
	    		region: 'center'
	    	});
	
			Ext.apply(this, {
		        height: 600,
	    	    width: 760,
				layout: 'border',        
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
		        ]
			});
    	} else {
    		Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.PhenotypePanel.setupErrorTitle, Gemma.HelpText.WidgetDefaults.PhenotypePanel.setupErrorText);
    	}

		this.superclass().initComponent.call(this);
		
		phenotypeGrid.getStore().on('load', 
			function() {
				if (Ext.get("phenotypeUrlId") != null && Ext.get("phenotypeUrlId").getValue() != "") {
					var currentRecord = phenotypeGrid.getStore().getById(Ext.get("phenotypeUrlId").getValue());
	
					phenotypeGrid.getSelectionModel().selectRecords( [ currentRecord ], false); // false to not keep existing selections
					phenotypeGrid.getView().focusRow(phenotypeGrid.getStore().indexOf(currentRecord));
				}
			},
			this, // scope
			{
				single: true,
				delay: 100  // Delay the handler. Otherwise, the current record is selected but not viewable in FireFox as of 2012-02-01 if it is not in the first page of the grid. There is no such issue in Chrome.
			});		
		geneGrid.getStore().on('load', 
			function() {
				if (Ext.get("geneId") != null && Ext.get("geneId").getValue() != "") {
					var currentRecord = geneGrid.getStore().getById(Ext.get("geneId").getValue());
					
					geneGrid.getSelectionModel().selectRecords( [ currentRecord ], false); // false to not keep existing selections
					geneGrid.getView().focusRow(geneGrid.getStore().indexOf(currentRecord));
				}
			},
			this, // scope
			{
				single: true,
				delay: 100 // Delay the handler. Otherwise, the current record is selected but not viewable in FireFox as of 2012-02-01 if it is not in the first page of the grid. There is no such issue in Chrome.
			});		
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
    onTrigger1Click : function() {
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
    initComponent: function() {
    	this.addEvents('filterApplied', 'filterRemoved');

        this.superclass().initComponent.call(this);
    }
});
