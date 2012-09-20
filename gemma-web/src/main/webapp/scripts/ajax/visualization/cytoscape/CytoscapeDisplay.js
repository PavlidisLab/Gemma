/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */
Ext.namespace('Gemma');

Gemma.CytoscapeDisplay = Ext.extend(Ext.FlashComponent, {

    ready: false,

    nodeDegreeVisualStyleFlag: true,

    //need to remember the previous value of the spinner when the user cancels a typed-in stringency change
    currentDisplayStringency: 2,

    forceDirectedLayoutCompressed: Gemma.CytoscapeSettings.forceDirectedLayoutCompressed,
    defaultForceDirectedLayout: Gemma.CytoscapeSettings.defaultForceDirectedLayout,

    currentLayout: {},

    visualStyleRegular: Gemma.CytoscapeSettings.visualStyleRegular,
    visualStyleNodeDegree: Gemma.CytoscapeSettings.visualStyleNodeDegree,

    dataSchemaJSON: {
        nodes: [{
            name: 'label',
            type: 'string'
        }, {
            name: 'geneid',
            type: 'number'
        }, {
            name: 'queryflag',
            type: 'boolean'
        }, {
            name: 'nodeDegree',
            type: 'number'
        }, {
            name: 'nodeDegreeBin',
            type: 'string'
        },{
            name: 'officialName',
            type: 'string'
        }, {
            name: 'ncbiId',
            type: 'number'
        }],
        edges: [{
            name: 'positivesupport',
            type: 'number'
        }, {
            name: 'negativesupport',
            type: 'number'
        }, {
            name: 'support',
            type: 'number'
        }, {
            name: 'supportsign',
            type: 'string'
        }, {
            name: 'nodeDegree',
            type: 'number'
        }]

    },
    options: {
        // where you have the Cytoscape Web SWF
        swfPath: "/Gemma/scripts/cytoscape/swf/CytoscapeWeb",
        // where you have the Flash installer SWF
        flashInstallerPath: "/Gemma/scripts/cytoscape/swf/playerProductInstall"
    },


    initComponent: function () {

        this.visualization = new org.cytoscapeweb.Visualization(this.id, this.options);

        this.currentLayout = this.defaultForceDirectedLayout;

        this.visualization["edgeOpacityMapper"] = function (data) {
            if (data["nodeDegree"] == null) {
                return 0.05;
            }
            return 1.05 - data["nodeDegree"];
        };

        //this is called every time that the graph is re-drawn(after visualization.draw is called i.e. after a search).
        //See CytoscapeWeb documentation for why this is called every redraw
        this.visualization.ready(function () {
            this.visualization.nodeTooltipsEnabled(true);
            this.visualization.edgeTooltipsEnabled(true);

            this.visualization.addListener("select", "nodes", function (evt) {

                this.controlBar.updateActionsButtons(this.visualization.selected("nodes").length > 0);

            }.createDelegate(this));

            this.visualization.addListener("deselect", "nodes", function (evt) {

                this.controlBar.updateActionsButtons(this.visualization.selected("nodes").length > 0);

            }.createDelegate(this));

 
            this.visualization.addListener("layout", function (evt) {
            	this.scaleFont(this.visualization.zoom());
            }.createDelegate(this));


            this.visualization.addListener("zoom", function (evt) {

            	var zoom = evt.value;
            	this.scaleFont(zoom);

            }.createDelegate(this));

            this.ready = true;

            this.scaleFont(this.visualization.zoom());
            this.fireEvent('doneDrawingCytoscape');

        }.createDelegate(this));
        // end vis.ready()
        Gemma.CytoscapeDisplay.superclass.initComponent.apply(this, arguments);

        this.addEvents('doneDrawingCytoscape', 'searchTextBoxMatch');

    },

    drawGraph: function (coexpressionSearchData) {
    	    	
        var dataMsg = {
            dataSchema: this.dataSchemaJSON,
            data: this.constructDataJSON(coexpressionSearchData.coexGridCoexCommand.geneIds, coexpressionSearchData.coexGridResults.queryGenes, coexpressionSearchData.cytoscapeResults.knownGeneResults)
        };
        this.initialZoomLevel = null;        
        // init and draw
        this.visualization.draw({
            network: dataMsg,
            visualStyle: this.nodeDegreeVisualStyleFlag ? this.visualStyleNodeDegree : this.visualStyleRegular,
            layout: this.currentLayout
        });
    },
    
    constructDataJSON: function (currentQueryGeneIds, qgenes, knowngenes) {
        return this.constructDataJSONWithStringencyFiltering(currentQueryGeneIds, qgenes, knowngenes, false, null);
    },

    //This is only called from constructDataJSON(qgenes, knowngenes, false, null) above so filterCurrentResults=false and filterStringency=null
    //this means no filtering will be done.
    constructDataJSONWithStringencyFiltering: function (currentQueryGeneIds, qgenes, knowngenes, filterCurrentResults, filterStringency) {
        var data = {
            nodes: [],
            edges: []
        };
        // helper array to prevent duplicate nodes from being
        // entered
        var graphNodeIds = [];
        var edgeSet = [];
        var kglength = knowngenes.length;
        var i;
        // populate node data plus populate edge data
        for (i = 0; i < kglength; i++) {

            // if not filtering go in, or if filtering: go in
            // only if the query or known gene is contained in
            // the original query geneids AND the stringency is
            // >= the filter stringency
            if (!filterCurrentResults || ((currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) !== -1 || (currentQueryGeneIds.indexOf(knowngenes[i].queryGene.id) !== -1)) && (knowngenes[i].posSupp >= filterStringency || knowngenes[i].negSupp >= filterStringency))

            ) {
                if (graphNodeIds.indexOf(knowngenes[i].foundGene.id) === -1) {
                    isQueryGene = false;

                    if (currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) !== -1) {
                        isQueryGene = true;
                    }

                    data.nodes.push({
                        id: knowngenes[i].foundGene.officialSymbol,
                        label: knowngenes[i].foundGene.officialSymbol,
                        geneid: knowngenes[i].foundGene.id,
                        queryflag: isQueryGene,
                        officialName: Gemma.CytoscapePanelUtil.ttSubstring(knowngenes[i].foundGene.officialName),
                        ncbiId: knowngenes[i].foundGene.ncbiId,
                        nodeDegreeBin: Gemma.CytoscapePanelUtil.nodeDegreeBinMapper(knowngenes[i].foundGeneNodeDegree),
                        nodeDegree: Gemma.CytoscapePanelUtil.decimalPlaceRounder(knowngenes[i].foundGeneNodeDegree)
                    });

                    graphNodeIds.push(knowngenes[i].foundGene.id);
                }

                if (graphNodeIds.indexOf(knowngenes[i].queryGene.id) === -1) {
                    isQueryGene = false;

                    if (currentQueryGeneIds.indexOf(knowngenes[i].queryGene.id) !== -1) {
                        isQueryGene = true;
                    }

                    data.nodes.push({
                        id: knowngenes[i].queryGene.officialSymbol,
                        label: knowngenes[i].queryGene.officialSymbol,
                        geneid: knowngenes[i].queryGene.id,
                        queryflag: isQueryGene,                        
                        officialName: Gemma.CytoscapePanelUtil.ttSubstring(knowngenes[i].queryGene.officialName),
                        ncbiId: knowngenes[i].queryGene.ncbiId,
                        nodeDegreeBin: Gemma.CytoscapePanelUtil.nodeDegreeBinMapper(knowngenes[i].queryGeneNodeDegree),
                        nodeDegree: Gemma.CytoscapePanelUtil.decimalPlaceRounder(knowngenes[i].queryGeneNodeDegree)
                    });
                    graphNodeIds.push(knowngenes[i].queryGene.id);
                }

                var support;
                var supportsign;
                if (knowngenes[i].posSupp > 0 && knowngenes[i].negSupp > 0) {
                    support = Math.max(knowngenes[i].posSupp, knowngenes[i].negSupp);
                    supportsign = "both";

                } else if (knowngenes[i].posSupp > 0) {
                    support = knowngenes[i].posSupp;
                    supportsign = "positive";
                } else if (knowngenes[i].negSupp > 0) {
                    support = knowngenes[i].negSupp;
                    supportsign = "negative";
                }
                // double edge check
                if (edgeSet.indexOf(knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol) == -1 && edgeSet.indexOf(knowngenes[i].queryGene.officialSymbol + "to" + knowngenes[i].foundGene.officialSymbol) == -1) {

                    data.edges.push({
                        id: knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol,
                        target: knowngenes[i].foundGene.officialSymbol,
                        source: knowngenes[i].queryGene.officialSymbol,
                        positivesupport: knowngenes[i].posSupp,
                        negativesupport: knowngenes[i].negSupp,
                        support: support,
                        supportsign: supportsign,
                        nodeDegree: Gemma.CytoscapePanelUtil.decimalPlaceRounder(Gemma.CytoscapePanelUtil.getMaxWithNull(
                        knowngenes[i].queryGeneNodeDegree, knowngenes[i].foundGeneNodeDegree))
                    });
                    edgeSet.push(knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol);
                    edgeSet.push(knowngenes[i].queryGene.officialSymbol + "to" + knowngenes[i].foundGene.officialSymbol);
                }
            } // end if(!filterResults
        } // end for (<kglength)
        // if we are filtering, we need to loop through again to
        // add edges that we missed the first time (because we
        // were unsure whether both nodes would be in the graph)
        if (filterCurrentResults) {
            var completeGraphEdges = [];

            for (i = 0; i < kglength; i++) {
                // if both nodes of the edge are in the graph,
                // and it meets the stringency threshold, and
                // neither of the nodes are query genes(because
                // there edges have already been added)
                if (graphNodeIds.indexOf(knowngenes[i].foundGene.id) !== -1 && graphNodeIds.indexOf(knowngenes[i].queryGene.id) !== -1 && (knowngenes[i].posSupp >= filterStringency || knowngenes[i].negSupp >= filterStringency) && currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) === -1 && currentQueryGeneIds.indexOf(knowngenes[i].queryGene.id) === -1) {

                    var support;
                    var supportsign;
                    if (knowngenes[i].posSupp >= filterStringency && knowngenes[i].negSupp >= filterStringency) {
                        support = Math.max(
                        knowngenes[i].posSupp, knowngenes[i].negSupp);
                        supportsign = "both";

                    } else if (knowngenes[i].posSupp >= filterStringency) {
                        support = knowngenes[i].posSupp;
                        supportsign = "positive";
                    } else if (knowngenes[i].negSupp >= filterStringency) {
                        support = knowngenes[i].negSupp;
                        supportsign = "negative";
                    }

                    data.edges.push({
                        id: knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol,
                        target: knowngenes[i].foundGene.officialSymbol,
                        source: knowngenes[i].queryGene.officialSymbol,
                        positivesupport: knowngenes[i].posSupp,
                        negativesupport: knowngenes[i].negSupp,
                        support: support,
                        supportsign: supportsign,
                        nodeDegreeBin: Gemma.CytoscapePanelUtil.nodeDegreeBinMapper(Gemma.CytoscapePanelUtil.getMaxWithNull(
                        knowngenes[i].queryGeneNodeDegree, knowngenes[i].foundGeneNodeDegree))
                    });

                    completeGraphEdges.push(knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol);
                    completeGraphEdges.push(knowngenes[i].queryGene.officialSymbol + "to" + knowngenes[i].foundGene.officialSymbol);
                }
            } // end for (<kglength)
        }

        /*
         * //take this out for now, it just makes the graph look
         * bad to have these orphaned nodes sitting around //add
         * query gene nodes NOT in knowngenes, node degree set
         * to zero 
         *var qlength = qgenes.length;
         *var isQueryGene = false;        
         *var i; for (i = 0; i < qlength; i++) {
         * 
         * if (graphNodeIds.indexOf(qgenes[i].id) === -1) {
         * 
         * //check if this gene was part of current/previous
         * query if
         * (currentQueryGeneIds.indexOf(qgenes[i].id) !==
         * -1) { isQueryGene = true; }
         * 
         * if (!filterCurrentResults ||
         * currentQueryGeneIds.indexOf(qgenes[i].id) !==
         * -1) {
         * 
         * data.nodes.push({ id: qgenes[i].officialSymbol,
         * label: qgenes[i].officialSymbol, geneid:
         * qgenes[i].id, queryflag: isQueryGene, officialName:
         * this.ttSubstring(qgenes[i].officialName), ncbiId:
         * qgenes[i].ncbiId, nodeDegreeBin: 0, nodeDegree: 0 });
         * 
         * graphNodeIds.push(qgenes[i].id);
         *  }
         * 
         * isQueryGene = false; } }
         */
       

        return data;
    },
    
    scaleFont: function(zoom){
    	if (this.ready) {            

            if (this.initialZoomLevel) {

                var newFontSize = Math.floor(this.initialFontSize / zoom);

                this.visualStyleRegular.nodes.labelFontSize = newFontSize;
                this.visualStyleNodeDegree.nodes.labelFontSize = newFontSize;

                if (this.nodeDegreeVisualStyleFlag) {
                    this.visualization.visualStyle(this.visualStyleNodeDegree);
                } else {
                    this.visualization.visualStyle(this.visualStyleRegular);
                }

            } else {
            	//first time in zoom handler after new search, need to set/reset some settings 
                this.initialZoomLevel = zoom;

                this.initialFontSize = Gemma.CytoscapeSettings.labelFontSize;
                var newFontSize = Math.floor(this.initialFontSize / zoom);

                this.visualStyleRegular.nodes.labelFontSize = newFontSize;
                this.visualStyleNodeDegree.nodes.labelFontSize = newFontSize;

                if (this.nodeDegreeVisualStyleFlag) {
                    this.visualization.visualStyle(this.visualStyleNodeDegree);
                } else {
                    this.visualization.visualStyle(this.visualStyleRegular);
                }
            }

        }
    	
    },

    filter: function (stringency, trimmedNodeIds, doZoom){

        filterFunctionNodes = function (node) {
            return trimmedNodeIds.indexOf(node.data.geneid) !== -1;
        };

        this.visualization.filter("nodes", filterFunctionNodes.createDelegate(this));

        filterFunctionEdges = function (edge) {
            return edge.data.support >= stringency;
        };
        
        if (doZoom){
        	this.visualization.zoomToFit();
        }

        this.visualization.filter("edges", filterFunctionEdges.createDelegate(this));
    },
    
    select: function (nodeIds){      

        this.visualization.select("nodes", nodeIds);
        
    },
    
    deselect : function (){
    	this.visualization.deselect("nodes");
    	this.visualization.deselect("edges");
    },

    extendSelectedNodesHandler: function () {
        if (this.ready) {
        	var selectedNodes = this.controller.restrictSelectedNodesByCurrentSettings(Gemma.CytoscapePanelUtil.getGeneIdArrayFromCytoscapeJSONNodeObjects(this.visualization.selected("nodes")));
            this.controller.extendNodes(selectedNodes);
        }
    },

    reRunSearchWithSelectedNodesHandler: function () {
        if (this.ready) {
        	var selectedNodes = this.controller.restrictSelectedNodesByCurrentSettings(Gemma.CytoscapePanelUtil.getGeneIdArrayFromCytoscapeJSONNodeObjects(this.visualization.selected("nodes")));
            this.controller.searchWithSelectedNodes(selectedNodes);
        }
    },    

    exportPNG: function () {
        var htmlString = '<img src="data:image/png;base64,' + this.visualization.png() + '"/>';

        var win = new Ext.Window({
            title: Gemma.HelpText.WidgetDefaults.CytoscapePanel.exportPNGWindowTitle,
            plain: true,
            html: htmlString,
            height: 700,
            width: 900,
            autoScroll: true
        });
        win.show();
    },

    exportGraphML: function () {
        var xmlString = this.visualization.graphml();

        var win = new Gemma.CytoscapeDownloadWindow({
            title: Gemma.HelpText.WidgetDefaults.CytoscapePanel.exportGraphMLWindowTitle
        });

        win.displayXML(xmlString);
    },

    exportXGMML: function () {
        var xmlString = this.visualization.xgmml();

        var win = new Gemma.CytoscapeDownloadWindow({
            title: Gemma.HelpText.WidgetDefaults.CytoscapePanel.exportXGMMLWindowTitle
        });

        win.displayXML(xmlString);
    },

    exportSIF: function () {
        var xmlString = this.visualization.sif();

        var win = new Gemma.CytoscapeDownloadWindow({
            title: Gemma.HelpText.WidgetDefaults.CytoscapePanel.exportSIFWindowTitle
        });

        win.displayXML(xmlString);
    },

    exportSVG: function () {
        var xmlString = this.visualization.svg();

        var win = new Gemma.CytoscapeDownloadWindow({
            title: Gemma.HelpText.WidgetDefaults.CytoscapePanel.exportSVGWindowTitle
        });

        win.displayXML(xmlString);
    },
    
    filterQueryGenesOnly: function(){
    	if (this.ready){    		
    		this.controller.filterQueryGenesOnly();    		
    	}
    },

    nodeDegreeEmphasis: function (isNodeDegreeEmphasis) {
        if (this.ready) {
            if (!isNodeDegreeEmphasis) {
                this.visualization.visualStyle(this.visualStyleRegular);
                this.nodeDegreeVisualStyleFlag=false;
            } else {
                this.visualization.visualStyle(this.visualStyleNodeDegree);
                this.nodeDegreeVisualStyleFlag=true;
            }
        }
    },

    refreshLayout: function () {
        if (this.ready) {
            this.visualization.layout(this.currentLayout);
        }
    },

    compressGraph: function (isLayoutCompressed) {
        if (this.ready) {
            this.currentLayout = isLayoutCompressed ? this.forceDirectedLayoutCompressed : this.defaultForceDirectedLayout;
            this.visualization.layout(this.currentLayout);
        }
    },

    toggleNodeLabels: function (isNodeLabelsVisible) {
        if (this.ready) {
            this.visualization.nodeLabelsVisible(isNodeLabelsVisible);
        }
    },

    updateStringency: function (stringency) {
        if (stringency) {
            this.currentDisplayStringency = stringency;
            this.controlBar.setStringency(stringency);
        } else {
            this.controlBar.setStringency(this.currentDisplayStringency);
        }

    },

    getStringency: function () {
        return this.currentDisplayStringency;
    },

    //called by the controlBar when the stringency spinner is used
    stringencyChange: function (stringency) {

        if (this.ready) {
        	//this.clearSearchBox();
            this.controller.stringencyChange(stringency);
        }
    },
    
    isQueryGenesOnly : function (){
    	return this.controlBar.getComponent('queryGenesOnly').getValue();
    },
    
    setQueryGenesOnly : function (checked){
    	this.controlBar.getComponent('queryGenesOnly').setValue(checked);
    },
    
    disableQueryGenesOnly : function (disabled){
    	this.controlBar.getComponent('queryGenesOnly').setDisabled(disabled);
    },
    
    textBoxMatchHandler: function(text){
    	this.controlBar.searchInCytoscapeBox.setValue(text);
    	this.selectSearchMatches(text);
    },
    
    selectSearchMatchesFromControlBar: function(text){
    	this.fireEvent('searchTextBoxMatch', text);
    	this.selectSearchMatches(text);
    },
    
    applyGeneListOverlay:function(text){
    	var nodeIdsToOverlay = this.controller.getMatchingGeneIdsByText(text);
    	
    	var bypass = { nodes: { }, edges: { } };
    	
    	var nodes = this.visualization.nodes();
    	
    	for (var i=0; i< nodes.length;i++){
    		
    		if (nodeIdsToOverlay.indexOf(nodes[i].data.id)!== -1){
    			bypass["nodes"][nodes[i].data.id]= Gemma.CytoscapeSettings.secondGeneListBypassOverlay;
    		}
    		
    	}
    	
    	this.visualization.visualStyleBypass(bypass);
    	
    },
    
    selectSearchMatches : function(text){   	
    	
    	this.deselect();
    	
    	if (text.length < 2) {    					
			return;
		}
    	
    	//call controller that tests coexSearchData
    	var nodeIdsToSelect = this.controller.getMatchingGeneIdsByText(text);
    	
    	if (nodeIdsToSelect.length>0){    		
    		this.select(nodeIdsToSelect);    		
    	}
    	
    	//highlight nodes
    	
    },
    
    clearSearchBox : function(){
    	this.controlBar.searchInCytoscapeBox.setValue('');
    	
    }

});