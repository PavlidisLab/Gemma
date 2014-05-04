Ext.namespace( "Gemma" );

/**
 * 
 * @class Gemma.EvidenceCodeCombo
 * @extends Ext.form.ComboBox
 * @author paul (based on CategoryCombo)
 * @version $Id$
 */
Gemma.EvidenceCodeCombo = Ext.extend( Ext.form.ComboBox, {

   editable : true,
   mode : 'local',
   selectOnFocus : true,
   triggerAction : 'all',
   forceSelection : true,
   displayField : 'code',

   getCode : function() {
      return this.selectedCode;
   },

   /**
    * @memberOf Gemma.EvidenceCodeCombo
    */
   initComponent : function() {

      this.store = new Ext.data.Store( {
         proxy : new Ext.data.MemoryProxy( [ [ "IEA" ], [ "IC" ] ] ),
         reader : new Ext.data.ArrayReader( {}, [ {
            name : "code"
         } ] )
      } );

      Gemma.EvidenceCodeCombo.superclass.initComponent.call( this );

      this.store.load();

      this.on( "select", function( combo, record, index ) {
         this.selectedCode = record.data;
      } );

   }
} );
