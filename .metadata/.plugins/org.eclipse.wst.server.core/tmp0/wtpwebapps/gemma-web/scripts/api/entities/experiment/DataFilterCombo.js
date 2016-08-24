/**
 * Combobox to filter data
 * 
 * @class Gemma.DataFilterCombo
 * @extends Ext.form.ComboBox
 * @version $Id$
 */
Ext.namespace( 'Gemma' );

/**
 * 
 */
Gemma.DataFilterCombo = Ext.extend( Ext.form.ComboBox, {
   editable : false,
   width : 150,
   triggerAction : 'all',
   lazyRender : true,
   mode : 'local',
   defaultValue : '50',
   emptyText : "Number to display",
   store : new Ext.data.ArrayStore( {
      fields : [ 'count', 'displayText' ],
      data : [ [ 50, '50 recently updated' ], [ 100, '100 recently updated' ], [ 200, '200 recently updated' ],
              [ 300, '300 recently updated' ], [ 500, '500 recently updated' ], [ -50, '50 oldest updates' ],
              [ -100, '100 oldest updates' ], [ -200, '200 oldest updates' ], [ -300, '300 oldest updates' ],
              [ -500, '500 oldest updates' ] ]
   } ),
   valueField : 'count',
   displayField : 'displayText'
} );