/** the data source component in the neurocarta filter * */
Ext.namespace('Gemma');
Gemma.ExternalDatabaseGrid = Ext.extend(Ext.grid.GridPanel, {

      initComponent : function() {

         var checkboxSelectionModel = new Ext.grid.CheckboxSelectionModel({

               singleSelect : false,

               listeners : {
                  rowdeselect : function(selectionModel, rowIndex, record) {
                     // I must defer changing the field isChecked. Otherwise,
                     // other events such as cellclick will not be fired.
                     Ext.defer(function() {
                           record.set('isChecked', false);
                        }, 500);
                  },
                  rowselect : function(selectionModel, rowIndex, record) {
                     // I must defer changing the field isChecked. Otherwise,
                     // other events such as cellclick will not be fired.
                     Ext.defer(function() {
                           record.set('isChecked', true);
                        }, 500);
                  },
                  scope : this
               }
            });

         var store = new Ext.data.Store({
               autoLoad : true,

               proxy : new Ext.data.DWRProxy(PhenotypeController.findExternalDatabaseName),

               reader : new Ext.data.JsonReader({
                     fields : ['id', {
                           name : 'name',
                           sortType : Ext.data.SortTypes.asUCString
                        }]
                  }),
               sortInfo : {
                  field : 'name',
                  direction : 'ASC'
               }
            });

         Ext.apply(this, {

               collapsed : true,
               collapsible : true,
               fieldLabel : 'Data source',
               title : 'Select',
               listeners : {
                  render : function(grid) {
                     // this to hide the column header
                     grid.getView().el.select('.x-grid3-header').setStyle('display', 'none');
                  },

                  collapse : function(p) {
                     Ext.getCmp('filterMenu').doLayout();
                  },

                  expand : function(p) {
                     Ext.getCmp('filterMenu').doLayout();
                  }
               },

               bodyBorder : false,
               viewConfig : {
                  forceFit : true
               },
               height : 280,
               width : 250,
               loadMask : true,
               record : Ext.data.Record.create([{
                     name : "id",
                     type : "int"
                  }, {
                     name : "name",
                     type : "string"
                  }, {
                     name : "isChecked",
                     type : "boolean"
                  }]),
               store : store,
               columns : [checkboxSelectionModel, {
                     header : "External Database",
                     dataIndex : "name",
                     width : 0.25
                  }],
               sm : checkboxSelectionModel

            });
         Gemma.AuditTrailGrid.superclass.initComponent.call(this);
      },

      // get all id of selected database
      getChosenDatabasesId : function() {

         var arr = [];

         this.store.each(function(rec) {
               if (rec.data.isChecked) {
                  arr.push(rec.data.id);
               }
            });
         return arr;
      },

      // get all names of selected database
      getChosenDatabasesName : function() {

         var result = '';

         this.store.each(function(rec) {
               if (rec.data.isChecked) {
                  result = result + " " + rec.data.name;
               }
            });
         return result;
      }

   });
