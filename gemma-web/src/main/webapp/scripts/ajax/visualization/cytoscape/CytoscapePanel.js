Ext.namespace('Gemma');

Gemma.CytoscapePanel = Ext.extend(
Ext.Panel, {

    title: 'Cytoscape',
    layout: 'fit',    
    
    //this is for a bug in ExtJS tabPanel that causes an unactivated Panel in a TabPanel to be rendered when the Panel is removed from the tabPanel
    stopRender: false,    
    
    coexpressionSearchData: {},

    initComponent: function () {
        var controlBar = new Gemma.CytoscapeControlBar();

        this.display = new Gemma.CytoscapeDisplay({
            id: 'cytoscapeweb',
            controlBar: controlBar,
            controller: this,
            initialZoomLevel: null,
            listeners: {
                afterrender: {
                    scope: this,
                    fn: this.cytoscapePanelAfterRenderHandler
                }
            }
        });

        controlBar.display = this.display;

        this.graphSizeMenu = new Ext.menu.Menu({
        	
            items: [{
                itemId: 'graphSizeLarge',
                text: 'Large',                
                handler: function () {
                	this.graphSizeMenuHandler("large");
                },
                scope: this
            }, {
                itemId: 'graphSizeMedium',
                text: 'Medium',                
                handler: function () {                	
                	this.graphSizeMenuHandler("medium");
                },
                scope: this
            }, {
                itemId: 'graphSizeSmall',
                text: 'Small',                
                handler: function () {
                	this.graphSizeMenuHandler("small");
                },
                scope: this
            }]
        });
        
        var bbar = new Ext.Toolbar({
        	hidden: true,
        	items: [{
                xtype: 'tbtext',
                text: '',
                itemId: 'bbarStatus'
            },
            {
                xtype: 'button',
                id: 'graphSizeMenu',
                text: '<b>Graph Size</b>',
                menu: this.graphSizeMenu
                
            }, {
                xtype: 'label',
                id: 'tooltipMenuNotEnabled',
                html: '&nbsp&nbsp<img ext:qtip="' + Gemma.HelpText.WidgetDefaults.CytoscapePanel.graphSizeMenuTT2 + '" src="/Gemma/images/icons/question_blue.png"/>&nbsp',
                height: 15
            }, {
                xtype: 'label',
                id: 'tooltipMenuEnabled',
                html: '&nbsp&nbsp<img ext:qtip="' + Gemma.HelpText.WidgetDefaults.CytoscapePanel.graphSizeMenuTT + '" src="/Gemma/images/icons/question_blue.png"/>&nbsp',
                height: 15
            }]
        	
        });
        
        Ext.apply(
        this, {
            tbar: controlBar,
            bbar: bbar,
            margins: {
                top: 0,
                right: 0,
                bottom: 0,
                left: 0
            },
            items: [this.display]
        });

        Gemma.CytoscapePanel.superclass.initComponent.apply(
        this, arguments);

        this.on('activate', function() {
        	
        	//check to see if coexGrid display stringency is below cytoscape results stringency, if so, give the user the option of reloading graph
        	//at new stringency or returning display to current cytoscape stringency
        	if (this.display.ready&&this.coexpressionSearchData.coexGridCoexCommand.displayStringency < this.coexpressionSearchData.cytoscapeCoexCommand.stringency) {
        		
        		Ext.Msg.show({
                    title: 'New Search Required to View Graph at Current Stringency',
                    msg: String.format(Gemma.HelpText.WidgetDefaults.CytoscapePanel.newSearchOrReturnToCurrentStringencyOption,
                    		this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency, this.coexpressionSearchData.coexGridCoexCommand.stringency),
                    buttons: {
                        ok: 'Search for new graph data',
                        cancel: 'Use lowest graph stringency'
                    },
                    fn: function (btn) {
                        if (btn == 'ok') {
                        	
                        	var resultsStringency = Gemma.CytoscapePanelUtil.restrictResultsStringency(this.coexpressionSearchData.coexGridCoexCommand.displayStringency);

                    		Ext.apply(
                    				this.coexpressionSearchData.cytoscapeCoexCommand, {
                    					stringency: resultsStringency,
                    					displayStringency: this.coexpressionSearchData.coexGridCoexCommand.displayStringency
                    				});
                    		                    		
                        	this.searchForCytoscapeData();                        	
                            
                        } else {
                        	//cancel was pressed
                        	this.stringencyChange(this.coexpressionSearchData.cytoscapeCoexCommand.stringency);
                        	
                        }
                    }.createDelegate(this)
                });   		
        		
        	} else if (this.display.ready){
        		this.refreshGraphFromCoexpressionSearchData();
        	}
        	
        }, this);
        
        this.on('doneDrawingCytoscape', function () {
        	
        	this.display.clearSearchBox();
        	this.fireEvent("textBoxMatchFromCoexpressionViz", '');
        	
        	//check to see if it is query genes only
            if (this.display.isQueryGenesOnly()){
            	this.display.filter(this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency,
    					this.coexpressionSearchData.coexGridCoexCommand.geneIds,
    					false);            	
            }        	
        	//do initial filtering of graph(the graph has already been drawn at results stringency, we need to filter this graph to the display stringency if necessary)
            else if (this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency > this.coexpressionSearchData.cytoscapeCoexCommand.stringency) {
                var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResultsWithQueryGenes(this.coexpressionSearchData.cytoscapeResults.knownGeneResults, this.coexpressionSearchData.coexGridCoexCommand.geneIds, this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency);
                this.display.filter(this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency, trimmed.trimmedNodeIds, true);
            }

        }, this);
        
        this.on('searchTextBoxMatch', function (text) {
        	this.fireEvent("textBoxMatchFromCoexpressionViz", text);       	

        }, this);
        
        this.on('searchForCytoscapeDataComplete', function() {
        	
        	if (this.coexpressionSearchData.coexGridCoexCommand.geneIds.length <2) {
				
				this.display.disableQueryGenesOnly(true);
				this.display.setQueryGenesOnly(false);
				
			}else{
				this.display.disableQueryGenesOnly(false);
			}      	
        	
        	this.coexpressionGraphData = new Gemma.CoexpressionGraphData(this.coexpressionSearchData);        	
        	
        	this.showUserMessageBarAfterNewData();        	
        	
        	this.drawGraph();
        	
        }, this);        
        

        //maybe have this take the results as an argument for more detailed error message
        this.on('searchErrorFromCoexpressionSearchData', function(result) {
        	
        	this.timeOutFromCoexSearchHandler();
        	
        }, this);
        
        this.on('stringencyUpdateFromCoexGrid', function() {
        	
        	if (this.display.ready){
        		
        		//if we already have viz results at current stringency
        		if (this.coexpressionSearchData.coexGridCoexCommand.displayStringency >= this.coexpressionSearchData.cytoscapeCoexCommand.stringency) {        	
        			
        			this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency = this.coexpressionSearchData.coexGridCoexCommand.displayStringency;
        	
        		}else if (this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency != this.coexpressionSearchData.coexGridCoexCommand.displayStringency){
        			//show results at lowest stringency available(if not already showing them at that stringency        			
        			this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency = this.coexpressionSearchData.cytoscapeCoexCommand.stringency;
        			
        		}  	
        	
        	}else{
        		//cytoscape hasn't loaded yet so update the command that will be used to grab the cytoscape results        		
        		var resultsStringency = Gemma.CytoscapePanelUtil.restrictResultsStringency(this.coexpressionSearchData.coexGridCoexCommand.displayStringency);
        		Ext.apply(
        		        this.coexpressionSearchData.cytoscapeCoexCommand, {
        		            stringency: resultsStringency,
        		            displayStringency: this.coexpressionSearchData.coexGridCoexCommand.displayStringency
        		        });
        	}
        	
        }, this);
        
        this.on('queryGenesOnlyUpdateFromCoexGrid', function(checked) {
        	
        	this.display.setQueryGenesOnly(checked);        	
        	
        }, this);
        
        this.on('textBoxMatchFromCoexGrid', function (text) {
        	if (this.display.ready){
        		this.display.textBoxMatchHandler(text);       	
        	}
        }, this);

        this.addEvents('stringencyUpdateFromCoexpressionViz', 'dataUpdateFromCoexpressionViz', 'queryUpdateFromCoexpressionViz','coexWarningAlreadyDisplayed','textBoxMatchFromCoexpressionViz');
        this.relayEvents(this.display, ['doneDrawingCytoscape', 'searchTextBoxMatch']);
        this.relayEvents(this.coexpressionSearchData, ['searchForCoexGridDataComplete','searchForCytoscapeDataComplete','searchErrorFromCoexpressionSearchData']);

        
        if(this.knownGeneGrid){
        	this.relayEvents(this.knownGeneGrid, ['stringencyUpdateFromCoexGrid','queryGenesOnlyUpdateFromCoexGrid','textBoxMatchFromCoexGrid']);
        	this.knownGeneGrid.relayEvents(this, ['stringencyUpdateFromCoexpressionViz', 'dataUpdateFromCoexpressionViz', 'queryGenesOnlyUpdateFromCoexpressionViz','textBoxMatchFromCoexpressionViz']);
        	this.knownGeneGrid.relayEvents(this.coexpressionSearchData, ['searchForCoexGridDataComplete']);	    
        }
	    
        if (this.searchPanel){
        	this.searchPanel.relayEvents(this, ['queryUpdateFromCoexpressionViz', 'beforesearch']);
        }
        
    },
    
    searchForCytoscapeData : function(){
    	
    	this.loadMask.show();
		this.display.updateStringency(this.coexpressionSearchData.coexGridCoexCommand.displayStringency);
    	this.coexpressionSearchData.searchForCytoscapeData();
    	
    },
    
    filterQueryGenesOnly : function(suppressEvent){
    	
    	if(!suppressEvent){
    		this.fireEvent('queryGenesOnlyUpdateFromCoexpressionViz', this.display.isQueryGenesOnly())
    	}
    	
		if (this.display.isQueryGenesOnly()){
			
			this.display.filter(this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency,
					this.coexpressionSearchData.coexGridCoexCommand.geneIds,
					false);
			
		}else{
			
			var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResultsWithQueryGenes(
					this.coexpressionSearchData.cytoscapeResults.knownGeneResults,
					this.coexpressionSearchData.coexGridCoexCommand.geneIds, this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency);
			this.display.filter(this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency,
					trimmed.trimmedNodeIds,
					false);    			
		}
    	
    },

    //called from controlBar
    stringencyChange: function (stringencyValue) {

        if (stringencyValue < 2) {
            stringencyValue = 2;
        }

        if (stringencyValue >= this.coexpressionSearchData.cytoscapeCoexCommand.stringency) {
        	this.fireEvent('stringencyUpdateFromCoexpressionViz', stringencyValue);
        	
        	this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency = stringencyValue;
        	
            this.refreshGraphFromCoexpressionSearchData();
        } else {
            //new search            	
            this.newSearchForLowerStringencyHandler(stringencyValue);
        }

    },    
    
    refreshGraphFromCoexpressionSearchData: function () {

        this.display.updateStringency(this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency);
        
        if (!this.display.isQueryGenesOnly()){
        
        	var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResultsWithQueryGenes(this.coexpressionSearchData.cytoscapeResults.knownGeneResults,
        			this.coexpressionSearchData.coexGridCoexCommand.geneIds, this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency);
        
        	this.display.filter(this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency, trimmed.trimmedNodeIds);
        
        }else{
        	
        	this.filterQueryGenesOnly(true);
        }
    },

    newSearchForLowerStringencyHandler: function (stringencyValue) {
    	
    	if (!this.warningAlreadyDisplayed){

        Ext.Msg.show({
            title: 'New Search',
            msg: Gemma.HelpText.WidgetDefaults.CytoscapePanel.lowStringencyWarning,
            buttons: {
                ok: 'Proceed',
                cancel: 'Cancel'
            },
            fn: function (btn) {
                if (btn == 'ok') {                	
                	this.warningAlreadyDisplayed = true;
                	this.newSearchForLowerStringencyHandlerNoWarning(stringencyValue);
                    
                } else {
                    this.display.updateStringency();
                }
            }.createDelegate(this)
        });
        
    	} else {
    		this.newSearchForLowerStringencyHandlerNoWarning(stringencyValue);    		
    	}

    },
    
    newSearchForLowerStringencyHandlerNoWarning: function (stringencyValue){
    	
        var resultsStringency = Gemma.CytoscapePanelUtil.restrictResultsStringency(stringencyValue);

        Ext.apply(
        this.coexpressionSearchData.cytoscapeCoexCommand, {
            stringency: resultsStringency,
            displayStringency: stringencyValue,            
            queryGenesOnly: true
        });        
        
        this.searchForCytoscapeData();        
   
    },

    searchWithSelectedNodes: function (selectedNodesGeneIdArray) {

        this.clearError();

        if (selectedNodesGeneIdArray.length > 0 && selectedNodesGeneIdArray.length <= Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {
                    	
            //initialSearch for coex grid data will be at 2
            var resultsStringency = 2;
            
            this.updateSearchFormGenes(selectedNodesGeneIdArray);

            this.loadMask.show();            
                      
            Ext.apply(this.coexpressionSearchData.coexGridCoexCommand, {
                stringency: resultsStringency,
                displayStringency: this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency,
                geneIds: selectedNodesGeneIdArray,
                queryGenesOnly: false
            });
            
            Ext.apply(
                    this.coexpressionSearchData.cytoscapeCoexCommand, {
                        stringency: Gemma.CytoscapePanelUtil.restrictResultsStringency(this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency),                                  
                        queryGenesOnly: true
                    });  
            
            this.coexpressionSearchData.searchForCoexGridDataAndCytoscapeData();            

        } else if (selectedNodesGeneIdArray.length > Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {
            Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, String.format(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooMany, Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY));
        } else if (selectedNodesGeneIdArray.length == 0) {
            Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooFew);
        }

    },

    extendNodes: function (selectedNodesGeneIdArray) {

        this.clearError();

        if (selectedNodesGeneIdArray.length > 0 && selectedNodesGeneIdArray.length <= Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {

            if (this.coexpressionSearchData.coexGridCoexCommand.geneIds.length + selectedNodesGeneIdArray.length <= Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {
            	
            	var newQueryGeneSelected=false;
                var i;
                
                for (i = 0; i < this.coexpressionSearchData.coexGridCoexCommand.geneIds.length; i++) {                    

                    if (selectedNodesGeneIdArray.indexOf(this.coexpressionSearchData.coexGridCoexCommand.geneIds[i]) === -1) {
                    	newQueryGeneSelected=true;
                    	selectedNodesGeneIdArray.push(this.coexpressionSearchData.coexGridCoexCommand.geneIds[i]);
                    }
                }
            	
                if (newQueryGeneSelected){
                	this.searchWithSelectedNodes(selectedNodesGeneIdArray);                	
                }
                else {
                	Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusNoExtraSelectedForExtend);
                }
                
            } else {
                Ext.Msg.confirm(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, String.format(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooManyReduce, Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY), function (btn) {
                    if (btn == 'yes') {
                    	
                        //ensure that selectedNodes includes the current query genes plus the newly selected genes and that the number of querygeneids is less than the max
                        this.coexpressionSearchData.coexGridCoexCommand.geneIds = this.coexpressionSearchData.coexGridCoexCommand.geneIds.splice(this.coexpressionSearchData.coexGridCoexCommand.geneIds.length - (Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY - selectedNodesGeneIdArray.length));                        
                        var i;                        
                        for (i = 0; i < this.coexpressionSearchData.coexGridCoexCommand.geneIds.length; i++) {                    

                            if (selectedNodesGeneIdArray.indexOf(this.coexpressionSearchData.coexGridCoexCommand.geneIds[i]) === -1) {                            	
                            	selectedNodesGeneIdArray.push(this.coexpressionSearchData.coexGridCoexCommand.geneIds[i]);
                            }
                        }
                        
                        this.searchWithSelectedNodes(selectedNodesGeneIdArray);
                    }
                }, this);
            }

        } else if (selectedNodesGeneIdArray.length > Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {
            Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, String.format(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooMany, Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY));
        } else {
            Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooFew);
        }

    },
 
    //This is called the first time the cytoscape panel is rendered.  
    //It is a special case because it uses the partial results from the coexpression grid to do the complete search    
    cytoscapePanelAfterRenderHandler: function () {

    	//stopRender is needed because of a bug in extjs where a tabpanel render its components upon removal
    	if (!this.stopRender){
    	
        if (!this.loadMask) {
            this.loadMask = new Ext.LoadMask(
            this.getEl(), {
                msg: Gemma.StatusText.Searching.analysisResults,
                msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
            });
        }        
        
        //in case a user has typed an invalid string into the spinner box on the grid, this resets it
        this.fireEvent('stringencyUpdateFromCoexpressionViz', this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency);
        
        this.searchForCytoscapeData();
        
    	}
    },
    
    //ensure that selected nodes that are filtered out due to stringency/my genes only are not used for new searches
    restrictSelectedNodesByCurrentSettings: function (selectedNodes){
    	
    	var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResultsWithQueryGenes(this.coexpressionSearchData.cytoscapeResults.knownGeneResults,
    			this.coexpressionSearchData.coexGridCoexCommand.geneIds, this.coexpressionSearchData.cytoscapeCoexCommand.displayStringency);
    
    	trimmed.trimmedNodeIds;
    	
    	var restrictedNodes = [];
    	var i;
    	
    	for (i = 0 ; i<selectedNodes.length ; i++){
    		if (trimmed.trimmedNodeIds.indexOf(selectedNodes[i]) !==-1){
    			
    			if (this.display.isQueryGenesOnly() && this.coexpressionSearchData.coexGridCoexCommand.geneIds.indexOf(selectedNodes[i]) ==-1){
    				continue;
    			}
    			
    			restrictedNodes.push(selectedNodes[i]);    			
    		}
    	}
    	
    	return restrictedNodes;
    	
    },

    drawGraph: function () {
        this.display.drawGraph(this.coexpressionSearchData);        
        this.loadMask.hide();
    },

    clearError: function () {
        if (Ext.get("analysis-results-search-form-messages")) {
            Ext.DomHelper.overwrite("analysis-results-search-form-messages", "");
        }
    },
	
	//after new data we need to determine whether it is appropriate to show the graph or not
	showUserMessageBarAfterNewData: function(){		
		
		if (!this.coexpressionGraphData.mediumGraphDataEnabled){
			
			if(this.coexpressionSearchData.cytoscapeResults.nonQueryGeneTrimmedValue>0){
				this.showUserMessageBar(this.coexpressionSearchData.cytoscapeResults.nonQueryGeneTrimmedValue, false);
			}else{
				this.getBottomToolbar().hide();
	    		this.doLayout();				
			}
			return;
		}
		
		var resultsObject = this.coexpressionGraphData.getGraphData();
    			
    	if (resultsObject.trimStringency){
    		this.coexpressionSearchData.cytoscapeResults.knownGeneResults = resultsObject.geneResults;
    		this.showUserMessageBar(resultsObject.trimStringency, true);
    	}else if(this.coexpressionSearchData.cytoscapeResults.nonQueryGeneTrimmedValue>0){
    		this.showUserMessageBar(this.coexpressionSearchData.cytoscapeResults.nonQueryGeneTrimmedValue, false);        		
    	} else{
    		this.getBottomToolbar().hide();
    		this.doLayout();
    	}
		
		
		
	},
	
    showUserMessageBar: function (trimmedValue, showGraphSizeMenu) {
        
    	var bbarText = this.getBottomToolbar().getComponent('bbarStatus');
    	
    	var graphSizeButton = this.getBottomToolbar().getComponent("graphSizeMenu");
    	
    	if (showGraphSizeMenu){
    		
    		bbarText.setText("Edges not involving query genes have been trimmed to a stringency of: ");
    		
    		if (trimmedValue == 0){
    			graphSizeButton.setText("No Trimming ");
    		}else{
    			graphSizeButton.setText(trimmedValue+" ");
    		}
    		this.getBottomToolbar().getComponent('tooltipMenuEnabled').setVisible(true);
    		this.getBottomToolbar().getComponent('tooltipMenuNotEnabled').setVisible(false);
    		
    		
    		if (this.coexpressionGraphData.largeGraphDataEnabled){
    			
    			//case where the original Results haven't been trimmed
    			if (this.coexpressionGraphData.originalResults.trimStringency == 0){
    				
    				this.graphSizeMenu.getComponent("graphSizeLarge").setText("No Trimming ("+this.coexpressionGraphData.originalResults.geneResults.length+" edges)");
    			}else{    			
    				this.graphSizeMenu.getComponent("graphSizeLarge").setText(this.coexpressionGraphData.originalResults.trimStringency+" ("+this.coexpressionGraphData.originalResults.geneResults.length+" edges)");    				
    			}
    			
    			this.graphSizeMenu.getComponent("graphSizeLarge").show();
    			
    			if (trimmedValue == this.coexpressionGraphData.originalResults.trimStringency){
    				this.graphSizeMenu.getComponent("graphSizeLarge").addClass("buttonBold");
    			}else{
    				this.graphSizeMenu.getComponent("graphSizeLarge").removeClass("buttonBold");
    			}
    		}else{
    			this.graphSizeMenu.getComponent("graphSizeLarge").hide();
    		}
    		
    		if (this.coexpressionGraphData.mediumGraphDataEnabled){
    			this.graphSizeMenu.getComponent("graphSizeMedium").setText(this.coexpressionGraphData.graphDataMedium.trimStringency+" ("+this.coexpressionGraphData.graphDataMedium.geneResults.length+" edges)");
    			this.graphSizeMenu.getComponent("graphSizeMedium").show();
    			
    			if (trimmedValue == this.coexpressionGraphData.graphDataMedium.trimStringency){
    				this.graphSizeMenu.getComponent("graphSizeMedium").addClass("buttonBold");
    			}else{
    				this.graphSizeMenu.getComponent("graphSizeMedium").removeClass("buttonBold");
    			}
    			
    		}else{
    			this.graphSizeMenu.getComponent("graphSizeMedium").hide();
    		}
    		
    		if (this.coexpressionGraphData.smallGraphDataEnabled){
    			this.graphSizeMenu.getComponent("graphSizeSmall").setText(this.coexpressionGraphData.graphDataSmall.trimStringency+" ("+this.coexpressionGraphData.graphDataSmall.geneResults.length+" edges)");
    			this.graphSizeMenu.getComponent("graphSizeSmall").show();
    			
    			if (trimmedValue == this.coexpressionGraphData.graphDataSmall.trimStringency){
    				this.graphSizeMenu.getComponent("graphSizeSmall").addClass("buttonBold");
    			}else{
    				this.graphSizeMenu.getComponent("graphSizeSmall").removeClass("buttonBold");
    			}
    		}else{
    			this.graphSizeMenu.getComponent("graphSizeSmall").hide();
    		}
    		
    		
    		if(!graphSizeButton.isVisible()){        		
    			graphSizeButton.show();
    		}
    		
    	}else{
    		
    		this.getBottomToolbar().getComponent('tooltipMenuEnabled').setVisible(false);
    		this.getBottomToolbar().getComponent('tooltipMenuNotEnabled').setVisible(true);
    		
    		if (graphSizeButton.isVisible()){        		
    			graphSizeButton.hide();
    		}    		
    		bbarText.setText("Edges not involving query genes have been trimmed to a stringency of: "+trimmedValue);
    	}
    	
    	this.getBottomToolbar().show();
            
        this.doLayout();            
    },

    updateSearchFormGenes: function (geneIds) {
        //this collects all the query Genevalueobjects and fires an event to let the search form listening know that the query has been changed.
        //We already have the geneValueObjects from the search results so this saves an extra call to the backend
        //because the search form usually queries the backend for this information
        var genesToPreview = [];
        var genesToPreviewIds = [];
        var knowngenes = this.coexpressionSearchData.cytoscapeResults.knownGeneResults;
        var kglength = knowngenes.length;
        var i;
        for (i = 0; i < kglength; i++) {
            if (genesToPreviewIds.indexOf(knowngenes[i].foundGene.id) === -1 && geneIds.indexOf(knowngenes[i].foundGene.id) !== -1) {
                genesToPreview.push(knowngenes[i].foundGene);
                genesToPreviewIds.push(knowngenes[i].foundGene.id);
            }
            if (genesToPreviewIds.indexOf(knowngenes[i].queryGene.id) === -1 && geneIds.indexOf(knowngenes[i].queryGene.id) !== -1) {
                genesToPreview.push(knowngenes[i].queryGene);
                genesToPreviewIds.push(knowngenes[i].queryGene.id);
            }
        } // end for (<kglength)
        
        //have to search through query genes in case none showed up
        var querygenes = this.coexpressionSearchData.coexGridResults.queryGenes;
        var qglength = querygenes.length;
        for (i = 0; i < qglength; i++) {
            if (genesToPreviewIds.indexOf(querygenes[i].id) === -1 && geneIds.indexOf(querygenes[i].id) !== -1) {
                genesToPreview.push(querygenes[i]);
                genesToPreviewIds.push(querygenes[i].id);
            }
        } // end for (<kglength)
        // add new genes
        this.fireEvent('queryUpdateFromCoexpressionViz', genesToPreview, genesToPreviewIds, this.taxonId, this.taxonName);

    },
    
    timeOutFromCoexSearchHandler : function() {
    	Ext.Msg.alert(Gemma.HelpText.CommonWarnings.Timeout.title, Gemma.HelpText.CommonWarnings.Timeout.text);
    	this.loadMask.hide();
    	this.fireEvent('beforesearch');
    },
    
    getMatchingGeneIdsByText : function(text){
    	
    	return Gemma.CoexValueObjectUtil.filterGeneResultsByText(text, this.coexpressionSearchData.cytoscapeResults.knownGeneResults);
    	
    },
    
    graphSizeMenuHandler: function(graphSize){
    	var resultsObject = this.coexpressionGraphData.getGraphData(graphSize);
    	this.coexpressionSearchData.cytoscapeResults.knownGeneResults = resultsObject.geneResults;
    	this.showUserMessageBar(resultsObject.trimStringency, true);    	
    	this.drawGraph(); 
    }
    
    

});

