Ext.namespace("Gemma");

/**
 * Dropdown menu of categories to use in annotations (previously known as MGEDCombo)
 * 
 * @class Gemma.CategoryCombo
 * @extends Ext.form.ComboBox
 * @version $Id$
 */
Gemma.CategoryCombo = Ext.extend(Ext.form.ComboBox, {

      editable : true,
      mode : 'local',
      selectOnFocus : true,
      triggerAction : 'all',
      typeAhead : true,
      forceSelection : true,
      displayField : 'term', 

      record : Ext.data.Record.create([{
            name : "uri"
         }, {
            name : "term"
         }, {
            name : "comment"
         }]),

      getTerm : function() {
         return this.selectedTerm;
      },

      initComponent : function() {

         this.dwrMethod = OntologyService.getCategoryTerms;
         this.dwrParams = [];

         this.store = new Ext.data.Store({
               proxy : new Ext.data.DWRProxy(this.dwrMethod),
               reader : new Ext.data.ListRangeReader({
                     id : "uri"
                  }, this.record),
               remoteSort : false,
               sortInfo : {
                  field : "term"
               }
            });

         Gemma.CategoryCombo.superclass.initComponent.call(this);

         // so that tabbing away still results in a 'select'. I wish there was a better way to do this.
         this.on("change", function(combo) {
               if (this.getValue()) {
                  var ix = this.getStore().find("term", this.getValue());
                  var rec = this.getStore().getAt(ix);
                  this.select(ix, true);
                  this.fireEvent('select', this, rec, ix);
               }
            });

         this.on("select", function(combo, record, index) {
               this.selectedTerm = record.data;
            });

         this.store.load({
               params : this.dwrParams
            });
      }
   });
