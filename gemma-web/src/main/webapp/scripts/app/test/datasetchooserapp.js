Ext.BLANK_IMAGE_URL = Gemma.CONTEXT_PATH + '/images/default/s.gif';

Ext.namespace('Gemma.DatasetChooser');

Gemma.DatasetChooser.app = (function() {

   var btn;
   var dcp;

   return {
      init : function() {

         Ext.QuickTips.init();
         Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

         // gcp = new Gemma.GeneGrid({
         // frame : true,
         // renderTo : 'but',
         // width : 400,
         // height : 150
         // });

         this.dg = new Gemma.DiffExpressionExperimentGrid({
               renderTo : 'but',

               height : 100,
               width : 400
            });

         this.dg.getStore().load({
               params : [544510, 0.01]
            });

         this.dg.getView().refresh();

         // tc = new Ext.Gemma.TaxonCombo({
         // renderTo : 'but'
         // });
         //
         // dcp = new Ext.Gemma.DatasetGroupComboPanel({
         // renderTo : 'but'
         // });
         //
         // tc.on("select", function(combo, record, index) {
         // dcp.filterByTaxon(record.data);
         // });
         //
         // tf = new Ext.form.TextField({
         // renderTo : 'but',
         // width : 150
         // });
         //
         // dcp.on("set-chosen", function(e) {
         // tf
         // .setValue(e.get("expressionExperimentIds").length
         // + " ee ids");
         // });

      }
   };
}());