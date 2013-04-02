/**
 * @deprecated
 */
Gemma.DiffExpressionGridRowExpander = function(config) {

   this.expandedElements = [];

   Gemma.DiffExpressionGridRowExpander.superclass.constructor.call(this, config);

};

/*
 * instance methods...
 */
Ext.extend(Gemma.DiffExpressionGridRowExpander, Ext.grid.RowExpander, {

   beforeExpand : function(record, body, rowIndex) {
      if ( this.fireEvent('beforeexpand', this, record, body, rowIndex) !== false ) {

         /*
          * Clear out old tables, otherwise they pile up. I haven't figured out a good way to cache this.
          */
         var bodyEl = new Ext.Element(body);
         Ext.DomHelper.overwrite(bodyEl, "<div height='300'></div>");

         var diffExGrid = new Gemma.ProbeLevelDiffExGrid({ width : 750,
            title : "Probe-level results for " + record.get("gene"), renderTo : bodyEl });

         var supporting = this.getDatasetRecords(record);
         diffExGrid.getStore().loadData(supporting);

         // Keep mouse events from propogating to the parent grid. See ExtJS
         // forums topic "nested grids problem" (242878).
         diffExGrid.getEl().swallowEvent([ 'mouseover', 'mousedown', 'click', 'dblclick' ]);

         return true;
      }
      return false;
   },

   getDatasetRecords : function(record) {
      return record.data.probeResults;
   },

   clearCache : function() {
      for ( var i = 0; i < this.expandedElements.length; ++i) {
         if ( this.expandedElements[i] ) {
            for ( var j = 0; j < this.expandedElements[i].length; ++j) {
               // grid.destroy() seems to be broken...
               try {
                  this.expandedElements[i][j].destroy();
               } catch (e) {
               }
            }
            this.expandedElements[i] = null;
         }
      }
   }

});
