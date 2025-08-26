Ext.namespace( 'Gemma' );

/**
 * A panel for displaying by-taxon counts of all existing, new and updated experiments in Gemma, with a column for each
 * count
 *
 * a similar table appeared on the classic Gemma's front page
 */
Gemma.ExpressionExperimentsSummaryPanel = Ext.extend( Ext.Panel,
   {
      title : "Summary and updates this week",
      collapsible : false,
      titleCollapse : false,
      animCollapse : false,

      listeners : {
         render : function() {

            this.loadCounts();

         }
      },

      constructor : function( config ) {
         Gemma.ExpressionExperimentsSummaryPanel.superclass.constructor.call( this, config );
      },

      initComponent : function() {
         Gemma.ExpressionExperimentsSummaryPanel.superclass.initComponent.call( this );
      }, // end of initComponent

      /**
       * @memberOf Gemma.ExpressionExperimentsSummaryPanel
       */
      loadCounts : function() {
         if ( this.getEl() && !this.loadMask ) {
            this.loadMask = new Ext.LoadMask( this.getEl(), {
               msg : "Loading summary ...",
               msgCls : 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
            } );
         }
         if ( this.loadMask ) {
            this.loadMask.show();
         }

         var parent = this;
         ExpressionExperimentController.loadCountsForDataSummaryTable( function( json ) {
            // update the panel with counts
            json.cmpId = Ext.id( this );
            this.update( json );
            this.countsLoaded = true;

            if ( parent.loadMask ) {
               parent.loadMask.hide();
            }

         }.createDelegate( this ) );
      },

      // this is so long because it is lifted from the original version of the summary table
      tpl : new Ext.XTemplate(
         '<div id="dataSummaryTable" class="smaller">'
         + '<div style="margin-bottom: 15px; padding: 20px;">'
         + '<div >'
         + '<div id="dataSummary" style="margin-left: 0px; margin-right: 15px">'
         + '<table style="border-spacing: 10px 2px;">'
         + '<tr>'
         + '<td></td>'
         + '<td style="text-align: right;">Total</td>'
         + '<tpl if="drawUpdatedColumn == true">'
         + '<td style="text-align: right;">Updated</td>'
         + '</tpl>'
         + '<tpl if="drawNewColumn == true">'
         + '<td style="text-align: right;">New</td>'
         + '</tpl>'
         + '</tr><tr>'
         + '<td><b>Datasets:</b></td>'
         + '<td style="text-align: right;">'
         + '<a href="' + Gemma.GEMBROW_URL + '/#">{expressionExperimentCount}'
         + '</td>'
         + '<td style="text-align: right;">'
         + '<b>{updatedExpressionExperimentCount}</b>'
         + '</td>'
         + '<td style="text-align: right;">'
         + '<b>{newExpressionExperimentCount}</b>'
         + '</td>'
         + '</tr>'
         + '<tpl for="sortedCountsPerTaxon">'
         + '<tr>'
         + '<tpl if="taxonCommonName !== null">'
         + '<td>&emsp;{taxonCommonName}</td>'
         + '<td style="text-align: right;"><a href="' + Gemma.GEMBROW_URL + '/#/t/{taxonCommonName}">{totalCount}</a></td>'
         + '</tpl>'
         + '<tpl if="taxonCommonName === null">'
         + '<td>&emsp;{taxonName}</td>'
         + '<td style="text-align: right;">{totalCount}</td>'
         + '</tpl>'
         + '<td style="text-align: right;">'
         + '<b>{updatedCount}</b>'
         + '</td>'
         + '<td style="text-align: right;">'
         + '<b>{newCount}</b>'
         + '</td>'
         + '</tr>'
         + '</tpl>'
         + '<tr>'
         + '<td><b>Platforms:</b></td>'
         + '<td style="text-align: right;">'
         + '<a href="' + Gemma.CONTEXT_PATH + '/arrays/showAllArrayDesigns.html">{arrayDesignCount}</a></td>'
         + '<td style="text-align: right;"><b>{updatedArrayDesignCount}</b></td>'
         + '<td style="text-align: right;"><b>{newArrayDesignCount}</b></td>'
         + '</tr>'
         + '<tr>'
         + '<td><b>Samples:</b></td>'
         + '<td style="text-align: right;">{bioMaterialCount}</td>'
         + '<td style="text-align: right;"><b>{updatedBioMaterialCount}</b></td>'
         + '<td style="text-align: right;"><b>{newBioMaterialCount}</b></td>'
         + '</tr>'
         + '</table>'
         + '</div>'
         + '</div>'
         + '</div>'
         + '</div>' )
   } );

Gemma.ExpressionExperimentsSummaryPanel.handleIdsLink = function( ids, cmpId ) {
   Ext.getCmp( cmpId ).fireEvent( 'showExperimentsByIds', ids );
};

Gemma.ExpressionExperimentsSummaryPanel.handleTaxonLink = function( id, cmpId ) {
   Ext.getCmp( cmpId ).fireEvent( 'showExperimentsByTaxon', id );
};