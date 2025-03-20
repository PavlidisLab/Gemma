Ext.namespace('Gemma');

Gemma.GeoBrowseGrid = Ext
    .extend(
        Gemma.GemmaGridPanel,
        {

            collapsible: false,
            loadMask: true,
            defaults: {
                autoScroll: true
            },

            height: 500,
            // width : 1300,
            autoScroll: true,
            loadMask: true,

            autoExpandColumn: 'description',

            record: Ext.data.Record.create([{
                name: "usable",
                type: "boolean"
            }, {
                name: "geoAccession"
            }, {
                name: "numSamples",
                type: "int"
            }, {
                name: 'title'
            }, {
                name: 'correspondingExperiments'
            }, {
                name: "releaseDate",
                type: "date"
            }, {
                name: "organisms"
            }, {
                name: "inGemma",
                type: "boolean"
            }, {
                name: "usable",
                type: "boolean"
            }, {
                name: "previousClicks",
                type: "int"
            }]),

            /**
             * @memberOf Gemma.GeoBrowseGrid
             */
            proceed: function (s) {
                // new start
                this.start = Number(s) > 0 ? this.start + Number(s) : (this.start + this.count);

                this.store.load({
                    params: [this.start, this.count, this.searchString]
                });

            },

            /**
             * S is the skip.
             *
             * @param s
             */
            back: function (s) {
                // new start. Either go to the skip, or go back one
                // 'page', make sure greater than zero.
                this.start = Math.max(0, Number(s) > 0 ? this.start - Number(s) : this.start - this.count);

                this.store.load({
                    params: [this.start, this.count, this.searchString]
                });

            },

            /**
             *
             * @param s
             */
            search: function (s) {
                this.start = 0;
                this.searchString = String(s);
                this.store.load({
                    params: [this.start, this.count, this.searchString]
                });
            },

            /**
             * Download the short names of all/selected experiments as text
             */
            showAsText: function () {
                var string = "";
                var sels = this.getSelectionModel().getSelections();

                // If no experiments are selected, show all names; otherwise show the names of selected experiments
                if (sels.length == 0) {
                    this.getStore().each(function (r) {
                        string += r.get('geoAccession') + "\n";
                    });
                } else {
                    for (var i = 0; i < sels.length; i++)
                        string += sels[i].get('geoAccession') + "\n";
                }

                // Throw popup window
                var w = new Ext.Window({
                    modal: true,
                    title: "You can copy this text",
                    html: string,
                    height: 400,
                    width: 200,
                    autoScroll: true,
                    bodyCfg: {
                        tag: 'textarea',
                        style: 'background-color : white;font-size:smaller'
                    }
                });
                w.show();
            },

            // initial starting point
            start: 0,

            // page size
            count: 20,

            // empty search term
            searchString: '',

            initComponent: function () {

                Ext.apply(this, {
                    store: new Ext.data.Store({
                        proxy: new Ext.data.DWRProxy({
                            apiActionToHandlerMap: {
                                read: {
                                    dwrFunction: GeoRecordBrowserController.browse
                                }
                            },
                            getDwrArgsFunction: function (request, recordDataArray) {
                                return [this.start, this.count, this.searchString];
                            }
                        }),
                        reader: new Ext.data.ListRangeReader({
                            id: "id"
                        }, this.record)
                    })
                });

                // Toolbar for navigation and search
                Ext.apply(this, {
                    tbar: new Gemma.GeoBrowseToolbar({
                        listeners: {
                            'back': {
                                fn: function (s) {
                                    this.back(s);
                                },
                                scope: this
                            },
                            'proceed': {
                                fn: function (s) {
                                    this.proceed(s);
                                },
                                scope: this
                            },
                            'search': {
                                fn: function (s) {
                                    this.search(s);
                                },
                                scope: this
                            },
                            'showText': {
                                fn: function () {
                                    this.showAsText();
                                },
                                scope: this
                            }
                        }
                    })
                });

                Ext.apply(this, {
                    columns: [{
                        header: "Accession",
                        dataIndex: "geoAccession",
                        scope: this,
                        renderer: this.geoAccessionRenderer
                    }, {
                        header: "Title",
                        dataIndex: "title",
                        renderer: this.titleRenderer,
                        width: 500
                    }, {
                        header: "Release date",
                        dataIndex: "releaseDate",
                        renderer: Gemma.Renderers.dateRenderer,
                        width: 76
                    }, {
                        header: "numSamples",
                        dataIndex: "numSamples",
                        sortable: true,
                        width: 40
                    }, {
                        header: "In Gemma?",
                        dataIndex: "inGemma",
                        renderer: this.inGemmaRenderer,
                        width: 40
                    }, {
                        header: "taxa",
                        dataIndex: "organisms",
                        scope: this,
                        renderer: this.taxonRenderer
                    }, {
                        header: "Usable?",
                        dataIndex: "usable",
                        scope: this,
                        width: 30,
                        renderer: this.usableRenderer
                    }, {
                        header: "Examined",
                        dataIndex: "previousClicks",
                        scope: this,
                        width: 30,
                        renderer: this.clicksRenderer
                    }]
                });

                Gemma.GeoBrowseGrid.superclass.initComponent.call(this);

                this.getStore().on(
                    "load",
                    function (store, records, options) {

                        // there must be a better way of doing this.
                        Ext.DomHelper.overwrite(Ext.getDom('numRecords'), "<span id='numRecords'>" + records.length
                            + "</span>");

                    }, this);

                this.getSelectionModel().on('rowselect', this.showDetails, this, {
                    buffer: 100
                    // keep from firing too many times at once
                });

                this.getStore().load({
                    params: [this.start, this.count, this.searchString]
                });

            },

            /**
             * Show some dots.
             */
            clicksRenderer: function (value, metadata, record, row, col, ds) {
                var m = record.get('previousClicks');
                var result = "";
                for (var i = 0; i < Math.min(m, 5); i++) {
                    result = result + "&bull;";
                }
                return result;
            },

            geoAccessionRenderer: function (value, metadata, record, row, col, ds) {
                return "<a target='_blank' href='http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc="
                    + record.get('geoAccession') + "'>" + record.get('geoAccession') + "</a>";
            },

            titleRenderer: function (value, metadata, record, row, col, ds) {
                return record.get('title');
            },

            inGemmaRenderer: function (value, metadata, record, row, col, ds) {

                if (record.get('correspondingExperiments').length == 0) {
                    return "<input type=\"button\" value=\"Load\" " + "\" onClick=\"load('" + record.get('geoAccession')
                        + "')\" >";
                }

                var r = "";
                for (var i = 0; i < record.get('correspondingExperiments').length; i++) {
                    var ee = record.get('correspondingExperiments')[i];
                    r = r + "<a href='" + Gemma.CONTEXT_PATH + "/expressionExperiment/showExpressionExperiment.html?" + "id=" + ee + "'>"
                        + record.get('geoAccession') + "</a>";

                }
                return r;

            },

            taxonRenderer: function (value, metadata, record, row, col, ds) {
                var r = "";
                for (var i = 0; i < record.get('organisms').length; i++) {
                    var ee = record.get('organisms')[i];
                    r = r + "&nbsp;" + ee;

                }
                return r;
            },

            usableRenderer: function (value, metadata, record, row, col, ds) {
                if (record.get('correspondingExperiments').length > 0) {
                    return "<img src='" + Gemma.CONTEXT_PATH + "/images/icons/gray-thumb.png' width='16' height='16' alt='Already loaded'/>";
                }
                if (record.get('usable')) {
                    return "<span id=\""
                        + record.get('geoAccession')
                        + "-rating\"  onClick=\"Gemma.GeoBrowse.toggleUsability('"
                        + record.get('geoAccession')
                        + "')\"><img src='" + Gemma.CONTEXT_PATH + "/images/icons/thumbsup.png'  width='16' height='16'   alt='Usable, click to toggle' /></span>";
                } else {
                    return "<span id=\""
                        + record.get('geoAccession')
                        + "-rating\"  onClick=\"Gemma.GeoBrowse.toggleUsability('"
                        + record.get('geoAccession')
                        + "')\"  ><img src='" + Gemma.CONTEXT_PATH + "/images/icons/thumbsdown-red.png'  alt='Judged unusable, click to toggle'  width='16' height='16'  /></span>";
                }
            },

            showDetails: function (model, rowindex, record) {
                var callParams = [];
                callParams.push(record.get('geoAccession'));

                var delegate = handleSuccess.createDelegate(this, [], true);
                var errorHandler = handleFailure.createDelegate(this, [], true);

                callParams.push({
                    callback: delegate,
                    errorHandler: errorHandler
                });

                GeoRecordBrowserController.getDetails.apply(this, callParams);
                Ext.DomHelper.overwrite("messages", {
                    tag: 'img',
                    src: Gemma.CONTEXT_PATH + '/images/default/tree/loading.gif'
                });
                Ext.DomHelper.append("messages", {
                    tag: 'span',
                    html: "&nbsp;Please wait..."
                });

            }

        });

