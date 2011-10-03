Ext.namespace('Gemma');

Gemma.CytoscapePanel = Ext.extend(
Ext.Panel, {

    title: 'Cytoscape',
    layoutArray: ["ForceDirected", "Circle", "Radial", "Tree", "Preset"],
    layoutIndex: 0,

    currentNodeGeneIds: [],

    currentQueryGeneIds: [],

    decPlaces: 4,

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
            type: 'string'

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
        }, ]

    },

    options: {
        // where you have the Cytoscape Web SWF
        swfPath: "/Gemma/scripts/cytoscape/swf/CytoscapeWeb",
        // where you have the Flash installer SWF
        flashInstallerPath: "/Gemma/scripts/cytoscape/swf/playerProductInstall"
    },

    visual_style: {
        global: {
            backgroundColor: "#ABCFD6"
        },
        nodes: {
            tooltipText: "Official Name:${officialName}<br/>Node Degree:${nodeDegree}<br/>NCBI Id:${ncbiId}<br/>",
            shape: "ELLIPSE",
            borderWidth: 3,
            borderColor: "#ffffff",
            size: {
                defaultValue: 30

            },
            labelHorizontalAnchor: "center",
            borderColor: {
                discreteMapper: {
                    attrName: "queryflag",
                    entries: [{
                        attrValue: true,
                        value: "#FF8C69"
                    }, {
                        attrValue: false,
                        value: "#E0FFFF"
                    }]
                }

            },
            color: {

                continuousMapper: {
                    attrName: "nodeDegreeBin",
                    minValue: "#FFF7BC",
                    maxValue: "#EC7014" //,
                    //minAttrValue: 1,
                    //maxAttrValue: 10
                }
            }
        },
        edges: {
            tooltipText: "Positive Support:${positivesupport}<br/>Negative Support:${negativesupport}",
            width: {
                defaultValue: 1,
                continuousMapper: {
                    attrName: "support",
                    minValue: 1,
                    maxValue: 6 /*, minAttrValue: 0.1, maxAttrValue: 1.0*/
                }

            },

            color: {
                discreteMapper: {
                    attrName: "supportsign",
                    entries: [{
                        attrValue: "both",
                        value: "#FDDBC7"
                    }, {
                        attrValue: "positive",
                        value: "#B2182B"
                    }, {
                        attrValue: "negative",
                        value: "#4D4D4D"
                    }]
                }
            }
        }
    },

    initComponent: function () {

        Ext.apply(
        this, {

            margins: {
                top: 0,
                right: 0,
                bottom: 0,
                left: 0
            },

            items: [{

                xtype: 'box',
                height: 575,
                width: 875,

                id: 'cytoscapeweb',
                listeners: {
                    afterrender: {
                        scope: this,
                        fn: function () {

                            if (!this.loadMask) {
                                this.loadMask = new Ext.LoadMask(this.getEl(), {
                                    msg: "Searching for analysis results ...",
                                    msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                                });
                            }
                            this.loadMask.show();

                            this.currentNodeGeneIds = [];

                            var qlength = this.queryGenes.length;

                            //populate geneid array for complete graph
                            var i;

                            for (i = 0; i < qlength; i++) {


                                this.currentNodeGeneIds.push(this.queryGenes[i].id);
                                this.currentQueryGeneIds.push(this.queryGenes[i].id);

                            }

                            var kglength = this.knownGeneResults.length;

                            for (i = 0; i < kglength; i++) {

                                if (this.currentNodeGeneIds.indexOf(this.knownGeneResults[i].foundGene.id) === -1) {
                                    this.currentNodeGeneIds.push(this.knownGeneResults[i].foundGene.id);
                                }

                            }

                            Ext.apply(
                            this.coexCommand, {
                                geneIds: this.currentNodeGeneIds,
                                queryGenesOnly: true
                            });

                            ExtCoexpressionSearchController.doSearchQuick2(
                            this.coexCommand, {
                                callback: this.completeCoexSearchCallback.createDelegate(this)

                            });

                        }
                    }

                }

            },

            ]
        });

        var div_id = "cytoscapeweb";

        var vis = new org.cytoscapeweb.Visualization(div_id, this.options);



        vis.panelRef = this;

        vis.ready(function () {

            vis.nodeTooltipsEnabled(true);

            vis.edgeTooltipsEnabled(true);

            // Add the Context menu item for radial layout
            vis.addContextMenuItem("Radial layout", "none", function () {
                vis.layout("Radial");
            }).addContextMenuItem("Circle layout", "none", function () {
                vis.layout("Circle");
            }).addContextMenuItem("ForceDirected layout", "none", function () {
                vis.layout("ForceDirected");
            });

            vis.addContextMenuItem("Extend this node", "nodes", function (evt) {

                var rootNode = evt.target;

                Ext.apply(
                vis.panelRef.coexCommand, {
                    geneIds: [rootNode.data.geneid],
                    queryGenesOnly: false
                });


                if (vis.panelRef.currentQueryGeneIds.indexOf(rootNode.data.geneid) === -1) {
                    vis.panelRef.currentQueryGeneIds.push(rootNode.data.geneid);
                }
                //this gene is now a query gene so go into dataJSON and change it
                var dataNodes = vis.panelRef.dataJSON.nodes;

                var dlength = dataNodes.length;
                var i;
                for (i = 0; i < dlength; i++) {

                    if (dataNodes[i]["geneid"] == rootNode.data.geneid) {

                        dataNodes[i]["queryflag"] = true;

                    }

                }

                vis.panelRef.loadMask.show();
                ExtCoexpressionSearchController.doSearchQuick2(
                vis.panelRef.coexCommand, {
                    callback: vis.panelRef.extendThisNodeInitialCoexSearchCallback.createDelegate(vis.panelRef)

                });

            });
            vis.addContextMenuItem("Extend Selected nodes", "none", function (evt) {
                
                var selectedNodes = vis.selected("nodes");

                if (selectedNodes.length > 0) {


                    var extendedNodesGeneIdArray = [];

                    var sNodesLength = selectedNodes.length;
                    var i;
                    for (i = 0; i < sNodesLength; i++) {

                        extendedNodesGeneIdArray[i] = selectedNodes[i].data.geneid;

                        if (vis.panelRef.currentQueryGeneIds.indexOf(selectedNodes[i].data.geneid) === -1) {
                            vis.panelRef.currentQueryGeneIds.push(selectedNodes[i].data.geneid);
                        }

                    }

                    Ext.apply(
                    vis.panelRef.coexCommand, {
                        geneIds: extendedNodesGeneIdArray,
                        queryGenesOnly: false
                    });



                    //these genes are now query genes so go into dataJSON and change it
                    var dataNodes = vis.panelRef.dataJSON.nodes;

                    var dlength = dataNodes.length;

                    for (i = 0; i < dlength; i++) {
                        var gid = dataNodes[i]["geneid"];
                        var k;
                        for (k = 0; k < extendedNodesGeneIdArray.length; k++) {

                            if (gid == extendedNodesGeneIdArray[k]) {

                                dataNodes[i]["queryflag"] = true;

                            }
                        }

                    }

                    vis.panelRef.loadMask.show();
                    ExtCoexpressionSearchController.doSearchQuick2(
                    vis.panelRef.coexCommand, {
                        callback: vis.panelRef.extendThisNodeInitialCoexSearchCallback.createDelegate(vis.panelRef)

                    });


                } else {

                    Ext.Msg.alert('Status of Search', 'No Genes Selected');
                }

            });

        });
        //end vis.ready()        
        Ext.apply(this, {

            visualization: vis,

        });

        Gemma.CytoscapePanel.superclass.initComponent.apply(
        this, arguments);


    },

    completeCoexSearchCallback: function (result) {

        this.queryGenes = result.queryGenes;

        this.knownGeneResults = result.knownGeneResults;

        this.dataJSON = this.constructDataJSON(this.queryGenes, this.knownGeneResults);

        this.loadMask.hide();

        this.drawGraph();

    },

    extendThisNodeInitialCoexSearchCallback: function (result) {


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
            Ext.Msg.alert('Status of Search', 'No more results found for this gene');
            this.loadMask.hide();
        } else {

            Ext.apply(this.coexCommand, {
                geneIds: this.currentNodeGeneIds,
                queryGenesOnly: true
            });

            ExtCoexpressionSearchController.doSearchQuick2(this.coexCommand, {
                callback: this.extendThisNodeCompleteCoexSearchCallback.createDelegate(this)

            });


        }
    },

    extendThisNodeCompleteCoexSearchCallback: function (result) {
        this.dataJSON = this.constructDataJSON(result.queryGenes, result.knownGeneResults);
        this.loadMask.hide();
        this.drawGraph();
    },

    //always send in qgenes
    constructDataJSON: function (qgenes, knowngenes) {

        var data = {
            nodes: [],
            edges: []
        }

        //helper array to prevent duplicate nodes from being entered
        var graphNodeIds = [];

        var kglength = knowngenes.length;
        //populate node data plus populate edge data
        for (i = 0; i < kglength; i++) {


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
                    officialName: knowngenes[i].foundGene.officialName,
                    ncbiId: knowngenes[i].foundGene.ncbiId,
                    nodeDegreeBin: Math.round(knowngenes[i].foundGeneNodeDegree * 10),
                    nodeDegree: Math.round(knowngenes[i].foundGeneNodeDegree * Math.pow(10, this.decPlaces)) / Math.pow(10, this.decPlaces)
                });

                graphNodeIds.push(knowngenes[i].foundGene.id);

            }

            if (graphNodeIds.indexOf(knowngenes[i].queryGene.id) === -1) {

                isQueryGene = false;

                if (this.currentQueryGeneIds.indexOf(knowngenes[i].queryGene.id) !== -1) {
                    isQueryGene = true;
                }

                //test to see if node is already there
                data.nodes.push({
                    id: knowngenes[i].queryGene.officialSymbol,
                    label: knowngenes[i].queryGene.officialSymbol,
                    geneid: knowngenes[i].queryGene.id,
                    queryflag: isQueryGene,
                    officialName: knowngenes[i].queryGene.officialName,
                    ncbiId: knowngenes[i].queryGene.ncbiId,
                    nodeDegreeBin: Math.round(knowngenes[i].queryGeneNodeDegree * 10),
                    nodeDegree: Math.round(knowngenes[i].queryGeneNodeDegree * Math.pow(10, this.decPlaces)) / Math.pow(10, this.decPlaces)
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

            data.edges.push({
                id: knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol,
                target: knowngenes[i].foundGene.officialSymbol,
                source: knowngenes[i].queryGene.officialSymbol,
                positivesupport: knowngenes[i].posSupp,
                negativesupport: knowngenes[i].negSupp,
                support: support,
                supportsign: supportsign
            });

        }

        var qlength = qgenes.length;

        var isQueryGene = false;

        //add query gene nodes NOT in knowngenes, node degree set to zero
        var i;
        for (i = 0; i < qlength; i++) {

            if (graphNodeIds.indexOf(qgenes[i].id) === -1) {

                graphNodeIds.push(qgenes[i].id);

                //check if this gene was part of current/previous query
                if (this.currentQueryGeneIds.indexOf(qgenes[i].id) !== -1) {
                    isQueryGene = true;
                }

                data.nodes.push({
                    id: qgenes[i].officialSymbol,
                    label: qgenes[i].officialSymbol,
                    geneid: qgenes[i].id,
                    queryflag: isQueryGene,
                    officialName: qgenes[i].officialName,
                    ncbiId: qgenes[i].ncbiId,
                    nodeDegree: 0
                });

                isQueryGene = false;
            }
        }

        this.currentNodeGeneIds = graphNodeIds;

        return data;

    },

    drawGraph: function () {

        var dataMsg = {

            dataSchema: this.dataSchemaJSON,

            data: this.dataJSON

        }

        // init and draw
        this.visualization.draw({
            network: dataMsg,
            visualStyle: this.visual_style,
            layout: "ForceDirected"
        });

    }

});