/**
 * It displays genes linked to the current phenotypes. 
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.PhenotypeGeneGridPanel = Ext.extend(Ext.grid.GridPanel, {
	title: 'Genes',
    autoScroll: true,
    stripeRows: true,
	loadMask: true,
    viewConfig: {
        forceFit: true
    },
    currentPhenotypes: null,
    initComponent: function() {
		var DEFAULT_TITLE = this.title; // A constant title that will be used when we don't have current phenotypes.
				
		var titleText = this.title; // Contains the title's text without any HTML code whereas title may contain HTML code.
    	
    	var downloadButton = new Ext.Button({
			text: '<b>Download</b>',
			disabled: true,
			icon: '/Gemma/images/download.gif',
			handler: function() {
				var columnConfig = [{
						header: 'NCBI ID', dataIndex: 'ncbiId' // first column
					}].concat(this.getColumnModel().config); // rest of columns 

				var downloadData = [];
			    var downloadDataRow = [];
				
			    for (var i = 0; i < columnConfig.length; i++) {
			        downloadDataRow.push(columnConfig[i].header);
			    }
			    downloadData.push(downloadDataRow);
			    
				this.getStore().each(function(record) {
				    downloadDataRow = [];
				    for (var i = 0; i < columnConfig.length; i++) {
				        downloadDataRow.push(record.get(columnConfig[i].dataIndex));
				    }
				    downloadData.push(downloadDataRow);
				});
		
				var downloadDataHeader = titleText;
				if (geneSearchField.getValue() !== '') {
					downloadDataHeader += ' AND matching pattern "' + geneSearchField.getValue() + '"';
				}
		  		var textWindow = new Gemma.DownloadWindow({
		  			windowTitleSuffix: 'Genes associated with selected Phenotype(s)',
		  			downloadDataHeader: downloadDataHeader, 
		  			downloadData: downloadData,
		  			modal: true
		  		});
		  		textWindow.convertToText ();
		  		textWindow.show();
			},
			scope: this
    	});
    	
    	var geneSearchField = new Gemma.PhenotypePanelSearchField({
			emptyText: 'Search Genes',
			disabled: true,
			listeners: {
				filterApplied: function(recordFilter) {
					var filterFields = ['officialSymbol', 'officialName'];
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

    	var createPhenotypeAssociationButton = new Ext.Button({
			disabled: true,
			handler: this.createPhenotypeAssociationHandler ?
				this.createPhenotypeAssociationHandler :
					function() {
						var phenotypeAssociationFormWindow = new Gemma.PhenotypeAssociationForm.Window();
						
						this.relayEvents(phenotypeAssociationFormWindow, ['phenotypeAssociationChanged']);	
						phenotypeAssociationFormWindow.showWindow(Gemma.PhenotypeAssociationForm.ACTION_CREATE,
							{
								gene: null,
								phenotypes: this.currentPhenotypes
							});
					},
			scope: this,
			icon: "/Gemma/images/icons/add.png",
			tooltip: "Add new phenotype association"
    	});

    	var onStoreRecordChange = function() {
			downloadButton.setDisabled(this.getStore().getCount() <= 0);
		};

		Ext.apply(this, {
			store: new Ext.data.Store({
				proxy: this.geneStoreProxy == null ?
							new Ext.data.DWRProxy({
						        apiActionToHandlerMap: {
					    	        read: {
					        	        dwrFunction: PhenotypeController.findCandidateGenes,
					            	    getDwrArgsFunction: function(request){
					            	    	return [request.params["phenotypeValueUris"]];
						                }
					    	        }
						        }
					    	}) :
					   		this.geneStoreProxy,
				reader: new Ext.data.JsonReader({
					root: 'records', // required.
					successProperty: 'success', // same as default.
					messageProperty: 'message', // optional
					totalProperty: 'totalRecords', // default is 'total'; optional unless paging.
					idProperty: 'id', // same as default
					fields: [
							'id',
							'ncbiId',
							'taxonId',
							{ name: 'officialSymbol', sortType: Ext.data.SortTypes.asUCString }, // case-insensitively
							{ name: 'officialName', sortType: Ext.data.SortTypes.asUCString }, // case-insensitively
							'taxonCommonName'
						]
			    }),
			    sortInfo: {	field: 'officialSymbol', direction: 'ASC' },
			    listeners: {
					clear: onStoreRecordChange,
					datachanged: onStoreRecordChange,
					load: onStoreRecordChange,
		            scope: this
			    }
			}),
			columns:[{
				header: "Symbol",
				dataIndex: 'officialSymbol',
				width: 65,
				sortable: true
			},{
				header: "Name",
				dataIndex: 'officialName',
				width: 215,
				renderToolTip: true,
				sortable: true
			},{
				header: "Species",
				dataIndex: 'taxonCommonName',
				width: 100,
				sortable: true
			}],
			selModel: new Ext.grid.RowSelectionModel({
				singleSelect: true,
				listeners: {
					selectionchange: function(selModel) {
						var selectedGene = null;
						
						if (selModel.hasSelection()) {
							var geneGridSelection = this.getSelectionModel().getSelected();
		
							selectedGene = {
								id: geneGridSelection.get('id'),
								ncbiId: geneGridSelection.get('ncbiId'),
								officialSymbol: geneGridSelection.get('officialSymbol'),
								officialName: geneGridSelection.get('officialName'),
								taxonCommonName: geneGridSelection.get('taxonCommonName'),
								taxonId: geneGridSelection.get('taxonId')
							};
						}						
						this.fireEvent('geneSelectionChange', this.currentPhenotypes, selectedGene);
					},
					scope: this
				}
			}),
			tbar: [
				geneSearchField,
				createPhenotypeAssociationButton,				
				downloadButton
			],
			setCurrentPhenotypes: function(currentPhenotypes) {
				this.currentPhenotypes = currentPhenotypes;

				var hasCurrentPhenotypes = (currentPhenotypes != null && currentPhenotypes.length > 0);

				createPhenotypeAssociationButton.setDisabled(!hasCurrentPhenotypes);
				geneSearchField.setDisabled(!hasCurrentPhenotypes);
				geneSearchField.setValue('');

				if (hasCurrentPhenotypes) {
					var currentPhenotypeValueUris = [];
					
					var selectedPhenotypePrefix = 'Genes associated with';
					var selectedPhenotypeHeader = selectedPhenotypePrefix + ' "';
					var selectedPhenotypeTooltip = '&nbsp;&nbsp;&nbsp;';
					
				    for (var i = 0; i < currentPhenotypes.length; i++) {
				    	var currPhenotypeValue = currentPhenotypes[i].value;
	
				        currentPhenotypeValueUris.push(currentPhenotypes[i].valueUri);
				        
						selectedPhenotypeHeader += currPhenotypeValue;
						selectedPhenotypeTooltip += currPhenotypeValue;
						
						if (i < currentPhenotypes.length - 1) {
							selectedPhenotypeHeader += '" + "';
							selectedPhenotypeTooltip += '<br />&nbsp;&nbsp;&nbsp;';
						} else {
							selectedPhenotypeHeader += '"';
						}	
					}

					this.getStore().reload({
			    		params: {
			    			'phenotypeValueUris': currentPhenotypeValueUris
			    		}
			    	});
					this.getSelectionModel().clearSelections(false);				    
					    
					this.setTitle("<div style='height: 15px; overflow: hidden;' " +  // Make the header one line only.
						"ext:qtitle='" + selectedPhenotypePrefix + "' " +
						"ext:qtip='" + selectedPhenotypeTooltip + "'>" + selectedPhenotypeHeader + "</div>");
					titleText = selectedPhenotypeHeader;
				} else {
					this.setTitle(DEFAULT_TITLE);						
					this.getStore().removeAll(false);
				}				
			}
		});

		this.superclass().initComponent.call(this);
    }
});
