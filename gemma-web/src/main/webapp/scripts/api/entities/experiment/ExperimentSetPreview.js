/**
 *
 * @author thea
 *
 */
Ext.namespace('Gemma');

/**
 *
 * Displays a small number of elements from the set with links to the set's page and to an editor
 *
 * @class Gemma.ExperimentSetPreview
 * @xtype Gemma.ExperimentSetPreview
 */
Gemma.ExperimentSetPreview = Ext
    .extend(
        Gemma.SetPreview,
        {

            /**
             * update the contents of the experiment preview box
             *
             * @memberOf Gemma.ExperimentSetPreview
             * @private
             * @param {Number[]}
             *           ids an array of experimentIds to use to populate preview
             */
            _loadExperimentPreviewFromIds: function (ids) {
                // this.entityIds = ids;
                // load some experiments to display
                var limit = (ids.length < this.preview_size) ? ids.length : this.preview_size;
                var previewIds = ids.slice(0, limit);

                ExpressionExperimentController.loadExpressionExperiments(previewIds, {
                    callback: function (ees) {
                        this.loadPreview(ees, ids.length);
                    }.createDelegate(this),
                    errorHandler: Gemma.Error.genericErrorHandler
                });
            },

            /**
             * @memberOf Gemma.ExperimentSetPreview
             */
            reset: function () {
                this.resetPreview();
                // this.entityIds = null;
                this.previewContent.setTitle(null);
                this.selectedSetValueObject = null;
            },

            /**
             * @public update the contents of the experiment preview box
             *
             * @param {ExperimentValueSetObject[]}
             *           experimentSet populate preview with members
             * @memberOf Gemma.ExperimentSetPreview
             */
            loadExperimentPreviewFromExperimentSet: function (eeSet) {

                this.setSelectedSetValueObject(eeSet);
                var ids = eeSet.expressionExperimentIds;

                if (ids.length > 0) {
                    this._loadExperimentPreviewFromIds(ids);
                } else if (eeSet.id > 0) {
                    // fetch from server.
                    ExpressionExperimentSetController.getExperimentsInSet.apply(this, [eeSet.id, this.preview_size, {
                        callback: function (experiments) {
                            this.loadPreview(experiments, this.selectedSetValueObject.size);
                            this.fireEvent('previewLoaded', experiments);
                        }.createDelegate(this),
                        errorHandler: Gemma.Error.genericErrorHandler
                    }]);
                } else {
                    alert("Could not load");
                }

            },

            /**
             * @memberOf Gemma.ExperimentSetPreview
             * @param mode
             */
            setMode: function (mode) {
                this.mode = mode;
                this.updateTitle();
            },

            hideUnanalyzedDatasets: false,

            /**
             * Whether we are in coexpression or differential expression mode
             */
            mode: 'coex',

            /**
             * public don't use params if you want to update name based on this.selectedEntityOrGroup
             *
             * @memberOf Gemma.ExperimentSetPreview
             * @param {Object}
             *           selectedSet
             *
             */
            updateTitle: function () {
                // debugger;
                var selectedSet = this.selectedSetValueObject;

                if (typeof selectedSet == undefined || selectedSet == null) {
                    return;
                }

                // if an experiment group page exists for this set, make title a link

                // note that normally the ids are not filled in, so we just use the size.
                var size = selectedSet.size > 0 ? selectedSet.size : selectedSet.expressionExperimentIds.length;
                var numWithCoex = selectedSet.numWithCoexpressionAnalysis;
                var numWithDiffex = selectedSet.numWithDifferentialExpressionAnalysis;

                if (!(selectedSet instanceof SessionBoundExpressionExperimentSetValueObject)) {
                    name = '<a target="_blank" href="' + Gemma.CONTEXT_PATH + '/expressionExperimentSet/showExpressionExperimentSet.html?id='
                        + selectedSet.id + '">' + selectedSet.name + '</a>';
                } else {
                    name = selectedSet.name;
                }

                var usableSize = 0;
                if (this.mode == 'coex') {
                    usableSize = numWithCoex > 0 ? numWithCoex : 0;
                } else {
                    usableSize = numWithDiffex > 0 ? numWithDiffex : 0;
                }

                // Whether the data sets have coexpression and/or differential expression available
                this.previewContent.setTitle('<span style="font-size:1.2em">' + name
                    + '</span><br /><span style="font-weight:normal">' + usableSize
                    + (size > 1 ? " experiments" : " experiment ") + ' of ' + size + ' have '
                    + (this.mode == 'coex' ? 'co' : 'diff. ') + 'expression available');
                // + (this.hideUnanalyzedDatasets ? '<br><span style="font-size:smaller">Unanalyzed experiments
                // hidden</span>'
                // : '') ); // NOTE this refers to those lacking both Coex and Diff analyses. The notice might be confusing,
                // not
                // necessary?

            },

            /**
             * Given the current selection, when the user selects another result from the combo: we merge it in
             *
             * @private
             * @param combo
             * @param record
             * @param index
             * @returns
             */
            _addToPreviewedSet: function (combo, record, index) {
                // FIXME see GeneSetPreview._addToPreviewedSet and harmonize.
                var rvo = record.data.get('resultValueObject');

                if (rvo instanceof SessionBoundExpressionExperimentSetValueObject) {
                    this._appendAndUpdate(combo, rvo);
                } else if (rvo instanceof ExpressionExperimentValueObject) {
                    this._appendAndUpdate(combo, rvo);
                } else {
                    // this shouldn't usually be happening? I mean, we should already have the ids.
                    ExpressionExperimentSetController.getExperimentIdsInSet(rvo.id, {
                        callback: function (expIds) {
                            rvo.expressionExperimentIds = expIds;
                            this._appendAndUpdate(combo, rvo);
                        }.createDelegate(this),
                        errorHandler: Gemma.Error.genericErrorHandler
                    });
                }

            },

            /**
             * @memberOf Gemma.ExperimentSetPreview
             * @param combo
             * @param valueObject
             * @private
             */
            _appendAndUpdate: function (combo, valueObject) {
                var allIds = this.selectedSetValueObject.expressionExperimentIds;

                var newIds = [];
                if (valueObject instanceof ExpressionExperimentValueObject) {
                    newIds.push(valueObject.id);
                } else {
                    newIds = valueObject.expressionExperimentIds;
                }

                // don't add duplicates
                var added = false;
                for (var i = 0; i < newIds.length; i++) {
                    if (allIds.indexOf(newIds[i]) < 0) {
                        allIds.push(newIds[i]);
                        added = true;
                    }
                }

                if (!added) {
                    this.fireEvent('doneModification');
                    return;
                }

                var editedGroup;

                if (this.selectedSetValueObject instanceof SessionBoundExpressionExperimentSetValueObject) {
                    /*
                     * Don't wipe it, just add on.
                     */
                    editedGroup = this.selectedSetValueObject;
                    editedGroup.modified = true;
                    editedGroup.expressionExperimentIds = allIds;
                    editedGroup.size = editedGroup.expressionExperimentIds.length;

                    ExpressionExperimentSetController.updateSessionGroups([editedGroup], // returns
                        // SessionBoundExpressionExperimentSetValueObject collection
                        // added
                        {
                            callback: function (eeSets) {

                                this._loadExperimentPreviewFromIds(editedGroup.expressionExperimentIds); // async
                                this.setSelectedSetValueObject(editedGroup);
                                this.updateTitle();
                                this.withinSetExperimentCombo.reset();
                                this.withinSetExperimentCombo.blur();

                                this.fireEvent('experimentListModified', editedGroup);
                                this.fireEvent('doneModification');

                            }.createDelegate(this),
                            errorHandler: Gemma.Error.genericErrorHandler
                        });

                } else {
                    // A group based on a database-bound group.
                    editedGroup = new SessionBoundExpressionExperimentSetValueObject();
                    editedGroup.id = null;

                    var currentTime = new Date();
                    var hours = currentTime.getHours();
                    var minutes = currentTime.getMinutes();
                    if (minutes < 10) {
                        minutes = "0" + minutes;
                    }
                    var time = '(' + hours + ':' + minutes + ') ';

                    editedGroup.name = time + " Custom Experiment Group";
                    editedGroup.description = "Temporary experiment group created " + currentTime.toString();

                    editedGroup.expressionExperimentIds = allIds;
                    editedGroup.taxonId = record.get('taxonId');
                    editedGroup.taxonName = record.get('taxonName');
                    editedGroup.size = editedGroup.expressionExperimentIds.length;
                    editedGroup.modified = true;
                    editedGroup.isPublic = false;

                    editedGroup.numWithCoexpressionAnalysis = -1;
                    editedGroup.numWithDifferentialExpressionAnalysis = -1;

                    ExpressionExperimentSetController.addSessionGroup(editedGroup, true, {
                        callback: function (newValueObject) {

                            this.setSelectedSetValueObject(newValueObject);
                            this.loadExperimentPreviewFromExperimentSet(newValueObject);

                            this.updateTitle();
                            this.withinSetExperimentCombo.reset();
                            this.withinSetExperimentCombo.blur();

                            this.fireEvent('experimentListModified', newValueObject);
                            this.fireEvent('doneModification');

                        }.createDelegate(this),
                        errorHandler: Gemma.Error.genericErrorHandler
                    });
                }
            },

            /**
             * @Override
             * @memberOf Gemma.ExperimentSetPreview
             */
            initComponent: function () {

                this.withinSetExperimentCombo = new Gemma.ExperimentAndExperimentGroupCombo({
                    width: 300,
                    style: 'margin:10px',
                    hideTrigger: true,
                    emptyText: 'Add experiments to your group'
                });

                this.withinSetExperimentCombo.setTaxonId(this.taxonId);

                this.withinSetExperimentCombo.on('selected', this._addToPreviewedSet, this);

                Ext
                    .apply(
                        this,
                        {
                            selectionEditor: new Gemma.ExpressionExperimentMembersGrid({
                                name: 'selectionEditor',
                                hideHeaders: true,
                                frame: false,
                                width: 500,
                                height: 500
                            }),
                            /*
                             * I'm hiding experiments that have neither coexpression nor differential expression analysis.
                             */
                            defaultTpl: new Ext.XTemplate(
                                '<tpl for=".">'
                                + '<tpl if="!this.hideUnanalyzedDatasets || hasCoexpressionAnalysis || hasDifferentialExpressionAnalysis">'
                                + ' <div style="padding-bottom:7px;">'
                                + '<a target="_blank" href="' + Gemma.CONTEXT_PATH + '/expressionExperiment/showExpressionExperiment.html?id=',
                                '{id}"',
                                ' ext:qtip="'
                                + '<tpl if="hasCoexpressionAnalysis">Has coexpression analysis&nbsp</tpl>&nbsp;'
                                + '<tpl if="hasDifferentialExpressionAnalysis">Has differential expression analysis</tpl>'
                                + '">{shortName}</a>&nbsp; {name} <span style="color:grey">({taxon})&nbsp;'
                                + '<tpl if="hasCoexpressionAnalysis">C</tpl>&nbsp;<tpl if="hasDifferentialExpressionAnalysis">D</tpl></span></div></tpl></tpl>',
                                {
                                    hideUnanalyzedDatasets: this.hideUnanalyzedDatasets
                                }),

                            defaultPreviewTitle: "Experiment Selection Preview",

                            addingCombo: this.withinSetExperimentCombo

                        });

                Gemma.ExperimentSetPreview.superclass.initComponent.call(this);

                this.selectionEditor.on('experimentListModified', function (newSet) {
                    if (typeof newSet.expressionExperimentIds !== 'undefined' && typeof newSet.name !== 'undefined') {
                        this._loadExperimentPreviewFromIds(newSet.expressionExperimentIds);
                        this.setSelectedSetValueObject(newSet);
                        this.updateTitle();
                    }
                    this.fireEvent('experimentListModified', newSet);
                }, this);

            }

        });

Ext.reg('Gemma.ExperimentSetPreview', Gemma.ExperimentSetPreview);