Ext.namespace('Gemma');

/**
 * This seems to be the main entry point. Constructor takes the data; performs the search
 *
 *
 * @type {*}
 */
Gemma.DiffExSearchAndVisualize = Ext
    .extend(
        Ext.Panel,
        {

            constructor: function (searchCommand) {
                if (typeof searchCommand !== 'undefined') {
                    Ext.apply(this, {
                        param: searchCommand
                    });
                }
                Gemma.DiffExSearchAndVisualize.superclass.constructor.call(this);
            },

            geneGroupValueObject: null,
            experimentGroupValueObject: null,

            waitingForGeneSessionGroupBinding: false,
            waitingForDatasetSessionGroupBinding: false,

            taxonId: null,

            /**
             * This is the callback for the differential expression search; just monitor progress.
             *
             * @private
             * @param taskId
             */
            _initBackgroundTaskProgress: function (taskId) {
                var task = new Gemma.ObservableSubmittedTask({
                    'taskId': taskId
                });
                task.on('task-completed', this._handleDiffExpSearchTaskResult, this);
                task.on('task-failed', this._handleFail, this);
                task.on('task-cancelling', this._handleFail, this);
                task.showTaskProgressWindow({});
            },

            /**
             * Callback to handle the return of the result from the server.
             *
             * @private
             * @memberOf Gemma.DiffExSearchAndVisualize
             * @param data
             * @param task
             */
            _handleDiffExpSearchTaskResult: function (data, task) {
                this.fireEvent("visualizationLoaded");

                // If data is null, there was an error.
                if (!data || data === null) {
                    if (this.applyToParam) {
                        Ext.DomHelper
                            .overwrite(
                                this.applyToParam,
                                {
                                    html: '<img src="' + Gemma.CONTEXT_PATH + '/images/icons/warning.png"/> Sorry, there was an error performing your search, data was null.'
                                });
                    }
                }

                // if no experiments were returned, don't show visualizer
                else if (data.conditions.length === 0) {
                    if (this.applyToParam) {
                        Ext.DomHelper.overwrite(this.applyToParam, {
                            html: '<img src="' + Gemma.CONTEXT_PATH + '/images/icons/warning.png"/>No data returned for your search.'
                        });
                    }

                } else {
                    // success
                    var title = '<b>Differential Expression Visualisation</b>';
                    var config = {
                        toolbarTitle: title,
                        visualizationData: data,
                        showTutorial: this.param.showTutorial
                    };
                    if (this.applyToParam) {
                        Ext.apply(config, {
                            applyTo: this.applyToParam
                        });
                    }
                    var _metaVizApp = new Gemma.Metaheatmap.Application(config);
                    _metaVizApp.doLayout();
                    _metaVizApp.refreshVisualization();
                }
            },

            /**
             * Callback for handling server-side error.
             *
             * @private
             * @memberOf Gemma.DiffExSearchAndVisualize
             * @param error
             */
            _handleFail: function (error) {
                this.fireEvent("visualizationLoaded");
                Ext.DomHelper.overwrite(this.applyToParam, {
                    html: '<img src="' + Gemma.CONTEXT_PATH + '/images/icons/warning.png"/> Sorry, there was an error performing your search: '
                    + error
                });
            },

            /**
             *
             * Initiate the actual search on the server.
             *
             * @private
             * @memberOf Gemma.DiffExSearchAndVisualize
             */
            doSearch: function () {

                if (typeof this.param === 'undefined') { // if not loading text from search interface (ex: when using a
                    // bookmarked link)
                    var waitMsg = Ext.Msg.wait("", "Loading your visualization...");
                }

                if (!this.taxonId || this.taxonId === null) {
                    // shouldn't happen, but ... DO SOMETHING!!
                }

                // here it is!
                DifferentialExpressionSearchController.scheduleDiffExpSearchTask(this.taxonId,
                    this.experimentGroupValueObject, this.geneGroupValueObject, {
                        callback: this._initBackgroundTaskProgress.createDelegate(this),
                        errorHandler: Gemma.Error.genericErrorHandler
                    });
            },

            /**
             * @memberOf Gemma.DiffExSearchAndVisualize Restore state from the URL (e.g., bookmarkable link)
             */
            initializeSearchFromURL: function (url) {

                alert("TODO initializeSearchFromURL");

                var param = Ext.urlDecode(url);
                var arrs;
                var i;

                /*
                 * if (param.p) { //we don't use this yet this.pvalue = param.p; }
                 */
                if (param.t) {
                    this.taxonId = param.t;
                }
                if (param.gs) {
                    this.initGeneSort = param.gs;
                }
                if (param.es) {
                    this.initExperimentSort = param.es;
                }
                if (param.ff) {
                    this.initFactorFilter = param.ff.split(',');
                }
                if (param.gq) {
                    this.initGeneSessionGroupQueries = param.gq.split(',');
                }
                if (param.eq) {
                    this.initExperimentSessionGroupQueries = param.gq.split(',');
                }
            },

            /**
             * @memberOf Gemma.DiffExSearchAndVisualize
             */
            initComponent: function () {

                // FOR TESTING !!!!!
                /*
                 * this.param2 = {
                 *
                 * geneNames : ["gene TEST", "gene TEST 2"], datasetNames : ["dataset TEST", "dataset TEST2"], taxonId : 2,
                 * pvalue : Gemma.DEFAULT_THRESHOLD };
                 */

                this.loadedFromURL = false;
                var queryStart = document.URL.indexOf("?");

                if (this.param && !this.loadedFromURL) { // if from search form
                    if (this.param.experimentSetValueObject) {
                        this.experimentGroupValueObject = this.param.experimentSetValueObject;
                    }
                    if (this.param.geneSetValueObject) {
                        this.geneGroupValueObject = this.param.geneSetValueObject;
                    }

                    if (this.param.taxonId) {
                        this.taxonId = this.param.taxonId;
                    }
                    if (this.param.taxonName) {
                        this.taxonName = this.param.taxonName;
                    }
                    if (this.param.geneSessionGroupQueries) {
                        this.geneSessionGroupQueries = this.param.geneSessionGroupQueries;
                    }
                    if (this.param.experimentSessionGroupQueries) {
                        this.experimentSessionGroupQueries = this.param.experimentSessionGroupQueries;
                    }
                    if (this.param.applyTo) {
                        this.applyToParam = this.param.applyTo;
                    }
                    /*
                     * if (this.param.pvalue) { //we don't use this yet this.pvalue = this.param.pvalue; }
                     */
                    // need to know for bookmarking
                    if (this.param.selectionsModified) {
                        this.selectionsModified = this.param.selectionsModified;
                    }
                }

                // this.on( 'geneGroupsReadyForVisualization', function() {
                // if ( !this.waitingForDatasetSessionGroupBinding ) {
                // this.doSearch();
                // }
                // }, this );
                // this.on( 'datasetGroupsReadyForVisualization', function() {
                // if ( !this.waitingForGeneSessionGroupBinding ) {
                // this.doSearch();
                // }
                // }, this );

                Gemma.DiffExSearchAndVisualize.superclass.initComponent.apply(this, arguments);

                // if we're already ready ...
                if (this.geneGroupValueObject && this.experimentGroupValueObject) {
                    this.doSearch();
                }
            }

        });

Ext.reg('metaVizDataSelection', Gemma.DiffExSearchAndVisualize);