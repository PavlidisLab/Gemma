Ext.namespace('Gemma');

function scoreToColor(i) {
    // normalize from [-1,1] to [0,1]
    i = i + 1;
    i = i / 2;
    // hsl red = 0° and green = 120°
    var hue = i * 120;
    return 'hsl(' + hue + ', 100%, 70%)';
}

function getStatusBadge(faIconClass, colorClass, title, qTip) {
    return '<span class="ee-status-badge bg-' + colorClass + ' " ext:qtip="' + qTip + '" >' +
        '<i class=" fa fa-' + faIconClass + ' fa-lg"></i> ' + title + '</span>';
}

function getGeeqBadges(quality, suitability) {
    return '' +
        '<span class="ee-status-badge geeq-badge" style="background-color: ' + scoreToColor(Number(quality)) + '" ' +
        'ext:qtip="Quality:&nbsp;' + roundScore(quality, 1) + '<br/>' +
        'Quality refers to data quality, wherein the same study could have been done twice with the same technical parameters and in one case yield bad quality data, and in another high quality data." >' +
        getGeeqIcon(Number(quality)) + "" +
        '</span>' +
        '<span class="ee-status-badge geeq-badge" style="background-color: ' + scoreToColor(Number(suitability)) + '" ' +
        'ext:qtip="Suitability:&nbsp;' + roundScore(suitability, 1) + '<br/>' +
        'Suitability refers to technical aspects which, if we were doing the study ourselves, we would have altered to make it optimal for analyses of the sort used in Gemma." >' +
        getGeeqIcon(Number(suitability)) + "" +
        '</span>';
}

function getGeeqIcon(score) {
    return "<i class='fa fa-lg " + getSmileyCls(score) + "'></i></span>";
}

function getGeeqIconColored(score) {
    return '' +
        '<span class="fa fa-lg fa-stack" ext:qtip="Suitability:&nbsp;' + roundScore(score, 1) + '">' +
        '   <i class="fa fa-stack-1x fa-circle" style="color: ' + scoreToColor(Number(score)) + '"></i>' +
        '   <i class="fa fa-stack-1x ' + getSmileyCls(score) + '"></i></span>' +
        '</span>'
}

function getSmileyCls(score) {
    return score > 0.3 ? "fa-smile-o" : score > -0.3 ? "fa-meh-o" : "fa-frown-o";
}

function roundScore(value, valDecimals) {
    return (Math.round(Number(value) * (Math.pow(10, valDecimals))) / Math.pow(10, valDecimals)).toFixed(valDecimals);
}

function getBatchInfoBadges(ee) {
    var result = "";

    if (ee.hasBatchInformation === false) {
        result = result + getStatusBadge('exclamation-triangle', 'dark-yellow', 'no batch info',
            Gemma.HelpText.WidgetDefaults.ExpressionExperimentDetails.noBatchInfo)
    }

    if (ee.batchEffect !== null && ee.batchEffect !== "") {
        if (ee.batchEffect === "Data has been batch-corrected") { // ExpressionExperimentServiceImpl::getBatchEffectDescription()
            result = result + getStatusBadge('cogs', 'green', 'batch corrected', ee.batchEffect)
        } else {
            result = result + getStatusBadge('exclamation-triangle', 'dark-yellow', 'batch effect', ee.batchEffect)
        }
    }

    if (ee.batchConfound !== null && ee.batchConfound !== "") {
        result = result + getStatusBadge('exclamation-triangle', 'dark-yellow', 'batch confound',
            ee.batchConfound)
    }

    return result;
}

/**
 *
 * Top level container for all sections of expression experiment info Sections are: 1. Details (has editing tools) 2.
 * Experimental design 3. Expression visualisation 4. Diagnostics 5. Quantitation Types ? 6. History (admin only) 7.
 * Admin (running analyses)
 *
 *
 * To open the page at a specific tab, include ?tab=[tabName] suffix in the URL. Tab names are each tab's itemId.
 *
 * @class Gemma.ExpressionExperimentPage
 * @extends Ext.TabPanel
 *
 */
