Ext.namespace('Gemma');

/**
 *
 * Panel with curation tools for curatable objects.
 * Should be displayed in the curation tabs for ExpressionExperiment and ArrayDesign (aka. platform).
 *
 * @class Gemma.CurationTools
 * @extends Ext.Panel
 */
Gemma.CurationTools = Ext.extend(Ext.Panel, {
    curatable: null, // for curation details info retrieval
    auditable: null, // to be passed to auditController when creating new events
    entityType: "",

    border: false,
    defaultType: 'box',
    defaults: {
        border: false
    },
    padding: 15,
    cls: 'curation-tool',

    emptyEvent: {
        performer: "--",
        date: "--",
        detail: "--"
    },

    _DEFAULT_EVENT_DESCRIPTION: "Untouched, default since creation.",

    listeners: {
        afterRender: function () {
            var chbox = document.getElementById('needsAttention');
            chbox.checked = (this.curatable.needsAttention === true);

            var tarea = document.getElementById('curationNote');
            tarea.value = this.curatable.curationNote;
        }
    },

    initComponent: function () {
        Gemma.CurationTools.superclass.initComponent.call(this);

        this.entityType = this.isExperiment() ? "Experiment" : "Platform";

        this.add(this.createHeaderInfoPanel());
        this.add(this.createCurationPanel());
        this.add(this.createTroublePanel());
    },

    /*
     PANEL COMPOSITION METHODS
     */

    /**
     * Creates an Ext.Panel with trouble details and editing tools.
     * @returns {Ext.Panel} panel that can be added to the page
     */
    createTroublePanel: function () {
        var panelTrouble = new Ext.Panel({
            cls: 'ee-tool-left',
            buttonAlign: 'left',
            defaults: {
                border: false,
                padding: 0
            }
        });
        // Status and input elements
        panelTrouble.add({
            html: '<hr class="normal"/>' +
            '<div class="v-padded"><span class="bold width130">Usability: </span>' + this.getTroubleStatusHtml() +
            '</div>'
        });

        var changeTroubleButton = new Ext.Button({
            text: 'Change usability status',
            tooltip: 'Create new event to change the usability status. ',
            handler: this.showAddTroubleEventDialog,
            scope: this,
            cls: 'default-button'
        });

        panelTrouble.addButton(changeTroubleButton);

        return panelTrouble;
    },

    /**
     * Creates an Ext.Panel with general curation info.
     * @returns {Ext.Panel} panel that can be added to the page
     */
    createHeaderInfoPanel: function () {
        var panelCuration = new Ext.Panel({
            layout: 'hbox',
            buttonAlign: 'left',
            defaults: {
                width: '100%',
                border: false,
                padding: 0
            }
        });

        // Status and input elements
        panelCuration.add({
            html: '<div class="v-padded">' +
            '<span class="bold width170">' + this.entityType + ' last updated: </span>' + Gemma.Renderers.dateTimeRenderer(this.curatable.lastUpdated) +
            '</div>'
        });

        return panelCuration;
    },

    /**
     * Creates an Ext.Panel with curation details and editing tools.
     * @returns {Ext.Panel} panel that can be added to the page
     */
    createCurationPanel: function () {
        var panelCuration = new Ext.Panel({
            cls: 'ee-tool-left',
            buttonAlign: 'left',
            defaults: {
                border: false,
                padding: 0
            }
        });

        // Status and input elements
        panelCuration.add({
            html: '<hr class="normal"/>' +
            '<div class="v-padded">' +
            '<span class="bold width130">Curation status: </span>' + this.getNeedsAttentionStatusHtml() +
            '<span class="v-padded"><span class="chb-correction"><input type="checkbox" id="needsAttention"></span> Needs Attention</span>' +
            '</div>' +
            '<div class="v-padded">' +
            '<label for="curationNote" class="curationNote-label">Curation notes:</label>' +
            '<textarea id="curationNote" rows="6" cols="60"></textarea>'
            + this.getCurationNoteDetails() +
            '</div>'
        });

        panelCuration.add({
            html: ""
        });

        var saveButton = new Ext.Button({
            text: 'Save Curation info',
            tooltip: 'Update curation status and note. Only works when changes to the attention status or note have been done. ' +
            'If you are un-setting the "Needs Attention" property, GEEQ scores will be run automatically. ',
            handler: function (b, e) {
                b.setText('Save Curation info <i class="fa fa-refresh fa-fw fa-spin"/>');
                b.setDisabled(true);
                this.saveCurationStatusAndNote();
            },
            scope: this
        });

        panelCuration.addButton(saveButton);

        return panelCuration;
    },

    /**
     *
     * @returns {string} Information about last edit of the curation note of this.curatable object.
     */
    getCurationNoteDetails: function () {
        if (this.curatable.lastNoteUpdateEvent) {
            return '<div class="dark-gray v-padded">Last edit: ' +
                ' <i class="fa fa-calendar"></i> ' +
                'As of ' + Gemma.Renderers.dateRenderer(this.curatable.lastNoteUpdateEvent.date) +
                ' <i class="fa fa-user"></i> ' +
                ' by: ' + this.curatable.lastNoteUpdateEvent.performer +
                '</div>'
        } else {
            return '<div class="dark-gray v-padded">' + this._DEFAULT_EVENT_DESCRIPTION + '</div>';
        }
    },

    /**
     * Composes the string for curation attention description
     * @returns {string} string containing the status with appropriate icon, color, date and performer.
     */
    getNeedsAttentionStatusHtml: function () {
        //Base status
        var str = this.curatable.needsAttention
            ? '<span class="gold width130"><i class="fa fa-exclamation-circle fa-lg fa-fw"></i>Needs attention</span>'
            : '<span class="green width130"><i class="fa fa-check-circle-o fa-lg fa-fw"></i>OK</span>';

        //Status description
        str += this.getEventDescriptionLine(this.curatable.lastNeedsAttentionEvent);
        return str;
    },

    /**
     * Composes the string for trouble status description
     * @returns {string} string containing the status with appropriate icon, color, date and performer.
     */
    getTroubleStatusHtml: function () {
        // Base status
        // This is messy because we have to detect whether we are dealing with ArrayDesign or ExpressionExperiment
        // Changing th model is undesirable in this case, since this is the only place in the UI we actually care
        // whether the trouble comes from the EE itself, or its platform.
        var str = (this.isExperiment() && this.curatable.actuallyTroubled === true)
        || (!this.isExperiment() && this.curatable.troubled)
            ? '<span class="red width190"><i class="fa fa-exclamation-triangle fa-lg fa-fw"></i>' + this.entityType + ' Unusable</span>'
            : '<span class="green width190"><i class="fa fa-check-circle fa-lg fa-fw"></i>' + this.entityType + ' Usable</span>';

        if (this.isExperiment()
            && this.curatable.platformTroubled) {
            str += '<span class="gray-red">' +
                ' <i class="fa fa-exclamation-triangle"></i> Platform unusable ' +
                '</span>';
        }

        // include the DifferentialExpressionSuitabilityEvents
        if (this.isExperiment() && !this.curatable.suitableForDEA) {
            str += ' <span class="red width190"><i class="fa fa-exclamation-triangle fa-lg fa-fw"></i>Unsuitable for DEA</span>';
        }

        // Status details
        str += this.getEventDescriptionLine(this.curatable.lastTroubledEvent);
        return str;
    },

    /**
     * Creates a gray line of text with icons, describing the given audit event.
     * @param AuditEvent the event to describe
     * @returns {string} html div element containing description about the given event.
     */
    getEventDescriptionLine: function (AuditEvent) {
        if (AuditEvent) {
            return '<div class="dark-gray v-padded">' +
                ' <i class="fa fa-calendar"></i> ' +
                'As of ' + Gemma.Renderers.dateRenderer(AuditEvent.date) +
                ' <i class="fa fa-user"></i> ' +
                ' set by: ' + AuditEvent.performer +
                ' <i class="fa fa-pencil"></i>' +
                ' Details: ' + this.getAuditEventNonNullDescription(AuditEvent) +
                '</div>'
        } else {
            return '<div class="dark-gray v-padded">' + this._DEFAULT_EVENT_DESCRIPTION + '</div>';
        }
    },

    /**
     * Iterates through the events detail and note, and creates an adequate description from the found strings.
     * @param AuditEvent the event to be described.
     * @returns {string} description of the given event, or "Unspecified" if no suitable string is found.
     */
    getAuditEventNonNullDescription: function (AuditEvent) {
        var str = "";
        var defaultStr = "Unspecified";


        if (AuditEvent.detail && AuditEvent.note) str += " - ";
        if (AuditEvent.note) str += AuditEvent.note;

        if (str.length > 100) {
            str = str.substring(0, 99) + "...";
        }

        if (str.length > 0) return str;
        return defaultStr;
    },

    /*
     EVENT HANDLING METHODS
     */

    /**
     * Saves the curation status and note, if they were changed, by creating new audit events.
     */
    saveCurationStatusAndNote: function () {
        var note = document.getElementById('curationNote').value;
        var needsAttention = document.getElementById('needsAttention').checked;

        var refreshCb = function () {
            this.fireEvent('reloadNeeded');
        }.createDelegate(this);

        var parent = this;
        var updateNeedsAttentionFunction = function () {
            if (needsAttention && !parent.curatable.needsAttention) {
                AuditController.addAuditEvent(parent.auditable, "NeedsAttentionEvent", "Needs attention.", null, {
                    callback: refreshCb
                });
            } else if (!needsAttention && parent.curatable.needsAttention) {
                var auditCallback = {callback: refreshCb};
                if (parent.isExperiment()) {
                    auditCallback = {
                        callback: function () {
                            ExpressionExperimentController.runGeeq(parent.auditable.id, "all", {callback: refreshCb})
                        }
                    }
                }
                AuditController.addAuditEvent(parent.auditable, "DoesNotNeedAttentionEvent", "Does not need attention.", null, auditCallback);
            }
        };

        // Check whether we need to wait for the note update before we update curation status. We have to wait to prevent
        // race conditions, if we are updating both attributes through different audit events.
        var noteCB = refreshCb;
        if (needsAttention !== this.curatable.needsAttention) {
            noteCB = updateNeedsAttentionFunction;
        }

        // Check for information change
        // Only update note if different from stored value, and it is non-empty, unless it has been non-empty before are we are clearing it
        if ((note && note !== this.curatable.curationNote) || (this.curatable.curationNote && note !== this.curatable.curationNote)) {
            AuditController.addAuditEvent(this.auditable, "CurationNoteUpdateEvent", note, null, {
                callback: noteCB
            });
        } else {
            updateNeedsAttentionFunction();
        }

    },

    /**
     * Shows the 'add new event' dialog.
     */
    showAddTroubleEventDialog: function () {
        if (!this.addEventDialog) {
            this.addEventDialog = new Gemma.AddAuditEventDialog({
                comment: false
            });
            this.addEventDialog.on("commit", function (resultObj) {
                this.createCurationEvent(resultObj);
            }.createDelegate(this));
        }
        this.addEventDialog.show();
    },

    /**
     * Calls audit controller to create a new audit event.
     * @param obj object containing necessary event information.
     */
    createCurationEvent: function (obj) {
        var cb = function () {
            this.fireEvent('reloadNeeded');
        }.createDelegate(this);
        AuditController.addAuditEvent(this.auditable, obj.type, obj.comment, obj.details, {
            callback: cb
        });
    },

    /**
     * @returns {boolean} true if the managed curatable object is an experiment, false otherwise (meaning it is likely
     * an array design).
     */
    isExperiment: function () {
        return this.curatable.actuallyTroubled !== undefined;
    }

});