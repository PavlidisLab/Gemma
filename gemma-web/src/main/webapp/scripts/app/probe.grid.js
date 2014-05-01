/*
 * The 'probe viewer' application.
 * 
 * This handles situations where we're viewing probes from an array design, or those for a specific gene. For array
 * designs it allows searches.
 * 
 * @author Paul
 * 
 * @version $Id$
 */
Ext.namespace( 'Gemma' );

Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

/**
 * 
 */
Gemma.ProbeBrowser.app = (function() {

   return {
      init : function() {
         Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );
         Ext.QuickTips.init();

         var csidstr = Ext.get( "cslist" ).getValue();

         if ( csidstr ) {
            var csids = csidstr.split( ',' );
         }

         var arrayDesignId = Ext.get( "arrayDesignId" ).getValue();

         this.initDetails();
         this.initMainGrid( arrayDesignId, csids );
       },

      /**
       * Initialize the main grid.
       * 
       * @param {boolean}
       *           isArrayDesign
       */
      initMainGrid : function( arrayDesignId, csIds ) {
         this.mainGrid = new Gemma.ProbeGrid( {
            csIds : csIds,
            arrayDesignId : arrayDesignId,
            detailsDataSource : this.detailsGrid.getStore(),
            renderTo : "probe-grid",

            height : 350,
            width : 630
         } );

      },

      /**
       * Separate grid for 'details' about the probe and its alignment results.
       */

      initDetails : function() {
         this.detailsGrid = new Gemma.ProbeDetailsGrid( {
            renderTo : "probe-details",
            height : 100,
            width : 620
         } );
      },

      /**
       * Used for displaying on the details of a given probe (for probe details page)
       */

      initOneDetail : function() {
         // create the grid for details.
         this.initDetails();
         // Get this id
         var csId = dwr.util.getValue( "cs" );

         // Load the details to be displayed.
         this.detailsGrid.getStore().load( {
            params : [ {
               id : csId
            } ]
         } );

      }
   };
}());
