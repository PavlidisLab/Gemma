Ext.namespace('Gemma');

var evidenceGrid;

// Should provide 'store', 'filterFields' and 'emptyText'.
SearchField = Ext.extend(Ext.form.TwinTriggerField, {
    constructor: function(config) {
        SearchField.superclass.constructor.apply(this, arguments);
        
       	this.store.on('load', function() {
       		this.onTrigger1Click();
       	}, this);
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
        	this.store.clearFilter(false);
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
		if (Ext.isArray(this.filterFields)) {
			this.store.filterBy(
				function(record) {
					for (var i = 0; i < this.filterFields.length; i++) {
						if (record.get(this.filterFields[i]).toLowerCase().indexOf(typedString) >= 0) {
							return true;
						}
					}
				    return false;
			    },
			    this	
			);
		} else {
			this.store.filter(this.filterFields, typedString, true, false, false);
		}
        this.hasSearch = true;
        this.triggers[0].show();
    }
});

var phenotypeStore = new Ext.data.Store({
	reader: new Ext.data.JsonReader({
		root: 'records', // required.
		successProperty: 'success', // same as default.
		messageProperty: 'message', // optional
		totalProperty: 'totalRecords', // default is 'total'; optional unless paging.
//		idProperty: "id", // same as default
		fields: [ 'value', { name: 'occurence', type: "long" } ]
	})
});
phenotypeStore.setDefaultSort('value', 'asc');

	
var geneStore = new Ext.data.Store({
	reader: new Ext.data.JsonReader({
		root: 'records', // required.
		successProperty: 'success', // same as default.
		messageProperty: 'message', // optional
		totalProperty: 'totalRecords', // default is 'total'; optional unless paging.
		idProperty: 'id', // same as default
		fields: [ 'id', 'officialSymbol', 'officialName', 'taxonCommonName', 'evidences' ]
    })
});
geneStore.setDefaultSort('officialSymbol', 'asc');

function tooltipRenderer(value, metadata){
    metadata.attr = 'ext:qtip="' + value + '"';
    return value;
}

var geneGrid = new Ext.grid.GridPanel({
	title: "Genes",
    autoScroll: true,
    stripeRows: true,
	region: "center",
	store: geneStore,
	loadMask: true,
    viewConfig: {
        forceFit: true
    },
	columns:[{
		header: "Symbol",
		dataIndex: 'officialSymbol',
		width: 60,
		sortable: true
	},{
		header: "Name",
		dataIndex: 'officialName',
		width: 220,
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

					var phenotypeGridSelections = phenotypeGrid.getSelectionModel().getSelections();
					var selectedPhenotypes = [];					
					for (var i = 0; i < phenotypeGridSelections.length; i++) {
						selectedPhenotypes.push(phenotypeGridSelections[i].get('value'));
					}				

					// Make the selected phenotypes bold and red.
					evidenceGrid.loadData(geneGridSelection.json.evidences, selectedPhenotypes, 'font-weight: bold; color: red;');
					evidenceGrid.setTitle("Evidence for " + geneGridSelection.get('officialSymbol'));					
				} else {
					evidenceGrid.removeAll(false);
					evidenceGrid.setTitle(evidenceGridDefaultTitle);					
				}                	
			}
		}
	}),
	tbar: [
		new SearchField({
			store: geneStore,
			filterFields: [ 'officialSymbol', 'officialName' ],
			emptyText: 'Search Genes'
		})
	]

//              bbar: new Ext.PagingToolbar({
//                  pageSize: 8,
//                  store: store,
//                  displayInfo: true
//              })
});

var checkboxSel = new Ext.grid.CheckboxSelectionModel({
//sortable: true,
//width: 100,
	singleSelect: false,
	header: '', // remove the "select all" checkbox on the header 
    listeners: {
        selectionchange: function(selModel) {
			if (selModel.hasSelection()) {
				var phenotypeSelections = selModel.getSelections();
				
				var storeBaseParams = [];
				
				var selectedPhenotypePrefix = 'Genes associated with';
				var selectedPhenotypeHeader = selectedPhenotypePrefix + ' "';
				var selectedPhenotypeTooltip = '&nbsp;&nbsp;&nbsp;';
				
			    for (var i = 0; i < phenotypeSelections.length; i++) {
			    	var currPhenotypeValue = phenotypeSelections[i].get('value');

			        storeBaseParams.push(currPhenotypeValue);
			        
					selectedPhenotypeHeader += currPhenotypeValue;
					selectedPhenotypeTooltip += currPhenotypeValue;
					
					if (i < phenotypeSelections.length - 1) {
						selectedPhenotypeHeader += '" + "';
						selectedPhenotypeTooltip += '<br />&nbsp;&nbsp;&nbsp;';
					} else {
						selectedPhenotypeHeader += '"';
					}	
				}
				geneStore.baseParams = geneStore.baseParams || {};
			    geneStore.baseParams['phenotypeValue[]'] = storeBaseParams;
//			    evidenceStore.removeAll(false);
evidenceGrid.removeAll(false);
			    geneStore.reload({
			    	params: {
						start: 0,
						limit: phenotypeGrid.myPageSize							
					}
			    });
				geneGrid.getSelectionModel().clearSelections(false);				    
				    
				geneGrid.setTitle("<div style='height: 15px; overflow: hidden;' " +  // Make the header one line only.
					"ext:qtitle='" + selectedPhenotypePrefix + "' " +
					"ext:qtip='" + selectedPhenotypeTooltip + "'>" + selectedPhenotypeHeader + "</div>");
			} else {
				geneGrid.setTitle("Genes");						
				geneStore.removeAll(false);
			}
        }
    }
});


