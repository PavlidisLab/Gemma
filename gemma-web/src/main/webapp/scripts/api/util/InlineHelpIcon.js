/**
 * Button extension to make creating uniform inline help icons easy
 * 
 * you must pass in the text/html for the tooltip as config property "tooltipText"
 * 
 * (note: the "ext:qtip" markup doesn't work in IE9)
 * 
 * 
 * @class Gemma.InlineHelpIcon
 * @extends Ext.Button
 */
Gemma.InlineHelpIcon = Ext.extend( Ext.Button, {

   icon : Gemma.CONTEXT_PATH + "/images/icons/question_blue.png",
   padding : '3px',
   cls : 'transparent-btn',
   initComponent : function() {

      Gemma.InlineHelpIcon.superclass.initComponent.call( this );

      Ext.apply( this, {
         tooltip : this.tooltipText
      } );
   }
} );

Ext.reg( 'Gemma.InlineHelpIcon', Gemma.InlineHelpIcon );