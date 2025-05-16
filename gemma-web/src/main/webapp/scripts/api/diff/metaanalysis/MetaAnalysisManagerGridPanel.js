/**
 * Meta-analysis manager displays all the available meta-analyses for the current user.
 *
 * @author frances
 *
 */
Ext.namespace('Gemma');

Gemma.MetaAnalysisManagerGridPanel = Ext
    .extend(
        Ext.grid.GridPanel,
        {
            title: "Meta-analysis Manager",
            autoScroll: true,
            stripeRows: true,
            loadMask: true,
            viewConfig: {
                forceFit: true,
                deferEmptyText: false,
                emptyText: 'No meta-analysis to display'
            },
            initComponent: function () {
                var DEFAULT_THRESHOLD = new Gemma.MetaAnalysisUtilities().getDefaultThreshold();

                var metaAnalysisCache = [];

                var metaAnalysisWindow;

                Gemma.Application.currentUser.on("logIn", function () {
                    this.getStore().reload();
                }, this);
                Gemma.Application.currentUser.on("logOut", function () {
                    this.getStore().reload();
                }, this);

                var generateLink = function (methodWithArguments, imageSrc, description, width, height) {
                    return '<span class="link" onClick="return Ext.getCmp(\'' + this.getId() + '\').' + methodWithArguments
                        + '"><img src="' + imageSrc + '" alt="' + description + '" ext:qtip="' + description + '" '
                        + ((width && height) ? 'width="' + width + '" height="' + height + '" ' : '') + '/></span>';

                }.createDelegate(this);

                var generateLinkPlaceholder = function () {
                    return '<img src="' + Gemma.CONTEXT_PATH + '/images/s.gif" height="16" width="16">';
                };

                var showLoadMask = function (msg) {
                    if (!this.loadMask) {
                        this.loadMask = new Ext.LoadMask(this.getEl());
                    }
                    this.loadMask.msg = msg ? msg : "Loading ...";

                    this.loadMask.show();
                }.createDelegate(this);

                var hideLoadMask = function () {
                    this.loadMask.hide();
                }.createDelegate(this);

                var numberColumnRenderer = function (value, metaData, record, rowIndex, colIndex, store) {
                    metaData.attr = 'style="padding-right: 15px;"';
                    return value;
                };

                var showLogInWindow = function (callback, args) {
                    Gemma.AjaxLogin.showLoginWindowFn();

                    Gemma.Application.currentUser.on("logIn", function (userName, isAdmin) {
                        callback.apply(this, args);
                    }, this, {
                        single: true
                    });
                };

                /**
                 * Pop up the details of a meta-analysis.
                 */
                var showMetaAnalysisWindow = function (metaAnalysis, analysisName, numGenesAnalyzed) {
                    metaAnalysis.name = analysisName;
                    metaAnalysis.numGenesAnalyzed = numGenesAnalyzed;

                    var viewMetaAnalysisWindow = new Gemma.MetaAnalysisWindow({
                        title: 'Details of: ' + unescape(analysisName),
                        metaAnalysis: metaAnalysis,
                        defaultQvalueThreshold: DEFAULT_THRESHOLD
                    });
                    viewMetaAnalysisWindow.show();
                };

                var showViewEvidenceWindow = function (metaAnalysis, id) {
                    var record = this.getStore().getById(id);
                    if (record != null) {
                        metaAnalysis.name = record.data.name;
                        metaAnalysis.numGenesAnalyzed = record.data.numGenesAnalyzed;

                        var viewEvidenceWindow = new Gemma.MetaAnalysisEvidenceWindow({
                            metaAnalysisId: id,
                            metaAnalysis: metaAnalysis,
                            showActionButton: record.data.editable,
                            title: 'View Phenocarta evidence for ' + record.data.name,
                            diffExpressionEvidence: record.data.diffExpressionEvidence,
                            modal: false,
                            listeners: {
                                evidenceRemoved: function () {
                                    this.store.reload();
                                },
                                scope: this
                            }
                        });
                        viewEvidenceWindow.show();
                    }
                };

                var processMetaAnalysis = function (id, errorDialogTitle, callback, args) {
                    var metaAnalysisFound = metaAnalysisCache[id];
                    if (metaAnalysisFound) {
                        // Put metaAnalysisFound at the beginning of args.
                        args.splice(0, 0, metaAnalysisFound);

                        callback.apply(this, args);
                    } else {
                        showLoadMask();
                        DiffExMetaAnalyzerController.findDetailMetaAnalysisById(id, function (baseValueObject) {
                            hideLoadMask();

                            if (baseValueObject.errorFound) {
                                Gemma.Error.alertUserToError(baseValueObject, errorDialogTitle);
                            } else {
                                metaAnalysisCache[id] = baseValueObject.valueObject;

                                // Put metaAnalysisFound at the beginning of args.
                                args.splice(0, 0, metaAnalysisCache[id]);

                                callback.apply(this, args);
                            }
                        }.createDelegate(this));
                    }
                }.createDelegate(this);

                Ext
                    .apply(
                        this,
                        {
                            store: new Ext.data.JsonStore({
                                autoLoad: true,
                                proxy: new Ext.data.DWRProxy(DiffExMetaAnalyzerController.loadAllMetaAnalyses),
                                fields: ['id', {
                                    name: 'name',
                                    sortType: Ext.data.SortTypes.asUCString
                                }, // case-insensitively
                                    'description', 'numGenesAnalyzed', 'numResults', 'numResultSetsIncluded', 'editable',
                                    'ownedByCurrentUser', 'public', 'shared', 'diffExpressionEvidence'],
                                idProperty: 'id',
                                sortInfo: {
                                    field: 'name',
                                    direction: 'ASC'
                                }
                            }),
                            eval: function (request) {
                                eval(request);
                            },
                            showSaveAsEvidenceWindow: function (id) {
                                var doShowForLoggedInUser = function (id) {
                                    var showSaveAsEvidenceWindowHelper = function (metaAnalysis) {
                                        var record = this.getStore().getById(id);
                                        if (record != null) {
                                            metaAnalysis.name = record.data.name;
                                            metaAnalysis.numGenesAnalyzed = record.data.numGenesAnalyzed;

                                            var saveAsEvidenceWindow = new Gemma.MetaAnalysisEvidenceWindow({
                                                metaAnalysisId: id,
                                                metaAnalysis: metaAnalysis,
                                                showActionButton: record.data.ownedByCurrentUser,
                                                defaultQvalueThreshold: DEFAULT_THRESHOLD,
                                                title: 'Save ' + record.data.name + ' as Phenocarta evidence',
                                                listeners: {
                                                    evidenceSaved: function () {
                                                        this.store.reload();
                                                    },
                                                    scope: this
                                                }
                                            });
                                            saveAsEvidenceWindow.show();
                                        }
                                    }.createDelegate(this);

                                    var metaAnalysisFound = metaAnalysisCache[id];
                                    if (metaAnalysisFound) {
                                        showSaveAsEvidenceWindowHelper(metaAnalysisFound);
                                    } else {
                                        showLoadMask();
                                        DiffExMetaAnalyzerController.findDetailMetaAnalysisById(id, function (baseValueObject) {
                                            hideLoadMask();

                                            if (baseValueObject.errorFound) {
                                                Gemma.Error.alertUserToError(baseValueObject,
                                                    'Cannot save meta-analysis as Phenocarta evidence');
                                            } else {
                                                metaAnalysisCache[id] = baseValueObject.valueObject;
                                                showSaveAsEvidenceWindowHelper(metaAnalysisCache[id]);
                                            }
                                        }.createDelegate(this));
                                    }
                                }.createDelegate(this);

                                SignupController.loginCheck({
                                    callback: function (result) {
                                        if (result.loggedIn) {
                                            doShowForLoggedInUser(id);
                                        } else {
                                            showLogInWindow.call(this, doShowForLoggedInUser, [id]);
                                        }
                                    }.createDelegate(this)
                                });
                            },
                            removeMetaAnalysis: function (id) {
                                var doRemoveForLoggedInUser = function (id) {
                                    var record = this.getStore().getById(id);
                                    if (record != null) {
                                        if (record.data.diffExpressionEvidence) {
                                            Ext.MessageBox
                                                .alert(
                                                    Gemma.HelpText.WidgetDefaults.MetaAnalysisManagerGridPanel.ErrorTitle.removeMetaAnalysis,
                                                    Gemma.HelpText.WidgetDefaults.MetaAnalysisManagerGridPanel.ErrorMessage.evidenceExist);
                                        } else {
                                            Ext.MessageBox
                                                .confirm(
                                                    'Confirm',
                                                    'Are you sure you want to remove meta-analysis "' + record.data.name + '"?',
                                                    function (button) {
                                                        if (button === 'yes') {
                                                            showLoadMask("Removing analysis ...");

                                                            DiffExMetaAnalyzerController
                                                                .removeMetaAnalysis(
                                                                    id,
                                                                    function (baseValueObject) {
                                                                        hideLoadMask();

                                                                        if (baseValueObject.errorFound) {
                                                                            Gemma.Error
                                                                                .alertUserToError(
                                                                                    baseValueObject,
                                                                                    Gemma.HelpText.WidgetDefaults.MetaAnalysisManagerGridPanel.ErrorTitle.removeMetaAnalysis);
                                                                        } else {
                                                                            this.store.reload();
                                                                        }
                                                                    }.createDelegate(this));
                                                        }
                                                    }, this);
                                        }
                                    }
                                }.createDelegate(this);

                                SignupController.loginCheck({
                                    callback: function (result) {
                                        if (result.loggedIn) {
                                            doRemoveForLoggedInUser(id);
                                        } else {
                                            showLogInWindow.call(this, doRemoveForLoggedInUser, [id]);
                                        }
                                    }.createDelegate(this)
                                });

                            },
                            columns: [
                                {
                                    header: 'Name',
                                    dataIndex: 'name',
                                    width: 0.3,
                                    renderer: function (value, metadata, record, rowIndex, colIndex, store) {
                                        return value
                                            + ' '
                                            + generateLink('eval(\'processMetaAnalysis(' + record.data.id + ', '
                                                + '\\\'Cannot view meta-analysis\\\', ' + 'showMetaAnalysisWindow, '
                                                + '[ \\\'' + escape(record.data.name) + '\\\', '
                                                + record.data.numGenesAnalyzed + ' ])\');',
                                                Gemma.CONTEXT_PATH + '/images/icons/magnifier.png', 'Show details', 10, 10);
                                    }
                                },
                                {
                                    header: 'Description',
                                    dataIndex: 'description',
                                    width: 0.4
                                },
                                {
                                    header: 'Genes analyzed',
                                    align: "right",
                                    dataIndex: 'numGenesAnalyzed',
                                    width: 0.2,
                                    renderer: numberColumnRenderer
                                },
                                {
                                    header: 'Genes with q-value < ' + DEFAULT_THRESHOLD,
                                    align: "right",
                                    dataIndex: 'numResults',
                                    width: 0.25,
                                    renderer: numberColumnRenderer
                                },
                                {
                                    header: 'Result sets included',
                                    align: "right",
                                    dataIndex: 'numResultSetsIncluded',
                                    width: 0.2,
                                    renderer: numberColumnRenderer
                                },
                                {
                                    header: 'Admin',
                                    id: 'id',
                                    width: 0.15,
                                    renderer: function (value, metadata, record, rowIndex, colIndex, store) {
                                        var adminLinks = '';

                                        if (record.data.diffExpressionEvidence == null) {
                                            if (record.data.ownedByCurrentUser) {
                                                adminLinks += generateLink('showSaveAsEvidenceWindow(' + record.data.id
                                                    + ');', Gemma.CONTEXT_PATH + '/images/icons/neurocarta-add.png',
                                                    'Save as Phenocarta evidence');
                                            } else {
                                                adminLinks += generateLinkPlaceholder();
                                            }
                                        } else {
                                            adminLinks += generateLink('eval(\'processMetaAnalysis(' + record.data.id
                                                + ', ' + '\\\'Cannot view Phenocarta evidence\\\', '
                                                + 'showViewEvidenceWindow, ' + '[ ' + record.data.id + ' ])\');',
                                                Gemma.CONTEXT_PATH + '/images/icons/neurocarta-check.png', 'View Phenocarta evidence');
                                        }
                                        adminLinks += ' ';

                                        if (record.data.editable) {
                                            adminLinks += generateLink('removeMetaAnalysis(' + record.data.id + ');',
                                                Gemma.CONTEXT_PATH + '/images/icons/cross.png', 'Remove meta-analysis');
                                        } else {
                                            adminLinks += generateLinkPlaceholder();
                                        }
                                        adminLinks += ' ';

                                        if (Ext.get("hasUser") != null && Ext.get("hasUser").getValue()) {
                                            adminLinks += Gemma.SecurityManager
                                                .getSecurityLink(
                                                    'ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis',
                                                    record.data.id, record.data.public, record.data.shared,
                                                    record.data.editable, // Can current user edit?
                                                    null, null, null, // It is title in Security dialog. Specify null to use
                                                    // the object name.
                                                    record.data.ownedByCurrentUser); // Is current user owner?
                                        }
                                        return adminLinks;
                                    },
                                    sortable: false
                                }],
                            tbar: [{
                                handler: function () {
                                    var showAddMetaAnalysisWindowForLoggedInUser = function () {
                                        if (!metaAnalysisWindow || metaAnalysisWindow.hidden) {
                                            metaAnalysisWindow = new Gemma.MetaAnalysisWindow({
                                                title: 'Add New Meta-analysis',
                                                defaultQvalueThreshold: DEFAULT_THRESHOLD,
                                                listeners: {
                                                    resultSaved: function () {
                                                        metaAnalysisWindow.close();
                                                        this.store.reload();
                                                    },
                                                    scope: this
                                                }
                                            });
                                        }

                                        metaAnalysisWindow.show();
                                    }.createDelegate(this);

                                    SignupController.loginCheck({
                                        callback: function (result) {
                                            if (result.loggedIn) {
                                                showAddMetaAnalysisWindowForLoggedInUser();
                                            } else {
                                                showLogInWindow.call(this, showAddMetaAnalysisWindowForLoggedInUser, []);
                                            }
                                        }.createDelegate(this)
                                    });
                                },
                                scope: this,
                                icon: Gemma.CONTEXT_PATH + "/images/icons/add.png",
                                tooltip: "Add new meta-analysis"
                            }]
                        });

                Gemma.MetaAnalysisManagerGridPanel.superclass.initComponent.call(this);
            }
        });
