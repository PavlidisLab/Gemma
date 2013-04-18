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

/**
 * Wrapper around cytoscape flash component.
 * Provides abstraction:
 *
 * - node degree emphasis on/off
 * - compress/uncompress graph, change layout
 * - filter, selection, overlay
 * - data export in various formats
 *
 * Dependencies: (ideally) work-in-progress
 * - displaySettings
 * - graphData
 *
 * @class
 */
Gemma.CytoscapeDisplay = Ext.extend( Ext.FlashComponent, {
    graphLayouts: {
        'regular': Gemma.CytoscapeSettings.defaultForceDirectedLayout,
        'compressed': Gemma.CytoscapeSettings.forceDirectedLayoutCompressed
    },

    visualStyles: {
        'regular': Gemma.CytoscapeSettings.visualStyleRegular,
        'nodeDegreeEmphasis': Gemma.CytoscapeSettings.visualStyleNodeDegree
    },

    initComponent: function () {
        this.ready = false;

        this.currentLayout = this.graphLayouts['regular'];
        this.currentStyle = this.visualStyles['nodeDegreeEmphasis'];
        this.currentStyleBypass = {nodes: {}, edges: {}};

        this.visualization = new org.cytoscapeweb.Visualization( this.id, {
            swfPath: "/Gemma/scripts/cytoscape/swf/CytoscapeWeb",
            flashInstallerPath: "/Gemma/scripts/cytoscape/swf/playerProductInstall"
        });

        var display = this;
        var cytoscape = this.visualization;

        cytoscape.edgeOpacityMapper = function (data) {
            if (data.nodeDegree === null) {
                return 0.05;
            }
            return 1.05 - data.nodeDegree;
        };

        // Register listeners for interacting with the cytoscape visualization.
        cytoscape.addListener('select', 'nodes', function () {
            if ( cytoscape.selected("nodes").length > 0 ) {
                display.fireEvent('selection_available');
            }
        });

        cytoscape.addListener('deselect', 'nodes', function () {
            if ( cytoscape.selected("nodes").length === 0 ) {
                display.fireEvent('selection_unavailable');
            }
        });

        cytoscape.addListener( 'layout', function () {
            display.scaleFont();
        });

        cytoscape.addListener('zoom', function () {
            display.scaleFont();
        });

        // This is called every time that the graph is re-drawn(after visualization.draw is called, e.g. after a search).
        cytoscape.ready( function () {
            display.ready = true;
            display.applyFilters();
            display.applySelection();
            display.applyGeneListOverlay();
            display.zoomToFit();
        });

        Gemma.CytoscapeDisplay.superclass.initComponent.apply(this, arguments);
        this.addEvents('selection_available', 'selection_unavailable');

        display.observableDisplaySettings.on('stringency_change', function() {
            display.applyFilters();
        });

        display.observableDisplaySettings.on('query_genes_only_change', function() {
            display.applyFilters();
        });

        display.observableDisplaySettings.on('gene_overlay', function() {
            display.applyGeneListOverlay();
        });

        display.observableDisplaySettings.on('search_text_change', function( text ) {
            display.selectNodesMatchingText(text);
        });
    },

    /**
     * @public
     * @param searchResults
     */
    drawGraph: function (searchResults) {
        this.ready = false;
        var graphData = Gemma.CytoscapeGraphDataBuilder.constructGraphData(
            searchResults.getQueryGeneIds(),
            searchResults.getCytoscapeCoexpressionPairs());

        this.visualization.draw({
            network: graphData,
            nodeTooltipsEnabled: true,
            edgeTooltipsEnabled: true,
            visualStyle: this.currentStyle,
            layout: this.currentLayout
        });
    },

    getSelectedGeneIds: function () {
        if (!this.ready) {return;}
        var nodes = this.visualization.selected("nodes");
        var geneIds = [];
        for (var i = 0; i < nodes.length; i++) {
            geneIds.push(nodes[i].data.geneid);
        }
        return geneIds;
    },

    getVisibleSelectedGeneIds: function () {
        if (!this.ready) {return;}
        var nodes = this.getVisibleNodes(this.visualization.selected("nodes"));
        var geneIds = [];
        for (var i = 0; i < nodes.length; i++) {
            geneIds.push(nodes[i].data.geneid);
        }
        return geneIds;
    },

    /**
     * @public
     * @param {boolean} emphasis
     */
    toggleNodeDegreeEmphasis: function (emphasis) {
        if (!this.ready) {return;}
        this.currentStyle = emphasis ? this.visualStyles['nodeDegreeEmphasis'] : this.visualStyles['regular'];
        this.visualization.visualStyle(this.currentStyle);
        this.applyGeneListOverlay();
    },

    /**
     * @public
     */
    refreshLayout: function () {
        if (!this.ready) {return;}
        this.visualization.layout( this.currentLayout );
    },

    /**
     * @public
     * @param {boolean} compress
     */
    compressGraph: function (compress) {
        if (!this.ready) {return;}
        this.currentLayout = compress ? this.graphLayouts['compressed'] : this.graphLayouts['regular'];
        this.visualization.layout(this.currentLayout);
    },

    /**
     * @public
     * @param {boolean} visible
     */
    toggleNodeLabels: function (visible) {
        if (!this.ready) {return;}
        this.visualization.nodeLabelsVisible( visible );
    },

    /**
     * @private
     */
    zoomToFit : function () {
        this.visualization.zoomToFit();
    },

    /**
     * @private
     * @param nodeIds
     */
    selectNodes: function (nodeIds) {
        this.visualization.select("nodes", nodeIds);
    },

    /**
     * @private
     */
    deselectNodesAndEdges: function () {
        this.visualization.deselect("nodes");
        this.visualization.deselect("edges");
    },

    /**
     * @private
     * @param nodes
     * @returns {Array}
     */
    getVisibleNodes: function (nodes) {
        var visibleNodes = [];
        for (var i = 0; i < nodes.length; i++) {
            if (nodes[i].visible) {
                visibleNodes.push(nodes[i]);
            }
        }
        return visibleNodes;
    },

    /**
     * @private
     */
    scaleFont: function () {
        if (!this.ready) {return;}

        var zoom = this.visualization.zoom();
        // Figure out what fontSize to pass to get desired font size on the screen.
        var newFontSize = Math.floor(Gemma.CytoscapeSettings.labelFontSize / zoom) + 1;

        this.visualStyles['nodeDegreeEmphasis'].nodes.labelFontSize = newFontSize;
        this.visualStyles['regular'].nodes.labelFontSize = newFontSize;

        this.visualization.visualStyle( this.currentStyle );
    },

    /**
     * @public
     */
    applyGeneListOverlay: function () {
        if (!this.ready) {return;}

        var overlayIds = this.observableDisplaySettings.getOverlayGeneIds();
        this.currentStyleBypass = { nodes: { }, edges: { } };

        // You cannot set mappers in visualStyleBypasses to my great dismay, looks like we will have to do some hackery.
        function pickBypass(nodeDegreeBin) {
            switch (nodeDegreeBin) {
                case Gemma.CytoscapeSettings.nodeDegreeColor.lightest.name:
                    return Gemma.CytoscapeSettings.secondGeneListBypassOverlayNodeDegreeLightest;
                case Gemma.CytoscapeSettings.nodeDegreeColor.light.name:
                    return Gemma.CytoscapeSettings.secondGeneListBypassOverlayNodeDegreeLight;
                case Gemma.CytoscapeSettings.nodeDegreeColor.moderate.name:
                    return Gemma.CytoscapeSettings.secondGeneListBypassOverlayNodeDegreeModerate;
                case Gemma.CytoscapeSettings.nodeDegreeColor.dark.name:
                    return Gemma.CytoscapeSettings.secondGeneListBypassOverlayNodeDegreeDark;
                case Gemma.CytoscapeSettings.nodeDegreeColor.darkest.name:
                    return Gemma.CytoscapeSettings.secondGeneListBypassOverlayNodeDegreeDarkest;
            }
        }

        for (var i = 0; i < overlayIds.length; i++) {
            if (this.currentStyle === this.visualStyles['regular']) {
                this.currentStyleBypass.nodes[overlayIds[i]] = Gemma.CytoscapeSettings.secondGeneListBypassOverlay;
            } else if (this.currentStyle === this.visualStyles['nodeDegreeEmphasis']) {
                var node = this.visualization.node( overlayIds[i] );
                this.currentStyleBypass.nodes[overlayIds[i]] = pickBypass( node.data.nodeDegreeBin );
            }
        }
        this.visualization.visualStyleBypass( this.currentStyleBypass );
    },

    /**
     * @public
     * @param nodeIds
     * @returns {{total: number, hidden: number}}
     */
    getNodeOverlap: function (nodeIds) {
        var overlap = {
            total: 0,
            hidden: 0
        };
        for (var i = 0; i < nodeIds.length; i++) {
            var node = this.visualization.node( nodeIds[i] );
            if (node !== null) {
                overlap.total += 1;
                if (!node.visible) {
                    overlap.hidden += 1;
                }
            }
        }
        return overlap;
    },

    /**
     * @private
     * @param text
     */
    selectNodesMatchingText: function (text) {
        this.deselectNodesAndEdges();
        if (text.length < 2) {
            return;
        }
        var nodeIdsToSelect = this.observableSearchResults.getCytoscapeGeneSymbolsMatchingQuery( text ); // TODO: remove depenency on searchResults
        this.selectNodes(nodeIdsToSelect);
    },

    /**
     * @private
     * @param stringency
     */
    filterEdgesNotMeetingStringency: function (stringency) {
        var filterEdgesFunction = function (edge) {
            return edge.data.support >= stringency;
        };
        this.visualization.filter( "edges", filterEdgesFunction );
    },

    /**
     * @private
     */
    applySelection: function () {
        if (!this.ready) {return;}

        this.selectNodesMatchingText( this.observableDisplaySettings.getSearchTextValue() );
    },

    /**
     * @private
     */
    applyFilters: function () {
        if (!this.ready) {return;}

        var stringency = this.observableDisplaySettings.getStringency();
        var queryGenesOnly = this.observableDisplaySettings.getQueryGenesOnly();

        var nodesToKeep;
        if ( queryGenesOnly ) {
            nodesToKeep = this.observableSearchResults.getQueryGeneIds();
        } else {
            // We need to filter this graph to the display stringency if necessary)
            // show only query genes and their neighbours, edges must meet stringency
             var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResultsWithQueryGenes(
                this.observableSearchResults.getCoexpressionPairs(),
                this.observableSearchResults.getQueryGeneIds(),
                stringency );
            nodesToKeep = trimmed.geneIds;
        }

        this.filterNodesNotInSet( nodesToKeep );
        this.filterEdgesNotMeetingStringency( stringency );
    },

    /**
     * @private
     * @param nodeIdsToShow
     */
    filterNodesNotInSet: function (nodeIdsToShow) {
        var filterNodesFunction = function (node) {
            return nodeIdsToShow.indexOf(node.data.geneid) !== -1;
        };
        this.visualization.filter( "nodes", filterNodesFunction );
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
    }
});