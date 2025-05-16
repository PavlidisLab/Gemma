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
 * @author Paul (originally)
 * @author cam
 *
 */
Gemma.SHOW_ONLY_MINE = "Show only my data";
Gemma.SHOW_ALL = "Show all results";

Gemma.CoexpressionGrid = Ext
    .extend(
        Ext.grid.GridPanel,
        {
            collapsible: false,
            editable: false,
            style: "margin-bottom: 1em;",
            height: 300,
            autoScroll: true,
            stateful: false,

            coexDisplaySettings: {},
            coexpressionSearchData: {},

            viewConfig: {
                forceFit: true,
                emptyText: 'No coexpression to display'
            },

            /**
             * @memberOf Gemma.CoexpressionGrid
             */
            initComponent: function () {
                // debugger;
                this.ds = new Ext.data.Store({
                    proxy: new Ext.data.MemoryProxy([]),
                    reader: new Ext.data.ListRangeReader({
                        id: "id"
                    }, Gemma.CoexpressionGridRecordConstructor),
                    sortInfo: {
                        field: 'sortKey',
                        direction: 'ASC'
                    }
                });

                Ext.apply(this, {
                    columns: [{
                        id: 'query',
                        header: "Query Gene",
                        hidden: true,
                        dataIndex: "queryGene",
                        tooltip: "Query Gene",
                        renderer: this.queryGeneStyler.createDelegate(this),
                        sortable: true
                    }, {
                        id: 'found',
                        header: "Coexpressed Gene",
                        dataIndex: "foundGene",
                        renderer: this.foundGeneStyler.createDelegate(this),
                        tooltip: "Coexpressed Gene",
                        sortable: true
                    }, {
                        id: 'support',
                        header: "Support",
                        dataIndex: "support",
                        width: 55,
                        renderer: this.supportStyler.createDelegate(this),
                        tooltip: Gemma.HelpText.WidgetDefaults.CoexpressionGrid.supportColumnTT,
                        sortable: true
                    }, {
                        id: 'nodeDegree',
                        header: "Specificity",
                        dataIndex: "queryGeneNodeDegree", // defines sort.
                        width: 60,
                        renderer: this.nodeDegreeStyler.createDelegate(this),
                        tooltip: Gemma.HelpText.WidgetDefaults.CoexpressionGrid.specificityColumnTT,
                        sortable: true
                    }, {
                        id: 'visualize',
                        header: "Visualize",
                        dataIndex: "visualize",
                        renderer: this.visStyler.createDelegate(this),
                        tooltip: "Link for visualizing raw data",
                        sortable: false,
                        width: 35
                    }]
                });

                /*
                 * toolbar
                 */
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
                                value: this.coexDisplaySettings.getStringency(),
                                width: 60,
                                enableKeyEvents: true,
                                listeners: {
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
                                html: '&nbsp&nbsp<img ext:qtip="'
                                + Gemma.HelpText.WidgetDefaults.CoexpressionGrid.stringencySpinnerTT
                                + '" src="' + Gemma.CONTEXT_PATH + '/images/icons/question_blue.png"/>',
                                height: 15
                            }, {
                                xtype: 'tbspacer'
                            }, ' ', ' ', {
                                xtype: 'textfield',
                                ref: 'searchInGrid',
                                id: this.id + '-search-in-grid',
                                tabIndex: 1,
                                enableKeyEvents: true,
                                emptyText: 'Find gene in results',
                                listeners: {
                                    "keyup": {
                                        fn: function (textField) {
                                            this.coexDisplaySettings.setSearchTextValue(textField.getValue());
                                        },
                                        scope: this,
                                        delay: 400
                                    }
                                }
                            }, ' ', ' ', {
                                xtype: 'checkbox',
                                itemId: 'queryGenesOnly',
                                boxLabel: 'Query Genes Only',
                                handler: this.onQueryGenesOnlyChange,
                                checked: false,
                                scope: this
                            }, {
                                xtype: 'tbspacer',
                                width: '180',
                                ref: 'arbitraryTutorialTooltip1'
                            }, {
                                xtype: 'tbspacer',
                                width: '180',
                                ref: 'arbitraryTutorialTooltip2'
                            }, {
                                xtype: 'tbspacer',
                                ref: 'arbitraryTutorialTooltip3'
                            }, '->', '-', {
                                xtype: 'button',
                                text: '<b>Download</b>',
                                tooltip: Gemma.HelpText.WidgetDefaults.CytoscapePanel.widgetHelpTT,
                                handler: this.exportData,
                                scope: this
                            }]
                    }),
                    // end tbar

                    // bottom bar
                    bbar: [{
                        xtype: 'tbtext',
                        text: '',
                        itemId: 'bbarStatus'
                    }, '->', {
                        xtype: 'button',
                        icon: Gemma.CONTEXT_PATH + "/images/icons/cross.png",
                        itemId: 'bbarClearButton',
                        handler: function () {
                            this.hideBottomToolbar();
                        },
                        scope: this
                    }]
                });

                Gemma.CoexpressionGrid.superclass.initComponent.call(this);

                var coexpressionGrid = this;

                /*
                 * Event handler: Respond to search results.
                 */
                this.coexpressionSearchData.on("search-results-ready",
                    function () {
                        var resultsData = coexpressionGrid.coexpressionSearchData.getResults(); // Array
                        var numDatasets = coexpressionGrid.coexpressionSearchData.getNumberOfDatasetsUsable();
                        var initialStringency = coexpressionGrid.coexpressionSearchData.getQueryStringency();

                        // this might have been set on the server.
                        var queryGenesOnly = coexpressionGrid.coexpressionSearchData.searchResults.queryGenesOnly;

                        // Lower stringency until results are visible.
                        // var displayStringency = Gemma.CoexVOUtil.findMaximalStringencyToApply( resultsData,
                        // initialStringency
                        // );
                        var displayStringency = initialStringency;

                        // var k = 50;
                        // if ( numDatasets > k ) {
                        // displayStringency = Gemma.MIN_STRINGENCY + Math.round( numDatasets / k );
                        // }

                        // should cause the spinner to update
                        coexpressionGrid.coexDisplaySettings.setStringency(displayStringency);
                        coexpressionGrid.coexDisplaySettings.setQueryGenesOnly(queryGenesOnly);

                        var bbarText = coexpressionGrid.getBottomToolbar().getComponent('bbarStatus');
                        // if ( displayStringency > Gemma.MIN_STRINGENCY ) {
                        // bbarText.setText( "Display Stringency set to " + displayStringency
                        // + " based on number of experiments used." );
                        // }

                        var numQueryGenes = coexpressionGrid.coexpressionSearchData.searchResults.queryGenes.length;
                        coexpressionGrid.decideQueryColumn(numQueryGenes);

                        if (resultsData.length < 1) {
                            bbarText.setText("No results to display");
                            coexpressionGrid.getBottomToolbar().show();
                        } else {
                            bbarText.setText(resultsData.length + " results from " + numDatasets
                                + " datasets usable in query for " + numQueryGenes + " genes at stringency "
                                + initialStringency);
                            coexpressionGrid.loadData(resultsData);
                        }

                    });

                /**
                 *
                 */
                this.getStore().on('save', function () {
                    coexpressionGrid.applyFilters();
                    if (coexpressionGrid.loadMask) {
                        coexpressionGrid.loadMask.hide();
                    }
                });

                /*
                 * Event handler: respond to change in the query genes
                 */
                this.coexpressionSearchData.on('query-genes-changed', function (queryGeneIds) {
                    if (queryGeneIds.length < 2) {
                        coexpressionGrid.getTopToolbar().getComponent('queryGenesOnly').setDisabled(true);
                        coexpressionGrid.getTopToolbar().getComponent('queryGenesOnly').setValue(false);
                    } else {
                        coexpressionGrid.getTopToolbar().getComponent('queryGenesOnly').setDisabled(false);
                    }
                });

                /*
                 * Event handler: stringency spinner.
                 */
                this.coexDisplaySettings.on("stringency_change", function (displayStringency) {
                    coexpressionGrid.getTopToolbar().getComponent('stringencySpinner').setValue(displayStringency);
                    coexpressionGrid.applyFilters();
                });

                /*
                 * Event handler: checkbox for "my genes only"
                 */
                this.coexDisplaySettings.on('query_genes_only_change', function (checked) {
                    coexpressionGrid.getTopToolbar().getComponent('queryGenesOnly').setValue(checked);
                    coexpressionGrid.applyFilters();
                });

                /*
                 * Event handler: filter by text
                 */
                this.coexDisplaySettings.on('search_text_change',
                    function (text) {
                        coexpressionGrid.getTopToolbar().getComponent(coexpressionGrid.id + '-search-in-grid').setValue(
                            text);
                        coexpressionGrid.applyFilters();
                    });

                this.on("cellclick", coexpressionGrid.rowClickHandler);
            },

            /**
             * called from toolbar
             *
             * @private
             * @param spinner
             */
            onStringencyChange: function (spinner) {
                var spinnerValue = spinner.field.getValue();

                /*
                 * Don't allow the stringency to go lower than that used in the query
                 */
                var appliedStringency = this.coexpressionSearchData.getQueryStringency();

                if (spinnerValue >= appliedStringency) {
                    this.coexDisplaySettings.setStringency(spinnerValue);
                    this.applyFilters();
                } else {
                    spinner.field.setValue(appliedStringency);
                }
            },

            // called from toolbar
            onQueryGenesOnlyChange: function () {
                this.coexDisplaySettings.setQueryGenesOnly(this.getTopToolbar().getComponent('queryGenesOnly')
                    .getValue());
                this.applyFilters();
            },

            /**
             *
             */
            applyFilters: function () {
                this.getStore().filterBy(this.filter(), this);
            },

            /**
             *
             */
            hideBottomToolbar: function () {
                if (!this.getBottomToolbar().hidden) {
                    this.getBottomToolbar().hide();
                    this.doLayout();
                }
            },

            /**
             *
             * @param numQueryGenes
             */
            decideQueryColumn: function (numQueryGenes) {
                var queryIndex = this.getColumnModel().getIndexById('query');
                this.getColumnModel().setHidden(queryIndex, false);
            },

            /**
             * Private; CoexpressionMetaValueObject
             *
             * @param numQueryGenes
             * @param data
             */
            loadData: function (data) {
                this.getStore().proxy.data = data;
                this.getStore().reload({
                    resetPage: true
                });

            },

            /**
             * Checks if store contains any results if not print message indicating that there are non. Stop loader. Called
             * when an error thrown of after data load processing
             */
            handleError: function (errorMessage) {
                if (Ext.get('coexpression-msg')) {
                    Ext.DomHelper.applyStyles("coexpression-msg", "height: 2.2em");
                    Ext.DomHelper.overwrite("coexpression-msg", [{
                        tag: 'img',
                        src: Gemma.CONTEXT_PATH + '/images/icons/information.png'
                    }, {
                        tag: 'span',
                        html: "&nbsp;&nbsp;" + errorMessage
                    }]);
                } else {
                    Ext.Msg.alert("Warning", errorMessage);
                    this.getView().refresh(); // show empty text
                }
                this.loadMask.hide();
            },

            clearError: function () {
                Ext.DomHelper.overwrite("coexpression-messages", "");
            },

            /**
             *
             * @returns {Function}
             */
            filter: function () {
                var text = Ext.getCmp(this.id + '-search-in-grid').getValue();

                var stringency = this.coexDisplaySettings.getStringency();
                var queryGenesOnly = this.coexDisplaySettings.getQueryGenesOnly();
                var queryGeneIds = this.coexpressionSearchData.getQueryGeneIds();

                var value = '';

                if (text && text.length > 1) {
                    value = new RegExp(Ext.escapeRe(text), 'i');
                }

                return function filterFn(record) {

                    if (record.get("posSupp") < stringency && record.get("negSupp") < stringency) {
                        return false;
                    }

                    if (queryGenesOnly) {
                        if (!(queryGeneIds.indexOf(record.get('queryGene').id) !== -1 && queryGeneIds.indexOf(record
                                .get('foundGene').id) !== -1)) {
                            return false;
                        }
                    }

                    if (value) {
                        var foundGene = (record.get("foundGene"));
                        var queryGene = (record.get("queryGene"));

                        if (value.test(foundGene.officialSymbol) || value.test(queryGene.officialSymbol)
                            || value.test(foundGene.officialName) || value.test(queryGene.officialName)) {
                            return true;
                        } else {
                            return false;
                        }
                    }

                    return true;
                };
            },

            /**
             *
             * @param value
             * @param metadata
             * @param {Ext.data.Record}
             *           record
             * @param row
             * @param col
             * @param ds
             * @return {String}
             */
            nodeDegreeStyler: function (value, metadata, record, row, col, ds) {
                var data = record.data;
                var qnd = data['queryGeneNodeDegree'];
                var fnd = data['foundGeneNodeDegree'];
                qnd = qnd ? qnd : "~1"; // exact value is missing - out of date information
                fnd = fnd ? fnd : "~1"; // exact value is missing - out of date information

                var qndr = data['queryGeneNodeDegreeRank'];
                var fndr = data['foundGeneNodeDegreeRank'];

                qndr = qndr ? qndr : "?";

                fndr = fndr ? fndr : "?";

                return "<span title='Rank=" + parseFloat(qndr).toFixed(2) + "'><b>" + qnd
                    + "</b></span>,<span title='Rank=" + parseFloat(fndr).toFixed(2) + "'>" + fnd + "</span>";
            },

            /**
             *
             */
            supportStyler: function (value, metadata, record, row, col, ds) {
                var data = record.data;
                if (data['posSupp'] || data['negSupp']) {
                    var style = "";
                    if (data['posSupp']) {
                        style = style + String.format("<span class='positiveLink'>{0}</span> ", data['posSupp']);
                    }
                    if (data['negSupp']) {
                        style = style + String.format("<span class='negativeLink'>{0}</span> ", data['negSupp']);
                    }

                    if (data['numTestedIn']) {
                        style = style + String.format("/ {0}", data['numTestedIn']);
                    }
                    return style;
                } else {
                    return "-";
                }
            },

            // /**
            // * For displaying Gene ontology similarity
            // */
            // goStyler: function (value, metadata, record, row, col, ds) {
            // var data = record.data;
            // if (data['goSim'] || data['maxGoSim']) {
            // return String.format("{0}/{1}", data['goSim'], data['maxGoSim']);
            // } else {
            // return "-";
            // }
            // },

            // getSpecificLinkString: function (total, nonSpecific) {
            // return nonSpecific ? String
            // .format("<span class='specificLink'> ({0})</span>", total - nonSpecific) : "";
            // },

            /**
             * Display the target (found) genes.
             *
             * @param value
             * @param metadata
             * @param record
             * @param row
             *           {number}
             * @param col
             *           {number}
             * @param ds
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

                if (this.coexpressionSearchData.getQueryGeneIds().indexOf(gene.id) !== -1) {
                    gene.fontWeight = 'bold';
                }
                return this.geneTemplate.apply(gene);
            },

            /**
             *
             */
            geneTemplate: new Ext.Template(
                "<a style='cursor:pointer;font-weight:{fontWeight};' target='_blank' href='" + Gemma.CONTEXT_PATH + "/gene/showGene.html?id={id}'>{officialSymbol}</a> {officialName}"),

            /**
             *
             * @param value
             * @param metadata
             * @param record
             * @param row
             * @param col
             * @param ds
             * @returns
             */
            queryGeneStyler: function (value, metadata, record, row, col, ds) {
                var gene = record.data.queryGene;
                if (gene.officialName === null) {
                    gene.officialName = "";
                }
                gene.abaGeneUrl = record.data.abaQueryGeneUrl;
                gene.fontWeight = 'bold';
                return this.geneTemplate.apply(gene);
            },

            visStyler: function (value, metadata, record, row, col, ds) {
                return "<img style='cursor:pointer' src='" + Gemma.CONTEXT_PATH + "/images/icons/chart_curve.png' ext:qtip='Visualize the data' />";
            },

            /**
             *
             * @param grid
             * @param rowIndex
             * @param columnIndex
             * @override
             */
            rowClickHandler: function (grid, rowIndex, columnIndex) {
                if (this.getSelectionModel().hasSelection()) {

                    var record = this.getStore().getAt(rowIndex);
                    var fieldName = this.getColumnModel().getDataIndex(columnIndex);
                    var queryGene = record.get("queryGene");
                    var foundGene = record.get("foundGene");

                    // show the data
                    if (fieldName === 'visualize') {
                        var activeExperiments = record.data['supportingExperiments'];

                        if (activeExperiments === null || activeExperiments.length == 0) {
                            Ext.Msg.alert("Unavailable", "Details about the experiments are not available for visualization");
                            return;
                        }

                        var coexpressionVisualizationWindow = new Gemma.CoexpressionVisualizationWindow({
                            cascadeOnFirstShow: true,
                            admin: false,
                            experiments: activeExperiments,
                            queryGene: queryGene,
                            foundGene: foundGene,
                            downloadLink: String.format(Gemma.CONTEXT_PATH + "/dedv/downloadDEDV.html?ee={0}&g={1},{2}", activeExperiments
                                .join(','), queryGene.id, foundGene.id),
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

            /**
             * Provide mechanism to get the coexpression data in a text format.
             */
            exportData: function () {
                var filteredData;
                var queryGenesOnly = this.coexDisplaySettings.getQueryGenesOnly();
                var stringency = this.coexDisplaySettings.getStringency();

                if (queryGenesOnly && !this.coexpressionSearchData.searchCommandUsed.queryGenesOnly) {
                    filteredData = Gemma.CoexVOUtil.trimResults(this.coexpressionSearchData.getQueryGenesOnlyResults(),
                        stringency);
                } else {
                    var combinedData = this.coexpressionSearchData.getResults();
                    filteredData = Gemma.CoexVOUtil.trimResults(combinedData, stringency);
                }

                var text = Ext.getCmp(this.id + '-search-in-grid').getValue();
                if (text.length > 1) {
                    filteredData = Gemma.CoexVOUtil.filterGeneResultsByText(text, filteredData);
                }

                if (filteredData.length == 0) {
                    Ext.Msg.alert('No data', 'Nothing to download');
                    return;
                }

                var win = new Gemma.CoexpressionDownloadWindow({
                    title: "Coexpression Data"
                });
                win.convertText(filteredData);
            }
        });
