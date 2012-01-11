Ext.namespace('Gemma');

Gemma.CytoscapePanel = Ext.extend(
Ext.Panel, {

    title: 'Cytoscape',

    layout: 'fit',

    //the stringency that the results were retrieved at
    currentResultsStringency: 2,
    //the stringency that the results were initially displayed at
    initialDisplayStringency: 2,
    //need to remember the previous value of the spinner when the user cancels a typed-in stringency change
    lastSpinnerValue: 2,

    currentNodeGeneIds: [],
    currentQueryGeneIds: [],
    // used to pass a subset of results that meet the stringency
    // threshold to the coexpressionGrid widget displaying the
    // results in a table
    trimmedKnownGeneResults: [],
    // used to apply a filter to the graph to remove nodes when
    // stringency changes
    trimmedNodeIds: [],
    //this flag is used to let the cytoscape panel know when the flash component has finished drawing(when vis.ready() has completed)
    //this is necessary because if you interact with the visualization before it is finished drawing, errors occur
    ready: false,

    dataJSON: {
        nodes: [],
        edges: []
    },
    //used by cytoscape web to define properties of nodes and edges
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

    nodeDegreeVisualStyleFlag: true,
    visualization: {},

    forceDirectedLayoutCompressed: Gemma.CytoscapeSettings.forceDirectedLayoutCompressed,
    defaultForceDirectedLayout: Gemma.CytoscapeSettings.defaultForceDirectedLayout,

    currentLayout: {},

    visualStyleRegular: Gemma.CytoscapeSettings.visualStyleRegular,
    visualStyleNodeDegree: Gemma.CytoscapeSettings.visualStyleNodeDegree,

    initComponent: function () {
        this.currentLayout = this.defaultForceDirectedLayout;

        this.visualOptionsMenu = new Ext.menu.Menu({
            items: [{
                itemId: 'refreshLayoutButton',
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.refreshLayoutText,
                tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.refreshLayoutTT,
                handler: this.refreshLayout,
                scope: this
            }, {
                itemId: 'compressGraphButton',
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.compressGraphText,
                tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.compressGraphTT,
                handler: this.compressGraph,
                scope: this
            }, {
                itemId: 'nodeLabelsButton',
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.noNodeLabelsText,
                tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeLabelsTT,
                handler: this.toggleNodeLabels,
                scope: this
            }]
        });

        this.actionsMenu = new Ext.menu.Menu({
            items: [{
                itemId: 'extendSelectedNodesButton',
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.extendNodeText,
                tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.extendNodeTT,
                handler: this.extendSelectedNodesHandler,
                scope: this
            }, {
                itemId: 'searchWithSelectedNodesButton',
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchWithSelectedText,                
                tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchWithSelectedTT,
                handler: this.reRunSearchWithSelectedNodes,
                scope: this
            }]
        });

        var actionsButtonHandler = function () {
                if (this.ready) {
                    var nodes = this.visualization.selected("nodes");
                    if (nodes.length > 0) {
                        this.actionsMenu.getComponent('extendSelectedNodesButton').setDisabled(false);
                        this.actionsMenu.getComponent('searchWithSelectedNodesButton').setDisabled(false);
                    } else {
                        this.actionsMenu.getComponent('extendSelectedNodesButton').setDisabled(true);
                        this.actionsMenu.getComponent('searchWithSelectedNodesButton').setDisabled(true);
                    }
                }
            };

        this.actionsButton = new Ext.Button({
            text: '<b>Actions</b>',
            itemId: 'actionsButton',
            menu: this.actionsMenu
        });

        this.actionsButton.addListener('mouseover', actionsButtonHandler, this);

        Ext.apply(
        this, {
            tbar: [{
                xtype: 'tbtext',
                text: 'Stringency:'
            }, {
                xtype: 'tbspacer'
            }, {
                xtype: 'spinnerfield',
                ref: 'stringencySpinner',
                itemId: 'stringencySpinner',
                decimalPrecision: 1,
                incrementValue: 1,
                accelerate: false,
                allowBlank: false,
                allowDecimals: false,
                allowNegative: false,
                minValue: Gemma.MIN_STRINGENCY,
                maxValue: 999,
                fieldLabel: 'Stringency ',
                value: this.coexCommand.initialDisplayStringency,
                width: 60,
                enableKeyEvents: true

            }, {
                xtype: 'label',
                html: '<img ext:qtip="' + Gemma.HelpText.WidgetDefaults.CytoscapePanel.stringencySpinnerTT + '" src="/Gemma/images/icons/question_blue.png"/>',
                height: 15
            }, '->', '-',
            {
                xtype: 'button',
                text: '<b>Help</b>',
                tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.widgetHelpTT,
                handler: function () {
                    var htmlString = '<img src="/Gemma/images/cytoscapehelp.png"/>';
                    var win = new Ext.Window({
                        title: 'Help',
                        height: 720,
                        plain: true,
                        html: htmlString,
                        autoScroll: true
                    });
                    win.show();
                },
                scope: this
            }, '->', '-',
            {
                xtype: 'button',
                text: '<b>Save As</b>',
                menu: new Ext.menu.Menu({
                    items: [{
                        text: 'Save as PNG',
                        tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.saveAsImageTT,
                        handler: this.exportPNG,
                        scope: this
                    }, {
                        text: 'Save as GraphML',
                        tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.saveAsGraphMLTT,
                        handler: this.exportGraphML,
                        scope: this
                    }, {
                        text: 'Save as XGMML',
                        tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.saveAsXGMMLTT,
                        handler: this.exportXGMML,
                        scope: this
                    }, {
                        text: 'Save as SIF',
                        tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.saveAsSIFTT,
                        handler: this.exportSIF,
                        scope: this
                    }, {
                        text: 'Save as SVG',
                        tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.saveAsSVGTT,
                        handler: this.exportSVG,
                        scope: this
                    }]
                })
            }, '->', '-',
            {
                xtype: 'button',
                text: '<b>Visual Options</b>',
                menu: this.visualOptionsMenu
            }, '->', '-', this.actionsButton, '->', '-',
            {
                xtype: 'button',
                itemId: 'nodeDegreeEmphasis',
                ref: 'nodeDegreeEmphasis',
                text: '<b>' + Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeDegreeEmphasisText + '</b>',
                enableToggle: 'true',
                pressed: 'true',
                handler: this.nodeDegreeEmphasis,
                scope: this
            }, {
                xtype: 'label',
                html: '<img ext:qtip="' + Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeDegreeEmphasisTT + '" src="/Gemma/images/icons/question_blue.png"/>',
                height: 15
            }],
            // end tbar
            bbar: [{
                xtype: 'tbtext',
                text: '',
                itemId: 'bbarStatus'
            }, '->',
            {
                xtype: 'button',
                icon: "/Gemma/images/icons/cross.png",
                itemId: 'bbarClearButton',
                handler: function () {
                    this.currentbbarText = "";
                    this.getBottomToolbar().hide();
                    this.doLayout();
                },
                scope: this
            }],
            margins: {
                top: 0,
                right: 0,
                bottom: 0,
                left: 0
            },
            items: [{
                xtype: 'flash',
                id: 'cytoscapeweb',
                listeners: {
                    afterrender: {
                        scope: this,
                        fn: this.cytoscapePanelAfterRenderHandler
                    }
                }
            }]
        });

        var vis = new org.cytoscapeweb.Visualization("cytoscapeweb", this.options);

        vis["edgeOpacityMapper"] = function (data) {
            if (data["nodeDegreeBin"] == null) {
                return 0.05;
            }
            return 1.05 - data["nodeDegreeBin"] / 10;
        };

        vis.panelRef = this;
        //this is called every time that the graph is re-drawn(after search, after refresh layout, after compress graph).
        //See CytoscapeWeb documentation for why this is called every redraw
        vis.ready(function () {
            vis.nodeTooltipsEnabled(true);
            vis.edgeTooltipsEnabled(true);

            vis.addListener("zoom", function (evt) {

                if (vis.panelRef.ready) {
                    var zoom = evt.value;

                    if (vis.panelRef.lastZoomLevel) {
                        //start changing font size            			
                        var newFontSize;
                        if (zoom < vis.panelRef.lastZoomLevel) {
                            //if the zoom is less than the biggest font threshold AND the last zoom level used to be bigger than the last zoom level
                            if (zoom < Gemma.CytoscapeSettings.zoomLevelBiggestFont && vis.panelRef.lastZoomLevel > Gemma.CytoscapeSettings.zoomLevelBiggestFont) {
                                newFontSize = Gemma.CytoscapeSettings.labelFontSizeBiggest;
                            } else if (zoom < Gemma.CytoscapeSettings.zoomLevelBiggerFont && vis.panelRef.lastZoomLevel > Gemma.CytoscapeSettings.zoomLevelBiggerFont) {
                                newFontSize = Gemma.CytoscapeSettings.labelFontSizeBigger;
                            }
                        } else if (zoom > vis.panelRef.lastZoomLevel) {

                            if (zoom > Gemma.CytoscapeSettings.zoomLevelBiggerFont && vis.panelRef.lastZoomLevel < Gemma.CytoscapeSettings.zoomLevelBiggerFont) {
                                newFontSize = Gemma.CytoscapeSettings.labelFontSize;
                            } else if (zoom > Gemma.CytoscapeSettings.zoomLevelBiggestFont && vis.panelRef.lastZoomLevel < Gemma.CytoscapeSettings.zoomLevelBiggestFont) {
                                newFontSize = Gemma.CytoscapeSettings.labelFontSizeBigger;
                            }
                        }
                        if (newFontSize) {
                            vis.panelRef.visualStyleRegular.nodes.labelFontSize = newFontSize;
                            vis.panelRef.visualStyleNodeDegree.nodes.labelFontSize = newFontSize;

                            if (vis.panelRef.nodeDegreeVisualStyleFlag) {
                                vis.visualStyle(vis.panelRef.visualStyleNodeDegree);
                            } else {
                                vis.visualStyle(vis.panelRef.visualStyleRegular);
                            }
                        }
                        //end changing font size            			
                        vis.panelRef.lastZoomLevel = zoom;
                    } else {
                        //first time in event handler            			
                        vis.panelRef.lastZoomLevel = zoom;
                        var newFontSize;
                        if (zoom < Gemma.CytoscapeSettings.zoomLevelBiggestFont) {
                            //biggest
                            newFontSize = Gemma.CytoscapeSettings.labelFontSizeBiggest;
                        } else if (zoom < Gemma.CytoscapeSettings.zoomLevelBiggerFont) {
                            //bigger
                            newFontSize = Gemma.CytoscapeSettings.labelFontSizeBigger;
                        } else {
                            //normal
                            newFontSize = Gemma.CytoscapeSettings.labelFontSize;
                        }

                        if (newFontSize) {
                            vis.panelRef.visualStyleRegular.nodes.labelFontSize = newFontSize;
                            vis.panelRef.visualStyleNodeDegree.nodes.labelFontSize = newFontSize;

                            if (vis.panelRef.nodeDegreeVisualStyleFlag) {
                                vis.visualStyle(vis.panelRef.visualStyleNodeDegree);
                            } else {
                                vis.visualStyle(vis.panelRef.visualStyleRegular);
                            }
                        }
                    } //end first time
                }
            });

            vis.panelRef.ready = true;

            // filter visable results based on
            // initialDisplayStringency
            if (vis.panelRef.initialDisplayStringency > vis.panelRef.currentResultsStringency) {
                var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResults(
                vis.panelRef.knownGeneResults, vis.panelRef.currentQueryGeneIds, vis.panelRef.initialDisplayStringency);
                vis.panelRef.stringencyUpdate(
                vis.panelRef.initialDisplayStringency, trimmed.trimmedKnownGeneResults, trimmed.trimmedNodeIds);
            }
        });
        // end vis.ready()
        Ext.apply(this, {
            visualization: vis
        });

        Gemma.CytoscapePanel.superclass.initComponent.apply(
        this, arguments);

        this.getTopToolbar().getComponent('stringencySpinner').addListener('specialkey', function (field, e) {

            if (this.ready && e.getKey() == e.ENTER) {
                var spinner = this.getTopToolbar().getComponent('stringencySpinner');

                if (spinner.getValue() < 2) {
                    spinner.setValue(2);
                }

                if (spinner.getValue() >= this.currentResultsStringency) {
                    this.stringencyChangeHandler(spinner);
                } else {
                    //new search                	
                    this.newSearchForLowerStringencyHandler(spinner, false);
                }
            }

        }, this);

        this.getTopToolbar().getComponent('stringencySpinner').addListener('spin', function (ev) {

            if (this.ready) {

                var spinner = this.getTopToolbar().getComponent('stringencySpinner');

                if (spinner.getValue() >= this.currentResultsStringency) {
                    this.stringencyChangeHandler(spinner);
                } else {
                    //new search            	            	
                    this.newSearchForLowerStringencyHandler(spinner, true);
                }
            }
        }, this);

    },
    //end initComponent
    stringencyChangeHandler: function (spinner) {
        this.lastSpinnerValue = spinner.getValue();

        var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResults(
        this.knownGeneResults, this.currentQueryGeneIds, this.getTopToolbar().getComponent('stringencySpinner').getValue());
        this.trimmedKnownGeneResults = trimmed.trimmedKnownGeneResults;
        this.trimmedNodeIds = trimmed.trimmedNodeIds;

        this.coexGridRef.cytoscapeUpdate(
        spinner.getValue(), this.queryGenes.length, this.trimmedKnownGeneResults);

        this.visualization.filter("nodes", function (
        node) {
            return this.trimmedNodeIds.indexOf(node.data.geneid) !== -1;

        }.createDelegate(this));

        this.visualization.filter("edges", function (
        edge) {

            return edge.data.support >= this.getTopToolbar().getComponent('stringencySpinner').getValue();

        }.createDelegate(this));
    },

    newSearchForLowerStringencyHandler: function (spinner, isSpinner) {

        Ext.Msg.show({
            title: 'New Search',
            msg: Gemma.HelpText.WidgetDefaults.CytoscapePanel.lowStringencyWarning,
            buttons: {
                ok: 'Proceed',
                cancel: 'Cancel'
            },
            fn: function (btn) {
                if (btn == 'ok') {
                    this.getBottomToolbar().hide();
                    this.doLayout();
                    this.coexGridRef.getBottomToolbar().hide();
                    this.coexGridRef.doLayout();

                    var displayStringency = spinner.getValue();                   
                    var resultsStringency = Gemma.CytoscapePanelUtil.restrictResultsStringency(displayStringency);

                    Ext.apply(
                    this.coexCommand, {
                        stringency: resultsStringency,
                        displayStringency: displayStringency,
                        geneIds: this.currentQueryGeneIds,
                        queryGenesOnly: false
                    });

                    this.loadMask.show();
                    ExtCoexpressionSearchController.doSearchQuick2(
                    this.coexCommand, {
                        callback: this.initialCoexSearchCallback.createDelegate(this)

                    });

                } else {
                    if (isSpinner) {
                        spinner.setValue(spinner.getValue() + 1);
                    } else {
                        spinner.setValue(this.lastSpinnerValue);
                    }
                }
            }.createDelegate(this)
        });

    },

    reRunSearchWithSelectedNodes: function () {

        if (this.ready) {
            this.clearError();

            var selectedNodes = this.visualization.selected("nodes");

            if (selectedNodes.length > 0 && selectedNodes.length <= Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {
                this.getBottomToolbar().setVisible(false);
                this.doLayout();
                this.currentbbarText = '';

                var displayStringency = this.getTopToolbar().getComponent('stringencySpinner').getValue();
                var resultsStringency = Gemma.CytoscapePanelUtil.restrictResultsStringency(displayStringency);

                var selectedNodesGeneIdArray = [];
                var sNodesLength = selectedNodes.length;
                var i;
                for (i = 0; i < sNodesLength; i++) {
                    selectedNodesGeneIdArray[i] = selectedNodes[i].data.geneid;
                }

                this.currentQueryGeneIds = selectedNodesGeneIdArray;
                this.updateSearchFormGenes(selectedNodesGeneIdArray);

                Ext.apply(this.coexCommand, {
                    stringency: resultsStringency,
                    displayStringency: displayStringency,
                    geneIds: selectedNodesGeneIdArray,
                    queryGenesOnly: false
                });

                this.loadMask.show();
                ExtCoexpressionSearchController.doSearchQuick2(
                this.coexCommand, {
                    callback: this.initialCoexSearchCallback.createDelegate(this)
                });

            } else if (selectedNodes.length > Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {
                Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, String.format(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooMany, Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY));
            } else if (selectedNodes.length == 0) {
                Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooFew);
            }
        }
    },

    extendSelectedNodesHandler: function () {
        if (this.ready) {
            this.clearError();

            var selectedNodes = this.visualization.selected("nodes");

            if (selectedNodes.length > 0 && selectedNodes.length <= Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {
                this.getBottomToolbar().setVisible(false);
                this.doLayout();
                this.currentbbarText = '';

                if (this.currentQueryGeneIds.length + selectedNodes.length <= Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {                	
                	this.extendSelectedNodesStepTwo(selectedNodes);
                	
                } else {

                    Ext.Msg.confirm(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, String.format(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooManyReduce, Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY), function (btn) {

                        if (btn == 'yes') {   
                        	// make room in currentQueryGeneIds for new genes
                        	this.currentQueryGeneIds = this.currentQueryGeneIds.splice(this.currentQueryGeneIds.length - (Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY - selectedNodes.length));
                        	this.extendSelectedNodesStepTwo(selectedNodes);
                        
                        }
                    }, this);
                }

            } else if (selectedNodes.length > Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {
                Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, String.format(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooMany, Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY));
            } else {
                Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooFew);
            }

        }

    },
    
    extendSelectedNodesStepTwo : function (selectedNodes){
    	
    	var displayStringency = this.getTopToolbar().getComponent('stringencySpinner').getValue();
        var resultsStringency = Gemma.CytoscapePanelUtil.restrictResultsStringency(displayStringency);                   

        var extendedNodesGeneIdArray = [];
        var sNodesLength = selectedNodes.length;
        var i;
        for (i = 0; i < sNodesLength; i++) {
            extendedNodesGeneIdArray[i] = selectedNodes[i].data.geneid;

            if (this.currentQueryGeneIds.indexOf(selectedNodes[i].data.geneid) === -1) {
                this.currentQueryGeneIds.push(selectedNodes[i].data.geneid);
            }
        }

        this.updateSearchFormGenes(this.currentQueryGeneIds);

        Ext.apply(this.coexCommand, {
            stringency: resultsStringency,
            displayStringency: displayStringency,
            geneIds: extendedNodesGeneIdArray,
            queryGenesOnly: false
        });

        this.loadMask.show();
        ExtCoexpressionSearchController.doSearchQuick2(
        this.coexCommand, {
            callback: this.extendThisNodeInitialCoexSearchCallback.createDelegate(this)

        });
    	
    },

    //This is called the first time the cytoscape panel is rendered.  
    //It is a special case because it uses the partial results from the coexpression grid to do the complete search
    //and needs to set up values of the top and bottom toolbar
    cytoscapePanelAfterRenderHandler: function () {
        if (this.coexGridRef) {
            if (!this.loadMask) {
                this.loadMask = new Ext.LoadMask(
                this.getEl(), {
                    msg: Gemma.StatusText.Searching.analysisResults,
                    msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                });
            }
            
            this.loadMask.show();
            this.currentNodeGeneIds = [];
            this.currentQueryGeneIds = [];

            var results = {};
            
            results.queryGenes = this.queryGenes;
            results.knownGeneResults = this.knownGeneResults;

            var spinner = this.getTopToolbar().getComponent('stringencySpinner');

            if (this.coexCommand.displayStringency > Gemma.MIN_STRINGENCY) {
                spinner.setValue(this.coexCommand.displayStringency);
                var bbarText = this.getBottomToolbar().getComponent('bbarStatus');
                this.currentbbarText = "Display Stringency set to " + this.coexCommand.displayStringency + " based on number of experiments chosen.";
                bbarText.setText(this.currentbbarText);
            } else {
                spinner.setValue(Gemma.MIN_STRINGENCY);
                this.getBottomToolbar().hide();
                this.doLayout();
            }

            this.initialCoexSearchCallback(results);
        }
    },

    initialCoexSearchCallback: function (result) {
        this.currentResultsStringency = this.coexCommand.stringency;
        this.initialDisplayStringency = this.coexCommand.displayStringency;
        this.currentNodeGeneIds = [];

        var qlength = result.queryGenes.length;
        // populate geneid array for complete graph
        var i;
        for (i = 0; i < qlength; i++) {
            this.currentNodeGeneIds.push(result.queryGenes[i].id);

            if (this.currentQueryGeneIds.indexOf(result.queryGenes[i].id) === -1) {
                this.currentQueryGeneIds.push(result.queryGenes[i].id);
            }
        }

        var kglength = result.knownGeneResults.length;

        for (i = 0; i < kglength; i++) {
            if (this.currentNodeGeneIds.indexOf(result.knownGeneResults[i].foundGene.id) === -1) {
                this.currentNodeGeneIds.push(result.knownGeneResults[i].foundGene.id);
            }
        }

        if (!this.coexCommand.queryGenesOnly && kglength > 0) {

            Ext.apply(this.coexCommand, {
                geneIds: this.currentNodeGeneIds,
                queryGenesOnly: true
            });

            ExtCoexpressionSearchController.doSearchQuick2Complete(
            this.coexCommand, this.currentQueryGeneIds, {
                callback: this.completeCoexSearchCallback.createDelegate(this)
            });
        } else {
            this.completeCoexSearchCallback(result);
        }

    },

    extendThisNodeInitialCoexSearchCallback: function (result) {
        this.currentResultsStringency = this.coexCommand.stringency;
        this.initialDisplayStringency = this.coexCommand.displayStringency;

        var qgenes = result.queryGenes;
        var knowngenes = result.knownGeneResults;
        var kglength = knowngenes.length;
        var completeSearchFlag = false;
        var i;
        for (i = 0; i < kglength; i++) {
            if (this.currentNodeGeneIds.indexOf(knowngenes[i].foundGene.id) === -1) {
                completeSearchFlag = true;
                this.currentNodeGeneIds.push(knowngenes[i].foundGene.id);
            }
        }

        if (!completeSearchFlag) {
            Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusNoMoreResults);

            this.loadMask.hide();
        } else {
            Ext.apply(this.coexCommand, {
                geneIds: this.currentNodeGeneIds,
                queryGenesOnly: true
            });

            ExtCoexpressionSearchController.doSearchQuick2Complete(
            this.coexCommand, this.currentQueryGeneIds, {
                callback: this.completeCoexSearchCallback.createDelegate(this)

            });

        }
    },

    completeCoexSearchCallback: function (result) {
        this.queryGenes = result.queryGenes;
        //get rid of duplicates
        this.knownGeneResults = Gemma.CoexValueObjectUtil.removeDuplicates(result.knownGeneResults);

        var spinner = this.getTopToolbar().getComponent('stringencySpinner');

        // update underlying data
        this.coexGridRef.knownGeneResults = this.knownGeneResults;
        this.coexGridRef.currentQueryGeneIds = this.currentQueryGeneIds;
        this.coexGridRef.currentResultsStringency = this.currentResultsStringency;

        // update the grid
        // filter visable results based on
        // initialDisplayStringency
        if (this.initialDisplayStringency > this.currentResultsStringency) {
            var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResults(this.knownGeneResults, this.currentQueryGeneIds, this.initialDisplayStringency);

            // update the grid with trimmed data
            // (underlying data in coexGridRef has
            // already been set)
            this.coexGridRef.cytoscapeUpdate(this.initialDisplayStringency, this.currentQueryGeneIds.length, trimmed.trimmedKnownGeneResults);

        } else {
            this.coexGridRef.cytoscapeUpdate(spinner.getValue(), this.queryGenes.length, this.knownGeneResults);
        }
        this.lastSpinnerValue = spinner.getValue();

        this.dataJSON = this.constructDataJSON(this.queryGenes, this.knownGeneResults);

        this.drawGraph();
        this.visualOptionsMenu.getComponent('nodeLabelsButton').setText(Gemma.HelpText.WidgetDefaults.CytoscapePanel.noNodeLabelsText);

        if (result.displayInfo) {
            var bbarText = this.getBottomToolbar().getComponent('bbarStatus');

            if (this.currentbbarText) {
                bbarText.setText(this.currentbbarText + ' ' + result.displayInfo);
            } else {
                bbarText.setText(result.displayInfo);
            }
            if (!this.getBottomToolbar().isVisible()) {
                this.getBottomToolbar().setVisible(true);
                this.doLayout();
            }
        }
        this.loadMask.hide();
    },

    constructDataJSON: function (qgenes, knowngenes) {
        return this.constructDataJSONWithStringencyFiltering(qgenes, knowngenes, false, null);
    },

    //This is only called from constructDataJSON(qgenes, knowngenes, false, null) above so filterCurrentResults=false and filterStringency=null
    //this means no filtering will be done.
    constructDataJSONWithStringencyFiltering: function (qgenes, knowngenes, filterCurrentResults, filterStringency) {
        var data = {
            nodes: [],
            edges: []
        }
        // helper array to prevent duplicate nodes from being
        // entered
        var graphNodeIds = [];
        var edgeSet = [];
        var kglength = knowngenes.length;
        // populate node data plus populate edge data
        for (i = 0; i < kglength; i++) {

            // if not filtering go in, or if filtering: go in
            // only if the query or known gene is contained in
            // the original query geneids AND the stringency is
            // >= the filter stringency
            if (!filterCurrentResults || ((this.currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) !== -1 || (this.currentQueryGeneIds.indexOf(knowngenes[i].queryGene.id) !== -1)) && (knowngenes[i].posSupp >= filterStringency || knowngenes[i].negSupp >= filterStringency))

            ) {
                if (graphNodeIds.indexOf(knowngenes[i].foundGene.id) === -1) {
                    isQueryGene = false;

                    if (this.currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) !== -1) {
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

                    if (this.currentQueryGeneIds.indexOf(knowngenes[i].queryGene.id) !== -1) {
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
                        nodeDegreeBin: Gemma.CytoscapePanelUtil.nodeDegreeBinMapper(Gemma.CytoscapePanelUtil.getMaxWithNull(
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
                if (graphNodeIds.indexOf(knowngenes[i].foundGene.id) !== -1 && graphNodeIds.indexOf(knowngenes[i].queryGene.id) !== -1 && (knowngenes[i].posSupp >= filterStringency || knowngenes[i].negSupp >= filterStringency) && this.currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) === -1 && this.currentQueryGeneIds.indexOf(knowngenes[i].queryGene.id) === -1) {

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
         * (this.currentQueryGeneIds.indexOf(qgenes[i].id) !==
         * -1) { isQueryGene = true; }
         * 
         * if (!filterCurrentResults ||
         * this.currentQueryGeneIds.indexOf(qgenes[i].id) !==
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
        this.currentNodeGeneIds = graphNodeIds;

        return data;
    },

    drawGraph: function () {
        var dataMsg = {
            dataSchema: this.dataSchemaJSON,
            data: this.dataJSON
        };
        // init and draw
        this.visualization.draw({
            network: dataMsg,
            visualStyle: this.nodeDegreeVisualStyleFlag ? this.visualStyleNodeDegree : this.visualStyleRegular,
            layout: this.currentLayout
        });
    },

    clearError: function () {
        if (Ext.get("analysis-results-search-form-messages")) {
            Ext.DomHelper.overwrite("analysis-results-search-form-messages", "");
        }
    },

    updateSearchFormGenes: function (geneIds) {
        // clear current
        this.searchPanelRef.geneChoosers.removeAll();
        // add new genesearchandpreview
        var geneChooser = this.searchPanelRef.addGeneChooser();
        var genesToPreview = [];
        var genesToPreviewIds = [];
        var knowngenes = this.knownGeneResults;
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
        var querygenes = this.queryGenes;
        var qglength = querygenes.length;        
        for (i = 0; i < qglength; i++) {
            if (genesToPreviewIds.indexOf(querygenes[i].id) === -1 && geneIds.indexOf(querygenes[i].id) !== -1) {
                genesToPreview.push(querygenes[i]);
                genesToPreviewIds.push(querygenes[i].id);
            }            
        } // end for (<kglength)
        // add new genes
        geneChooser.getGenesFromGeneValueObjects(
        genesToPreview, genesToPreviewIds, this.taxonId, this.taxonName);

    },

    //Coex grid calls this function to update
    stringencyUpdate: function (stringency, trimmedKnownGeneResults, trimmedNodeIds, updateSpinnerBoolean) {

        if (updateSpinnerBoolean && this.getTopToolbar()) {
            this.getTopToolbar().getComponent('stringencySpinner').setValue(stringency);
        }

        this.trimmedKnownGeneResults = trimmedKnownGeneResults;
        this.trimmedNodeIds = trimmedNodeIds;

        filterFunctionNodes = function (node) {
            return this.trimmedNodeIds.indexOf(node.data.geneid) !== -1;
        };

        this.visualization.filter("nodes", filterFunctionNodes.createDelegate(this));

        filterFunctionEdges = function (edge) {
            return edge.data.support >= stringency;
        };

        this.visualization.filter("edges", filterFunctionEdges.createDelegate(this));

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

    nodeDegreeEmphasis: function () {
        if (this.ready) {
            if (this.nodeDegreeVisualStyleFlag) {
                this.nodeDegreeVisualStyleFlag = false;
                this.visualization.visualStyle(this.visualStyleRegular);
            } else {
                this.nodeDegreeVisualStyleFlag = true;
                this.visualization.visualStyle(this.visualStyleNodeDegree);
            }
        }
    },

    refreshLayout: function () {
        if (this.ready) {
            this.visualization.layout(this.currentLayout);
        }
    },

    compressGraph: function () {
        if (this.ready) {
            if (this.layoutCompressed) {
                this.currentLayout = this.defaultForceDirectedLayout;
                this.layoutCompressed = false;
                this.visualOptionsMenu.getComponent('compressGraphButton').setText(Gemma.HelpText.WidgetDefaults.CytoscapePanel.compressGraphText);
            } else {
                this.currentLayout = this.forceDirectedLayoutCompressed;
                this.layoutCompressed = true;
                this.visualOptionsMenu.getComponent('compressGraphButton').setText(Gemma.HelpText.WidgetDefaults.CytoscapePanel.unCompressGraphText);
            }

            this.visualization.layout(this.currentLayout);
            this.visualization.zoomToFit();
        }
    },

    toggleNodeLabels: function () {
        if (this.ready) {
            if (!this.visualization.nodeLabelsVisible()) {
                this.visualOptionsMenu.getComponent('nodeLabelsButton').setText(Gemma.HelpText.WidgetDefaults.CytoscapePanel.noNodeLabelsText);
                this.visualization.nodeLabelsVisible(true);
            } else {
                this.visualOptionsMenu.getComponent('nodeLabelsButton').setText(Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeLabelsText);
                this.visualization.nodeLabelsVisible(false);
            }
        }
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