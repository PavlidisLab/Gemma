Ext.namespace('Gemma');

var phenotypeGrid;
var geneGrid;
var evidenceGrid;

Gemma.PhenotypePanel = Ext.extend(Ext.Panel, {
    initComponent: function() {
    	if ((this.phenotypeStoreProxy && this.geneStoreProxy && this.geneColumnRenderer) ||
    	    (!this.phenotypeStoreProxy && !this.geneStoreProxy && !this.geneColumnRenderer)) {
	    	geneGrid = new Gemma.GeneGrid({
				geneStoreProxy: this.geneStoreProxy ?
							   		this.geneStoreProxy :
							   		new Ext.data.DWRProxy({
								        apiActionToHandlerMap: {
							    	        read: {
							        	        dwrFunction: PhenotypeController.findCandidateGenes,
							            	    getDwrArgsFunction: function(request){
							            	    	return [request.params["phenotypeValue"]];
								                }
							    	        }
								        }
							    	})
			});
			geneGrid.getColumnModel().setRenderer(0,
				this.geneColumnRenderer ?
					this.geneColumnRenderer :
					function(value, metadata, record, row, col, ds) {
						return String.format("{1} <a target='_blank' href='/Gemma/gene/showGene.html?id={0}' ext:qtip='Go to {1} Details (in new window)'><img src='/Gemma/images/icons/magnifier.png' height='10' width='10'/></a> ",
							record.data.id, record.data.officialSymbol);
					}
			);
			
	    	evidenceGrid = new Gemma.PhenotypeEvidenceGridPanel({
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
					        new Gemma.PhenotypeGrid({
					        	phenotypeStoreProxy: this.phenotypeStoreProxy ?
							   		this.phenotypeStoreProxy :
							   		new Ext.data.DWRProxy(PhenotypeController.loadAllPhenotypes)
					        }),
					        geneGrid
					    ],
						region: 'north',
						split: true
		        	},
		            evidenceGrid
		        ]
			});
    	} else {
    		Ext.Msg.alert('Error in Gemma.PhenotypePanel', 'If you are using PhenotypePanel inside of Gemma,<br />' +
    			'<b>phenotypeStoreProxy</b>, <b>geneStoreProxy</b> and<br />' +
    			'<b>geneColumnRenderer</b> should not be set <br />' +
    			'in config. Otherwise, all of them should be set.');
    	}

		Gemma.PhenotypePanel.superclass.initComponent.call(this);
		
		var isStoreFirstLoad = true;
		phenotypeGrid.getStore().on('load', function() {
			if (isStoreFirstLoad && Ext.get("phenotypeValue") != null && Ext.get("phenotypeValue").getValue() != "") {
				var currentRecord = phenotypeGrid.getStore().getById(Ext.get("phenotypeValue").getValue());

				phenotypeGrid.getSelectionModel().selectRecords( [ currentRecord ], false); // false to not keep existing selections
				
				var a = function() {
					return phenotypeGrid.getView().focusRow(phenotypeGrid.getStore().indexOf(currentRecord));
				};
				a.defer(100);
			}
		});
		geneGrid.getStore().on('load', function() {
			if (isStoreFirstLoad && Ext.get("geneId") != null && Ext.get("geneId").getValue() != "") {
				var currentRecord = geneGrid.getStore().getById(Ext.get("geneId").getValue());
				
				geneGrid.getSelectionModel().selectRecords( [ currentRecord ], false); // false to not keep existing selections
					
				var a = function() {
					return geneGrid.getView().focusRow(geneGrid.getStore().indexOf(currentRecord));
				};
				a.defer(100);
			}
			isStoreFirstLoad = false;
		});	
    }
});

Gemma.PhenotypeGrid = Ext.extend(Ext.grid.GridPanel, {
    initComponent: function() {
    	phenotypeGrid = this;
    	var checkboxSelectionModel = new Gemma.PhenotypeCheckboxSelectionModel();
		Ext.apply(this, {
			title: "Phenotypes",
		    autoScroll: true,
		    stripeRows: true,
			width: 350,
height: 300,			
			region: "west",
			split: true,
			store: new Gemma.PhenotypeStore({
				proxy: this.phenotypeStoreProxy
			}),
			loadMask: true,
		    viewConfig: {
		        forceFit: true
		    },
			myPageSize:500,
			
			// grid columns
			columns:[
				checkboxSelectionModel,
				{
					header: "Phenotype",
					dataIndex: 'value',
					width: 285,
					renderer: tooltipRenderer,
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
		    			phenotypeGrid.getStore().sort('isChecked');
		    		}
		    	}
		    },
			tbar: [
				new Gemma.PhenotypePanelSearchField({
					getSelectionModel: function() { return phenotypeGrid.getSelectionModel(); },
					getStore: function() { return phenotypeGrid.getStore(); },
					filterFields: [ 'value' ],
					emptyText: 'Search Phenotypes'
				})
			]
		});
		Gemma.PhenotypeGrid.superclass.initComponent.call(this);

		this.getStore().setDefaultSort('value', 'asc');
    }
});

Gemma.PhenotypeStore = Ext.extend(Ext.data.Store, {
	constructor: function(config) {
		Gemma.PhenotypeStore.superclass.constructor.call(this, config);
	},
	reader: new Ext.data.JsonReader({
		root: 'records', // required.
		successProperty: 'success', // same as default.
		messageProperty: 'message', // optional
		totalProperty: 'totalRecords', // default is 'total'; optional unless paging.
		idProperty: "value",
		fields: [ 
			'value',
			'valueUri',
			{ name: 'occurence', type: "long" },
			{ name: 'isChecked', sortDir: 'DESC' }
		]
	}),
	autoLoad: true
});

