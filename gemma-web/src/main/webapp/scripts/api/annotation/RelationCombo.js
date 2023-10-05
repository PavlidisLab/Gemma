Ext.namespace("Gemma");

/**
 * Dropdown menu of relations to use in annotations such as "has_role"
 *
 * @class Gemma.RelationCombo - similar to CategoryCombo, but for relations
 * @extends Ext.form.ComboBox
 */
Gemma.RelationCombo = Ext.extend(Ext.form.ComboBox, {

   editable: true,
   mode: 'local',
   selectOnFocus: true,
   triggerAction: 'all',
   typeAhead: true,
   forceSelection: true,
   displayField: 'label',

   record: Ext.data.Record.create([{
      name: "uri"
   },  {
      name: "label"
   }
   ]),

   getTerm: function () {
      return this.selectedTerm;
   },

   initComponent: function () {

      this.dwrMethod = AnnotationController.getRelationTerms;
      this.dwrParams = [];

      this.store = new Ext.data.Store({
         proxy: new Ext.data.DWRProxy(this.dwrMethod),
         reader: new Ext.data.ListRangeReader({
            id: "uri"
         }, this.record),
         remoteSort: false,
         sortInfo: {
            field: "label"
         }
      });

      Gemma.RelationCombo.superclass.initComponent.call(this);

      this.on("change", function (combo) {
         if(combo.value){
            this.selectedTerm = combo.store.data.items[combo.selectedIndex].data;
            combo.setValue(this.selectedTerm.label);
         }else{
            this.selectedTerm = undefined;
         }
      });

      // Otherwise the combo is only firing this event after losing focus
      this.on("select", function (combo, record, index) {
         this.fireEvent("change", combo);
      });

      this.store.load({
         params: this.dwrParams
      });
   }
});
