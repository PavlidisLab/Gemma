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
            type: 'number'
        }, {
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
            name: 'nodeDegreeBin',
            type: 'number'
        }]

    },
    options: {
        // where you have the Cytoscape Web SWF
        swfPath: "/Gemma/scripts/cytoscapev8/swf/CytoscapeWeb",
        // where you have the Flash installer SWF
        flashInstallerPath: "/Gemma/scripts/cytoscapev8/swf/playerProductInstall"
    },


    initComponent: function () {

        this.visualization = new org.cytoscapeweb.Visualization(this.id, this.options);

        this.currentLayout = this.defaultForceDirectedLayout;

        this.visualization["edgeOpacityMapper"] = function (data) {
            if (data["nodeDegreeBin"] == null) {
                return 0.05;
            }
            return 1.05 - data["nodeDegreeBin"] / 10;
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


            this.visualization.addListener("filter", function (evt) {

                if (this.doZoomAfterFilter) {
                    this.visualization.zoomToFit();
                }
                this.doZoomAfterFilter = false;


            }.createDelegate(this));
            
            this.visualization.addListener("layout", function (evt) {
            	this.initialZoomLevel = null
            	this.visualization.zoomToFit();
            }.createDelegate(this));


            this.visualization.addListener("zoom", function (evt) {

                if (this.ready) {
                    var zoom = evt.value;

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

            }.createDelegate(this));

            this.ready = true;

            this.fireEvent('doneDrawingCytoscape');

        }.createDelegate(this));
        // end vis.ready()
        Gemma.CytoscapeDisplay.superclass.initComponent.apply(this, arguments);

        this.addEvents('doneDrawingCytoscape');

    },

    drawGraph: function (dataJSON) {
        var dataMsg = {
            dataSchema: this.dataSchemaJSON,
            data: dataJSON
        };

        // init and draw
        this.visualization.draw({
            network: dataMsg,
            visualStyle: this.nodeDegreeVisualStyleFlag ? this.visualStyleNodeDegree : this.visualStyleRegular,
            layout: this.currentLayout
        });
    },

    filter: function (stringency, trimmedNodeIds, doZoom) {

        filterFunctionNodes = function (node) {
            return trimmedNodeIds.indexOf(node.data.geneid) !== -1;
        };

        this.visualization.filter("nodes", filterFunctionNodes.createDelegate(this));

        filterFunctionEdges = function (edge) {
            return edge.data.support >= stringency;
        };

        if (doZoom) {
            this.doZoomAfterFilter = true;
        }

        this.visualization.filter("edges", filterFunctionEdges.createDelegate(this));
    },

    extendSelectedNodesHandler: function () {
        if (this.ready) {
            this.controller.extendNodes(this.visualization.selected("nodes"));
        }
    },

    reRunSearchWithSelectedNodesHandler: function () {
        if (this.ready) {
            this.controller.reRunSearchWithSelectedNodes(this.visualization.selected("nodes"));
        }
    },

    changeFontSize: function (fontSize) {
        this.visualStyleRegular.nodes.labelFontSize = fontSize;
        this.visualStyleNodeDegree.nodes.labelFontSize = fontSize;

        if (this.nodeDegreeVisualStyleFlag) {
            this.visualization.visualStyle(this.visualStyleNodeDegree);
        } else {
            this.visualization.visualStyle(this.visualStyleRegular);
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

    nodeDegreeEmphasis: function (isNodeDegreeEmphasis) {
        if (this.ready) {
            if (!isNodeDegreeEmphasis) {
                this.visualization.visualStyle(this.visualStyleRegular);
            } else {
                this.visualization.visualStyle(this.visualStyleNodeDegree);
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
            this.controlBar.setStringency(this.currentDisplayStringency)
        }

    },

    getStringency: function () {
        return this.currentDisplayStringency;
    },

    //called by the controlBar when the stringency spinner is used
    stringencyChange: function (stringency) {

        if (this.ready) {
            this.controller.stringencyChange(stringency);
        }
    }

});