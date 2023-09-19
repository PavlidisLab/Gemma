Ext.namespace('Gemma');

/**
 * A panel for displaying by-taxon counts of all existing, new and updated experiments in Gemma, with a column for each
 * count
 *
 * a similar table appeared on the classic Gemma's front page
 */
Gemma.ExpressionExperimentsSummaryPanel = Ext.extend(Ext.Panel,
    {
        title: "Summary and updates this week",
        collapsible: false,
        titleCollapse: false,
        animCollapse: false,

        listeners: {
            render: function () {

                    this.loadCounts();

            }
        },

        stateful: true,
        stateId: 'showAllExpressionExperimentsSummaryGridState',

        // what describes the state of this panel - in this case it is the "collapsed" field
        getState: function () {
            return {
                collapsed: this.collapsed
            };
        },

        // specify when the state should be saved - in this case after panel was collapsed or expanded
        stateEvents: ['collapse', 'expand'],

        constructor: function (config) {
            Gemma.ExpressionExperimentsSummaryPanel.superclass.constructor.call(this, config);
        },

        initComponent: function () {
            Gemma.ExpressionExperimentsSummaryPanel.superclass.initComponent.call(this);
        }, // end of initComponent

        /**
         * @memberOf Gemma.ExpressionExperimentsSummaryPanel
         */
        loadCounts: function () {
            if (this.getEl() && !this.loadMask) {
                this.loadMask = new Ext.LoadMask(this.getEl(), {
                    msg: "Loading summary ...",
                    msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                });
            }
            if (this.loadMask) {
                this.loadMask.show();
            }

            var parent = this;
            ExpressionExperimentController.loadCountsForDataSummaryTable(function (json) {
                // update the panel with counts
                json.cmpId = Ext.id(this);
                this.update(json);
                this.countsLoaded = true;

                if (parent.loadMask) {
                    parent.loadMask.hide();
                }

            }.createDelegate(this));
        },

        // this is so long because it is lifted from the original version of the summary table
        tpl: new Ext.XTemplate(
            '<div id="dataSummaryTable">'
            + '<div class="roundedcornr_box_777249" style="margin-bottom: 15px; padding: 10px; -moz-border-radius: 15px; border-radius: 15px;">'
            + '<div class="roundedcornr_content_777249">'
            + '<div id="dataSummary" style="margin-left: 15px; margin-right: 15px">'
            + '<table style="white-space: nowrap">'
            + '<tr>'
            + '<td style="padding-right: 10px">'
            + '<span style="white-space: nowrap">'
            + '&nbsp; </span>'
            + '</td>'
            + '<td style="padding-right: 10px" text-align="right">'
            + 'Total'
            + '</td>'
            + '<tpl if="drawUpdatedColumn == true">'
            + '<td style="text-align:right;padding-right: 10px">'
            + 'Updated'
            + '</td>'
            + '</tpl>'
            + '<tpl if="drawNewColumn == true">'
            + '<td style="text-align:right;padding-right: 10px">'
            + 'New'
            + '</td>'
            + '</tpl>'
            + '</tr><tr>'
            + '<td style="padding-right: 10px">'
            + '<span style="white-space: nowrap"> <!-- for IE --> '
            + '<b>Expression Experiments:</b></span>'
            + '</td>'
            + '<td style="text-align:right;padding-right: 10px">'
            + '<b><a href="' + ctxBasePath + '/expressionExperiment/showAllExpressionExperiments.html">{expressionExperimentCount}</b>'
            + '</td>'
            + '<td style="text-align:right;padding-right: 10px">'
            + '<b><a style="cursor:pointer" onClick="Gemma.ExpressionExperimentsSummaryPanel.handleIdsLink([{updatedExpressionExperimentIds}],\'{cmpId}\');">'
            + '{updatedExpressionExperimentCount}</a></b>&nbsp;&nbsp;'
            + '</td>'
            + '<td text-align="right">'
            + '<b><a style="cursor:pointer" onClick="Gemma.ExpressionExperimentsSummaryPanel.handleIdsLink([{newExpressionExperimentIds}],\'{cmpId}\');">'
            + '{newExpressionExperimentCount}</a></b>&nbsp;'
            + '</td>'
            + '</tr>'
            + '<tpl for="sortedCountsPerTaxon">'
            + '<tr>'
            + '<td style="text-align:right;padding-right: 10px">'
            + '<span style="white-space: nowrap"> <!-- for IE --> &emsp;'
            + '{taxonName}'
            + '</td><td style="text-align:right;padding-right: 10px">'
            + '<a style="cursor:pointer" onClick="Gemma.ExpressionExperimentsSummaryPanel.handleTaxonLink({taxonId},\'{parent.cmpId}\');">'
            + '{totalCount}</a>'
            + '</td><td style="text-align:right;padding-right: 10px">'
            + '<b><a style="cursor:pointer" onClick="Gemma.ExpressionExperimentsSummaryPanel.handleIdsLink([{updatedIds}],\'{parent.cmpId}\');">'
            + '{updatedCount}</a></b>&nbsp;&nbsp;'
            + '</a>'
            + '</td><td style="text-align:right">'
            + '<b><a style="cursor:pointer" onClick="Gemma.ExpressionExperimentsSummaryPanel.handleIdsLink([{newIds}],\'{parent.cmpId}\');">'
            + '{newCount}</a></b>&nbsp;' + '</a>' + '</td>' + '</tr>' + '</tpl>' + '<tr>'
            + '<td style="text-align:right;padding-right: 10px">' + '<span style="white-space: nowrap"> <!-- for IE -->'

            + '<b>Platforms:</b>  </span>' + '</td>'
            + '<td style="text-align:right;padding-right: 10px">'
            + '<a href="' + ctxBasePath + '/arrays/showAllArrayDesigns.html">' + '<b>{arrayDesignCount}</b></a>' + '</td>'
            + '<td style="text-align:right;padding-right: 10px">' + '<b>{updatedArrayDesignCount}</b>&nbsp;&nbsp;'
            + '</td>' + '<td style="text-align:right">' + '<b>{newArrayDesignCount}</b>&nbsp;' + '</td>' + '</tr>' + '<tr>'
            + '<td style="text-align:right;padding-right: 10px">'

           + '<span style="white-space: nowrap"> <!-- for IE --> <b>Samples:</b>' + '</span>' + '</td>'
            + '<td style="text-align:right;padding-right: 10px">' + '{bioMaterialCount}' + '</td>'
            + '<td style="text-align:right;padding-right: 10px">' + '&nbsp;&nbsp;' + '</td>' + '<td text-align="right">'
            + '<b>{newBioMaterialCount}</b>&nbsp;' + '</td>' + '</tr>' + '</table>' + '</div>' + '</div>' + '</div>'
            + '</div>')
    });

Gemma.ExpressionExperimentsSummaryPanel.handleIdsLink = function (ids, cmpId) {
    Ext.getCmp(cmpId).fireEvent('showExperimentsByIds', ids);
};

Gemma.ExpressionExperimentsSummaryPanel.handleTaxonLink = function (id, cmpId) {
    Ext.getCmp(cmpId).fireEvent('showExperimentsByTaxon', id);
};