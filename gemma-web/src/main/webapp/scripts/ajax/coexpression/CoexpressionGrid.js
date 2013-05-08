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
 * Grid for showing coexpression results.
 *
 * @class Gemma.CoexpressionGrid
 * @extends Ext.grid.GridPanel
 *
 */
Gemma.SHOW_ONLY_MINE = "Show only my data";
Gemma.SHOW_ALL = "Show all results";

/**
 *
 * @param {} observableDisplaySettings
 * @param {} observableSearchResults
 *
 * @type {*}
 */
Gemma.CoexpressionGrid = Ext.extend(Ext.grid.GridPanel, {
    collapsible: false,
    editable: false,
    style: "margin-bottom: 1em;",
    height: 300,
    autoScroll: true,
    stateful: false,

    observableDisplaySettings: {},
    observableSearchResults: {},

    viewConfig: {
        forceFit: true,
        emptyText: 'No coexpressed genes to display'
    },

    record: Gemma.CoexpressionGridRecordConstructor,

    initComponent: function () {
        this.ds = new Ext.data.Store({
            proxy: new Ext.data.MemoryProxy([]),
            reader: new Ext.data.ListRangeReader({
                id: "id"
            }, this.record),
            sortInfo: {
                field: 'sortKey',
                direction: 'ASC'
            }
        });

        Ext.apply(this, {
            columns: [
                {
                    id: 'query',
                    header: "Query Gene",
                    hidden: true,
                    dataIndex: "queryGene",
                    tooltip: "Query Gene",
                    renderer: this.queryGeneStyler.createDelegate(this),
                    sortable: true
                },
                {
                    id: 'found',
                    header: "Coexpressed Gene",
                    dataIndex: "foundGene",
                    renderer: this.foundGeneStyler.createDelegate(this),
                    tooltip: "Coexpressed Gene",
                    sortable: true
                },
                {
                    id: 'support',
                    header: "Support",
                    dataIndex: "supportKey",
                    width: 75,
                    renderer: this.supportStyler.createDelegate(this),
                    tooltip: Gemma.HelpText.WidgetDefaults.CoexpressionGrid.supportColumnTT,
                    sortable: true
                },
                {
                    id: 'nodeDegree',
                    header: "Specificity",
                    dataIndex: "foundGeneNodeDegree",
                    width: 60,
                    renderer: this.nodeDegreeStyler.createDelegate(this),
                    tooltip: "Specificity",
                    sortable: true
                },
                {
                    id: 'visualize',
                    header: "Visualize",
                    dataIndex: "visualize",
                    renderer: this.visStyler.createDelegate(this),
                    tooltip: "Link for visualizing raw data",
                    sortable: false,
                    width: 35
                },
                {
                    id: 'go',
                    header: "GO Overlap",
                    dataIndex: "goSim",
                    width: 75,
                    renderer: this.goStyler.createDelegate(this),
                    tooltip: "GO Similarity Score",
                    sortable: true,
                    hidden: true
                },
                {
                    id: 'linkOut',
                    dataIndex: "foundGene",
                    header: "More",
                    sortable: false,
                    width: 30,
                    tooltip: "Links to other websites for more information",
                    renderer: this.linkOutStyler,
                    hidden: true
                }
            ]
        });

        Ext.apply(this, {
            tbar: new Ext.Toolbar({
                items: [
                    {
                        xtype: 'tbtext',
                        text: 'Stringency:'
                    },
                    ' ',
                    {
                        xtype: 'spinnerfield',
                        itemId: 'stringencySpinner',
                        decimalPrecision: 1,
                        incrementValue: 1,
                        accelerate: false,
                        ref: 'stringencyfield',
                        allowBlank: false,
                        allowDecimals: false,
                        allowNegative: false,
                        minValue: Gemma.MIN_STRINGENCY,
                        maxValue: 999,
                        fieldLabel: 'Stringency ',
                        value: this.observableDisplaySettings.getStringency(),
                        width: 60,
                        enableKeyEvents: true,
                        listeners: {
                            /* TODO: pass new value to handler. */
                            'spin': {
                                fn: this.onStringencyChange,
                                scope: this
                            },
                            'keyup': {
                                fn: this.onStringencyChange,
                                scope: this,
                                delay: 500
                            }
                        }
                    },
                    {
                        xtype: 'label',
                        html: '&nbsp&nbsp<img ext:qtip="' +
                            Gemma.HelpText.WidgetDefaults.CoexpressionGrid.stringencySpinnerTT +
                            '" src="/Gemma/images/icons/question_blue.png"/>',
                        height: 15
                    },
                    {
                        xtype: 'tbspacer'
                    },
                    ' ',
                    ' ',
                    {
                        xtype: 'textfield',
                        ref: 'searchInGrid',
                        id: this.id + '-search-in-grid',
                        tabIndex: 1,
                        enableKeyEvents: true,
                        emptyText: 'Find gene in results',
                        listeners: {
                            "keyup": {
                                fn: function (textField) {
                                    this.observableDisplaySettings.setSearchTextValue(textField.getValue());
                                },
                                scope: this,
                                delay: 400
                            }
                        }
                    },
                    ' ',
                    ' ',
                    {
                        xtype: 'checkbox',
                        itemId: 'queryGenesOnly',
                        boxLabel: 'Query Genes Only',
                        handler: this.onQueryGenesOnlyChange,
                        checked: false,
                        scope: this
                    },
                    {
                        xtype: 'tbspacer',
                        width: '180',
                        ref: 'arbitraryTutorialTooltip1'
                    },
                    {
                        xtype: 'tbspacer',
                        width: '180',
                        ref: 'arbitraryTutorialTooltip2'
                    },
                    {
                        xtype: 'tbspacer',
                        ref: 'arbitraryTutorialTooltip3'
                    },
                    '->',
                    '-',
                    {
                        xtype: 'button',
                        text: '<b>Download</b>',
                        tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.widgetHelpTT,
                        handler: this.exportData,
                        scope: this
                    }
                ]
            }),
            // end tbar
            bbar: [
                {
                    xtype: 'tbtext',
                    text: '',
                    itemId: 'bbarStatus'
                },
                '->',
                {
                    xtype: 'button',
                    icon: "/Gemma/images/icons/cross.png",
                    itemId: 'bbarClearButton',
                    handler: function () {
                        this.hideBottomToolbar();
                    },
                    scope: this
                }
            ]
        });

        Gemma.CoexpressionGrid.superclass.initComponent.call(this);

        var coexpressionGrid = this;

        this.observableSearchResults.on("search-results-ready", function (results) {
            var displayStringency = coexpressionGrid.observableDisplaySettings.getStringency();
            if (displayStringency > Gemma.MIN_STRINGENCY) {
                var bbarText = coexpressionGrid.getBottomToolbar().getComponent('bbarStatus');
                bbarText.setText("Display Stringency set to " + displayStringency + " based on number of experiments chosen.");
            } else {
                coexpressionGrid.hideBottomToolbar();
            }

            coexpressionGrid.loadData(results.queryGenes.length, results.knownGeneResults, null);
            coexpressionGrid.applyFilters();
        });

        this.observableSearchResults.on('query-genes-changed', function (queryGeneIds) {
            if (queryGeneIds.length < 2) {
                coexpressionGrid.getTopToolbar().getComponent('queryGenesOnly').setDisabled(true);
                coexpressionGrid.getTopToolbar().getComponent('queryGenesOnly').setValue(false);
            } else {
                coexpressionGrid.getTopToolbar().getComponent('queryGenesOnly').setDisabled(false);
            }
        });

        this.observableDisplaySettings.on("stringency_change", function (displayStringency) {
            coexpressionGrid.getTopToolbar().getComponent('stringencySpinner').setValue(displayStringency);
            coexpressionGrid.applyFilters();
        });

        this.observableDisplaySettings.on('query_genes_only_change', function (checked) {
            coexpressionGrid.getTopToolbar().getComponent('queryGenesOnly').setValue(checked);
            coexpressionGrid.applyFilters();
        });

        this.observableDisplaySettings.on('search_text_change', function (text) {
            coexpressionGrid.getTopToolbar().getComponent(coexpressionGrid.id + '-search-in-grid').setValue(text);
            coexpressionGrid.applyFilters();
        });

        this.on("cellclick", coexpressionGrid.rowClickHandler);
    },

    // called from toolbar
    onStringencyChange: function () {
        var spinnerValue = this.getTopToolbar().getComponent('stringencySpinner').getValue();
        if (Ext.isNumber(spinnerValue) && spinnerValue > 1) {
            this.observableDisplaySettings.setStringency(spinnerValue);
            this.applyFilters();
        }
    },

    // called from toolbar
    onQueryGenesOnlyChange: function () {
        this.observableDisplaySettings.setQueryGenesOnly(this.getTopToolbar().getComponent('queryGenesOnly').getValue());
        this.applyFilters();
    },

    applyFilters: function () {
        this.getStore().filterBy(this.filter(), this);
    },

    hideBottomToolbar: function () {
        if (!this.getBottomToolbar().hidden) {
            this.getBottomToolbar().hide();
            this.doLayout();
        }
    },

    /**
     *
     * @param numQueryGenes
     * @param data
     * @param datasets
     */
    loadData: function (numQueryGenes, data, datasets) {
        var queryIndex = this.getColumnModel().getIndexById('query');
        if (numQueryGenes > 1) {
            this.getColumnModel().setHidden(queryIndex, false);
        } else {
            this.getColumnModel().setHidden(queryIndex, true);

        }
        this.getStore().proxy.data = data;
        this.getStore().reload({
            resetPage: true
        });

        this.datasets = datasets; // the datasets that are 'relevant'.

        if (this.loadMask) {
            this.loadMask.hide();
        }
    },

    /**
     * Load the data if there is no data returned an errorState message is set on the result to indicate what the exact
     * problem was.
     * @param result
     */
    loadDataCb: function (result) {
        if (result.errorState) {
            this.handleError(result.errorState);
        } else {
            this.loadData(
                result.queryGenes.length,
                result.knownGeneResults,
                result.knownGeneDatasets);
        }
    },

    /**
     * Checks if store contains any results if not print message indicating that there are non. Stop loader. Called when
     * an error thrown of after data load processing
     */
    handleError: function (errorMessage) {
        if (Ext.get('coexpression-msg')) {
            Ext.DomHelper.applyStyles("coexpression-msg", "height: 2.2em");
            Ext.DomHelper.overwrite("coexpression-msg", [
                {
                    tag: 'img',
                    src: '/Gemma/images/icons/information.png'
                },
                {
                    tag: 'span',
                    html: "&nbsp;&nbsp;" + errorMessage
                }
            ]);
        } else {
            Ext.Msg.alert("Warning", errorMessage);
            this.getView().refresh(); //show empty text
        }
        this.loadMask.hide();
    },

    clearError: function () {
        Ext.DomHelper.overwrite("coexpression-messages", "");
    },

    filter: function () {
        var text = Ext.getCmp(this.id + '-search-in-grid').getValue();

        var stringency = this.observableDisplaySettings.getStringency();
        var queryGenesOnly = this.observableDisplaySettings.getQueryGenesOnly();
        var queryGeneIds = this.observableSearchResults.getQueryGeneIds();

        var value;

        if (text && text.length > 1) {
            value = new RegExp(Ext.escapeRe(text), 'i');
        }

        return function filterFn (record) {

            if (record.get("supportKey") < stringency) {
                return false;
            }

            if (queryGenesOnly) {
                if (! (queryGeneIds.indexOf(record.get('queryGene').id) !== -1 &&
                       queryGeneIds.indexOf(record.get('foundGene').id) !== -1) )
                {
                    return false;
                }
            }

            if (value) {
                var foundGene = (record.get("foundGene"));
                var queryGene = (record.get("queryGene"));

                if (value.test(foundGene.officialSymbol) ||
                    value.test(queryGene.officialSymbol) ||
                    value.test(foundGene.officialName) ||
                    value.test(queryGene.officialName))
                {
                    return true;
                } else {
                    return false;
                }
            }

            return true;
        };
    },

    linkOutStyler: function (value, metadata, record, row, col, ds) {

        var call = "Gemma.CoexpressionGrid.getAllenAtlasImage(\'" + value.officialSymbol + "\')";

        return String
            .format(
                '<span onClick="{0}" id="aba-{1}-button"><img height=15 width=15 src="/Gemma/images/logo/aba-icon.png"' +
                    ' ext:qtip="Link to expression data from the Allen Brain Atlas for {2}" />' + '</span>',
                call, value.officialSymbol, value.officialSymbol);
    },

    // link for protein interactions
    proteinLinkStyler: function (value, metadata, record, row, col, ds) {
        var data = record.data;
        var result = "";

        if (data['gene2GeneProteinAssociationStringUrl']) {
            result = String
                .format(
                    '<span>' +
                        '<a href="{0}"  target="_blank" class="external">' +
                        '<img src="/Gemma/images/logo/string_logo.gif" ' +
                        'ext:qtip="Click to view the protein protein interaction obtained from {1} ' +
                        'evidence with a combined association score of {2} from STRING" />' +
                        '</a>' +
                        '</span>',
                    data['gene2GeneProteinAssociationStringUrl'],
                    data['gene2GeneProteinInteractionEvidence'],
                    data['gene2GeneProteinInteractionConfidenceScore']);
        }
        if (data['queryRegulatesFound']) {
            result = result + " " + '<span> <img height="16" width = "16" src="/Gemma/images/logo/pazar-icon.png"' +
                ' ext:qtip="Query may regulate the coexpressed gene, according to Pazar" />' + '</span>';
        } else if (data['foundRegulatesQuery']) {
            result = result + " " + '<span> <img height="16" width = "16" src="/Gemma/images/logo/pazar-icon.png"' +
                ' ext:qtip="The query may be regulated by the coexpressed gene, according to Pazar" />' + '</span>';
        }
        return result;
    },

    nodeDegreeStyler: function (value, metadata, record, row, col, ds) {
        var data = record.data;
        // display the 'worst' (highest) node degree
        var displayedNodeDegree;

        if (data['foundGeneNodeDegree'] === null) {
            return 0;
        } else if (data['queryGeneNodeDegree'] > data['foundGeneNodeDegree']) {
            displayedNodeDegree = data['queryGeneNodeDegree'];
        } else {
            displayedNodeDegree = data['foundGeneNodeDegree'];
        }
        return Gemma.CytoscapePanelUtil.nodeDegreeBinMapper(displayedNodeDegree);
    },

    /**
     *
     */
    supportStyler: function (value, metadata, record, row, col, ds) {
        var data = record.data;
        if (data['posSupp'] || data['negSupp']) {
            var style = "";
            if (data['posSupp']) {
                style = style +
                    String.format("<span class='positiveLink'>{0}{1}</span> ",
                        data['posSupp'],
                        this.getSpecificLinkString(data['posSupp'], data['nonSpecPosSupp']));
            }
            if (data['negSupp']) {
                style = style +
                    String.format("<span class='negativeLink'>{0}{1}</span> ",
                        data['negSupp'], this.getSpecificLinkString(data['negSupp'], data['nonSpecNegSupp']));
            }

            if (data['numTestedIn']) {
                style = style + String.format("/ {0}", data['numTestedIn']);
            }
            return style;
        } else {
            return "-";
        }
    },

    /**
     * For displaying Gene ontology similarity
     */
    goStyler: function (value, metadata, record, row, col, ds) {
        var data = record.data;
        if (data['goSim'] || data['maxGoSim']) {
            return String.format("{0}/{1}", data['goSim'], data['maxGoSim']);
        } else {
            return "-";
        }
    },

    getSpecificLinkString: function (total, nonSpecific) {
        return nonSpecific ? String.format("<span class='specificLink'> ({0})</span>", total - nonSpecific) : "";
    },

    /**
     * Display the target (found) genes.
     *
     */
    foundGeneStyler: function (value, metadata, record, row, col, ds) {
        var gene = record.data.foundGene;

        if (gene.officialName === null) {
            gene.officialName = "";
        }

        if (gene.taxonId !== null) {
            gene.taxonId = gene.taxonId;
            gene.taxonName = gene.taxonCommonName;
        } else {
            gene.taxonId = -1;
            gene.taxonName = "?";
        }

        if (this.observableSearchResults.getQueryGeneIds().indexOf(gene.id) !== -1) {
            gene.fontWeight = 'bold';
        }
        return this.foundGeneTemplateNoGemma.apply(gene);
    },

    /**
     * FIXME this should use the same analysis as the last query. Here we always use 'All'.
     */
    foundGeneTemplate: new Ext.Template(
        "<a href='/Gemma/searchCoexpression.html?g={id}&s=3&t={taxonId}&an=All {taxonName}'> <img src='/Gemma/images/logo/gemmaTiny.gif' ext:qtip='Make {officialSymbol} the query gene' /> </a>",
        " &nbsp; ", "<a target='_blank' href='/Gemma/gene/showGene.html?id={id}'>{officialSymbol}</a> {officialName}"),

    foundGeneTemplateNoGemma: new Ext.Template("<a style='font-weight:{fontWeight};' target='_blank' href='/Gemma/gene/showGene.html?id={id}'>{officialSymbol}</a> {officialName}"),

    queryGeneStyler: function (value, metadata, record, row, col, ds) {
        var gene = record.data.queryGene;
        if (gene.officialName === null) {
            gene.officialName = "";
        }
        gene.abaGeneUrl = record.data.abaQueryGeneUrl;
        gene.fontWeight = 'bold';
        return this.foundGeneTemplateNoGemma.apply(gene);
    },

    visStyler: function (value, metadata, record, row, col, ds) {
        return "<img src='/Gemma/images/icons/chart_curve.png' ext:qtip='Visualize the data' />";
    },

    /**
     *
     * @param grid
     * @param rowIndex
     * @param columnIndex
     */
    rowClickHandler: function (grid, rowIndex, columnIndex) {
        if (this.getSelectionModel().hasSelection()) {

            var record = this.getStore().getAt(rowIndex);
            var fieldName = this.getColumnModel().getDataIndex(columnIndex);
            var queryGene = record.get("queryGene");
            var foundGene = record.get("foundGene");

            if (fieldName === 'visualize') {
                var activeExperiments = record.data['supportingExperiments'];

                var coexpressionVisualizationWindow = new Gemma.CoexpressionVisualizationWindow({
                    cascadeOnFirstShow: true,
                    admin: false,
                    experiments: activeExperiments,
                    queryGene: queryGene,
                    foundGene: foundGene,
                    downloadLink: String.format("/Gemma/dedv/downloadDEDV.html?ee={0}&g={1},{2}",
                        activeExperiments.join(','), queryGene.id, foundGene.id),
                    title: "Coexpression for:  " + queryGene.name + " + " + foundGene.name
                });

                var params = [];
                params.push(activeExperiments);
                params.push(queryGene.id);
                params.push(foundGene.id);

                coexpressionVisualizationWindow.show({
                    params: params
                });
            }
        }
    },

    // TODO: would it be simpler just returning items currently in the store? i.e. let the grid do all the filtering?
    exportData: function () {
        var filteredData;
        var queryGenesOnly = this.observableDisplaySettings.getQueryGenesOnly();
        var stringency = this.observableDisplaySettings.getStringency();

        if (queryGenesOnly) {
            filteredData = Gemma.CoexValueObjectUtil.trimKnownGeneResults(
                this.observableSearchResults.getQueryGenesOnlyResults(),
                stringency);
        } else {
            var combinedData = this.observableSearchResults.getCoexpressionPairs();
            if (this.observableSearchResults.getQueryGenesOnlyResults()) {
                combinedData = Gemma.CoexValueObjectUtil.combineKnownGeneResultsAndQueryGeneOnlyResults(
                    this.observableSearchResults.getCoexpressionPairs(),
                    this.observableSearchResults.getQueryGenesOnlyResults());
            }
            filteredData = Gemma.CoexValueObjectUtil.trimKnownGeneResults(combinedData, stringency);
        }

        var text = Ext.getCmp(this.id + '-search-in-grid').getValue();
        if (text.length > 1) {
            filteredData = Gemma.CoexValueObjectUtil.filterGeneResultsByText(text, filteredData);
        }

        var win = new Gemma.CoexpressionDownloadWindow({
            title: "Coexpression Data"
        });
        win.convertText(filteredData);
    }
});