function handleSuccess(data) {
    Ext.DomHelper.overwrite("messages", {
        tag: 'div',
        html: data
    });
}

function handleUsabilitySuccess(data, accession) {

    if (data) {
        Ext.DomHelper.overwrite(accession + "-rating", {
            tag: 'img',
            src: Gemma.CONTEXT_PATH + '/images/icons/thumbsup.png'
        });
    } else {
        Ext.DomHelper.overwrite(accession + "-rating", {
            tag: 'img',
            src: Gemma.CONTEXT_PATH + '/images/icons/thumbsdown-red.png'
        });
    }

}

function handleFailure(data, e) {
    Ext.DomHelper.overwrite("taskId", "");
    Ext.DomHelper.overwrite("messages", {
        tag: 'img',
        src: Gemma.CONTEXT_PATH + '/images/icons/warning.png'
    });
    Ext.DomHelper.append("messages", {
        tag: 'span',
        html: "&nbsp;There was an error: " + data
    });
}

Gemma.GeoBrowse = {};
Gemma.GeoBrowse.toggleUsability = function(accession) {
    var callParams = [];
    callParams.push(accession);

    var delegate = handleUsabilitySuccess.createDelegate(this, [accession], true);
    var errorHandler = handleFailure.createDelegate(this, [], true);

    callParams.push({
        callback: delegate,
        errorHandler: errorHandler
    });

    GeoRecordBrowserController.toggleUsability.apply(this, callParams);
    Ext.DomHelper.overwrite(accession + "-rating", {
        tag: 'img',
        src: Gemma.CONTEXT_PATH + '/images/default/tree/loading.gif'
    });
}