var phenotypeGrid = new Ext.grid.GridPanel({
	title: "All Phenotypes",
    autoScroll: true,
    stripeRows: true,
	width: 350,
	region: "west",
	split: true,
	store: phenotypeStore,
	loadMask: true,
    viewConfig: {
        forceFit: true
    },
	myPageSize:500,
	
	// grid columns
	columns:[
		checkboxSel,
		{
			header: "Phenotype",
			dataIndex: 'value',
			width: 310,
			renderer: tooltipRenderer,		
			sortable: true
		},{
			header: "Gene Count",
			dataIndex: 'occurence',
			align: "right",
			width: 110,
			renderer: Ext.util.Format.numberRenderer('0,0'),
			sortable: true
	    }
	],
    sm: checkboxSel,
	tbar: [
		new SearchField({
			store: phenotypeStore,
			filterFields: 'value',
			emptyText: 'Search Phenotypes'
		})
	]
//    bbar: new Ext.PagingToolbar({
//        pageSize: 8,
//        store: store,
//        displayInfo: true
//    })
});

var upperPanel = new Ext.Panel({
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
});
    	
/**
 * Config required if not accessing page from withing Gemma site:
 * 
 * phenotypeStoreProxy - TODO describe function
 * geneStoreProxy - TODO describe
 * geneColumnRenderer - TODO describe
 * 
 * If one of the proxy configs are set but not the other, the panel will throw an error msg and use Gemma defaults
 * 
 * useGemmaDefaults 
 * 
 * 
 * @class Gemma.PhenotypesPanel
 * @extends Ext.Panel
 */
Gemma.PhenotypesPanel = Ext.extend(Ext.Panel, {
	// default proxy configs
	 
    initComponent: function() {
    	evidenceGrid = new Gemma.PhenotypeEvidenceGridPanel();
    	
		Ext.apply(this, {
	        height: 600,
    	    width: 760,
			layout: 'border',        
        	items: [
	            upperPanel,
	            evidenceGrid
	        ]
		});
		Gemma.PhenotypesPanel.superclass.initComponent.apply(this, arguments);
		
		// make sure you either: 
		// (1) want to use gemma default proxies and renderer or 
		// (2) want to use proxies and renderer defined in component config
		if (!this.useGemmaDefaults && (!this.phenotypeStoreProxy || !this.geneStoreProxy || !this.geneColumnRenderer)) {
			Ext.Msg.alert('Error', 'You must specify phenotypeStoreProxy, geneStoreProxy and geneColumnRenderer in config');
		} else {
			var phenotypeStoreProxy, geneStoreProxy, geneColumnRenderer;
			// using gemma defaults
			if (this.useGemmaDefaults){
				phenotypeStoreProxy = new Ext.data.DWRProxy(PhenotypeController.loadAllPhenotypes),
				geneStoreProxy = new Ext.data.DWRProxy({
			        apiActionToHandlerMap: {
		    	        read: {
		        	        dwrFunction: PhenotypeController.findCandidateGenes,
		            	    getDwrArgsFunction: function(request){
		            	    	return [request.params["phenotypeValue[]"]];
			                }
		    	        }
			        }
		    	});
    			// default gene column renderer
    			geneColumnRenderer = function(value, metadata, record, row, col, ds) {
					return String.format("<a target='_blank' href='/Gemma/gene/showGene.html?id={0}'>{1}</a> ",
						record.data.id, record.data.officialSymbol);
				};
			} else {
				// using config values
				phenotypeStoreProxy = this.phenotypeStoreProxy;
				geneStoreProxy = this.geneStoreProxy;
				geneColumnRenderer = this.geneColumnRenderer;
			}

			phenotypeStore.proxy = phenotypeStoreProxy;
			phenotypeStore.load();

			geneStore.proxy = geneStoreProxy;
		
			geneGrid.getColumnModel().setRenderer(0, geneColumnRenderer);
		}
    }

/*	initializePanel: function(phenotypeStoreProxy, geneStoreProxy, geneColumnRenderer) {    
		phenotypeStore.proxy = phenotypeStoreProxy;
		phenotypeStore.load();

		geneStore.proxy = geneStoreProxy;
		
		geneGrid.getColumnModel().setRenderer(0, geneColumnRenderer);
	}
  */  
});
