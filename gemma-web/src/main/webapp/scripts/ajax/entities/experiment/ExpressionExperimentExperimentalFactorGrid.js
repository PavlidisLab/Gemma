Ext.namespace('Gemma');

/**
 * A grid holding the experiments and their associated factors.
 * 
 * @author keshav
 * @version $Id$
 * @class Gemma.ExperimentalFactorGrid
 * @extends Gemma.GemmaGridPanel
 */
Gemma.ExpressionExperimentExperimentalFactorGrid = Ext.extend(Ext.grid.PropertyGrid, {

      loadMask : {
         msg : Gemma.StatusText.Loading.experimentFactors
      },
      collapsible : true,
      editable : true,
      stateful : false,
      autoScroll : true,
      height : 250,

      /**
       * Implementation note: PropertyGrid does not support custom cell renders the way regular Grids do, so we
       * preformat the Factor name on the server.
       */

      initComponent : function() {

         var source = [];
         var customEditors = [];
         for (i in this.data) {
            var d = this.data[i];
            if (d.expressionExperiment) {

               var s = new Ext.data.SimpleStore({
                     fields : [{
                           name : 'id',
                           type : 'int'
                        }, {
                           name : 'name',
                           type : 'string'
                        }, {
                           name : 'category', // e.g. DiseaseState
                           type : 'string'
                        }, {
                           name : 'categoryUri',
                           type : 'string'
                        }, {
                           name : 'factorValues', // string describing the factor values.
                           type : 'string'
                        }, {
                           name : 'numValues',
                           type : 'int'
                        }]
                  });

               var myData = [];

               var combo = new Ext.form.ComboBox({
                     store : s,
                     editable : false,
                     forceSelection : true,
                     displayField : 'name',
                     selectOnFocus : true,
                     triggerAction : 'all',
                     mode : 'local'
                  });

               for (j in d.experimentalFactors) {
                  var f = d.experimentalFactors[j];
                  if (f.id) {
                     var row = [f.id, f.name, f.category, f.categoryUri, f.factorValues, f.numValues];
                     myData.push(row);
                  }
               }

               s.on("load", function(store, records, options) {
                     if (records.length < 2) {
                        /* disable the combo using this store */
                        this.disable();
                     }
                  }, combo);

               customEditors[d.expressionExperiment.name] = new Ext.grid.GridEditor(combo);
               source[d.expressionExperiment.name] = d.experimentalFactors[0].name;

               s.loadData(myData);
            }
         }

         Ext.apply(this, {
               source : source,
               customEditors : customEditors
            });

         Gemma.ExpressionExperimentExperimentalFactorGrid.superclass.initComponent.call(this);

         this.originalTitle = this.title;
         this.colModel.config[0].header = "Experiments";
         this.colModel.config[1].header = "Factors";
      }
   });
