Ext.namespace( 'Gemma' );
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Ext.onReady( function() {
   // all the bioassays

   Ext.QuickTips.init();
   Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );

   var manager = new Gemma.EEManager( {
      editable : true,
      id : "eemanager"
   } );

   manager.on( 'done', function() {
      window.location.reload( true );
   } );

   var dragItems = document.getElementsByClassName( 'dragItem' );

   var windowIdArray = new Array( dragItems.length );
   for (var j = 0; j < dragItems.length; j++) {
      windowIdArray[j] = dragItems[j].id;
   }

   for (var i = 0; i < windowIdArray.length; i++) {
      var windowId = windowIdArray[i];
      // set to be draggable
      $( windowId ).draggable();
      /*
       * new Draggable(windowId, { revert :true, ghosting :true });
       */

      // set to be droppable
      $( windowId ).droppable(
         {
            drop : function( event, ui ) {

               var element = event.target;
               var droppableElement = event.draggable;

               // error check
               // if between columns (ArrayDesigns), do not allow
               if ( element.getAttribute( 'arrayDesign' ) == droppableElement.getAttribute( 'arrayDesign' ) ) {
                  // initialize variables
                  var removeFromElement = element.getAttribute( 'material' );
                  var removeFromDroppable = droppableElement.getAttribute( 'material' );
                  // swap the assays
                  var temp = element.getAttribute( 'assay' );
                  element.setAttribute( 'assay', droppableElement.getAttribute( 'assay' ) );
                  droppableElement.setAttribute( 'assay', temp );

                  // retrieve the JSON object and parse it
                  var materialString = document.getElementById( 'assayToMaterialMap' ).value;
                  var materialMap = Ext.util.JSON.decode( materialString );

                  // write the new values into the materialMap
                  materialMap[element.getAttribute( 'assay' )].push( element.getAttribute( 'material' ) );
                  materialMap[droppableElement.getAttribute( 'assay' )].push( droppableElement
                     .getAttribute( 'material' ) );

                  // remove the old values from the materialMap
                  var elementToRemove;
                  for (var k = 0; k < materialMap[element.getAttribute( 'assay' )].length; k++) {
                     if ( materialMap[element.getAttribute( 'assay' )][k] = removeFromElement ) {
                        elementToRemove = k;
                        break;
                     }
                  }

                  materialMap[element.getAttribute( 'assay' )].splice( k, 1 );
                  for (var k = 0; k < materialMap[droppableElement.getAttribute( 'assay' )].length; k++) {
                     if ( materialMap[droppableElement.getAttribute( 'assay' )][k] = removeFromDroppable ) {
                        elementToRemove = k;
                        break;
                     }
                  }

                  if ( elementToRemove != null ) {
                     materialMap[droppableElement.getAttribute( 'assay' )].splice( elementToRemove, 1 );
                  }

                  // serialize the JSON object
                  document.getElementById( 'assayToMaterialMap' ).value = Ext.util.JSON.encode( materialMap );

                  // swap inner HTML
                  var content1 = element.innerHTML;
                  var content2 = droppableElement.innerHTML;
                  droppableElement.innerHTML = content1;
                  element.innerHTML = content2;
               } else {
                  /*
                   * new Effect.Highlight(droppableElement.id, { delay :0, duration :0.25, startcolor :'#ff0000',
                   * endcolor :'#ff0000' }); new Effect.Highlight(droppableElement.id, { delay :0.5, duration :0.25,
                   * startcolor :'#ff0000', endcolor :'#ff0000' });
                   */
               }

            }
         } );
   }
} );