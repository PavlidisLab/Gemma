/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
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
 *
 * observableSearchResults
 * observableDisplaySettings
 *
 * @class
 */
Gemma.CytoscapePanel = Ext.extend(Ext.Panel, {
    title: 'Cytoscape',
    layout: 'fit',

    // This is for a bug in ExtJS tabPanel that causes an unactivated Panel in a TabPanel to be rendered
    // when the Panel is removed from the tabPanel
    stopRender: false,

    initComponent: function () {
        this.display = new Gemma.CytoscapeDisplay({
            id: 'cytoscapeweb',
            cytoscapePanel: this,
            listeners: {
                afterrender: {
                    fn: function () { /* TODO: is there cleaner way? move inside CytoscapeDisplay */
                        // stopRender is needed because of a bug in extjs where a tabpanel renders its components upon removal
                        if (!this.stopRender) {
                            this.loadMask = new Ext.LoadMask(this.getEl(), {
                                msg: Gemma.StatusText.Searching.analysisResults,
                                msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                            });
                        }
                    },
                    scope: this
                }
            },
            observableSearchResults: this.observableSearchResults,
            observableDisplaySettings: this.observableDisplaySettings
        });

        this.controlBar = new Gemma.CytoscapeControlBar({
            observableSearchResults: this.observableSearchResults,
            observableDisplaySettings: this.observableDisplaySettings,
            display: this.display,
            cytoscapePanel: this
        });

        var bottomToolbar = new Gemma.CytoscapeBottomToolbar({
            cytoscapePanel: this
        });

        Ext.apply(this, {
            tbar: this.controlBar,
            bbar: bottomToolbar,
            margins: {
                top: 0,
                right: 0,
                bottom: 0,
                left: 0
            },
            items: [this.display]
        });

        Gemma.CytoscapePanel.superclass.initComponent.apply(this, arguments);

        // Extjs event, fired by TabPanel when this tab is activated/displayed.
        this.on('activate', this.onPanelActivation, this);

        this.on('complete-search-results-ready', function (searchResults, cytoscapSearchCommmand) {
            // TODO: move this to controlbar? or just delete?
            if (this.observableSearchResults.getQueryGeneIds().length < 2) {
                this.observableDisplaySettings.setQueryGenesOnly(false);
                this.controlBar.disableQueryGenesOnlyCheckBox(true);
            } else {
                this.controlBar.disableQueryGenesOnlyCheckBox(false);
            }

            this.coexpressionGraphData = new Gemma.CoexpressionGraphData(
                searchResults,
                cytoscapSearchCommmand,
                this.observableSearchResults.getQueryGeneIds());

            // If we had to trim the graph on front-end or back-end, communicate this to the user.
            if (this.coexpressionGraphData.graphSizeOptions.length > 1) {
                this.changeGraph( this.coexpressionGraphData.getSmallestGraph() );
                bottomToolbar.showMessageBar(this.coexpressionGraphData, this.observableSearchResults.getNonQueryGeneTrimmedValue(), /*show menu*/ true);
            } else if (this.coexpressionGraphData.isTrimmedOnBackend) {
                this.drawGraph();
                bottomToolbar.showMessageBar(this.coexpressionGraphData, this.observableSearchResults.getNonQueryGeneTrimmedValue(), /*no menu*/ false);
            } else {
                this.drawGraph();
                this.getBottomToolbar().hide();
                this.doLayout();
            }
        }, this);

        this.observableSearchResults.on('aftersearch', function() {
            if (this.loadMask) {
                this.loadMask.hide();
            }
        }, this);

        this.on('search-error', function (error) {
            Ext.Msg.alert(Gemma.HelpText.CommonWarnings.Timeout.title, Gemma.HelpText.CommonWarnings.Timeout.text);
            this.loadMask.hide();
            this.fireEvent('beforesearch');
        }, this);

        this.addEvents('queryUpdateFromCoexpressionViz',
            'coexWarningAlreadyDisplayed');

        this.relayEvents(this.observableSearchResults,
            ['search-results-ready',
                'complete-search-results-ready',
                'search-error']);

        if (this.searchPanel) {
            this.searchPanel.relayEvents(this, ['queryUpdateFromCoexpressionViz', 'beforesearch']);
        }
    },

    /**
     *
     */
    searchForCytoscapeData: function () {
        this.loadMask.show();
        this.observableSearchResults.searchForCytoscapeData();
    },

    /**
     * @private
     * Since we are a panel inside tabpanel [grid, visualization].
     * This is run when user selects visualization tab.
     *
     */
    onPanelActivation: function () {
        if (!this.observableSearchResults.cytoscapeResultsUpToDate) {
            this.loadMask.show();
            this.observableSearchResults.searchForCytoscapeData();
            return;
        }

        // check to see if coexGrid display stringency is below cytoscape results stringency, if so, give the user the option of reloading graph
        // at new stringency or returning display to current cytoscape stringency
        var displayStringency = this.observableDisplaySettings.getStringency();
        var resultsStringency = this.observableSearchResults.getResultsStringency();

        if (this.display.ready) {
            if (displayStringency < resultsStringency) {
                Ext.Msg.show({
                    title: 'New Search Required to View Graph at Current Stringency',
                    msg: String.format(
                        Gemma.HelpText.WidgetDefaults.CytoscapePanel.newSearchOrReturnToCurrentStringencyOption,
                        displayStringency,
                        resultsStringency),
                    buttons: {
                        ok: 'Search for new graph data',
                        cancel: 'Use lowest graph stringency'
                    },
                    fn: function (button) {
                        var resultsStringency = this.observableSearchResults.getResultsStringency();
                        if (button === 'ok') {
                            resultsStringency = Gemma.CytoscapePanelUtil.restrictResultsStringency(displayStringency);
                            this.observableSearchResults.searchForCytoscapeDataWithStringency(resultsStringency);
                            this.observableDisplaySettings.setStringency(resultsStringency);
                        } else { // 'cancel'
                            this.observableDisplaySettings.setStringency(resultsStringency);
                        }
                    }.createDelegate(this)
                });
            }
        }
    },

    /**
     *
     */
    searchWithSelectedNodes: function () {
        this.clearError();

        var selectedGeneIds = this.display.getVisibleSelectedGeneIds();
        var currentResultsStringency = this.observableSearchResults.getResultsStringency();

        if (selectedGeneIds.length === 0) {
            Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle,
                Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooFew);
            return;
        }

        if (selectedGeneIds.length > Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {
            Ext.Msg.alert(
                Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle,
                String.format(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooMany,
                    Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY));
            return;
        }

        this.updateSearchFormGenes(selectedGeneIds);
        this.loadMask.show();
        this.observableSearchResults.searchWithGeneIds(selectedGeneIds, currentResultsStringency);
    },

    extendSelectedNodes: function () {
        this.clearError();

        var selectedGeneIds = this.display.getVisibleSelectedGeneIds();
        var queryGeneIds = this.observableSearchResults.getQueryGeneIds();
        var currentResultsStringency = this.observableSearchResults.getResultsStringency();

        // TODO: merge arrays
        // TODO: check if it's the same as original query genes

        function containsAll(needles, haystack) {
            for (var i = 0, len = needles.length; i < len; i++) {
                if (haystack.indexOf(needles[i]) === -1) {
                    return false;
                }
            }
            return true;
        }

        if (selectedGeneIds.length === 0) {
            Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle,
                Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooFew);
            return;
        }

        if (containsAll(selectedGeneIds, queryGeneIds)) {
            Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle,
                Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusNoExtraSelectedForExtend);
            return;
        }

        var i;
        if (( queryGeneIds.length + selectedGeneIds.length ) <= Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY) {
            for (i = 0; i < selectedGeneIds.length; i++) {
                queryGeneIds.push(selectedGeneIds[i]);
            }
            this.updateSearchFormGenes(queryGeneIds);
            this.loadMask.show();
            this.observableSearchResults.searchWithGeneIds( queryGeneIds, currentResultsStringency );
        } else {
            Ext.Msg.confirm(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTitle,
                String.format(Gemma.HelpText.WidgetDefaults.CytoscapePanel.searchStatusTooManyReduce,
                    Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY), function (btn) {
                    if (btn === 'yes') {
                        // Ensure that selectedNodes includes the current query genes plus the newly selected genes
                        // and that the number of querygeneids is less than the max
                        queryGeneIds = selectedGeneIds.splice(selectedGeneIds.length - (Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY - selectedGeneIds.length));
                        var i;
                        for (i = 0; i < selectedGeneIds.length; i++) {
                            if (queryGeneIds.indexOf(selectedGeneIds[i]) === -1) {
                                queryGeneIds.push(selectedGeneIds[i]);
                            }
                        }
                        this.updateSearchFormGenes(queryGeneIds);
                        this.loadMask.show();
                        this.observableSearchResults.searchWithGeneIds( queryGeneIds, currentResultsStringency );
                    }
                }, this);
        }
    },

    /**
     *
     */
    drawGraph: function () {
        this.display.drawGraph(this.observableSearchResults);
        this.loadMask.hide();
    },

    /**
     * FIXME: can we avoid doing this? i.e. can we have error message as an explicit component?
     */
    clearError: function () {
        if (Ext.get("analysis-results-search-form-messages")) {
            Ext.DomHelper.overwrite("analysis-results-search-form-messages", "");
        }
    },

    /**
     *
     *
     * @param geneIds
     */
    updateSearchFormGenes: function (geneIds) {
        // This collects all the query Genevalueobjects and fires an event to let the search form listening know that the query has been changed.
        // We already have the geneValueObjects from the search results so this saves an extra call to the backend
        // because the search form usually queries the backend for this information
        var genesToPreview = [];
        var genesToPreviewIds = [];
        var coexpressionPairs = this.observableSearchResults.getCoexpressionPairs();
        var kglength = coexpressionPairs.length;
        var i;
        for (i = 0; i < kglength; i++) {
            if (genesToPreviewIds.indexOf(coexpressionPairs[i].foundGene.id) === -1 &&
                geneIds.indexOf(coexpressionPairs[i].foundGene.id) !== -1) {
                genesToPreview.push(coexpressionPairs[i].foundGene);
                genesToPreviewIds.push(coexpressionPairs[i].foundGene.id);
            }
            if (genesToPreviewIds.indexOf(coexpressionPairs[i].queryGene.id) === -1 &&
                geneIds.indexOf(coexpressionPairs[i].queryGene.id) !== -1) {
                genesToPreview.push(coexpressionPairs[i].queryGene);
                genesToPreviewIds.push(coexpressionPairs[i].queryGene.id);
            }
        }

        // We have to search through query genes in case none showed up.
        var queryGenes = this.observableSearchResults.getQueryGenes();
        var qglength = queryGenes.length;
        for (i = 0; i < qglength; i++) {
            if (genesToPreviewIds.indexOf(queryGenes[i].id) === -1 &&
                geneIds.indexOf(queryGenes[i].id) !== -1) {
                genesToPreview.push(queryGenes[i]);
                genesToPreviewIds.push(queryGenes[i].id);
            }
        }
        // Add new genes to search from.
        this.fireEvent('queryUpdateFromCoexpressionViz', genesToPreview, genesToPreviewIds, this.taxonId, this.taxonName);
    },

    /**
     *
     * @param [graph]
     */
    changeGraph: function (graph) {
        this.observableSearchResults.setCytoscapeCoexpressionPairs(graph.geneResults);
        this.drawGraph();
    }
});