Ext.namespace('Gemma');

/**
 *
 * Grid to display expression experiments with differential evidence for each platform element (e.g., for a single gene).
 *
 */
Gemma.ProbeLevelDiffExGrid = Ext
    .extend(
        Ext.grid.GridPanel,
        {

            autoExpandColumn: 'efs',
            height: 300,
            stateful: false,

            viewConfig: {
                forceFit: true,
                emptyText: 'No differential expression to display'
                // ,deferEmptyText: false
            },

            readMethod: DifferentialExpressionSearchController.getDifferentialExpressionWithoutBatch,

            convertEE: function (s) {
                return s.shortName;
            },

            convertEF: function (s) {
                return s[0].name;
            },

            getEEName: function (v, record) {
                return record.expressionExperiment.name;
            },

            /**
             * @memberOf Gemma.ProbeLevelDiffExGrid
             */
            initComponent: function () {

                Ext.apply(this, {
                    record: Ext.data.Record.create([{
                        name: "expressionExperiment",
                        sortType: this.convertEE
                    }, {
                        name: "gene"
                    }, {
                        name: "expressionExperimentName",
                        convert: this.getEEName
                    }, {
                        name: "probe"
                    }, {
                        name: "experimentalFactors",
                        sortType: this.convertEF
                    }, {
                        name: "metThreshold",
                        type: "boolean"
                    }, {
                        name: "fisherContribution",
                        type: "boolean"
                    }, {
                        name: "p",
                        type: "float"
                    }])
                });

                this.searchInGridField = new Ext.form.TextField({
                    enableKeyEvents: true,
                    emptyText: 'Filter',
                    tooltip: "Text typed here will ",
                    listeners: {
                        "keyup": {
                            fn: this.searchForText.createDelegate(this),
                            scope: this,
                            options: {
                                delay: 100
                            }
                        }
                    }
                });

                Ext.apply(this, {
                    store: new Ext.data.Store({
                        proxy: new Ext.data.DWRProxy(this.readMethod),
                        reader: new Ext.data.ListRangeReader({}, this.record),
                        sortInfo: {
                            field: "p",
                            direction: "ASC"
                        }
                    })
                });

                Ext.apply(this, {
                    bbar: new Ext.Toolbar({
                        items: ['->', {
                            xtype: 'button',
                            handler: this.clearFilter.createDelegate(this),
                            scope: this,
                            cls: 'x-btn-text',
                            text: 'Reset filter'
                        }, ' ', this.searchInGridField]
                    })
                });

                Ext
                    .apply(
                        this,
                        {
                            columns: [
                                {
                                    id: 'expressionExperiment',
                                    header: "Dataset",
                                    dataIndex: "expressionExperiment",
                                    tooltip: "The study/expression experiment the result came from",
                                    sortable: true,
                                    renderer: Gemma.ProbeLevelDiffExGrid.getEEStyler(),
                                    width: 50
                                },
                                {
                                    id: 'visualize',
                                    hidden: true,
                                    header: "Visualize",
                                    dataIndex: "visualize",
                                    renderer: this.visStyler.createDelegate(this),
                                    tooltip: "Link for visualizing raw data",
                                    sortable: false,
                                    width: 30

                                },
                                {
                                    id: 'expressionExperimentName',
                                    header: "Name",
                                    width: 100,
                                    dataIndex: "expressionExperimentName",
                                    tooltip: "The experiment name (abbreviated)",
                                    sortable: true,
                                    renderer: function (value, metadata, record, row, col, ds) {
                                        return '<span ext:qtip="' + record.get('expressionExperiment').name + '">'
                                            + Ext.util.Format.ellipsis(record.get('expressionExperiment').name, 70)
                                            + '</span>';
                                    }.createDelegate(this)
                                },
                                {
                                    id: 'probe',
                                    header: "Probe",
                                    dataIndex: "probe",
                                    width: 80,
                                    tooltip: "The specific platform element",
                                    renderer: Gemma.ProbeLevelDiffExGrid.getProbeStyler(),
                                    sortable: true
                                },
                                {
                                    id: 'efs',
                                    header: "Factor",
                                    tooltip: "The factor that was examined",
                                    dataIndex: "experimentalFactors",
                                    renderer: Gemma.ProbeLevelDiffExGrid.getEFStyler(),
                                    sortable: true
                                },
                                {
                                    id: 'p',
                                    header: "Sig. (q-value)",
                                    tooltip: "The significance measure of the result for the element, shown in color if it met threshold",
                                    dataIndex: "p",
                                    width: 80,
                                    renderer: function (p, metadata, record) {
                                        if (record.get("metThreshold")) {
                                            metadata.css = "metThreshold"; // typo.css
                                        }
                                        if (p < 0.001) {
                                            return sprintf("%.3e", p);
                                        } else {
                                            return sprintf("%.3f", p);
                                        }
                                    },
                                    sortable: true
                                }]
                        });

                Gemma.ProbeLevelDiffExGrid.superclass.initComponent.call(this);

                this.on("cellclick", this.rowClickHandler.createDelegate(this), this);

                // once the store is loaded validate to see if it has any records and pass delegate reference to store
                this.store.on("load", this.validate.createDelegate(this));

            },

            /**
             * Checks if store contains any results if not print message indicating that there are no differential analyses
             * for this taxon. Triggered on load event firing.
             */
            validate: function () {
                if (this.store.getCount() == 0) {
                    this.handleError("No differential expression results available");
                }
            },

            /**
             * Print error message called when application throws and exception or error message set on result.
             */
            handleError: function (errorMessage) {
                if (Ext.get("diffExpression-msg")) {
                    Ext.DomHelper.applyStyles("diffExpression-msg", "height: 2.2em");
                    Ext.DomHelper.overwrite("diffExpression-msg", [{
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

            },

            searchForText: function (button, keyev) {
                var text = this.searchInGridField.getValue();
                if (text.length < 2) {
                    this.getStore().clearFilter();
                    return;
                }
                this.getStore().filterBy(this.getSearchFun(text), this, 0);
            },

            clearFilter: function () {
                this.searchInGridField.setValue("");
                this.getStore().clearFilter();
            },

            getSearchFun: function (text) {
                var value = new RegExp(Ext.escapeRe(text), 'i');
                return function (r, id) {
                    var obj = r.data;
                    return value.test(obj.expressionExperiment.name) || value.test(obj.expressionExperiment.shortName)
                        || value.test(obj.experimentalFactors[0].name);
                };
            },

            getEEIds: function () {
                var result = [];
                this.store.each(function (rec) {
                    result.push(rec.get("id"));
                });
                return result;
            },

            isEditable: function () {
                return this.editable;
            },

            setEditable: function (b) {
                this.editable = b;
            },

            metThresholdStyler: function (value, metadata, record, row, col, ds) {
                if (value) {
                    return "&bull;";
                } else {
                    return "";
                }
            },

            visStyler: function (value, metadata, record, row, col, ds) {
                return "<span style='cursor:pointer' ><img src='" + Gemma.CONTEXT_PATH + "/images/icons/chart_curve.png' ext:qtip='Visualize the data' /></span>";
            },

            rowClickHandler: function (grid, rowIndex, columnIndex, e) {

                if (!this.getSelectionModel().hasSelection())
                    return;

                var record = this.getStore().getAt(rowIndex);
                var fieldName = this.getColumnModel().getDataIndex(columnIndex);

                if (fieldName == 'visualize') {

                    var ee = record.data.expressionExperiment;
                    var gene = record.data.gene;
                    var geneId;

                    // Gene object might not be available in store, if not check if geneId is embeded in page.
                    if (gene != null) {
                        geneId = gene.id;
                    } else {
                        geneId = dwr.util.getValue("gene"); // would be nice if there was a way to get the gene
                        // object....
                    }

                    var title = "Visualization of elements ";

                    // ID passed is either for the EE or a subset.

                    var visDifWindow = new Gemma.VisualizationDifferentialWindow({
                        cascadeOnFirstShow: true,
                        title: title,
                        thumbnails: false,
                        downloadLink: String.format(Gemma.CONTEXT_PATH + '/dedv/downloadDEDV.html?ee={0}&g={1}', ee.id, geneId),
                        readMethod: DEDVController.getDEDVForDiffExVisualizationByExperiment
                    });
                    visDifWindow.show({
                        params: [ee.id, geneId, null, ee.sourceExperiment != null]
                    });
                }
            }

        });

/*
 * Gemma.ExpressionExperimentGrid.updateDatasetInfo = function(datasets, eeMap) { for (var i = 0; i < datasets.length;
 * ++i) { var ee = eeMap[datasets[i].id]; if (ee) { datasets[i].shortName = ee.shortName; datasets[i].name = ee.name; } } };
 */

/* stylers */
Gemma.ProbeLevelDiffExGrid.getEEStyler = function () {
    if (Gemma.ProbeLevelDiffExGrid.eeNameStyler === undefined) {
        Gemma.ProbeLevelDiffExGrid.eeNameTemplate = new Ext.XTemplate(
            '<tpl for="."><a target="_blank" title="{name}" href="' + Gemma.CONTEXT_PATH + '/expressionExperiment/showExpressionExperiment.html?id=',
            '{[values.sourceExperiment ? values.sourceExperiment : values.id]}"',
            ' ext:qtip="{name}">{[values.sourceExperiment ? "Subset of " + values.sourceExperiment : values.shortName]}</a></tpl>');
        Gemma.ProbeLevelDiffExGrid.eeNameStyler = function (value, metadata, record, row, col, ds) {
            var ee = record.data.expressionExperiment;
            return Gemma.ProbeLevelDiffExGrid.eeNameTemplate.apply(ee);
        };
    }
    return Gemma.ProbeLevelDiffExGrid.eeNameStyler;
};

Gemma.ProbeLevelDiffExGrid.getEENameStyler = function () {
    if (Gemma.ProbeLevelDiffExGrid.eeStyler === undefined) {
        Gemma.ProbeLevelDiffExGrid.eeTemplate = new Ext.Template("{name}");
        Gemma.ProbeLevelDiffExGrid.eeStyler = function (value, metadata, record, row, col, ds) {
            var ee = record.data.expressionExperiment;
            return Gemma.ProbeLevelDiffExGrid.eeTemplate.apply(ee);
        };
    }
    return Gemma.ProbeLevelDiffExGrid.eeStyler;
};

Gemma.ProbeLevelDiffExGrid.getProbeStyler = function () {
    if (Gemma.ProbeLevelDiffExGrid.probeStyler === undefined) {
        Gemma.ProbeLevelDiffExGrid.probeStyler = function (value, metadata, record, row, col, ds) {

            var probe = record.data.probe;

            if (record.data.fisherContribution) {
                return "<span style='color:#3A3'>" + probe + "</span>";
            } else {
                return "<span style='color:#808080'>" + probe + "</span>";
            }
        };
    }
    return Gemma.ProbeLevelDiffExGrid.probeStyler;
};

Gemma.ProbeLevelDiffExGrid.getEFStyler = function () {
    if (Gemma.ProbeLevelDiffExGrid.efStyler === undefined) {
        Gemma.ProbeLevelDiffExGrid.efTemplate = new Ext.XTemplate(
            '<tpl for=".">',
            // "<a target='_blank' ext:qtip='{factorValues}'>{name}</a>\n",
            "<div ext:qtip='{factorValues}'>{name}</div>", '</tpl>'
        );
        Gemma.ProbeLevelDiffExGrid.efStyler = function (value, metadata, record, row, col, ds) {
            var efs = record.data.experimentalFactors;
            return Gemma.ProbeLevelDiffExGrid.efTemplate.apply(efs);
        };
    }
    return Gemma.ProbeLevelDiffExGrid.efStyler;
};