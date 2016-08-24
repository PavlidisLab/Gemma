Ext.namespace( 'Gemma' );
/**
 * Based on probe.grid.js, but more self-contained. Shows a list of elements; when you click on one the details are
 * displayed in a separate panel.
 * 
 * @author paul
 */
Gemma.PlatformElementsPanel = Ext
   .extend(
      Ext.Panel,
      {

         padding : 10,

         /**
          * @memberOf Gemma.PlatformElementsPanel
          */
         initComponent : function() {
            var sdp = new Gemma.SequenceDetailsPanel( {
               autoHeight : true,
               width : 600,
               id : 'sdp'
            } );

            Ext
               .apply(
                  this,
                  {
                     items : [
                              sdp,
                              new Gemma.PlatformElementGrid( {
                                 arrayDesignId : this.platformId,
                                 height : 400,
                                 width : 600,
                                 listeners : {
                                    'select' : function( r ) {
                                       sdp.updateSequenceInfo( r );
                                    }
                                 }
                              } ),
                              {
                                 border : false,
                                 padding : 8,
                                 width : 400,
                                 html : 'Only a subset of elements for the platform are shown. Use the search function to find specific items'
                              } ]
                  } );

            Gemma.PlatformElementsPanel.superclass.initComponent.call( this );
         }

      } );