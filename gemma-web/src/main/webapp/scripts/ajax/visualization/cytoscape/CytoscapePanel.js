Ext.namespace('Gemma');

Gemma.CytoscapePanel = Ext.extend(
Ext.Panel, {

    title: 'Cytoscape',
    layout: 'fit',    
    currentResultsStringency: 2,    
    initialDisplayStringency: 2,

    currentNodeGeneIds: [],
    currentQueryGeneIds: [],    

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

        Ext.apply(
        this, {
            tbar: controlBar,
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
                    this.currentbbarText = null;
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
            items: [this.display]
        });

        Gemma.CytoscapePanel.superclass.initComponent.apply(
        this, arguments);

        this.on('doneDrawingCytoscape', function () {

            //do initial filtering of graph if necessary
            if (this.initialDisplayStringency > this.currentResultsStringency) {
                var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResults(this.knownGeneResults, this.currentQueryGeneIds, this.initialDisplayStringency);
                this.display.filter(this.initialDisplayStringency, trimmed.trimmedNodeIds, true);
            } else if (!this.display.initialZoomLevel) {
                this.display.visualization.zoomToFit();
            }

        }, this);


        this.addEvents('stringencyUpdateFromCoexpressionViz', 'dataUpdateFromCoexpressionViz', 'queryUpdateFromCoexpressionViz');
        this.relayEvents(this.display, ['doneDrawingCytoscape']);

    },

    stringencyChange: function (stringencyValue) {

        if (stringencyValue < 2) {
            stringencyValue = 2;
        }

        if (stringencyValue >= this.currentResultsStringency) {
            this.stringencyChangeHandler(stringencyValue);
        } else {
            //new search            	
            this.newSearchForLowerStringencyHandler(stringencyValue);
        }

    },

    stringencyChangeHandler: function (stringencyValue) {

        this.display.updateStringency(stringencyValue);

        var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResults(this.knownGeneResults, this.currentQueryGeneIds, stringencyValue);

        this.fireEvent('stringencyUpdateFromCoexpressionViz', trimmed.trimmedKnownGeneResults, stringencyValue);
        this.display.filter(stringencyValue, trimmed.trimmedNodeIds)
    },

    newSearchForLowerStringencyHandler: function (stringencyValue) {

        Ext.Msg.show({
            title: 'New Search',
            msg: Gemma.HelpText.WidgetDefaults.CytoscapePanel.lowStringencyWarning,
            buttons: {
                ok: 'Proceed',
                cancel: 'Cancel'
            },
            fn: function (btn) {
                if (btn == 'ok') {
                    this.currentbbarText = null;
                    this.getBottomToolbar().hide();
                    this.doLayout();

                    var resultsStringency = Gemma.CytoscapePanelUtil.restrictResultsStringency(stringencyValue);

                    Ext.apply(
                    this.coexCommand, {
                        stringency: resultsStringency,
                        displayStringency: stringencyValue,
                        geneIds: this.currentQueryGeneIds,
                        queryGenesOnly: false
                    });

                    this.loadMask.show();
                    ExtCoexpressionSearchController.doSearchQuick2(
                    this.coexCommand, {
                        callback: this.initialCoexSearchCallback.createDelegate(this)

                    });
                } else {
                    this.display.updateStringency();
                }
            }.createDelegate(this)
        });

    },

    reRunSearchWithSelectedNodes: function (selectedNodes) {

        this.clearError();

        if (selectedNodes.length > 0 && selectedNodes.length <= Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {
            this.getBottomToolbar().setVisible(false);
            this.doLayout();
            this.currentbbarText = null;

            var displayStringency = this.display.getStringency();
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

    },

    extendNodes: function (selectedNodes) {

        this.clearError();

        if (selectedNodes.length > 0 && selectedNodes.length <= Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {

            if (this.currentQueryGeneIds.length + selectedNodes.length <= Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {
                this.extendNodesStepTwo(selectedNodes);
            } else {
                Ext.Msg.confirm(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, String.format(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooManyReduce, Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY), function (btn) {
                    if (btn == 'yes') {
                        // make room in currentQueryGeneIds for new genes
                        this.currentQueryGeneIds = this.currentQueryGeneIds.splice(this.currentQueryGeneIds.length - (Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY - selectedNodes.length));
                        this.extendNodesStepTwo(selectedNodes);
                    }
                }, this);
            }

        } else if (selectedNodes.length > Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {
            Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, String.format(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooMany, Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY));
        } else {
            Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle, Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooFew);
        }

    },

    extendNodesStepTwo: function (selectedNodes) {
        
        var extendedNodesGeneIdArray = [];
        var sNodesLength = selectedNodes.length;
        var i;
        for (i = 0; i < sNodesLength; i++) {
            extendedNodesGeneIdArray[i] = selectedNodes[i].data.geneid;

            if (this.currentQueryGeneIds.indexOf(selectedNodes[i].data.geneid) === -1) {
                this.currentQueryGeneIds.push(selectedNodes[i].data.geneid);
            }
        }

        this.currentbbarText = null;
        this.getBottomToolbar().setVisible(false);
        this.doLayout();

        this.updateSearchFormGenes(this.currentQueryGeneIds);

        Ext.apply(this.coexCommand, {
            stringency: Gemma.CytoscapePanelUtil.restrictResultsStringency(this.display.getStringency()),
            displayStringency: this.display.getStringency(),
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
        this.display.updateStringency(this.coexCommand.displayStringency);

        if (this.coexCommand.displayStringency > Gemma.MIN_STRINGENCY) {
            var bbarText = this.getBottomToolbar().getComponent('bbarStatus');
            this.currentbbarText = "Display Stringency set to " + this.coexCommand.displayStringency + " based on number of experiments chosen.";
            bbarText.setText(this.currentbbarText);
        } else {
            this.getBottomToolbar().hide();
            this.doLayout();
        }

        this.initialCoexSearchCallback(results);
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

        var kglength = result.knownGeneResults.length;
        var completeSearchFlag = false;
        var i;
        for (i = 0; i < kglength; i++) {
            if (this.currentNodeGeneIds.indexOf(result.knownGeneResults[i].foundGene.id) === -1) {
                completeSearchFlag = true;
                this.currentNodeGeneIds.push(result.knownGeneResults[i].foundGene.id);
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
        this.knownGeneResults = Gemma.CoexValueObjectUtil.removeDuplicates(result.knownGeneResults);
        this.fireEvent('dataUpdateFromCoexpressionViz', this.knownGeneResults, this.currentQueryGeneIds, this.currentResultsStringency, this.initialDisplayStringency);        
        this.display.initialZoomLevel = null;
        this.display.drawGraph(this.constructDataJSON(this.queryGenes, this.knownGeneResults));
        this.showUserMessageBar(result.displayInfo);
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

    clearError: function () {
        if (Ext.get("analysis-results-search-form-messages")) {
            Ext.DomHelper.overwrite("analysis-results-search-form-messages", "");
        }
    },

    showUserMessageBar: function (displayInfo) {
        var bbarText = this.getBottomToolbar().getComponent('bbarStatus');

        if (displayInfo) {

            if (this.currentbbarText) {
                bbarText.setText(this.currentbbarText + ' ' + displayInfo);
            } else {
                bbarText.setText(displayInfo);
            }
            if (!this.getBottomToolbar().isVisible()) {
                this.getBottomToolbar().setVisible(true);
                this.doLayout();
            }
        } else if (this.currentbbarText) {

            bbarText.setText(this.currentbbarText);

            if (!this.getBottomToolbar().isVisible()) {
                this.getBottomToolbar().setVisible(true);
                this.doLayout();
            }
        } else {

            if (this.getBottomToolbar().isVisible()) {
                this.getBottomToolbar().setVisible(false);
                this.doLayout();
            }

        }

    },

    updateSearchFormGenes: function (geneIds) {
        //this collects all the query Genevalueobjects and fires an event to let the search form listening know that the query has been changed.
        //We already have the geneValueObjects from the search results so this saves an extra call to the backend
        //because the search form usually queries the backend for this information
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
        this.fireEvent('queryUpdateFromCoexpressionViz', genesToPreview, genesToPreviewIds, this.taxonId, this.taxonName);

    },

    //Coex grid calls this function to update
    stringencyUpdate: function (stringency, trimmed) {
        this.display.updateStringency(stringency);
        this.display.filter(stringency, trimmed.trimmedNodeIds);
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