//Gemma.CoexpressionGrid.getAllenAtlasImage = function (geneSymbol) {
//    LinkOutController.getAllenBrainAtlasLink(geneSymbol, Gemma.CoexpressionGrid.linkOutPopUp);
//
//    /*
//     * Show the throbber
//     */
//    Ext.DomHelper.overwrite("aba-" + geneSymbol + "-button", {
//        tag: 'img',
//        src: '/Gemma/images/default/tree/loading.gif'
//    });
//};
//
///**
// * Callback.
// */
//Gemma.CoexpressionGrid.linkOutPopUp = function (linkOutValueObject) {
//
//    /*
//     * Put the aba icon back for the throbber.
//     */
//    Ext.DomHelper.overwrite("aba-" + linkOutValueObject.geneSymbol + "-button", {
//        tag: 'img',
//        src: '/Gemma/images/logo/aba-icon.png'
//    });
//
//    // TODO: Make pop up window show more than one image (have a button for
//    // scrolling to next image)
//    var popUpHtml;
//
//    if (linkOutValueObject.abaGeneImageUrls.length === 0) {
//        window.alert("No Allen Brain Atlas images available for this gene");
//        return;
//    } else {
//        popUpHtml = String.format("<img height=200 width=400 src={0}>", linkOutValueObject.abaGeneImageUrls[0]);
//    }
//
//    var abaWindowId = "coexpressionAbaWindow";
//
//    var popUpLinkOutWin = Ext.getCmp(abaWindowId);
//    if (popUpLinkOutWin !== undefined && popUpLinkWin !== null) {
//        popUpLinkOutWin.close();
//        popUpLinkOutWin = null;
//    }
//
//    popUpLinkOutWin = new Ext.Window({
//        id: abaWindowId,
//        html: popUpHtml,
//        stateful: false,
//        resizable: false
//    });
//    popUpLinkOutWin
//        .setTitle("<a href='"
//            + linkOutValueObject.abaGeneUrl
//            + "' target='_blank'>  <img src='/Gemma/images/logo/aba-icon.png' ext:qtip='Link to Allen Brain Atlas gene details' />  </a> &nbsp; &nbsp;<img height=15  src=/Gemma/images/abaExpressionLegend.gif>  "
//            + linkOutValueObject.geneSymbol);
//
//    // An attempt at adding a button to the window so that the different image
//    // from allen brain atlas could be seen clicking on it.
//    // failed to work because window wouldn't refresh with new html information.
//    // :(
//    // Also was a host of scope issue... should have made in own widget.
//    // popUpLinkOutWin.linkOutValueObject = linkOutValueObject;
//    // popUpLinkOutWin.currentImageIndex = 0;
//    //
//    // popUpLinkOutWin.nextImage = function(e){
//    //
//    // if (e.scope.currentImageIndex ==
//    // e.scope.linkOutValueObject.abaGeneImageUrls.length)
//    // e.scope.currentImageIndex = 0;
//    // else
//    // e.scope.currentImageIndex++;
//    //
//    // e.scope.innerHTML= String.format("<img height=200 width=400 src={0}>",
//    // e.scope.linkOutValueObject.abaGeneImageUrls[e.scope.currentImageIndex]);
//    // e.scope.render();
//    //
//    // };
//    // popUpLinkOutWin.addButton('next image',
//    // popUpLinkOutWin.nextImage.createDelegate(this), popUpLinkOutWin);
//
//    popUpLinkOutWin.show(this);
//};