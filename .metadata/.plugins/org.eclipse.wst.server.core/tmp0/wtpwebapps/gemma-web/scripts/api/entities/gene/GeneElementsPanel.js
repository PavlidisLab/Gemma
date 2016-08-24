Ext.namespace( 'Gemma' );
/**
 * Based on probe.grid.js, but more self-contained. Shows a list of elements; when you click on one the details are
 * displayed in a separate panel.
 * 
 * @author paul
 */
Gemma.GeneElementsPanel = Ext.extend( Ext.Panel, {

   padding : 10,

   /**
    * @memberOf Gemma.GeneElementsPanel
    */
   initComponent : function() {
      var sdp = new Gemma.SequenceDetailsPanel( {
         autoHeight : true,
         width : 600,
         id : 'sdp'
      } );

      Ext.apply( this, {
         items : [ sdp, new Gemma.PlatformElementGrid( {
            geneId : this.geneId,
            height : 400,
            width : 600,
            listeners : {
               'select' : function( r ) {
                  sdp.updateSequenceInfo( r );
               }
            }
         } ) ]
      } );

      Gemma.GeneElementsPanel.superclass.initComponent.call( this );
   }

} );