Gemma.PhenotypeCheckboxSelectionModel = Ext.extend(Ext.grid.CheckboxSelectionModel, {
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
			if (selectionModel.hasSelection()) {
				var phenotypeSelections = selectionModel.getSelections();
				
				var storeBaseParams = [];
				
				var selectedPhenotypePrefix = 'Genes associated with';
				var selectedPhenotypeHeader = selectedPhenotypePrefix + ' "';
				var selectedPhenotypeTooltip = '&nbsp;&nbsp;&nbsp;';
				
			    for (var i = 0; i < phenotypeSelections.length; i++) {
			    	var currPhenotypeValue = phenotypeSelections[i].get('value');

			        storeBaseParams.push(phenotypeSelections[i].get('valueUri'));
			        
					selectedPhenotypeHeader += currPhenotypeValue;
					selectedPhenotypeTooltip += currPhenotypeValue;
					
					if (i < phenotypeSelections.length - 1) {
						selectedPhenotypeHeader += '" + "';
						selectedPhenotypeTooltip += '<br />&nbsp;&nbsp;&nbsp;';
					} else {
						selectedPhenotypeHeader += '"';
					}	
				}
				var geneStore = geneGrid.getStore();
				geneStore.baseParams = geneStore.baseParams || {};
			    geneStore.baseParams['phenotypeValue'] = storeBaseParams;
evidenceGrid.removeAll(false);
			    geneStore.reload({
			    	params: {
						start: 0,
						limit: geneGrid.myPageSize							
					}
			    });
				geneGrid.getSelectionModel().clearSelections(false);				    
				    
				geneGrid.setTitle("<div style='height: 15px; overflow: hidden;' " +  // Make the header one line only.
					"ext:qtitle='" + selectedPhenotypePrefix + "' " +
					"ext:qtip='" + selectedPhenotypeTooltip + "'>" + selectedPhenotypeHeader + "</div>");
			} else {
				geneGrid.setTitle("Genes");						
				geneGrid.getStore().removeAll(false);
			}
        }
    }
});


Gemma.GeneGrid = Ext.extend(Ext.grid.GridPanel, {
    initComponent: function() {
		Ext.apply(this, {
			title: "Genes",
		    autoScroll: true,
		    stripeRows: true,
			region: "center",
			store: new Gemma.GeneStore({
				proxy: this.geneStoreProxy
			}),
			loadMask: true,
		    viewConfig: {
		        forceFit: true
		    },
			myPageSize: 50,
			columns:[{
				header: "Symbol",
				dataIndex: 'officialSymbol',
				width: 65,
				sortable: true
			},{
				header: "Name",
				dataIndex: 'officialName',
				width: 215,
				renderer: tooltipRenderer,
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
						if (selModel.hasSelection()) {
							var geneGridSelection = geneGrid.getSelectionModel().getSelected();
		
							evidenceGrid.loadData(geneGridSelection.json.evidence);
							evidenceGrid.setTitle("Evidence for " + geneGridSelection.get('officialSymbol'));					
						} else {
							evidenceGrid.removeAll(false);
							evidenceGrid.setTitle(evidenceGridDefaultTitle);					
						}                	
					}
				}
			}),
			tbar: [
				new Gemma.PhenotypePanelSearchField({
					getSelectionModel: function() { return geneGrid.getSelectionModel(); },
					getStore: function() { return geneGrid.getStore(); },
					filterFields: [ 'officialSymbol', 'officialName' ],
					emptyText: 'Search Genes'
				})
			]
		});
		Gemma.GeneGrid.superclass.initComponent.call(this);
		
		this.getStore().setDefaultSort('officialSymbol', 'asc');
    }
});

Gemma.GeneStore = Ext.extend(Ext.data.Store, {
	constructor: function(config) {
		Gemma.GeneStore.superclass.constructor.call(this, config);
	},
	reader: new Ext.data.JsonReader({
		root: 'records', // required.
		successProperty: 'success', // same as default.
		messageProperty: 'message', // optional
		totalProperty: 'totalRecords', // default is 'total'; optional unless paging.
		idProperty: 'id', // same as default
		fields: [ 'id', 'officialSymbol', 'officialName', 'taxonCommonName', 'evidence' ]
    })
});

Gemma.PhenotypePanelSearchField = Ext.extend(Ext.form.TwinTriggerField, {
    initComponent: function() {
    	if (this.getSelectionModel && this.getStore && this.filterFields && this.emptyText) {
	        Gemma.PhenotypePanelSearchField.superclass.initComponent.call(this);
	        this.on('specialkey', function(f, e) {
	            if (e.getKey() == e.ENTER) {
	                this.onTrigger2Click();
	            }
	        }, this);
    	} else {
    		Ext.Msg.alert('Error in Gemma.PhenotypePanelSearchField', 'You should set all these configs: <b>getSelectionModel</b>, <b>getStore</b>, <b>filterFields</b> and <b>emptyText</b>.');
    	}
    },
	
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
        	this.getStore().clearFilter(false);
            this.el.dom.value = '';
            this.triggers[0].hide();
            this.hasSearch = false;
        }
    },
    onTrigger2Click: function() {
        var typedString = this.getRawValue().toLowerCase();
        if (typedString.length < 1) {
            this.onTrigger1Click();
            return;
        }

		this.getStore().filterBy(
			function(record) {
				for (var i = 0; i < this.filterFields.length; i++) {
					if (record.get(this.filterFields[i]).toLowerCase().indexOf(typedString) >= 0 || this.getSelectionModel().isSelected(record)) {
						return true;
					}
				}
			    return false;
		    },
		    this	
		);

        this.hasSearch = true;
        this.triggers[0].show();
    }
});

function tooltipRenderer(value, metadata) {
    metadata.attr = 'ext:qtip="' + value + '"';
    return value;
}
