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
         '<div id="dataSummaryTable">'
          + '<div id="dataSummary" style="margin-left: 15px; margin-right: 15px">'
         + '<table style="white-space: nowrap">'
         + '<tr>'
         + '<td>'
         + '</td>'
         + '<td style="padding:5px">'
         + '<b>Total</b>'
         + '</td>'
         + '<tpl if="drawUpdatedColumn == true">'
         + '<td style="padding:5px">'
         + '<b>Updated</b>'
         + '</td> </tpl>'
         + '<tpl if="drawNewColumn == true">'
         + '<td style="padding:5px">'
         + '<b>New</b>'
         + '</td> </tpl> </tr><tr> <td style="padding-right: 10px">'
         + '<b>Expression Experiments:</b>'
         + '</td>'
         + '<td style="text-align:right">'
         //  + '<b><a href="' + ctxBasePath + '/expressionExperiment/showAllExpressionExperiments.html">{expressionExperimentCount}</b>'
         + '<b><a href="' + ctxBasePath + '/browse/">{expressionExperimentCount}</b>'
         + '</td>'
         + '<td style="text-align:right"> <b>'
         //+'<a style="cursor:pointer" onClick="Gemma.ExpressionExperimentsSummaryPanel.handleIdsLink([{updatedExpressionExperimentIds}],\'{cmpId}\');">'
         + '{updatedExpressionExperimentCount}'
         //+ '</a>'
         + '</b> </td> <td style="text-align:right"><b>'
         //+ '<a style="cursor:pointer" onClick="Gemma.ExpressionExperimentsSummaryPanel.handleIdsLink([{newExpressionExperimentIds}],\'{cmpId}\');">'
         + '{newExpressionExperimentCount}'
         //+ '</a>'
         + '</b> </td> </tr>'
         + ' <tpl for="sortedCountsPerTaxon"> <tr> <td style="text-align:right;padding-right: 10px">'
         + '{taxonName}'
         + '</td><td style="text-align:right">'
         // + '<a style="cursor:pointer" onClick="Gemma.ExpressionExperimentsSummaryPanel.handleTaxonLink({taxonId},\'{parent.cmpId}\');">'
         + '{totalCount}'
         //+ '</a>'
         + '</td><td style="text-align:right"> <b>'
         // + '<a style="cursor:pointer" onClick="Gemma.ExpressionExperimentsSummaryPanel.handleIdsLink([{updatedIds}],\'{parent.cmpId}\');">'
         + '{updatedCount}'
         // + '</a>'
         + '</b></td><td style="text-align:right"><b>'
         //+ '<a style="cursor:pointer" onClick="Gemma.ExpressionExperimentsSummaryPanel.handleIdsLink([{newIds}],\'{parent.cmpId}\');">'
         + '{newCount}'
         //+'</a>'
         + '</b></td> </tr> </tpl> <tr>'
         + '<td style="text-align:right;padding-right: 10px">'

         + '<b>Platforms:</b>  </span>' + '</td>'
         + '<td style="text-align:right">'
         + '<a href="' + ctxBasePath + '/arrays/showAllArrayDesigns.html">' + '<b>{arrayDesignCount}</b></a>' + '</td>'
         + '<td style="text-align:right">' + '<b>{updatedArrayDesignCount}</b> '
         + '</td>' + '<td style="text-align:right">' + '<b>{newArrayDesignCount}</b> </td> </tr> <tr>'
         + '<td style="text-align:right;padding-right: 10px">'

         + '<b>Samples:</b></td>'
         + '<td style="text-align:right">' + '{bioMaterialCount}' + '</td>'
         + '<td style="text-align:right"></td><td style="text-align:right">'
         + '<b>{newBioMaterialCount}</b></td></tr> </table> </div> '
          + '</div>' )
   } );

Gemma.ExpressionExperimentsSummaryPanel.handleIdsLink = function( ids, cmpId ) {
   Ext.getCmp( cmpId ).fireEvent( 'showExperimentsByIds', ids );
};

Gemma.ExpressionExperimentsSummaryPanel.handleTaxonLink = function( id, cmpId ) {
   Ext.getCmp( cmpId ).fireEvent( 'showExperimentsByTaxon', id );
};