function load(accession) {

    var suppressMatching = "false";
    var loadPlatformOnly = "false";
    var arrayExpress = "false";
    var arrayDesign = "";

    var commandObj = {
        accession: accession,
        suppressMatching: suppressMatching,
        loadPlatformOnly: loadPlatformOnly,
        arrayExpress: arrayExpress,
        arrayDesignName: arrayDesign
    };

    var errorHandler = handleFailure.createDelegate(this, [], true);

    Ext.DomHelper.overwrite("messages", {
        tag: 'img',
        src: Gemma.CONTEXT_PATH + '/images/default/tree/loading.gif'
    });
    Ext.DomHelper.append("messages", "&nbsp;Submitting job...");

    ExpressionExperimentLoadController.load(commandObj, {
        callback: function (taskId) {
            var task = new Gemma.ObservableSubmittedTask({
                'taskId': taskId
            });

            task.on('task-completed', function (payload) {
                document.location.reload(true);
                Ext.DomHelper.overwrite("messages", "Successfully loaded");
            });

            task.showTaskProgressWindow({
                showLogButton: true
            });

        },
        errorHandler: errorHandler
    });

}

Gemma.GeoBrowseToolbar = Ext.extend(Ext.Toolbar, {

    /**
     * @memberOf Gemma.GeoBrowseToolbar
     */
    initComponent: function () {
        Ext.apply(this, {
            items: [{
                id: 'skipfield',
                xtype: 'textfield',
                name: 'Skip',
                emptyText: 'Skip',
                width: 80
            }, {
                id: 'back',
                xtype: 'button',
                text: 'Back',
                handler: function () {
                    this.fireEvent('back', [Ext.get('skipfield').getValue()]);
                    Ext.get('skipfield').setValue('');
                }.createDelegate(this),
                width: 50
            }, {
                id: 'next',
                xtype: 'button',
                text: 'Next',
                handler: function () {
                    this.fireEvent('proceed', [Ext.get('skipfield').getValue()]);
                }.createDelegate(this),
                width: 50
            }, '-', {
                id: 'searchfield',
                xtype: 'textfield',
                emptyText: 'Enter search term',
                width: 200
            }, {
                id: 'searchbutton',
                xtype: 'button',
                text: 'Search',
                width: 60,
                handler: function () {
                    this.fireEvent('search', [Ext.get('searchfield').getValue()]);
                }.createDelegate(this)
            }, {
                id: 'show-as-text',
                xtype: 'button',
                icon: Gemma.CONTEXT_PATH + '/images/icons/disk.png',
                handler: function () {
                    this.fireEvent('showText');
                }.createDelegate(this)
            }]
        });
        Gemma.GeoBrowseToolbar.superclass.initComponent.call(this);
    }
});