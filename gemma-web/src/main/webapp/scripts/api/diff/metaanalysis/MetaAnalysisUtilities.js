/**
 * Meta-analysis utilities
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace( 'Gemma' );

Gemma.MetaAnalysisUtilities = Ext.extend( Object, {
   constructor : function() {
      var DEFAULT_THRESHOLD = 0.1;

      var DIRECTION_UP_STYLE = 'style="font-size: 12px; color: #0B6138;"'; // green
      var DIRECTION_DOWN_STYLE = 'style="font-size: 12px; color: #FF0000;"'; // red

      Ext.apply( this, {
         getDefaultThreshold : function() {
            return DEFAULT_THRESHOLD;
         },
         generateDirectionHtml : function( isUp ) {
            return '<span ' + (isUp ? DIRECTION_UP_STYLE + '>&uarr;' : DIRECTION_DOWN_STYLE + '>&darr;') + '</span>';
         }
      } );
   }
} );
