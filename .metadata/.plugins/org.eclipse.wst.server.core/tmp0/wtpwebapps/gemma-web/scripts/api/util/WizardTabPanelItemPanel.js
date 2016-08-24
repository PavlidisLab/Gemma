/**
 * It is a child panel inside wizard tab panel.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace( 'Gemma' );

Gemma.WizardTabPanelItemPanel = Ext.extend( Ext.Panel, {
   nextButtonText : 'Next',
   nextButtonHandler : null,

   /**
    * @memberOf Gemma.WizardTabPanelItemPanel
    */
   createNextButton : function() {
      var nextButton = new Ext.Button( {
         text : this.nextButtonText,
         handler : function( button, eventObject ) {
            if ( this.nextButtonHandler ) {
               this.nextButtonHandler.call( this );

            } else {
               this.fireEvent( 'nextButtonClicked', this );
            }
         },
         scope : this
      } );

      return nextButton;
   },
   maskWindow : function( msg ) {
      var window = this.findParentBy( function( container ) {
         return container instanceof Ext.Window;
      } );

      if ( window ) {
         window.getEl().mask( msg == null ? 'Loading ...' : msg );
      }
   },
   unmaskWindow : function() {
      var window = this.findParentBy( function( container ) {
         return container instanceof Ext.Window;
      } );

      if ( window ) {
         window.getEl().unmask();
      }
   }
} );