Gemma.ExpressionExperimentPage = Ext.extend(Ext.TabPanel, {

    height: 600,
    defaults: {
        autoScroll: true,
        width: 850
    },
    initialTab: 'details',
    deferredRender: true,
    listeners: {
        'tabchange': function (tabPanel, newTab) {
            newTab.fireEvent('tabChanged');
        },
        'beforetabchange': function (tabPanel, newTab, currTab) {
            // if false is returned, tab isn't changed
            if (currTab) {
                return currTab.fireEvent('leavingTab');
            }
            return true;
        }
    },

    checkURLforInitialTab: function () {
        this.loadSpecificTab = (document.URL.indexOf("?") > -1 && (document.URL.indexOf("tab=") > -1));
        if (this.loadSpecificTab) {
            var param = Ext.urlDecode(document.URL.substr(document.URL.indexOf("?") + 1));
            if (param.tab) {
                if (this.getComponent(param.tab) !== undefined) {
                    this.initialTab = param.tab;
                }
            }
        }
    },

    /**
     * @memberOf Gemma.ExpressionExperimentPage
     */
    initComponent: function () {

        var eeId = this.eeId;

        var isAdmin = Ext.get("hasAdmin").getValue() == 'true';

        Gemma.ExpressionExperimentPage.superclass.initComponent.call(this);
        this.on('render', function () {
            if (!this.loadMask) {
                this.loadMask = new Ext.LoadMask(this.getEl(), {
                    msg: Gemma.StatusText.Loading.generic,
                    msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                });
            }
            this.loadMask.show();
        });

        ExpressionExperimentController.loadExpressionExperimentDetails(eeId, {
            callback: function (experimentDetails) {
                if (experimentDetails === null) throw "Experiment can not be accessed, please log in first.";
                this.initFromExperimentValueObject(experimentDetails, isAdmin);

                this.checkURLforInitialTab();
                this.setActiveTab(this.initialTab);

            }.createDelegate(this),
            errorHandler: Gemma.genericErrorHandler
        });

        Gemma.Application.currentUser.on("logIn", function (userName, isAdmin) {
            var appScope = this;
            ExpressionExperimentController.canCurrentUserEditExperiment(this.experimentDetails.id, {
                callback: function (editable) {
                    // console.log(this);
                    appScope.adjustForIsAdmin(isAdmin, editable);
                },
                scope: appScope
            });

        }, this);

        Gemma.Application.currentUser.on("logOut", function () {

            this.adjustForIsAdmin(false, false);

        }, this);
    },
    initFromExperimentValueObject: function (experimentDetails, isAdmin) {

        /**
         * The ExpressionExperimentValueObject - see the Java object for details.
         *
         * The following is here to hide JS warnings for unresolved variables.
         * @param experimentDetails.id
         * @param experimentDetails.currentUserHasWritePermission
         * @param experimentDetails.currentUserIsOwner
         * @param experimentDetails.hasBatchInformation
         * @param experimentDetails.batchConfound
         * @param experimentDetails.batchEffect
         * @param experimentDetails.troubled
         * @param experimentDetails.troubleDetails
         * @param experimentDetails.reprocessedFromRawData
         * @param experimentDetails.QChtml
         * @param experimentDetails.hasMultiplePreferredQuantitationTypes
         * @param experimentDetails.hasMultipleTechnologyTypes
         * @param experimentDetails.parentTaxonId
         * @param experimentDetails.externalDatabase
         * @param experimentDetails.coexpressionLinkCount
         * @param experimentDetails.bioAssayCount
         * @param experimentDetails.dateLinkAnalysis
         * @param experimentDetails.technologyType
         * @param experimentDetails.hasEitherIntensity
         * @param experimentDetails.dateMissingValueAnalysis
         * @param experimentDetails.dateProcessedDataVectorComputation
         * @param experimentDetails.dateDifferentialAnalysis
         * @param experimentDetails.differentialAnalysisEventType
         * @param experimentDetails.pubmedId
         * @param experimentDetails.expressionExperimentSets
         * @param experimentDetails.lastArrayDesignUpdateDate
         * @param experimentDetails.needsAttention
         * @param experimentDetails.geeq.publicQualityScore,
         * @param experimentDetails.geeq.publicSuitabilityScore
         */
        this.experimentDetails = experimentDetails;
        this.editable = experimentDetails.currentUserHasWritePermission || isAdmin;
        this.ownedByCurrentUser = experimentDetails.currentUserIsOwner;

        if (this.loadMask) {
            this.loadMask.hide();
        }

        // DETAILS TAB
        this.add(this.makeDetailsTab(experimentDetails));

        // EXPERIMENT DESIGN TAB
        this.add(this.makeDesignTab(experimentDetails));

        // VISUALISATION TAB
        this.add(this.makeVisualisationTab(experimentDetails, isAdmin));

        // DIAGNOSTICS TAB
        this.add(this.makeDiagnosticsTab(experimentDetails, isAdmin));

        // QUANTITATION TYPES TAB
        this.add(new Gemma.ExpressionExperimentQuantitationTypeGrid({
            title: 'Quantitation Types',
            itemId: 'quantitation',
            eeid: experimentDetails.id
        }));

        this.adjustForIsAdmin(isAdmin, this.editable);

    },
    makeDetailsTab: function (experimentDetails) {
        return new Gemma.ExpressionExperimentDetails({
            title: 'Overview',
            itemId: 'details',
            id: 'ee-details-panel',
            experimentDetails: experimentDetails,
            editable: this.editable,
            owned: this.ownedByCurrentUser,
            admin: this.admin,
            listeners: {
                'experimentDetailsReloadRequired': function () {
                    var myMask = new Ext.LoadMask(Ext.getBody(), {
                        msg: "Refreshing..."
                    });
                    myMask.show();
                    window.location.reload(false); // could do something fancier like reloading just
                    // the component
                },
                scope: this
            }
        });
    },
    makeDesignTab: function (experimentDetails) {
        var batchInfo = '<div class="ed-batch-info">' + getBatchInfoBadges(experimentDetails) + '</div>';

        return {
            title: 'Experimental Design',
            tbar: [{
                text: 'Show Details',
                itemId: 'design',
                tooltip: 'Go to the design details',
                icon: ctxBasePath + '/images/magnifier.png',
                handler: function () {
                    window.open(ctxBasePath + "/experimentalDesign/showExperimentalDesign.html?eeid=" + experimentDetails.id);
                }
            }],
            html: batchInfo + '<div id="eeDesignMatrix" style="height:80%">Loading...</div>',
            layout: 'absolute',
            // items -> bar chart and table?
            listeners: {
                render: function () {
                    DesignMatrix.init({
                        id: experimentDetails.id
                    });
                }
            }
        };
    },
    makeVisualisationTab: function (experimentDetails, isAdmin) {
        var eeId = this.eeId;
        var title = "Data for a 'random' sampling of probes";
        var geneList = [];
        var downloadLink = String.format(ctxBasePath + "/dedv/downloadDEDV.html?ee={0}", eeId);
        var viz = new Gemma.VisualizationWithThumbsPanel({
            thumbnails: false,
            downloadLink: downloadLink,
            params: [[eeId], geneList]
        });
        viz.on('render', function () {
            viz.loadFromParam({
                params: [[eeId], geneList]
            });
        });
        var geneTBar = new Gemma.VisualizationWidgetGeneSelectionToolbar({
            eeId: eeId,
            visPanel: viz,
            taxonId: experimentDetails.parentTaxonId
            // showRefresh : (isAdmin || this.editable)
        });
        geneTBar.on('refreshVisualisation', function () {
            viz.loadFromParam({
                params: [[eeId], geneList]
            });
        });
        return {
            items: viz,
            itemId: 'visualize',
            layout: 'fit',
            padding: 0,
            title: 'Visualize Expression',
            tbar: geneTBar
        };
    },
    makeDiagnosticsTab: function (experimentDetails, isAdmin) {

        var refreshDiagnosticsLink = '';
        if (this.editable || isAdmin) {
            refreshDiagnosticsLink = '<a href="refreshCorrMatrix.html?id=' + experimentDetails.id + '"><img '
                + 'src="' + ctxBasePath + '/images/icons/arrow_refresh_small.png" title="refresh" ' + 'alt="refresh" />Refresh</a><br>';
        }
        this.refreshDiagnosticsBtn = new Ext.Button({
            icon: ctxBasePath + '/images/icons/arrow_refresh_small.png',
            text: 'Refresh',
            handler: function () {
                window.location = "refreshCorrMatrix.html?id=" + experimentDetails.id;
            },
            hidden: (this.editable || isAdmin)
        });
        return {
            title: 'Diagnostics',
            itemId: 'diagnostics',
            items: [this.refreshDiagnosticsBtn, {
                html: experimentDetails.QChtml,
                border: false
            }]
        };
    },
    adjustForIsAdmin: function (isAdmin, isEditable) {
        // hide/show 'refresh' link to diagnostics tab
        this.refreshDiagnosticsBtn.setVisible(isAdmin || isEditable);

        /* HISTORY TAB */
        if ((isAdmin || isEditable) && !this.historyTab) {
            this.historyTab = new Gemma.AuditTrailGrid({
                title: 'History',
                itemId: 'history',
                bodyBorder: false,
                collapsible: false,
                viewConfig: {
                    forceFit: true
                },
                auditable: {
                    id: this.experimentDetails.id,
                    classDelegatingFor: "ubic.gemma.model.expression.experiment.ExpressionExperiment"
                },
                loadOnlyOnRender: true
            });
            this.add(this.historyTab);
        } else if (this.historyTab) {
            this.historyTab.setVisible((isAdmin || isEditable));
        }

        /* ADMIN TOOLS TAB */
        if ((isAdmin || isEditable) && !this.toolTab) {
            this.toolTab = new Gemma.ExpressionExperimentTools({
                experimentDetails: this.experimentDetails,
                title: 'Admin & Curation',
                itemId: 'admin',
                editable: isEditable,
                listeners: {
                    'reloadNeeded': function () {
                        var myMask = new Ext.LoadMask(Ext.getBody(), {
                            msg: "Refreshing..."
                        });
                        myMask.show();
                        var reloadToAdminTab = document.URL;
                        reloadToAdminTab = reloadToAdminTab.replace(/&*tab=\w*/, '');
                        reloadToAdminTab += '&tab=admin';
                        window.location.href = reloadToAdminTab;

                    }
                }
            });
            this.add(this.toolTab);
        } else if (this.toolTab) {
            this.toolTab.setVisible((isAdmin || isEditable));
        }
    }
});

/**
 * Used to make the correlation heatmap clickable. See ExperimentQCTag.java
 *
 * @param {Object}
 *           bigImageUrl
 */
var popupImage = function (url, width, height) {
    url = url + "&nocache=" + Math.floor(Math.random() * 1000);
    var b = new Ext.Window({
        modal: true,
        stateful: false,
        resizable: true,
        autoScroll: true,
        height: height, // or false.
        width: width || 200,
        padding: 10,
        html: '<img src=\"' + url + '"\" />'
    });
    b.show();
};
