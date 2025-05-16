/**
 * The input form to pick genes and experiments, and run coexpression or differential expression searches. This form has
 * three main parts: a search mode chooser, an experiment (group) searcher, and a gene (group) searcher
 * <p>
 * This supports queries that originate from a URL. Example:
 *
 * <pre>
 * /home.html?taxon=1&amp;geneList=ARHGAP42P5,TRAV8-5,OR11H12,RNU6-1239P,OR4K1,POTEG,OR11H13P,DUXAP10,POTEM,
 * </pre>
 *
 *
 * @requires SessionBoundExpressionExperimentSetValueObject
 * @requires Gemma.ExperimentSearchAndPreview
 * @requires Gemma.GeneSearchAndPreview
 * @author thea
 *
 */
Gemma.AnalysisResultsSearchForm = Ext
    .extend(
        Ext.FormPanel,
        {
            layout: 'table',
            layoutConfig: {
                columns: 5
            },
            width: 900,
            frame: false,
            border: false,
            bodyBorder: false,
            bodyStyle: "backgroundColor:white",
            defaults: {
                border: false
            },
            ctCls: 'titleBorderBox',

            /**
             * @type {Boolean}
             */
            stateful: false,

            stateEvents: ["beforesearch"],

            /**
             * @type {Boolean}
             */
            eeSetReady: false,

            /**
             * @type {Number}
             */
            taxonId: null,

            /**
             * @type {Boolean}
             */
            defaultIsDiffEx: false,

            /**
             *
             */
            searchbBar: null,

            /**
             * @type {Gemma.ExperimentSearchAndPreview}
             */
            experimentSearchAndPreview: null,

            /**
             * @type {Gemma.GeneSearchAndPreview}
             */
            geneSearchAndPreview: null,

            /**
             * @private
             * @param msg
             * @param e
             * @memberOf Gemma.AnalysisResultsSearchForm
             */
            handleWarning: function (msg, e) {
                if (Ext.get("analysis-results-search-form-messages")) {
                    msg = (msg === "") ? "Error retrieving results" : msg;

                    Ext.DomHelper.overwrite("analysis-results-search-form-messages", {
                        tag: 'img',
                        src: Gemma.CONTEXT_PATH + '/images/icons/warning.png'
                    });

                    Ext.DomHelper.append("analysis-results-search-form-messages", {
                        tag: 'span',
                        html: "&nbsp;&nbsp;" + msg
                    });
                } else {
                    this.fireEvent("search_error", msg);
                }
            },

            /**
             * @private
             *
             * @param msg
             * @param e
             */
            handleError: function (msg, e) {
                // console.log( e.stack );

                this.handleWarning(msg);

                if (this.loadMask)
                    this.loadMask.hide();

                this.fireEvent('aftersearch', this, e);
                Ext.Msg.alert("Error", e + "/n" + msg);

            },

            /**
             * @private
             */
            clearError: function () {
                if (Ext.get("analysis-results-search-form-messages")) {
                    Ext.DomHelper.overwrite("analysis-results-search-form-messages", "");
                }
            },

            /**
             * @private
             *
             * @return {boolean}
             */
            wereSelectionsModified: function () {

                if (this.geneSearchAndPreview.getSelectedGeneSetValueObject()
                    && this.geneSearchAndPreview.getSelectedGeneSetValueObject().modified) {
                    return true;
                }

                if (this.experimentSearchAndPreview.getSelectedExpressionExperimentSetValueObject()
                    && this.experimentSearchAndPreview.listModified) {
                    return true;
                }

                return false;
            },

            /**
             *
             * @return {Array}
             */
            getGeneSessionGroupQueries: function () {
                return this.geneSearchAndPreview.queryUsedToGetSessionGroup;
            },

            /**
             *
             * @return {Array}
             */
            getExperimentSessionGroupQueries: function () {
                if (this.experimentSearchAndPreview.getSelectedExpressionExperimentSetValueObject()
                    && this.experimentSearchAndPreview.queryUsedToGetSessionGroup !== null) {
                    return this.experimentSearchAndPreview.queryUsedToGetSessionGroup;
                }
            },

            /**
             * Get selections as geneSetValueObjects (selected single genes will be wrapped as single-member geneSets)
             *
             * @return GeneSetValueObject
             */
            getSelectedGeneSetValueObject: function () {
                var selectedVO = this.geneSearchAndPreview.getSelectedGeneSetValueObject();
                // we allow searches without specifying genes in some cases.

                // if ( !selectedVO ) {
                // throw "No genes selected";
                // }

                if (selectedVO instanceof GeneValueObject) {
                    // this should never happen
                    // console.log( "got a single gene" );
                    var gene = selectedVO;
                    // Why do we do it this way? it would be easier to have selected always be a set in the first place.
                    var singleGeneSet = new SessionBoundGeneSetValueObject();
                    singleGeneSet.id = null;
                    singleGeneSet.geneIds = [gene.id];
                    singleGeneSet.name = gene.officialSymbol;
                    singleGeneSet.description = gene.officialName;
                    singleGeneSet.size = gene.size;
                    singleGeneSet.taxonName = gene.taxonCommonName;
                    singleGeneSet.taxonId = gene.taxonId;
                    singleGeneSet.modified = false;
                    return singleGeneSet;
                }
                return selectedVO;

            },

            /**
             * Get selections as expressionExperimentSetValueObjects (selected single genes will be wrapped as
             * single-member experimentSets)
             *
             * @return ExpressionExperimentSetValueObject
             */
            getSelectedExperimentSetValueObject: function () {
                var selectedVO = this.experimentSearchAndPreview.getSelectedExpressionExperimentSetValueObject();

                // we allow searches without specifying experiments in some cases.
                // if ( selectedVO == null ) {
                // throw "No experiment set was selected";
                // }

                if (selectedVO instanceof ExpressionExperimentValueObject) {
                    // we should not let this happen.
                    // console.log( "got a single experiment " );
                    var ee = selectedVO;
                    var singleExperimentSet = new SessionBoundExpressionExperimentSetValueObject();
                    singleExperimentSet.id = null;
                    singleExperimentSet.expressionExperimentIds = [ee.id];
                    singleExperimentSet.name = ee.shortName;
                    singleExperimentSet.description = ee.name;
                    singleExperimentSet.size = 1;
                    singleExperimentSet.taxonName = ee.taxon;
                    singleExperimentSet.taxonId = ee.taxonId;
                    singleExperimentSet.modified = false;
                    singleExperimentSet.numWithCoexpressionAnalysis = ee.hasCoexpressionAnalysis ? 1 : 0;
                    singleExperimentSet.numWithDifferentialExpressionAnalysis = ee.hasDifferentialExpressionAnalysis ? 1 : 0;
                    return singleExperimentSet;
                }

                return selectedVO;
            },

            /**
             *
             * @private
             *
             */
            runSearch: function () {

                this.searchMethods = new Gemma.AnalysisResultsSearchMethods({
                    taxonId: this.getTaxonId(),
                    taxonName: this.getTaxonName()
                });

                this.relayEvents(this.searchMethods, ['beforesearch']);

                this.searchMethods.on('differential_expression_search_query_ready', function (result, data) {
                    Ext.apply(data, {
                        geneSessionGroupQueries: this.getGeneSessionGroupQueries(),
                        experimentSessionGroupQueries: this.getExperimentSessionGroupQueries(),
                        selectionsModified: this.wereSelectionsModified(),
                        showTutorial: this.runningExampleQuery
                    });
                    this.fireEvent('differential_expression_search_query_ready', this, result, data);
                }, this);

                this.searchMethods.on('coexpression_search_query_ready', function (searchCommand) {
                    this.fireEvent('coexpression_search_query_ready', this, searchCommand, this.runningExampleQuery);
                }, this);

                this.searchMethods.on('warning', function (msg, e) {
                    this.handleWarning(msg, e);
                }, this);

                this.searchMethods.on('error', function (msg, e) {
                    this.handleError(msg, e);
                }, this);

                this.searchMethods.on('clearerror', function () {
                    this.clearError();
                }, this);

                this.searchMethods.on('searchAborted', function () {
                    this.loadMask.hide();
                }, this);

                this.collapsePreviews();
                this.collapseExamples();

                if (!this.loadMask) {
                    this.loadMask = new Ext.LoadMask(this.getEl(), {
                        msg: Gemma.StatusText.Searching.analysisResults,
                        msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                    });
                }

                /*
                 * Key entry point.
                 */
                if (this.coexToggle.pressed) {
                    this.searchMethods.searchCoexpression(this.getSelectedGeneSetValueObject(), this
                        .getSelectedExperimentSetValueObject());
                } else {
                    this.searchMethods.searchDifferentialExpression(this.getSelectedGeneSetValueObject(), this
                        .getSelectedExperimentSetValueObject());
                }
            },

            /**
             * @Override
             */
            initComponent: function () {

                // experiment chooser panels. This panel is just a bit of a wrapper.
                this.experimentSearchAndPreviewPanel = new Ext.Panel({
                    // width: 319,
                    frame: false,
                    bodyStyle: 'background-color:transparent',
                    defaults: {
                        border: false,
                        bodyStyle: 'background-color:transparent'
                    },
                    style: 'padding-bottom: 10px',
                    autoDestroy: true
                });

                // gene chooser panels
                this.geneSearchAndPreviewPanel = new Ext.Panel({
                    // width: 319,
                    frame: false,
                    defaults: {
                        border: false
                    },
                    style: 'padding-bottom: 10px;',
                    autoDestroy: true
                });

                /**
                 * ***** BUTTONS ******
                 */
                this.coexToggle = new Ext.Button({
                    text: "<span style=\"font-size:1.3em\">Coexpression</span>",
                    cls: 'highlightToggle',
                    scale: 'medium',
                    width: 150,
                    enableToggle: true,
                    pressed: !this.defaultIsDiffEx,
                    tooltip: 'Look for patterns of coexpression between genes of your choosing, across multiple data sets'
                });
                this.coexToggle.on('click', function () {
                    this.coexToggle.toggle(true);
                    this.diffExToggle.toggle(false);
                    this.fireEvent('modechange', 'coex');
                }, this);

                this.diffExToggle = new Ext.Button({
                    text: "<span style=\"font-size:1.3em\">Differential Expression</span>",
                    scale: 'medium',
                    cls: 'highlightToggle',
                    width: 150,
                    enableToggle: true,
                    pressed: this.defaultIsDiffEx,
                    tooltip: 'Look for differential experession of genes of your choosing, across multiple conditions'
                });
                this.diffExToggle.on('click', function () {
                    this.diffExToggle.toggle(true);
                    this.coexToggle.toggle(false);
                    this.fireEvent('modechange', 'diffex');
                }, this);

                this.initializeGeneSearchAndPreview();
                this.initializeExperimentSearchAndPreview();

                this.searchBar = new Ext.Panel({

                    border: false,
                    layout: 'table',
                    layoutConfig: {
                        columns: 5
                    },
                    width: 490,
                    style: 'margin: 0 7px',
                    defaults: {
                        border: false
                    },
                    items: [{
                        html: 'Test for ',
                        style: 'white-space: nowrap;text-align:center;vertical-align:middle;font-size:1.7em;margin-top:7px'
                    }, this.coexToggle, {
                        html: 'or',
                        style: 'white-space: nowrap;text-align:center;vertical-align:middle;font-size:1.7em;margin-top:7px'
                    }, this.diffExToggle, {
                        html: 'of:',
                        style: 'white-space: nowrap;text-align:center;vertical-align:middle;font-size:1.7em;margin-top:7px'
                    }]

                });

                /** ************* TEXT ******************** */

                // little areas in the middle.
                this.theseExperimentsPanel = new Ext.Panel({
                    html: 'these experiments',
                    style: 'text-align:center;font-size:1.4em;',
                    tpl: new Ext.XTemplate('these <span class="blue-text-not-link" style="font-weight:bold " ',
                        'ext:qtip="' + Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.taxonModeTT + '">',
                        '{taxonCommonName} </span> experiments '),
                    tplWriteMode: 'overwrite'
                });
                this.theseGenesPanel = new Ext.Panel({
                    html: 'these genes',
                    style: 'text-align:center;font-size:1.4em;',
                    tpl: new Ext.XTemplate('these <span class="blue-text-not-link" style="font-weight:bold " ',
                        'ext:qtip="' + Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.taxonModeTT + '">',
                        '{taxonCommonName}</span> genes '),
                    tplWriteMode: 'overwrite'
                });

                this.searchExamples = new Gemma.AnalysisResultsSearchExamples({
                    ref: 'searchExamples',
                    defaultIsDiffEx: false
                });

                this.searchExamples.on('startingExample', function () {
                    if (!this.loadMask) {
                        this.loadMask = new Ext.LoadMask(this.getEl(), {
                            msg: Gemma.StatusText.Searching.analysisResults,
                            msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                        });
                    }
                    this.loadMask.show();
                }, this);

                this.searchExamples.on('examplesReady', function (taxonId, geneSetExampleRecord,
                                                                  experimentSetExampleRecord) {
                    this.runExampleQuery(taxonId, geneSetExampleRecord, experimentSetExampleRecord);
                }, this);

                this.diffExToggle.on('toggle', function () {
                    if (this.diffExToggle.pressed) {
                        this.searchExamples.showDiffExExamples();
                    } else {
                        this.searchExamples.showCoexExamples();
                    }
                }, this);
                /** ************* PUT ITEMS IN PANEL ******************** */

                Ext.apply(this, {
                    style: '',
                    items: {
                        xtype: 'fieldset',
                        title: '&nbsp;',
                        border: true,
                        searchBar: this.searchBar,
                        listeners: {
                            render: function (c) {
                                // Ext bug workaround
                                var floatType = Ext.isIE ? 'styleFloat' : 'cssFloat';
                                c.header.child('span').applyStyles(floatType + ':left;padding:5px 5px 0 0');
                                this.searchBar.render(c.header, 1);
                                // this.searchBar.wrap.applyStyles(floatType + ':left');
                                c.on('destroy', function () {
                                    this.searchBar.destroy();
                                }, c, {
                                    single: true
                                });
                            }.createDelegate(this)
                        },
                        items: [{
                            layout: 'table', // needs to be table so panel stretches with content growth
                            layoutConfig: {
                                columns: 4
                            },
                            width: 850,
                            border: false,
                            defaults: {
                                border: false,
                                bodyStyle: 'padding: 0px;margin:0px'
                            },
                            items: [{
                                defaults: {
                                    border: false
                                },
                                items: [this.theseGenesPanel, this.geneSearchAndPreviewPanel]
                            }, {
                                html: 'in ',
                                style: 'white-space: nowrap;font-size:1.7em;padding-top: 32px;padding-right:5px;'
                            }, {
                                defaults: {
                                    border: false
                                },
                                items: [this.theseExperimentsPanel, this.experimentSearchAndPreviewPanel]
                            }, {
                                style: 'padding:20 0 0 0px;margin:0px;',
                                items: [{
                                    xtype: 'button',
                                    text: "<span style=\"font-size:1.3em;padding-top:15px\">Go!</span>",
                                    width: 55,
                                    tooltip: 'Run the search',
                                    scale: 'medium',
                                    listeners: {
                                        click: function () {
                                            this.runningExampleQuery = false;
                                            this.runSearch();
                                        }.createDelegate(this, [], false)
                                    }

                                }, {
                                    xtype: 'button',
                                    width: 55,
                                    icon: Gemma.CONTEXT_PATH + '/images/icons/arrow_refresh_small.png',
                                    style: 'margin-top: 8px',
                                    text: 'Reset',
                                    tooltip: 'Clear all selections and reset the taxon mode ',
                                    handler: this.reset.createDelegate(this)
                                }]
                            }, this.searchExamples]
                        }]
                    }
                });

                Gemma.AnalysisResultsSearchForm.superclass.initComponent.call(this);

                this.on('queryUpdateFromCoexpressionViz', function (geneIds) {
                    this.getEl().unmask();

                    if (this.geneSearchAndPreview.preview) {
                        this.geneSearchAndPreview.preview.selectionEditorWindow.hide();
                    }

                    if (this.experimentSearchAndPreviewPanel.preview) {
                        this.experimentSearchAndPreviewPanel.preview.selectionEditorWindow.hide();
                    }

                    this.geneSearchAndPreview.getGenes(geneIds);

                }, this);

                this.addEvents('beforesearch', 'aftersearch', 'differential_expression_search_query_ready',
                    'coexpression_search_query_ready', 'modechange');

                // for events triggered when this page was reached using parameters encoded in the url.
                this.relayEvents(this.experimentSearchAndPreview.experimentCombo,
                    ['experimentGroupUrlSelectionComplete']);
                this.relayEvents(this.geneSearchAndPreview, ['geneListUrlSelectionComplete']);

                this.on('experimentGroupUrlSelectionComplete', function () {
                    this.experimentGroupUrlSelectionComplete = true;
                    this.initiateSearch();
                }, this);

                this.on('geneListUrlSelectionComplete', function () {
                    this.geneListUrlSelectionComplete = true;
                    this.initiateSearch();
                }, this);

                this.doLayout();

                this.fireEvent('modechange', this.defaultIsDiffEx ? 'diffex' : 'coex');
            },

            /**
             * If the URL used to reach this page provides search parameters, immediately do a search. This was initially
             * put in place to support AspireDB queries.
             */
            checkUrlParams: function () {
                var urlparams = Ext.urlDecode(location.search.substring(1));

                // if these parameters are in the URL then do a search.
                if (urlparams.geneList && urlparams.taxon) {

                    this.geneSearchAndPreview.getGenesFromUrl();

                    if (urlparams.type) {

                        if (urlparams.type == 'diff') {
                            this.coexToggle.toggle(false);
                            this.diffExToggle.toggle(true);
                        } else {
                            this.coexToggle.toggle(true);
                            this.diffExToggle.toggle(false);
                        }

                    } else {
                        // backwards compatibility; assume coexpression
                        this.coexToggle.toggle(true);
                        this.diffExToggle.toggle(false);

                    }
                }
            },

            /**
             * Start search if everything is ready.
             *
             * @private
             */
            initiateSearch: function () {
                if (this.experimentGroupUrlSelectionComplete && this.geneListUrlSelectionComplete) {
                    this.runSearch();
                }
            },

            /**
             * Go back to initial state - including resetting the taxon. Behaviour is different if user clicks on reset for
             * just genes or just experiments.
             *
             * @private
             */
            reset: function () {
                // reset experiment and gene choosers
                this.geneSearchAndPreview.reset();
                this.experimentSearchAndPreview.reset();

                // reset taxon
                this.setTaxonId(null);

                // reset taxon id and titles
                Ext.DomHelper.overwrite(this.theseGenesPanel.body, {
                    cn: 'these genes'
                });
                Ext.DomHelper.overwrite(this.theseExperimentsPanel.body, {
                    cn: 'these experiments'
                });
            },

            getTaxonId: function () {
                return this.taxonId;
            },

            setTaxonId: function (taxonId) {
                this.taxonId = taxonId;
                this.geneSearchAndPreview.geneCombo.setTaxonId(taxonId);
                this.experimentSearchAndPreview.taxonId = taxonId;
                this.experimentSearchAndPreview.experimentCombo.setTaxonId(taxonId);
            },

            getTaxonName: function () {
                return this.taxonName;
            },

            setTaxonName: function (taxonName) {
                this.taxonName = taxonName;
                this.theseExperimentsPanel.update({
                    taxonCommonName: taxonName
                });
                this.theseGenesPanel.update({
                    taxonCommonName: taxonName
                });
            },

            /**
             * Check if the taxon needs to be changed, and if so, update the geneAndGroupCombo and reset the gene preivew
             *
             * @param taxonId
             *           {Number}
             *
             * @param taxonName
             *           {String}
             *
             */
            taxonChanged: function (taxonId, taxonName) {

                // if the 'new' taxon is the same as the 'old' taxon for the experiment
                // combo, don't do anything
                if (taxonId && this.getTaxonId() && (this.getTaxonId() === taxonId)) {
                    return;
                }

                // if the 'new' and 'old' taxa are different, reset the gene preview and
                // filter the geneCombo
                else if (taxonId) {
                    this.setTaxonId(taxonId);
                    if (taxonName) {
                        this.setTaxonName(taxonName);
                    }
                }

                // this.fireEvent( "taxonchanged", taxonId );
                Gemma.EVENTBUS.fireEvent('taxonchanged', taxonId);
            },

            // collapse all gene and experiment previews
            collapsePreviews: function () {
                this.collapseGenePreviews();
                this.collapseExperimentPreviews();
            },

            collapseGenePreviews: function () {
                this.geneSearchAndPreview.collapsePreview(false);
            },

            collapseExperimentPreviews: function () {
                this.experimentSearchAndPreview.collapsePreview(false);
            },

            initializeGeneSearchAndPreview: function () {
                this.geneSearchAndPreview = new Gemma.GeneSearchAndPreview({
                    searchForm: this,
                    taxonId: this.getTaxonId(),
                    style: 'padding-top:10px;'
                });

                this.geneSearchAndPreviewPanel.add(this.geneSearchAndPreview);
                if (typeof Ext.getCmp('geneChooser' + (this.geneChooserIndex - 1) + 'Button') !== 'undefined') {
                    Ext.getCmp('geneChooser' + (this.geneChooserIndex - 1) + 'Button').show().setIcon(
                        Gemma.CONTEXT_PATH + '/images/icons/delete.png').setTooltip('Remove this gene or group from your search')
                        .setHandler(
                            this.removeGeneChooser.createDelegate(this, ['geneChooserPanel' + (this.geneChooserIndex - 1)],
                                false));
                }

                this.geneSearchAndPreviewPanel.doLayout();
            },

            /**
             * Set up the {Gemma.ExperimentSearchAndPreview}
             *
             * @memberOf Gemma.AnalysisResultsSearchForm
             */
            initializeExperimentSearchAndPreview: function () {
                this.experimentSearchAndPreview = new Gemma.ExperimentSearchAndPreview({
                    searchForm: this,
                    taxonId: this.getTaxonId(),
                    style: 'padding-top:10px;',
                    mode: this.diffExToggle.pressed ? 'diffex' : 'coex'
                });

                this.experimentSearchAndPreviewPanel.add(this.experimentSearchAndPreview);
                this.experimentSearchAndPreviewPanel.doLayout();
            },

            /**
             * hide the example queries
             */
            collapseExamples: function () {
                this.searchExamples.collapseExamples(false);
            },

            /**
             * For example queries: set the experiment chooser to have chosen a set and show its preview
             *
             * @private
             * @param record
             */
            setExampleExperimentSet: function (record) {

                var chooser = this.experimentSearchAndPreview;
                chooser.setSelectedExpressionExperimentSetValueObject(record.data);

                var eeCombo = chooser.experimentCombo;

                // insert record into combo's store
                eeCombo.getStore().insert(0, record);
                eeCombo.selectedIndex = 0;

                // tell gene combo the group was selected
                eeCombo.fireEvent('select', eeCombo, record, 0);
            },

            /**
             * For example queries: set the gene chooser to have chosen a go group and show its preview
             *
             * @private
             * @param record
             *
             */
            setExampleGeneSet: function (record) {

                // get the chooser to inject
                var chooser = this.geneSearchAndPreview;

                chooser.setSelectedGeneSetValueObject(record.data);

                // get the chooser's gene combo
                var geneCombo = chooser.geneCombo;
                geneCombo.getStore().insert(0, record);

                // tell gene combo the GO group was selected
                geneCombo.fireEvent('select', geneCombo, record, 0);

            },

            /**
             *
             * @param taxonId
             *           {Number}
             * @param geneSetRecord
             * @param experimentSetRecord
             */
            runExampleQuery: function (taxonId, geneSetRecord, experimentSetRecord) {
                if (!geneSetRecord || !experimentSetRecord) {
                    throw "Need gene set and experiment set";
                }

                // taxon needs to be set before gene group is chosen because otherwise there will be >1 choice provided
                this.setTaxonId(taxonId);

                this.setExampleExperimentSet(experimentSetRecord);
                this.setExampleGeneSet(geneSetRecord);

                this.runningExampleQuery = true;
                this.runSearch();
            }
        });

Ext.reg('analysisResultsSearchForm', Gemma.AnalysisResultsSearchForm);