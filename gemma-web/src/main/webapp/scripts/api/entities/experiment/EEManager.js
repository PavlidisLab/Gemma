/**
 * Common tasks to run on experiments. To use this, construct it with a id defined in the config (e.g., {id :
 * 'eemanager}). Then you can use things like onClick=Ext.getCmp('eemanager').updateEEReport(id).
 *
 * @class Gemma.EEManager
 * @extends Ext.Component
 */
Gemma.EEManager = Ext.extend(Ext.Component, {
    name: 'eemanager',

    record: Ext.data.Record.create([{
        name: "id",
        type: "int"
    }, {
        name: "shortName"
    }, {
        name: "name"
    }, {
        name: "arrayDesignCount",
        type: "int"
    }, {
        name: "technologyType"
    }, {
        name: "hasBothIntensities",
        type: 'bool'
    }, {
        name: "hasEitherIntensity",
        type: 'bool'
    }, {
        name: "bioAssayCount",
        type: "int"
    }, {
        name: "processedExpressionVectorCount",
        type: "int"
    }, {
        name: "externalUri"
    }, {
        name: "externalDatabase"
    }, {
        name: "description"
    }, {
        name: "taxon"
    }, {
        name: "taxonId"
    }, {
        name: "numAnnotations"
    }, {
        name: "numPopulatedFactors"
    }, {
        name: "isPublic",
        type: "boolean"
    }, {
        name: "shared",
        type: "boolean"
    }, {
        name: "userCanWrite"
    }, {
        name: "userOwned"
    }, {
        name: "sourceExperiment"
    }, {
        name: "coexpressionLinkCount"
    }, {
        name: "diffExpressedProbes"
    }, {
        name: "troubled",
        type: "boolean"
    }, {
        name: "troubleDetails"
    }, {
        name: "needsAttention",
        type: "boolean"
    }, {
        name: "curationNote"
    }, {
        name: "missingValueAnalysisEventType"
    }, {
        name: "processedDataVectorComputationEventType"
    }, {
        name: "dateProcessedDataVectorComputation",
        type: 'date'
    }, {
        name: "dateMissingValueAnalysis",
        type: 'date'
    }, {
        name: "dateDifferentialAnalysis",
        type: 'date'
    }, {
        name: "lastUpdated",
        type: 'date'
    }, {
        name: "dateLinkAnalysis",
        type: 'date'
    }, {
        name: "datePcaAnalysis",
        type: 'date'
    }, {
        name: "dateBatchFetch",
        type: 'date'
    }, {
        name: "linkAnalysisEventType"
    }, {
        name: "processedDataVectorComputationEventType"
    }, {
        name: "missingValueAnalysisEventType"
    }, {
        name: "differentialAnalysisEventType"
    }, {
        name: "batchFetchEventType"
    }, {
        name: "pcaAnalysisEventType"
    }, {
        name: "differentialExpressionAnalyses"
    }, {
        name: "geeq"
    }]),

    /**
     *
     * @param id
     * @param throbberEl -
     *           optional element to show the throbber. If omitted, a popup progressbar is shown.
     * @memberOf Gemma.EEManager
     */
    updateEEReport: function (id, throbberEl) {
        var eeManager = this;
        ExpressionExperimentReportGenerationController.run(id, {
            callback: function (taskId) {
                var task = new Gemma.ObservableSubmittedTask({
                    'taskId': taskId
                });
                task.on('task-completed', function (payload) {
                    eeManager.fireEvent('reportUpdated', payload);
                });
                if (throbberEl) {
                    task.showTaskProgressThrobber(throbberEl);
                } else {
                    task.showTaskProgressWindow({});
                }
            },
            errorHandler: eeManager.onTaskSubmissionError
        });
    },

    historyWindow: null,

    showAuditWindow: function (id) {
        if (this.historyWindow !== null) {
            this.historyWindow.destroy();
        }
        this.historyWindow = new Ext.Window({
            layout: 'fit',
            title: 'History',
            modal: false,
            items: [new Gemma.AuditTrailGrid({
                title: '',
                collapsible: false,
                auditable: {
                    id: id,
                    classDelegatingFor: "ubic.gemma.model.expression.experiment.ExpressionExperiment"
                }
            })]
        });
        this.historyWindow.show();
    },

    onTaskSubmissionError: function (message) {
        Ext.Msg.alert("There was an error", message);
        Ext.getBody().unmask();
    },

    updateAllEEReports: function () {
        var eeManager = this;
        ExpressionExperimentReportGenerationController.runAll({
            callback: function (taskId) {
                var task = new Gemma.ObservableSubmittedTask({
                    'taskId': taskId
                });
                task.on('task-completed', function (payload) {
                    eeManager.fireEvent('reportUpdated', payload);
                });
                task.showTaskProgressWindow({
                    'showLogButton': true,
                    'showBackgroundButton': true
                });
            },
            errorHandler: eeManager.onTaskSubmissionError
        });
    },

    /**
     * Break the relationships between bioassays and biomaterials, such that there is only one bioassay per biomaterial.
     */
    unmatchBioAssays: function (id) {
        Ext.Msg.show({
            title: 'Are you sure?',
            msg: 'Are you sure you to unmatch the bioassays? (This has no effect if there is only one platform)',
            buttons: Ext.Msg.YESNO,
            fn: function (btn, text) {
                if (btn == 'yes') {
                    var callParams = [];
                    callParams.push(id);
                    callParams.push({
                        callback: function (data) {
                            // var k = new Gemma.WaitHandler();
                            // this.relayEvents(k, ['done',
                            // 'fail']);
                            // k.handleWait(data, false);
                            this.fireEvent('done');
                        }.createDelegate(this),
                        errorHandler: function (message, exception) {
                            Ext.Msg.alert("There was an error", message);
                            Ext.getBody().unmask();
                        }
                    });

                    ExpressionExperimentController.unmatchAllBioAssays.apply(this, callParams);
                }
            },
            scope: this
        });
    },

    /**
     * Display the annotation tagger window.
     *
     * @param id
     * @param taxonId
     * @param canEdit
     */
    tagger: function (id, taxonId, canEdit) {
        var annotator = new Ext.Panel({
            id: 'annotator-wrap',
            collapsible: false,
            stateful: false,
            bodyBorder: false,
            layout: 'fit',
            items: [new Gemma.AnnotationGrid({
                id: 'annotator-grid',
                readMethod: ExpressionExperimentController.getAnnotation,
                writeMethod: ExpressionExperimentController.createExperimentTag,
                removeMethod: ExpressionExperimentController.removeExperimentTag,
                readParams: [{
                    id: id
                }],
                editable: canEdit,
                showParent: false,
                taxonId: taxonId,
                entId: id
            })]
        });

        this.change = false;
        Ext.getCmp('annotator-grid').on('refresh', function () {
            this.change = true;
        }.createDelegate(this));

        var w = new Ext.Window({
            modal: false,
            stateful: false,
            title: "Experiment tags",
            layout: 'fit',
            width: 600,
            height: 200,
            items: [annotator],
            buttons: [
                {
                    text: 'Help',
                    handler: function () {
                        Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.AnnotationGrid.taggingHelpTitle,
                            Gemma.HelpText.WidgetDefaults.AnnotationGrid.taggingHelpText);
                    }
                },
                {
                    text: 'Done',
                    handler: function () {

                        var r = Ext.getCmp('annotator-grid').getEditedCharacteristics();

                        if (r.length > 0) {
                            Ext.Msg.confirm(Gemma.HelpText.CommonWarnings.UnsavedChanges.title,
                                Gemma.HelpText.CommonWarnings.UnsavedChanges.text, function (btn, txt) {
                                    if (btn === 'OK') {
                                        w.hide();
                                    }
                                });
                        } else {
                            w.hide();
                        }

                        if (this.change) {
                            /* Update the display of the tags. */
                            this.fireEvent('tagsUpdated');
                        }
                    },
                    scope: this
                }]
        });
        w.show();
    },

    deleteExperiment: function (id, redirectHome) {
        var eeManager = this;
        Ext.Msg.show({
            title: Gemma.HelpText.CommonWarnings.Deletion.title,
            msg: String.format(Gemma.HelpText.CommonWarnings.Deletion.text, 'experiment'),
            buttons: Ext.Msg.YESNO,
            fn: function (btn, text) {
                if (btn === 'yes') {
                    Ext.getBody().mask();
                    ExpressionExperimentController.deleteById(id, {
                        callback: function (taskId) {
                            var task = new Gemma.ObservableSubmittedTask({
                                'taskId': taskId
                            });
                            task.showTaskProgressWindow({
                                'showLogButton': true,
                                'showBackgroundButton': true
                            });
                            task.on('task-completed', function (payload) {
                                eeManager.fireEvent('deleted', redirectHome);
                            });
                            Ext.getBody().unmask();
                        },
                        errorHandler: eeManager.onTaskSubmissionError
                    });
                }
            },
            animEl: 'elId',
            icon: Ext.MessageBox.WARNING
        });
    },

    deleteExperimentAnalysis: function (eeId, analysisId, redirectHome) {
        var eeManager = this;
        Ext.Msg.show({
            title: Gemma.HelpText.CommonWarnings.Deletion.title,
            msg: String.format(Gemma.HelpText.CommonWarnings.Deletion.text, 'analysis'),
            buttons: Ext.Msg.YESNO,
            fn: function (btn, text) {
                if (btn === 'yes') {
                    Ext.getBody().mask();
                    DifferentialExpressionAnalysisController.remove(eeId, analysisId, {
                        callback: function (taskId) {
                            var task = new Gemma.ObservableSubmittedTask({
                                'taskId': taskId
                            });
                            task.showTaskProgressWindow({
                                'showLogButton': true,
                                'showBackgroundButton': true
                            });
                            Ext.getBody().unmask();
                            task.on('task-completed', function (payload) {
                                eeManager.fireEvent('deletedAnalysis');
                            });
                        },
                        errorHandler: eeManager.onTaskSubmissionError
                    });
                }
            },
            animEl: 'elId',
            icon: Ext.MessageBox.WARNING
        });
    },

    redoExperimentAnalysis: function (eeId, analysisId, redirectHome) {
        var eeManager = this;
        Ext.Msg.show({
            title: Gemma.HelpText.CommonWarnings.Redo.title,
            msg: String.format(Gemma.HelpText.CommonWarnings.Redo.text, 'analysis'),
            buttons: Ext.Msg.YESNO,
            fn: function (btn, text) {
                if (btn === 'yes') {
                    Ext.getBody().mask();
                    DifferentialExpressionAnalysisController.redo(eeId, analysisId, {
                        callback: function (taskId) {
                            var task = new Gemma.ObservableSubmittedTask({
                                'taskId': taskId
                            });
                            task.on('task-completed', function (payload) {
                                eeManager.fireEvent('differential', payload);
                            });
                            Ext.getBody().unmask();
                            task.showTaskProgressWindow({
                                'showLogButton': true,
                                'showBackgroundButton': true
                            });
                        },
                        errorHandler: eeManager.onTaskSubmissionError
                    });
                }
            },
            animEl: 'elId',
            icon: Ext.MessageBox.WARNING
        });
    },

    refreshDiffExStats: function (eeId, analysisId, redirectHome) {
        var eeManager = this;
        Ext.Msg.show({
            title: Gemma.HelpText.CommonWarnings.RefreshStats.title,
            msg: String.format(Gemma.HelpText.CommonWarnings.RefreshStats.text, 'analysis'),
            buttons: Ext.Msg.YESNO,
            fn: function (btn, text) {
                if (btn === 'yes') {
                    Ext.getBody().mask();
                    DifferentialExpressionAnalysisController.refreshStats(eeId, analysisId, {
                        callback: function (taskId) {
                            Ext.getBody().unmask();
                            var task = new Gemma.ObservableSubmittedTask({
                                'taskId': taskId
                            });
                            task.on('task-completed', function (payload) {
                                // this isn't necessarily the right event, but seems reasonable.
                                eeManager.fireEvent('differential', payload);
                            });
                            task.showTaskProgressWindow({
                                'showLogButton': true,
                                'showBackgroundButton': true
                            });
                        },
                        errorHandler: eeManager.onTaskSubmissionError
                    });
                }
            },
            animEl: 'elId',
            icon: Ext.MessageBox.WARNING
        });
    },

    /**
     * Deprecated, use collection version instead.
     */
    markOutlierBioAssay: function (bioAssayId) {
        var eeManager = this;
        Ext.Msg.show({
            title: 'Are you sure?',
            msg: 'Are you sure you want to mark this bioAssay as an outlier?',
            buttons: Ext.Msg.YESNO,
            fn: function (btn, text) {
                if (btn === 'yes') {
                    Ext.getBody().mask();
                    BioAssayController.markOutlier([bioAssayId], {
                        callback: function (taskId) {
                            Ext.getBody().unmask();
                            var task = new Gemma.ObservableSubmittedTask({
                                'taskId': taskId
                            });
                            task.showTaskProgressWindow({
                                'showLogButton': true,
                                'showBackgroundButton': true
                            });
                            Ext.getBody().unmask();
                        },
                        errorHandler: eeManager.onTaskSubmissionError
                    });
                }
            },
            animEl: 'elId',
            icon: Ext.MessageBox.WARNING
        });
    },

    /**
     *
     *
     * @param bioAssayIds
     */
    markOutlierBioAssays: function (bioAssayIds) {
        var eeManager = this;
        Ext.Msg.show({
            title: 'Are you sure?',
            msg: 'This will mark selected assays as outliers.',
            buttons: Ext.Msg.YESNO,
            fn: function (btn, text) {
                if (btn === 'yes') {
                    Ext.getBody().mask();
                    BioAssayController.markOutlier(bioAssayIds, {
                        callback: function (taskId) {
                            Ext.getBody().unmask();
                            var task = new Gemma.ObservableSubmittedTask({
                                'taskId': taskId
                            });
                            task.showTaskProgressWindow({
                                'showLogButton': true,
                                'showBackgroundButton': true
                            });
                            Ext.getBody().unmask();
                        },
                        errorHandler: eeManager.onTaskSubmissionError
                    });
                }
            },
            animEl: 'elId',
            icon: Ext.MessageBox.WARNING
        });
    },

    /**
     * Un-mark a single outlier
     */
    unMarkOutlierBioAssays: function (bioAssayIds) {
        var eeManager = this;
        Ext.Msg.show({
            title: 'Are you sure?',
            msg: 'This will mark de-selected assays as non-outliers.',
            buttons: Ext.Msg.YESNO,
            fn: function (btn, text) {
                if (btn === 'yes') {
                    Ext.getBody().mask();
                    BioAssayController.unmarkOutlier(bioAssayIds, {
                        callback: function (taskId) {
                            var task = new Gemma.ObservableSubmittedTask({
                                'taskId': taskId
                            });
                            task.showTaskProgressWindow({
                                'showLogButton': true,
                                'showBackgroundButton': true
                            });
                            Ext.getBody().unmask();
                        },
                        errorHandler: eeManager.onTaskSubmissionError
                    });
                }
            },
            animEl: 'elId',
            icon: Ext.MessageBox.WARNING
        });
    },

    /**
     * Compute coexpression for the data set.
     */
    doLinks: function (id) {
        var eeManager = this;
        Ext.Msg.show({
            title: 'Link analysis',
            msg: 'Please confirm. Previous analysis results will be deleted.',
            buttons: Ext.Msg.YESNO,
            fn: function (btn, text) {
                if (btn === 'yes') {
                    Ext.getBody().mask();
                    LinkAnalysisController.run(id, {
                        callback: function (taskId) {
                            Ext.getBody().unmask();
                            var task = new Gemma.ObservableSubmittedTask({
                                'taskId': taskId
                            });
                            task.showTaskProgressWindow({
                                'showLogButton': true,
                                'showBackgroundButton': true
                            });
                            task.on('task-completed', function (payload) {
                                eeManager.fireEvent('link', payload);
                            });
                        },
                        errorHandler: eeManager.onTaskSubmissionError
                    });
                }
            },
            animEl: 'elId',
            icon: Ext.MessageBox.WARNING
        });
    },

    doPca: function (id, hasPca) {
        var eeManager = this;
        Ext.Msg.show({
            title: 'PCA analysis',
            msg: 'Please confirm. Any previous PCA results will be deleted',
            buttons: Ext.Msg.YESNO,
            fn: function (btn, text) {
                Ext.getBody().mask();
                SvdController.run(id, {
                    callback: function (taskId) {
                        Ext.getBody().unmask();
                        var task = new Gemma.ObservableSubmittedTask({
                            'taskId': taskId
                        });
                        task.showTaskProgressWindow({
                            'showLogButton': true,
                            'showBackgroundButton': true
                        });
                        task.on('task-completed', function (payload) {
                            eeManager.fireEvent('pca', payload);
                        });
                    },
                    errorHandler: eeManager.onTaskSubmissionError
                });
            },
            animEl: 'elId',
            icon: Ext.MessageBox.WARNING
        });
    },

    visualizePcaHandler: function (eeid, component, count) {
        this.vispcaWindow = new Gemma.VisualizationWithThumbsWindow({
            thumbnails: false,
            readMethod: DEDVController.getDEDVForPcaVisualization,
            title: "Top loaded elements for PC" + component,
            showLegend: false,
            downloadLink: String.format(Gemma.CONTEXT_PATH + "/dedv/downloadDEDV.html?ee={0}&component={1}&thresh={2}&pca=1", eeid,
                component, count)
        });
        this.vispcaWindow.show({
            params: [eeid, component, count]
        });
    },

    doBatchInfoFetch: function (id) {
        var eeManager = this;
        Ext.Msg.show({
            title: 'Sample batches information fetcher',
            msg: 'Please confirm. Previous results will be deleted, including "batch" factor.',
            buttons: Ext.Msg.YESNO,
            fn: function (btn, text) {
                if (btn === 'yes') {
                    Ext.getBody().mask();
                    BatchInfoFetchController.run(id, {
                        callback: function (taskId) {
                            Ext.getBody().unmask();
                            var task = new Gemma.ObservableSubmittedTask({
                                'taskId': taskId
                            });
                            task.showTaskProgressWindow({
                                'showLogButton': true,
                                'showBackgroundButton': true
                            });
                            task.on('task-completed', function (payload) {
                                eeManager.fireEvent('batchinfo', payload);
                            });
                        },
                        errorHandler: eeManager.onTaskSubmissionError
                    });
                }
            },
            animEl: 'elId',
            icon: Ext.MessageBox.WARNING
        });
    },

    /**
     * Compute the missing values. This is only relevant for two-channel arrays.
     */
    doMissingValues: function (id) {
        var eeManager = this;
        Ext.Msg.show({
            title: 'Missing value analysis',
            msg: 'Please confirm. Previous analysis results will be deleted.',
            buttons: Ext.Msg.YESNO,
            fn: function (btn, text) {
                if (btn === 'yes') {
                    Ext.getBody().mask();
                    TwoChannelMissingValueController.run(id, {
                        callback: function (taskId) {
                            Ext.getBody().unmask();
                            var task = new Gemma.ObservableSubmittedTask({
                                'taskId': taskId
                            });
                            task.showTaskProgressWindow({
                                'showLogButton': true,
                                'showBackgroundButton': true
                            });
                            task.on('task-completed', function (payload) {
                                eeManager.fireEvent('missingValue', payload);
                            });
                        },
                        errorHandler: eeManager.onTaskSubmissionError
                    });
                }
            },
            animEl: 'elId',
            icon: Ext.MessageBox.WARNING
        });
    },

    /**
     * Interactive setup and running of a differential expression analysis.
     */
    doDifferential: function (id) {
        var eeManager = this;
        /*
         * Do an analysis interactively.
         */
        var customize = function (analysisInfo) {
            var factors = analysisInfo.factors;
            var proposedAnalysis = analysisInfo.type;
            /*
             * Set up buttons for the subset form.
             */
            var subsetRadios = [];
            subsetRadios.push(new Ext.form.Radio({
                boxLabel: 'None', // need so they can unset it.
                name: 'diff-ex-analyze-subset', // same name ->
                // grouped.
                id: 'no-factor-subset-radio',
                checked: true,
                listeners: {
                    check: validateFactorsChosen.createDelegate(this, [factors])
                }
            }));

            for (var i = 0; i < factors.length; i++) {
                var f = factors[i];
                if (!f.name) {
                    continue;
                }

                /*
                 * set up the subset radios
                 */
                subsetRadios.push(new Ext.form.Radio({
                    boxLabel: "<b>" + f.name + "</b> (" + Ext.util.Format.ellipsis(f.description, 50) + ")",
                    name: 'diff-ex-analyze-subset', // same
                    // name
                    // ->
                    // grouped.
                    id: f.id + '-factor-subset-radio',
                    checked: false
                }));
            }

            /*
             * DifferentialExpressionAnalysisCustomization - only available if there is more than one factor. We should
             * refactor this code.
             */
            var deasw = new Ext.Window({
                name: 'diff-customization-window',
                modal: true,
                stateful: false,
                resizable: false,
                autoHeight: true,
                width: 460,
                plain: true,
                border: false,
                title: "Differential analysis settings",
                padding: 10,
                items: [{
                    xtype: 'form',
                    bodyBorder: false,
                    autoHeight: true,
                    items: [{
                        xtype: 'fieldset',
                        title: "Select factor(s) to use",
                        autoHeight: true,
                        labelWidth: 375,
                        id: 'diff-ex-analysis-customize-factors'
                    }, {
                        xtype: 'fieldset',
                        title: "Optional: Select a subset factor",
                        items: [{
                            xtype: 'radiogroup',
                            columns: 1,
                            allowBlank: true,
                            autoHeight: true,
                            id: 'diff-ex-analysis-subset-factors',
                            items: subsetRadios,
                            hideLabel: true,
                            listeners: {
                                change: validateFactorsChosen.createDelegate(this, [factors])
                            }
                        }]
                    },

                        {
                            xtype: 'fieldset',
                            labelWidth: 375,
                            autoHeight: true,
                            hidden: false,

                            /*
                             * we hide this if we have more than 2 factors -- basically where we're not going to bother supporting
                             * interactions.
                             */
                            items: [{
                                xtype: 'checkbox',
                                id: 'diff-ex-analysis-customize-include-interactions-checkbox',
                                fieldLabel: 'Include interactions if possible'
                            }]
                        }]
                }],

                buttons: [
                    {
                        text: "Help",
                        id: 'diff-ex-customize-help-button',
                        disabled: false,
                        scope: this,
                        handler: function () {
                            Ext.Msg.show({
                                title: Gemma.HelpText.WidgetDefaults.EEManager.customiseDiffExHelpTitle,
                                msg: Gemma.HelpText.WidgetDefaults.EEManager.customiseDiffExHelpText,
                                buttons: Ext.Msg.OK,
                                icon: Ext.MessageBox.INFO
                            });
                        }
                    },
                    {
                        text: 'Proceed',
                        id: 'diff-ex-customize-proceed-button',
                        disabled: false,
                        scope: this,
                        handler: function (btn, text) {

                            var includeInteractions = Ext.getCmp(
                                'diff-ex-analysis-customize-include-interactions-checkbox').getValue();

                            /*
                              * Get the factors the user checked. See checkbox creation code below.
                              */
                            var factorsToUseIds = getFactorsToUseIds(factors);
                            var subsetFactor = getSubsetFactorId(factors);

                            if (factorsToUseIds.length < 1) {
                                Ext.Msg.alert("Invalid selection", "Please pick at least one factor.");
                                return;
                            }

                            /*
                              * This should be disallowed by the interface, but just in case.
                              */
                            if (subsetFactor !== null && factorsToUseIds.indexOf(subsetFactor) >= 0) {
                                Ext.Msg.alert("Invalid selection",
                                    "You cannot subset on a factor included in the model.");
                                return;
                            }

                            /*
                              * Pass back the factors to be used, and the choice of whether interactions are to be
                              * used.
                              */
                            Ext.getBody().mask();
                            DifferentialExpressionAnalysisController.runCustom(id, factorsToUseIds,
                                includeInteractions, subsetFactor, {
                                    callback: function (taskId) {
                                        Ext.getBody().unmask();
                                        var task = new Gemma.ObservableSubmittedTask({
                                            'taskId': taskId
                                        });
                                        task.on('task-completed', function (payload) {
                                            eeManager.fireEvent('differential', payload);
                                        });
                                        task.showTaskProgressWindow({
                                            'showLogButton': true,
                                            'showBackgroundButton': true
                                        });
                                    },
                                    errorHandler: eeManager.onTaskSubmissionError
                                });
                            deasw.close();
                        }
                    }, {
                        text: 'Cancel',
                        handler: function () {
                            deasw.close();
                        }
                    }]
            });

            /*
             * Create the checkboxes for user choice of factors. We assume there is more than one.
             */
            if (factors) {
                for (var i = 0; i < factors.length; i++) {
                    var f = factors[i];
                    if (!f.name) {
                        continue;
                    }

                    /*
                     * For now we don't allow analyzing batch as a factor (though it can be used for subsetting)
                     */
                    if (f.name === "batch") { // ExperimentalFactorService.BATCH_FACTOR_NAME
                        continue;
                    }

                    /*
                     * Checkbox for one factor.
                     */

                    Ext.getCmp('diff-ex-analysis-customize-factors').add(new Ext.form.Checkbox({
                        fieldLabel: "<b>" + f.name + "</b> (" + Ext.util.Format.ellipsis(f.description) + ")",
                        // labelWidth : 375,
                        id: String.format("{0}-factor-checkbox", f.id),
                        tooltip: f.name,
                        checked: false,
                        listeners: {
                            check: validateFactorsChosen.createDelegate(this, [factors])
                        }
                    }));
                }

            }

            deasw.doLayout();
            deasw.show();
        };

        var getFactorsToUseIds = function (factors) {
            var factorsToUseIds = [];
            for (var i = 0; i < factors.length; i++) {
                var f = factors[i];
                if (!f.name || f.name === 'batch') {
                    continue;
                }
                var chkbox = Ext.getCmp(String.format("{0}-factor-checkbox", f.id));
                var checked = chkbox.getValue();
                if (checked) {
                    factorsToUseIds.push(f.id);
                }
            }
            return factorsToUseIds;
        };

        var getSubsetFactorId = function (factors) {
            var subsetFactor = null;
            /*
             * get values of subset radios
             */
            for (var i = 0; i < factors.length; i++) {
                var f = factors[i];
                if (!f.name) {
                    continue;
                }
                var checked = Ext.getCmp(f.id + '-factor-subset-radio').getValue();
                if (checked) {
                    subsetFactor = f.id;
                    break;
                }
            }
            return subsetFactor;
        };

        /**
         * Callback for analysis type determination. This gets the type of analysis, if it can be determined. If the type
         * is non-null, then just ask the user for confirmation. If they say no, or the type is null, show them the
         * DifferentialExpressionAnalysisSetupWindow.
         */
        var cb = function (analysisInfo) {
            if (analysisInfo.type) {
                var customizable = false;
                // console.log(analysisInfo.type);
                var analysisType = '';
                if (analysisInfo.type === 'TWO_WAY_ANOVA_WITH_INTERACTION') {
                    analysisType = 'Two-way ANOVA with interactions';
                    customizable = true;
                } else if (analysisInfo.type === 'TWO_WAY_ANOVA_NO_INTERACTION') {
                    analysisType = 'Two-way ANOVA without interactions';
                    customizable = true;
                } else if (analysisInfo.type === 'TTEST') {
                    analysisType = 'T-test (two-sample)';
                } else if (analysisInfo.type === 'OSTTEST') {
                    analysisType = 'T-test (one-sample)';
                } else if (analysisInfo.type === 'OWA') {
                    analysisType = 'One-way ANOVA';
                } else {
                    analysisType = 'Generic ANOVA/ANCOVA';
                    customizable = true;
                }

                // ask for confirmation.
                var w = new Ext.Window({
                    name: 'diffex-dialog',
                    autoCreate: true,
                    resizable: false,
                    constrain: true,
                    constrainHeader: true,
                    minimizable: false,
                    maximizable: false,
                    stateful: false,
                    modal: true,
                    shim: true,
                    buttonAlign: "center",
                    width: 400,
                    height: 130,
                    minHeight: 80,
                    plain: true,
                    footer: true,
                    closable: true,
                    title: 'Differential expression analysis',
                    html: 'Please confirm. The analysis performed will be a ' + analysisType
                    + '. If there is an existing analysis on the same factor(s), it will be deleted. '
                    + 'To redo or refresh a specific analysis, use the controls on the experiment\'s main tab.',
                    buttons: [{
                        text: 'Proceed',
                        handler: function (btn, text) {
                            Ext.getBody().mask();
                            DifferentialExpressionAnalysisController.run(id, {
                                callback: function (taskId) {
                                    Ext.getBody().unmask();
                                    var task = new Gemma.ObservableSubmittedTask({
                                        'taskId': taskId
                                    });
                                    task.showTaskProgressWindow({
                                        'showLogButton': true,
                                        'showBackgroundButton': true
                                    });
                                    task.on('task-completed', function (payload) {
                                        eeManager.fireEvent('differential', payload);
                                    });
                                },
                                errorHandler: eeManager.onTaskSubmissionError
                            });
                            w.close();
                        }
                    }, {
                        text: 'Cancel',
                        handler: function () {
                            w.close();
                        }
                    }, {
                        disabled: !customizable,
                        hidden: !customizable,
                        text: 'Customize',
                        handler: function () {
                            w.close();
                            customize(analysisInfo);
                        }
                    }],
                    iconCls: Ext.MessageBox.QUESTION
                });

                w.show();

            } else {
                /*
                 * System couldn't guess the analysis type, so force user to customize.
                 */
                customize(analysisInfo);
            }
        };

        /*
         * Make sure checkboxes are logically consistent (warning: this might not work 100% perfectly, so it's a good idea
         * to validate again later on the client side)
         */
        var validateFactorsChosen = function (factors) {
            var factorsToUseIds = getFactorsToUseIds(factors);
            var subsetFactor = getSubsetFactorId(factors);

            if (factorsToUseIds.length !== 2) {
                Ext.getCmp('diff-ex-analysis-customize-include-interactions-checkbox').setValue(false);
                Ext.getCmp('diff-ex-analysis-customize-include-interactions-checkbox').disable();
            } else {
                Ext.getCmp('diff-ex-analysis-customize-include-interactions-checkbox').enable();
            }

            /*
             * The top checkboxes take precendence. We unset the 'subset' if there is a conflict.
             */
            if (subsetFactor !== null && factorsToUseIds.indexOf(subsetFactor) >= 0) {
                Ext.getCmp(subsetFactor + '-factor-subset-radio').setValue(false);
                Ext.getCmp('no-factor-subset-radio').setValue(true);
                Ext.getCmp(subsetFactor + '-factor-checkbox').disable();
            } else {
                for (var i = 0; i < factorsToUseIds.length; i++) {
                    Ext.getCmp(factorsToUseIds[i] + '-factor-checkbox').enable();
                }
            }
        };

        /*
         * Get the analysis type.
         */
        var eh = function (error) {
            Ext.Msg.alert("There was an error", error);
        };

        DifferentialExpressionAnalysisController.determineAnalysisType(id, {
            callback: cb,
            errorhandler: eh
        });

    },

    /*
     * Run the vector processing and downstream effects.
     */
    doProcessedVectors: function (id) {
        var eeManager = this;
        Ext.Msg.show({
            title: 'Preprocess',
            msg: 'Please confirm. Any existing processed vectors, PCA and other down-stream outputs will be deleted and updated.',
            buttons: Ext.Msg.YESNO,
            fn: function (btn, text) {
                if (btn === 'yes') {
                    Ext.getBody().mask();
                    PreprocessController.run(id, {
                        callback: function (taskId) {
                            Ext.getBody().unmask();
                            var task = new Gemma.ObservableSubmittedTask({
                                'taskId': taskId
                            });
                            task.on('task-completed', function (payload) {
                                eeManager.fireEvent('processedVector', payload);
                            });
                            task.showTaskProgressWindow({
                                'showLogButton': true,
                                'showBackgroundButton': true
                            });
                        },
                        errorHandler: eeManager.onTaskSubmissionError
                    });
                }
            },
            animEl: 'elId',
            icon: Ext.MessageBox.WARNING
        });
    },
    
    /*
     * Just update diagnostics (PCA, sample correlation and MV)
     */
    doDiagnostics :  function (id) {
       var eeManager = this;
       Ext.Msg.show({
           title: 'Update diagnostics',
           msg: 'Please confirm. PCA, sample correlation and M-V will be created or updated.',
           buttons: Ext.Msg.YESNO,
           fn: function (btn, text) {
               if (btn === 'yes') {
                   Ext.getBody().mask();
                   PreprocessController.diagnostics(id, {
                       callback: function (taskId) {
                           Ext.getBody().unmask();
                           var task = new Gemma.ObservableSubmittedTask({
                               'taskId': taskId
                           });
                           task.on('task-completed', function (payload) {
                               eeManager.fireEvent('diagnostics', payload);
                           });
                           task.showTaskProgressWindow({
                               'showLogButton': true,
                               'showBackgroundButton': true
                           });
                       },
                       errorHandler: eeManager.onTaskSubmissionError
                   });
               }
           },
           animEl: 'elId',
           icon: Ext.MessageBox.WARNING
       });
   },

    initComponent: function () {
        Gemma.EEManager.superclass.initComponent.call(this);

        this.addEvents('done', 'reportUpdated', 'differential', 'missingValue', 'link', 'processedVector', 'deleted',
            'tagsUpdated', 'updated', 'pca', 'batchinfo');

        this.on('deleted', function (redirectHome) {
            if (redirectHome) {
                window.location = Gemma.CONTEXT_PATH + '/home.html';

            } else {
                /* after deletion, clear bottom details pane */
                Ext.get('dataSetDetailsPanel').first().last().dom.innerHTML = '<span></span>';
            }
        });
    }
});