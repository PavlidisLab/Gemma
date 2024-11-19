/**
 * Draggable bioassays association with biomaterials.
 */

Ext.namespace( 'Gemma' );
Ext.BLANK_IMAGE_URL = Gemma.CONTEXT_PATH + '/images/default/s.gif';
Ext.onReady( function() {

   Ext.QuickTips.init();
   Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );

   var manager = new Gemma.EEManager( {
      editable : true,
      id : "eemanager"
   } );

   manager.on( 'done', function() {
      window.location.reload( true );
   } );

   // see AssayViewTag

} );

jQuery.fn.swap = function( b ) {
   // method from: http://blog.pengoworks.com/index.cfm/2008/9/24/A-quick-and-dirty-swap-method-for-jQuery
   b = jQuery( b )[0];
   var a = this[0];
   var t = a.parentNode.insertBefore( document.createTextNode( '' ), a );
   b.parentNode.insertBefore( a, b );
   t.parentNode.insertBefore( b, t );
   t.parentNode.removeChild( t );
   return this;
};

$( function() {

   $( '.dragItem' ).draggable( {
      axis : 'y',
      cursor : 'move',
      revert : true
   } );

   $( '.dragItem' ).droppable(
      {
         accept : '.dragItem',
         activeClass : "ui-state-hover",
         hoverClass : "ui-state-active",
         drop : function( event, ui ) {

            var target = $( this ); // the cell we dropped on to.
            var source = ui.draggable; // the item that was dropped.

            // if between columns (ArrayDesigns), do not allow
            if ( target.attr( 'arrayDesign' ) != source.attr( 'arrayDesign' ) ) {
               return;
            }

            // deserialize the JSON object ; this is the data that will be used on the server.
            var materialString = $( 'input#assayToMaterialMap' ).val();
            var materialMap = Ext.util.JSON.decode( materialString ); // map of bioassay id to biomaterial id

            var targetBioMaterial = target.attr( 'material' );
            var sourceBioMaterial = source.attr( 'material' );
            console.log( "dropped " + sourceBioMaterial + ' on ' + targetBioMaterial );

            // write the new values into the materialMap
            materialMap[target.attr( 'assay' )] = sourceBioMaterial;
            materialMap[source.attr( 'assay' )] = targetBioMaterial;

            target.attr( 'material', sourceBioMaterial );
            source.attr( 'material', targetBioMaterial );

            // debugger;
            console.log( 'Bioassay ' + target.attr( 'assay' ) + " will be associated with "
               + materialMap[target.attr( 'assay' )] + " and in the html it is " + target.attr( 'material' ) );

            console.log( 'Bioassay ' + source.attr( 'assay' ) + " will be associated with "
               + materialMap[source.attr( 'assay' )] + " and in the html it is " + source.attr( 'material' ) );

            // swap the dom items
            // put the target where the source was.
            source.swap( target );

            // put in the right place.
            source.position( {
               of : $( this ),
               my : 'left top',
               at : 'left top'
            } );

            // reserialize the JSON object
            $( 'input#assayToMaterialMap' ).val( Ext.util.JSON.encode( materialMap ) );

         }
      } );
} );
