Ext.namespace( 'Gemma' );
/**
 * global pub/sub
 * 
 * @class Gemma.EVENTBUS
 * 
 * @singleton
 */
Ext.define( 'Gemma.EVENTBUS', {
   singleton : true, // does anything in ext 3.4?
   extend : 'Ext.util.Observable',
   constructor : function( config ) {
      this.callParent( arguments );
   }
} );
