Ext.namespace('Gemma');

Gemma.CytoscapeSettings = {

    backgroundColor: "#FFF7FB",

    // node stuff
    labelFontName: 'Arial',
    labelFontColor: "#252525",
    labelFontColorFade: "#BDBDBD",
    labelGlowStrength: 100,
    labelFontWeight: "bold",
    labelFontSize: 11,
    
    labelFontSizeBigger: 18,
    labelFontSizeBiggest: 25,

    labelYOffset: -20,

    labelHorizontalAnchor: "center",

    nodeColor: "#969696",
    nodeColorFade: "#FFF7FB",
    nodeSize: 25,

    nodeQueryColorTrue: "#E41A1C",
    nodeQueryColorFalse: "#6BAED6",

    // edge stuff
    supportColorBoth: "#CCCCCC",
    supportColorPositive: "#E66101",
    supportColorNegative: "#5E3C99",

    selectionGlowColor: "#0000FF",

    selectionGlowOpacity: 1,    
    
    zoomLevelBiggerFont: 0.65,    
    zoomLevelBiggestFont: 0.4,

};

Gemma.CytoscapePanel = Ext.extend(
Ext.Panel, {

    title: 'Cytoscape',    
    
    layout: 'fit',

    currentResultsStringency: 2,

    initialDisplayStringency: 2,
    
    currentSpinnerValue: 2,

    currentNodeGeneIds: [],

    currentQueryGeneIds: [],

    // used to pass a subset of results that meet the stringency
    // threshold to the coexpressionGrid widget displaying the
    // results in a table
    trimmedKnownGeneResults: [],

    // used to apply a filter to the graph to remove nodes when
    // stringency changes
    trimmedNodeIds: [],

    ready: false,

    dataJSON: {
        nodes: [],
        edges: []
    },

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

        }

        ],
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
    
    forceDirectedLayoutCompressed:{
    	
    	name: "ForceDirected",
    	options: {
    		mass : 2,
    		gravitation :-300,
    		tension: 0.3,
    		drag: 0.4,
    		minDistance: 1,
    		maxDistance: 10000,
    		iterations: 400,
    		maxTime: 30000
    	}
    	
    	
    },
    
    defaultForceDirectedLayout:{
    	
    	name: "ForceDirected"    	    	
    	
    },
    
    currentLayout:{},       

    visual_style_regular: {
        global: {

            backgroundColor: Gemma.CytoscapeSettings.backgroundColor
        },
        nodes: {
            tooltipText: "Official Name:${officialName}<br/>Node Degree:${nodeDegree}<br/>NCBI Id:${ncbiId}<br/>",
            shape: "ELLIPSE",
            borderWidth: {
                discreteMapper: {
                    attrName: "queryflag",
                    entries: [{
                        attrValue: true,
                        value: 3
                    }, {
                        attrValue: false,
                        value: 0
                    }]
                }

            },

            size: {
                defaultValue: Gemma.CytoscapeSettings.nodeSize

            },

            labelFontName: Gemma.CytoscapeSettings.labelFontName,
            labelFontColor: Gemma.CytoscapeSettings.labelFontColor,
            labelGlowStrength: Gemma.CytoscapeSettings.labelGlowStrength,
            labelFontWeight: Gemma.CytoscapeSettings.labelFontWeight,

            labelYOffset: Gemma.CytoscapeSettings.labelYOffset,

            labelHorizontalAnchor: Gemma.CytoscapeSettings.labelHorizontalAnchor,
            borderColor: {
                discreteMapper: {
                    attrName: "queryflag",
                    entries: [{
                        attrValue: true,
                        value: Gemma.CytoscapeSettings.nodeQueryColorTrue
                    }, {
                        attrValue: false,
                        value: Gemma.CytoscapeSettings.nodeQueryColorFalse
                    }]
                }

            },

            color: Gemma.CytoscapeSettings.nodeColor,

            selectionGlowColor: Gemma.CytoscapeSettings.selectionGlowColor,

            selectionGlowOpacity: Gemma.CytoscapeSettings.selectionGlowOpacity
        },
        edges: {
            tooltipText: "Edge Nodes: ${target} to ${source}<br/>Positive Support:${positivesupport}<br/>Negative Support:${negativesupport}",
            width: {
                defaultValue: 1,
                continuousMapper: {
                    attrName: "support",
                    minValue: 1,
                    maxValue: 6
                }

            },

            color: {

                discreteMapper: {
                    attrName: "supportsign",
                    entries: [{
                        attrValue: "both",
                        value: Gemma.CytoscapeSettings.supportColorBoth
                    }, {
                        attrValue: "positive",
                        value: Gemma.CytoscapeSettings.supportColorPositive
                    }, {
                        attrValue: "negative",
                        value: Gemma.CytoscapeSettings.supportColorNegative
                    }]
                }
            },

            selectionGlowColor: Gemma.CytoscapeSettings.selectionGlowColor,

            selectionGlowOpacity: Gemma.CytoscapeSettings.selectionGlowOpacity
        }
    },
    visual_style_node_degree: {
        global: {
            backgroundColor: Gemma.CytoscapeSettings.backgroundColor
        },
        nodes: {
            tooltipText: "Official Name:${officialName}<br/>Node Degree:${nodeDegree}<br/>NCBI Id:${ncbiId}<br/>",
            shape: "ELLIPSE",
            borderWidth: {
                discreteMapper: {
                    attrName: "queryflag",
                    entries: [{
                        attrValue: true,
                        value: 3
                    }, {
                        attrValue: false,
                        value: 0
                    }]
                }

            },

            size: {
                defaultValue: Gemma.CytoscapeSettings.nodeSize

            },

            labelFontColor: {
                continuousMapper: {
                    attrName: "nodeDegreeBin",
                    minValue: Gemma.CytoscapeSettings.labelFontColor,
                    maxValue: Gemma.CytoscapeSettings.labelFontColorFade,
                    maxAttrValue: 11

                }

            },

            labelFontName: Gemma.CytoscapeSettings.labelFontName,

            labelGlowStrength: Gemma.CytoscapeSettings.labelGlowStrength,
            labelFontWeight: Gemma.CytoscapeSettings.labelFontWeight,

            labelYOffset: Gemma.CytoscapeSettings.labelYOffset,

            labelHorizontalAnchor: Gemma.CytoscapeSettings.labelHorizontalAnchor,

            borderColor: {
                discreteMapper: {
                    attrName: "queryflag",
                    entries: [{
                        attrValue: true,
                        value: Gemma.CytoscapeSettings.nodeQueryColorTrue
                    }, {
                        attrValue: false,
                        value: Gemma.CytoscapeSettings.nodeQueryColorFalse
                    }]
                }

            },
            color: {

                continuousMapper: {
                    attrName: "nodeDegreeBin",
                    minValue: Gemma.CytoscapeSettings.nodeColor,
                    maxValue: Gemma.CytoscapeSettings.nodeColorFade,
                    maxAttrValue: 11
                }
            },

            selectionGlowColor: Gemma.CytoscapeSettings.selectionGlowColor,

            selectionGlowOpacity: Gemma.CytoscapeSettings.selectionGlowOpacity
        },
        edges: {
            tooltipText: "Edge Nodes: ${target} to ${source}<br/>Positive Support:${positivesupport}<br/>Negative Support:${negativesupport}",
            width: {
                defaultValue: 1,
                continuousMapper: {
                    attrName: "support",
                    minValue: 1,
                    maxValue: 6
                }

            },

            opacity: {

                customMapper: {
                    functionName: "edgeOpacityMapper"
                }

            },

            color: {

                discreteMapper: {
                    attrName: "supportsign",
                    entries: [{
                        attrValue: "both",
                        value: Gemma.CytoscapeSettings.supportColorBoth
                    }, {
                        attrValue: "positive",
                        value: Gemma.CytoscapeSettings.supportColorPositive
                    }, {
                        attrValue: "negative",
                        value: Gemma.CytoscapeSettings.supportColorNegative
                    }]
                }
            },

            selectionGlowColor: Gemma.CytoscapeSettings.selectionGlowColor,

            selectionGlowOpacity: Gemma.CytoscapeSettings.selectionGlowOpacity

        }
    },

    initComponent: function () {

        var vis = new org.cytoscapeweb.Visualization("cytoscapeweb", this.options);
        
        this.currentLayout = this.defaultForceDirectedLayout;

        vis["edgeOpacityMapper"] = function (data) {

            if (data["nodeDegreeBin"] == null) {
                return 0.05;

            }

            return 1.05 - data["nodeDegreeBin"] / 10;

        };
        
        this.visualOptionsMenu = new Ext.menu.Menu({
            items: [{
            	itemId: 'refreshLayoutButton',
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.refreshLayoutText,                
                tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.refreshLayoutTT,
                handler: this.refreshLayout,
                scope: this
            },{
            	itemId: 'compressGraphButton',
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.compressGraphText,                
                tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.compressGraphTT,
                handler: this.compressGraph,
                scope: this
            },{
            	itemId: 'nodeLabelsButton',
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.noNodeLabelsText,                
                tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeLabelsTT,
                handler: this.nodeLabelsToggle,
                scope: this
            }
            ]
        });

        this.actionsMenu = new Ext.menu.Menu({
            items: [{
                itemId: 'extendSelectedNodesButton',
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.extendNodeText,
                tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.extendNodeTT,
                handler: this.extendSelectedNodes,
                scope: this
            }, {
                itemId: 'searchWithSelectedNodesButton',
                text: Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchWithSelectedText,
                // icon:
                // '/Gemma/images/icons/picture.png',
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

            },

            {
                xtype: 'tbspacer'
            }, {
                xtype: 'spinnerfield',
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

            },

            {
                xtype: 'label',

                html: '<img ext:qtip="'
                	+ Gemma.HelpText.WidgetDefaults.CytoscapePanel.stringencySpinnerTT
                	+ '" src="/Gemma/images/icons/question_blue.png"/>',
                // text : 'Specificity
                // filter',
                height: 15
            },

            '->', '-',
            {
                xtype: 'button',
                // icon:
                // '/Gemma/images/icons/question_blue.png',
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
                // icon:
                // '/Gemma/images/download.gif',
                menu: new Ext.menu.Menu({
                    items: [{
                        text: 'Save as PNG',
                        tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.saveAsImageTT,
                        handler: this.exportPNG,
                        scope: this
                    }]
                })
            },

            '->', '-',

            {
                xtype: 'button',
                text: '<b>Visual Options</b>',
                // icon:
                // '/Gemma/images/download.gif',
                menu: this.visualOptionsMenu
            },

            '->', '-',

            this.actionsButton,

            '->', '-',
            {
                xtype: 'button',
                itemId: 'nodeDegreeEmphasis',
                text: '<b>'+Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeDegreeEmphasisText+'</b>',

                enableToggle: 'true',
                pressed: 'true',
                handler: this.nodeDegreeEmphasis,
                scope: this
            }, {
                xtype: 'label',

                html: '<img ext:qtip="' +
                Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeDegreeEmphasisTT 
                + '" src="/Gemma/images/icons/question_blue.png"/>',
                // text : 'Specificity
                // filter',
                height: 15
            }

            ],
            // end tbar
            bbar: [{
                xtype: 'tbtext',
                text: '',
                itemId: 'bbarStatus'
            },'->', {
				xtype : 'button',
				icon: "/Gemma/images/icons/cross.png",
				itemId: 'bbarClearButton',
				handler : function(){					
					this.currentbbarText="";
					this.getBottomToolbar().hide();
					this.doLayout();
					
				},
				scope : this
			}

            ],

            margins: {
                top: 0,
                right: 0,
                bottom: 0,
                left: 0
            },

            items: [

            {
                xtype: 'flash',                

                id: 'cytoscapeweb',
                listeners: {
                    afterrender: {
                        scope: this,
                        fn: function () {

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

                                Ext.apply(
                                results, {
                                    queryGenes: this.queryGenes,
                                    knownGeneResults: this.knownGeneResults
                                });
                                results.queryGenes = this.queryGenes;
                                results.knownGeneResults = this.knownGeneResults;

                                var spinner = this.getTopToolbar().getComponent('stringencySpinner');

                                if (this.coexCommand.displayStringency > Gemma.MIN_STRINGENCY) {
                                    spinner.setValue(this.coexCommand.displayStringency);
                                    
                                    var bbarText = this.getBottomToolbar().getComponent('bbarStatus');
                                    
                                    this.currentbbarText = "Display Stringency set to "+this.coexCommand.displayStringency+" based on number of experiments chosen.";
                                    
                                    bbarText.setText(this.currentbbarText);                                                                      
                                                                        
                                }
                                else{
                                	spinner.setValue(Gemma.MIN_STRINGENCY);
                                	this.getBottomToolbar().hide();
                					this.doLayout();
                                }

                                // spinner.minValue
                                // =
                                // this.currentResultsStringency;
                                this.initialCoexSearchCallback(results);

                            }

                        }
                    }

                }

            }

            ]
        });

        vis.panelRef = this;

        vis.ready(function () {

            vis.nodeTooltipsEnabled(true);

            vis.edgeTooltipsEnabled(true);
            
            vis.addListener("zoom", function(evt){
            	
            	if (vis.panelRef.ready){
            		
            		var zoom = evt.value; 
            		
            		if (vis.panelRef.lastZoomLevel){
            			
            			//start changing font size
            			
            			var newFontSize;
            			if (zoom < vis.panelRef.lastZoomLevel){            				
            				
            				//if the zoom is less than the biggest font threshold AND the last zoom level used to be bigger than the last zoom level
            				if (zoom < Gemma.CytoscapeSettings.zoomLevelBiggestFont && vis.panelRef.lastZoomLevel>Gemma.CytoscapeSettings.zoomLevelBiggestFont){
            					newFontSize = Gemma.CytoscapeSettings.labelFontSizeBiggest;
            				}
            				else if (zoom < Gemma.CytoscapeSettings.zoomLevelBiggerFont && vis.panelRef.lastZoomLevel>Gemma.CytoscapeSettings.zoomLevelBiggerFont){
            					newFontSize = Gemma.CytoscapeSettings.labelFontSizeBigger;
            				}
            				
            			} else if (zoom > vis.panelRef.lastZoomLevel){            				
            				
            				if (zoom > Gemma.CytoscapeSettings.zoomLevelBiggerFont && vis.panelRef.lastZoomLevel<Gemma.CytoscapeSettings.zoomLevelBiggerFont){            					
            					newFontSize = Gemma.CytoscapeSettings.labelFontSize;
            				} else if (zoom > Gemma.CytoscapeSettings.zoomLevelBiggestFont && vis.panelRef.lastZoomLevel<Gemma.CytoscapeSettings.zoomLevelBiggestFont){            					
            					newFontSize = Gemma.CytoscapeSettings.labelFontSizeBigger;
            				}
            				
            			}
            			
            			if (newFontSize){
            				
            				vis.panelRef.visual_style_regular.nodes.labelFontSize = newFontSize;        					
        					vis.panelRef.visual_style_node_degree.nodes.labelFontSize = newFontSize;
        					
        					if (vis.panelRef.nodeDegreeVisualStyleFlag) {
        						vis.visualStyle(vis.panelRef.visual_style_node_degree);
                            } else {
                            	vis.visualStyle(vis.panelRef.visual_style_regular);
                            }            					
        					
            			}           			
            			
            			//end changing font size
            			
            			vis.panelRef.lastZoomLevel = zoom;
                    
            		} else {
            			//first time in event handler
            			
            			vis.panelRef.lastZoomLevel = zoom;
            			var newFontSize;
            			
        					if (zoom < Gemma.CytoscapeSettings.zoomLevelBiggestFont){
        						//biggest
        						newFontSize = Gemma.CytoscapeSettings.labelFontSizeBiggest;
        					}else if (zoom < Gemma.CytoscapeSettings.zoomLevelBiggerFont){
        						//bigger
        						newFontSize = Gemma.CytoscapeSettings.labelFontSizeBigger;
        					} else {
        						//normal
        						newFontSize = Gemma.CytoscapeSettings.labelFontSize;
        					}
            			
            			if (newFontSize){
            				
            				vis.panelRef.visual_style_regular.nodes.labelFontSize = newFontSize;        					
        					vis.panelRef.visual_style_node_degree.nodes.labelFontSize = newFontSize;
        					
        					if (vis.panelRef.nodeDegreeVisualStyleFlag) {
        						vis.visualStyle(vis.panelRef.visual_style_node_degree);
                            } else {
                            	vis.visualStyle(vis.panelRef.visual_style_regular);
                            }            					
        					
            			} 
            		}//end first time
            	}            	            	
            	
            });

            if (!vis.panelRef.getTopToolbar().getComponent('stringencySpinner').hasListener('specialkey')) {

                vis.panelRef.getTopToolbar().getComponent('stringencySpinner').addListener('specialkey', function (field, e) {

                    if (e.getKey() == e.ENTER) {
                        var spinner = vis.panelRef.getTopToolbar().getComponent('stringencySpinner');

                        // prevent
                        // spinner from
                        // going below 2
                        // by manual
                        // entry
                        if (spinner.getValue() < 2) {
                            spinner.setValue(2);
                        }

                        if (spinner.getValue() >= vis.panelRef.currentResultsStringency) {
                        	
                        	vis.panelRef.currentSpinnerValue = spinner.getValue();

                            var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResults(
                            vis.panelRef.knownGeneResults, vis.panelRef.currentQueryGeneIds, vis.panelRef.getTopToolbar().getComponent('stringencySpinner').getValue());
                            vis.panelRef.trimmedKnownGeneResults = trimmed.trimmedKnownGeneResults;
                            vis.panelRef.trimmedNodeIds = trimmed.trimmedNodeIds;

                            vis.panelRef.coexGridRef.cytoscapeUpdate(
                            spinner.getValue(), vis.panelRef.queryGenes.length, vis.panelRef.trimmedKnownGeneResults);

                            vis.filter("nodes", function (
                            node) {

                                return vis.panelRef.trimmedNodeIds.indexOf(node.data.geneid) !== -1;

                            });

                            vis.filter("edges", function (
                            edge) {

                                return edge.data.support >= vis.panelRef.getTopToolbar().getComponent('stringencySpinner').getValue();

                            });

                        } else { // new
                            // search
                            Ext.Msg.confirm('New Search', Gemma.HelpText.WidgetDefaults.CytoscapePanel.lowStringencyWarning, function (
                            btn) {

                                if (btn == 'yes') {
                                	
                                	vis.panelRef.currentSpinnerValue = spinner.getValue();

                                    var displayStringency = spinner.getValue();
                                    var resultsStringency = spinner.getValue();

                                    if (displayStringency > 5) {
                                        resultsStringency = displayStringency - Math.round(displayStringency / 4);
                                    }

                                    Ext.apply(
                                    vis.panelRef.coexCommand, {
                                        stringency: resultsStringency,
                                        displayStringency: displayStringency,
                                        geneIds: vis.panelRef.currentQueryGeneIds,
                                        queryGenesOnly: false
                                    });

                                    vis.panelRef.loadMask.show();
                                    ExtCoexpressionSearchController.doSearchQuick2(
                                    vis.panelRef.coexCommand, {
                                        callback: vis.panelRef.initialCoexSearchCallback.createDelegate(vis.panelRef)

                                    });

                                } else {
                                    spinner.setValue(this.currentSpinnerValue);
                                }
                            }, this);

                        }
                    }

                }, this);
            }

            if (!vis.panelRef.getTopToolbar().getComponent('stringencySpinner').hasListener('spin')) {

                vis.panelRef.getTopToolbar().getComponent('stringencySpinner').addListener('spin', function (ev) {

                    var spinner = vis.panelRef.getTopToolbar().getComponent('stringencySpinner');

                    /*
                     * //prevent spinner
                     * from going below
                     * currentResultsStringency
                     * if
                     * (spinner.getValue() <
                     * vis.panelRef.currentResultsStringency){
                     * spinner.setValue(vis.panelRef.currentResultsStringency); }
                     */

                    if (spinner.getValue() >= vis.panelRef.currentResultsStringency) {
                    	
                    	vis.panelRef.currentSpinnerValue = spinner.getValue();

                        var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResults(
                        vis.panelRef.knownGeneResults, vis.panelRef.currentQueryGeneIds, vis.panelRef.getTopToolbar().getComponent('stringencySpinner').getValue());
                        vis.panelRef.trimmedKnownGeneResults = trimmed.trimmedKnownGeneResults;
                        vis.panelRef.trimmedNodeIds = trimmed.trimmedNodeIds;

                        vis.panelRef.coexGridRef.cytoscapeUpdate(
                        spinner.getValue(), vis.panelRef.queryGenes.length, vis.panelRef.trimmedKnownGeneResults);

                        vis.filter("nodes", function (
                        node) {

                            return vis.panelRef.trimmedNodeIds.indexOf(node.data.geneid) !== -1;

                        });

                        vis.filter("edges", function (
                        edge) {

                            return edge.data.support >= vis.panelRef.getTopToolbar().getComponent('stringencySpinner').getValue();

                        });

                    } else { // new
                        // search
                        Ext.Msg.confirm('New Search',Gemma.HelpText.WidgetDefaults.CytoscapePanel.lowStringencyWarning, function (
                        btn) {

                            if (btn == 'yes') {

                                var displayStringency = spinner.getValue();
                                var resultsStringency = spinner.getValue();

                                if (displayStringency > 5) {
                                    resultsStringency = displayStringency - Math.round(displayStringency / 4);
                                }

                                Ext.apply(
                                vis.panelRef.coexCommand, {
                                    stringency: resultsStringency,
                                    displayStringency: displayStringency,
                                    geneIds: vis.panelRef.currentQueryGeneIds,
                                    queryGenesOnly: false
                                });

                                vis.panelRef.loadMask.show();
                                ExtCoexpressionSearchController.doSearchQuick2(
                                vis.panelRef.coexCommand, {
                                    callback: vis.panelRef.initialCoexSearchCallback.createDelegate(vis.panelRef)

                                });

                            } else {
                                spinner.setValue(spinner.getValue() + 1);
                            }
                        }, this);

                    }

                }, this);

            }

            /*
             * vis.addContextMenuItem("Export Graph as
             * graphml", "none", function () {
             * 
             * 
             * var htmlString = vis.graphml();
             * 
             * var win = new Ext.Window({ title:
             * 'graphml', height: 600, width: 800,
             * plain: true, html: htmlString });
             * win.show();
             * 
             * 
             * });
             * 
             * vis.addContextMenuItem("Export Graph as
             * sif", "none", function () {
             * 
             * var htmlString = vis.sif();
             * 
             * var win = new Ext.Window({ title: 'sif',
             * height: 600, width: 800, plain: true,
             * html: htmlString }); win.show();
             * 
             * 
             * }); vis.addContextMenuItem("Export Graph
             * as xgmml", "none", function () {
             * 
             * var htmlString = vis.xgmml();
             * 
             * var win = new Ext.Window({ title:
             * 'xgmml', height: 600, width: 800, plain:
             * true, html: htmlString }); win.show();
             * 
             * 
             * });
             */

            vis.panelRef.ready = true;

            // filter visable results based on
            // initialDisplayStringency
            if (vis.panelRef.initialDisplayStringency > vis.panelRef.currentResultsStringency) {
                var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResults(
                vis.panelRef.knownGeneResults, vis.panelRef.currentQueryGeneIds, vis.panelRef.initialDisplayStringency);
                vis.panelRef.stringencyUpdate(
                vis.panelRef.initialDisplayStringency, trimmed.trimmedKnownGeneResults, trimmed.trimmedNodeIds);

                // update the grid with trimmed data
                // (underlying data in coexGridRef has
                // already been set)
                vis.panelRef.coexGridRef.cytoscapeUpdate(
                vis.panelRef.initialDisplayStringency, vis.panelRef.currentQueryGeneIds.length, trimmed.trimmedKnownGeneResults, this.currentResultsStringency);

            }

        });
        // end vis.ready()
        Ext.apply(this, {
            visualization: vis
        });

        Gemma.CytoscapePanel.superclass.initComponent.apply(
        this, arguments);

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

    nodeDegreeEmphasis: function () {

        if (this.ready) {

            if (this.nodeDegreeVisualStyleFlag) {
                this.nodeDegreeVisualStyleFlag = false;
                this.visualization.visualStyle(this.visual_style_regular);
            } else {
                this.nodeDegreeVisualStyleFlag = true;
                this.visualization.visualStyle(this.visual_style_node_degree);
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
        	
        	if (this.layoutCompressed){
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
    
    nodeLabelsToggle: function () {

        if (this.ready) {
        	
        	if (!this.visualization.nodeLabelsVisible()){        		
        		this.visualOptionsMenu.getComponent('nodeLabelsButton').setText(Gemma.HelpText.WidgetDefaults.CytoscapePanel.noNodeLabelsText);
        		this.visualization.nodeLabelsVisible(true);        		
           	} else {           		
           		this.visualOptionsMenu.getComponent('nodeLabelsButton').setText(Gemma.HelpText.WidgetDefaults.CytoscapePanel.nodeLabelsText); 
           		this.visualization.nodeLabelsVisible(false); 
           	}
        	
        }
    },

    reRunSearchWithSelectedNodes: function () {

        if (this.ready) {

            this.clearError();

            var selectedNodes = this.visualization.selected("nodes");

            if (selectedNodes.length > 0 && selectedNodes.length <= Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {
            	
            	this.getBottomToolbar().setVisible(false);
            	this.doLayout();
            	this.currentbbarText = '';

                var spinner = this.getTopToolbar().getComponent('stringencySpinner');

                var displayStringency = spinner.getValue();
                var resultsStringency = spinner.getValue();

                if (displayStringency > 5) {
                    resultsStringency = displayStringency - Math.round(displayStringency / 4);
                }

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

                Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle,
                		String.format(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooMany, Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY));

            } else if (selectedNodes.length == 0) {

                Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, 
                		Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooFew);
            }

        }
    },

    extendSelectedNodes: function () {

        if (this.ready) {
        	
            this.clearError();

            var selectedNodes = this.visualization.selected("nodes");

            if (selectedNodes.length > 0 && selectedNodes.length <= Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {
            	
            	this.getBottomToolbar().setVisible(false);
            	this.doLayout();
            	this.currentbbarText = '';

                if (this.currentQueryGeneIds.length + selectedNodes.length <= Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {

                    var spinner = this.getTopToolbar().getComponent('stringencySpinner');
                    var displayStringency = spinner.getValue();
                    var resultsStringency = spinner.getValue();

                    if (displayStringency > 5) {
                        resultsStringency = displayStringency - Math.round(displayStringency / 4);
                    }

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

                } else {

                    Ext.Msg.confirm(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, 
    						String.format(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooManyReduce, Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY), function (btn) {

                        if (btn == 'yes') {

                            var spinner = this.getTopToolbar().getComponent('stringencySpinner');
                            var displayStringency = spinner.getValue();
                            var resultsStringency = spinner.getValue();

                            if (displayStringency > 5) {
                                resultsStringency = displayStringency - Math.round(displayStringency / 4);
                            }

                            var extendedNodesGeneIdArray = [];
                            var sNodesLength = selectedNodes.length;

                            // make room in
                            // currentQueryGeneIds
                            // for new genes
                            this.currentQueryGeneIds = this.currentQueryGeneIds.splice(this.currentQueryGeneIds.length - (Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY - selectedNodes.length));

                            var i;
                            for (i = 0; i < sNodesLength; i++) {
                                extendedNodesGeneIdArray[i] = selectedNodes[i].data.geneid;

                                if (this.currentQueryGeneIds.indexOf(selectedNodes[i].data.geneid) === -1) {
                                    this.currentQueryGeneIds.push(selectedNodes[i].data.geneid);
                                }
                            }

                            this.updateSearchFormGenes(this.currentQueryGeneIds);

                            Ext.apply(
                            this.coexCommand, {
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
                        }
                    }, this);
                }

            } else if (selectedNodes.length > Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {

            	Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, 
						String.format(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooMany, Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY));

            } else {

            	 Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, 
 						Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooFew);
            }

        }

    },

    ttSubstring: function (tString) {

        if (!tString) {
            return null;
        }

        var maxLength = 60;

        if (tString.length > maxLength) {
            return tString.substr(0, maxLength) + "...";
        }

        return tString;
    },
    // inputs should be two node degrees between 0 and 1, if
    // null(missing data) return 1 as nodes/edges with 1 fade
    // into the background
    getMaxWithNull: function (n1, n2) {

        // missing data check
        if (n1 == null || n2 == null) {
            return 1;
        }

        return Math.max(n1, n2);

    },

    decimalPlaceRounder: function (number) {

        if (number == null) {
            return null;
        }
        return Ext.util.Format.round(number, 4);

    },

    nodeDegreeBinMapper: function (nodeDegree) {

        // no data for some genes
        if (nodeDegree == null) {
            return null;
        }
        return nodeDegree * 10;
        /*
         * //this should stay zero for the opacity use var
         * lowVis = 0; if (nodeDegree > 0.9989) { return lowVis +
         * 10; } else if (nodeDegree > 0.9899) { return lowVis +
         * 9; } else if (nodeDegree > 0.8999) { return lowVis +
         * 8; } else if (nodeDegree > 0.8499) { return lowVis +
         * 7; } else if (nodeDegree > 0.7999) { return lowVis +
         * 6; } else if (nodeDegree > 0.1999) { //this should be
         * bland colour return lowVis + 5; } else if (nodeDegree >
         * 0.1499) { return lowVis + 4; } else if (nodeDegree >
         * 0.0999) { return lowVis + 4; } else if (nodeDegree >
         * 0.0499) { return lowVis + 3; } else if (nodeDegree >
         * 0.0099) { return lowVis + 2; } else if (nodeDegree >
         * 0.0009) { return lowVis + 1; } else { return lowVis; }
         * 
         */
    },

    completeCoexSearchCallback: function (result) {

        this.queryGenes = result.queryGenes;

        //get rid of duplicates
        var duplicatesRemoved = Gemma.CoexValueObjectUtil.removeDuplicates(result.knownGeneResults);


        this.knownGeneResults = duplicatesRemoved;
        var spinner = this.getTopToolbar().getComponent('stringencySpinner');

        // update underlying data
        this.coexGridRef.knownGeneResults = this.knownGeneResults;
        this.coexGridRef.currentQueryGeneIds = this.currentQueryGeneIds;

        // update the grid
        this.coexGridRef.cytoscapeUpdate(spinner.getValue(), this.queryGenes.length, this.knownGeneResults, this.currentResultsStringency);
        
        this.currentSpinnerValue = spinner.getValue();

        this.dataJSON = this.constructDataJSON(this.queryGenes, this.knownGeneResults);

        this.drawGraph();
        this.visualOptionsMenu.getComponent('nodeLabelsButton').setText(Gemma.HelpText.WidgetDefaults.CytoscapePanel.noNodeLabelsText);
        
        if (result.displayInfo){        
        	var bbarText = this.getBottomToolbar().getComponent('bbarStatus');        	
        	
        	if (this.currentbbarText){        	
        		bbarText.setText(this.currentbbarText + ' ' +result.displayInfo);
        	}
        	else{
        		bbarText.setText(result.displayInfo);
        	}
        	if (!this.getBottomToolbar().isVisible()){
        		this.getBottomToolbar().setVisible(true);
        		this.doLayout();
        	}
        	
        }

        this.loadMask.hide();

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
        	Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, 
					Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusNoMoreResults);
					
            this.loadMask.hide();
        } else {

            Ext.apply(this.coexCommand, {
                geneIds: this.currentNodeGeneIds,
                queryGenesOnly: true
            });

            ExtCoexpressionSearchController.doSearchQuick2Complete(
            this.coexCommand, this.currentQueryGeneIds, {
                callback: this.extendThisNodeCompleteCoexSearchCallback.createDelegate(this)

            });

        }
    },

    extendThisNodeCompleteCoexSearchCallback: function (result) {

        this.queryGenes = result.queryGenes;

        //get rid of duplicates
        var duplicatesRemoved = Gemma.CoexValueObjectUtil.removeDuplicates(result.knownGeneResults);

        this.knownGeneResults = duplicatesRemoved;

        var spinner = this.getTopToolbar().getComponent('stringencySpinner');

        // update underlying data because of new results
        this.coexGridRef.knownGeneResults = this.knownGeneResults;
        this.coexGridRef.currentQueryGeneIds = this.currentQueryGeneIds;

        // update the grid
        this.coexGridRef.cytoscapeUpdate(spinner.getValue(), this.queryGenes.length, this.knownGeneResults, this.currentResultsStringency);
        
        this.currentSpinnerValue = spinner.getValue();

        this.dataJSON = this.constructDataJSON(
        result.queryGenes, result.knownGeneResults);

        this.loadMask.hide();

        this.drawGraph();
        
        this.visualOptionsMenu.getComponent('nodeLabelsButton').setText(Gemma.HelpText.WidgetDefaults.CytoscapePanel.noNodeLabelsText);
        
        if (result.displayInfo){        
        	var bbarText = this.getBottomToolbar().getComponent('bbarStatus');        	
        	
        	if (this.currentbbarText){        	
        		bbarText.setText(this.currentbbarText + ' ' +result.displayInfo);
        	}
        	else{
        		bbarText.setText(result.displayInfo);
        	}
        	if (!this.getBottomToolbar().isVisible()){
        		this.getBottomToolbar().setVisible(true);
        		this.doLayout();
        	}
        	
        }
    },

    constructDataJSON: function (qgenes, knowngenes) {

        return this.constructDataJSONFilter(qgenes, knowngenes, false, null);

    },

    // always send in qgenes
    constructDataJSONFilter: function (qgenes, knowngenes, filterCurrentResults, filterStringency) {

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
                        officialName: this.ttSubstring(knowngenes[i].foundGene.officialName),
                        ncbiId: knowngenes[i].foundGene.ncbiId,
                        nodeDegreeBin: this.nodeDegreeBinMapper(knowngenes[i].foundGeneNodeDegree),
                        nodeDegree: this.decimalPlaceRounder(knowngenes[i].foundGeneNodeDegree)
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
                        officialName: this.ttSubstring(knowngenes[i].queryGene.officialName),
                        ncbiId: knowngenes[i].queryGene.ncbiId,
                        nodeDegreeBin: this.nodeDegreeBinMapper(knowngenes[i].queryGeneNodeDegree),
                        nodeDegree: this.decimalPlaceRounder(knowngenes[i].queryGeneNodeDegree)
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
                        nodeDegreeBin: this.nodeDegreeBinMapper(this.getMaxWithNull(
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
                        nodeDegreeBin: this.nodeDegreeBinMapper(this.getMaxWithNull(
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
            visualStyle: this.nodeDegreeVisualStyleFlag ? this.visual_style_node_degree : this.visual_style_regular,
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
        // add new genes
        geneChooser.getGenesFromGeneValueObjects(
        genesToPreview, genesToPreviewIds, this.taxonId, this.taxonName);

    },

    stringencyUpdate: function (stringency, trimmedKnownGeneResults, trimmedNodeIds) {

        if (this.getTopToolbar()) {
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
    
    changeFontSize: function (fontSize){
    	this.visual_style_regular.nodes.labelFontSize = fontSize;
		
		this.visual_style_node_degree.nodes.labelFontSize = fontSize;
		
		if (this.nodeDegreeVisualStyleFlag) {
			this.visualization.visualStyle(this.visual_style_node_degree);
        } else {
        	this.visualization.visualStyle(this.visual_style_regular);
        }
    }
    

});