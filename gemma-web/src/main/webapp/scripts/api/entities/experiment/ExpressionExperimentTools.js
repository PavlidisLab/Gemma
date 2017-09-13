Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

/**
 *
 * Used as one tab of the EE page - the "Admin" tab.
 *
 * pass in the ee details obj as experimentDetails
 *
 * @class Gemma.ExpressionExperimentDetails
 * @extends Gemma.CurationTools
 *
 */
Gemma.ExpressionExperimentTools = Ext.extend(Gemma.CurationTools, {

    experimentDetails: null,
    tbar: new Ext.Toolbar(),

    /**
     * @memberOf Gemma.ExpressionExperimentTools
     */
    initComponent: function () {
        this.curatable = this.experimentDetails;
        this.auditable = {
            id: this.experimentDetails.id,
            classDelegatingFor: "ubic.gemma.model.expression.experiment.ExpressionExperiment"
        };
        Gemma.ExpressionExperimentTools.superclass.initComponent.call(this);
        var manager = new Gemma.EEManager({
            editable: this.editable
        });
        manager.on('reportUpdated', function () {
            this.fireEvent('reloadNeeded');
        }, this);
        var refreshButton = new Ext.Button({
            text: 'Refresh',
            icon: '/Gemma/images/icons/arrow_refresh_small.png',
            tooltip: 'Refresh statistics (not including the differential expression ones)',
            handler: function () {
                manager.updateEEReport(this.experimentDetails.id);
            },
            scope: this

        });

        this.getTopToolbar().addButton(refreshButton);

        this.add({
            html: '<hr class="normal"/><h4>Preprocessing:<br></h4>'
        });
        var missingValueInfo = this.missingValueAnalysisPanelRenderer(this.experimentDetails, manager);
        this.add(missingValueInfo);
        var processedVectorInfo = this.processedVectorCreatePanelRenderer(this.experimentDetails, manager);
        this.add(processedVectorInfo);
        // PCA analysis
        var pcaInfo = this.pcaPanelRenderer(this.experimentDetails, manager);
        this.add(pcaInfo);
        // Batch information
        var batchInfo = this.batchPanelRenderer(this.experimentDetails, manager);
        this.add(batchInfo);
        this.add({html: "<br/>"});
        var batchConfoundPanel = this.batchConfoundRenderer();
        this.add(batchConfoundPanel);
        var differentialAnalysisInfo = this.differentialAnalysisPanelRenderer(this.experimentDetails, manager);
        this.add(differentialAnalysisInfo);
        var linkAnalysisInfo = this.linkAnalysisPanelRenderer(this.experimentDetails, manager);
        this.add(linkAnalysisInfo);

    },

    batchConfoundRenderer: function(){
        if (this.experimentDetails.batchConfound !== null && this.experimentDetails.batchConfound !== "") {

            var panelBC = new Ext.Panel({
                layout: 'hbox',
                defaults: {
                    border: false,
                    padding: 2
                },
                items: []
            });

            var be = {
                html: '<h4 style="display:inline-block">Analyses:</h4>&nbsp;<i class="dark-yellow fa fa-exclamation-triangle fa-lg" ext:qtip="'
                + this.experimentDetails.batchConfound + " (batch confound)"
                + '"></i> Batch confound '
            };

            panelBC.add(be);

            var self = this;

            var recalculateBCBtn = new Ext.Button({
                tooltip: 'Recalculate batch confound',
                handler: function(b, e) {
                    ExpressionExperimentController.recalculateBatchConfound(self.experimentDetails.id, {
                        callback: function () {
                            window.location.reload();
                        }
                    });
                    b.setIconClass("btn-loading");
                    },
                scope: this,
                cls: 'transparent-btn'
            });

            recalculateBCBtn.setIconClass('btn-not-loading');

            //return recalculateBCBtn;
            panelBC.add(recalculateBCBtn);
            return panelBC;

        } else {
            return {
                html: '<h4>Analyses:</h4>'
            };
        }
    },

    linkAnalysisPanelRenderer: function (ee, manager) {
        var panel = new Ext.Panel({
            layout: 'hbox',
            defaults: {
                border: false,
                padding: 2
            },
            items: [{
                html: 'Link Analysis: '
            }]
        });
        var id = ee.id;
        var runBtn = new Ext.Button({
            icon: '/Gemma/images/icons/control_play_blue.png',
            tooltip: 'missing value computation',
            handler: manager.doLinks.createDelegate(this, [id]),
            scope: this,
            cls: 'transparent-btn'
        });
        if (ee.dateLinkAnalysis) {
            var type = ee.linkAnalysisEventType;
            var color = "#000";
            var suggestRun = true;
            var qtip = 'ext:qtip="Analysis was OK"';
            if (type == 'FailedLinkAnalysisEvent') {
                color = 'red';
                qtip = 'ext:qtip="Analysis failed"';
            } else if (type == 'TooSmallDatasetLinkAnalysisEvent') {
                color = '#CCC';
                qtip = 'ext:qtip="Analysis was too small"';
                suggestRun = false;
            }
            panel.add({
                html: '<span style="color:' + color + ';" ' + qtip + '>'
                + Gemma.Renderers.dateRenderer(ee.dateLinkAnalysis)
            });
            if (suggestRun) {
                panel.add(runBtn);
            }
            return panel;
        } else {
            panel.add({
                html: '<span style="color:#3A3;">Needed</span>&nbsp;'
            });
            panel.add(runBtn);
            return panel;
        }

    },
    missingValueAnalysisPanelRenderer: function (ee, manager) {
        var panel = new Ext.Panel({
            layout: 'hbox',
            defaults: {
                border: false,
                padding: 2
            },
            items: [{
                html: 'Missing values: '
            }]
        });
        var id = ee.id;
        var runBtn = new Ext.Button({
            icon: '/Gemma/images/icons/control_play_blue.png',
            tooltip: 'missing value computation',
            handler: manager.doMissingValues.createDelegate(this, [id]),
            scope: this,
            cls: 'transparent-btn'
        });
        /*
         * Offer missing value analysis if it's possible (this might need tweaking).
         */
        if (ee.technologyType != 'ONECOLOR' && ee.technologyType != 'NONE' && ee.hasEitherIntensity) {

            if (ee.dateMissingValueAnalysis) {
                var type = ee.missingValueAnalysisEventType;
                var color = "#000";
                var suggestRun = true;
                var qtip = 'ext:qtip="OK"';
                if (type == 'FailedMissingValueAnalysisEvent') {
                    color = 'red';
                    qtip = 'ext:qtip="Failed"';
                }

                panel.add({
                    html: '<span style="color:' + color + ';" ' + qtip + '>'
                    + Gemma.Renderers.dateRenderer(ee.dateMissingValueAnalysis) + '&nbsp;'
                });
                if (suggestRun) {
                    panel.add(runBtn);
                }
                return panel;
            } else {
                panel.add({
                    html: '<span style="color:#3A3;">Needed</span>&nbsp;'
                });
                panel.add(runBtn);
                return panel;
            }

        } else {

            panel
                .add({
                    html: '<span ext:qtip="Only relevant for two-channel microarray studies with intensity data available." style="color:#CCF;">NA</span>'
                });
            return panel;
        }
    },

    processedVectorCreatePanelRenderer: function (ee, manager) {
        var panel = new Ext.Panel({
            layout: 'hbox',
            defaults: {
                border: false,
                padding: 2
            },
            items: [{
                html: 'Processed Vector Computation: '
            }]
        });
        var id = ee.id;
        var runBtn = new Ext.Button({
            icon: '/Gemma/images/icons/control_play_blue.png',
            tooltip: 'processed vector computation',
            handler: manager.doProcessedVectors.createDelegate(this, [id]),
            scope: this,
            cls: 'transparent-btn'
        });
        if (ee.dateProcessedDataVectorComputation) {
            var type = ee.processedDataVectorComputationEventType;
            var color = "#000";

            var suggestRun = true;
            var qtip = 'ext:qtip="OK"';
            if (type == 'FailedProcessedVectorComputationEvent') { // note:
                // no
                // such
                // thing.
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            }
            panel.add({
                html: '<span style="color:' + color + ';" ' + qtip + '>'
                + Gemma.Renderers.dateRenderer(ee.dateProcessedDataVectorComputation) + '&nbsp;'
            });
            if (suggestRun) {
                panel.add(runBtn);
            }
            return panel;
        } else {
            panel.add({
                html: '<span style="color:#3A3;">Needed</span>&nbsp;'
            });
            panel.add(runBtn);
            return panel;
        }
    },

    differentialAnalysisPanelRenderer: function (ee, manager) {
        var panel = new Ext.Panel({
            layout: 'hbox',
            defaults: {
                border: false,
                padding: 2
            },
            items: [{
                html: 'Differential Expression Analysis: '
            }]
        });
        var id = ee.id;
        var runBtn = new Ext.Button({
            icon: '/Gemma/images/icons/control_play_blue.png',
            tooltip: 'differential expression analysis',
            handler: manager.doDifferential.createDelegate(this, [id]),
            scope: this,
            cls: 'transparent-btn'
        });
        if (ee.numPopulatedFactors > 0) {
            if (ee.dateDifferentialAnalysis) {
                var type = ee.differentialAnalysisEventType;

                var color = "#000";
                var suggestRun = true;
                var qtip = 'ext:qtip="OK"';
                if (type == 'FailedDifferentialExpressionAnalysisEvent') { // note:
                    // no
                    // such
                    // thing.
                    color = 'red';
                    qtip = 'ext:qtip="Failed"';
                }
                panel.add({
                    html: '<span style="color:' + color + ';" ' + qtip + '>'
                    + Gemma.Renderers.dateRenderer(ee.dateDifferentialAnalysis) + '&nbsp;'
                });
                if (suggestRun) {
                    panel.add(runBtn);
                }
                return panel;
            } else {

                panel.add({
                    html: '<span style="color:#3A3;">Needed</span>&nbsp;'
                });
                panel.add(runBtn);
                return panel;
            }
        } else {

            panel.add({
                html: '<span style="color:#CCF;">NA</span>'
            });
            return panel;
        }
    },

    renderProcessedExpressionVectorCount: function (e) {
        return e.processedExpressionVectorCount ? e.processedExpressionVectorCount : ' [count not available] ';
    },

    /*
     * Get the last date PCA was run, add a button to run PCA
     */
    pcaPanelRenderer: function (ee, manager) {
        var panel = new Ext.Panel({
            layout: 'hbox',
            defaults: {
                border: false,
                padding: 2
            },
            items: [{
                html: 'Principal Component Analysis: '
            }]
        });
        var id = ee.id;
        var runBtn = new Ext.Button({
            icon: '/Gemma/images/icons/control_play_blue.png',
            tooltip: 'principal component analysis',
            // See EEManger.js doPca(id, hasPca)
            handler: manager.doPca.createDelegate(this, [id, true]),
            scope: this,
            cls: 'transparent-btn'
        });

        // Get date and info
        if (ee.datePcaAnalysis) {
            var type = ee.pcaAnalysisEventType;

            var color = "#000";
            var qtip = 'ext:qtip="OK"';
            var suggestRun = true;

            if (type == 'FailedPCAAnalysisEvent') {
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            }
            panel.add({
                html: '<span style="color:' + color + ';" ' + qtip + '>'
                + Gemma.Renderers.dateRenderer(ee.datePcaAnalysis) + '&nbsp;'
            });
        } else
            panel.add({
                html: '<span style="color:#3A3;">Needed</span>&nbsp;'
            });

        panel.add(runBtn);
        return panel;

    },

    /*
     * Get the last date batch info was downloaded, add a button to download
     */
    batchPanelRenderer: function (ee, manager) {
        var panel = new Ext.Panel({
            layout: 'hbox',
            defaults: {
                border: false,
                padding: 2
            },
            items: [{
                html: 'Batch Information: '
            }]
        });
        var id = ee.id;
        var hasBatchInformation = ee.hasBatchInformation;
        var technologyType = ee.technologyType;
        var runBtn = new Ext.Button({
            icon: '/Gemma/images/icons/control_play_blue.png',
            tooltip: 'batch information',
            // See EEManager.js doBatchInfoFetch(id)
            handler: manager.doBatchInfoFetch.createDelegate(this, [id]),
            scope: this,
            cls: 'transparent-btn'
        });

        // Batch info fetching not allowed for RNA seq and other non-microarray data
        if (technologyType == 'NONE') {
            panel.add({
                html: '<span style="color:#CCF; "ext:qtip="Not microarray data">' + 'NA' + '</span>&nbsp;'
            });
            return panel;
        }

        // If present, display the date and info. If batch information exists without date, display 'Provided'.
        // If no batch information, display 'Needed' with button for GEO and ArrayExpress data. Otherwise, NA.
        if (ee.dateBatchFetch) {
            var type = ee.batchFetchEventType;

            var color = "#000";
            var qtip = 'ext:qtip="OK"';

            if (type == 'FailedBatchInformationFetchingEvent') {
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            } else if (type == 'FailedBatchInformationMissingEvent') {
                color = '#CCC';
                qtip = 'ext:qtip="Raw data files not available from source"';
            }

            panel.add({
                html: '<span style="color:' + color + ';" ' + qtip + '>'
                + Gemma.Renderers.dateRenderer(ee.dateBatchFetch) + '&nbsp;'
            });
            panel.add(runBtn);
        } else if (hasBatchInformation) {
            panel.add({
                html: '<span style="color:#000;">Provided</span>'
            });
        } else if (ee.externalDatabase == "GEO" || ee.externalDatabase == "ArrayExpress") {
            panel.add({
                html: '<span style="color:#3A3;">Needed</span>&nbsp;'
            });
            panel.add(runBtn);
        } else
            panel.add({
                html: '<span style="color:#CCF; "'
                + 'ext:qtip="Add batch information by creating a \'batch\' experiment factor">' + 'NA'
                + '</span>&nbsp;'
            });

        return panel;
    }
});