Gemma.CytoscapeDownloadWindow = Ext.extend(Ext.Window, {
    width: 800,
    height: 400,
    layout: 'fit',

    timeToString: function (timeStamp) {
        // Make minutes double digits.
        var min = (timeStamp.getMinutes() < 10) ? '0' + timeStamp.getMinutes() : timeStamp.getMinutes();
        return timeStamp.getFullYear() + "/" + timeStamp.getMonth() + "/" + timeStamp.getDate() + " " + timeStamp.getHours() + ":" + min;
    },

    displayXML: function (xmlString) {
        var text = '<!-- Generated by Gemma\n' + ' ' + this.timeToString(new Date()) + '\n' + ' \n' + ' If you use this file for your research, please cite the Gemma web site\n' + ' chibi.ubc.ca/Gemma \n' + '-->\n\n';
        this.textAreaPanel.setValue(text + xmlString);
        this.show();
    },

    initComponent: function () {
        Ext.apply(this, {
            tbar: [{
                ref: 'selectAllButton',
                xtype: 'button',
                text: 'Select All',
                scope: this,
                handler: function () {
                    this.textAreaPanel.selectText();
                }
            }],
            items: [new Ext.form.TextArea({
                ref: 'textAreaPanel',
                readOnly: true,
                autoScroll: true,
                wordWrap: false
            })]
        });
        Gemma.CytoscapeDownloadWindow.superclass.initComponent.call(this);
    },

    onRender: function () {
        Gemma.CytoscapeDownloadWindow.superclass.onRender.apply(this, arguments);
    }
});